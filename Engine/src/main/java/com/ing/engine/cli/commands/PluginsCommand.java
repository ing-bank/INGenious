package com.ing.engine.cli.commands;

import com.ing.engine.aicli.tools.Tool;
import com.ing.engine.aicli.tools.ToolPlugin;
import com.ing.engine.aicli.tools.ToolRegistry;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Lists the core tool packs and any {@link ToolPlugin} capability packs
 * discovered via ServiceLoader (drop plugin jars on the classpath /
 * {@code lib} directory to install them).
 */
@Command(
    name = "plugins",
    description = "List installed AI CLI tool packs (core categories + ServiceLoader plugins)"
)
public class PluginsCommand implements Callable<Integer> {
    @Parameters(index = "0", arity = "0..1", description = "Subcommand: list (default)")
    private String action;

    @Override
    public Integer call() {
        ToolRegistry registry = ToolRegistry.create();

        System.out.println("Core tool packs:");
        for (Map.Entry<String, List<Tool>> e : registry.byCategory().entrySet()) {
            System.out.printf("  %-12s %d tools%n", e.getKey(), e.getValue().size());
        }

        System.out.println();
        System.out.println("Plugins (ServiceLoader):");
        boolean any = false;
        for (ToolPlugin plugin : ServiceLoader.load(ToolPlugin.class)) {
            System.out.printf(
                "  %-20s %-10s %d tools%n",
                plugin.name(),
                plugin.version(),
                plugin.tools().size()
            );
            any = true;
        }
        if (!any) {
            System.out.println("  (none installed)");
            System.out.println();
            System.out.println(
                "To install a capability pack, add a jar that provides a " +
                "META-INF/services/com.ing.engine.aicli.tools.ToolPlugin entry to the classpath."
            );
        }
        return 0;
    }
}
