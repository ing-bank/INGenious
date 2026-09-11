package com.ing.ide.main.mainui.components.aichat.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.ide.main.mainui.components.aichat.model.ChatCompletionRequest;
import com.ing.ide.main.mainui.components.aichat.model.ChatCompletionResponse;
import com.ing.ide.main.mainui.components.aichat.model.ModelInfo;
import com.ing.ide.main.mainui.components.aichat.model.TokenUsage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thin client for the official, public GitHub Models API
 * ({@code https://models.github.ai}). It exposes the model catalog and
 * OpenAI-compatible chat completions with Server-Sent-Events streaming.
 *
 * <p>This is the documented GitHub <em>Models</em> API and is intentionally not
 * the proprietary GitHub Copilot backend.</p>
 */
public class GitHubModelsClient {
    private static final Logger LOG = Logger.getLogger(GitHubModelsClient.class.getName());

    private static final String CATALOG_URL = "https://models.github.ai/catalog/models";
    private static final String CHAT_URL = "https://models.github.ai/inference/chat/completions";

    // Endpoints are instance fields so the client can be pointed at a local
    // VS Code Copilot bridge (OpenAI-compatible) instead of the GitHub Models API.
    private String catalogUrl = CATALOG_URL;
    private String chatUrl = CHAT_URL;

    /**
     * Route this client at a local OpenAI-compatible bridge (e.g. the VS Code
     * Copilot LLM Bridge). {@code baseUrl} is like {@code http://127.0.0.1:8765/v1}.
     * When in bridge mode the access token is optional.
     */
    public void useLocalBridge(String baseUrl) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.chatUrl = base + "/chat/completions";
        this.catalogUrl = base + "/models";
    }

    /** True when a bearer token should be sent (real GitHub Models, not the bridge). */
    private static boolean hasToken(String token) {
        return token != null && !token.isBlank();
    }

    private final ObjectMapper mapper = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final HttpClient httpClient = HttpClient
        .newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    /** Callbacks for an in-progress streaming chat completion. */
    public interface StreamListener {
        /** Called for each incremental token/text fragment. */
        void onToken(String text);

        /** Called once with final usage if the API reports it. */
        default void onUsage(TokenUsage usage) {}

        /** Called once with rate-limit headers (raw values, may be null). */
        default void onRateLimit(String remaining, String limit, String reset) {}

        /** Called when the stream completes successfully. */
        void onComplete();

        /** Called if the request fails. */
        void onError(Throwable error);
    }

    /** Raised for non-2xx API responses, carrying the HTTP status. */
    public static class ApiException extends IOException {
        private final int statusCode;

        public ApiException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

    /**
     * Fetches the available model catalog.
     *
     * @param token GitHub access token with the {@code models} scope
     */
    public List<ModelInfo> catalog(String token) throws IOException, InterruptedException {
        HttpRequest.Builder catalogBuilder = HttpRequest
            .newBuilder()
            .uri(URI.create(catalogUrl))
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(30))
            .GET();
        if (hasToken(token)) {
            catalogBuilder.header("Authorization", "Bearer " + token);
        }
        HttpRequest request = catalogBuilder.build();
        HttpResponse<String> response = httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() / 100 != 2) {
            throw new ApiException(
                response.statusCode(),
                "Failed to load model catalog: HTTP " + response.statusCode()
            );
        }
        List<ModelInfo> models = new ArrayList<>();
        JsonNode root = mapper.readTree(response.body());
        // The catalog may be returned as a bare array or under a "models"/"data" key.
        JsonNode array = root.isArray()
            ? root
            : root.has("models") ? root.get("models") : root.has("data") ? root.get("data") : null;
        if (array != null && array.isArray()) {
            for (JsonNode node : array) {
                models.add(mapper.treeToValue(node, ModelInfo.class));
            }
        }
        return models;
    }

    /**
     * Performs a non-streaming chat completion and returns the full response.
     */
    public ChatCompletionResponse complete(String token, ChatCompletionRequest request)
        throws IOException, InterruptedException {
        request.setStream(false);
        HttpRequest.Builder completeBuilder = HttpRequest
            .newBuilder()
            .uri(URI.create(chatUrl))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(120))
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(request)));
        if (hasToken(token)) {
            completeBuilder.header("Authorization", "Bearer " + token);
        }
        HttpRequest httpRequest = completeBuilder.build();
        HttpResponse<String> response = httpClient.send(
            httpRequest,
            HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() / 100 != 2) {
            throw new ApiException(
                response.statusCode(),
                describeError(response.statusCode(), response.body())
            );
        }
        return mapper.readValue(response.body(), ChatCompletionResponse.class);
    }

    /**
     * Performs a streaming chat completion, invoking the listener as tokens
     * arrive. Blocks until the stream finishes; intended to run on a background
     * thread. Errors are reported via {@link StreamListener#onError}.
     */
    public void streamComplete(
        String token,
        ChatCompletionRequest request,
        StreamListener listener
    ) {
        request.setStream(true);
        request.requestStreamingUsage();
        try {
            HttpRequest.Builder streamBuilder = HttpRequest
                .newBuilder()
                .uri(URI.create(chatUrl))
                .header("Accept", "text/event-stream")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(5))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(request)));
            if (hasToken(token)) {
                streamBuilder.header("Authorization", "Bearer " + token);
            }
            HttpRequest httpRequest = streamBuilder.build();

            HttpResponse<InputStream> response = httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofInputStream()
            );

            reportRateLimit(response, listener);

            if (response.statusCode() / 100 != 2) {
                String body = readAll(response.body());
                listener.onError(
                    new ApiException(
                        response.statusCode(),
                        describeError(response.statusCode(), body)
                    )
                );
                return;
            }

            parseSse(response.body(), listener);
            listener.onComplete();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            listener.onError(ex);
        } catch (Exception ex) {
            listener.onError(ex);
        }
    }

    private void parseSse(InputStream body, StreamListener listener) throws IOException {
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(body, StandardCharsets.UTF_8)
            )
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || !line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring("data:".length()).trim();
                if ("[DONE]".equals(data)) {
                    break;
                }
                try {
                    JsonNode chunk = mapper.readTree(data);
                    JsonNode choices = chunk.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        JsonNode content = choices.get(0).path("delta").path("content");
                        if (content.isTextual()) {
                            listener.onToken(content.asText());
                        }
                    }
                    JsonNode usage = chunk.get("usage");
                    if (usage != null && !usage.isNull()) {
                        listener.onUsage(mapper.treeToValue(usage, TokenUsage.class));
                    }
                } catch (IOException ex) {
                    LOG.log(Level.FINE, "Skipping unparseable SSE chunk", ex);
                }
            }
        }
    }

    private void reportRateLimit(HttpResponse<?> response, StreamListener listener) {
        Map<String, List<String>> headers = response.headers().map();
        listener.onRateLimit(
            firstHeader(headers, "x-ratelimit-remaining"),
            firstHeader(headers, "x-ratelimit-limit"),
            firstHeader(headers, "x-ratelimit-reset")
        );
    }

    private String firstHeader(Map<String, List<String>> headers, String name) {
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name) && !e.getValue().isEmpty()) {
                return e.getValue().get(0);
            }
        }
        return null;
    }

    private String describeError(int status, String body) {
        switch (status) {
            case 401:
                return "Unauthorized (401): your GitHub token is invalid or expired. Please sign in again.";
            case 429:
                return "Rate limited (429): too many requests. Please wait and try again.";
            default:
                return "Request failed (HTTP " + status + "): " + body;
        }
    }

    private String readAll(InputStream in) {
        try {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }
}
