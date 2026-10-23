package com.ing.engine.aicli.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ing.engine.aicli.ai.ChatMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Conversation transcript: rolling in-memory window for AI context plus an
 * append-only {@code .ingenious/history.jsonl} for {@code /history}.
 */
public final class ConversationManager {
    private static final ObjectMapper M = new ObjectMapper();
    private static final int MAX_TURNS = 40;
    private static final int TAIL_FOR_AI = 10;

    private final Path historyFile;
    private final List<ChatMessage> turns = new ArrayList<>();

    public ConversationManager(Path baseDir) {
        this.historyFile = baseDir.resolve(".ingenious").resolve("history.jsonl");
    }

    public void addUser(String content) {
        add(ChatMessage.user(content));
    }

    public void addAssistant(String content) {
        add(ChatMessage.assistant(content));
    }

    private void add(ChatMessage m) {
        turns.add(m);
        while (turns.size() > MAX_TURNS) turns.remove(0);
        try {
            ObjectNode n = M.createObjectNode();
            n.put("ts", System.currentTimeMillis());
            n.put("role", m.role());
            n.put("content", m.content());
            Files.createDirectories(historyFile.getParent());
            Files.writeString(
                historyFile,
                n.toString() + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
            // transcript persistence is best-effort
        }
    }

    /** Recent turns for the AI context window (truncated for token budget). */
    public List<ChatMessage> tail() {
        int from = Math.max(0, turns.size() - TAIL_FOR_AI);
        List<ChatMessage> out = new ArrayList<>();
        for (ChatMessage m : turns.subList(from, turns.size())) {
            String c = m.content();
            out.add(c.length() > 2000 ? new ChatMessage(m.role(), c.substring(0, 2000) + "…") : m);
        }
        return out;
    }

    public List<ChatMessage> turns() {
        return turns;
    }

    public void clear() {
        turns.clear();
    }
}
