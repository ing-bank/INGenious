
package com.ing.datalib.or;

import com.ing.datalib.component.Project;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.web.WebOR;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.datalib.or.web.WebOR.ORScope;
import com.ing.datalib.or.web.WebORObject;
import com.ing.datalib.or.web.WebORPage;
import com.ing.datalib.or.mobile.ResolvedMobileObject;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.datalib.or.mobile.MobileORPage;


import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
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
    
    private final Set<String> sharedUsageProjects = new HashSet<>();

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
            File sharedFile = new File(getSharedORLocation());
            if (sharedFile.exists()) {
                webSharedOR = XML_MAPPER.readValue(sharedFile, WebOR.class);
                webSharedOR.setName("Shared Web Objects");
            } else {
                webSharedOR = new WebOR("Shared Web Objects");
            }

            File projFile = new File(getORLocation());
            if (projFile.exists()) {
                webProjectOR = XML_MAPPER.readValue(projFile, WebOR.class);
                webProjectOR.setName(sProject.getName());
            } else {
                webProjectOR = new WebOR(sProject.getName());
            }
            
            File sharedmorFile = new File(getSharedMORLocation());
            if (sharedmorFile.exists()) {
                mobileSharedOR = XML_MAPPER.readValue(sharedmorFile, MobileOR.class);
                mobileSharedOR.setName("Shared Mobile Objects");
            } else {
                mobileSharedOR = new MobileOR("Shared Mobile Objects");
            }

            File morFile = new File(getMORLocation());
            if (morFile.exists()) {
                mobileProjectOR = XML_MAPPER.readValue(morFile, MobileOR.class);
                mobileProjectOR.setName(sProject.getName());
            } else {
                mobileProjectOR = new MobileOR(sProject.getName());
            }

            if (webSharedOR != null) {
                webSharedOR.setObjectRepository(this);
                webSharedOR.setSaved(true);
                webSharedOR.setRepLocationOverride(getSharedORRepLocation());
                webSharedOR.setScope(ORScope.SHARED);
            }
            if (webProjectOR != null) {
                webProjectOR.setObjectRepository(this);
                webProjectOR.setSaved(true);
                webProjectOR.setScope(ORScope.PROJECT);
            }
            if (mobileSharedOR != null) {
                mobileSharedOR.setObjectRepository(this);
                mobileSharedOR.setSaved(true);
                mobileSharedOR.setRepLocationOverride(getSharedMORRepLocation());
                mobileSharedOR.setScope(MobileOR.ORScope.SHARED);
                
            }
            if (mobileProjectOR != null) {
                mobileProjectOR.setObjectRepository(this);
                mobileProjectOR.setSaved(true);
                mobileProjectOR.setScope(MobileOR.ORScope.PROJECT);
            }


            LOG.log(Level.INFO, "Shared WebOR loaded: {0}", (webSharedOR != null));
            LOG.log(Level.INFO, "Project WebOR loaded: {0}", (webProjectOR != null));
            LOG.log(Level.INFO, "Shared MobileOR loaded: {0}", (mobileSharedOR != null));
            LOG.log(Level.INFO, "Project MobileOR loaded: {0}", (mobileProjectOR != null));
        } catch (IOException ex) {
            Logger.getLogger(ObjectRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getORLocation() {
        return sProject.getLocation() + File.separator + "OR.object";
    }
    public String getSharedORLocation() {
        return "Shared" + File.separator + "SharedWebObjects" + File.separator + "SharedOR.object";
    }
    public String getIORLocation() {
        return sProject.getLocation() + File.separator + "IOR.object";
    }
    public String getMORLocation() {
        return sProject.getLocation() + File.separator + "MOR.object";
    }
    public String getSharedMORLocation() {
        return "Shared" + File.separator + "SharedMobileObjects" + File.separator + "SharedMOR.object";
    }
    public String getORRepLocation() {
        return sProject.getLocation() + File.separator + "ObjectRepository";
    }
    public String getSharedORRepLocation() {
        return "Shared" + File.separator + "SharedWebObjects" + File.separator + "SharedObjectRepository";
    }
    public String getIORRepLocation() {
        return sProject.getLocation() + File.separator + "ImageObjectRepository";
    }
    public String getMORRepLocation() {
        return sProject.getLocation() + File.separator + "MobileObjectRepository";
    }
    public String getSharedMORRepLocation() {
        return "Shared" + File.separator + "SharedMobileObjects" + File.separator + "MobileObjectRepository";
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

    /**
     * Saves updated shared, project, and mobile ORs to disk.
     * Also updates shared project usage metadata when required.
     */

    public void save() {
        try {
            java.util.List<String> existingProjects = (webSharedOR != null) ? webSharedOR.getProjects() : java.util.List.of();
            java.util.LinkedHashSet<String> mergedProjects = new java.util.LinkedHashSet<>();
            if (existingProjects != null) mergedProjects.addAll(existingProjects);
            mergedProjects.addAll(sharedUsageProjects);
            boolean projectsChanged = false;
            if (webSharedOR != null) {
                java.util.ArrayList<String> mergedList = new java.util.ArrayList<>(mergedProjects);
                java.util.List<String> current = webSharedOR.getProjects();
                projectsChanged = (current == null) || !new java.util.LinkedHashSet<>(current).equals(mergedProjects);
                if (projectsChanged) {
                    webSharedOR.setProjects(mergedList);
                }
            }
            if (webSharedOR != null && (!webSharedOR.isSaved() || projectsChanged)) {
                XML_MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(getSharedORLocation()), webSharedOR);
                webSharedOR.setSaved(true);
            }
            if (webProjectOR != null && !webProjectOR.isSaved()) {
                XML_MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(getORLocation()), webProjectOR);
                webProjectOR.setSaved(true);
            }
            if (mobileSharedOR != null && !mobileSharedOR.isSaved()) {
                XML_MAPPER.writerWithDefaultPrettyPrinter()
                        .writeValue(new File(getSharedMORLocation()), mobileSharedOR);
                mobileSharedOR.setSaved(true);
            }

            if (mobileProjectOR != null && !mobileProjectOR.isSaved()) {
                XML_MAPPER.writerWithDefaultPrettyPrinter()
                        .writeValue(new File(getMORLocation()), mobileProjectOR);
                mobileProjectOR.setSaved(true);
            }
        } catch (IOException ex) {
            Logger.getLogger(ObjectRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
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

    public void renameObject(ObjectGroup<WebORObject> group, String newName) {
        if (group == null || newName == null || newName.isBlank()) return;
        var parentPage = group.getParent();
        if (parentPage == null) return;
        String oldName = group.getName();
        if (oldName.equals(newName)) return;
        boolean inProject = (webProjectOR != null) &&
            (webProjectOR.getPageByName(parentPage.getName()) == parentPage);

        boolean inShared  = !inProject && (webSharedOR != null) &&
            (webSharedOR.getPageByName(parentPage.getName()) == parentPage);
        if (inProject && webProjectOR != null) {
            webProjectOR.setSaved(false);
            sProject.refactorObjectName(WebOR.ORScope.PROJECT, parentPage.getName(), oldName, newName);
        } else if (inShared && webSharedOR != null) {
            webSharedOR.setSaved(false);
            markSharedUsage();
            sProject.refactorObjectName(WebOR.ORScope.SHARED, parentPage.getName(), oldName, newName);
        } else {
            sProject.refactorObjectName(parentPage.getName(), oldName, newName);
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
            }
        }
        if (renamed) {
            sProject.refactorPageName(scopeRenamed, oldName, newName);
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

        if (pageRef.scope == WebOR.ORScope.PROJECT) {
            var g = getFrom(webProjectOR, pageRef.name, objectName);
            if (g != null) {
                String actualPageName = g.getParent() != null ? g.getParent().getName() : pageRef.name;
                return new ResolvedWebObject(WebOR.ORScope.PROJECT, actualPageName, objectName, g);
            }
            return null;
        }
        if (pageRef.scope == WebOR.ORScope.SHARED) {
            var g = getFrom(webSharedOR, pageRef.name, objectName);
            if (g != null) {
                markSharedUsage();
                String actualPageName = g.getParent() != null ? g.getParent().getName() : pageRef.name;
                return new ResolvedWebObject(WebOR.ORScope.SHARED, actualPageName, objectName, g);
            }
            return null;
        }

        var proj = getFrom(webProjectOR, pageRef.name, objectName);
        if (proj != null) {
            String actualPageName = proj.getParent() != null ? proj.getParent().getName() : pageRef.name;
            return new ResolvedWebObject(WebOR.ORScope.PROJECT, actualPageName, objectName, proj);
        }
        var shared = getFrom(webSharedOR, pageRef.name, objectName);
        if (shared != null) {
            markSharedUsage();
            String actualPageName = shared.getParent() != null ? shared.getParent().getName() : pageRef.name;
            return new ResolvedWebObject(WebOR.ORScope.SHARED, actualPageName, objectName, shared);
        }
        return null;
    }

    public ResolvedMobileObject resolveMobileObject(ResolvedMobileObject.PageRef pageRef, String objectName) {
        if (pageRef == null || objectName == null) return null;
        if (pageRef.scope == ORScope.PROJECT) {
            var g = getFrom(mobileProjectOR, pageRef.name, objectName);
            if (g != null) {
                String actualPageName = g.getParent() != null ? g.getParent().getName() : pageRef.name;
                return new ResolvedMobileObject(ORScope.PROJECT, actualPageName, objectName, g);
            }
            return null;
        }
        if (pageRef.scope == ORScope.SHARED) {
            var g = getFrom(mobileSharedOR, pageRef.name, objectName);
            if (g != null) {
                markSharedUsage();
                String actualPageName = g.getParent() != null ? g.getParent().getName() : pageRef.name;
                return new ResolvedMobileObject(ORScope.SHARED, actualPageName, objectName, g);
            }
            return null;
        }
        return resolveMobileObjectWithScope(pageRef.name, objectName);
    }

    /**
     * Resolves a WebOR object by searching project scope first, then shared scope.
     *
     * @param pageName   page to search
     * @param objectName object group name
     * @return resolved WebOR object with scope metadata
     */

    public ResolvedWebObject resolveWebObjectWithScope(String pageName, String objectName) {
        var proj = getFrom(webProjectOR, pageName, objectName);
        if (proj != null) {
            String actualPageName = proj.getParent() != null ? proj.getParent().getName() : pageName;
            return new ResolvedWebObject(ORScope.PROJECT, actualPageName, objectName, proj);
        }
        var shared = getFrom(webSharedOR, pageName, objectName);
        if (shared != null) {
            markSharedUsage();
            String actualPageName = shared.getParent() != null ? shared.getParent().getName() : pageName;
            return new ResolvedWebObject(ORScope.SHARED, actualPageName, objectName, shared);
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
       var proj = getFrom(mobileProjectOR, pageName, objectName);
       if (proj != null) {
           String actualPageName = proj.getParent() != null ? proj.getParent().getName() : pageName;
           return new ResolvedMobileObject(ORScope.PROJECT, actualPageName, objectName, proj);
       }
       var shared = getFrom(mobileSharedOR, pageName, objectName);
       if (shared != null) {
           markSharedUsage();
           String actualPageName = shared.getParent() != null ? shared.getParent().getName() : pageName;
           return new ResolvedMobileObject(ORScope.SHARED, actualPageName, objectName, shared);
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

    private String generateUniqueName(String baseName, java.util.function.Predicate<String> exists) {
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
        String baseName   = originalGroup.getName();
        String uniqueName = generateUniqueGroupName(targetPage, baseName);
        ObjectGroup<WebORObject> newGroup = cloneGroupIntoPage(originalGroup, targetPage, uniqueName);
        targetPage.getObjectGroups().add(newGroup);
        sharedOR.setSaved(false);
        LOG.info(() -> "Copied Web Object '" + baseName + "' to SHARED as '" + uniqueName + "'");
        return uniqueName;
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
        LOG.info(() -> "Copied Mobile Page '" + sourcePageName
                + "' to SHARED page '" + uniqueTargetName + "' successfully.");
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
        String baseName   = originalGroup.getName();
        String uniqueName = generateUniqueMobileGroupName(targetPage, baseName);
        ObjectGroup<MobileORObject> newGroup = cloneMobileGroupIntoPage(originalGroup, targetPage, uniqueName);
        targetPage.getObjectGroups().add(newGroup);
        sharedMOR.setSaved(false);
        LOG.info(() -> "Copied Mobile Object '" + baseName + "' to SHARED as '" + uniqueName + "'");
        return uniqueName;
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

    private void markSharedUsage() {
        if (sProject != null && sProject.getName() != null && !sProject.getName().isBlank()) {
            sharedUsageProjects.add(sProject.getName());
        }
    }
}