package com.ing.ide.main.mainui.components.testdesign.tree.model;

/**
 * Intermediate grouping node in the Test Plan tree. Groups bundle a set of
 * scenarios under a user-defined name (e.g. "Payment Initiation") and are
 * persisted to {@code TestPlan/.groups}.
 * <p>
 * A single implicit {@code (Ungrouped)} node (marked via {@link #isUngrouped()})
 * always holds scenarios that do not belong to any named group. It is always
 * shown last and cannot be renamed or deleted.
 * </p>
 */
public class TestPlanGroupNode extends GroupNode {
    /** Display name of the implicit catch-all group. */
    public static final String UNGROUPED = "(Ungrouped)";

    private boolean ungrouped;

    public TestPlanGroupNode(String name, boolean ungrouped) {
        super(name);
        this.ungrouped = ungrouped;
    }

    /**
     * @return {@code true} when this is the implicit catch-all node for scenarios
     *         not assigned to any named group.
     */
    public boolean isUngrouped() {
        return ungrouped;
    }

    /**
     * Marks whether this is the implicit catch-all node. Used when promoting the
     * {@code (Ungrouped)} node into a real named group.
     * @param ungrouped new flag value
     */
    public void setUngrouped(boolean ungrouped) {
        this.ungrouped = ungrouped;
    }

    /**
     * Renames this group, rejecting the change when a sibling group already uses
     * the requested name.
     * @param name new group name
     * @return {@code true} if the rename was applied
     */
    @Override
    public boolean rename(String name) {
        if (getParent() instanceof TestPlanNode) {
            TestPlanNode root = (TestPlanNode) getParent();
            if (root.getGroupByName(name) == null) {
                setName(name);
                return true;
            }
            return false;
        }
        setName(name);
        return true;
    }
}
