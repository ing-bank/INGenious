# Plan: INGenious AI Assistant — VS Code-style Sidebar + MCP Coupling

> **Goal**: Replace the current full-screen AI "slide" with a persistent right-hand sidebar (à la GitHub Copilot Chat in VS Code) that stays visible while the user works in TestDesign or TestExecution, and wire it to the full 75-tool MCP engine so the prompts in `MCP-GETTING-STARTED.md` work directly inside INGenious.

---

## 1. What's broken with the current implementation

| Issue | Root cause |
|-------|-----------|
| AI hides everything else when opened | `AICopilotUI` is a `SlideShow` card — opening it calls `card.show("AICopilot")` which replaces the entire centre panel |
| Only 8 tools available | `INGeniousToolServer` exposes `listScenarios`, `listTestCases`, `readTestCase`, `listActions`, `createScenario`, `addTestCase`, `addStep`, `createORObject` — far short of the 75 in `MCPTools.java` |
| Tool approval is a blocking modal | `ToolApprovalDialog` shows a `JOptionPane`-style dialog that stops all work while waiting for the user |
| No context — AI doesn't know what's selected | The agent loop in `AgentOrchestrator` doesn't inject the currently-open project / scenario / testcase |
| No streaming feedback during tool calls | `onToolStart` / `onToolResult` callbacks exist but the WebView chat just shows a spinner |
| No prompt shortcuts | Users must type every prompt from scratch; no built-in library |

---

## 2. Target UX — VS Code Copilot Chat layout

```
┌─────────────────────────────────────────────────────────────────┐
│  Toolbar  (TestDesign | TestExecution | Dashboard | APITester)  │
├─────────────────────────────────────────┬───────────────────────┤
│                                         │  ≡  INGenious AI      │
│                                         │ ┌─────────────────┐  │
│         SlideShow (existing)            │ │ Prompt library  │  │
│  ┌────────────────────────────────┐     │ │ [Explore ▾]     │  │
│  │  TestDesign / TestExecution /  │     │ │ [Author ▾]      │  │
│  │  APITester (unchanged)         │     │ │ [Data ▾]        │  │
│  │                                │     │ │ [Run ▾]         │  │
│  │                                │     │ └─────────────────┘  │
│  │                                │     │ ┌─────────────────┐  │
│  └────────────────────────────────┘     │ │  Chat transcript │  │
│                                         │ │  (ChatWebView)  │  │
│                                         │ │                 │  │
│                                         │ │  ▶ Used tool    │  │
│                                         │ │  testcase_create│  │
│                                         │ │  ✓ created      │  │
│                                         │ └─────────────────┘  │
│                                         │ ┌─────────────────┐  │
│                                         │ │ [input area   ] │  │
│                                         │ │ [Send] [Stop]   │  │
│                                         │ └─────────────────┘  │
└─────────────────────────────────────────┴───────────────────────┘
```

Key principles borrowed from VS Code Copilot Chat:
- **Sidebar is always visible** — the user never loses sight of their test design
- **Collapsible** — a single button in the toolbar toggles the sidebar, persisting its width
- **Tool calls appear inline** — each `ingenious_*` invocation renders as a collapsible "used tool" row with args and result summary, identical to GitHub Copilot's "Used X" disclosure triangles
- **Context bar** — a thin strip at the top of the sidebar shows `Project: CLIDemo  ·  Scenario: APIBasics  ·  TestCase: GetUsers` pulled live from the TestDesign selection
- **Prompt library** — sectioned chips above the input that fill the input box when clicked (no typing required)
- **Non-blocking approval** — instead of a modal, approvals appear as inline "Apply?" buttons inside the chat transcript itself

---

## 3. Architecture overview

```
┌─────────────────────────────────────────────────────────────────┐
│  IDE module (Swing / JavaFX)                                    │
│                                                                 │
│  AppMainFrame                                                   │
│  ├── JSplitPane (new)                                           │
│  │   ├── LEFT:  SlideShow [TestDesign, TestExecution, ...]      │
│  │   └── RIGHT: AICopilotSidebar (new)                         │
│  │               ├── ContextBar (project/scenario/testcase)     │
│  │               ├── PromptLibraryPanel (chips from guide)      │
│  │               ├── ChatWebView (existing, tweaked CSS)        │
│  │               └── InputPanel (existing, + Send/Stop/Clear)   │
│  │                                                              │
│  │  AICopilot (controller — mostly unchanged)                   │
│  │  ├── ChatSession                                             │
│  │  ├── GitHubModelsClient                                      │
│  │  ├── AgentOrchestrator (extended for streaming feedback)     │
│  │  └── MCPToolBridge  ◄── NEW — replaces INGeniousToolServer   │
│  │       │                                                      │
│  │       └── calls MCPTools.dispatch() directly (in-process)   │
│  │                                                              │
├─────────────────────────────────────────────────────────────────┤
│  Engine module                                                  │
│  └── MCPTools (75 tools — no changes needed)                   │
└─────────────────────────────────────────────────────────────────┘
```

### Data-flow for a single agent turn

```
User types: "Create a test case called PingAPI in HealthChecks that GETs /health"
          │
          ▼
AICopilot.sendUserMessage(text)
          │
          ▼  builds ChatCompletionRequest with:
          │    • system prompt (AuthoringSkill + live context snapshot)
          │    • full conversation history
          │    • tool definitions from MCPToolBridge.toolDefinitions()
          │
          ▼
AgentOrchestrator.run()  ──► GitHub Models API (gpt-4o-mini / claude-3.5-sonnet)
          │
          │  model returns: tool_call { ingenious_action_search, args: {query:"status code"} }
          │
          ▼
MCPToolBridge.execute("ingenious_action_search", args)
          │  ── calls MCPTools.dispatch("action_search", args)  (in-process, no subprocess)
          │
          ▼  result JSON
          │
ChatWebView.appendToolCall(name, args, result)   ◄── collapsible "used tool" row
          │
          │  model sees result, calls next tool:
          │  tool_call { ingenious_testcase_create, args: {scenario:"HealthChecks", ...} }
          │
          ▼
MCPToolBridge.execute("ingenious_testcase_create", args)
          │  approval: inline "Apply?" button rendered in ChatWebView
          │  user clicks "Apply" ──► mutation proceeds
          │
          ▼
TestDesign.refreshTree()   ◄── triggered by mutation hooks in MCPToolBridge
          │
          ▼
model returns: "Done! Created PingAPI with 3 steps."
          │
ChatWebView.appendAssistantMessage(text)
```

---

## 4. Phase breakdown

### Phase A — UI: Sidebar replaces Slide  
**Touch points**: `AppMainFrame`, `SlideShow`, new `AICopilotSidebar`

| Step | What changes |
|------|-------------|
| A1 | Remove `"AICopilot"` from `slideShow.addSlide(...)` and remove the "AICopilot" toolbar button that calls `slideShow.showSlide("AICopilot")` |
| A2 | Wrap the current `SlideShow` and a new `AICopilotSidebar` in a `JSplitPane(HORIZONTAL_SPLIT)` and add that to `BorderLayout.CENTER` instead of `slideShow` directly |
| A3 | Create `AICopilotSidebar extends JPanel` — thin wrapper that assembles `ContextBar + PromptLibraryPanel + ChatWebView + InputPanel` in a `BorderLayout`. The existing `AICopilotUI` layout is refactored into this. |
| A4 | Add a **toggle button** ("✦ AI") to the existing `AppToolBar` that calls `splitPane.setDividerLocation(...)` to hide/show the sidebar. Persist the last divider position in `AppSettings`. |
| A5 | Default sidebar width: 360 px. Minimum: 280 px. Maximum: 50% of frame width. |
| A6 | `AICopilot` still implements `SlideShow.SlideChangeListener` — use the callback to update the `ContextBar` instead of swapping slides. |

**Key class**: `AICopilotSidebar.java` (new, in `ui/` package)

---

### Phase B — Prompt Library  
**Touch points**: new `PromptLibraryPanel`, `AICopilotSidebar`

The 41 prompts from `MCP-GETTING-STARTED.md` become clickable chips. Chips are grouped into collapsible sections backed by `JXTaskPane` (or a simple accordion with `JButton` headers + `JPanel`).

#### Section → Prompts mapping

| Section chip | Prompts included |
|-------------|-----------------|
| **Explore** | Prompt 1-A, 1-B, 1-C, 1-D |
| **Author** | Prompt 2-A, 2-B, 2-C, 2-D |
| **Object Repo** | Prompt 3-A, 3-B, 3-C, 3-D |
| **Data** | Prompt 4-A, 4-B, 4-C, 4-D |
| **Generate** | Prompt 5-A, 5-B, 5-C, 5-D, 5-E, 5-F |
| **Run** | Prompt 6-A, 6-B, 6-C |
| **Reports** | Prompt 7-A, 7-B, 7-C, 7-D, 7-E |
| **Test Sets** | Prompt 8-A, 8-B |
| **Browser** | Prompt 9-A, 9-B |
| **Preview** | Prompt 10-A, 10-B, 10-C, 10-D |
| **Config** | Prompt 11-A, 11-B |
| **Gauntlet** | Prompt 12 |

#### Prompt token substitution

Prompts reference `CLIDemo`, `APIBasics`, etc. as defaults. When a project or scenario is already selected in TestDesign, the `ContextBar` injects the live values via simple token replacement:

```
"${currentProject}"  →  AppMainFrame.getProject().getProjectName()
"${currentScenario}" →  TestDesign.getSelectedScenario()
"${currentTestCase}" →  TestDesign.getSelectedTestCase()
```

So clicking the "Drill into a scenario" chip generates:
> *"Show me everything inside the **APIBasics** scenario of **CLIDemo** — list all test cases with their step counts…"*

instead of the raw template.

**Key class**: `PromptLibraryPanel.java` (new), `PromptChip.java` (simple `JButton` subclass)

---

### Phase C — MCPToolBridge (connect 75 tools)  
**Touch points**: `INGeniousToolServer` (replace/extend), `AgentOrchestrator`, `MCPTools` (Engine, no changes)

The existing `INGeniousToolServer` has 8 tools backed by direct Datalib calls. We replace it with `MCPToolBridge` that delegates to `MCPTools.dispatch()` — the same engine that powers the external MCP server, now called **in-process**.

#### C1 · Create `MCPToolBridge.java`

```
IDE module → aichat/mcp/MCPToolBridge.java

Responsibility:
  1. At construction, create an MCPTools instance (Engine module)
     passing the active project root and AppMainFrame reference.
  2. Expose toolDefinitions() → List<Tool>
     by deserialising the JSON array returned by MCPTools.listTools().
  3. Execute a tool call:
     MCPToolBridge.execute(name, args) {
       JsonNode result = mcpTools.dispatch(name, args);   // in-process call
       triggerUIRefresh(name);                            // see C3
       return ToolResult.fromJson(result);
     }
  4. isReadOnly(name) — delegated to MCPTools tool metadata.
  5. toolDescriptions() — used to populate the approval dialog text.
```

#### C2 · Retire `INGeniousToolServer`

`INGeniousToolServer` is kept as a **read-only compatibility shim** for the 8 original tool names only (for any code that still references them). `AgentOrchestrator` is updated to accept a `MCPToolBridge` instead.

#### C3 · UI refresh hooks

When `MCPToolBridge` executes a mutating tool, it fires a named event so the IDE can refresh:

| Tool group | Refresh action |
|-----------|---------------|
| `testcase_*`, `testset_*`, `scenario_*` | `TestDesign.refreshTree()` |
| `object_*` | `TestDesign.refreshObjectRepository()` |
| `data_*`, `env_*` | `TestDesign.refreshDataSheets()` |
| `run`, `run_async` | Switch to TestExecution slide, reload run panel |
| `report_*` | Open/refresh Dashboard |
| `config_*` | Reload project config cache |

These are `SwingUtilities.invokeLater(...)` calls on the EDT, registered as `RefreshListener` callbacks so `MCPToolBridge` doesn't depend on concrete UI classes.

#### C4 · System prompt enhancement

`AuthoringSkill.systemPrompt()` is extended to prepend a live context snapshot:

```
You are the INGenious AI assistant…

# Current context
- Project: CLIDemo  (path: /…/Projects/CLIDemo)
- Open slide: TestDesign
- Selected scenario: APIBasics
- Selected test case: GetUsers  (6 steps)
- Selected step: 3 — action=assertResponseCode

# Available tools
[auto-generated from MCPToolBridge.toolDefinitions()]

# INGenious conventions
[existing AuthoringSkill.text()]
```

This is regenerated at the start of every agent turn so the model always has fresh context.

---

### Phase D — Inline tool-call feedback  
**Touch points**: `AgentOrchestrator`, `ChatWebView`, `AICopilotSidebar`

Currently `AgentOrchestrator.AgentListener` fires `onToolStart` / `onToolResult` but they are not visually surfaced. We add VS Code-style inline disclosure rows.

#### D1 · `ChatWebView` additions

Add two new JavaScript methods to the existing WebView HTML:

```js
// Append a "used tool" row (collapsible)
appendToolCall(id, toolName, argsSummary)

// Update the row with the result and mark done/error
resolveToolCall(id, resultSummary, isError)
```

Each row renders like:
```
▶  testcase_create  "HealthChecks / PingAPI"          [spinning]
✓  testcase_create  created: true, steps: 3            [green]
```

Clicking the `▶` expands the full JSON args/result in a monospace block.

#### D2 · Inline approval (non-blocking)

Replace the current `ToolApprovalDialog` modal with an inline approval row:

```
⚠️  testcase_create  will create "PingAPI" in "HealthChecks"
    [Apply]  [Skip]  [Cancel all]
```

The approval row is appended to the chat, `AgentOrchestrator` pauses on a `CountDownLatch(1)`, and the EDT resolves it when the user clicks a button. This is non-blocking to the UI thread.

#### D3 · Auto-scroll and progress indicator

The `AICopilotSidebar` shows a thin animated progress bar at the top (like VS Code's "Running..." bar) while the agent loop is active. It disappears when `onComplete()` fires.

---

### Phase E — Context awareness  
**Touch points**: `AICopilot`, `AppMainFrame`, `TestDesign`, `TestExecution`

#### E1 · `ContextBar` component

A new `JPanel` at the top of `AICopilotSidebar`:

```
[CLIDemo] › [APIBasics] › [GetUsers]   Step 3 of 6   [use as context]
```

- Breadcrumbs are `JButton`-style labels that navigate to the item when clicked
- "use as context" checkbox: when checked, the selected test case content is injected into every message as a `<context>` block

#### E2 · Selection change listener

`TestDesign` fires a `SelectionChangeEvent` whenever the user clicks a scenario, testcase, or step. `AICopilot` listens and:
1. Updates the `ContextBar` display
2. Regenerates the context snapshot string used in system prompt (Phase C4)
3. Optionally pre-fills the input with a context-relevant prompt (e.g., selecting a failing step could pre-fill "Explain why step 3 of GetUsers might fail")

#### E3 · Execution context

When TestExecution fires a run event, `AICopilot` receives `onRunStart(target)` and `onRunComplete(report)`. The AI sidebar automatically shows a "run in progress" indicator, and when the run completes, a notification chip appears: _"Run finished — 3 passed, 1 failed.  [View failures]"_ — clicking it fires the `explain_failure` prompt.

---

## 5. New files to create

| Module | File | Purpose |
|--------|------|---------|
| IDE | `aichat/ui/AICopilotSidebar.java` | Replaces `AICopilotUI` as the sidebar container |
| IDE | `aichat/ui/ContextBar.java` | Live context breadcrumbs strip |
| IDE | `aichat/ui/PromptLibraryPanel.java` | Collapsible prompt chip accordion |
| IDE | `aichat/ui/PromptChip.java` | Single chip button with template + label |
| IDE | `aichat/ui/InlineApprovalRow.java` | Replaces `ToolApprovalDialog` |
| IDE | `aichat/mcp/MCPToolBridge.java` | In-process bridge to `MCPTools.dispatch()` |
| IDE | `aichat/mcp/RefreshListener.java` | Interface: `onMutation(toolName, result)` |
| IDE | `aichat/model/PromptTemplate.java` | Data class: section, label, templateText, tokens[] |
| IDE | `aichat/skills/PromptLibrary.java` | Registry of all 41 prompts with token metadata |
| Engine | (none — `MCPTools.java` unchanged) | Bridge calls it directly |

---

## 6. Files to modify

| File | Change |
|------|--------|
| `AppMainFrame.java` | Wrap `slideShow` + `AICopilotSidebar` in `JSplitPane`; remove `slideShow.addSlide("AICopilot", ...)` |
| `AppToolBar.java` | Add "✦ AI" toggle button; remove old "AICopilot" nav button |
| `AICopilot.java` | Swap `INGeniousToolServer` → `MCPToolBridge`; add context snapshot injection; listen to selection changes and run events |
| `AgentOrchestrator.java` | Accept `MCPToolBridge` instead of `INGeniousToolServer`; add `CountDownLatch` pause for inline approval; increase `MAX_ITERATIONS` from 8 to 20 (75-tool workflows are longer) |
| `AICopilotUI.java` | Refactor into `AICopilotSidebar` — extract `ContextBar` and `PromptLibraryPanel` slots |
| `ChatWebView.java` | Add `appendToolCall()` / `resolveToolCall()` JS + CSS for tool call disclosure rows |
| `AuthoringSkill.java` | Extend `systemPrompt()` to accept context snapshot string |
| `AppSettings.java` | Add `aiSidebarWidth`, `aiSidebarVisible`, `promptLibraryExpanded` keys |

---

## 7. Prompt Library — full registry (`PromptLibrary.java`)

Each entry has:
- **id** (e.g. `"explore-1a"`)
- **section** (e.g. `"Explore"`)
- **label** (short chip label, e.g. `"Orientation"`)
- **template** (full prompt text with `${currentProject}` tokens)
- **tokens[]** (list of token names so the UI can show "requires selection" warning)

```java
// Example entries
PromptTemplate.of("explore-1a", "Explore", "Orientation",
    "What INGenious projects are available? For each one, tell me how many "
    + "scenarios and test cases it contains.")

PromptTemplate.of("explore-1b", "Explore", "Drill into scenario",
    "Show me everything inside the ${currentScenario} scenario of "
    + "${currentProject} — list all test cases with their step counts, "
    + "then show me the full steps for ${currentTestCase}.",
    "currentProject", "currentScenario", "currentTestCase")

PromptTemplate.of("author-2a", "Author", "API test from scratch",
    "In ${currentProject}, create a new scenario called \"HealthChecks\". "
    + "Inside it, create a test case called \"PingAPI\" that:\n"
    + "  1. Sets the endpoint to https://jsonplaceholder.typicode.com/users/1\n"
    + "  2. Sends a GET request\n"
    + "  3. Verifies the response code is 200\n"
    + "  4. Verifies the JSON field \"name\" equals \"Leanne Graham\"\n\n"
    + "Look up the correct action names before creating the steps.",
    "currentProject")

// ... 38 more entries mirroring MCP-GETTING-STARTED.md sections 1–12
```

---

## 8. MCPToolBridge — key implementation sketch

```java
// IDE/src/main/java/com/ing/ide/main/mainui/components/aichat/mcp/MCPToolBridge.java

public class MCPToolBridge {
    private final MCPTools mcpTools;          // Engine module, in-process
    private final ObjectMapper json = new ObjectMapper();
    private final List<RefreshListener> refreshListeners = new ArrayList<>();

    public MCPToolBridge(AppMainFrame frame) {
        // MCPTools needs the project root and a project resolver
        this.mcpTools = new MCPTools(json, new IDEProjectResolver(frame));
    }

    /** Returns tool definitions in OpenAI tool_choice format. */
    public List<Tool> toolDefinitions() {
        // MCPTools.listTools() returns a JsonNode; convert to List<Tool>
        JsonNode tools = mcpTools.listTools();
        List<Tool> result = new ArrayList<>();
        for (JsonNode t : tools) {
            result.add(Tool.fromMCPDescriptor(t));
        }
        return result;
    }

    /** Executes one tool call. Safe to call from the agent background thread. */
    public ToolResult execute(String name, JsonNode args) {
        try {
            JsonNode response = mcpTools.dispatch(name, args);
            boolean isError = response.path("error").isMissingNode() == false;
            String summary = extractSummary(response);

            if (!isError && isMutation(name)) {
                fireRefresh(name, response);
            }

            return new ToolResult(name, summary, isError, response);
        } catch (Exception e) {
            return ToolResult.error(name, e.getMessage());
        }
    }

    public boolean isReadOnly(String name) {
        // Prefix-based: list_*, show, info, search, gen_list = read-only
        return name.matches("ingenious_(project_list|project_info|scenario_list|"
            + "scenario_info|testcase_list|testcase_show|testcase_validate|"
            + "action_.*|object_list|object_show|object_search|"
            + "data_show|data_get|data_list|env_list|"
            + "gen_list|report_.*|config_show|config_drivers|config_get|"
            + "run_status|run_logs|testset_list|testset_show|doctor)");
    }

    private static final Set<String> MUTATION_PREFIXES = Set.of(
        "testcase_create", "testcase_add_step", "testcase_edit_step",
        "testcase_insert_step", "testcase_remove_step", "testcase_move_step",
        "testcase_delete", "scenario_create", "scenario_delete",
        "object_add", "object_update", "object_delete", "object_import_page",
        "data_sheet_create", "data_column_add", "data_row_add", "data_set",
        "data_row_delete", "data_import", "env_create", "env_delete",
        "testset_create", "testset_add", "gen_testcase", "gen_from_openapi",
        "gen_from_har", "import_curl", "import_postman", "import_bruno",
        "import_playwright", "config_set", "run", "run_async"
    );

    private boolean isMutation(String name) {
        String stripped = name.replace("ingenious_", "");
        return MUTATION_PREFIXES.contains(stripped);
    }

    private void fireRefresh(String toolName, JsonNode result) {
        SwingUtilities.invokeLater(() ->
            refreshListeners.forEach(l -> l.onMutation(toolName, result)));
    }

    public void addRefreshListener(RefreshListener l) { refreshListeners.add(l); }
}
```

---

## 9. Inline tool call rendering — ChatWebView additions

The existing `ChatWebView` renders an HTML page in a JavaFX `WebView`. Add these CSS classes and JS functions to the page template:

```css
.tool-call-row {
    display: flex; align-items: center; gap: 8px;
    padding: 4px 12px; font-size: 12px;
    color: var(--text-secondary); cursor: pointer;
}
.tool-call-row .icon { font-size: 10px; }
.tool-call-row .name { font-family: monospace; color: var(--text-accent); }
.tool-call-row .args { opacity: 0.7; }
.tool-call-row.done .icon::before { content: "✓"; color: green; }
.tool-call-row.error .icon::before { content: "✗"; color: red; }
.tool-call-detail { display: none; padding: 4px 24px; font-size: 11px;
    font-family: monospace; white-space: pre; }
.tool-call-row.expanded + .tool-call-detail { display: block; }

.approval-row {
    display: flex; gap: 8px; align-items: center;
    padding: 8px 12px; background: var(--warning-bg);
    border-left: 3px solid var(--warning-color);
}
```

```js
function appendToolCall(id, toolName, argsSummary) {
    const row = `
      <div class="tool-call-row" id="tc-${id}" onclick="toggleDetail('${id}')">
        <span class="icon spin">◌</span>
        <span class="name">${toolName}</span>
        <span class="args">${argsSummary}</span>
      </div>
      <div class="tool-call-detail" id="td-${id}"></div>`;
    document.getElementById('messages').insertAdjacentHTML('beforeend', row);
    scrollToBottom();
}

function resolveToolCall(id, resultSummary, isError, fullJson) {
    const row = document.getElementById('tc-' + id);
    if (!row) return;
    row.classList.remove('spin');
    row.classList.add(isError ? 'error' : 'done');
    row.querySelector('.icon').textContent = isError ? '✗' : '✓';
    row.querySelector('.args').textContent = resultSummary;
    document.getElementById('td-' + id).textContent = fullJson;
}

function appendApproval(id, toolName, summary) {
    const row = `
      <div class="approval-row" id="apr-${id}">
        <span>⚠️ <b>${toolName}</b> — ${summary}</span>
        <button onclick="approve('${id}', true)">Apply</button>
        <button onclick="approve('${id}', false)">Skip</button>
        <button onclick="cancelAll()">Cancel all</button>
      </div>`;
    document.getElementById('messages').insertAdjacentHTML('beforeend', row);
}

function approve(id, accepted) {
    document.getElementById('apr-' + id).remove();
    javaBridge.onApproval(id, accepted);   // calls back into Java
}
```

---

## 10. System prompt context snapshot

Generated at the start of every agent turn inside `AICopilot.buildContextSnapshot()`:

```
# Current IDE context
Project   : CLIDemo  (/path/to/Projects/CLIDemo)
Open view : TestDesign
Scenario  : APIBasics  (5 test cases)
Test case : GetUsers   (6 steps)
  Step 1: action=setEndPoint  input=https://...
  Step 2: action=getRestRequest
  Step 3: action=assertResponseCode  input=200
  ...

# Your available tools
ingenious_project_list      – List all projects
ingenious_scenario_list     – List scenarios in a project
ingenious_testcase_create   – Create a test case (dryRun, ifExists supported)
... (all 75, one line each)
```

This replaces the current static system prompt and keeps the model from hallucinating names.

---

## 11. Implementation phases & effort estimates

| Phase | Scope | Rough effort |
|-------|-------|-------------|
| **A — Sidebar UI** | `AppMainFrame` + `AICopilotSidebar` + toolbar toggle | 3–5 days |
| **B — Prompt Library** | `PromptLibraryPanel` + `PromptLibrary` registry (41 prompts) | 2–3 days |
| **C — MCPToolBridge** | `MCPToolBridge` + refresh hooks + system prompt context | 3–5 days |
| **D — Inline feedback** | `ChatWebView` JS/CSS + `InlineApprovalRow` + `AgentOrchestrator` pause | 3–4 days |
| **E — Context awareness** | `ContextBar` + selection listeners + execution notifications | 2–3 days |

Suggested order: **A → C → B → D → E** (get the sidebar and tools working first, then add the UX polish).

---

## 12. Dependencies & risks

| Risk | Mitigation |
|------|-----------|
| `MCPTools` constructs with different assumptions than IDE environment | Add `IDEProjectResolver` adapter so MCPTools reads project from `AppMainFrame.getProject()` rather than a CLI `--project` flag |
| JavaFX WebView `jsObject.call()` threading | All `appendToolCall` / `resolveToolCall` JS calls must be dispatched via `Platform.runLater(...)` |
| `MCPTools.dispatch()` may do file I/O on the EDT | Agent loop already runs on a background thread; ensure `MCPToolBridge.execute()` is never called on the EDT |
| AgentOrchestrator `MAX_ITERATIONS = 8` too low for 75-tool chains | Raise to 20; add configurable limit in `AISettingsDialog` |
| Sidebar takes up space on small screens | Make sidebar collapsible by default on screens < 1280 px wide; persist state per machine in `AppSettings` |
| GitHub Models API token expiry mid-session | Existing device auth refresh flow in `AICopilot` already handles this; expose it in `ContextBar` as a subtle "re-auth" indicator |

---

## 13. Summary of what the user gets

After all phases:

1. **Open INGenious, start working in TestDesign** — the AI sidebar is visible on the right without needing to switch views
2. **Click any prompt chip** (e.g. "API test from scratch") — the template fills the input with the current project and scenario already substituted
3. **Press Send** — the AI discovers the right action names, then creates the test case. Each tool call shows inline as a collapsible row. The TestDesign tree refreshes automatically when steps are added.
4. **Mutating tools show an inline "Apply?" button** — no modal, no interruption to the test design view
5. **Run finishes** — a chip appears: "3 passed, 1 failed. [View failures]" — clicking it fires the `explain_failure` prompt automatically
6. **All 75 MCP tools available** — everything documented in `MCP-GETTING-STARTED.md` works directly in the IDE chat, no external client needed
