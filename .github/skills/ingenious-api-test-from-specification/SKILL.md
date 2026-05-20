---
name: ingenious-api-test-from-specification
description: 'Create a new INGenious API test case from a business flow. Use when user provides API workflow steps and wants Scenario -> TestCase CSV, reusable API components, and data sheets wired together.'
argument-hint: 'API business flow + scenario name + testcase name + endpoints/methods/assertions'
user-invocable: true
requires:
  ingenious: ">=3.0.0 <3.1.0"
metadata:
  author: ingenious-team
  category: test-generation
---

**🔴 MANDATORY: This is an INTERACTIVE workflow**

- When asking for inputs, **present suggested values as selectable options** when possible, and allow free text input when needed.
- **Always verify** output artifacts after creation or fixes by running the test case and checking for unknown action errors, missing object errors, and assertion results.
- **Always clean up** resources after execution; Do not leave terminal processes, files, or environment changes without user confirmation. 
- **Always show** the procedure step number and description to the user before executing it.
- **Always show** the terminal that you are using for commands and explain what you are doing.
- **Always show** the command to run the new test case after creation, with resolved placeholders.
- **Always validate** that actions exist before adding them to any CSV file.
- **Never assume** missing inputs; ask concise follow-up questions to fill gaps
- **Never modify** unrelated files; only create/update what is necessary for the requested test case
- **Do not** consult external documentation or resources; rely solely on the provided references and project structure or use existing project artifacts as templates.

# Test Case From API Business Flow

Create a new API test case inside an existing or new scenario in any INGenious project, following the project pattern:
- Build or reuse reusable API components first
- Reference those components from the main test case CSV
- Build or update data sheets in TestData
- Use INGenious Webservice actions from Engine command definitions
- Keep assertions explicit for status, payload, and critical fields

Project root for this skill:
- Projects/<ProjectName>

Reference patterns are available in separate files for progressive loading:
- [TestPlan CSV (orchestration)](./references/testplan-csv-pattern.md) - Main test case structure
- [Reusable Component (create/POST)](./references/reusable-create-pattern.md) - Creating resources
- [Reusable Component (validate/GET)](./references/reusable-validate-pattern.md) - Validating resources
- [TestData CSV](./references/testdata-csv-pattern.md) - Data sheet structure
- [.project metadata](./references/project-metadata-pattern.md) - Registration file

Load only the specific pattern needed for the current authoring step.

## When To Use
- User provides API workflow steps and expected outcomes
- User asks to add a new API test case under a scenario
- User wants reusable API flow components before main testcase composition
- User wants data-driven API test design in INGenious CSV format

## Inputs To Collect
Collect these fields before writing files:
1. Project name (for example: Tutorial)
2. Scenario name
3. Test case name
4. API flow steps (ordered)
5. Reusable flow split (proposed components)
6. API request matrix (MANDATORY per step) with separate fields:
   - Endpoint
   - Method
   - Headers
     - present suggested header keys as selectable options when possible (for example Content-Type, Authorization, etc.)
     - format key-value pairs as "HeaderKey=HeaderValue"
   - Payload
   - Expected Status Code
   - Assertions
7. Test data fields and sample row values
8. Runtime needs (proxy/SSL/context requirements if special)

### Mandatory Input Gate (Do Not Proceed)
- The agent MUST ask for and receive these six inputs as separate values for each API step before creating or editing any artifact:
  - Endpoint
  - Method
  - Headers
  - Payload
    - For POST/PUT/PATCH/DELETE requests, ask user to choose payload pattern:
      - **Pattern 3A (inline JSON/XML)**: Build JSON/XML inline with embedded `{Sheet:Column}` variables (recommended for small payloads with 1-5 fields)
      - **Pattern 3B (datasheet column)**: Store entire payload in a datasheet column `{Sheet:RequestBody}` (recommended for complex payloads with 6+ fields or nested structures)
    - Present both options as selectable choices when collecting Payload input
    - If user provides raw JSON/XML, clarify which pattern to use and identify which fields should be variables
  - Expected Status Code
  - Assertions
- Do not merge these into one broad question unless all six are still captured distinctly in the final response map.
- If any of the six values is missing, ambiguous, or placeholder-like (for example "TBD", "same as before" without explicit value), STOP and ask follow-up questions.
- Do not generate CSV/YAML/TestData/.project changes until the mandatory input gate is satisfied.
- If the user provides endpoint only, ask specifically for payload, headers, status code, and assertions before proceeding.

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
- Data sheets: `Projects/<ProjectName>/TestData/<Sheet>.csv`

Optionally update:
- `Projects/<ProjectName>/.project` only if scenario metadata management is explicitly required
- `Projects/<ProjectName>/Settings/API/*.properties` only when API run behavior needs changes
- `Projects/<ProjectName>/api/collections/*.json` only if user explicitly asks for API tester collection sync
- `Projects/<ProjectName>/api/environments/*.json` only if user explicitly asks for API tester environment sync

If Project folder does not exist:
- Create `Projects/<ProjectName>` with minimum scaffold:
  - `ReusableComponents`
  - `Settings`
  - `Settings/API/default.properties`
    - see - [API default properties](./references/settings-api-default.md)
  - `TestData`
  - `TestPlan`
  - `Results`
  - `Recording`
  - `api/collections`
  - `api/environments`
  - `api/history`
- Create `Projects/<ProjectName>/.project` from an existing project template if available.

## Preflight Checks
Run these checks before creating or editing API flow artifacts:
1. Run backup process before making any changes, if available in the workspace, to allow easy rollback in case of issues.
1.1 Show a message to the user that a backup is being created before proceeding with any file changes.
1.2. Run zip backup of the entire `Projects/<ProjectName>` folder with a datetimestamp and the session hash in the filename, and store it in a `Backups` folder at the workspace root.
1.3. Create just one backup per session, even if multiple iterations of flow creation or edits are needed; do not create a new backup for each edit within the same session to avoid excessive storage use.
1.4. if backup fails, report the failure and halt further changes to prevent data loss.
- for example: `Backups/<ProjectName>_backup_<datetimestamp>.zip`
- For every session, do not overwrite existing backups; create a new one with a unique datetimestamp and session hash

2. Baseline runtime health
- Run one known testcase in the target project to confirm core webservice actions resolve.
- Minimum baseline actions: `setEndPoint`, `addHeader`, one request action (`getRestRequest` or `postRestRequest`), and `assertResponseCode`.
- If baseline fails with broad unknown-action errors, stop flow generation and report an environment/runtime blocker.

3. Execution mode confirmation
- Confirm preferred run command in this workspace (`./ingenious.command`).
- Confirm whether browser argument is required by the local runtime wrapper; if unknown, default to `-browser "No Browser"`.

4. Project metadata behavior
- Check whether the project requires explicit metadata registration in `.project` for new scenarios/reusables/testcases.
- If required, plan metadata updates as part of the change.

5. Project selection validation
- If Project name was not explicitly provided in the request, run the Project Selection Rule first.
- Confirm the selected project exists, or scaffold it if user selected `Create new project`.

## Procedure
1. Run mandatory input interview first
- Collect Endpoint, Method, Headers, Payload, Expected Status Code, and Assertions as separate inputs for each API step.
- Confirm the final per-step request matrix back to the user when multi-step flows are involved.
- Only continue to authoring after all mandatory inputs are complete.

2. Parse and normalize API business flow
- Convert user narrative into explicit API actions and assertions.
- Identify shared sequences suitable for reusability (auth setup, create entity, update entity, validation, cleanup).

3. Build the Action dictionary from webservice Java class
- Source valid Action values from methods annotated with `@Action` in:
  - `Engine/src/main/java/com/ing/engine/commands/webservice/Webservice.java`
- Parse pattern: `@Action(...)` followed by `public void <methodName>()`
- Use method names as canonical Action values in CSV rows.
- Keep existing project-typical actions when present:
  - `setEndPoint`, `addHeader`, `addURLParam`
  - `getRestRequest`, `postRestRequest`, `putRestRequest`, `patchRestRequest`, `deleteRestRequest`, `deleteWithPayload`
  - `assertResponseCode`, `assertResponsebodycontains`
  - `assertJSONelementEquals`, `assertJSONelementContains`, `assertXMLelementEquals`, `assertXMLelementContains`
  - `storeJSONelementInDataSheet`, `storeXMLelementInDataSheet`, `storeResponseBodyInDataSheet`, `storeHeaderByNameInDatasheet`
- Allow `Execute` only for orchestration steps in the main test case.

4. Build Object Repository YAML for JSONPath/XPath elements if needed
- Source valid Action values from methods annotated with `@Action` in:
  - `Engine/src/main/java/com/ing/engine/commands/structuredData/StructuredData.java`
- If any assertion uses JSONPath or XPath, create or update a YAML file under `ObjectRepository` to define those elements.
- Follow the pattern in [Structured Data](./references/structured-data-or.md) for JSONPath/XPath element definitions.
- For a different part of the json/xml response, create a new element with a unique name and the corresponding path expression within the same YAML file.
- Reference those elements in the assertion steps using the defined element names.
- For example, if the user wants to assert `$.customer.name` equals "John", then in the test case CSV, the assertion step would be like:
  - `<stepNumber>,<elementName>,Assert JsonPath Result Contains,assertJsonPathResultContains,@John,,[Project] <pagename>`

5. Design reusable API components first
- Create or reuse flow CSVs under `ReusableComponents`.
- Keep each component cohesive and short (about 2-12 steps).
- For API action rows, enforce:
  - `ObjectName=Webservice`
  - `Action` uses webservice action dictionary values
- For assertion rows, use action dictionary values from Structured Data if JSONPath/XPath is involved, otherwise use Webservice assertion actions.
- Never leave an empty `Action` column.

6. Define endpoint and payload parameterization
- See [input-field-patterns.md](./references/input-field-patterns.md) for comprehensive input field syntax guidance.
- Use `@` prefix for literal values in Input when project style follows that pattern (for example `@http://localhost:3000/customers`).
- Use `{Sheet:Column}` format (with braces) for data-driven values consistently.
- Map mandatory inputs to step columns explicitly:
  - Endpoint -> `setEndPoint` Input
  - Method -> request Action (`getRestRequest`/`postRestRequest`/`putRestRequest`/`patchRestRequest`/`deleteRestRequest`)
  - Headers -> `addHeader` Input (one row per header when multiple headers exist)
  - Payload -> request action Input (blank only when method does not require payload)
    - **Pattern 3A (inline)**: Build JSON/XML inline with embedded variables: `"{ \"name\": \"{API:CustomerName}\", \"country\": \"{API:Country}\" }"`
    - **Pattern 3B (datasheet)**: Reference full payload column: `{API:RequestBody}` and populate corresponding TestData column with complete JSON/XML
    - Choose Pattern 3A for small payloads (1-5 fields); Pattern 3B for complex payloads (6+ fields, nested structures)
  - Expected Status Code -> `assertResponseCode` Input
  - Assertions -> one or more assertion rows (`assertResponsebodycontains`, `assertJSONelementEquals`, `assertJSONelementContains`, `assertXMLelementEquals`, `assertHeaderValueEquals`, etc.)
- For JSONPath/XPath assertions, use `Condition` for the path expression and `Input` for expected value.
- Use `storeJSONelementInDataSheet` and related store actions to carry IDs across steps.

7. Build or update test data sheets
- Add/extend CSVs under `TestData` with columns required by reusable flows.
- Include `Scenario,Flow,Iteration,SubIteration` where relevant to existing pattern.
- Keep references consistent with `Sheet:Column` syntax from steps.
- Avoid trailing spaces in column names and references.

8. Compose the main test case
- In `TestPlan/<Scenario>/<TestCase>.csv`, orchestrate with `Execute` steps.
- For orchestration rows, enforce this schema:
  - `ObjectName=Execute`
  - `Action=<ReusableScenario>:<ReusableName>`
  - `Input` must be blank for Execute rows
- Example valid row:
  - `1,Execute,Run customer creation reusable,API Common:Create Customer,,,`
- Example invalid row (do not use):
  - `1,Execute,Run customer creation reusable,Execute,API Common:Create Customer,,`
- Reference reusable components in execution order.

9. Add critical API assertions
- Ensure final business outcome is asserted (not only request calls).
- Include at least one status code assertion and one payload/header assertion for critical outcomes.

10. Validate consistency before finishing
- Project path exists or was scaffolded correctly under `Projects/<ProjectName>`.
- Every non-Execute action exists in `Webservice.java` `@Action` method names.
- Every Execute reference maps to an existing reusable component.
- Every `Input` reference like `Sheet:Column` exists in TestData headers.
- No blank `Action` values.
- API rows use `ObjectName=Webservice`; orchestration rows use `ObjectName=Execute`.
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
- If requested action is not found in webservice action definitions:
  - Map to the closest available action, document the mapping, and ask for confirmation.
- If assertion confidence is low due to ambiguous response shape:
  - Add TODO placeholder only when unavoidable and explicitly flag it to the user.

## Quality Gates
A change is complete only when all are true:
1. Main test case uses Execute steps to reusable components.
2. Reusable component actions are non-empty and valid.
3. Reusable component Action values come from webservice `@Action` method names.
4. API rows use `ObjectName=Webservice` and orchestration rows use `ObjectName=Execute`.
5. Data references resolve to existing sheets/columns.
6. At least one business outcome assertion exists (status + payload/header).
7. Execute row schema is valid (`ObjectName=Execute`, reusable reference in `Action`, `Input` empty).
8. Baseline testcase runtime is healthy or blocker is explicitly reported before generation.
9. No unrelated files are modified.
10. Mandatory input gate was satisfied before authoring (Endpoint, Method, Headers, Payload, Status Code, Assertions captured separately for each API step).
11. Payload patterns are correctly formatted:
    - Pattern 3A: Embedded variables use `{Sheet:Column}` with braces inside JSON/XML strings
    - Pattern 3B: Full payload reference uses `{Sheet:Column}` format and corresponding TestData column exists with complete payload content

## Testing Strategy
After creating or updating an API flow, validate it in seven layers.

1. Static pre-run validation
- Validate every non-Execute Action against the webservice Action dictionary sourced from:
  - `Engine/src/main/java/com/ing/engine/commands/webservice/Webservice.java`
- Validate each Execute reference points to an existing reusable component file.
- Validate each `Sheet:Column` input exists in TestData CSV headers.
- Validate API rows use `ObjectName=Webservice`.
- Fail fast on blank Action cells.

2. Baseline environment sanity
- Execute one known-good API testcase from the same project.
- If baseline also fails with unknown-action errors, classify as environment/runtime blocker and stop authoring-level debugging.

3. Structural smoke execution
- Run the new testcase once in a single execution mode.
- Confirm no runtime parsing errors such as unknown actions or missing data columns.
- Confirm reports are produced under:
  - `Projects/<ProjectName>/Results`

4. Assertion verification
- Verify at least one business-critical assertion is executed.
- Prefer explicit assertion actions (`assertResponseCode`, `assertJSONelementEquals`, `assertJSONelementContains`, `assertXMLelementEquals`, `assertHeaderValueEquals`) over passive request-only coverage.
- Confirm pass/fail for those assertions in console and report artifacts.

5. Stability and regression checks
- Re-run the same testcase at least 2 times to detect flaky behavior.
- Add one negative-path variant when the API flow allows it (for example invalid auth or invalid payload).
- If reusable components were modified, run at least one existing testcase that depends on them to catch regressions.

6. Failure triage rules
- Unknown action errors in both new and baseline testcase:
  - Treat as environment/runtime issue.
- Unknown action errors only in new testcase:
  - Treat as flow authoring/schema issue.
- Missing data reference errors only in new testcase:
  - Treat as TestData/header issue.
- HTTP assertion failures with valid schema:
  - Treat as endpoint behavior/test-data issue.

7. Completion criteria for testing
- Test run has zero unknown-action or empty-action errors.
- All newly added assertions execute and pass for happy-path data.
- Expected negative-path behavior is observed when applicable.
- No unrelated existing testcase regressed due to reusable-component changes.
- If baseline runtime is unhealthy, stop and return blocker diagnostics instead of continuing flow edits.

## Execute Command Template
After creating an API test flow, print and use an execution command with resolved placeholders.

1. Scenario and testcase execution (design mode)
- Command template:
  - `./ingenious.command -run -project_location "Projects/<ProjectName>" -scenario "<Scenario>" -testcase "<TestCase>" -browser "<Browser>"`
- Alternative wrapper command (if used in workspace):
  - `./Run.command -run -project_location "Projects/<ProjectName>" -scenario "<Scenario>" -testcase "<TestCase>" -browser "<Browser>"`
- Default browser if not provided by user:
  - `Chromium`

2. Release and testset execution (batch mode)
- Command template:
  - `./ingenious.command -run -project_location "Projects/<ProjectName>" -release "<Release>" -testset "<TestSet>" -browser "<Browser>"`

3. Optional execution flags
- Headless:
  - `-op_setHeadless true`
- Tag filter:
  - `-tags "<Tag1,Tag2>"`

4. Post-run checks
- Verify latest run artifacts under:
  - `Projects/<ProjectName>/Results`
- Verify console output and report for:
  - Unknown action errors
  - Missing data reference errors
  - Final API business assertion status
- If broad unknown-action errors also occur in known baseline testcase:
  - Report environment/runtime blocker and halt further flow modifications.
- Include the resolved run command in the final summary to user.

## Output Summary Format
When done, report:
- Files created/updated
- Reusable components added/reused
- Assertions added
- Data sheets/columns added
- Any assumptions or pending payload/assertion confirmations

## Reference Patterns

Canonical format examples are available in individual reference files (load only what's needed):

- [TestPlan CSV Pattern](./references/testplan-csv-pattern.md) - Main test case orchestration with Execute steps
- [Reusable Create Pattern](./references/reusable-create-pattern.md) - Create resources with POST
- [Reusable Validate Pattern](./references/reusable-validate-pattern.md) - Validate resources with GET
- [TestData CSV Pattern](./references/testdata-csv-pattern.md) - Data sheet structure and iteration
- [Project Metadata Pattern](./references/project-metadata-pattern.md) - .project file registration

## Example Invocation Prompts
- `/test-case-from-api-business-flow Project: Tutorial. Scenario: API Testing. Testcase: Create and verify customer. Business flow: create customer -> fetch customer -> assert name and response code.`
- `/test-case-from-api-business-flow Add scenario "API Regression" testcase "Create account and transaction" using reusable API components first and data-driven IDs.`
- `/test-case-from-api-business-flow Scenario: API Testing. Testcase: Delete transaction. Business flow: create transaction -> delete -> verify not found.`
- `/test-case-from-api-business-flow Create a failed-auth testcase with invalid token data and explicit status/body assertions.`
