
package com.ing.ide.main.mainui.components.testdesign.or;

import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.datalib.or.common.ORRootInf;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.ide.main.utils.dnd.TransferableNode;
import com.ing.ide.main.utils.fileoperation.FileOptions;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import static javax.swing.TransferHandler.COPY_OR_MOVE;
import static javax.swing.TransferHandler.MOVE;
import javax.swing.TransferHandler.TransferSupport;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * 
 */
public class ObjectDnD extends TransferHandler {

    public static final DataFlavor OBJECT_FLAVOR = new DataFlavor(ObjectRepDnD.class, ObjectRepDnD.class.getSimpleName());

    private final JTree tree;

    private Boolean isCut = false;

    public ObjectDnD(JTree tree) {
        this.tree = tree;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    private ORRootInf getRoot() {
        return (ORRootInf) tree.getModel().getRoot();
    }

    @Override
    protected Transferable createTransferable(JComponent source) {
        TreePath[] paths = ((JTree) source).getSelectionPaths();
        if (paths != null && paths.length > 0) {
            List<ORPageInf> pages = new ArrayList<>();
            List<ObjectGroup> groups = new ArrayList<>();
            List<ORObjectInf> objects = new ArrayList<>();
            for (TreePath path : paths) {
                Object selected = path.getLastPathComponent();
                if (selected instanceof ORPageInf) {
                    pages.add((ORPageInf) selected);
                } /*else if (selected instanceof ObjectGroup) {
                    groups.add((ObjectGroup) selected);
                }*/ else if (selected instanceof ORObjectInf) {
                    objects.add((ORObjectInf) selected);
                }
            }
            if (!pages.isEmpty()) {
                return new TransferableNode(new ObjectRepDnD().withPages(pages), OBJECT_FLAVOR);
            }
            if (!groups.isEmpty()) {
                return new TransferableNode(new ObjectRepDnD().withObjectGroups(groups), OBJECT_FLAVOR);
            }
            if (!objects.isEmpty()) {
                return new TransferableNode(new ObjectRepDnD().withObjects(objects), OBJECT_FLAVOR);
            }
        }
        return null;
    }

    @Override
    public boolean canImport(TransferSupport ts) {
        return ts.getComponent().equals(tree)
                && ts.isDataFlavorSupported(OBJECT_FLAVOR)
                && getDestinationObject(ts) != null
                && !(getDestinationObject(ts) instanceof ORRootInf);
    }

    @Override
public boolean importData(TransferSupport ts) {
        /**********************************Commented out to disable accidental Object grouping ********************/
        if (canImport(ts)) {
            ObjectRepDnD oDnd;
            try {
                oDnd = (ObjectRepDnD) ts.getTransferable().getTransferData(OBJECT_FLAVOR);
            } catch (UnsupportedFlavorException | IOException ex) {
                Logger.getLogger(ObjectDnD.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            Object object = getDestinationObject(ts);
            Boolean shouldCut = ts.isDrop() ? ts.getDropAction() == MOVE : isCut;
//            if (object instanceof ORPageInf) {
//                if (oDnd.isGroup()) {
//                    copyObjectGroups((ORPageInf) object, oDnd, shouldCut);
//                    return true;
//                } else if (oDnd.isObject()) {
                    copyObjects((ORPageInf) object, oDnd, shouldCut);
                    return true;
//                }
//            } /*else if (object instanceof ObjectGroup) {
//                if (oDnd.isObject()) {
//                    copyObjects((ObjectGroup) object, oDnd, shouldCut);
//                    return true;
//                }
//            } else if (object instanceof ORObjectInf) {
//                if (oDnd.isObject()) {
//                    if (copyObjects(((ORObjectInf) object).getParent(), oDnd, shouldCut)) {
//                        if (((ORObjectInf) object).getParent().getChildCount() == 2) {
//                            reload(((ORObjectInf) object).getParent().getParent());
//                        }
//                    }
//                    return true;
//                }
//            }
//        }
//            }
        }
        return false;
    }

    private Object getDestinationObject(TransferSupport ts) {
        TreePath path;
        if (ts.isDrop()) {
            path = ((JTree.DropLocation) ts.getDropLocation()).getPath();
        } else {
            path = ((JTree) ts.getComponent()).getSelectionPath();
        }
        if (path != null) {
            return path.getLastPathComponent();
        }
        return null;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        isCut = action == MOVE;
//        super.exportDone(source, data, action);
    }

    private void copyObjects(ORPageInf page, ObjectRepDnD oDnd, Boolean shouldCut) {
        for (String value : oDnd.getValues()) {
            if (value.contains("###")) {
                String[] objVals = value.split("###");
                if (objVals.length == 3) {
                    ObjectGroup parent = getRoot().getPageByName(objVals[2])
                            .getObjectGroupByName(objVals[1]);
                    ORObjectInf obj = parent.getObjectByName(objVals[0]);

                    if (shouldCut) {
                        if (obj.getPage().equals(page)
                                && parent.getObjects().size() == 1) {
                            continue;
                        }
                    }

                    ORObjectInf newObj = page.addObject(getObjectName(obj.getName(), page));
                    if (newObj != null) {
                        obj.clone(newObj);
                        objectAdded(newObj);
                        if (shouldCut) {
                            FileOptions.moveDirectory(obj.getRepLocation(), newObj.getRepLocation());
                            objectRemoved(obj);
                            refactorCObject(parent, newObj.getParent());
                            obj.removeFromParent();
                        } else {
                            FileOptions.copyDirectory(obj.getRepLocation(), newObj.getRepLocation());
                        }
                    }
                }
            }
        }
    }

    private Boolean copyObjects(ObjectGroup group, ObjectRepDnD oDnd, Boolean shouldCut) {
        for (String value : oDnd.getValues()) {
            if (value.contains("###")) {
                String[] objVals = value.split("###");
                if (objVals.length == 3) {
                    ObjectGroup parent = getRoot().getPageByName(objVals[2])
                            .getObjectGroupByName(objVals[1]);
                    ORObjectInf obj = parent.getObjectByName(objVals[0]);

                    if (shouldCut) {
                        if (group.getObjects().contains(obj)) {
                            continue;
                        }
                    }

                    ORObjectInf newObj = group.addObject(
                            getObjectName(obj.getName(), group));
                    if (newObj != null) {
                        obj.clone(newObj);
                        objectAdded(newObj);
                        if (shouldCut) {
                            FileOptions.moveDirectory(obj.getRepLocation(), newObj.getRepLocation());
                            objectRemoved(obj);
                            refactorCObject(parent, newObj.getParent());
                            obj.removeFromParent();
                        } else {
                            FileOptions.copyDirectory(obj.getRepLocation(), newObj.getRepLocation());
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String getObjectName(String name, ObjectGroup group) {
        String newName = name;
        int i = 1;
        while (group.getObjectByName(newName) != null) {
            newName = name + " Copy(" + i++ + ")";
        }
        return newName;
    }

    private String getObjectName(String name, ORPageInf page) {
        String newName = name;
        int i = 1;
        while (page.getObjectGroupByName(newName) != null) {
            newName = name + " Copy(" + i++ + ")";
        }
        return newName;
    }

    private void copyObjectGroups(ORPageInf page, ObjectRepDnD oDnd, Boolean shouldCut) {
        for (String value : oDnd.getValues()) {
            if (value.contains("###")) {
                String[] objVals = value.split("###");
                if (objVals.length == 2) {
                    ORPageInf parent = getRoot().getPageByName(objVals[1]);
                    ObjectGroup obj = parent.getObjectGroupByName(objVals[0]);

                    if (shouldCut) {
                        if (page.getObjectGroups().contains(obj)) {
                            continue;
                        }
                    }

                    ObjectGroup newObj = page.addObjectGroup(getObjectName(obj.getName(), page));
                    if (newObj != null) {
                        obj.clone(newObj);
                        nodeAdded(newObj);
                        if (shouldCut) {
                            FileOptions.moveDirectory(obj.getRepLocation(), newObj.getRepLocation());
                            nodeRemoved(obj);
                            refactorObject(obj, newObj);
                            obj.removeFromParent();
                        } else {
                            FileOptions.copyDirectory(obj.getRepLocation(), newObj.getRepLocation());
                        }
                    }
                }
            }
        }
    }

    private void objectAdded(ORObjectInf object) {
        TreeNode parent;
        if (object.getParent().getChildCount() == 1) {
            parent = object.getPage();
        } else {
            parent = object.getParent();
        }
        ((DefaultTreeModel) tree.getModel()).nodesWereInserted(parent, new int[]{parent.getChildCount() - 1});
    }

    private void reload(ORPageInf page) {
        ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(page);
    }

    private void objectRemoved(ORObjectInf object) {
        TreeNode parent;
        TreeNode child;
        if (object.getParent().getChildCount() == 1) {
            parent = object.getPage();
            child = object.getParent();
        } else {
            parent = object.getParent();
            child = object;
        }
        ((DefaultTreeModel) tree.getModel()).nodesWereRemoved(parent, new int[]{parent.getIndex(child)}, new Object[]{child});
    }

    private void nodeAdded(TreeNode node) {
        ((DefaultTreeModel) tree.getModel()).nodesWereInserted(node.getParent(), new int[]{node.getParent().getChildCount() - 1});
    }

    private void nodeRemoved(TreeNode node) {
        ((DefaultTreeModel) tree.getModel()).nodesWereRemoved(node.getParent(), new int[]{node.getParent().getIndex(node)}, new Object[]{node});
    }

    private void refactorCObject(ObjectGroup fromObject, ObjectGroup toObject) {
        if (fromObject.getChildCount() == 1) {
            getRoot().getObjectRepository().getsProject()
                    .refactorObjectName(fromObject.getParent().getName(), fromObject.getName(), toObject.getParent().getName(), toObject.getName());
        }
    }

    private void refactorObject(ObjectGroup fromObject, ObjectGroup toObject) {
        getRoot().getObjectRepository().getsProject()
                .refactorObjectName(fromObject.getParent().getName(), fromObject.getName(), toObject.getParent().getName(), toObject.getName());
    }

}
