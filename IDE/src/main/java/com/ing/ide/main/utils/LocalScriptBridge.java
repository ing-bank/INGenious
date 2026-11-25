package com.ing.ide.main.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Feeds JavaScript to a WebView page without {@code WebEngine.executeScript}, which crashes the
 * JVM on JDK 26+ (see {@link WebViewCompat}). The page is served from an in-process HTTP server
 * bound to the loopback interface only, under an unguessable token path, and polls it for queued
 * scripts which it evaluates itself. Pending scripts are queued until the page asks for them.
 */
public final class LocalScriptBridge {
    private static final String UPDATE_PREFIX = "updateAssistant(";
    private static final int POLL_INTERVAL_MS = 100;
    private static final String POLLER =
        "<script>(function(){var from=0;" +
        "function poll(){var x=new XMLHttpRequest();x.open('GET','poll?from='+from,true);" +
        "x.onload=function(){var t=x.responseText;var nl=t.indexOf('\\n');" +
        "var next=parseInt(t.substring(0,nl),10);var s=t.substring(nl+1);" +
        "if(next>from){from=next;try{window.eval(s);}catch(e){}}" +
        "setTimeout(poll," +
        POLL_INTERVAL_MS +
        ");};" +
        "x.onerror=function(){setTimeout(poll,500);};x.send();}poll();})();</script>";

    private final HttpServer server;
    private final String basePath;
    private final String pageHtml;
    private final Object lock = new Object();
    private final List<String> queue = new ArrayList<>();
    /** Index of {@code queue.get(0)}; entries below it were acknowledged by the page. */
    private int base;
    /** Entries below this index have already been sent to the page. */
    private int delivered;

    /** Starts the server; {@code html} is the page to serve, with the polling script added. */
    public LocalScriptBridge(String html) throws IOException {
        basePath = "/" + UUID.randomUUID();
        int end = html.lastIndexOf("</body>");
        pageHtml = end < 0 ? html + POLLER : html.substring(0, end) + POLLER + html.substring(end);
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext(basePath + "/", this::handle);
        server.setExecutor(
            Executors.newSingleThreadExecutor(
                runnable -> {
                    Thread thread = new Thread(runnable, "script-bridge");
                    thread.setDaemon(true);
                    return thread;
                }
            )
        );
        server.start();
    }

    /** URL of the page; load this in the WebView. */
    public String getUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + basePath + "/";
    }

    /**
     * Queues a script for the page. A pending (not yet sent) {@code updateAssistant(...)} is
     * replaced by a newer one, since each carries the full text so far.
     */
    public void post(String script) {
        if (script == null || script.isEmpty()) {
            return;
        }
        synchronized (lock) {
            int last = queue.size() - 1;
            boolean lastPending = last >= 0 && base + last >= delivered;
            if (
                lastPending &&
                script.startsWith(UPDATE_PREFIX) &&
                queue.get(last).startsWith(UPDATE_PREFIX)
            ) {
                queue.set(last, script);
            } else {
                queue.add(script);
            }
        }
    }

    public void close() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (!"GET".equals(exchange.getRequestMethod())) {
                respond(exchange, 405, "text/plain; charset=utf-8", "");
            } else if (path.equals(basePath + "/")) {
                respond(exchange, 200, "text/html; charset=utf-8", pageHtml);
            } else if (path.equals(basePath + "/poll")) {
                int from = parseFrom(exchange.getRequestURI().getRawQuery());
                respond(exchange, 200, "text/plain; charset=utf-8", poll(from));
            } else {
                respond(exchange, 404, "text/plain; charset=utf-8", "");
            }
        } finally {
            exchange.close();
        }
    }

    /** Returns "next index", a newline, then every script from {@code from} onwards. */
    private String poll(int from) {
        synchronized (lock) {
            int end = base + queue.size();
            int start = Math.max(Math.min(from, end), base);
            int drop = start - base;
            if (drop > 0) {
                queue.subList(0, drop).clear();
                base = start;
            }
            delivered = end;
            StringBuilder out = new StringBuilder().append(end).append('\n');
            for (String script : queue) {
                out.append(script).append('\n');
            }
            return out.toString();
        }
    }

    private static int parseFrom(String query) {
        if (query == null) {
            return 0;
        }
        for (String pair : query.split("&")) {
            if (pair.startsWith("from=")) {
                try {
                    return Math.max(0, Integer.parseInt(pair.substring("from=".length())));
                } catch (NumberFormatException ex) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private static void respond(HttpExchange exchange, int status, String type, String body)
        throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", type);
        exchange.getResponseHeaders().add("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
    }
}
