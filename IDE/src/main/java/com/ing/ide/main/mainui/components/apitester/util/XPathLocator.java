package com.ing.ide.main.mainui.components.apitester.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes an XPath expression for a character offset within an XML document.
 * <p>
 * Used by the API Workbench response panel so that right-clicking on any line of
 * an XML response body can auto-suggest assertions based on the XPath / value at
 * the click position. Resilient to pretty-printing whitespace and missing
 * declarations / processing instructions / comments.
 * <p>
 * The generated XPath uses positional predicates ({@code /root/items/item[2]/id})
 * so it always selects exactly one node, matching how the runtime
 * STRUCTUREDDATA actions evaluate XPath against the response body.
 */
public final class XPathLocator {

    /** Result of a path lookup. */
    public static final class Hit {
        public final String path; // e.g. "/root/items/item[1]/id"
        public final String value; // text content of the element, or null
        public final boolean isAttribute;

        public Hit(String path, String value, boolean isAttribute) {
            this.path = path;
            this.value = value;
            this.isAttribute = isAttribute;
        }
    }

    private XPathLocator() {}

    /**
     * Locate the deepest XPath enclosing {@code offset} in {@code text}.
     *
     * @return a {@link Hit} or {@code null} if the document is empty / not XML.
     */
    public static Hit locate(String text, int offset) {
        if (text == null || text.isEmpty()) return null;
        int clamped = Math.max(0, Math.min(offset, text.length()));
        Parser p = new Parser(text, clamped);
        try {
            p.parse();
        } catch (RuntimeException ignored) {
            // Malformed XML: return whatever we've gathered so far.
        }
        return p.best;
    }

    // ---------------------------------------------------------------------

    private static final class Frame {
        final String name;
        final int siblingIndex; // 1-based index among siblings with the same name
        final int startOffset; // start of opening '<'

        Frame(String name, int siblingIndex, int startOffset) {
            this.name = name;
            this.siblingIndex = siblingIndex;
            this.startOffset = startOffset;
        }
    }

    private static final class Parser {
        final String t;
        final int len;
        final int offset;
        int i;

        Hit best;
        int bestRange = Integer.MAX_VALUE;

        // Stack of open elements.
        final List<Frame> stack = new ArrayList<>();
        // For each open element, track child name -> count so we can assign sibling indexes.
        final List<java.util.Map<String, Integer>> childCounts = new ArrayList<>();

        Parser(String text, int offset) {
            this.t = text;
            this.len = text.length();
            this.offset = offset;
        }

        void record(int start, int end, String path, String value, boolean isAttribute) {
            if (offset >= start && offset <= end) {
                int range = end - start;
                if (range < bestRange) {
                    bestRange = range;
                    best = new Hit(path, value, isAttribute);
                }
            }
        }

        void parse() {
            // Seed root-level child counter.
            childCounts.add(new java.util.HashMap<>());

            while (i < len) {
                char c = t.charAt(i);
                if (c == '<') {
                    if (startsWith("<!--")) {
                        skipUntil("-->", 3);
                    } else if (startsWith("<![CDATA[")) {
                        skipUntil("]]>", 3);
                    } else if (startsWith("<!") || startsWith("<?")) {
                        // DOCTYPE / declaration / PI
                        skipUntil(">", 1);
                    } else if (i + 1 < len && t.charAt(i + 1) == '/') {
                        // Closing tag </name>
                        int tagStart = i;
                        i += 2;
                        String name = readName();
                        skipUntilChar('>');
                        if (i < len) i++; // consume '>'
                        int tagEnd = i;
                        if (!stack.isEmpty()) {
                            Frame top = stack.get(stack.size() - 1);
                            // Record both the closing tag and the entire element span
                            // so right-clicks on the closing tag still map to this element.
                            record(tagStart, tagEnd, currentPath(), null, false);
                            record(top.startOffset, tagEnd, currentPath(), null, false);
                            stack.remove(stack.size() - 1);
                            childCounts.remove(childCounts.size() - 1);
                        }
                        // suppress unused warning
                        if (name == null) {
                            /* tolerate */
                        }
                    } else {
                        // Opening tag <name ...>  or self-closing <name ... />
                        int tagStart = i;
                        i++; // consume '<'
                        String name = readName();
                        if (name == null || name.isEmpty()) {
                            // Stray '<' - skip char to avoid infinite loop
                            continue;
                        }
                        // Sibling index = count of previously-seen siblings of this name + 1.
                        java.util.Map<String, Integer> counts = childCounts.get(
                            childCounts.size() - 1
                        );
                        int idx = counts.getOrDefault(name, 0) + 1;
                        counts.put(name, idx);

                        // Push new frame.
                        Frame frame = new Frame(name, idx, tagStart);
                        stack.add(frame);
                        childCounts.add(new java.util.HashMap<>());

                        // Parse attributes.
                        parseAttributes(currentPath(), tagStart);

                        // Tag terminator.
                        boolean selfClosed = false;
                        if (i < len && t.charAt(i) == '/') {
                            selfClosed = true;
                            i++;
                        }
                        if (i < len && t.charAt(i) == '>') i++;
                        int tagEnd = i;

                        // Record the opening tag span so right-clicks on the tag map to the element.
                        record(tagStart, tagEnd, currentPath(), null, false);

                        if (selfClosed) {
                            // Element has no body / text.
                            record(tagStart, tagEnd, currentPath(), null, false);
                            stack.remove(stack.size() - 1);
                            childCounts.remove(childCounts.size() - 1);
                        }
                    }
                } else {
                    // Text content.
                    int textStart = i;
                    StringBuilder sb = new StringBuilder();
                    while (i < len && t.charAt(i) != '<') {
                        sb.append(t.charAt(i));
                        i++;
                    }
                    int textEnd = i;
                    String text = sb.toString();
                    String trimmed = text.trim();
                    if (!trimmed.isEmpty() && !stack.isEmpty()) {
                        // Record text span against the current element's path.
                        record(textStart, textEnd, currentPath(), trimmed, false);
                        // Also record the entire element span so clicks anywhere in
                        // <elem>value</elem> map to elem with its value.
                        Frame top = stack.get(stack.size() - 1);
                        record(top.startOffset, textEnd, currentPath(), trimmed, false);
                    }
                }
            }
        }

        void parseAttributes(String elementPath, int tagStart) {
            while (i < len) {
                skipXmlWs();
                if (i >= len) return;
                char c = t.charAt(i);
                if (c == '>' || c == '/') return;
                int attrStart = i;
                String name = readName();
                if (name == null || name.isEmpty()) {
                    // Stray char - advance to avoid infinite loop.
                    if (i < len) i++;
                    continue;
                }
                skipXmlWs();
                String value = null;
                if (i < len && t.charAt(i) == '=') {
                    i++;
                    skipXmlWs();
                    if (i < len) {
                        char q = t.charAt(i);
                        if (q == '"' || q == '\'') {
                            i++;
                            int vStart = i;
                            while (i < len && t.charAt(i) != q) i++;
                            value = t.substring(vStart, i);
                            if (i < len) i++; // closing quote
                        } else {
                            int vStart = i;
                            while (
                                i < len &&
                                !Character.isWhitespace(t.charAt(i)) &&
                                t.charAt(i) != '>' &&
                                t.charAt(i) != '/'
                            ) i++;
                            value = t.substring(vStart, i);
                        }
                    }
                }
                int attrEnd = i;
                String attrPath = elementPath + "/@" + name;
                record(attrStart, attrEnd, attrPath, value, true);
                // suppress unused warning
                if (tagStart < 0) {
                    /* unused */
                }
            }
        }

        String currentPath() {
            if (stack.isEmpty()) return "/";
            StringBuilder sb = new StringBuilder();
            for (Frame f : stack) {
                sb.append('/').append(f.name);
                // Always emit positional predicate so the path selects exactly one node.
                sb.append('[').append(f.siblingIndex).append(']');
            }
            return sb.toString();
        }

        String readName() {
            int start = i;
            while (i < len) {
                char c = t.charAt(i);
                if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == ':' || c == '.') {
                    i++;
                } else {
                    break;
                }
            }
            return start == i ? null : t.substring(start, i);
        }

        boolean startsWith(String s) {
            if (i + s.length() > len) return false;
            for (int k = 0; k < s.length(); k++) {
                if (t.charAt(i + k) != s.charAt(k)) return false;
            }
            return true;
        }

        void skipUntil(String terminator, int skipInitial) {
            i += skipInitial;
            while (i < len) {
                if (i + terminator.length() <= len) {
                    boolean match = true;
                    for (int k = 0; k < terminator.length(); k++) {
                        if (t.charAt(i + k) != terminator.charAt(k)) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        i += terminator.length();
                        return;
                    }
                }
                i++;
            }
        }

        void skipUntilChar(char ch) {
            while (i < len && t.charAt(i) != ch) i++;
        }

        void skipXmlWs() {
            while (i < len && Character.isWhitespace(t.charAt(i))) i++;
        }
    }
}
