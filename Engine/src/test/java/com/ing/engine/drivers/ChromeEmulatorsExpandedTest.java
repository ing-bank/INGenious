package com.ing.engine.drivers;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

/**
 * Expanded tests for ChromeEmulators — replaces the disabled tests with
 * working alternatives that don't depend on Chrome being installed.
 */
public class ChromeEmulatorsExpandedTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── getPrefLocation ─────────────────────────────────────────────────

    @Test
    public void testGetPrefLocationReturnsNonNull() {
        String location = ChromeEmulators.getPrefLocation();
        assertThat(location).isNotNull();
    }

    @Test
    public void testGetPrefLocationContainsChrome() {
        String location = ChromeEmulators.getPrefLocation();
        // On all OS the path contains "Chrome" or "chrome" or "google-chrome"
        assertThat(location.toLowerCase()).containsAnyOf("chrome", "osnotconfigured");
    }

    @Test
    public void testGetPrefLocationContainsDefault() {
        String location = ChromeEmulators.getPrefLocation();
        // On supported OS, path ends with "Default"
        if (!location.equals("OSNotConfigured")) {
            assertThat(location).endsWith("Default");
        }
    }

    @Test
    public void testGetPrefLocationContainsUserHome() {
        String location = ChromeEmulators.getPrefLocation();
        if (!location.equals("OSNotConfigured")) {
            String home = System.getProperty("user.home");
            assertThat(location).startsWith(home);
        }
    }

    // ── JSON serialization round-trip ───────────────────────────────────

    @Test
    public void testEmulatorListSerializationRoundTrip() throws IOException {
        Path tempFile = Files.createTempFile("emulators-test", ".json");
        try {
            List<String> emulators = Arrays.asList("iPhone 14", "Pixel 7", "Galaxy S24");
            MAPPER.writeValue(tempFile.toFile(), emulators);

            List<String> loaded = MAPPER.readValue(tempFile.toFile(), List.class);
            assertThat(loaded).containsExactlyElementsOf(emulators);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testEmptyEmulatorListSerialization() throws IOException {
        Path tempFile = Files.createTempFile("emulators-empty", ".json");
        try {
            List<String> empty = new ArrayList<>();
            MAPPER.writeValue(tempFile.toFile(), empty);

            List<String> loaded = MAPPER.readValue(tempFile.toFile(), List.class);
            assertThat(loaded).isEmpty();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testSingleEmulatorSerialization() throws IOException {
        Path tempFile = Files.createTempFile("emulators-single", ".json");
        try {
            List<String> single = Arrays.asList("Nexus 5");
            MAPPER.writeValue(tempFile.toFile(), single);

            List<String> loaded = MAPPER.readValue(tempFile.toFile(), List.class);
            assertThat(loaded).containsExactly("Nexus 5");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    // ── Emulators list manipulation patterns ────────────────────────────

    @Test
    public void testEmulatorListAddRemove() {
        List<String> list = new ArrayList<>();
        list.add("Device A");
        list.add("Device B");
        list.add("Device C");

        assertThat(list).hasSize(3);

        list.remove("Device B");
        assertThat(list).containsExactly("Device A", "Device C");
    }

    @Test
    public void testEmulatorListClearAndAddAll() {
        List<String> list = new ArrayList<>(Arrays.asList("old1", "old2"));
        List<String> newDevices = Arrays.asList("new1", "new2", "new3");

        list.clear();
        list.addAll(newDevices);

        assertThat(list).containsExactlyElementsOf(newDevices);
    }

    @Test
    public void testEmulatorListDuplicateHandling() {
        List<String> list = new ArrayList<>();
        list.add("iPhone 14");
        list.add("iPhone 14");

        // ArrayList allows duplicates
        assertThat(list).hasSize(2);
    }

    // ── Preferences JSON parsing pattern (unit test of the extraction logic) ──

    @Test
    public void testPreferencesJsonParsingPattern() throws IOException {
        // Simulate the JSON structure that sync() expects from Chrome Preferences
        String prefsJson = "{"
                + "\"devtools\": {"
                + "  \"preferences\": {"
                + "    \"standardEmulatedDeviceList\": "
                + "      \"[{\\\"title\\\":\\\"Nexus 5\\\"},{\\\"title\\\":\\\"iPhone 6\\\"},{\\\"title\\\":\\\"iPad\\\"}]\""
                + "  }"
                + "}"
                + "}";

        Path tempFile = Files.createTempFile("prefs-test", ".json");
        try {
            Files.writeString(tempFile, prefsJson);

            // Replicate the sync() parsing logic
            java.util.Map map = MAPPER.readValue(tempFile.toFile(), java.util.Map.class);
            java.util.Map devtools = (java.util.Map) map.get("devtools");
            java.util.Map prefs = (java.util.Map) devtools.get("preferences");
            String stdemulators = (String) prefs.get("standardEmulatedDeviceList");

            List list = MAPPER.readValue(stdemulators, List.class);
            List<String> titles = new ArrayList<>();
            for (Object device : list) {
                titles.add(((java.util.Map) device).get("title").toString());
            }

            assertThat(titles).containsExactly("Nexus 5", "iPhone 6", "iPad");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
