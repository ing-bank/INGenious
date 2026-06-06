package com.ing.datalib.api.importer.bruno;

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
import com.ing.datalib.api.importer.bruno.BrunoParser.Block;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses a Bruno collection (folder with {@code bruno.json} and {@code *.bru} files)
 * into a {@link NormalizedCollection}.
 */
public class BrunoImporter implements CollectionImporter {

    @Override
    public ImportSource source() {
        return ImportSource.BRUNO;
    }

    @Override
    public boolean supports(File fileOrDir) {
        if (fileOrDir == null) return false;
        File root = resolveRoot(fileOrDir);
        return root != null && new File(root, "bruno.json").isFile();
    }

    private static File resolveRoot(File fileOrDir) {
        if (fileOrDir == null) return null;
        File f = fileOrDir.getAbsoluteFile();
        if (f.isFile()) f = f.getParentFile();
        while (f != null) {
            if (new File(f, "bruno.json").isFile()) return f;
            f = f.getParentFile();
        }
        return null;
    }

    @Override
    public NormalizedCollection parse(File fileOrDir, List<ImportWarning> warnings) throws ImportException {
        File root = resolveRoot(fileOrDir);
        if (root == null) {
            throw new ImportException("No bruno.json found in or above: " + fileOrDir);
        }

        String collectionName = root.getName();
        try {
            String brunoJson = new String(Files.readAllBytes(new File(root, "bruno.json").toPath()));
            Matcher m = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"").matcher(brunoJson);
            if (m.find()) collectionName = m.group(1);
        } catch (IOException ignored) {
            // fall back to folder name
        }

        NormalizedCollection nc = new NormalizedCollection(collectionName, ImportSource.BRUNO);

        // Walk filesystem: every .bru file (excluding environments/) is a request
        Path rootPath = root.toPath();
        Path envDir = rootPath.resolve("environments");
        try (Stream<Path> stream = Files.walk(rootPath)) {
            List<Path> bruFiles = stream
                    .filter(p -> p.toString().toLowerCase(Locale.ROOT).endsWith(".bru"))
                    .filter(p -> !p.startsWith(envDir))
                    .sorted()
                    .collect(Collectors.toList());
            for (Path p : bruFiles) {
                NormalizedRequest nr = parseRequestFile(p, rootPath, warnings);
                if (nr != null) nc.getRequests().add(nr);
            }
        } catch (IOException e) {
            throw new ImportException("Failed to walk Bruno collection: " + e.getMessage(), e);
        }

        // Environments
        if (Files.isDirectory(envDir)) {
            try (Stream<Path> s = Files.list(envDir)) {
                s.filter(p -> p.toString().endsWith(".bru")).forEach(p -> {
                    try {
                        nc.getEnvironments().add(parseEnvironmentFile(p));
                    } catch (IOException e) {
                        warnings.add(ImportWarning.warn("environments/" + p.getFileName(),
                                "Failed to read environment: " + e.getMessage()));
                    }
                });
            } catch (IOException ignored) {
                // no envs
            }
        }

        return nc;
    }

    private NormalizedRequest parseRequestFile(Path file, Path root, List<ImportWarning> warnings) {
        List<Block> blocks;
        try {
            blocks = BrunoParser.parseFile(file);
        } catch (IOException e) {
            warnings.add(ImportWarning.warn(root.relativize(file).toString(),
                    "Failed to read: " + e.getMessage()));
            return null;
        }

        Path rel = root.relativize(file);
        List<String> folderPath = new ArrayList<>();
        for (int i = 0; i < rel.getNameCount() - 1; i++) {
            folderPath.add(rel.getName(i).toString());
        }
        String location = rel.toString();

        APIRequest req = new APIRequest();
        // Default name from file
        String fileName = file.getFileName().toString();
        if (fileName.endsWith(".bru")) fileName = fileName.substring(0, fileName.length() - 4);
        req.setName(fileName);

        NormalizedRequest nr = new NormalizedRequest(folderPath, req);

        // Iterate blocks
        for (Block b : blocks) {
            String name = b.name;
            switch (name) {
                case "meta":
                    if (b.entries.containsKey("name")) req.setName(b.entries.get("name"));
                    break;
                case "get": case "post": case "put": case "patch": case "delete":
                case "head": case "options":
                    req.setMethod(parseMethod(name));
                    if (b.entries.containsKey("url")) {
                        req.setUrl(ImportUtils.rewriteVariables(b.entries.get("url")));
                    }
                    break;
                case "headers":
                    for (Map.Entry<String, String> e : b.entries.entrySet()) {
                        req.getHeaders().add(new KeyValuePair(
                                ImportUtils.rewriteVariables(e.getKey()),
                                ImportUtils.rewriteVariables(e.getValue()), true));
                    }
                    break;
                case "query":
                case "params:query":
                    for (Map.Entry<String, String> e : b.entries.entrySet()) {
                        req.getQueryParams().add(new KeyValuePair(
                                ImportUtils.rewriteVariables(e.getKey()),
                                ImportUtils.rewriteVariables(e.getValue()), true));
                    }
                    break;
                case "body:json": case "body:text": case "body:xml":
                case "body:graphql": case "body:sparql": {
                    RequestBody body = new RequestBody();
                    body.setBodyType(RequestBody.BodyType.RAW);
                    body.setRawFormat(mapRawFormat(name.substring("body:".length())));
                    body.setRawContent(ImportUtils.rewriteVariables(b.raw.trim()));
                    req.setBody(body);
                    break;
                }
                case "body:multipartForm": {
                    RequestBody body = new RequestBody();
                    body.setBodyType(RequestBody.BodyType.FORM_DATA);
                    for (Map.Entry<String, String> e : b.entries.entrySet()) {
                        body.getFormData().add(new KeyValuePair(
                                ImportUtils.rewriteVariables(e.getKey()),
                                ImportUtils.rewriteVariables(e.getValue()), true));
                    }
                    req.setBody(body);
                    break;
                }
                case "body:formUrlEncoded": {
                    RequestBody body = new RequestBody();
                    body.setBodyType(RequestBody.BodyType.URL_ENCODED);
                    for (Map.Entry<String, String> e : b.entries.entrySet()) {
                        body.getUrlEncodedData().add(new KeyValuePair(
                                ImportUtils.rewriteVariables(e.getKey()),
                                ImportUtils.rewriteVariables(e.getValue()), true));
                    }
                    req.setBody(body);
                    break;
                }
                case "auth:basic": {
                    req.setAuth(AuthConfig.basic(
                            ImportUtils.rewriteVariables(b.entries.getOrDefault("username", "")),
                            ImportUtils.rewriteVariables(b.entries.getOrDefault("password", ""))));
                    break;
                }
                case "auth:bearer": {
                    req.setAuth(AuthConfig.bearer(
                            ImportUtils.rewriteVariables(b.entries.getOrDefault("token", ""))));
                    break;
                }
                case "auth:apikey": {
                    String k = b.entries.getOrDefault("key", "X-API-Key");
                    String v = b.entries.getOrDefault("value", "");
                    String placement = b.entries.getOrDefault("placement", "header");
                    AuthConfig.ApiKeyLocation locEnum = "queryparams".equalsIgnoreCase(placement)
                            ? AuthConfig.ApiKeyLocation.QUERY_PARAM
                            : AuthConfig.ApiKeyLocation.HEADER;
                    if (locEnum == AuthConfig.ApiKeyLocation.QUERY_PARAM) {
                        warnings.add(ImportWarning.warn(location,
                                "API key in query string — only header injection is generated automatically."));
                    }
                    req.setAuth(AuthConfig.apiKey(
                            ImportUtils.rewriteVariables(k),
                            ImportUtils.rewriteVariables(v), locEnum));
                    break;
                }
                case "assert":
                    translateAsserts(b.entries, req);
                    break;
                case "tests":
                case "script:post-response":
                    nr.setTestScript(b.raw);
                    warnings.add(ImportWarning.info(location,
                            "Bruno '" + name + "' block preserved verbatim — review manually."));
                    break;
                case "script:pre-request":
                    nr.setPreRequestScript(b.raw);
                    warnings.add(ImportWarning.info(location,
                            "Bruno pre-request script preserved verbatim — not executed in INGenious."));
                    break;
                case "vars:pre-request":
                case "vars:post-response":
                    warnings.add(ImportWarning.info(location,
                            "Bruno '" + name + "' block — variables are not set automatically."));
                    break;
                default:
                    if (name.startsWith("auth:")) {
                        warnings.add(ImportWarning.warn(location,
                                "Auth type '" + name + "' not converted — manual configuration required."));
                    }
            }
        }

        if (req.getUrl() == null || req.getUrl().isEmpty()) {
            warnings.add(ImportWarning.warn(location, "Request has no URL — skipped."));
            return null;
        }
        return nr;
    }

    private static void translateAsserts(Map<String, String> entries, APIRequest req) {
        for (Map.Entry<String, String> e : entries.entrySet()) {
            String key = e.getKey();
            String val = e.getValue();
            // Examples: res.status: eq 200    res.body.json.id: eq 42
            String[] parts = val.split("\\s+", 2);
            String op = parts.length > 0 ? parts[0] : "eq";
            String expected = parts.length > 1 ? parts[1] : "";
            if (key.equals("res.status")) {
                if ("eq".equalsIgnoreCase(op)) {
                    try {
                        req.getAssertions().add(APIAssertion.statusCode(Integer.parseInt(expected.trim())));
                    } catch (NumberFormatException ignored) {}
                }
            } else if (key.startsWith("res.body")) {
                APIAssertion a = new APIAssertion();
                a.setType(APIAssertion.AssertionType.JSON_PATH);
                a.setOperator("contains".equalsIgnoreCase(op)
                        ? APIAssertion.Operator.CONTAINS : APIAssertion.Operator.EQUALS);
                a.setTarget(key.substring("res.body".length()));
                a.setExpectedValue(stripQuotes(expected.trim()));
                req.getAssertions().add(a);
            }
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

    private static APIRequest.HttpMethod parseMethod(String name) {
        try {
            return APIRequest.HttpMethod.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return APIRequest.HttpMethod.GET;
        }
    }

    private static RequestBody.RawFormat mapRawFormat(String f) {
        switch (f.toLowerCase(Locale.ROOT)) {
            case "json": case "graphql": case "sparql": return RequestBody.RawFormat.JSON;
            case "xml": return RequestBody.RawFormat.XML;
            case "html": return RequestBody.RawFormat.HTML;
            default: return RequestBody.RawFormat.TEXT;
        }
    }

    private NormalizedEnvironment parseEnvironmentFile(Path file) throws IOException {
        String envName = file.getFileName().toString();
        if (envName.endsWith(".bru")) envName = envName.substring(0, envName.length() - 4);
        NormalizedEnvironment env = new NormalizedEnvironment(envName);
        List<Block> blocks = BrunoParser.parseFile(file);
        for (Block b : blocks) {
            if ("vars".equals(b.name) || "vars:secret".equals(b.name)) {
                boolean secret = "vars:secret".equals(b.name);
                for (Map.Entry<String, String> e : b.entries.entrySet()) {
                    env.getVariables().add(new NormalizedVariable(e.getKey(), e.getValue(), secret));
                }
            }
        }
        return env;
    }
}
