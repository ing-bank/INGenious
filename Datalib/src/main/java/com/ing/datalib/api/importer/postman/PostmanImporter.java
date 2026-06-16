package com.ing.datalib.api.importer.postman;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.datalib.api.APIAssertion;
import com.ing.datalib.api.APIRequest;
import com.ing.datalib.api.AuthConfig;
import com.ing.datalib.api.KeyValuePair;
import com.ing.datalib.api.RequestBody;
import com.ing.datalib.api.importer.ImportException;
import com.ing.datalib.api.importer.ImportSource;
import com.ing.datalib.api.importer.ImportUtils;
import com.ing.datalib.api.importer.ImportWarning;
import com.ing.datalib.api.importer.NormalizedCollection;
import com.ing.datalib.api.importer.NormalizedEnvironment;
import com.ing.datalib.api.importer.NormalizedRequest;
import com.ing.datalib.api.importer.NormalizedVariable;
import com.ing.datalib.api.importer.spi.CollectionImporter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Postman Collection v2.1 JSON file into a {@link NormalizedCollection}.
 * <p>
 * Best-effort: supports nested folders, all standard auth types (basic, bearer, apikey
 * — header location), raw / urlencoded / formdata / graphql / file bodies, URL string
 * or object form, and a small set of script patterns translated into assertions.
 */
public class PostmanImporter implements CollectionImporter {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern STATUS_PATTERN = Pattern.compile(
        "pm\\.response\\.to\\.have\\.status\\(\\s*(\\d{3})\\s*\\)"
    );
    private static final Pattern JSON_EQ_PATTERN = Pattern.compile(
        "pm\\.expect\\s*\\(\\s*pm\\.response\\.json\\(\\)([^)]+?)\\)\\s*\\.to\\.eql\\s*\\(\\s*([^)]+?)\\s*\\)"
    );
    private static final Pattern BODY_INCLUDE_PATTERN = Pattern.compile(
        "pm\\.expect\\s*\\(\\s*pm\\.response\\.text\\(\\)\\s*\\)\\s*\\.to\\.include\\s*\\(\\s*\"([^\"]+)\"\\s*\\)"
    );
    private static final Pattern ENV_SET_PATTERN = Pattern.compile(
        "pm\\.environment\\.set\\s*\\(\\s*\"([^\"]+)\"\\s*,\\s*pm\\.response\\.json\\(\\)([^)]+?)\\)"
    );

    @Override
    public ImportSource source() {
        return ImportSource.POSTMAN;
    }

    @Override
    public boolean supports(File fileOrDir) {
        if (fileOrDir == null || !fileOrDir.isFile()) return false;
        String name = fileOrDir.getName().toLowerCase(Locale.ROOT);
        return (
            name.endsWith(".postman_collection.json") ||
            (name.endsWith(".json") && peekIsPostman(fileOrDir))
        );
    }

    private static boolean peekIsPostman(File f) {
        try {
            JsonNode root = MAPPER.readTree(f);
            JsonNode info = root.path("info");
            JsonNode schema = info.path("schema");
            return schema.isTextual() && schema.asText().contains("schema.getpostman.com");
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Picks the most likely Postman collection file inside a folder. Prefers
     * names ending in {@code .postman_collection.json}; falls back to any
     * {@code *.json} that parses as a Postman collection.
     */
    private static File resolveCollectionInDir(File dir) {
        File[] direct = dir.listFiles(
            (d, name) -> name.toLowerCase().endsWith(".postman_collection.json")
        );
        if (direct != null && direct.length > 0) {
            return direct[0];
        }
        File[] jsons = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".json"));
        if (jsons != null) {
            for (File j : jsons) {
                if (peekIsPostman(j)) return j;
            }
        }
        return null;
    }

    @Override
    public NormalizedCollection parse(File file, List<ImportWarning> warnings)
        throws ImportException {
        if (file == null || !file.exists()) {
            throw new ImportException("Postman collection path does not exist: " + file);
        }
        // Allow the user to pick a folder — auto-resolve the collection JSON inside it.
        if (file.isDirectory()) {
            File resolved = resolveCollectionInDir(file);
            if (resolved == null) {
                throw new ImportException(
                    "No Postman collection (*.postman_collection.json) found in folder: " +
                    file.getAbsolutePath()
                );
            }
            warnings.add(
                ImportWarning.info(
                    file.getName(),
                    "Resolved Postman collection: " + resolved.getName()
                )
            );
            file = resolved;
        } else if (!file.isFile()) {
            throw new ImportException("Postman collection file does not exist: " + file);
        }
        // User picked an environment-only file: wrap it in an otherwise-empty NormalizedCollection
        if (file.getName().toLowerCase().endsWith(".postman_environment.json")) {
            try {
                NormalizedEnvironment env = parseEnvironment(file);
                NormalizedCollection envOnly = new NormalizedCollection(
                    env.getName(),
                    ImportSource.POSTMAN
                );
                envOnly.getEnvironments().add(env);
                warnings.add(
                    ImportWarning.info(
                        file.getName(),
                        "Postman environment file detected — no requests will be imported."
                    )
                );
                return envOnly;
            } catch (ImportException ex) {
                throw ex;
            }
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(file);
        } catch (IOException e) {
            throw new ImportException(
                "Failed to read Postman collection JSON: " + e.getMessage(),
                e
            );
        }

        JsonNode info = root.path("info");
        String schema = info.path("schema").asText("");
        // Accept Postman v2.0.0 and v2.1.0 (both common in exports). Warn only if the
        // schema looks completely unknown.
        boolean knownSchema = schema.contains("v2.1.0") || schema.contains("v2.0.0");
        if (!knownSchema) {
            warnings.add(
                ImportWarning.warn(
                    "info.schema",
                    "Unrecognised Postman schema '" +
                    schema +
                    "' — import will continue best-effort."
                )
            );
        }

        String collectionName = info.path("name").asText(file.getName());
        NormalizedCollection nc = new NormalizedCollection(collectionName, ImportSource.POSTMAN);
        nc.setDescription(info.path("description").asText(null));

        // Collection-level variables
        for (JsonNode v : root.path("variable")) {
            nc
                .getVariables()
                .add(
                    new NormalizedVariable(
                        v.path("key").asText(),
                        v.path("value").asText(""),
                        "secret".equalsIgnoreCase(v.path("type").asText(""))
                    )
                );
        }

        // Collection-level auth applies as default to requests with no auth of their own
        AuthConfig defaultAuth = parseAuth(root.path("auth"), warnings, "collection.auth");

        // Walk items recursively
        walkItems(root.path("item"), new ArrayList<>(), defaultAuth, nc, warnings);

        // Auto-discover sibling Postman environment files in the same folder.
        File parent = file.getParentFile();
        if (parent != null && parent.isDirectory()) {
            File[] envFiles = parent.listFiles(
                (d, n) -> n.toLowerCase().endsWith(".postman_environment.json")
            );
            if (envFiles != null) {
                for (File ef : envFiles) {
                    try {
                        NormalizedEnvironment env = parseEnvironment(ef);
                        nc.getEnvironments().add(env);
                        warnings.add(
                            ImportWarning.info(
                                ef.getName(),
                                "Imported environment '" +
                                env.getName() +
                                "' (" +
                                env.getVariables().size() +
                                " variables)."
                            )
                        );
                    } catch (ImportException ex) {
                        warnings.add(
                            ImportWarning.warn(
                                ef.getName(),
                                "Failed to parse environment: " + ex.getMessage()
                            )
                        );
                    }
                }
            }
        }

        return nc;
    }

    private void walkItems(
        JsonNode items,
        List<String> folderPath,
        AuthConfig inheritedAuth,
        NormalizedCollection nc,
        List<ImportWarning> warnings
    ) {
        if (items == null || !items.isArray()) return;
        for (JsonNode item : items) {
            if (item.has("item") && item.get("item").isArray()) {
                String folderName = item.path("name").asText("Folder");
                List<String> child = new ArrayList<>(folderPath);
                child.add(folderName);
                AuthConfig folderAuth = item.has("auth")
                    ? parseAuth(item.path("auth"), warnings, String.join("/", child) + ".auth")
                    : inheritedAuth;
                walkItems(item.path("item"), child, folderAuth, nc, warnings);
            } else {
                NormalizedRequest nr = parseRequest(item, folderPath, inheritedAuth, warnings);
                if (nr != null) {
                    nc.getRequests().add(nr);
                }
            }
        }
    }

    private NormalizedRequest parseRequest(
        JsonNode item,
        List<String> folderPath,
        AuthConfig inheritedAuth,
        List<ImportWarning> warnings
    ) {
        String name = item.path("name").asText("Unnamed Request");
        String location = (folderPath.isEmpty() ? "" : String.join("/", folderPath) + "/") + name;
        JsonNode requestNode = item.path("request");
        if (requestNode.isMissingNode()) {
            warnings.add(ImportWarning.warn(location, "Item has no 'request' — skipped."));
            return null;
        }

        APIRequest req = new APIRequest();
        req.setName(name);
        req.setMethod(parseMethod(requestNode.path("method").asText("GET")));
        req.setUrl(parseUrl(requestNode.path("url"), req, warnings, location));

        for (JsonNode h : requestNode.path("header")) {
            if (h.path("disabled").asBoolean(false)) continue;
            req
                .getHeaders()
                .add(
                    new KeyValuePair(
                        ImportUtils.rewriteVariables(h.path("key").asText()),
                        ImportUtils.rewriteVariables(h.path("value").asText("")),
                        true
                    )
                );
        }

        // Auth: per-request overrides inherited
        AuthConfig auth = requestNode.has("auth")
            ? parseAuth(requestNode.path("auth"), warnings, location + ".auth")
            : inheritedAuth;
        req.setAuth(auth == null ? new AuthConfig() : auth);

        // Body
        req.setBody(parseBody(requestNode.path("body"), warnings, location));

        NormalizedRequest nr = new NormalizedRequest(folderPath, req);

        // Scripts (events)
        for (JsonNode ev : item.path("event")) {
            String listen = ev.path("listen").asText("");
            JsonNode execNode = ev.path("script").path("exec");
            String script = joinScript(execNode);
            if (script.isEmpty()) continue;
            if ("test".equalsIgnoreCase(listen)) {
                nr.setTestScript(script);
                translateTestScript(script, req, warnings, location);
            } else if ("prerequest".equalsIgnoreCase(listen)) {
                nr.setPreRequestScript(script);
                warnings.add(
                    ImportWarning.info(
                        location,
                        "Pre-request script preserved verbatim (not executed in INGenious)."
                    )
                );
            }
        }

        return nr;
    }

    private static String joinScript(JsonNode execNode) {
        if (execNode == null || !execNode.isArray()) return "";
        StringBuilder sb = new StringBuilder();
        for (JsonNode line : execNode) {
            sb.append(line.asText("")).append('\n');
        }
        return sb.toString();
    }

    private APIRequest.HttpMethod parseMethod(String m) {
        try {
            return APIRequest.HttpMethod.valueOf(m.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return APIRequest.HttpMethod.GET;
        }
    }

    private String parseUrl(
        JsonNode urlNode,
        APIRequest req,
        List<ImportWarning> warnings,
        String loc
    ) {
        if (urlNode.isTextual()) {
            return ImportUtils.rewriteVariables(urlNode.asText());
        }
        if (urlNode.isObject()) {
            // Prefer the raw form when present
            String raw = urlNode.path("raw").asText(null);
            if (raw != null && !raw.isEmpty()) {
                // Capture query params for visibility (already in 'raw')
                for (JsonNode q : urlNode.path("query")) {
                    if (q.path("disabled").asBoolean(false)) continue;
                    req
                        .getQueryParams()
                        .add(
                            new KeyValuePair(
                                ImportUtils.rewriteVariables(q.path("key").asText()),
                                ImportUtils.rewriteVariables(q.path("value").asText("")),
                                true
                            )
                        );
                }
                return ImportUtils.rewriteVariables(raw);
            }
            // Reconstruct
            StringBuilder sb = new StringBuilder();
            String protocol = urlNode.path("protocol").asText("https");
            sb.append(protocol).append("://");
            JsonNode host = urlNode.path("host");
            if (host.isArray()) {
                List<String> parts = new ArrayList<>();
                host.forEach(n -> parts.add(n.asText()));
                sb.append(String.join(".", parts));
            } else if (host.isTextual()) {
                sb.append(host.asText());
            }
            JsonNode path = urlNode.path("path");
            if (path.isArray()) {
                for (JsonNode p : path) sb.append('/').append(p.asText());
            }
            return ImportUtils.rewriteVariables(sb.toString());
        }
        warnings.add(ImportWarning.warn(loc, "Request has no URL."));
        return "";
    }

    private RequestBody parseBody(JsonNode bodyNode, List<ImportWarning> warnings, String loc) {
        if (bodyNode == null || bodyNode.isMissingNode() || bodyNode.isNull()) {
            return new RequestBody();
        }
        String mode = bodyNode.path("mode").asText("");
        switch (mode) {
            case "raw":
                {
                    RequestBody b = new RequestBody();
                    b.setBodyType(RequestBody.BodyType.RAW);
                    String lang = bodyNode
                        .path("options")
                        .path("raw")
                        .path("language")
                        .asText("text");
                    b.setRawFormat(mapRawFormat(lang));
                    b.setRawContent(ImportUtils.rewriteVariables(bodyNode.path("raw").asText("")));
                    return b;
                }
            case "urlencoded":
                {
                    RequestBody b = new RequestBody();
                    b.setBodyType(RequestBody.BodyType.URL_ENCODED);
                    for (JsonNode kv : bodyNode.path("urlencoded")) {
                        if (kv.path("disabled").asBoolean(false)) continue;
                        b
                            .getUrlEncodedData()
                            .add(
                                new KeyValuePair(
                                    ImportUtils.rewriteVariables(kv.path("key").asText()),
                                    ImportUtils.rewriteVariables(kv.path("value").asText("")),
                                    true
                                )
                            );
                    }
                    return b;
                }
            case "formdata":
                {
                    RequestBody b = new RequestBody();
                    b.setBodyType(RequestBody.BodyType.FORM_DATA);
                    for (JsonNode kv : bodyNode.path("formdata")) {
                        if (kv.path("disabled").asBoolean(false)) continue;
                        String type = kv.path("type").asText("text");
                        if ("file".equalsIgnoreCase(type)) {
                            warnings.add(
                                ImportWarning.warn(
                                    loc,
                                    "form-data file field '" +
                                    kv.path("key").asText() +
                                    "' — file paths must be set manually."
                                )
                            );
                        }
                        b
                            .getFormData()
                            .add(
                                new KeyValuePair(
                                    ImportUtils.rewriteVariables(kv.path("key").asText()),
                                    ImportUtils.rewriteVariables(kv.path("value").asText("")),
                                    true
                                )
                            );
                    }
                    return b;
                }
            case "graphql":
                {
                    RequestBody b = new RequestBody();
                    b.setBodyType(RequestBody.BodyType.RAW);
                    b.setRawFormat(RequestBody.RawFormat.JSON);
                    String q = bodyNode.path("graphql").path("query").asText("");
                    String v = bodyNode.path("graphql").path("variables").asText("");
                    b.setRawContent(
                        ImportUtils.rewriteVariables(
                            "{\"query\":" +
                            jsonStr(q) +
                            ",\"variables\":" +
                            (v.isEmpty() ? "null" : v) +
                            "}"
                        )
                    );
                    return b;
                }
            case "file":
                {
                    RequestBody b = new RequestBody();
                    b.setBodyType(RequestBody.BodyType.BINARY);
                    b.setBinaryFilePath(bodyNode.path("file").path("src").asText(""));
                    warnings.add(
                        ImportWarning.warn(
                            loc,
                            "Binary file body — verify the path is reachable on the executor."
                        )
                    );
                    return b;
                }
            default:
                return new RequestBody();
        }
    }

    private static String jsonStr(String s) {
        if (s == null) return "\"\"";
        return (
            "\"" +
            s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") +
            "\""
        );
    }

    private static RequestBody.RawFormat mapRawFormat(String lang) {
        if (lang == null) return RequestBody.RawFormat.TEXT;
        switch (lang.toLowerCase(Locale.ROOT)) {
            case "json":
                return RequestBody.RawFormat.JSON;
            case "xml":
                return RequestBody.RawFormat.XML;
            case "html":
                return RequestBody.RawFormat.HTML;
            case "javascript":
                return RequestBody.RawFormat.JAVASCRIPT;
            default:
                return RequestBody.RawFormat.TEXT;
        }
    }

    private AuthConfig parseAuth(JsonNode authNode, List<ImportWarning> warnings, String loc) {
        if (authNode == null || authNode.isMissingNode() || authNode.isNull()) {
            return new AuthConfig();
        }
        String type = authNode.path("type").asText("noauth").toLowerCase(Locale.ROOT);
        switch (type) {
            case "noauth":
                return new AuthConfig();
            case "basic":
                {
                    String user = findAuthParam(authNode, "basic", "username");
                    String pass = findAuthParam(authNode, "basic", "password");
                    return AuthConfig.basic(
                        ImportUtils.rewriteVariables(user),
                        ImportUtils.rewriteVariables(pass)
                    );
                }
            case "bearer":
                {
                    String token = findAuthParam(authNode, "bearer", "token");
                    return AuthConfig.bearer(ImportUtils.rewriteVariables(token));
                }
            case "apikey":
                {
                    String key = findAuthParam(authNode, "apikey", "key");
                    String value = findAuthParam(authNode, "apikey", "value");
                    String in = findAuthParam(authNode, "apikey", "in"); // "header" or "query"
                    AuthConfig.ApiKeyLocation locEnum = "query".equalsIgnoreCase(in)
                        ? AuthConfig.ApiKeyLocation.QUERY_PARAM
                        : AuthConfig.ApiKeyLocation.HEADER;
                    if (locEnum == AuthConfig.ApiKeyLocation.QUERY_PARAM) {
                        warnings.add(
                            ImportWarning.warn(
                                loc,
                                "API key in query string — only header injection is generated automatically."
                            )
                        );
                    }
                    return AuthConfig.apiKey(
                        ImportUtils.rewriteVariables(key),
                        ImportUtils.rewriteVariables(value),
                        locEnum
                    );
                }
            default:
                warnings.add(
                    ImportWarning.warn(
                        loc,
                        "Auth type '" + type + "' not converted — manual configuration required."
                    )
                );
                return new AuthConfig();
        }
    }

    private static String findAuthParam(JsonNode authNode, String section, String key) {
        // Postman v2.1: auth.<section> is array of {key,value,type}
        JsonNode arr = authNode.path(section);
        if (arr.isArray()) {
            for (JsonNode kv : arr) {
                if (key.equals(kv.path("key").asText())) {
                    return kv.path("value").asText("");
                }
            }
        }
        // v2.0 style: auth.<section> is an object
        if (arr.isObject() && arr.has(key)) {
            return arr.path(key).asText("");
        }
        return "";
    }

    private void translateTestScript(
        String script,
        APIRequest req,
        List<ImportWarning> warnings,
        String loc
    ) {
        boolean anyTranslated = false;
        Matcher m = STATUS_PATTERN.matcher(script);
        while (m.find()) {
            req.getAssertions().add(APIAssertion.statusCode(Integer.parseInt(m.group(1))));
            anyTranslated = true;
        }
        m = JSON_EQ_PATTERN.matcher(script);
        while (m.find()) {
            String path = m.group(1).trim();
            String val = stripQuotes(m.group(2).trim());
            APIAssertion a = new APIAssertion();
            a.setType(APIAssertion.AssertionType.JSON_PATH);
            a.setOperator(APIAssertion.Operator.EQUALS);
            a.setTarget(path);
            a.setExpectedValue(val);
            req.getAssertions().add(a);
            anyTranslated = true;
        }
        m = BODY_INCLUDE_PATTERN.matcher(script);
        while (m.find()) {
            APIAssertion a = new APIAssertion();
            a.setType(APIAssertion.AssertionType.BODY_CONTAINS);
            a.setOperator(APIAssertion.Operator.CONTAINS);
            a.setExpectedValue(m.group(1));
            req.getAssertions().add(a);
            anyTranslated = true;
        }
        m = ENV_SET_PATTERN.matcher(script);
        while (m.find()) {
            warnings.add(
                ImportWarning.info(
                    loc,
                    "Detected pm.environment.set('" +
                    m.group(1) +
                    "', ...) — converted to TODO comment; " +
                    "use Webservice.storeJsonElement to set the variable."
                )
            );
        }
        if (!anyTranslated) {
            warnings.add(
                ImportWarning.info(
                    loc,
                    "Test script preserved verbatim — no recognised patterns to translate."
                )
            );
        }
    }

    private static String stripQuotes(String s) {
        if (s == null || s.length() < 2) return s;
        char c = s.charAt(0);
        if ((c == '"' || c == '\'') && s.charAt(s.length() - 1) == c) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    /**
     * Loads a Postman environment file ({@code *.postman_environment.json}).
     */
    public static NormalizedEnvironment parseEnvironment(File file) throws ImportException {
        try {
            JsonNode root = MAPPER.readTree(file);
            NormalizedEnvironment env = new NormalizedEnvironment(
                root.path("name").asText(file.getName())
            );
            for (JsonNode v : root.path("values")) {
                env
                    .getVariables()
                    .add(
                        new NormalizedVariable(
                            v.path("key").asText(),
                            v.path("value").asText(""),
                            "secret".equalsIgnoreCase(v.path("type").asText(""))
                        )
                    );
            }
            return env;
        } catch (IOException e) {
            throw new ImportException("Failed to read Postman environment: " + e.getMessage(), e);
        }
    }
}
