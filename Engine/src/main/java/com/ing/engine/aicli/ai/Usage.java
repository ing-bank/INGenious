package com.ing.engine.aicli.ai;

import com.fasterxml.jackson.databind.JsonNode;

/** AI consumption for a task: model requests (credits) plus token counts. */
public final class Usage {
    public int requests;
    public int promptTokens;
    public int completionTokens;
    public int totalTokens;

    public Usage() {}

    public Usage(int requests, int promptTokens, int completionTokens, int totalTokens) {
        this.requests = requests;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }

    /** Records one model response's usage; counts the response as one request (credit). */
    public synchronized void addResponse(JsonNode usageNode) {
        requests++;
        if (usageNode != null && usageNode.isObject()) {
            int in = usageNode.path("prompt_tokens").asInt(0);
            int out = usageNode.path("completion_tokens").asInt(0);
            int total = usageNode.path("total_tokens").asInt(0);
            promptTokens += in;
            completionTokens += out;
            totalTokens += total > 0 ? total : in + out;
        }
    }

    public synchronized Usage copy() {
        return new Usage(requests, promptTokens, completionTokens, totalTokens);
    }

    /** Consumption accrued since an earlier snapshot. */
    public Usage since(Usage before) {
        if (before == null) return copy();
        return new Usage(
            requests - before.requests,
            promptTokens - before.promptTokens,
            completionTokens - before.completionTokens,
            totalTokens - before.totalTokens
        );
    }

    public boolean isEmpty() {
        return requests == 0 && totalTokens == 0;
    }

    /** Compact one-line summary, e.g. {@code 2 requests · 1,234 tokens (980 in / 254 out)}. */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append(requests).append(requests == 1 ? " request" : " requests");
        if (totalTokens > 0) {
            sb
                .append(" · ")
                .append(String.format("%,d", totalTokens))
                .append(" tokens (")
                .append(String.format("%,d", promptTokens))
                .append(" in / ")
                .append(String.format("%,d", completionTokens))
                .append(" out)");
        }
        return sb.toString();
    }
}
