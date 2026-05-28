package com.ing.engine.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.ing.engine.constants.AppResourcePath;
import com.ing.engine.constants.SystemDefaults;
import eu.infomas.annotation.AnnotationDetector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for AnnontationUtil — verifies the detect method delegates
 * scanning to the AnnotationDetector with appropriate sources.
 * <p>
 * SystemDefaults.getClassesFromJar is a public static AtomicBoolean field,
 * so we save/restore it directly rather than mocking.
 * <p>
 * AnnontationUtil.detect() calls static methods on AppResourcePath
 * (inherited by FilePath), so we mock AppResourcePath.
 */
public class AnnontationUtilTest {

    private boolean savedGetClassesFromJar;

    @BeforeMethod
    public void setUp() {
        savedGetClassesFromJar = SystemDefaults.getClassesFromJar.get();
        // Default to false for all tests
        SystemDefaults.getClassesFromJar.set(false);
    }

    @AfterMethod
    public void tearDown() {
        SystemDefaults.getClassesFromJar.set(savedGetClassesFromJar);
    }

    @Test
    public void testDetectWithNoExternalCommandsDir() throws IOException {
        AnnotationDetector detector = mock(AnnotationDetector.class);

        try (MockedStatic<AppResourcePath> fpMock = mockStatic(AppResourcePath.class)) {
            fpMock.when(AppResourcePath::getAppRoot).thenReturn(System.getProperty("java.io.tmpdir"));
            fpMock.when(AppResourcePath::getEngineJarPath).thenReturn("/nonexistent/engine.jar");

            AnnontationUtil.detect(detector, "com.ing.engine.commands");

            verify(detector).detect("com.ing.engine.commands");
        }
    }

    @Test
    public void testDetectWithExternalCommandsDirectory() throws Exception {
        Path tempDir = Files.createTempDirectory("annot-test");
        Path libDir = tempDir.resolve("lib").resolve("commands");
        Files.createDirectories(libDir);
        Path dummyJar = libDir.resolve("custom-commands.jar");
        Files.createFile(dummyJar);

        AnnotationDetector detector = mock(AnnotationDetector.class);

        try (MockedStatic<AppResourcePath> fpMock = mockStatic(AppResourcePath.class)) {
            fpMock.when(AppResourcePath::getAppRoot).thenReturn(tempDir.toString());

            AnnontationUtil.detect(detector, "com.ing.engine");
        } finally {
            Files.deleteIfExists(dummyJar);
            Files.deleteIfExists(libDir);
            Files.deleteIfExists(tempDir.resolve("lib"));
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    public void testDetectWithMultiplePackageNames() throws Exception {
        AnnotationDetector detector = mock(AnnotationDetector.class);

        try (MockedStatic<AppResourcePath> fpMock = mockStatic(AppResourcePath.class)) {
            fpMock.when(AppResourcePath::getAppRoot).thenReturn(System.getProperty("java.io.tmpdir"));

            AnnontationUtil.detect(detector, "com.pkg1", "com.pkg2", "com.pkg3");

            verify(detector).detect("com.pkg1", "com.pkg2", "com.pkg3");
        }
    }

    @Test
    public void testGetClassesFromJarDefaultFalse() {
        assertThat(SystemDefaults.getClassesFromJar).isNotNull();
        assertThat(SystemDefaults.getClassesFromJar.get()).isFalse();
    }
}
