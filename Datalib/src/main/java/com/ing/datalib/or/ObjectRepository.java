
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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ObjectRepository {
    private static final XmlMapper XML_MAPPER = new XmlMapper();
    private static final Logger LOG = Logger.getLogger(ObjectRepository.class.getName());

    private final Project sProject;
    private WebOR webSharedOR;
    private WebOR webProjectOR;
    private MobileOR mobileOR;
    
    private final Set<String> sharedUsageProjects = new HashSet<>();

    public ObjectRepository(Project sProject) {
        this.sProject = sProject;
        init();
    }

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

            File morFile = new File(getMORLocation());
            if (morFile.exists()) {
                mobileOR = XML_MAPPER.readValue(morFile, MobileOR.class);
                mobileOR.setName(sProject.getName());
            } else {
                mobileOR = new MobileOR(sProject.getName());
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
            if (mobileOR != null) {
                mobileOR.setObjectRepository(this);
            }

            LOG.log(Level.INFO, "Shared WebOR loaded: {0}", (webSharedOR != null));
            LOG.log(Level.INFO, "Project WebOR loaded: {0}", (webProjectOR != null));
        } catch (IOException ex) {
            Logger.getLogger(ObjectRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getORLocation() {
        return sProject.getLocation() + File.separator + "OR.object";
    }
    public String getSharedORLocation() {
        return "Projects" + File.separator + "SharedWebObjects" + File.separator + "SharedOR.object";
    }
    public String getIORLocation() {
        return sProject.getLocation() + File.separator + "IOR.object";
    }
    public String getMORLocation() {
        return sProject.getLocation() + File.separator + "MOR.object";
    }
    public String getORRepLocation() {
        return sProject.getLocation() + File.separator + "ObjectRepository";
    }
    public String getSharedORRepLocation() {
        return "Projects" + File.separator + "SharedWebObjects" + File.separator + "SharedObjectRepository";
    }
    public String getIORRepLocation() {
        return sProject.getLocation() + File.separator + "ImageObjectRepository";
    }
    public String getMORRepLocation() {
        return sProject.getLocation() + File.separator + "MobileObjectRepository";
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
        return mobileOR;
    }

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
            if (mobileOR != null) {
                XML_MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(getMORLocation()), mobileOR);
            }
        } catch (IOException ex) {
            Logger.getLogger(ObjectRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Boolean isObjectPresent(String pageName, String objectName) {
        return resolveWebObjectWithScope(pageName, objectName) != null;
    }

    public void renameObject(ObjectGroup group, String newName) {
        sProject.refactorObjectName(group.getParent().getName(), group.getName(), newName);
    }

    public void renamePage(ORPageInf page, String newName) {
        if (page == null || newName == null || newName.isBlank()) return;

        String oldName = page.getName();
        boolean renamed = false;
        ORScope scopeRenamed = null;

        if (webProjectOR != null) {
            var p = webProjectOR.getPageByName(oldName);
            if (p == page) {
                p.setName(newName);
                webProjectOR.setSaved(false);
                renamed = true;
                scopeRenamed = ORScope.PROJECT;
            }
        }
        if (!renamed && webSharedOR != null) {
            var s = webSharedOR.getPageByName(oldName);
            if (s == page) {
                s.setName(newName);
                webSharedOR.setSaved(false);
                renamed = true;
                scopeRenamed = ORScope.SHARED;
            }
        }
        if (scopeRenamed != null) {
            sProject.refactorPageName(scopeRenamed, oldName, newName);
        } else {
            sProject.refactorPageName(oldName, newName);
        }
    }

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

    private ObjectGroup<WebORObject> getFrom(WebOR or, String page, String obj) {
        if (or == null) return null;
        var p = or.getPageByName(page);
        return (p == null) ? null : p.getObjectGroupByName(obj);
    }
    
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

    private WebORPage getOrCreatePage(WebOR or, String pageName) {
        if (or == null || pageName == null) return null;
        WebORPage page = or.getPageByName(pageName);
        return (page != null) ? page : or.addPage(pageName);
    }

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
    
    private void markSharedUsage() {
        if (sProject != null && sProject.getName() != null && !sProject.getName().isBlank()) {
            sharedUsageProjects.add(sProject.getName());
        }
    }
}