package com.ing.ide.main.mainui.components.aichat.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Detects a locally running VS Code Copilot LLM Bridge so the AI assistant can
 * use VS Code's GitHub Copilot session instead of a direct GitHub token.
 *
 * <p>The bridge extension writes {@code ~/.ingenious/bridge.json} while it is
 * running (and removes it on stop). This class also honours the
 * {@code INGENIOUS_AI_BASE_URL} environment variable as an explicit override.</p>
 */
public final class BridgeDiscovery {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BridgeDiscovery() {}

    /**
     * @return the OpenAI-compatible base URL of a running bridge (e.g.
     *     {@code http://127.0.0.1:8765/v1}), or {@code null} if none is available.
     */
    public static String detect() {
        String override = System.getenv("INGENIOUS_AI_BASE_URL");
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        try {
            Path file = Paths.get(System.getProperty("user.home"), ".ingenious", "bridge.json");
            File f = file.toFile();
            if (!f.isFile()) {
                return null;
            }
            JsonNode n = MAPPER.readTree(f);
            String baseUrl = n.path("baseUrl").asText(null);
            if (baseUrl != null && !baseUrl.isBlank()) {
                return baseUrl.trim();
            }
            int port = n.path("port").asInt(0);
            if (port > 0) {
                String host = n.path("host").asText("127.0.0.1");
                return "http://" + host + ":" + port + "/v1";
            }
        } catch (Exception ignored) {
            // No usable bridge; fall back to GitHub auth.
        }
        return null;
    }
}
