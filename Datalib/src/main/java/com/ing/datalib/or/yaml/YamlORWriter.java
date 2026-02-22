package com.ing.datalib.or.yaml;

import com.ing.datalib.or.api.APIOR;
import com.ing.datalib.or.api.APIORPage;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.mobile.MobileORPage;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebORPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes YAML-based Object Repository files.
 * 
 * Output Directory Structure:
 * <pre>
 * ObjectRepository/
 *   Web/
 *     pages/
 *       HomePage.yaml
 *       LoginPage.yaml
 *   Mobile/
 *     pages/
 *       LoginScreen.yaml
 * </pre>
 * 
 * Benefits of YAML format:
 * - 75% smaller file size (only non-empty properties)
 * - Clean Git diffs (one element per line)
 * - Human-readable and editable
 * - Page-per-file for better version control
 */
public class YamlORWriter {
    
    private static final Logger LOGGER = Logger.getLogger(YamlORWriter.class.getName());
    
    private final ObjectMapper yamlMapper;
    
    public YamlORWriter() {
        YAMLFactory factory = new YAMLFactory();
        factory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        factory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        factory.enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR);
        
        this.yamlMapper = new ObjectMapper(factory);
        this.yamlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.yamlMapper.findAndRegisterModules();
    }
    
    /**
     * Write entire Web OR to YAML files (one per page).
     * 
     * @param webOR The WebOR to write
     * @param orLocation The ObjectRepository directory
     */
    public void writeWebOR(WebOR webOR, File orLocation) throws IOException {
        File webPagesDir = new File(orLocation, "Web/pages");
        ensureDirectory(webPagesDir);
        
        List<WebORPage> pages = webOR.getPages();
        LOGGER.info("Writing " + pages.size() + " Web pages to YAML");
        
        for (WebORPage page : pages) {
            writeWebPage(page, webPagesDir);
        }
    }
    
    /**
     * Write entire Mobile OR to YAML files (one per page).
     * 
     * @param mobileOR The MobileOR to write
     * @param orLocation The ObjectRepository directory
     */
    public void writeMobileOR(MobileOR mobileOR, File orLocation) throws IOException {
        File mobilePagesDir = new File(orLocation, "Mobile/pages");
        ensureDirectory(mobilePagesDir);
        
        List<MobileORPage> pages = mobileOR.getPages();
        LOGGER.info("Writing " + pages.size() + " Mobile pages to YAML");
        
        for (MobileORPage page : pages) {
            writeMobilePage(page, mobilePagesDir);
        }
    }
    
    /**
     * Write entire API OR to YAML files (one per page).
     * 
     * @param apiOR The APIOR to write
     * @param orLocation The ObjectRepository directory
     */
    public void writeAPIOR(APIOR apiOR, File orLocation) throws IOException {
        File apiPagesDir = new File(orLocation, "API/pages");
        ensureDirectory(apiPagesDir);
        
        List<APIORPage> pages = apiOR.getPages();
        LOGGER.info("Writing " + pages.size() + " API pages to YAML");
        
        for (APIORPage page : pages) {
            writeAPIPage(page, apiPagesDir);
        }
    }
    
    /**
     * Write a single Web page to YAML.
     */
    public void writeWebPage(WebORPage page, File pagesDir) throws IOException {
        YamlPageDefinition pageDef = YamlPageDefinition.fromWebORPage(page);
        File yamlFile = new File(pagesDir, sanitizeFileName(page.getName()) + ".yaml");
        
        yamlMapper.writeValue(yamlFile, pageDef);
        LOGGER.fine("Wrote Web page: " + yamlFile.getName());
    }
    
    /**
     * Write a single Mobile page to YAML.
     */
    public void writeMobilePage(MobileORPage page, File pagesDir) throws IOException {
        YamlMobilePageDefinition pageDef = YamlMobilePageDefinition.fromMobileORPage(page);
        File yamlFile = new File(pagesDir, sanitizeFileName(page.getName()) + ".yaml");
        
        yamlMapper.writeValue(yamlFile, pageDef);
        LOGGER.fine("Wrote Mobile page: " + yamlFile.getName());
    }
    
    /**
     * Write a single API page to YAML.
     */
    public void writeAPIPage(APIORPage page, File pagesDir) throws IOException {
        YamlAPIPageDefinition pageDef = YamlAPIPageDefinition.fromAPIORPage(page);
        File yamlFile = new File(pagesDir, sanitizeFileName(page.getName()) + ".yaml");
        
        yamlMapper.writeValue(yamlFile, pageDef);
        LOGGER.fine("Wrote API page: " + yamlFile.getName());
    }
    
    /**
     * Delete a Web page YAML file.
     */
    public boolean deleteWebPage(String pageName, File orLocation) {
        File webPagesDir = new File(orLocation, "Web/pages");
        File yamlFile = new File(webPagesDir, sanitizeFileName(pageName) + ".yaml");
        
        if (yamlFile.exists()) {
            boolean deleted = yamlFile.delete();
            if (deleted) {
                LOGGER.info("Deleted Web page YAML: " + pageName);
            }
            return deleted;
        }
        return false;
    }
    
    /**
     * Delete a Mobile page YAML file.
     */
    public boolean deleteMobilePage(String pageName, File orLocation) {
        File mobilePagesDir = new File(orLocation, "Mobile/pages");
        File yamlFile = new File(mobilePagesDir, sanitizeFileName(pageName) + ".yaml");
        
        if (yamlFile.exists()) {
            boolean deleted = yamlFile.delete();
            if (deleted) {
                LOGGER.info("Deleted Mobile page YAML: " + pageName);
            }
            return deleted;
        }
        return false;
    }
    
    /**
     * Delete an API page YAML file.
     */
    public boolean deleteAPIPage(String pageName, File orLocation) {
        File apiPagesDir = new File(orLocation, "API/pages");
        File yamlFile = new File(apiPagesDir, sanitizeFileName(pageName) + ".yaml");
        
        if (yamlFile.exists()) {
            boolean deleted = yamlFile.delete();
            if (deleted) {
                LOGGER.info("Deleted API page YAML: " + pageName);
            }
            return deleted;
        }
        return false;
    }
    
    /**
     * Rename a Web page YAML file.
     * 
     * @param oldName The current page name
     * @param newName The new page name
     * @param orLocation The ObjectRepository directory
     * @return true if rename was successful
     */
    public boolean renameWebPage(String oldName, String newName, File orLocation) {
        File webPagesDir = new File(orLocation, "Web/pages");
        File oldFile = new File(webPagesDir, sanitizeFileName(oldName) + ".yaml");
        File newFile = new File(webPagesDir, sanitizeFileName(newName) + ".yaml");
        
        if (oldFile.exists() && !newFile.exists()) {
            boolean renamed = oldFile.renameTo(newFile);
            if (renamed) {
                LOGGER.info("Renamed Web page YAML: " + oldName + " -> " + newName);
            }
            return renamed;
        }
        return false;
    }
    
    /**
     * Rename a Mobile page YAML file.
     * 
     * @param oldName The current page name
     * @param newName The new page name
     * @param orLocation The ObjectRepository directory
     * @return true if rename was successful
     */
    public boolean renameMobilePage(String oldName, String newName, File orLocation) {
        File mobilePagesDir = new File(orLocation, "Mobile/pages");
        File oldFile = new File(mobilePagesDir, sanitizeFileName(oldName) + ".yaml");
        File newFile = new File(mobilePagesDir, sanitizeFileName(newName) + ".yaml");
        
        if (oldFile.exists() && !newFile.exists()) {
            boolean renamed = oldFile.renameTo(newFile);
            if (renamed) {
                LOGGER.info("Renamed Mobile page YAML: " + oldName + " -> " + newName);
            }
            return renamed;
        }
        return false;
    }
    
    /**
     * Rename an API page YAML file.
     * 
     * @param oldName The current page name
     * @param newName The new page name
     * @param orLocation The ObjectRepository directory
     * @return true if rename was successful
     */
    public boolean renameAPIPage(String oldName, String newName, File orLocation) {
        File apiPagesDir = new File(orLocation, "API/pages");
        File oldFile = new File(apiPagesDir, sanitizeFileName(oldName) + ".yaml");
        File newFile = new File(apiPagesDir, sanitizeFileName(newName) + ".yaml");
        
        if (oldFile.exists() && !newFile.exists()) {
            boolean renamed = oldFile.renameTo(newFile);
            if (renamed) {
                LOGGER.info("Renamed API page YAML: " + oldName + " -> " + newName);
            }
            return renamed;
        }
        return false;
    }
    
    /**
     * Convert existing XML OR to YAML format.
     * 
     * @param webOR The WebOR loaded from XML
     * @param mobileOR The MobileOR loaded from XML
     * @param orLocation The ObjectRepository directory
     */
    public void convertFromXml(WebOR webOR, MobileOR mobileOR, File orLocation) throws IOException {
        LOGGER.info("Converting OR from XML to YAML format");
        
        if (webOR != null && !webOR.getPages().isEmpty()) {
            writeWebOR(webOR, orLocation);
            LOGGER.info("Converted " + webOR.getPages().size() + " Web pages to YAML");
        }
        
        if (mobileOR != null && !mobileOR.getPages().isEmpty()) {
            writeMobileOR(mobileOR, orLocation);
            LOGGER.info("Converted " + mobileOR.getPages().size() + " Mobile pages to YAML");
        }
    }
    
    /**
     * Sanitize page name for use as filename.
     */
    private String sanitizeFileName(String name) {
        if (name == null) {
            return "unnamed";
        }
        // Replace invalid filename characters
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
    
    /**
     * Ensure directory exists, creating if necessary.
     */
    private void ensureDirectory(File dir) throws IOException {
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new IOException("Failed to create directory: " + dir.getAbsolutePath());
            }
            LOGGER.info("Created directory: " + dir.getAbsolutePath());
        }
    }
    
    /**
     * Get the ObjectMapper for external use.
     */
    public ObjectMapper getYamlMapper() {
        return yamlMapper;
    }
}
