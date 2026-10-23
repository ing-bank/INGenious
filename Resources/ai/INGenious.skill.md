---
name: ingenious-authoring
description: Authoritative INGenious test-authoring conventions - step input grammar, naming model, data parameterization workflow and quality rules. Use when creating, editing, parameterizing or reviewing INGenious test cases, reusables, object repositories or data sheets.
---

# INGenious authoring conventions

INGenious organizes work as: **Projects** contain **Scenarios** (folders)
containing **Test Cases** (YAML step lists). Steps reference **Actions**,
**Object Repository** pages/objects and **data sheets**. **Test Sets**
(TestLab) group test cases for execution.

## Step input grammar

| Form | Meaning | Example |
|------|---------|---------|
| `@literal` | Hard-coded value | `@200`, `@https://example.com` |
| `Sheet:Column` | Whole-input data-sheet reference | `LoginData:Username` |
| `{Sheet:Column}` | Data reference embedded in an API payload | `{Payment:AccountNumber}` |
| `#id` | GlobalData environment id — data-sheet cells ONLY | `#test` |
| `%var%` | Runtime variable | `%orderId%` |

* Payload-bearing actions (`postRestRequest`, `putRestRequest`,
  `patchRestRequest`, `deleteWithPayload`) take the **raw body** as input — the
  body is *not* `@`-prefixed; parameterize individual JSON/XML values with
  `{Sheet:Column}` tokens instead.
* Object references are **never** `@`-prefixed (engine specials like
  `@Browser` excepted).
* Environment ids live in the project's GlobalData sheet — discover them,
  never assume names like `#dev`.

## Structure and naming

* **TestPlan** scenario = business flow (`Mortgage Calculation`); test case =
  user journey (`Young Single buying a High Energy Label home`).
* **ReusableComponents** scenario = user-intent group (`Common`, `Flow`);
  reusable = one user intent (`Launch the App`, `Fill Income`).
* A scenario name must NEVER be used by both TestPlan/ and ReusableComponents/.
* Test cases compose reusables:

```yaml
  - step: 1
    object: Execute
    action: Common:Launch the App
    reference: "[Project]"
```

## Authoring workflow

1. Discover real action names first (`ingenious action search` / the
   `ingenious_action_search` tool) — **never invent action names**.
2. Create the test case with `@literal` inputs.
3. Externalise data with the parameterize tool (`mode=scan` first, then apply
   `mode=all` or a selection) — values move into a data-sheet row keyed to the
   test case; inputs become `Sheet:Column` / `{Sheet:Column}`.
4. Validate, run, triage.

## Browser flow discovery (deterministic routing)

When a browser test (plain English, BDD, or any format) needs objects that are
**not yet in the Object Repository**, treat it as a *discovery* flow and route
it deterministically through `@playwright/cli` — never hand-write locators or
guess the flow. The only non-deterministic part is what the exploration finds.

1. Confirm intent: check `ingenious_object_list` / `ingenious_object_search`.
   If the objects already exist, author steps normally (no discovery session).
2. `ingenious_browser_discover` — pass the `url` and the user's `prompt`
   verbatim. It opens a live session and returns the first ref'd snapshot plus
   a fixed protocol.
3. `ingenious_browser_session_do` — realise the flow one action at a time using
   only refs from the returned snapshots. Each call blocks until the CLI
   finishes; re-read the snapshot before the next ref.
4. `ingenious_browser_session_save` — discovered locators become WebOR objects
   and the recorded steps are linked to them (scenario/testcase/page are
   pre-bound by `ingenious_browser_discover`).
5. `ingenious_browser_session_close`, then `ingenious_testcase_validate`.

## Performance testing (k6 Performance Studio)

Functional assets double as k6 load tests; the pipeline is deterministic:

1. `ingenious_perf_export` — generate a script from an API test case, a web
   test case (`type=browser`), or a HAR recording
   (`ingenious_perf_record_start/stop` captures one proxy-free). Load shape
   comes from profiles (smoke/average/stress/spike/soak or
   `Performance/profiles/*.yaml`) — never hand-edit generated options.
2. `ingenious_perf_validate` — ALWAYS debug-run (1 VU, 1 iteration) first.
3. `ingenious_perf_run` (blocking) or `ingenious_perf_run_async` +
   `ingenious_perf_status` polling (live vus/rps/p95/error-rate; the returned
   `dashboardUrl` serves live graphs). `ingenious_perf_scale` /
   `ingenious_perf_cancel` control a running test.
4. `ingenious_perf_report` and `ingenious_perf_compare` — persisted results
   under `Results/Performance/`, with regression detection for CI gates.

Dynamic values (tokens, session ids) in HAR exports: pass `autoCorrelate=true`
— proposed correlation rules land in `Performance/rules/<script>.rules.yaml`
(review them). Credential headers are scrubbed at import and only re-enter
scripts via correlation rules.

## Quality rules

* Never use fixed sleeps; use `waitFor*` actions before interacting.
* Every test case ends with at least one assertion.
* Reuse existing reusables and data-sheet rows before creating new ones.
* Never copy plaintext passwords into steps or data sheets — use placeholders
  such as `PLACEHOLDER_TEST_DO_NOT_COMMIT`.
* MANUAL marker steps carry the note in `action` only (object/input empty).
