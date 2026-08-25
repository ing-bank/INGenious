# Copilot instructions — INGenious 3.1.x

This repository is the INGenious test-automation platform. These instructions are
loaded automatically into every Copilot chat request in this workspace.

## Tooling model (read this first)

INGenious exposes **96 `ingenious_*` tools** (test/scenario/object/data/run/report/
API-collection authoring). How they reach you depends on the client:

- **INGenious CLI (`ingenious ai`) and the IDE AI assistant**: tools are **in-process** —
  no MCP server needed. This is the cheapest, most deterministic way to run a workflow.
- **VS Code Copilot chat (here)**: tools arrive through the **`ingenious` MCP server**,
  which is defined in `.vscode/mcp.json` and **auto-starts on demand** — you do not need
  to start it manually.

If the `ingenious_*` tools are not available, tell the user the `ingenious` MCP server
failed to start (check `.vscode/mcp.json` and that `Dist/release` is built), and do NOT
fall back to hand-writing test files.

## Skill routing — ALWAYS follow the matching skill

When the user's request matches one of these tasks, **first open and follow the matching
`SKILL.md`** under `Resources/ai/skills/`, then execute its tool playbook exactly:

| If the user wants to… | Read and follow |
|---|---|
| Author a browser/UI test from a flow | `Resources/ai/skills/ingenious-browser-test-from-specification/SKILL.md` |
| Author an API test (spec/collection/curl/HAR/flow) | `Resources/ai/skills/ingenious-api-test-from-specification/SKILL.md` |
| Migrate Selenium/Gherkin UI tests | `Resources/ai/skills/ingenious-ui-migrator/SKILL.md` |
| Create/fix an INGenious plugin | `Resources/ai/skills/ingenious-plugin-creation/SKILL.md` |
| Detect customizations vs the official release | `Resources/ai/skills/ingenious-customization-detection/SKILL.md` |

The skills are **tool-first**: the engine owns every file format and validation. Never
hand-author `TestPlan/**`, `ObjectRepository/**`, `TestData/**`, `api/**`, or `.project`.

## Cost & determinism rules (keep credit usage low)

Multi-step tool orchestration in Copilot chat is expensive because every tool result
round-trips the full context through the model. To keep runs cheap and deterministic:

- **Follow the skill's fixed playbook.** Do not improvise, explore, or re-derive formats.
- **Discover, don't read source.** Get valid action names from `ingenious_action_search` /
  `ingenious_action_info`. NEVER read `Engine/src/**/commands/**` Java to find actions.
- **Don't scan the workspace.** Don't open large files or search broadly unless the skill
  says to. Use the targeted `ingenious_*` read tools (`_list`, `_show`, `_search`).
- **Prefer one-shot generators** (`ingenious_gen_testcase`, `ingenious_gen_from_openapi`,
  `ingenious_apicollection_*`) over many small `add_step` calls.
- **Validate once** with `ingenious_testcase_validate`; fix by re-calling the relevant
  tool, not by re-reading everything.
- **On a tool error**, use `error.data.suggestions`; never hand-fix files.

> For heavy, multi-request orchestration (e.g. a full API suite), the **`ingenious ai`
> CLI** does the same work at a fraction of the credits because its ReAct loop keeps a
> lean context. Prefer it for large jobs; it can even use your VS Code Copilot models via
> the INGenious bridge extension (no API key). See `docs/VS-CODE-RUN-AND-DEBUG.md`.

## Authoring conventions (enforced by the engine)

- **Step inputs**: hard-coded values are `@`-prefixed (`@200`); data-driven values are
  `Sheet:Column`; API payload bodies are raw (not `@`-prefixed) and may embed
  `{Sheet:Column}`; GlobalData `#id` values belong in data-sheet cells only; `%var%` are
  runtime variables.
- **Objects**: OR references are `Page.element`, `Webservice` for API steps, or `Execute`
  for reusable calls — never `@`-prefixed.
- **Naming**: TestPlan scenarios are business flows; test cases are user journeys.
  ReusableComponents scenarios are user-intent groups; a reusable's scenario name MUST
  differ from any test-case scenario name.
- **Composition**: test cases call reusables with `object: Execute`,
  `action: <ReusableScenario>:<ReusableName>`.
- **Quality**: no fixed sleeps (use `waitFor*`); end every test case with an assertion;
  never store plaintext passwords — use `PLACEHOLDER_<ENV>_DO_NOT_COMMIT`.
