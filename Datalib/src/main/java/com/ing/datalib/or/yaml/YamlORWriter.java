package com.ing.datalib.or.yaml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.mobile.MobileORPage;
import com.ing.datalib.or.sap.SapOR;
import com.ing.datalib.or.sap.SapORPage;
import com.ing.datalib.or.structureddata.StructuredDataOR;
import com.ing.datalib.or.structureddata.StructuredDataORPage;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebORPage;

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
        File webPagesDir = new File(orLocation, "Web");
        ensureDirectory(webPagesDir);
        
        List<WebORPage> pages = webOR.getPages();
        LOGGER.info(() -> "Writing " + pages.size() + " Web pages to YAML");
        
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
        File mobilePagesDir = new File(orLocation, "Mobile");
        ensureDirectory(mobilePagesDir);
        
        List<MobileORPage> pages = mobileOR.getPages();
        LOGGER.info(() -> "Writing " + pages.size() + " Mobile pages to YAML");
        
        for (MobileORPage page : pages) {
            writeMobilePage(page, mobilePagesDir);
        }
    }
    
    /**
     * Write entire Structured Data OR to YAML files (one per page).
     * 
     * @param structuredDataOR The StructuredDataOR to write
     * @param orLocation The ObjectRepository directory
     */
    public void writeStructuredDataOR(StructuredDataOR structuredDataOR, File orLocation) throws IOException {
        File structuredDataPagesDir = new File(orLocation, "StructuredData");
        ensureDirectory(structuredDataPagesDir);
        
        List<StructuredDataORPage> pages = structuredDataOR.getPages();
        LOGGER.info("Writing " + pages.size() + " Structured Data pages to YAML");
        
        for (StructuredDataORPage page : pages) {
            writeStructuredDataPage(page, structuredDataPagesDir);
        }
    }
    
    /**
     * Write entire SAP OR to YAML files (one per page).
     * 
     * @param sapOR The SapOR to write
     * @param orLocation The ObjectRepository directory
     */
    public void writeSapOR(SapOR sapOR, File orLocation) throws IOException {
        File sapPagesDir = new File(orLocation, "SAP");
        ensureDirectory(sapPagesDir);
        
        List<SapORPage> pages = sapOR.getPages();
        LOGGER.info("Writing " + pages.size() + " SAP pages to YAML");
        
        for (SapORPage page : pages) {
            writeSapPage(page, sapPagesDir);
        }
    }
    
    /**
     * Write a single Web page to YAML.
     */
    public void writeWebPage(WebORPage page, File pagesDir) throws IOException {
        YamlPageDefinition pageDef = YamlPageDefinition.fromWebORPage(page);
        File yamlFile = new File(pagesDir, sanitizeFileName(page.getName()) + ".yaml");
        
        yamlMapper.writeValue(yamlFile, pageDef);
        LOGGER.fine(() -> "Wrote Web page: " + yamlFile.getName());
    }
    
    /**
     * Write a single Mobile page to YAML.
     */
    public void writeMobilePage(MobileORPage page, File pagesDir) throws IOException {
        YamlMobilePageDefinition pageDef = YamlMobilePageDefinition.fromMobileORPage(page);
        File yamlFile = new File(pagesDir, sanitizeFileName(page.getName()) + ".yaml");
        
        yamlMapper.writeValue(yamlFile, pageDef);
        LOGGER.fine(() -> "Wrote Mobile page: " + yamlFile.getName());
    }
    
    /**
     * Write a single Structured Data page to YAML.
     */
    public void writeStructuredDataPage(StructuredDataORPage page, File pagesDir) throws IOException {
        YamlStructuredDataPageDefinition pageDef = YamlStructuredDataPageDefinition.fromStructuredDataORPage(page);
        File yamlFile = new File(pagesDir, sanitizeFileName(page.getName()) + ".yaml");
        
        yamlMapper.writeValue(yamlFile, pageDef);
        LOGGER.fine("Wrote Structured Data page: " + yamlFile.getName());
    }
    
    /**
     * Write a single SAP page to YAML.
     */
    public void writeSapPage(SapORPage page, File pagesDir) throws IOException {
        YamlSapPageDefinition pageDef = YamlSapPageDefinition.fromSapORPage(page);
        File yamlFile = new File(pagesDir, sanitizeFileName(page.getName()) + ".yaml");
        
        yamlMapper.writeValue(yamlFile, pageDef);
        LOGGER.fine("Wrote SAP page: " + yamlFile.getName());
    }
    
    /**
     * Delete a Web page YAML file.
     */
    public boolean deleteWebPage(String pageName, File orLocation) {
        File webPagesDir = new File(orLocation, "Web");
        File yamlFile = new File(webPagesDir, sanitizeFileName(pageName) + ".yaml");
        
        if (yamlFile.exists()) {
            boolean deleted = yamlFile.delete();
            if (deleted) {
                LOGGER.info(() -> "Deleted Web page YAML: " + pageName);
            }
            return deleted;
        }
        return false;
    }
    
    /**
     * Delete a Mobile page YAML file.
     */
    public boolean deleteMobilePage(String pageName, File orLocation) {
        File mobilePagesDir = new File(orLocation, "Mobile");
        File yamlFile = new File(mobilePagesDir, sanitizeFileName(pageName) + ".yaml");
        
        if (yamlFile.exists()) {
            boolean deleted = yamlFile.delete();
            if (deleted) {
                LOGGER.info(() -> "Deleted Mobile page YAML: " + pageName);
            }
            return deleted;
        }
        return false;
    }
    
    /**
     * Delete an Structured Data page YAML file.
     */
    public boolean deleteStructuredDataPage(String pageName, File orLocation) {
        File structuredDataPagesDir = new File(orLocation, "StructuredData");
        File yamlFile = new File(structuredDataPagesDir, sanitizeFileName(pageName) + ".yaml");
        
        if (yamlFile.exists()) {
            boolean deleted = yamlFile.delete();
            if (deleted) {
                LOGGER.info("Deleted Structured Data page YAML: " + pageName);
            }
            return deleted;
        }
        return false;
    }
    
    /**
     * Delete a SAP page YAML file.
     */
    public boolean deleteSapPage(String pageName, File orLocation) {
        File sapPagesDir = new File(orLocation, "SAP");
        File yamlFile = new File(sapPagesDir, sanitizeFileName(pageName) + ".yaml");
        
        if (yamlFile.exists()) {
            boolean deleted = yamlFile.delete();
            if (deleted) {
                LOGGER.info("Deleted SAP page YAML: " + pageName);
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
        File webPagesDir = new File(orLocation, "Web");
        File oldFile = new File(webPagesDir, sanitizeFileName(oldName) + ".yaml");
        File newFile = new File(webPagesDir, sanitizeFileName(newName) + ".yaml");
        
        if (oldFile.exists() && !newFile.exists()) {
            boolean renamed = oldFile.renameTo(newFile);
            if (renamed) {
                LOGGER.info(() -> "Renamed Web page YAML: " + oldName + " -> " + newName);
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
        File mobilePagesDir = new File(orLocation, "Mobile");
        File oldFile = new File(mobilePagesDir, sanitizeFileName(oldName) + ".yaml");
        File newFile = new File(mobilePagesDir, sanitizeFileName(newName) + ".yaml");
        
        if (oldFile.exists() && !newFile.exists()) {
            boolean renamed = oldFile.renameTo(newFile);
            if (renamed) {
                LOGGER.info(() -> "Renamed Mobile page YAML: " + oldName + " -> " + newName);
            }
            return renamed;
        }
        return false;
    }
    
    /**
     * Rename an Structured Data page YAML file.
     * 
     * @param oldName The current page name
     * @param newName The new page name
     * @param orLocation The ObjectRepository directory
     * @return true if rename was successful
     */
    public boolean renameStructuredDataPage(String oldName, String newName, File orLocation) {
        File structuredDataPagesDir = new File(orLocation, "StructuredData");
        File oldFile = new File(structuredDataPagesDir, sanitizeFileName(oldName) + ".yaml");
        File newFile = new File(structuredDataPagesDir, sanitizeFileName(newName) + ".yaml");
        
        if (oldFile.exists() && !newFile.exists()) {
            boolean renamed = oldFile.renameTo(newFile);
            if (renamed) {
                LOGGER.info("Renamed Structured Data page YAML: " + oldName + " -> " + newName);
            }
            return renamed;
        }
        return false;
    }
    
    /**
     * Rename a SAP page YAML file.
     * 
     * @param oldName The current page name
     * @param newName The new page name
     * @param orLocation The ObjectRepository directory
     * @return true if rename was successful
     */
    public boolean renameSapPage(String oldName, String newName, File orLocation) {
        File sapPagesDir = new File(orLocation, "SAP");
        File oldFile = new File(sapPagesDir, sanitizeFileName(oldName) + ".yaml");
        File newFile = new File(sapPagesDir, sanitizeFileName(newName) + ".yaml");
        
        if (oldFile.exists() && !newFile.exists()) {
            boolean renamed = oldFile.renameTo(newFile);
            if (renamed) {
                LOGGER.info("Renamed SAP page YAML: " + oldName + " -> " + newName);
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
    public void convertFromXml(WebOR webOR, MobileOR mobileOR, StructuredDataOR structuredDataOR, File orLocation) throws IOException {
        LOGGER.info("Converting OR from XML to YAML format");
        
        if (webOR != null && !webOR.getPages().isEmpty()) {
            writeWebOR(webOR, orLocation);
            writeSharedMetadata(webOR, orLocation);
            LOGGER.info(() -> "Converted " + webOR.getPages().size() + " Web pages to YAML");
        }
        
        if (mobileOR != null && !mobileOR.getPages().isEmpty()) {
            writeMobileOR(mobileOR, orLocation);
            writeSharedMetadata(mobileOR, orLocation);
            LOGGER.info(() -> "Converted " + mobileOR.getPages().size() + " Mobile pages to YAML");
        }

        if (structuredDataOR != null && !structuredDataOR.getPages().isEmpty()) {
            writeStructuredDataOR(structuredDataOR, orLocation);
            writeSharedMetadata(structuredDataOR, orLocation);
            LOGGER.info(() -> "Converted " + structuredDataOR.getPages().size() + " Structured Data pages to YAML");
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

    private void writeSharedMetadataInternal(String orType, List<String> newProjects, String fileName, File sharedRoot) throws IOException {
        if (newProjects == null || newProjects.isEmpty()) {
            return;
        }
        File metadataFile = new File(sharedRoot, fileName);

        Set<String> mergedProjects = new LinkedHashSet<>();
        mergedProjects.addAll(readExistingProjects(metadataFile));
        mergedProjects.addAll(newProjects);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("type", orType);
        metadata.put("scope", "SHARED");
        metadata.put("projects", new ArrayList<>(mergedProjects));

        yamlMapper.writeValue(metadataFile, metadata);
    }

    public boolean sharedMetadataExists(String fileName, File sharedRoot) {
        return new File(sharedRoot, fileName).exists();
    }
    
    @SuppressWarnings("unchecked")
    public List<String> readExistingProjects(File metadataFile) {
        if (metadataFile == null || !metadataFile.exists()) {
            return List.of();
        }
        try {
            Map<String, Object> data = yamlMapper.readValue(metadataFile, Map.class);
            Object projectsObj = data.get("projects");
            if (projectsObj instanceof List<?>) {
                List<String> result = new ArrayList<>();
                for (Object o : (List<?>) projectsObj) {
                    if (o != null) {
                        result.add(o.toString().trim());
                    }
                }
                return result;
            }
        } catch (Exception e) {
        }
        return List.of();
    }
    public void writeSharedMetadata(WebOR webSharedOR, File sharedRoot) throws IOException {
        if (webSharedOR == null || !webSharedOR.isShared()) {
            return;
        }
        writeSharedMetadataInternal(
            "WebOR",
            webSharedOR.getSharedProjects(),
            "webor-projectsdata.yaml",
            sharedRoot
        );
    }

    public void writeSharedMetadata(MobileOR mobileSharedOR, File sharedRoot) throws IOException {
        if (mobileSharedOR == null || !mobileSharedOR.isShared()) {
            return;
        }
        writeSharedMetadataInternal(
            "MobileOR",
            mobileSharedOR.getSharedProjects(),
            "mobileor-projectsdata.yaml",
            sharedRoot
        );
    }
    
    public void writeSharedMetadata(StructuredDataOR structuredDataSharedOR, File sharedRoot) throws IOException {
        if (structuredDataSharedOR == null || !structuredDataSharedOR.isShared()) {
            return;
        }
        writeSharedMetadataInternal(
            "StructuredDataOR",
            structuredDataSharedOR.getSharedProjects(),
            "structuredDataor-projectsdata.yaml",
            sharedRoot
        );
    }

    public void writeSharedMetadata(SapOR sapSharedOR, File sharedRoot) throws IOException {
        if (sapSharedOR == null || !sapSharedOR.isShared()) {
            return;
        }
        writeSharedMetadataInternal(
            "SapOR",
            sapSharedOR.getProjects(),
            "sapor-projectsdata.yaml",
            sharedRoot
        );
    }
}
