package com.ing.ide.main.mainui.components.aichat.history;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ing.ide.main.mainui.components.aichat.model.ChatMessage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists AI assistant conversations to {@code ~/.ingenious/aichat/} as JSON,
 * one file per conversation, so past chats survive across IDE sessions and can
 * be browsed and reloaded.
 */
public final class ChatHistoryStore {
    private static final Logger LOG = Logger.getLogger(ChatHistoryStore.class.getName());

    private final ObjectMapper mapper = new ObjectMapper()
    .enable(SerializationFeature.INDENT_OUTPUT);
    private final Path dir;

    public ChatHistoryStore() {
        this(Path.of(System.getProperty("user.home"), ".ingenious", "aichat"));
    }

    public ChatHistoryStore(Path dir) {
        this.dir = dir;
    }

    /** A stored conversation. System messages are never persisted. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Conversation {
        public String id;
        public String title;
        public String model;
        public long createdAt;
        public long updatedAt;
        public List<ChatMessage> messages = new ArrayList<>();
    }

    /** Lightweight listing entry for the history menu. */
    public static final class Entry {
        public final String id;
        public final String title;
        public final long updatedAt;

        public Entry(String id, String title, long updatedAt) {
            this.id = id;
            this.title = title;
            this.updatedAt = updatedAt;
        }
    }

    public String newId() {
        return Long.toString(System.currentTimeMillis());
    }

    /** Saves (or overwrites) a conversation. Never throws. */
    public void save(Conversation conversation) {
        if (conversation == null || conversation.id == null) {
            return;
        }
        try {
            Files.createDirectories(dir);
            File f = dir.resolve(conversation.id + ".json").toFile();
            mapper.writeValue(f, conversation);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Could not save conversation " + conversation.id, ex);
        }
    }

    /** Loads a conversation by id, or {@code null} if missing/unreadable. */
    public Conversation load(String id) {
        try {
            File f = dir.resolve(id + ".json").toFile();
            if (!f.isFile()) {
                return null;
            }
            return mapper.readValue(f, Conversation.class);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Could not load conversation " + id, ex);
            return null;
        }
    }

    /** Lists saved conversations, most recently updated first. */
    public List<Entry> list() {
        List<Entry> entries = new ArrayList<>();
        File d = dir.toFile();
        File[] files = d.listFiles((f, name) -> name.endsWith(".json"));
        if (files == null) {
            return entries;
        }
        for (File f : files) {
            try {
                Conversation c = mapper.readValue(f, Conversation.class);
                if (c != null && c.id != null) {
                    entries.add(
                        new Entry(c.id, c.title == null ? "(untitled)" : c.title, c.updatedAt)
                    );
                }
            } catch (IOException ex) {
                LOG.log(Level.FINE, "Skipping unreadable history file: " + f, ex);
            }
        }
        entries.sort(Comparator.comparingLong((Entry e) -> e.updatedAt).reversed());
        return entries;
    }

    /** Deletes a stored conversation. Never throws. */
    public void delete(String id) {
        try {
            Files.deleteIfExists(dir.resolve(id + ".json"));
        } catch (IOException ex) {
            LOG.log(Level.FINE, "Could not delete conversation " + id, ex);
        }
    }
}
