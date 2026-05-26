
package com.ing.datalib.or.sap;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.TreeNode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.common.ORRootInf;
import com.ing.datalib.or.common.ORUtils;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "Root")
public class SapOR implements ORRootInf<SapORPage> {

    public final static List<String> OBJECT_PROPS
            = new ArrayList<>(Arrays.asList(
                    "id",
                    "name",
                    "Text"));

    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String name;

    @JacksonXmlProperty(localName = "Page")
    @JacksonXmlElementWrapper(useWrapping = false, localName = "Page")
    private List<SapORPage> pages;

    @JacksonXmlProperty(isAttribute = true)
    private String type;
    
    @JacksonXmlProperty(isAttribute = true)
    private ORScope scope = ORScope.PROJECT;
    
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JacksonXmlElementWrapper(localName = "projects")
    @JacksonXmlProperty(localName = "project")
    private List<String> projects = new ArrayList<>();

    @JsonIgnore
    private ObjectRepository objectRepository;

    @JsonIgnore
    private Boolean saved = true;
    
    @JsonIgnore
    private String repLocationOverride;

    public SapOR() {
        this.pages = new ArrayList<>();
    }

    public SapOR(String name) {
        this.name = name;
        this.type = "SapOR";
        this.pages = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public List<SapORPage> getPages() {
        return pages;
    }

    @Override
    public void setPages(List<SapORPage> pages) {
        this.pages = pages;
        for (SapORPage page : pages) {
            page.setRoot(this);
            if (page.getSource() == null) {
                page.setSource(isShared() ? ORScope.SHARED : ORScope.PROJECT);
            }
        }
    }

    @JsonIgnore
    @Override
    public SapORPage getPageByName(String pageName) {
        for (SapORPage page : pages) {
            if (page.getName().equalsIgnoreCase(pageName)) {
                return page;
            }
        }
        return null;
    }

    @JsonIgnore
    @Override
    public SapORPage addPage() {
        String pName = "SapPage";
        int i = 0;
        String pageName;
        do {
            pageName = pName + i++;
        } while (getPageByName(pageName) != null);

        return addPage(pageName);
    }

    @JsonIgnore
    @Override
    public SapORPage addPage(String pageName) {
        if (getPageByName(pageName) == null) {
            SapORPage page = new SapORPage(pageName, this);
            page.setSource(isShared() ? ORScope.SHARED : ORScope.PROJECT);
            pages.add(page);
            // Only create folder for non-YAML formats
            if (objectRepository == null || !objectRepository.isUsingYamlFormat()) {
                new File(page.getRepLocation()).mkdirs();
            }
            setSaved(false);
            
            // Auto-save for YAML format
            if (objectRepository != null && objectRepository.isUsingYamlFormat()) {
                objectRepository.saveSapPageNow(page);
            }
            return page;
        }
        return null;
    }

    @JsonIgnore
    @Override
    public void deletePage(String pageName) {
        SapORPage page = getPageByName(pageName);
        if (page != null) {
            pages.remove(page);
            setSaved(false);
        }
    }

    @JsonIgnore
    @Override
    public void setObjectRepository(ObjectRepository objRep) {
        this.objectRepository = objRep;
    }

    @JsonIgnore
    @Override
    public ObjectRepository getObjectRepository() {
        return objectRepository;
    }

    @JsonIgnore
    @Override
    public TreeNode getChildAt(int i) {
        return pages.get(i);
    }

    @JsonIgnore
    @Override
    public int getChildCount() {
        return pages == null ? 0
                : pages.size();
    }

    @JsonIgnore
    @Override
    public TreeNode getParent() {
        return null;
    }

    @JsonIgnore
    @Override
    public int getIndex(TreeNode tn) {
        return pages.indexOf(tn);
    }

    @JsonIgnore
    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isLeaf() {
        return getChildCount() == 0;
    }

    @JsonIgnore
    @Override
    public Enumeration<SapORPage> children() {
        return Collections.enumeration(pages);
    }

    @Override
    public String toString() {
        return name;
    }

    @JsonIgnore
    @Override
    public Boolean isSaved() {
        return saved;
    }

    @JsonIgnore
    @Override
    public void setSaved(Boolean saved) {
        this.saved = saved;
    }

    @JsonIgnore
    @Override
    public TreeNode[] getPath() {
        return new TreeNode[]{this};
    }

    @JsonIgnore
    public void setRepLocationOverride(String path) {
        this.repLocationOverride = path;
    }

    @JsonIgnore
    @Override
    public String getRepLocation() {
        return repLocationOverride != null
                ? repLocationOverride
                : getObjectRepository().getORRepLocation();
    }

    @JsonIgnore
    @Override
    public void sort() {
        ORUtils.sort(this);
    }
    
    public enum ORScope { 
        PROJECT, SHARED 
    }

    @JsonIgnore
    public ORScope getScope() { 
        return scope; 
    }
    
    public void setScope(ORScope scope) { 
        this.scope = scope; 
    }

    @JsonIgnore
    public boolean isShared() { 
        return scope == ORScope.SHARED; 
    }
    
    public List<String> getProjects() {
        return projects;
    }
    
    public void setProjects(List<String> projects) {
        this.projects = (projects == null) ? new ArrayList<>() : projects;
    }

    public List<String> getSharedProjects() {
        return isShared() ? projects : Collections.emptyList();
    }
    
    public void setSharedProjects(List<String> projects) {
        this.projects = projects;
    }
}
