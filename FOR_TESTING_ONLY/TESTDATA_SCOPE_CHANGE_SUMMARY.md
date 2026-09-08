# Scoped Test Data (`[Project]` / `[Shared]`) — Change Summary & Test Impact

Self-contained record of the Test Data scoping change (commit *"Refactor Test Data Handling:
Introduce TestDataToken for Scoped References"*): **why**, **what changed**, **which features
are impacted (by archetype / command category)**, **what to test**, and a **step-by-step
testing guide for people with no prior context** (§5).

Test Data is the data-referencing mechanism for *every* action, so the blast radius is
wide even though most changes are additive.

---

## 0. Why — what was broken

The Shared Test Data feature added a `[Shared]` / `[Project]` scope tag to Test Data
references. The main step-resolution path (`DataProcessor`) understood it, but **~15 other
places parse a Test Data reference with their own hand-rolled regex / `split(":")`**, and
those did not. Two failure modes:

| Failure mode | Effect |
|---|---|
| Iterates **only the project's** sheet list and rebuilds the literal `{sheet:col}` token | `{[Shared] …}` (and `{[Project] …}`) tokens sent **verbatim** in webservice/MQ payloads, SQL, file templates, headers, DB connection config |
| `input.split(":")` with no brace/tag strip | sheet name becomes `"{[Shared] Sheet"` → lookup fails → empty value, or a Param-loop ends one iteration early |

Every fix routes through **one** new parser, `TestDataToken`, so the grammar can't drift
again.

---

## 1. The contract (final, settled)

| Where the reference sits | Form | Braces |
|---|---|---|
| **Whole-input** — the entire Input / Condition / reference *is* the ref (normal steps, write-back actions, param-loop columns) | `Sheet:Column`, `[Project] Sheet:Column`, `[Shared] Sheet:Column` | **optional** — bare is canonical |
| **Embedded** — the ref sits inside a larger string (webservice & MQ payloads / endpoints / headers, SQL text, file templates, browser-context values, **DB connection config**, String Operations fragments) | `{Sheet:Column}`, `{[Project] …}`, `{[Shared] …}` | **required** — `{…}` is the delimiter |

- **Untagged ≡ `[Project]`** → the project's own Test Data. `[Shared]` → the app-root
  `Shared/SharedTestData` store (resolved against `sharedRunEnv()`, independent of the
  project `runEnv()`).
- A config / payload value may **mix** literal + `%runtimeVar%` + `{testdata}` in one string.
- **No migration.** Existing `Sheet:Column` steps are never rewritten; bare and
  `[Project]/[Shared]` bare behave exactly as before.
- **One behaviour change:** in the embedded-substitution sites, a token whose sheet/column
  does not resolve is now left **literal** instead of raising `DataNotFoundException`
  mid-build (JSON-safe; consistent with String Operations).

---

## 2. Code changes

### New

| File | Purpose |
|---|---|
| `Engine/.../execution/data/TestDataToken.java` | The single parser/resolver: `parse` (whole-input, braces optional), `resolveEmbeddedTokens` (embedded, braces required), `scopeTag`, `isReference`, `unwrapBraces`. Grammar mirrors `DataProcessor.SCOPED_DATASHEET_PATTERN` and `TestStep.isScopedTestDataRef`. |
| `Engine/.../execution/data/TestDataTokenTest.java` | 12 unit tests (bare/braced, `[Project]`/`[Shared]`, whitespace, first-colon split, JSON-payload safety, malformed → null). |

### Modified — engine (behaviour)

| # | File | Change |
|---|---|---|
| 1 | `core/CommandControl.java` | `getDataSheetValue` / `parseScopedDataSheetRef` delegate to `TestDataToken`; scoped branch → `userData.getData(taggedSheet, col)`, legacy project-sheet scan kept for untagged. |
| 2 | `execution/run/TestStepRunner.java` | `executeStringOperation` pre-resolution switched to non-logging resolvers (bad ref reported **once** by the action). |
| 3 | `execution/run/TestCaseRunner.java` | `checkIfLastData` (Param-loop / dynamic *Start Param* end-of-data detection): `getInput().split(":")` → `TestDataToken.parse` — fixes braced / scoped refs feeding a broken sheet name. |
| 4 | `commands/webservice/GeneralWebservice.java` | `handleDataSheetVariables` → `TestDataToken.resolveEmbeddedTokens` (REST/SOAP body, endpoint URL, URL params). |
| 5 | `commands/webservice/Webservice.java` | `addHeader` inline loop → `resolveEmbeddedTokens` (HTTP headers). |
| 6 | `commands/browser/RequestFulfill.java` | `handleDataSheetVariables` → `resolveEmbeddedTokens` (Playwright route fulfil body/resource). |
| 7 | `commands/browser/Switch.java` | `handleDataSheet` → `resolveEmbeddedTokens` (browser-context option values). |
| 8 | `commands/database/General.java` | `handleDataSheetVariables` → `resolveEmbeddedTokens` (SQL text); `resolveVars` datasheet branch → `TestDataToken.parse`; `resolveAllVariables` (DB **connection config** values) now resolves `{[Project]/[Shared] …}` + doc. |
| 9 | `commands/file/FileOperations.java` | `handleDataSheetVariables` → `resolveEmbeddedTokens` (file-template content). |
| 10 | `commands/queue/QueueOperations.java` | `handleDataSheetVariables` → `resolveEmbeddedTokens` (JMS/MQ message body). |
| 11 | `commands/general/GeneralOperations.java` | `storeVariableInDataSheet` + `storeInGlobalDataSheet` write path: `Input/Condition.split(":")` → `TestDataToken.parse`; Global Data keeps the `[Scope]` tag (inserts `#` after it). |
| 12 | `commands/database/Database.java` | `storeDBValueinDataSheet` write path → `TestDataToken.parse`. |
| 13 | `mcp/ConventionCatalog.java` | `DATA_REF` / `PAYLOAD_TOKEN` patterns accept an optional `[Project] `/`[Shared] ` prefix. |
| 14 | `mcp/MCPTools.java` | `dataRefExists` routes a `[Shared]`-tagged sheet to `project.getSharedTestData()`; validation call sites use `TestDataToken.parse`. |
| 15 | `perf/TestCaseFlattener.java` | `resolveInput` dynamic-input warning uses `TestDataToken.isReference`. |
| 16 | `cli/commands/DataCommand.java` | `data get` / `data set` accept `[[Project]|[Shared]] Sheet:Column:Row`; `resolveSheetCsv` reads `Shared/SharedTestData/` for `[Shared]`. |

### Modified — datalib / IDE

| # | File | Change |
|---|---|---|
| 17 | `Datalib/.../component/TestStep.java` | `isScopedTestDataRef` regex `\s+` → `\s*` (grammar parity with the engine — `[Project]Sheet:Col` with no space now classifies the same); doc on `isTestDataStep`. |
| 18 | `IDE/.../testcase/InlinePropertyDialog.java` | Dropping a Test Data column onto an inline object-property value now inserts `[Project] Sheet:Column` (`[Shared] …` for a shared sheet) — matches `TestCaseTableDnD`, which already did this. |
| 19 | `IDE/.../settings/DriverSettings.java` | `isDatasheetOrVariable` recognises a bare `[Project]`/`[Shared]`-tagged ref (so it isn't encrypted as a literal password). |

### Not changed — already correct (reference implementations)

`DataProcessor` (`isInputPatternDataSheet`, `SCOPED_DATASHEET_PATTERN`, `resolve`) — **the
whole-input resolution path for every normal action**; `DataAccess` / `DataAccessInternal`
(the resolution engine); `StringOperations.getVarValue` (braces already required);
`StructuredData.getInputValue` (inherits the fix via `getDatasheet`);
`InputRenderer` (IDE, uses the `TestStep` helpers).

### Docs

This file; javadoc on `TestDataToken`, `TestStep.isTestDataStep`,
`General.resolveAllVariables`; `Resources/Projects/RegressionTests/README.md`.

### Regression project

`Resources/Projects/RegressionTests/` — scenario `TestDataScope`
(`AllReferenceForms`, `WriteBack`, `RenderTemplate`, `DbConnectionConfig`) +
`run-regression.sh` + `README.md`; `Resources/Shared/SharedTestData/RegShared.csv`.

### Known gap — NOT changed (same pattern, not part of the sweep)

`commands/webservice/Webservice.java` has **5 more write-to-datasheet actions** that still
use `strObj.split(":", 2)` for the destination and were not migrated:
`storeJSONelementInDataSheet`, `storeXMLelementInDataSheet`, `storeResponseBodyInDataSheet`,
`storeJsonElementCountInDataSheet`, `storeResponseHeaderInDataSheet`. Bare `Sheet:Column`
and bare `[Project]/[Shared] Sheet:Column` work (like the migrated write actions); a
**braced** `{…}` destination fails (caught → `Status.DEBUG`). Low impact (destination refs
are conventionally bare), but flagged for a follow-up.

---

## 3. Impacted features — by Archetype

The shipped archetypes (`ArchetypeCatalog`) and where a Test Data reference can feed in:

| Archetype | Category | Test-data touchpoint | Sweep-changed path |
|---|---|---|---|
| `browser-login` | Browser | `Fill` / `Click` inputs (`@${username}` … parameterised, may be `{Sheet:Col}` or whole-input `Sheet:Col`) | whole-input → `DataProcessor` (**unchanged**); values built via String Operations → `getDataSheetValue` (changed) |
| `form-submit` | Browser | element inputs from Test Data | same |
| `search-flow` | Browser | search query from Test Data | same |
| `api-get-assert` | API | `setEndPoint` URL, `assertResponseCode` expected | **`GeneralWebservice` embedded tokens** |
| `api-post-assert` | API | `setEndPoint` URL, `addHeader`, `postRestRequest` **body**, `assertResponseCode` | **`GeneralWebservice` + `Webservice.addHeader` embedded tokens** |
| `api-json-assert` | API | endpoint, `assertJSONelementEquals` expected path/value | **`GeneralWebservice` embedded tokens** |
| `ui-then-api-verify` | General | UI inputs + webservice endpoint/body/assert | Browser (unchanged) + **webservice embedded tokens** |

Broader command taxonomy (`ActionCatalog`) — every category consumes Test Data:

| Feature category | Where Test Data is referenced | Changed by the sweep? |
|---|---|---|
| **Browser / Web** | whole-input Input refs; `Switch` context-config values; `RequestFulfill` body/resource | `Switch`, `RequestFulfill` (embedded, fixed). Whole-input: unchanged. |
| **API / Webservice** | request body, endpoint, URL params, headers; `store*InDataSheet` destinations | `GeneralWebservice`, `Webservice.addHeader` (**HIGH**, fixed). 5 `store*InDataSheet` destinations: **gap** (bare works). |
| **Database** | connection-config values, SQL query `{…}`, `storeDBValueinDataSheet` | `General.handleDataSheetVariables` + `resolveVars` + `resolveAllVariables` (**HIGH**, fixed); `Database.storeDBValueinDataSheet` (fixed). |
| **Kafka / Queue** | message body `{…}` | `QueueOperations` (**HIGH**, fixed). |
| **File** | `populateData` template `{…}` | `FileOperations` (**HIGH**, fixed). |
| **String Operations** | `Concat`/`Trim`/`Substring`/`Replace`/`ToLower`/`ToUpper`/`Split`/`GetOccurence`/`GetLength` fragments `{Sheet:Col}` | `getDataSheetValue` via `getDatasheet` (fixed — now tag-aware). |
| **General** | `AddVar`, `assertVariable*`, `print`, `storeVariableInDataSheet`, `storeInGlobalDataSheet`, Param loops | `storeVariableInDataSheet` / `storeInGlobalDataSheet` parse (fixed); `checkIfLastData` param-loop (fixed). Whole-input `print` / `assert*`: unchanged. |
| **Structured Data** | `getInputValue` (`sheet:col` in JSONPath/XPath assertions) | inherits `getDatasheet` fix. |
| **Mobile / SAP / Image / Synthetic Data** | whole-input Input refs only | via `DataProcessor` — **not touched by the sweep**. |
| **MCP tooling** | `testcase_validate` E4 data-ref check; auto-parameterisation | `ConventionCatalog`, `MCPTools.dataRefExists` (fixed). |
| **CLI `data`** | `ingenious data get` / `set` | `DataCommand` (fixed — `[Shared]` support). |
| **Perf / k6 flattener** | dynamic-input warning | `TestCaseFlattener` (regex widened). |
| **IDE test design** | Input-cell validation / colouring; drag-drop insert; driver-settings password detection | `TestStep`, `InlinePropertyDialog`, `DriverSettings` (fixed). |

---

## 4. Critical features to test

### P0 — must pass (broad blast radius / changed by the sweep)

1. **Whole-input datasheet refs on any action** — `Set` / `Fill` / `Type` / `assertText` /
   `Execute` sub-iterations with Input `Sheet:Column` **and** `[Project] Sheet:Column` /
   `[Shared] Sheet:Column` (bare). Must be **byte-identical** to before for bare/`[Scope]`;
   `[Shared]` must now resolve against the Shared store.
2. **Webservice REST/SOAP** — request body, endpoint URL, URL params and headers containing
   embedded `{Sheet:Col}`, `{[Project] …}`, `{[Shared] …}` across GET/POST/PUT/PATCH/DELETE
   and SOAP. Include a **mixed** value (literal + `%var%` + `{…}`).
3. **Database** — (a) SQL query text with `{…}` tokens (SELECT + DML); (b) **connection
   config** (`connectionString` / `user` / `password` / `driver` / `timeout`) with `{…}`
   incl. mixed literal + `%var%` + `{[Project]/[Shared] …}`; (c) `storeDBValueinDataSheet`
   with bare + `[Project]` + `[Shared]`.
4. **String Operations** — all 9 actions with `{Sheet:Col}` / `{[Project] …}` /
   `{[Shared] …}` parts; the "reported once" behaviour for a bad ref.
5. **Param loops / dynamic Start-Param** — a data-driven loop whose Input is a `{…}` or
   `[Scope]`-tagged datasheet ref: iterates the right number of times and stops (no
   premature exit / infinite loop). This is `checkIfLastData`.
6. **Write-back actions** — `storeVariableInDataSheet` / `storeInGlobalDataSheet` with bare
   (canonical), bare `[Project]`, bare `[Shared]`, and braced `{…}`; read back and verify.

### P1 — should pass

7. **File Operations** `populateData` — template with embedded tokens → rendered file.
8. **Queue / MQ** — message body with embedded tokens.
9. **Browser `Switch`** — context option values with `{…}`.
10. **RequestFulfill** — fulfil body / resource with `{…}`.
11. **Structured Data** — `getInputValue` path (JSONPath / XPath assertions referencing
    `sheet:col`).
12. **Reusable-component row Scope** — `[Project]` / `[Shared]` / empty `Scope` column
    filtering still works; rename / column-rename (`withScopeTag`) preserves the step's
    existing tag and doesn't cross scopes.
13. **Environment independence** — `[Shared]` resolves against `sharedRunEnv()` and
    `[Project]` against `runEnv()` when the two are set to different environments.
14. **Global Data** — `#gid`, `[Shared] #gid`, `[Project] #gid` read and write.
15. **The 5 un-migrated Webservice `store*InDataSheet`** — confirm bare + `[Project]`/
    `[Shared]` bare destinations still work (braced not expected).

### P2 — regression safety / tooling

16. **IDE test design** — Input-cell validation + colour for every form; drag Test Data
    column → step (already emits `[Project] `); drop → inline object property (now emits
    `[Project] `); driver-settings password field not encrypted when it's a `[Scope]` ref.
17. **MCP** — `ingenious_testcase_validate` E4 data-reference check for scoped refs;
    auto-parameterisation does not double-wrap an already-`{…}` value.
18. **CLI** — `ingenious data get "[Shared] Sheet:Column:Row"` reads
    `Shared/SharedTestData/`.
19. **Migration / back-compat** — a legacy project (no `Scope` column, only bare
    `Sheet:Col` steps) loads and runs unchanged; `Scope` column auto-populates.
20. **Payload safety** — a JSON body `{"a":"b","c":{"d":"e"}}` and a GraphQL-ish
    `{ field: value }` are **not** mangled by `resolveEmbeddedTokens`.
21. **Missing-datasheet behaviour** — an unresolved embedded token is left **literal** (no
    `DataNotFoundException`) in the 7 substitution sites; a missing whole-input ref still
    behaves as before.

### Automated coverage that already exists

- `TestDataTokenTest` (unit, 12) — the parser/resolver incl. JSON-payload safety.
- `SharedTestDataResolutionIntegrationTest` — real-file `[Shared]` vs `[Project]` vs
  untagged resolution, env independence.
- `CommandControlScopedDataSheetRefTest`, `StringOperationsActionTest`, `StringOpsStaticTest`,
  `DataProcessor*Test`, `ConventionCatalog*` / `ArgSpec*`.
- `Resources/Projects/RegressionTests` (`run-regression.sh`) — end-to-end headless: String
  Operations, write-back, file template, DB connection config (mixed value).

---

## 5. Testing guide — no prior context needed

You do **not** need to understand the code. You are checking one thing: **a test step can
point at Test Data three ways, and all three still work.**

### 5.1 The three forms

| Form | Looks like | Means |
|---|---|---|
| Untagged (the old, normal way) | `Basic:URL` | column `URL` in project sheet `Basic` |
| `[Project]` (new, explicit — same as untagged) | `[Project] Basic:URL` | same as above |
| `[Shared]` (new — the shared library) | `[Shared] Common:Token` | column `Token` in shared sheet `Common` |

Two places a reference can appear:

- **Whole Input** — the step's Input box contains *only* the reference. Write it **without
  braces**: `Basic:URL` or `[Project] Basic:URL`.
- **Inside a bigger string** — e.g. a REST body, a SQL query, a DB connection string, a
  file template, a String Operations formula. Wrap it in **braces**:
  `{"url":"{[Shared] Common:BaseUrl}"}`.

Rule of thumb: *box contains only the reference → no braces. Reference is buried in other
text → braces.*

### 5.2 Fastest check — run the ready-made suite (5 min)

A headless project already exists that exercises every path.

1. Build the app (once): from the repo root, `mvn -q -o install -DskipTests -pl Engine,Datalib -am`,
   then copy the fresh jars into your run dir:
   `cp Engine/target/ingenious-engine-4.0.0.jar Datalib/target/ingenious-datalib-4.0.0.jar
   "TestData - Csv/target/ingenious-testdata-csv-4.0.0.jar" Dist/release/lib/`
   *(or just use an installer build that already contains the change).*
2. Copy the project + shared sheet into your run dir:
   `cp -r Resources/Projects/RegressionTests Dist/release/Projects/` and
   `cp Resources/Shared/SharedTestData/RegShared.csv Dist/release/Shared/SharedTestData/`
3. Run it:
   ```bash
   cd Dist/release
   JAVA='/c/Program Files/Java/jdk-17/bin/java' bash Projects/RegressionTests/run-regression.sh
   ```
4. **Expect the last line to be `REGRESSION: PASS` (exit 0).** If it says `FAIL`, the
   line above it names the broken area; `Projects/RegressionTests/README.md` has a
   "Regression signatures" table that maps each failure to a code area.

### 5.3 Manual checks in the IDE (if you want to click through it)

**Setup** (once): open any project.

- In a project Test Data sheet — say `Basic` — pick the row whose *Scenario* and *Flow*
  match the test case you will run. Add columns: `URL` = `PROJ_URL`, `Name` = `alice`.
- Open **Shared Test Data** (Test Data panel → Shared) and create a sheet `Common` with a
  row for the same Scenario/Flow: column `Token` = `SH_TOKEN`.

Then add these steps to a test case and run it. **Pass = the value shown in the report is
the resolved value, not the literal `{...}` text, and the step is DONE/PASS.**

| # | Area | Object | Action | Input | Condition | Expect in report |
|---|---|---|---|---|---|---|
| 1 | Basic value ref — untagged | General | `print` | `Basic:URL` | — | prints `PROJ_URL` |
| 2 | Basic value ref — `[Project]` | General | `print` | `[Project] Basic:URL` | — | prints `PROJ_URL` |
| 3 | Basic value ref — `[Shared]` | General | `print` | `[Shared] Common:Token` | — | prints `SH_TOKEN` |
| 4 | String Operations — braces required | String Operations | `Concat` | `"url=",{[Project] Basic:URL}` | `%r%` | `%r%` = `url=PROJ_URL` |
| 5 | String Operations — shared | String Operations | `ToUpper` | `{[Shared] Common:Token}` | `%u%` | `%u%` = `SH_TOKEN` |
| 6 | Write to a sheet — no braces | General | `storeVariableInDataSheet` | `[Project] Basic:Name` | `%r%` (from #4) | "stored into the data sheet"; read back with `assertVariableFromDataSheet` `[Project] Basic:Name` / `%r%` → matched |
| 7 | Webservice body — braces | Webservice | `postRestRequest` | `{"user":"{[Project] Basic:Name}","tok":"{[Shared] Common:Token}"}` | (endpoint set earlier) | the logged **Payload** line shows `{"user":"alice","tok":"SH_TOKEN"}` — no `{[...]}` left |
| 8 | SQL text — braces | Database | `executeSelectQuery` | `@SELECT '{[Shared] Common:Token}' AS t` | (DB connected) | the logged **Query** line shows `SELECT 'SH_TOKEN' AS t` |
| 9 | DB connection config — braces + mixable | *(edit `Settings/Databases/<alias>.properties`)* | — | `connectionString=jdbc:x://{[Project] Basic:URL}/%dbName%` | — | on connect, the resolved string in the log/error shows `jdbc:x://PROJ_URL/<value of %dbName%>` |
| 10 | File template — braces | File | `populateData` | `@line: {[Project] Basic:URL} / {[Shared] Common:Token}` | (`%fileName%`,`%fileLocation%` set) | the written file contains `line: PROJ_URL / SH_TOKEN` |
| 11 | Param loop end detection | General | `print` inside a `Start Param` … `End Param` block, Input `{[Project] LoopSheet:Col}` driven by a 3-row sheet | — | prints exactly 3 times then stops (no early stop, no infinite loop) |

### 5.4 What "broken" looks like

- A step prints/sends the literal text `{[Shared] Common:Token}` (or `[Shared] Common:Token`)
  instead of the value.
- A `[Shared]` reference resolves to **empty** while the same sheet/column in the project
  works.
- A Param loop stops after **1** iteration instead of 3.
- A JSON body like `{"a":"b"}` comes out mangled or the step errors on it.

### 5.5 Sign-off checklist

- [ ] `run-regression.sh` prints `REGRESSION: PASS`.
- [ ] Untagged `Sheet:Col` behaves exactly as before this change (spot-check an existing suite).
- [ ] `[Project] Sheet:Col` resolves to the project value (same as untagged).
- [ ] `[Shared] Sheet:Col` resolves to the shared-sheet value.
- [ ] Braced `{...}` refs resolve inside: REST body, SQL text, DB connection string, file
      template, String Operations formula.
- [ ] A mixed value `literal + %var% + {testdata}` in DB config resolves all three parts.
- [ ] `storeVariableInDataSheet` writes with a bare `Sheet:Col` / `[Project] Sheet:Col`
      destination.
- [ ] Param-loop / Start-Param blocks driven by a scoped ref iterate the right number of times.
- [ ] A JSON/GraphQL-style `{ ... }` payload with no real data reference is left untouched.
