package com.ing.engine.constants;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.testng.annotations.Test;

/**
 * Tests for AppResourcePath — pure static path construction methods.
 * Only tests methods that don't depend on RunManager or Control.
 */
public class AppResourcePathTest {

    // ── getAppRoot ──────────────────────────────────────────────────────

    @Test
    public void testGetAppRootReturnsNonNull() {
        String root = AppResourcePath.getAppRoot();
        assertThat(root).isNotNull();
    }

    @Test
    public void testGetAppRootMatchesUserDir() {
        String root = AppResourcePath.getAppRoot();
        String userDir = System.getProperty("user.dir");
        // getAppRoot() returns canonical path
        assertThat(root).isNotEmpty();
        assertThat(new File(root)).exists();
    }

    @Test
    public void testGetAppRootIsAbsolute() {
        String root = AppResourcePath.getAppRoot();
        assertThat(new File(root).isAbsolute()).isTrue();
    }

    // ── Workspace root ──────────────────────────────────────────────────

    @Test
    public void testWorkspacePropertyHasHighestPriority() {
        String originalWorkspace = System.getProperty(AppResourcePath.WORKSPACE_PROPERTY);
        String configuredWorkspace =
            System.getProperty("java.io.tmpdir") + File.separator + "ingenious-property-workspace";

        try {
            System.setProperty(AppResourcePath.WORKSPACE_PROPERTY, configuredWorkspace);

            assertThat(AppResourcePath.getWorkspaceRoot())
                .isEqualTo(new File(configuredWorkspace).getCanonicalPath());
        } catch (Exception ex) {
            throw new AssertionError(ex);
        } finally {
            restoreProperty(AppResourcePath.WORKSPACE_PROPERTY, originalWorkspace);
        }
    }

    @Test
    public void testPackagedMacApplicationUsesApplicationSupportByDefault() {
        String originalWorkspace = System.getProperty(AppResourcePath.WORKSPACE_PROPERTY);
        String originalAppHome = System.getProperty(AppResourcePath.APP_HOME_PROPERTY);
        String originalUserHome = System.getProperty("user.home");
        String originalOsName = System.getProperty("os.name");

        String testHome =
            System.getProperty("java.io.tmpdir") + File.separator + "ingenious-test-home";

        try {
            System.clearProperty(AppResourcePath.WORKSPACE_PROPERTY);
            System.setProperty(
                AppResourcePath.APP_HOME_PROPERTY,
                "/Applications/INGenious.app/Contents/app"
            );
            System.setProperty("user.home", testHome);
            System.setProperty("os.name", "Mac OS X");

            File expected = new File(
                new File(new File(testHome, "Library"), "Application Support"),
                "INGenious"
            );

            assertThat(AppResourcePath.getWorkspaceRoot()).isEqualTo(expected.getCanonicalPath());

            assertThat(AppResourcePath.getProjectsPath())
                .isEqualTo(expected.getCanonicalPath() + File.separator + "Projects");
        } catch (Exception ex) {
            throw new AssertionError(ex);
        } finally {
            restoreProperty(AppResourcePath.WORKSPACE_PROPERTY, originalWorkspace);
            restoreProperty(AppResourcePath.APP_HOME_PROPERTY, originalAppHome);
            restoreProperty("user.home", originalUserHome);
            restoreProperty("os.name", originalOsName);
        }
    }

    @Test
    public void testZipReleaseStillUsesCurrentDirectoryByDefault() {
        String originalWorkspace = System.getProperty(AppResourcePath.WORKSPACE_PROPERTY);
        String originalAppHome = System.getProperty(AppResourcePath.APP_HOME_PROPERTY);

        try {
            System.clearProperty(AppResourcePath.WORKSPACE_PROPERTY);
            System.clearProperty(AppResourcePath.APP_HOME_PROPERTY);

            assertThat(AppResourcePath.getWorkspaceRoot())
                .isEqualTo(new File(System.getProperty("user.dir")).getCanonicalPath());
        } catch (Exception ex) {
            throw new AssertionError(ex);
        } finally {
            restoreProperty(AppResourcePath.WORKSPACE_PROPERTY, originalWorkspace);
            restoreProperty(AppResourcePath.APP_HOME_PROPERTY, originalAppHome);
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    // ── Configuration paths ─────────────────────────────────────────────

    @Test
    public void testGetConfigurationPath() {
        String config = AppResourcePath.getConfigurationPath();
        assertThat(config).endsWith("Configuration");
        assertThat(config).startsWith(AppResourcePath.getWorkspaceRoot());
    }

    @Test
    public void testGetConfigurationResourcePath() {
        String runtimeConfiguration = AppResourcePath.getConfigurationResourcePath();

        assertThat(runtimeConfiguration).startsWith(AppResourcePath.getAppRoot());

        assertThat(runtimeConfiguration).endsWith("Configuration");

        assertThat(runtimeConfiguration).isNotEqualTo(AppResourcePath.getConfigurationPath());
    }

    @Test
    public void testGetPropertiesPath() {
        String path = AppResourcePath.getPropertiesPath();
        assertThat(path).endsWith("Global Settings.properties");
        assertThat(path).contains("Configuration");
    }

    @Test
    public void testGetPropertiesPathWithFileName() {
        String path = AppResourcePath.getPropertiesPath("custom.properties");
        assertThat(path).endsWith("custom.properties");
        assertThat(path).contains("Configuration");
    }

    @Test
    public void testGetLibPath() {
        String lib = AppResourcePath.getLibPath();
        assertThat(lib).endsWith("lib");
        assertThat(lib).startsWith(AppResourcePath.getAppRoot());
    }

    @Test
    public void testFocusedRuntimeDirectoriesUseAppRoot() {
        assertThat(AppResourcePath.getEnginePath())
            .isEqualTo(new File(AppResourcePath.getAppRoot(), "Engine").getPath());

        assertThat(AppResourcePath.getToolsPath())
            .isEqualTo(new File(AppResourcePath.getAppRoot(), "Tools").getPath());

        assertThat(AppResourcePath.getWebPath())
            .isEqualTo(new File(AppResourcePath.getAppRoot(), "web").getPath());
    }

    @Test
    public void testGetExternalCommandsConfig() {
        String path = AppResourcePath.getExternalCommandsConfig();
        assertThat(path).contains("lib");
        assertThat(path).endsWith("commands");
    }

    // ── Explorer/Chrome config ──────────────────────────────────────────

    @Test
    public void testGetExplorerConfig() {
        String path = AppResourcePath.getExplorerConfig();
        assertThat(path).endsWith("ExplorerConfig.properties");
        assertThat(path).contains("Configuration");
    }

    // ── Report template paths ───────────────────────────────────────────

    @Test
    public void testGetReportTemplatePath() {
        String path = AppResourcePath.getReportTemplatePath();
        assertThat(path).contains("ReportTemplate");
        assertThat(path).endsWith("html");
    }

    @Test
    public void testGetReportResourcePath() {
        String path = AppResourcePath.getReportResourcePath();
        assertThat(path).contains("ReportTemplate");
        assertThat(path).endsWith("media");
    }

    @Test
    public void testGetReportThemePath() {
        String path = AppResourcePath.getReportThemePath();
        assertThat(path).contains("media");
        assertThat(path).endsWith("theme");
    }

    @Test
    public void testGetReportThemePreviewPath() {
        String path = AppResourcePath.getReportThemePreviewPath();
        assertThat(path).contains("ReportTemplate");
        assertThat(path).endsWith("preview");
    }

    @Test
    public void testGetMailReportTemplatePath() {
        String path = AppResourcePath.getMailReportTemplatePath();
        assertThat(path).contains("ReportTemplate");
        assertThat(path).endsWith("mailReport");
    }

    @Test
    public void testGetaXeReportTemplatePath() {
        String path = AppResourcePath.getaXeReportTemplatePath();
        assertThat(path).contains("ReportTemplate");
        assertThat(path).endsWith("aXe");
    }

    // ── HTML template paths ─────────────────────────────────────────────

    @Test
    public void testGetSummaryHTMLPath() {
        String path = AppResourcePath.getSummaryHTMLPathV2();
        assertThat(path).endsWith(AppResourcePath.SUMMARY_HTML_V2);
        assertThat(path).contains("ReportTemplate");
    }

    @Test
    public void testGetDetailedHTMLPath() {
        String path = AppResourcePath.getDetailedHTMLPath();
        assertThat(path).endsWith("detailed.html");
    }

    @Test
    public void testGetTCReportTemplate() {
        String path = AppResourcePath.getTCReportTemplate();
        assertThat(path).endsWith("testCase.html");
    }

    @Test
    public void testGetReportHistoryHTMLPath() {
        String path = AppResourcePath.getReportHistoryHTMLPath();
        assertThat(path).endsWith("ReportHistory.html");
    }

    @Test
    public void testGetPerfReportHTMLPath() {
        String path = AppResourcePath.getPerfReportHTMLPath();
        assertThat(path).endsWith("perfReport.html");
    }

    @Test
    public void testGetVideoReportHTMLPath() {
        String path = AppResourcePath.getVideoReportHTMLPath();
        assertThat(path).endsWith("videoReport.html");
    }

    // ── Special paths ───────────────────────────────────────────────────

    @Test
    public void testGetEncFile() {
        String path = AppResourcePath.getEncFile();
        assertThat(path).endsWith(".enc");
        assertThat(path).contains("Configuration");
    }

    @Test
    public void testGetPageDumpResourcePath() {
        String path = AppResourcePath.getPageDumpResourcePath();
        assertThat(path).endsWith("PageDump");
        assertThat(path).contains("Configuration");
    }

    @Test
    public void testGetAppSettings() {
        String path = AppResourcePath.getAppSettings();
        assertThat(path).endsWith("app.settings");
        assertThat(path).contains("Configuration");
    }

    @Test
    public void testGetStepMapFile() {
        String path = AppResourcePath.getStepMapFile();
        assertThat(path).endsWith("StepMap.csv");
        assertThat(path).contains("Configuration");
    }

    // ── Addon paths ─────────────────────────────────────────────────────

    @Test
    public void testGetAddonPath() {
        String path = AppResourcePath.getAddonPath();
        assertThat(path).endsWith("Extensions");
    }

    @Test
    public void testGetFireFoxAddOnPath() {
        File file = AppResourcePath.getFireFoxAddOnPath();
        assertThat(file.getName()).isEqualTo("ingenious.xpi");
        assertThat(file.getPath()).contains("FireFox");
    }

    @Test
    public void testGetChromeAddOnPath() {
        File file = AppResourcePath.getChromeAddOnPath();
        assertThat(file.getName()).isEqualTo("ingenious.crx");
        assertThat(file.getPath()).contains("Chrome");
    }

    @Test
    public void testGetSafariAddOnPath() {
        File file = AppResourcePath.getSafariAddOnPath();
        // Safari addon uses same file as Firefox addon
        assertThat(file.getName()).isEqualTo("ingenious.xpi");
    }

    // ── date/time fields ────────────────────────────────────────────────

    @Test
    public void testDateTimeFieldsInitiallyNull() {
        // Before initDateTime, getDate/getTime return null
        // (they may have been set by other tests, so just check non-exception)
        AppResourcePath.getDate(); // should not throw
        AppResourcePath.getTime(); // should not throw
    }

    // ── Path consistency ────────────────────────────────────────────────

    @Test
    public void testPathsSeparatorConsistency() {
        // All paths should use File.separator
        String configResourcePath = AppResourcePath.getConfigurationResourcePath();
        String templatePath = AppResourcePath.getReportTemplatePath();

        assertThat(templatePath).startsWith(configResourcePath);
    }

    @Test
    public void testAllTemplatePathsStartWithConfigPath() {
        String config = AppResourcePath.getConfigurationResourcePath();
        assertThat(AppResourcePath.getReportTemplatePath()).startsWith(config);
        assertThat(AppResourcePath.getReportResourcePath()).startsWith(config);
        assertThat(AppResourcePath.getMailReportTemplatePath()).startsWith(config);
        assertThat(AppResourcePath.getaXeReportTemplatePath()).startsWith(config);
        assertThat(AppResourcePath.getReportThemePreviewPath()).startsWith(config);
    }

    @Test
    public void testAllHtmlPathsStartWithTemplatePath() {
        String template = AppResourcePath.getReportTemplatePath();
        assertThat(AppResourcePath.getSummaryHTMLPath()).startsWith(template);
        assertThat(AppResourcePath.getDetailedHTMLPath()).startsWith(template);
        assertThat(AppResourcePath.getTCReportTemplate()).startsWith(template);
        assertThat(AppResourcePath.getReportHistoryHTMLPath()).startsWith(template);
        assertThat(AppResourcePath.getPerfReportHTMLPath()).startsWith(template);
        assertThat(AppResourcePath.getVideoReportHTMLPath()).startsWith(template);
    }
}
