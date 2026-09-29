# INGenious Interactive AI CLI — Implementation Plan

> Build an interactive, conversational terminal experience for INGenious — inspired by
> Claude Code, Gemini CLI, OpenCode and KaneCLI — that becomes the **primary AI interface**
> to INGenious. The CLI orchestrates all INGenious capabilities directly through a shared
> Tool Registry; MCP is retained as a thin compatibility adapter over the same registry.

---

## 1. Vision

```
$ ingenious
╭──────────────────────────────────────────────╮
│               INGenious CLI                  │
│         AI-assisted test automation          │
╰──────────────────────────────────────────────╯
Project: MortgageTests
Framework: Playwright Java
> create a login test
Thinking...
✓ Analysing application
✓ Discovering pages
✓ Generating page objects
✓ Building test
✓ Validating selectors
Done.
Created
  Scenarios/Login/LoginTest.yaml
  ObjectRepository/LoginPage.csv
  TestData/login.csv
```

An engineer installs INGenious, opens a terminal, types natural-language requests, and
creates / migrates / executes / validates / explains automated tests — without touching
MCP directly. MCP stays alive as *another consumer* of the same tools (VS Code, GitHub
Copilot agent mode, external agents).

---

## 2. Current State (what we build on)

The Engine module already contains almost everything the CLI needs — the work is
**refactoring into a registry + adding the interactive AI loop**, not reimplementing tools.

| Asset | Location | Status |
|---|---|---|
| 75 MCP tools (thin adapters over Engine/Datalib) | `Engine/src/main/java/com/ing/engine/mcp/MCPTools.java` | ✅ Working, verified vs CLIDemo |
| JSON-RPC dispatcher | `mcp/MCPServer.java` | ✅ Working (13 prompts, 5 resources) |
| Picocli CLI (16 top-level commands) | `cli/INGeniousCLI.java`, `cli/commands/*` | ✅ Working |
| Basic interactive shell | `cli/commands/ShellCommand.java` | ✅ Exists (non-AI REPL) |
| Shared catalog pattern | `mcp/ActionCatalog.java` (used by both CLI + MCP) | ✅ The pattern to generalize |
| Playwright-CLI bridge | `mcp/PlaywrightCliTranslator.java`, browser_session_* tools | ✅ Exists (daemon-backed sessions) |
| Archetype generation | `mcp/ArchetypeCatalog.java` (7 archetypes) | ✅ Exists |
| Rich errors + suggestions | `MCPException` with `data.suggestions[]`, "Did you mean?" | ✅ Exists |
| Dry-run + idempotency | `dryRun` arg on main write tools, `ifExists` on create | ✅ Partial (extend to all writes) |
| Terminal libs | picocli 4.7.5, JLine 3.25.0, progressbar 0.10.0, asciitable 0.3.2 | ✅ Already on Engine classpath |
| Jackson (+ YAML) | Engine classpath | ✅ Present |

Key existing constraint: the project targets **Java 17**, Maven multi-module reactor
(`StoryWriter, Datalib, TestData - Csv, Engine, IDE, Common, Dist`; `ingenious-api` is
version-pinned outside the reactor).

---

## 3. Target Architecture

```
                        User (terminal)
                              │
                     Interactive CLI (REPL)          ← JLine 3 line reader, key bindings
                              │
                    Conversation Manager             ← history, context, memory, approvals
                              │
              ┌───────────────┴───────────────┐
              │                               │
           Planner                     Deterministic Workflows
   (AI-generated tool plans)         (predefined plans, no AI needed)
              │                               │
              └───────────────┬───────────────┘
                              │
                      Execution Engine               ← sequential/parallel steps, undo journal
                              │
                     Tool Registry API               ← single source of truth for capabilities
              ┌───────────────┴───────────────┐
              │                               │
      Native Java Tools                MCP Adapter (compat layer)
   (Engine/Datalib logic)          exposes same registry via JSON-RPC
              │                    to VS Code / Copilot / agents
     Plugin Tools (ServiceLoader)
```

Design rules:

1. **One implementation per capability.** Tools live in the registry; both the
   interactive CLI and the MCP server are *thin front-ends* over it.
2. **`MCPTools.java` is refactored, not duplicated.** Its 75 handlers migrate into
   registry `Tool` classes; `MCPTools` shrinks to a dispatch shim.
3. **Planner and Execution Engine are separate.** Plans are data (JSON), reviewable,
   replayable, and diffable — this enables `/plan`, approvals, undo, and determinism.
4. **AI is optional per request.** Deterministic workflows and slash commands run with
   zero LLM calls; the Planner is engaged only for natural-language input.

### 3.1 New package layout (Engine module)

```
com.ing.engine.aicli
├── AiCliMain.java                 // entry: `ingenious` with no args → interactive mode
├── repl/
│   ├── Repl.java                  // JLine loop, prompt, key bindings, interrupts
│   ├── SlashCommands.java         // /help /tools /plan /undo ... dispatch
│   ├── Completer.java             // slash commands, tool names, project entities
│   └── InputClassifier.java       // slash vs deterministic vs natural language
├── conversation/
│   ├── ConversationManager.java   // turns, transcript, token budgeting
│   ├SessionContext.java           // project/framework/language/package/recent files
│   ├── MemoryStore.java           // persisted context (~/.ingenious/ + project-local)
│   └── ApprovalQueue.java         // pending approvals, diff previews
├── planning/
│   ├── Planner.java               // NL → Plan (via AI provider) with tool schemas
│   ├── Plan.java / PlanStep.java  // serializable plan model (JSON)
│   ├── PlanValidator.java         // tools exist, args valid, deps acyclic
│   └── workflows/                 // deterministic predefined plans
│       ├── WorkflowCatalog.java
│       ├── CreateBrowserTestWorkflow.java
│       ├── CreateApiTestWorkflow.java
│       ├── FixFailingTestsWorkflow.java
│       └── MigrateSeleniumWorkflow.java
├── execution/
│   ├── ExecutionEngine.java       // runs Plan: seq + parallel steps, retries
│   ├── ExecutionContext.java      // step outputs → later step inputs (${step1.out.x})
│   ├── UndoJournal.java           // file snapshots per plan, /undo /redo
│   └── EventBus.java              // progress events → UI renderers
├── tools/
│   ├── Tool.java                  // interface: id, description, JSON schema, execute()
│   ├── ToolRegistry.java          // discovery, lookup, categories, ServiceLoader
│   ├── ToolResult.java            // structured result + file mutations manifest
│   ├── annotations/@ToolSpec      // declarative registration metadata
│   └── impl/                      // migrated from MCPTools (by category, §5)
│       ├── discovery/  authoring/  data/  generation/
│       ├── execution/  reporting/  browser/
├── ai/
│   ├── AiProvider.java            // interface: chat, toolCall, stream
│   ├── CopilotProvider.java       // GitHub Copilot (device-flow login)
│   ├── OpenAiCompatProvider.java  // any OpenAI-compatible endpoint (incl. local)
│   ├── ProviderConfig.java        // ~/.ingenious/ai.json, `/model` switching
│   └── TokenStore.java            // OS-keychain-or-file credential storage
└── ui/
    ├── Theme.java                 // colors, unicode/ascii fallback, NO_COLOR support
    ├── Panels.java                // boxed banners, tables (asciitable)
    ├── ProgressRenderer.java      // spinners, checkmark streams (progressbar/JLine)
    ├── DiffRenderer.java          // +/- colored diffs for approval mode
    ├── MarkdownRenderer.java      // terminal markdown (headings, code, lists)
    └── Selector.java              // interactive ○/● list selection (JLine widgets)
```

MCP compat layer: `com.ing.engine.mcp.MCPServer` remains at its current location; its
tool dispatch is rewired to `ToolRegistry` (§6).

---

## 4. Core Components

### 4.1 Tool interface & registry

```java
public interface Tool {
    String id();                        // e.g. "testcase_create"
    String category();                  // discovery | authoring | data | generation | execution | reporting | browser
    String description();               // used in AI tool schemas AND /tools output
    JsonNode inputSchema();             // JSON Schema (same shape MCP already uses)
    boolean mutatesFiles();             // drives approval mode + undo journaling
    boolean supportsDryRun();
    ToolResult execute(JsonNode args, ToolExecutionContext ctx) throws ToolException;
}
```

* `ToolExecutionContext` carries: resolved project, `EventBus` for progress emission,
  cancellation token, dry-run flag.
* `ToolResult` carries: structured JSON payload, human summary line, and a
  **mutation manifest** (`created[] / modified[] / deleted[]` file paths) — this feeds
  the "Created: tests/LoginTest.java …" output, approvals, and the undo journal.
* `ToolRegistry` builds the registry from: (a) built-in tools, (b) `ServiceLoader`
  plugin discovery (§9). It can render the complete tool list as JSON Schema for the
  AI provider's function-calling API and as a table for `/tools`.
* Reuse the existing MCP JSON schemas verbatim — they are already written for all 75
  tools in `MCPTools.toolDefinitions()`.

### 4.2 Conversation Manager & Session Memory

Remembered across turns (and persisted between sessions):

| Item | Persistence |
|---|---|
| Current project, framework, language, package | `<project>/.ingenious/session.json` |
| Recent generated files (last N mutation manifests) | project-local |
| Current feature/scenario being worked on | project-local |
| Pending approvals | in-memory (cleared on exit, warn if pending) |
| Conversation transcript | project-local `history.jsonl`, `/history` reads it |
| Model/provider selection | `~/.ingenious/ai.json` (global) |

* Context assembly per AI call: system prompt (INGenious domain primer) + session
  context summary + rolling window of recent turns + tool results (truncated/tokened).
* The context avoids repeated questions: e.g. once framework=Playwright is known, the
  Planner never asks again; `/context` displays it; `/clear` resets the transcript but
  keeps project facts unless `--all`.

### 4.3 Planner

* Input: user natural-language request + session context + registry tool schemas.
* Output: a **Plan** — validated JSON:

```json
{
  "goal": "Create a login test",
  "steps": [
    { "id": "s1", "tool": "project_info",       "args": {} },
    { "id": "s2", "tool": "browser_session_start", "args": {"url": "${config.baseUrl}"} },
    { "id": "s3", "tool": "browser_inspect",    "args": {}, "dependsOn": ["s2"] },
    { "id": "s4", "tool": "object_import_page", "args": {"page": "LoginPage"}, "dependsOn": ["s3"] },
    { "id": "s5", "tool": "data_sheet_create",  "args": {"sheet": "login"},  "dependsOn": ["s1"] },
    { "id": "s6", "tool": "testcase_create",    "args": {"...": "..."},      "dependsOn": ["s4","s5"] },
    { "id": "s7", "tool": "testcase_validate",  "args": {"...": "..."},      "dependsOn": ["s6"] }
  ]
}
```

* `dependsOn` gives a DAG → the Execution Engine runs independent steps in parallel
  (e.g. s4 and s5).
* `PlanValidator` rejects hallucinated tools/args *before* execution and asks the AI to
  repair (max 2 retries) — deterministic guardrail around the LLM.
* Planning modes:
  * **Auto** (default): plan → show → execute after approval (or auto if `--yolo`/config).
  * **Plan-only** (`/plan` or "show execution plan"): print plan, store as pending;
    "execute plan" / `/approve` runs it.
  * **Deterministic bypass**: `InputClassifier` matches known intents
    ("create login test", "run smoke tests") to `WorkflowCatalog` templates —
    parameterized plans instantiated *without* an LLM round-trip when unambiguous.
* Clarifying questions: only when a required plan parameter cannot be resolved from
  context (e.g. no base URL configured). Rendered as an interactive prompt/selector.

### 4.4 Execution Engine

* Executes the DAG: topological order, bounded parallelism (configurable, default 4)
  for read-only steps; **mutating steps always serialized**.
* Streams events to `EventBus`: `StepStarted / StepProgress / StepFinished / StepFailed`
  → `ProgressRenderer` shows spinner → ✓/✗ lines, collapsible detail with `--verbose`.
* Step output piping: `${sN.out.path}` references resolved from `ExecutionContext`.
* Failure policy: stop-on-failure by default; the failure, remaining steps, and tool
  error (`suggestions[]` already exist in MCP errors) are handed back to the
  conversation so the AI can propose a repaired plan ("fix and continue?").
* **Undo journal**: before any `mutatesFiles()` step, snapshot affected files (content
  or absence) into `<project>/.ingenious/undo/<planId>/`. `/undo` restores the last
  plan's snapshots; `/redo` re-applies. Journal is bounded (last 10 plans).

### 4.5 Approval Mode

* Any plan containing mutating steps pauses before those steps:

```
Modify  Scenarios/Login/LoginTest.yaml   +42 −5
Create  ObjectRepository/LoginPage.csv   +18
Approve? [Y/n/d(iff)/a(lways)]
```

* `d` renders full colored diff (`DiffRenderer`, java-diff-utils); `a` sets
  auto-approve for the session; config `approvals: auto|prompt|readonly`.
* Implementation: mutating tools run in **dry-run first** (extend `dryRun` to *every*
  write tool — currently only 5 support it; this closes an already-tracked gap), diff
  is computed from would-be output, then the real write executes on approval.

### 4.6 AI Provider Abstraction

```java
public interface AiProvider {
    String name();
    List<String> models();
    ChatResponse chat(ChatRequest req);              // messages + tool schemas
    Stream<ChatDelta> chatStream(ChatRequest req);   // token streaming
}
```

* **CopilotProvider** — GitHub Copilot login via OAuth device flow
  (`ingenious login github`): open `https://github.com/login/device`, poll for token,
  exchange for Copilot API token, store via `TokenStore` (macOS Keychain /
  libsecret / DPAPI, fallback `~/.ingenious/credentials.json` chmod 600).
* **OpenAiCompatProvider** — any OpenAI-compatible chat-completions endpoint
  (covers OpenAI, Azure OpenAI, Ollama/local, corporate gateways) via base-URL + key.
* Provider/model chosen with `/model` or `ingenious config set ai.model=...`.
* All planning/repair/explain calls go through this interface; no provider types leak
  into planner or tools. HTTP via `java.net.http.HttpClient` (no new heavy deps).
* Graceful degradation: with no provider configured, the CLI still fully works for
  slash commands + deterministic workflows, and prints how to enable AI.

---

## 5. Tool Migration Map (MCP → Registry)

All 75 existing MCP tools migrate 1:1 into `tools/impl/**`. Handlers move; behavior,
names, and schemas stay identical (MCP clients must not notice the refactor).

| Category | Tools (existing MCP names) | Count |
|---|---|---|
| **Discovery** | project_list, project_info, scenario_list, scenario_info, testcase_list, testcase_show, action_list, action_search, action_info, action_categories | 10 |
| **Authoring** | testcase_create, testcase_add_step, testcase_insert_step, testcase_edit_step, testcase_move_step, testcase_remove_step, testcase_delete, testcase_validate, scenario_create, scenario_delete, object_list, object_show, object_search, object_add, object_update, object_delete, object_import_page, testset_create, testset_list, testset_show, testset_add, project_create | 22 |
| **Data & Environments** | env_list, env_create, env_delete, data_sheet_create, data_show, data_get, data_set, data_column_add, data_row_add, data_row_delete, data_import, data_generate, config_show, config_get, config_set, config_drivers | 16 |
| **Generation** | gen_list, gen_testcase, gen_from_openapi, gen_from_har, import_playwright, import_curl, import_postman, import_bruno | 8 |
| **Execution** | run, run_async, run_status, run_logs, run_cancel, run_dry | 6 |
| **Reporting** | report_latest, report_history, report_show, report_failures, report_compare, report_export, doctor | 7 |
| **Browser** | browser_session_start, browser_session_do, browser_session_snapshot, browser_session_save, browser_session_close, browser_inspect | 6 |

New tools needed for the CLI experience (also automatically exposed via MCP once in
the registry):

| New tool | Purpose |
|---|---|
| `testcase_clone` | authoring gap called out in requirements |
| `report_screenshot` | screenshot retrieval for failure analysis |
| `report_trends` | trend analysis across run history |
| `project_compile` | compile/validate custom-code projects (deterministic workflow final gate) |
| `workspace_read` / `workspace_diff` | read project files + compute diffs for approval mode |

### 5.1 Browser-test authoring rule (deterministic)

All **new browser-based tests** must follow this pipeline (encoded in
`CreateBrowserTestWorkflow`, never left to AI improvisation):

1. `browser_session_start` — launch playwright-cli daemon session.
2. `browser_inspect` / `browser_session_snapshot` — discovery & object identification
   (accessibility snapshot refs).
3. `object_import_page` — store identified objects in the **INGenious Web OR**
   (`ObjectRepository/<page>.csv`).
4. Author steps as **logical reusable YAMLs** (reusable scenarios referencing OR objects).
5. Compose the **full test case** from those reusables (`testcase_create` + steps).
6. `testcase_validate` → done.

---

## 6. MCP as Compatibility Layer

* `MCPServer` keeps its JSON-RPC protocol handling, prompts, and resources.
* `tools/list` → `ToolRegistry.toolDefinitions()`; `tools/call` →
  `ToolRegistry.get(name).execute(args, mcpContext)`.
* `MCPTools.java` is deleted in the final phase after parity is proven by the existing
  smoke-test procedure (run from `Resources/`, pipe JSON-RPC frames, compare
  `tools/list` output before/after byte-for-byte on names + schemas).
* MCP execution context = non-interactive: approvals auto-resolved by `dryRun`
  convention (unchanged from today), no EventBus UI, no undo journal.
* Result: **zero duplicate implementations**; VS Code / Copilot integration keeps
  working throughout the migration.

---

## 7. Interactive REPL & UX

### 7.1 Entry points

* `ingenious` (no args) → interactive AI CLI (new default; current behavior of
  printing help moves to `ingenious --help`).
* `ingenious <command>` → existing picocli one-shot commands, unchanged.
* Existing `ingenious shell` → alias for the new REPL (supersedes old ShellCommand
  behavior; keep old plain REPL under `shell --basic` during transition).

### 7.2 Slash commands

| Command | Behavior |
|---|---|
| `/help` | command + usage overview panel |
| `/tools [category]` | registry listing (table: id, category, description, ⚠ mutating) |
| `/history` | recent conversation turns (from history.jsonl) |
| `/plan` | show pending/last plan; `/plan run` executes |
| `/clear` | clear transcript (`/clear --all` also clears session facts) |
| `/context` | show session memory (project, framework, recent files, …) |
| `/model` | list/switch provider + model |
| `/undo`, `/redo` | undo journal operations |
| `/status` | active runs (`run_async` sessions), pending approvals, provider status |
| `/config` | view/edit config (delegates to config tools) |
| `/approve` | approve pending plan / file modifications |
| `/exit` | quit (warn on pending approvals / active runs) |

Autocomplete (JLine `Completer`): slash commands, tool ids, and *live project
entities* (scenario names, test case names, OR pages, sheets — pulled from
discovery tools with a small cache).

### 7.3 Rendering

* **Colors/theme**: JLine `AttributedString`; honor `NO_COLOR`, `--no-ansi`, and
  Windows terminals (JLine Jansi bridge already bundled).
* **Panels/banners**: box-drawing with ASCII fallback.
* **Tables**: existing asciitable dependency.
* **Progress**: spinner + streaming ✓ lines via EventBus subscriber; long tool steps
  (run, browser) stream sub-progress (`StepProgress` events piped from engine callbacks).
* **Markdown**: minimal renderer for AI explanations (headings, bold, `code`,
  fenced blocks with basic syntax coloring for java/yaml/json, lists, tables).
* **Diffs**: unified diff, green/red.
* **Interactive selection**: single/multi-select list widget (○/●) for ambiguous
  entity choices ("Found 12 page objects…").
* **Collapsible logs**: default collapsed; `/status` and `--verbose` expand; full logs
  always written to `<project>/.ingenious/logs/`.

---

## 8. Deterministic Workflows (WorkflowCatalog)

Predefined, parameterized plans — instant, reproducible, no LLM required:

| Workflow | Trigger phrases / command | Plan skeleton |
|---|---|---|
| Create browser test | "create login test", `ingenious new test --ui` | project_info → browser discovery pipeline (§5.1) → data_sheet_create → testcase_create → testcase_validate → project_compile |
| Create API test | "create API tests for X", gen_from_openapi path | project_info → gen_from_openapi/archetype → testcase_validate |
| Run suite | "run all smoke tests" | run (tags=smoke) → report_latest → report_failures |
| Fix failing tests | "fix failing tests" | report_failures → (AI analysis) → patch plan w/ approval → re-run failed |
| Migrate Selenium | "migrate this selenium class" | workspace_read → (AI translation via archetypes/guidelines) → OR import → testcase_create → validate |
| Record actions | "open browser", "record actions" | browser_session_start → interactive do-loop → browser_session_save |
| Generate data | "generate data" | data_generate → data_import |

Workflows are themselves Plans, so they flow through the same Execution Engine,
approvals, streaming, and undo — one runtime for AI plans and deterministic plans.

---

## 9. Plugin System

* **Mechanism**: `java.util.ServiceLoader<ToolPlugin>`; plugin jars dropped into the
  existing `Resources/plugins/` directory (already on the runtime scan path).

```java
public interface ToolPlugin {
    String name();                 // "Browser", "API", "Appium", ...
    String version();
    List<Tool> tools();
    default List<Workflow> workflows() { return List.of(); }
}
```

* Built-in tool categories ship as **core** (always present). Optional capability
  packs (aligned with existing INGenious plugin taxonomy): Browser/Playwright +
  Accessibility, Appium, API, Desktop, Performance, Kafka, Database.
* `ingenious plugins` — list installed/available; `ingenious plugins install <name>`
  (Phase 6: local jar / URL; registry download later).
* Registry namespacing prevents id collisions (`plugin:tool_id` on conflict).
* The MCP adapter automatically exposes plugin tools too — one discovery mechanism.

---

## 10. Implementation Phases

### Phase 0 — Scaffolding & Registry Extraction *(foundation, no user-visible change)*
1. Create `com.ing.engine.aicli` package tree; add `Tool`, `ToolResult`,
   `ToolRegistry`, `EventBus`.
2. Extract 75 MCP handlers from `MCPTools.java` into `tools/impl/**` classes
   (mechanical move; keep schemas identical).
3. Rewire `MCPServer` `tools/list` / `tools/call` to the registry.
4. **Gate**: MCP smoke test byte-parity on `tools/list`; all existing CLI commands +
   CLIDemo verification pass; `mvn -pl Engine clean install -DskipTests` green.

### Phase 1 — Interactive REPL (no AI yet)
1. `Repl` with JLine: prompt with project/framework header, history file,
   Ctrl-C = cancel line / Ctrl-D = exit, multiline input (`\` continuation).
2. `SlashCommands`: /help /tools /context /config /status /clear /exit.
3. `SessionContext` + `MemoryStore` (project detection from cwd, persisted facts).
4. UI kit: Theme, Panels, ProgressRenderer, tables; `ingenious` (no args) → REPL.
5. **Gate**: manual UX review; REPL can invoke any registry tool by structured input
   (`/tools run testcase_list`), streaming progress works.

### Phase 2 — Plans, Execution Engine, Approvals, Undo
1. `Plan`/`PlanStep` model + `PlanValidator` + `ExecutionEngine` (DAG, parallel reads).
2. Extend `dryRun` to **all** mutating tools; mutation manifests in `ToolResult`.
3. `ApprovalQueue` + `DiffRenderer`; `/approve`, approval config modes.
4. `UndoJournal`; `/undo`, `/redo`.
5. **Gate**: golden-file tests — execute canned plans against CLIDemo project copy,
   assert file outputs; undo restores byte-identical state.

### Phase 3 — Deterministic Workflows
1. `WorkflowCatalog` + `InputClassifier` (intent matching: keyword/pattern based).
2. Implement Create Browser Test (§5.1 pipeline), Create API Test, Run Suite,
   Generate Data, Record Actions workflows.
3. New tools: `testcase_clone`, `project_compile`, `workspace_read/diff`.
4. **Gate**: "create a login test" end-to-end on CLIDemo *without* any AI provider
   configured; produced test passes `testcase_validate` and runs.

### Phase 4 — AI Providers & Planner
1. `AiProvider` + `OpenAiCompatProvider` (fastest to test, incl. local models) +
   `CopilotProvider` (device-flow login) + `TokenStore`; `/model`, `ingenious login`.
2. `Planner`: system prompt with INGenious domain primer + tool schemas from registry;
   NL → Plan JSON; validation-repair loop.
3. `ConversationManager`: transcript, context windowing, tool-result feedback,
   clarifying-question protocol.
4. Streaming AI output ("Thinking…" + token stream) through EventBus.
5. **Gate**: NL examples matrix (§"Natural Language Examples" in requirements) each
   produce a valid plan; hallucinated-tool rate caught 100% by validator; recorded
   provider-mock tests keep CI offline.

### Phase 5 — AI-assisted Workflows & Repair Loop
1. Fix Failing Tests workflow (report_failures → AI diagnosis → patch plan → re-run).
2. Migrate Selenium / convert-locators flows (AI translation constrained by
   ActionCatalog + guidelines; output always through registry tools, never raw file writes).
3. "Explain" mode (`explain this assertion`, report explanations) with markdown render.
4. Failure-repair loop in Execution Engine (offer AI-repaired plan on step failure).
5. **Gate**: seeded-failure scenario on CLIDemo fixed end-to-end with approvals.

### Phase 6 — Plugin System & Packaging
1. `ToolPlugin` SPI + ServiceLoader discovery from `Resources/plugins/`;
   `ingenious plugins` command; split one optional pack (e.g. Appium tools) as pilot.
2. Distribution: extend existing `ingenious` / `ingenious.bat` / `ingenious.command`
   launchers in `Resources/`; ensure JLine terminal works through them on
   macOS/Linux/Windows.
3. Docs: `Engine/docs/AI-CLI-USER-MANUAL.md`, `AI-CLI-TUTORIAL.md`; update
   `MCP-USER-MANUAL.md` (registry note + fix stale tool count).
4. **Gate**: fresh-install walkthrough on a clean machine profile; plugin
   install/uninstall round-trip.

### Phase 7 — Hardening & Flagship Cutover
1. Delete legacy duplicate paths (`MCPTools` shim internals, old ShellCommand REPL).
2. Performance: REPL cold-start budget (<2s via lazy tool/schema init), entity-cache
   invalidation, token-budget tuning.
3. Telemetry hooks (opt-in only), crash-safe undo journal, Windows terminal QA.
4. **Gate**: success-criteria checklist (§12) signed off.

---

## 11. Testing Strategy

| Layer | Approach |
|---|---|
| Tool registry | JUnit per tool (already-proven CLIDemo fixtures; restore via `git checkout` pattern used for MCP verification). `-Djacoco.skip=true` on JDK 26 (known gotcha). |
| MCP parity | Scripted JSON-RPC smoke test from `Resources/` — `tools/list` + representative `tools/call` before/after refactor. |
| Execution engine | Golden plans → golden file trees; undo/redo round-trip assertions. |
| Planner | Mocked provider with recorded responses; PlanValidator fuzzing (unknown tools, bad args, cyclic deps). |
| REPL/UI | JLine dumb-terminal mode for scripted I/O tests; snapshot tests for panels/tables/diffs. |
| Providers | Contract tests against OpenAI-compat mock server; Copilot device-flow tested manually + token-refresh unit tests. |
| E2E | CI job: launch REPL in dumb terminal, run deterministic "create login test" on CLIDemo, assert artifacts + validate + doctor. |

---

## 12. Success Criteria

- [ ] `ingenious` opens the interactive AI CLI with project/framework banner.
- [ ] All 75+ tools invocable through the registry; `/tools` lists them.
- [ ] MCP server (`ingenious server mcp`) exposes the identical tool set via the
      registry — VS Code/Copilot integration unaffected.
- [ ] "create a login test" works end-to-end **without** an AI provider (deterministic)
      and **with** one (planned), producing OR entries, reusable YAMLs, data CSV, and a
      validated test case per §5.1.
- [ ] Every mutating action supports dry-run, diff preview, approval, and undo.
- [ ] Plans are visible (`/plan`), deterministic workflows reproducible, parallel-safe.
- [ ] GitHub Copilot login + at least one OpenAI-compatible provider; `/model` switches.
- [ ] Plugin jar in `Resources/plugins/` contributes tools discovered at startup.
- [ ] Streaming progress, colors, tables, markdown, diffs render on macOS, Linux,
      Windows terminals (with ASCII/NO_COLOR fallbacks).
- [ ] No duplicated capability implementations between CLI and MCP.

---

## 13. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Refactor breaks MCP clients | Phase 0 gate = byte-parity smoke test; schemas frozen. |
| LLM hallucinates tools/args | PlanValidator hard gate + repair loop; deterministic workflows for hot paths. |
| playwright-cli unavailable on user machine | Existing `playwrightCliBase()` fallback to `npx @playwright/cli`; `doctor` tool pre-flight check surfaced in workflow step 1. |
| Copilot API/auth changes | Provider abstraction isolates it; OpenAI-compat provider is the fallback. |
| JLine quirks on Windows terminals | Jansi bridge already bundled; dumb-terminal fallback; Phase 7 QA pass. |
| Undo journal corruption on crash | Journal writes are snapshot-before-mutate + fsync manifest; partial journals detected and quarantined. |
| Token/credential leakage | OS keychain first, 600-perm file fallback, never logged, redacted from transcripts. |
| Scope creep in AI features | AI is additive: Phases 0–3 deliver full value without any provider. |

---

## 14. Decisions Locked / Deliberate Choices

* **Java 17 / Maven / Engine module** — align with existing ecosystem; no new module
  until plugin split (Phase 6) justifies one.
* **JLine 3 (already a dependency) over Lanterna** — Lanterna implies full-screen TUI;
  the target UX is a scrolling conversational REPL (Claude-Code-style), which JLine
  does natively with less risk. Lanterna can be revisited for a future full-screen mode.
* **Plans as JSON data** — enables plan-only mode, approvals, replay, undo, and tests.
* **MCP kept as adapter, not deleted** — flagship UX is the CLI; MCP remains for IDE
  and external agent integration at near-zero marginal cost.
* **Deterministic-first** — the classifier prefers WorkflowCatalog over the Planner;
  AI handles the long tail, not the hot path.
