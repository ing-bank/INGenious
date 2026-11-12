package com.ing.ide.main.mainui.components.aichat.ui;

import com.ing.ide.main.mainui.components.aichat.model.ChatMessage;
import com.ing.ide.main.mainui.components.aichat.util.MarkdownRenderer;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javax.swing.JComponent;

/**
 * Renders the chat transcript using an embedded JavaFX {@link WebView} hosted in
 * a Swing {@link JFXPanel}. User and assistant messages are shown as bubbles
 * with Markdown rendered to HTML. Supports incremental streaming updates to the
 * in-progress assistant message.
 *
 * <p>All typography is expressed in {@code rem} units relative to the document
 * root size, so {@link #zoomIn()}, {@link #zoomOut()} and {@link #resetZoom()}
 * scale the entire transcript — body text, headings, tables, code and badges —
 * proportionally. The chosen size is persisted across IDE sessions.</p>
 *
 * <p>All WebView/DOM mutations are marshalled onto the JavaFX application thread
 * via {@link Platform#runLater}; callers may invoke the public methods from any
 * thread.</p>
 */
public class ChatWebView {
    private static final Logger LOG = Logger.getLogger(ChatWebView.class.getName());

    private static final Preferences PREFS = Preferences.userNodeForPackage(ChatWebView.class);
    private static final String PREF_FONT_PX = "chat.font.px";

    /** Root font size in CSS pixels; every other size is a {@code rem} multiple. */
    private static final double DEFAULT_FONT_PX = 14.5;
    private static final double MIN_FONT_PX = 10.0;
    private static final double MAX_FONT_PX = 26.0;
    private static final double FONT_STEP_PX = 1.0;

    /** Classpath directories searched for the bundled INGMe fonts, in order. */
    private static final String[] FONT_DIRS = { "/ui/resources/fonts/", "/fonts/" };

    /** A bundled INGMe cut and the CSS weight/style it should be registered as. */
    private record FontVariant(String fileName, String weight, String style) {}

    /**
     * Cross-platform monospace stack. {@code ui-monospace}/{@code Menlo} resolve
     * on macOS (where {@code Consolas} is absent and the text would otherwise
     * fall through to the generic default), {@code Cascadia Mono}/{@code
     * Consolas} on Windows, and {@code DejaVu Sans Mono} on Linux.
     */
    private static final String MONO_STACK =
        "ui-monospace,'SFMono-Regular','Menlo','Cascadia Mono','Consolas'," +
        "'DejaVu Sans Mono',monospace";

    private static final FontVariant[] FONT_VARIANTS = {
        new FontVariant("ingme_regular.ttf", "400", "normal"),
        new FontVariant("ingme_italic.ttf", "400", "italic"),
        new FontVariant("ingme_bold.ttf", "700", "normal"),
        new FontVariant("ingme_bolditalic.ttf", "700", "italic")
    };

    private final JFXPanel fxPanel = new JFXPanel();
    private final AtomicReference<WebEngine> engineRef = new AtomicReference<>();
    private volatile boolean ready;
    private final StringBuilder pendingScript = new StringBuilder();
    private volatile double fontPx;

    public ChatWebView() {
        this.fontPx = loadFontPx();
        Platform.runLater(this::initFx);
    }

    private void initFx() {
        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);
        WebEngine engine = webView.getEngine();
        engine.loadContent(baseHtml());
        engine
            .getLoadWorker()
            .stateProperty()
            .addListener(
                (obs, oldState, newState) -> {
                    if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                        ready = true;
                        flushPending(engine);
                    }
                }
            );
        engineRef.set(engine);
        Scene scene = new Scene(webView);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleZoomShortcut);
        fxPanel.setScene(scene);
    }

    /**
     * Handles Ctrl/Cmd {@code +}, {@code -} and {@code 0} while the transcript
     * has focus. The equivalent Swing bindings live in {@code AICopilotUI} so the
     * shortcuts also work while the user is typing in the composer.
     */
    private void handleZoomShortcut(KeyEvent event) {
        if (!event.isShortcutDown()) {
            return;
        }
        KeyCode code = event.getCode();
        if (code == KeyCode.PLUS || code == KeyCode.ADD || code == KeyCode.EQUALS) {
            zoomIn();
        } else if (code == KeyCode.MINUS || code == KeyCode.SUBTRACT) {
            zoomOut();
        } else if (code == KeyCode.DIGIT0 || code == KeyCode.NUMPAD0) {
            resetZoom();
        } else {
            return;
        }
        event.consume();
    }

    /** Increases the transcript font size by one step and persists it. */
    public void zoomIn() {
        setFontPx(fontPx + FONT_STEP_PX);
    }

    /** Decreases the transcript font size by one step and persists it. */
    public void zoomOut() {
        setFontPx(fontPx - FONT_STEP_PX);
    }

    /** Restores the default transcript font size and persists it. */
    public void resetZoom() {
        setFontPx(DEFAULT_FONT_PX);
    }

    private void setFontPx(double requested) {
        double clamped = Math.max(MIN_FONT_PX, Math.min(MAX_FONT_PX, requested));
        if (Math.abs(clamped - fontPx) < 0.001) {
            return;
        }
        fontPx = clamped;
        storeFontPx(clamped);
        runScript("setRootFontSize('" + formatPx(clamped) + "');");
    }

    private static double loadFontPx() {
        double stored = PREFS.getDouble(PREF_FONT_PX, DEFAULT_FONT_PX);
        return Math.max(MIN_FONT_PX, Math.min(MAX_FONT_PX, stored));
    }

    private static void storeFontPx(double px) {
        try {
            PREFS.putDouble(PREF_FONT_PX, px);
            PREFS.flush();
        } catch (Exception ex) {
            // A read-only preferences backing store must not break the chat UI.
            LOG.log(Level.FINE, "Unable to persist chat font size", ex);
        }
    }

    private static String formatPx(double px) {
        return String.format(Locale.ROOT, "%.2f", px) + "px";
    }

    public JComponent getComponent() {
        return fxPanel;
    }

    /** Appends a finalized message bubble for the given role. */
    public void addMessage(ChatMessage message) {
        if (message == null) {
            return;
        }
        String html = MarkdownRenderer.toHtml(message.getContent());
        runScript("appendMessage(" + jsString(message.getRole()) + ", " + jsString(html) + ");");
    }

    /** Starts a new, empty assistant bubble that subsequent tokens append to. */
    public void beginAssistantMessage() {
        runScript("beginAssistant();");
    }

    /**
     * Updates the in-progress assistant bubble with the full accumulated text so
     * far (re-rendered as Markdown). Passing the cumulative text keeps Markdown
     * structures such as code fences correct while streaming.
     */
    public void updateAssistantMessage(String fullTextSoFar) {
        String html = MarkdownRenderer.toHtml(fullTextSoFar);
        runScript("updateAssistant(" + jsString(html) + ");");
    }

    /** Finalizes the in-progress assistant bubble. */
    public void endAssistantMessage() {
        runScript("endAssistant();");
    }

    /**
     * Appends a small credit-consumption badge beneath the last message, so the
     * cost of each prompt stays visible in the transcript (including reloaded
     * conversations, where the footer no longer applies).
     */
    public void appendUsage(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        runScript("appendUsage(" + jsString(MarkdownRenderer.escape(text)) + ");");
    }

    /** Removes all messages from the transcript. */
    public void clear() {
        runScript("clearAll();");
    }

    /** Shows an animated "Thinking.." placeholder while awaiting a response. */
    public void showThinking() {
        runScript("showThinking();");
    }

    /** Removes the "Thinking.." placeholder. */
    public void hideThinking() {
        runScript("hideThinking();");
    }

    /** Shows a transient error bubble. */
    public void showError(String text) {
        runScript("appendError(" + jsString(MarkdownRenderer.escape(text)) + ");");
    }

    /** Appends a collapsible "used tool" row in the running state. */
    public void appendToolCall(String id, String toolName, String argsSummary) {
        runScript(
            "appendToolCall(" +
            jsString(id) +
            "," +
            jsString(toolName) +
            "," +
            jsString(argsSummary) +
            ");"
        );
    }

    /** Marks a previously-appended tool row as done or error, with detail JSON. */
    public void resolveToolCall(String id, String resultSummary, boolean isError, String detail) {
        runScript(
            "resolveToolCall(" +
            jsString(id) +
            "," +
            jsString(resultSummary) +
            "," +
            (isError ? "true" : "false") +
            "," +
            jsString(detail) +
            ");"
        );
    }

    /**
     * Appends a coloured "Summary" card with status pills (OK/WARN/FAIL/INFO
     * counts) and a compact tool-call footnote for the turn. Deliberately does
     * NOT itemize every tool call — that's noise, not a user-facing outcome;
     * the assistant's own answer already explains what happened.
     */
    public void appendActivityReport(com.ing.engine.aicli.ai.ActivityReport.Result r) {
        if (r == null || r.totalCalls == 0) {
            return;
        }
        runScript("appendActivity(" + jsString(buildActivityHtml(r)) + ");");
    }

    private static String buildActivityHtml(com.ing.engine.aicli.ai.ActivityReport.Result r) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='act-head'>Summary</div>");
        StringBuilder pills = new StringBuilder();
        if (r.okCount > 0) {
            pills.append("<span class='pill ok'>").append(r.okCount).append(" OK</span>");
        }
        if (r.infoCount > 0) {
            pills.append("<span class='pill info'>").append(r.infoCount).append(" INFO</span>");
        }
        if (r.warnCount > 0) {
            pills.append("<span class='pill warn'>").append(r.warnCount).append(" WARN</span>");
        }
        if (r.failCount > 0) {
            pills.append("<span class='pill fail'>").append(r.failCount).append(" FAIL</span>");
        }
        if (pills.length() > 0) {
            sb.append("<div class='act-pills'>").append(pills).append("</div>");
        }
        String foot = r.totalCalls + (r.totalCalls == 1 ? " tool call" : " tool calls");
        if (r.minorCount > 0) {
            foot += " \u00b7 " + r.minorCount + " lookup" + (r.minorCount == 1 ? "" : "s");
        }
        if (r.retryCount > 0) {
            foot += " \u00b7 " + r.retryCount + " self-corrected";
        }
        sb.append("<div class='act-foot'>").append(MarkdownRenderer.escape(foot)).append("</div>");
        return sb.toString();
    }

    private void runScript(String script) {
        Platform.runLater(
            () -> {
                WebEngine engine = engineRef.get();
                if (ready && engine != null) {
                    engine.executeScript(script);
                } else {
                    synchronized (pendingScript) {
                        pendingScript.append(script).append('\n');
                    }
                }
            }
        );
    }

    private void flushPending(WebEngine engine) {
        synchronized (pendingScript) {
            if (pendingScript.length() > 0) {
                engine.executeScript(pendingScript.toString());
                pendingScript.setLength(0);
            }
        }
    }

    private static String jsString(String value) {
        if (value == null) {
            return "''";
        }
        String escaped = value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\r", "")
            .replace("\n", "\\n");
        return "'" + escaped + "'";
    }

    /**
     * Builds {@code @font-face} rules for every bundled INGMe variant that is
     * present at runtime, so the WebView uses the real bold and italic cuts
     * instead of synthesizing (faux-bolding / slanting) them from the regular
     * face. Variants that cannot be loaded are simply omitted, in which case the
     * browser falls back to synthesis for that style.
     */
    private String ingMeFontFace() {
        StringBuilder css = new StringBuilder();
        for (FontVariant variant : FONT_VARIANTS) {
            String b64 = loadFontBase64(variant.fileName);
            if (b64 == null) {
                continue;
            }
            css
                .append("@font-face{font-family:'INGMe';font-display:block;font-weight:")
                .append(variant.weight)
                .append(";font-style:")
                .append(variant.style)
                .append(";src:url('data:font/ttf;base64,")
                .append(b64)
                .append("') format('truetype');}");
        }
        return css.toString();
    }

    /**
     * Reads a bundled font from the classpath and inlines it as base64, so the
     * WebView resolves it regardless of the working directory. Returns
     * {@code null} when the file is unavailable on every candidate path.
     */
    private static String loadFontBase64(String fileName) {
        for (String dir : FONT_DIRS) {
            try (java.io.InputStream in = ChatWebView.class.getResourceAsStream(dir + fileName)) {
                if (in != null) {
                    return java.util.Base64.getEncoder().encodeToString(in.readAllBytes());
                }
            } catch (Exception ex) {
                LOG.log(Level.FINE, "Unable to load bundled font " + dir + fileName, ex);
            }
        }
        return null;
    }

    private String baseHtml() {
        String fontFace = ingMeFontFace();
        return (
            "<!DOCTYPE html><html><head><meta charset='utf-8'>" +
            "<style>" +
            fontFace +
            "*{box-sizing:border-box;}" +
            // Every size below is a rem multiple of this root, so changing it
            // (via setRootFontSize) scales the whole transcript proportionally.
            "html{font-size:" +
            formatPx(fontPx) +
            ";}" +
            "body{font-family:'INGMe','Segoe UI',sans-serif;font-size:1rem;line-height:1.65;" +
            "margin:0;padding:14px 12px;background:#faf9fc;color:#1e1e1e;" +
            "text-rendering:optimizeLegibility;font-kerning:normal;" +
            "font-variant-ligatures:common-ligatures contextual;" +
            "font-feature-settings:'kern' 1,'liga' 1,'clig' 1;" +
            "-webkit-font-smoothing:antialiased;-moz-osx-font-smoothing:grayscale;}" +
            ".msg{position:relative;margin:10px 0;padding:12px 15px;border-radius:14px;max-width:88%;" +
            "white-space:normal;word-wrap:break-word;overflow-wrap:anywhere;overflow:hidden;" +
            "animation:fadeIn .25s ease;}" +
            "@keyframes fadeIn{from{opacity:0;transform:translateY(4px);}to{opacity:1;transform:none;}}" +
            ".user{background:linear-gradient(135deg,#9a6bff,#7a45f0);color:#ffffff;margin-left:auto;" +
            "border-bottom-right-radius:4px;box-shadow:0 2px 8px rgba(122,69,240,.28);}" +
            ".assistant{background:#ffffff;border:1px solid #ece4fa;color:#1e1e1e;" +
            "border-bottom-left-radius:4px;box-shadow:0 2px 10px rgba(90,50,160,.08);}" +
            ".error{background:#fdecea;border:1px solid #f3b5b0;color:#b3261e;border-radius:12px;}" +
            ".role{font-size:0.81rem;opacity:0.6;margin-bottom:4px;text-transform:uppercase;}" +
            "table{border-collapse:collapse;margin:10px 0;font-size:0.93rem;width:auto;" +
            "border-radius:8px;overflow:hidden;box-shadow:0 1px 4px rgba(0,0,0,.06);}" +
            "th,td{border:1px solid #e4dbf5;padding:6px 12px;text-align:left;}" +
            "th{background:#f0e8ff;color:#4a2b9c;font-weight:700;}" +
            "tr:nth-child(even) td{background:#faf8ff;}" +
            ".lnk{color:#6b3fd4;text-decoration:underline;cursor:pointer;}" +
            "a{color:#6b3fd4;text-decoration:none;} a:hover{text-decoration:underline;}" +
            ".user a,.user .lnk{color:#ffffff;}" +
            ".thinking{display:flex;align-items:center;gap:6px;color:#6a6a6a;font-style:italic;}" +
            ".thinking .dot{width:6px;height:6px;border-radius:50%;background:#B487FF;" +
            "display:inline-block;animation:blink 1.2s infinite ease-in-out;}" +
            ".thinking .dot:nth-child(2){animation-delay:0.2s;}" +
            ".thinking .dot:nth-child(3){animation-delay:0.4s;}" +
            "@keyframes blink{0%,80%,100%{opacity:0.2;}40%{opacity:1;}}" +
            "pre{background:#2b2540;padding:12px 14px;border-radius:10px;overflow-x:auto;" +
            "color:#f3f0ff;margin:10px 0;box-shadow:0 2px 8px rgba(43,37,64,.25);}" +
            "pre code{color:#f3f0ff;font-size:0.93rem;background:none;padding:0;}" +
            // Ligatures are helpful in prose but misleading in code (=>, !=, --).
            "code{font-family:" +
            MONO_STACK +
            ";font-size:0.93rem;" +
            "font-variant-ligatures:none;font-feature-settings:'liga' 0,'clig' 0;" +
            "background:#efe9fb;color:#6234c9;padding:1px 6px;border-radius:5px;}" +
            ".user code{background:rgba(255,255,255,.22);color:#ffffff;}" +
            "p{margin:7px 0;} p:first-child{margin-top:0;} p:last-child{margin-bottom:0;}" +
            "ul,ol{margin:7px 0 7px 22px;} li{margin:3px 0;} ul li::marker{color:#7a45f0;}" +
            "h1,h2,h3,h4{margin:12px 0 6px;line-height:1.3;font-weight:700;color:#3a2b6b;}" +
            "h1{font-size:1.33rem;} h2{font-size:1.19rem;} h3{font-size:1.04rem;} h4{font-size:0.96rem;}" +
            "h1,h2{border-bottom:1px solid #ece4fa;padding-bottom:4px;}" +
            "strong{color:#4a2b9c;font-weight:700;} .user strong{color:#ffffff;}" +
            "blockquote{margin:8px 0;padding:6px 12px;border-left:3px solid #b79cff;" +
            "background:#f6f2ff;color:#4a4560;border-radius:0 6px 6px 0;}" +
            "hr{border:none;border-top:1px solid #e4dbf5;margin:12px 0;}" +
            ".toolcall{margin:4px 0;padding:4px 8px;font-size:0.89rem;border-radius:6px;" +
            "background:#f3f3f3;border:1px solid #d0d7de;cursor:pointer;color:#333;}" +
            ".toolcall .tname{font-family:" +
            MONO_STACK +
            ";color:#0a7d6b;}" +
            ".toolcall .targs{opacity:0.7;margin-left:6px;}" +
            ".toolcall .ticon{margin-right:6px;}" +
            ".toolcall.done{border-color:#2d5a2d;} .toolcall.done .ticon{color:#2e7d32;}" +
            ".toolcall.err{border-color:#c0392b;} .toolcall.err .ticon{color:#c0392b;}" +
            ".tdetail{display:none;margin:2px 0 6px 0;padding:8px;font-size:0.81rem;" +
            "font-family:" +
            MONO_STACK +
            ";white-space:pre-wrap;word-break:break-word;" +
            "background:#f0f1f3;border-radius:6px;color:#2a6b2a;}" +
            ".tdetail.open{display:block;}" +
            ".activity{margin:8px 0;padding:10px 12px;border-radius:8px;" +
            "background:#faf7ff;border:1px solid #e0d3f7;}" +
            ".act-head{font-size:0.81rem;text-transform:uppercase;letter-spacing:.5px;" +
            "color:#6b3fd4;font-weight:bold;margin-bottom:8px;}" +
            ".act-pills{margin-top:8px;}" +
            ".pill{display:inline-block;padding:2px 10px;border-radius:12px;font-size:0.81rem;" +
            "font-weight:bold;margin-right:6px;}" +
            ".pill.ok{background:#e6f4ea;color:#1e7e34;}.pill.info{background:#e3f2fd;color:#0277bd;}" +
            ".pill.warn{background:#fff4e5;color:#b26a00;}.pill.fail{background:#fdecea;color:#c62828;}" +
            ".act-foot{margin-top:8px;font-size:0.81rem;color:#8a8a8a;}" +
            ".usage{margin:-4px 0 10px 4px;font-size:0.81rem;color:#7a6f92;display:flex;" +
            "align-items:center;gap:5px;}" +
            ".usage .ucoin{color:#b08400;}" +
            ".approval{margin:8px 0;padding:10px 12px;border-radius:8px;" +
            "background:#3a2f16;border-left:3px solid #d7a017;color:#f0e0b0;}" +
            ".approval b{color:#ffd479;}" +
            ".approval .btns{margin-top:8px;}" +
            ".approval button{margin-right:8px;padding:4px 12px;border:none;border-radius:4px;" +
            "cursor:pointer;font-size:0.89rem;}" +
            ".approval .apply{background:#2d7d2d;color:#fff;}" +
            ".approval .skip{background:#5a5a5a;color:#fff;}" +
            "</style></head><body><div id='chat'></div>" +
            "<script>" +
            "var chat=document.getElementById('chat');var cur=null;" +
            "function setRootFontSize(px){document.documentElement.style.fontSize=px;scroll();}" +
            "function scroll(){window.scrollTo(0,document.body.scrollHeight);}" +
            "function hideThinking(){var t=document.getElementById('thinking');" +
            "if(t){t.parentNode.removeChild(t);}}" +
            "function showThinking(){if(document.getElementById('thinking')){return;}" +
            "var d=document.createElement('div');d.className='msg assistant thinking';d.id='thinking';" +
            "d.innerHTML=\"<span>Thinking..</span><span class='dot'></span>\"+" +
            "\"<span class='dot'></span><span class='dot'></span>\";chat.appendChild(d);scroll();}" +
            "function appendMessage(role,html){hideThinking();var d=document.createElement('div');" +
            "d.className='msg '+role;d.innerHTML=html;" +
            "chat.appendChild(d);scroll();}" +
            "function appendActivity(html){hideThinking();var d=document.createElement('div');" +
            "d.className='activity';d.innerHTML=html;chat.appendChild(d);scroll();}" +
            "function appendUsage(text){var d=document.createElement('div');" +
            "d.className='usage';d.innerHTML=\"<span class='ucoin'>\\u25C9</span><span>\"+text+" +
            "\"</span>\";chat.appendChild(d);scroll();}" +
            "function appendError(text){hideThinking();var d=document.createElement('div');" +
            "d.className='msg error';d.innerHTML=text;chat.appendChild(d);scroll();}" +
            "function beginAssistant(){hideThinking();cur=document.createElement('div');" +
            "cur.className='msg assistant';cur.innerHTML=\"<div class='body'></div>\";" +
            "chat.appendChild(cur);scroll();}" +
            "function updateAssistant(html){if(cur){cur.querySelector('.body').innerHTML=html;" +
            "scroll();}}" +
            "function endAssistant(){cur=null;}" +
            "function clearAll(){chat.innerHTML='';cur=null;}" +
            "function toggleDetail(id){var d=document.getElementById('td-'+id);" +
            "if(d){d.classList.toggle('open');}}" +
            "function appendToolCall(id,name,args){hideThinking();var r=document.createElement('div');" +
            "r.className='toolcall';r.id='tc-'+id;r.setAttribute('onclick',\"toggleDetail('\"+id+\"')\");" +
            "r.innerHTML=\"<span class='ticon'>\\u25CC</span><span class='tname'>\"+name+" +
            "\"</span><span class='targs'>\"+args+\"</span>\";chat.appendChild(r);" +
            "var d=document.createElement('div');d.className='tdetail';d.id='td-'+id;" +
            "chat.appendChild(d);scroll();}" +
            "function resolveToolCall(id,summary,isErr,detail){var r=document.getElementById('tc-'+id);" +
            "if(r){r.classList.add(isErr?'err':'done');" +
            "r.querySelector('.ticon').innerHTML=isErr?'\\u2717':'\\u2713';" +
            "r.querySelector('.targs').innerHTML=summary;}" +
            "var d=document.getElementById('td-'+id);if(d){d.textContent=detail;}scroll();}" +
            "</script></body></html>"
        );
    }
}
