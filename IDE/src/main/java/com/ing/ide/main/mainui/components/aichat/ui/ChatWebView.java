package com.ing.ide.main.mainui.components.aichat.ui;

import com.ing.ide.main.mainui.components.aichat.model.ChatMessage;
import com.ing.ide.main.mainui.components.aichat.util.MarkdownRenderer;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javax.swing.JComponent;

/**
 * Renders the chat transcript using an embedded JavaFX {@link WebView} hosted in
 * a Swing {@link JFXPanel}. User and assistant messages are shown as bubbles
 * with Markdown rendered to HTML. Supports incremental streaming updates to the
 * in-progress assistant message.
 *
 * <p>All WebView/DOM mutations are marshalled onto the JavaFX application thread
 * via {@link Platform#runLater}; callers may invoke the public methods from any
 * thread.</p>
 */
public class ChatWebView {
    private final JFXPanel fxPanel = new JFXPanel();
    private final AtomicReference<WebEngine> engineRef = new AtomicReference<>();
    private volatile boolean ready;
    private final StringBuilder pendingScript = new StringBuilder();

    public ChatWebView() {
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
        fxPanel.setScene(new Scene(webView));
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
     * Appends a coloured "Activity" card summarizing what the turn actually did
     * (test cases created, tests executed, objects captured, …) with status
     * badges and pills. Raw tool calls are reduced to a minimal footer line.
     */
    public void appendActivityReport(com.ing.engine.aicli.ai.ActivityReport.Result r) {
        if (r == null || r.isEmpty()) {
            return;
        }
        runScript("appendActivity(" + jsString(buildActivityHtml(r)) + ");");
    }

    private static String buildActivityHtml(com.ing.engine.aicli.ai.ActivityReport.Result r) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='act-head'>Activity</div>");
        for (com.ing.engine.aicli.ai.ActivityReport.Activity a : r.activities) {
            String cls = badgeClass(a.status);
            sb
                .append("<div class='act-row'><div class='act-line'>")
                .append("<span class='badge ")
                .append(cls)
                .append("'>")
                .append(badgeLabel(a.status))
                .append("</span><span class='act-title'>")
                .append(MarkdownRenderer.escape(a.title))
                .append("</span></div>");
            if (a.details != null && !a.details.isEmpty()) {
                StringBuilder det = new StringBuilder();
                for (int i = 0; i < a.details.size(); i++) {
                    if (i > 0) {
                        det.append("  \u00b7  ");
                    }
                    det.append(MarkdownRenderer.escape(a.details.get(i)));
                }
                sb.append("<div class='act-details'>").append(det).append("</div>");
            }
            sb.append("</div>");
        }
        if (!r.activities.isEmpty()) {
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
        }
        String foot = r.totalCalls + (r.totalCalls == 1 ? " tool call" : " tool calls");
        if (r.minorCount > 0) {
            foot += " \u00b7 " + r.minorCount + " lookup" + (r.minorCount == 1 ? "" : "s");
        }
        sb.append("<div class='act-foot'>").append(MarkdownRenderer.escape(foot)).append("</div>");
        return sb.toString();
    }

    private static String badgeClass(com.ing.engine.aicli.ai.ActivityReport.Status s) {
        switch (s) {
            case FAIL:
                return "fail";
            case WARN:
                return "warn";
            case INFO:
                return "info";
            default:
                return "ok";
        }
    }

    private static String badgeLabel(com.ing.engine.aicli.ai.ActivityReport.Status s) {
        switch (s) {
            case FAIL:
                return "FAIL";
            case WARN:
                return "WARN";
            case INFO:
                return "INFO";
            default:
                return "OK";
        }
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
     * Builds a {@code @font-face} rule for the bundled INGMe font if the TTF is
     * present at runtime, so the WebView can use it. Falls back to the default
     * font when the file is unavailable.
     */
    private String ingMeFontFace() {
        // Load the bundled INGMe TTF from the classpath and inline it as a base64
        // data URI so the JavaFX WebView renders it regardless of working dir.
        String[] paths = { "/ui/resources/fonts/ingme_regular.ttf", "/fonts/ingme_regular.ttf" };
        for (String p : paths) {
            try (java.io.InputStream in = ChatWebView.class.getResourceAsStream(p)) {
                if (in != null) {
                    byte[] bytes = in.readAllBytes();
                    String b64 = java.util.Base64.getEncoder().encodeToString(bytes);
                    return (
                        "@font-face{font-family:'INGMe';font-display:block;" +
                        "src:url('data:font/ttf;base64," +
                        b64 +
                        "') format('truetype');}"
                    );
                }
            } catch (Exception ex) {
                // try next path / fall back to the default font
            }
        }
        return "";
    }

    private String baseHtml() {
        String fontFace = ingMeFontFace();
        return (
            "<!DOCTYPE html><html><head><meta charset='utf-8'>" +
            "<style>" +
            fontFace +
            "*{box-sizing:border-box;}" +
            "body{font-family:'INGMe','Segoe UI',sans-serif;font-size:13.5px;line-height:1.55;" +
            "margin:0;padding:14px 12px;background:#faf9fc;color:#1e1e1e;" +
            "-webkit-font-smoothing:antialiased;}" +
            ".msg{position:relative;margin:10px 0;padding:12px 15px;border-radius:14px;max-width:88%;" +
            "white-space:normal;word-wrap:break-word;overflow-wrap:anywhere;overflow:hidden;" +
            "animation:fadeIn .25s ease;}" +
            "@keyframes fadeIn{from{opacity:0;transform:translateY(4px);}to{opacity:1;transform:none;}}" +
            ".user{background:linear-gradient(135deg,#9a6bff,#7a45f0);color:#ffffff;margin-left:auto;" +
            "border-bottom-right-radius:4px;box-shadow:0 2px 8px rgba(122,69,240,.28);}" +
            ".assistant{background:#ffffff;border:1px solid #ece4fa;color:#1e1e1e;" +
            "border-bottom-left-radius:4px;box-shadow:0 2px 10px rgba(90,50,160,.08);}" +
            ".error{background:#fdecea;border:1px solid #f3b5b0;color:#b3261e;border-radius:12px;}" +
            ".role{font-size:11px;opacity:0.6;margin-bottom:4px;text-transform:uppercase;}" +
            "table{border-collapse:collapse;margin:10px 0;font-size:12.5px;width:auto;" +
            "border-radius:8px;overflow:hidden;box-shadow:0 1px 4px rgba(0,0,0,.06);}" +
            "th,td{border:1px solid #e4dbf5;padding:6px 12px;text-align:left;}" +
            "th{background:#f0e8ff;color:#4a2b9c;font-weight:600;}" +
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
            "pre code{color:#f3f0ff;font-size:12.5px;background:none;padding:0;}" +
            "code{font-family:'Consolas','SFMono-Regular',monospace;font-size:12.5px;" +
            "background:#efe9fb;color:#6234c9;padding:1px 6px;border-radius:5px;}" +
            ".user code{background:rgba(255,255,255,.22);color:#ffffff;}" +
            "p{margin:7px 0;} p:first-child{margin-top:0;} p:last-child{margin-bottom:0;}" +
            "ul,ol{margin:7px 0 7px 22px;} li{margin:3px 0;} ul li::marker{color:#7a45f0;}" +
            "h1,h2,h3,h4{margin:12px 0 6px;line-height:1.3;font-weight:600;color:#3a2b6b;}" +
            "h1{font-size:18px;} h2{font-size:16px;} h3{font-size:14px;} h4{font-size:13px;}" +
            "h1,h2{border-bottom:1px solid #ece4fa;padding-bottom:4px;}" +
            "strong{color:#4a2b9c;font-weight:600;} .user strong{color:#ffffff;}" +
            "blockquote{margin:8px 0;padding:6px 12px;border-left:3px solid #b79cff;" +
            "background:#f6f2ff;color:#4a4560;border-radius:0 6px 6px 0;}" +
            "hr{border:none;border-top:1px solid #e4dbf5;margin:12px 0;}" +
            ".toolcall{margin:4px 0;padding:4px 8px;font-size:12px;border-radius:6px;" +
            "background:#f3f3f3;border:1px solid #d0d7de;cursor:pointer;color:#333;}" +
            ".toolcall .tname{font-family:'Consolas',monospace;color:#0a7d6b;}" +
            ".toolcall .targs{opacity:0.7;margin-left:6px;}" +
            ".toolcall .ticon{margin-right:6px;}" +
            ".toolcall.done{border-color:#2d5a2d;} .toolcall.done .ticon{color:#2e7d32;}" +
            ".toolcall.err{border-color:#c0392b;} .toolcall.err .ticon{color:#c0392b;}" +
            ".tdetail{display:none;margin:2px 0 6px 0;padding:8px;font-size:11px;" +
            "font-family:'Consolas',monospace;white-space:pre-wrap;word-break:break-word;" +
            "background:#f0f1f3;border-radius:6px;color:#2a6b2a;}" +
            ".tdetail.open{display:block;}" +
            ".activity{margin:8px 0;padding:10px 12px;border-radius:8px;" +
            "background:#faf7ff;border:1px solid #e0d3f7;}" +
            ".act-head{font-size:11px;text-transform:uppercase;letter-spacing:.5px;" +
            "color:#6b3fd4;font-weight:bold;margin-bottom:8px;}" +
            ".act-row{margin:8px 0;}" +
            ".act-line{display:flex;align-items:center;gap:8px;}" +
            ".act-title{font-weight:bold;color:#1e1e1e;}" +
            ".act-details{margin:2px 0 0 4px;color:#555;font-size:12px;}" +
            ".badge{display:inline-block;padding:1px 8px;border-radius:10px;font-size:10px;" +
            "font-weight:bold;text-transform:uppercase;color:#ffffff;}" +
            ".badge.ok{background:#2e7d32;}.badge.info{background:#0277bd;}" +
            ".badge.warn{background:#ed6c02;}.badge.fail{background:#c62828;}" +
            ".act-pills{margin-top:8px;}" +
            ".pill{display:inline-block;padding:2px 10px;border-radius:12px;font-size:11px;" +
            "font-weight:bold;margin-right:6px;}" +
            ".pill.ok{background:#e6f4ea;color:#1e7e34;}.pill.info{background:#e3f2fd;color:#0277bd;}" +
            ".pill.warn{background:#fff4e5;color:#b26a00;}.pill.fail{background:#fdecea;color:#c62828;}" +
            ".act-foot{margin-top:8px;font-size:11px;color:#8a8a8a;}" +
            ".approval{margin:8px 0;padding:10px 12px;border-radius:8px;" +
            "background:#3a2f16;border-left:3px solid #d7a017;color:#f0e0b0;}" +
            ".approval b{color:#ffd479;}" +
            ".approval .btns{margin-top:8px;}" +
            ".approval button{margin-right:8px;padding:4px 12px;border:none;border-radius:4px;" +
            "cursor:pointer;font-size:12px;}" +
            ".approval .apply{background:#2d7d2d;color:#fff;}" +
            ".approval .skip{background:#5a5a5a;color:#fff;}" +
            "</style></head><body><div id='chat'></div>" +
            "<script>" +
            "var chat=document.getElementById('chat');var cur=null;" +
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
