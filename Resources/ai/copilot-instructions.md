<!-- Append to your repository's .github/copilot-instructions.md (or equivalent). -->

## INGenious test authoring

This repository contains INGenious test-automation projects. When creating or
editing INGenious artefacts, always follow these conventions:

- **Step inputs**: hard-coded values are `@`-prefixed (`@200`); data-driven
  values reference a data sheet as `Sheet:Column`; API payload bodies are raw
  (not `@`-prefixed) and may embed `{Sheet:Column}` tokens; GlobalData `#id`
  values belong in data-sheet cells only, never in step inputs; `%var%` are
  runtime variables.
- **Objects**: Object Repository references (`Page.element`), `Webservice` for
  API steps, or `Execute` for reusable calls — never `@`-prefixed.
- **Naming**: TestPlan scenarios are business flows; test cases are user
  journeys. ReusableComponents scenarios are user-intent groups (`Common`,
  `Flow`); each reusable is one user intent. Scenario names must be unique
  across TestPlan/ and ReusableComponents/.
- **Composition**: test cases call reusables with
  `object: Execute`, `action: <ReusableScenario>:<ReusableName>`,
  `reference: "[Project]"`.
- **Workflow**: discover real action names first (never invent them); create
  with `@literal` values; then parameterize into data sheets; then validate
  and run.
- **Quality**: no fixed sleeps (use `waitFor*`); end every test case with an
  assertion; never store plaintext passwords — use
  `PLACEHOLDER_<ENV>_DO_NOT_COMMIT` placeholders.
