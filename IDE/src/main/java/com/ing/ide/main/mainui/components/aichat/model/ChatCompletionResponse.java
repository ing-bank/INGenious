package com.ing.ide.main.mainui.components.aichat.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response body for a non-streaming chat completion, and also the shape of each
 * streamed Server-Sent-Event chunk (OpenAI-compatible). For streamed chunks the
 * assistant text is found in {@code choices[].delta.content}; for non-streaming
 * responses it is in {@code choices[].message.content}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionResponse {
    @JsonProperty("id")
    private String id;

    @JsonProperty("model")
    private String model;

    @JsonProperty("choices")
    private List<Choice> choices;

    @JsonProperty("usage")
    private TokenUsage usage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public TokenUsage getUsage() {
        return usage;
    }

    public void setUsage(TokenUsage usage) {
        this.usage = usage;
    }

    /**
     * Convenience accessor returning the assistant text from the first choice,
     * preferring the streamed {@code delta} but falling back to {@code message}.
     */
    public String firstContent() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Choice c = choices.get(0);
        if (c.getDelta() != null && c.getDelta().getContent() != null) {
            return c.getDelta().getContent();
        }
        if (c.getMessage() != null) {
            return c.getMessage().getContent();
        }
        return null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        @JsonProperty("index")
        private int index;

        @JsonProperty("message")
        private ChatMessage message;

        @JsonProperty("delta")
        private ChatMessage delta;

        @JsonProperty("finish_reason")
        private String finishReason;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public ChatMessage getMessage() {
            return message;
        }

        public void setMessage(ChatMessage message) {
            this.message = message;
        }

        public ChatMessage getDelta() {
            return delta;
        }

        public void setDelta(ChatMessage delta) {
            this.delta = delta;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
    }
}
