package com.ing.ide.main.mainui.components.apitester.util;

/**
 * Computes a JSONPath expression for a character offset within a JSON document.
 * <p>
 * Used by the API Workbench response panel so that right-clicking on any line of
 * the response body can auto-suggest assertions based on the JSON path / value at
 * the click position. Resilient to pretty-printing whitespace.
 */
public final class JsonPathLocator {

    /** Result of a path lookup. */
    public static final class Hit {
        public final String path;       // e.g. "$.user.name" or "$.items[2]"
        public final String value;      // primitive value at path (unescaped), or null
        public final boolean isString;  // true if the value is a JSON string literal

        public Hit(String path, String value, boolean isString) {
            this.path = path;
            this.value = value;
            this.isString = isString;
        }
    }

    private JsonPathLocator() {}

    /**
     * Locate the deepest JSON path enclosing {@code offset} in {@code text}.
     *
     * @return a {@link Hit} or {@code null} if the document is empty / not JSON.
     */
    public static Hit locate(String text, int offset) {
        if (text == null || text.isEmpty()) return null;
        Parser p = new Parser(text, Math.max(0, Math.min(offset, text.length())));
        try {
            p.skipWs();
            if (p.i < p.len) {
                p.parseValue("$");
            }
        } catch (RuntimeException ignored) {
            // Malformed JSON: return whatever we've gathered so far
        }
        return p.best;
    }

    // ---------------------------------------------------------------------

    private static final class Parser {
        final String t;
        final int len;
        final int offset;
        int i;
        Hit best;
        int bestRange = Integer.MAX_VALUE;

        Parser(String text, int offset) {
            this.t = text;
            this.len = text.length();
            this.offset = offset;
        }

        void record(int start, int end, String path, String value, boolean isString) {
            if (offset >= start && offset <= end) {
                int range = end - start;
                if (range < bestRange) {
                    bestRange = range;
                    best = new Hit(path, value, isString);
                }
            }
        }

        void skipWs() {
            while (i < len && Character.isWhitespace(t.charAt(i))) i++;
        }

        void parseValue(String path) {
            skipWs();
            if (i >= len) return;
            char c = t.charAt(i);
            int start = i;
            if (c == '{') {
                parseObject(path, start);
            } else if (c == '[') {
                parseArray(path, start);
            } else {
                boolean isString = c == '"';
                String v;
                if (isString) {
                    v = parseString();
                } else {
                    v = parsePrimitive();
                }
                int end = i;
                record(start, end, path, v, isString);
            }
        }

        void parseObject(String path, int objStart) {
            i++; // consume '{'
            while (true) {
                skipWs();
                if (i >= len) break;
                char c = t.charAt(i);
                if (c == '}') {
                    i++;
                    record(objStart, i, path, null, false);
                    return;
                }
                if (c == ',') { i++; continue; }
                if (c != '"') { i++; continue; } // tolerate stray chars
                int keyStart = i;
                String key = parseString();
                String childPath = buildChildPath(path, key);
                skipWs();
                if (i < len && t.charAt(i) == ':') i++;
                skipWs();
                int valStart = i;
                parseValue(childPath);
                int valEnd = i;
                // Swallow optional trailing comma so the whole "  "key": value,\n"
                // line maps to childPath when the user right-clicks anywhere on it.
                skipWs();
                if (i < len && t.charAt(i) == ',') i++;
                int entryEnd = i;
                record(keyStart, entryEnd, childPath,
                        extractPrimitive(valStart, valEnd),
                        valStart < len && t.charAt(valStart) == '"');
            }
        }

        void parseArray(String path, int arrStart) {
            i++; // consume '['
            int idx = 0;
            while (true) {
                skipWs();
                if (i >= len) break;
                char c = t.charAt(i);
                if (c == ']') {
                    i++;
                    record(arrStart, i, path, null, false);
                    return;
                }
                if (c == ',') { i++; idx++; continue; }
                String childPath = path + "[" + idx + "]";
                int elemStart = i;
                parseValue(childPath);
                int elemEnd = i;
                skipWs();
                boolean hadComma = false;
                if (i < len && t.charAt(i) == ',') { i++; hadComma = true; }
                int entryEnd = i;
                record(elemStart, entryEnd, childPath,
                        extractPrimitive(elemStart, elemEnd),
                        elemStart < len && t.charAt(elemStart) == '"');
                if (hadComma) idx++;
            }
        }

        String parseString() {
            // assumes t[i] == '"'
            StringBuilder sb = new StringBuilder();
            i++; // opening quote
            while (i < len) {
                char c = t.charAt(i);
                if (c == '\\' && i + 1 < len) {
                    char n = t.charAt(i + 1);
                    switch (n) {
                        case '"':  sb.append('"');  i += 2; break;
                        case '\\': sb.append('\\'); i += 2; break;
                        case '/':  sb.append('/');  i += 2; break;
                        case 'b':  sb.append('\b'); i += 2; break;
                        case 'f':  sb.append('\f'); i += 2; break;
                        case 'n':  sb.append('\n'); i += 2; break;
                        case 'r':  sb.append('\r'); i += 2; break;
                        case 't':  sb.append('\t'); i += 2; break;
                        case 'u':
                            if (i + 5 < len) {
                                try {
                                    sb.append((char) Integer.parseInt(t.substring(i + 2, i + 6), 16));
                                    i += 6;
                                } catch (NumberFormatException e) {
                                    sb.append(n);
                                    i += 2;
                                }
                            } else {
                                i += 2;
                            }
                            break;
                        default:
                            sb.append(n);
                            i += 2;
                            break;
                    }
                } else if (c == '"') {
                    i++;
                    return sb.toString();
                } else {
                    sb.append(c);
                    i++;
                }
            }
            return sb.toString();
        }

        String parsePrimitive() {
            int start = i;
            while (i < len) {
                char c = t.charAt(i);
                if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) break;
                i++;
            }
            return t.substring(start, i);
        }

        /** Return the primitive value text in [start,end) — null for objects/arrays. */
        String extractPrimitive(int start, int end) {
            if (start >= end || start >= len) return null;
            char c = t.charAt(start);
            if (c == '{' || c == '[') return null;
            if (c == '"') {
                // re-parse to unescape
                int saved = i;
                i = start;
                String s = parseString();
                i = saved;
                return s;
            }
            return t.substring(start, end).trim();
        }
    }

    private static String buildChildPath(String parent, String key) {
        if (key != null && !key.isEmpty() && key.matches("[a-zA-Z_][a-zA-Z_0-9]*")) {
            return parent + "." + key;
        }
        String escaped = key == null ? "" : key.replace("\\", "\\\\").replace("'", "\\'");
        return parent + "['" + escaped + "']";
    }
}
