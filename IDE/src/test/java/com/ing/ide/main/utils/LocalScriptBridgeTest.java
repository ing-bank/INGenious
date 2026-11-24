package com.ing.ide.main.utils;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LocalScriptBridgeTest {
    private LocalScriptBridge bridge;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeMethod
    public void start() throws Exception {
        bridge = new LocalScriptBridge("<html><body><div id='x'></div></body></html>");
    }

    @AfterMethod(alwaysRun = true)
    public void stop() {
        bridge.close();
    }

    private HttpResponse<String> get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String poll(int from) throws Exception {
        return get(bridge.getUrl() + "poll?from=" + from).body();
    }

    @Test
    public void servesThePageWithThePollerBeforeTheClosingBodyTag() throws Exception {
        HttpResponse<String> page = get(bridge.getUrl());
        assertEquals(page.statusCode(), 200);
        String html = page.body();
        assertTrue(html.contains("<div id='x'></div>"));
        assertTrue(html.contains("XMLHttpRequest"));
        assertTrue(html.indexOf("XMLHttpRequest") < html.indexOf("</body>"));
    }

    @Test
    public void listensOnLoopbackUnderAnUnguessablePath() {
        String url = bridge.getUrl();
        assertTrue(url.startsWith("http://127.0.0.1:"));
        String token = url.substring(url.indexOf('/', "http://".length()) + 1, url.length() - 1);
        assertTrue(token.length() >= 32);
    }

    @Test
    public void otherPathsAreNotFound() throws Exception {
        String root = bridge.getUrl().substring(0, bridge.getUrl().indexOf('/', 8));
        assertEquals(get(root + "/").statusCode(), 404);
        assertEquals(get(root + "/poll?from=0").statusCode(), 404);
        assertEquals(get(bridge.getUrl() + "other").statusCode(), 404);
    }

    @Test
    public void pollReturnsQueuedScriptsInOrderWithTheNextIndex() throws Exception {
        assertEquals(poll(0), "0\n");
        bridge.post("a();");
        bridge.post("b();");
        assertEquals(poll(0), "2\na();\nb();\n");
        assertEquals(poll(2), "2\n");
        bridge.post("c();");
        assertEquals(poll(2), "3\nc();\n");
    }

    @Test
    public void pendingStreamingUpdatesAreCoalescedIntoTheLatest() throws Exception {
        bridge.post("beginAssistant();");
        bridge.post("updateAssistant('Hel');");
        bridge.post("updateAssistant('Hello');");
        bridge.post("updateAssistant('Hello world');");
        assertEquals(poll(0), "2\nbeginAssistant();\nupdateAssistant('Hello world');\n");
    }

    @Test
    public void anUpdateAlreadySentIsNeverOverwritten() throws Exception {
        bridge.post("updateAssistant('one');");
        assertEquals(poll(0), "1\nupdateAssistant('one');\n");
        bridge.post("updateAssistant('one two');");
        assertEquals(poll(1), "2\nupdateAssistant('one two');\n");
    }

    @Test
    public void scriptsAcknowledgedByThePageAreDropped() throws Exception {
        bridge.post("a();");
        bridge.post("b();");
        assertEquals(poll(0), "2\na();\nb();\n");
        assertEquals(poll(2), "2\n");
        assertEquals(poll(0), "2\n");
    }

    @Test
    public void nonAsciiAndQuotesSurviveTheRoundTrip() throws Exception {
        bridge.post("add('caf\u00e9 \u2713 \"q\" \\\\ x');");
        assertEquals(poll(0), "1\nadd('caf\u00e9 \u2713 \"q\" \\\\ x');\n");
    }
}
