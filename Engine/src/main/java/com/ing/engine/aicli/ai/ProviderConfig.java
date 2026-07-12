package com.ing.engine.aicli.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Global AI provider configuration at {@code ~/.ingenious/ai.json}:
 * {@code {"provider": "copilot"|"openai", "model": "...", "baseUrl": "...",
 * "apiKeyEnv": "OPENAI_API_KEY"}}. Switched at runtime with {@code /model}.
 */
public final class ProviderConfig {
    private static final ObjectMapper M = new ObjectMapper();

    private final Path file;
    public String provider = "copilot";
    public String model = "gpt-4o";
    public String baseUrl = "https://api.openai.com/v1";
    public String apiKeyEnv = "OPENAI_API_KEY";

    private ProviderConfig(Path file) {
        this.file = file;
    }

    public static ProviderConfig load() {
        return load(Path.of(System.getProperty("user.home"), ".ingenious", "ai.json"));
    }

    public static ProviderConfig load(Path file) {
        ProviderConfig c = new ProviderConfig(file);
        try {
            if (Files.exists(file)) {
                JsonNode n = M.readTree(file.toFile());
                c.provider = n.path("provider").asText(c.provider);
                c.model = n.path("model").asText(c.model);
                c.baseUrl = n.path("baseUrl").asText(c.baseUrl);
                c.apiKeyEnv = n.path("apiKeyEnv").asText(c.apiKeyEnv);
            }
        } catch (IOException ignored) {
            // defaults apply
        }
        return c;
    }

    public void save() throws IOException {
        ObjectNode n = M.createObjectNode();
        n.put("provider", provider);
        n.put("model", model);
        n.put("baseUrl", baseUrl);
        n.put("apiKeyEnv", apiKeyEnv);
        Files.createDirectories(file.getParent());
        Files.writeString(file, n.toPrettyString());
    }

    /** Build the configured provider; never returns null (copilot is the default). */
    public AiProvider createProvider(TokenStore store) {
        if ("bridge".equalsIgnoreCase(provider)) {
            // VS Code Copilot LLM Bridge: OpenAI-compatible, no key, no GitHub auth.
            String url = discoverBridgeBaseUrl();
            if (url == null || url.isBlank()) {
                url = baseUrl; // fall back to the configured baseUrl
            }
            return new OpenAiCompatProvider(url, null, model);
        }
        if ("openai".equalsIgnoreCase(provider)) {
            String key = System.getenv(apiKeyEnv);
            return new OpenAiCompatProvider(baseUrl, key, model);
        }
        return new CopilotProvider(store, model);
    }

    /**
     * Locate a running VS Code Copilot bridge. Honours {@code INGENIOUS_AI_BASE_URL},
     * then the discovery file {@code ~/.ingenious/bridge.json} written by the
     * bridge extension. Returns {@code null} when none is available.
     */
    public static String discoverBridgeBaseUrl() {
        String override = System.getenv("INGENIOUS_AI_BASE_URL");
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        try {
            Path file = Path.of(System.getProperty("user.home"), ".ingenious", "bridge.json");
            if (!Files.exists(file)) {
                return null;
            }
            JsonNode n = M.readTree(file.toFile());
            String url = n.path("baseUrl").asText(null);
            if (url != null && !url.isBlank()) {
                return url.trim();
            }
            int port = n.path("port").asInt(0);
            if (port > 0) {
                String host = n.path("host").asText("127.0.0.1");
                return "http://" + host + ":" + port + "/v1";
            }
        } catch (IOException ignored) {
            // no usable bridge
        }
        return null;
    }
}
