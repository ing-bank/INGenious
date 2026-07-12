package com.ing.engine.aicli.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Any OpenAI-compatible chat-completions endpoint: OpenAI, Azure OpenAI
 * (compat mode), Ollama / local models, corporate gateways.
 */
public final class OpenAiCompatProvider implements AiProvider {
    private static final HttpClient HTTP = HttpClient
        .newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public OpenAiCompatProvider(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String id() {
        return "openai";
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public String describe() {
        return "openai-compatible @ " + baseUrl + " (model " + model + ")";
    }

    @Override
    public String chat(List<ChatMessage> messages) throws AiException {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.2);
        ArrayNode arr = body.putArray("messages");
        for (ChatMessage m : messages) {
            ObjectNode mn = arr.addObject();
            mn.put("role", m.role());
            mn.put("content", m.content());
        }
        HttpRequest.Builder req = HttpRequest
            .newBuilder(URI.create(baseUrl + "/chat/completions"))
            .timeout(Duration.ofSeconds(120))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        if (apiKey != null && !apiKey.isBlank()) {
            req.header("Authorization", "Bearer " + apiKey);
        }
        try {
            HttpResponse<String> resp = HTTP.send(
                req.build(),
                HttpResponse.BodyHandlers.ofString()
            );
            if (resp.statusCode() / 100 != 2) {
                throw new AiException(
                    "Provider returned HTTP " + resp.statusCode() + ": " + truncate(resp.body())
                );
            }
            JsonNode root = mapper.readTree(resp.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isTextual()) {
                throw new AiException("Unexpected provider response: " + truncate(resp.body()));
            }
            return content.asText();
        } catch (IOException e) {
            throw new AiException("Provider request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("Interrupted while waiting for provider.", e);
        }
    }

    private static String truncate(String s) {
        if (s == null) return "";
        String one = s.replace('\n', ' ');
        return one.length() <= 300 ? one : one.substring(0, 300) + "…";
    }

    @Override
    public boolean supportsTools() {
        return true;
    }

    @Override
    public AgentReply chatWithTools(List<AgentMessage> messages, List<ToolSpec> tools)
        throws AiException {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.1);
        ArrayNode msgs = body.putArray("messages");
        for (AgentMessage m : messages) {
            ObjectNode mn = msgs.addObject();
            mn.put("role", m.role);
            if ("tool".equals(m.role)) {
                mn.put("tool_call_id", m.toolCallId == null ? "" : m.toolCallId);
                if (m.toolName != null) mn.put("name", m.toolName);
                mn.put("content", m.content == null ? "" : m.content);
            } else if (
                "assistant".equals(m.role) && m.toolCalls != null && !m.toolCalls.isEmpty()
            ) {
                if (m.content == null) mn.putNull("content"); else mn.put("content", m.content);
                ArrayNode tcs = mn.putArray("tool_calls");
                for (AgentToolCall c : m.toolCalls) {
                    ObjectNode tc = tcs.addObject();
                    tc.put("id", c.id == null ? "" : c.id);
                    tc.put("type", "function");
                    ObjectNode fn = tc.putObject("function");
                    fn.put("name", c.name);
                    fn.put("arguments", c.argumentsJson == null ? "{}" : c.argumentsJson);
                }
            } else {
                mn.put("content", m.content == null ? "" : m.content);
            }
        }
        if (tools != null && !tools.isEmpty()) {
            ArrayNode ts = body.putArray("tools");
            for (ToolSpec t : tools) {
                ObjectNode tn = ts.addObject();
                tn.put("type", "function");
                ObjectNode fn = tn.putObject("function");
                fn.put("name", t.name);
                if (t.description != null) fn.put("description", t.description);
                fn.set(
                    "parameters",
                    t.parameters != null ? t.parameters : mapper.createObjectNode()
                );
            }
        }

        HttpRequest.Builder req = HttpRequest
            .newBuilder(URI.create(baseUrl + "/chat/completions"))
            .timeout(Duration.ofSeconds(180))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        if (apiKey != null && !apiKey.isBlank()) {
            req.header("Authorization", "Bearer " + apiKey);
        }
        try {
            HttpResponse<String> resp = HTTP.send(
                req.build(),
                HttpResponse.BodyHandlers.ofString()
            );
            if (resp.statusCode() / 100 != 2) {
                throw new AiException(
                    "Provider returned HTTP " + resp.statusCode() + ": " + truncate(resp.body())
                );
            }
            JsonNode message = mapper.readTree(resp.body()).path("choices").path(0).path("message");
            String content = message.path("content").isTextual()
                ? message.path("content").asText()
                : null;
            List<AgentToolCall> calls = new ArrayList<>();
            JsonNode toolCalls = message.path("tool_calls");
            if (toolCalls.isArray()) {
                for (JsonNode tc : toolCalls) {
                    calls.add(
                        new AgentToolCall(
                            tc.path("id").asText(""),
                            tc.path("function").path("name").asText(""),
                            tc.path("function").path("arguments").asText("{}")
                        )
                    );
                }
            }
            return new AgentReply(content, calls);
        } catch (IOException e) {
            throw new AiException("Provider request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException("Interrupted while waiting for provider.", e);
        }
    }
}
