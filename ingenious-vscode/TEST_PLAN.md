# INGenious VS Code Extension — Test Plan

This plan validates the extension delivered under `ingenious-vscode/`. It is
organised by phase (P0–P5) so you can sign each off independently. Every test
lists **Steps** and **Expected result**.

---

## 0. Prerequisites & setup

| # | Check | How |
|---|---|---|
| 0.1 | Java 17+ available | `java -version` |
| 0.2 | `ingenious` engine built | From repo root: `mvn -pl Engine -am package -DskipTests`, or use `Dist/release`. |
| 0.3 | CLI reachable | `ingenious --version` prints a version. If not on PATH, note the launcher path (e.g. `Resources/ingenious.command`) for setting `ingenious.cliPath`. |
| 0.4 | Extension builds | In `ingenious-vscode/`: `npm install && npm run compile` → no errors. |
| 0.5 | Unit tests pass | `npm test` → **6 passing**. |

### Launch the Extension Development Host
1. Open the `ingenious-vscode/` folder in VS Code.
2. Press **F5** (Run Extension). A second VS Code window opens.
3. In that window, open a folder containing an INGenious project (e.g. the repo's
   `Resources/Projects/Tutorial`, or `Resources/` so `Projects/` is visible).
4. If the CLI is not on PATH: Settings → search `ingenious.cliPath` → set it to the
   launcher (e.g. `.../Resources/ingenious.command`).

> Expected on activation: the **INGenious** icon appears in the Activity Bar and
> the status bar shows `INGenious`, `Project: …`, `env: …`, `MCP: …`.

---

## P0 — Engine surface (verify only; no engine changes required)

| # | Test | Steps | Expected |
|---|---|---|---|
| P0.1 | MCP tools present | Terminal: `ingenious server mcp` then paste `{"jsonrpc":"2.0","id":1,"method":"initialize","params":{}}` and `{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}` (Enter after each). | `initialize` returns `serverInfo.name = ingenious-mcp-server`; `tools/list` returns ~96 tools. Ctrl-C to exit. |
| P0.2 | `doctor` tool | Command Palette → **INGenious: Doctor**. | The `INGenious` output channel prints JDK / Playwright / drivers / k6 / project blocks (from `ingenious_doctor`), or a CLI-version fallback line. |
| P0.3 | JSON output | `ingenious --json project list` (from a folder with `Projects/`). | Valid JSON is printed. |
| P0.4 | Streaming note | N/A | Per-step `run --stream` NDJSON is intentionally **not** implemented; the extension uses task output + report polling. No action needed. |

---

## P1 — MVP extension

### Trees / Activity bar
| # | Test | Steps | Expected |
|---|---|---|---|
| P1.1 | Projects view | Open the INGenious view container. | **Projects** lists discovered projects; the active one shows `active`. |
| P1.2 | Switch project | Click a different project (or status bar `Project:`), pick another. | Active project updates; other trees refresh. |
| P1.3 | Test Plan tree | Expand **Test Plan**. | Scenarios list; expanding a scenario lists its test cases. |
| P1.4 | Open test case | Click a test case leaf. | The file opens (YAML in the text editor, or CSV in the visual editor). |
| P1.5 | Reusables tree | Expand **Reusables**. | Reusable scenarios/components appear (may be empty for some projects). |
| P1.6 | Test Sets tree | Expand **Test Sets**. | Releases group test sets. |
| P1.7 | Object Repository tree | Expand **Object Repository**. | Pages list with counts; expanding a page lists objects with type + locator tooltip. |
| P1.8 | Test Data tree | Expand **Test Data**. | Environments listed; clicking one selects it (status bar `env:` updates). |
| P1.9 | API Collections tree | Expand **API Collections**. | Collections list, or “No API collections”. |
| P1.10 | Reports tree | Expand **Reports**. | Scenarios → test cases; clicking opens the report webview. |
| P1.11 | Welcome view | Open a folder with **no** project. | Projects view shows welcome buttons: Create / Open / Install CLI / Doctor. |

### Language features (open a `TestPlan/**/*.csv` step file)
| # | Test | Steps | Expected |
|---|---|---|---|
| P1.12 | Action completion | In the **Action** column (4th), trigger IntelliSense (Ctrl-Space). | Real action names appear (e.g. `Click`, `Fill`, `getRestRequest`). |
| P1.13 | Object completion | In the **Object** column (2nd), Ctrl-Space. | `@Browser`, `Webservice`, `Execute`, plus `Page.element` entries. |
| P1.14 | Input completion | In the **Input** column (5th), Ctrl-Space. | `@literal`, `Sheet:Column`, `%var%` snippets. |
| P1.15 | Hover | Hover an action name. | Markdown popup with description / parameters / example. |
| P1.16 | CodeLens | Look at the top of the file. | `Run · Debug · Dry-run · Validate · Last report` lenses. |
| P1.17 | Snippets | Type `ing-` in a CSV. | Snippet suggestions (`ing-open`, `ing-api-get`, …). |
| P1.18 | Diagnostics | Introduce an unknown action, save. | A squiggle/Problem appears sourced from `ingenious`; fixing + saving clears it. |

### Run / report / status bar
| # | Test | Steps | Expected |
|---|---|---|---|
| P1.19 | Run via CodeLens | Click **Run** on a browser or API test case. | A terminal task runs `ingenious run …`; output streams. |
| P1.20 | Report auto-open | After the run finishes (setting `report.openOnRunEnd` on). | The report webview opens showing `summary-v2.html`. |
| P1.21 | Status bar last run | After a run. | `Last: ✓ <testcase>` (red ✗ on failure); clicking opens the report. |
| P1.22 | Editor title run | Open a step CSV; use the ▶ in the editor title bar. | Same run behaviour. |
| P1.23 | Dry-run | CodeLens **Dry-run**. | Output channel shows format, step count, validation counts (no execution). |
| P1.24 | Validate | CodeLens **Validate**. | Info toast with error/warning counts; diagnostics update. |
| P1.25 | MCP status | Watch the status bar `MCP:`. | `starting` → `connected`. Clicking restarts it. |
| P1.26 | Task provider | Command Palette → **Tasks: Run Task** → `ingenious`. | An ingenious task is offered/resolvable. |

---

## P2 — Visual test-case (CSV) editor

Use a project whose test cases are **CSV** (or create one). Open a
`TestPlan/**/*.csv` via the tree or **Open With → INGenious Test Case**.

| # | Test | Steps | Expected |
|---|---|---|---|
| P2.1 | Grid renders | Open the CSV in the visual editor. | A table with `# / Object / Description / Action / Input / Condition / Reference`. |
| P2.2 | Edit a cell | Change the Action cell of a row. | The underlying CSV file updates (check via the plain-text editor / git diff). |
| P2.3 | Action datalist | Focus an Action cell. | Autocomplete list is populated from the catalog. |
| P2.4 | Object datalist | Focus an Object cell. | Autocomplete includes OR objects + engine specials. |
| P2.5 | Add step | Click **+ Step**. | A new row appears and is persisted; step numbers renumber. |
| P2.6 | Delete/reorder | Use ✕ and ↑. | Rows delete/move and the file reflects it. |
| P2.7 | Payload safety | Put a JSON body with commas in an Input cell (e.g. `@{"a":1,"b":2}`). | Saved CSV keeps it in one field (quoted); reopening shows it intact. |
| P2.8 | External edit sync | Edit the CSV in a separate plain-text view. | The grid refreshes to match. |
| P2.9 | Run from editor | Click **▶ Run** in the grid toolbar. | Runs the test case. |

---

## P3 — Object Repository YAML editor

Open an `ObjectRepository/**/*.yaml` page (e.g. via the file explorer or the
Object Repository tree → reveal file).

| # | Test | Steps | Expected |
|---|---|---|---|
| P3.1 | Editor renders | Open a page YAML. | Table with **Name** + locator columns (`role`, `text`, `label`, `css`, `xpath`, `testId`, …). |
| P3.2 | Edit a locator | Change a `css`/`xpath` value. | The YAML file updates; reopening in a text editor confirms. |
| P3.3 | Add object | Click **+ Object**, name it, set a locator. | New element persisted under `elements:`. |
| P3.4 | Rename page | Change the Page field. | `page:` key updates. |
| P3.5 | Delete object | Click ✕ on a row. | Element removed from YAML. |
| P3.6 | Model consistency | After edits, run a test that uses the page (or run Doctor). | The engine still reads the page (no parse error). |

---

## P4 — Test results & rerun

| # | Test | Steps | Expected |
|---|---|---|---|
| P4.1 | Failing run status | Run a test case that fails. | Status bar shows red `✗`; report opens with the failure. |
| P4.2 | Re-run failed | CodeLens/command **Re-run Failed** on the same target. | `ingenious run … --rerun` executes only failed cases. |
| P4.3 | Report history | Reports tree → open a target that has multiple runs. | The latest report shows; “Open in browser” works. |
| P4.4 | Report webview assets | Open any report. | Charts/styles render (assets resolve via injected `<base>`). |

---

## P5 — v2 / advanced features

| # | Test | Steps | Expected |
|---|---|---|---|
| P5.1 | AI terminal | Command Palette → **INGenious: Open AI Assistant (CLI)**. | A terminal opens and runs `ingenious ai` in the project. |
| P5.2 | Browser discovery | **INGenious: Discover Objects in Browser**, enter a URL + prompt. | A `browser_discover` session starts; output channel shows the session/snapshot JSON. (Requires `@playwright/cli`/npx.) |
| P5.3 | API Collections | Ensure the project has an `api/collections/*.json`; expand the tree. | Collections appear. |
| P5.4 | Create project | **INGenious: New Project**, enter a name. | A project scaffold is created under `Projects/` and trees refresh. |
| P5.5 | Install/Doctor help | **INGenious: Install / Update CLI**. | Guidance dialog with Settings / Docs actions. |

---

## Regression / non-functional

| # | Test | Expected |
|---|---|---|
| R.1 | No CLI installed | With a bogus `ingenious.cliPath`, running a test shows a friendly “CLI not found” prompt with Install / Settings actions (no crash). |
| R.2 | MCP crash recovery | Kill the MCP process; status goes `stopped` then auto-restarts (up to 5 attempts, backoff). |
| R.3 | Security | Runs use `ProcessExecution`/`spawn` with `shell:false` (no shell injection). Webviews use CSP with a nonce. |
| R.4 | Multi-root | Open a multi-root workspace; projects from each root are discovered. |
| R.5 | Deactivate | Close the window; no orphaned `java`/MCP process remains. |

---

## Known limitations (by design for this milestone)

- **Per-step live gutter decorations** are not wired; run progress is shown via
  the task terminal + report (the `--stream` NDJSON engine gap is deferred).
- **Run** uses the CLI task (so the project resolves via cwd); the MCP `run`
  tool is not used for execution.
- **Reusables tree** relies on the engine's reusable scenario listing; some
  projects may surface reusables only through the file explorer.
- Packaging to a `.vsix` on this machine currently fails due to a pre-existing
  npm cache ownership issue (`sudo chown -R $(id -u):$(id -g) ~/.npm`), unrelated
  to the extension. `npm run compile` and `npm test` are the validation gates.

---

## Sign-off checklist

- [ ] P0 verified (MCP tools, doctor)
- [ ] P1 MVP (trees, language features, run, report, status bar)
- [ ] P2 visual CSV editor
- [ ] P3 Object Repository YAML editor
- [ ] P4 results & rerun
- [ ] P5 advanced features
- [ ] Regression/non-functional
