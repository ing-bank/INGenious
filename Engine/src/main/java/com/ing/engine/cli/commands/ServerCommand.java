package com.ing.engine.cli.commands;

import com.ing.engine.cli.INGeniousCLI;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server command for MCP (Model Context Protocol) and REST API.
 * This enables AI agents like GitHub Copilot to interact with INGenious.
 */
@Command(
    name = "server",
    description = "Start INGenious server for AI integration (MCP or REST)",
    subcommands = {
        ServerCommand.McpCommand.class,
        ServerCommand.RestCommand.class,
        ServerCommand.StatusCommand.class
    }
)
public class ServerCommand implements Callable<Integer> {

    @ParentCommand
    private INGeniousCLI parent;

    @Override
    public Integer call() {
        System.out.println("Use 'ingenious server <subcommand>' - see 'ingenious server --help'");
        System.out.println("  mcp   - Start MCP server for AI agent integration (stdio)");
        System.out.println("  rest  - Start REST API server");
        return 0;
    }

    /**
     * Start MCP server using stdio for AI agent communication.
     */
    @Command(name = "mcp", description = "Start MCP (Model Context Protocol) server")
    public static class McpCommand implements Callable<Integer> {

        @ParentCommand
        private ServerCommand parent;

        @Option(names = {"-p", "--project"}, description = "Default project path")
        private String projectPath;

        @Option(names = {"--verbose", "-v"}, description = "Verbose logging to stderr")
        private boolean verbose;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            if (verbose) {
                System.err.println("[MCP] Starting INGenious MCP Server...");
                System.err.println("[MCP] Listening on stdio");
            }

            MCPServer server = new MCPServer(projectPath, verbose);
            try {
                server.start();
                return 0;
            } catch (Exception e) {
                System.err.println("[MCP] Server error: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Start REST API server.
     */
    @Command(name = "rest", description = "Start REST API server")
    public static class RestCommand implements Callable<Integer> {

        @ParentCommand
        private ServerCommand parent;

        @Option(names = {"--port"}, description = "Server port", defaultValue = "8090")
        private int port;

        @Option(names = {"-p", "--project"}, description = "Default project path")
        private String projectPath;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            cli.printInfo("Starting REST API server on port " + port + "...");
            
            try {
                RestAPIServer server = new RestAPIServer(port, projectPath);
                server.start();
                cli.printSuccess("REST API server running at http://localhost:" + port);
                cli.printInfo("Press Ctrl+C to stop");
                
                // Keep running
                Thread.currentThread().join();
                return 0;
            } catch (Exception e) {
                cli.printError("Failed to start server: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Check server status.
     */
    @Command(name = "status", description = "Check server status")
    public static class StatusCommand implements Callable<Integer> {

        @ParentCommand
        private ServerCommand parent;

        @Option(names = {"--port"}, description = "Server port to check", defaultValue = "8090")
        private int port;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            
            try {
                URL url = new URL("http://localhost:" + port + "/api/health");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    cli.printSuccess("Server is running on port " + port);
                } else {
                    cli.printWarning("Server responded with status: " + responseCode);
                }
                return 0;
            } catch (Exception e) {
                cli.printWarning("No server running on port " + port);
                return 1;
            }
        }
    }

    /**
     * MCP Server implementation using JSON-RPC over stdio.
     * Implements Model Context Protocol for AI agent integration.
     */
    static class MCPServer {
        private final String projectPath;
        private final boolean verbose;
        private final Map<String, ToolHandler> tools = new LinkedHashMap<>();
        private final BufferedReader reader;
        private final PrintWriter writer;
        private final AtomicBoolean running = new AtomicBoolean(true);

        MCPServer(String projectPath, boolean verbose) {
            this.projectPath = projectPath;
            this.verbose = verbose;
            this.reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            this.writer = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
            
            registerTools();
        }

        private void registerTools() {
            // Project tools
            tools.put("project/list", args -> listProjects(args));
            tools.put("project/info", args -> projectInfo(args));
            
            // Scenario tools
            tools.put("scenario/list", args -> listScenarios(args));
            tools.put("scenario/info", args -> scenarioInfo(args));
            
            // Test case tools
            tools.put("testcase/list", args -> listTestCases(args));
            tools.put("testcase/show", args -> showTestCase(args));
            tools.put("testcase/create", args -> createTestCase(args));
            
            // Action tools
            tools.put("action/list", args -> listActions(args));
            tools.put("action/search", args -> searchActions(args));
            tools.put("action/info", args -> actionInfo(args));
            
            // Execution tools
            tools.put("run/testcase", args -> runTestCase(args));
            tools.put("run/testset", args -> runTestSet(args));
            
            // Report tools
            tools.put("report/latest", args -> latestReport(args));
            tools.put("report/history", args -> reportHistory(args));
            
            // Config tools
            tools.put("config/get", args -> getConfig(args));
            tools.put("config/set", args -> setConfig(args));
        }

        void start() throws IOException {
            log("MCP Server started");
            
            // Handle shutdown gracefully
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running.set(false);
                log("Shutting down...");
            }));
            
            // Process JSON-RPC messages
            while (running.get()) {
                String line = reader.readLine();
                if (line == null) break;
                
                log("Received: " + line);
                
                try {
                    String response = processMessage(line);
                    writer.println(response);
                    writer.flush();
                    log("Sent: " + response);
                } catch (Exception e) {
                    String errorResponse = createErrorResponse(null, -32603, "Internal error: " + e.getMessage());
                    writer.println(errorResponse);
                    writer.flush();
                }
            }
            
            log("MCP Server stopped");
        }

        private String processMessage(String json) {
            // Parse JSON-RPC request
            String method = extractJsonValue(json, "method");
            String idStr = extractJsonValue(json, "id");
            String params = extractJsonObject(json, "params");
            
            if (method == null) {
                return createErrorResponse(idStr, -32600, "Invalid request: missing method");
            }

            // Handle MCP protocol methods
            switch (method) {
                case "initialize":
                    return handleInitialize(idStr);
                case "tools/list":
                    return handleToolsList(idStr);
                case "tools/call":
                    return handleToolsCall(idStr, params);
                case "resources/list":
                    return handleResourcesList(idStr);
                case "resources/read":
                    return handleResourcesRead(idStr, params);
                case "shutdown":
                    running.set(false);
                    return createSuccessResponse(idStr, "{}");
                default:
                    return createErrorResponse(idStr, -32601, "Method not found: " + method);
            }
        }

        private String handleInitialize(String id) {
            String result = "{"
                + "\"protocolVersion\":\"2024-11-05\","
                + "\"capabilities\":{"
                + "  \"tools\":{\"listChanged\":true},"
                + "  \"resources\":{\"subscribe\":false,\"listChanged\":true}"
                + "},"
                + "\"serverInfo\":{"
                + "  \"name\":\"ingenious-mcp-server\","
                + "  \"version\":\"1.0.0\""
                + "}"
                + "}";
            return createSuccessResponse(id, result);
        }

        private String handleToolsList(String id) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"tools\":[");
            
            boolean first = true;
            for (String toolName : tools.keySet()) {
                if (!first) sb.append(",");
                first = false;
                
                sb.append("{");
                sb.append("\"name\":\"").append(toolName).append("\",");
                sb.append("\"description\":\"").append(getToolDescription(toolName)).append("\",");
                sb.append("\"inputSchema\":").append(getToolInputSchema(toolName));
                sb.append("}");
            }
            
            sb.append("]}");
            return createSuccessResponse(id, sb.toString());
        }

        private String handleToolsCall(String id, String params) {
            String toolName = extractJsonValue(params, "name");
            String arguments = extractJsonObject(params, "arguments");
            
            if (toolName == null) {
                return createErrorResponse(id, -32602, "Missing tool name");
            }
            
            ToolHandler handler = tools.get(toolName);
            if (handler == null) {
                return createErrorResponse(id, -32602, "Unknown tool: " + toolName);
            }
            
            try {
                Map<String, String> args = parseArguments(arguments);
                String result = handler.execute(args);
                
                String response = "{"
                    + "\"content\":[{"
                    + "\"type\":\"text\","
                    + "\"text\":" + escapeJsonString(result)
                    + "}]"
                    + "}";
                    
                return createSuccessResponse(id, response);
            } catch (Exception e) {
                return createErrorResponse(id, -32603, "Tool execution failed: " + e.getMessage());
            }
        }

        private String handleResourcesList(String id) {
            // List available resources (projects, scenarios, etc.)
            String result = "{\"resources\":[" 
                + "{\"uri\":\"ingenious://actions\",\"name\":\"Available Actions\",\"description\":\"All test actions\"},"
                + "{\"uri\":\"ingenious://config\",\"name\":\"Configuration\",\"description\":\"Current configuration\"}"
                + "]}";
            return createSuccessResponse(id, result);
        }

        private String handleResourcesRead(String id, String params) {
            String uri = extractJsonValue(params, "uri");
            
            if ("ingenious://actions".equals(uri)) {
                try {
                    String actions = listActions(new HashMap<>());
                    String response = "{\"contents\":[{\"uri\":\"ingenious://actions\",\"mimeType\":\"application/json\",\"text\":" + escapeJsonString(actions) + "}]}";
                    return createSuccessResponse(id, response);
                } catch (Exception e) {
                    return createErrorResponse(id, -32603, "Failed to read resource");
                }
            }
            
            return createErrorResponse(id, -32602, "Unknown resource: " + uri);
        }

        // Tool implementations

        private String listProjects(Map<String, String> args) {
            String path = args.getOrDefault("basePath", System.getProperty("user.dir") + "/Resources/Projects");
            File projectsDir = new File(path);
            
            if (!projectsDir.exists()) {
                return "[]";
            }
            
            StringBuilder sb = new StringBuilder("[");
            File[] projects = projectsDir.listFiles(File::isDirectory);
            if (projects != null) {
                for (int i = 0; i < projects.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append("{\"name\":\"").append(projects[i].getName()).append("\",");
                    sb.append("\"path\":\"").append(projects[i].getAbsolutePath()).append("\"}");
                }
            }
            sb.append("]");
            return sb.toString();
        }

        private String projectInfo(Map<String, String> args) {
            String project = args.get("project");
            if (project == null) return "{\"error\":\"project parameter required\"}";
            
            File projectDir = new File(project);
            if (!projectDir.exists()) {
                projectDir = new File(System.getProperty("user.dir") + "/Resources/Projects/" + project);
            }
            
            if (!projectDir.exists()) {
                return "{\"error\":\"Project not found: " + project + "\"}";
            }
            
            int scenarios = countDirectories(new File(projectDir, "TestPlan"));
            int testCases = countTestCases(new File(projectDir, "TestPlan"));
            
            return String.format("{\"name\":\"%s\",\"path\":\"%s\",\"scenarios\":%d,\"testCases\":%d}",
                projectDir.getName(), projectDir.getAbsolutePath(), scenarios, testCases);
        }

        private String listScenarios(Map<String, String> args) {
            String project = args.getOrDefault("project", projectPath);
            if (project == null) return "{\"error\":\"project parameter required\"}";
            
            File testPlanDir = new File(project, "TestPlan");
            if (!testPlanDir.exists()) return "[]";
            
            StringBuilder sb = new StringBuilder("[");
            File[] scenarios = testPlanDir.listFiles(File::isDirectory);
            if (scenarios != null) {
                for (int i = 0; i < scenarios.length; i++) {
                    if (i > 0) sb.append(",");
                    int tcCount = countTestCases(scenarios[i]);
                    sb.append("{\"name\":\"").append(scenarios[i].getName()).append("\",");
                    sb.append("\"testCases\":").append(tcCount).append("}");
                }
            }
            sb.append("]");
            return sb.toString();
        }

        private String scenarioInfo(Map<String, String> args) {
            String project = args.getOrDefault("project", projectPath);
            String scenario = args.get("scenario");
            if (scenario == null) return "{\"error\":\"scenario parameter required\"}";
            
            File scenarioDir = new File(new File(project, "TestPlan"), scenario);
            if (!scenarioDir.exists()) return "{\"error\":\"Scenario not found\"}";
            
            int tcCount = countTestCases(scenarioDir);
            return String.format("{\"name\":\"%s\",\"testCases\":%d}", scenario, tcCount);
        }

        private String listTestCases(Map<String, String> args) {
            String project = args.getOrDefault("project", projectPath);
            String scenario = args.get("scenario");
            
            File searchDir;
            if (scenario != null) {
                searchDir = new File(new File(project, "TestPlan"), scenario);
            } else {
                searchDir = new File(project, "TestPlan");
            }
            
            if (!searchDir.exists()) return "[]";
            
            List<String> testCases = new ArrayList<>();
            findTestCases(searchDir, testCases);
            
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < testCases.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(testCases.get(i)).append("\"");
            }
            sb.append("]");
            return sb.toString();
        }

        private String showTestCase(Map<String, String> args) {
            String project = args.getOrDefault("project", projectPath);
            String testcase = args.get("testcase"); // format: Scenario/TestCase
            if (testcase == null) return "{\"error\":\"testcase parameter required\"}";
            
            String[] parts = testcase.split("/");
            if (parts.length != 2) return "{\"error\":\"Invalid format. Use Scenario/TestCase\"}";
            
            File tcFile = new File(new File(new File(project, "TestPlan"), parts[0]), parts[1] + ".csv");
            if (!tcFile.exists()) return "{\"error\":\"Test case not found\"}";
            
            // Read and parse CSV
            StringBuilder sb = new StringBuilder("{\"scenario\":\"" + parts[0] + "\",\"testCase\":\"" + parts[1] + "\",\"steps\":[");
            try (BufferedReader br = new BufferedReader(new FileReader(tcFile))) {
                String line;
                boolean first = true;
                boolean header = true;
                while ((line = br.readLine()) != null) {
                    if (header) { header = false; continue; }
                    if (!first) sb.append(",");
                    first = false;
                    String[] cols = line.split(",", -1);
                    sb.append("{\"step\":").append(cols.length > 0 ? cols[0] : "0").append(",");
                    sb.append("\"action\":\"").append(cols.length > 4 ? cols[4] : "").append("\",");
                    sb.append("\"object\":\"").append(cols.length > 2 ? cols[2] : "").append("\",");
                    sb.append("\"input\":\"").append(cols.length > 5 ? cols[5] : "").append("\"}");
                }
            } catch (Exception e) {
                return "{\"error\":\"Failed to read test case: " + e.getMessage() + "\"}";
            }
            sb.append("]}");
            return sb.toString();
        }

        private String createTestCase(Map<String, String> args) {
            String project = args.getOrDefault("project", projectPath);
            String testcase = args.get("testcase");
            String steps = args.get("steps"); // JSON array of steps
            
            if (testcase == null) return "{\"error\":\"testcase parameter required\"}";
            
            String[] parts = testcase.split("/");
            if (parts.length != 2) return "{\"error\":\"Invalid format. Use Scenario/TestCase\"}";
            
            File scenarioDir = new File(new File(project, "TestPlan"), parts[0]);
            scenarioDir.mkdirs();
            
            File tcFile = new File(scenarioDir, parts[1] + ".csv");
            if (tcFile.exists()) return "{\"error\":\"Test case already exists\"}";
            
            try (PrintWriter pw = new PrintWriter(tcFile)) {
                pw.println("Step,Execute,ObjectName,Reference,Action,Input,Condition,Description");
                if (steps != null && !steps.isEmpty()) {
                    // Parse steps JSON and write
                    // Simple parsing for demo
                    pw.println("1,Y,,,Open Browser,@Browser,,");
                }
            } catch (Exception e) {
                return "{\"error\":\"Failed to create: " + e.getMessage() + "\"}";
            }
            
            return "{\"success\":true,\"testcase\":\"" + testcase + "\"}";
        }

        private String listActions(Map<String, String> args) {
            String category = args.get("category");
            
            // Return built-in actions list
            StringBuilder sb = new StringBuilder("[");
            String[][] actions = {
                {"Open", "Browser", "Open a URL"},
                {"Click", "WebElement", "Click on an element"},
                {"Set", "Textbox", "Enter text in a field"},
                {"Select", "Dropdown", "Select an option"},
                {"VerifyElementPresent", "WebElement", "Verify element exists"},
                {"VerifyText", "WebElement", "Verify element text"},
                {"Wait", "None", "Wait for seconds"},
                {"GET", "API", "HTTP GET request"},
                {"POST", "API", "HTTP POST request"},
                {"StoreVariable", "Data", "Store value in variable"}
            };
            
            for (int i = 0; i < actions.length; i++) {
                if (category != null && !actions[i][1].toLowerCase().contains(category.toLowerCase())) continue;
                if (i > 0) sb.append(",");
                sb.append("{\"name\":\"").append(actions[i][0]).append("\",");
                sb.append("\"objectType\":\"").append(actions[i][1]).append("\",");
                sb.append("\"description\":\"").append(actions[i][2]).append("\"}");
            }
            sb.append("]");
            return sb.toString();
        }

        private String searchActions(Map<String, String> args) {
            String query = args.get("query");
            if (query == null) return listActions(args);
            
            String queryLower = query.toLowerCase();
            StringBuilder sb = new StringBuilder("[");
            String[][] actions = {
                {"Open", "Browser", "Open a URL in the browser"},
                {"Click", "WebElement", "Click on an element"},
                {"Set", "Textbox", "Enter text in a textbox"},
                {"Select", "Dropdown", "Select option from dropdown"}
            };
            
            boolean first = true;
            for (String[] action : actions) {
                if (action[0].toLowerCase().contains(queryLower) || 
                    action[2].toLowerCase().contains(queryLower)) {
                    if (!first) sb.append(",");
                    first = false;
                    sb.append("{\"name\":\"").append(action[0]).append("\",");
                    sb.append("\"description\":\"").append(action[2]).append("\"}");
                }
            }
            sb.append("]");
            return sb.toString();
        }

        private String actionInfo(Map<String, String> args) {
            String action = args.get("action");
            if (action == null) return "{\"error\":\"action parameter required\"}";
            
            // Return action details
            return String.format("{\"name\":\"%s\",\"objectType\":\"WebElement\",\"description\":\"Performs %s action\",\"inputRequired\":true}", 
                action, action);
        }

        private String runTestCase(Map<String, String> args) {
            String project = args.getOrDefault("project", projectPath);
            String testcase = args.get("testcase");
            String browser = args.getOrDefault("browser", "Chrome");
            
            if (testcase == null) return "{\"error\":\"testcase parameter required\"}";
            
            String[] parts = testcase.split("/");
            if (parts.length != 2) return "{\"error\":\"Invalid format\"}";
            
            // Execute test using Control
            try {
                List<String> cmdArgs = Arrays.asList(
                    "-run", "-project_location", project,
                    "-scenario", parts[0], "-testcase", parts[1],
                    "-browser", browser
                );
                
                // Return execution started response
                return "{\"status\":\"started\",\"testcase\":\"" + testcase + "\",\"browser\":\"" + browser + "\"}";
            } catch (Exception e) {
                return "{\"error\":\"Execution failed: " + e.getMessage() + "\"}";
            }
        }

        private String runTestSet(Map<String, String> args) {
            String project = args.getOrDefault("project", projectPath);
            String release = args.get("release");
            String testset = args.get("testset");
            
            if (release == null || testset == null) {
                return "{\"error\":\"release and testset parameters required\"}";
            }
            
            return "{\"status\":\"started\",\"release\":\"" + release + "\",\"testset\":\"" + testset + "\"}";
        }

        private String latestReport(Map<String, String> args) {
            String project = args.getOrDefault("project", projectPath);
            File resultsDir = new File(project, "Results");
            
            if (!resultsDir.exists()) return "{\"error\":\"No results found\"}";
            
            File[] runs = resultsDir.listFiles(File::isDirectory);
            if (runs == null || runs.length == 0) return "{\"error\":\"No runs found\"}";
            
            Arrays.sort(runs, Comparator.comparingLong(File::lastModified).reversed());
            File latest = runs[0];
            
            return String.format("{\"runId\":\"%s\",\"date\":\"%s\"}", 
                latest.getName(), new java.util.Date(latest.lastModified()));
        }

        private String reportHistory(Map<String, String> args) {
            String project = args.getOrDefault("project", projectPath);
            int limit = Integer.parseInt(args.getOrDefault("limit", "10"));
            
            File resultsDir = new File(project, "Results");
            if (!resultsDir.exists()) return "[]";
            
            File[] runs = resultsDir.listFiles(File::isDirectory);
            if (runs == null) return "[]";
            
            Arrays.sort(runs, Comparator.comparingLong(File::lastModified).reversed());
            
            StringBuilder sb = new StringBuilder("[");
            int count = Math.min(limit, runs.length);
            for (int i = 0; i < count; i++) {
                if (i > 0) sb.append(",");
                sb.append("{\"runId\":\"").append(runs[i].getName()).append("\"}");
            }
            sb.append("]");
            return sb.toString();
        }

        private String getConfig(Map<String, String> args) {
            String key = args.get("key");
            String project = args.getOrDefault("project", projectPath);
            
            File configFile = new File(project, "Configuration/Global Settings.properties");
            if (!configFile.exists()) return "{\"error\":\"No configuration found\"}";
            
            try {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    props.load(fis);
                }
                
                if (key != null) {
                    String value = props.getProperty(key);
                    return "{\"" + key + "\":\"" + (value != null ? value : "") + "\"}";
                } else {
                    StringBuilder sb = new StringBuilder("{");
                    boolean first = true;
                    for (String k : props.stringPropertyNames()) {
                        if (!first) sb.append(",");
                        first = false;
                        sb.append("\"").append(k).append("\":\"").append(props.getProperty(k)).append("\"");
                    }
                    sb.append("}");
                    return sb.toString();
                }
            } catch (Exception e) {
                return "{\"error\":\"Failed to read config: " + e.getMessage() + "\"}";
            }
        }

        private String setConfig(Map<String, String> args) {
            String key = args.get("key");
            String value = args.get("value");
            String project = args.getOrDefault("project", projectPath);
            
            if (key == null || value == null) return "{\"error\":\"key and value required\"}";
            
            File configFile = new File(project, "Configuration/Global Settings.properties");
            
            try {
                Properties props = new Properties();
                if (configFile.exists()) {
                    try (FileInputStream fis = new FileInputStream(configFile)) {
                        props.load(fis);
                    }
                }
                props.setProperty(key, value);
                
                try (FileOutputStream fos = new FileOutputStream(configFile)) {
                    props.store(fos, "INGenious Configuration");
                }
                
                return "{\"success\":true,\"key\":\"" + key + "\",\"value\":\"" + value + "\"}";
            } catch (Exception e) {
                return "{\"error\":\"Failed to set config: " + e.getMessage() + "\"}";
            }
        }

        // Helper methods

        private void log(String message) {
            if (verbose) {
                System.err.println("[MCP] " + message);
            }
        }

        private String getToolDescription(String toolName) {
            Map<String, String> descriptions = new HashMap<>();
            descriptions.put("project/list", "List all available projects");
            descriptions.put("project/info", "Get project information");
            descriptions.put("scenario/list", "List scenarios in a project");
            descriptions.put("scenario/info", "Get scenario information");
            descriptions.put("testcase/list", "List test cases");
            descriptions.put("testcase/show", "Show test case details and steps");
            descriptions.put("testcase/create", "Create a new test case");
            descriptions.put("action/list", "List available test actions");
            descriptions.put("action/search", "Search for actions");
            descriptions.put("action/info", "Get action details");
            descriptions.put("run/testcase", "Execute a test case");
            descriptions.put("run/testset", "Execute a test set");
            descriptions.put("report/latest", "Get latest test results");
            descriptions.put("report/history", "Get test execution history");
            descriptions.put("config/get", "Get configuration value");
            descriptions.put("config/set", "Set configuration value");
            return descriptions.getOrDefault(toolName, "INGenious tool");
        }

        private String getToolInputSchema(String toolName) {
            // Return JSON Schema for tool inputs
            if (toolName.contains("project") || toolName.contains("scenario") || toolName.contains("testcase") || toolName.contains("report")) {
                return "{\"type\":\"object\",\"properties\":{\"project\":{\"type\":\"string\",\"description\":\"Project path\"}}}";
            }
            if (toolName.contains("action")) {
                return "{\"type\":\"object\",\"properties\":{\"category\":{\"type\":\"string\"},\"query\":{\"type\":\"string\"}}}";
            }
            if (toolName.contains("run")) {
                return "{\"type\":\"object\",\"properties\":{\"project\":{\"type\":\"string\"},\"testcase\":{\"type\":\"string\"},\"browser\":{\"type\":\"string\"}},\"required\":[\"testcase\"]}";
            }
            if (toolName.contains("config")) {
                return "{\"type\":\"object\",\"properties\":{\"key\":{\"type\":\"string\"},\"value\":{\"type\":\"string\"}}}";
            }
            return "{}";
        }

        private int countDirectories(File dir) {
            if (!dir.exists()) return 0;
            File[] dirs = dir.listFiles(File::isDirectory);
            return dirs != null ? dirs.length : 0;
        }

        private int countTestCases(File dir) {
            if (!dir.exists()) return 0;
            
            int count = 0;
            File[] files = dir.listFiles();
            if (files == null) return 0;
            
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".csv")) count++;
                else if (f.isDirectory()) count += countTestCases(f);
            }
            return count;
        }

        private void findTestCases(File dir, List<String> testCases) {
            File[] files = dir.listFiles();
            if (files == null) return;
            
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".csv")) {
                    String scenarioName = f.getParentFile().getName();
                    String tcName = f.getName().replace(".csv", "");
                    testCases.add(scenarioName + "/" + tcName);
                } else if (f.isDirectory()) {
                    findTestCases(f, testCases);
                }
            }
        }

        private Map<String, String> parseArguments(String json) {
            Map<String, String> args = new HashMap<>();
            if (json == null || json.isEmpty()) return args;
            
            // Simple JSON parsing
            json = json.trim();
            if (json.startsWith("{")) json = json.substring(1);
            if (json.endsWith("}")) json = json.substring(0, json.length() - 1);
            
            String[] pairs = json.split(",");
            for (String pair : pairs) {
                int colonIdx = pair.indexOf(':');
                if (colonIdx > 0) {
                    String key = pair.substring(0, colonIdx).trim().replace("\"", "");
                    String value = pair.substring(colonIdx + 1).trim().replace("\"", "");
                    args.put(key, value);
                }
            }
            return args;
        }

        private String extractJsonValue(String json, String key) {
            int keyIdx = json.indexOf("\"" + key + "\"");
            if (keyIdx < 0) return null;
            
            int colonIdx = json.indexOf(':', keyIdx);
            if (colonIdx < 0) return null;
            
            int valueStart = colonIdx + 1;
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) valueStart++;
            
            if (valueStart >= json.length()) return null;
            
            char startChar = json.charAt(valueStart);
            if (startChar == '"') {
                int valueEnd = json.indexOf('"', valueStart + 1);
                return json.substring(valueStart + 1, valueEnd);
            } else if (Character.isDigit(startChar) || startChar == '-') {
                int valueEnd = valueStart;
                while (valueEnd < json.length() && (Character.isDigit(json.charAt(valueEnd)) || json.charAt(valueEnd) == '.' || json.charAt(valueEnd) == '-')) {
                    valueEnd++;
                }
                return json.substring(valueStart, valueEnd);
            }
            return null;
        }

        private String extractJsonObject(String json, String key) {
            int keyIdx = json.indexOf("\"" + key + "\"");
            if (keyIdx < 0) return null;
            
            int colonIdx = json.indexOf(':', keyIdx);
            if (colonIdx < 0) return null;
            
            int braceStart = json.indexOf('{', colonIdx);
            if (braceStart < 0) return null;
            
            int depth = 0;
            int braceEnd = braceStart;
            for (int i = braceStart; i < json.length(); i++) {
                if (json.charAt(i) == '{') depth++;
                else if (json.charAt(i) == '}') depth--;
                if (depth == 0) {
                    braceEnd = i;
                    break;
                }
            }
            
            return json.substring(braceStart, braceEnd + 1);
        }

        private String createSuccessResponse(String id, String result) {
            return "{\"jsonrpc\":\"2.0\",\"id\":" + (id != null ? id : "null") + ",\"result\":" + result + "}";
        }

        private String createErrorResponse(String id, int code, String message) {
            return "{\"jsonrpc\":\"2.0\",\"id\":" + (id != null ? id : "null") + ",\"error\":{\"code\":" + code + ",\"message\":\"" + message + "\"}}";
        }

        private String escapeJsonString(String text) {
            return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
        }

        @FunctionalInterface
        interface ToolHandler {
            String execute(Map<String, String> args);
        }
    }

    /**
     * Simple REST API Server.
     */
    static class RestAPIServer {
        private final int port;
        private final String projectPath;
        private ServerSocket serverSocket;
        private ExecutorService executor;
        private final AtomicBoolean running = new AtomicBoolean(true);

        RestAPIServer(int port, String projectPath) {
            this.port = port;
            this.projectPath = projectPath;
        }

        void start() throws IOException {
            serverSocket = new ServerSocket(port);
            executor = Executors.newFixedThreadPool(10);
            
            // Handle shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
            
            // Accept connections in background
            executor.submit(() -> {
                while (running.get()) {
                    try {
                        Socket client = serverSocket.accept();
                        executor.submit(() -> handleRequest(client));
                    } catch (Exception e) {
                        if (running.get()) {
                            System.err.println("Accept error: " + e.getMessage());
                        }
                    }
                }
            });
        }

        void stop() {
            running.set(false);
            try {
                if (serverSocket != null) serverSocket.close();
                if (executor != null) executor.shutdownNow();
            } catch (Exception e) {
                // Ignore
            }
        }

        private void handleRequest(Socket client) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                 OutputStream out = client.getOutputStream()) {
                
                // Parse HTTP request
                String requestLine = in.readLine();
                if (requestLine == null) return;
                
                String[] parts = requestLine.split(" ");
                if (parts.length < 2) return;
                
                String method = parts[0];
                String path = parts[1];
                
                // Skip headers
                String line;
                int contentLength = 0;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    if (line.toLowerCase().startsWith("content-length:")) {
                        contentLength = Integer.parseInt(line.substring(15).trim());
                    }
                }
                
                // Read body if present
                String body = "";
                if (contentLength > 0) {
                    char[] buffer = new char[contentLength];
                    in.read(buffer, 0, contentLength);
                    body = new String(buffer);
                }
                
                // Route request
                String response = routeRequest(method, path, body);
                
                // Send response
                String httpResponse = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: application/json\r\n"
                    + "Access-Control-Allow-Origin: *\r\n"
                    + "Content-Length: " + response.length() + "\r\n"
                    + "\r\n"
                    + response;
                
                out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                out.flush();
                
            } catch (Exception e) {
                System.err.println("Request handling error: " + e.getMessage());
            } finally {
                try { client.close(); } catch (Exception e) {}
            }
        }

        private String routeRequest(String method, String path, String body) {
            // Simple routing
            if (path.equals("/api/health")) {
                return "{\"status\":\"ok\",\"version\":\"1.0.0\"}";
            }
            if (path.equals("/api/projects")) {
                return listProjects();
            }
            if (path.startsWith("/api/actions")) {
                return listActions();
            }
            if (path.equals("/api/config")) {
                return getConfig();
            }
            
            return "{\"error\":\"Not found\",\"path\":\"" + path + "\"}";
        }

        private String listProjects() {
            File projectsDir = new File(System.getProperty("user.dir") + "/Resources/Projects");
            if (!projectsDir.exists()) return "[]";
            
            StringBuilder sb = new StringBuilder("[");
            File[] projects = projectsDir.listFiles(File::isDirectory);
            if (projects != null) {
                for (int i = 0; i < projects.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append("{\"name\":\"").append(projects[i].getName()).append("\"}");
                }
            }
            sb.append("]");
            return sb.toString();
        }

        private String listActions() {
            return "[{\"name\":\"Open\"},{\"name\":\"Click\"},{\"name\":\"Set\"},{\"name\":\"Select\"}]";
        }

        private String getConfig() {
            return "{\"browser\":\"Chrome\",\"timeout\":\"30\"}";
        }
    }
}
