# User Story: [SAP] Enable VBScript and JavaScript Import for SAP Recordings

Source: New request — no Excel item / formal user story yet. Captured here as request #11,
extending the recording importer first described in
[`01-SAP-Connections-Redesign.md`](01-SAP-Connections-Redesign.md)'s "SAP Scripting Tracker
import" section.

**Status: Implemented.** All changes below have shipped, as scoped (including the `FXMenuBar`
PowerShell parity decision). `IDE` module compiles clean (`mvn compile`, 332 source files, no
errors). `SapParserLangJavaScriptSessionTest` (new) and the existing `SapLanguageParserSessionTest`
/`SapParserLangPowerShellSessionTest` all pass (8/8). The new `VA01_CreateSalesOrder.js` sample
was verified directly against the compiled parser classes alongside the existing `.vbs` sample:
both produce an identical **18 objects / 25 actions**, with the same action-type sequence in the
same order — confirming the two languages align correctly through the shared parsing/mapping
engine, not just by inspection.

## User Story

As an INGenious engineer, I want to import SAP GUI Scripting Tracker recordings written in
VBScript (`.vbs`/`.vba`) and JavaScript (`.js`) — the two native formats the Tracker itself
produces — through the same **Tools → Import SAP Recording** menu already used for PowerShell
and Java, so that testers aren't forced to re-record or hand-convert a recording just because of
the language the Tracker happened to save it in.

## Acceptance Criteria (ATDD)

### Scenario 1: Import a VBScript recording via the menu

**Given** a `.vbs` or `.vba` SAP GUI Scripting Tracker recording

**When** the tester selects Tools → Import SAP Recording → VBScript and picks the file

**Then** the recording is parsed into a SAP Object Repository page and a test case, identically
to how a `.ps1`/`.java` recording of the same scenario is imported today

### Scenario 2: Import a JavaScript recording via the menu

**Given** a `.js` SAP GUI Scripting Tracker recording

**When** the tester selects Tools → Import SAP Recording → JavaScript and picks the file

**Then** the recording is parsed into a SAP Object Repository page and a test case, identically
to how a `.ps1`/`.java` recording of the same scenario is imported today

### Scenario 3: Multi-session recordings import correctly in both languages

**Given** a VBScript or JavaScript recording that switches between multiple `session`/`sessionN`
variables

**When** it is imported

**Then** `SAP.openSession` / `SAP.switchSession` steps are emitted at each switch point, matching
the behavior already verified for PowerShell and Java (Phase 4 of
[`01-SAP-Connections-Redesign.md`](01-SAP-Connections-Redesign.md))

### Scenario 4: Existing PowerShell and Java import paths are unaffected

**Given** the existing `.ps1` and `.java`/`.jsh` import flows

**When** this change ships

**Then** both continue to parse and import exactly as before — no regression in file-extension
routing, menu behavior, or generated output

### Scenario 5: Documentation is updated

**Given** VBScript and JavaScript import support is implemented

**When** documentation is updated

**Then** the SAPDemo sample-recordings README no longer describes VBScript as "not yet
reachable", and a JavaScript sample/description is added alongside it

### Scenario 6: FXMenuBar reaches menu parity with the Swing menu

**Given** the JavaFX menu (`FXMenuBar`) currently only exposes Java for SAP import, while the
Swing menu (`AppMenuBar`) already exposes PowerShell and Java

**When** this change ships

**Then** `FXMenuBar`'s Import SAP Recording menu exposes PowerShell, VBScript, Java and
JavaScript — the same set as the Swing menu, decided alongside this request rather than tracked
as separate follow-up cleanup

## Related SAP Request Items

None — this request has no corresponding row in `SAP Ingenious Issues 2026.xlsx`. Raised
directly during this session, prompted by a question about whether the importer already
supports the SAP GUI Scripting Tracker's native Record-and-Playback output formats.

## Context

`SapScriptParser`'s own class Javadoc already lists VBScript and JavaScript as "Supported
Languages" alongside PowerShell, Python and AutoIt, and both have had working parser classes
(`SapParserLangVBScript`, `SapParserLangJavaScript`) for some time. Neither, however, is reachable
from the running application: `SapParserFactory`'s extension map and both menu
implementations (`AppMenuBar` for Swing, `FXMenuBar` for JavaFX) only wire up `.ps1` and
`.java`/`.jsh`, with the VBScript/JavaScript (and Python/AutoIt/C#/VB.NET) entries left as
commented-out dead code. This gap, and the reason PowerShell was prioritized, is already recorded
in Phase 4 of [`01-SAP-Connections-Redesign.md`](01-SAP-Connections-Redesign.md):

> "PowerShell is the only language actually reachable from the IDE's Import SAP Recording menu
> today (every other menu entry is commented out in `AppActionListener`), so it was the
> priority; C# is not wired (also menu-disabled, lowest value)."

## Goal

Wire VBScript and JavaScript all the way through the existing, already-correct parsing engine —
no new parsing logic — so both are selectable from both menu implementations and routed
end-to-end by `SapScriptParser`/`SapParserFactory`.

## Value

- Covers the two formats the SAP GUI Scripting Tracker actually produces natively (VBScript is
  the Tracker's classic default output; JavaScript is its other first-class option), instead of
  only the two formats this project's own earlier work added (PowerShell, Java).
- Removes a re-recording/hand-conversion step for any tester who already has a `.vbs`/`.js`
  recording on hand.
- Very low implementation risk: the parsing logic is already implemented, tested (indirectly, for
  VBScript) and documented — this is a wiring change, not new parser development.

## Scope

Affected Components:

- `IDE/src/main/java/com/ing/ide/main/sapscript/parser/SapParserFactory.java`
- `IDE/src/main/java/com/ing/ide/main/mainui/AppActionListener.java` (`handleSapImport`)
- `IDE/src/main/java/com/ing/ide/main/mainui/AppMenuBar.java` (Swing menu)
- `IDE/src/main/java/com/ing/ide/main/fx/FXMenuBar.java` (JavaFX menu)
- `Resources/Projects/SAPDemo/SampleRecordings/` (new JavaScript sample)
- `Resources/Projects/SAPDemo/README.md`
- `IDE/src/test/java/com/ing/ide/main/sapscript/parser/` (new JavaScript-specific test)

Explicitly **out of scope**: Python, AutoIt, C#, VB.NET — their parser classes also exist and
share the same base-class engine, but the request that triggered this doc was specifically about
VBScript and JavaScript (the Tracker's own native formats). Wiring the rest would follow the
identical pattern described below and can be split into a separate, smaller follow-up if wanted.

## Current State (gap analysis)

**The parsing engine already fully supports both languages — this is pure wiring, not new
parsing work.** `SapParserLangVBScript` and `SapParserLangJavaScript` each only override the four
small language-identification hooks (`getLanguageName`, `getSupportedExtensions`, `isComment`,
`getSessionPrefix`); neither overrides `parse()`. Both therefore run through
`SapLanguageParser`'s shared, regex-based `parse()`/`parseSapAction()` machinery — the same
machinery the Phase 4 multi-session work (session variable tracking, `openSession`/
`switchSession` emission, session-scoped object naming) was built into and already covers, with
**no VBScript/JavaScript-specific gap**:

- The regex patterns used throughout `SapLanguageParser` (e.g.
  `session\w*\.findById("...")\.sendVKey\s*\(?\s*(\d+)\s*\)?`) already tolerate both VBScript's
  optional-parentheses call style (`sendVKey 0`) and JavaScript's required-parentheses style
  (`sendVKey(0)`) with the same pattern — no syntax divergence to special-case.
- The tab-selection gap noted for PowerShell and Java (a parameterless `.select()` call is
  silently dropped because their hand-written parsers only add an action when a parameter is
  present) **does not apply here**: `parseTabSelectAction` in the shared base class fires
  unconditionally, so both VBScript and JavaScript recordings capture tab switches correctly.
- `SapLanguageParserSessionTest` already exercises this exact code path end-to-end (three tests:
  single-session, multi-session switch-and-switch-back, and session-scoped object naming) —
  instantiating `SapParserLangVBScript` specifically because, per that test's own Javadoc, "it
  relies entirely on `SapLanguageParser`'s own parse()/parseSapAction() machinery (no
  language-specific override to duplicate against)". The same statement is true of
  `SapParserLangJavaScript`.
- A hand-authored sample recording already exists and is verified to parse correctly:
  `Resources/Projects/SAPDemo/SampleRecordings/VA01_CreateSalesOrder.vbs` — 18 objects / 25
  actions, per `Resources/Projects/SAPDemo/README.md`. No equivalent `.js` sample exists yet.

**What's actually missing is three commented-out registration points:**

1. `SapParserFactory.PARSER_MAP` — `"vbs"`/`"vba"` → `SapParserLangVBScript.class` and `"js"` →
   `SapParserLangJavaScript.class` are both commented out (only `"ps1"` and `"java"`/`"jsh"` are
   registered).
2. `AppActionListener.handleSapImport(String language)` — the `"VBScript"` and `"JavaScript"`
   cases in its file-dialog switch are commented out, so even reaching this code via a
   constructed action command would show no file picker for either.
3. Both menu builders — `AppMenuBar` (Swing) has the two menu items commented out entirely;
   `FXMenuBar` (JavaFX) doesn't even have placeholders for them (today it only wires up Java,
   making it already narrower than the Swing menu's reachable PowerShell+Java). **Decided:**
   this change also closes that pre-existing `FXMenuBar` gap for PowerShell, so both menus reach
   the same four languages (PowerShell, VBScript, Java, JavaScript) rather than leaving the FX
   menu permanently one language behind.

## Proposed Changes

| # | File | Change |
|---|---|---|
| 1 | `SapParserFactory.java` | Uncomment the `"vbs"`/`"vba"` → `SapParserLangVBScript.class` and `"js"` → `SapParserLangJavaScript.class` map entries. |
| 2 | `AppActionListener.java` | Uncomment the `"VBScript"` and `"JavaScript"` cases in `handleSapImport`'s switch, restoring their language-specific `Utils.openDialog(...)` file filters (`*.vbs, *.vba` / `*.js`). |
| 3 | `AppMenuBar.java` | Uncomment the two existing `sapOptionsMenu.add(...)` lines for VBScript and JavaScript. |
| 4 | `FXMenuBar.java` | Add PowerShell, VBScript and JavaScript entries to the `sapRecording` menu via the existing `createSapImportItem(...)` helper, following the same pattern already used for Java — closes the pre-existing PowerShell gap at the same time, so the FX menu reaches parity with the Swing menu. |
| 5 | `Resources/Projects/SAPDemo/SampleRecordings/VA01_CreateSalesOrder.js` | New — a JavaScript recording of the same VA01 scenario as the existing `.vbs`/`.ps1`/`.jsh` samples, for manual verification through the real menu once wired. |
| 6 | `IDE/src/test/java/.../parser/SapParserLangJavaScriptSessionTest.java` | New — mirrors `SapLanguageParserSessionTest`/`SapParserLangPowerShellSessionTest`'s cases but instantiates `SapParserLangJavaScript`, giving JavaScript its own explicit regression coverage instead of relying solely on the VBScript instantiation to stand in for the shared code path. |
| 7 | `Resources/Projects/SAPDemo/README.md` | Update the `SampleRecordings/` section: flip the VBScript bullet from "not yet reachable through the IDE's Import menu" to "importable today", and add a JavaScript bullet describing the new sample and its expected object/action counts. |

No changes are needed to `SapLanguageParser.java`, `SapParserLangVBScript.java`,
`SapParserLangJavaScript.java`, or `SapScriptParser.java` — the parsing and generation pipeline
is already correct for both languages.

## Testing

- New `SapParserLangJavaScriptSessionTest` (single-session, multi-session switch, session-scoped
  object naming — same three cases as the existing VBScript test).
- Existing `SapLanguageParserSessionTest` and `SapParserLangPowerShellSessionTest` must keep
  passing unchanged (regression — confirms the shared base class and the PowerShell override are
  untouched).
- Manual verification: import `VA01_CreateSalesOrder.vbs` and the new `VA01_CreateSalesOrder.js`
  through both Tools menus (Swing and JavaFX) once wired, and confirm the object/action counts
  match the existing `.ps1`/`.jsh`/`.vbs` baselines documented in the SAPDemo README.

## Decisions

- **`FXMenuBar`'s PowerShell gap is closed in this same change** — menu parity between Swing and
  JavaFX is in scope (see Scenario 6 and change #4 above), not deferred to separate cleanup.
- **Python, AutoIt, C#, VB.NET stay commented out.** No appetite to wire them alongside this
  request even though the pattern would generalize cleanly — left for a separate request if ever
  needed, per this doc's scope.
