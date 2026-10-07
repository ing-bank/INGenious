package com.ing.ide.main.sapscript.parser;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Same Phase 4 multi-session coverage as {@link SapLanguageParserSessionTest}, exercised against
 * {@link SapParserLangJavaScript} instead of {@link SapParserLangVBScript} - both share the
 * identical {@link SapLanguageParser} parse()/parseSapAction() machinery (neither overrides
 * parse()), so this gives JavaScript its own explicit regression coverage rather than relying
 * solely on the VBScript instantiation to stand in for the shared code path.
 */
public class SapParserLangJavaScriptSessionTest {
    private File tempDir;

    @BeforeMethod
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("sap-session-js-test").toFile();
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
        File f = new File(tempDir, "recording.js");
        try (FileWriter w = new FileWriter(f)) {
            for (String line : lines) {
                w.write(line);
                w.write("\n");
            }
        }
        return f;
    }

    @Test
    public void singleSessionRecording_emitsNoSessionActions() throws Exception {
        File f = scriptFile(
            "session.findById(\"wnd[0]/usr/txtRSYST-BNAME\").text = \"TESTUSER\";",
            "session.findById(\"wnd[0]\").sendVKey(0);"
        );
        SapParserLangJavaScript parser = new SapParserLangJavaScript();
        parser.parse(f);

        List<String> types = actionTypes(parser);
        assertEquals(types, List.of("Set", "SendVKey"));
        for (SapLanguageParser.SapAction action : parser.getSapActions()) {
            assertNull(action.sessionLabel, "single-session recording must tag nothing");
        }
    }

    @Test
    public void switchingToASecondSessionVariable_opensThenSwitchesBack() throws Exception {
        File f = scriptFile(
            "session.findById(\"wnd[0]/usr/txtRSYST-BNAME\").text = \"TESTUSER\";",
            "session.findById(\"wnd[0]\").sendVKey(0);",
            "session2.findById(\"wnd[0]/usr/txtRSYST-BNAME\").text = \"TESTUSER2\";",
            "session2.findById(\"wnd[0]/tbar[0]/btn[0]\").press();",
            "session.findById(\"wnd[0]/usr/txtFOO\").text = \"back to primary\";"
        );
        SapParserLangJavaScript parser = new SapParserLangJavaScript();
        parser.parse(f);

        List<String> types = actionTypes(parser);
        assertEquals(
            types,
            List.of("Set", "SendVKey", "OpenSession", "Set", "Click", "SwitchSession", "Set")
        );

        List<SapLanguageParser.SapAction> actions = parser.getSapActions();
        SapLanguageParser.SapAction openSession = actions.get(2);
        assertEquals(openSession.objectId, "s1");

        SapLanguageParser.SapAction switchBack = actions.get(5);
        assertEquals(
            switchBack.objectId,
            "",
            "switching back to the primary session must be blank input, not a made-up label"
        );

        // The two elements captured under session2 (indices 3, 4) are tagged "s1"...
        assertEquals(actions.get(3).sessionLabel, "s1");
        assertEquals(actions.get(4).sessionLabel, "s1");
        // ...while the primary session's own elements (0, 1, 6) carry no label at all.
        assertNull(actions.get(0).sessionLabel);
        assertNull(actions.get(1).sessionLabel);
        assertNull(actions.get(6).sessionLabel);
    }

    @Test
    public void sameRelativeIdOnTwoSessions_producesTwoDistinctObjects() throws Exception {
        File f = scriptFile(
            "session.findById(\"wnd[0]/usr/txtRSYST-BNAME\").text = \"TESTUSER\";",
            "session2.findById(\"wnd[0]/usr/txtRSYST-BNAME\").text = \"TESTUSER2\";"
        );
        SapParserLangJavaScript parser = new SapParserLangJavaScript();
        parser.parse(f);

        long matchingId = parser
            .getSapObjects()
            .values()
            .stream()
            .filter(o -> o.id.equals("wnd[0]/usr/txtRSYST-BNAME"))
            .count();
        assertEquals(matchingId, 2, "the primary and s1 copies must not collapse into one object");

        boolean hasPrimary = parser
            .getSapObjects()
            .values()
            .stream()
            .anyMatch(o -> o.id.equals("wnd[0]/usr/txtRSYST-BNAME") && o.sessionLabel == null);
        boolean hasS1 = parser
            .getSapObjects()
            .values()
            .stream()
            .anyMatch(o -> o.id.equals("wnd[0]/usr/txtRSYST-BNAME") && "s1".equals(o.sessionLabel));
        assertTrue(hasPrimary);
        assertTrue(hasS1);
    }

    private static List<String> actionTypes(SapParserLangJavaScript parser) {
        List<String> types = new ArrayList<>();
        for (SapLanguageParser.SapAction action : parser.getSapActions()) {
            types.add(action.actionType);
        }
        return types;
    }
}
