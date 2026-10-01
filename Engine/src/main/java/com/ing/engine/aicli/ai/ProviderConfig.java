package com.ing.engine.aicli.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

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
    /** "attended" or "unattended"; see {@link OperatingMode}. Remembered as the next session's default. */
    public String mode = OperatingMode.UNATTENDED.label();

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
                c.mode = n.path("mode").asText(c.mode);
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
        n.put("mode", mode);
        Files.createDirectories(file.getParent());
        Files.writeString(file, n.toPrettyString());
    }

    /** Typed accessor over {@link #mode}. */
    public OperatingMode operatingMode() {
        return OperatingMode.fromString(mode);
    }

    /** Build the configured provider; never returns null (copilot is the default). */
    public AiProvider createProvider(TokenStore store) {
        return createProvider(store, () -> null);
    }

    /**
     * Build the configured provider. {@code projectDir} supplies the active
     * project directory for providers that need it (e.g. the Copilot SDK provider,
     * which launches the INGenious MCP server scoped to that project).
     */
    public AiProvider createProvider(TokenStore store, Supplier<String> projectDir) {
        if ("copilot-sdk".equalsIgnoreCase(provider)) {
            // Drives the Copilot CLI via the SDK; the INGenious MCP server is
            // exposed to the CLI so the model can call the ingenious_* tools.
            return new CopilotSdkProvider(model, projectDir);
        }
        if ("openai".equalsIgnoreCase(provider)) {
            String key = System.getenv(apiKeyEnv);
            return new OpenAiCompatProvider(baseUrl, key, model);
        }
        return new CopilotProvider(store, model);
    }
}
