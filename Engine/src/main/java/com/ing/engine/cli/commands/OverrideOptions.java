package com.ing.engine.cli.commands;

import com.ing.engine.constants.SystemDefaults;
import java.util.List;
import picocli.CommandLine.Option;

/**
 * Shared typed override flags for the {@code ingenious run …} subcommands.
 *
 * <p>Each flag accepts a {@code key=value} (or {@code <bucket>.key=value})
 * payload and writes the corresponding {@code <prefix>.…} entry into
 * {@link SystemDefaults#EnvVars}, so that {@code ProjectRunner.overrideWithEnv()}
 * picks it up just as if the user had passed {@code -setEnv "<prefix>.k=v"}.
 *
 * <p>Why a separate Mixin? Picocli will splice these {@code @Option}-annotated
 * fields into every command that declares
 * {@code @Mixin private OverrideOptions overrides;} — no duplication across
 * the four run sub-subcommands ({@code testcase / testset / tags / rerun}).
 *
 * <p>Call {@link #applyAll()} from each command's {@code call()} method
 * <em>before</em> delegating to the legacy {@code Control.main(…)} bridge.
 */
public class OverrideOptions {
    @Option(
        names = { "--set-env" },
        description = "Raw override, equivalent to legacy -setEnv. Format: 'key=value' (repeatable).",
        paramLabel = "<k=v>"
    )
    List<String> setEnv;

    @Option(
        names = { "--driver" },
        description = "Driver / Launch Configurations override. Format: 'key=value' (repeatable).",
        paramLabel = "<k=v>"
    )
    List<String> driver;

    @Option(
        names = { "--user" },
        description = "UserDefined variable override. Format: 'key=value' (repeatable).",
        paramLabel = "<k=v>"
    )
    List<String> user;

    @Option(
        names = { "--tm" },
        description = "Flat Test Manager setting override. Format: 'key=value' (repeatable).",
        paramLabel = "<k=v>"
    )
    List<String> tm;

    @Option(
        names = { "--capability" },
        description = "Per-browser capability override. Format: '<browser>.<key>=value' (repeatable).",
        paramLabel = "<b.k=v>"
    )
    List<String> capability;

    @Option(
        names = { "--db" },
        description = "Database property override. Format: '<alias>.<key>=value' (repeatable).",
        paramLabel = "<db.k=v>"
    )
    List<String> db;

    @Option(
        names = { "--context" },
        description = "Context property override. Format: '<alias>.<key>=value' (repeatable).",
        paramLabel = "<ctx.k=v>"
    )
    List<String> context;

    @Option(
        names = { "--api" },
        description = "API property override. Format: '<alias>.<key>=value' (repeatable).",
        paramLabel = "<api.k=v>"
    )
    List<String> api;

    @Option(
        names = { "--kafka-ssl" },
        description = "Kafka SSL configuration override. Format: 'key=value' (repeatable). " +
        "Accepts the canonical 'kafkaSsl' spelling.",
        paramLabel = "<k=v>"
    )
    List<String> kafkaSsl;

    @Option(
        names = { "--kafka-producer" },
        description = "Kafka producer config override. Format: '<name>.<key>=value' (repeatable).",
        paramLabel = "<name.k=v>"
    )
    List<String> kafkaProducer;

    @Option(
        names = { "--kafka-consumer" },
        description = "Kafka consumer config override. Format: '<name>.<key>=value' (repeatable).",
        paramLabel = "<name.k=v>"
    )
    List<String> kafkaConsumer;

    @Option(
        names = { "--lambdatest-cap" },
        description = "LambdaTest Grid Capabilities override. Format: 'key=value' (repeatable).",
        paramLabel = "<k=v>"
    )
    List<String> lambdatestCap;

    @Option(
        names = { "--browser-arg" },
        description = "Indexed per-browser launch flag. Format: '<browser>.<index>=<arg>' " +
        "(e.g. 'Chrome.1=--headless=new'). Repeatable.",
        paramLabel = "<b.n=v>"
    )
    List<String> browserArg;

    @Option(
        names = { "--browser-set" },
        description = "Arbitrary per-browser property (create-on-missing). " +
        "Format: '<browser>.<key>=value' (repeatable).",
        paramLabel = "<b.k=v>"
    )
    List<String> browserSet;

    @Option(
        names = { "--device" },
        description = "Per-device override (Manage Devices). " +
        "Format: '<name>.<key>=value' (repeatable). " +
        "Reserved keys: RemoteURL, LambdaTest, __enabled.",
        paramLabel = "<dev.k=v>"
    )
    List<String> device;

    @Option(
        names = { "--tm-module" },
        description = "AzureDevOps TestPlan per-module option. " +
        "Format: '<module>.<key>=value' (repeatable). " +
        "Reserved key: __enabled.",
        paramLabel = "<mod.k=v>"
    )
    List<String> tmModule;

    /**
     * Apply every flag value into {@link SystemDefaults#EnvVars} so the
     * downstream {@code ProjectRunner.overrideWithEnv()} dispatcher sees
     * them.
     */
    public void applyAll() {
        // 1) raw pass-through
        addAll(setEnv, "");
        // 2) typed flags -> prefixed entries
        addAll(driver, "driver.");
        addAll(user, "user.");
        addAll(tm, "tm.");
        addAll(capability, "capability.");
        addAll(db, "db.");
        addAll(context, "context.");
        addAll(api, "api.");
        addAll(kafkaSsl, "kafkaSsl.");
        addAll(kafkaProducer, "kafkaProducer.");
        addAll(kafkaConsumer, "kafkaConsumer.");
        addAll(lambdatestCap, "lambdatest.");
        addAll(browserArg, "browserArg.");
        addAll(browserSet, "browser.");
        addAll(device, "device.");
        addAll(tmModule, "tmModule.");
    }

    private static void addAll(List<String> items, String prefix) {
        if (items == null) return;
        for (String item : items) {
            if (item == null) continue;
            // Accept either a single 'k=v' (raw) or '<bucket>.k=v' (typed) - the
            // OverrideOptions caller has already chosen the right list, so we
            // just splice the prefix in front of whatever the user provided.
            int eq = item.indexOf('=');
            if (eq <= 0) continue;
            String key = item.substring(0, eq);
            String value = item.substring(eq + 1);
            if (prefix.isEmpty()) {
                SystemDefaults.EnvVars.put(key, value);
            } else {
                SystemDefaults.EnvVars.put(prefix + key, value);
            }
        }
    }

    /** True when the user supplied at least one override flag. */
    public boolean hasAny() {
        return (
            nonEmpty(setEnv) ||
            nonEmpty(driver) ||
            nonEmpty(user) ||
            nonEmpty(tm) ||
            nonEmpty(capability) ||
            nonEmpty(db) ||
            nonEmpty(context) ||
            nonEmpty(api) ||
            nonEmpty(kafkaSsl) ||
            nonEmpty(kafkaProducer) ||
            nonEmpty(kafkaConsumer) ||
            nonEmpty(lambdatestCap) ||
            nonEmpty(browserArg) ||
            nonEmpty(browserSet) ||
            nonEmpty(device) ||
            nonEmpty(tmModule)
        );
    }

    private static boolean nonEmpty(List<?> l) {
        return l != null && !l.isEmpty();
    }
}
