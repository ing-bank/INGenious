#!/bin/bash
################################################################################
# error_handling_utils.sh
#
# Purpose: Common error handling and validation functions
# Usage: source error_handling_utils.sh
#
# Provides:
# - check_disk_space: Verify sufficient disk space
# - check_git_available: Verify git is installed
# - check_internet: Check internet connectivity
# - validate_path: Verify path exists and is accessible
################################################################################

# Check disk space (requires at least 1GB free)
check_disk_space() {
    local path="${1:-.}"
    local required_mb=1000
    
    available_mb=$(df -m "$path" | awk 'NR==2 {print $4}')
    
    if [ "$available_mb" -lt "$required_mb" ]; then
        echo "❌ Error: Insufficient disk space"
        echo "   Required: ${required_mb}MB, Available: ${available_mb}MB"
        return 1
    fi
    
    echo "✓ Sufficient disk space: ${available_mb}MB available"
    return 0
}

# Verify git is available
check_git_available() {
    if ! command -v git >/dev/null 2>&1; then
        echo "❌ Error: Git is not installed"
        echo "   Please install git to proceed"
        return 1
    fi
    
    echo "✓ Git is available: $(git --version)"
    return 0
}

# Check internet connectivity
check_internet() {
    if ! ping -c 1 github.com >/dev/null 2>&1; then
        echo "⚠️  Warning: No internet connection detected"
        echo "   Cannot download official release"
        return 1
    fi
    
    echo "✓ Internet connection available"
    return 0
}

# Validate path exists and is accessible
validate_path() {
    local path="$1"
    
    if [ -z "$path" ]; then
        echo "❌ Error: Path not provided"
        return 1
    fi
    
    if [ ! -d "$path" ]; then
        echo "❌ Error: Path not found: $path"
        return 1
    fi
    
    if [ ! -r "$path" ]; then
        echo "❌ Error: Path not readable: $path"
        return 1
    fi
    
    echo "✓ Valid path: $path"
    return 0
}

# Run all validation checks
run_all_checks() {
    local path="${1:-.}"
    
    check_disk_space "$path" || return 1
    check_git_available || return 1
    validate_path "$path" || return 1
    
    return 0
}
