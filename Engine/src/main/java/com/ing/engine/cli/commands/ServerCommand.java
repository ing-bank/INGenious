package com.ing.engine.cli.commands;

import com.ing.engine.cli.INGeniousCLI;
import com.ing.engine.mcp.MCPServer;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Server commands – exposes INGenious to AI agents (MCP) and dashboards (REST).
 *
 * <p>The actual MCP protocol implementation lives in {@link com.ing.engine.mcp}
 * so the protocol code can be unit-tested independently of Picocli.
 */
@Command(
    name = "server",
    mixinStandardHelpOptions = true,
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
     * Start the MCP (Model Context Protocol) server on stdio so LLM agents
     * (GitHub Copilot, Claude Desktop, Cursor, etc.) can drive INGenious
     * via natural-language commands.
     */
    @Command(name = "mcp", description = "Start MCP (Model Context Protocol) server")
    public static class McpCommand implements Callable<Integer> {
        @ParentCommand
        private ServerCommand parent;

        @Option(
            names = { "-p", "--project" },
            description = "Default project (name or absolute path)"
        )
        private String projectPath;

        @Option(names = { "--verbose", "-v" }, description = "Verbose logging to stderr")
        private boolean verbose;

        @Override
        public Integer call() {
            // The MCP server is stdio-driven – we MUST keep stdout silent
            // except for JSON-RPC frames. Diagnostics go to stderr.
            if (verbose) {
                System.err.println("[mcp] starting INGenious MCP server on stdio");
                System.err.println(
                    "[mcp] default project: " + (projectPath != null ? projectPath : "<none>")
                );
            }
            try {
                new MCPServer(projectPath, verbose).start();
                return 0;
            } catch (Exception e) {
                System.err.println("[mcp] fatal: " + e.getMessage());
                if (verbose) e.printStackTrace(System.err);
                return 1;
            }
        }
    }

    /**
     * Start the small REST companion server (dashboards, health probes).
     */
    @Command(name = "rest", description = "Start REST API server")
    public static class RestCommand implements Callable<Integer> {
        @ParentCommand
        private ServerCommand parent;

        @Option(names = { "--port" }, description = "Server port", defaultValue = "8090")
        private int port;

        @Option(names = { "-p", "--project" }, description = "Default project path")
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
                Thread.currentThread().join();
                return 0;
            } catch (Exception e) {
                cli.printError("Failed to start server: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Probe whether a previously started REST server is alive.
     */
    @Command(name = "status", description = "Check server status")
    public static class StatusCommand implements Callable<Integer> {
        @ParentCommand
        private ServerCommand parent;

        @Option(names = { "--port" }, description = "Server port to check", defaultValue = "8090")
        private int port;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            try {
                URL url = new URL("http://localhost:" + port + "/api/health");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                int code = conn.getResponseCode();
                if (code == 200) cli.printSuccess(
                    "Server is running on port " + port
                ); else cli.printWarning("Server responded with status: " + code);
                return 0;
            } catch (Exception e) {
                cli.printWarning("No server running on port " + port);
                return 1;
            }
        }
    }

    // ==================================================================
    // REST companion server (unchanged from previous behaviour)
    // ==================================================================

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
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
            executor.submit(
                () -> {
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
                }
            );
        }

        void stop() {
            running.set(false);
            try {
                if (serverSocket != null) serverSocket.close();
                if (executor != null) executor.shutdownNow();
            } catch (Exception ignored) {}
        }

        private void handleRequest(Socket client) {
            try (
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(client.getInputStream())
                );
                OutputStream out = client.getOutputStream()
            ) {
                String requestLine = in.readLine();
                if (requestLine == null) return;
                String[] parts = requestLine.split(" ");
                if (parts.length < 2) return;
                String method = parts[0];
                String path = parts[1];
                String line;
                int contentLength = 0;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    if (line.toLowerCase().startsWith("content-length:")) {
                        contentLength = Integer.parseInt(line.substring(15).trim());
                    }
                }
                String body = "";
                if (contentLength > 0) {
                    char[] buffer = new char[contentLength];
                    in.read(buffer, 0, contentLength);
                    body = new String(buffer);
                }
                String response = routeRequest(method, path, body);
                String httpResponse =
                    "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Access-Control-Allow-Origin: *\r\n" +
                    "Content-Length: " +
                    response.getBytes(StandardCharsets.UTF_8).length +
                    "\r\n" +
                    "\r\n" +
                    response;
                out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (Exception e) {
                System.err.println("Request handling error: " + e.getMessage());
            } finally {
                try {
                    client.close();
                } catch (Exception ignored) {}
            }
        }

        private String routeRequest(String method, String path, String body) {
            if (path.equals("/api/health")) return "{\"status\":\"ok\",\"version\":\"2.0.0\"}";
            if (path.equals("/api/projects")) return listProjects();
            if (path.startsWith("/api/actions")) return "[]";
            if (path.equals("/api/config")) return "{}";
            return "{\"error\":\"Not found\",\"path\":\"" + path + "\"}";
        }

        private String listProjects() {
            String base = projectPath != null
                ? projectPath
                : System.getProperty("user.dir") + File.separator + "Projects";
            File dir = new File(base);
            if (!dir.exists()) return "[]";
            StringBuilder sb = new StringBuilder("[");
            File[] projects = dir.listFiles(File::isDirectory);
            if (projects != null) {
                for (int i = 0; i < projects.length; i++) {
                    if (i > 0) sb.append(",");
                    sb.append("{\"name\":\"").append(projects[i].getName()).append("\"}");
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
