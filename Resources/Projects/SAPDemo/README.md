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

Two hand-authored recordings of the same VA01 (Create Sales Order) scenario, cross-checked
in a second session with VA03 (Display Sales Order), in the two formats INGenious's SAP
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
  here — `SapParserLangVBScript`'s equivalent case is unconditional). **Not yet reachable
  through the IDE's Import menu** — `.vbs`/`.vba` registration is commented out in
  `SapParserFactory.java` (only `.ps1` and `.java`/`.jsh` are wired up today). It parses
  correctly if driven directly against `SapParserLangVBScript` (e.g. from a test or script);
  use the `.ps1` file to exercise the actual menu-driven import.
- `VA01_CreateSalesOrder.jsh` — importable **today** via Tools -> Import SAP Recording ->
  Java (.java, .jsh), using the JACOB `ActiveXComponent`/`.invoke(...)`/`.setProperty(...)`
  idiom from `SapParserLangJava`'s own Javadoc example. Verified: parses into 11 objects /
  16 actions — same shape as the `.ps1` sample (including the `OpenSession`/`SwitchSession`
  pair), because it deliberately **omits the tab-selection step**: `SapParserLangJava`'s
  `"select"` case has the identical gap as PowerShell's (only fires with a non-empty
  parameter), so a parameterless `.invoke("select")` would silently import as nothing —
  no point including a step that doesn't do anything.
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

All five were verified against the real parser classes (not just eyeballed) before being
added here — see the parse breakdown above for each.

### The imported test cases are already in this project

`VA01_CreateSalesOrder.jsh` and `VA01_MultiWindowMultiSession.ps1` weren't just parsed in
isolation — they were run through the real import pipeline (`SapParserLang*` +
`Project`/`ObjectRepository`/`SapOR`, the same classes `SapScriptParser.java` uses) against
this project, exactly as if imported via the IDE menu. That produced, and left in place:

- `ObjectRepository/SAP/VA01_CreateSalesOrder.yaml` (11 elements) and
  `ObjectRepository/SAP/VA01_MultiWindowMultiSession.yaml` (16 elements, session/window
  scoped).
- `TestPlan/VA01_CreateSalesOrder/VA01_CreateSalesOrder.yaml` (18 steps) and
  `TestPlan/VA01_MultiWindowMultiSession/VA01_MultiWindowMultiSession.yaml` (24 steps) —
  auto-migrated from the generator's CSV output on the next project load (original CSVs kept
  under `.migration-backup/`).

Both were then actually executed under fake mode (`ingenious run
SAPDemo/VA01_CreateSalesOrder/VA01_CreateSalesOrder` /
`.../VA01_MultiWindowMultiSession/VA01_MultiWindowMultiSession`). Both **fail overall**, by
design: only the `sapInitConnection`/`sapOpenSession`/`sapSwitchSession`/`sapCloseConnection`
steps pass (4 of 28 and 4 of 39 steps respectively) — every `sapFill`/`sapClick`/
`sapExecuteTransaction`/etc. step fails, because those still use the legacy raw-Dispatch path
(see "Scope" above). That's expected and correct: it demonstrates the import produces a
genuinely valid, runnable test case, not that fake mode can run arbitrary recordings.

## Running against a real SAP system instead

Build without `-Dsap.fakeMode=true` (or explicitly `-Dsap.fakeMode=false`), edit
`Settings/SAP/DEMO.properties` with real `user`/`password`/`connectionName`/`client`, and
make sure SAP GUI Scripting is enabled (see the "Known limitation — environment
prerequisites" section of `SAP-Enhancement/SAP-Connections-Redesign.md`). The same test
case will then drive a real SAP GUI session.
