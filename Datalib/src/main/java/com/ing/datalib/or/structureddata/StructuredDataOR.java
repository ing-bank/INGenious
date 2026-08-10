package com.ing.datalib.or.structureddata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.common.ORRootInf;
import com.ing.datalib.or.common.ORUtils;
import com.ing.datalib.or.web.WebOR;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.swing.tree.TreeNode;

/**
 * API Object Repository root class.
 * Contains JsonPath and Xpath as the only locator attributes.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "Root")
public class StructuredDataOR implements ORRootInf<StructuredDataORPage> {
    public static final List<String> OBJECT_PROPS = new ArrayList<>(
        Arrays.asList("JsonPath", "Xpath")
    );

    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String name;

    @JacksonXmlProperty(localName = "Page")
    @JacksonXmlElementWrapper(useWrapping = false, localName = "Page")
    private List<StructuredDataORPage> pages;

    @JacksonXmlProperty(isAttribute = true)
    private String type;

    @JacksonXmlProperty(isAttribute = true)
    private ORScope scope = ORScope.PROJECT;

    @JacksonXmlElementWrapper(localName = "projects")
    @JacksonXmlProperty(localName = "project")
    private List<String> projects = new ArrayList<>();

    @JsonIgnore
    private ObjectRepository objectRepository;

    @JsonIgnore
    private Boolean saved = true;

    public StructuredDataOR() {
        this.pages = new ArrayList<>();
    }

    public StructuredDataOR(String name) {
        this.name = name;
        this.type = "StructuredDataOR";
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

    @Override
    public List<StructuredDataORPage> getPages() {
        return pages;
    }

    @Override
    public void setPages(List<StructuredDataORPage> pages) {
        this.pages = pages;
        for (StructuredDataORPage page : pages) {
            page.setRoot(this);
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonIgnore
    @Override
    public StructuredDataORPage getPageByName(String pageName) {
        for (StructuredDataORPage page : pages) {
            if (page.getName().equalsIgnoreCase(pageName)) {
                return page;
            }
        }
        return null;
    }

    @JsonIgnore
    public StructuredDataORPage getPageByTitle(String title) {
        for (StructuredDataORPage page : pages) {
            if (page.getTitle().equals(title)) {
                return page;
            }
        }
        return null;
    }

    @JsonIgnore
    @Override
    public StructuredDataORPage addPage() {
        String pName = "StructuredDataPage";
        int i = 0;
        String pageName;
        do {
            pageName = pName + i++;
        } while (getPageByName(pageName) != null);

        return addPage(pageName);
    }

    @JsonIgnore
    @Override
    public StructuredDataORPage addPage(String pageName) {
        if (getPageByName(pageName) == null) {
            StructuredDataORPage page = new StructuredDataORPage(pageName, this);
            pages.add(page);
            // Structured Data OR uses YAML format - no folder creation needed
            setSaved(false);

            // Auto-save for YAML format
            if (objectRepository != null && objectRepository.isUsingYamlFormat()) {
                objectRepository.saveStructuredDataPageNow(page);
            }
            return page;
        }
        return null;
    }

    @JsonIgnore
    @Override
    public void deletePage(String pageName) {
        StructuredDataORPage page = getPageByName(pageName);
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
        return pages == null ? 0 : pages.size();
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
    public Enumeration children() {
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
        return new TreeNode[] { this };
    }

    @JsonIgnore
    @Override
    public String getRepLocation() {
        return getObjectRepository().getORRepLocation();
    }

    @JsonIgnore
    @Override
    public void sort() {
        ORUtils.sort(this);
    }

    public enum ORScope {
        PROJECT,
        SHARED
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

    public List<String> getSharedProjects() {
        return isShared() ? projects : Collections.emptyList();
    }

    public void setSharedProjects(List<String> projects) {
        this.projects = projects;
    }
}
