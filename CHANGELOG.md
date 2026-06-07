# INGenious — `ingenious-mcp` Branch Change Log

> Scope: new development on the `ingenious-mcp` branch on top of
> `enhancements/3.1.1`. Everything in this document is **new** to this
> branch — earlier features are not repeated.

## Table of contents

1. [INGenious MCP server — full AI-agent surface over JSON-RPC stdio](#1-ingenious-mcp-server--full-ai-agent-surface-over-json-rpc-stdio)
2. [`ingenious import` — unified curl / Postman / Bruno / Playwright entry point](#2-ingenious-import--unified-curl--postman--bruno--playwright-entry-point)
3. [CLI expansions — `scenario` / `testcase` / `data` create-side coverage](#3-cli-expansions--scenario--testcase--data-create-side-coverage)
4. [`ServerCommand` slim-down — MCP logic extracted to `com.ing.engine.mcp`](#4-servercommand-slim-down--mcp-logic-extracted-to-comingenginemcp)
5. [Playwright recording importer — unified into Datalib](#5-playwright-recording-importer--unified-into-datalib)
6. [Engine-side `RequestToTestCaseBuilder` — UI-free port of the IDE step builder](#6-engine-side-requesttotestcasebuilder--ui-free-port-of-the-ide-step-builder)
7. [`BrowserNames` normalisation — `No Browser` literal accepted everywhere](#7-browsernames-normalisation--no-browser-literal-accepted-everywhere)
8. [CLI dispatcher — whitelist `import` so it routes to picocli](#8-cli-dispatcher--whitelist-import-so-it-routes-to-picocli)
9. [IDE — File → Reload Project (in-place, no app restart)](#9-ide--file--reload-project-in-place-no-app-restart)
10. [IDE — `ProjectWatcher` auto-reload on external project changes](#10-ide--projectwatcher-auto-reload-on-external-project-changes)
11. [IDE — `TreeStateSaver`: expand/select preservation across reload](#11-ide--treestatesaver-expandselect-preservation-across-reload)
12. [IDE — Object Repository DnD = move-only](#12-ide--object-repository-dnd--move-only)
13. [Repository hygiene — legacy `IOR.object` stubs removed from bundled projects](#13-repository-hygiene--legacy-iorobject-stubs-removed-from-bundled-projects)
14. [Documentation — `Engine/docs/MCP-USER-MANUAL.md`](#14-documentation--enginedocsmcp-user-manualmd)

---

## 1. INGenious MCP server — full AI-agent surface over JSON-RPC stdio

**Feature**
A new long-running server process that exposes the full INGenious CLI
surface over the **Model Context Protocol** (revision `2024-11-05`), so
AI agents such as GitHub Copilot, Claude Desktop, Cursor and Continue can
**create, run and debug** INGenious tests from natural language.

**Module layout** — new package `com.ing.engine.mcp`:

| File | Purpose |
|---|---|
| [Engine/src/main/java/com/ing/engine/mcp/MCPServer.java](Engine/src/main/java/com/ing/engine/mcp/MCPServer.java) | JSON-RPC 2.0 dispatcher over newline-delimited UTF-8 stdio frames; routes `initialize`, `tools/list`, `tools/call`, `prompts/list`, `prompts/get`, `resources/list`, `resources/read`, `ping`, `logging/setLevel`, `shutdown`. |
| [Engine/src/main/java/com/ing/engine/mcp/MCPTools.java](Engine/src/main/java/com/ing/engine/mcp/MCPTools.java) | **36 tools** — project / scenario / test-case / test-set / action-catalog / run / report / config / data / env / import. Every tool returns both `content[].text` and `structuredContent`. |
| [Engine/src/main/java/com/ing/engine/mcp/MCPPrompts.java](Engine/src/main/java/com/ing/engine/mcp/MCPPrompts.java) | **7 prompts** — `create_test_case`, `convert_manual_steps`, `explain_failure`, `debug_test`, `suggest_locator`, `review_test_case`, `run_and_summarize`. |
| [Engine/src/main/java/com/ing/engine/mcp/MCPResources.java](Engine/src/main/java/com/ing/engine/mcp/MCPResources.java) | **3 resources** — `ingenious://catalog/actions`, `ingenious://docs/getting-started`, `ingenious://docs/step-schema` (+ a fourth `ingenious://project/<name>/summary` when launched with `--project`). |
| [Engine/src/main/java/com/ing/engine/mcp/ActionCatalog.java](Engine/src/main/java/com/ing/engine/mcp/ActionCatalog.java) | Reflection over `@Action` annotations to populate the action catalogue used by `ingenious_action_*` tools and the `actions` resource. Falls back to a package-based default (`commands.browser.*` → Browser, etc.) when the annotation doesn't set `object()`. |

**Tool inventory (36)** — naming convention `ingenious_<area>_<verb>`:

| Area | Tools |
|---|---|
| Project | `ingenious_project_list`, `ingenious_project_info`, `ingenious_project_create` |
| Scenario | `ingenious_scenario_list`, `ingenious_scenario_create` |
| Test case | `ingenious_testcase_list`, `ingenious_testcase_show`, `ingenious_testcase_create`, `ingenious_testcase_add_step`, `ingenious_testcase_delete` |
| Test set | `ingenious_testset_list`, `ingenious_testset_show` |
| Action catalog | `ingenious_action_list`, `ingenious_action_search`, `ingenious_action_info`, `ingenious_action_categories` |
| Run | `ingenious_run`, `ingenious_run_async`, `ingenious_run_status`, `ingenious_run_logs`, `ingenious_run_cancel` |
| Report | `ingenious_report_latest`, `ingenious_report_history`, `ingenious_report_failures` |
| Config | `ingenious_config_get`, `ingenious_config_set` |
| Data sheets | `ingenious_data_sheet_create`, `ingenious_data_row_add`, `ingenious_data_column_add` |
| Environments | `ingenious_env_list`, `ingenious_env_create`, `ingenious_env_delete` |
| Import | `ingenious_import_curl`, `ingenious_import_postman`, `ingenious_import_bruno`, `ingenious_import_playwright` |

**CLI entry point** — `ingenious server mcp`, registered in
[ServerCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/ServerCommand.java)
as `McpCommand`. Flags:

- `-p, --project <name|path>` — default project for tools that take an optional `project` arg
- `-v, --verbose` — echo every RX/TX frame to **stderr** (never stdout)

**Critical safeguard — stdout protection**
`MCPServer` redirects `System.out` → `System.err` at startup and captures
the real stdout descriptor for JSON-RPC frames. Datalib and plugins
routinely print diagnostic messages to `System.out` ("properties file
already exists", etc.) which would otherwise corrupt the framing
protocol. This is the single most important reason the server isn't a
thin shell around `ProcessBuilder` — the JVM has to be the same one that
loads Datalib.

**Run-tool process model**
`ingenious_run` / `ingenious_run_async` spawn a child
`java … INGeniousCLI run` process. Sync version blocks up to
`timeoutSeconds` (default 1800). Async version returns a `runId`
immediately; `ingenious_run_status` / `ingenious_run_logs` /
`ingenious_run_cancel` work against that id. Status values:
`RUNNING`, `PASS`, `FAIL`, `TIMEOUT`, `CANCELLED`, `INTERRUPTED`.

**How to verify**
```bash
mvn -DskipTests install
cd Dist/release
./ingenious server mcp --help              # subcommand visible
printf '%s\n' \
'{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"smoke","version":"1.0"}}}' \
'{"jsonrpc":"2.0","method":"notifications/initialized"}' \
'{"jsonrpc":"2.0","id":2,"method":"tools/list"}' \
| ./ingenious server mcp
```
The full CRUD smoke test (create → append → show → delete) is documented
in [Engine/docs/MCP-USER-MANUAL.md §4.3](Engine/docs/MCP-USER-MANUAL.md).

**Breaking changes:** none — additive feature.

---

## 2. `ingenious import` — unified curl / Postman / Bruno / Playwright entry point

**Feature**
A new top-level CLI command that converts external API and UI artefacts
into INGenious test cases or reusable components. Four format-specific
subcommands behind one entry point.

**New file** —
[Engine/src/main/java/com/ing/engine/cli/commands/ImportCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/ImportCommand.java):

| Subcommand | Source | Datalib parser used |
|---|---|---|
| `ingenious import curl <project> <curl-string>` | inline curl invocation | `CurlParser.parse(String) → APIRequest` |
| `ingenious import postman <project> <file-or-folder>` | Postman v2/v2.1 collection (JSON file or folder) | `PostmanImporter` (impl. of `spi.CollectionImporter`) |
| `ingenious import bruno <project> <folder>` | Bruno `.bru` collection folder | `BrunoImporter` |
| `ingenious import playwright <project> <file>` | Playwright `codegen` recording (JS/TS) | `PlaywrightRecordingImporter` (see §5) |

All four flow through the engine-side
[RequestToTestCaseBuilder](Engine/src/main/java/com/ing/engine/cli/lib/RequestToTestCaseBuilder.java)
(see §6) so the generated steps are identical to what the API Workbench
produces in the IDE.

**Wiring**
- [INGeniousCLI.java](Engine/src/main/java/com/ing/engine/cli/INGeniousCLI.java) — `ImportCommand.class` added to the `subcommands = { … }` array.
- [Control.java](Engine/src/main/java/com/ing/engine/core/Control.java) — `"import"` added to `isNewCLICommand()` whitelist (see §8).

**MCP mirror**
The four importers are also exposed as MCP tools
(`ingenious_import_curl`, `…_postman`, `…_bruno`, `…_playwright`) in
[MCPTools.java](Engine/src/main/java/com/ing/engine/mcp/MCPTools.java),
sharing the same datalib + `RequestToTestCaseBuilder` plumbing.

**Breaking changes:** none — new command group.

---

## 3. CLI expansions — `scenario` / `testcase` / `data` create-side coverage

**Feature**
Several existing CLI groups gained "create" / "add" subcommands so that
everything achievable through the IDE is now scriptable.

**Scenario** —
[ScenarioCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/ScenarioCommand.java)

- `ingenious scenario create <name> [--reusable]` — creates a scenario
  under `TestPlan/` (default) or `ReusableComponents/` (`--reusable`).

**Test case** —
[TestCaseCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/TestCaseCommand.java)

- `ingenious testcase create <scenario> <testcase> [--reusable] [--format YAML|CSV]`
  - Honors `ProjectInfo.getTestCaseFormat()` by default; `--format`
    overrides per-invocation.
  - With `--reusable`, creates the test case under the matching
    reusable scenario.

**Data sheets, rows, columns, environments** —
[DataCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/DataCommand.java)
(+363 lines)

| Subcommand | Purpose |
|---|---|
| `ingenious data sheet create <name> [--env <env>]` | Create a new test-data sheet (optionally inside an environment). |
| `ingenious data row add <sheet> --scenario <s> --testcase <tc> [--iteration N] [--sub-iteration N] [--col key=val …]` | Append a record; reusable references use the conventional `"(R) "` prefix on the Scenario column. |
| `ingenious data column add <sheet> <columnName>` | Add a new column to an existing sheet. |
| `ingenious data env list` | List configured environments. |
| `ingenious data env create <name> [--from <env>] [--sheets <a,b,c>] [--with-global]` | Create a new environment, optionally duplicating sheets from an existing one and including Global Data. |
| `ingenious data env delete <name>` | Delete an environment. |

**Datalib hooks used (verified)**
- `EnvTestData.getAllEnvironments()`, `getEnvironments()`,
  `getTestDataFor(name)`, `createNewEnvironment(name, dupFrom, sheets, withGlobal)`, `deleteEnvironment(name)`
- `TestData.getTestDataList()`
- `TestDataModel.loadTableModel() / addRecord() / addColumn() / getColumnIndex() / setValueAt() / getName()`
- `Record.setScenario/setTestcase/setIteration/setSubIteration()`

**Small touch-ups in adjacent commands** (additive, no behaviour change
for existing callers):
- [ActionCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/ActionCommand.java) — +14 lines
- [ProjectCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/ProjectCommand.java) — +55 lines
- [RunCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/RunCommand.java) — +61 lines (BrowserNames hook in all 5 spots + rerun, see §7)
- [UpgradeCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/UpgradeCommand.java) — 1-line fix

**MCP mirror**
All new CLI subcommands have matching MCP tools (see §1 inventory).

**Breaking changes:** none.

---

## 4. `ServerCommand` slim-down — MCP logic extracted to `com.ing.engine.mcp`

**Refactor**
[ServerCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/ServerCommand.java)
shrank from **~1100 → 259 lines** (~879 deletions). All MCP protocol /
tool / prompt / resource code moved into the dedicated
`com.ing.engine.mcp.*` package (see §1) — keeping
the picocli command file focused on flag parsing and process lifecycle.

**Resulting structure**
- `ServerCommand` (parent) — `mcp`, `rest`, `status` subcommands.
- `ServerCommand.McpCommand` — parses `-p` / `-v`, instantiates
  `new MCPServer(project, verbose).start()`.
- `ServerCommand.RestCommand` / `StatusCommand` — unchanged.

**Why** — the MCP package is now independently unit-testable (no picocli
dependency, no `System.exit` traps), and the CLI surface for `server`
stays readable.

**Breaking changes:** none — public CLI behaviour is identical.

---

## 5. Playwright recording importer — unified into Datalib

**Refactor**
There were two completely separate Playwright parsers in the codebase
(IDE Swing wizard vs. an Engine-side `PlaywrightImporter`). They have
been unified into a single datalib class that the IDE, the new
`ingenious import playwright` CLI subcommand, and the
`ingenious_import_playwright` MCP tool all delegate to.

**Single source of truth** — new file:
[Datalib/src/main/java/com/ing/datalib/api/importer/playwright/PlaywrightRecordingImporter.java](Datalib/src/main/java/com/ing/datalib/api/importer/playwright/PlaywrightRecordingImporter.java).

**IDE delegate** —
[IDE/src/main/java/com/ing/ide/main/playwrightrecording/PlaywrightRecordingParser.java](IDE/src/main/java/com/ing/ide/main/playwrightrecording/PlaywrightRecordingParser.java)
collapsed from **597 → 33 lines** (-564). It is now a thin wrapper:

```java
PlaywrightRecordingImporter.importInto(project, file, null, null);
```

**Engine CLI + MCP** — both call the same datalib entry point. No
`--reusable` flag (the IDE parser never supported reusables either).

**Behaviour details**
- Uses `Scenario.addTestCase()` + `tc.addNewStep()` + `tc.save()` (not
  raw CSV writes), so it respects the project's YAML format default.
- Drops the auto-created blank step at index 0 if real steps were
  produced.
- Object selectors land in the OR (page → ObjectGroup → WebORObject
  with `Role`/`xpath`/`Text`/`css`/`Placeholder`/`Label`/`AltText`/
  `Title`/`TestId`/`ChainedLocator`); the test step references the
  object via `setReference("[Project] <PageName>")`.
- Chained `page.locator("X").fill("v")` calls are handled correctly —
  the parser splits on `\)\.` and uses per-method attribute extraction
  (the deleted engine-side `PlaywrightImporter` used a single
  `page.X(...)` regex that swallowed `.fill(...)` and
  `assertThat(...)`).

**Breaking changes:** none — IDE behaviour is preserved; CLI / MCP gain
the same capability.

---

## 6. Engine-side `RequestToTestCaseBuilder` — UI-free port of the IDE step builder

**Feature**
A new helper class that turns an `APIRequest` into a fully-formed test
case under a given `Scenario`, without any Swing / IDE dependency. This
is the engine-side equivalent of `APITester.buildStepsForRequest()`.

**New file** —
[Engine/src/main/java/com/ing/engine/cli/lib/RequestToTestCaseBuilder.java](Engine/src/main/java/com/ing/engine/cli/lib/RequestToTestCaseBuilder.java).

**Step shape produced**

```
setEndPoint   → addHeader (× N) → auth header (BASIC / BEARER / API_KEY)
              → HTTP method     (getRestRequest / postRestRequest / putRestRequest
                                 / patchRestRequest / deleteRestRequest /
                                 deleteWithPayload)
              → assertions      (STATUS_CODE / BODY_CONTAINS / HEADER /
                                 JSON_PATH / XPATH)
```

All input values are prefixed with `@` (per the Webservice action
literal convention).

**Used by**
- `ingenious import curl` (CLI) + `ingenious_import_curl` (MCP)
- `ingenious import postman` + `ingenious_import_postman`
- `ingenious import bruno` + `ingenious_import_bruno`
- `ingenious import playwright` + `ingenious_import_playwright` (via the
  unified datalib importer in §5, for API-shaped recordings)

**Why** — avoids re-implementing the API-Workbench step generator
three times. Identical output regardless of entry point.

**Breaking changes:** none — new helper.

---

## 7. `BrowserNames` normalisation — `No Browser` literal accepted everywhere

**Feature**
A small normalisation helper so that CLI and MCP callers can pass any
common spelling (`NoBrowser`, `no-browser`, `no browser`, `none`-style
mistakes flagged) and have it resolve to the engine's canonical literal
`No Browser`.

**New file** —
[Engine/src/main/java/com/ing/engine/cli/lib/BrowserNames.java](Engine/src/main/java/com/ing/engine/cli/lib/BrowserNames.java)
with a single `normalize(String) → String` entry point.

**Wired into**
- [RunCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/RunCommand.java)
  — all 5 spots that read `--browser` plus the rerun command.
- [MCPTools.java](Engine/src/main/java/com/ing/engine/mcp/MCPTools.java)
  `parseRunSpec()` — same normalisation, same allow-list.

**Engine acceptance (unchanged)** —
`WebDriverFactory` and `PlaywrightDriverFactory` already accept the
literal `"No Browser"`. Only the CLI / MCP input layer needed
normalisation.

**Valid `browser` values for `ingenious_run` / `ingenious_run_async`:**
`Chromium`, `Firefox`, `WebKit`, or `No Browser`
(aliases: `NoBrowser`, `no-browser`).
For headless browser runs use `browser=Chromium` + `headless=true` —
do **not** pass `nogui`/`headless`/`none` as a browser value (those used
to fail silently, with status `PASS` but an NPE in
`Control.endExecution`).

**Breaking changes:** none — pure normalisation; canonical names still
work as before.

---

## 8. CLI dispatcher — whitelist `import` so it routes to picocli

**Change** —
[Control.java](Engine/src/main/java/com/ing/engine/core/Control.java)

`isNewCLICommand()` had to learn the new top-level verb `"import"`,
otherwise `ingenious import curl …` would silently fall through to the
legacy `-run`-style CLI parser and fail with `Unrecognized option: …`
(the same class of bug fixed previously for `object / testset / data`).

```diff
 String[] newCommands = {
     "project", "scenario", "testcase", "testset",
     "object", "objects", "or",
     "data", "action", "actions",
+    "import",
     "run", "report", "config", "server",
     "shell", "interactive", "repl",
     "help", "--help", "-h", "--version", "-v", "-V"
 };
```

**Breaking changes:** none.

---

## 9. IDE — File → Reload Project (in-place, no app restart)

**Feature**
A new **File → Reload Project** menu item (and a reload icon in the Test
Plan panel header) re-loads the current project from disk **in place**,
preserving the `Project` instance. Refreshes TestDesign, TestExecution,
dashboard, API Workbench and recent-items lists. Picks up new test
cases / reusables / data sheets / OR objects added from outside the IDE
(CLI, MCP server, another editor) without an app restart.

**Files**
- [IDE/src/main/java/com/ing/ide/main/mainui/AppMainFrame.java](IDE/src/main/java/com/ing/ide/main/mainui/AppMainFrame.java) — new `reloadProject()` method (+185 lines incl. tree snapshot plumbing, see §11). Calls `saveLoadedProject()`, then `sProject.reload()`, then `load()` + `afterProjectChange()`.
- [IDE/src/main/java/com/ing/ide/main/mainui/AppActionListener.java](IDE/src/main/java/com/ing/ide/main/mainui/AppActionListener.java) — handles `"Reload Project"` action command (Cmd/Ctrl+Shift+R).
- [IDE/src/main/java/com/ing/ide/main/mainui/AppMenuBar.java](IDE/src/main/java/com/ing/ide/main/mainui/AppMenuBar.java) / [IDE/src/main/java/com/ing/ide/main/fx/FXMenuBar.java](IDE/src/main/java/com/ing/ide/main/fx/FXMenuBar.java) — menu entry under **File**.
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/TestDesignUI.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/TestDesignUI.java) — adds a reload icon to `getTreeInPanel`.

**Breaking changes:** none — additive menu entry.

---

## 10. IDE — `ProjectWatcher` auto-reload on external project changes

**Feature**
When the IDE detects that files in the open project changed outside the
app (e.g. CLI / MCP added a scenario, another editor edited a YAML), it
triggers `reloadProject()` automatically.

**New file** —
[IDE/src/main/java/com/ing/ide/main/mainui/ProjectWatcher.java](IDE/src/main/java/com/ing/ide/main/mainui/ProjectWatcher.java).

**Design**
- Polling-based daemon thread (2 s poll interval, 1.5 s stability
  window). FNV-mixed fingerprint over path + mtime + size.
- Polling chosen over `WatchService` because macOS silently falls back
  to a slow `PollingWatchService` for FSEvents anyway — explicit
  polling gives predictable behaviour cross-platform.
- Skips `.git / .svn / .idea / .vscode / node_modules / target / build
  / dist / out` and any hidden files. Ignores
  `~/.tmp/.swp/.bak/.lock/.part/.crswap` suffixes.
- Feedback-loop guard: `beginIdeWrite() / endIdeWrite()` brackets
  `saveLoadedProject()` and `reloadProject()` so the IDE's own writes
  are re-baselined as the new "known good" fingerprint instead of being
  detected as external changes. 2.5 s grace period after each write.

**Persistent toggle** —
[AppSettings.java](IDE/src/main/java/com/ing/ide/settings/AppSettings.java)
gains `AUTO_RELOAD` (default `true`). Controlled by a new
**Auto Reload** pill toggle in
[FXToolBar.java](IDE/src/main/java/com/ing/ide/main/fx/FXToolBar.java)
(next to **Auto Save**, +38 lines). Old menu placeholders were removed
from `AppMenuBar.java` and `FXMenuBar.java`.

`AppMainFrame.setAutoReloadEnabled(boolean)` persists the setting,
starts / stops the watcher, and syncs the toolbar state.

**Breaking changes:** none — opt-out via the toolbar pill.

---

## 11. IDE — `TreeStateSaver`: expand/select preservation across reload

**Feature**
A small utility that captures and restores `JTree` expanded paths and
selection by node `toString()` keys, so that the in-place reload
(see §9) doesn't collapse the test-design / OR trees.

**New file** —
[IDE/src/main/java/com/ing/ide/main/utils/tree/TreeStateSaver.java](IDE/src/main/java/com/ing/ide/main/utils/tree/TreeStateSaver.java).

**Design**
- Keys are joined with NUL separators (`\u0000`), so labels containing
  `/` or `.` don't collide.
- Survives model rebuilds where the `TreeNode` *instances* are recreated
  but the displayed labels stay the same — exactly the `Project.reload`
  pattern.

**Wiring**
- `AppMainFrame.reloadProject()` calls `captureTreeSnapshots()` before
  `sProject.reload()` and restores via a nested
  `SwingUtilities.invokeLater` after `afterProjectChange()`, so
  scenarios, reusables and OR pages stay expanded across reload.
- Trees handled: TestPlan ProjectTree, ReusableTree, and all 8 OR
  trees (Web / Mobile / StructuredData / SAP × project / shared, via
  `ObjectRepo.getWebOR().getProjectTree().getTree()` etc.).
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/ObjectTree.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/ObjectTree.java)
  — the public `reload()` also wraps `DefaultTreeModel.reload()` with
  `TreeStateSaver.capture/restore`, so cut/copy/paste/delete and DnD no
  longer collapse expanded pages. (Sort still calls
  `getModel().reload(node)` directly — collapses only that subtree, by
  design.)

**Breaking changes:** none.

---

## 12. IDE — Object Repository DnD = move-only

**Change** —
[IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/ObjectDnD.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/ObjectDnD.java).

- `getSourceActions()` now returns `MOVE` only (was `MOVE|COPY`).
- `importData()` forces `shouldCut = true` for any drop
  (`ts.isDrop() ? TRUE : isCut`), so dragging an object between pages
  always **moves** it.
- Clipboard-driven paste still respects cut/copy intent — only the DnD
  path changed.

**Why** — drag-then-copy was a frequent source of duplicate-object
bugs in the OR.

**Breaking changes:** users who relied on drag-to-copy behaviour need
to use **Copy → Paste** instead (which still works).

---

## 13. Repository hygiene — legacy `IOR.object` stubs removed from bundled projects

**Cleanup**
The three bundled sample projects had empty/stub legacy XML
`IOR.object` files left over from pre-YAML days. They were removed so
fresh users don't see ghost files that the new YAML OR loader doesn't
need:

- `Resources/Projects/CLIDemo/IOR.object` (deleted)
- `Resources/Projects/Mobile/IOR.object` (deleted)
- `Resources/Projects/Tutorial/IOR.object` (deleted)

These projects already had per-page YAML OR files; the XML stubs were
1-byte placeholders.

**Breaking changes:** none — the YAML OR loader is the active code path
and was already in use.

---

## 14. Documentation — `Engine/docs/MCP-USER-MANUAL.md`

**New document** —
[Engine/docs/MCP-USER-MANUAL.md](Engine/docs/MCP-USER-MANUAL.md)
(~680 lines).

**Contents**
1. What is the MCP server? (capability counts + module layout)
2. Prerequisites (JDK 17+, Maven 3.8+, MCP client)
3. Build and install
4. Quick start — manual smoke test (initialize handshake + full CRUD
   round-trip with a Python validator that asserts `CLEAN` framing)
5. Wiring an AI client — Claude Desktop, VS Code / GitHub Copilot,
   Cursor, Continue (with the exact JSON for each `mcpServers` block)
6. **Tool reference (36)** — every tool with required/optional args
   and return shape
7. **Prompt reference (7)**
8. **Resource reference (3)** (+ the conditional 4th
   `ingenious://project/<name>/summary`)
9. End-to-end workflows — author from English, convert manual script,
   triage a failure, long-running suite via async + poll + tail +
   cancel
10. Operations & troubleshooting — stdout pollution, empty `steps`,
    delete-not-found, wrong action categories, run hangs, logging
11. Reference — full JSON-RPC envelopes for `initialize` / `tools/call`
    / errors / supported methods

**Breaking changes:** none — pure documentation.

---

## Build & verification

- Build command: `mvn -DskipTests install` (full reactor, ~15 s).
- Engine-only fast path: `mvn -pl Engine -am -DskipTests install`.
- Smoke test for the MCP server: see §1 ("How to verify") and the full
  CRUD round-trip in [Engine/docs/MCP-USER-MANUAL.md §4.3](Engine/docs/MCP-USER-MANUAL.md).
- All changes are additive at the public CLI / IDE level; existing
  workflows are unchanged.
