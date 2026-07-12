package com.ing.engine.aicli.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * GitHub Copilot provider. Authenticates with the standard OAuth device flow
 * (same public client id used by the open-source Copilot editor plugins),
 * exchanges the GitHub token for a short-lived Copilot session token, and
 * calls the Copilot chat-completions API.
 *
 * <p>Tokens are cached in {@link TokenStore}; the Copilot session token is
 * refreshed transparently when expired. Endpoints are GitHub-internal and may
 * change; failures surface as actionable {@link AiException}s.
 */
public final class CopilotProvider implements AiProvider {
    private static final String CLIENT_ID = "Iv1.b507a08c87ecfe98";
    private static final String DEVICE_CODE_URL = "https://github.com/login/device/code";
    private static final String ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String COPILOT_TOKEN_URL =
        "https://api.github.com/copilot_internal/v2/token";
    private static final String CHAT_URL = "https://api.githubcopilot.com/chat/completions";

    private static final HttpClient HTTP = HttpClient
        .newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    private final ObjectMapper mapper = new ObjectMapper();
    private final TokenStore store;
    private final String model;

    public CopilotProvider(TokenStore store, String model) {
        this.store = store;
        this.model = model == null || model.isBlank() ? "gpt-4o" : model;
    }

    @Override
    public String id() {
        return "copilot";
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public String describe() {
        boolean loggedIn = store.get("github_token") != null;
        return (
            "GitHub Copilot (model " +
            model +
            ", " +
            (loggedIn ? "logged in" : "not logged in — run /login") +
            ")"
        );
    }

    public boolean isLoggedIn() {
        return store.get("github_token") != null;
    }

    @Override
    public String chat(List<ChatMessage> messages) throws AiException {
        String token = copilotToken();
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.2);
        body.put("stream", false);
        ArrayNode arr = body.putArray("messages");
        for (ChatMessage m : messages) {
            ObjectNode mn = arr.addObject();
            mn.put("role", m.role());
            mn.put("content", m.content());
        }
        HttpRequest req = HttpRequest
            .newBuilder(URI.create(CHAT_URL))
            .timeout(Duration.ofSeconds(120))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + token)
            .header("Copilot-Integration-Id", "vscode-chat")
            .header("Editor-Version", "INGenious/3.1.1")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
        try {
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 401 || resp.statusCode() == 403) {
                store.put("copilot_token", null);
                throw new AiException(
                    "Copilot rejected the request (HTTP " +
                    resp.statusCode() +
                    "). Try /login again or check your Copilot subscription."
                );
            }
            if (resp.statusCode() / 100 != 2) {
                throw new AiException(
                    "Copilot returned HTTP " + resp.statusCode() + ": " + truncate(resp.body())
                );
            }
            JsonNode content = mapper
                .readTree(resp.body())
                .path("choices")
                .path(0)
                .path("message")
                .path("content");
            if (!content.isTextual()) {
                throw new AiException("Unexpected Copilot response: " + truncate(resp.body()));
            }
            return content.asText();
        } catch (IOException e) {
            throw new AiException("Copilot request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("Interrupted while waiting for Copilot.", e);
        }
    }

    // ------------------------------------------------------------------
    // auth
    // ------------------------------------------------------------------

    /**
     * Interactive OAuth device flow. Prints the user code + verification URL
     * and blocks until the user authorizes (or the code expires).
     */
    public void login() throws AiException {
        try {
            JsonNode device = postForm(
                DEVICE_CODE_URL,
                "client_id=" +
                CLIENT_ID +
                "&scope=" +
                URLEncoder.encode("read:user", StandardCharsets.UTF_8)
            );
            String userCode = device.path("user_code").asText();
            String verificationUri = device
                .path("verification_uri")
                .asText("https://github.com/login/device");
            String deviceCode = device.path("device_code").asText();
            int interval = Math.max(device.path("interval").asInt(5), 5);
            int expiresIn = device.path("expires_in").asInt(900);

            System.out.println();
            System.out.println("  To sign in with GitHub Copilot:");
            System.out.println("    1. Open " + verificationUri);
            System.out.println("    2. Enter code: " + userCode);
            System.out.println();
            System.out.println("  Waiting for authorization…");

            long deadline = System.currentTimeMillis() + expiresIn * 1000L;
            while (System.currentTimeMillis() < deadline) {
                Thread.sleep(interval * 1000L);
                JsonNode poll = postForm(
                    ACCESS_TOKEN_URL,
                    "client_id=" +
                    CLIENT_ID +
                    "&device_code=" +
                    deviceCode +
                    "&grant_type=" +
                    URLEncoder.encode(
                        "urn:ietf:params:oauth:grant-type:device_code",
                        StandardCharsets.UTF_8
                    )
                );
                if (poll.hasNonNull("access_token")) {
                    store.put("github_token", poll.get("access_token").asText());
                    store.put("copilot_token", null);
                    System.out.println("  Signed in successfully.");
                    return;
                }
                String err = poll.path("error").asText("");
                if ("slow_down".equals(err)) {
                    interval += 5;
                } else if (!"authorization_pending".equals(err)) {
                    throw new AiException("GitHub login failed: " + err);
                }
            }
            throw new AiException("Device code expired before authorization. Run /login again.");
        } catch (IOException e) {
            throw new AiException("GitHub login failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("Login interrupted.", e);
        }
    }

    /** Cached-or-refreshed Copilot session token. */
    private String copilotToken() throws AiException {
        String cached = store.get("copilot_token");
        String expiry = store.get("copilot_expires");
        if (cached != null && expiry != null) {
            try {
                if (Instant.now().getEpochSecond() < Long.parseLong(expiry) - 60) {
                    return cached;
                }
            } catch (NumberFormatException ignored) {
                // fall through to refresh
            }
        }
        String gh = store.get("github_token");
        if (gh == null) {
            throw new AiException("Not logged in to GitHub Copilot. Run /login first.");
        }
        HttpRequest req = HttpRequest
            .newBuilder(URI.create(COPILOT_TOKEN_URL))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "token " + gh)
            .header("Accept", "application/json")
            .header("Editor-Version", "INGenious/3.1.1")
            .GET()
            .build();
        try {
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new AiException(
                    "Could not obtain a Copilot session token (HTTP " +
                    resp.statusCode() +
                    "). Ensure your GitHub account has Copilot access, or run /login again."
                );
            }
            JsonNode node = mapper.readTree(resp.body());
            String token = node.path("token").asText(null);
            if (token == null) {
                throw new AiException("Unexpected Copilot token response.");
            }
            store.put("copilot_token", token);
            store.put("copilot_expires", String.valueOf(node.path("expires_at").asLong(0)));
            return token;
        } catch (IOException e) {
            throw new AiException("Copilot token exchange failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("Interrupted during token exchange.", e);
        }
    }

    private JsonNode postForm(String url, String form) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest
            .newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(resp.body());
    }

    private static String truncate(String s) {
        if (s == null) return "";
        String one = s.replace('\n', ' ');
        return one.length() <= 300 ? one : one.substring(0, 300) + "…";
    }
}
