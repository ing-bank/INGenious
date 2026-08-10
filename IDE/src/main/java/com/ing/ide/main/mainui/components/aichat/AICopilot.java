package com.ing.ide.main.mainui.components.aichat;

import com.ing.engine.support.DesktopApi;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.mainui.SlideShow;
import com.ing.ide.main.mainui.components.aichat.agent.AgentOrchestrator;
import com.ing.ide.main.mainui.components.aichat.auth.AICredentials;
import com.ing.ide.main.mainui.components.aichat.auth.DeviceCodeResponse;
import com.ing.ide.main.mainui.components.aichat.auth.GitHubDeviceAuthService;
import com.ing.ide.main.mainui.components.aichat.client.GitHubModelsClient;
import com.ing.ide.main.mainui.components.aichat.mcp.INGeniousToolServer;
import com.ing.ide.main.mainui.components.aichat.model.ChatCompletionRequest;
import com.ing.ide.main.mainui.components.aichat.model.ChatMessage;
import com.ing.ide.main.mainui.components.aichat.model.ChatSession;
import com.ing.ide.main.mainui.components.aichat.model.ModelInfo;
import com.ing.ide.main.mainui.components.aichat.model.TokenUsage;
import com.ing.ide.main.mainui.components.aichat.skills.AuthoringSkill;
import com.ing.ide.main.mainui.components.aichat.ui.AICopilotUI;
import com.ing.ide.main.mainui.components.aichat.ui.AISettingsDialog;
import com.ing.ide.main.mainui.components.aichat.ui.ToolApprovalDialog;
import com.ing.ide.main.mainui.components.aichat.util.TokenUsageTracker;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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
    private final INGeniousToolServer toolServer;

    private volatile Thread activeTurn;
    private volatile GitHubDeviceAuthService activeAuth;
    private volatile AgentOrchestrator activeAgent;

    public AICopilot(AppMainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.session = new ChatSession(credentials.getSelectedModel());
        this.session.addMessage(ChatMessage.system(AuthoringSkill.systemPrompt()));
        this.toolServer = new INGeniousToolServer(mainFrame);
        this.ui = new AICopilotUI(this);
        refreshAuthState();
    }

    public AICopilotUI getAICopilotUI() {
        return ui;
    }

    public AppMainFrame getMainFrame() {
        return mainFrame;
    }

    private void refreshAuthState() {
        boolean signedIn = credentials.isSignedIn();
        ui.setSignedIn(signedIn, credentials.getLogin());
        if (signedIn) {
            loadCatalogAsync();
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
        String token = credentials.getToken();
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
        if (!credentials.isSignedIn()) {
            showError("Please sign in to GitHub before chatting.");
            return;
        }
        ChatMessage userMessage = ChatMessage.user(text);
        session.addMessage(userMessage);
        ui.getChatWebView().addMessage(userMessage);
        if (ui.isAgentMode()) {
            startAgentTurn();
        } else {
            startStreamingTurn();
        }
    }

    private void startStreamingTurn() {
        final String token = credentials.getToken();
        if (token == null) {
            showError("Not signed in.");
            return;
        }
        ui.setGenerating(true);
        ui.getChatWebView().beginAssistantMessage();

        final ChatCompletionRequest request = new ChatCompletionRequest(
            session.getModel(),
            new ArrayList<>(session.getMessages()),
            true
        );

        final StringBuilder accumulated = new StringBuilder();

        Thread turn = new Thread(
            () -> {
                client.streamComplete(
                    token,
                    request,
                    new GitHubModelsClient.StreamListener() {

                        @Override
                        public void onToken(String chunk) {
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
        final String token = credentials.getToken();
        if (token == null) {
            showError("Not signed in.");
            return;
        }
        if (mainFrame.getProject() == null) {
            showError("Open a project before using agent mode.");
            return;
        }
        ui.setGenerating(true);
        final boolean[] mutated = { false };
        final AgentOrchestrator orchestrator = new AgentOrchestrator(client, toolServer);
        activeAgent = orchestrator;

        Thread turn = new Thread(
            () -> {
                orchestrator.run(
                    token,
                    session,
                    (toolName, argumentsJson) ->
                        ToolApprovalDialog.confirm(ui, toolName, argumentsJson),
                    new AgentOrchestrator.AgentListener() {

                        @Override
                        public void onAssistantText(String text) {
                            ui.getChatWebView().addMessage(ChatMessage.assistant(text));
                        }

                        @Override
                        public void onToolStart(String toolName, String argumentsJson) {
                            ui.setFooter("Running tool: " + toolName + "…");
                        }

                        @Override
                        public void onToolResult(String toolName, boolean error, String summary) {
                            if (!toolServer.isReadOnly(toolName) && !error) {
                                mutated[0] = true;
                            }
                            ui
                                .getChatWebView()
                                .addMessage(
                                    ChatMessage.assistant(
                                        (error ? "⚠️ **" : "🔧 **") + toolName + "** — " + summary
                                    )
                                );
                        }

                        @Override
                        public void onComplete() {
                            ui.setGenerating(false);
                            ui.setFooter(usageTracker.statusText());
                            activeAgent = null;
                            if (mutated[0]) {
                                refreshTrees();
                            }
                        }

                        @Override
                        public void onError(Throwable error) {
                            ui.setGenerating(false);
                            activeAgent = null;
                            if (mutated[0]) {
                                refreshTrees();
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

    private void refreshTrees() {
        SwingUtilities.invokeLater(
            () -> {
                try {
                    mainFrame.getTestDesign().getProjectTree().load();
                    mainFrame.getTestDesign().getReusableTree().load();
                } catch (Exception ex) {
                    LOG.log(Level.FINE, "Could not refresh trees after agent run", ex);
                }
            }
        );
    }

    public void clearConversation() {
        session.clear();
        session.addMessage(ChatMessage.system(AuthoringSkill.systemPrompt()));
        usageTracker.reset();
        ui.getChatWebView().clear();
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
