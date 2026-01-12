
package com.ing.datalib.or;

import com.ing.datalib.component.Project;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.web.WebOR;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * 
 */
public class ObjectRepository {

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private final Project sProject;

    private WebOR webSharedOR;
    private WebOR webProjectOR;
    private WebOR webOR;
    private MobileOR mobileOR;

    public ObjectRepository(Project sProject) {
        this.sProject = sProject;
        init();
    }

    private void init() {
        try {
            // --- Shared OR ---
            File sharedFile = new File(geSharedORLocation());
            if (sharedFile.exists()) {
                webSharedOR = XML_MAPPER.readValue(sharedFile, WebOR.class);
                webSharedOR.setName("Shared Web Objects");
            }

            // --- Project OR ---
            File projFile = new File(getORLocation());
            if (projFile.exists()) {
                webProjectOR = XML_MAPPER.readValue(projFile, WebOR.class);
                webProjectOR.setName(sProject.getName());
            } else {
                webProjectOR = new WebOR(sProject.getName());
            }
            //webOR = mergeWebOR(webSharedOR, webProjectOR);

            File morFile = new File(getMORLocation());
            if (morFile.exists()) {
                mobileOR = XML_MAPPER.readValue(morFile, MobileOR.class);
                mobileOR.setName(sProject.getName());
            } else {
                mobileOR = new MobileOR(sProject.getName());
            }

            webSharedOR.setObjectRepository(this);
            webSharedOR.setSaved(true);
            webProjectOR.setObjectRepository(this);
            webProjectOR.setSaved(true);
            mobileOR.setObjectRepository(this);

            System.out.println("[OR] Shared loaded: " + (webSharedOR != null));
            System.out.println("[OR] Project loaded: " + (webProjectOR != null));
            System.out.println("[OR] Effective name: " + webOR.getName());

        } catch (IOException ex) {
            Logger.getLogger(ObjectRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getORLocation() {
        return sProject.getLocation() + File.separator + "OR.object";
    }
    
    public String geSharedORLocation() {
        return "Projects" + File.separator + "SharedOR.object";
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
        //return webOR;
        return webProjectOR;
    }

    public WebOR getWebSharedOR() {
        return webSharedOR;
    }

    public MobileOR getMobileOR() {
        return mobileOR;
    }
//
//    private WebOR mergeWebOR(WebOR shared, WebOR project) {
//        if (shared == null) return project;
//        if (project == null) return shared;
//
//        WebOR merged = new WebOR(
//            project.getName() != null ? project.getName() : shared.getName()
//        );
//
//        try {
//            BeanInfo info = Introspector.getBeanInfo(WebOR.class, Object.class);
//            for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
//                Method getter = pd.getReadMethod();
//                Method setter = pd.getWriteMethod();
//                if (getter == null || setter == null) continue;
//
//                Object sharedVal  = getter.invoke(shared);
//                Object projectVal = getter.invoke(project);
//
//                Object mergedVal = mergeValue(sharedVal, projectVal);
//                setter.invoke(merged, mergedVal);
//            }
//        } catch (Exception e) {
//            return project;
//        }
//
//        return merged;
//    }
    
    @SuppressWarnings({"rawtypes","unchecked"})
    private Object mergeValue(Object sharedVal, Object projectVal) {
        if (projectVal == null) return sharedVal;
        if (sharedVal  == null) return projectVal;

        if (sharedVal instanceof Map && projectVal instanceof Map) {
            Map merged = new LinkedHashMap<>();
            merged.putAll((Map) sharedVal);
            merged.putAll((Map) projectVal);
            return merged;
        }

        if (sharedVal instanceof Collection && projectVal instanceof Collection) {
            Collection<?> a = (Collection<?>) sharedVal;
            Collection<?> b = (Collection<?>) projectVal;
            Collection merged = (a instanceof List || b instanceof List) ? new ArrayList<>() : new LinkedHashSet<>();
            merged.addAll(a);
            merged.addAll(b);
            return merged;
        }

        if (sharedVal.getClass().isArray() && projectVal.getClass().isArray()) {
            int lenA = Array.getLength(sharedVal);
            int lenB = Array.getLength(projectVal);
            Object merged = Array.newInstance(sharedVal.getClass().getComponentType(), lenA + lenB);
            System.arraycopy(sharedVal, 0, merged, 0, lenA);
            System.arraycopy(projectVal, 0, merged, lenA, lenB);
            return merged;
        }

        try {
            Object merged = XML_MAPPER.updateValue(sharedVal, projectVal);
            return merged;
        } catch (com.fasterxml.jackson.databind.JsonMappingException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE,
                "WebOR merge failed: " + e.getMessage(), e);
            throw new IllegalStateException("Failed to merge WebOR with updateValue", e);
        }
    }


    public void save() {
        try {
            if (!webOR.isSaved()) {
                XML_MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(getORLocation()), webOR);
            }
            XML_MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(getMORLocation()), mobileOR);
        } catch (IOException ex) {
            Logger.getLogger(ObjectRepository.class.getName()).log(Level.SEVERE, null, ex);
        }
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
        return present;
    }

    public void renameObject(ObjectGroup group, String newName) {
        sProject.refactorObjectName(group.getParent().getName(), group.getName(), newName);
    }

    public void renamePage(ORPageInf page, String newName) {
        sProject.refactorPageName(page.getName(), newName);
    }

}
