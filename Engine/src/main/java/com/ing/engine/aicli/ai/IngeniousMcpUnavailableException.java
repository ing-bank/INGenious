package com.ing.engine.aicli.ai;

/**
 * Thrown when the Copilot CLI session started but the INGenious stdio MCP server
 * ({@code ingenious}) never reached the connected state. Without this check the
 * model only reports "'ingenious' MCP not discoverable"; the message of this
 * exception carries the actual root cause (server status, Copilot CLI error and
 * the tail of the MCP startup log) so the CLI and IDE can show it verbatim.
 */
public class IngeniousMcpUnavailableException extends AiProvider.AiException {

    /**
     * @param message user-facing diagnosis, safe to display as-is
     */
    public IngeniousMcpUnavailableException(String message) {
        super(message);
    }

    /**
     * @param message user-facing diagnosis, safe to display as-is
     * @param cause underlying failure
     */
    public IngeniousMcpUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
