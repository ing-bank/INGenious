# INGenious AI CLI — Testing Guide & Tutorial Prompts

Hands-on guide to verify everything built from
[AI-CLI-IMPLEMENTATION-PLAN.md](AI-CLI-IMPLEMENTATION-PLAN.md).
Each section lists **exact prompts/commands to type** and the **expected result**.

---

## 0. Implementation Status

| Phase | Scope | Status |
|---|---|---|
| 0 | Tool Registry over the MCP surface (75 tools), plugin SPI | ✅ Done |
| 1 | Interactive REPL, slash commands, session memory, UI kit | ✅ Done |
| 2 | Plans (JSON DAG), validator, execution engine, approvals, undo/redo | ✅ Done |
| 3 | Deterministic workflows (7: login test, API test, data, run, page objects, clone, rerun-failed) | ✅ Done |
| 4 | AI providers (Copilot device-flow, OpenAI-compatible), NL planner + repair round | ✅ Done |
| 5 | AI repair loop on failed steps, explain/answer mode | ✅ Done |
| 6 | `ingenious plugins` (list core packs + ServiceLoader plugins) | ✅ List only |
| 6+ | Plugin install-from-registry, per-file diff approvals, token streaming, full §5.1 live-browser pipeline as one workflow, packaging/docs polish, cutover QA | ⏳ Future |

---

## 1. Setup

```bash
cd INGenious
mvn -pl Engine clean install -DskipTests
mvn -pl Engine dependency:build-classpath -Dmdep.outputFile=/tmp/engine-cp.txt

# The CLI must run from a directory containing ./Projects, ./Configuration, ./plugins:
cd Resources
alias ing='java -cp "../Engine/target/classes:$(cat /tmp/engine-cp.txt)" com.ing.engine.core.Control'
```

> In a packaged install, plain `ingenious` (no args) opens the REPL in a real
> terminal; when stdin is piped it prints help instead (scripts never hang).

Start the REPL:

```bash
ing ai --project CLIDemo        # aliases: ing chat / ing assistant
```

You should see the **INGenious CLI** panel, `Project: CLIDemo`, the AI provider
status line, and a `CLIDemo ❯` prompt.

---

## 2. REPL & Slash Commands (no AI required)

Type each and check the expectation:

| Type this | Expect |
|---|---|
| `/help` | Boxed panel listing all commands |
| `/tools` | 75 tools grouped in 7 categories; ⚠ marks file-mutating tools |
| `/tools discovery` | Only the discovery category (16 tools) |
| `/tools run project_list {}` | JSON listing of projects under ./Projects |
| `/tools run testcase_list {"scenario":"API"}` | Test cases of the API scenario (project auto-injected) |
| `/tools run testcase_show {"scenario":"API","testcase":"NoSuchTest"}` | Error **plus "Did you mean: …?"** suggestions |
| `/workflows` | The 7 deterministic workflows with descriptions |
| `/project` | Project list + hint |
| `/project CLIDemo` | `✓ Project: CLIDemo <resolved path>` |
| `/context` | Session memory panel (project, framework, recent files) |
| `/status` | Provider, project, pending plan, undo/redo depth |
| `/config` | Global Settings.properties dump via the config_show tool |
| `/history` | Recent conversation turns |
| `/clear` then `/clear --all` | Transcript cleared; `--all` also wipes session facts |
| `/model` | Current provider + model and change hints |
| `/exit` | `Goodbye.` |

Also verify: **tab-completion** works for slash commands and tool ids;
**arrow-up** recalls history across restarts (`~/.ingenious/repl_history`);
**Ctrl-C** cancels the line, **Ctrl-D** exits.

---

## 3. Deterministic Workflows (zero LLM calls)

### 3.1 Create an API test → approval → mutation manifest
```
create api test for Ping
  Endpoint URL: https://example.com/ping
Proceed? [Y/n] y
```
Expect: plan panel (`gen_testcase` ⚠ + `testcase_validate`), streamed
`✓ gen_testcase — created PingApiTest`, `✓ testcase_validate — valid`,
`Files: created TestPlan/API/PingApiTest.yaml`, `✓ Done.`

### 3.2 Undo / redo
```
/undo        →  ✓ Undone: Create API test API/PingApiTest   (file gone)
/redo        →  ✓ Redone: …                                 (file back)
/undo        →  file gone again
```
Verify on disk: `ls Projects/CLIDemo/TestPlan/API/`.

### 3.3 Clone a test case (structured output piping `${s1.out.steps}`)
```
create api test for Ping            (answer url + y, as above)
clone testcase API/PingApiTest as PingCopy
Proceed? [Y/n] y
```
Expect: 3-step plan (`testcase_show → testcase_create → testcase_validate`),
`created TestPlan/API/PingCopy.yaml` with identical steps. Then `/undo` twice.

### 3.4 Rejecting an approval keeps the plan pending
```
create api test for Ping     (give url)
Proceed? [Y/n] n
/plan          →  shows the pending plan again
/approve       →  executes it now
```

### 3.5 Generate data
```
generate 10 rows of data for login
```
Expect: single-step plan `data_generate {sheet: login, rows: 10}`; after
approval, modified `TestData/*.csv` in the Files list. `/undo` restores.

### 3.6 Create a login test (archetype)
```
create a login test
  Login page URL: https://the-internet.herokuapp.com/login
  Username to type: tomsmith
  Password to type: SuperSecretPassword!
Proceed? [Y/n] y
```
Expect: `TestPlan/Login/LoginTest.yaml` created and valid.

### 3.7 Run tests / rerun failures (needs runnable targets + browsers)
```
run all smoke tests
  Run target …: CLIDemo/R1/Smoke
```
```
fix failing tests
  Run target …: CLIDemo/R1/Smoke
```
Expect: `report_failures` for `R1/Smoke`, then `run` with `rerun:true`.

### 3.8 Page-object discovery (§5.1 — needs `npm i -g @playwright/cli`)
```
generate page objects from https://the-internet.herokuapp.com/login
  Object Repository page name: LoginPage
```
Expect with playwright-cli installed: `ObjectRepository/LoginPage.csv` created,
then `object_show` prints the imported objects. Without it: a clear
"@playwright/cli not found" error (graceful failure). Pre-check: `/tools run doctor {}`.

---

## 4. AI Provider & Natural-Language Planning

### 4.1 Graceful degradation (no login)
Type any non-workflow request, e.g. `explain what a test set is`.
Expect the yellow hint: *"AI is not configured yet. Run /login …"* — no crash.

### 4.2 GitHub Copilot login
```
/login
```
Expect: a `github.com/login/device` URL + user code; after authorizing,
`Signed in successfully.` Then `/status` shows `logged in`.
Token cache: `~/.ingenious/credentials.json` (mode 600).

### 4.3 OpenAI-compatible provider (incl. local models)
```
/model provider openai
/model url http://localhost:11434/v1        # e.g. Ollama
/model llama3.1                             # any model name
```
API key read from env `OPENAI_API_KEY` (not needed by Ollama).
`/model provider copilot` switches back.

### 4.4 NL planning prompts (with AI active)
| Prompt | Expected plan shape |
|---|---|
| `list all scenarios in this project` | 1 read step (`scenario_list`) — runs without approval |
| `create a test that opens https://example.com and checks the heading is visible` | gen/testcase authoring steps + validate, approval asked |
| `add a step to API/PingApiTest that asserts the response code is 404` | `testcase_edit_step`/`add_step` ⚠ steps |
| `what actions are available for assertions?` | `{"type":"answer"}` → markdown answer, no plan |
| `explain this assertion: assertResponseCode 200` | Markdown explanation |
| `create a data sheet called customers with name and email columns and 5 rows` | `data_sheet_create` → `data_generate` |
| `show execution plan for creating a smoke test set` | Plan proposed; answer `n`, inspect with `/plan`, run with `/approve` |

Guardrail check: the planner **cannot invent tools** — any hallucinated tool is
rejected by the validator and repaired (or fails with a clear message).

### 4.5 AI repair loop
Force a failure while logged in:
```
clone testcase API/DoesNotExist as Copy
Proceed? [Y/n] y
```
Expect: `✗ testcase_show — Test case not found …`, then
`Ask AI to propose a fix? [y/N]` — answering `y` sends the failed plan + error
to the AI for a corrected plan.

---

## 5. MCP Compatibility Layer (must stay intact)

```bash
printf '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}\n{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}\n{"jsonrpc":"2.0","id":3,"method":"shutdown","params":{}}\n' \
  | ing server mcp 2>/dev/null | tail -2 | head -1 | python3 -c "import sys,json; print(len(json.loads(sys.stdin.read())['result']['tools']), 'tools')"
```
Expect: `75 tools` — VS Code / Copilot MCP integration is unaffected.

---

## 6. Plugin System

```bash
ing plugins
```
Expect: the 7 core packs with tool counts and `(none installed)` for plugins.
To test a real plugin: build a jar containing a `ToolPlugin` implementation and
a `META-INF/services/com.ing.engine.aicli.tools.ToolPlugin` file, add it to the
classpath, re-run — it appears here and its tools show in `/tools` + `/status`.

---

## 7. Unit / Regression Tests

```bash
mvn -pl Engine test -Djacoco.skip=true          # full suite (523+ tests)
mvn -pl Engine test -Djacoco.skip=true -Dtest=PlanningTest,ToolRegistryTest,PlannerJsonTest
```
Covers: registry surface ≥75 tools, mutability/category classification,
prompt catalog completeness, plan validation (unknown tools/deps, cycles),
topological ordering, JSON round-trip, workflow matching/param extraction,
clone step piping, rerun-failed target derivation, planner JSON extraction
(bare/fenced/prose-embedded).

> JDK 26 note: always pass `-Djacoco.skip=true` (JaCoCo can't instrument JDK 26).

---

## 8. Five-Minute Smoke Script (copy/paste)

```bash
cd Resources
printf '/help\n/tools discovery\n/project CLIDemo\ncreate api test for Ping\nhttps://example.com/ping\ny\nclone testcase API/PingApiTest as PingCopy\ny\n/status\n/undo\n/undo\n/exit\n' \
  | ing ai --no-banner 2>/dev/null
git status --porcelain Projects/CLIDemo/TestPlan   # must be empty
```
Pass criteria: both workflows complete with `✓ Done.`, both `✓ Undone:` lines
appear, and git reports no leftover changes.

---

## 9. Known Limitations (expected behaviors, not bugs)

- Browser tools require `@playwright/cli` on PATH (or npx); otherwise they fail
  with an instructive message. Run `/tools run doctor {}` first.
- Copilot endpoints are GitHub-internal and may change; the OpenAI-compatible
  provider is the fallback.
- Approvals are plan-level (step list + file manifest afterwards); per-file
  diff previews are future work.
- Undo tracks files ≤1 MB inside the project directory (skips Results/, .git/,
  .ingenious/).
- Session state lives in `.ingenious/` directories (git-ignored).
