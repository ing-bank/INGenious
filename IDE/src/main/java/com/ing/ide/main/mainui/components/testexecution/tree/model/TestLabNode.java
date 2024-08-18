
package com.ing.ide.main.mainui.components.testexecution.tree.model;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Release;
import com.ing.datalib.component.TestSet;
import com.ing.ide.main.utils.tree.CommonNode;
import java.util.Collections;
import javax.swing.tree.TreeNode;

/**
 *
 * 
 */
public class TestLabNode extends CommonNode {

    Project project;

    public void setProject(Project project) {
        removeAllChildren();
        this.project = project;
        filterTestSets();
    }

    private void filterTestSets() {
        for (Release scenario : project.getReleases()) {
            for (TestSet testSet : scenario.getTestSets()) {
                addReleaseIfNotPresent(testSet.getRelease()).addTestSetIfNotPresent(testSet);
            }
        }
    }

    public ReleaseNode addReleaseIfNotPresent(Release release) {
        addRelease(release);
        return getReleaseBy(release);
    }

    public ReleaseNode addRelease(Release release) {
        if (getReleaseBy(release) == null) {
            ReleaseNode node = new ReleaseNode(release);
            add(node);
            return node;
        }
        return null;
    }

    public ReleaseNode getReleaseBy(Release release) {
        for (TreeNode releaseNode : Collections.list(children())) {
            if (((ReleaseNode)releaseNode).getRelease().equals(release)) {
                return (ReleaseNode)releaseNode;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return project != null ? project.getName() : "Reusable";
    }

}
