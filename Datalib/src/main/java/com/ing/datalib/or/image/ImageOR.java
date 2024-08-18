
package com.ing.datalib.or.image;

import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.common.ORRootInf;
import com.ing.datalib.or.common.ORUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import javax.swing.tree.TreeNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "Root")
public class ImageOR implements ORRootInf<ImageORPage> {

    public final static List<String> OBJECT_PROPS
            = new ArrayList<>(Arrays.asList(
                    "Coordinates",
                    "Index",
                    "Offset",
                    "Precision",
                    "ROI",
                    "Reference",
                    "Text",
                    "Url"));

    @JacksonXmlProperty(isAttribute = true, localName = "ref")
    private String name;

    @JacksonXmlProperty(localName = "Page")
    @JacksonXmlElementWrapper(useWrapping = false, localName = "Page")
    private List<ImageORPage> pages;

    @JacksonXmlProperty(isAttribute = true)
    private String type;

    @JsonIgnore
    private ObjectRepository objectRepository;

    @JsonIgnore
    private Boolean saved = true;

    public ImageOR() {
        pages = new ArrayList<>();
    }

    public ImageOR(String name) {
        this.name = name;
        this.type = "OR";
        pages = new ArrayList<>();
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
    public List<ImageORPage> getPages() {
        return pages;
    }

    @Override
    public void setPages(List<ImageORPage> pages) {
        this.pages = pages;
        for (ImageORPage page : pages) {
            page.setRoot(this);
        }
    }

    @Override
    public ImageORPage getPageByName(String pageName) {
        for (ImageORPage page : pages) {
            if (page.getName().equalsIgnoreCase(pageName)) {
                return page;
            }
        }
        return null;
    }

    @Override
    public ImageORPage addPage() {
        String pName = "IPage";
        int i = 0;
        String pageName;
        do {
            pageName = pName + i++;
        } while (getPageByName(pageName) != null);

        return addPage(pageName);
    }

    @Override
    public ImageORPage addPage(String pageName) {
        if (getPageByName(pageName) == null) {
            ImageORPage page = new ImageORPage(pageName, this);
            pages.add(page);
            new File(page.getRepLocation()).mkdirs();
            setSaved(false);
            return page;
        }
        return null;
    }

    @Override
    public void deletePage(String pageName) {
        ImageORPage page = getPageByName(pageName);
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
        return new TreeNode[]{this};
    }

    @JsonIgnore
    @Override
    public String getRepLocation() {
        return getObjectRepository().getIORRepLocation();
    }

    @JsonIgnore
    @Override
    public void sort() {
        ORUtils.sort(this);
    }
}
