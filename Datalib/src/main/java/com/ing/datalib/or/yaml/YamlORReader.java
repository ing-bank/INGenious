package com.ing.datalib.or.yaml;

import com.ing.datalib.or.api.APIOR;
import com.ing.datalib.or.api.APIORPage;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.mobile.MobileORPage;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebORPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

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
    
    public YamlORReader() {
        YAMLFactory factory = new YAMLFactory();
        factory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        this.yamlMapper = new ObjectMapper(factory);
        // Configure for clean YAML
        this.yamlMapper.findAndRegisterModules();
    }
    
    /**
     * Check if a YAML-based Web OR exists.
     */
    public boolean webORExists(File orLocation) {
        File webPagesDir = new File(orLocation, "Web/pages");
        return webPagesDir.exists() && webPagesDir.isDirectory();
    }
    
    /**
     * Check if a YAML-based Mobile OR exists.
     */
    public boolean mobileORExists(File orLocation) {
        File mobilePagesDir = new File(orLocation, "Mobile/pages");
        return mobilePagesDir.exists() && mobilePagesDir.isDirectory();
    }
    
    /**
     * Check if a YAML-based API OR exists.
     */
    public boolean apiORExists(File orLocation) {
        File apiPagesDir = new File(orLocation, "API/pages");
        return apiPagesDir.exists() && apiPagesDir.isDirectory();
    }
    
    /**
     * Read Web OR from YAML files.
     * 
     * @param orLocation The ObjectRepository directory
     * @return WebOR populated with pages from YAML files
     */
    public WebOR readWebOR(File orLocation) throws IOException {
        WebOR webOR = new WebOR();
        File webPagesDir = new File(orLocation, "Web/pages");
        
        if (!webPagesDir.exists()) {
            LOGGER.info("No Web OR YAML directory found at: " + webPagesDir.getAbsolutePath());
            return webOR;
        }
        
        List<File> yamlFiles = listYamlFiles(webPagesDir);
        LOGGER.info("Found " + yamlFiles.size() + " Web OR YAML files");
        
        for (File yamlFile : yamlFiles) {
            try {
                YamlPageDefinition pageDef = yamlMapper.readValue(yamlFile, YamlPageDefinition.class);
                WebORPage page = pageDef.toWebORPage(webOR);
                webOR.getPages().add(page);
                LOGGER.fine("Loaded Web page: " + page.getName() + " with " + pageDef.getElementCount() + " elements");
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
        File mobilePagesDir = new File(orLocation, "Mobile/pages");
        
        if (!mobilePagesDir.exists()) {
            LOGGER.info("No Mobile OR YAML directory found at: " + mobilePagesDir.getAbsolutePath());
            return mobileOR;
        }
        
        List<File> yamlFiles = listYamlFiles(mobilePagesDir);
        LOGGER.info("Found " + yamlFiles.size() + " Mobile OR YAML files");
        
        for (File yamlFile : yamlFiles) {
            try {
                YamlMobilePageDefinition pageDef = yamlMapper.readValue(yamlFile, YamlMobilePageDefinition.class);
                MobileORPage page = pageDef.toMobileORPage(mobileOR);
                mobileOR.getPages().add(page);
                LOGGER.fine("Loaded Mobile page: " + page.getName() + " with " + pageDef.getElementCount() + " elements");
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to read YAML file: " + yamlFile.getName(), e);
            }
        }
        
        return mobileOR;
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
        YamlMobilePageDefinition pageDef = yamlMapper.readValue(yamlFile, YamlMobilePageDefinition.class);
        return pageDef.toMobileORPage(root);
    }
    
    /**
     * Read API OR from YAML files.
     * 
     * @param orLocation The ObjectRepository directory
     * @return APIOR populated with pages from YAML files
     */
    public APIOR readAPIOR(File orLocation) throws IOException {
        APIOR apiOR = new APIOR();
        File apiPagesDir = new File(orLocation, "API/pages");
        
        if (!apiPagesDir.exists()) {
            LOGGER.info("No API OR YAML directory found at: " + apiPagesDir.getAbsolutePath());
            return apiOR;
        }
        
        List<File> yamlFiles = listYamlFiles(apiPagesDir);
        LOGGER.info("Found " + yamlFiles.size() + " API OR YAML files");
        
        for (File yamlFile : yamlFiles) {
            try {
                YamlAPIPageDefinition pageDef = yamlMapper.readValue(yamlFile, YamlAPIPageDefinition.class);
                APIORPage page = pageDef.toAPIORPage(apiOR);
                apiOR.getPages().add(page);
                LOGGER.fine("Loaded API page: " + page.getName() + " with " + pageDef.getElementCount() + " elements");
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to read YAML file: " + yamlFile.getName(), e);
            }
        }
        
        return apiOR;
    }
    
    /**
     * Read a single API page from a YAML file.
     */
    public APIORPage readAPIPage(File yamlFile, APIOR root) throws IOException {
        YamlAPIPageDefinition pageDef = yamlMapper.readValue(yamlFile, YamlAPIPageDefinition.class);
        return pageDef.toAPIORPage(root);
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
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return name.endsWith(".yaml") || name.endsWith(".yml");
                })
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
