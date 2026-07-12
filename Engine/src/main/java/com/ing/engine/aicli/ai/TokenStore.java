package com.ing.engine.aicli.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * Credential storage at {@code ~/.ingenious/credentials.json} with 0600
 * permissions (best effort on non-POSIX filesystems). Values are never
 * logged or echoed.
 */
public final class TokenStore {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path file;

    public TokenStore() {
        this(Path.of(System.getProperty("user.home"), ".ingenious", "credentials.json"));
    }

    public TokenStore(Path file) {
        this.file = file;
    }

    public synchronized String get(String key) {
        try {
            if (!Files.exists(file)) return null;
            JsonNode node = mapper.readTree(file.toFile()).path(key);
            return node.isTextual() ? node.asText() : null;
        } catch (IOException e) {
            return null;
        }
    }

    public synchronized void put(String key, String value) throws IOException {
        ObjectNode root;
        if (Files.exists(file)) {
            root = (ObjectNode) mapper.readTree(file.toFile());
        } else {
            root = mapper.createObjectNode();
        }
        if (value == null) {
            root.remove(key);
        } else {
            root.put(key, value);
        }
        Files.createDirectories(file.getParent());
        Files.writeString(file, root.toPrettyString());
        try {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException ignored) {
            // non-POSIX filesystem
        }
    }
}
