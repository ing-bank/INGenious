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

## Inputs (ask only if missing)

1. Project (if absent, `ingenious_project_list` → let the user pick, or offer create)
2. Scenario name, Test case name
3. Ordered business-flow steps + expected outcomes (assertions)
4. Start URL (for discovery) and page/element hints
5. Data fields + sample values (if data-driven)

## Deterministic playbook

Run these tool calls in order. Fill slots from the inputs; do not reorder.

1. **Project**
   - `ingenious_project_list` → confirm/select. If new: `ingenious_project_create`.

2. **Scenario**
   - `ingenious_scenario_create` `{project, scenario}` (idempotent; ignore "exists").

3. **Object Repository coverage**
   - `ingenious_object_list` `{project}` and `ingenious_object_search` for the pages/
     elements the flow needs.
   - **If coverage is missing**, discover with the bounded browser session (no manual
     snapshots, no hand-written selectors):
     1. `ingenious_browser_session_start` `{project, url, scenario, testcase, page}`
     2. `ingenious_browser_session_do` for each UI action (click/fill/select …),
        walking the user's flow.
     3. `ingenious_browser_session_snapshot` only if you need to confirm refs.
     4. `ingenious_browser_session_save` — materializes discovered elements as **YAML OR**
        objects under `ObjectRepository/Web/<Page>.yaml` AND rewrites recorded steps to
        reference `Page.objName`. (Returns `objectsCreated`.)
     5. `ingenious_browser_session_close`.
   - For elements you already know, add them explicitly with `ingenious_object_add`
     `{project, page, name, locator}` (locator strategy = role/text/label/css/xpath/…).

4. **Reusable components** (cohesive 2–8 step flows: login, search, checkout …)
   - Prefer `ingenious_gen_testcase` with a browser archetype (`browser-login`,
     `browser-flow`, `browser-search`) `{project, scenario, name, reusable:true, params}`.
   - Otherwise `ingenious_testcase_create` `{reusable:true}` then
     `ingenious_testcase_add_step` per step. Object refs use `Page.objName` from step 3.

5. **Main test case** (orchestrates reusables with `Execute` steps)
   - `ingenious_testcase_create` `{project, scenario, name}`.
   - One `Execute` step per reusable via `ingenious_testcase_add_step`
     (`object:Execute`, `action:<ReusableScenario>:<ReusableName>`, input blank).
   - Add at least one business-outcome assertion step (discover the action name with
     `ingenious_action_search assert`).

6. **Data (only if data-driven)**
   - `ingenious_data_sheet_create` → `ingenious_data_column_add` →
     `ingenious_data_row_add` (or `ingenious_data_generate` for synthetic values).
   - Reference from steps as `Sheet:Column`.

7. **Validate**
   - `ingenious_testcase_validate` `{project, scenario, testcase}` → must return
     `valid:true`. Fix reported `errors`/`warnings` by re-calling the relevant tool.

8. **Smoke run**
   - `ingenious_run_dry` then `ingenious_run` `{project, scenario, testcase, browser}`
     (default browser Chromium).
   - `ingenious_report_latest` / `ingenious_report_failures` to confirm the outcome
     assertion passed.

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
