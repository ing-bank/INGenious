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

    /** Shows a transient error bubble. */
    public void showError(String text) {
        runScript("appendError(" + jsString(MarkdownRenderer.escape(text)) + ");");
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

    private String baseHtml() {
        return (
            "<!DOCTYPE html><html><head><meta charset='utf-8'>" +
            "<style>" +
            "body{font-family:'Segoe UI',sans-serif;font-size:13px;margin:0;padding:12px;" +
            "background:#1e1e1e;color:#e0e0e0;}" +
            ".msg{margin:8px 0;padding:10px 12px;border-radius:8px;max-width:92%;" +
            "white-space:normal;word-wrap:break-word;}" +
            ".user{background:#0e639c;color:#fff;margin-left:auto;}" +
            ".assistant{background:#2d2d2d;border:1px solid #3a3a3a;}" +
            ".error{background:#5a1d1d;border:1px solid #7a2a2a;color:#ffd0d0;}" +
            ".role{font-size:11px;opacity:0.6;margin-bottom:4px;text-transform:uppercase;}" +
            "pre{background:#0d0d0d;padding:10px;border-radius:6px;overflow-x:auto;}" +
            "code{font-family:'Consolas',monospace;font-size:12px;}" +
            "p{margin:4px 0;} ul,ol{margin:4px 0 4px 20px;}" +
            "</style></head><body><div id='chat'></div>" +
            "<script>" +
            "var chat=document.getElementById('chat');var cur=null;" +
            "function scroll(){window.scrollTo(0,document.body.scrollHeight);}" +
            "function appendMessage(role,html){var d=document.createElement('div');" +
            "d.className='msg '+role;d.innerHTML=\"<div class='role'>\"+role+\"</div>\"+html;" +
            "chat.appendChild(d);scroll();}" +
            "function appendError(text){var d=document.createElement('div');" +
            "d.className='msg error';d.innerHTML=text;chat.appendChild(d);scroll();}" +
            "function beginAssistant(){cur=document.createElement('div');" +
            "cur.className='msg assistant';cur.innerHTML=\"<div class='role'>assistant</div>" +
            "<div class='body'></div>\";chat.appendChild(cur);scroll();}" +
            "function updateAssistant(html){if(cur){cur.querySelector('.body').innerHTML=html;" +
            "scroll();}}" +
            "function endAssistant(){cur=null;}" +
            "function clearAll(){chat.innerHTML='';cur=null;}" +
            "</script></body></html>"
        );
    }
}
