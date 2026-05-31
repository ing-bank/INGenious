package com.ing.datalib.api;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Parses a {@code curl} command line string into an {@link APIRequest}.
 *
 * <p>Mirrors the behaviour offered by Postman's "paste a curl command" feature.
 * Supports the most common curl options used when sharing API examples:
 * <ul>
 *   <li>{@code -X} / {@code --request} – HTTP method</li>
 *   <li>{@code -H} / {@code --header} – request headers</li>
 *   <li>{@code -d} / {@code --data} / {@code --data-raw} / {@code --data-binary} /
 *       {@code --data-ascii} / {@code --data-urlencode} – request body</li>
 *   <li>{@code -F} / {@code --form} – multipart form data</li>
 *   <li>{@code -u} / {@code --user} – HTTP Basic auth</li>
 *   <li>{@code -k} / {@code --insecure} – disable SSL verification</li>
 *   <li>{@code -L} / {@code --location} – follow redirects</li>
 *   <li>{@code --url} or a positional URL argument</li>
 * </ul>
 *
 * <p>The parser is tolerant of shell-style single/double quoted strings,
 * backslash escapes and trailing line-continuations ({@code \\\n}).
 */
public final class CurlParser {

    private CurlParser() {
    }

    /**
     * Returns {@code true} if the given text starts with a {@code curl}
     * invocation (after trimming leading whitespace).
     */
    public static boolean looksLikeCurl(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        if (trimmed.length() < 5) {
            return false;
        }
        // Allow "curl ", "curl\n", "curl\t" or "CURL ..."
        String lower = trimmed.toLowerCase();
        if (!lower.startsWith("curl")) {
            return false;
        }
        char next = trimmed.charAt(4);
        return Character.isWhitespace(next) || next == '\\';
    }

    /**
     * Parses the given curl command string into an {@link APIRequest}.
     *
     * @param curl the curl command (may span multiple lines with {@code \\} continuations)
     * @return the parsed request, never {@code null}
     * @throws IllegalArgumentException if the input does not look like a curl command
     */
    public static APIRequest parse(String curl) {
        if (curl == null) {
            throw new IllegalArgumentException("curl command is null");
        }
        String normalized = normalize(curl);
        List<String> tokens = tokenize(normalized);
        if (tokens.isEmpty() || !"curl".equalsIgnoreCase(tokens.get(0))) {
            throw new IllegalArgumentException("Not a curl command");
        }

        APIRequest request = new APIRequest();
        // Start clean – callers will typically replace whatever was on the form.
        request.setHeaders(new ArrayList<>());
        request.setQueryParams(new ArrayList<>());
        request.setBody(new RequestBody());
        request.setAuth(new AuthConfig());

        String explicitMethod = null;
        boolean hasBody = false;
        StringBuilder rawBody = new StringBuilder();
        List<KeyValuePair> formData = new ArrayList<>();
        List<KeyValuePair> urlEncoded = new ArrayList<>();
        String url = null;

        for (int i = 1; i < tokens.size(); i++) {
            String tok = tokens.get(i);
            switch (tok) {
                case "-X":
                case "--request":
                    if (i + 1 < tokens.size()) {
                        explicitMethod = tokens.get(++i).toUpperCase();
                    }
                    break;
                case "-H":
                case "--header": {
                    if (i + 1 < tokens.size()) {
                        addHeader(request, tokens.get(++i));
                    }
                    break;
                }
                case "-A":
                case "--user-agent": {
                    if (i + 1 < tokens.size()) {
                        request.getHeaders().add(new KeyValuePair("User-Agent", tokens.get(++i)));
                    }
                    break;
                }
                case "-e":
                case "--referer": {
                    if (i + 1 < tokens.size()) {
                        request.getHeaders().add(new KeyValuePair("Referer", tokens.get(++i)));
                    }
                    break;
                }
                case "-b":
                case "--cookie": {
                    if (i + 1 < tokens.size()) {
                        request.getHeaders().add(new KeyValuePair("Cookie", tokens.get(++i)));
                    }
                    break;
                }
                case "-d":
                case "--data":
                case "--data-raw":
                case "--data-binary":
                case "--data-ascii": {
                    if (i + 1 < tokens.size()) {
                        appendData(rawBody, tokens.get(++i));
                        hasBody = true;
                    }
                    break;
                }
                case "--data-urlencode": {
                    if (i + 1 < tokens.size()) {
                        String value = tokens.get(++i);
                        int eq = value.indexOf('=');
                        if (eq >= 0) {
                            urlEncoded.add(new KeyValuePair(value.substring(0, eq), value.substring(eq + 1)));
                        } else {
                            urlEncoded.add(new KeyValuePair("", value));
                        }
                        hasBody = true;
                    }
                    break;
                }
                case "-F":
                case "--form": {
                    if (i + 1 < tokens.size()) {
                        String value = tokens.get(++i);
                        int eq = value.indexOf('=');
                        if (eq >= 0) {
                            formData.add(new KeyValuePair(value.substring(0, eq), value.substring(eq + 1)));
                        } else {
                            formData.add(new KeyValuePair(value, ""));
                        }
                        hasBody = true;
                    }
                    break;
                }
                case "-u":
                case "--user": {
                    if (i + 1 < tokens.size()) {
                        String creds = tokens.get(++i);
                        int colon = creds.indexOf(':');
                        String user = colon >= 0 ? creds.substring(0, colon) : creds;
                        String pass = colon >= 0 ? creds.substring(colon + 1) : "";
                        request.setAuth(AuthConfig.basic(user, pass));
                    }
                    break;
                }
                case "-k":
                case "--insecure":
                    request.setSslVerificationEnabled(false);
                    break;
                case "-L":
                case "--location":
                case "--location-trusted":
                    request.setFollowRedirects(true);
                    break;
                case "--compressed":
                case "--compressed-ssh":
                case "-s":
                case "--silent":
                case "-v":
                case "--verbose":
                case "-i":
                case "--include":
                case "-I":
                case "--head":
                case "-#":
                case "--progress-bar":
                case "--no-progress-meter":
                case "-g":
                case "--globoff":
                case "-J":
                case "--remote-header-name":
                case "-O":
                case "--remote-name":
                case "-f":
                case "--fail":
                case "--fail-with-body":
                case "--http1.0":
                case "--http1.1":
                case "--http2":
                case "--http2-prior-knowledge":
                case "--tlsv1":
                case "--tlsv1.0":
                case "--tlsv1.1":
                case "--tlsv1.2":
                case "--tlsv1.3":
                    // Flags with no argument – safe to ignore.
                    break;
                case "--url": {
                    if (i + 1 < tokens.size()) {
                        url = tokens.get(++i);
                    }
                    break;
                }
                case "-o":
                case "--output":
                case "--connect-timeout":
                case "--max-time":
                case "--retry":
                case "--retry-delay":
                case "--retry-max-time":
                case "--proxy":
                case "-x":
                case "--cacert":
                case "--cert":
                case "--key":
                case "--cert-type":
                case "--key-type":
                case "--resolve":
                case "--dns-servers":
                case "--write-out":
                case "-w":
                case "-T":
                case "--upload-file":
                case "--form-string":
                    // Options that take an argument but we don't represent yet – skip value.
                    if (i + 1 < tokens.size()) {
                        i++;
                    }
                    break;
                default:
                    if (tok.startsWith("-")) {
                        // Unknown flag – best-effort: if next token isn't another flag, swallow it.
                        if (i + 1 < tokens.size() && !tokens.get(i + 1).startsWith("-")) {
                            i++;
                        }
                    } else if (url == null) {
                        url = tok;
                    }
                    break;
            }
        }

        if (url != null) {
            url = stripWrappingQuotes(url);
            applyUrl(request, url);
        }

        // Resolve method – explicit -X wins, otherwise infer from body presence.
        if (explicitMethod != null) {
            request.setMethod(toHttpMethod(explicitMethod));
        } else if (hasBody) {
            request.setMethod(APIRequest.HttpMethod.POST);
        } else {
            request.setMethod(APIRequest.HttpMethod.GET);
        }

        // Body assembly.
        if (!formData.isEmpty()) {
            RequestBody body = new RequestBody();
            body.setBodyType(RequestBody.BodyType.FORM_DATA);
            body.setFormData(formData);
            request.setBody(body);
        } else if (!urlEncoded.isEmpty()) {
            RequestBody body = new RequestBody();
            body.setBodyType(RequestBody.BodyType.URL_ENCODED);
            body.setUrlEncodedData(urlEncoded);
            request.setBody(body);
        } else if (rawBody.length() > 0) {
            String content = rawBody.toString();
            RequestBody body = new RequestBody();
            body.setBodyType(RequestBody.BodyType.RAW);
            body.setRawFormat(detectRawFormat(content, request.getHeaders()));
            body.setRawContent(content);
            request.setBody(body);
        }

        // Promote an Authorization header to a typed AuthConfig when possible.
        promoteAuthFromHeaders(request);

        return request;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private static String normalize(String input) {
        // Join shell line-continuations: backslash followed by newline.
        String s = input.replace("\r\n", "\n").replace('\r', '\n');
        s = s.replaceAll("\\\\\n", " ");
        // Many copy/paste sources prefix with a shell prompt – strip a leading $.
        s = s.trim();
        if (s.startsWith("$ ")) {
            s = s.substring(2).trim();
        }
        return s;
    }

    /**
     * Tokenizes a shell-style command line honouring single quotes, double quotes
     * and backslash escapes. Quotes themselves are removed from the resulting tokens.
     */
    static List<String> tokenize(String input) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inToken = false;
        char quote = 0; // 0 = not in quotes

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (quote != 0) {
                if (c == '\\' && quote == '"' && i + 1 < input.length()) {
                    char next = input.charAt(i + 1);
                    // Inside double quotes, backslash only escapes a small set.
                    if (next == '"' || next == '\\' || next == '$' || next == '`' || next == '\n') {
                        current.append(next);
                        i++;
                        continue;
                    }
                    current.append(c);
                } else if (c == quote) {
                    quote = 0;
                } else {
                    current.append(c);
                }
                continue;
            }

            if (c == '\'' || c == '"') {
                quote = c;
                inToken = true;
                continue;
            }
            if (c == '\\' && i + 1 < input.length()) {
                current.append(input.charAt(++i));
                inToken = true;
                continue;
            }
            if (Character.isWhitespace(c)) {
                if (inToken) {
                    out.add(current.toString());
                    current.setLength(0);
                    inToken = false;
                }
                continue;
            }
            current.append(c);
            inToken = true;
        }
        if (inToken) {
            out.add(current.toString());
        }
        return out;
    }

    private static void addHeader(APIRequest request, String headerSpec) {
        int colon = headerSpec.indexOf(':');
        if (colon < 0) {
            return; // malformed – ignore
        }
        String name = headerSpec.substring(0, colon).trim();
        String value = headerSpec.substring(colon + 1).trim();
        if (name.isEmpty()) {
            return;
        }
        request.getHeaders().add(new KeyValuePair(name, value));
    }

    private static void appendData(StringBuilder buf, String data) {
        if (buf.length() > 0) {
            buf.append('&');
        }
        buf.append(data);
    }

    private static APIRequest.HttpMethod toHttpMethod(String value) {
        try {
            return APIRequest.HttpMethod.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return APIRequest.HttpMethod.GET;
        }
    }

    private static RequestBody.RawFormat detectRawFormat(String content, List<KeyValuePair> headers) {
        if (headers != null) {
            for (KeyValuePair h : headers) {
                if (h.getKey() != null && "content-type".equalsIgnoreCase(h.getKey())) {
                    String v = h.getValue() == null ? "" : h.getValue().toLowerCase();
                    if (v.contains("json")) return RequestBody.RawFormat.JSON;
                    if (v.contains("xml")) return RequestBody.RawFormat.XML;
                    if (v.contains("html")) return RequestBody.RawFormat.HTML;
                    if (v.contains("javascript")) return RequestBody.RawFormat.JAVASCRIPT;
                    if (v.contains("text/plain")) return RequestBody.RawFormat.TEXT;
                }
            }
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return RequestBody.RawFormat.JSON;
        }
        if (trimmed.startsWith("<")) {
            return RequestBody.RawFormat.XML;
        }
        return RequestBody.RawFormat.TEXT;
    }

    private static void applyUrl(APIRequest request, String url) {
        // Split off the query string so the URL field stays clean and params
        // populate the table – matches Postman's behaviour.
        int q = url.indexOf('?');
        if (q < 0) {
            request.setUrl(url);
            return;
        }
        request.setUrl(url.substring(0, q));
        String query = url.substring(q + 1);
        if (query.isEmpty()) {
            return;
        }
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            String k = eq >= 0 ? pair.substring(0, eq) : pair;
            String v = eq >= 0 ? pair.substring(eq + 1) : "";
            request.getQueryParams().add(new KeyValuePair(urlDecode(k), urlDecode(v)));
        }
    }

    private static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private static String stripWrappingQuotes(String s) {
        if (s.length() >= 2) {
            char first = s.charAt(0);
            char last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    /**
     * If the parsed headers contain an {@code Authorization} header that maps to a
     * supported scheme, hoist it into the typed {@link AuthConfig} and drop the
     * raw header – this gives a nicer experience in the Auth tab.
     */
    private static void promoteAuthFromHeaders(APIRequest request) {
        List<KeyValuePair> headers = request.getHeaders();
        if (headers == null) return;
        for (int i = 0; i < headers.size(); i++) {
            KeyValuePair h = headers.get(i);
            if (h.getKey() == null || !"authorization".equalsIgnoreCase(h.getKey())) continue;
            String value = h.getValue() == null ? "" : h.getValue().trim();
            if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
                request.setAuth(AuthConfig.bearer(value.substring(7).trim()));
                headers.remove(i);
                return;
            }
            if (value.regionMatches(true, 0, "Basic ", 0, 6)) {
                String encoded = value.substring(6).trim();
                try {
                    String decoded = new String(Base64.getDecoder().decode(encoded));
                    int colon = decoded.indexOf(':');
                    if (colon >= 0) {
                        request.setAuth(AuthConfig.basic(decoded.substring(0, colon),
                                decoded.substring(colon + 1)));
                        headers.remove(i);
                        return;
                    }
                } catch (IllegalArgumentException ignore) {
                    // Not valid base64 – leave the header intact.
                }
            }
            // Unknown scheme – leave as-is.
            return;
        }
    }
}
