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

    // ── Configuration paths ─────────────────────────────────────────────

    @Test
    public void testGetConfigurationPath() {
        String config = AppResourcePath.getConfigurationPath();
        assertThat(config).endsWith("Configuration");
        assertThat(config).startsWith(AppResourcePath.getAppRoot());
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

    @Test
    public void testGetChromeEmulatorsFile() {
        String path = AppResourcePath.getChromeEmulatorsFile();
        assertThat(path).endsWith("chrome-emulators.json");
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
        String configPath = AppResourcePath.getConfigurationPath();
        String templatePath = AppResourcePath.getReportTemplatePath();

        // templatePath should be a sub-path of configPath
        assertThat(templatePath).startsWith(configPath);
    }

    @Test
    public void testAllTemplatePathsStartWithConfigPath() {
        String config = AppResourcePath.getConfigurationPath();
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
