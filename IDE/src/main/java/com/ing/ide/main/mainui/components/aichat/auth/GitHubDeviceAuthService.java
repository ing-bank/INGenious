package com.ing.ide.main.mainui.components.aichat.auth;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements the GitHub OAuth <strong>Device Flow</strong> used to sign the user
 * in without an embedded browser, mirroring the {@code gh} CLI experience:
 *
 * <ol>
 *   <li>Request a device + user code from GitHub.</li>
 *   <li>Show the user code and verification URL (via {@link CodePrompt}).</li>
 *   <li>Poll GitHub until the user approves and a token is issued.</li>
 * </ol>
 *
 * <p>Requires a registered GitHub OAuth App client id (a public client). The
 * {@code models} scope grants access to the GitHub Models inference API.</p>
 */
public class GitHubDeviceAuthService {
    private static final Logger LOG = Logger.getLogger(GitHubDeviceAuthService.class.getName());

    private static final String DEVICE_CODE_URL = "https://github.com/login/device/code";
    private static final String ACCESS_TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code";
    private static final String SCOPE = "read:user";

    private final ObjectMapper mapper = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final HttpClient httpClient = HttpClient
        .newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(30))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    /** Callback invoked once the user code is available, so the UI can show it. */
    public interface CodePrompt {
        void show(DeviceCodeResponse deviceCode);
    }

    /** Thrown when the device flow fails or is denied. */
    public static class AuthException extends Exception {

        public AuthException(String message) {
            super(message);
        }
    }

    private volatile boolean cancelled;

    /** Cancels an in-progress polling loop. */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Runs the full device flow and returns the access token. Blocks while
     * polling; intended to be called from a background thread.
     *
     * @param clientId the GitHub OAuth App client id
     * @param prompt   callback to display the user code to the user
     * @return the issued access token
     */
    public String authorize(String clientId, CodePrompt prompt) throws AuthException {
        if (clientId == null || clientId.isEmpty()) {
            throw new AuthException(
                "No GitHub OAuth client id configured. Set one in AI settings."
            );
        }
        cancelled = false;
        DeviceCodeResponse device = requestDeviceCode(clientId);
        if (prompt != null) {
            prompt.show(device);
        }
        return pollForToken(clientId, device);
    }

    private DeviceCodeResponse requestDeviceCode(String clientId) throws AuthException {
        String body = "client_id=" + enc(clientId) + "&scope=" + enc(SCOPE);
        HttpRequest request = HttpRequest
            .newBuilder()
            .uri(URI.create(DEVICE_CODE_URL))
            .header("Accept", "application/json")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        try {
            HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() / 100 != 2) {
                throw new AuthException(
                    "Failed to start device flow (HTTP " +
                    response.statusCode() +
                    "): " +
                    response.body()
                );
            }
            DeviceCodeResponse device = mapper.readValue(response.body(), DeviceCodeResponse.class);
            if (device.getDeviceCode() == null || device.getUserCode() == null) {
                throw new AuthException("Invalid device code response: " + response.body());
            }
            return device;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AuthException("Network error starting device flow: " + ex.getMessage());
        }
    }

    private String pollForToken(String clientId, DeviceCodeResponse device) throws AuthException {
        long intervalMs = Math.max(device.getInterval(), 5) * 1000L;
        long deadline = System.currentTimeMillis() + device.getExpiresIn() * 1000L;
        String body =
            "client_id=" +
            enc(clientId) +
            "&device_code=" +
            enc(device.getDeviceCode()) +
            "&grant_type=" +
            enc(GRANT_TYPE);

        while (System.currentTimeMillis() < deadline) {
            if (cancelled) {
                throw new AuthException("Sign-in cancelled.");
            }
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AuthException("Sign-in interrupted.");
            }

            HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create(ACCESS_TOKEN_URL))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            try {
                HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
                );
                JsonNode node = mapper.readTree(response.body());
                if (node.hasNonNull("access_token")) {
                    return node.get("access_token").asText();
                }
                String error = node.path("error").asText("");
                switch (error) {
                    case "authorization_pending":
                        break; // keep polling
                    case "slow_down":
                        intervalMs += 5000L;
                        break;
                    case "expired_token":
                        throw new AuthException(
                            "Device code expired. Please try signing in again."
                        );
                    case "access_denied":
                        throw new AuthException("Sign-in was denied.");
                    default:
                        if (!error.isEmpty()) {
                            throw new AuthException("Authorization failed: " + error);
                        }
                }
            } catch (IOException | InterruptedException ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                LOG.log(Level.FINE, "Transient error while polling for token", ex);
            }
        }
        throw new AuthException("Timed out waiting for authorization.");
    }

    private static String enc(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
