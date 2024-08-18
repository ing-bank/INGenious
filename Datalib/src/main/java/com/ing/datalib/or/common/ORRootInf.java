
package com.ing.datalib.or.common;

import com.ing.datalib.or.ObjectRepository;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import javax.swing.tree.TreeNode;

/**
 *
 * 
 * @param <T>
 */
public interface ORRootInf<T extends ORPageInf> extends TreeNode {

    @JsonIgnore
    public T addPage();

    @JsonIgnore
    public T addPage(String pageName);

    @JsonIgnore
    public void deletePage(String pageName);

    public String getName();

    @JsonIgnore
    public ObjectRepository getObjectRepository();

    @JsonIgnore
    public T getPageByName(String pageName);

    public List<T> getPages();

    @JsonIgnore
    public Boolean isSaved();

    @JsonIgnore
    public void setSaved(Boolean saved);

    public void setName(String name);

    @JsonIgnore
    public void setObjectRepository(ObjectRepository objRep);

    public void setPages(List<T> pages);

    @JsonIgnore
    public TreeNode[] getPath();

    @JsonIgnore
    public String getRepLocation();

    @JsonIgnore
    public void sort();
}
