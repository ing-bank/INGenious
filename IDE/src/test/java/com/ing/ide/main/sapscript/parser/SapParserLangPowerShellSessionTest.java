package com.ing.ide.main.sapscript.parser;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Phase 4 multi-session import for PowerShell recordings - the only language actually reachable
 * through the IDE's "Import SAP Recording" menu today (every other menu entry is commented out
 * in AppActionListener), so it gets its own dedicated session-variable wiring rather than relying
 * on the base class's line-oriented regex (PowerShell's {@code -object $session} syntax has no
 * trailing dot for that regex to match).
 */
public class SapParserLangPowerShellSessionTest {
    private File tempDir;

    @BeforeMethod
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("sap-session-ps-test").toFile();
    }

    @AfterMethod
    public void tearDown() {
        deleteRecursively(tempDir);
    }

    private static void deleteRecursively(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        f.delete();
    }

    private File scriptFile(String... lines) throws Exception {
        File f = new File(tempDir, "recording.ps1");
        try (FileWriter w = new FileWriter(f)) {
            for (String line : lines) {
                w.write(line);
                w.write("\n");
            }
        }
        return f;
    }

    @Test
    public void secondSessionVariable_opensThenSwitchesBackToPrimary() throws Exception {
        File f = scriptFile(
            "$ID = Invoke-Method -object $session -methodName \"findById\" -methodParameter @(\"wnd[0]/usr/txtRSYST-BNAME\")",
            "Set-Property -object $ID -propertyName \"text\" -propertyValue @(\"TESTUSER\")",
            "$ID2 = Invoke-Method -object $session2 -methodName \"findById\" -methodParameter @(\"wnd[0]/usr/txtRSYST-BNAME\")",
            "Set-Property -object $ID2 -propertyName \"text\" -propertyValue @(\"TESTUSER2\")",
            "Invoke-Method -object $ID2 -methodName \"press\"",
            "$ID3 = Invoke-Method -object $session -methodName \"findById\" -methodParameter @(\"wnd[0]/usr/txtFOO\")",
            "Set-Property -object $ID3 -propertyName \"text\" -propertyValue @(\"back to primary\")"
        );
        SapParserLangPowerShell parser = new SapParserLangPowerShell();
        parser.parse(f);

        List<String> types = actionTypes(parser);
        assertEquals(types, List.of("Set", "OpenSession", "Set", "Click", "SwitchSession", "Set"));

        List<SapLanguageParser.SapAction> actions = parser.getSapActions();
        assertEquals(actions.get(1).objectId, "s1"); // OpenSession label
        assertEquals(actions.get(4).objectId, ""); // SwitchSession back to primary = blank

        assertNull(actions.get(0).sessionLabel);
        assertEquals(actions.get(2).sessionLabel, "s1");
        assertEquals(actions.get(3).sessionLabel, "s1");
        assertNull(actions.get(5).sessionLabel);
    }

    @Test
    public void singleSessionRecording_emitsNoSessionActions() throws Exception {
        File f = scriptFile(
            "$ID = Invoke-Method -object $session -methodName \"findById\" -methodParameter @(\"wnd[0]/usr/txtRSYST-BNAME\")",
            "Set-Property -object $ID -propertyName \"text\" -propertyValue @(\"TESTUSER\")"
        );
        SapParserLangPowerShell parser = new SapParserLangPowerShell();
        parser.parse(f);

        assertEquals(actionTypes(parser), List.of("Set"));
        assertNull(parser.getSapActions().get(0).sessionLabel);
    }

    private static List<String> actionTypes(SapParserLangPowerShell parser) {
        List<String> types = new ArrayList<>();
        for (SapLanguageParser.SapAction action : parser.getSapActions()) {
            types.add(action.actionType);
        }
        return types;
    }
}
