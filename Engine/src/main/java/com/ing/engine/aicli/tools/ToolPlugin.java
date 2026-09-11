package com.ing.engine.aicli.tools;

import java.util.List;

/**
 * Service Provider Interface for optional capability packs (Appium, Kafka,
 * Database, ...). Implementations are discovered via
 * {@link java.util.ServiceLoader} from jars on the classpath (including
 * {@code Resources/plugins/}).
 */
public interface ToolPlugin {
    /** Human-readable pack name, e.g. "Appium". */
    String name();

    String version();

    /** Tools contributed by this pack. */
    List<Tool> tools();
}
