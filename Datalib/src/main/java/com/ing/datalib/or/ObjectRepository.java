
package com.ing.datalib.or;

import com.ing.datalib.component.Project;
import com.ing.datalib.or.api.APIOR;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.yaml.YamlORReader;
import com.ing.datalib.or.yaml.YamlORWriter;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * 
 */
public class ObjectRepository {

    private static final Logger LOGGER = Logger.getLogger(ObjectRepository.class.getName());
    private static final XmlMapper XML_MAPPER = new XmlMapper();
    
    private final YamlORReader yamlReader = new YamlORReader();
    private final YamlORWriter yamlWriter = new YamlORWriter();

    private final Project sProject;
    
    // Default to YAML format for new projects
    private boolean usingYamlFormat = true;

    private WebOR webOR;
    private MobileOR mobileOR;
    private APIOR apiOR;

    public ObjectRepository(Project sProject) {
        this.sProject = sProject;
        init();
    }

    private void init() {
        try {
            File orRepLocation = new File(getORRepLocation());
            
            // Try YAML format first (modern format)
            if (yamlReader.webORExists(orRepLocation)) {
                LOGGER.info("Loading Web OR from YAML format");
                webOR = yamlReader.readWebOR(orRepLocation);
                webOR.setName(sProject.getName());
                usingYamlFormat = true;
            } else if (new File(getORLocation()).exists()) {
                // Fall back to XML format (legacy)
                LOGGER.info("Loading Web OR from XML format");
                webOR = XML_MAPPER.readValue(new File(getORLocation()), WebOR.class);
                webOR.setName(sProject.getName());
                usingYamlFormat = false; // Use XML format for legacy projects
            } else {
                webOR = new WebOR(sProject.getName());
                // usingYamlFormat stays true for new projects
            }
            
            // Try YAML format first for Mobile OR
            if (yamlReader.mobileORExists(orRepLocation)) {
                LOGGER.info("Loading Mobile OR from YAML format");
                mobileOR = yamlReader.readMobileOR(orRepLocation);
                mobileOR.setName(sProject.getName());
                usingYamlFormat = true;
            } else if (new File(getMORLocation()).exists()) {
                // Fall back to XML format (legacy)
                LOGGER.info("Loading Mobile OR from XML format");
                mobileOR = XML_MAPPER.readValue(new File(getMORLocation()), MobileOR.class);
                mobileOR.setName(sProject.getName());
                usingYamlFormat = false; // Use XML format for legacy projects
            } else {
                mobileOR = new MobileOR(sProject.getName());
                // usingYamlFormat stays true for new projects
            }

            // Try YAML format first for API OR
            if (yamlReader.apiORExists(orRepLocation)) {
                LOGGER.info("Loading API OR from YAML format");
                apiOR = yamlReader.readAPIOR(orRepLocation);
                apiOR.setName(sProject.getName());
                usingYamlFormat = true;
            } else if (new File(getAPIORLocation()).exists()) {
                // Fall back to XML format (legacy)
                LOGGER.info("Loading API OR from XML format");
                apiOR = XML_MAPPER.readValue(new File(getAPIORLocation()), APIOR.class);
                apiOR.setName(sProject.getName());
                usingYamlFormat = false; // Use XML format for legacy projects
            } else {
                apiOR = new APIOR(sProject.getName());
                // usingYamlFormat stays true for new projects
            }

            webOR.setObjectRepository(this);
            webOR.setSaved(true);
            mobileOR.setObjectRepository(this);
            apiOR.setObjectRepository(this);
          
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error loading Object Repository", ex);
        }
    }

    public String getORLocation() {
        return sProject.getLocation() + File.separator + "OR.object";
    }

    public String getIORLocation() {
        return sProject.getLocation() + File.separator + "IOR.object";
    }

    public String getMORLocation() {
        return sProject.getLocation() + File.separator + "MOR.object";
    }

    public String getAPIORLocation() {
        return sProject.getLocation() + File.separator + "APIOR.object";
    }

    public String getORRepLocation() {
        return sProject.getLocation() + File.separator + "ObjectRepository";
    }

    public String getIORRepLocation() {
        return sProject.getLocation() + File.separator + "ImageObjectRepository";
    }

    public String getMORRepLocation() {
        return sProject.getLocation() + File.separator + "MobileObjectRepository";
    }

    public String getAPIORRepLocation() {
        return sProject.getLocation() + File.separator + "APIObjectRepository";
    }

    public Project getsProject() {
        return sProject;
    }

    public WebOR getWebOR() {
        return webOR;
    }

    public MobileOR getMobileOR() {
        return mobileOR;
    }

    public APIOR getAPIOR() {
        return apiOR;
    }

    public void save() {
        try {
            if (usingYamlFormat) {
                saveAsYaml();
            } else {
                // Save in XML format (legacy)
                if (!webOR.isSaved()) {
                    XML_MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(getORLocation()), webOR);
                }
                XML_MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(getMORLocation()), mobileOR);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error saving Object Repository", ex);
        }
    }
    
    /**
     * Save Object Repository in YAML format.
     * Creates page-per-file structure under ObjectRepository/Web/pages/, ObjectRepository/Mobile/pages/, and ObjectRepository/API/pages/
     */
    public void saveAsYaml() {
        try {
            File orRepLocation = new File(getORRepLocation());
            yamlWriter.writeWebOR(webOR, orRepLocation);
            yamlWriter.writeMobileOR(mobileOR, orRepLocation);
            yamlWriter.writeAPIOR(apiOR, orRepLocation);
            webOR.setSaved(true);
            usingYamlFormat = true;
            LOGGER.info("Saved Object Repository in YAML format");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error saving Object Repository as YAML", ex);
        }
    }
    
    /**
     * Convert existing XML-based OR to YAML format.
     * This creates YAML files while preserving the original XML files.
     */
    public void convertToYaml() {
        try {
            File orRepLocation = new File(getORRepLocation());
            yamlWriter.convertFromXml(webOR, mobileOR, orRepLocation);
            usingYamlFormat = true;
            LOGGER.info("Converted Object Repository to YAML format");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error converting Object Repository to YAML", ex);
        }
    }
    
    /**
     * Check if the Object Repository is using YAML format.
     */
    public boolean isUsingYamlFormat() {
        return usingYamlFormat;
    }
    
    /**
     * Set whether to use YAML format for saving.
     */
    public void setUsingYamlFormat(boolean useYaml) {
        this.usingYamlFormat = useYaml;
    }

    public Boolean isObjectPresent(String pageName, String objectName) {
        Boolean present = false;
        if (webOR.getPageByName(pageName) != null) {
            present = webOR.getPageByName(pageName).getObjectGroupByName(objectName) != null;
        }
        if (!present) {
            if (mobileOR.getPageByName(pageName) != null) {
                present = mobileOR.getPageByName(pageName).getObjectGroupByName(objectName) != null;
            }
        }
        if (!present) {
            if (apiOR.getPageByName(pageName) != null) {
                present = apiOR.getPageByName(pageName).getObjectGroupByName(objectName) != null;
            }
        }
        return present;
    }

    public void renameObject(ObjectGroup group, String newName) {
        sProject.refactorObjectName(group.getParent().getName(), group.getName(), newName);
    }

    public void renamePage(ORPageInf page, String newName) {
        sProject.refactorPageName(page.getName(), newName);
    }
    
    /**
     * Rename a Web page YAML file.
     * 
     * @param oldName The current page name
     * @param newName The new page name
     * @return true if rename was successful
     */
    public boolean renameWebPageYaml(String oldName, String newName) {
        if (usingYamlFormat) {
            return yamlWriter.renameWebPage(oldName, newName, new File(getORRepLocation()));
        }
        return true; // For XML format, file rename is handled separately
    }
    
    /**
     * Rename a Mobile page YAML file.
     * 
     * @param oldName The current page name
     * @param newName The new page name
     * @return true if rename was successful
     */
    public boolean renameMobilePageYaml(String oldName, String newName) {
        if (usingYamlFormat) {
            return yamlWriter.renameMobilePage(oldName, newName, new File(getORRepLocation()));
        }
        return true; // For XML format, file rename is handled separately
    }
    
    /**
     * Rename an API page YAML file.
     * 
     * @param oldName The current page name
     * @param newName The new page name
     * @return true if rename was successful
     */
    public boolean renameAPIPageYaml(String oldName, String newName) {
        if (usingYamlFormat) {
            return yamlWriter.renameAPIPage(oldName, newName, new File(getORRepLocation()));
        }
        return true; // For XML format, file rename is handled separately
    }
    
    /**
     * Get the YAML writer for advanced operations.
     */
    public YamlORWriter getYamlWriter() {
        return yamlWriter;
    }
    
    /**
     * Immediately save a specific Web page to YAML.
     * Called when objects are added/modified for auto-save functionality.
     * 
     * @param page The WebORPage to save
     */
    public void saveWebPageNow(com.ing.datalib.or.web.WebORPage page) {
        if (usingYamlFormat && page != null) {
            try {
                java.io.File pagesDir = new java.io.File(getORRepLocation(), "Web/pages");
                if (!pagesDir.exists()) {
                    pagesDir.mkdirs();
                }
                yamlWriter.writeWebPage(page, pagesDir);
                LOGGER.fine("Auto-saved Web page: " + page.getName());
            } catch (java.io.IOException ex) {
                LOGGER.log(Level.WARNING, "Error auto-saving Web page: " + page.getName(), ex);
            }
        }
    }
    
    /**
     * Immediately save a specific Mobile page to YAML.
     * Called when objects are added/modified for auto-save functionality.
     * 
     * @param page The MobileORPage to save
     */
    public void saveMobilePageNow(com.ing.datalib.or.mobile.MobileORPage page) {
        if (usingYamlFormat && page != null) {
            try {
                java.io.File pagesDir = new java.io.File(getORRepLocation(), "Mobile/pages");
                if (!pagesDir.exists()) {
                    pagesDir.mkdirs();
                }
                yamlWriter.writeMobilePage(page, pagesDir);
                LOGGER.fine("Auto-saved Mobile page: " + page.getName());
            } catch (java.io.IOException ex) {
                LOGGER.log(Level.WARNING, "Error auto-saving Mobile page: " + page.getName(), ex);
            }
        }
    }
    
    /**
     * Immediately save a specific API page to YAML.
     * Called when objects are added/modified for auto-save functionality.
     * 
     * @param page The APIORPage to save
     */
    public void saveAPIPageNow(com.ing.datalib.or.api.APIORPage page) {
        if (usingYamlFormat && page != null) {
            try {
                java.io.File pagesDir = new java.io.File(getORRepLocation(), "API/pages");
                if (!pagesDir.exists()) {
                    pagesDir.mkdirs();
                }
                yamlWriter.writeAPIPage(page, pagesDir);
                LOGGER.fine("Auto-saved API page: " + page.getName());
            } catch (java.io.IOException ex) {
                LOGGER.log(Level.WARNING, "Error auto-saving API page: " + page.getName(), ex);
            }
        }
    }

}
