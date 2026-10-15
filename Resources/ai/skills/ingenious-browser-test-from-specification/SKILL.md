---
name: ingenious-browser-test-from-specification
description: 'Create an INGenious 3.1.x browser test from a business flow by orchestrating the ingenious_* MCP tools. Use when the user gives checkout/login/order steps and wants a YAML Scenario -> TestCase, YAML reusable components, YAML Object Repository pages, and data sheets — all authored through tools, not hand-written files.'
argument-hint: 'Business flow + scenario name + testcase name + expected outcomes'
user-invocable: true
version: "2.0.0"
requires:
  ingenious: ">=3.1.0 <3.2.0"
metadata:
  author: ingenious-team
  category: test-generation
---

# Browser Test From Business Flow (tool-first, YAML)

Author a browser test in an INGenious 3.1.x project by **calling `ingenious_*` MCP
tools**. The engine owns every file format (YAML test cases, YAML Object Repository,
CSV data) and validates against the live action catalog. Your job is to orchestrate
tools deterministically — never to hand-write `TestPlan/**`, `ObjectRepository/**`,
`TestData/**`, or `.project`.

## Non-negotiable rules

- **One discovery pass only.** Walk the flow with the Playwright CLI exactly once,
  export it, and import it. After `ingenious_import_playwright` succeeds the test
  EXISTS — do NOT discover again, do NOT call `ingenious_browser_session_save`, and
  do NOT create a V2/V3 copy. Re-running discovery is the biggest waste of time and
  credits. If a locator is wrong, fix that one object — never redo the whole journey.
- **Tool-first.** Every artifact mutation goes through an `ingenious_*` tool. Do NOT
  create or edit test/OR/data/project files with file-edit tools.
- **Discover, don't derive.** Get valid actions from `ingenious_action_search` /
  `ingenious_action_info`. NEVER read `Engine/src/**/commands/**` Java to find actions.
- **Validate via engine.** Correctness is whatever `ingenious_testcase_validate` says.
  Do not reason about YAML shape yourself.
- **Dry-run first.** Pass `dryRun:true` on create/add tools and use `ingenious_run_dry`
  before a real `ingenious_run`.
- **On tool error, use the suggestion.** MCP errors carry `error.data.suggestions`
  ("Did you mean: …"). Re-call with a suggested value; never hand-fix files.
- **Ask only for genuinely missing inputs.** One concise plan up front, then execute.
  No per-step narration.

## Attended vs. unattended mode

The calling client (AI CLI / IDE assistant) declares `OPERATING MODE: ATTENDED` or
`OPERATING MODE: UNATTENDED` in its system prompt for this session — check it before
the smoke run (step 7):

- **UNATTENDED** — behave exactly as step 7 describes: on failure, read
  `ingenious_report_failures`, fix everything at once, and run **once** more before
  reporting.
- **ATTENDED** — the user is watching this turn. Still finish discovery + import (they
  are cheap, deterministic, and not worth interrupting for), but treat the FIRST smoke
  run as a hard checkpoint: attempt `ingenious_run` **once**. If it fails, STOP — do not
  fix-and-rerun, do not re-discover, do not try a second run. End the turn with the
  **need-help report** below instead of continuing to iterate; the user usually knows
  the app/project better than you do and can point you at the real cause in seconds.

### Need-help report (attended mode, on run failure)

Reply in plain text (no further tool calls) with:
1. What you attempted (scenario/testcase name, the flow in one line).
2. The test case as it stands — steps, objects, and reference used per step.
3. The relevant Object Repository entries — page name + locator for every object the
   failing step(s) touch.
4. The data/sheet rows involved, if any.
5. What `ingenious_report_failures` said, and a specific question about what might be
   wrong (e.g. "is this locator/label still correct in the app?").

## Inputs (ask only if missing)

1. Project (if absent, `ingenious_project_list` → let the user pick, or offer create)
2. Scenario name, Test case name
3. Ordered business-flow steps + expected outcomes (assertions)
4. Start URL (for discovery) and page/element hints
5. Data fields + sample values (if data-driven)

## Deterministic playbook

The standard technique is **import-first**: let Playwright produce a Java recording,
import it so the engine builds the Page-Object-Model, steps, and locators
deterministically, then refine on top. This is far more deterministic and far less
token-intensive than hand-authoring steps and locators.

Run these tool calls in order. Fill slots from the inputs; do not reorder.

1. **Project**
   - `ingenious_project_list` → confirm/select. If new: `ingenious_project_create`.

2. **Scenario**
   - `ingenious_scenario_create` `{project, scenario}` (idempotent; ignore "exists").

3. **Obtain a Playwright Java recording of the flow** (the discovery step)
   - **If the user already has a Playwright Java script** (from
     `playwright codegen --target java`), use its path directly — skip to step 4.
   - **Otherwise discover with the Playwright CLI**, then export it to Java:
     1. `ingenious_browser_discover` `{project, url, prompt, scenario, testcase, page}`
        — opens the flow and returns a ref'd snapshot + a fixed protocol.
     2. `ingenious_browser_session_do` for each UI action (fill/click/select …),
        walking the user's flow. Use only the refs from the latest snapshot; never
        invent refs or locators. Each call blocks until the CLI finishes.
     3. `ingenious_browser_session_export` `{name}` — writes the recorded actions as a
        **Playwright Java** recording file and returns its `file` path.
     4. `ingenious_browser_session_close` `{name}`.

4. **Import the recording deterministically** (POM + steps + locators)
   - `ingenious_import_playwright` `{project, file, scenario, testcase}` — the engine
     parses the Java into an Object-Repository page (YAML) + a test case with standard
     locators. No hand-authored steps or locators. This is the deterministic base.
   - **Commit to this one path.** Once the recording is exported and imported, the test
     exists — do NOT also run `ingenious_browser_session_save`, and do NOT re-run the
     whole discovery. `ingenious_browser_session_close` is best-effort: if it errors
     (e.g. "No such session"), ignore it and continue; never restart discovery over it.
     If `ingenious_browser_session_export` fails, retry the export once — do not
     re-discover from scratch.

5. **Refine on top of the imported test** (this is the AI's value-add)
   - **Split into reusable components by user intent.** Group cohesive 2–8 step blocks
     (e.g. "Create Account", "Add Bank Account", "Make Payment") into reusables:
     `ingenious_testcase_create` `{reusable:true}` (+ `ingenious_testcase_add_step`, or
     move the imported steps), then replace them in the main test with `Execute` steps
     (`object:Execute`, `action:<ReusableScenario>:<ReusableName>`).
   - **Distribute Object-Repository objects across per-screen pages.** The import lands
     objects on one page; move them onto per-screen pages with `ingenious_object_add` /
     `ingenious_object_update` (and update step references) so each page maps to a screen.
     **Batch this**: `ingenious_object_add` accepts an `objects` array — collect every
     object for a target page and add them with ONE call
     (`{page, objects:[{name, locator, value}, ...]}`), not one call per object.
     Likewise, `ingenious_testcase_edit_step` accepts an `edits` array — retarget every
     affected step's `object`/`reference` in ONE call per test case
     (`{scenario, testcase, edits:[{index, object, reference}, ...]}`), not one call
     per step.
   - **Parameterize data into sheets.** Run `ingenious_testcase_parameterize`
     (mode=scan → apply), or create sheets with `ingenious_data_sheet_create` /
     `ingenious_data_column_add` / `ingenious_data_row_add`; reference as `Sheet:Column`.
   - **Dynamic / unique / random data** (spec says "dynamic email", "unique username",
     "random order id", "for repeatability", etc.): never hardcode or hand-invent the
     value. Insert a synthetic-data step (`object: Data`; find it with
     `ingenious_action_search "email"` / `"uuid"` / `"random"` / `"number"` etc., e.g.
     `emailAddress`, `internetUUID`, `randomNumberWithNoOfDigits`) immediately before the
     step that first needs the value, with `Input` set to the `Sheet:Column` where the
     generated value is stored — then reference that same `Sheet:Column` in the steps
     that consume it. This generates a fresh value every run, so re-running the test
     never collides with data from a prior run.
   - **Add at least one business-outcome assertion** (discover the action with
     `ingenious_action_search assert`).

6. **Validate**
   - `ingenious_testcase_validate` `{project, scenario, testcase}` → must return
     `valid:true`. Fix reported `errors`/`warnings` by re-calling the relevant tool.

7. **Smoke run (once — then triage, don't loop; see Attended vs. unattended mode above)**
   - `ingenious_run_dry` then `ingenious_run` `{project, scenario, testcase, browser}`
     (default browser Chromium). Leave `headless` unset — it defaults to `false` so the
     run is headed (visible); only pass `headless:true` if the user explicitly asks for it.
   - **UNATTENDED**: on failure, do NOT re-run the whole journey per fix. Read
     `ingenious_report_failures`, fix **all** reported issues at once (verify a
     suspect locator fast with `ingenious_browser_inspect` or the still-open
     discovery session), then run **once** more. Running a multi-reusable journey
     repeatedly to debug one locator is the main time sink — avoid it.
   - **ATTENDED**: on failure, stop after this one attempt and produce the
     need-help report above instead of retrying.

## Locator rules (avoid the run→fix loop)

- **Role locators use `Role;Name` with a SEMICOLON** — e.g. `button;Create a new account`.
  The colon in `Sheet:Column` is the **data** separator; never put a `:` in a role
  locator (it crashes `AriaRole.valueOf` at run time).
- **Prefer import-first locators.** The import path produces working locators; don't
  hand-edit them unless a step actually fails.
- **Dynamic option text** (e.g. a generated account label like
  `Current account · NL •••• 4300 — €1,000.00`): select by **index** or **partial
  text**, or store the value from a prior step — never hardcode volatile exact text.

> Fallback (only if the Playwright CLI is unavailable): author objects and steps
> directly with `ingenious_browser_session_save` or `ingenious_object_add` +
> `ingenious_testcase_add_step`. Prefer the import-first flow whenever possible.

## Naming conventions (fixed — do not vary)

- OR object name: `<Business Label> [<Type>]` — e.g. `Next [Button]`,
  `Gross Yearly Income [Input]`, `Energy Label [Dropdown]`. Title Case, page-local,
  no selector fragments in the name. Reuse an existing name when the element matches.
- Reusable scenario names use a `Flow` suffix (`LoginFlow`); test-case scenarios use a
  domain name (`LoginTests`). A reusable's scenario MUST NOT equal a test case's scenario.
- No hardcoded data in step inputs — all values go in a data sheet, referenced as
  `Sheet:Column`.

## Done when

1. All mutations went through `ingenious_*` tools (no hand-authored files).
2. `ingenious_testcase_validate` → `valid:true`.
3. Test case + reusables + OR pages exist as YAML on disk (via the tools).
4. At least one business-outcome assertion runs and passes on a smoke run.
5. No unrelated files changed.

## Output summary

Report: project/scenario/testcase, reusables created/reused, OR objects created
(`objectsCreated`), data sheet/columns, assertions, the resolved `ingenious_run`
command, and validation result.
