import * as vscode from 'vscode';
import express, { Express, Request, Response } from 'express';
import { Server } from 'http';
import { AddressInfo } from 'net';
import * as fs from 'fs';
import * as os from 'os';
import * as path from 'path';

// Module-level provider reference
let provider: SidebarProvider;

// Preferred loopback port for the OpenAI-compatible endpoint. INGenious (CLI + IDE)
// point at http://127.0.0.1:<port>/v1. Override with INGENIOUS_BRIDGE_PORT.
const PREFERRED_PORT: number = (() => {
    const env = process.env.INGENIOUS_BRIDGE_PORT;
    const n = env ? parseInt(env, 10) : NaN;
    return Number.isInteger(n) && n > 0 && n < 65536 ? n : 8765;
})();

// Discovery file so INGenious can find the (possibly fallback) port without config.
const DISCOVERY_FILE: string = path.join(os.homedir(), '.ingenious', 'bridge.json');

function writeDiscoveryFile(port: number): void {
    try {
        fs.mkdirSync(path.dirname(DISCOVERY_FILE), { recursive: true });
        const info = {
            host: '127.0.0.1',
            port,
            baseUrl: `http://127.0.0.1:${port}/v1`,
            pid: process.pid,
            startedAt: new Date().toISOString()
        };
        fs.writeFileSync(DISCOVERY_FILE, JSON.stringify(info, null, 2), 'utf8');
    } catch (e) {
        console.error('[LLM Bridge] Failed to write discovery file:', e);
    }
}

function removeDiscoveryFile(): void {
    try {
        if (fs.existsSync(DISCOVERY_FILE)) {
            fs.unlinkSync(DISCOVERY_FILE);
        }
    } catch (e) {
        console.error('[LLM Bridge] Failed to remove discovery file:', e);
    }
}

// Output channel for logging
let outputChannel: vscode.OutputChannel;

const LOG_FILE: string = path.join(os.homedir(), '.ingenious', 'bridge.log');

function log(message: string): void {
    const timestamp = new Date().toISOString();
    const line = `[${timestamp}] ${message}`;
    try {
        outputChannel?.appendLine(line);
    } catch (e) {
        // output channel not ready yet
    }
    console.log(`[LLM Bridge] ${message}`);
    try {
        fs.mkdirSync(path.dirname(LOG_FILE), { recursive: true });
        fs.appendFileSync(LOG_FILE, line + '\n');
    } catch (e) {
        // best-effort file logging
    }
}

interface AskRequestBody {
    model_name?: string;
    prompt: string;
    system_prompt?: string;
    retain_history?: boolean;
}

interface LogEntry {
    prompt: string;
    response: string;
    modelName: string;
}

interface SelectedModel {
    vendor: string;
    family: string;
}

// ============================================================================
// SidebarProvider - State, Server Lifecycle, UI
// ============================================================================
class SidebarProvider implements vscode.WebviewViewProvider {
    public selectedModel?: SelectedModel;
    public sidebarSystemPrompt: string = '';
    public history: vscode.LanguageModelChatMessage[] = [];

    private _view?: vscode.WebviewView;
    private _serverInstance?: Server;
    private _serverPort: number = 0;
    private _isServerRunning: boolean = false;
    private _app: Express;
    private _extensionUri: vscode.Uri;
    private _logEntries: LogEntry[] = [];

    constructor(extensionUri: vscode.Uri, app: Express) {
        this._extensionUri = extensionUri;
        this._app = app;
    }

    // ========================================================================
    // Server Lifecycle Methods
    // ========================================================================

    public setPort(port: number): void {
        this._serverPort = port;
        if (this._view) {
            this._view.webview.postMessage({ type: 'portUpdate', value: port });
        }
    }

    public serverToggle(): void {
        if (this._isServerRunning) {
            this.stopServer();
        } else {
            this.startServer();
        }
    }

    public startServer(): void {
        log('Starting server...');
        // Bind to loopback only (never expose the Copilot proxy on the network).
        // Try the preferred fixed port first; on EADDRINUSE fall back to an
        // OS-assigned port and rely on the discovery file for the actual value.
        this._startOn(PREFERRED_PORT, true);
    }

    private _startOn(port: number, allowFallback: boolean): void {
        const server = this._app.listen(port, '127.0.0.1', () => {
            const addressInfo = server.address() as AddressInfo;
            const assignedPort = addressInfo.port;
            this._serverInstance = server;
            this._isServerRunning = true;
            this.setPort(assignedPort);
            writeDiscoveryFile(assignedPort);
            log(`Server started on 127.0.0.1:${assignedPort}`);
            vscode.window.showInformationMessage(`VSCode INGenious Bridge is Active on 127.0.0.1:${assignedPort}`);
        });

        server.on('error', (err: NodeJS.ErrnoException) => {
            if (err.code === 'EADDRINUSE' && allowFallback) {
                log(`Port ${port} in use; falling back to an OS-assigned port.`);
                this._startOn(0, false);
                return;
            }
            log(`Server error: ${err.message}`);
            vscode.window.showErrorMessage(`Server Error: ${err.message}`);
            this.stopServer();
        });
    }

    public stopServer(): void {
        // Guard against undefined _serverInstance (bug fix)
        if (this._serverInstance) {
            this._serverInstance.close();
        }
        this._serverInstance = undefined;
        this._isServerRunning = false;
        this.setPort(0);
        removeDiscoveryFile();
        vscode.window.showWarningMessage("VSCode INGenious Bridge has been stopped");
    }

    // ========================================================================
    // Hybrid History Compression
    // ========================================================================

    public async updateHybridHistory(model: vscode.LanguageModelChat): Promise<void> {
        if (this.history.length <= 8) {
            return;
        }

        try {
            // Keep last 4 messages raw
            const rawMessages = this.history.slice(-4);
            // Older messages to summarize
            const toSummarize = this.history.slice(0, -4);

            // Build summary prompt
            const summaryPromptText = `Analyze the conversation above and produce a dense, factual "Context Snapshot" with these sections:
- ACTIVE GOAL: What is the user trying to accomplish?
- DATA/ENTITIES: Key data, variables, files, or entities mentioned
- DECISIONS MADE: Important choices or conclusions reached
- CONSTRAINTS: Any limitations or requirements established

Be extremely concise. No conversational filler. Facts only.`;

            const summaryTextPart = new vscode.LanguageModelTextPart(summaryPromptText);
            const summaryPrompt = vscode.LanguageModelChatMessage.User([summaryTextPart]);

            // Request summary from model
            const summaryRequest = await model.sendRequest([...toSummarize, summaryPrompt]);

            let summaryText = '';
            for await (const fragment of summaryRequest.text) {
                summaryText += fragment;
            }

            // Wrap as internal memory snapshot
            const snapshotContent = `[INTERNAL MEMORY SNAPSHOT]\n${summaryText}`;
            const snapshotTextPart = new vscode.LanguageModelTextPart(snapshotContent);
            const summaryMessage = vscode.LanguageModelChatMessage.Assistant([snapshotTextPart]);

            // Replace history with compressed version
            this.history = [summaryMessage, ...rawMessages];

            this.addLogEntry("Internal Context Compression", snapshotContent, "System");

        } catch (err) {
            console.error('Failed to compress history:', err);
            // Leave history unchanged on failure
        }
    }

    // ========================================================================
    // Logging
    // ========================================================================

    public addLogEntry(prompt: string, response: string, modelName: string): void {
        const entry: LogEntry = { prompt, response, modelName };
        this._logEntries.push(entry);

        if (this._view) {
            this._view.webview.postMessage({
                type: 'newLog',
                data: entry
            });
        }
    }

    // ========================================================================
    // Webview Provider Implementation
    // ========================================================================

    public resolveWebviewView(
        webviewView: vscode.WebviewView,
        _context: vscode.WebviewViewResolveContext,
        _token: vscode.CancellationToken
    ): void {
        this._view = webviewView;

        webviewView.webview.options = {
            enableScripts: true
        };

        this.updateHtml();

        // Handle messages from webview
        webviewView.webview.onDidReceiveMessage(async (msg) => {
            switch (msg.type) {
                case 'webviewReady':
                    this.syncWebviewState();
                    break;
                case 'modelChanged':
                    this.selectedModel = JSON.parse(msg.value);
                    break;
                case 'serverToggle':
                    this.serverToggle();
                    break;
                case 'systemPromptChanged':
                    this.sidebarSystemPrompt = msg.value;
                    break;
                case 'refreshModels':
                    await this.updateHtml();
                    break;
                case 'clearHistory':
                    this.history = [];
                    this._logEntries = [];
                    vscode.window.showInformationMessage("History cleared");
                    break;
            }
        });
    }

    private async updateHtml(): Promise<void> {
        if (!this._view) {
            return;
        }

        let models: vscode.LanguageModelChat[] = [];

        // Try up to 3 times with 1-second waits (models may not be ready immediately)
        for (let attempt = 0; attempt < 3; attempt++) {
            models = await vscode.lm.selectChatModels({});
            if (models.length > 0) {
                break;
            }
            await this.sleep(1000);
        }

        let optionsHtml: string;
        if (models.length > 0) {
            optionsHtml = models.map(m => {
                const value = JSON.stringify({ vendor: m.vendor, family: m.family });
                const escapedValue = this.escapeHtml(value);
                return `<option value="${escapedValue}">${this.escapeHtml(m.vendor)} - ${this.escapeHtml(m.family)}</option>`;
            }).join('\n');
        } else {
            optionsHtml = `<option value="" disabled selected>No models found - Check Copilot Login</option>`;
        }

        this._view.webview.html = this._generateHtml(optionsHtml);
    }

    private syncWebviewState(): void {
        if (!this._view) {
            return;
        }

        // Resend port
        this._view.webview.postMessage({
            type: 'portUpdate',
            value: this._serverPort
        });

        // Resend system prompt
        this._view.webview.postMessage({
            type: 'syncSystemPrompt',
            value: this.sidebarSystemPrompt
        });

        // Resend selected model
        if (this.selectedModel) {
            this._view.webview.postMessage({
                type: 'syncModel',
                value: JSON.stringify(this.selectedModel)
            });
        }

        // Replay log entries
        for (const entry of this._logEntries) {
            this._view.webview.postMessage({
                type: 'newLog',
                data: entry
            });
        }
    }

    private sleep(ms: number): Promise<void> {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    private escapeHtml(text: string): string {
        const map: Record<string, string> = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.replace(/[&<>"']/g, char => map[char]);
    }

    private _generateHtml(options: string): string {
        return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>VSCode INGenious Bridge</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: var(--vscode-font-family);
            font-size: var(--vscode-font-size);
            color: var(--vscode-foreground);
            background-color: var(--vscode-sideBar-background);
            padding: 12px;
        }
        .status-bar {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 8px 12px;
            background-color: var(--vscode-editor-background);
            border-radius: 6px;
            margin-bottom: 12px;
        }
        .status-dot {
            width: 10px;
            height: 10px;
            border-radius: 50%;
            background-color: #6b7280;
        }
        .status-dot.active {
            background-color: #22c55e;
        }
        .status-text {
            flex: 1;
            font-weight: 500;
        }
        .toggle-btn {
            padding: 6px 12px;
            background-color: var(--vscode-button-background);
            color: var(--vscode-button-foreground);
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 12px;
        }
        .toggle-btn:hover {
            background-color: var(--vscode-button-hoverBackground);
        }
        .action-row {
            display: flex;
            gap: 8px;
            margin-bottom: 12px;
        }
        .action-btn {
            flex: 1;
            padding: 6px 8px;
            background-color: var(--vscode-button-secondaryBackground);
            color: var(--vscode-button-secondaryForeground);
            border: none;
            border-radius: 4px;
            cursor: pointer;
            font-size: 11px;
        }
        .action-btn:hover {
            background-color: var(--vscode-button-secondaryHoverBackground);
        }
        .section {
            margin-bottom: 16px;
        }
        .section-label {
            font-size: 11px;
            font-weight: 600;
            text-transform: uppercase;
            color: var(--vscode-descriptionForeground);
            margin-bottom: 6px;
        }
        textarea {
            width: 100%;
            min-height: 80px;
            padding: 8px;
            background-color: var(--vscode-input-background);
            color: var(--vscode-input-foreground);
            border: 1px solid var(--vscode-input-border);
            border-radius: 4px;
            resize: vertical;
            font-family: inherit;
            font-size: inherit;
        }
        textarea:focus {
            outline: 1px solid var(--vscode-focusBorder);
        }
        select {
            width: 100%;
            padding: 8px;
            background-color: var(--vscode-dropdown-background);
            color: var(--vscode-dropdown-foreground);
            border: 1px solid var(--vscode-dropdown-border);
            border-radius: 4px;
            font-family: inherit;
            font-size: inherit;
        }
        select:focus {
            outline: 1px solid var(--vscode-focusBorder);
        }
        .tabs {
            display: flex;
            gap: 0;
            margin-bottom: 8px;
            border-bottom: 1px solid var(--vscode-panel-border);
        }
        .tab {
            padding: 8px 16px;
            background: none;
            border: none;
            color: var(--vscode-foreground);
            cursor: pointer;
            opacity: 0.7;
            border-bottom: 2px solid transparent;
            margin-bottom: -1px;
        }
        .tab.active {
            opacity: 1;
            border-bottom-color: var(--vscode-focusBorder);
        }
        .tab:hover {
            opacity: 1;
        }
        .tab-content {
            display: none;
        }
        .tab-content.active {
            display: block;
        }
        .logs-container {
            max-height: 400px;
            overflow-y: auto;
        }
        .log-card {
            background-color: var(--vscode-editor-background);
            border-radius: 6px;
            padding: 10px;
            margin-bottom: 8px;
            border-left: 3px solid var(--vscode-focusBorder);
        }
        .log-card.memory-snapshot {
            border-left-color: #f59e0b;
        }
        .log-card-header {
            font-size: 10px;
            color: var(--vscode-descriptionForeground);
            margin-bottom: 4px;
        }
        .log-card-prompt {
            font-weight: 500;
            margin-bottom: 6px;
            color: var(--vscode-textLink-foreground);
        }
        .log-card-response {
            white-space: pre-wrap;
            word-break: break-word;
            font-size: 12px;
            line-height: 1.4;
        }
        .empty-state {
            text-align: center;
            padding: 24px;
            color: var(--vscode-descriptionForeground);
            font-style: italic;
        }
    </style>
</head>
<body>
    <div class="status-bar">
        <div class="status-dot" id="statusDot"></div>
        <span class="status-text" id="statusText">BRIDGE: Offline</span>
        <button class="toggle-btn" id="toggleBtn" onclick="toggleServer()">Start</button>
    </div>

    <div class="action-row">
        <button class="action-btn" onclick="clearHistory()">Clear History</button>
        <button class="action-btn" onclick="refreshModels()">Refresh Models</button>
    </div>

    <div class="section">
        <div class="section-label">System Prompt</div>
        <textarea id="systemPrompt" placeholder="Enter a system prompt..." oninput="onSystemPromptChange()"></textarea>
    </div>

    <div class="section">
        <div class="section-label">Model Selector</div>
        <select id="modelSelector" onchange="onModelChange()">
            ${options}
        </select>
    </div>

    <div class="tabs">
        <button class="tab active" data-tab="current" onclick="switchTab('current')">Current</button>
        <button class="tab" data-tab="history" onclick="switchTab('history')">History (Context)</button>
    </div>

    <div class="tab-content active" id="currentTab">
        <div class="logs-container" id="currentLogs">
            <div class="empty-state">No responses yet</div>
        </div>
    </div>

    <div class="tab-content" id="historyTab">
        <div class="logs-container" id="historyLogs">
            <div class="empty-state">No memory snapshots</div>
        </div>
    </div>

    <script>
        const vscode = acquireVsCodeApi();

        // Send ready message on load
        window.addEventListener('load', () => {
            vscode.postMessage({ type: 'webviewReady' });
        });

        function toggleServer() {
            vscode.postMessage({ type: 'serverToggle' });
        }

        function clearHistory() {
            vscode.postMessage({ type: 'clearHistory' });
            document.getElementById('currentLogs').innerHTML = '<div class="empty-state">No responses yet</div>';
            document.getElementById('historyLogs').innerHTML = '<div class="empty-state">No memory snapshots</div>';
        }

        function refreshModels() {
            vscode.postMessage({ type: 'refreshModels' });
        }

        function onSystemPromptChange() {
            const value = document.getElementById('systemPrompt').value;
            vscode.postMessage({ type: 'systemPromptChanged', value: value });
        }

        function onModelChange() {
            const value = document.getElementById('modelSelector').value;
            vscode.postMessage({ type: 'modelChanged', value: value });
        }

        function switchTab(tabName) {
            // Update tab buttons
            document.querySelectorAll('.tab').forEach(tab => {
                tab.classList.toggle('active', tab.dataset.tab === tabName);
            });
            // Update tab content
            document.getElementById('currentTab').classList.toggle('active', tabName === 'current');
            document.getElementById('historyTab').classList.toggle('active', tabName === 'history');
        }

        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }

        // Handle messages from extension
        window.addEventListener('message', event => {
            const msg = event.data;
            switch (msg.type) {
                case 'portUpdate':
                    updatePortStatus(msg.value);
                    break;
                case 'syncSystemPrompt':
                    document.getElementById('systemPrompt').value = msg.value;
                    break;
                case 'syncModel':
                    const selector = document.getElementById('modelSelector');
                    const options = selector.options;
                    for (let i = 0; i < options.length; i++) {
                        if (options[i].value === msg.value) {
                            selector.selectedIndex = i;
                            break;
                        }
                    }
                    break;
                case 'newLog':
                    addLogCard(msg.data);
                    break;
            }
        });

        function updatePortStatus(port) {
            const dot = document.getElementById('statusDot');
            const text = document.getElementById('statusText');
            const btn = document.getElementById('toggleBtn');

            if (port > 0) {
                dot.classList.add('active');
                text.textContent = 'BRIDGE: Port ' + port;
                btn.textContent = 'Stop';
            } else {
                dot.classList.remove('active');
                text.textContent = 'BRIDGE: Offline';
                btn.textContent = 'Start';
            }
        }

        function addLogCard(data) {
            const isMemorySnapshot = data.response.includes('[INTERNAL MEMORY SNAPSHOT]');
            const containerId = isMemorySnapshot ? 'historyLogs' : 'currentLogs';
            const container = document.getElementById(containerId);

            // Remove empty state if present
            const emptyState = container.querySelector('.empty-state');
            if (emptyState) {
                emptyState.remove();
            }

            const card = document.createElement('div');
            card.className = 'log-card' + (isMemorySnapshot ? ' memory-snapshot' : '');

            const headerText = isMemorySnapshot ? 'Memory Snapshot' : 'Response';

            card.innerHTML = \`
                <div class="log-card-header">\${headerText} · \${escapeHtml(data.modelName)}</div>
                <div class="log-card-prompt">\${escapeHtml(data.prompt)}</div>
                <div class="log-card-response">\${escapeHtml(data.response)}</div>
            \`;

            container.insertBefore(card, container.firstChild);
        }
    </script>
</body>
</html>`;
    }
}

// ============================================================================
// POST /ask Handler
// ============================================================================
async function handleAskRequest(req: Request, res: Response): Promise<void> {
    log('Received /ask request');
    log(`Request body: ${JSON.stringify(req.body)}`);
    
    try {
        const body = req.body as AskRequestBody;

        // Validate prompt is provided (bug fix)
        if (!body.prompt || typeof body.prompt !== 'string' || body.prompt.trim() === '') {
            log('Error: prompt is missing or empty');
            res.status(400).json({ error: 'prompt is required and must be a non-empty string' });
            return;
        }

        const { model_name, prompt, system_prompt, retain_history } = body;

        // If retain_history is false, wipe history
        if (retain_history === false) {
            provider.history = [];
        }

        // Discover models
        log('Discovering available models...');
        const allModels = await vscode.lm.selectChatModels({});
        log(`Found ${allModels.length} models:`);
        allModels.forEach((m, i) => {
            log(`  [${i}] family: ${m.family}, id: ${m.id}, vendor: ${m.vendor}, name: ${m.name}`);
        });

        // Select a model via fallback chain
        // Prefer models from 'copilot' vendor over 'copilotcli' as they tend to work better
        let model: vscode.LanguageModelChat | undefined;

        if (model_name) {
            log(`Looking for model by name: ${model_name}`);
            // First try to find from 'copilot' vendor
            model = allModels.find(m => (m.family === model_name || m.id === model_name) && m.vendor === 'copilot');
            // If not found, try any vendor
            if (!model) {
                model = allModels.find(m => m.family === model_name || m.id === model_name);
            }
            if (model) {
                log(`Found model: ${model.family} (id: ${model.id}, vendor: ${model.vendor}, name: ${model.name})`);
                log(`Model version: ${model.version}, maxInputTokens: ${model.maxInputTokens}`);
            }
        } else if (provider.selectedModel) {
            log(`Using UI-selected model: ${provider.selectedModel.family}`);
            // Match UI-selected model by family, prefer 'copilot' vendor
            model = allModels.find(m => m.family === provider.selectedModel!.family && m.vendor === 'copilot');
            if (!model) {
                model = allModels.find(m => m.family === provider.selectedModel!.family);
            }
        }

        if (!model) {
            // Fallback to gpt-4.1 from copilot vendor
            log('No model found, trying fallback...');
            const fallbackModels = await vscode.lm.selectChatModels({ family: 'gpt-4.1' });
            // Prefer copilot vendor
            model = fallbackModels.find(m => m.vendor === 'copilot') || fallbackModels[0];
            if (model) {
                log(`Using fallback model: ${model.family} (vendor: ${model.vendor})`);
            }
        }

        if (!model) {
            const requestedModel = model_name || provider.selectedModel?.family || 'gpt-4.1';
            log(`ERROR: No model found! Requested: ${requestedModel}`);
            res.status(404).json({ error: `Model "${requestedModel}" not found. Available models: ${allModels.map(m => `${m.family}@${m.vendor}`).join(', ')}` });
            return;
        }

        // Build the payload using explicit LanguageModelTextPart
        const payload: vscode.LanguageModelChatMessage[] = [];

        // Handle system prompt (bug fix: send as User message, not Assistant)
        const effectiveSystemPrompt = system_prompt || provider.sidebarSystemPrompt || null;

        if (effectiveSystemPrompt) {
            // Prepend system prompt as a User message with explicit text part
            const systemTextPart = new vscode.LanguageModelTextPart(`[System Instructions]\n${effectiveSystemPrompt}`);
            payload.push(vscode.LanguageModelChatMessage.User([systemTextPart]));
        }

        // Add history (skip any with empty content from previous failed requests)
        for (const histMsg of provider.history) {
            const content = histMsg.content;
            // Check if content is meaningful (content is always an array of parts)
            const hasContent = content.some(part => {
                if (part instanceof vscode.LanguageModelTextPart) {
                    return part.value && part.value.trim().length > 0;
                }
                return true; // Other part types (tool calls, etc.) are considered valid
            });
            if (hasContent) {
                payload.push(histMsg);
            }
        }

        // Create and add user message with explicit text part
        const userTextPart = new vscode.LanguageModelTextPart(prompt);
        const userMessage = vscode.LanguageModelChatMessage.User([userTextPart]);
        payload.push(userMessage);

        // Send the request
        log(`Sending request to model: ${model.family}`);
        log(`Payload has ${payload.length} messages`);
        
        // Log each message in payload for debugging
        payload.forEach((msg, i) => {
            const role = msg.role === vscode.LanguageModelChatMessageRole.User ? 'User' : 'Assistant';
            let contentStr = '';
            if (Array.isArray(msg.content)) {
                contentStr = msg.content.map(part => {
                    if (part instanceof vscode.LanguageModelTextPart) {
                        return part.value;
                    }
                    return JSON.stringify(part);
                }).join('');
            } else {
                contentStr = String(msg.content);
            }
            log(`  Message ${i}: [${role}] "${contentStr.substring(0, 100)}${contentStr.length > 100 ? '...' : ''}"`);
        });
        
        // Send request with justification
        const requestOptions: vscode.LanguageModelChatRequestOptions = {
            justification: 'Processing user prompt via LLM Bridge API'
        };
        
        log('Calling model.sendRequest...');
        const chatRequest = await model.sendRequest(payload, requestOptions);
        log(`sendRequest returned successfully`);
        log(`chatRequest keys: ${Object.keys(chatRequest).join(', ')}`);

        // Try using the stream API for more detailed info
        let responseText = '';
        let fragmentCount = 0;
        log('Starting to consume response stream...');
        
        try {
            for await (const part of chatRequest.stream) {
                fragmentCount++;
                if (part instanceof vscode.LanguageModelTextPart) {
                    log(`  TextPart ${fragmentCount}: "${part.value.substring(0, 50)}..." (${part.value.length} chars)`);
                    responseText += part.value;
                } else {
                    log(`  Part ${fragmentCount}: type=${(part as any).constructor?.name || typeof part}`);
                }
            }
        } catch (streamError) {
            log(`Stream error: ${streamError}`);
            // Fallback to text iterator
            log('Falling back to text iterator...');
            for await (const fragment of chatRequest.text) {
                fragmentCount++;
                log(`  Fragment ${fragmentCount}: "${fragment.substring(0, 50)}..." (${fragment.length} chars)`);
                responseText += fragment;
            }
        }
        
        log(`Stream complete. Total fragments: ${fragmentCount}, Total chars: ${responseText.length}`);
        
        // If response is empty, try to get more info
        if (responseText.length === 0) {
            log('WARNING: Empty response received!');
            log('Possible causes:');
            log('  1. Model rate limit or quota exceeded');
            log('  2. Model rejected the request silently');
            log('  3. Network/API issue');
            log('Try a different model like claude-sonnet-4.5 or gpt-4o-mini');
        }

        // Update history if retain_history is not false
        if (retain_history !== false && responseText.length > 0) {
            provider.history.push(userMessage);
            // Use explicit text part for assistant response
            const assistantTextPart = new vscode.LanguageModelTextPart(responseText);
            provider.history.push(vscode.LanguageModelChatMessage.Assistant([assistantTextPart]));
            await provider.updateHybridHistory(model);
        }

        // Log the entry
        provider.addLogEntry(prompt, responseText, model.family);

        // Send response
        log(`Response received (${responseText.length} chars)`);
        res.json({
            response: responseText,
            model_used: model.family
        });

    } catch (err) {
        const errorMessage = err instanceof Error ? err.message : String(err);
        const errorStack = err instanceof Error ? err.stack : '';
        log(`ERROR in /ask handler: ${errorMessage}`);
        log(`Stack trace: ${errorStack}`);
        console.error('Error in /ask handler:', errorMessage);
        res.status(500).json({ error: errorMessage });
    }
}

// ============================================================================
// OpenAI-compatible endpoints (so INGenious CLI + IDE can talk to the bridge
// with zero protocol changes on their side).
// ============================================================================

interface OpenAiMessage {
    role: string;
    content: unknown;
    tool_calls?: any[];
    tool_call_id?: string;
    name?: string;
}

interface ChatCompletionsBody {
    model?: string;
    messages?: OpenAiMessage[];
    stream?: boolean;
    temperature?: number;
    tools?: unknown;
}

/** Flatten OpenAI content (string, or array of {type:'text',text} parts) to text. */
function extractMessageText(content: unknown): string {
    if (typeof content === 'string') {
        return content;
    }
    if (Array.isArray(content)) {
        return content
            .map(part => {
                if (typeof part === 'string') {
                    return part;
                }
                if (part && typeof part === 'object' && 'text' in (part as any)) {
                    return String((part as any).text ?? '');
                }
                return '';
            })
            .join('');
    }
    if (content == null) {
        return '';
    }
    return String(content);
}

/**
 * Resolve a model by family/id, preferring the 'copilot' vendor, falling back
 * to gpt-4.1 then the UI-selected model then any available model.
 */
async function resolveModel(modelName?: string): Promise<{ model?: vscode.LanguageModelChat; available: vscode.LanguageModelChat[] }> {
    const available = await vscode.lm.selectChatModels({});
    let model: vscode.LanguageModelChat | undefined;

    if (modelName) {
        model = available.find(m => (m.family === modelName || m.id === modelName) && m.vendor === 'copilot')
            || available.find(m => m.family === modelName || m.id === modelName);
    }
    if (!model && provider?.selectedModel) {
        model = available.find(m => m.family === provider.selectedModel!.family && m.vendor === 'copilot')
            || available.find(m => m.family === provider.selectedModel!.family);
    }
    if (!model) {
        const fallback = await vscode.lm.selectChatModels({ family: 'gpt-4.1' });
        model = fallback.find(m => m.vendor === 'copilot') || fallback[0];
    }
    if (!model && available.length > 0) {
        model = available.find(m => m.vendor === 'copilot') || available[0];
    }
    return { model, available };
}

/** Convert OpenAI tool definitions into VS Code LanguageModelChatTool objects. */
function openAiToolsToLm(tools: unknown): any[] {
    if (!Array.isArray(tools)) {
        return [];
    }
    const out: any[] = [];
    for (const t of tools as any[]) {
        const fn = t && t.function ? t.function : t;
        if (!fn || !fn.name) {
            continue;
        }
        out.push({
            name: fn.name,
            description: fn.description || '',
            inputSchema: fn.parameters || { type: 'object', properties: {} }
        });
    }
    return out;
}

/** True when a streamed part is a tool call (guards for older API versions). */
function isToolCallPart(part: unknown): boolean {
    const ctor = (vscode as any).LanguageModelToolCallPart;
    return typeof ctor === 'function' && part instanceof ctor;
}

/** Convert OpenAI-style messages into the VS Code Language Model payload. */
function messagesToPayload(messages: OpenAiMessage[]): vscode.LanguageModelChatMessage[] {
    const payload: vscode.LanguageModelChatMessage[] = [];
    const ToolCallPart: any = (vscode as any).LanguageModelToolCallPart;
    const ToolResultPart: any = (vscode as any).LanguageModelToolResultPart;
    for (const msg of messages) {
        const role = (msg.role || 'user').toLowerCase();
        const text = extractMessageText(msg.content);
        if (role === 'assistant') {
            const parts: any[] = [];
            if (text && text.trim().length > 0) {
                parts.push(new vscode.LanguageModelTextPart(text));
            }
            if (Array.isArray(msg.tool_calls) && ToolCallPart) {
                for (const tc of msg.tool_calls) {
                    const fn = (tc && tc.function) || {};
                    let input: any = {};
                    try {
                        input = fn.arguments ? JSON.parse(fn.arguments) : {};
                    } catch {
                        input = {};
                    }
                    parts.push(new ToolCallPart(tc.id, fn.name, input));
                }
            }
            if (parts.length > 0) {
                payload.push(vscode.LanguageModelChatMessage.Assistant(parts));
            }
        } else if (role === 'tool') {
            if (ToolResultPart) {
                const resultPart = new ToolResultPart(msg.tool_call_id, [
                    new vscode.LanguageModelTextPart(text || '')
                ]);
                payload.push(vscode.LanguageModelChatMessage.User([resultPart]));
            } else if (text) {
                payload.push(
                    vscode.LanguageModelChatMessage.User([
                        new vscode.LanguageModelTextPart(`[Tool result]\n${text}`)
                    ])
                );
            }
        } else if (role === 'system') {
            if (text && text.trim().length > 0) {
                payload.push(
                    vscode.LanguageModelChatMessage.User([
                        new vscode.LanguageModelTextPart(`[System Instructions]\n${text}`)
                    ])
                );
            }
        } else {
            if (text && text.trim().length > 0) {
                payload.push(
                    vscode.LanguageModelChatMessage.User([new vscode.LanguageModelTextPart(text)])
                );
            }
        }
    }
    return payload;
}

/** GET /v1/models — OpenAI-compatible model catalog. */
async function handleListModels(_req: Request, res: Response): Promise<void> {
    try {
        const models = await vscode.lm.selectChatModels({});
        const created = Math.floor(Date.now() / 1000);
        const seen = new Set<string>();
        const data = models
            .map(m => m.family)
            .filter(id => (seen.has(id) ? false : (seen.add(id), true)))
            .map(id => ({ id, object: 'model', created, owned_by: 'copilot' }));
        res.json({ object: 'list', data });
    } catch (err) {
        const message = err instanceof Error ? err.message : String(err);
        res.status(500).json({ error: { message, type: 'bridge_error' } });
    }
}

/** POST /v1/chat/completions — OpenAI-compatible, supports stream + non-stream. */
async function handleChatCompletions(req: Request, res: Response): Promise<void> {
    log('Received /v1/chat/completions request');
    try {
        const body = req.body as ChatCompletionsBody;
        const messages = Array.isArray(body.messages) ? body.messages : [];
        if (messages.length === 0) {
            res.status(400).json({ error: { message: 'messages is required and must be a non-empty array', type: 'invalid_request_error' } });
            return;
        }

        const { model, available } = await resolveModel(body.model);
        for (const m of messages) {
            if ((m.role || '').toLowerCase() === 'tool') {
                const rc = extractMessageText(m.content).replace(/\s+/g, ' ').slice(0, 240);
                log(`  <- tool result [${m.name ?? '?'}]: ${rc}`);
            }
        }
        if (!model) {
            const requested = body.model || '(default)';
            res.status(404).json({
                error: {
                    message: `Model "${requested}" not found. Available: ${available.map(m => `${m.family}@${m.vendor}`).join(', ') || 'none — check Copilot sign-in'}`,
                    type: 'model_not_found'
                }
            });
            return;
        }

        const payload = messagesToPayload(messages);
        const lmTools = openAiToolsToLm(body.tools);
        log(`chat: ${messages.length} messages, ${lmTools.length} tools provided`);
        log(`model: requested=${body.model ?? '(none)'} resolved=${model.family}@${model.vendor}`);
        const requestOptions: vscode.LanguageModelChatRequestOptions = {
            justification: 'Processing chat completion via INGenious LLM Bridge'
        };
        if (lmTools.length > 0) {
            (requestOptions as any).tools = lmTools;
        }

        const chatRequest = await model.sendRequest(payload, requestOptions);
        const id = `chatcmpl-${Date.now()}`;
        const created = Math.floor(Date.now() / 1000);
        const stream = body.stream === true;

        if (stream) {
            res.setHeader('Content-Type', 'text/event-stream');
            res.setHeader('Cache-Control', 'no-cache');
            res.setHeader('Connection', 'keep-alive');
            res.flushHeaders?.();

            // Initial role chunk.
            const roleChunk = {
                id, object: 'chat.completion.chunk', created, model: model.family,
                choices: [{ index: 0, delta: { role: 'assistant' }, finish_reason: null }]
            };
            res.write(`data: ${JSON.stringify(roleChunk)}\n\n`);

            let full = '';
            for await (const fragment of chatRequest.text) {
                full += fragment;
                const chunk = {
                    id, object: 'chat.completion.chunk', created, model: model.family,
                    choices: [{ index: 0, delta: { content: fragment }, finish_reason: null }]
                };
                res.write(`data: ${JSON.stringify(chunk)}\n\n`);
            }

            const doneChunk = {
                id, object: 'chat.completion.chunk', created, model: model.family,
                choices: [{ index: 0, delta: {}, finish_reason: 'stop' }]
            };
            res.write(`data: ${JSON.stringify(doneChunk)}\n\n`);
            res.write('data: [DONE]\n\n');
            res.end();
            provider?.addLogEntry(extractMessageText(messages[messages.length - 1]?.content), full, model.family);
            return;
        }

        let responseText = '';
        const toolCalls: any[] = [];
        for await (const part of chatRequest.stream) {
            if (part instanceof vscode.LanguageModelTextPart) {
                responseText += part.value;
            } else if (isToolCallPart(part)) {
                const p: any = part;
                toolCalls.push({
                    id: p.callId,
                    type: 'function',
                    function: { name: p.name, arguments: JSON.stringify(p.input ?? {}) }
                });
            }
        }

        const message: any = { role: 'assistant', content: responseText || null };
        if (toolCalls.length > 0) {
            message.tool_calls = toolCalls;
        }
        const names = toolCalls.map(t => t.function.name).join(', ');
        log(
            `response: ${toolCalls.length} tool_calls${names ? ' [' + names + ']' : ''}, ` +
            `${responseText.length} text chars`
        );
        provider?.addLogEntry(
            extractMessageText(messages[messages.length - 1]?.content),
            toolCalls.length > 0
                ? '[tool_calls] ' + toolCalls.map(t => t.function.name).join(', ')
                : responseText,
            model.family
        );
        res.json({
            id,
            object: 'chat.completion',
            created,
            model: model.family,
            choices: [
                {
                    index: 0,
                    message,
                    finish_reason: toolCalls.length > 0 ? 'tool_calls' : 'stop'
                }
            ],
            usage: { prompt_tokens: 0, completion_tokens: 0, total_tokens: 0 }
        });
    } catch (err) {
        const message = err instanceof Error ? err.message : String(err);
        log(`ERROR in /v1/chat/completions: ${message}`);
        if (!res.headersSent) {
            res.status(500).json({ error: { message, type: 'bridge_error' } });
        } else {
            res.end();
        }
    }
}

// ============================================================================
// Extension Activation
// ============================================================================
export function activate(context: vscode.ExtensionContext): void {
    // Create output channel for logging
    outputChannel = vscode.window.createOutputChannel('LLM Bridge');
    outputChannel.show();
    log('Extension activating...');
    
    // Create Express app
    const app = express();
    // No practical size cap: run/tool results can carry large captured output
    // (stack traces, HTML error bodies, etc.). The default 100kb limit trips a
    // 413 PayloadTooLargeError before any route handler runs.
    app.use(express.json({ limit: Infinity }));

    // Create SidebarProvider
    provider = new SidebarProvider(context.extensionUri, app);

    // Register POST /ask route
    app.post('/ask', handleAskRequest);

    // OpenAI-compatible routes (used by INGenious CLI + IDE)
    app.post('/v1/chat/completions', handleChatCompletions);
    app.get('/v1/models', handleListModels);
    // Convenience aliases without the /v1 prefix
    app.post('/chat/completions', handleChatCompletions);
    app.get('/models', handleListModels);
    // Lightweight health check
    app.get('/health', (_req, res) => { res.json({ status: 'ok' }); });

    // Register webview provider
    const webviewDisposable = vscode.window.registerWebviewViewProvider(
        'ingenious-bridge-view',
        provider
    );
    context.subscriptions.push(webviewDisposable);

    // Ensure server is stopped on deactivation
    context.subscriptions.push({
        dispose: () => {
            provider.stopServer();
        }
    });

    // Auto-start on a known loopback port so INGenious works without a manual click.
    provider.startServer();

    log('VSCode INGenious Bridge extension activated successfully');
}

export function deactivate(): void {
    // Cleanup handled by disposable
}
