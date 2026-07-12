# INGenious MCP Server — User Manual

> AI-driven test automation for INGenious via the
> [Model Context Protocol](https://modelcontextprotocol.io/).
>
> Lets agents like GitHub Copilot, Claude Desktop, Cursor, Continue, and any
> custom JSON-RPC client **create, run, and debug** INGenious tests from
> natural language.

---

## Table of contents

1. [What is the MCP server?](#1-what-is-the-mcp-server)
2. [Prerequisites](#2-prerequisites)
3. [Build and install](#3-build-and-install)
4. [Quick start — manual smoke test](#4-quick-start--manual-smoke-test)
5. [Wiring an AI client](#5-wiring-an-ai-client)
   - [Claude Desktop](#claude-desktop)
   - [VS Code / GitHub Copilot](#vs-code--github-copilot)
   - [Cursor](#cursor)
   - [Continue](#continue)
6. [Tool reference (25)](#6-tool-reference-25)
7. [Prompt reference (7)](#7-prompt-reference-7)
8. [Resource reference (3)](#8-resource-reference-3)
9. [End-to-end workflows](#9-end-to-end-workflows)
10. [Operations & troubleshooting](#10-operations--troubleshooting)
11. [Reference — JSON-RPC envelopes](#11-reference--json-rpc-envelopes)

---

## 1. What is the MCP server?

The INGenious MCP server is a long-running process that exposes the full
`ingenious` CLI surface over the **Model Context Protocol (revision
2024-11-05)**. It speaks **JSON-RPC 2.0 over stdio** with newline-delimited
UTF-8 frames.

When you connect an AI agent, it sees:

| Capability    | Count | Purpose                                                   |
|---------------|-------|-----------------------------------------------------------|
| **tools**     | 25    | Real operations — create scenarios, run tests, read reports |
| **prompts**   | 7     | Pre-built workflows like *create-test* or *explain-failure* |
| **resources** | 3     | Reference docs the agent can read on demand               |

Every tool is backed by a real Datalib / filesystem / subprocess call —
nothing is mocked.

Module layout:

```
Engine/src/main/java/com/ing/engine/mcp/
  MCPServer.java        # JSON-RPC dispatcher (stdio)
  MCPTools.java         # 25 tools
  MCPPrompts.java       # 7 prompts
  MCPResources.java     # 3 resources
  ActionCatalog.java    # @Action reflection (shared with `ingenious action`)
```

CLI entry point: `ingenious server mcp`.

---

## 2. Prerequisites

| Requirement   | Version                 |
|---------------|-------------------------|
| JDK           | 17 or newer             |
| Maven         | 3.8+                    |
| Python 3      | only for the smoke tests in this guide |
| MCP client    | Claude Desktop ≥ 0.7, VS Code with the MCP extension, or any JSON-RPC stdio client |

You also need an INGenious project to act against — the bundled `CLIDemo`
project works out of the box.

---

## 3. Build and install

From the repository root:

```bash
mvn -DskipTests install
```

The launcher is then available at:

```
Dist/release/ingenious            # Linux / macOS / WSL
Dist/release/ingenious.bat        # Windows
Dist/release/ingenious.command    # macOS double-clickable
```

Verify the build:

```bash
./Dist/release/ingenious --help
./Dist/release/ingenious server --help
./Dist/release/ingenious server mcp --help
```

You should see `mcp`, `rest`, and `status` as subcommands of `server`.

---

## 4. Quick start — manual smoke test

The fastest way to confirm everything works is to talk to the server
yourself with a few JSON-RPC frames.

### 4.1 Start the server

```bash
cd Dist/release
./ingenious server mcp --verbose --project CLIDemo
```

Flags:

| Flag                          | Purpose                                                                 |
|-------------------------------|-------------------------------------------------------------------------|
| `-p`, `--project <name|path>` | Default project for tools that take an optional `project` argument.     |
| `-v`, `--verbose`             | Echo every RX / TX frame to **stderr** (never stdout).                  |

> ⚠️ **The server speaks only JSON-RPC on stdout.** Anything else printed to
> stdout — by Datalib, by plugins, by your `System.out.println` — would
> corrupt the stream. The server therefore redirects `System.out` →
> `System.err` at startup and writes JSON-RPC to the original stdout
> descriptor.

### 4.2 Pipe an initialize handshake

In another terminal:

```bash
cd Dist/release
printf '%s\n' \
'{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"smoke","version":"1.0"}}}' \
'{"jsonrpc":"2.0","method":"notifications/initialized"}' \
'{"jsonrpc":"2.0","id":2,"method":"tools/list"}' \
| ./ingenious server mcp
```

You should see one JSON-RPC response per request (the notification has no
response).

### 4.3 Full CRUD round-trip

The canonical smoke test exercises create → append → show → delete:

```bash
cd Dist/release
printf '%s\n' \
'{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"smoke","version":"1.0"}}}' \
'{"jsonrpc":"2.0","method":"notifications/initialized"}' \
'{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"ingenious_testcase_create","arguments":{"project":"CLIDemo","scenario":"MCPSmoke","testcase":"Roundtrip","steps":[{"action":"OpenURL","input":"https://example.com"},{"action":"Click","object":"home.link"}]}}}' \
'{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"ingenious_testcase_add_step","arguments":{"project":"CLIDemo","scenario":"MCPSmoke","testcase":"Roundtrip","action":"VerifyText","object":"home.heading","input":"Welcome"}}}' \
'{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"ingenious_testcase_show","arguments":{"project":"CLIDemo","scenario":"MCPSmoke","testcase":"Roundtrip"}}}' \
'{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"ingenious_testcase_delete","arguments":{"project":"CLIDemo","scenario":"MCPSmoke","testcase":"Roundtrip"}}}' \
| ./ingenious server mcp 2>/dev/null \
| python3 -c "
import json, sys
ok = True
for line in sys.stdin:
    line = line.strip()
    if not line: continue
    try:
        obj = json.loads(line)
    except Exception:
        print(f'!!! NON-JSON: {line[:160]}'); ok = False; continue
    if 'result' in obj:
        sc = obj['result'].get('structuredContent')
        if sc is not None:
            print(f'id={obj.get(\"id\")} -> {json.dumps(sc)[:240]}')
        elif 'protocolVersion' in obj['result']:
            print(f'id={obj.get(\"id\")} init OK')
    elif 'error' in obj:
        print(f'ERROR id={obj.get(\"id\")}: {json.dumps(obj[\"error\"])}'); ok = False
print('CLEAN' if ok else 'POLLUTED')"
```

Expected output:

```
id=1 init OK
id=2 -> {"created": true, "scenario": "MCPSmoke", "testcase": "Roundtrip", "steps": 2}
id=3 -> {"added": true, "totalSteps": 3}
id=4 -> {"project": "CLIDemo", "scenario": "MCPSmoke", "testcase": "Roundtrip", "steps": [...]}
id=5 -> {"deleted": true, "path": ".../MCPSmoke/Roundtrip.yaml"}
CLEAN
```

`CLEAN` confirms no rogue stdout output broke the framing.

Don't forget to clean up the smoke scenario:

```bash
rm -rf Dist/release/Projects/CLIDemo/TestPlan/MCPSmoke
```

---

## 5. Wiring an AI client

All clients launch the server the same way: spawn the launcher with
`server mcp` as arguments and talk to it over stdio.

### Claude Desktop

Edit `~/Library/Application Support/Claude/claude_desktop_config.json`
(macOS) or `%APPDATA%\Claude\claude_desktop_config.json` (Windows):

```json
{
  "mcpServers": {
    "ingenious": {
      "command": "/absolute/path/to/INGenious/Dist/release/ingenious",
      "args": ["server", "mcp", "-p", "CLIDemo"]
    }
  }
}
```

Restart Claude Desktop. The Tools panel should now list 25
`ingenious_*` tools.

### VS Code / GitHub Copilot

If you use a VS Code MCP extension (e.g. the official MCP support or
`continue.dev`), add to your settings:

```json
{
  "mcp.servers": {
    "ingenious": {
      "command": "/absolute/path/to/INGenious/Dist/release/ingenious",
      "args": ["server", "mcp", "-p", "CLIDemo"]
    }
  }
}
```

### Cursor

`~/.cursor/mcp.json`:

```json
{
  "mcpServers": {
    "ingenious": {
      "command": "/absolute/path/to/INGenious/Dist/release/ingenious",
      "args": ["server", "mcp", "-p", "CLIDemo"]
    }
  }
}
```

### Continue

In `~/.continue/config.json` under `mcpServers`:

```json
{
  "mcpServers": [
    {
      "name": "ingenious",
      "command": "/absolute/path/to/INGenious/Dist/release/ingenious",
      "args": ["server", "mcp", "-p", "CLIDemo"]
    }
  ]
}
```

> **Always use an absolute path** to the launcher. Most clients spawn the
> server from an unspecified working directory.

---

## 6. Tool reference (25)

All tools follow the `ingenious_<area>_<verb>` naming convention. Required
arguments are marked **R**, optional ones **O**. Every tool returns both a
human-readable `content[].text` block and a machine-readable
`structuredContent` object.

### 6.1 Project tools

| Tool                    | Args | Returns |
|-------------------------|------|---------|
| `ingenious_project_list` | `basePath` *(O)* — directory to scan (defaults to `./Projects`). | `[{name, path}, …]` |
| `ingenious_project_info` | `project` *(R)* | `{name, path, scenarios, testCases}` |

### 6.2 Scenario tools

| Tool                       | Args | Returns |
|----------------------------|------|---------|
| `ingenious_scenario_list`  | `project` *(O)* | `[{scenario, testCases}, …]` |
| `ingenious_scenario_create`| `project` *(O)*, `scenario` *(R)* | `{created, scenario}` |

### 6.3 Test-case tools

| Tool                          | Args | Returns |
|-------------------------------|------|---------|
| `ingenious_testcase_list`     | `project` *(O)*, `scenario` *(O)* | `[{scenario, testcase, steps}, …]` |
| `ingenious_testcase_show`     | `project` *(O)*, `scenario` *(R)*, `testcase` *(R)* | full `{project, scenario, testcase, steps:[…]}` |
| `ingenious_testcase_create`   | `project` *(O)*, `scenario` *(R)*, `testcase` *(R)*, `steps` *(O)* array of `{action, object, input, condition, description}` | `{created, scenario, testcase, steps}` |
| `ingenious_testcase_add_step` | `project` *(O)*, `scenario` *(R)*, `testcase` *(R)*, `action` *(R)*, plus `object`, `input`, `condition`, `description` *(O)* | `{added, totalSteps}` |
| `ingenious_testcase_delete`   | `project` *(O)*, `scenario` *(R)*, `testcase` *(R)* | `{deleted, path}` — probes `.yaml`/`.yml`/`.csv` |

### 6.4 Test-set tools

| Tool                     | Args | Returns |
|--------------------------|------|---------|
| `ingenious_testset_list` | `project` *(O)*, `release` *(O)* | `[{release, testset, rows}, …]` |
| `ingenious_testset_show` | `project` *(O)*, `release` *(R)*, `testset` *(R)* | `{release, testset, rows:[…]}` |

### 6.5 Action-catalog tools

These let the agent discover what actions exist **before** composing
steps, so it doesn't invent action names.

| Tool                          | Args | Returns |
|-------------------------------|------|---------|
| `ingenious_action_list`       | `category` *(O)*, `limit` *(O)*. Categories: `Browser`, `API`, `Database`, `Mobile`, `Kafka`, `General`. | `[{name, category, objectType, description, inputRequired, conditionSupported}, …]` |
| `ingenious_action_search`     | `query` *(R)* — free text matched against name, description, object type. | filtered list |
| `ingenious_action_info`       | `action` *(R)* — exact action name. | single action object |
| `ingenious_action_categories` | — | `{API: 35, Browser: 154, Database: 11, General: 398, Kafka: 22, Mobile: 103}` (numbers depend on loaded plugins) |

### 6.6 Run tools

These actually execute tests by spawning a child `java … INGeniousCLI run`
process.

| Tool                    | Args | Returns |
|-------------------------|------|---------|
| `ingenious_run`         | `target` *(R)* — `<Project>/<Scenario>/<TestCase>` or `<Project>/<Release>/<TestSet>`. `browser`, `headless`, `parallel`, `tags`, `timeoutSeconds` *(O)*. | `{runId, status, exitCode, durationMs, command, output}` (synchronous, blocks up to `timeoutSeconds`, default 1800). |
| `ingenious_run_async`   | same as `ingenious_run` minus timeout | `{runId, status:"RUNNING", command}` |
| `ingenious_run_status`  | `runId` *(O)*; omit to list all | `{runId, status, exitCode?, durationMs?}` or full list |
| `ingenious_run_logs`    | `runId` *(R)*, `tail` *(O)* — default 200 lines | `{runId, status, lines:[…]}` |
| `ingenious_run_cancel`  | `runId` *(R)* | `{runId, status:"CANCELLED"}` |

**Status values:** `RUNNING`, `PASS`, `FAIL`, `TIMEOUT`, `CANCELLED`,
`INTERRUPTED`.

### 6.7 Report tools

Parse INGenious report files under
`Projects/<P>/Results/{TestDesign|TestExecution}/<target>/Latest/data.js`.

| Tool                       | Args | Returns |
|----------------------------|------|---------|
| `ingenious_report_latest`  | `project` *(O)*, `target` *(R)* — `<Scenario>/<TestCase>` (TestDesign) or `<Release>/<TestSet>` (TestExecution). | `{target, latest:{timestamp, status, executions:[…]}}` |
| `ingenious_report_history` | `project` *(O)*, `target` *(R)*, `limit` *(O)* — default 10 | `{target, history:[{timestamp, status}, …]}` |
| `ingenious_report_failures`| `project` *(O)*, `target` *(R)* | `{target, failures:[{scenarioName, testcaseName, status}, …]}` |

### 6.8 Config tools

Wrap `<Project>/Configuration/*.properties`.

| Tool                  | Args | Returns |
|-----------------------|------|---------|
| `ingenious_config_get`| `project` *(O)*, `key` *(O)*, `file` *(O)* — default `Global Settings.properties` | one value or full dump |
| `ingenious_config_set`| `project` *(O)*, `key` *(R)*, `value` *(R)*, `file` *(O)* | `{set:true, file, key, value}` |

---

## 7. Prompt reference (7)

Prompts are pre-built natural-language workflows that teach the LLM which
tools to call and in what order. Invoked via `prompts/get`.

| Prompt              | Required args                                                 | Optional | Purpose |
|---------------------|---------------------------------------------------------------|----------|---------|
| `create_test_case`  | `scenario`, `testcase`, `description`                         | `project`, `browser` | Discover actions → compose steps → create → verify. |
| `convert_manual_steps` | `scenario`, `testcase`, `steps`                            | `project` | Turn Gherkin / plain-English steps into INGenious steps. |
| `explain_failure`   | `target`                                                      | `project` | Read the latest report and explain what broke. |
| `debug_test`        | `scenario`, `testcase`                                        | `project` | Walk through a test case looking for fragile patterns. |
| `suggest_locator`   | `description`                                                 | `html`   | Recommend a stable Playwright-style locator. |
| `review_test_case`  | `scenario`, `testcase`                                        | `project` | Quality review — waits, locators, assertions. |
| `run_and_summarize` | `target`                                                      | `project`, `browser` | Execute, then summarise pass / fail counts and top failures. |

Example invocation:

```json
{
  "jsonrpc": "2.0",
  "id": 99,
  "method": "prompts/get",
  "params": {
    "name": "create_test_case",
    "arguments": {
      "scenario": "Login",
      "testcase": "ValidUserCanSignIn",
      "description": "Open the demo site, log in with user 'demo@x.com' / 'pw', verify the dashboard greeting."
    }
  }
}
```

The response contains a single `user` message instructing the LLM to call
`ingenious_action_list`, then `ingenious_testcase_create`, then
`ingenious_testcase_show` — fully scripted.

---

## 8. Resource reference (3)

Resources are read-only documents the agent can fetch with
`resources/read`.

| URI                                | Type      | Purpose |
|------------------------------------|-----------|---------|
| `ingenious://catalog/actions`      | JSON      | Full action catalog (same data as `ingenious_action_list`). |
| `ingenious://docs/getting-started` | Markdown  | A short guide the LLM reads when unsure how to start. |
| `ingenious://docs/step-schema`     | Markdown  | Field-by-field description of a test step row. |

A fourth resource — `ingenious://project/<name>/summary` — is registered
**only** when the server was launched with `--project`.

---

## 9. End-to-end workflows

Below are conversation examples you can paste into any wired AI client.

### 9.1 Author a test from English

> **You:** Create a smoke test under `CLIDemo` / scenario `Login` called
> `ValidUserCanSignIn` that opens the demo site, signs in with
> `demo@x.com` / `pw` and verifies a welcome banner. Then run it headless
> and report the result.

Behind the scenes the agent should:

1. `prompts/get name="create_test_case"` → receives a scripted plan.
2. `tools/call ingenious_action_list category="Browser"` → learns the
   verb vocabulary.
3. `tools/call ingenious_testcase_create` with concrete steps.
4. `tools/call ingenious_testcase_show` → reads back and self-reviews.
5. `tools/call ingenious_run target="CLIDemo/Login/ValidUserCanSignIn"
   headless=true`.
6. `tools/call ingenious_report_latest target="Login/ValidUserCanSignIn"`
   → summarises.

### 9.2 Convert a manual test script

> **You:** Here is our manual regression script — convert each step into
> INGenious steps under `Regression/Checkout`.
>
> ```
> 1. Go to https://shop.example.com
> 2. Search for "running shoes"
> 3. Click the first product
> 4. Click Add to Cart
> 5. Verify cart count is 1
> ```

Agent uses the `convert_manual_steps` prompt → calls
`ingenious_testcase_create` once with all five steps.

### 9.3 Triage a failure

> **You:** The last run of `R1/Smoke` failed — what broke?

1. `tools/call ingenious_report_failures target="R1/Smoke"`.
2. For each failure: `tools/call ingenious_testcase_show` to inspect the
   steps.
3. `tools/call ingenious_run_logs runId=<id>` (if a recent async run id
   is known) — or the agent re-runs synchronously with
   `ingenious_run target="…" headless=true`.
4. Agent suggests a fix and offers to `ingenious_testcase_add_step` a
   wait/assertion.

### 9.4 Long-running suite (async)

```jsonc
// kick off
{"method":"tools/call","params":{"name":"ingenious_run_async",
  "arguments":{"target":"CLIDemo/R1/Regression","parallel":4,"headless":true}}}

// later — poll
{"method":"tools/call","params":{"name":"ingenious_run_status",
  "arguments":{"runId":"<uuid>"}}}

// tail output while still running
{"method":"tools/call","params":{"name":"ingenious_run_logs",
  "arguments":{"runId":"<uuid>","tail":100}}}

// emergency stop
{"method":"tools/call","params":{"name":"ingenious_run_cancel",
  "arguments":{"runId":"<uuid>"}}}
```

---

## 10. Operations & troubleshooting

### 10.1 "Nothing happens when I start the server"

That's normal — MCP servers read from stdin and stay silent until a
client speaks. Use `--verbose` to see frames on stderr.

### 10.2 "My client says the server died"

Watch stderr with `--verbose`. The most common cause is a JVM
classpath problem; the second is **stdout pollution** (see below).

### 10.3 Stdout pollution

If you write a plugin that does `System.out.println(...)`, those bytes
would normally break the JSON-RPC frame stream. The server defends
against this by redirecting `System.out` → `System.err` on startup, so
your plugin output safely appears in the verbose log instead.

If you suspect pollution anyway, run the smoke test in
[§4.3](#43-full-crud-round-trip) — it asserts `CLEAN`.

### 10.4 Empty `steps` returned by `ingenious_testcase_show`

This was a Datalib lazy-loading bug. It's fixed: the server calls
`TestCase.loadTestCaseTableModel()` before reading steps. If you see it
again, rebuild from the latest source — `mvn -DskipTests install`.

### 10.5 `ingenious_testcase_delete` reports "file not found"

Datalib defaults to **YAML** for new test cases. The delete tool probes
`.yaml`, `.yml`, then `.csv`, and falls back to a name-prefix scan. If
none match, check the actual file extension in
`<Project>/TestPlan/<Scenario>/`.

### 10.6 Wrong action categories

If `ingenious_action_categories` shows everything under `General`, your
loaded `@Action` methods don't expose an `object()` value the catalog
recognises. Check
[`ActionCatalog.java`](../src/main/java/com/ing/engine/mcp/ActionCatalog.java)
— add the new object type to the `CATEGORY` map (keys are lower-case,
whitespace-stripped).

### 10.7 Test run hangs

`ingenious_run` blocks until completion or `timeoutSeconds` (default
1800). For long suites use `ingenious_run_async` plus
`ingenious_run_status`/`ingenious_run_logs`/`ingenious_run_cancel`.

### 10.8 Logging

Verbose logging goes **only** to stderr. Redirect at the launch site:

```bash
./ingenious server mcp --verbose 2>/tmp/ingenious-mcp.log
```

---

## 11. Reference — JSON-RPC envelopes

All requests follow MCP revision `2024-11-05` over JSON-RPC 2.0.

### Initialize

**Request**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {},
    "clientInfo": { "name": "my-client", "version": "1.0" }
  }
}
```

**Response**

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2024-11-05",
    "capabilities": {
      "tools":     { "listChanged": false },
      "prompts":   { "listChanged": false },
      "resources": { "subscribe": false, "listChanged": false },
      "logging":   {}
    },
    "serverInfo": {
      "name":    "ingenious-mcp-server",
      "version": "2.0.0",
      "title":   "INGenious Test Automation"
    }
  }
}
```

### Tools / call

```json
{
  "jsonrpc": "2.0",
  "id": 42,
  "method": "tools/call",
  "params": {
    "name": "ingenious_run",
    "arguments": {
      "target":   "CLIDemo/APIBasics/GetUsers",
      "headless": true
    }
  }
}
```

Successful response shape:

```json
{
  "jsonrpc": "2.0",
  "id": 42,
  "result": {
    "content": [
      { "type": "text", "text": "<pretty-printed JSON>" }
    ],
    "structuredContent": { "runId": "…", "status": "PASS", "exitCode": 0, "...": "..." }
  }
}
```

### Errors

```json
{
  "jsonrpc": "2.0",
  "id": 42,
  "error": {
    "code": -32602,
    "message": "Scenario not found: Login"
  }
}
```

Error codes used:

| Code     | Meaning                                            |
|----------|----------------------------------------------------|
| `-32700` | Parse error (bad JSON)                             |
| `-32600` | Invalid request (missing `method`)                 |
| `-32601` | Method not found / unknown tool                    |
| `-32602` | Invalid params or referenced entity not found      |
| `-32603` | Internal error during the tool implementation     |

### Methods supported

| Method                       | Notes                                            |
|------------------------------|--------------------------------------------------|
| `initialize`                 | Required first call.                             |
| `notifications/initialized`  | Notification (no `id`, no response).             |
| `tools/list`                 | Lists the 25 tools.                              |
| `tools/call`                 | Executes a tool.                                 |
| `prompts/list`               | Lists the 7 prompts.                             |
| `prompts/get`                | Renders a prompt with arguments.                 |
| `resources/list`             | Lists the 3 (or 4) resources.                    |
| `resources/read`             | Reads a resource by URI.                         |
| `ping`                       | Returns `{}`.                                    |
| `logging/setLevel`           | Accepted; logs go to stderr regardless.          |
| `shutdown`                   | Server stops cleanly.                            |

---

*Server module:
[`Engine/src/main/java/com/ing/engine/mcp/`](../src/main/java/com/ing/engine/mcp/).
CLI entry point:
[`Engine/src/main/java/com/ing/engine/cli/commands/ServerCommand.java`](../src/main/java/com/ing/engine/cli/commands/ServerCommand.java).*
