# INGenious MCP — Hands-On Tutorial

> Learn to drive INGenious test automation from an AI agent, end to end, using
> the **75 MCP tools**. Every example below is real: the tool names, arguments,
> and responses match the shipping server
> ([`MCPTools.java`](../src/main/java/com/ing/engine/mcp/MCPTools.java)) and were
> verified against the bundled **CLIDemo** project.
>
> Highlights: full CRUD authoring (create → **edit steps** → validate → run →
> triage), **Object-Repository read & write** (incl. live URL scraping),
> environment-aware data editing/import, report export/compare, `run_dry`,
> `doctor`, and the **Playwright Agent CLI `browser session`** suite.

---

## Table of contents

1. [Prerequisites](#1-prerequisites)
2. [Start the server](#2-start-the-server)
3. [Tool cheat sheet (all 75)](#3-tool-cheat-sheet-all-75)
4. [Tutorial 1 — Explore a project](#4-tutorial-1--explore-a-project)
5. [Tutorial 2 — Author an API test with discovery](#5-tutorial-2--author-an-api-test-with-discovery)
6. [Tutorial 3 — Validate, then fix](#6-tutorial-3--validate-then-fix)
7. [Tutorial 4 — Build a data-driven test](#7-tutorial-4--build-a-data-driven-test)
8. [Tutorial 5 — Assemble & run a test set](#8-tutorial-5--assemble--run-a-test-set)
9. [What's new: the 14 latest tools](#9-whats-new-the-14-latest-tools)
10. [Reporting & triage](#10-reporting--triage)
11. [Object Repository](#11-object-repository)
12. [Live browser authoring & doctor](#12-live-browser-authoring--doctor)
13. [Tips & troubleshooting](#13-tips--troubleshooting)

---

## 1. Prerequisites

| Requirement | Notes |
|-------------|-------|
| JDK 17+ | Same as the engine. |
| Maven 3.8+ | To build. |
| An MCP client | Claude Desktop, VS Code (Copilot/MCP), Cursor, Continue, or any JSON-RPC stdio client. |
| A project | The bundled **CLIDemo** works out of the box. |

Build once from the repo root:

```bash
mvn -DskipTests install
```

> The full wiring reference (Claude/VS Code/Cursor/Continue config JSON) lives in
> the [MCP User Manual](./MCP-USER-MANUAL.md#5-wiring-an-ai-client). This tutorial
> focuses on *what to do* once connected.

---

## 2. Start the server

```bash
cd Dist/release
./ingenious server mcp --project CLIDemo        # add --verbose to see frames on stderr
```

`--project CLIDemo` sets a **default project**, so you can omit the `project`
argument in every tool call below. Without it, pass `"project": "CLIDemo"` (a
name resolved under `./Projects`, or an absolute path).

Every tool returns two things:

- `content[].text` — a human-readable summary, and
- `structuredContent` — a machine-readable JSON object your agent reasons over.

---

## 3. Tool cheat sheet (all 75)

| Area | Tools |
|------|-------|
| **Project** | `project_list`, `project_info`, `project_create` |
| **Scenario** | `scenario_list`, `scenario_info`, `scenario_create`, `scenario_delete` |
| **Test case** | `testcase_list`, `testcase_show`, `testcase_create`, `testcase_add_step`, `testcase_delete`, `testcase_validate`, `testcase_edit_step`, `testcase_insert_step`, `testcase_remove_step`, `testcase_move_step` |
| **Test set** | `testset_list`, `testset_show`, `testset_create`, `testset_add` |
| **Object Repository** | `object_list`, `object_show`, `object_search`, `object_add`, `object_update`, `object_delete`, `object_import_page` |
| **Actions** | `action_list`, `action_search`, `action_info`, `action_categories` |
| **Run** | `run`, `run_async`, `run_status`, `run_logs`, `run_cancel`, `run_dry` |
| **Report** | `report_latest`, `report_history`, `report_failures`, `report_show`, `report_compare`, `report_export` |
| **Config** | `config_get`, `config_set`, `config_show`, `config_drivers` |
| **Data** | `data_sheet_create`, `data_row_add`, `data_column_add`, `data_show`, `data_get`, `data_set`, `data_row_delete`, `data_import` |
| **Generation** | `gen_list`, `gen_testcase`, `gen_from_openapi`, `gen_from_har`, `data_generate` |
| **Environments** | `env_list`, `env_create`, `env_delete` |
| **Import** | `import_curl`, `import_postman`, `import_bruno`, `import_playwright` |
| **Browser (Playwright Agent CLI)** | `browser_session_start`, `browser_session_do`, `browser_session_snapshot`, `browser_session_save`, `browser_session_close`, `browser_inspect` |
| **Diagnostics** | `doctor` |

All are prefixed `ingenious_` on the wire (e.g. `ingenious_scenario_info`).

---

## 4. Tutorial 1 — Explore a project

**Goal:** understand what's in CLIDemo before touching anything.

> **You:** What scenarios and test cases are in CLIDemo, and how big is APIBasics?

The agent chains three read-only tools:

```jsonc
// 1. list scenarios
{"name":"ingenious_scenario_list","arguments":{}}
// → [{"name":"DataDriven","testCases":1},{"name":"APIBasics","testCases":5}]

// 2. drill into one scenario (NEW)
{"name":"ingenious_scenario_info","arguments":{"scenario":"APIBasics"}}
// → { "scenario":"APIBasics", "testCaseCount":5, "totalSteps":…,
//     "testCases":[{"name":"GetUsers","steps":6}, …] }

// 3. read a single test case's steps
{"name":"ingenious_testcase_show","arguments":{"scenario":"APIBasics","testcase":"GetUsers"}}
// → full step-by-step list (action / object / input / condition / description)
```

`scenario_info` is the fastest way to see per-test-case step counts without
opening each one.

---

## 5. Tutorial 2 — Author an API test with discovery

**Goal:** create a test case *without inventing action names*. The golden rule:
**discover, then compose.**

> **You:** Create `APIBasics/HealthCheck` that GETs `https://api.example.com/health`
> and verifies HTTP 200.

```jsonc
// 1. discover the API vocabulary FIRST
{"name":"ingenious_action_list","arguments":{"category":"API"}}
// or search by intent:
{"name":"ingenious_action_search","arguments":{"query":"status code"}}
// → learn the exact action names, e.g. "SendRequest", "VerifyStatusCode"

// 2. inspect one action's contract
{"name":"ingenious_action_info","arguments":{"action":"VerifyStatusCode"}}
// → { inputRequired:true, objectType:"Webservice", conditionSupported:… }

// 3. compose the test case with real actions
{"name":"ingenious_testcase_create","arguments":{
  "scenario":"APIBasics",
  "testcase":"HealthCheck",
  "steps":[
    {"action":"SetRequestURL","input":"https://api.example.com/health"},
    {"action":"SendRequest","input":"GET"},
    {"action":"VerifyStatusCode","input":"200"}
  ]
}}
// → { created:true, format:"YAML", steps:3 }

// 4. read it back to self-review
{"name":"ingenious_testcase_show","arguments":{"scenario":"APIBasics","testcase":"HealthCheck"}}
```

> **Shortcut:** already have a curl command or a Postman/Bruno collection? Skip
> hand-authoring — use `ingenious_import_curl`, `ingenious_import_postman`, or
> `ingenious_import_bruno` to generate the test case(s) directly.

> **Even faster — archetypes & generators:** start from a template instead of a
> blank step list. `ingenious_gen_list` shows archetypes (e.g. `browser-login`,
> `api-get`, `api-json-verify`); `ingenious_gen_testcase` materialises one and
> reports any `unresolvedParams` you still need to fill:
>
> ```jsonc
> {"name":"ingenious_gen_testcase","arguments":{
>   "archetype":"api-get","scenario":"APIBasics","testcase":"HealthCheck",
>   "params":{"url":"https://api.example.com/health","status":"200"}}}
> ```
>
> Bulk-generate from a spec/capture with `ingenious_gen_from_openapi` (one test
> case per operation) or `ingenious_gen_from_har`, and synthesise data rows with
> `ingenious_data_generate` (typed columns: name/email/uuid/int/date/…).

---

## 6. Tutorial 3 — Validate, then fix

**Goal:** catch mistakes before running. `testcase_validate` 🆕 statically lints
your steps.

```jsonc
// validate one test case…
{"name":"ingenious_testcase_validate","arguments":{"scenario":"APIBasics","testcase":"HealthCheck"}}
// …or the whole project (omit scenario/testcase)
{"name":"ingenious_testcase_validate","arguments":{}}
```

Response:

```json
{
  "checked": 5,
  "valid": true,
  "errors": [],
  "warnings": [
    "APIBasics/HealthCheck step 2: action 'SendReqest' is not a known built-in (may be a reusable component)"
  ]
}
```

- **errors** — hard problems (a step with no `action`). `valid` is `false` when any exist.
- **warnings** — an action name that isn't a known built-in (a typo *or* a legitimate reusable-component call).

Typo caught? **Fix the step in place** — no need to recreate the test case:

```jsonc
// change one field of step 2
{"name":"ingenious_testcase_edit_step","arguments":{
  "scenario":"APIBasics","testcase":"HealthCheck","index":2,"action":"SendRequest"}}

// insert a step at position 2 (others shift down)
{"name":"ingenious_testcase_insert_step","arguments":{
  "scenario":"APIBasics","testcase":"HealthCheck","index":2,"action":"SetRequestHeader","input":"Accept: application/json"}}

// reorder / delete
{"name":"ingenious_testcase_move_step","arguments":{"scenario":"APIBasics","testcase":"HealthCheck","from":3,"to":1}}
{"name":"ingenious_testcase_remove_step","arguments":{"scenario":"APIBasics","testcase":"HealthCheck","index":4}}
```

All indices are **1-based**. Re-run `testcase_validate` to confirm the fix.

---

## 7. Tutorial 4 — Build a data-driven test

**Goal:** parameterize a test with a data sheet, then read/write individual
cells. All data tools are **environment-aware**.

```jsonc
// 0. see which environments exist
{"name":"ingenious_env_list","arguments":{}}
// → ["Default","QA"]

// 1. create a sheet (in all environments by default)
{"name":"ingenious_data_sheet_create","arguments":{"sheet":"Users"}}
// → { sheet:"Users", environments:2 }

// 2. add a column
{"name":"ingenious_data_column_add","arguments":{"sheet":"Users","column":"username"}}

// 3. write a cell (NEW) — row is 1-based; column & rows auto-created on demand
{"name":"ingenious_data_set","arguments":{"sheet":"Users","column":"username","row":1,"value":"alice"}}
// → { sheet:"Users", column:"username", row:1, value:"alice", environments:2 }

// 4. read it back (NEW)
{"name":"ingenious_data_get","arguments":{"sheet":"Users","column":"username","row":1}}
// → { value:"alice" }

// 5. view the whole sheet (NEW) — columns + rows, env-aware, capped by `limit`
{"name":"ingenious_data_show","arguments":{"sheet":"Users","limit":20}}
// → { columns:[...,"username"], rows:[{ "username":"alice", ... }], totalRows:1 }
```

Bind rows to a scenario/test case with `ingenious_data_row_add`. In your test
steps, reference a column with the `${username}` token in the `input` field —
INGenious substitutes the row value at run time.

> **Env targeting:** `data_set` accepts `"env":"QA"` to write one environment, or
> the default `"all"` to write every environment. `data_show`/`data_get` read the
> first matching environment unless you pass `env`.

---

## 8. Tutorial 5 — Assemble & run a test set

**Goal:** collect test cases into a runnable suite under `TestLab/`, then execute.

```jsonc
// 1. create an empty test set (creates the release if needed) — NEW
{"name":"ingenious_testset_create","arguments":{"release":"Regression","testset":"Smoke"}}
// → { created:true, release:"Regression", testset:"Smoke" }

// 2. add rows — NEW. Execute defaults to true; browser defaults to Chrome
{"name":"ingenious_testset_add","arguments":{
  "release":"Regression","testset":"Smoke",
  "scenario":"APIBasics","testcase":"GetUsers"}}
// → { added:true, rows:1 }

{"name":"ingenious_testset_add","arguments":{
  "release":"Regression","testset":"Smoke",
  "scenario":"APIBasics","testcase":"GetUser","browser":"Firefox"}}
// → { added:true, rows:2 }

// 3. review the assembled set
{"name":"ingenious_testset_show","arguments":{"release":"Regression","testset":"Smoke"}}

// 4. run it (headless, 2 parallel threads)
{"name":"ingenious_run","arguments":{
  "target":"CLIDemo/Regression/Smoke","headless":true,"parallel":2}}
// → { runId:"run-…", status:"PASS", exitCode:0, durationMs:… }
```

- `testset_create` + `testset_add` write clean `ExecutionStep` rows the IDE and
  engine both understand (no placeholder rows).
- For long suites, use `ingenious_run_async` and poll with `run_status` /
  `run_logs`, or stop with `run_cancel`.
- To run a single test case instead: `"target":"CLIDemo/APIBasics/GetUsers"`.

---

## 9. What's new: the 14 latest tools

A quick reference for everything added in this release, with the arguments that
matter.

| Tool | Required args | Optional | Returns |
|------|---------------|----------|---------|
| `scenario_info` | `scenario` | `project`, `reusable` | test cases + per-test-case step counts |
| `scenario_delete` | `scenario` | `project`, `reusable` | `{deleted, path}` (irreversible) |
| `testcase_validate` | — | `project`, `scenario`, `testcase` | `{checked, valid, errors[], warnings[]}` |
| `testset_create` | `release`, `testset` | `project` | `{created}` (empty set under TestLab/) |
| `testset_add` | `release`, `testset`, `scenario`, `testcase` | `project`, `browser`, `iteration`, `execute` | `{added, rows}` |
| `object_list` | — | `project` | pages + object counts |
| `object_show` | `page` | `project` | objects: name/type/locator/value/description |
| `object_search` | `query` | `project` | matching objects across all pages |
| `data_show` | `sheet` | `project`, `env`, `limit` | columns + rows (env-aware) |
| `data_get` | `sheet`, `column` | `project`, `row`, `env` | `{value}` |
| `data_set` | `sheet`, `column`, `value` | `project`, `row`, `env` | `{environments}` (adds column/rows on demand) |
| `report_show` | `target`, `runId` | `project` | full parsed `data.js` for one run |
| `report_compare` | `target`, `runA`, `runB` | `project` | pass/fail totals for both runs |
| `config_show` | — | `project` | list of `Configuration/*.properties` files |

All are **thin adapters** over the same Datalib/CLI logic the desktop IDE uses,
so anything created here opens correctly in the IDE.

---

## 10. Reporting & triage

After a run, walk from summary → history → deep dive → comparison.

```jsonc
// latest result summary
{"name":"ingenious_report_latest","arguments":{"target":"APIBasics/GetUsers"}}

// just the failures
{"name":"ingenious_report_failures","arguments":{"target":"APIBasics/GetUsers"}}

// list historical runs (timestamped folders)
{"name":"ingenious_report_history","arguments":{"target":"APIBasics/GetUsers","limit":5}}
// → [{ runId:"2026-07-01_…", modified:… }, …]

// deep-dive one run (NEW) — pass a runId from history
{"name":"ingenious_report_show","arguments":{"target":"APIBasics/GetUsers","runId":"2026-07-01_10-22-05"}}

// compare two runs (NEW) — did something regress?
{"name":"ingenious_report_compare","arguments":{
  "target":"APIBasics/GetUsers","runA":"2026-06-30_18-00-00","runB":"2026-07-01_10-22-05"}}
// → { runA:{total,pass,fail}, runB:{total,pass,fail} }
```

Typical triage prompt:

> **You:** Compare the last two runs of `Regression/Smoke` and tell me what
> regressed.

The agent calls `report_history` → picks the two newest `runId`s →
`report_compare` → then `report_show` on the newer run to read the failing
executions.

---

## 11. Object Repository

Browse the locators your tests bind to. Pages are stored as
`ObjectRepository/<page>.csv` (columns: name, type, locator, value, description).

```jsonc
// list pages with object counts
{"name":"ingenious_object_list","arguments":{}}
// → [{ page:"LoginPage", objects:8 }, …]

// show one page's objects
{"name":"ingenious_object_show","arguments":{"page":"LoginPage"}}
// → { objects:[{ name:"user.field", type:"WebElement", locator:"id", value:"username" }, …] }

// find a locator anywhere by name/locator/value
{"name":"ingenious_object_search","arguments":{"query":"login"}}
```

Use `object_search` before composing a browser step so the agent references an
existing locator instead of inventing one.

**Write locators too:**

```jsonc
// add a locator (creates the page if missing)
{"name":"ingenious_object_add","arguments":{
  "page":"LoginPage","name":"login.user","locator":"id","value":"username"}}

// update or delete an existing locator
{"name":"ingenious_object_update","arguments":{"page":"LoginPage","name":"login.user","value":"user_input"}}
{"name":"ingenious_object_delete","arguments":{"page":"LoginPage","name":"login.user"}}

// scaffold an entire page from a live URL (needs @playwright/cli — see §12)
{"name":"ingenious_object_import_page","arguments":{"url":"https://app.example.com/login","page":"LoginPage"}}
```

---

## 12. Live browser authoring & doctor

### 12.1 Environment health — `doctor`
Before browser work, check the toolchain:

```jsonc
{"name":"ingenious_doctor","arguments":{"project":"CLIDemo"}}
// → { jdk:{version}, playwrightCli:{available, invocation, hint},
//     drivers:[{driver,present}], project:{healthy, missingFolders[]} }
```

`config_drivers` gives the same driver/Playwright-CLI view without a project.

### 12.2 Playwright Agent CLI browser sessions
The `browser_session_*` tools drive the
[Playwright Agent CLI](https://playwright.dev/agent-cli/introduction) — a
ref-based, daemon-backed browser. The agent explores a live page one command at
a time; each command is **recorded as an INGenious step**, and `save` writes a
real test case (wrapped in `OpenBrowser`/`CloseBrowser`).

> **Requires** `@playwright/cli` (`npm i -g @playwright/cli`; the server falls
> back to `npx @playwright/cli`). Run `doctor` first to confirm availability.

```jsonc
// 1. start a named session and open a URL (returns the first ref'd snapshot)
{"name":"ingenious_browser_session_start","arguments":{
  "name":"Checkout","url":"https://demo.playwright.dev/todomvc"}}

// 2. act using snapshot refs (e.g. e21); each call records an INGenious step
{"name":"ingenious_browser_session_do","arguments":{"name":"Checkout","command":"fill e5 \"Buy milk\""}}
{"name":"ingenious_browser_session_do","arguments":{"name":"Checkout","command":"press Enter"}}

// 3. re-snapshot any time to see the current ref'd accessibility tree
{"name":"ingenious_browser_session_snapshot","arguments":{"name":"Checkout"}}

// 4. save the recording to a test case
{"name":"ingenious_browser_session_save","arguments":{
  "project":"CLIDemo","name":"Checkout","scenario":"Todo","testcase":"AddItem"}}
// → { created:true, steps:N }  (OpenBrowser + NavigateTo + recorded + CloseBrowser)

// 5. close the session
{"name":"ingenious_browser_session_close","arguments":{"name":"Checkout"}}
```

Each `do` command maps to an INGenious action via `PlaywrightCliTranslator`
(`click`→`Click`, `fill`→`SetText`, `press`→`PressKey`, `open`→`NavigateTo`, …);
the snapshot **ref** (e.g. `e21`) is carried through as the step's object so you
can later bind it to a durable locator.

### 12.3 One-shot locator inspection
```jsonc
{"name":"ingenious_browser_inspect","arguments":{
  "url":"https://app.example.com/login","describe":"the Sign in button"}}
// → opens a throwaway session, returns the ref'd accessibility snapshot
```

---

## 13. Tips & troubleshooting

**Preview & safety (Phase 5):**
- **Dry-run** any of the main write tools with `"dryRun": true`
  (`testcase_create`, `gen_testcase`, `testset_add`, `object_add`, `data_set`) to
  see what *would* happen without persisting.
- **Idempotent creates:** pass `"ifExists": "skip"` (or `"overwrite"`) to
  `testcase_create` / `gen_testcase` instead of getting a duplicate error.
- **Rich errors:** a wrong scenario / test case / page / sheet / archetype name
  comes back with a *"Did you mean: …?"* hint and a structured
  `error.data.suggestions[]` list — feed those straight back into the next call.

| Symptom | Cause / fix |
|---------|-------------|
| *"No project specified…"* | Launch with `--project`, or pass `"project"` in every call. |
| `data_show` says *"Data sheet not found"* | The sheet name must be an env-scoped sheet (`env_list` shows environments). The error suggests the closest sheet names. |
| Test set has a blank first row | Fixed — `testset_add` prunes placeholder rows. Rebuild if you see it on an old server. |
| `report_show` *"Run not found"* | The `runId` must be a folder name from `report_history` (not "Latest"). |
| Tool count isn't 75 | Rebuild: `mvn -DskipTests install`. Confirm with a `tools/list` call. |
| `browser_session_*` fails with "Playwright Agent CLI not found" | Install `@playwright/cli` (`npm i -g @playwright/cli`) or ensure `npx` is on PATH. Run `ingenious_doctor` to check. |
| Non-JSON on stdout breaks the client | The server redirects `System.out`→`System.err`; run with `--verbose 2>log` to inspect. Never `println` to stdout from a plugin. |

**Golden workflow (memorize this):**

```
discover (action_search / object_search / data_show)
   → compose (testcase_create / testset_add / data_set)
   → validate (testcase_validate)
   → run (run / run_async)
   → triage (report_failures / report_compare / report_show)
```

---

*Companion docs: [MCP User Manual](./MCP-USER-MANUAL.md) (protocol & wiring) ·
[MCP Implementation Plan](./MCP-IMPLEMENTATION-PLAN.md) (roadmap & architecture).
Server module: [`Engine/src/main/java/com/ing/engine/mcp/`](../src/main/java/com/ing/engine/mcp/).*
