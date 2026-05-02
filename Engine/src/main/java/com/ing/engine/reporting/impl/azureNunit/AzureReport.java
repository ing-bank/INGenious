
package com.ing.engine.reporting.impl.azureNunit;


import java.io.File;
import java.io.IOException;

public class AzureReport {

    public static long totalDuration = 0;
    public static int failed = 0;
    public static int passed = 0;

    public static File testCasesFile;

    static {
        try {
            testCasesFile = File.createTempFile("test-cases", ".xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
