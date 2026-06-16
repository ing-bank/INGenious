package com.ing.datalib.settings.migration;

import com.ing.datalib.settings.Devices;
import com.ing.datalib.settings.Emulators;
import com.ing.datalib.settings.emulators.Device;
import com.ing.datalib.settings.emulators.Emulator;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * One-time, idempotent migration of legacy "Manage Browsers" emulator entries
 * (Settings/Emulators.json) into the new "Manage Devices" store
 * (Settings/Devices.json).
 *
 * <p>Rules:
 * <ul>
 *   <li>The "SAP" emulator is preserved as-is (SAP execution still depends on it).</li>
 *   <li>Only emulators with {@code Type == "Remote URL"} are migrated; other
 *       legacy types are left untouched.</li>
 *   <li>If a {@link Device} with the same name already exists, the emulator
 *       entry is removed without overwriting the device (capability
 *       {@code .properties} file is shared so no caps are lost).</li>
 *   <li>{@code LambdaTest} is inferred from the remote URL.</li>
 *   <li>Capability files under {@code Settings/Capabilities/&lt;Name&gt;.properties}
 *       are shared between the two stores; no file move is required.</li>
 * </ul>
 *
 * <p>The migration is naturally idempotent: a successfully migrated emulator
 * is deleted from the source list, so a re-run finds nothing to do.
 */
public final class EmulatorToDeviceMigration {
    private static final Logger LOGGER = Logger.getLogger(
        EmulatorToDeviceMigration.class.getName()
    );

    private static final String SAP_NAME = "SAP";
    private static final String REMOTE_URL_TYPE = "Remote URL";
    private static final String LAMBDATEST_URL_SUFFIX = "hub.lambdatest.com/wd/hub";

    private EmulatorToDeviceMigration() {}

    /**
     * Performs the migration in-memory and persists both stores if anything
     * changed. Safe to call on every project load.
     *
     * @return summary of what was done.
     */
    public static MigrationResult migrate(Emulators src, Devices dst) {
        MigrationResult result = new MigrationResult();
        if (src == null || dst == null) {
            return result;
        }

        // Snapshot first — we mutate src.getEmulators() while iterating.
        List<Emulator> snapshot = new ArrayList<>(src.getEmulators());
        for (Emulator e : snapshot) {
            String name = e.getName();
            if (name == null || name.isEmpty()) {
                continue;
            }
            if (SAP_NAME.equals(name)) {
                continue;
            }
            if (!REMOTE_URL_TYPE.equals(e.getType())) {
                // Legacy chrome-emulation entry (Driver/Size/UserAgent) —
                // unsupported by current Engine. Leave as-is and log.
                result.skippedLegacy.add(name);
                continue;
            }
            if (dst.getDevice(name) != null) {
                // Device already exists (probably a re-run, or user already
                // recreated it on the Devices tab). Drop the emulator copy
                // so the UI shows only one source of truth.
                src.deleteEmulator(name);
                result.skippedExisting.add(name);
                continue;
            }

            dst.addDevice(name);
            Device d = dst.getDevice(name);
            String url = e.getRemoteUrl();
            if (url != null && !url.isEmpty()) {
                d.setRemoteUrl(url);
                d.setLambdaTest(url.endsWith(LAMBDATEST_URL_SUFFIX));
            }
            src.deleteEmulator(name);
            result.migrated.add(name);
        }

        if (result.changed()) {
            try {
                src.save();
                dst.save();
                LOGGER.log(
                    Level.INFO,
                    "Emulator->Device migration: migrated={0}, skippedExisting={1}, skippedLegacy={2}",
                    new Object[] { result.migrated, result.skippedExisting, result.skippedLegacy }
                );
            } catch (RuntimeException ex) {
                LOGGER.log(Level.SEVERE, "Failed to persist Emulator->Device migration", ex);
            }
        }
        return result;
    }

    /** Summary of a migration run. */
    public static final class MigrationResult {
        public final List<String> migrated = new ArrayList<>();
        public final List<String> skippedExisting = new ArrayList<>();
        public final List<String> skippedLegacy = new ArrayList<>();

        public boolean changed() {
            return !migrated.isEmpty() || !skippedExisting.isEmpty();
        }
    }
}
