
package com.ing.datalib.or;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.structureddata.StructuredData;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.datalib.or.mobile.MobileORPage;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.yaml.YamlORReader;
import com.ing.datalib.or.yaml.YamlORWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.ing.datalib.or.structureddata.StructuredDataORObject;
import com.ing.datalib.or.structureddata.StructuredDataORPage;
import com.ing.datalib.or.structureddata.ResolvedStructuredDataObject;
import com.ing.datalib.or.mobile.ResolvedMobileObject;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.datalib.or.web.WebOR.ORScope;
import static com.ing.datalib.or.web.WebOR.ORScope.PROJECT;
import static com.ing.datalib.or.web.WebOR.ORScope.SHARED;
import com.ing.datalib.or.web.WebORObject;
import com.ing.datalib.or.web.WebORPage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages all Object Repository types (Web Project OR, Web Shared OR, Mobile OR)
 * for a project. Handles loading, saving, renaming, lookup, copying of pages and
 * objects, and resolving objects across project/shared scopes.
 */
public class ObjectRepository {
    private static final XmlMapper XML_MAPPER = new XmlMapper();
    private static final Logger LOG = Logger.getLogger(ObjectRepository.class.getName());
    private final Project sProject;
    private WebOR webSharedOR;
    private WebOR webProjectOR;
    private MobileOR mobileProjectOR;
    private MobileOR mobileSharedOR;
    private StructuredData structuredDataProjectOR;
    private StructuredData structuredDataSharedOR;
    
    private final Set<String> webSharedUsageProjects = new HashSet<>();
    private final Set<String> mobileSharedUsageProjects = new HashSet<>();
    private List<String> webSharedProjectsFromXml = List.of();
    private List<String> mobileSharedProjectsFromXml = List.of();
    
    // YAML support
    private boolean useYamlFormat = true; // Default to YAML for new projects
    private YamlORReader yamlReader;
    private YamlORWriter yamlWriter;
    
    /**
     * Creates an ObjectRepository for the given project and loads all OR files
     * (project WebOR, shared WebOR, and MobileOR), initializing defaults when missing.
     *
     * @param sProject the project owning this repository
     */
    public ObjectRepository(Project sProject) {
        this.sProject = sProject;
        init();
    }

    /**
     * Loads OR files from disk (shared, project, mobile), updates names, sets scopes,
     * and links them to this repository.
     */
    private void init() {
        try {
            yamlReader = new YamlORReader(this);
            yamlWriter = new YamlORWriter();

            boolean projectYamlExists = hasYamlOR();
            boolean sharedYamlExists  = hasSharedYamlOR();
            boolean xmlExists = hasAnyXmlOR();

            if (xmlExists) {
                loadXmlObjectRepositories();
                convertXmlOrsToYamlAndArchive();
                loadYamlObjectRepositories();
                useYamlFormat = true;
            }
            
            else if (projectYamlExists || sharedYamlExists) {
                loadYamlObjectRepositories();
                useYamlFormat = true;
            }
            
            else {
                webProjectOR = new WebOR();
                webProjectOR.setScope(WebOR.ORScope.PROJECT);
                webProjectOR.setObjectRepository(this);
                webProjectOR.setName(sProject.getName());
                
                mobileProjectOR = new MobileOR();
                mobileProjectOR.setScope(MobileOR.ORScope.PROJECT);
                mobileProjectOR.setObjectRepository(this);
                mobileProjectOR.setName(sProject.getName());
                
                structuredDataProjectOR = new StructuredData();
                structuredDataProjectOR.setObjectRepository(this);
                structuredDataProjectOR.setName(sProject.getName());
                useYamlFormat = true;
                
                if (webSharedOR == null && !sharedYamlExists) {
                    webSharedOR = new WebOR("Shared Web Objects");
                    webSharedOR.setScope(WebOR.ORScope.SHARED);
                    webSharedOR.setObjectRepository(this);
                    webSharedOR.setSaved(true);
                }
                if (mobileSharedOR == null && !sharedYamlExists) {
                    mobileSharedOR = new MobileOR("Shared Mobile Objects");
                    mobileSharedOR.setScope(MobileOR.ORScope.SHARED);
                    mobileSharedOR.setObjectRepository(this);
                    mobileSharedOR.setSaved(true);
                }
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Failed to initialize ObjectRepository", e);
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
    public String getStructuredDataORLocation() {
        return sProject.getLocation() + File.separator + "StructuredDataOR.object";
    }
    public String getSharedORLocation() {
        return "Shared" + File.separator + "SharedWebObjects" + File.separator + "SharedOR.object";
    }
    public String getSharedMORLocation() {
        return "Shared" + File.separator + "SharedMobileObjects" + File.separator + "SharedMOR.object";
    }
    public String getORRepLocation() {
        return sProject.getLocation() + File.separator + "ObjectRepository";
    }
    public String getIORRepLocation() {
        return sProject.getLocation() + File.separator + "ImageObjectRepository";
    }
    public String getStructuredDataORRepLocation() {
        return sProject.getLocation() + File.separator + "StructuredDataObjectRepository";
    }
    public String getSharedORRepLocation() {
        File projectDir = new File(sProject.getLocation());   
        File projectsRoot = projectDir.getParentFile();       
        File ingeniousRoot = projectsRoot.getParentFile();    
        File sharedOR = new File(ingeniousRoot,"Shared" + File.separator + "SharedObjectRepository");
        return sharedOR.getAbsolutePath();
    }
    public Project getsProject() {
        return sProject;
    }
    public WebOR getWebOR() {
        return webProjectOR;
    }
    public WebOR getWebSharedOR() {
        return webSharedOR;
    }
    public MobileOR getMobileOR() {
        return mobileProjectOR;
    }
    public MobileOR getMobileSharedOR() {
        return mobileSharedOR;
    }
    public StructuredData getStructuredDataOR() {
        return structuredDataProjectOR;
    }
    public StructuredData getStructuredDataSharedOR() {
        return structuredDataSharedOR;
    }

    /**
     * Saves updated shared, project, and mobile ORs to disk.
     * Also updates shared project usage metadata when required.
     */
    public void save() {
        try {
            boolean webProjectsChanged = false;
            if (webSharedOR != null) {
                LinkedHashSet<String> webMerged = new LinkedHashSet<>();
                webMerged.addAll(webSharedProjectsFromXml);
                webMerged.addAll(webSharedUsageProjects);
                File sharedRoot = new File(getSharedORRepLocation());
                File webMetaFile = new File(sharedRoot, "webor-projectsdata.yaml");
                webMerged.addAll(yamlWriter.readExistingProjects(webMetaFile));
                List<String> current = webSharedOR.getSharedProjects();
                webProjectsChanged = current == null || !new LinkedHashSet<>(current).equals(webMerged);
                if (webProjectsChanged) {
                    webSharedOR.setSharedProjects(
                        new ArrayList<>(webMerged)
                    );
                }
            }
            
            boolean mobileProjectsChanged = false;
            if (mobileSharedOR != null) {
                LinkedHashSet<String> mobileMerged = new LinkedHashSet<>();
                mobileMerged.addAll(mobileSharedProjectsFromXml);
                mobileMerged.addAll(mobileSharedUsageProjects);
                File sharedRoot = new File(getSharedORRepLocation());
                File mobileMetaFile = new File(sharedRoot, "mobileor-projectsdata.yaml");
                mobileMerged.addAll(yamlWriter.readExistingProjects(mobileMetaFile));
                List<String> current = mobileSharedOR.getSharedProjects();
                mobileProjectsChanged = current == null || !new LinkedHashSet<>(current).equals(mobileMerged);
                if (mobileProjectsChanged) {
                    mobileSharedOR.setSharedProjects(
                        new ArrayList<>(mobileMerged)
                    );
                }
            }

            if (useYamlFormat) {
                File sharedRoot = new File(getSharedORRepLocation());
                if (webSharedOR != null && (!webSharedOR.isSaved() || webProjectsChanged)) {
                    yamlWriter.writeWebOR(webSharedOR, sharedRoot);
                    if (webSharedOR.getSharedProjects() != null &&
                        !webSharedOR.getSharedProjects().isEmpty()) {
                        yamlWriter.writeSharedMetadata(webSharedOR,sharedRoot);
                    }
                    webSharedOR.setSaved(true);
                }

                if (mobileSharedOR != null && (!mobileSharedOR.isSaved() || mobileProjectsChanged)) {
                    yamlWriter.writeMobileOR(mobileSharedOR, sharedRoot);
                    if (mobileSharedOR.getSharedProjects() != null &&
                        !mobileSharedOR.getSharedProjects().isEmpty()) {
                        yamlWriter.writeSharedMetadata(mobileSharedOR,sharedRoot);
                    }
                    mobileSharedOR.setSaved(true);
                }
                saveAsYaml();
                LOG.info("Saved project ORs in YAML format");
            }
        } catch (IOException ex) {
            Logger.getLogger(ObjectRepository.class.getName())
                  .log(Level.SEVERE, "Failed to save Object Repository", ex);
        }
    }
    
    /**
     * Save Object Repository in YAML format.
     * Creates page-per-file structure under ObjectRepository/Web/pages/, ObjectRepository/Mobile/pages/, and ObjectRepository/API/pages/
     */
    public void saveAsYaml() {
        try {
            File orRepLocation = new File(getORRepLocation());
            yamlWriter.writeWebOR(webProjectOR, orRepLocation);
            yamlWriter.writeMobileOR(mobileProjectOR, orRepLocation);
            yamlWriter.writeStructuredDataOR(structuredDataProjectOR, orRepLocation);
            webProjectOR.setSaved(true);
            mobileProjectOR.setSaved(true);
            structuredDataProjectOR.setSaved(true);
            useYamlFormat = true;
            LOG.info("Saved Object Repository in YAML format");
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Error saving Object Repository as YAML", ex);
        }
    }
    
    /**
     * Convert existing XML-based OR to YAML format.
     * This creates YAML files while preserving the original XML files.
     */
    private boolean hasProjectXmlOR() {
        return new File(getORLocation()).exists() || new File(getMORLocation()).exists();
    }

    private boolean hasSharedXmlOR() {
        return new File(getSharedORLocation()).exists() || new File(getSharedMORLocation()).exists();
    }

    private boolean hasAnyXmlOR() {
        return hasProjectXmlOR() || hasSharedXmlOR();
    }

    private boolean hasYamlOR() {
        File webPages = new File(getORRepLocation(), "Web");
        File mobilePages = new File(getORRepLocation(), "Mobile");
        File structuredDataPages = new File(getORRepLocation(), "StructuredData");
        return (webPages.exists() && webPages.isDirectory()) || (mobilePages.exists() && mobilePages.isDirectory()) || (structuredDataPages.exists() && structuredDataPages.isDirectory());
    }

    private boolean hasSharedYamlOR() {
        File sharedWeb = new File(getSharedORRepLocation(), "Web");
        File sharedMobile = new File(getSharedORRepLocation(), "Mobile");
        File sharedStructuredDataPages = new File(getORRepLocation(), "StructuredData");
        return (sharedWeb.exists() && sharedWeb.isDirectory()) || (sharedMobile.exists() && sharedMobile.isDirectory()) || (sharedStructuredDataPages.exists() && sharedStructuredDataPages.isDirectory());
    }
    
    private void convertXmlOrsToYamlAndArchive() throws IOException {
        LOG.info("Legacy XML ORs detected. Converting to YAML...");
        XmlToYamlORConverter converter = new XmlToYamlORConverter(yamlWriter);

        File projectYamlRoot = new File(getORRepLocation());
        File sharedYamlRoot  = new File(getSharedORRepLocation());

        converter.convertAll(
            webProjectOR,
            webSharedOR,
            mobileProjectOR,
            mobileSharedOR,
            projectYamlRoot,
            sharedYamlRoot
        );

        archiveProjectXmlORs();
        archiveSharedXmlORs();
        cleanupLegacySharedXmlFolders();
        useYamlFormat = true;

        LOG.info("XML OR migration complete. YAML is now active.");
    }

    private void archiveProjectXmlORs() {
        File archiveDir = new File(sProject.getLocation(), "ProjectXMLOR");
        archiveDir.mkdirs();
        moveXmlToBak(getORLocation(), archiveDir);
        moveXmlToBak(getMORLocation(), archiveDir);
    }
    
    private void archiveSharedXmlORs() {
        File archiveDir = new File("Shared" + File.separator + "SharedXMLOR");
        archiveDir.mkdirs();
        moveXmlToBak(getSharedORLocation(), archiveDir);
        moveXmlToBak(getSharedMORLocation(), archiveDir);
    }

        private void moveXmlToBak(String xmlPath, File targetDir) {
        if (xmlPath == null) return;
        File xmlFile = new File(xmlPath);
        if (!xmlFile.exists()) return;
        File bakFile = new File(targetDir, xmlFile.getName() + ".bak");
        if (xmlFile.renameTo(bakFile)) {
            LOG.info(() -> "Archived XML OR: " + bakFile.getAbsolutePath());
        } else {
            LOG.warning(() -> "Failed to archive XML OR: " + xmlFile.getAbsolutePath());
        }
    }

    private void deleteDirectoryRecursively(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] contents = dir.listFiles();
        if (contents != null) {
            for (File file : contents) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursively(file);
                } else {
                    file.delete();
                }
            }
        }
        boolean deleted = dir.delete();
        if (deleted) {
            LOG.info(() -> "Deleted legacy XML directory: " + dir.getAbsolutePath());
        } else {
            LOG.warning(() -> "Failed to delete legacy XML directory: " + dir.getAbsolutePath());
        }
    }

    private void cleanupLegacySharedXmlFolders() {
        File sharedWebXmlDir = new File("Shared" + File.separator + "SharedWebObjects");
        File sharedMobileXmlDir = new File("Shared" + File.separator + "SharedMobileObjects");
        deleteDirectoryRecursively(sharedWebXmlDir);
        deleteDirectoryRecursively(sharedMobileXmlDir);
    }

    private void loadYamlObjectRepositories() throws IOException {
        File projectRoot = new File(getORRepLocation());
        
        webProjectOR = yamlReader.readWebOR(projectRoot);
        if (webProjectOR != null) {
            webProjectOR.setObjectRepository(this);
            webProjectOR.setScope(WebOR.ORScope.PROJECT);
            webProjectOR.setName(sProject.getName());
            normalizeWebOR(webProjectOR);
        }

        mobileProjectOR = yamlReader.readMobileOR(projectRoot);
        if (mobileProjectOR != null) {
            mobileProjectOR.setObjectRepository(this);
            mobileProjectOR.setScope(MobileOR.ORScope.PROJECT);
            mobileProjectOR.setName(sProject.getName());
            normalizeMobileOR(mobileProjectOR);
        }

        structuredDataProjectOR = yamlReader.readStructuredDataOR(projectRoot);
        if (structuredDataProjectOR != null) {
            structuredDataProjectOR.setObjectRepository(this);
            structuredDataProjectOR.setName(sProject.getName());
            normalizestructuredDataOR(structuredDataProjectOR);
        }

        File sharedRoot = new File(getSharedORRepLocation());
        
        if (sharedRoot.exists() && sharedRoot.isDirectory()) {

            webSharedOR = yamlReader.readWebOR(sharedRoot);
            if (webSharedOR != null) {
                webSharedOR.setObjectRepository(this);
                webSharedOR.setScope(WebOR.ORScope.SHARED);
                webSharedOR.setName("Shared Web Objects");
                normalizeWebOR(webSharedOR);
            }

            mobileSharedOR = yamlReader.readMobileOR(sharedRoot);
            if (mobileSharedOR != null) {
                mobileSharedOR.setObjectRepository(this);
                mobileSharedOR.setScope(MobileOR.ORScope.SHARED);
                mobileSharedOR.setName("Shared Mobile Objects");
                normalizeMobileOR(mobileSharedOR);
            }
        }
    }
    
    /*
    * Loads legacy XML-based Object Repositories into memory.
    *
    * This method is ONLY used during XML → YAML migration.
    * It must NOT be called once YAML ORs exist.
    */
    private void loadXmlObjectRepositories() throws IOException {
        LOG.info("Loading legacy XML Object Repositories for migration...");
        File projectWebXml = new File(getORLocation());
        if (projectWebXml.exists()) {
            webProjectOR = XML_MAPPER.readValue(projectWebXml, WebOR.class);
            webProjectOR.setObjectRepository(this);
            LOG.info("Loaded PROJECT Web XML OR");
        }

        File projectMobileXml = new File(getMORLocation());
        if (projectMobileXml.exists()) {
            mobileProjectOR = XML_MAPPER.readValue(projectMobileXml, MobileOR.class);
            mobileProjectOR.setObjectRepository(this);
            LOG.info("Loaded PROJECT Mobile XML OR");
        }

        File sharedWebXml = new File(getSharedORLocation());
        if (sharedWebXml.exists()) {
            webSharedOR = XML_MAPPER.readValue(sharedWebXml, WebOR.class);
            webSharedOR.setObjectRepository(this);
            webSharedOR.setScope(WebOR.ORScope.SHARED);
            LOG.info("Loaded SHARED Web XML OR");
        }

        File sharedMobileXml = new File(getSharedMORLocation());
        if (sharedMobileXml.exists()) {
            mobileSharedOR = XML_MAPPER.readValue(sharedMobileXml, MobileOR.class);
            mobileSharedOR.setObjectRepository(this);
            mobileSharedOR.setScope(MobileOR.ORScope.SHARED);
            LOG.info("Loaded SHARED Mobile XML OR");
        }

        if (webSharedOR != null && webSharedOR.getSharedProjects() != null) {
             webSharedProjectsFromXml = new ArrayList<>(webSharedOR.getSharedProjects());
         }

         if (mobileSharedOR != null && mobileSharedOR.getSharedProjects() != null) {
             mobileSharedProjectsFromXml = new ArrayList<>(mobileSharedOR.getSharedProjects());
         }
    }

    private void normalizeWebOR(WebOR webOR) {
         if (webOR == null) return;

         for (WebORPage page : webOR.getPages()) {
             page.setRoot(webOR);

             for (ObjectGroup<WebORObject> group : page.getObjectGroups()) {
                 group.setParent(page);

                 for (WebORObject obj : group.getObjects()) {
                     obj.setParent(group);
                 }
             }
         }
         webOR.setSaved(true);
     }

    private void normalizeMobileOR(MobileOR mobileOR) {
         if (mobileOR == null) return;

         for (MobileORPage page : mobileOR.getPages()) {
             page.setRoot(mobileOR);

             for (ObjectGroup<MobileORObject> group : page.getObjectGroups()) {
                 group.setParent(page);

                 for (MobileORObject obj : group.getObjects()) {
                     obj.setParent(group);
                 }
             }
         }
         mobileOR.setSaved(true);
     }

    private void normalizestructuredDataOR(StructuredData apiOR) {
         if (apiOR == null) return;

         for (StructuredDataORPage page : apiOR.getPages()) {
             page.setRoot(apiOR);

             for (ObjectGroup<StructuredDataORObject> group : page.getObjectGroups()) {
                 group.setParent(page);

                 for (StructuredDataORObject obj : group.getObjects()) {
                     obj.setParent(group);
                 }
             }
         }
         apiOR.setSaved(true);
     }

    /**
     * Checks whether the given object exists in either PROJECT or SHARED scope.
     *
     * @param pageName   page containing the object
     * @param objectName object name
     * @return true if present in project or shared OR
     */
    public Boolean isObjectPresent(String pageName, String objectName) {
        return resolveWebObjectWithScope(pageName, objectName) != null;
    }

    public Boolean isMobileObjectPresent(String pageName, String objectName) {
        return resolveMobileObjectWithScope(pageName, objectName) != null;
    }

    /**
     * Renames an object (object group) within its parent page. Determines whether the object
     * is in project or shared scope and triggers corresponding scenario refactor in Project.
     *
     * @param group   object group containing the object
     * @param newName new object name
     */
    public void renameObject(ObjectGroup group, String newName) {
        if (group == null || newName == null || newName.isBlank()) return;

        var parentPage = group.getParent();
        if (parentPage == null) return;

        String oldName = group.getName();
        if (oldName.equals(newName)) return;

        boolean webProject =
            webProjectOR != null &&
            webProjectOR.getPageByName(parentPage.getName()) == parentPage;

        boolean webShared =
            webSharedOR != null &&
            webSharedOR.getPageByName(parentPage.getName()) == parentPage;

        if (webProject) {
            webProjectOR.setSaved(false);
            sProject.refactorObjectName(
                WebOR.ORScope.PROJECT,
                parentPage.getName(),
                oldName,
                newName
            );
            return;
        }

        if (webShared) {
            webSharedOR.setSaved(false);
            markSharedUsage("WebOR");
            sProject.refactorObjectName(
                WebOR.ORScope.SHARED,
                parentPage.getName(),
                oldName,
                newName
            );
            return;
        }

        boolean mobileProject =
            mobileProjectOR != null &&
            mobileProjectOR.getPageByName(parentPage.getName()) == parentPage;

        boolean mobileShared =
            mobileSharedOR != null &&
            mobileSharedOR.getPageByName(parentPage.getName()) == parentPage;

        if (mobileProject) {
            mobileProjectOR.setSaved(false);
            sProject.refactorMobileObjectName(
                MobileOR.ORScope.PROJECT,
                parentPage.getName(),
                oldName,
                newName
            );
            return;
        }

        if (mobileShared) {
            mobileSharedOR.setSaved(false);
            markSharedUsage("MobileOR");
            sProject.refactorMobileObjectName(
                MobileOR.ORScope.SHARED,
                parentPage.getName(),
                oldName,
                newName
            );
        }
    }
    
    /**
     * Renames a page in project or shared OR, respecting scope rules and preventing collisions,
     * then propagates refactor changes into Project.
     *
     * @param page    page object reference
     * @param newName new page name
     */
    public void renamePage(ORPageInf page, String newName) {
        if (page == null || newName == null || newName.isBlank()) return;

        String oldName = page.getName();
        if (oldName.equals(newName)) return;

        boolean renamed = false;
        ORScope scopeRenamed = null;

        if (webProjectOR != null) {
            var p = webProjectOR.getPageByName(oldName);
            if (p == page) {
                var existsSameScope = webProjectOR.getPageByName(newName);
                if (existsSameScope != null && existsSameScope != page) {
                    return;
                }
                p.setName(newName);
                webProjectOR.setSaved(false);
                renamed = true;
                scopeRenamed = ORScope.PROJECT;

                if (useYamlFormat) {
                    renameWebPageYaml(oldName, newName, scopeRenamed);
                }
            }
        }

        if (!renamed && webSharedOR != null) {
            var s = webSharedOR.getPageByName(oldName);
            if (s == page) {
                var existsSameScope = webSharedOR.getPageByName(newName);
                if (existsSameScope != null && existsSameScope != page) {
                    return;
                }
                s.setName(newName);
                webSharedOR.setSaved(false);
                renamed = true;
                scopeRenamed = ORScope.SHARED;

                if (useYamlFormat) {
                    renameWebPageYaml(oldName, newName, scopeRenamed);
                }
            }
        }

        if (renamed) {
            sProject.refactorPageName(scopeRenamed, oldName, newName);
        } else {
            sProject.refactorPageName(oldName, newName);
        }
    }

    public void renamePage(MobileORPage page, String newName) {
        if (page == null || newName == null || newName.isBlank()) return;

        String oldName = page.getName();
        if (oldName.equals(newName)) return;

        boolean renamed = false;
        MobileOR.ORScope mScope = null;

        if (mobileProjectOR != null) {
            var p = mobileProjectOR.getPageByName(oldName);
            if (p == page) {
                var existsSameScope = mobileProjectOR.getPageByName(newName);
                if (existsSameScope != null && existsSameScope != page) return;

                p.setName(newName);
                mobileProjectOR.setSaved(false);
                renamed = true;
                mScope = MobileOR.ORScope.PROJECT;

                if (useYamlFormat) {
                    renameMobilePageYaml(oldName, newName, mScope);
                }
            }
        }

        if (!renamed && mobileSharedOR != null) {
            var s = mobileSharedOR.getPageByName(oldName);
            if (s == page) {
                var existsSameScope = mobileSharedOR.getPageByName(newName);
                if (existsSameScope != null && existsSameScope != page) return;

                s.setName(newName);
                mobileSharedOR.setSaved(false);
                renamed = true;
                mScope = MobileOR.ORScope.SHARED;

                if (useYamlFormat) {
                    renameMobilePageYaml(oldName, newName, mScope);
                }
            }
        }

        if (renamed) {
            var webLikeScope =
                (mScope == MobileOR.ORScope.PROJECT)
                    ? WebOR.ORScope.PROJECT
                    : WebOR.ORScope.SHARED;

            sProject.refactorPageName(webLikeScope, oldName, newName);
        } else {
            sProject.refactorPageName(oldName, newName);
        }
    }

    /**
     * Resolves a WebOR object from a scoped PageRef and object name, returning a
     * ResolvedWebObject containing scope, page, object name, and object group.
     */
    public ResolvedWebObject resolveWebObject(ResolvedWebObject.PageRef pageRef, String objectName) {
        if (pageRef == null || objectName == null) return null;
        if (pageRef.scope != null) {
            return resolveWebObjectInScope(pageRef.scope,pageRef.name,objectName);
        }
        return resolveWebObjectWithScope(pageRef.name, objectName);
    }
    
    private ResolvedWebObject resolveWebObjectInScope(ORScope scope,String pageName, String objectName) {
        WebOR or = (scope == ORScope.PROJECT) ? webProjectOR : webSharedOR;
        ObjectGroup g = getFrom(or, pageName, objectName);
        if (g == null) return null;
        if (scope == ORScope.SHARED) {
            markSharedUsage("WebOR");
        }
        String actualPage = g.getParent() != null ? g.getParent().getName() : pageName;
        return new ResolvedWebObject(scope, actualPage, objectName, g);
    }

    public ResolvedMobileObject resolveMobileObject(ResolvedMobileObject.PageRef pageRef, String objectName) {
        if (pageRef == null || objectName == null) return null;
        if (pageRef.scope != null) {
            return resolveMobileObjectInScope(
                    pageRef.scope,
                    pageRef.name,
                    objectName
            );
        }
        return resolveMobileObjectWithScope(pageRef.name, objectName);
    }
    
    private ResolvedMobileObject resolveMobileObjectInScope(ORScope scope, String pageName, String objectName) {
        MobileOR or = (scope == ORScope.PROJECT) ? mobileProjectOR : mobileSharedOR;
        ObjectGroup g = getFrom(or, pageName, objectName);
        if (g == null) return null;
        if (scope == ORScope.SHARED) {
            markSharedUsage("MobileOR");
        }
        String actualPage = g.getParent() != null ? g.getParent().getName() : pageName;
        return new ResolvedMobileObject(scope, actualPage, objectName, g);
    }
    
    public ResolvedStructuredDataObject resolveStructuredDataObject(ResolvedStructuredDataObject.PageRef pageRef, String objectName) {
        if (pageRef == null || objectName == null) return null;
        if (pageRef.scope == ORScope.PROJECT) {
            var g = getFrom(structuredDataProjectOR, pageRef.name, objectName);
            if (g != null) {
                String actualPageName = g.getParent() != null ? g.getParent().getName() : pageRef.name;
                return new ResolvedStructuredDataObject(ORScope.PROJECT, actualPageName, objectName, g);
            }
            return null;
        }
        if (pageRef.scope == ORScope.SHARED) {
            var g = getFrom(structuredDataSharedOR, pageRef.name, objectName);
            if (g != null) {
                markSharedUsage("StructuredDataOR");
                String actualPageName = g.getParent() != null ? g.getParent().getName() : pageRef.name;
                return new ResolvedStructuredDataObject(ORScope.SHARED, actualPageName, objectName, g);
            }
            return null;
        }
        return resolveStructuredDataObjectWithScope(pageRef.name, objectName);
    }

    /**
     * Resolves a WebOR object by searching project scope first, then shared scope.
     *
     * @param pageName   page to search
     * @param objectName object group name
     * @return resolved WebOR object with scope metadata
     */
    public ResolvedWebObject resolveWebObjectWithScope(String pageName, String objectName) {
        ResolvedWebObject proj = resolveWebObjectInScope(ORScope.PROJECT, pageName, objectName);
        if (proj != null) return proj;
        return resolveWebObjectInScope(ORScope.SHARED, pageName, objectName);
    }

    public ResolvedWebObject resolveWebObjectWithScope(String pageName, String objectName,TestStep step) {
        var proj = getFrom(webProjectOR, pageName, objectName);
        if (proj != null) {
            return new ResolvedWebObject(PROJECT, pageName, objectName, proj);
        }
        var shared = getFrom(webSharedOR, pageName, objectName);
        if (shared != null) {
            if (step != null &&
                step.getReference().startsWith("[Project] ")) {
                step.setReference("[Shared] " + pageName);
                step.getTestCase().setSaved(false);
            }
            return new ResolvedWebObject(SHARED, pageName, objectName, shared);
        }
        return null;
    }

    /**
    * Resolves a MobileOR object by searching project scope first, then shared scope.
    *
    * @param pageName page to search
    * @param objectName object group name
    * @return resolved MobileOR object with scope metadata
    */
    public ResolvedMobileObject resolveMobileObjectWithScope(String pageName, String objectName) {
        ResolvedMobileObject proj = resolveMobileObjectInScope(ORScope.PROJECT, pageName, objectName);
        if (proj != null) return proj;
        return resolveMobileObjectInScope(ORScope.SHARED, pageName, objectName);
    }
   
   /**
    * Resolves a StructuredDataOR object by searching project scope first, then shared scope.
    *
    * @param pageName page to search
    * @param objectName object group name
    * @return resolved StructuredDataOR object with scope metadata
    */
   public ResolvedStructuredDataObject resolveStructuredDataObjectWithScope(String pageName, String objectName) {
       var proj = getFrom(structuredDataProjectOR, pageName, objectName);
       if (proj != null) {
           String actualPageName = proj.getParent() != null ? proj.getParent().getName() : pageName;
           return new ResolvedStructuredDataObject(ORScope.PROJECT, actualPageName, objectName, proj);
       }
       var shared = getFrom(structuredDataSharedOR, pageName, objectName);
       if (shared != null) {
           markSharedUsage("StructuredDataOR");
           String actualPageName = shared.getParent() != null ? shared.getParent().getName() : pageName;
           return new ResolvedStructuredDataObject(ORScope.SHARED, actualPageName, objectName, shared);
       }
       return null;
   }

    private ObjectGroup<WebORObject> getFrom(WebOR or, String page, String obj) {
        if (or == null) return null;
        var p = or.getPageByName(page);
        return (p == null) ? null : p.getObjectGroupByName(obj);
    }
    
    private ObjectGroup<MobileORObject> getFrom(MobileOR or, String page, String obj) {
        if (or == null) return null;
        MobileORPage p = or.getPageByName(page);
        return (p == null) ? null : p.getObjectGroupByName(obj);
    }
    
    private ObjectGroup<StructuredDataORObject> getFrom(StructuredData or, String page, String obj) {
        if (or == null) return null;
        StructuredDataORPage p = or.getPageByName(page);
        return (p == null) ? null : p.getObjectGroupByName(obj);
    }
    
    /**
     * Deep-clones an object group and its objects into another page.
     */
    private ObjectGroup<WebORObject> cloneGroupIntoPage(ObjectGroup<WebORObject> originalGroup, WebORPage targetPage) {
        ObjectGroup<WebORObject> newGroup = new ObjectGroup<>(originalGroup.getName(), targetPage);
        for (WebORObject obj : originalGroup.getObjects()) {
            WebORObject cloned = new WebORObject();
            cloned.setName(obj.getName());
            cloned.setParent(newGroup);
            obj.clone(cloned);
            newGroup.getObjects().add(cloned);
        }
        return newGroup;
    }

    /**
     * Generates a unique name by appending "(n)" when duplicates exist.
     */
    private String generateUniqueName(String baseName, Predicate<String> exists) {
        if (baseName == null || baseName.isBlank()) return baseName;
        String candidate = baseName;
        int counter = 1;
        while (exists.test(candidate)) {
            candidate = baseName + " (" + counter + ")";
            counter++;
        }
        return candidate;
    }

    private String generateUniquePageName(WebOR or, String baseName) {
        if (or == null) return baseName;
        return generateUniqueName(baseName, name -> or.getPageByName(name) != null);
    }

    private String generateUniqueGroupName(WebORPage page, String baseName) {
        if (page == null) return baseName;
        return generateUniqueName(baseName, name -> page.getObjectGroupByName(name) != null);
    }

    /**
     * Ensures a page exists in the given OR; creates one if missing.
     */
    private WebORPage getOrCreatePage(WebOR or, String pageName) {
        if (or == null || pageName == null) return null;
        WebORPage page = or.getPageByName(pageName);
        return (page != null) ? page : or.addPage(pageName);
    }

    /**
     * Copies all object groups from a source page to a target page.
     */
    private void copyAllGroups(WebORPage sourcePage, WebORPage targetPage) {
        if (sourcePage == null || targetPage == null) return;
        for (ObjectGroup<WebORObject> originalGroup : sourcePage.getObjectGroups()) {
            if (originalGroup == null) continue;
            targetPage.getObjectGroups().add(cloneGroupIntoPage(originalGroup, targetPage));
        }
    }

    private ObjectGroup<WebORObject> cloneGroupIntoPage(ObjectGroup<WebORObject> originalGroup, WebORPage targetPage, String newGroupName) {
        ObjectGroup<WebORObject> newGroup = new ObjectGroup<>(newGroupName, targetPage);
        if (originalGroup.getObjects() != null && !originalGroup.getObjects().isEmpty()) {
            WebORObject sourceObj = originalGroup.getObjects().get(0);
            WebORObject cloned = new WebORObject();
            cloned.setName(newGroupName);
            cloned.setParent(newGroup);
            sourceObj.clone(cloned);
            newGroup.getObjects().add(cloned);
        }
        return newGroup;
    }
    
    /**
     * Copies a project WebOR page into the shared OR using a unique name.
     *
     * @param sourcePageName project page
     * @param targetPageName desired shared page name
     * @return actual created page name
     */
    public String copyWebPage(String sourcePageName, String targetPageName) {
        WebOR projectOR = getWebOR();
        WebOR sharedOR  = getWebSharedOR();
        if (projectOR == null || sharedOR == null) {
            return null;
        }
        WebORPage sourcePage = projectOR.getPageByName(sourcePageName);
        if (sourcePage == null) {
            return null;
        }
        String uniqueTargetName = generateUniquePageName(sharedOR, targetPageName);
        WebORPage targetPage = getOrCreatePage(sharedOR, uniqueTargetName);
        copyAllGroups(sourcePage, targetPage);
        sharedOR.setSaved(false);
        if (useYamlFormat) {
            saveWebPageNow(targetPage);
        }
        LOG.info(() -> "Copied Web Page '" + sourcePageName + "' to SHARED page '" + uniqueTargetName + "' successfully.");
        return uniqueTargetName;
    }

    /**
     * Copies a WebOR object into a shared page (creating the page if needed)
     * using a unique object group name.
     *
     * @param source          resolved web object
     * @param targetPageName  target page in shared OR
     * @return new object name
     */
    public String copyWebObject(ResolvedWebObject source, String targetPageName) {
        if (source == null) return null;
        WebOR sharedOR = getWebSharedOR();
        if (sharedOR == null) return null;
        WebORPage targetPage = getOrCreatePage(sharedOR, targetPageName);
        if (targetPage == null) return null;
        ObjectGroup<WebORObject> originalGroup = source.getGroup();
        if (originalGroup == null) return null;
        String originalName = originalGroup.getName();
        String baseName = originalName.replaceAll("_Copy_\\d+$", "");
        String uniqueName;
        int index = 1;
        do {
            uniqueName = baseName + "_Copy_" + index++;
        } while (targetPage.getObjectGroupByName(uniqueName) != null);
        ObjectGroup<WebORObject> newGroup = cloneGroupIntoPage(originalGroup, targetPage, uniqueName);
        targetPage.getObjectGroups().add(newGroup);
        sharedOR.setSaved(false);
        if (useYamlFormat) {
            saveWebPageNow(targetPage);
        }
        LOG.info(
            "Copied Web Object '" + originalName +
            "' to SHARED as '" + uniqueName + "'"
        );
        return uniqueName;
    }
    
    public String moveWebObject(ResolvedWebObject source, String targetPageName) {
        if (source == null) return null;
        WebOR sharedOR = getWebSharedOR();
        if (sharedOR == null) return null;
        WebORPage targetPage = getOrCreatePage(sharedOR, targetPageName);
        if (targetPage == null) return null;
        ObjectGroup<WebORObject> originalGroup = source.getGroup();
        if (originalGroup == null) return null;
        String originalName = originalGroup.getName();
        if (targetPage.getObjectGroupByName(originalName) != null) {
            return null;
        }
        ORPageInf sourcePage = originalGroup.getParent();
        if (sourcePage != null) {
            sourcePage.getObjectGroups().remove(originalGroup);
            sourcePage.getRoot().setSaved(false);
            if (useYamlFormat && sourcePage instanceof WebORPage) {
                saveWebPageNow((WebORPage) sourcePage);
            }
        }
        originalGroup.setParent(targetPage);
        targetPage.getObjectGroups().add(originalGroup);
        sharedOR.setSaved(false);
        if (useYamlFormat) {
            saveWebPageNow(targetPage);
        }
        LOG.info(
            "Moved Web Object '" + originalName +
            "' to SHARED page '" + targetPageName + "'"
        );
        return originalName;
    }

    /**
     * Copies a project MobileOR page into the shared Mobile OR using a unique name.
     * @param sourcePageName project page to copy from
     * @param targetPageName desired shared page name (will uniquify if needed)
     * @return actual created page name in shared OR, or null on failure
     */
    public String copyMobilePage(String sourcePageName, String targetPageName) {
        MobileOR projectMOR = getMobileOR();
        MobileOR sharedMOR  = getMobileSharedOR();
        if (projectMOR == null || sharedMOR == null) return null;
        MobileORPage sourcePage = projectMOR.getPageByName(sourcePageName);
        if (sourcePage == null) return null;
        String uniqueTargetName = generateUniquePageName(sharedMOR, targetPageName);
        MobileORPage targetPage = getOrCreateMobilePage(sharedMOR, uniqueTargetName);
        copyAllMobileGroups(sourcePage, targetPage);
        sharedMOR.setSaved(false);
        if (useYamlFormat) {
            saveMobilePageNow(targetPage);
        }
        LOG.info(() -> "Copied Mobile Page '" + sourcePageName + "' to SHARED page '" + uniqueTargetName + "' successfully.");
        return uniqueTargetName;
    }

    /**
     * Copies a MobileOR object into a target shared Mobile page (creates page if needed)
     * using a unique object group name.
     * @param source resolved mobile object (from project OR)
     * @param targetPageName target page name in shared Mobile OR
     * @return new object name created in shared OR, or null on failure
     */
    public String copyMobileObject(ResolvedMobileObject source, String targetPageName) {
        if (source == null) return null;
        MobileOR sharedMOR = getMobileSharedOR();
        if (sharedMOR == null) return null;
        MobileORPage targetPage = getOrCreateMobilePage(sharedMOR, targetPageName);
        if (targetPage == null) return null;
        ObjectGroup<MobileORObject> originalGroup = source.getGroup();
        if (originalGroup == null) return null;
        String originalName = originalGroup.getName();
        String baseName = originalName.replaceAll("_Copy_\\d+$", "");
        String uniqueName;
        int index = 1;
        do {
            uniqueName = baseName + "_Copy_" + index++;
        } while (targetPage.getObjectGroupByName(uniqueName) != null);
        ObjectGroup<MobileORObject> newGroup = cloneMobileGroupIntoPage(originalGroup, targetPage, uniqueName);
        targetPage.getObjectGroups().add(newGroup);
        sharedMOR.setSaved(false);
        if (useYamlFormat) {
            saveMobilePageNow(targetPage);
        }
        LOG.info(
            "Copied Mobile Object '" + originalName +
            "' to SHARED as '" + uniqueName + "'"
        );
        return uniqueName;
    }
    
    public String moveMobileObject(ResolvedMobileObject source, String targetPageName) {
        if (source == null) return null;
        MobileOR sharedOR = getMobileSharedOR();
        if (sharedOR == null) return null;
        MobileORPage targetPage = getOrCreateMobilePage(sharedOR, targetPageName);
        if (targetPage == null) return null;
        ObjectGroup<MobileORObject> originalGroup = source.getGroup();
        if (originalGroup == null) return null;
        String originalName = originalGroup.getName();
        if (targetPage.getObjectGroupByName(originalName) != null) {
            return null;
        }
        ORPageInf sourcePage = originalGroup.getParent();
        if (sourcePage != null) {
            sourcePage.getObjectGroups().remove(originalGroup);
            sourcePage.getRoot().setSaved(false);

            if (useYamlFormat && sourcePage instanceof MobileORPage) {
                saveMobilePageNow((MobileORPage) sourcePage);
            }
        }
        originalGroup.setParent(targetPage);
        targetPage.getObjectGroups().add(originalGroup);
        sharedOR.setSaved(false);
        if (useYamlFormat) {
            saveMobilePageNow(targetPage);
        }
        LOG.info(
            "Moved Mobile Object Group '" + originalName +
            "' to SHARED page '" + targetPageName + "'"
        );
        return originalName;
    }

    private String generateUniquePageName(MobileOR mor, String baseName) {
        if (mor == null) return baseName;
        return generateUniqueName(baseName, name -> mor.getPageByName(name) != null);
    }

    private String generateUniqueMobileGroupName(MobileORPage page, String baseName) {
        if (page == null) return baseName;
        return generateUniqueName(baseName, name -> page.getObjectGroupByName(name) != null);
    }

    private MobileORPage getOrCreateMobilePage(MobileOR mor, String pageName) {
        if (mor == null || pageName == null) return null;
        MobileORPage page = mor.getPageByName(pageName);
        return (page != null) ? page : mor.addPage(pageName);
    }

    private void copyAllMobileGroups(MobileORPage sourcePage, MobileORPage targetPage) {
        if (sourcePage == null || targetPage == null) return;
        for (ObjectGroup<MobileORObject> originalGroup : sourcePage.getObjectGroups()) {
            if (originalGroup == null) continue;
            targetPage.getObjectGroups().add(cloneMobileGroupIntoPage(originalGroup, targetPage, originalGroup.getName()));
        }
    }

    private ObjectGroup<MobileORObject> cloneMobileGroupIntoPage(ObjectGroup<MobileORObject> originalGroup, MobileORPage targetPage, String newGroupName) {
        ObjectGroup<MobileORObject> newGroup = new ObjectGroup<>(newGroupName, targetPage);
        if (originalGroup.getObjects() != null && !originalGroup.getObjects().isEmpty()) {
            MobileORObject sourceObj = originalGroup.getObjects().get(0);
            MobileORObject cloned = new MobileORObject();
            cloned.setName(newGroupName);
            cloned.setParent(newGroup);
            sourceObj.clone(cloned);
            newGroup.getObjects().add(cloned);
        }
        return newGroup;
    }
    
    /**
     * Marks that the current project has used a shared object,
     * updating shared OR metadata.
     */
    private void markSharedUsage(String orType) {
        if (sProject == null || sProject.getName() == null) {
            return;
        }

        if ("WebOR".equals(orType)) {
            webSharedUsageProjects.add(sProject.getName());
        } else if ("MobileOR".equals(orType)) {
            mobileSharedUsageProjects.add(sProject.getName());
        }
    }
    
    // ============ YAML-related stub methods for backward compatibility ============
    // These methods are placeholders for future YAML OR support integration

    /**
     * Check if using YAML format.
     * @return true if YAML format is enabled, false if using XML
     */
    public boolean isUsingYamlFormat() {
        return useYamlFormat;
    }

    /**
     * Enable or disable YAML format for this repository.
     * @param useYaml true to use YAML format, false for XML
     */
    public void setUseYamlFormat(boolean useYaml) {
        this.useYamlFormat = useYaml;
        if (useYaml && yamlReader == null) {
            yamlReader = new YamlORReader(this);
            yamlWriter = new YamlORWriter();
        }
    }
    
    /**
     * Save a Web page immediately.
     * For YAML format, writes individual page file.
     * For XML format, this is a no-op as XML saves the entire OR at once.
     * @param page the page to save
     */
    public void saveWebPageNow(WebORPage page) {
        if (!useYamlFormat || yamlWriter == null || page == null) return;
        try {
            File repoRoot =
                (page.getRoot().getScope() == WebOR.ORScope.SHARED)
                    ? new File(getSharedORRepLocation())
                    : new File(getORRepLocation());
            File webPagesDir = new File(repoRoot, "Web");
            if (!webPagesDir.exists()) {
                webPagesDir.mkdirs();
            }
            yamlWriter.writeWebPage(page, webPagesDir);
            page.getRoot().setSaved(true);
        } catch (IOException e) {
            LOG.log(
                Level.SEVERE,
                "Failed to save Web page: " + page.getName(),
                e
            );
        }
    }
    
    /**
     * Save a Mobile page immediately.
     * For YAML format, writes individual page file.
     * For XML format, this is a no-op as XML saves the entire OR at once.
     * @param page the page to save
     */
    public void saveMobilePageNow(MobileORPage page) {
        if (!useYamlFormat || yamlWriter == null || page == null) return;
        try {
            File repoRoot =
                (page.getRoot().getScope() == MobileOR.ORScope.SHARED)
                    ? new File(getSharedORRepLocation())
                    : new File(getORRepLocation());
            File mobilePagesDir = new File(repoRoot, "Mobile");
            if (!mobilePagesDir.exists()) {
                mobilePagesDir.mkdirs();
            }
            yamlWriter.writeMobilePage(page, mobilePagesDir);
            page.getRoot().setSaved(true);
        } catch (IOException e) {
            LOG.log(
                Level.SEVERE,
                "Failed to save Mobile page: " + page.getName(),
                e
            );
        }
    }
    
    /**
     * Save an Structured Data page immediately.
     * For YAML format, writes individual page file.
     * For XML format, this is a no-op as XML saves the entire OR at once.
     * @param page the page to save
     */
    public void saveStructuredDataPageNow(com.ing.datalib.or.structureddata.StructuredDataORPage page) {
        if (useYamlFormat && yamlWriter != null) {
            try {
                File structuredDataOrRepLocation = new File(getStructuredDataORRepLocation());
                File structuredDataPagesDir = new File(structuredDataOrRepLocation, "StructuredData/pages");
                if (!structuredDataPagesDir.exists()) {
                    structuredDataPagesDir.mkdirs();
                }
                yamlWriter.writeStructuredDataPage(page, structuredDataPagesDir);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to save Structured Data page: " + page.getName(), e);
            }
        }
        // XML format doesn't need per-page saves
    }
    
    /**
     * Rename a Web page YAML file.
     * Only works in YAML format mode.
     * @param oldName old page name
     * @param newName new page name
     * @return true if renamed successfully, false otherwise
     */
    public boolean renameWebPageYaml(String oldName, String newName, WebOR.ORScope scope) {
        if (!useYamlFormat || yamlWriter == null) {
            return true;
        }
        if (oldName == null || newName == null || oldName.equals(newName)) {
            return true;
        }
        try {
            File repoRoot =
                (scope == WebOR.ORScope.SHARED)
                    ? new File(getSharedORRepLocation())
                    : new File(getORRepLocation());
            return yamlWriter.renameWebPage(oldName, newName, repoRoot);

        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                "Failed to rename Web page [" + scope + "] from " +
                oldName + " to " + newName, e);
            return false;
        }
    }
    
    /**
     * Rename a Mobile page YAML file.
     * Only works in YAML format mode.
     * @param oldName old page name
     * @param newName new page name
     * @return true if renamed successfully, false otherwise
     */
    public boolean renameMobilePageYaml(String oldName, String newName, MobileOR.ORScope scope) {
        if (!useYamlFormat || yamlWriter == null) {
            return true; // XML mode - no-op
        }
        if (oldName == null || newName == null || oldName.equals(newName)) {
            return true;
        }
        try {
            File morRepLocation =
                (scope == MobileOR.ORScope.SHARED)
                    ? new File(getSharedORRepLocation())
                    : new File(getORRepLocation());
            return yamlWriter.renameMobilePage(oldName, newName, morRepLocation);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to rename Mobile page from " + oldName + " to " + newName, e);
            return false;
        }
    }
    
    /**
     * Rename an Structured Data page YAML file.
     * Only works in YAML format mode.
     * @param oldName old page name
     * @param newName new page name
     * @return true if renamed successfully, false otherwise
     */
    public boolean renameStructuredDataPageYaml(String oldName, String newName) {
        if (!useYamlFormat || yamlWriter == null) {
            return true; // XML mode - no-op
        }
        try {
            File structuredDataRepLocation = new File(getStructuredDataORRepLocation());
            return yamlWriter.renameStructuredDataPage(oldName, newName, structuredDataRepLocation);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to rename Structured Data page from " + oldName + " to " + newName, e);
            return false;
        }
    }
    
    public boolean deleteWebPageYaml(String pageName) {
        if (!useYamlFormat || yamlWriter == null) {
            return true; // XML mode - no-op
        }
        if (pageName == null || pageName.isBlank()) {
            return true;
        }
        try {
            File orRepLocation = new File(getORRepLocation());
            return yamlWriter.deleteWebPage(pageName, orRepLocation);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to delete Web page YAML: " + pageName, e);
            return false;
        }
    }
    
    public boolean deleteMobilePageYaml(String pageName) {
        if (!useYamlFormat || yamlWriter == null) {
            return true; // XML mode - no-op
        }
        if (pageName == null || pageName.isBlank()) {
            return true;
        }
        try {
            File morRepLocation = new File(getORRepLocation());
            return yamlWriter.deleteMobilePage(pageName, morRepLocation);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to delete Mobile page YAML: " + pageName, e);
            return false;
        }
    }
    
    public boolean deleteStructuredDataPageYaml(String pageName) {
        if (!useYamlFormat || yamlWriter == null) {
            return true; // XML mode - no-op
        }
        if (pageName == null || pageName.isBlank()) {
            return true;
        }
        try {
            File apiorRepLocation = new File(getORRepLocation());
            return yamlWriter.deleteStructuredDataPage(pageName, apiorRepLocation);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to delete Structured Data page YAML: " + pageName, e);
            return false;
        }
    }
}