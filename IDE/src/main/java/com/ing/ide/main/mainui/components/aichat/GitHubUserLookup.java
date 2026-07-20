package com.ing.ide.main.mainui.components.aichat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Best-effort lookup of the authenticated user's GitHub login, used only for a
 * cosmetic "Signed in as …" label. Failures are non-fatal to sign-in.
 */
class GitHubUserLookup {
    private static final String USER_URL = "https://api.github.com/user";

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient
        .newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    String login(String token) throws Exception {
        HttpRequest request = HttpRequest
            .newBuilder()
            .uri(URI.create(USER_URL))
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer " + token)
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString()
        );
        if (response.statusCode() / 100 != 2) {
            return "";
        }
        JsonNode node = mapper.readTree(response.body());
        return node.path("login").asText("");
    }
}
