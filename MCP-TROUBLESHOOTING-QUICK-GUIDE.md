# INGenious MCP Startup - Quick Troubleshooting Guide

## 🔴 MCP Server Not Starting?

Follow this checklist to diagnose and fix the issue:

---

## Quick Diagnostic (1 minute)

```bash
# 1. Check if ingenious CLI exists
which ingenious
# Should output: /path/to/ingenious
# If not found: Install INGenious or add to PATH

# 2. Check Java version
java -version
# Should show: Java 17 or newer
# If Java 8 or 11: Install Java 17+

# 3. Test MCP server (should hang waiting for input - Ctrl+C to exit)
echo '{}' | ingenious server mcp
# If "command not found": CLI not installed
# If Java error: Classpath or Java issue
```

---

## Common Issues and Solutions

### Issue 1: "command not found: ingenious"

**Root Cause:** INGenious CLI is not installed or not on PATH

**Solution:**
```bash
# Find where ingenious is installed
find /usr/local /opt -name "ingenious" -type f 2>/dev/null

# Or install it:
# macOS: brew install ingenious
# Linux: apt-get install ingenious
# Windows: Download from package repository

# Add to PATH (edit ~/.zshrc or ~/.bash_profile):
export PATH="/usr/local/bin/ingenious:$PATH"
```

**In VS Code Settings:**
```json
{
    "ingenious.cliPath": "/usr/local/bin/ingenious"
}
```

---

### Issue 2: "java: command not found"

**Root Cause:** Java is not installed or not on PATH

**Solution:**
```bash
# Install Java 17+:
# macOS:
brew install openjdk@17

# Linux (Ubuntu/Debian):
sudo apt-get install openjdk-17-jdk

# Linux (RHEL/Fedora):
sudo dnf install java-17-openjdk

# Verify:
java -version  # Should show Java 17+
```

**In VS Code Settings (if needed):**
```json
{
    "ingenious.javaHome": "/usr/libexec/java_home -v 17"  // macOS
    // or
    "ingenious.javaHome": "/usr/lib/jvm/java-17-openjdk"  // Linux
}
```

---

### Issue 3: MCP Exits with Code 1 (No Error Message)

**Root Cause:** Error is happening but not being logged (silent crash)

**Solution:**
```bash
# 1. Enable verbose mode in VS Code settings:
{
    "ingenious.debug": true,
    "ingenious.mcp.verbose": true
}

# 2. Check VS Code Output panel:
# View > Output > INGenious > MCP

# 3. Or run from terminal with verbose flag:
ingenious server mcp --verbose

# Look for error messages like:
# - ClassNotFoundException
# - FileNotFoundException
# - UnsatisfiedLinkError
# - NoSuchMethodError
```

---

### Issue 4: "ClassNotFoundException" or "NoClassDefFoundError"

**Root Cause:** Java classpath is broken - JARs not found

**Solution:**
```bash
# Check classpath:
java -XshowSettings:properties -version 2>&1 | grep java.class.path

# Verify all JAR files exist:
# Look for entries like: /path/to/lib/*
# Run: ls /path/to/lib/ | grep .jar

# If lib directory is empty or missing:
# 1. Reinstall INGenious CLI
# 2. Rebuild from source: mvn clean install
```

---

### Issue 5: "Timeout" - Server Takes Too Long to Start

**Root Cause:** Machine is slow or classpath is very large

**Solution:**
```bash
# In VS Code settings, increase timeout:
{
    "ingenious.mcp.initTimeoutMs": 300000  // 5 minutes instead of 2
}

# Or on command line:
INGENIOUS_MCP_TIMEOUT=300 ingenious server mcp
```

---

### Issue 6: "Configured ingenious.cliPath does not exist"

**Root Cause:** Setting points to wrong or deleted file

**Solution:**
```bash
# 1. Check the setting:
# VS Code: File > Preferences > Settings
# Search: "ingenious.cliPath"

# 2. Update to correct path:
which ingenious  # Gets current path
# Copy output to settings

# 3. Or clear the setting to use PATH default
# (delete the cliPath entry entirely)
```

---

### Issue 7: JAVA_HOME Points to Wrong Java Version

**Root Cause:** JAVA_HOME points to Java 8 or 11, but needs Java 17+

**Solution:**
```bash
# Check current JAVA_HOME:
echo $JAVA_HOME
$JAVA_HOME/bin/java -version  # Check version

# Find correct Java 17:
# macOS:
/usr/libexec/java_home -v 17

# Linux:
update-java-alternatives -l

# Update VS Code settings:
{
    "ingenious.javaHome": "/path/to/java-17"
}
```

---

## Advanced Debugging

### 1. Run MCP Server in Terminal

```bash
cd /path/to/project
ingenious server mcp --verbose

# Test with manual JSON-RPC message:
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"0.1.0"}}}' | ingenious server mcp --verbose

# Expected response:
# {"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2024-11-05",...}}
```

### 2. Check MCP Stderr Log

The Copilot SDK may capture stderr to a file:

```bash
# Look for diagnostic log:
find ~/.config/Code -name "*mcp*" -o -name "*ingenious*" 2>/dev/null
find ~/Library -name "*mcp*" -o -name "*ingenious*" 2>/dev/null  # macOS

# Or check runtime directory:
cat "${RUNTIME_ROOT}/.ingenious-mcp-stderr.log"
```

### 3. Check Java Process

```bash
# In another terminal, monitor the process:
ps aux | grep java | grep mcp

# Check system resources:
top  # Look for Java process using high CPU/memory
free -h  # Check available memory
df -h  # Check disk space

# If process uses 100% CPU, it's likely:
# - Loading a large project
# - Building classpath from many JARs
# - Initializing Playwright
```

### 4. Test Classpath Manually

```bash
# Extract classpath from running process:
CLASSPATH=$(java -XshowSettings:properties -version 2>&1 | grep java.class.path | sed 's/.*= //')

# Try to load main class:
java -cp "$CLASSPATH" com.ing.engine.core.Control --version

# Should work without errors
```

---

## VS Code Settings Reference

```json
{
    // Path to ingenious CLI (auto-detected if not set)
    "ingenious.cliPath": "/usr/local/bin/ingenious",
    
    // Path to Java home directory (auto-detected if not set)
    "ingenious.javaHome": "/Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home",
    
    // MCP initialization timeout in milliseconds
    "ingenious.mcp.initTimeoutMs": 120000,
    
    // Enable verbose MCP logging
    "ingenious.mcp.verbose": true,
    
    // Enable debug mode overall
    "ingenious.debug": true
}
```

---

## When to Get Help

If none of the above solutions work:

1. **Collect diagnostic information:**
   ```bash
   bash ingenious-mcp-diagnostic.sh > diagnostic_report.txt 2>&1
   ```

2. **Gather logs:**
   ```bash
   # VS Code Output panel: INGenious > MCP (copy all)
   # Terminal output from: ingenious server mcp --verbose
   # System info: uname -a, java -version, echo $PATH
   ```

3. **Create a minimal reproduction:**
   - Create empty project with just `TestPlan/` and `ObjectRepository/`
   - Try running MCP server in that project

4. **Share with support team:**
   - diagnostic_report.txt
   - MCP error messages
   - VS Code version and extensions
   - OS and Java version

---

## File Locations

### INGenious Files
- **Installed at:** `/usr/local/bin/ingenious` or `C:\Program Files\ingenious\`
- **Config dir:** `~/.ingenious/`
- **Project structure:**
  - `TestPlan/` - Test scenarios
  - `ObjectRepository/` - UI objects
  - `Configuration/` - Settings
  - `Projects/` - Sub-projects

### VS Code Extension
- **Extension data:** `~/.config/Code/extensions/` or `~/Library/Application Support/Code/extensions/`
- **Output panel:** View > Output, select "INGenious" channel
- **Settings:** File > Preferences > Settings, search "ingenious"

### Logs
- **MCP logs:** In VS Code Output > INGenious > MCP
- **Extension logs:** F1 > "Developer: Show Logs Folder"
- **Java stderr:** May be in `.ingenious-mcp-stderr.log` or captured by parent process

---

## Diagnostic Script

Run the comprehensive diagnostic:

```bash
bash ingenious-mcp-diagnostic.sh
# OR
bash ingenious-mcp-diagnostic.sh --verbose  # More details
```

This checks:
- ✓ CLI availability and executability
- ✓ Java installation and version
- ✓ INGenious installation structure
- ✓ MCP server can start and respond
- ✓ Classpath validity
- ✓ Network connectivity
- ✓ Platform-specific issues
- ✓ VS Code configuration

---

## Summary

| Issue | First Check | Quick Fix |
|---|---|---|
| CLI not found | `which ingenious` | Install or add to PATH |
| Java not found | `java -version` | Install Java 17+ |
| Wrong Java version | `java -version` | Switch to Java 17+ |
| Classpath error | `ls lib/` | Reinstall INGenious |
| Silent crash | Enable verbose | Run with `--verbose` |
| Timeout | Check machine specs | Increase `initTimeoutMs` |
| Wrong settings | Check settings.json | Correct cliPath/javaHome |

---

**Need more help?** Check the full documentation:
- [`copilot-mcp-diagnostic.md`](copilot-mcp-diagnostic.md) - Technical deep dive
- [`MCP-ROOT-CAUSE-ANALYSIS.md`](MCP-ROOT-CAUSE-ANALYSIS.md) - Architecture and root causes
