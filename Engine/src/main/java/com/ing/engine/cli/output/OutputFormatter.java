package com.ing.engine.cli.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.util.List;
import java.util.Map;

/**
 * Output formatter for CLI results.
 * Supports JSON, YAML, and Table formats.
 */
public abstract class OutputFormatter {

    protected boolean colored;

    protected OutputFormatter(boolean colored) {
        this.colored = colored;
    }

    /**
     * Format a single object.
     */
    public abstract String format(Object obj);

    /**
     * Format a list of objects as a table.
     */
    public abstract String formatTable(List<String> headers, List<List<String>> rows);

    /**
     * Format a key-value map.
     */
    public abstract String formatKeyValue(Map<String, Object> data);

    /**
     * Format a message with optional status.
     */
    public abstract String formatMessage(String message, MessageType type);

    /**
     * Create a JSON formatter.
     */
    public static OutputFormatter json() {
        return new JsonOutputFormatter();
    }

    /**
     * Create a YAML formatter.
     */
    public static OutputFormatter yaml() {
        return new YamlOutputFormatter();
    }

    /**
     * Create a table formatter.
     */
    public static OutputFormatter table(boolean colored) {
        return new TableOutputFormatter(colored);
    }

    public enum MessageType {
        SUCCESS, ERROR, WARNING, INFO
    }

    /**
     * JSON output formatter.
     */
    private static class JsonOutputFormatter extends OutputFormatter {
        private final ObjectMapper mapper;

        public JsonOutputFormatter() {
            super(false);
            this.mapper = new ObjectMapper();
            this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }

        @Override
        public String format(Object obj) {
            try {
                return mapper.writeValueAsString(obj);
            } catch (Exception e) {
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        }

        @Override
        public String formatTable(List<String> headers, List<List<String>> rows) {
            try {
                return mapper.writeValueAsString(Map.of("headers", headers, "rows", rows));
            } catch (Exception e) {
                return "{\"error\": \"" + e.getMessage() + "\"}";
            }
        }

        @Override
        public String formatKeyValue(Map<String, Object> data) {
            return format(data);
        }

        @Override
        public String formatMessage(String message, MessageType type) {
            return format(Map.of("message", message, "type", type.name().toLowerCase()));
        }
    }

    /**
     * YAML output formatter.
     */
    private static class YamlOutputFormatter extends OutputFormatter {
        private final YAMLMapper mapper;

        public YamlOutputFormatter() {
            super(false);
            this.mapper = new YAMLMapper();
        }

        @Override
        public String format(Object obj) {
            try {
                return mapper.writeValueAsString(obj);
            } catch (Exception e) {
                return "error: " + e.getMessage();
            }
        }

        @Override
        public String formatTable(List<String> headers, List<List<String>> rows) {
            try {
                return mapper.writeValueAsString(Map.of("headers", headers, "rows", rows));
            } catch (Exception e) {
                return "error: " + e.getMessage();
            }
        }

        @Override
        public String formatKeyValue(Map<String, Object> data) {
            return format(data);
        }

        @Override
        public String formatMessage(String message, MessageType type) {
            return format(Map.of("message", message, "type", type.name().toLowerCase()));
        }
    }

    /**
     * Table output formatter with ASCII art tables.
     */
    private static class TableOutputFormatter extends OutputFormatter {
        // ANSI color codes
        private static final String RESET = "\u001B[0m";
        private static final String GREEN = "\u001B[32m";
        private static final String RED = "\u001B[31m";
        private static final String YELLOW = "\u001B[33m";
        private static final String CYAN = "\u001B[36m";
        private static final String BOLD = "\u001B[1m";

        public TableOutputFormatter(boolean colored) {
            super(colored);
        }

        @Override
        public String format(Object obj) {
            if (obj instanceof Map) {
                return formatKeyValue((Map<String, Object>) obj);
            }
            return obj.toString();
        }

        @Override
        public String formatTable(List<String> headers, List<List<String>> rows) {
            if (headers.isEmpty()) {
                return "";
            }

            // Calculate column widths
            int[] widths = new int[headers.size()];
            for (int i = 0; i < headers.size(); i++) {
                widths[i] = headers.get(i).length();
            }
            for (List<String> row : rows) {
                for (int i = 0; i < Math.min(row.size(), widths.length); i++) {
                    widths[i] = Math.max(widths[i], row.get(i) != null ? row.get(i).length() : 0);
                }
            }

            StringBuilder sb = new StringBuilder();

            // Top border
            sb.append("┌");
            for (int i = 0; i < widths.length; i++) {
                sb.append("─".repeat(widths[i] + 2));
                sb.append(i < widths.length - 1 ? "┬" : "┐");
            }
            sb.append("\n");

            // Headers
            sb.append("│");
            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i);
                if (colored) {
                    sb.append(" ").append(BOLD).append(CYAN).append(padRight(header, widths[i])).append(RESET).append(" │");
                } else {
                    sb.append(" ").append(padRight(header, widths[i])).append(" │");
                }
            }
            sb.append("\n");

            // Header separator
            sb.append("├");
            for (int i = 0; i < widths.length; i++) {
                sb.append("─".repeat(widths[i] + 2));
                sb.append(i < widths.length - 1 ? "┼" : "┤");
            }
            sb.append("\n");

            // Rows
            for (List<String> row : rows) {
                sb.append("│");
                for (int i = 0; i < widths.length; i++) {
                    String cell = i < row.size() && row.get(i) != null ? row.get(i) : "";
                    sb.append(" ").append(padRight(cell, widths[i])).append(" │");
                }
                sb.append("\n");
            }

            // Bottom border
            sb.append("└");
            for (int i = 0; i < widths.length; i++) {
                sb.append("─".repeat(widths[i] + 2));
                sb.append(i < widths.length - 1 ? "┴" : "┘");
            }

            return sb.toString();
        }

        @Override
        public String formatKeyValue(Map<String, Object> data) {
            int maxKeyLength = data.keySet().stream()
                    .mapToInt(String::length)
                    .max()
                    .orElse(10);

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = padRight(entry.getKey(), maxKeyLength);
                String value = entry.getValue() != null ? entry.getValue().toString() : "null";
                if (colored) {
                    sb.append(CYAN).append(key).append(RESET).append(" : ").append(value).append("\n");
                } else {
                    sb.append(key).append(" : ").append(value).append("\n");
                }
            }
            return sb.toString().trim();
        }

        @Override
        public String formatMessage(String message, MessageType type) {
            if (!colored) {
                String prefix;
                switch (type) {
                    case SUCCESS: prefix = "✓"; break;
                    case ERROR: prefix = "✗"; break;
                    case WARNING: prefix = "⚠"; break;
                    case INFO: prefix = "ℹ"; break;
                    default: prefix = ""; break;
                }
                return prefix + " " + message;
            }

            switch (type) {
                case SUCCESS: return GREEN + "✓ " + message + RESET;
                case ERROR: return RED + "✗ " + message + RESET;
                case WARNING: return YELLOW + "⚠ " + message + RESET;
                case INFO: return CYAN + "ℹ " + message + RESET;
                default: return message;
            }
        }

        private String padRight(String s, int length) {
            if (s.length() >= length) {
                return s;
            }
            return s + " ".repeat(length - s.length());
        }
    }
}
