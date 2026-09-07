# Copilot instructions — INGenious 3.1.x (shipped in the distribution)

You are working inside an **INGenious** distribution. This folder is the INGenious
runtime: launchers (`ingenious`, `ingenious.bat`), `lib/`, `Configuration/`, `Projects/`,
and the AI assets under `ai/`. These instructions load automatically in VS Code Copilot
chat when this folder is opened as the workspace.

## How tools reach the model (NO user-managed MCP)

INGenious ships **96 in-process tools** (test / scenario / object / data / run / report /
API-collection authoring). They are executed by the INGenious engine in `lib/`, never by a
separate MCP server you have to start:

- **INGenious AI CLI (`./ingenious ai`)** and the **IDE AI assistant**: tools run
  **in-process** through the engine's tool registry; the LLM is the **GitHub Copilot SDK**.
  Nothing to configure, no server to start.
- **VS Code Copilot chat**: the **INGenious VS Code extension** (see
  `.vscode/extensions.json`) registers those same tools as native language-model tools and
  executes them by calling the engine in `lib/`. No `mcp.json`, no MCP server.

If the `ingenious_*` tools are not available in chat, tell the user to install the
recommended INGenious extension (Extensions view → filter "Recommended"), then reload —
do NOT fall back to hand-writing test files.

## Skill routing — ALWAYS follow the matching skill

When the user's request matches one of these tasks, **first open and follow the matching
`SKILL.md`** under `ai/skills/`, then execute its tool playbook exactly:

| If the user wants to… | Read and follow |
|---|---|
| Author a browser/UI test from a flow | `ai/skills/ingenious-browser-test-from-specification/SKILL.md` |
| Author an API test (spec/collection/curl/HAR/flow) | `ai/skills/ingenious-api-test-from-specification/SKILL.md` |
| Migrate Selenium/Gherkin UI tests | `ai/skills/ingenious-ui-migrator/SKILL.md` |
| Create/fix an INGenious plugin | `ai/skills/ingenious-plugin-creation/SKILL.md` |
| Detect customizations vs the official release | `ai/skills/ingenious-customization-detection/SKILL.md` |

The skills are **tool-first**: the engine owns every file format and validation. Never
hand-author `TestPlan/**`, `ObjectRepository/**`, `TestData/**`, `api/**`, or `.project`.

## Cost & determinism rules (keep credit usage low)

- **Follow the skill's fixed playbook.** Do not improvise, explore, or re-derive formats.
- **Discover, don't read source.** Get action names from `ingenious_action_search` /
  `ingenious_action_info`. Never read engine Java to find actions.
- **Don't scan the folder.** Use the targeted `ingenious_*` read tools (`_list`, `_show`,
  `_search`) instead of opening files broadly.
- **Prefer one-shot generators** (`ingenious_gen_testcase`, `ingenious_gen_from_openapi`,
  `ingenious_apicollection_*`) over many small `add_step` calls.
- **Validate once** with `ingenious_testcase_validate`; fix by re-calling the tool.
- For heavy, multi-request orchestration, the **`./ingenious ai` CLI** does the same work
  at a fraction of the credits (lean agent loop, Copilot SDK). Prefer it for large jobs.

## Authoring conventions (enforced by the engine)

- **Step inputs**: hard-coded values are `@`-prefixed (`@200`); data-driven values are
  `Sheet:Column`; API payload bodies are raw (not `@`-prefixed) and may embed
  `{Sheet:Column}`; GlobalData `#id` values belong in data-sheet cells only; `%var%` are
  runtime variables.
- **Objects**: OR references are `Page.element`, `Webservice` for API steps, or `Execute`
  for reusable calls — never `@`-prefixed.
- **Naming**: TestPlan scenarios are business flows; test cases are user journeys. A
  reusable's scenario name MUST differ from any test-case scenario name.
- **Composition**: test cases call reusables with `object: Execute`,
  `action: <ReusableScenario>:<ReusableName>`.
- **Quality**: no fixed sleeps (use `waitFor*`); end every test case with an assertion;
  never store plaintext passwords — use `PLACEHOLDER_<ENV>_DO_NOT_COMMIT`.
