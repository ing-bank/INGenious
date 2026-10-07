# SAPDemo — sample SAP project

A minimal INGenious project that exercises SAP connection/session lifecycle end-to-end.
Built to run under **SAP fake mode** (see `SAP-Enhancement/SAP-Fake-Mode.md` at the repo
root) — no SAP GUI, no COM, no real SAP system required.

## Layout

```
SAPDemo/
  Settings/SAP/DEMO.properties       one SAP connection alias ("DEMO")
  Settings/SAP/_config.properties    project default connection + model
  TestPlan/SAP/SAP_Smoke.yaml        the test case (run directly)
  TestLab/R1/Smoke.yaml              the same test case, wrapped as a test set
  SampleRecordings/                  sample SAP GUI Scripting Tracker recordings,
                                      for testing the "Import SAP Recording" feature
```

## Running it

Build INGenious in fake mode first (from the repo root):

```
mvn clean install -Dsap.fakeMode=true
```

Then, from `Dist/release`, put this project where the CLI can find it (either an absolute
path, or under a `Projects/` folder next to `ingenious.bat`), and run:

```
ingenious run SAPDemo/SAP/SAP_Smoke      # the test case directly
ingenious run SAPDemo/R1/Smoke           # the same thing, as a test set
```

Look for `⚠ SAP FAKE MODE ACTIVE` in the run's `console.txt` (under
`Results/TestDesign/SAP/SAP_Smoke/<timestamp>/` or
`Results/TestExecution/R1/Smoke/<timestamp>/`) to confirm it ran against the fake.

## What `SAP_Smoke` does

1. `sapInitConnection` `#DEMO` — opens the `DEMO` connection (its primary session).
2. `sapOpenSession` `@stock` — opens a second, concurrent session on the same connection.
3. `sapSwitchSession` (blank) — switches back to the primary session.
4. `sapSwitchSession` `@stock` — switches to the `stock` session again.
5. `sapCloseSession` `@stock` — closes the `stock` session only.
6. `sapCloseConnection` `#DEMO` — releases the connection.

All six steps route through `SapSessionManager`/`FakeSap` and pass cleanly under fake mode.

## Scope — what fake mode does and doesn't cover

Only the **connection/session lifecycle** actions in `SAPActions.java`
(`sapInitConnection`, `sapSwitchConnection`, `sapCloseConnection`, `sapCloseAllConnection`,
`sapOpenSession`, `sapSwitchSession`, `sapCloseSession`) go through the new
`SapSessionManager` seam that `FakeSap` implements — that's why this demo only uses those.

**`sapExecuteTransaction`, `sapEndTransaction`, `sapRefreshSession`, and every
element/window-level SAP action (`sapFill`, `sapClick`, `sapSelect...`, grid/tree actions,
etc.) still call the legacy raw JACOB `Dispatch`/`ActiveXComponent` directly** — they have
not been migrated onto the `SapGuiSession`/`SapElement` interfaces yet. Under fake mode,
`FakeSap.Session.raw()` returns `null`, so any of those steps will fail with a
null-Dispatch error. Don't add them to this demo (or any fake-mode test) until that
migration happens — see `SAP-Enhancement/SAP-Connections-Redesign.md`, "Effort" note under
"Testing without a SAP environment".

## `SampleRecordings/` — testing the SAP recording importer

Hand-authored recordings of the same VA01 (Create Sales Order) scenario, cross-checked
in a second session with VA03 (Display Sales Order), in the four formats INGenious's SAP
import parses:

- `VA01_CreateSalesOrder.ps1` — importable **today** through the running app: Tools ->
  Import SAP Recording -> PowerShell (.ps1). Verified: parses into 12 objects / 16 actions
  (transaction, text sets, a button press, sendVKey, setFocus, a checkbox `selected`, and
  the multi-session `OpenSession`/`SwitchSession` pair from switching to the VA03 session
  and back). **One caveat found while verifying**: the tab-selection step
  (`Invoke-Method ... -methodName "select"` with no parameters) does not produce an action —
  `SapParserLangPowerShell.parseInvokeMethod`'s `"select"` case only calls `addAction` when
  `-methodParameter` is non-empty, so a parameterless tab select (the normal real-world
  syntax) is silently dropped. Everything else in the file imports correctly.
- `VA01_CreateSalesOrder.vbs` — the classic, authentic SAP GUI Scripting Tracker output
  format (same boilerplate `If Not IsObject(application)...` header a real recording has).
  Verified: parses into 18 objects / 25 actions, including the tab-selection steps (no gap
  here — `SapParserLangVBScript`'s equivalent case is unconditional). Importable **today**
  through the running app: Tools -> Import SAP Recording -> VBScript (.vbs, .vba).
- `VA01_CreateSalesOrder.jsh` — importable **today** via Tools -> Import SAP Recording ->
  Java (.java, .jsh), using the JACOB `ActiveXComponent`/`.invoke(...)`/`.setProperty(...)`
  idiom from `SapParserLangJava`'s own Javadoc example. Verified: parses into 11 objects /
  16 actions — same shape as the `.ps1` sample (including the `OpenSession`/`SwitchSession`
  pair), because it deliberately **omits the tab-selection step**: `SapParserLangJava`'s
  `"select"` case has the identical gap as PowerShell's (only fires with a non-empty
  parameter), so a parameterless `.invoke("select")` would silently import as nothing —
  no point including a step that doesn't do anything.
- `VA01_CreateSalesOrder.js` — the same scenario translated to JavaScript, the Scripting
  Tracker's other native output format (required parens/semicolons instead of VBScript's
  optional ones). Shares `SapParserLangVBScript`'s unconditional tab-selection handling (both
  extend `SapLanguageParser` without overriding `parse()`), so it parses identically to the
  `.vbs` sample: 18 objects / 25 actions, tab-selection steps included. Importable **today**
  through the running app: Tools -> Import SAP Recording -> JavaScript (.js).
- `VA01_MultiWindowMultiSession.ps1` — a denser stress test: **two SAP GUI windows per
  session** (`wnd[0]` main screen + a `wnd[1]` popup), in **two sessions**, including a
  *nested* popup (`wnd[1]` -> `wnd[2]`) in the second session. Session 1 runs VA01 with an
  F4 customer search-help popup; session 2 runs VA02 and hits an incomplete-data-log popup
  that itself triggers a Yes/No "exit processing?" confirmation. Verified: parses into 16
  objects / 22 actions, with `SapScriptParser`'s window (`w1_`/`w2_`) and session
  (`sess_s1_`) name prefixes combining correctly — e.g. the nested confirmation button
  becomes `sess_s1_w2_btnSPOP_OPTION1`. See `ObjectRepository/SAP/VA01_MultiWindowMultiSession.yaml`
  for the full generated page.
- `VA01_MultiWindowMultiSession.jsh` — the same multi-window/multi-session scenario as the
  `.ps1` above, translated to the JACOB `ActiveXComponent` idiom, importable via Tools ->
  Import SAP Recording -> Java (.java, .jsh). Verified: parses into the identical 16 objects /
  22 actions, same window/session prefixing. Not separately imported into this project (would
  either collide with or duplicate the `.ps1` version's `VA01_MultiWindowMultiSession` scenario
  and OR page) — it exists purely as an alternate-format sample for testing the Java import
  path specifically.
- `VA01_MultiWindowMultiSession.vbs` / `.js` — the same multi-window/multi-session scenario
  again, translated to VBScript and JavaScript respectively. Both go through the shared
  `SapLanguageParser` engine (same as the single-scenario `.vbs`/`.js` samples above), so both
  parse into the identical 16 objects / 22 actions as the `.ps1`/`.jsh` versions — same
  `OpenSession`/`SwitchSession` pair at the session-2 boundary, same `w1_`/`w2_`/`sess_s1_`
  name-prefix combining. Importable **today** via Tools -> Import SAP Recording -> VBScript
  (.vbs, .vba) / JavaScript (.js). Not separately imported into this project, same reasoning as
  the `.jsh` sample above — alternate-format samples for exercising each import path.

All eight were verified against the real parser classes (not just eyeballed) before being
added here — see the parse breakdown above for each.

### The imported test cases are already in this project

`VA01_CreateSalesOrder.jsh` and `VA01_MultiWindowMultiSession.ps1` weren't just parsed in
isolation — they were run through the real import pipeline (`SapParserLang*` +
`Project`/`ObjectRepository`/`SapOR`, the same classes `SapScriptParser.java` uses) against
this project, exactly as if imported via the IDE menu. That produced, and left in place:

- `ObjectRepository/SAP/VA01_CreateSalesOrder.yaml` (18 elements) and
  `ObjectRepository/SAP/VA01_MultiWindowMultiSession.yaml` (16 elements, session/window
  scoped).
- `TestPlan/VA01_CreateSalesOrder/VA01_CreateSalesOrder.yaml` (27 steps) and
  `TestPlan/VA01_MultiWindowMultiSession/VA01_MultiWindowMultiSession.yaml` (24 steps) —
  auto-migrated from the generator's CSV output on the next project load (original CSVs kept
  under `.migration-backup/`).

**Regenerated from the complete scenario / fixed parser output.** The `VA01_CreateSalesOrder`
artifacts above were originally generated from the sparse `.jsh` sample (11 elements / 16
actions — the `.jsh`/`.ps1`-only subset documented above), which left `ctxtVBAK-SPART`,
`ctxtVBKD-BSTKD`, `ctxtRV45A-MABNR[1,0]`, both tab-select steps, and the initial
`okcd`/`sendVKey` pair out of the imported test case entirely even though they exist in the
`.vbs`/`.js` samples of the same scenario. Both files have since been regenerated by hand from
the verified, real parser output (`SapParserLangVBScript` against `VA01_CreateSalesOrder.vbs` —
the full 18 objects / 25 actions), so the project's actual test case now matches what importing
the complete recording produces, not the earlier partial one. The `VA01_MultiWindowMultiSession`
test case has also been corrected: steps 14/15 (`sapExecuteTransaction VA02` /
`sapOpenSession @s1`) were swapped to put the session switch **before** the transaction it
actually belongs to — fixing the `Transaction`/`OpenSession` ordering bug found in
`SapParserLangPowerShell`/`SapParserLangJava` (now fixed in both parsers; see their `parse()`
methods' `trackSessionVariable()` call).

**`sapExecuteTransaction` steps' object literal was also wrong, project-wide.** Every
`Transaction` step the importer generated (`SapScriptParser.generateTestCase()`) used
`SAP_SYSTEM` as the step's `object` — but that string is not a registered object type anywhere
(`ObjectType.SAP = "SAP"` is the only literal registered for session-level, object-less actions
like `sapExecuteTransaction`; see the table in `SAP-Enhancement/01-SAP-Connections-Redesign.md`
and `CommandControlSapGuardrailTest`'s `object("SAP")` usage), so every imported transaction
step failed design-time object validation. Fixed at the source in `SapScriptParser.java`, and
in every fixture that had copied the wrong convention: `VA01_CreateSalesOrder.yaml`,
`VA01_MultiWindowMultiSession.yaml`, `VA03_LineItemAuditAndNavigation.yaml`, and
`SM37_JobStatusValidation.yaml` now all use `object: SAP` for their transaction steps.

**The same `sapExecuteTransaction` steps' input was also missing its `@` prefix** — bare
literal inputs like `VA01` must be `@`-prefixed per the step input grammar (see
`StepNormalizer`'s doc comment: "a bare literal input ... is `@`-prefixed"); `VA01` alone is
ambiguous, `@VA01` isn't. Fixed in `SapScriptParser.generateTestCase()`'s `Transaction` branch
and in the same four fixtures above (`input: VA01` → `input: '@VA01'`, etc.).

Both VA01 test cases were previously executed under fake mode (`ingenious run
SAPDemo/VA01_CreateSalesOrder/VA01_CreateSalesOrder` /
`.../VA01_MultiWindowMultiSession/VA01_MultiWindowMultiSession`) and **failed overall**, by
design: only the `sapInitConnection`/`sapOpenSession`/`sapSwitchSession`/`sapCloseConnection`
steps pass — every `sapFill`/`sapClick`/`sapExecuteTransaction`/etc. step fails, because those
still use the legacy raw-Dispatch path (see "Scope" above). That's expected and correct: it
demonstrates the import produces a genuinely valid, runnable test case, not that fake mode can
run arbitrary recordings. **The exact pass/fail step counts quoted in an earlier version of this
section are stale** following the regeneration above and haven't been re-captured by an actual
`ingenious run` yet — re-run both under fake mode to get current counts before quoting them
again.

## Running against a real SAP system instead

Build without `-Dsap.fakeMode=true` (or explicitly `-Dsap.fakeMode=false`), edit
`Settings/SAP/DEMO.properties` with real `user`/`password`/`connectionName`/`client`, and
make sure SAP GUI Scripting is enabled (see the "Known limitation — environment
prerequisites" section of `SAP-Enhancement/SAP-Connections-Redesign.md`). The same test
case will then drive a real SAP GUI session.
