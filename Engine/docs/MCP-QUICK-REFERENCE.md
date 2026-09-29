# MCP Quick Reference — 75 Tools at a Glance

## Discovery (Tools 1–10)

| Tool | Args | Returns |
|------|------|---------|
| `ingenious_project_list` | — | `[{name, scenarios, releases, ...}]` |
| `ingenious_project_info` | `project` | `{scenarios, releases, testCount, ...}` |
| `ingenious_scenario_list` | `project` | `[{name, testCount}]` |
| `ingenious_scenario_info` | `project, scenario` | `{tests, description, ...}` |
| `ingenious_testcase_list` | `project, scenario` | `[{name, steps, tags}]` |
| `ingenious_testcase_show` | `project, scenario, testcase` | `{steps, data, tags}` |
| `ingenious_action_categories` | — | `["Browser", "API", "Database", ...]` |
| `ingenious_action_list` | `category, limit?` | `[{name, params, description}]` |
| `ingenious_action_search` | `query` | `[{name, category, match%}]` |
| `ingenious_action_info` | `name` | `{name, category, params, description}` |

## Authoring (Tools 11–30)

| Tool | Args | Returns |
|------|------|---------|
| `ingenious_testcase_create` | `project, scenario, testcase, [steps], [reusable], [ifExists], [dryRun]` | `{created, testcase, steps}` |
| `ingenious_testcase_add_step` | `project, scenario, testcase, action, [object], [input], [description]` | `{testcase, steps}` |
| `ingenious_testcase_insert_step` | `project, scenario, testcase, index, action, ...` | `{testcase, steps}` |
| `ingenious_testcase_edit_step` | `project, scenario, testcase, index, action, ...` | `{testcase, steps}` |
| `ingenious_testcase_move_step` | `project, scenario, testcase, from, to` | `{testcase, steps}` |
| `ingenious_testcase_remove_step` | `project, scenario, testcase, index` | `{testcase, steps}` |
| `ingenious_testcase_validate` | `project, scenario, testcase` | `{valid, errors[], warnings[]}` |
| `ingenious_testcase_delete` | `project, scenario, testcase` | `{deleted}` |
| `ingenious_object_list` | `project` | `[{page, objectCount}]` |
| `ingenious_object_show` | `project, page` | `[{name, locator, type}]` |
| `ingenious_object_search` | `project, query` | `[{page, name, locator}]` |
| `ingenious_object_add` | `project, page, name, type, locator, value, [description], [dryRun]` | `{wouldAdd, page, name}` |
| `ingenious_object_update` | `project, page, name, value, [description]` | `{updated, page, name}` |
| `ingenious_object_delete` | `project, page, name` | `{deleted}` |
| `ingenious_object_import_page` | `project, url, page, browser` | `{page, objectCount}` |
| `ingenious_testset_create` | `project, release, testset` | `{created, testset}` |
| `ingenious_testset_list` | `project` | `[{release, testset, rowCount}]` |
| `ingenious_testset_show` | `project, release, testset` | `[{scenario, testcase, browser, row}]` |
| `ingenious_testset_add` | `project, release, testset, scenario, testcase, [browser], [iteration], [dryRun]` | `{wouldAdd, testset, rows}` |
| `ingenious_testcase_clone` | `project, scenario, testcase, newScenario, newTestcase` | `{cloned}` |

## Data & Environments (Tools 31–45)

| Tool | Args | Returns |
|------|------|---------|
| `ingenious_env_list` | `project` | `["dev", "staging", "prod", ...]` |
| `ingenious_env_create` | `project, environment` | `{created, environment}` |
| `ingenious_env_delete` | `project, environment` | `{deleted}` |
| `ingenious_data_sheet_create` | `project, sheet` | `{created, sheet}` |
| `ingenious_data_list` | `project` | `[{sheet, columns, rows, envs}]` |
| `ingenious_data_show` | `project, sheet, [env], [limit]` | `[{row, col1, col2, ...}]` |
| `ingenious_data_get` | `project, sheet, column, row, [env]` | `{value}` |
| `ingenious_data_set` | `project, sheet, column, row, value, [env], [dryRun]` | `{set, value}` |
| `ingenious_data_column_add` | `project, sheet, column` | `{sheet, columns}` |
| `ingenious_data_row_add` | `project, sheet, [env], [row]` | `{sheet, rows}` |
| `ingenious_data_row_delete` | `project, sheet, row, [env]` | `{deleted, row}` |
| `ingenious_data_import` | `project, file, sheet, [env]` | `{imported, rows, columns}` |
| `ingenious_data_export` | `project, sheet, [env], [format]` | `{exported, file}` |
| `ingenious_data_generate` | `project, sheet, rows, columns, [env], [seed]` | `{generated, rows, columns}` |
| `ingenious_config_show` | `project` | `{driverConfig, playlistConfig, ...}` |
| `ingenious_config_set` | `project, key, value` | `{set, key, value}` |

## Generation (Tools 46–50)

| Tool | Args | Returns |
|------|------|---------|
| `ingenious_gen_list` | `[category]` | `[{name, category, params[], steps[]}]` |
| `ingenious_gen_testcase` | `project, archetype, scenario, testcase, params, [ifExists], [dryRun]` | `{created, testcase, unresolvedParams[]}` |
| `ingenious_gen_from_openapi` | `project, file, scenario, [baseUrl]` | `{generated, testCount}` |
| `ingenious_gen_from_har` | `project, file, scenario, [urlFilter]` | `{generated, testCount}` |
| `ingenious_gen_from_playwright` | `project, file, scenario` | `{generated, testCount}` |

## Execution (Tools 51–60)

| Tool | Args | Returns |
|------|------|---------|
| `ingenious_run` | `project, target, [browser], [tags], [dryRun], [rerun], [async]` | `{runId, status, result?, logs?}` |
| `ingenious_run` (async check) | `action: "status", runId` | `{runId, status, elapsed, progress}` |
| `ingenious_run` (logs) | `action: "logs", runId` | `{runId, logs, exitCode}` |
| `ingenious_run` (cancel) | `action: "cancel", runId` | `{cancelled, runId}` |
| `ingenious_run` (dry) | `target, dryRun: true` | `{dryRun, wouldRun, stepCount}` |
| `ingenious_run` (history) | `project, target, [limit]` | `[{runId, date, status}]` |

## Reporting (Tools 61–70)

| Tool | Args | Returns |
|------|------|---------|
| `ingenious_report_latest` | `project, target` | `{runId, status, passed, failed, skipped}` |
| `ingenious_report_history` | `project, target, [limit]` | `[{runId, date, status, passed, failed}]` |
| `ingenious_report_show` | `project, runId` | `{runId, target, steps[], results[]}` |
| `ingenious_report_failures` | `project, target` | `[{testcase, step, error, screenshot}]` |
| `ingenious_report_compare` | `project, runA, runB` | `{regressions: [], fixes: [], unchanged: []}` |
| `ingenious_report_export` | `project, runId, format, [output]` | `{exported, file, format}` |
| `ingenious_report_trend` | `project, target, days` | `[{date, passed, failed, duration}]` |
| `ingenious_config_drivers` | — | `{jdk, maven, playwright, browsers[]}` |
| `ingenious_doctor` | `project` | `{health, issues[], suggestions[]}` |
| `ingenious_screenshot_get` | `project, runId, stepIndex` | `{imageBase64}` |

## Browser Control (Tools 71–75)

| Tool | Args | Returns |
|------|------|---------|
| `ingenious_browser_session_start` | `project, browser` | `{sessionId, browser, pid}` |
| `ingenious_browser_session_do` | `sessionId, action, object?, input?` | `{result, screenshot?}` |
| `ingenious_browser_session_snapshot` | `sessionId` | `{screenshot, dom}` |
| `ingenious_browser_session_save` | `sessionId, scenario, testcase` | `{saved, testcase}` |
| `ingenious_browser_session_close` | `sessionId` | `{closed}` |

---

## Key Features

### Phase 5 Quality-of-Life Flags

**`dryRun: true`**  
Preview what *would* happen without writing files. Available on all write tools.  
Returns: `{dryRun: true, wouldCreate/wouldAdd/would...: boolean, ...}`

**`ifExists: "error" | "skip" | "overwrite"`**  
Control behavior when test case / archetype / object already exists.
- `error` (default): throw error
- `skip`: return `{created: false, existing: true}`
- `overwrite`: replace existing

**Rich Error Suggestions**  
When a name is misspelled, error includes `data.suggestions[]` with top 3 matches ranked by Levenshtein distance.  
Example: `"APIBasc"` → `{error: "Scenario not found", data: {suggestions: ["APIBasics"]}}`

---

## Prompts (13 Natural-Language Templates)

| Prompt | Purpose |
|--------|---------|
| `create_test_case` | Author a test from description |
| `convert_manual_steps` | Convert written steps to actions |
| `create_scenario` | Set up a new scenario |
| `author_by_archetype` | Use archetype to generate test case |
| `build_data_driven_suite` | Parameterize test with data sheet |
| `harden_test` | Stabilize brittle test (waits, selectors, assertions) |
| `explain_failure` | Analyze a failed run and suggest fixes |
| `debug_test` | Find root causes of flakiness |
| `triage_run` | Compare runs and identify regressions |
| `record_browser_test` | Guide user through Playwright recording |
| `bootstrap_project` | Create new project with sample scenarios |
| `connect_api_client` | Set up API test with auth/headers |
| `best_practices` | Advice on locators, waits, data-driven patterns |

---

## Resources (5 Knowledge Base Items)

| Resource | URI | Contains |
|----------|-----|----------|
| **Getting Started** | `ingenious://docs/getting-started` | Tutorial walkthrough |
| **Step Schema** | `ingenious://docs/step-schema` | Step structure & fields |
| **Best Practices** | `ingenious://docs/best-practices` | Locators, waits, assertions, data patterns |
| **Action Catalog** | `ingenious://catalog/actions` | All 155+ action definitions (JSON) |
| **Archetypes** | `ingenious://catalog/archetypes` | All 7 templates with params (JSON) |

---

## Common Workflows

### 1️⃣ Create & run a browser test
```
projectList → scenarioList → testcaseCreate → objectAdd → testcaseValidate → run
```

### 2️⃣ Data-driven API test
```
dataSheetCreate → dataColumnAdd → dataGenerate → genFromOpenAPI → run
```

### 3️⃣ Debug a failing test
```
reportLatest → reportFailures → testcaseShow → suggestHardening → run
```

### 4️⃣ Generate from spec
```
genFromOpenAPI (yaml) → testcaseValidate → testsetAdd → run → reportExport
```

### 5️⃣ Record & save
```
browserSessionStart → browserSessionDo (multiple) → browserSessionSave → testcaseValidate
```

---

## Error Handling

All errors follow JSON-RPC 2.0 error format:
```json
{
  "code": -32602,
  "message": "Scenario not found: APIBasc",
  "data": {
    "suggestions": ["APIBasics"]
  }
}
```

Common codes:
- `-32600` : Invalid request
- `-32601` : Method not found
- `-32602` : Invalid params
- `-32700` : Parse error

---

## MCP Protocol Reference

**Tools**: `tools/list` → tools, `tools/call` → invoke tool
**Prompts**: `prompts/list` → prompts, `prompts/get` → render template
**Resources**: `resources/list` → resources, `resources/read` → read by URI

All methods use JSON-RPC 2.0 over stdio.

---

**Need details?** See `MCP-TEST-SCENARIOS.md` for complete examples with expected outputs.
**Running tests?** Use `mcp_test_runner.py` for a comprehensive test suite.
**Implementation?** Check `MCP-IMPLEMENTATION-PLAN.md` for architecture.
