package com.ing.datalib.or.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.mobile.MobileORPage;
import com.ing.datalib.or.sap.SapOR;
import com.ing.datalib.or.sap.SapORPage;
import com.ing.datalib.or.structureddata.StructuredDataOR;
import com.ing.datalib.or.structureddata.StructuredDataORPage;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebORPage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads YAML-based Object Repository files.
 *
 * Directory Structure:
 * <pre>
 * ObjectRepository/
 *   Web/
 *     pages/
 *       HomePage.yaml
 *       LoginPage.yaml
 *       ContactUs.yaml
 *   Mobile/
 *     pages/
 *       LoginScreen.yaml
 *       DashboardScreen.yaml
 * </pre>
 */
public class YamlORReader {
    private static final Logger LOGGER = Logger.getLogger(YamlORReader.class.getName());

    private final ObjectMapper yamlMapper;

    private ObjectRepository objectRepository;

    public YamlORReader() {
        YAMLFactory factory = new YAMLFactory();
        factory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        this.yamlMapper = new ObjectMapper(factory);
        // Configure for clean YAML
        this.yamlMapper.findAndRegisterModules();
    }

    public YamlORReader(ObjectRepository objectRepository) {
        this.objectRepository = objectRepository;
        YAMLFactory factory = new YAMLFactory();
        factory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        this.yamlMapper = new ObjectMapper(factory);
        this.yamlMapper.findAndRegisterModules();
    }

    /**
     * Check if a YAML-based Web OR exists.
     */
    public boolean webORExists(File orLocation) {
        File webPagesDir = new File(orLocation, "Web");
        return webPagesDir.exists() && webPagesDir.isDirectory();
    }

    /**
     * Check if a YAML-based Mobile OR exists.
     */
    public boolean mobileORExists(File orLocation) {
        File mobilePagesDir = new File(orLocation, "Mobile");
        return mobilePagesDir.exists() && mobilePagesDir.isDirectory();
    }

    /**
     * Check if a YAML-based Structured Data OR exists.
     */
    public boolean structuredDataORExists(File orLocation) {
        File structuredDataPagesDir = new File(orLocation, "StructuredData");
        return structuredDataPagesDir.exists() && structuredDataPagesDir.isDirectory();
    }

    /**
     * Check if a YAML-based SAP OR exists.
     */
    public boolean sapORExists(File orLocation) {
        File sapPagesDir = new File(orLocation, "SAP");
        return sapPagesDir.exists() && sapPagesDir.isDirectory();
    }

    /**
     * Read Web OR from YAML files.
     *
     * @param orLocation The ObjectRepository directory
     * @return WebOR populated with pages from YAML files
     */
    public WebOR readWebOR(File orLocation) throws IOException {
        WebOR webOR = new WebOR();
        File webPagesDir = new File(orLocation, "Web");

        if (orLocation.getPath().contains(File.separator + "Shared" + File.separator)) {
            webOR.setScope(WebOR.ORScope.SHARED);
        } else {
            webOR.setScope(WebOR.ORScope.PROJECT);
        }

        if (!webPagesDir.exists()) {
            LOGGER.info("No Web OR YAML directory found at: " + webPagesDir.getAbsolutePath());
            return webOR;
        }

        List<File> yamlFiles = listYamlFiles(webPagesDir);
        LOGGER.info("Found " + yamlFiles.size() + " Web OR YAML files");

        for (File yamlFile : yamlFiles) {
            try {
                YamlPageDefinition pageDef = yamlMapper.readValue(
                    yamlFile,
                    YamlPageDefinition.class
                );
                WebORPage page = pageDef.toWebORPage(webOR);
                webOR.getPages().add(page);
                LOGGER.fine(
                    "Loaded Web page: " +
                    page.getName() +
                    " with " +
                    pageDef.getElementCount() +
                    " elements"
                );
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to read YAML file: " + yamlFile.getName(), e);
            }
        }

        return webOR;
    }

    /**
     * Read Mobile OR from YAML files.
     *
     * @param orLocation The ObjectRepository directory
     * @return MobileOR populated with pages from YAML files
     */
    public MobileOR readMobileOR(File orLocation) throws IOException {
        MobileOR mobileOR = new MobileOR();
        File mobilePagesDir = new File(orLocation, "Mobile");

        if (orLocation.getPath().contains(File.separator + "Shared" + File.separator)) {
            mobileOR.setScope(MobileOR.ORScope.SHARED);
        } else {
            mobileOR.setScope(MobileOR.ORScope.PROJECT);
        }

        if (!mobilePagesDir.exists()) {
            LOGGER.info(
                "No Mobile OR YAML directory found at: " + mobilePagesDir.getAbsolutePath()
            );
            return mobileOR;
        }

        List<File> yamlFiles = listYamlFiles(mobilePagesDir);
        LOGGER.info("Found " + yamlFiles.size() + " Mobile OR YAML files");

        for (File yamlFile : yamlFiles) {
            try {
                YamlMobilePageDefinition pageDef = yamlMapper.readValue(
                    yamlFile,
                    YamlMobilePageDefinition.class
                );
                MobileORPage page = pageDef.toMobileORPage(mobileOR);
                mobileOR.getPages().add(page);
                LOGGER.fine(
                    "Loaded Mobile page: " +
                    page.getName() +
                    " with " +
                    pageDef.getElementCount() +
                    " elements"
                );
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to read YAML file: " + yamlFile.getName(), e);
            }
        }

        return mobileOR;
    }

    /**
     * Read Structured Data OR from YAML files.
     *
     * @param orLocation The ObjectRepository directory
     * @return StructuredDataOR populated with pages from YAML files
     */
    public StructuredDataOR readStructuredDataOR(File orLocation) throws IOException {
        StructuredDataOR structuredDataOR = new StructuredDataOR();
        File structuredDataPagesDir = new File(orLocation, "StructuredData");

        if (orLocation.getPath().contains(File.separator + "Shared" + File.separator)) {
            structuredDataOR.setScope(StructuredDataOR.ORScope.SHARED);
        } else {
            structuredDataOR.setScope(StructuredDataOR.ORScope.PROJECT);
        }

        if (!structuredDataPagesDir.exists()) {
            LOGGER.info(
                "No Structured Data OR YAML directory found at: " +
                structuredDataPagesDir.getAbsolutePath()
            );
            return structuredDataOR;
        }

        List<File> yamlFiles = listYamlFiles(structuredDataPagesDir);
        LOGGER.info("Found " + yamlFiles.size() + " Structured Data OR YAML files");

        for (File yamlFile : yamlFiles) {
            try {
                YamlStructuredDataPageDefinition pageDef = yamlMapper.readValue(
                    yamlFile,
                    YamlStructuredDataPageDefinition.class
                );
                StructuredDataORPage page = pageDef.toStructuredDataORPage(structuredDataOR);
                structuredDataOR.getPages().add(page);
                LOGGER.fine(
                    "Loaded Structured Data page: " +
                    page.getName() +
                    " with " +
                    pageDef.getElementCount() +
                    " elements"
                );
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to read YAML file: " + yamlFile.getName(), e);
            }
        }

        return structuredDataOR;
    }

    /**
     * Read a single Web page from a YAML file.
     */
    public WebORPage readWebPage(File yamlFile, WebOR root) throws IOException {
        YamlPageDefinition pageDef = yamlMapper.readValue(yamlFile, YamlPageDefinition.class);
        return pageDef.toWebORPage(root);
    }

    /**
     * Read a single Mobile page from a YAML file.
     */
    public MobileORPage readMobilePage(File yamlFile, MobileOR root) throws IOException {
        YamlMobilePageDefinition pageDef = yamlMapper.readValue(
            yamlFile,
            YamlMobilePageDefinition.class
        );
        return pageDef.toMobileORPage(root);
    }

    /**
     * Read a single Structured Data page from a YAML file.
     */
    public StructuredDataORPage readStructuredDataPage(File yamlFile, StructuredDataOR root)
        throws IOException {
        YamlStructuredDataPageDefinition pageDef = yamlMapper.readValue(
            yamlFile,
            YamlStructuredDataPageDefinition.class
        );
        return pageDef.toStructuredDataORPage(root);
    }

    /**
     * Read SAP OR from YAML files.
     *
     * @param orLocation The ObjectRepository directory
     * @return SapOR populated with pages from YAML files
     */
    public SapOR readSapOR(File orLocation) throws IOException {
        SapOR sapOR = new SapOR();
        File sapPagesDir = new File(orLocation, "SAP");

        if (orLocation.getPath().contains(File.separator + "Shared" + File.separator)) {
            sapOR.setScope(SapOR.ORScope.SHARED);
        } else {
            sapOR.setScope(SapOR.ORScope.PROJECT);
        }

        if (!sapPagesDir.exists()) {
            LOGGER.info("No SAP OR YAML directory found at: " + sapPagesDir.getAbsolutePath());
            return sapOR;
        }

        List<File> yamlFiles = listYamlFiles(sapPagesDir);
        LOGGER.info("Found " + yamlFiles.size() + " SAP OR YAML files");

        for (File yamlFile : yamlFiles) {
            try {
                YamlSapPageDefinition pageDef = yamlMapper.readValue(
                    yamlFile,
                    YamlSapPageDefinition.class
                );
                SapORPage page = pageDef.toSapORPage(sapOR);
                sapOR.getPages().add(page);
                LOGGER.fine(
                    "Loaded SAP page: " +
                    page.getName() +
                    " with " +
                    pageDef.getElementCount() +
                    " elements"
                );
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to read YAML file: " + yamlFile.getName(), e);
            }
        }

        return sapOR;
    }

    /**
     * Read a single SAP page from a YAML file.
     */
    public SapORPage readSapPage(File yamlFile, SapOR root) throws IOException {
        YamlSapPageDefinition pageDef = yamlMapper.readValue(yamlFile, YamlSapPageDefinition.class);
        return pageDef.toSapORPage(root);
    }

    /**
     * List all YAML files in a directory.
     */
    private List<File> listYamlFiles(File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return new ArrayList<>();
        }

        try (Stream<Path> paths = Files.walk(directory.toPath(), 1)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(
                    p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".yaml") || name.endsWith(".yml");
                    }
                )
                .map(Path::toFile)
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error listing YAML files in: " + directory, e);
            return new ArrayList<>();
        }
    }

    /**
     * Get the ObjectMapper for external use.
     */
    public ObjectMapper getYamlMapper() {
        return yamlMapper;
    }
}
