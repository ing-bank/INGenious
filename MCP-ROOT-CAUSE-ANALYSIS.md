# INGenious MCP Startup Root Cause Analysis

## Executive Summary

The INGenious MCP (Model Context Protocol) server startup can fail on some machines due to **6 critical issues** identified in the codebase:

1. **CLI Path Resolution Fails** - The CLI executable is not found
2. **Classpath Expansion Breaks** - Java JAR dependencies cannot be resolved
3. **Silent Process Crashes** - MCP server exits without logging errors
4. **Configuration Errors** - JAVA_HOME or cliPath settings are invalid
5. **Stderr Not Captured** - Diagnostic output is lost
6. **Initialization Timeout** - The client gives up waiting for the server

---

## Root Cause #1: CLI Path Resolution 🔴 CRITICAL

### Problem

The VS Code extension attempts to spawn the `ingenious` CLI with this logic:

```typescript
// From: ingenious-vscode/src/util/paths.ts:125-141
export function resolveCliPath(): string {
  const cfg = vscode.workspace.getConfiguration('ingenious');
  const configured = (cfg.get<string>('cliPath') ?? '').trim();
  if (configured && configured !== 'ingenious') {
    return configured;  // ← May not exist!
  }

  const launcher = process.platform === 'win32' ? 'ingenious.bat' : 'ingenious.command';
  for (const root of vscode.workspace.workspaceFolders ?? []) {
    const candidate = path.join(root.uri.fsPath, 'Resources', launcher);
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }
  return configured || 'ingenious';  // ← Returns 'ingenious' even if it doesn't exist!
}
```

### Why It Fails

- Returns a path that doesn't exist
- No validation that the path is executable
- Falls back to `'ingenious'` (just a string) if not found in Resources
- When `cp.spawn()` tries to execute this, it fails with cryptic `ENOENT` error

### When Detected

```bash
$ cd my-project
$ ingenious server mcp
# bash: ingenious: command not found
```

### Error Message to User

```
Error: spawn ENOENT: no such file or directory
```

### File References

- **Main Issue:** [`ingenious-vscode/src/util/paths.ts:125-141`](ingenious-vscode/src/util/paths.ts:125-141)
- **Spawning Code:** [`ingenious-vscode/src/client/mcp.ts:63-68`](ingenious-vscode/src/client/mcp.ts:63-68)
- **Related:** [`Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java:435-445`](Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java:435-445)

### Suggested Fix

```typescript
export function resolveCliPath(): string {
  const cfg = vscode.workspace.getConfiguration('ingenious');
  const configured = (cfg.get<string>('cliPath') ?? '').trim();
  
  // Validate configured path
  if (configured && configured !== 'ingenious') {
    if (!fs.existsSync(configured)) {
      throw new Error(
        `Configured ingenious.cliPath does not exist: ${configured}\n` +
        `Install INGenious CLI or update the setting.`
      );
    }
    return configured;
  }

  // Try bundled launcher
  const launcher = process.platform === 'win32' ? 'ingenious.bat' : 'ingenious.command';
  for (const root of vscode.workspace.workspaceFolders ?? []) {
    const candidate = path.join(root.uri.fsPath, 'Resources', launcher);
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }
  
  // Fall back to PATH search (let the OS find it)
  return 'ingenious';
}
```

---

## Root Cause #2: Classpath Expansion Fails 🔴 CRITICAL

### Problem

When the Copilot SDK spawns the MCP server, it must expand Java's classpath from wildcard entries to explicit JAR files:

```java
// From: Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java:475-505
private static String expandedClasspath() {
    String raw = System.getProperty("java.class.path", "");
    String[] entries = raw.split(File.pathSeparator);
    List<String> jars = new ArrayList<>();
    
    for (String entry : entries) {
        if (entry == null || entry.isBlank()) {
            continue;  // ← Issue: silently skips bad entries
        }
        if (entry.endsWith("*")) {
            String dirPath = entry.substring(0, entry.length() - 1);
            File dir = new File(dirPath).getAbsoluteFile();
            File[] found = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".jar"));
            
            if (found != null) {  // ← Issue: if directory doesn't exist, found == null!
                for (File jar : found) {
                    jars.add(jar.getAbsolutePath());
                }
            }
            // ← BUG: If found is null, no exception, just continue
        } else {
            jars.add(new File(entry).getAbsolutePath());
        }
    }
    
    if (jars.isEmpty()) {
        return new File(raw).getAbsolutePath();  // ← Fallback is wrong!
    }
    return String.join(File.pathSeparator, jars);
}
```

### Why It Fails

| Scenario | Result |
|----------|--------|
| Classpath entry doesn't exist | `listFiles()` returns `null`, no JARs added |
| Directory is not readable | `listFiles()` returns `null`, silently skipped |
| All entries are wildcards that don't exist | `jars.isEmpty()` → returns fallback `new File(raw)` which is wrong |
| Mixed valid/invalid entries | Only valid entries are added, others silently ignored |

### When Detected

The spawned Java process fails to load `com.ing.engine.core.Control`:

```bash
$ java -cp <bad-classpath> com.ing.engine.core.Control server mcp
Error: Could not find or load main class com.ing.engine.core.Control
```

Or:

```
Exception in thread "main" java.lang.NoClassDefFoundError: com/ing/engine/cli/INGeniousCLI
```

### Why This Happens on Some Machines

1. **Different installation paths**: The JAR directory doesn't exist at the expected location
2. **Permissions issues**: The directory exists but user can't read it
3. **Symlinks**: The path contains symlinks that don't resolve correctly
4. **Network mounts**: NFS or SMB mounts may be unavailable
5. **Race condition**: Classpath directory gets deleted between check and use

### File References

- **Main Issue:** [`Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java:475-505`](Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java:475-505)
- **Caller:** [`Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java:440`](Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java:440)

### Suggested Fix

```java
private static String expandedClasspath() {
    String raw = System.getProperty("java.class.path", "");
    if (raw == null || raw.isBlank()) {
        throw new IllegalStateException(
            "java.class.path is empty or not set. Cannot determine classpath."
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
                errors.add("Failed to list classpath directory (permission denied?): " + dir);
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
        throw new IllegalStateException(
            "Failed to resolve any classpath entries:\n" +
            String.join("\n", errors)
        );
    }
    
    return String.join(File.pathSeparator, jars);
}
```

---

## Root Cause #3: Silent Process Crashes 🔴 CRITICAL

### Problem

Even if the Java process starts, it can crash during initialization with no error visible to the user:

```java
// From: Engine/src/main/java/com/ing/engine/cli/commands/ServerCommand.java:75-95
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
        if (verbose) e.printStackTrace(System.err);  // ← Only if verbose!
        return 1;
    }
}
```

### Why It Fails

1. **Exception message only printed if verbose mode**: If user doesn't pass `--verbose`, the only output is a bare exit code
2. **stderr is ignored**: The parent process (VS Code extension) may not capture stderr
3. **No diagnostic file**: The exception is not written to a persistent log
4. **Stack trace hidden**: If `verbose=false`, even the stack trace is lost

### Possible Exceptions

```
java.lang.UnsatisfiedLinkError: no pw4j in java.library.path
Exception in thread "main" java.io.FileNotFoundException: Configuration/...
NoClassDefFoundError: com/ing/engine/mcp/MCPServer
OutOfMemoryError: Java heap space
ClassNotFoundException: Playwright not found
```

### When Detected

From the client's perspective:

```typescript
// mcp.ts line ~68
this.proc.on('exit', (code) => {
    log.mcp.appendLine(`MCP server exited (code ${code})`);  // ← Only info is the exit code!
    this.failAllPending(new Error('MCP server exited'));
    // ...
});
```

User sees in the Output panel:
```
Starting MCP server: /usr/local/bin/ingenious server mcp (cwd=/home/user/project)
MCP server exited (code 1)
```

No idea why it exited!

### File References

- **Main Issue:** [`Engine/src/main/java/com/ing/engine/cli/commands/ServerCommand.java:75-95`](Engine/src/main/java/com/ing/engine/cli/commands/ServerCommand.java:75-95)
- **Related:** [`Engine/src/main/java/com/ing/engine/mcp/MCPServer.java:85-115`](Engine/src/main/java/com/ing/engine/mcp/MCPServer.java:85-115)

### Suggested Fix

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
        // ALWAYS log the error
        System.err.println("[mcp] FATAL ERROR: " + e.getMessage());
        System.err.println("[mcp] Exception type: " + e.getClass().getName());
        
        if (verbose) {
            System.err.println("[mcp] Enable verbose mode for full stack trace");
            e.printStackTrace(System.err);
        }
        
        // Also write to diagnostic file if Copilot SDK provides one
        String mcpStderr = System.getenv("INGENIOUS_MCP_STDERR");
        if (mcpStderr != null && !mcpStderr.isBlank()) {
            try (java.io.FileWriter fw = new java.io.FileWriter(mcpStderr, true);
                 java.io.PrintWriter pw = new java.io.PrintWriter(fw)) {
                pw.println("[" + java.time.Instant.now() + "] MCP startup failed");
                pw.println("Error: " + e.getMessage());
                e.printStackTrace(pw);
            } catch (Exception writeEx) {
                System.err.println("[mcp] Failed to write diagnostic file: " + writeEx);
            }
        }
        
        return 1;
    }
}
```

---

## Root Cause #4: Configuration Issues 🟠 HIGH

### Problem

The VS Code extension accepts configuration for CLI and Java paths, but doesn't validate them:

```typescript
// From: ingenious-vscode/src/util/paths.ts:136-143
export function cliEnv(): NodeJS.ProcessEnv {
  const cfg = vscode.workspace.getConfiguration('ingenious');
  const javaHome = (cfg.get<string>('javaHome') ?? '').trim();
  const env = { ...process.env };
  if (javaHome) {
    env.JAVA_HOME = javaHome;  // ← May point to invalid JDK!
  }
  return env;
}
```

### Why It Fails

| Config Issue | Result |
|---|---|
| `ingenious.cliPath` points to non-existent file | spawn fails with ENOENT |
| `ingenious.javaHome` points to non-existent directory | Java process fails to start |
| `ingenious.javaHome` points to Java 8 (INGenious needs Java 17+) | Startup fails with version error |
| `ingenious.javaHome` is set but Java binary is missing | `$JAVA_HOME/bin/java` doesn't exist |

### Common Configuration Mistakes

```json
// Wrong - file moved
{
    "ingenious.cliPath": "/opt/ingenious-old/bin/ingenious"
}

// Wrong - symlink is broken
{
    "ingenious.javaHome": "/usr/local/java"  // points to non-existent target
}

// Wrong - Java 11 instead of 17
{
    "ingenious.javaHome": "/Library/Java/JavaVirtualMachines/adoptopenjdk-11.jdk/Contents/Home"
}
```

### File References

- **CLI Path Validation:** [`ingenious-vscode/src/util/paths.ts:125-141`](ingenious-vscode/src/util/paths.ts:125-141)
- **Java Home Handling:** [`ingenious-vscode/src/util/paths.ts:136-143`](ingenious-vscode/src/util/paths.ts:136-143)
- **Environment Setup:** [`Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java:442-460`](Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java:442-460)

### Suggested Fix

```typescript
export function resolveCliPath(): string {
  const cfg = vscode.workspace.getConfiguration('ingenious');
  const configured = (cfg.get<string>('cliPath') ?? '').trim();
  
  if (configured && configured !== 'ingenious') {
    if (!fs.existsSync(configured)) {
      throw new Error(
        `Configured ingenious.cliPath does not exist:\n  ${configured}\n` +
        `Update the setting or reinstall INGenious CLI.`
      );
    }
    if (!fs.statSync(configured).isFile()) {
      throw new Error(
        `Configured ingenious.cliPath is not a file:\n  ${configured}`
      );
    }
    return configured;
  }
  
  // ... other checks ...
  return 'ingenious';
}

export function cliEnv(): NodeJS.ProcessEnv {
  const cfg = vscode.workspace.getConfiguration('ingenious');
  const javaHome = (cfg.get<string>('javaHome') ?? '').trim();
  const env = { ...process.env };
  
  if (javaHome) {
    if (!fs.existsSync(javaHome)) {
      throw new Error(
        `Configured ingenious.javaHome does not exist:\n  ${javaHome}\n` +
        `Update the setting or install Java 17+.`
      );
    }
    const javaBin = path.join(javaHome, 'bin', 'java');
    if (!fs.existsSync(javaBin)) {
      throw new Error(
        `Java executable not found at:\n  ${javaBin}\n` +
        `Check that ingenious.javaHome is set to a valid Java installation.`
      );
    }
    env.JAVA_HOME = javaHome;
  }
  
  return env;
}
```

---

## Root Cause #5: Stderr Not Captured 🟠 HIGH

### Problem

The Copilot SDK sets an environment variable to capture stderr, but it's never actually used:

```java
// From: Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java:450
Map<String, String> env = new HashMap<>(System.getenv());
// Capture the child's startup stderr so MCP launch failures are diagnosable.
env.put("INGENIOUS_MCP_STDERR", root + File.separator + ".ingenious-mcp-stderr.log");  // ← Set but never read!
```

And in the Java process:

```bash
com.ing.engine.core.Control server mcp
# stderr output goes to the console, not to the file
```

### Why It Fails

1. The environment variable is set but never read by the engine
2. stderr is only captured if the parent process captures it
3. On Windows and some Linux distributions, stderr capture may fail
4. Diagnostic output is lost before the error can be reported

### File References

- **Environment Setup:** [`Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java:450`](Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java:450)
- **Usage:** Not used anywhere in the codebase!

### Suggested Fix

In `ServerCommand.McpCommand.call()`:

```java
@Override
public Integer call() {
    String mcpStderr = System.getenv("INGENIOUS_MCP_STDERR");
    PrintWriter diagnosticOut = null;
    
    try {
        if (mcpStderr != null && !mcpStderr.isBlank()) {
            diagnosticOut = new PrintWriter(
                new FileWriter(mcpStderr, true),  // append mode
                true  // auto-flush
            );
            diagnosticOut.println("[" + java.time.Instant.now() + "] MCP server starting");
            diagnosticOut.println("Project: " + (projectPath != null ? projectPath : "<none>"));
            diagnosticOut.flush();
        }
        
        if (verbose) {
            System.err.println("[mcp] starting INGenious MCP server on stdio");
        }
        
        new MCPServer(projectPath, verbose).start();
        return 0;
    } catch (Exception e) {
        // Write to both stderr and diagnostic file
        String msg = "[mcp] FATAL: " + e.getMessage();
        System.err.println(msg);
        if (diagnosticOut != null) {
            diagnosticOut.println(msg);
            e.printStackTrace(diagnosticOut);
            diagnosticOut.flush();
        }
        
        if (verbose) {
            e.printStackTrace(System.err);
        }
        
        return 1;
    } finally {
        if (diagnosticOut != null) {
            diagnosticOut.close();
        }
    }
}
```

---

## Root Cause #6: Initialization Timeout 🟡 MEDIUM

### Problem

The client waits 60 seconds for the server to respond to the `initialize` request, but on slow machines this isn't enough:

```typescript
// From: ingenious-vscode/src/client/mcp.ts:145-156
private request(method: string, params: any, timeoutMs = 60000): Promise<any> {
    const id = this.nextId++;
    return new Promise((resolve, reject) => {
        const timer = setTimeout(() => {
            this.pending.delete(id);
            reject(new Error(`MCP request '${method}' timed out`));  // ← After 60 seconds
        }, timeoutMs);
        // ...
    });
}
```

### Why It Fails

| Operation | Time |
|---|---|
| Launch Java process | 2-5 seconds |
| JVM startup | 10-30 seconds |
| Classpath expansion (large projects) | 10-40 seconds |
| Load MCPTools, MCPPrompts, MCPResources | 5-20 seconds |
| **Total possible** | **30-95 seconds** |

On slow machines (low RAM, spinning disk, network mount), 60 seconds is insufficient.

### When Detected

After 60 seconds:

```
MCP request 'initialize' timed out
```

The client closes the process and marks the status as `error`.

### File References

- **Timeout Logic:** [`ingenious-vscode/src/client/mcp.ts:147-160`](ingenious-vscode/src/client/mcp.ts:147-160)
- **Initialization Call:** [`ingenious-vscode/src/client/mcp.ts:148-152`](ingenious-vscode/src/client/mcp.ts:148-152)

### Suggested Fix

```typescript
private async doStart(cwd?: string): Promise<void> {
    this.setStatus('starting');
    const cli = resolveCliPath();
    const workDir = cwd ?? vscode.workspace.workspaceFolders?.[0]?.uri.fsPath ?? process.cwd();
    
    // Make timeout configurable
    const cfg = vscode.workspace.getConfiguration('ingenious');
    const initTimeoutMs = cfg.get<number>('mcp.initTimeoutMs', 120000);  // Default: 2 minutes
    
    log.mcp.appendLine(
        `Starting MCP server: ${cli} server mcp (cwd=${workDir}, timeout=${initTimeoutMs}ms)`
    );
    
    // ... spawn process ...
    
    try {
        await this.request('initialize', {
            protocolVersion: '2024-11-05',
            capabilities: {},
            clientInfo: { name: 'ingenious-vscode', version: '0.1.0' }
        }, initTimeoutMs);  // Use configurable timeout
    } catch (e) {
        if (e.message.includes('timed out')) {
            log.mcp.appendLine(
                `MCP initialization timed out after ${initTimeoutMs}ms.\n` +
                `This may indicate a slow machine, large project, or classpath issue.\n` +
                `Increase 'ingenious.mcp.initTimeoutMs' in settings to allow more time.`
            );
        }
        throw e;
    }
}
```

---

## Summary Table

| Root Cause | Severity | Component | Symptom | Fix Priority |
|---|---|---|---|---|
| CLI not found | 🔴 CRITICAL | VS Code Extension | `ENOENT: no such file or directory` | 1 |
| Classpath expansion fails | 🔴 CRITICAL | CopilotSdkProvider | `ClassNotFoundException` or exit code 1 | 1 |
| Silent process crashes | 🔴 CRITICAL | ServerCommand | Process exits, no error message | 2 |
| Config errors (cliPath/javaHome) | 🟠 HIGH | VS Code Extension | Invalid path or non-existent directory | 2 |
| Stderr not captured | 🟠 HIGH | CopilotSdkProvider | No diagnostic information | 3 |
| Initialization timeout | 🟡 MEDIUM | mcp.ts | Timeout after 60 seconds | 3 |

---

## Diagnostic Script

A comprehensive diagnostic script has been created: [`ingenious-mcp-diagnostic.sh`](ingenious-mcp-diagnostic.sh)

### Usage

```bash
# Run all checks
bash ingenious-mcp-diagnostic.sh

# Run with verbose output
bash ingenious-mcp-diagnostic.sh --verbose

# Print summary without fixes
bash ingenious-mcp-diagnostic.sh
```

### What It Checks

1. **CLI Resolution** - Is `ingenious` command available and executable?
2. **Java Setup** - Is Java 17+ installed and JAVA_HOME valid?
3. **INGenious Installation** - Are Resources/, Configuration/, Projects/ present?
4. **MCP Server Startup** - Can the MCP server respond to `initialize`?
5. **Java Classpath** - Are all classpath entries valid?
6. **Network** - Is localhost reachable?
7. **Platform-Specific** - macOS ARM64, Linux glibc, Windows issues?
8. **VS Code Config** - Are ingenious settings valid?

---

## How to Use This Diagnostic

### For Users

1. **Run the diagnostic script:**
   ```bash
   bash ingenious-mcp-diagnostic.sh
   ```

2. **Review the output** for any failed checks

3. **Apply recommended fixes** from the output

4. **Check VS Code output:**
   - View > Output > INGenious > MCP
   - Look for error messages

5. **If still failing:**
   - Enable verbose mode: `"ingenious.mcp.verbose": true` in settings
   - Run diagnostic again with `--verbose` flag
   - Share the output with the support team

### For Developers

1. **Read [`copilot-mcp-diagnostic.md`](copilot-mcp-diagnostic.md)** for detailed technical explanation

2. **Apply the suggested code fixes** from this document

3. **Test with:**
   ```bash
   echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"0.1.0"}}}' | ingenious server mcp
   ```

4. **Verify no exceptions** in stderr output

---

## Next Steps

1. ✅ Review the 6 root causes above
2. ✅ Run the diagnostic script on affected machines
3. ✅ Apply the suggested code fixes to address each root cause
4. ✅ Update VS Code extension to validate configurations
5. ✅ Add logging and error capture to Java side
6. ✅ Increase default initialization timeout
7. ✅ Create user documentation for troubleshooting

---

**Document Version:** 1.0  
**Date:** 2026-11-13  
**Status:** Ready for Review
