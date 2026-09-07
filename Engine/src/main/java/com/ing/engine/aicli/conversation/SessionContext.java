package com.ing.engine.aicli.conversation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ing.engine.aicli.planning.Plan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Session memory: remembers the active project, framework, language, and
 * recently generated files across turns AND across sessions
 * ({@code <cwd>/.ingenious/session.json}), so the CLI never asks twice.
 */
public final class SessionContext {
    private static final ObjectMapper M = new ObjectMapper();

    private final Path file;
    private String project; // name or path as configured
    private String projectPath; // resolved absolute directory, may be null
    private String framework;
    private String language;
    private final List<String> recentFiles = new ArrayList<>();

    /** In-memory only: the plan awaiting /approve. */
    private transient Plan pendingPlan;

    private SessionContext(Path file) {
        this.file = file;
    }

    public static SessionContext load(Path baseDir) {
        SessionContext s = new SessionContext(
            baseDir.resolve(".ingenious").resolve("session.json")
        );
        try {
            if (Files.exists(s.file)) {
                JsonNode n = M.readTree(s.file.toFile());
                s.project = text(n, "project");
                s.projectPath = text(n, "projectPath");
                s.framework = text(n, "framework");
                s.language = text(n, "language");
                for (JsonNode f : n.path("recentFiles")) s.recentFiles.add(f.asText());
            }
        } catch (IOException ignored) {
            // start fresh
        }
        return s;
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isTextual() && !v.asText().isBlank() ? v.asText() : null;
    }

    public void save() {
        try {
            ObjectNode n = M.createObjectNode();
            if (project != null) n.put("project", project);
            if (projectPath != null) n.put("projectPath", projectPath);
            if (framework != null) n.put("framework", framework);
            if (language != null) n.put("language", language);
            ArrayNode arr = n.putArray("recentFiles");
            recentFiles.stream().limit(20).forEach(arr::add);
            Files.createDirectories(file.getParent());
            Files.writeString(file, n.toPrettyString());
        } catch (IOException ignored) {
            // session persistence is best-effort
        }
    }

    /** Set the active project, resolving its directory when possible. */
    public void setProject(String nameOrPath, Path cwd) {
        this.project = nameOrPath;
        this.projectPath = null;
        if (nameOrPath == null) return;
        Path p = Path.of(nameOrPath);
        if (p.isAbsolute() && Files.isDirectory(p)) {
            this.projectPath = p.toString();
            this.project = p.getFileName().toString();
        } else {
            Path candidate = cwd.resolve("Projects").resolve(nameOrPath);
            if (Files.isDirectory(candidate)) {
                this.projectPath = candidate.toAbsolutePath().toString();
            }
        }
    }

    /** Auto-select when exactly one project exists under ./Projects. */
    public void autoDetectProject(Path cwd) {
        if (project != null) return;
        Path projects = cwd.resolve("Projects");
        if (!Files.isDirectory(projects)) return;
        try (java.util.stream.Stream<Path> stream = Files.list(projects)) {
            List<Path> dirs = stream.filter(Files::isDirectory).toList();
            if (dirs.size() == 1) {
                setProject(dirs.get(0).getFileName().toString(), cwd);
            }
        } catch (IOException ignored) {
            // detection is best-effort
        }
    }

    public void rememberFiles(List<String> files) {
        for (String f : files) {
            recentFiles.remove(f);
            recentFiles.add(0, f);
        }
        while (recentFiles.size() > 20) recentFiles.remove(recentFiles.size() - 1);
    }

    /** Compact multi-line summary for /context and AI prompts. */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb
            .append("Project: ")
            .append(project == null ? "(none — set with /project)" : project)
            .append('\n');
        if (projectPath != null) {
            String cwd = System.getProperty("user.dir");
            String loc = projectPath;
            if (cwd != null && loc.startsWith(cwd + java.io.File.separator)) {
                loc = loc.substring(cwd.length() + 1);
            }
            sb.append("Location: ").append(loc).append('\n');
        }
        if (framework != null) sb.append("Framework: ").append(framework).append('\n');
        if (language != null) sb.append("Language: ").append(language).append('\n');
        if (!recentFiles.isEmpty()) {
            sb.append("Recent files:\n");
            recentFiles.stream().limit(8).forEach(f -> sb.append("  ").append(f).append('\n'));
        }
        return sb.toString().stripTrailing();
    }

    public void clearFacts() {
        project = null;
        projectPath = null;
        framework = null;
        language = null;
        recentFiles.clear();
        pendingPlan = null;
    }

    // accessors -----------------------------------------------------------

    public String project() {
        return project;
    }

    public String projectPath() {
        return projectPath;
    }

    public String framework() {
        return framework;
    }

    public void setFramework(String framework) {
        this.framework = framework;
    }

    public String language() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<String> recentFiles() {
        return recentFiles;
    }

    public Plan pendingPlan() {
        return pendingPlan;
    }

    public void setPendingPlan(Plan plan) {
        this.pendingPlan = plan;
    }
}
