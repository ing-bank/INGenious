# INGenious MCP — Comprehensive Implementation & Enhancement Plan

> A modern, AI-first roadmap for turning the INGenious MCP server into a
> complete conversational test-automation platform. This document builds on
> the **25 tools / 7 prompts / 3 resources** already shipped (see
> [`MCP-USER-MANUAL.md`](./MCP-USER-MANUAL.md)) and lays out what to build
> next, how to expose it through new CLI commands, and how agents can author
> *every* kind of INGenious test — including live Playwright-driven browser
> authoring.

---

## Table of contents

1. [Guiding principles](#1-guiding-principles)
2. [Where we are today](#2-where-we-are-today)
3. [Gap analysis — CLI ↔ MCP parity](#3-gap-analysis--cli--mcp-parity)
4. [Missing CLI commands to build](#4-missing-cli-commands-to-build)
5. [New MCP tools to expose](#5-new-mcp-tools-to-expose)
6. [Test-case archetypes — authoring every test type](#6-test-case-archetypes--authoring-every-test-type)
7. [Playwright CLI integration for browser tests](#7-playwright-cli-integration-for-browser-tests)
8. [Modern UX layer — prompts, resources, guided flows](#8-modern-ux-layer--prompts-resources-guided-flows)
9. [Cross-cutting capabilities](#9-cross-cutting-capabilities)
10. [Phased roadmap](#10-phased-roadmap)
11. [Architecture & implementation notes](#11-architecture--implementation-notes)

---

## 1. Guiding principles

| Principle | What it means for this plan |
|-----------|-----------------------------|
| **CLI-first, MCP-thin** | Every capability lands as a `picocli` subcommand first. The MCP tool is a thin adapter that reuses the same Datalib/engine call — never a second implementation. This is the pattern already used by `ActionCatalog` (shared by `ingenious action` and MCP). |
| **Discover before compose** | Agents must never invent action names, object locators, or data columns. Every authoring flow starts with a *discovery* tool (`action_search`, `object_search`, `data_show`). |
| **Deterministic, structured output** | Every tool returns both `content[].text` (human) and `structuredContent` (machine). New tools follow the same contract. |
| **Round-trippable** | Anything an agent creates it can read back (`show`) and delete. No write-only operations. |
| **Safe by default** | Destructive tools (`delete`, `reset`, `run_cancel`) require explicit names and never wildcard. Long runs go async. |
| **Fidelity to the IDE** | MCP writes the same YAML/CSV the desktop IDE reads. No parallel formats. |

---

## 2. Where we are today

**Shipped CLI surface (15 top-level commands):**

```
project   scenario   testcase   testset   object   data   action
run       report     config     server    shell    import legacy   upgrade
```

**Shipped MCP tools (75):** project (`list`/`info`/`create`), scenario
(`list`/`info`/`create`/`delete`), testcase
(`list`/`show`/`create`/`add_step`/`delete`/`validate`/`edit_step`/`insert_step`/`remove_step`/`move_step`),
testset (`list`/`show`/`create`/`add`), object
(`list`/`show`/`search`/`add`/`update`/`delete`/`import_page`), action
(`list`/`search`/`info`/`categories`), run (`sync`/`async`/`status`/`logs`/`cancel`/`dry`),
report (`latest`/`history`/`failures`/`show`/`compare`/`export`), config
(`get`/`set`/`show`/`drivers`), data
(`sheet_create`/`row_add`/`column_add`/`show`/`get`/`set`/`row_delete`/`import`/`generate`),
env (`list`/`create`/`delete`), import (`curl`/`postman`/`bruno`/`playwright`),
gen (`list`/`testcase`/`from_openapi`/`from_har`),
browser (`session_start`/`session_do`/`session_snapshot`/`session_save`/`session_close`/`inspect`),
and `doctor`.

> **Update (Phases 1–4 + all follow-ups landed):** the parity tools plus
> step-level test-case editing, Object-Repository write (add/update/delete +
> live `import_page`), data `row_delete`/`import`/`generate`, `report_export`,
> `config_drivers`, `run_dry`, `run --rerun`, `doctor`, the full **Playwright
> Agent CLI `browser session`** suite, and **Phase 3 archetype generation**
> (`gen_list`/`gen_testcase` + `gen_from_openapi`/`gen_from_har`, backed by
> `ArchetypeCatalog`) are implemented in `MCPTools.java` (+ `PlaywrightCliTranslator`,
> `ArchetypeCatalog`). All are thin adapters over existing CLI/Datalib logic;
> the non-browser tools are verified end-to-end against CLIDemo. The browser
> tools drive the real `@playwright/cli` (falling back to `npx @playwright/cli`)
> and degrade with a clear message when absent — `ingenious_doctor` detects it.

**Shipped prompts (13):** `create_test_case`, `convert_manual_steps`,
`explain_failure`, `debug_test`, `suggest_locator`, `review_test_case`,
`run_and_summarize`, `author_by_archetype`, `build_data_driven_suite`,
`harden_test`, `triage_run`, `record_browser_test`, `bootstrap_project`.

**Shipped resources (5):** `ingenious://catalog/actions`,
`ingenious://catalog/archetypes`, `ingenious://docs/getting-started`,
`ingenious://docs/step-schema`, `ingenious://docs/best-practices`
(+ a per-project `.../summary` when launched with `--project`).

> The foundation is strong: create → run → report is fully covered. The gaps
> are in **authoring depth** (object repository, data-driven, test sets),
> **discoverability** (import, environments, run history), and **modern
> browser authoring** (Playwright live capture).

---

## 3. Gap analysis — CLI ↔ MCP parity

Original gaps between the CLI and MCP. Most are now closed (✅ **done**); a
few remain as follow-ups:

| CLI capability | In CLI | MCP tool | Status |
|----------------|:------:|:--------:|--------|
| `scenario info` | ✅ | `ingenious_scenario_info` | ✅ done |
| `scenario delete` | ✅ | `ingenious_scenario_delete` | ✅ done |
| `testcase validate` | ✅ | `ingenious_testcase_validate` | ✅ done |
| `testset create` | ✅ | `ingenious_testset_create` | ✅ done |
| `testset add` | ✅ | `ingenious_testset_add` | ✅ done |
| `object list/show/search` | ✅ | `ingenious_object_list/show/search` | ✅ done |
| `data show/get/set` | ✅ | `ingenious_data_show/get/set` | ✅ done |
| `data env list/create/delete` | ✅ | `ingenious_env_list/create/delete` | ✅ done |
| `report show/compare` | ✅ | `ingenious_report_show/compare` | ✅ done |
| `config show` | ✅ | `ingenious_config_show` | ✅ done |
| `import curl/postman/bruno/playwright` | ✅ | `ingenious_import_*` | ✅ done |
| `object create/update/delete` | partial | `ingenious_object_add/update/delete` | ✅ done |
| `data import` | ✅ | `ingenious_data_import` | ✅ done |
| `report export` | ✅ | `ingenious_report_export` | ✅ done |
| `config drivers` | ✅ | `ingenious_config_drivers` | ✅ done |
| `run tags` / `run --rerun` | ✅ | `ingenious_run` (tags + `rerun`) | ✅ done |

**Result:** the parity tools were delivered as thin adapters over existing CLI /
Datalib logic — no new engine code — and the authoring-depth, follow-up,
live-browser, and generation tools followed the same pattern, bringing the
server to **75 tools / 13 prompts / 5 resources**.

---

## 4. Missing CLI commands to build

Capabilities that need **new CLI work** (and will then be mirrored in MCP).

### 4.1 `ingenious testcase` — authoring depth

```
ingenious testcase edit-step   <P>/<S>/<TC> --index N --action ... --object ... --input ...
ingenious testcase remove-step <P>/<S>/<TC> --index N
ingenious testcase move-step   <P>/<S>/<TC> --from N --to M
ingenious testcase insert-step <P>/<S>/<TC> --at N --action ...
ingenious testcase copy        <P>/<S>/<TC> --to <S2>/<TC2>
ingenious testcase rename      <P>/<S>/<TC> --name <new>
ingenious testcase tag         <P>/<S>/<TC> --add @smoke --remove @wip
ingenious testcase from-template <archetype> ...   # see §6
```

> Today only `create` / `add-step` / `delete` / `validate` exist. Step-level
> editing (`edit`/`remove`/`move`/`insert`) is the single biggest authoring
> gap — an agent cannot fix a test without recreating it.

### 4.2 `ingenious object` — locator lifecycle

```
ingenious object add     <P> --page Login --name user.field --locator "id=username"
ingenious object update  <P> --page Login --name user.field --locator "css=#u"
ingenious object delete  <P> --page Login --name user.field
ingenious object import-page <P> --url https://... --headless   # auto-scrape locators
```

> `create` exists but there is no `update`/`delete`, and no way to
> **auto-generate** an object page from a live URL — a natural Playwright hook
> (see §7.4).

### 4.3 `ingenious scenario` — organization

```
ingenious scenario rename  <P>/<S> --name <new>
ingenious scenario move    <P>/<S> --to-reusable | --to-shared
ingenious scenario group   <P>/<S> --group "Checkout Flows"   # see repo grouping work
ingenious scenario tag     <P>/<S> --add @regression
```

### 4.4 `ingenious data` — full data-driven authoring

```
ingenious data row set    <P> --sheet Login --row 2 --col password --value "***"
ingenious data row delete <P> --sheet Login --row 2
ingenious data env create <P> --name QA --copy-from DEV
ingenious data generate   <P> --sheet Users --rows 50 --schema faker.json  # synthetic data
```

### 4.5 `ingenious gen` — new top-level command (AI authoring bridge)

A new command family dedicated to **generation** so the surface reads
naturally for agents:

```
ingenious gen testcase   --archetype browser-login --from-url https://... 
ingenious gen from-openapi <spec.yaml> --scenario API --tag @contract
ingenious gen from-har     <capture.har> --scenario Recorded
ingenious gen data         --sheet Users --schema faker.json --rows 100
```

### 4.6 `ingenious run` — richer execution

```
ingenious run --dry-run          # validate + print resolved plan, no execution
ingenious run --trace            # emit Playwright trace.zip per step
ingenious run --video            # record video (headed/headless)
ingenious run --grep "pattern"   # filter by test-case name regex
ingenious run --shard 1/4        # CI sharding
ingenious watch <P>/<S>/<TC>     # re-run on file change (author loop)
```

### 4.7 `ingenious doctor` — environment diagnostics

```
ingenious doctor            # JDK, Playwright browsers, drivers, project health
ingenious doctor --fix      # install missing Playwright browsers, drivers
```

> Removes the #1 friction for new users: "why won't my browser test run?"

---

## 5. New MCP tools to expose

Grouped by area, following the `ingenious_<area>_<verb>` convention. Tools
marked **(adapter)** wrap existing CLI logic; **(new)** need §4 CLI work first.

### 5.1 Authoring (test structure)
| Tool | Kind | Purpose |
|------|------|---------|
| `ingenious_testcase_edit_step` | new | Replace one step by index. |
| `ingenious_testcase_remove_step` | new | Delete step by index. |
| `ingenious_testcase_move_step` | new | Reorder a step. |
| `ingenious_testcase_insert_step` | new | Insert at index. |
| `ingenious_testcase_validate` | adapter | Static lint (unknown actions, missing objects). |
| `ingenious_testcase_copy` | new | Duplicate a test case. |
| `ingenious_testcase_tag` | new | Add/remove tags. |
| `ingenious_scenario_info` / `_delete` / `_rename` | adapter/new | Scenario lifecycle. |

### 5.2 Object Repository (currently *zero* MCP coverage)
| Tool | Kind | Purpose |
|------|------|---------|
| `ingenious_object_list` | adapter | Pages in the OR. |
| `ingenious_object_show` | adapter | Objects on a page. |
| `ingenious_object_search` | adapter | Find a locator by name/description. |
| `ingenious_object_add` / `_update` / `_delete` | new | Locator lifecycle. |
| `ingenious_object_import_page` | new | Scrape a live URL into a page (§7.4). |

### 5.3 Data & environments
| Tool | Kind | Purpose |
|------|------|---------|
| `ingenious_data_show` / `_get` / `_set` | adapter | Read/write cells. |
| `ingenious_data_import` | adapter | Import CSV/JSON. |
| `ingenious_data_generate` | new | Synthetic/faker data (§6.6). |
| `ingenious_env_list` / `_create` / `_delete` | adapter/new | Environment management. |

### 5.4 Test sets & execution
| Tool | Kind | Purpose |
|------|------|---------|
| `ingenious_testset_create` / `_add` | adapter | Build execution suites. |
| `ingenious_run_dry` | new | Resolve & validate a plan without running. |
| `ingenious_run` (extend) | adapter | Add `tags`, `rerun`, `grep`, `trace`, `video`, `shard`. |

### 5.5 Import & generation
| Tool | Kind | Purpose |
|------|------|---------|
| `ingenious_import_curl` / `_postman` / `_bruno` / `_playwright` | adapter | Surface the existing importers. |
| `ingenious_import_openapi` | new | OpenAPI → API test cases (§6.2). |
| `ingenious_import_har` | new | HAR → API/browser flow. |
| `ingenious_gen_testcase` | new | Archetype-driven generation (§6). |

### 5.6 Reporting & diagnostics
| Tool | Kind | Purpose |
|------|------|---------|
| `ingenious_report_show` / `_export` / `_compare` | adapter | Deep report access. |
| `ingenious_report_trace` | new | Return path to Playwright trace.zip for a failed step. |
| `ingenious_doctor` | new | Environment health (§4.7). |
| `ingenious_config_show` / `_drivers` | adapter | Full config visibility. |

### 5.7 Live browser authoring (the headline feature)
| Tool | Kind | Purpose |
|------|------|---------|
| `ingenious_browser_session_start` / `_do` / `_snapshot` / `_save` / `_close` | new | Interactive Playwright Agent CLI session: daemon-backed persistent browser, step recording, ref-based locators (§7.3). |
| `ingenious_browser_inspect` | new | Given a URL + intent, suggest stable ranked locators from live accessibility tree (§7.4). |
| `ingenious_object_import_page` | new | Scrape a URL's interactive elements into an OR page via CLI snapshot (§7.5). |

---

## 6. Test-case archetypes — authoring every test type

The core UX idea: agents author from **archetypes** (templates that encode
INGenious best practice per test type) rather than from a blank step list.
Expose these via `ingenious gen testcase --archetype <name>` and the MCP
`ingenious_gen_testcase` tool, and back them with a new prompt
`author_by_archetype`.

Each archetype declares: required inputs, discovery step, step skeleton, and
default assertions.

### 6.1 Browser UI flow (`browser-flow`)
- **Inputs:** start URL, intent, target locators (or auto via §7).
- **Discovery:** `action_list category=Browser`, `object_search`.
- **Skeleton:** `OpenBrowser` → `NavigateTo` → interaction steps → assertions
  (`VerifyText`, `VerifyElementPresent`) → `CloseBrowser`.
- **Defaults:** explicit waits before assertions; no `Sleep`.

### 6.2 API / contract test (`api-request`)
- **Inputs:** method, URL, headers, body, expected status/schema.
- **Discovery:** `action_list category=API`.
- **Skeleton:** `SetRequestHeader*` → `SendRequest` → `VerifyStatusCode` →
  `VerifyJsonPath` / `ValidateSchema`.
- **Bridges:** `import curl`, `import postman`, `import openapi`.

### 6.3 Database validation (`db-verify`)
- **Inputs:** connection (from config), query, expected rows/values.
- **Skeleton:** `ConnectDB` → `ExecuteQuery` → `VerifyDBValue` / `VerifyRowCount`.

### 6.4 Mobile flow (`mobile-flow`)
- **Inputs:** app/package, device capability, gestures.
- **Skeleton:** `LaunchApp` → tap/swipe/type → assertions → `CloseApp`.

### 6.5 Kafka / messaging (`kafka-pubsub`)
- **Inputs:** topic, payload, consumer group, expected message.
- **Skeleton:** `ProduceMessage` → `ConsumeMessage` → `VerifyMessagePayload`.

### 6.6 Data-driven (`data-driven`)
- Wraps any archetype above: creates the data sheet, binds
  scenario/testcase rows, parameterizes step inputs with `${column}` tokens.
- **Tools:** `data_sheet_create` → `data_column_add` → `data_row_add` →
  `data_generate` (synthetic).

### 6.7 Hybrid / end-to-end (`e2e-journey`)
- Composes reusable components across UI + API + DB (e.g. "sign up via UI,
  verify record in DB, confirm email via API").
- **Skeleton:** `CallFunction` steps referencing reusable components +
  inline verifications.

### 6.8 Visual / accessibility (`visual-check`, `a11y-check`)
- **Visual:** `CaptureScreenshot` → `CompareImage` (baseline in project).
- **A11y:** run axe-core via a Playwright hook → `VerifyNoA11yViolations`.

> Deliverable: a small `archetypes/*.yaml` registry under
> `Engine/src/main/resources/archetypes/`, loaded by a new
> `ArchetypeCatalog` class (mirrors `ActionCatalog`), shared by CLI + MCP.

---

## 7. Playwright Agent CLI integration for browser tests

> Reference: **Playwright CLI (Agent CLI)** — https://playwright.dev/agent-cli/introduction. Install with
> `npm install -g @playwright/cli`; the binary is `playwright-cli`.

This is **not** codegen. The Playwright Agent CLI is a *ref-based, daemon-backed,
shell-driven* browser designed for coding agents. The agent itself drives a live
browser one command at a time (`open`, `click <ref>`, `fill <ref> <text>`,
`snapshot`) and after **every** command the CLI prints the current page state
plus an **accessibility snapshot** whose interactive elements carry stable refs
(e.g. `e21`). The agent reasons over that snapshot, issues the next command, and
INGenious captures the sequence — turning a live exploration into a durable
INGenious test case with real Object-Repository locators.

**Why this fits INGenious better than codegen:**
- **Agent-native, not human-native.** No human clicking, no window to close, no
  emitted `.java` to re-parse. The LLM operates the browser directly.
- **Ref-based determinism.** Every action targets a snapshot ref, so translation
  to an INGenious locator is unambiguous (role/name → test-id → css).
- **Daemon = fast.** A persistent browser process means the whole authoring
  loop runs without per-command startup cost.
- **Token-efficient.** Concise CLI output keeps snapshots small enough to reason
  over inside a context window.

### 7.1 What already exists (keep, but reframe)
`ingenious import playwright <file>` (`PlaywrightRecordingImporter`) stays as
an **offline** path for teams that already have codegen scripts. The Agent CLI
work below is the new **online / interactive** path and is the primary story.

### 7.2 The bridge — a Playwright-CLI → INGenious translation layer
A new `PlaywrightCliTranslator` (sibling to `PlaywrightRecordingImporter`, reusing
its step-mapping logic) maps each Playwright CLI verb to an INGenious step + an OR
locator derived from the snapshot ref:

| Playwright CLI command | INGenious step | OR locator source |
|---|---|---|
| `open <url>` / `goto <url>` | `OpenBrowser` + `NavigateTo` | — |
| `click <ref>` / `dblclick <ref>` | `Click` / `DoubleClick` | ref → role/name → css |
| `fill <ref> <text>` / `type <text>` | `SetText` / `Type` | ref → locator |
| `check <ref>` / `uncheck <ref>` | `Check` / `Uncheck` | ref → locator |
| `select <ref> <val>` | `SelectByValue` | ref → locator |
| `hover <ref>` / `drag <a> <b>` | `Hover` / `DragTo` | ref → locator |
| `press <key>` | `PressKey` | — |
| `upload <file>` | `UploadFile` | ref → locator |
| `snapshot` / `screenshot` | assertion anchor | drives `VerifyText` / `CaptureScreenshot` suggestions |
| `state-save` / `cookie-*` / `localstorage-*` | `SetStorageState` / cookie & storage actions | — |
| `route <pattern>` / `unroute` | `MockRequest` / `FulfillRequest` | — |
| `tracing-start` / `tracing-stop` | trace artifact for §7.6 | — |

### 7.3 Interactive authoring — `ingenious browser session`
A thin wrapper around the Playwright CLI **daemon + sessions** model so one named
browser persists across many commands while INGenious records each step:
```
ingenious browser session start   --name Checkout --url https://app.example.com [--headed] [--browser firefox]
ingenious browser session do       --name Checkout --command 'click e21'   # proxied to playwright-cli, step recorded
ingenious browser session snapshot --name Checkout                          # returns the ref'd accessibility tree
ingenious browser session save     --name Checkout --scenario Checkout --testcase HappyPath
ingenious browser session close    --name Checkout
```
- `start` runs `playwright-cli -s=<name> open <url>` (headless by default) and
  returns the first snapshot.
- `do` proxies any Playwright CLI verb via `-s=<name>`, runs the result through
  `PlaywrightCliTranslator`, appends the derived step to a buffer, returns
  `{step, newRef, snapshot}`.
- `save` flushes the buffer to a real test case and auto-registers every
  referenced locator into the Object Repository.
- Mirrored as MCP tools `ingenious_browser_session_start/_do/_snapshot/_save/_close`.

### 7.4 Live locator suggestion — `ingenious browser inspect`
Uses the Agent CLI (`open` then `snapshot`) and matches the intent against the
ref'd accessibility tree, returning ranked stable locators (role/name → test-id
→ css) with confidence scores and the source ref. Powers a smarter `suggest_locator`
prompt.

### 7.5 Auto-scrape an Object Repository page — `object import-page`
Runs one `open` + `snapshot`, walks the ref'd accessibility tree, and writes every
interactive element as a named OR locator. Feeds §4.2 and archetype §6.1.

### 7.6 Trace & video for debugging
On a normal run, tracing/video come from the Playwright-**Java** engine;
`ingenious_report_trace` returns the engine `trace.zip` path. During authoring,
the Agent CLI's own `tracing-start/stop` and `video-start/stop` are exposed via
`browser session do`.

### 7.7 Provisioning — folded into `ingenious doctor`
```
ingenious doctor            # checks for @playwright/cli, browsers, drivers
ingenious doctor --fix      # npm i -g @playwright/cli; playwright-cli install --skills
```

> **Node dependency, stated honestly.** The Agent CLI is an npm package
> (`@playwright/cli`) requiring Node — unlike the in-process Playwright-Java
> engine INGenious runs on. Treat it as an **optional authoring-time dependency**:
> `doctor` detects it, `browser session` degrades gracefully when absent, and
> test execution never needs it. The offline `import playwright` path (§7.1) is
> the zero-Node fallback.

### 7.8 Authoring loop (the modern UX)
```
session start (open+snapshot) → agent reads refs → session do 'click e21' / 'fill e5 "text"'
   → step + locator recorded per command → session save (test case + OR locators)
   → run --trace → agent reviews trace → edit-step to harden → commit
```

---

## 8. Modern UX layer — prompts, resources, guided flows

> **Status: ✅ implemented.** All prompts and resources below ship in
> `MCPPrompts.java` / `MCPResources.java` (13 prompts, 5 resources).

### 8.1 New prompts
| Prompt | Purpose |
|--------|---------|
| `author_by_archetype` | Pick an archetype (§6), gather inputs, discover actions/objects, generate + verify. |
| `record_browser_test` | Drive the §7.3 Playwright Agent CLI session loop end-to-end: start session → agent explores with ref-based commands → save to test case. |
| `harden_test` | Replace fragile locators/sleeps, add explicit waits & assertions (uses `inspect`). |
| `build_data_driven_suite` | Turn one test into a parameterized suite (§6.6). |
| `triage_run` | From a run id: pull failures → traces → suggest fixes → offer `edit-step`. |
| `bootstrap_project` | New project → sample archetype per capability → first green run. |
| `migrate_from_playwright` / `migrate_from_postman` | Guided bulk import. |

### 8.2 New resources
| URI | Purpose |
|-----|---------|
| `ingenious://catalog/archetypes` | The archetype registry (§6). |
| `ingenious://catalog/objects/<page>` | OR page locators, for grounding. |
| `ingenious://docs/best-practices` | Locator strategy, wait strategy, assertion patterns. |
| `ingenious://project/<name>/health` | Live doctor output. |

### 8.3 Conversational quality-of-life
> **Status: ✅ implemented** (see Phase 5).
- **Dry-run:** the main write tools (`testcase_create`, `gen_testcase`,
  `testset_add`, `object_add`, `data_set`) accept `dryRun:true` → return the
  planned outcome without persisting. Lets agents "show before commit."
- **Idempotent creates:** `testcase_create` / `gen_testcase` accept
  `ifExists=error|skip|overwrite`; `skip` returns `{created:false, existing:true}`.
- **Rich errors:** not-found errors for scenario / test case / object page /
  data sheet / archetype return `{code, message, data.suggestions[]}` with the
  nearest matches (Levenshtein + substring ranking), and the message carries a
  "Did you mean: …?" hint.

---

## 9. Cross-cutting capabilities

| Capability | Why it matters | Where |
|------------|----------------|-------|
| **Validation service** | Catch bad actions/locators/data refs before run. | `testcase validate` + `run --dry-run` |
| **Tagging & grouping** | Scale suites; ties into the existing `.project` meta/tag system and scenario-grouping work. | `scenario/testcase tag`, `scenario group` |
| **Environments** | QA/DEV/PROD data + config switching. | `data env *`, `config` per-env |
| **CI ergonomics** | Sharding, JUnit/Allure export, exit codes. | `run --shard`, `report export --format junit` |
| **Secrets hygiene** | Never echo passwords in `data show`; mask by convention. | data tools |
| **Telemetry (opt-in)** | Which archetypes/tools agents use, to prioritize. | MCP server |

---

## 10. Phased roadmap

### Phase 1 — Parity (adapters only, no new engine code) — ✅ **DONE**
Surfaced existing CLI logic as MCP tools: `scenario_info/delete`,
`testcase_validate`, `testset_create/add`, `object_list/show/search`,
`data_show/get/set`, `env_list/create/delete`, `report_show/compare`,
`config_show`, `import_curl/postman/bruno/playwright`. Verified end-to-end
against CLIDemo. Follow-ups still open: `data_import`, `report_export`,
`config_drivers`, and `run` `tags`/`rerun` extensions.

### Phase 2 — Authoring depth — ✅ **DONE**
Step-level editing (`testcase_edit_step`/`insert_step`/`remove_step`/`move_step`),
`object_add`/`update`/`delete`, `data_row_delete`, and `run_dry` are implemented
and verified against CLIDemo.

### Phase 3 — Archetypes & generation — ✅ **DONE**
`ArchetypeCatalog` (7 templates across Browser/API/hybrid) + `gen_list`/`gen_testcase`,
`gen_from_openapi` (YAML/JSON via Jackson), `gen_from_har`, and `data_generate`
(built-in synthetic values — no external faker dependency). Verified against
CLIDemo (generated test cases pass `testcase_validate`). Follow-up: richer
archetype library and OpenAPI request-body/param modelling.

### Phase 4 — Playwright live authoring — ✅ **DONE**
`browser_session_start`/`do`/`snapshot`/`save`/`close`, `browser_inspect`,
`object_import_page`, `doctor`, and `run --rerun` are implemented, backed by
`PlaywrightCliTranslator`. They drive the real `@playwright/cli` (or
`npx @playwright/cli`) and degrade gracefully when it is not installed.
`run --trace/--video` and `report_trace` remain follow-ups.

### Phase 5 — Polish & scale — ✅ **mostly done**
Done: **dry-run** on the main write tools (`testcase_create`, `gen_testcase`,
`testset_add`, `object_add`, `data_set`); **idempotent creates**
(`ifExists=error|skip|overwrite`); **rich errors** with `data.suggestions[]` +
"did you mean" hints on not-found lookups; JUnit/CSV/JSON export (`report_export`).
Still open: dry-run on *every* write tool, opt-in telemetry, CI sharding
(`run --shard`), and `run --trace/--video` + `report_trace` (need engine support).

---

## 11. Architecture & implementation notes

- **Reuse, don't fork.** Follow `ActionCatalog`'s pattern: put shared logic in
  a plain class under `com.ing.engine.*`, then call it from *both* the picocli
  command and the MCP tool. `MCPTools` should contain adapters, not business
  logic.
- **Registry classes.** Add `ArchetypeCatalog` (loads
  `resources/archetypes/*.yaml`) and reuse the existing Object Repository
  reader for `object_*` tools.
- **Playwright without Node.** Invoke `com.microsoft.playwright.CLI` in-process
  for `codegen`/`install`/`trace` so there is no Node/`npx` prerequisite. Keep
  a subprocess fallback.
- **Stdout discipline.** Every new tool must respect the JSON-RPC stdout rule
  (see manual §10.3). Any Playwright/subprocess chatter goes to stderr.
- **Formats.** New writers emit the same YAML/CSV the IDE reads — verify with a
  round-trip (`create` → open in IDE → `show`).
- **Testing.** Extend the CRUD smoke test (manual §4.3) with an authoring
  smoke (`gen testcase` → `validate` → `run --dry-run`) that asserts `CLEAN`.
- **Build gotcha.** Any change touching `@Action` object types or
  `ingenious-api` constants requires
  `cd ingenious-api && mvn -q clean install -DskipTests` **then**
  `mvn -pl Engine,IDE -am clean install -DskipTests` (inlined constants).
- **Docs.** Keep `MCP-USER-MANUAL.md` tool/prompt/resource counts in sync as
  each phase lands.

---

*Companion to [`MCP-USER-MANUAL.md`](./MCP-USER-MANUAL.md). Server module:
`Engine/src/main/java/com/ing/engine/mcp/`. CLI commands:
`Engine/src/main/java/com/ing/engine/cli/commands/`.*
