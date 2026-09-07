---
description: Migrate UI tests from Java Selenium or Gherkin feature files to INGenious 3.1.x (YAML Object Repository + YAML test cases, Playwright engine)
name: ingenious-ui-migrator
tools: ['runCommands', 'view', 'create', 'edit', 'grep', 'glob']
version: "2.0.0"
requires:
  ingenious: ">=3.1.0 <3.2.0"
metadata:
  author: ingenious-team
  category: test-generation
---

# UI Selenium / Gherkin -> INGenious 3.1.x Migration (YAML)

Targets **INGenious 3.1.x**, which stores everything as YAML: a per-page YAML
Object Repository, YAML reusable components, YAML test cases, and a YAML test
lab. Project metadata lives in a JSON `.project` (schema `3.1.0`).

Inputs: a `.feature` file, a folder of `.feature` files, or a Selenium Java
project path.

Canonical reference project (study it for exact formatting): `Projects/P1`.

## NEVER / FORBIDDEN — Hard Rules (violation = migration failure)

Every rule below MUST be enforced on every migration without exception.

1. **No hardcoded data in step inputs**: MUST NOT write `input: "@<literal>"` in any
   step. ALL input values MUST be placed in a CSV data sheet and referenced as
   `Sheet:Column`. If no data sheet exists yet, create one. No exceptions.

2. **No duplicate scenario names across reusables and test cases**: The `scenario:`
   field in a Reusable (`ReusableComponents/`) MUST NEVER equal the `scenario:` field
   in a Test Case (`TestPlan/`). Convention — use a `Flow` suffix for reusable
   scenarios (`LoginFlow`, `SubscriptionFlow`) and a meaningful test-domain name for
   test-case scenarios (`LoginTests`, `SubscriptionSuite`). Both names must be
   descriptive — not generic labels like `Test`, `Flow`, or `Scenario`.

3. **No `#id` in step inputs**: GlobalData environment IDs (`#dev`, `#test`, `#acc`)
   MUST only appear as **cell values** inside data sheet CSVs. MUST NOT place them in
   a step `input:` field. Steps always reference `Sheet:Column`.

4. **No css/xpath for shadow DOM elements**: When a source locator is a Java `String`
   that chains `.shadowRoot.querySelector()` calls, it is a shadow DOM path — Selenium
   used JS execution because its native locators cannot traverse shadow roots. MUST map
   to `jsPath:` in the OR and strip the leading `return ` keyword. MUST NOT attempt
   to convert shadow DOM paths to `css:` or `xpath:`.

5. **Multi-environment is mandatory when detected**: If the source project references
   more than one environment (multiple base URLs, credential sets, or environment-switch
   logic), MUST scaffold with `--with-env-helpers` / `-WithEnvHelpers` and populate
   GlobalData with one row per environment. MUST NOT hard-code any environment-specific
   value anywhere in the project.

   **Detection scope — MUST scan ALL of the following, not just page objects**:
   - `*Constants.java`, `*TestConstants.java`, `*Config.java`, `*Settings.java`
   - `application*.properties`, `application*.yml`
   - `.env`, `.env.*`, `*.properties`

   **Detection signals — any one is sufficient to trigger multi-env**:
   - Two or more `static final` String constants whose names share an environment
     prefix (`ACCP_`, `TEST_`, `DEV_`, `PROD_`, `STAGING_`, `QA_`) and end in
     `_URL`, `_USERNAME`, or `_PASSWORD`.
   - A method or switch that selects a URL/credential based on a string argument
     (e.g. `if (env.equals("TEST"))`, `switch(environment)`).
   - A `.properties` or `.yml` file that defines keys per environment profile.

   **Password handling — MUST NEVER copy plaintext passwords**: Any `*_PASSWORD`
   constant found in source MUST become `PLACEHOLDER_<ENV>_DO_NOT_COMMIT` in
   GlobalData. MUST NOT copy the literal password value into any INGenious file.

   **Enforcement**: The pre-scaffolding environment scan in §2 MUST be run and its
   result recorded before any OR, reusable, or test-case work begins (see ENV-GATE
   in §11).

6. **Tags MUST be preserved**: If the source framework (Gherkin or Selenium) declares
   tags on a Feature, Scenario, or Scenario Outline, those tags MUST appear in the
   corresponding INGenious YAML `tags:` list. MUST NOT silently drop any tag. Rules:
   - **Feature-level tags** (e.g. `@UX01 @MPSSUI @Ignore` on the `Feature:` line) →
     apply to **every** test case migrated from that feature file.
   - **Scenario / Scenario Outline-level tags** (e.g. `@11306067` directly above a
     `Scenario:`) → apply only to **that specific test case** YAML.
   - Tag format in YAML: include the leading `@` character, e.g. `tags: ['@smoke', '@UX01']`.
   - All unique tags used MUST also be registered in `.project` via `--tags` during
     scaffolding or `--sync` after adding files.

## 0) Execution model — TOOL-FIRST (overrides all file-authoring below)

**All writes MUST go through `ingenious_*` MCP tools.** The YAML/CSV format sections
(§4–§8, §10) below are **reference for what the tools produce** — read them to understand
shapes and to map source constructs, but DO NOT hand-write those files. The engine owns
the canonical format and validation; hand-authoring reintroduces the drift this upgrade
removes.

Artifact → tool mapping:

| Artifact | Author with |
|---|---|
| Project scaffold + `.project` + tags | `ingenious_project_create` (tags auto-register; no manual `--sync`) |
| Object Repository page/object (YAML) | `ingenious_object_add` / `ingenious_object_update` / `ingenious_object_import_page` / `ingenious_object_list` / `ingenious_object_search` |
| Reusable component (YAML) | `ingenious_testcase_create {reusable:true}` + `ingenious_testcase_add_step` |
| Test case (YAML) | `ingenious_testcase_create` + `ingenious_testcase_add_step` / `_edit_step` / `_move_step` |
| Test lab / set | `ingenious_testset_create` / `ingenious_testset_add` |
| Test data (CSV) | `ingenious_data_sheet_create` / `ingenious_data_column_add` / `ingenious_data_row_add` |
| Environments / GlobalData | `ingenious_env_create` / `ingenious_env_list` / `ingenious_data_*` |
| Action validity | `ingenious_action_search` / `ingenious_action_info` (NEVER read engine Java) |
| Correctness gate | `ingenious_testcase_validate` (the oracle) |

Rules that still bind (from §NEVER above): no hardcoded step inputs (use `Sheet:Column`),
distinct reusable vs test-case scenario names, tags preserved, shadow-DOM → `jsPath`,
mandatory multi-env detection. Enforce them **through the tool arguments** — e.g. pass
locator `jsPath:` to `ingenious_object_add`, pass `tags:` to `ingenious_testcase_create`.

On any tool error, follow `error.data.suggestions`; do not fall back to editing files by
hand. Finish with `ingenious_testcase_validate` returning `valid:true`.

## 1) Assessment Questions

- Source framework / POM style / number of tests?
- Data sources (`.properties/.json/.csv/.xlsx`) and wait strategy?
- Custom utilities, target browsers, parallel mode, API/DB dependencies, CI/reporting?

Decision path: `.feature` file -> parse; feature folder -> batch parse; Selenium
project -> extract page objects, locators, actions, test data.

## 2) Input Analysis & Detection

Classify the input, then survey it with native shell commands (no extra runtime).

- `.feature` file → **FeatureFile** (parse directly).
- Folder containing any `.feature` → **FeatureFolder** (batch parse).
- Folder with `*Page.java` / `*Test.java` → **SeleniumProject** (extract page
  objects, locators, actions, data).

macOS / Linux:
```bash
IN=path/to/input
find "$IN" -name '*.feature' | wc -l   # feature files
find "$IN" -name '*Test.java' | wc -l   # tests
find "$IN" -name '*Page.java' | wc -l   # page objects
find "$IN" \( -name '*.properties' -o -name '*.json' -o -name '*.csv' -o -name '*.xlsx' \) | wc -l
```

Windows (PowerShell):
```powershell
$IN = 'path\to\input'
(Get-ChildItem $IN -Recurse -Filter *.feature).Count
(Get-ChildItem $IN -Recurse -Filter *Test.java).Count
(Get-ChildItem $IN -Recurse -Filter *Page.java).Count
(Get-ChildItem $IN -Recurse -Include *.properties,*.json,*.csv,*.xlsx).Count
```

### Pre-scaffolding environment scan — MUST run before any scaffold or OR work

Run these commands on the source project root and record the result. If the output
contains **2 or more distinct environment prefixes** (`ACCP`, `TEST`, `DEV`, `PROD`,
`STAGING`, `QA`) paired with `URL`/`USERNAME`/`PASSWORD` constants, multi-env is
detected and `--with-env-helpers` MUST be used in the scaffold call.

macOS / Linux:
```bash
# Step 1 — locate all constants / config / settings files
find "$IN" \( -name '*Constants*.java' -o -name '*Config*.java' \
              -o -name '*Settings*.java' -o -name 'application*.properties' \
              -o -name 'application*.yml'  -o -name '.env' -o -name '.env.*' \)

# Step 2 — grep for environment-prefixed URL / credential constants
grep -rn 'URL\|USERNAME\|PASSWORD\|LOGIN_URL\|BASE_URL' "$IN" \
  --include='*.java' --include='*.properties' --include='*.yml' \
  | grep -iE '(ACCP|ACCEPT|TEST|DEV|PROD|STAGING|QA)_'
```

Windows (PowerShell):
```powershell
# Step 1 — locate all constants / config / settings files
Get-ChildItem $IN -Recurse | Where-Object {
  $_.Name -match 'Constants|Config|Settings' -or
  $_.Name -match '^application.*\.(properties|yml)$' -or
  $_.Name -match '^\.env'
} | Select-Object FullName

# Step 2 — grep for environment-prefixed URL / credential constants
Select-String -Path (Get-ChildItem $IN -Recurse -Include *.java,*.properties,*.yml) \
  -Pattern '(ACCP|ACCEPT|TEST|DEV|PROD|STAGING|QA)_.*(URL|USERNAME|PASSWORD)'
```

**Decision rule**:
- 0 or 1 environment prefix found → single-env → scaffold without `--with-env-helpers`;
  record `Environments modelled: single` in the summary report.
- ≥ 2 environment prefixes found → **multi-env detected** → scaffold MUST include
  `--with-env-helpers`; map each prefix to a GlobalData row (see §8 mapping table);
  record all `#ids` in the summary report.

## 3) Project Setup (scaffolder — pick the script for the OS)

Generate an independent project skeleton from the bundled template. Two native
scripts (no Node / extra runtime) — use the one matching the current OS:

macOS / Linux:
```bash
cd .github/skills/ingenious-ui-migrator/templates
./create-project.sh --name YourProjectName --projects-root ../../../../Projects
```

Windows (PowerShell):
```powershell
cd .github\skills\ingenious-ui-migrator\templates
.\create-project.ps1 -Name YourProjectName -ProjectsRoot ..\..\..\..\Projects
```

Options (bash uses `--flag value`; PowerShell uses `-Flag value`):
- `--no-samples` / `-NoSamples` — empty skeleton (no `Sample*` content).
- `--with-env-helpers` / `-WithEnvHelpers` — add `ReusableComponents/GetEnv/*`.
  **MUST** use this when the source project references more than one environment.
- `--with-db-helpers` / `-WithDbHelpers` — add `ReusableComponents/DatabaseConnection/*`.
- `--scenarios A,B` / `-Scenarios A,B` — pre-register scenarios.
- `--tags @x,@y` / `-Tags @x,@y` — pre-register tags.
- `--sync` / `-Sync` — **re-scan** the project and regenerate `.project` after
  you add YAML files (run whenever you add pages, reusables or test cases).

It creates: `ObjectRepository/{Web,Mobile,SAP,StructuredData}`,
`ReusableComponents`, `TestPlan`, `TestLab`, `TestData`, full `Settings/*`,
`Recording`, `api/*`, and a generated `.project` (schema 3.1.0).

## 4) Object Repository — `ObjectRepository/Web/<Page>.yaml`

One YAML file per page. Each element is keyed by a friendly name and carries a
**single** locator.

```yaml
page: LoginPage
scope: PROJECT          # PROJECT or SHARED
elements:
  Username:
    role: TEXTBOX;Username      # ROLE;AccessibleName  (getByRole)
  Login:
    role: BUTTON;Login
  Accept cookies:
    testId: accept              # getByTestId
  Remember Me:
    label: Remember me          # getByLabel
  Search:
    placeholder: Search         # getByPlaceholder
  Terms Link:
    text: Terms and Conditions  # getByText
  Brand Logo:
    altText: Company logo       # getByAltText
  Error Message:
    css: .alert-danger          # CSS fallback
  Legacy Field:
    xpath: //input[@name='legacy']                    # XPath fallback
  Shadow Element:
    jsPath: document.querySelector("body > app").shadowRoot.querySelector("#btn")  # shadow DOM
  Card Number:
    role: TEXTBOX;Card number
    frame: "#payment-iframe"    # element lives inside an iframe
```

Supported locator keys (one per element, preference top→bottom): `testId`,
`role` (`ROLE;Name`), `label`, `placeholder`, `text`, `altText`, `title`,
`css`, `xpath`, `chainedLocator`, `jsPath`. Optional per-element: `frame`,
`exact`, `description`. A `role` name part may embed a CSS selector, e.g.
`RADIO;#debt`.

**Shadow DOM / `jsPath` detection rule**: If a source Java `static final String`
contains a chain of `.shadowRoot.querySelector()` calls, it is a shadow DOM path
(Selenium stored it as a JS string because its native locators cannot pierce shadow
roots). MUST map to `jsPath:` and strip the leading `return ` keyword:

```yaml
# Source Java:
# public static final String BTN = "return document.querySelector(\"body > app\")"
#   + ".shadowRoot.querySelector(\"#btn\")";
Submit Button:
  jsPath: document.querySelector("body > app").shadowRoot.querySelector("#btn")
```

MUST NOT attempt to convert shadow DOM paths to `css:` or `xpath:`.

**CSS-over-XPath rule (MUST)**: Prefer `css:` with Playwright text pseudo-classes
over `xpath:`. Only fall back to `xpath:` when no CSS expression can target the
element. Playwright text engines:
- `article:has-text("Playwright")`        — ancestor that *contains* the text
- `#nav-bar :text("Home")`                — substring match
- `#nav-bar :text-is("Home")`             — exact match
- `#nav-bar :text-matches("reg?ex", "i")` — regex match

So `//tr[.//*[contains(normalize-space(.),'BE55')]]//input[@type='checkbox']`
becomes `tr:has-text('BE55') input[type='checkbox']`.

**Locator data rule (MUST)**: A locator value MUST NOT contain hard-coded test
data (account numbers, names, IDs, amounts, dates, etc.). Bake a `#token`
placeholder into the locator instead, and inject the real value at runtime from a
data sheet with a mandatory `setObjectProperty` step (see §5). Example OR element:

```yaml
Bank Account Checkbox:
  css: "tr:has-text('#accNo') input[type='checkbox']"   # #accNo replaced at runtime
```

MUST NOT write the literal into the OR file:

```yaml
# WRONG — hard-coded data in the locator
Bank Account Checkbox:
  xpath: "//tr[.//*[contains(normalize-space(.),'BE67 3803 8905 7893')]]//input[@type='checkbox']"
```

## 5) Reusable Components — `ReusableComponents/<Scenario>/<Name>.yaml`

Group related object-level steps into a reusable. Steps reference OR elements
via `object` + `reference: "[Project] <PageName>"`.

```yaml
schemaVersion: 1
testCase: SampleFlow          # name of the reusable
scenario: SampleScenario      # folder / scenario group
steps:
  - step: 1
    object: Username
    description: "Enter the value [<Data>] in the Field [<Object>]"
    action: Fill
    input: SampleData:Username
    reference: "[Project] SamplePage"

  - step: 2
    object: Login
    description: "Click the [<Object>] "
    action: Click
    reference: "[Project] SamplePage"

  - step: 3
    object: Welcome Header
    description: "Assert if [<Object>] is visible"
    action: assertElementIsVisible
    reference: "[Project] SamplePage"
```

Step keys: `step, object, description, action, input, condition, reference`
(optional `comment: true`, `breakpoint: true`, `hardAssertion: true`).

**Naming rule**: The `scenario:` value here MUST NEVER match any `scenario:` value
used in `TestPlan/`. Use a `Flow`-suffixed name — e.g. `LoginFlow`,
`SubscriptionFlow`, `CheckoutFlow`.

**Data rule**: `input:` MUST always be `Sheet:Column`. MUST NOT write
`input: "@<literal>"` for any data-driven value.

**Runtime locator-data rule (MUST)**: When an element's locator carries a `#token`
placeholder (see §4 Locator data rule), the value MUST be injected from a data
sheet via a `setObjectProperty` step placed immediately before the action step.
`setObjectProperty` is **mandatory** for every parameterised locator — never bake
the literal into the OR file, and never put data inside the locator at design time.

```yaml
  - step: 1
    object: Bank Account 01 Checkbox
    description: "Set object [<Object>] property  as [<Data>] at runtime"
    action: setObjectProperty
    input: DataSheet:AccountNumber
    condition: '#accNo'
    reference: "[Project] CreateSubscriptionRolePage"

  - step: 2
    object: Bank Account 01 Checkbox
    description: "Check the [<Object>] element with account number [<Data>]"
    action: Check
    reference: "[Project] CreateSubscriptionRolePage"
```

- `condition:` is the `#token` to replace; it MUST exactly match the token in the
  OR locator (`#accNo` here).
- `input:` is the `Sheet:Column` holding the real value.
- Every datatable column whose value ends up in a locator MUST become a data-sheet
  column consumed this way — this is how Gherkin datatable cells stay traceable.

**Manual marker rule (MUST)**: When a step cannot be auto-derived and needs human
attention, emit a marker step that **only** a
`action`. The `object`, `description`, and `input` fields MUST be omitted/empty so
the step renders in RED and clearly flags where work is needed. MUST NOT disguise a
marker as a real step (e.g. `General` / `pause` / `@0`).

```yaml
  # CORRECT — renders red, no-op, carries only the note
  - step: 7
    action: "MANUAL: configure per-asset power type (Asset 1 'Limited power' / Asset 2 'Consult & download Only') in the one-by-one dialog."
```

```yaml
  # WRONG — object/action/input populated; the marker no longer stands out
  - step: 7
    object: General
    action: pause
    input: "@0"
    comment: true
    description: "MANUAL: ..."
```

## 6) Test Cases — `TestPlan/<Scenario>/<TestCase>.yaml`

**Naming rule**: The `scenario:` value here MUST NEVER match any `scenario:` in
`ReusableComponents/`. Use a distinct, meaningful test-domain name —
e.g. `LoginSuite`, `SubscriptionTests`, `CheckoutSuite`.

A test case orchestrates reusables. Use `object: Execute` and
`action: <Scenario>:<ReusableName>`. Add direct object steps too if needed.

```yaml
schemaVersion: 1
testCase: SampleTestCase
scenario: SampleScenario
tags:
  - '@smoke'          # scenario-level tag from source
  - '@UX01'           # feature-level tag inherited by all test cases in that feature
steps:
  - step: 1
    object: Execute
    action: Common:Launch

  - step: 2
    object: Execute
    action: SampleScenario:SampleFlow
```

**Tag migration rules (MUST follow)**:
- Collect tags from both the `Feature:` line and the individual `Scenario:`/`Scenario Outline:` line.
- Merge them (feature-level tags first, then scenario-level) into the test case `tags:` list.
- Preserve **all** tags including `@Ignore`, numeric IDs (`@11306067`), and custom labels.
- Do **not** duplicate a tag that appears on both the feature line and a scenario line.
- Example source:
  ```gherkin
  @UX01 @MPSSUI @Ignore
  Feature: Create subscription
    @11306067
    Scenario: No PKI customer
  ```
  Resulting `tags:` in the test case YAML: `['@UX01', '@MPSSUI', '@Ignore', '@11306067']`.

## 7) Test Lab — `TestLab/<Release>/<Set>.yaml`

```yaml
schemaVersion: 1
name: SampleSet
release: SampleRelease
executions:
  - execute: true
    testScenario: SampleScenario
    testCase: SampleTestCase
    iteration: All
    status: NotExecuted
    browser: Chromium          # Chromium | Firefox | WebKit
    browserVersion: Default
    platform: Any
```

## 8) Test Data — CSV sheets in `TestData/`

`TestData/GlobalData.csv` is an **environment table** — one row per environment,
identified by a `GlobalDataID` (`#dev`, `#test`, `#acc`, etc.). Column names hold
every environment-specific value and MUST be **exactly** the same as the column
names in data sheets that reference them:

```csv
GlobalDataID,URL,Username,Password
"#dev",https://devapp.com,devuser,devpass
"#test",https://testapp.com,testuser,testpass
"#acc",https://accapp.com,accuser,accpass
```

Named data sheet — `TestData/<SheetName>.csv`, keyed by test-case context. When a
cell value is a GlobalData ID (e.g. `#dev`), the engine substitutes the matching
column value from GlobalData at runtime. Column names MUST match GlobalData exactly:

```csv
Scenario,Flow,Iteration,SubIteration,URL,Username,Password
SampleScenario,SampleTestCase,1,1,#dev,#dev,#dev
SampleScenario,SampleTestCase,2,1,#test,#test,#test
```

Rules:
- Steps MUST always use `Sheet:Column` (e.g. `SampleData:Username`).
- MUST NOT reference `#id` directly in a step `input:` field — GlobalData IDs are
  cell values in data sheets only.
- Column names in GlobalData and data sheets MUST be exactly the same (case-sensitive).
- Multiple iterations = multiple rows (Iteration 1, 2, …).
- If the source project references more than one environment, MUST populate GlobalData
  with one row per environment and scaffold with `--with-env-helpers`.

### Source constants → GlobalData row mapping

For each environment prefix detected in §2, create one GlobalData row using this
mapping:

| Source constant prefix | GlobalDataID |
|---|---|
| `ACCP_` / `ACCEPT_` | `#accp` |
| `TEST_` | `#test` |
| `DEV_` | `#dev` |
| `PROD_` / `PRD_` | `#prod` |
| `STAGING_` / `STG_` | `#staging` |
| `QA_` | `#qa` |

Column values per row:
- `URL` ← the matching `*_LOGIN_URL` or `*_URL` constant value (pick the most
  specific one — prefer `*_LOGIN_URL` over a bare `*_URL`).
- `Username` ← the matching `*_USERNAME` constant value.
- `Password` ← `PLACEHOLDER_<ENV>_DO_NOT_COMMIT` — NEVER copy the literal password.
- Any other env-specific column (e.g. `WorkLocation`) follows the same pattern;
  set to an empty string or a sensible placeholder if unknown.

Example GlobalData for a project with ACCP + TEST environments:
```csv
GlobalDataID,URL,Username,Password,WorkLocation
"#accp",https://accp.iris.ing.net/assisted/dashboard/home,TD17DS,PLACEHOLDER_ACCP_DO_NOT_COMMIT,393180
"#test",https://test.iris.ing.net/assisted/dashboard/home,TD17DS,PLACEHOLDER_TEST_DO_NOT_COMMIT,393180
```

Data sheet rows MUST use `#accp` / `#test` / etc. as cell values for every
environment-specific column so the engine substitutes the real value at runtime:
```csv
Scenario,Flow,Iteration,SubIteration,URL,Username,Password
MyScenario,MyFlow,1,1,#accp,#accp,#accp
MyScenario,MyFlow,2,1,#test,#test,#test
```

## 9) Selenium / Gherkin -> INGenious Action Map

| Selenium / Gherkin | object | action | input |
|---|---|---|---|
| `driver.get(url)` / `Given navigate` | `Browser` | `Open` | `Sheet:URL` |
| `sendKeys(text)` | `<Element>` | `Fill` | `Sheet:Col` |
| `clear()` | `<Element>` | `Fill` | `@` |
| `click()` / `submit()` | `<Element>` | `Click` | - |
| `selectByVisibleText()` | `<Element>` | `SelectSingleByText` | `Sheet:Col` |
| check / select radio | `<Element>` | `Check` | - |
| `getText()` | `<Element>` | `getText` | - |
| `getAttribute(attr)` | `<Element>` | `getAttribute` | `@attr` |
| `isDisplayed()` / `Then verify visible` | `<Element>` | `assertElementIsVisible` | - |
| `isEnabled()` | `<Element>` | `assertElementIsEnabled` | - |
| `WebDriverWait().until(visible)` | `<Element>` | `waitForElementToBeVisible` | - |
| page load wait | `Browser` | `WaitforLoadState` | - |
| screenshot | `Browser` | `TakePageScreenshot` | - |
| `Thread.sleep(ms)` (avoid) | `General` | `pause` | `@ms` |
| key press `Keys.ENTER` | `<Element>` | `KeyPress` | `@enter` |
| set locator at runtime | `<Element>` | `setObjectProperty` | `Sheet:Col` (+ `condition`) |
| store/add variable | `General` | `AddVar` / `AddGlobalVar` | value (+ `condition` = var) |

## 10) Naming + .project metadata

- Object naming: input `{Purpose}Field`/role name, button `{Action}`, dropdown
`{Purpose}Dropdown`, message `{Type}Message`.

`.project` (schema `3.1.0`) is **generated** by the scaffolder. After adding
YAML pages/reusables/test cases, regenerate it (use the OS-matching script):
```bash
# macOS / Linux
./create-project.sh --name YourProjectName --projects-root ../../../../Projects --sync
# Windows (PowerShell)
.\create-project.ps1 -Name YourProjectName -ProjectsRoot ..\..\..\..\Projects -Sync
```
It scans `TestPlan/*.yaml` (→ `testcase` entries) and
`ReusableComponents/*.yaml` (→ `reusable` entries), then rebuilds `_meta`
(scenarios + tags) and `data` (`Scenario#Name` ids with tags/attributes).

## 11) Validation Checklist

- Project scaffolded via `create-project.sh`/`create-project.ps1`; `.project` is `version: "3.1.0"`.
- Each OR page is `ObjectRepository/Web/<Page>.yaml` with `page`/`scope`/`elements`.
- Shadow DOM elements use `jsPath:` with the leading `return ` stripped — not `css:`/`xpath:`.
- Locators prefer `css:` + Playwright pseudo-classes (`:has-text`, `:text`, `:text-is`,
  `:text-matches`); `xpath:` only when no CSS expression works.
- No locator contains hard-coded test data; every parameterised locator uses a `#token`
  placeholder injected at runtime via a mandatory `setObjectProperty` step from a data sheet.
- Manual markers use **only** a `action`; `object`/`description`/`input`
  are empty (never `General`/`pause`/`@0`).
- Reusables under `ReusableComponents/<Scenario>/`, test cases under
  `TestPlan/<Scenario>/`, test lab under `TestLab/<Release>/`, all valid YAML.
- No `scenario:` name is shared between any reusable and any test case.
- Step `reference` matches an existing OR page (`[Project] <PageName>`).
- Every step with a data value uses `Sheet:Column` — no `@<literal>` anywhere.
- No step `input:` contains a GlobalData ID (`#dev`, `#test`, etc.) directly.
- **ENV-GATE** (must be confirmed before any OR/reusable/test-case work begins):
  Ran the pre-scaffolding environment scan (§2). Result must be one of:
  - **Single-env** → confirmed no multi-env signals; summary reports `single`.
  - **Multi-env** → confirmed: `--with-env-helpers` used; GlobalData has one row per
    detected environment; every `*_PASSWORD` constant is `PLACEHOLDER_<ENV>_DO_NOT_COMMIT`;
    no environment-specific URL, username, or password appears anywhere except in
    GlobalData/data-sheet cells.
- GlobalData has one row per environment; data sheet column names match GlobalData exactly.
- `TestData` sheets exist with matching `Scenario,Flow,Iteration,SubIteration` keys.
- Ran `--sync` so `.project` lists every reusable and test case.
- **Tags**: every tag from the source (feature-level + scenario-level) is present in
  the corresponding test case `tags:` list. All tags are registered in `.project`.

Common fixes: object not found → check element name + `reference` page; data not
substituting → check `Sheet:Column` / `#id` and the data-sheet keys; flaky →
prefer `role`/`testId` locators and explicit `waitForElementToBeVisible`; wrong
action → use the map above.

## 12) Quick Commands

macOS / Linux:
```bash
# Scaffold
./create-project.sh --name YourProjectName --projects-root ../../../../Projects
# Regenerate .project after adding YAML files
./create-project.sh --name YourProjectName --projects-root ../../../../Projects --sync
# Count Gherkin scenarios
grep -c -E '^[[:space:]]*Scenario' path/to/file.feature
```

Windows (PowerShell):
```powershell
# Scaffold
.\create-project.ps1 -Name YourProjectName -ProjectsRoot ..\..\..\..\Projects
# Regenerate .project after adding YAML files
.\create-project.ps1 -Name YourProjectName -ProjectsRoot ..\..\..\..\Projects -Sync
# Count Gherkin scenarios
(Select-String -Path path\to\file.feature -Pattern '^\s*Scenario').Count
```

## 13) Migration Summary Report

**MUST produce this report at the end of every migration** (print it as a fenced
code block so it is easy to copy). Never skip or abbreviate it.

```
╔══════════════════════════════════════════════════════════════╗
║              INGenious Migration Summary Report              ║
╠══════════════════════════════════════════════════════════════╣
║ Source file(s)   : <path(s) migrated>                        ║
║ Target project   : Projects/<ProjectName>                    ║
╠═════════════════════════╦════════════════════════════════════╣
║ Features (feature files) ║  <N>                              ║
║ Test Cases migrated      ║  <N>  (Scenario: X, Outline: Y)   ║
║ Reusable flows created   ║  <N>                              ║
║ OR pages created         ║  <N>                              ║
║ Tags preserved           ║  <N> unique tags across all TCs   ║
╠═════════════════════════╩════════════════════════════════════╣
║ PERFECT MIGRATIONS                                           ║
║  ✓ <TestCaseName> — all steps mapped, all data externalised  ║
║  ✓ ...                                                       ║
╠══════════════════════════════════════════════════════════════╣
║ NEEDS REVISITING                                             ║
║  ⚠ <TestCaseName>                                            ║
║    - <reason: e.g. "3 steps marked MANUAL — no clean OR      ║
║      locator found for summary data-table verification">     ║
║  ⚠ ...                                                       ║
╠══════════════════════════════════════════════════════════════╣
║ CONFIDENCE   <NN>%                                           ║
║  Basis: <fully-mapped steps> / <total steps> steps mapped    ║
║  automatically; <M> step(s) left as MANUAL markers.          ║
╠══════════════════════════════════════════════════════════════╣
║ ADDITIONAL DETAILS                                           ║
║  • Data sheets created  : <list sheet names>                 ║
║  • Shadow DOM elements  : <N> (mapped to jsPath)             ║
║  • Environments modelled: <list #ids or "single-env">        ║
║  • Tags migrated        : <list all unique tags>             ║
║  • Manual markers       : <N> steps need human review        ║
║  • Scaffolder used      : create-project.sh / .ps1           ║
╚══════════════════════════════════════════════════════════════╝
```

**Confidence % calculation**:
```
confidence = round( (total_steps - manual_steps) / total_steps * 100 )
```
A "MANUAL" step is any step where no clean
INGenious action or locator could be derived automatically. Such steps MUST have
empty `object`/`description`/`input` and carry the note only in `action` (see §5
Manual marker rule). Perfect score = 100%.

**Categorisation rules**:
- **PERFECT** — test case has zero MANUAL marker steps AND every `input:` value is
  externalised to a CSV sheet AND all source tags are present in `tags:`.
- **NEEDS REVISITING** — test case has ≥1 MANUAL marker step, OR has a locator that
  could not be classified (fell back to a broad css/xpath), OR has a multi-row data
  table that was only partially mapped. State the specific reason for each item.

Do not output: commentary, repeated input, unchanged files, or unnecessary
full-file regeneration.
