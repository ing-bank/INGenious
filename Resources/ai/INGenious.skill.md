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

## Quality rules

* Never use fixed sleeps; use `waitFor*` actions before interacting.
* Every test case ends with at least one assertion.
* Reuse existing reusables and data-sheet rows before creating new ones.
* Never copy plaintext passwords into steps or data sheets — use placeholders
  such as `PLACEHOLDER_TEST_DO_NOT_COMMIT`.
* MANUAL marker steps carry the note in `action` only (object/input empty).
