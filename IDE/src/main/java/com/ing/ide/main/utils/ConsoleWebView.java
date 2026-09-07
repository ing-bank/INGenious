package com.ing.ide.main.utils;

import com.ing.engine.support.DesktopApi;
import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javax.swing.JComponent;

/**
 * Renders live-streamed execution logs using an embedded JavaFX {@link WebView}
 * hosted in a Swing {@link JFXPanel}, giving a consistent, modern look on both
 * Windows and Mac (unlike native look-and-feel text components).
 *
 * <p>Each log line is shown as a row with a timestamp and a colored status
 * "pill" derived from its leading {@code [TAG]} (PASS/FAIL/DONE/WARNING/DEBUG/
 * etc.), styled around the brand accent color #7724FF. Follows the app's
 * light/dark theme (never forces dark) and stays in sync when the user
 * toggles it at runtime. All WebView/DOM mutations are marshalled onto the
 * JavaFX application thread via {@link Platform#runLater}; callers may invoke
 * the public methods from any thread.</p>
 */
public class ConsoleWebView {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern(
        "HH:mm:ss.SSS"
    );
    private static final Pattern ANSI_ESCAPE = Pattern.compile("\u001B\\[[\\d;]*[A-Za-z]");
    // JavaFX's bundled WebView can't render color emoji reliably on either
    // platform, so emoji are stripped from all displayed text rather than shown
    // as tofu/mono glyphs. Covers the common blocks used across Engine logging
    // (pictographs, dingbats, misc symbols/technical, flags) plus variation
    // selectors and the zero-width joiner used to build compound emoji.
    private static final Pattern EMOJI = Pattern.compile(
        "[\\x{1F300}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{2300}-\\x{23FF}\\x{2B00}-\\x{2BFF}" +
        "\\x{1F1E6}-\\x{1F1FF}\\x{FE0E}\\x{FE0F}\\x{200D}\\x{203C}\\x{2049}]"
    );
    private static final Pattern TAG = Pattern.compile("^\\[([^\\[\\]]{1,24})\\]\\s*(.*)$");
    private static final Pattern DIVIDER = Pattern.compile("^[=\\-\u2500\u2550]{6,}$");
    private static final String SUMMARY_TAG = "[SUMMARY]";
    private static final String REPORT_PATH_TAG = "[REPORTPATH]";
    private static final String OPEN_REPORT_PREFIX = "OPEN_REPORT:";

    // Emoji-capable font fallbacks so emoji glyphs render as proper color icons
    // instead of tofu boxes inside the monospace/sans-serif font stacks below.
    private static final String EMOJI_FONTS =
        "'Apple Color Emoji','Segoe UI Emoji','Noto Color Emoji','Segoe UI Symbol'";
    private static final String MONO_STACK =
        "'Cascadia Mono','SF Mono','Consolas','Menlo'," + EMOJI_FONTS + ",monospace";
    private static final String EMOJI_TEXT_STACK =
        "'Segoe UI','SF Pro Text'," + EMOJI_FONTS + ",sans-serif";

    private static final List<ConsoleWebView> INSTANCES = new ArrayList<>();

    private final JFXPanel fxPanel = new JFXPanel();
    private final AtomicReference<WebEngine> engineRef = new AtomicReference<>();
    private volatile boolean ready;
    private final StringBuilder pendingScript = new StringBuilder();

    public ConsoleWebView() {
        synchronized (INSTANCES) {
            INSTANCES.add(this);
        }
        boolean dark = com.ing.ide.main.Main.isDarkMode();
        Platform.runLater(() -> initFx(dark));
    }

    /** Notifies every live console that the app-wide theme changed. */
    public static void applyThemeToAll(boolean dark) {
        List<ConsoleWebView> snapshot;
        synchronized (INSTANCES) {
            snapshot = new ArrayList<>(INSTANCES);
        }
        for (ConsoleWebView view : snapshot) {
            view.setDarkMode(dark);
        }
    }

    private void initFx(boolean dark) {
        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);
        WebEngine engine = webView.getEngine();
        // No netscape.javascript.JSObject bridge (not resolvable with this
        // javafx-web setup) - the report-path click instead calls
        // window.alert('OPEN_REPORT:'+path), intercepted here. This is the
        // standard JS->Java bridge technique that needs no extra module.
        engine.setOnAlert(
            event -> {
                String data = event.getData();
                if (data != null && data.startsWith(OPEN_REPORT_PREFIX)) {
                    DesktopApi.open(new File(data.substring(OPEN_REPORT_PREFIX.length())));
                }
            }
        );
        engine.loadContent(baseHtml(dark));
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

    /** Switches between light and dark styling without reloading the log content. */
    public void setDarkMode(boolean dark) {
        runScript("setDarkMode(" + dark + ");");
    }

    /** Appends a stdout line, auto-classified into a status pill from its leading [TAG]. */
    public void appendLine(String text) {
        String line = sanitize(text);
        if (line.startsWith(REPORT_PATH_TAG)) {
            runScript(
                "appendReportPath(" + jsString(line.substring(REPORT_PATH_TAG.length())) + ");"
            );
            return;
        }
        if (line.startsWith(SUMMARY_TAG)) {
            runScript("renderSummary(" + jsString(line.substring(SUMMARY_TAG.length())) + ");");
            return;
        }
        if (line.trim().isEmpty()) {
            runScript("appendSpacer();");
            return;
        }
        if (DIVIDER.matcher(line.trim()).matches()) {
            runScript("appendDivider();");
            return;
        }
        String[] tag = classify(line, false);
        appendRow(line, tag[0], tag[1]);
    }

    /** Appends a stderr line; falls back to the ERROR pill when it has no status [TAG]. */
    public void appendErrorLine(String text) {
        String line = sanitize(text);
        String[] tag = classify(line, true);
        appendRow(line, tag[0], tag[1]);
    }

    /** Removes all rows from the console. */
    public void clear() {
        runScript("clearAll();");
    }

    private void appendRow(String text, String label, String cssClass) {
        String line = text == null ? "null" : text;
        String time = LocalTime.now().format(TIME_FORMAT);
        runScript(
            "appendRow(" +
            jsString(time) +
            "," +
            jsString(label) +
            "," +
            jsString(cssClass) +
            "," +
            jsString(line) +
            ");"
        );
    }

    private static String stripAnsi(String text) {
        if (text == null) {
            return "";
        }
        return ANSI_ESCAPE.matcher(text).replaceAll("");
    }

    /** Strips ANSI escapes and emoji, collapsing any whitespace gap they leave behind. */
    private static String sanitize(String text) {
        String noAnsi = stripAnsi(text);
        String noEmoji = EMOJI.matcher(noAnsi).replaceAll("");
        return noEmoji.equals(noAnsi) ? noAnsi : noEmoji.replaceAll("[ \\t]{2,}", " ");
    }

    /**
     * Classifies a line into a {@code {label, cssClass}} pair. The label is the
     * literal status tag as authored (e.g. "DONE", "WARNING") so any current or
     * future tag is shown verbatim rather than being collapsed into a generic
     * "LOG" pill; the cssClass buckets it into a color family by keyword.
     */
    private static String[] classify(String text, boolean isErrStream) {
        String line = text == null ? "" : text.trim();
        Matcher m = TAG.matcher(line);
        if (m.matches()) {
            String label = m.group(1).trim().toUpperCase(Locale.ROOT);
            return new String[] { label, bucketFor(label) };
        }
        return isErrStream ? new String[] { "ERROR", "fail" } : new String[] { "LOG", "log" };
    }

    private static String bucketFor(String label) {
        String l = label.toLowerCase(Locale.ROOT);
        if (l.contains("pass") || l.contains("success")) {
            return "pass";
        } else if (l.contains("done") || l.contains("complete")) {
            return "done";
        } else if (l.contains("fail") || l.contains("error") || l.contains("fatal")) {
            return "fail";
        } else if (l.contains("warn")) {
            return "warn";
        } else if (l.contains("debug") || l.contains("trace")) {
            return "debug";
        } else if (
            l.contains("skip") ||
            l.contains("info") ||
            l.contains("pending") ||
            l.contains("running") ||
            l.contains("start")
        ) {
            return "info";
        }
        return "log";
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

    private String baseHtml(boolean dark) {
        return (
            "<!DOCTYPE html><html><head><meta charset='utf-8'>" +
            "<style>" +
            "*{box-sizing:border-box;}" +
            "html,body{height:100%;}" +
            ":root{" +
            "--bg:#fbfaff;--fg:#241c33;--muted:#7a7188;--panel:#f1ecfb;--border:#e1d6f7;" +
            "--accent:#7724FF;--accent2:#b487ff;--row-hover:rgba(119,36,255,0.07);" +
            "--pass:#1f9d55;--pass-bg:rgba(31,157,85,0.10);--pass-bd:rgba(31,157,85,0.35);" +
            "--done:#0e8f8f;--done-bg:rgba(14,143,143,0.10);--done-bd:rgba(14,143,143,0.35);" +
            "--fail:#d1304a;--fail-bg:rgba(209,48,74,0.10);--fail-bd:rgba(209,48,74,0.35);" +
            "--warn:#a6650a;--warn-bg:rgba(166,101,10,0.12);--warn-bd:rgba(166,101,10,0.35);" +
            "--debug:#6b6b76;--debug-bg:rgba(107,107,118,0.10);--debug-bd:rgba(107,107,118,0.3);" +
            "--info:#1f6fd6;--info-bg:rgba(31,111,214,0.10);--info-bd:rgba(31,111,214,0.3);" +
            "--log:#7724FF;--log-bg:rgba(119,36,255,0.10);--log-bd:rgba(119,36,255,0.35);" +
            "--scrollbar-track:#f1ecfb;}" +
            "body.dark{" +
            "--bg:#15121e;--fg:#e4e0ee;--muted:#8b83a6;--panel:#1c1728;--border:#2c2440;" +
            "--accent:#7724FF;--accent2:#b487ff;--row-hover:rgba(119,36,255,0.10);" +
            "--pass:#33ff9c;--pass-bg:rgba(51,255,156,0.15);--pass-bd:rgba(51,255,156,0.35);" +
            "--done:#41e0c2;--done-bg:rgba(65,224,194,0.15);--done-bd:rgba(65,224,194,0.35);" +
            "--fail:#ff4d6d;--fail-bg:rgba(255,77,109,0.15);--fail-bd:rgba(255,77,109,0.35);" +
            "--warn:#ffb74d;--warn-bg:rgba(255,183,77,0.15);--warn-bd:rgba(255,183,77,0.35);" +
            "--debug:#b0a8cc;--debug-bg:rgba(139,131,166,0.15);--debug-bd:rgba(139,131,166,0.35);" +
            "--info:#6fb3ff;--info-bg:rgba(111,179,255,0.15);--info-bd:rgba(111,179,255,0.35);" +
            "--log:#b487ff;--log-bg:rgba(119,36,255,0.15);--log-bd:rgba(119,36,255,0.4);" +
            "--scrollbar-track:#15121e;}" +
            "body{margin:0;padding:0;background:var(--bg);color:var(--fg);" +
            "font-family:" +
            EMOJI_TEXT_STACK +
            ";" +
            "font-size:12px;overflow-x:hidden;transition:background .15s ease,color .15s ease;}" +
            "#topbar{position:sticky;top:0;display:flex;align-items:center;gap:8px;" +
            "padding:8px 12px;background:var(--panel);border-bottom:1px solid var(--border);" +
            "background-image:linear-gradient(90deg,var(--accent),var(--accent2) 45%,var(--accent));" +
            "background-size:200% 100%;animation:sheen 6s linear infinite;z-index:2;}" +
            "@keyframes sheen{0%{background-position:0% 0;}100%{background-position:200% 0;}}" +
            "#topbar .title{font-weight:600;letter-spacing:.06em;color:#ffffff;" +
            "text-transform:uppercase;font-size:11px;}" +
            "#topbar .live{display:flex;align-items:center;gap:5px;margin-left:auto;" +
            "font-size:10px;color:#ffffff;opacity:.9;text-transform:uppercase;letter-spacing:.05em;}" +
            "#topbar .live .dot{width:7px;height:7px;border-radius:50%;background:#33ff9c;" +
            "box-shadow:0 0 6px #33ff9c;animation:pulse 1.4s ease-in-out infinite;}" +
            "@keyframes pulse{0%,100%{opacity:1;transform:scale(1);}" +
            "50%{opacity:.4;transform:scale(.75);}}" +
            "#log{padding:4px 0 12px 0;}" +
            ".row{display:flex;align-items:flex-start;gap:8px;padding:3px 12px;" +
            "border-left:2px solid transparent;}" +
            ".row:hover{background:var(--row-hover);border-left-color:var(--accent);}" +
            ".row .ts{flex:0 0 auto;color:var(--muted);font-family:" +
            MONO_STACK +
            ";" +
            "font-size:11px;padding-top:1px;user-select:none;}" +
            ".pill{flex:0 0 auto;display:inline-block;min-width:44px;text-align:center;" +
            "padding:1px 8px;border-radius:999px;font-size:10px;font-weight:700;" +
            "letter-spacing:.04em;text-transform:uppercase;}" +
            ".pill.pass{background:var(--pass-bg);color:var(--pass);border:1px solid var(--pass-bd);}" +
            ".pill.done{background:var(--done-bg);color:var(--done);border:1px solid var(--done-bd);}" +
            ".pill.fail{background:var(--fail-bg);color:var(--fail);border:1px solid var(--fail-bd);}" +
            ".pill.warn{background:var(--warn-bg);color:var(--warn);border:1px solid var(--warn-bd);}" +
            ".pill.debug{background:var(--debug-bg);color:var(--debug);" +
            "border:1px solid var(--debug-bd);}" +
            ".pill.info{background:var(--info-bg);color:var(--info);border:1px solid var(--info-bd);}" +
            ".pill.log{background:var(--log-bg);color:var(--log);border:1px solid var(--log-bd);}" +
            ".row .msg{flex:1 1 auto;white-space:pre-wrap;word-break:break-word;" +
            "font-family:" +
            MONO_STACK +
            ";" +
            "font-size:12px;line-height:1.5;color:var(--fg);padding-top:1px;}" +
            ".row.fail .msg{color:var(--fail);}" +
            ".spacer{height:6px;}" +
            ".divider{border:none;border-top:1px solid var(--border);margin:8px 12px;}" +
            ".summary{margin:12px;border-radius:12px;overflow:hidden;" +
            "border:1px solid var(--border);background:var(--panel);}" +
            ".summary.ok{border-color:var(--pass-bd);}" +
            ".summary.bad{border-color:var(--fail-bd);}" +
            ".summary-head{display:flex;align-items:center;gap:8px;padding:10px 14px;" +
            "background:linear-gradient(90deg,var(--accent),var(--accent2));}" +
            ".summary-title{flex:1;color:#fff;font-weight:700;letter-spacing:.05em;" +
            "text-transform:uppercase;font-size:12px;}" +
            ".summary-badge{padding:2px 10px;border-radius:999px;font-size:10px;font-weight:800;" +
            "letter-spacing:.05em;color:#fff;}" +
            ".summary-badge.pass{background:#1f9d55;}" +
            ".summary-badge.fail{background:#d1304a;}" +
            ".summary-grid{display:grid;grid-template-columns:repeat(4,1fr);" +
            "gap:1px;background:var(--border);}" +
            ".stat{background:var(--panel);padding:12px 6px;text-align:center;}" +
            ".stat-value{font-size:18px;font-weight:800;font-family:" +
            MONO_STACK +
            ";}" +
            ".stat-label{margin-top:2px;font-size:10px;color:var(--muted);" +
            "text-transform:uppercase;letter-spacing:.05em;}" +
            ".stat.pass .stat-value{color:var(--pass);}" +
            ".stat.fail .stat-value{color:var(--fail);}" +
            ".stat.info .stat-value{color:var(--info);}" +
            ".stat.log .stat-value{color:var(--log);}" +
            ".report-path{display:flex;align-items:center;gap:10px;margin:10px 12px;" +
            "padding:9px 14px;border-radius:8px;background:var(--log-bg);" +
            "border:1px solid var(--log-bd);cursor:pointer;}" +
            ".report-path:hover{background:var(--row-hover);border-color:var(--accent);}" +
            ".report-path .rp-link{flex:1;color:var(--accent);text-decoration:underline;" +
            "word-break:break-all;font-family:" +
            MONO_STACK +
            ";font-size:12px;}" +
            ".report-path .rp-open{flex:0 0 auto;font-size:10px;font-weight:700;" +
            "letter-spacing:.05em;text-transform:uppercase;color:var(--muted);}" +
            "::-webkit-scrollbar{width:10px;height:10px;}" +
            "::-webkit-scrollbar-track{background:var(--scrollbar-track);}" +
            "::-webkit-scrollbar-thumb{background:var(--accent);border-radius:6px;}" +
            "::-webkit-scrollbar-thumb:hover{background:var(--accent2);}" +
            "#empty{padding:24px 12px;color:var(--muted);font-style:italic;}" +
            "</style></head><body class='" +
            (dark ? "dark" : "") +
            "'>" +
            "<div id='topbar'><span class='title'>Console</span>" +
            "<span class='live'><span class='dot'></span>Live</span></div>" +
            "<div id='log'><div id='empty'>Waiting for output…</div></div>" +
            "<script>" +
            "var log=document.getElementById('log');var empty=document.getElementById('empty');" +
            "function setDarkMode(dark){document.body.classList.toggle('dark',dark);}" +
            "function nearBottom(){return (window.innerHeight+window.scrollY)>=" +
            "(document.body.scrollHeight-48);}" +
            "function esc(s){var d=document.createElement('div');d.innerText=s;" +
            "return d.innerHTML;}" +
            "function dropEmpty(){if(empty){empty.remove();empty=null;}}" +
            "function appendRow(ts,label,cssClass,text){" +
            "dropEmpty();var stick=nearBottom();" +
            "var r=document.createElement('div');r.className='row '+cssClass;" +
            "r.innerHTML=\"<span class='ts'>\"+ts+\"</span>\"+" +
            "\"<span class='pill \"+cssClass+\"'>\"+esc(label)+\"</span>\"+" +
            "\"<span class='msg'></span>\";" +
            "r.querySelector('.msg').innerHTML=esc(text);" +
            "log.appendChild(r);" +
            "if(stick){window.scrollTo(0,document.body.scrollHeight);}}" +
            "function appendSpacer(){dropEmpty();var stick=nearBottom();" +
            "var d=document.createElement('div');d.className='spacer';log.appendChild(d);" +
            "if(stick){window.scrollTo(0,document.body.scrollHeight);}}" +
            "function appendDivider(){dropEmpty();var stick=nearBottom();" +
            "var d=document.createElement('hr');d.className='divider';log.appendChild(d);" +
            "if(stick){window.scrollTo(0,document.body.scrollHeight);}}" +
            "function appendReportPath(path){dropEmpty();var stick=nearBottom();" +
            "var box=document.createElement('div');box.className='report-path';" +
            "box.title='Click to open';" +
            "box.innerHTML=\"<span class='pill log'>REPORT</span>\"+" +
            "\"<span class='rp-link'></span><span class='rp-open'>Open</span>\";" +
            "box.querySelector('.rp-link').textContent=path;" +
            "box.onclick=function(){alert('" +
            OPEN_REPORT_PREFIX +
            "'+path);};" +
            "log.appendChild(box);" +
            "if(stick){window.scrollTo(0,document.body.scrollHeight);}}" +
            "function summaryStat(label,value,kind){var d=document.createElement('div');" +
            "d.className='stat '+kind;" +
            "d.innerHTML=\"<div class='stat-value'></div><div class='stat-label'></div>\";" +
            "d.querySelector('.stat-value').textContent=value;" +
            "d.querySelector('.stat-label').textContent=label;return d;}" +
            "function renderSummary(json){dropEmpty();var stick=nearBottom();var data;" +
            "try{data=JSON.parse(json);}catch(e){return;}" +
            "var passed=data.status==='PASSED';" +
            "var card=document.createElement('div');" +
            "card.className='summary '+(passed?'ok':'bad');" +
            "var head=document.createElement('div');head.className='summary-head';" +
            "head.innerHTML=\"<span class='summary-title'>Execution Summary</span>\"+" +
            "\"<span class='summary-badge \"+(passed?'pass':'fail')+\"'>\"+data.status+\"</span>\";" +
            "card.appendChild(head);" +
            "var grid=document.createElement('div');grid.className='summary-grid';" +
            "grid.appendChild(summaryStat('Total',data.total,'info'));" +
            "grid.appendChild(summaryStat('Passed',data.passed,'pass'));" +
            "grid.appendChild(summaryStat('Failed',data.failed,'fail'));" +
            "grid.appendChild(summaryStat('Duration',data.duration,'log'));" +
            "card.appendChild(grid);log.appendChild(card);" +
            "if(stick){window.scrollTo(0,document.body.scrollHeight);}}" +
            "function clearAll(){log.innerHTML='';" +
            "var d=document.createElement('div');d.id='empty';" +
            "d.textContent='Waiting for output\\u2026';" +
            "log.appendChild(d);empty=d;}" +
            "</script></body></html>"
        );
    }
}
