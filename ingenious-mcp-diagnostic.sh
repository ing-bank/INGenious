#!/usr/bin/env bash
################################################################################
# INGenious MCP Diagnostic Tool
#
# This script diagnoses why the Ingenious MCP server may not be starting on
# some machines. It performs systematic checks and provides actionable feedback.
#
# Usage:
#   bash ingenious-mcp-diagnostic.sh
#   bash ingenious-mcp-diagnostic.sh --verbose
#   bash ingenious-mcp-diagnostic.sh --fix
################################################################################

set -o pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

VERBOSE=0
AUTOFIX=0
PASSED=0
FAILED=0
WARNINGS=0

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --verbose|-v) VERBOSE=1; shift ;;
        --fix) AUTOFIX=1; shift ;;
        *) echo "Unknown option: $1"; exit 1 ;;
    esac
done

print_header() {
    echo -e "\n${BLUE}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}\n"
}

print_pass() {
    echo -e "${GREEN}✓${NC} $1"
    ((PASSED++))
}

print_fail() {
    echo -e "${RED}✗${NC} $1"
    ((FAILED++))
}

print_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
    ((WARNINGS++))
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

print_hint() {
    echo -e "  ${YELLOW}→${NC} $1"
}

# ==============================================================================
# CHECK 1: CLI Resolution
# ==============================================================================

check_cli_resolution() {
    print_header "CHECK 1: CLI Path Resolution"

    # Try to find ingenious CLI
    local cli_path=""
    
    # First, check if ingenious is on PATH
    if command -v ingenious &>/dev/null; then
        cli_path=$(command -v ingenious)
        print_pass "Found 'ingenious' on PATH: $cli_path"
    else
        print_fail "'ingenious' command not found on PATH"
        print_hint "Install INGenious CLI or add it to PATH"
        print_hint "Check: echo \$PATH"
        return 1
    fi

    # Verify it's executable
    if [[ -x "$cli_path" ]]; then
        print_pass "ingenious is executable"
    else
        print_fail "ingenious is not executable: $cli_path"
        print_hint "Fix with: chmod +x '$cli_path'"
        return 1
    fi

    # Test basic functionality
    if "$cli_path" --version &>/dev/null; then
        local version=$("$cli_path" --version 2>&1 | head -1)
        print_pass "ingenious --version works: $version"
    else
        print_fail "ingenious --version failed"
        return 1
    fi

    return 0
}

# ==============================================================================
# CHECK 2: Java Installation and Configuration
# ==============================================================================

check_java_setup() {
    print_header "CHECK 2: Java Installation & Configuration"

    # Check if Java is available
    if ! command -v java &>/dev/null; then
        print_fail "Java not found on PATH"
        print_hint "Install Java 17+ or set JAVA_HOME"
        return 1
    fi
    print_pass "Java found on PATH"

    # Check Java version
    local java_version=$(java -version 2>&1 | grep -oP '(?<=version ").*?(?=")' | head -1)
    if [[ -z "$java_version" ]]; then
        java_version=$(java -version 2>&1 | head -1)
    fi
    print_pass "Java version: $java_version"

    # Extract major version
    local major_version=$(echo "$java_version" | grep -oP '^\d+' | head -1 || echo "0")
    if [[ $major_version -lt 17 ]]; then
        print_fail "Java version is $major_version, but INGenious requires Java 17+"
        print_hint "Install Java 17+ and update JAVA_HOME if needed"
        return 1
    fi
    print_pass "Java version $major_version is compatible"

    # Check JAVA_HOME
    if [[ -z "$JAVA_HOME" ]]; then
        print_warn "JAVA_HOME is not set (this is usually OK if java works)"
    else
        print_pass "JAVA_HOME is set: $JAVA_HOME"
        
        if [[ ! -d "$JAVA_HOME" ]]; then
            print_fail "JAVA_HOME points to non-existent directory: $JAVA_HOME"
            return 1
        fi
        
        if [[ ! -f "$JAVA_HOME/bin/java" ]]; then
            print_fail "JAVA_HOME/bin/java not found: $JAVA_HOME/bin/java"
            return 1
        fi
        print_pass "JAVA_HOME/bin/java exists and is valid"
    fi

    return 0
}

# ==============================================================================
# CHECK 3: INGenious Installation
# ==============================================================================

check_ingenious_installation() {
    print_header "CHECK 3: INGenious Installation Structure"

    # Check for typical INGenious directories
    local missing=0
    
    for dir in "Resources" "Configuration" "Projects"; do
        if [[ -d "$dir" ]]; then
            print_pass "Directory exists: $dir"
        else
            print_warn "Directory not found: $dir"
            ((missing++))
        fi
    done

    if [[ -f "pom.xml" ]]; then
        print_pass "Found pom.xml (Maven project)"
    else
        print_warn "No pom.xml found (not a Maven project, may be OK)"
    fi

    if [[ -d ".git" ]]; then
        print_pass "Git repository found"
    else
        print_warn "Not a git repository"
    fi

    return 0
}

# ==============================================================================
# CHECK 4: MCP Server Startup Simulation
# ==============================================================================

check_mcp_startup() {
    print_header "CHECK 4: MCP Server Startup Test"

    local cli_path=$(command -v ingenious)
    if [[ -z "$cli_path" ]]; then
        print_fail "ingenious CLI not found (checked in CHECK 1)"
        return 1
    fi

    # Test if MCP server can start and respond to initialize
    print_info "Testing MCP server handshake..."
    
    local init_request='{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"diagnostic","version":"1.0"}}}'
    
    if [[ $VERBOSE -eq 1 ]]; then
        print_info "Sending: $init_request"
    fi

    # Run MCP server with a timeout
    local response=$(timeout 30 bash -c "echo '$init_request' | '$cli_path' server mcp 2>/dev/null" 2>&1)
    local exit_code=$?

    if [[ $exit_code -eq 124 ]]; then
        print_fail "MCP server timed out after 30 seconds (may indicate classpath issue)"
        print_hint "Check if Java startup is slow or classpath expansion is failing"
        return 1
    elif [[ $exit_code -ne 0 ]]; then
        print_fail "MCP server exited with code $exit_code"
        if [[ -n "$response" ]]; then
            print_hint "Error output: $response"
        fi
        return 1
    fi

    if echo "$response" | grep -q '"jsonrpc":"2.0"'; then
        print_pass "MCP server responded to initialize request"
        
        if echo "$response" | grep -q '"serverInfo"'; then
            print_pass "Server info included in response (full initialization worked)"
        else
            print_warn "Response received but may be incomplete"
        fi
    else
        print_fail "MCP server did not respond with valid JSON-RPC"
        if [[ -n "$response" ]]; then
            print_hint "Response: ${response:0:100}..."
        fi
        return 1
    fi

    if [[ $VERBOSE -eq 1 && -n "$response" ]]; then
        print_info "Full response: $response"
    fi

    return 0
}

# ==============================================================================
# CHECK 5: Classpath Validation
# ==============================================================================

check_classpath() {
    print_header "CHECK 5: Java Classpath Validation"

    # Get Java classpath
    local classpath=$(java -XshowSettings:properties -version 2>&1 | grep 'java.class.path' | sed 's/.*java.class.path = //')
    
    if [[ -z "$classpath" ]]; then
        print_warn "Could not determine classpath from java"
        return 0
    fi

    print_info "Java classpath:"
    
    # Split and validate each entry
    local entry_count=0
    local invalid_count=0
    IFS=':' read -ra entries <<< "$classpath"
    
    for entry in "${entries[@]}"; do
        if [[ -z "$entry" ]]; then
            continue
        fi
        
        ((entry_count++))
        
        # Check if it's a wildcard entry
        if [[ "$entry" == *"*" ]]; then
            local dir="${entry%/*}"
            if [[ -d "$dir" ]]; then
                local jar_count=$(find "$dir" -maxdepth 1 -name "*.jar" 2>/dev/null | wc -l)
                if [[ $jar_count -gt 0 ]]; then
                    print_pass "Wildcard entry has JARs: $entry ($jar_count jars)"
                else
                    print_fail "Wildcard entry has no JARs: $entry"
                    ((invalid_count++))
                fi
            else
                print_fail "Wildcard directory does not exist: $dir"
                ((invalid_count++))
            fi
        else
            # Regular entry
            if [[ -f "$entry" ]] || [[ -d "$entry" ]]; then
                print_pass "Classpath entry exists: ${entry:0:60}..."
            else
                print_fail "Classpath entry does not exist: $entry"
                ((invalid_count++))
            fi
        fi
    done

    if [[ $entry_count -eq 0 ]]; then
        print_fail "Classpath is empty"
        return 1
    fi

    print_info "Total classpath entries: $entry_count (invalid: $invalid_count)"
    
    if [[ $invalid_count -gt 0 ]]; then
        print_warn "Some classpath entries are invalid"
        return 1
    fi

    return 0
}

# ==============================================================================
# CHECK 6: Network and Ports (for REST API)
# ==============================================================================

check_network() {
    print_header "CHECK 6: Network Configuration"

    # Check if port 8090 is available (default REST API port)
    if command -v lsof &>/dev/null; then
        if lsof -i :8090 &>/dev/null; then
            print_warn "Port 8090 is in use (REST API default port)"
        else
            print_pass "Port 8090 is available"
        fi
    else
        print_info "lsof not available, skipping port check"
    fi

    # Check localhost connectivity
    if ping -c 1 127.0.0.1 &>/dev/null; then
        print_pass "Localhost (127.0.0.1) is reachable"
    else
        print_warn "Cannot ping localhost"
    fi

    return 0
}

# ==============================================================================
# CHECK 7: Platform-Specific Issues
# ==============================================================================

check_platform() {
    print_header "CHECK 7: Platform-Specific Checks"

    local os=$(uname -s)
    case "$os" in
        Darwin)
            print_pass "Running on macOS: $(sw_vers -productVersion)"
            
            # Check for Rosetta/ARM issues
            if uname -m | grep -q arm64; then
                print_info "Running on ARM64 (Apple Silicon)"
                
                # Check Java architecture
                if java -version 2>&1 | grep -q "64-Bit"; then
                    print_pass "Java is 64-bit"
                else
                    print_warn "Java may not be native ARM64"
                fi
            fi
            
            # Check for xattr issues (code signature problems)
            local cli_path=$(command -v ingenious 2>/dev/null)
            if [[ -n "$cli_path" ]]; then
                if xattr -l "$cli_path" 2>&1 | grep -q "com.apple.quarantine"; then
                    print_warn "ingenious has quarantine attribute (may be blocked on first run)"
                    print_hint "Run: xattr -d com.apple.quarantine '$cli_path'"
                fi
            fi
            ;;
        Linux)
            print_pass "Running on Linux: $(uname -r)"
            
            # Check glibc
            if ldd --version 2>/dev/null | head -1 | grep -q glibc; then
                print_pass "glibc is available"
            fi
            ;;
        MINGW*|MSYS*|CYGWIN*)
            print_pass "Running on Windows"
            ;;
        *)
            print_warn "Unknown OS: $os"
            ;;
    esac

    return 0
}

# ==============================================================================
# CHECK 8: VS Code Extension Configuration
# ==============================================================================

check_vscode_config() {
    print_header "CHECK 8: VS Code Extension Configuration"

    # Look for VS Code settings
    local settings_file="$HOME/.config/Code/User/settings.json"
    if [[ -f "$settings_file" ]]; then
        print_pass "Found VS Code settings: $settings_file"
        
        if grep -q '"ingenious' "$settings_file"; then
            print_pass "Found ingenious configuration in settings"
            
            if [[ $VERBOSE -eq 1 ]]; then
                print_info "Ingenious settings:"
                grep '"ingenious' "$settings_file" | sed 's/^/  /'
            fi
        else
            print_info "No ingenious configuration found (using defaults)"
        fi
    else
        print_info "VS Code settings not found (may not be using VS Code)"
    fi

    # Check for .vscode/settings.json
    if [[ -f ".vscode/settings.json" ]]; then
        print_pass "Found workspace settings: .vscode/settings.json"
        
        if grep -q '"ingenious' ".vscode/settings.json"; then
            print_pass "Found ingenious configuration in workspace settings"
        fi
    fi

    return 0
}

# ==============================================================================
# Summary and Recommendations
# ==============================================================================

print_summary() {
    print_header "SUMMARY"
    
    local total=$((PASSED + FAILED + WARNINGS))
    
    echo "Checks completed: $total"
    echo -e "${GREEN}Passed: $PASSED${NC}"
    echo -e "${RED}Failed: $FAILED${NC}"
    echo -e "${YELLOW}Warnings: $WARNINGS${NC}"
    echo ""

    if [[ $FAILED -eq 0 ]]; then
        print_pass "All critical checks passed!"
        
        if [[ $WARNINGS -gt 0 ]]; then
            print_warn "Some warnings detected, review above"
        fi
    else
        print_fail "Some critical checks failed. Review the issues above."
        return 1
    fi
}

# ==============================================================================
# Recommendations
# ==============================================================================

print_recommendations() {
    print_header "RECOMMENDATIONS"

    if [[ $FAILED -gt 0 ]]; then
        echo "Top issues to fix:"
        echo "1. Ensure 'ingenious' CLI is installed and on PATH"
        echo "2. Verify Java 17+ is installed and JAVA_HOME is set correctly"
        echo "3. Check that INGenious classpath is valid (no missing directories)"
        echo "4. Enable verbose logging: 'ingenious server mcp --verbose'"
        echo "5. Review .ingenious-mcp-stderr.log for detailed error messages"
        echo ""
    fi

    echo "For further debugging:"
    echo "  • Run: ingenious server mcp --verbose"
    echo "  • Check VS Code output: View > Output > INGenious > MCP"
    echo "  • Enable debug logging in VS Code settings.json:"
    echo "    \"ingenious.debug\": true"
    echo ""
    
    if [[ $WARNINGS -gt 0 ]]; then
        echo "Additional recommendations:"
        echo "  • Review warning messages above"
        echo "  • Check Java version and compatibility"
        echo "  • Verify network connectivity"
        echo ""
    fi
}

# ==============================================================================
# Main Execution
# ==============================================================================

main() {
    echo -e "${BLUE}"
    cat << "EOF"
╔══════════════════════════════════════════════════════════════════════════════╗
║                                                                              ║
║                 INGenious MCP Server Diagnostic Tool v1.0                    ║
║                                                                              ║
║         Diagnoses why the Ingenious MCP server may not be starting          ║
║                   on your machine. This is a no-op script.                   ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝
EOF
    echo -e "${NC}\n"

    # Run all checks
    check_cli_resolution || true
    check_java_setup || true
    check_ingenious_installation || true
    check_mcp_startup || true
    check_classpath || true
    check_network || true
    check_platform || true
    check_vscode_config || true

    # Print summary
    print_summary
    print_recommendations

    # Return appropriate exit code
    if [[ $FAILED -gt 0 ]]; then
        exit 1
    fi
    exit 0
}

main
