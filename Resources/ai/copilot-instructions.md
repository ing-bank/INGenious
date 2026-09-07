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
- **Browser flow discovery**: when a browser test needs objects/locators that
  are not yet in the Object Repository, route it deterministically through
  `@playwright/cli` — confirm intent, then `ingenious_browser_discover` (pass
  the url and the prompt verbatim), explore with `ingenious_browser_session_do`
  using only refs from the returned snapshots (each call waits for the CLI),
  then `ingenious_browser_session_save` to translate discovered locators into
  WebOR objects and linked steps. Never hand-write locators or guess the flow;
  the exploration outcome is the only non-deterministic part.
- **Performance (k6)**: functional test cases and HAR recordings export to k6
  load tests via `ingenious_perf_export` (`type=http` for API/HAR, `type=browser`
  for web flows). ALWAYS `ingenious_perf_validate` (1 VU, 1 iteration) before a
  load run. Load shape comes from profiles (smoke/average/stress/spike/soak or
  `Performance/profiles/*.yaml`) — never hand-edit generated options. Long runs:
  `ingenious_perf_run_async` + poll `ingenious_perf_status`; the returned
  `dashboardUrl` serves live graphs. For HAR exports pass `autoCorrelate=true`
  so dynamic tokens become reviewable correlation rules in `Performance/rules/`;
  credential headers are scrubbed and only re-enter scripts via those rules.
  Gate regressions with `ingenious_perf_compare` (baseline vs candidate run ids).
- **Quality**: no fixed sleeps (use `waitFor*`); end every test case with an
  assertion; never store plaintext passwords — use
  `PLACEHOLDER_<ENV>_DO_NOT_COMMIT` placeholders.
