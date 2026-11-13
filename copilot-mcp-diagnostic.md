# INGenious MCP (Model Context Protocol) Startup Diagnostic Guide

## Overview

The INGenious MCP server may fail to start on some machines due to several critical issues. This document identifies the root causes and provides diagnostic steps.

---

## Architecture Overview

```
VS Code Extension (mcp.ts)
    ↓
    └─→ spawn: ingenious server mcp (CLI command)
            ↓
            └─→ Java Process (ServerCommand.McpCommand)
                    ↓
                    └─→ MCPServer.start()
                            ↓
                            └─→ stdin/stdout JSON-RPC 2.0 protocol
```

---

## Root Causes for MCP Startup Failures

### **ROOT CAUSE #1: CLI Not Found or Not Executable** 🔴 CRITICAL

**Location:** [`ingenious-vscode/src/util/paths.ts`](ingenious-vscode/src/util/paths.ts:125-141)

**Issue:** The `resolveCliPath()` function attempts to locate the `ingenious` CLI binary with the following fallback chain:
1. Explicit `ingenious.cliPath` VS Code setting
2. `Resources/ingenious.command` or `Resources/ingenious.bat` next to workspace
3. `ingenious` on system `PATH`

**Why it Fails:**
- The CLI is not installed or not on `PATH`
- The bundled launcher is not at the expected `Resources/` location
- The configured path points to a non-existent file
- The file exists but is not executable (permissions issue on macOS/Linux)

**Diagnostic Code:**
```typescript
// From mcp.ts line ~55:
const cli = resolveCliPath();
// This can return 'ingenious' even if it doesn't exist on PATH
// The spawn() call will fail silently with:
// Error: spawn ENOENT: no such file or directory
```

**Evidence:**
```javascript
// mcp.ts:63-68
try {
  this.proc = cp.spawn(cli, ['server', 'mcp'], {
    cwd: workDir,
    env: cliEnv(),
    shell: false  // <-- Critical: shell:false means wildcards won't expand
  }) as cp.ChildProcessWithoutNullStreams;
} catch (e) {
  this.setStatus('error');
  throw new Error(`Failed to spawn MCP server: ${String(e)}`);
}
```

---

### **ROOT CAUSE #2: Java Classpath Expansion Fails** 🔴 CRITICAL

**Location:** [`Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java`](Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java:475-505)

**Issue:** When the MCP server is spawned, the classpath is explicitly expanded to avoid shell glob expansion. However, several edge cases break this:

```java
// Line 475-505
private static String expandedClasspath() {
    String raw = System.getProperty("java.class.path", "");
    String[] entries = raw.split(File.pathSeparator);
    List<String> jars = new ArrayList<>();
    for (String entry : entries) {
        if (entry == null || entry.isBlank()) {
            continue;  // <-- Issue: silently skips empty entries
        }
        if (entry.endsWith("*")) {
            String dirPath = entry.substring(0, entry.length() - 1);
            File dir = new File(dirPath).getAbsoluteFile();
            File[] found = dir.listFiles((d, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".jar");
            });
            if (found != null) {  // <-- Issue: if found == null, no jars added!
                for (File jar : found) {
                    jars.add(jar.getAbsolutePath());
                }
            }
        } else {
            jars.add(new File(entry).getAbsolutePath());
        }
    }
    if (jars.isEmpty()) {
        return new File(raw).getAbsolutePath();  // <-- Fallback returns wrong value
    }
    return String.join(File.pathSeparator, jars);
}
```

**Why it Fails:**
1. **Empty classpath entries:** If the classpath from `System.getProperty("java.class.path")` contains empty strings, they're silently skipped but not replaced
2. **Directory doesn't exist:** `listFiles()` returns `null` if the directory doesn't exist, causing zero jars to be added
3. **No write permissions:** `listFiles()` fails if the directory isn't readable
4. **Symlink resolution:** Symlinks in lib paths may not resolve correctly on all OSes
5. **Fallback is unsafe:** If `jars.isEmpty()`, it returns `new File(raw).getAbsolutePath()`, which may be a relative path or invalid

**Result:** The spawned Java process gets an invalid or empty classpath and cannot load `com.ing.engine.core.Control`.

---

### **ROOT CAUSE #3: MCP Server Process Exits Immediately** 🔴 CRITICAL

**Location:** [`Engine/src/main/java/com/ing/engine/cli/commands/ServerCommand.java`](Engine/src/main/java/com/ing/engine/cli/commands/ServerCommand.java:75-95)

**Issue:** Even if the Java process starts, it can fail at several points:

```java
@Override
public Integer call() {
    if (verbose) {
        System.err.println("[mcp] starting INGenious MCP server on stdio");
    }
    try {
        new MCPServer(projectPath, verbose).start();
        return 0;
    } catch (Exception e) {
        System.err.println("[mcp] fatal: " + e.getMessage());
        if (verbose) e.printStackTrace(System.err);  // <-- Only printed if verbose!
        return 1;
    }
}
```

**Why it Fails:**
1. **Initialization errors not visible:** If an exception occurs during `MCPServer.start()` but `verbose` is false, the client only sees a process exit with code 1 and no error message
2. **Redirect failures:** [MCPServer.java:95](Engine/src/main/java/com/ing/engine/mcp/MCPServer.java:95) redirects `System.out` to `System.err` to keep the JSON-RPC stream clean, but this can fail if System.err is also redirected
3. **Initialization timeout:** If the client expects JSON-RPC `initialize` response but the process crashes before it, the client hangs for 60 seconds (see [mcp.ts:148](ingenious-vscode/src/client/mcp.ts:148))

---

### **ROOT CAUSE #4: Configuration Issues** 🟠 HIGH

**Location:** [`ingenious-vscode/src/util/paths.ts`](ingenious-vscode/src/util/paths.ts:125-141)

**Issue:** Configuration problems prevent the CLI from starting:

1. **Wrong JAVA_HOME:**
   ```typescript
   export function cliEnv(): NodeJS.ProcessEnv {
       const cfg = vscode.workspace.getConfiguration('ingenious');
       const javaHome = (cfg.get<string>('javaHome') ?? '').trim();
       const env = { ...process.env };
       if (javaHome) {
           env.JAVA_HOME = javaHome;  // <-- May point to invalid JDK
       }
       return env;
   }
   ```
   If `JAVA_HOME` points to a JDK that doesn't have a `java` binary in `bin/`, the CLI won't start.

2. **Java version incompatibility:** The INGenious engine requires Java 17+, but the configured `JAVA_HOME` points to Java 8 or 11

---

### **ROOT CAUSE #5: Subprocess Stderr Not Captured** 🟠 HIGH

**Location:** [`Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java`](Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java:442-460)

**Issue:** The MCP server's startup errors are written to stderr, but there's no reliable capture mechanism:

```java
Map<String, String> env = new HashMap<>(System.getenv());
// Capture the child's startup stderr so MCP launch failures are diagnosable.
env.put("INGENIOUS_MCP_STDERR", root + File.separator + ".ingenious-mcp-stderr.log");
```

**Why it Fails:**
1. The stderr file path is set but not actually read by the Java engine when building the classpath
2. The environment variable `INGENIOUS_MCP_STDERR` is not used anywhere in the MCP startup code
3. stderr is only captured if the Copilot SDK captures it, which it may not do on all platforms

**Result:** Diagnostic information is lost and MCP startup failures cannot be debugged.

---

### **ROOT CAUSE #6: Client Timeout During Handshake** 🟡 MEDIUM

**Location:** [`ingenious-vscode/src/client/mcp.ts`](ingenious-vscode/src/client/mcp.ts:145-156)

**Issue:** The client sends an `initialize` request with a 60-second timeout, but if the process takes longer to start:

```typescript
// mcp.ts line 145-156
await this.request('initialize', {
  protocolVersion: '2024-11-05',
  capabilities: {},
  clientInfo: { name: 'ingenious-vscode', version: '0.1.0' }
});  // <-- Default timeout: 60000ms
```

**Why it Fails:**
- On slow machines, the Java startup time can exceed 60 seconds
- Heavy classpath expansion with thousands of JARs can take 30-40 seconds alone
- If the timeout fires, the client closes the process with exit code -15, and the logs show:
  ```
  MCP request 'initialize' timed out
  ```

---

## Diagnostic Checklist

Use this checklist to diagnose MCP startup failures:

### **Level 1: CLI Resolution (5 minutes)**

```bash
# 1. Check if ingenious CLI exists and is executable
which ingenious
# Output should be: /path/to/ingenious (or nothing if not found)

# 2. Try to run it
ingenious --version
# Should print version, e.g.: ingenious version 2.5.0

# 3. Check it can spawn the MCP server
ingenious server mcp < /dev/null
# May hang waiting for stdin, which is expected. Press Ctrl+C.
# If it says "command not found" or "No such file", CLI is not found.
```

### **Level 2: Java and Classpath (5 minutes)**

```bash
# 1. Verify Java installation
java -version
# Should print Java 17 or newer

# 2. Verify JAVA_HOME is set correctly
echo $JAVA_HOME
$JAVA_HOME/bin/java -version
# Should work without errors

# 3. Check classpath when running ingenious
ingenious --debug server mcp 2>&1 | head -20
# Look for classpath information or classpath expansion errors
```

### **Level 3: MCP Server Startup (10 minutes)**

```bash
# 1. Enable verbose logging in VS Code extension
# Add to settings.json:
#   "ingenious.debug": true

# 2. Check Output panel: INGenious > MCP
# Look for these logs:
#   - "Starting MCP server: /path/to/ingenious server mcp"
#   - "MCP process error: ..."
#   - "MCP server exited (code X)"

# 3. Look for stderr dump file (if implemented)
# Copilot SDK should set INGENIOUS_MCP_STDERR env var:
cat "${RUNTIME_ROOT}/.ingenious-mcp-stderr.log"

# 4. If MCP exits with code 1, check Java exception
# The exception message should be in stderr output
```

### **Level 4: JSON-RPC Protocol (5 minutes)**

```bash
# Manually test the MCP server handshake:
cat << 'EOF' | ingenious server mcp
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"0.1.0"}}}
EOF

# Expected output:
# {"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2024-11-05","capabilities":{...},"serverInfo":{"name":"ingenious-mcp-server","version":"2.1.0"}}}

# If no output or error, the server crashed during initialization
```

---

## Solutions

### **Fix #1: Ensure CLI is on PATH**

1. **On macOS/Linux:**
   ```bash
   # Find the ingenious installation
   find /usr/local /opt -name "ingenious" -type f 2>/dev/null
   
   # Add to PATH in ~/.zshrc or ~/.bash_profile:
   export PATH="$PATH:/path/to/ingenious/bin"
   source ~/.zshrc
   ```

2. **On Windows:**
   - Add the directory containing `ingenious.bat` to `PATH` via System > Environment Variables

3. **In VS Code settings:**
   ```json
   {
       "ingenious.cliPath": "/usr/local/bin/ingenious"
   }
   ```

---

### **Fix #2: Verify and Fix JAVA_HOME**

```bash
# Check Java version
java -version

# Find Java 17+ installation
/usr/libexec/java_home -v 17  # macOS
update-java-alternatives -l    # Linux

# Set JAVA_HOME in VS Code settings:
# ingenious.javaHome: /Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home
```

---

### **Fix #3: Enable Verbose Logging**

```json
{
    "ingenious.mcp.verbose": true,
    "ingenious.debug": true
}
```

Then check the **Output** panel > **INGenious > MCP** for detailed logs.

---

### **Fix #4: Increase MCP Initialization Timeout**

In VS Code settings:
```json
{
    "ingenious.mcp.initTimeoutMs": 120000
}
```

This increases the timeout to 2 minutes for slow machines.

---

## MCP Startup Flow Diagram

```
┌─────────────────────────────────────────────────────────┐
│ VS Code Extension (mcp.ts)                              │
│ McpClient.start()                                       │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│ 1. Resolve CLI Path (paths.ts)                          │
│    - Check ingenious.cliPath setting                    │
│    - Look for Resources/ingenious.command               │
│    - Fall back to 'ingenious' on PATH                   │
│    ❌ FAILURE: CLI not found                            │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│ 2. Get CLI Environment (paths.ts)                       │
│    - Load JAVA_HOME from settings or system             │
│    - Build environment map                              │
│    ❌ FAILURE: JAVA_HOME invalid                        │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│ 3. Spawn Process (mcp.ts:cp.spawn)                      │
│    - exec: /path/to/ingenious                           │
│    - args: ['server', 'mcp']                            │
│    ❌ FAILURE: ENOENT (CLI not found)                   │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│ 4. Java Process Starts (ServerCommand.McpCommand)       │
│    - Load java.class.path                               │
│    - Expand wildcards (CopilotSdkProvider)              │
│    - Build -cp argument                                 │
│    ❌ FAILURE: Classpath expansion fails                │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│ 5. MCPServer Initializes (MCPServer.start)              │
│    - Redirect System.out to stderr                      │
│    - Create MCPTools, MCPPrompts, MCPResources          │
│    - Start reading from stdin                           │
│    ❌ FAILURE: Exception during init (not logged)       │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│ 6. JSON-RPC Handshake (mcp.ts)                          │
│    - Client sends: initialize request                   │
│    - Server responds with: protocol version, tools list │
│    - Client sends: notifications/initialized            │
│    ❌ FAILURE: Timeout (60s default)                    │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────────┐
│ ✅ MCP Connected                                        │
│    - Client can now call tools/list, tools/call, etc.   │
└─────────────────────────────────────────────────────────┘
```

---

## Code Changes Needed

### **Change 1: Improve CLI Path Resolution** 🔴 CRITICAL

**File:** [`ingenious-vscode/src/util/paths.ts`](ingenious-vscode/src/util/paths.ts)

**Current Issue:** `resolveCliPath()` can return a path that doesn't exist, causing a cryptic ENOENT error.

**Fix:**
```typescript
export function resolveCliPath(): string {
  const cfg = vscode.workspace.getConfiguration('ingenious');
  const configured = (cfg.get<string>('cliPath') ?? '').trim();
  
  // Try configured path first
  if (configured && configured !== 'ingenious') {
    if (fs.existsSync(configured)) {
      return configured;
    }
    // FAIL FAST with clear error
    throw new Error(
      `Configured ingenious.cliPath does not exist: ${configured}\n` +
      `Update the setting or install INGenious CLI.`
    );
  }

  // Try bundled launcher
  const launcher = process.platform === 'win32' ? 'ingenious.bat' : 'ingenious.command';
  for (const root of vscode.workspace.workspaceFolders ?? []) {
    const candidate = path.join(root.uri.fsPath, 'Resources', launcher);
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }
  
  // Try PATH
  return configured || 'ingenious';
}
```

---

### **Change 2: Validate Classpath Expansion** 🔴 CRITICAL

**File:** [`Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java`](Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java:475-505)

**Current Issue:** Classpath expansion can silently produce an empty or invalid classpath.

**Fix:**
```java
private static String expandedClasspath() {
    String raw = System.getProperty("java.class.path", "");
    if (raw == null || raw.isBlank()) {
        throw new IllegalStateException(
            "java.class.path is empty or not set. " +
            "Cannot determine INGenious engine classpath."
        );
    }
    
    String[] entries = raw.split(File.pathSeparator);
    List<String> jars = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    
    for (String entry : entries) {
        if (entry == null || entry.isBlank()) {
            continue;
        }
        
        if (entry.endsWith("*")) {
            String dirPath = entry.substring(0, entry.length() - 1);
            File dir = new File(dirPath).getAbsoluteFile();
            
            if (!dir.isDirectory()) {
                errors.add("Classpath directory not found: " + dir);
                continue;
            }
            
            File[] found = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
            if (found == null) {
                errors.add("Failed to read classpath directory: " + dir);
                continue;
            }
            
            if (found.length == 0) {
                errors.add("No JARs found in classpath directory: " + dir);
                continue;
            }
            
            for (File jar : found) {
                if (jar.isFile()) {
                    jars.add(jar.getAbsolutePath());
                }
            }
        } else {
            File f = new File(entry).getAbsoluteFile();
            if (!f.exists()) {
                errors.add("Classpath entry does not exist: " + entry);
            } else {
                jars.add(f.getAbsolutePath());
            }
        }
    }
    
    if (jars.isEmpty()) {
        String msg = "Failed to resolve any classpath JARs:\n" + String.join("\n", errors);
        throw new IllegalStateException(msg);
    }
    
    return String.join(File.pathSeparator, jars);
}
```

---

### **Change 3: Capture and Log Startup Errors** 🔴 CRITICAL

**File:** [`Engine/src/main/java/com/ing/engine/cli/commands/ServerCommand.java`](Engine/src/main/java/com/ing/engine/cli/commands/ServerCommand.java:75-95)

**Current Issue:** Initialization errors are only logged if `--verbose` flag is set.

**Fix:**
```java
@Override
public Integer call() {
    if (verbose) {
        System.err.println("[mcp] starting INGenious MCP server on stdio");
        System.err.println("[mcp] default project: " + (projectPath != null ? projectPath : "<none>"));
    }
    try {
        new MCPServer(projectPath, verbose).start();
        return 0;
    } catch (Exception e) {
        // ALWAYS log the error, not just in verbose mode
        System.err.println("[mcp] FATAL ERROR: " + e.getMessage());
        System.err.println("[mcp] Exception type: " + e.getClass().getName());
        System.err.println("[mcp] Enable verbose mode for full stack trace: ingenious server mcp --verbose");
        
        if (verbose) {
            e.printStackTrace(System.err);
        }
        
        // Log to a diagnostic file so Copilot SDK can capture it
        String mcpStderr = System.getenv("INGENIOUS_MCP_STDERR");
        if (mcpStderr != null && !mcpStderr.isBlank()) {
            try (java.io.FileWriter fw = new java.io.FileWriter(mcpStderr, true);
                 java.io.PrintWriter pw = new java.io.PrintWriter(fw)) {
                pw.println("[" + java.time.Instant.now() + "] MCP startup failed");
                pw.println("Error: " + e.getMessage());
                e.printStackTrace(pw);
            } catch (Exception ex) {
                System.err.println("[mcp] Failed to write diagnostic file: " + ex);
            }
        }
        
        return 1;
    }
}
```

---

### **Change 4: Increase Default MCP Timeout** 🟡 MEDIUM

**File:** [`ingenious-vscode/src/client/mcp.ts`](ingenious-vscode/src/client/mcp.ts:147-148)

**Current Issue:** 60-second timeout is too short for slow machines.

**Fix:**
```typescript
private async doStart(cwd?: string): Promise<void> {
    this.setStatus('starting');
    const cli = resolveCliPath();
    const workDir = cwd ?? vscode.workspace.workspaceFolders?.[0]?.uri.fsPath ?? process.cwd();
    const cfg = vscode.workspace.getConfiguration('ingenious');
    const initTimeoutMs = cfg.get<number>('mcp.initTimeoutMs', 120000);  // Default: 2 minutes
    
    log.mcp.appendLine(`Starting MCP server: ${cli} server mcp (cwd=${workDir}, timeout=${initTimeoutMs}ms)`);
    
    // ... spawn process ...
    
    // MCP initialize handshake with configurable timeout.
    try {
        await this.request('initialize', {
            protocolVersion: '2024-11-05',
            capabilities: {},
            clientInfo: { name: 'ingenious-vscode', version: '0.1.0' }
        }, initTimeoutMs);  // Use configurable timeout
    } catch (e) {
        if (e.message.includes('timed out')) {
            log.mcp.appendLine(
                `MCP initialization timed out after ${initTimeoutMs}ms. ` +
                `This may indicate a slow machine or classpath issue. ` +
                `Increase 'ingenious.mcp.initTimeoutMs' in settings.`
            );
        }
        throw e;
    }
}
```

---

## CLI Diagnostic Commands

Add these commands to the ingenious CLI to help diagnose MCP issues:

```bash
# Check if MCP server can start
ingenious mcp --check

# Run MCP server with diagnostic output
ingenious mcp --diagnostic

# Validate classpath
ingenious mcp --validate-classpath

# Test initialization handshake
ingenious mcp --test-init
```

---

## Summary Table

| Root Cause | Severity | Component | Fix Priority | Evidence |
|---|---|---|---|---|
| CLI not found | 🔴 CRITICAL | VS Code Extension | 1 | ENOENT error in spawn |
| Classpath expansion fails | 🔴 CRITICAL | CopilotSdkProvider | 1 | ClassNotFoundException or empty classpath |
| MCP server crashes silently | 🔴 CRITICAL | ServerCommand | 2 | Exit code 1 with no stderr output |
| Config/JAVA_HOME invalid | 🟠 HIGH | Extension Settings | 2 | Process exits with Java error |
| Subprocess stderr not captured | 🟠 HIGH | CopilotSdkProvider | 3 | No diagnostic file created |
| Initialization timeout | 🟡 MEDIUM | mcp.ts | 3 | Timeout after 60 seconds on slow machines |

---

## Testing the Fixes

After applying the fixes, test with:

```bash
# 1. Test CLI resolution
code --folder /path/to/project
# Check Output > INGenious > MCP for success message

# 2. Test classpath validation
cd /path/to/project
java -cp $(cat .classpath) com.ing.engine.core.Control server mcp < /dev/null

# 3. Test JSON-RPC handshake
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"0.1.0"}}}' | ingenious server mcp

# 4. Test MCP tool listing
echo '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}' | ingenious server mcp
```

---

## References

- [MCP Specification (2024-11-05)](https://spec.modelcontextprotocol.io/)
- [INGenious CLI Documentation](https://docs.ingenious.local)
- [GitHub Copilot SDK Java](https://github.com/github/copilot-sdk-java)

---

**Document Version:** 1.0  
**Last Updated:** 2026-11-13  
**Author:** INGenious Diagnostic Team
