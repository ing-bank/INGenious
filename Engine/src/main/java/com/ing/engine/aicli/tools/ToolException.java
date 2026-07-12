package com.ing.engine.aicli.tools;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/** Failure raised by a {@link Tool}. Carries the optional structured error payload. */
public class ToolException extends Exception {
    private final int code;
    private final transient JsonNode data;

    public ToolException(String message) {
        this(message, -32603, null);
    }

    public ToolException(String message, int code, JsonNode data) {
        super(message);
        this.code = code;
        this.data = data;
    }

    public int code() {
        return code;
    }

    public JsonNode data() {
        return data;
    }

    /** "Did you mean?" candidates when the underlying tool provided them. */
    public List<String> suggestions() {
        List<String> out = new ArrayList<>();
        if (data != null && data.has("suggestions")) {
            data.get("suggestions").forEach(n -> out.add(n.asText()));
        }
        return out;
    }
}
