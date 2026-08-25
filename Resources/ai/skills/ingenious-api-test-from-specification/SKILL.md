---
name: ingenious-api-test-from-specification
description: 'Create an INGenious 3.1.x API test by orchestrating the ingenious_* MCP tools using a collection-first pipeline: ingest APIs as a collection, run/test them against a live environment, then convert the observed run into a YAML INGenious test. Use when the user provides an OpenAPI spec, Postman/Bruno collection, curl, HAR, or an API business flow.'
argument-hint: 'API spec/collection/flow + scenario name + testcase name + endpoints/methods/assertions'
user-invocable: true
version: "2.0.0"
requires:
  ingenious: ">=3.1.0 <3.2.0"
metadata:
  author: ingenious-team
  category: test-generation
---

# API Test From Specification (tool-first, collection-first, YAML)

Author an API test in an INGenious 3.1.x project by **calling `ingenious_*` MCP tools**.
The engine owns file formats (YAML test cases, CSV data) and validates against the live
action catalog. Prefer the **collection-first pipeline** so the APIs are exercised
*before* they become a test, and assertions are seeded from **observed** responses (not
guessed).

## Non-negotiable rules

- **Tool-first.** Every mutation goes through an `ingenious_*` tool. Do NOT hand-write
  `TestPlan/**`, `ObjectRepository/**`, `TestData/**`, `api/**`, or `.project`.
- **Discover, don't derive.** Get valid actions from `ingenious_action_search` /
  `ingenious_action_info`. NEVER read `Webservice.java` / `StructuredData.java`.
- **Assertions from evidence.** When a collection run exists, seed status + field
  assertions from the captured response, not from model reasoning.
- **Validate via engine.** Correctness = `ingenious_testcase_validate`.
- **Dry-run first.** `dryRun:true` on create/add; `ingenious_run_dry` before `ingenious_run`.
- **On tool error, use `error.data.suggestions`.** Never hand-fix files.

## Inputs (ask only if missing)

1. Project (if absent: `ingenious_project_list` → pick, or offer create)
2. Source: OpenAPI spec / Postman / Bruno / curl / HAR file path, OR a described flow
3. Scenario name, Test case name
4. Environment: base URL + any auth/vars (for the collection run)
5. Data fields + sample values (if data-driven)

For a described flow with no artifact, collect per API step (separate values, do not
merge): **Endpoint, Method, Headers, Payload, Expected Status, Assertions.**

## Preferred pipeline — collection-first (when `apicollection_*` tools are available)

Check availability with `ingenious_action_categories` / the tool list. If the
`apicollection_*` tools exist, run this fixed 3-stage playbook:

1. **Ingest as a collection (Stage 1)**
   - `ingenious_apicollection_import` `{project, name, file|source}` → persists
     `api/collections/<name>.json`. Review with `ingenious_apicollection_show`.
   - `ingenious_apicollection_env_set` `{project, env, baseUrl, vars, auth}` if needed.

2. **Test from the collection (Stage 2)**
   - `ingenious_apicollection_run` `{project, name, env}` → executes requests against the
     live environment, captures status/headers/latency/body into `api/history/<run>.json`.
   - Use `ingenious_apicollection_request_run` for a single ad-hoc request.
   - Inspect the run; confirm endpoints behave. This is where expected values come from.

3. **Convert to an INGenious test (Stage 3)**
   - `ingenious_apicollection_to_testcase` `{project, name, run, scenario, testcase,
     dryRun:true}` → creates YAML reusable components per request + an `Execute` test
     case + a data sheet, seeding `assertResponseCode` and `assertJSONelementEquals` from
     the observed run. Re-run without `dryRun` to commit.

4. **Validate + smoke**
   - `ingenious_testcase_validate` → `valid:true`.
   - `ingenious_run_dry` → `ingenious_run` `{browser:"No Browser"}` →
     `ingenious_report_latest` / `ingenious_report_failures`.

## Fallback — one-shot conversion (until `apicollection_*` tools ship)

If the `apicollection_*` tools are not available, use the existing generators. This
skips the "test-from-collection" stage — **say so explicitly** in the summary.

- OpenAPI spec → `ingenious_gen_from_openapi` `{project, file, scenario}` (one test per
  operation).
- HAR capture → `ingenious_gen_from_har` `{project, file, scenario, urlFilter}`.
- Postman/Bruno/curl → `ingenious_import_postman` / `ingenious_import_bruno` /
  `ingenious_import_curl`.
- Described flow with no artifact → `ingenious_gen_testcase` with an API archetype
  (`api-get`, `api-post`, `api-json-verify`, `e2e-ui-then-api`), then refine with
  `ingenious_testcase_add_step` / `ingenious_testcase_edit_step`.

Then run the same **Validate + smoke** step as above.

## JSONPath / XPath assertions

Define structured-data elements via `ingenious_object_add` on a structured-data page
(not hand YAML). Discover the assertion action name with
`ingenious_action_search "json"` / `"xpath"` and reference the element by name in the
assertion step.

## Data (only if data-driven)

`ingenious_data_sheet_create` → `ingenious_data_column_add` → `ingenious_data_row_add`
(or `ingenious_data_generate`). Reference from steps as `Sheet:Column`. For large
payloads, store the whole body in one column and reference it; for small payloads embed
`{Sheet:Column}` inside the JSON string.

## Naming conventions (fixed)

- Reusable scenario uses a `Flow` suffix; test-case scenario uses a domain name; the two
  MUST differ.
- No hardcoded values in step inputs — use `Sheet:Column`. Never copy plaintext secrets;
  use a placeholder cell value.

## Done when

1. All mutations went through `ingenious_*` tools.
2. `ingenious_testcase_validate` → `valid:true`.
3. Test case + reusables exist as YAML; assertions include at least one status + one
   payload/header check.
4. Smoke run passes the outcome assertion.
5. If the fallback path was used, the skipped collection-run stage is called out.

## Output summary

Report: pipeline used (collection-first vs fallback), collection/run ids (if any),
scenario/testcase, reusables, data sheet/columns, assertions (and their source =
observed run vs provided), resolved `ingenious_run` command, and validation result.
