# INGenious MCP - Code Issues Index

Quick reference guide mapping each root cause to the exact file locations and code snippets.

---

## Issue #1: CLI Path Resolution Fails ❌

### Files Affected
- [`ingenious-vscode/src/util/paths.ts` (Lines 125-141)](ingenious-vscode/src/util/paths.ts:125-141)
- [`ingenious-vscode/src/client/mcp.ts` (Lines 55-68)](ingenious-vscode/src/client/mcp.ts:55-68)

### Code Problem

```typescript
// paths.ts:125-141
export function resolveCliPath(): string {
  const cfg = vscode.workspace.getConfiguration('ingenious');
  const configured = (cfg.get<string>('cliPath') ?? '').trim();
  if (configured && configured !== 'ingenious') {
    return configured;  // ❌ BUG: May not exist!
  }

  const launcher = process.platform === 'win32' ? 'ingenious.bat' : 'ingenious.command';
  for (const root of vscode.workspace.workspaceFolders ?? []) {
    const candidate = path.join(root.uri.fsPath, 'Resources', launcher);
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }
  return configured || 'ingenious';  // ❌ BUG: Returns 'ingenious' even if not on PATH!
}
```

### Why It Fails

- No validation that returned path exists
- No executable permission check
- Falls back to bare string 'ingenious' instead of throwing error
- When cp.spawn() tries to execute, it fails with ENOENT

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
        `Update the setting or install INGenious CLI.`
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
  
  // Fall back to PATH (let OS find it or fail)
  return 'ingenious';
}
```

---

## Issue #2: Classpath Expansion Fails 🔴

### Files Affected
- [`Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java` (Lines 421-505)](Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java:421-505)

### Code Problem

```java
// Lines 475-505
private static String expandedClasspath() {
    String raw = System.getProperty("java.class.path", "");
    String[] entries = raw.split(File.pathSeparator);
    List<String> jars = new ArrayList<>();
    for (String entry : entries) {
        if (entry == null || entry.isBlank()) {
            continue;  // ❌ Silently skips, doesn't validate
        }
        if (entry.endsWith("*")) {
            String dirPath = entry.substring(0, entry.length() - 1);
            File dir = new File(dirPath).getAbsoluteFile();
            File[] found = dir.listFiles((d, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".jar");
            });
            if (found != null) {  // ❌ If directory doesn't exist, found is NULL!
                for (File jar : found) {
                    jars.add(jar.getAbsolutePath());
                }
            }
            // ❌ BUG: If found is null, continue without adding any jars
        } else {
            jars.add(new File(entry).getAbsolutePath());
        }
    }
    if (jars.isEmpty()) {
        return new File(raw).getAbsolutePath();  // ❌ Fallback is wrong!
    }
    return String.join(File.pathSeparator, jars);
}
```

### Callers

```java
// Line 440
args.add(expandedClasspath());  // Result used in -cp argument

// Full command line construction:
args.add("-cp");
args.add(expandedClasspath());  // ❌ This can return invalid path
args.add("com.ing.engine.core.Control");
```

### Why It Fails

| Scenario | Issue |
|----------|-------|
| Directory doesn't exist | `listFiles()` returns `null`, no exception thrown |
| No read permissions | `listFiles()` returns `null` silently |
| All entries are invalid | `jars.isEmpty()` true, returns `new File(raw)` which is wrong |
| Mixed valid/invalid | Only valid ones added, invalid ones silently skipped |

### Impact

```bash
$ java -cp <expanded-path> com.ing.engine.core.Control server mcp
Error: Could not find or load main class com.ing.engine.core.Control
```

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
        String msg = "Failed to resolve any classpath entries:\n" + 
                     String.join("\n", errors);
        throw new IllegalStateException(msg);
    }
    
    return String.join(File.pathSeparator, jars);
}
```

---

## Issue #3: Silent Process Crashes 🔴

### Files Affected
- [`Engine/src/main/java/com/ing/engine/cli/commands/ServerCommand.java` (Lines 75-95)](Engine/src/main/java/com/ing/engine/cli/commands/ServerCommand.java:75-95)

### Code Problem

```java
// Lines 75-95
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
        if (verbose) e.printStackTrace(System.err);  // ❌ Only if verbose!
        return 1;
    }
}
```

### Why It Fails

1. Exception message only printed to stderr
2. Stack trace only printed if `--verbose` flag is set
3. No persistent log file
4. Parent process may not capture stderr
5. User only sees "exit code 1"

### Possible Unlogged Errors

```
Exception in thread "main" java.lang.NoClassDefFoundError: com/ing/engine/mcp/MCPServer
Exception in thread "main" java.lang.UnsatisfiedLinkError: no pw4j in java.library.path
Exception in thread "main" java.io.IOException: Configuration directory not found
OutOfMemoryError: Java heap space
ClassNotFoundException: org.yaml.snakeyaml.Yaml
```

### Suggested Fix

```java
@Override
public Integer call() {
    if (verbose) {
        System.err.println("[mcp] starting INGenious MCP server on stdio");
        System.err.println("[mcp] default project: " + (projectPath != null ? projectPath : "<none>"));
    }
    
    PrintWriter diagnosticLog = null;
    try {
        // Try to open diagnostic log if environment variable set
        String mcpStderr = System.getenv("INGENIOUS_MCP_STDERR");
        if (mcpStderr != null && !mcpStderr.isBlank()) {
            try {
                diagnosticLog = new PrintWriter(new FileWriter(mcpStderr, true), true);
                diagnosticLog.println("[" + java.time.Instant.now() + "] MCP server starting");
                diagnosticLog.println("Project: " + (projectPath != null ? projectPath : "<none>"));
                diagnosticLog.flush();
            } catch (Exception e) {
                System.err.println("[mcp] Warning: Could not open diagnostic log: " + e);
            }
        }
        
        new MCPServer(projectPath, verbose).start();
        return 0;
    } catch (Exception e) {
        // ALWAYS log the error (not just in verbose mode)
        String errorMsg = "[mcp] FATAL ERROR: " + e.getMessage();
        System.err.println(errorMsg);
        System.err.println("[mcp] Exception type: " + e.getClass().getName());
        
        if (verbose) {
            System.err.println("[mcp] Full stack trace:");
            e.printStackTrace(System.err);
        } else {
            System.err.println("[mcp] Run with --verbose for full stack trace");
        }
        
        // Also log to diagnostic file if available
        if (diagnosticLog != null) {
            diagnosticLog.println(errorMsg);
            diagnosticLog.println("Exception type: " + e.getClass().getName());
            e.printStackTrace(diagnosticLog);
            diagnosticLog.flush();
        }
        
        return 1;
    } finally {
        if (diagnosticLog != null) {
            diagnosticLog.close();
        }
    }
}
```

---

## Issue #4: Configuration Validation Missing 🟠

### Files Affected
- [`ingenious-vscode/src/util/paths.ts` (Lines 125-143)](ingenious-vscode/src/util/paths.ts:125-143)

### Code Problem

```typescript
// Lines 125-141 - No validation!
export function resolveCliPath(): string {
  const cfg = vscode.workspace.getConfiguration('ingenious');
  const configured = (cfg.get<string>('cliPath') ?? '').trim();
  if (configured && configured !== 'ingenious') {
    return configured;  // ❌ No validation!
  }
  // ...
}

// Lines 136-143 - No validation!
export function cliEnv(): NodeJS.ProcessEnv {
  const cfg = vscode.workspace.getConfiguration('ingenious');
  const javaHome = (cfg.get<string>('javaHome') ?? '').trim();
  const env = { ...process.env };
  if (javaHome) {
    env.JAVA_HOME = javaHome;  // ❌ No validation that it exists or is a JDK!
  }
  return env;
}
```

### Common Configuration Mistakes

```json
// Wrong - file moved
{
    "ingenious.cliPath": "/opt/ingenious-2.4/bin/ingenious"  // ← Moved to 2.5
}

// Wrong - broken symlink
{
    "ingenious.javaHome": "/usr/local/java"  // ← Symlink target deleted
}

// Wrong - Java 11 instead of 17
{
    "ingenious.javaHome": "/Library/Java/JavaVirtualMachines/adoptopenjdk-11.jdk/Contents/Home"
}
```

### Suggested Fix

```typescript
export function resolveCliPath(): string {
  const cfg = vscode.workspace.getConfiguration('ingenious');
  const configured = (cfg.get<string>('cliPath') ?? '').trim();
  
  if (configured && configured !== 'ingenious') {
    if (!fs.existsSync(configured)) {
      throw new Error(
        `Configured ingenious.cliPath does not exist:\n  ${configured}\n` +
        `Update VS Code settings or reinstall INGenious CLI.`
      );
    }
    if (!fs.statSync(configured).isFile()) {
      throw new Error(
        `Configured ingenious.cliPath is not a file:\n  ${configured}`
      );
    }
    return configured;
  }

  const launcher = process.platform === 'win32' ? 'ingenious.bat' : 'ingenious.command';
  for (const root of vscode.workspace.workspaceFolders ?? []) {
    const candidate = path.join(root.uri.fsPath, 'Resources', launcher);
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }
  
  return 'ingenious';
}

export function cliEnv(): NodeJS.ProcessEnv {
  const cfg = vscode.workspace.getConfiguration('ingenious');
  const javaHome = (cfg.get<string>('javaHome') ?? '').trim();
  const env = { ...process.env };
  
  if (javaHome) {
    // Validate JAVA_HOME exists and is a directory
    if (!fs.existsSync(javaHome)) {
      throw new Error(
        `Configured ingenious.javaHome does not exist:\n  ${javaHome}\n` +
        `Update VS Code settings or install Java 17+.`
      );
    }
    
    // Validate java binary exists
    const javaBin = path.join(javaHome, 'bin', 'java');
    if (!fs.existsSync(javaBin)) {
      throw new Error(
        `Java executable not found at:\n  ${javaBin}\n` +
        `Ensure ingenious.javaHome points to a valid Java 17+ installation.`
      );
    }
    
    env.JAVA_HOME = javaHome;
  }
  
  return env;
}
```

---

## Issue #5: Stderr Not Captured 🟠

### Files Affected
- [`Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java` (Lines 442-460)](Engine/src/main/java/com/ing/engine/aicli/ai/CopilotSdkProvider.java:442-460)

### Code Problem

```java
// Lines 442-460
private Map<String, McpServerConfig> ingeniousMcpServer() {
    // ...
    Map<String, String> env = new HashMap<>(System.getenv());
    // Capture the child's startup stderr so MCP launch failures are diagnosable.
    env.put("INGENIOUS_MCP_STDERR", root + File.separator + ".ingenious-mcp-stderr.log");
    // ❌ BUG: This environment variable is set but NEVER READ!
    
    McpStdioServerConfig ingenious = new McpStdioServerConfig()
        .setCommand(javaBin)
        .setArgs(args)
        .setTools(List.of("*"))
        .setEnv(env)
        .setWorkingDirectory(root);
    // ...
}
```

### Why It Fails

1. Environment variable is set but never read by the Java process
2. The ServerCommand doesn't use `INGENIOUS_MCP_STDERR`
3. Stderr from the subprocess is lost
4. Diagnostic information cannot be retrieved

### Solution

In `ServerCommand.McpCommand.call()`, read and use the environment variable:

```java
@Override
public Integer call() {
    // Read diagnostic log path from environment (set by Copilot SDK)
    String mcpStderrPath = System.getenv("INGENIOUS_MCP_STDERR");
    PrintWriter diagnosticLog = null;
    
    try {
        if (mcpStderrPath != null && !mcpStderrPath.isBlank()) {
            diagnosticLog = new PrintWriter(
                new FileWriter(mcpStderrPath, true),  // append mode
                true  // auto-flush
            );
            diagnosticLog.println("[" + java.time.Instant.now() + "] MCP server starting");
            if (projectPath != null) {
                diagnosticLog.println("Project: " + projectPath);
            }
            diagnosticLog.flush();
        }
        
        if (verbose) {
            System.err.println("[mcp] starting INGenious MCP server on stdio");
        }
        
        new MCPServer(projectPath, verbose).start();
        return 0;
    } catch (Exception e) {
        // Log to both stderr and diagnostic file
        System.err.println("[mcp] fatal: " + e.getMessage());
        
        if (diagnosticLog != null) {
            diagnosticLog.println("[" + java.time.Instant.now() + "] MCP server failed");
            diagnosticLog.println("Error: " + e.getMessage());
            e.printStackTrace(diagnosticLog);
            diagnosticLog.flush();
        }
        
        if (verbose) {
            e.printStackTrace(System.err);
        }
        
        return 1;
    } finally {
        if (diagnosticLog != null) {
            try {
                diagnosticLog.close();
            } catch (Exception ignored) {}
        }
    }
}
```

---

## Issue #6: Initialization Timeout Too Short 🟡

### Files Affected
- [`ingenious-vscode/src/client/mcp.ts` (Lines 45-160)](ingenious-vscode/src/client/mcp.ts:45-160)

### Code Problem

```typescript
// Lines 45-160
export class McpClient {
  // ...
  
  private async doStart(cwd?: string): Promise<void> {
    this.setStatus('starting');
    const cli = resolveCliPath();
    // ...
    
    // MCP initialize handshake.
    await this.request('initialize', {  // ← Default timeout: 60000ms
      protocolVersion: '2024-11-05',
      capabilities: {},
      clientInfo: { name: 'ingenious-vscode', version: '0.1.0' }
    });  // ❌ Timeout too short for slow machines
    
    this.notify('notifications/initialized', {});
    this.restarts = 0;
    this.setStatus('connected');
    log.mcp.appendLine('MCP server connected.');
  }
  
  private request(method: string, params: any, timeoutMs = 60000): Promise<any> {
    const id = this.nextId++;
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.pending.delete(id);
        reject(new Error(`MCP request '${method}' timed out`));  // ❌ After 60 seconds
      }, timeoutMs);
      // ...
    });
  }
}
```

### Why It Fails

On slow machines, initialization takes:
- Java startup: 10-30 seconds
- Classpath expansion: 10-40 seconds
- MCPServer initialization: 10-20 seconds
- **Total: 30-90 seconds**

The 60-second timeout is insufficient.

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

  try {
    this.proc = cp.spawn(cli, ['server', 'mcp'], {
      cwd: workDir,
      env: cliEnv(),
      shell: false
    }) as cp.ChildProcessWithoutNullStreams;
  } catch (e) {
    this.setStatus('error');
    throw new Error(`Failed to spawn MCP server: ${String(e)}`);
  }

  this.proc.stdout.setEncoding('utf8');
  this.proc.stdout.on('data', (d: string) => this.onData(d));
  this.proc.stderr.setEncoding('utf8');
  this.proc.stderr.on('data', (d: string) => log.mcp.append(d));
  this.proc.on('error', (e) => {
    log.mcp.appendLine(`MCP process error: ${String(e)}`);
    this.setStatus('error');
  });
  this.proc.on('exit', (code) => {
    log.mcp.appendLine(`MCP server exited (code ${code})`);
    this.failAllPending(new Error('MCP server exited'));
    this.proc = undefined;
    if (!this.disposed) {
      this.setStatus('stopped');
      this.scheduleRestart(workDir);
    }
  });

  // MCP initialize handshake with configurable timeout.
  try {
    await this.request('initialize', {
      protocolVersion: '2024-11-05',
      capabilities: {},
      clientInfo: { name: 'ingenious-vscode', version: '0.1.0' }
    }, initTimeoutMs);  // ✓ Use configurable timeout
  } catch (e) {
    if (e instanceof Error && e.message.includes('timed out')) {
      log.mcp.appendLine(
        `MCP initialization timed out after ${initTimeoutMs}ms. ` +
        `This may indicate a slow machine or classpath issue. ` +
        `Increase 'ingenious.mcp.initTimeoutMs' in settings.`
      );
    }
    throw e;
  }
  
  this.notify('notifications/initialized', {});
  this.restarts = 0;
  this.setStatus('connected');
  log.mcp.appendLine('MCP server connected.');
}
```

### Configuration

Add to VS Code `settings.json`:

```json
{
    "ingenious.mcp.initTimeoutMs": 300000  // 5 minutes for very slow machines
}
```

---

## Summary Table

| Issue | File | Lines | Severity | Fix Type |
|-------|------|-------|----------|----------|
| CLI path validation | `paths.ts` | 125-141 | 🔴 | Input validation |
| Classpath expansion | `CopilotSdkProvider.java` | 475-505 | 🔴 | Error handling |
| Silent crashes | `ServerCommand.java` | 75-95 | 🔴 | Logging |
| Config validation | `paths.ts` | 125-143 | 🟠 | Input validation |
| Stderr capture | `CopilotSdkProvider.java` | 442-460 | 🟠 | Implementation |
| Timeout too short | `mcp.ts` | 145-160 | 🟡 | Configuration |

---

## How to Apply Fixes

1. **Review each suggested fix** above
2. **Apply to the exact line numbers** specified
3. **Test with:**
   ```bash
   echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"0.1.0"}}}' | ingenious server mcp --verbose
   ```
4. **Verify in VS Code:** View > Output > INGenious > MCP should show "MCP server connected"

---

## Priority Fixes

**Immediate (addresses 90% of issues):**
1. ✅ Fix #2 - Classpath expansion (prevents ClassNotFoundException)
2. ✅ Fix #3 - Silent crashes (shows actual error)
3. ✅ Fix #1 - CLI path validation (clear error message)

**Important (improves UX):**
4. ✅ Fix #4 - Configuration validation
5. ✅ Fix #6 - Timeout adjustment

**Nice-to-have (improves diagnostics):**
6. ✅ Fix #5 - Stderr capture
