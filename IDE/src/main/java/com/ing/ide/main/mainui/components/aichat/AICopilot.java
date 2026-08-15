package com.ing.ide.main.mainui.components.aichat;

import com.ing.engine.support.DesktopApi;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.mainui.SlideShow;
import com.ing.ide.main.mainui.components.aichat.agent.AgentOrchestrator;
import com.ing.ide.main.mainui.components.aichat.auth.AICredentials;
import com.ing.ide.main.mainui.components.aichat.auth.DeviceCodeResponse;
import com.ing.ide.main.mainui.components.aichat.auth.GitHubDeviceAuthService;
import com.ing.ide.main.mainui.components.aichat.client.GitHubModelsClient;
import com.ing.ide.main.mainui.components.aichat.history.ChatHistoryStore;
import com.ing.ide.main.mainui.components.aichat.mcp.MCPToolBridge;
import com.ing.ide.main.mainui.components.aichat.model.ChatCompletionRequest;
import com.ing.ide.main.mainui.components.aichat.model.ChatMessage;
import com.ing.ide.main.mainui.components.aichat.model.ChatSession;
import com.ing.ide.main.mainui.components.aichat.model.ModelInfo;
import com.ing.ide.main.mainui.components.aichat.model.TokenUsage;
import com.ing.ide.main.mainui.components.aichat.skills.AuthoringSkill;
import com.ing.ide.main.mainui.components.aichat.ui.AICopilotUI;
import com.ing.ide.main.mainui.components.aichat.ui.AISettingsDialog;
import com.ing.ide.main.mainui.components.aichat.util.TokenUsageTracker;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Controller for the INGenious AI assistant panel. Owns the chat session, the
 * GitHub Models client, authentication, and the streaming turn loop, and keeps
 * the {@link AICopilotUI} in sync.
 *
 * <p>This is Phase 2 (chat). The agent tool-calling loop (Phase 3) plugs into
 * {@link #sendUserMessage(String)} later.</p>
 */
public class AICopilot implements SlideShow.SlideChangeListener {
    private static final Logger LOG = Logger.getLogger(AICopilot.class.getName());

    private final AppMainFrame mainFrame;
    private final AICredentials credentials = new AICredentials();
    private final GitHubModelsClient client = new GitHubModelsClient();
    private final TokenUsageTracker usageTracker = new TokenUsageTracker();
    private final ChatSession session;
    private final AICopilotUI ui;
    private final MCPToolBridge toolServer;
    private final ChatHistoryStore historyStore = new ChatHistoryStore();
    private String currentConversationId;

    /**
     * OpenAI-compatible base URL of a running VS Code Copilot bridge, or
     * {@code null} to use the direct GitHub Models auth flow. When set, the
     * assistant works without a GitHub token. Re-evaluated by {@link #connectToVsCode()}.
     */
    private volatile String bridgeBaseUrl;

    private volatile Thread activeTurn;
    private volatile GitHubDeviceAuthService activeAuth;
    private volatile AgentOrchestrator activeAgent;

    /** Inline approval futures keyed by tool-call id, resolved by chat buttons. */
    private final java.util.Map<String, CompletableFuture<Boolean>> pendingApprovals = new ConcurrentHashMap<>();
    private final AtomicInteger toolSeq = new AtomicInteger();

    /** Live-context system message kept in sync before each turn. */
    private ChatMessage contextMessage;

    public AICopilot(AppMainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.session = new ChatSession(credentials.getSelectedModel());
        this.session.addMessage(ChatMessage.system(AuthoringSkill.systemPrompt()));
        this.toolServer = new MCPToolBridge(mainFrame);
        this.toolServer.addRefreshListener((toolName, result) -> scheduleLiveReload());
        this.ui = new AICopilotUI(this);
        // Prefer a locally running VS Code Copilot bridge over direct GitHub auth.
        this.bridgeBaseUrl =
            com.ing.ide.main.mainui.components.aichat.client.BridgeDiscovery.detect();
        if (bridgeBaseUrl != null) {
            client.useLocalBridge(bridgeBaseUrl);
            LOG.log(Level.INFO, "Using VS Code Copilot bridge at {0}", bridgeBaseUrl);
        }
        refreshAuthState();
    }

    /** True when the assistant is backed by a local VS Code Copilot bridge. */
    private boolean bridgeActive() {
        return bridgeBaseUrl != null;
    }

    /**
     * The token to send with requests: the real GitHub token when present, a
     * non-null placeholder when the bridge is active (so token gates pass and
     * the client simply omits the {@code Authorization} header), else null.
     */
    private String effectiveToken() {
        String token = credentials.getToken();
        if (token != null && !token.isEmpty()) {
            return token;
        }
        return bridgeActive() ? "local-bridge" : null;
    }

    public AICopilotUI getAICopilotUI() {
        return ui;
    }

    public AppMainFrame getMainFrame() {
        return mainFrame;
    }

    /**
     * Resolves a prompt-library token to a live value from the current IDE
     * selection. Returns {@code null} when nothing is selected so the UI can
     * fall back to a guide-friendly default.
     */
    public String resolveContextToken(String token) {
        try {
            if ("currentProject".equals(token)) {
                return mainFrame.getProject() == null ? null : mainFrame.getProject().getName();
            }
            com.ing.datalib.component.TestCase tc = mainFrame
                .getTestDesign()
                .getTestCaseComp()
                .getCurrentTestCase();
            if (tc == null) {
                return null;
            }
            if ("currentTestCase".equals(token)) {
                return tc.getName();
            }
            if ("currentScenario".equals(token)) {
                return tc.getScenario() == null ? null : tc.getScenario().getName();
            }
        } catch (Exception ex) {
            LOG.log(Level.FINE, "Could not resolve context token: " + token, ex);
        }
        return null;
    }

    private void refreshAuthState() {
        if (bridgeActive()) {
            ui.setConnected(
                true,
                "Connected \u00b7 port " + bridgePort() + " \u00b7 " + toolCount() + " tools"
            );
            loadCatalogAsync();
        } else if (credentials.isSignedIn()) {
            String login = credentials.getLogin();
            ui.setConnected(true, login != null && !login.isEmpty() ? login : "Connected");
            loadCatalogAsync();
        } else {
            ui.setConnected(false, "Not connected");
        }
    }

    /**
     * (Re)detect a running VS Code Copilot bridge and connect to it. Invoked by
     * the "Connect to VS Code" button. On success the status bulb turns green
     * and a temporary message reveals the port.
     */
    public void connectToVsCode() {
        ui.setStatus("Connecting to VS Code\u2026");
        new Thread(
            () -> {
                String url = com.ing.ide.main.mainui.components.aichat.client.BridgeDiscovery.detect();
                if (url == null) {
                    SwingUtilities.invokeLater(
                        () -> {
                            ui.setConnected(false, "Not connected");
                            showError(
                                "VS Code bridge not found. In VS Code, install and run the " +
                                "'VSCode INGenious Bridge' extension (it starts automatically), " +
                                "then click Connect to VS Code."
                            );
                        }
                    );
                    return;
                }
                bridgeBaseUrl = url;
                client.useLocalBridge(url);
                int port = bridgePort();
                LOG.log(Level.INFO, "Connected to VS Code Copilot bridge at {0}", url);
                SwingUtilities.invokeLater(
                    () -> {
                        int tools = toolCount();
                        ui.setConnected(
                            true,
                            "Connected \u00b7 port " + port + " \u00b7 " + tools + " tools"
                        );
                        ui.showTemporaryMessage(
                            "\u2713 Connected on port " +
                            port +
                            " \u2014 " +
                            tools +
                            " tools available"
                        );
                    }
                );
                loadCatalogAsync();
            },
            "aichat-connect"
        )
        .start();
    }

    /** Port of the currently connected bridge, or -1 when unknown. */
    private int bridgePort() {
        try {
            return bridgeBaseUrl == null ? -1 : URI.create(bridgeBaseUrl).getPort();
        } catch (Exception ex) {
            return -1;
        }
    }

    /** Number of INGenious tools currently exposed to the model (diagnostic). */
    private int toolCount() {
        try {
            return toolServer.toolDefinitions().size();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Could not list tool definitions", ex);
            return -1;
        }
    }

    // ── Model selection ───────────────────────────────────────────────────

    public void onModelSelected(String modelId) {
        if (modelId != null && !modelId.isEmpty()) {
            session.setModel(modelId);
            credentials.setSelectedModel(modelId);
        }
    }

    private void loadCatalogAsync() {
        String token = effectiveToken();
        if (token == null) {
            return;
        }
        new Thread(
            () -> {
                try {
                    List<ModelInfo> models = client.catalog(token);
                    ui.setModels(models, credentials.getSelectedModel());
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Failed to load model catalog", ex);
                }
            },
            "aichat-catalog"
        )
        .start();
    }

    // ── Authentication ────────────────────────────────────────────────────

    public void toggleSignIn() {
        if (credentials.isSignedIn()) {
            credentials.clear();
            ui.setSignedIn(false, null);
            ui.setModels(new ArrayList<>(), null);
        } else {
            signInAsync();
        }
    }

    /** Opens the AI settings dialog (OAuth client id, model info, sign-out). */
    public void openSettings() {
        AISettingsDialog.show(
            ui,
            credentials,
            () -> {
                credentials.clear();
                ui.setSignedIn(false, null);
                ui.setModels(new ArrayList<>(), null);
            }
        );
    }

    private void signInAsync() {
        final GitHubDeviceAuthService auth = new GitHubDeviceAuthService();
        this.activeAuth = auth;
        ui.setStatus("Signing in…");
        new Thread(
            () -> {
                try {
                    String token = auth.authorize(credentials.getClientId(), this::showDeviceCode);
                    credentials.setToken(token);
                    String login = fetchLogin(token);
                    credentials.setLogin(login);
                    ui.setSignedIn(true, login);
                    loadCatalogAsync();
                } catch (GitHubDeviceAuthService.AuthException ex) {
                    ui.setStatus("Not signed in");
                    showError(ex.getMessage());
                } finally {
                    activeAuth = null;
                }
            },
            "aichat-signin"
        )
        .start();
    }

    private void showDeviceCode(DeviceCodeResponse device) {
        try {
            DesktopApi.browse(new URI(device.getVerificationUri()));
        } catch (Exception ex) {
            LOG.log(Level.FINE, "Could not open browser for device flow", ex);
        }
        SwingUtilities.invokeLater(
            () ->
                JOptionPane.showMessageDialog(
                    ui,
                    "To sign in to GitHub Models:\n\n" +
                    "1. Your browser is opening " +
                    device.getVerificationUri() +
                    "\n" +
                    "2. Enter this code:  " +
                    device.getUserCode() +
                    "\n\n" +
                    "This dialog will close automatically once you approve.",
                    "GitHub sign-in",
                    JOptionPane.INFORMATION_MESSAGE
                )
        );
    }

    private String fetchLogin(String token) {
        // Best-effort: the login is cosmetic. A failure should not block sign-in.
        try {
            return new GitHubUserLookup().login(token);
        } catch (Exception ex) {
            LOG.log(Level.FINE, "Could not fetch GitHub login", ex);
            return "";
        }
    }

    // ── Chat turn ─────────────────────────────────────────────────────────

    public void sendUserMessage(String text) {
        if (!credentials.isSignedIn() && !bridgeActive()) {
            showError(
                "Not connected. Click \"Connect to VS Code\" to use GitHub Copilot via the bridge."
            );
            return;
        }
        syncContextMessage();
        ChatMessage userMessage = ChatMessage.user(text);
        session.addMessage(userMessage);
        ui.getChatWebView().addMessage(userMessage);
        // Tools require an open project. Whenever one is open, use the tool-calling
        // (agent) loop so the assistant can actually act on the project — the model
        // still decides whether to call any tools. Fall back to plain chat otherwise.
        if (mainFrame.getProject() != null) {
            startAgentTurn();
        } else {
            startStreamingTurn();
        }
    }

    /**
     * Keeps a single system message with the live IDE context (project,
     * scenario, test case) just after the base authoring prompt, refreshing it
     * before each turn so the model always sees the current selection without
     * the history growing unbounded.
     */
    private void syncContextMessage() {
        try {
            List<ChatMessage> msgs = session.getMessages();
            if (contextMessage != null) {
                msgs.remove(contextMessage);
            }
            contextMessage = ChatMessage.system(buildContextSnapshot());
            int idx = msgs.isEmpty() ? 0 : 1;
            msgs.add(Math.min(idx, msgs.size()), contextMessage);
        } catch (Exception ex) {
            LOG.log(Level.FINE, "Could not sync context message", ex);
        }
    }

    private String buildContextSnapshot() {
        StringBuilder sb = new StringBuilder("# Current IDE context\n");
        try {
            com.ing.datalib.component.Project p = mainFrame.getProject();
            sb.append("Project: ").append(p == null ? "(none)" : p.getName());
            if (p != null) {
                sb.append("  (path: ").append(p.getLocation()).append(")");
            }
            sb.append('\n');
            com.ing.datalib.component.TestCase tc = mainFrame
                .getTestDesign()
                .getTestCaseComp()
                .getCurrentTestCase();
            if (tc != null) {
                if (tc.getScenario() != null) {
                    sb
                        .append("Selected scenario: ")
                        .append(tc.getScenario().getName())
                        .append('\n');
                }
                sb.append("Selected test case: ").append(tc.getName()).append('\n');
            }
        } catch (Exception ex) {
            LOG.log(Level.FINE, "Could not build context snapshot", ex);
        }
        sb.append(
            "\nWhen the user refers to \"this\"/\"current\" project, scenario, or " +
            "test case, use the values above. Pass the project path to tools."
        );
        return sb.toString();
    }

    private void startStreamingTurn() {
        final String token = effectiveToken();
        if (token == null) {
            showError("Not signed in.");
            return;
        }
        ui.setGenerating(true);
        usageTracker.beginTask();

        final ChatCompletionRequest request = new ChatCompletionRequest(
            session.getModel(),
            new ArrayList<>(session.getMessages()),
            true
        );

        final StringBuilder accumulated = new StringBuilder();
        final boolean[] started = { false };

        Thread turn = new Thread(
            () -> {
                client.streamComplete(
                    token,
                    request,
                    new GitHubModelsClient.StreamListener() {

                        @Override
                        public void onToken(String chunk) {
                            if (!started[0]) {
                                started[0] = true;
                                ui.getChatWebView().beginAssistantMessage();
                            }
                            accumulated.append(chunk);
                            ui.getChatWebView().updateAssistantMessage(accumulated.toString());
                        }

                        @Override
                        public void onUsage(TokenUsage usage) {
                            usageTracker.record(usage);
                            ui.setFooter(usageTracker.statusText());
                        }

                        @Override
                        public void onRateLimit(String remaining, String limit, String reset) {
                            usageTracker.recordRateLimit(remaining, limit, reset);
                        }

                        @Override
                        public void onComplete() {
                            String full = accumulated.toString();
                            session.addMessage(ChatMessage.assistant(full));
                            ui.getChatWebView().endAssistantMessage();
                            ui.setGenerating(false);
                            activeTurn = null;
                            persistCurrentConversation();
                        }

                        @Override
                        public void onError(Throwable error) {
                            ui.getChatWebView().endAssistantMessage();
                            ui.setGenerating(false);
                            activeTurn = null;
                            handleTurnError(error);
                        }
                    }
                );
            },
            "aichat-turn"
        );
        activeTurn = turn;
        turn.start();
    }

    private void handleTurnError(Throwable error) {
        String message = error.getMessage() == null ? error.toString() : error.getMessage();
        if (
            error instanceof GitHubModelsClient.ApiException &&
            ((GitHubModelsClient.ApiException) error).getStatusCode() == 401
        ) {
            credentials.clear();
            ui.setSignedIn(false, null);
        }
        showError(message);
    }

    public void cancelGeneration() {
        AgentOrchestrator agent = activeAgent;
        if (agent != null) {
            agent.cancel();
        }
        failPendingApprovals();
        Thread turn = activeTurn;
        if (turn != null) {
            turn.interrupt();
            activeTurn = null;
            ui.setGenerating(false);
            ui.getChatWebView().endAssistantMessage();
        }
    }

    // ── Agent turn (tool-calling) ─────────────────────────────────────────

    private void startAgentTurn() {
        final String token = effectiveToken();
        if (token == null) {
            showError("Not signed in.");
            return;
        }
        if (mainFrame.getProject() == null) {
            showError("Open a project before using agent mode.");
            return;
        }
        ui.setGenerating(true);
        usageTracker.beginTask();
        final boolean[] mutated = { false };
        final AtomicReference<String> currentId = new AtomicReference<>("tc0");
        final AgentOrchestrator orchestrator = new AgentOrchestrator(client, toolServer);
        activeAgent = orchestrator;

        Thread turn = new Thread(
            () -> {
                orchestrator.run(
                    token,
                    session,
                    (toolName, argumentsJson) ->
                        requestInlineApproval(currentId.get(), toolName, argumentsJson),
                    new AgentOrchestrator.AgentListener() {

                        @Override
                        public void onAssistantText(String text) {
                            ui.getChatWebView().addMessage(ChatMessage.assistant(text));
                        }

                        @Override
                        public void onToolStart(String toolName, String argumentsJson) {
                            String id = "tc" + toolSeq.incrementAndGet();
                            currentId.set(id);
                            ui.setFooter("Running tool: " + toolName + "…");
                            ui
                                .getChatWebView()
                                .appendToolCall(id, toolName, oneLine(argumentsJson, 80));
                        }

                        @Override
                        public void onUsage(TokenUsage usage) {
                            usageTracker.record(usage);
                            ui.setFooter(usageTracker.statusText());
                        }

                        @Override
                        public void onToolResult(String toolName, boolean error, String summary) {
                            if (!toolServer.isReadOnly(toolName) && !error) {
                                mutated[0] = true;
                            }
                            ui
                                .getChatWebView()
                                .resolveToolCall(
                                    currentId.get(),
                                    oneLine(summary, 80),
                                    error,
                                    summary
                                );
                        }

                        @Override
                        public void onComplete() {
                            ui.setGenerating(false);
                            ui.setFooter(usageTracker.statusText());
                            activeAgent = null;
                            failPendingApprovals();
                            if (mutated[0]) {
                                triggerLiveReloadNow();
                            }
                            persistCurrentConversation();
                        }

                        @Override
                        public void onError(Throwable error) {
                            ui.setGenerating(false);
                            activeAgent = null;
                            failPendingApprovals();
                            if (mutated[0]) {
                                triggerLiveReloadNow();
                            }
                            handleTurnError(error);
                        }
                    }
                );
            },
            "aichat-agent"
        );
        activeTurn = turn;
        turn.start();
    }

    /**
     * Renders an inline approval row in the transcript and blocks the calling
     * (agent) thread until the user clicks Apply or Skip, or the turn is
     * cancelled. Returns {@code true} only if the user approved.
     */
    private boolean requestInlineApproval(String id, String toolName, String argsJson) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingApprovals.put(id, future);
        String summary = "will run with " + oneLine(argsJson, 120);
        ui.showApproval(
            toolName,
            summary,
            approved -> {
                CompletableFuture<Boolean> f = pendingApprovals.remove(id);
                if (f != null) {
                    f.complete(approved);
                }
            }
        );
        try {
            return future.get();
        } catch (Exception ex) {
            return false;
        } finally {
            pendingApprovals.remove(id);
        }
    }

    /** Resolves any outstanding approval rows as denied (e.g. on cancel/complete). */
    private void failPendingApprovals() {
        pendingApprovals.forEach((id, future) -> future.complete(false));
        pendingApprovals.clear();
        ui.hideApproval();
    }

    private static String oneLine(String s, int max) {
        if (s == null) {
            return "";
        }
        String flat = s.replaceAll("\\s+", " ").trim();
        if (flat.length() > max) {
            flat = flat.substring(0, max - 1) + "…";
        }
        return escapeHtml(flat);
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private javax.swing.Timer reloadDebounce;

    /**
     * Coalesces the agent's rapid, successive mutations into a single live
     * project reload so the IDE reflects changes as the AI works, without
     * thrashing the trees on every individual tool call.
     */
    private void scheduleLiveReload() {
        SwingUtilities.invokeLater(
            () -> {
                if (reloadDebounce == null) {
                    reloadDebounce = new javax.swing.Timer(400, e -> mainFrame.reloadProject());
                    reloadDebounce.setRepeats(false);
                }
                reloadDebounce.restart();
            }
        );
    }

    /**
     * Reloads the project immediately (e.g. at the end of an agent turn),
     * cancelling any pending debounced reload.
     */
    private void triggerLiveReloadNow() {
        SwingUtilities.invokeLater(
            () -> {
                if (reloadDebounce != null) {
                    reloadDebounce.stop();
                }
                mainFrame.reloadProject();
            }
        );
    }

    public void clearConversation() {
        session.clear();
        contextMessage = null;
        session.addMessage(ChatMessage.system(AuthoringSkill.systemPrompt()));
        usageTracker.reset();
        ui.getChatWebView().clear();
        ui.setFooter(" ");
    }

    // ── Conversation history ───────────────────────────────────

    /** Persists the current conversation (excluding the system prompt). */
    private void persistCurrentConversation() {
        try {
            List<ChatMessage> saved = new ArrayList<>();
            String title = null;
            for (ChatMessage m : session.getMessages()) {
                if (ChatMessage.ROLE_SYSTEM.equals(m.getRole())) {
                    continue;
                }
                saved.add(m);
                if (
                    title == null &&
                    ChatMessage.ROLE_USER.equals(m.getRole()) &&
                    m.getContent() != null
                ) {
                    title = m.getContent();
                }
            }
            if (saved.isEmpty() || title == null) {
                return;
            }
            if (currentConversationId == null) {
                currentConversationId = historyStore.newId();
            }
            ChatHistoryStore.Conversation c = new ChatHistoryStore.Conversation();
            c.id = currentConversationId;
            c.title = title.length() > 60 ? title.substring(0, 60) + "\u2026" : title;
            c.model = session.getModel();
            long now = System.currentTimeMillis();
            c.createdAt = now;
            c.updatedAt = now;
            c.messages = saved;
            historyStore.save(c);
        } catch (Exception ex) {
            LOG.log(Level.FINE, "Could not persist conversation", ex);
        }
    }

    /** Saves the current chat and starts a fresh one. */
    public void newConversation() {
        persistCurrentConversation();
        currentConversationId = null;
        clearConversation();
    }

    /** History entries for the menu, most recently updated first. */
    public List<ChatHistoryStore.Entry> listHistory() {
        return historyStore.list();
    }

    /** Loads a saved conversation into the chat, saving the current one first. */
    public void loadConversation(String id) {
        ChatHistoryStore.Conversation c = historyStore.load(id);
        if (c == null) {
            return;
        }
        persistCurrentConversation();
        currentConversationId = id;
        session.clear();
        contextMessage = null;
        session.addMessage(ChatMessage.system(AuthoringSkill.systemPrompt()));
        if (c.model != null && !c.model.isEmpty()) {
            session.setModel(c.model);
        }
        usageTracker.reset();
        ui.getChatWebView().clear();
        for (ChatMessage m : c.messages) {
            session.addMessage(m);
            String role = m.getRole();
            boolean renderable =
                (ChatMessage.ROLE_USER.equals(role) || ChatMessage.ROLE_ASSISTANT.equals(role)) &&
                m.getContent() != null &&
                !m.getContent().isBlank();
            if (renderable) {
                ui.getChatWebView().addMessage(m);
            }
        }
        ui.setFooter(" ");
    }

    private void showError(String message) {
        ui.getChatWebView().showError(message);
    }

    @Override
    public void onSlideLeaving(String slideName) {
        // No-op for now; reserved for autosave/cleanup parity with other panels.
    }
}
