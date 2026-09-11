package com.ing.engine.cli.commands;

import com.ing.engine.aicli.repl.Repl;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Entry point for the interactive AI CLI (conversational REPL).
 * Also the default when {@code ingenious} is launched with no arguments in a
 * real terminal.
 */
@Command(
    name = "ai",
    aliases = { "chat", "assistant" },
    description = "Start the interactive AI assistant (conversational REPL over all INGenious tools)"
)
public class AiCommand implements Callable<Integer> {
    @Option(names = { "-p", "--project" }, description = "Project to work with (name or path)")
    private String project;

    @Option(names = { "--no-banner" }, description = "Don't show the welcome banner")
    private boolean noBanner;

    @Override
    public Integer call() {
        return new Repl(project, noBanner).run();
    }
}
