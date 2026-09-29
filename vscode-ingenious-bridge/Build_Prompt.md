# Build Prompt — VSCode Copilot LLM Bridge

> **How to use this file:** Move only this `.md` file to your VM. In an empty folder,
> open your AI coding assistant and say:
> *"Read BUILD_PROMPT.md and build the complete VS Code extension it describes,
> end to end. Create every file, install dependencies, compile, and package it."*
>
> This document is the full specification. It is written so an agent can implement it
> without ever seeing the original source. Sections marked **VERBATIM** contain exact
> API contracts that must be reproduced as-is — these use VS Code APIs that are easy to
> get wrong, so do not paraphrase them.

---

## 1. What you are building

A VS Code extension that exposes the user's **GitHub Copilot** language models as a
**local HTTP API**. External programs (Python, cURL, etc.) POST a prompt to a local
port and get the model's text response back as JSON.

Critically: the extension does **not** call GitHub, OpenAI, or any API directly and
holds **no API keys**. It uses VS Code's built-in Language Model API (`vscode.lm`),
which brokers the request to the installed GitHub Copilot Chat extension, which in turn
owns the authentication and the real network call. The data flow is:

```
HTTP client ──POST /ask──▶ Express server (in extension)
                              │  vscode.lm.selectChatModels() + model.sendRequest()
                              ▼
                          VS Code Language Model API
                              ▼
                          GitHub Copilot Chat extension (auth + network)
                              ▼
                          Copilot backend ──streamed text──▶ back up the chain ──▶ JSON
```

---

## 2. Tech stack & metadata

- **Language:** TypeScript, compiled to CommonJS JavaScript in `./out`.
- **Runtime dep:** `express` v5.
- **VS Code engine:** target `^1.94.0` (works on newer; if the API errors, the user can
  bump this to match their VS Code version).
- **Publisher/name:** `vscode-copilot-llm-bridge` (any publisher id is fine for local use).
- **Activation:** `onStartupFinished`.
- **Packaging tool:** `@vscode/vsce`.

### `package.json` (reproduce essentially as-is)

```json
{
  "name": "vscode-copilot-llm-bridge",
  "displayName": "VSCode Copilot LLM Bridge",
  "description": "Bridges gap between your code and VSCode LLM",
  "publisher": "local",
  "version": "1.1.0",
  "engines": { "vscode": "^1.94.0" },
  "categories": ["Other"],
  "activationEvents": ["onStartupFinished"],
  "main": "./out/extension.js",
  "contributes": {
    "viewsContainers": {
      "activitybar": [
        { "id": "llm-bridge-explorer", "title": "VSCode Copilot LLM Bridge", "icon": "$(radio-tower)" }
      ]
    },
    "views": {
      "llm-bridge-explorer": [
        { "type": "webview", "id": "llm-sidebar-view", "name": "Panel" }
      ]
    }
  },
  "scripts": {
    "vscode:prepublish": "npm run compile",
    "compile": "tsc -p ./",
    "watch": "tsc -watch -p ./",
    "lint": "eslint src"
  },
  "devDependencies": {
    "@types/express": "^5.0.6",
    "@types/node": "22.x",
    "@types/vscode": "^1.94.0",
    "typescript": "^5.9.3"
  },
  "dependencies": {
    "express": "^5.2.1"
  }
}
```

Also create a standard `tsconfig.json` for a VS Code extension: target `ES2022`,
module `Node16`/`commonjs`, `outDir: "out"`, `rootDir: "src"`, `strict: true`,
`lib: ["ES2022"]`.

---

## 3. File layout

```
.
├── package.json
├── tsconfig.json
└── src/
    └── extension.ts      ← ALL logic lives in this one file
```

Everything below describes `src/extension.ts`.

---

## 4. `activate(context)` — entry point

1. Create an Express app, enable `express.json()` middleware.
2. Construct a single `SidebarProvider` instance, passing it the extension URI and the
   Express `app`. Keep it in a module-level `let provider`.
3. Register the `POST /ask` route (section 5).
4. Register the webview provider for view id `llm-sidebar-view`.
5. Push a disposable that calls `provider.stopServer()` on deactivation.

> Note: do **not** start the HTTP listener here. The server only starts when the user
> clicks START in the sidebar (section 7).

---

## 5. `POST /ask` handler — the core endpoint

**Request body:** `{ model_name?: string, prompt: string, system_prompt?: string, retain_history?: boolean }`

Behavior, in order:

1. If `retain_history === false`, wipe `provider.history = []`.
2. **Discover models** (VERBATIM — this exact call):
   ```ts
   const allModels = await vscode.lm.selectChatModels({});
   ```
   This returns an array of `vscode.LanguageModelChat`. Each has `id`, `family`,
   `vendor`, `name`. The list reflects whatever the user's Copilot subscription exposes.
   The first time it runs, VS Code shows a one-time "Allow access" consent prompt.
3. **Select a model** via this fallback chain:
   - If `model_name` given: `allModels.find(m => m.family === model_name || m.id === model_name)`.
   - Else if a UI model was selected: match on `m.family === provider.selectedModel.family`.
   - Else fallback: `const [m] = await vscode.lm.selectChatModels({ family: 'gpt-4o' });`
   - If still none, throw `Error("Model ... not found")`.
4. **Build the payload** as `vscode.LanguageModelChatMessage[]`:
   - System prompt = `system_prompt || provider.sidebarSystemPrompt || null`.
     If present, push it. **(See FIX note below — the original pushes it as an
     `Assistant` message, which is wrong.)**
   - Spread in `...provider.history`.
   - Create `const userMessage = vscode.LanguageModelChatMessage.User(prompt)` and push it.
5. **Send the request** (VERBATIM):
   ```ts
   const chatRequest = await model.sendRequest(payload);
   ```
6. **Consume the streamed response** (VERBATIM — `chatRequest.text` is an async iterable
   of string fragments):
   ```ts
   let responseText = "";
   for await (const fragment of chatRequest.text) { responseText += fragment; }
   ```
7. **Update history** if `retain_history !== false`:
   - Push `userMessage`, then push `vscode.LanguageModelChatMessage.Assistant(responseText)`.
   - Call `await provider.updateHybridHistory(model)` (section 8).
8. Call `provider.addLogEntry(prompt, responseText, model.family)`.
9. Respond `res.json({ response: responseText, model_used: model.family })`.
10. Wrap everything in try/catch; on error respond `res.status(500).json({ error: err.message })`.

> **FIX note (the original has bugs — fix these in your build):**
> - The system prompt should NOT be sent via `.Assistant(...)`. This API has only
>   `.User()` and `.Assistant()` constructors. Prepend the system prompt to the first
>   user message, or send it as a leading `.User()` message — not Assistant.
> - `prompt` is required; return a 400 if it's missing instead of letting it throw.

---

## 6. `SidebarProvider` class — state, server lifecycle, UI

Implements `vscode.WebviewViewProvider`. Fields:

- `selectedModel?: { vendor, family }` — set from the UI dropdown.
- `sidebarSystemPrompt: string` — set from the UI textarea.
- `history: vscode.LanguageModelChatMessage[]` — conversation context.
- private `_view?`, `_serverInstance`, `_serverPort`, `_isServerRunning`, `_app`.

Constructor stores the extension URI and the Express `app`.

---

## 7. Server lifecycle methods

- `setPort(port)`: store `_serverPort`; if `_view` exists, `postMessage({ type:'portUpdate', value: port })`.
- `serverToggle()`: if running → `stopServer()`, else `startServer()`.
- `startServer()`: **(VERBATIM)** listen on port `0` so the OS assigns a free port:
  ```ts
  this._serverInstance = this._app.listen(0, () => {
      const assignedPort = this._serverInstance.address().port;
      this._isServerRunning = true;
      this.setPort(assignedPort);
      vscode.window.showInformationMessage("VSCode Copilot LLM Bridge is Active");
  });
  this._serverInstance.on('error', err => { vscode.window.showErrorMessage(`Server Error: ${err.message}`); this.stopServer(); });
  ```
- `stopServer()`: guard against `_serverInstance` being undefined (the original does
  not — **fix this**), then `close()` it, reset flags, `setPort(0)`, show a warning.

---

## 8. `updateHybridHistory(model)` — token-saving compression

Goal: keep the context short. When `history.length > 8`:

1. `rawMessages = history.slice(-4)` (keep last 4 raw).
2. `toSummarize = history.slice(0, -4)` (older ones).
3. Build a summary prompt as a `User` message asking the model for a dense, factual
   "Context Snapshot" with sections: ACTIVE GOAL, DATA/ENTITIES, DECISIONS MADE,
   CONSTRAINTS (no conversational filler).
4. `await model.sendRequest([...toSummarize, summaryPrompt])`, stream its `.text` into a string.
5. Wrap result as `vscode.LanguageModelChatMessage.Assistant("[INTERNAL MEMORY SNAPSHOT]\n" + summaryText)`.
6. Replace history: `this.history = [summaryMessage, ...rawMessages]`.
7. `addLogEntry("Internal Context Compression", "[INTERNAL MEMORY SNAPSHOT]\n"+summaryText, "System")`.
8. Wrap in try/catch; on failure just `console.error` and leave history unchanged.

---

## 9. Webview (`resolveWebviewView`) and UI

- Set `webview.options = { enableScripts: true }`.
- `updateHtml()` helper: call `vscode.lm.selectChatModels({})` up to **3 times** with a
  1-second wait between tries (models may not be ready immediately after startup).
  Build `<option>` tags for each model: value = `JSON.stringify({vendor, family})`,
  label = `${vendor} - ${family}`. If none found, render one disabled option
  "No models found - Check Copilot Login". Then set `webview.html = _generateHtml(options)`.
- Handle inbound webview messages by `msg.type`:
  - `webviewReady` → `syncWebviewState()` (and resend port).
  - `modelChanged` → `selectedModel = JSON.parse(msg.value)`.
  - `serverToggle` → `serverToggle()`.
  - `systemPromptChanged` → `sidebarSystemPrompt = msg.value`.
  - `refreshModels` → `updateHtml()`.
  - `clearHistory` → `history = []`.

`syncWebviewState()` re-pushes port, system prompt, selected model, and replays history
log entries so a reopened panel isn't blank.

`addLogEntry(prompt, response, modelName)` → `_view?.webview.postMessage({ type:'newLog', data:{prompt,response,modelName} })`.

### `_generateHtml(options)` — the panel UI

Return an HTML string (dark GitHub-style theme, using `--vscode-*` CSS vars) containing:

- A **status bar**: a status dot, text `BRIDGE: <Offline|Port N>`, and a Start/Stop toggle button.
- An action row: **Clear History** and **Refresh Models** buttons.
- A **System Prompt** `<textarea>` (`oninput` posts `systemPromptChanged`).
- A **Model Selector** `<select>` populated with `${options}` (`onchange` posts `modelChanged`).
- Two tabs: **Current** and **History (Context)**, each with a logs container.
- A `<script>` that: calls `acquireVsCodeApi()`, posts `webviewReady` on load, implements
  tab switching, and listens for messages:
  - `portUpdate` → toggle dot/button text ("Start"/"Stop") and show `Port N` or `Offline`.
  - `syncSystemPrompt`, `syncModel` → restore field values.
  - `newLog` → build a card; if the response contains `"[INTERNAL MEMORY SNAPSHOT]"`,
    render it as a "Memory Snapshot" card in the **History** tab; otherwise a "Response"
    card in the **Current** tab. **Always HTML-escape** the response text before inserting
    it (XSS safety): escape `& < > " '`.

---

## 10. Build, run, package

On the VM, after the agent generates the files:

```bash
npm install
npm run compile          # tsc → out/extension.js
```

To test: open the folder in VS Code and press **F5** (Extension Development Host), open
the "VSCode Copilot LLM Bridge" view from the activity bar, click **START**, note the port.

To package into an installable `.vsix`:

```bash
npm i -g @vscode/vsce     # or npx @vscode/vsce
vsce package              # produces vscode-copilot-llm-bridge-1.1.0.vsix
```

Install the `.vsix` via VS Code → Extensions → "··· → Install from VSIX".

**Prerequisites on the VM:** Node.js, VS Code with the **GitHub Copilot Chat** extension
installed and signed in to a Copilot-entitled account. Without Copilot, `selectChatModels`
returns an empty list and the bridge has nothing to call.

---

## 11. Verifying it works

Send a request to the port shown in the panel:

```python
import requests
r = requests.post("http://localhost:PORT/ask", json={
    "model_name": "gpt-4o",
    "prompt": "What is 2+3?",
    "system_prompt": "You are a precise mathematician.",
    "retain_history": True
})
print(r.json()["response"])
```

Expected: a 200 with `{ "response": "...", "model_used": "gpt-4o" }`. The first call may
trigger VS Code's consent prompt — accept it.

---

## 12. Summary of contracts that must not be paraphrased

- `await vscode.lm.selectChatModels({})` — model discovery.
- `await vscode.lm.selectChatModels({ family: 'gpt-4o' })` — fallback.
- `vscode.LanguageModelChatMessage.User(text)` / `.Assistant(text)` — message construction.
- `const chatRequest = await model.sendRequest(payload);`
- `for await (const fragment of chatRequest.text) { ... }` — streaming consumption.
- Express listens on port `0` for OS-assigned port.

Everything else (UI styling, logging text, status bar messages) can be implemented freely.
```
 