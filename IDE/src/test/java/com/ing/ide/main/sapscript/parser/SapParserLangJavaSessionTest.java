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
 * Session-tracking coverage for {@link SapParserLangJava}, mirroring
 * {@link SapParserLangPowerShellSessionTest} - Java overrides {@code parse()} completely (JACOB's
 * {@code .invoke(...)}/{@code .setProperty(...)} idiom, not the base class's generic regex
 * dispatch), so it needs the same explicit regression coverage PowerShell has.
 */
public class SapParserLangJavaSessionTest {
    private File tempDir;

    @BeforeMethod
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("sap-session-java-test").toFile();
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
        File f = new File(tempDir, "recording.jsh");
        try (FileWriter w = new FileWriter(f)) {
            for (String line : lines) {
                w.write(line);
                w.write("\n");
            }
        }
        return f;
    }

    @Test
    public void startTransactionOnNewSessionVariable_opensSessionBeforeTransaction()
        throws Exception {
        // Regression: a line that is BOTH the first reference to a new session variable AND a
        // startTransaction call must open/tag the new session before the Transaction action is
        // recorded, not after - otherwise the transaction runs against the still-current (old)
        // session instead of the one it was actually recorded against.
        File f = scriptFile(
            "session.startTransaction(\"VA01\");",
            "session2.startTransaction(\"VA02\");"
        );
        SapParserLangJava parser = new SapParserLangJava();
        parser.parse(f);

        assertEquals(actionTypes(parser), List.of("Transaction", "OpenSession", "Transaction"));
        List<SapLanguageParser.SapAction> actions = parser.getSapActions();
        assertNull(actions.get(0).sessionLabel, "VA01 recorded on the primary session");
        assertEquals(actions.get(1).objectId, "s1");
        assertEquals(
            actions.get(2).sessionLabel,
            "s1",
            "VA02 must be tagged to the new session, not the old one"
        );
    }

    private static List<String> actionTypes(SapParserLangJava parser) {
        List<String> types = new ArrayList<>();
        for (SapLanguageParser.SapAction action : parser.getSapActions()) {
            types.add(action.actionType);
        }
        return types;
    }
}
