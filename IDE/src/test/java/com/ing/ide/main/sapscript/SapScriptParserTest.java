package com.ing.ide.main.sapscript;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import com.ing.ide.main.sapscript.parser.SapLanguageParser;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for {@link SapScriptParser}'s window-id disambiguation fix (Phase 3: "Multi-window
 * (wnd[1..n]) import fix") and the balanced SAP.initConnection / SAP.closeConnection pair it now
 * emits around the generated test case. Uses reflection to reach the parser's private methods
 * directly - {@code generateObjectName}/{@code generateUniqueObjectName} and
 * {@code generateTestCase} don't touch {@code AppMainFrame} at all, so a real IDE frame isn't
 * needed to exercise them.
 */
public class SapScriptParserTest {
    private SapScriptParser parser;
    private File tempDir;

    @BeforeMethod
    public void setUp() throws Exception {
        parser = new SapScriptParser(null);
        tempDir = Files.createTempDirectory("sapscriptparser-test").toFile();
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

    private String generateObjectName(String id) throws Exception {
        Method m = SapScriptParser.class.getDeclaredMethod("generateObjectName", String.class);
        m.setAccessible(true);
        return (String) m.invoke(parser, id);
    }

    private String generateUniqueObjectName(String id) throws Exception {
        Method m =
            SapScriptParser.class.getDeclaredMethod("generateUniqueObjectName", String.class);
        m.setAccessible(true);
        return (String) m.invoke(parser, id);
    }

    // ---- generateObjectName: window-scope disambiguation ----

    @Test
    public void mainWindowIdGetsNoPrefix() throws Exception {
        assertEquals(generateObjectName("wnd[0]/usr/txtRSYST-BNAME"), "txtRSYST_BNAME");
    }

    @Test
    public void popupWindowIdGetsDistinctNameFromMainWindow() throws Exception {
        String mainName = generateObjectName("wnd[0]/usr/txtRSYST-BNAME");
        String popupName = generateObjectName("wnd[1]/usr/txtRSYST-BNAME");

        assertNotEquals(
            popupName,
            mainName,
            "a popup field must never collapse onto the main window's same-named field"
        );
        assertEquals(popupName, "w1_txtRSYST_BNAME");
    }

    @Test
    public void deeperPopupLevelsAreAlsoDisambiguated() throws Exception {
        assertEquals(generateObjectName("wnd[2]/usr/btnOK"), "w2_btnOK");
    }

    @Test
    public void generateUniqueObjectName_cachesConsistentlyPerId() throws Exception {
        String first = generateUniqueObjectName("wnd[1]/usr/txtFOO");
        String second = generateUniqueObjectName("wnd[1]/usr/txtFOO");
        assertEquals(first, second, "the same id must always resolve to the same generated name");
    }

    @Test
    public void generateUniqueObjectName_noLongerCollidesAcrossWindows() throws Exception {
        // Before the fix, wnd[0]/usr/txtFOO and wnd[1]/usr/txtFOO both produced "txtFOO",
        // silently colliding into "txtFOO" / "txtFOO_1" instead of encoding the window.
        String mainField = generateUniqueObjectName("wnd[0]/usr/txtFOO");
        String popupField = generateUniqueObjectName("wnd[1]/usr/txtFOO");

        assertEquals(mainField, "txtFOO");
        assertEquals(popupField, "w1_txtFOO");
    }

    // ---- generateTestCase: balanced init/close connection pair ----

    @SuppressWarnings("unchecked")
    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field f = SapScriptParser.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(parser, value);
    }

    private List<String> generatedCsvLines() throws Exception {
        Map<String, String> testCase = new HashMap<>();
        String scenarioDir = new File(tempDir, "TestPlan/SapImport").getAbsolutePath();
        new File(scenarioDir).mkdirs();
        testCase.put("testScenarioName", scenarioDir);
        testCase.put("pageName", "SapImport");
        setPrivateField("testCase", testCase);

        List<SapLanguageParser.SapAction> actions = new ArrayList<>();
        actions.add(new SapLanguageParser.SapAction("Set", "wnd[0]/usr/txtFOO", "bar", 1));
        setPrivateField("sapActions", actions);

        Method generateTestCase = SapScriptParser.class.getDeclaredMethod("generateTestCase");
        generateTestCase.setAccessible(true);
        generateTestCase.invoke(parser);

        File csv = new File(scenarioDir, "SapImport.csv");
        return Files.readAllLines(csv.toPath());
    }

    @Test
    public void generatedTestCaseOpensWithBlankInitConnection() throws Exception {
        List<String> lines = generatedCsvLines();
        String firstStep = lines.get(1); // line 0 is the CSV header
        assertTrue(firstStep.contains("sapInitConnection"), firstStep);
        assertTrue(firstStep.startsWith("1,SAP,"), firstStep);
        // blank Input column: "...,sapInitConnection,,,"
        assertTrue(firstStep.matches("1,SAP,[^,]*,sapInitConnection,,,"), firstStep);
    }

    @Test
    public void generatedTestCaseClosesWithBlankCloseConnection() throws Exception {
        List<String> lines = generatedCsvLines();
        String lastStep = lines.get(lines.size() - 1);
        assertTrue(lastStep.contains("sapCloseConnection"), lastStep);
        assertTrue(lastStep.matches("3,SAP,[^,]*,sapCloseConnection,,,"), lastStep);
    }
}
