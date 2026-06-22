package com.ing.ide.main.mainui.components.aichat.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Request body for the GitHub Models {@code POST /inference/chat/completions}
 * endpoint (OpenAI-compatible).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {
    @JsonProperty("model")
    private String model;

    @JsonProperty("messages")
    private List<ChatMessage> messages = new ArrayList<>();

    @JsonProperty("stream")
    private Boolean stream;

    @JsonProperty("temperature")
    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("stream_options")
    private StreamOptions streamOptions;

    @JsonProperty("tools")
    private List<Tool> tools;

    public ChatCompletionRequest() {}

    public ChatCompletionRequest(String model, List<ChatMessage> messages, boolean stream) {
        this.model = model;
        this.messages = messages;
        this.stream = stream;
    }

    public String getModel() {
        return model;
    }

    public List<Tool> getTools() {
        return tools;
    }

    public void setTools(List<Tool> tools) {
        this.tools = tools;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public StreamOptions getStreamOptions() {
        return streamOptions;
    }

    public void setStreamOptions(StreamOptions streamOptions) {
        this.streamOptions = streamOptions;
    }

    /** Asks the API to emit a final usage chunk while streaming. */
    public void requestStreamingUsage() {
        this.streamOptions = new StreamOptions(true);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StreamOptions {
        @JsonProperty("include_usage")
        private Boolean includeUsage;

        public StreamOptions() {}

        public StreamOptions(Boolean includeUsage) {
            this.includeUsage = includeUsage;
        }

        public Boolean getIncludeUsage() {
            return includeUsage;
        }

        public void setIncludeUsage(Boolean includeUsage) {
            this.includeUsage = includeUsage;
        }
    }
}
