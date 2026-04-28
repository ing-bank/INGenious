---
name: Browser-test-generation
description: 'Create a new INGenious Test project test case from a business flow. Use when user gives checkout/login/order business steps and wants Scenario -> TestCase CSV, reusable component flows, page object YAMLs, and test data sheets wired together.'
argument-hint: 'Business flow + scenario name + testcase name + expected outcomes'
user-invocable: true
---

# Test Case From Business Flow

Create a new test case inside an existing or new scenario in any INGenious project, following the project pattern:
- Build or reuse reusable components first
- Reference those components from the main test case CSV
- Build or reuse page object YAML files in ObjectRepository
- Build or update data sheets in TestData
- Discover unknown UI flow steps and selectors with microsoft/playwright-cli when repository coverage is missing
- Populate Action values from Java browser command definitions in Engine/src/main/java/com/ing/engine/commands/browser

Project root for this skill:
- Projects/<ProjectName>

Reference patterns are embedded directly in this skill under the `## Reference Patterns` section.
Do not rely on runtime file paths — use the inline examples below as the canonical reference.

## When To Use
- User provides business workflow steps and expected outcomes
- User asks to add a new test case under a scenario
- User wants reusable flow components before main test-case composition
- User wants POM and data-driven test design in INGenious CSV/YAML format

## Inputs To Collect
Collect these fields before writing files:
1. Project name (for example: Sauce Demo)
2. Scenario name
3. Test case name
4. Business flow steps (ordered)
5. Reusable flow split (proposed components)
6. Page names and element names/selectors (or AUT hints to derive selectors)
7. Test data fields and sample row values
8. Browser/runtime needs (if special)
9. Assertions (critical outcomes)
10. Preferred action set, if the user wants a subset of browser actions
11. Whether microsoft/playwright-cli is required for locator discovery

If key inputs are missing, ask concise follow-up questions before editing files.

### Project Selection Rule (when Project name is missing)
- If the user does not provide Project name, do not assume one.
- Read all folders under `Projects/` and present them as a selectable list to the user.
- Include one additional selectable option: `Create new project`.
- Allow the user to either:
  - Choose any existing project from the list, or
  - Choose `Create new project` and provide a new project name.
- Continue artifact creation only after user selection is confirmed.

## Target File Layout
Create or update only relevant files:
- Main test case: `Projects/<ProjectName>/TestPlan/<Scenario>/<TestCase>.csv`
- Reusable components: `Projects/<ProjectName>/ReusableComponents/<FlowGroup>/<FlowName>.csv`
- Page object YAML: `Projects/<ProjectName>/ObjectRepository/Web/pages/<Page>.yaml`
- Data sheets: `Projects/<ProjectName>/TestData/<Sheet>.csv`

Optionally update:
- `Projects/<ProjectName>/.project` only if scenario metadata management is explicitly required
- `Projects/<ProjectName>/Settings/*.Properties` only when run behavior needs changes

If Project folder does not exist:
- Create `Projects/<ProjectName>` with minimum scaffold:
  - `ObjectRepository/Web/pages`
  - `ReusableComponents`
  - `Settings`
  - `TestData`
  - `TestPlan`
  - `Results`
  - `Recording`
  - `api`
- Create `Projects/<ProjectName>/.project` from an existing project template if available.

## Preflight Checks
Run these checks before creating or editing flow artifacts:
1. Baseline runtime health
- Run one known testcase in the target project to confirm core actions resolve (Open, Click, Fill, assertion actions).
- If baseline fails with broad unknown-action errors, stop flow generation and report an environment/runtime blocker.

2. Execution mode confirmation
- Confirm preferred run command in this workspace (`./ingenious.command`).
- Confirm browser choice (default Chromium if user did not specify).

3. Project metadata behavior
- Check whether the project requires explicit metadata registration in `.project` for new scenarios/reusables/testcases.
- If required, plan metadata updates as part of the change.

4. Project selection validation
- If Project name was not explicitly provided in the request, run the Project Selection Rule first.
- Confirm the selected project exists, or scaffold it if user selected `Create new project`.

## Procedure
1. Parse and normalize business flow
- Convert user narrative into explicit UI/API actions and assertions.
- Identify shared steps suitable for reusability (login, search, checkout, confirmation).

2. Discover flow and locators with microsoft/playwright-cli (when needed)
- Use playwright-cli only when existing ObjectRepository pages do not already provide reliable selectors.
- Store all discovery artifacts under `.playwright-cli/` only.
- Never write step-level snapshot YAML files to workspace root.
- Local availability check (mandatory):
  - npx --no-install playwright-cli --version
- Fail-fast rule (mandatory):
  - If the local check fails, STOP and report blocker: "microsoft/playwright-cli is required but not installed locally in this working directory. Install it locally and retry."
  - Do not install playwright-cli automatically.
  - Do not use global playwright-cli.
  - Do not fall back to playwright-test or any other browser automation library.
- Command prefix rule:
  - Always run discovery commands as: npx playwright-cli <command>
- Command pattern:
  - npx playwright-cli open "<StartUrl>"
  - npx playwright-cli goto "<StepUrl>"
  - npx playwright-cli snapshot
  - npx playwright-cli click <ref>
  - npx playwright-cli fill <ref> "<value>"
  - npx playwright-cli tab-new "<Url>" (when business flow opens another tab)
  - npx playwright-cli tab-list and npx playwright-cli tab-select <index>
  - npx playwright-cli eval "el => el.getAttribute('data-testid')" <ref> (to extract stable locator signals)
  - npx playwright-cli snapshot --filename=.playwright-cli/after-step.yaml
  - npx playwright-cli snapshot --filename=.playwright-cli/<flow>-step-<n>.yaml
- Walk through user-provided business flow in browser and capture candidate selectors.
- Prefer snapshot refs first (`e1`, `e2`, etc.) while exploring; convert them to stable YAML selectors before finalizing.
- Normalize selectors to stable forms (prefer data-test, id, role/name), then map into page YAML.
- Do not use playwright-cli as final test execution; it is for discovery and authoring support only.

3. Build the Action dictionary from browser Java classes
- Source valid Action values from methods annotated with @Action in:
  - Engine/src/main/java/com/ing/engine/commands/browser
- Parse pattern: @Action(...) followed by public void <methodName>()
- Use method names as canonical Action values in CSV rows.
- Keep existing project-typical actions when present (Open, Fill, Click, assertElementTextMatches).
- Allow Execute only for orchestration steps in the main test case.

4. Design reusable components first
- Create or reuse flow CSVs under `ReusableComponents`.
- Keep each component cohesive and short (about 2-8 steps).
- Action column must use values from the browser Action dictionary (or Execute for composition rows).
- Never leave an empty `Action` column.

5. Build or reuse page objects (POM)
- For each referenced page/object, ensure YAML exists in `ObjectRepository/Web/pages`.
- In ObjectRepository entries, prioritize AriaRole-based identification for interactive elements.
- Preferred selector order: `role` -> `data-testid` -> `aria-label` -> stable id -> scoped css/text selector.
- For exact role+name matching, add:
  - `exact:`
  - `  - role`
- Prefer `role: Button;Next` over css patterns like `css: "button:has-text('Next')"`.
- Use css only when role is not reliable or element type is not well expressed by role.
- **Role value format rule:** The role type and element name MUST be combined in a single `role:` value using a semicolon separator: `role: Tab;Guest`. NEVER split them into separate keys such as `role: Tab` + `text: Guest`. The `text:` sub-key is not a valid INGenious selector field.
  - Correct: `role: Tab;Guest`
  - Incorrect: `role: Tab` with `text: Guest` on a separate line
- Avoid generic global selectors unless scoped to a stable parent container.
- Example:
  ```yaml
  page: Mortgage - Plans
  elements:
    Within 3 Months [Radio]:
      role: Radio;Within 3 months
      exact:
        - role
    Energy Label [Dropdown]:
      css: "select[aria-label*='energy label']"
    Next [Button]:
      role: Button;Next
  ```
- Reuse existing element names if equivalent to avoid duplication.

5a. POM object naming convention
- Use format: `<Business Label> [<Type>]` for interactive elements.
- Examples: `Next [Button]`, `Gross Yearly Income [Input]`, `Energy Label [Dropdown]`, `No Loans [Radio]`.
- Use Title Case for the business label and singular nouns where possible.
- Keep names page-local and unambiguous; avoid generic names like `Button1` or `Field`.
- Prefer stable business wording over positional wording; avoid `Top Next`, `Left Button`, etc.
- Reuse existing names when the element meaning is equivalent on the same page.
- If same label appears for different types, distinguish with type suffix only (for example `Status [Text]` vs `Status [Input]`).
- Do not include selector syntax, ids, or css fragments in object names.
- For non-interactive assertion targets, allow descriptive names without forced type suffix only when this matches existing project style.

6. Build or update test data sheets
- Add/extend CSVs under `TestData` with columns required by the reusable flows.
- Include `Scenario,Flow,Iteration,SubIteration` where relevant to existing pattern.
- Keep references consistent with `Sheet:Column` syntax from steps.
- Avoid trailing spaces in column names and references.

7. Compose the main test case
- In `TestPlan/<Scenario>/<TestCase>.csv`, orchestrate with `Execute` steps.
- For orchestration rows, enforce this schema:
  - `ObjectName=Execute`
  - `Action=<ReusableScenario>:<ReusableName>`
  - `Input` must be blank for Execute rows
- Example valid row:
  - `1,Execute,Run login reusable,Common:Login,,,`
- Example invalid row (do not use):
  - `1,Execute,Run login reusable,Execute,Common:Login,,`
- Reference reusable components in execution order.
- Keep top-level test case orchestration readable and short.

8. Add critical assertions
- Ensure final business outcome is asserted (not only clicked).
- Prefer explicit assert actions for completion text/status and key checkpoints.

9. Validate consistency before finishing
- Project path exists or was scaffolded correctly under `Projects/<ProjectName>`.
- Every `ObjectName` used in CSV exists in referenced page YAML.
- Every page YAML object name follows the POM naming convention or matches an existing approved project naming style.
- Every `Input` reference like `Sheet:Column` exists in CSV headers.
- No blank `Action` values.
- Every non-Execute `Action` value is present in browser @Action method names.
- Every `Execute` reference maps to an existing reusable component.
- File names and scenario/test names match user request exactly.

## Branching Rules
- If scenario folder exists:
  - Add new testcase CSV without modifying unrelated testcases.
- If project folder does not exist:
  - Scaffold project structure first, then create scenario/testcase artifacts.
- If project metadata requires explicit registration:
  - Update `.project` with new scenario/reusable/testcase entries and verify they are discoverable.
- If reusable flow already exists and semantically matches:
  - Reuse it; do not duplicate.
- If flow is similar but not identical:
  - Create a new flow with clear name under the same flow group.
- If selector confidence is low:
  - Add TODO placeholder only when unavoidable and explicitly flag it to the user.
- If requested action is not found in browser action definitions:
  - Map to the closest available action, document the mapping, and ask for confirmation.
- If locator coverage is missing in ObjectRepository:
  - Run microsoft/playwright-cli discovery only if local `npx playwright-cli` is available.
  - If local playwright-cli is unavailable, stop and report blocker; do not switch tooling.
  - Then update page YAML before generating final flow CSV steps.
  - For complex flows with redirects or multi-tab behavior, capture tab transitions and final URL/title snapshots as evidence.
  - Ensure all captured step snapshots are in `.playwright-cli/`.

## Quality Gates
A change is complete only when all are true:
1. Main test case uses Execute steps to reusable components.
2. Reusable component actions are non-empty and valid.
3. Reusable component Action values come from browser @Action method names.
4. Page object YAML keys match step object names.
5. Page object YAML keys follow the POM naming convention.
6. Data references resolve to existing sheets/columns.
7. At least one business outcome assertion exists.
8. Execute row schema is valid (`ObjectName=Execute`, reusable reference in `Action`, `Input` empty).
9. Baseline testcase runtime is healthy or blocker is explicitly reported before generation.
10. No unrelated files are modified.

## Testing Strategy
After creating or updating a flow, validate it in seven layers.

1. Static pre-run validation
- Validate every non-Execute Action against the browser @Action dictionary sourced from:
  - Engine/src/main/java/com/ing/engine/commands/browser
- Validate each CSV ObjectName exists in referenced page YAML.
- Validate each Sheet:Column input exists in TestData CSV headers.
- Validate each Execute reference points to an existing reusable component file.
- Fail fast on blank Action cells.
- If new selectors were introduced from unknown flow discovery, require local `npx --no-install playwright-cli --version` to pass first.
- If the check fails, stop and return environment blocker; do not install packages or switch to other automation libraries.

2. Baseline environment sanity
- Execute one known-good testcase from the same project.
- If baseline also fails with unknown-action errors, classify as environment/runtime blocker and stop authoring-level debugging.

3. Structural smoke execution
- Run the new testcase once in a single browser, single iteration mode.
- Confirm no runtime parsing errors such as unknown actions, missing objects, or missing data columns.
- Confirm reports are produced under:
  - Projects/<ProjectName>/Results

4. Assertion verification
- Verify at least one business-critical assertion is executed (not only click/fill steps).
- For outcome checks, prefer assertion actions such as assertElementTextMatches over passive clicks.
- Confirm pass/fail for those assertions in console and data report artifacts.

5. Stability and regression checks
- Re-run the same testcase at least 2 times to detect flaky selectors or timing issues.
- Add one negative-path variant when the business flow allows it.
- If reusable components were modified, run at least one existing testcase that depends on them to catch regressions.

6. Failure triage rules
- Unknown action errors in both new and baseline testcase:
  - Treat as environment/runtime issue.
- Unknown action errors only in new testcase:
  - Treat as flow authoring/schema issue.
- Missing object errors only in new testcase:
  - Treat as ObjectRepository issue.
- Missing data reference errors only in new testcase:
  - Treat as TestData/header issue.

7. Completion criteria for testing
- Test run has zero unknown-action or empty-action errors.
- All newly added assertions execute and pass for happy-path data.
- Expected negative-path behavior is observed when applicable.
- No unrelated existing testcase regressed due to reusable-component changes.
- If baseline runtime is unhealthy, stop and return blocker diagnostics instead of continuing flow edits.

## Execute Command Template
After creating a test flow, print and use an execution command with resolved placeholders.

1. Scenario and testcase execution (design mode)
- Command template:
  - ./ingenious.command -run -project_location "Projects/<ProjectName>" -scenario "<Scenario>" -testcase "<TestCase>" -browser "<Browser>"
- Alternative wrapper command (if used in workspace):
  - ./Run.command -run -project_location "Projects/<ProjectName>" -scenario "<Scenario>" -testcase "<TestCase>" -browser "<Browser>"
- Default browser if not provided by user:
  - Chromium

2. Release and testset execution (batch mode)
- Command template:
  - ./ingenious.command -run -project_location "Projects/<ProjectName>" -release "<Release>" -testset "<TestSet>" -browser "<Browser>"

3. Optional execution flags
- Headless:
  - -op_setHeadless true
- Tag filter:
  - -tags "<Tag1,Tag2>"

4. Post-run checks
- Verify latest run artifacts under:
  - Projects/<ProjectName>/Results
- Verify console output and data report for:
  - Unknown action errors
  - Missing object or data reference errors
  - Final business assertion status
- If broad unknown-action errors also occur in known baseline testcase:
  - Report environment/runtime blocker and halt further flow modifications.
- Include the resolved run command in the final summary to user.

## Output Summary Format
When done, report:
- Files created/updated
- Reusable components added/reused
- Assertions added
- Data sheets/columns added
- Any assumptions or pending selector confirmations

## Reference Patterns

All patterns below are extracted directly from a real INGenious project ("Purchase a product" flow).
Use them as the canonical format reference — do not depend on any runtime file path.

### 1. Main TestPlan CSV (orchestration)
File: `Projects/<ProjectName>/TestPlan/<Scenario>/<TestCase>.csv`
```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
1,Execute,,Common:Login,,,
2,Execute,,Purchase Flow:Add item to cart,,,
3,Execute,,Purchase Flow:Fill Checkout Info,,,
4,Execute,,Purchase Flow:Finish Checkout,,,
```
- `ObjectName` must be `Execute` for all orchestration rows.
- `Action` must be `<ReusableScenario>:<ReusableName>`.
- `Input`, `Condition`, `Reference` must all be blank.

### 2. Reusable Component CSV (login)
File: `Projects/<ProjectName>/ReusableComponents/Common/Login.csv`
```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
1,Browser,Open the Url [<Data>] in the Browser,Open,@https://www.saucedemo.com/,,
2,Username,Enter the value [<Data>] in the Field [<Object>],Fill,Login:Username,,[Project] Login
3,Password,Enter the value [<Data>] in the Field [<Object>],Fill,Login:Password,,[Project] Login
4,Login [Button],Click the [<Object>],Click,,,[Project] Login
```
- `Reference` column uses `[Project] <PageName>` to point at the ObjectRepository page.
- `Input` for data-driven steps uses `<Sheet>:<Column>` syntax.
- `Open` with `@<url>` hardcodes the URL inline; use a data reference (`<Sheet>:URL`) for parameterised URLs.

### 3. Reusable Component CSV (add item to cart + assertion)
File: `Projects/<ProjectName>/ReusableComponents/Purchase Flow/Add Item to Cart.csv`
```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
1,Add to Cart [button],Click the [<Object>],Click,,,[Project] Product
2,Shopping Cart [icon],Click the [<Object>],Click,,,[Project] Product
3,Item Name,Assert if [<Object>] has text [<Data>],assertElementTextMatches,Purchase Details:Product,,[Project] Your Cart
```

### 4. Reusable Component CSV (checkout form fill)
File: `Projects/<ProjectName>/ReusableComponents/Purchase Flow/Fill Checkout Info.csv`
```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
1,Checkout [button],Click the [<Object>],Click,,,[Project] Your Cart
2,First Name,Enter the value [<Data>] in the Field [<Object>],Fill,Purchase Details:FirstName,,[Project] Checkout - Your Information
3,Last Name,Enter the value [<Data>] in the Field [<Object>],Fill,Purchase Details:LastName,,[Project] Checkout - Your Information
4,Postal Code,Enter the value [<Data>] in the Field [<Object>],Fill,Purchase Details:PostCode,,[Project] Checkout - Your Information
5,Continue [button],Click the [<Object>],Click,,,[Project] Checkout - Your Information
```

### 5. Page Object YAML
File: `Projects/<ProjectName>/ObjectRepository/Web/pages/<Page>.yaml`
```yaml
page: Login
elements:
  Username:
    css: "[data-test=\"username\"]"
  Password:
    css: "[data-test=\"password\"]"
  Login [Button]:
    css: "[data-test=\"login-button\"]"
```
Role-based example (preferred for interactive elements):
```yaml
page: Mortgage - Plans
elements:
  Within 3 Months [Radio]:
    role: Radio;Within 3 months
    exact:
      - role
  Energy Label [Dropdown]:
    css: "select[aria-label*='energy label']"
  Next [Button]:
    role: Button;Next
  Guest [Tab]:
    role: Tab;Guest
```
> **Rule:** Role type and element name are always written as `Type;Name` in a single `role:` value.
> Never split into `role: Tab` + `text: Guest` — `text:` is not a valid INGenious YAML key.

### 6. TestData CSV
File: `Projects/<ProjectName>/TestData/<Sheet>.csv`
```csv
Scenario,Flow,Iteration,SubIteration,URL,Username,Password
Common,Login,1,1,https://www.saucedemo.com/,standard_user,secret_sauce
```
With multiple data columns:
```csv
Scenario,Flow,Iteration,SubIteration,Product,FirstName,LastName,PostCode,SuccessMessage
Purchase a product,Buy a single item,1,1,Sauce Labs Backpack,John,Doe,1121LK,Thank you for your order!
```

### 7. .project metadata file
File: `Projects/<ProjectName>/.project`
```json
{
  "id": "<ProjectName>",
  "name": "<ProjectName>",
  "version": "2.4",
  "attributes": [],
  "tags": [],
  "_meta": [
    { "type": "scenario", "name": "Common", "ref": "com.ing.datalib.model.Attribute", "attributes": [], "tags": [] },
    { "type": "scenario", "name": "<YourScenario>", "ref": "com.ing.datalib.model.Attribute", "attributes": [], "tags": [] }
  ],
  "data": [
    {
      "id": "<Scenario>#<TestCaseName>",
      "name": "<TestCaseName>",
      "tags": [],
      "attributes": [
        { "name": "type", "value": "testcase" },
        { "name": "scenario", "value": "<Scenario>" }
      ]
    },
    {
      "id": "<FlowGroup>#<ReusableName>",
      "name": "<ReusableName>",
      "tags": [],
      "attributes": [
        { "name": "type", "value": "reusable" },
        { "name": "scenario", "value": "<FlowGroup>" }
      ]
    }
  ]
}
```
- Every new scenario, reusable, and testcase must be registered in `_meta` and `data` respectively.
- Use the same id format `<Scenario>#<Name>` for all entries.

## Example Invocation Prompts
- `/test-case-from-business-flow Project: Test. Scenario: Purchase a product. Testcase: Buy a single item. Reuse existing Login reusable flow and update page objects/test data as needed.`
- `/test-case-from-business-flow Add scenario "Returns" testcase "Return single item" using existing login flow; build reusable flows first, create page objects, and data sheets.`
- `/test-case-from-business-flow Scenario: Checkout. Testcase: Buy 2 items with coupon. Business flow: login -> add products -> apply coupon -> checkout -> verify total and success.`
- `/test-case-from-business-flow Create a failed-login testcase with data-driven invalid users; reuse existing Login page object and add assertions.`