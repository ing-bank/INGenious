package com.ing.ide.main.mainui.components.aichat.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A tool definition sent to the model in the request {@code tools} array,
 * following the OpenAI-compatible function-calling schema. The
 * {@code parameters} field is a JSON Schema object describing the tool inputs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tool {
    @JsonProperty("type")
    private String type = "function";

    @JsonProperty("function")
    private Function function;

    public Tool() {}

    public Tool(String name, String description, JsonNode parameters) {
        this.function = new Function(name, description, parameters);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Function getFunction() {
        return function;
    }

    public void setFunction(Function function) {
        this.function = function;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Function {
        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("parameters")
        private JsonNode parameters;

        public Function() {}

        public Function(String name, String description, JsonNode parameters) {
            this.name = name;
            this.description = description;
            this.parameters = parameters;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public JsonNode getParameters() {
            return parameters;
        }

        public void setParameters(JsonNode parameters) {
            this.parameters = parameters;
        }
    }
}
