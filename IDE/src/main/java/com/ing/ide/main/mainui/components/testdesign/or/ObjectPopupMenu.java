package com.ing.ide.main.mainui.components.testdesign.or;

import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.datalib.or.common.ORRootInf;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.sap.SapOR;
import com.ing.datalib.or.structureddata.StructuredDataOR;
import com.ing.datalib.or.web.WebOR;
import com.ing.ide.main.mainui.components.testdesign.or.clipboard.ORClipboardManager;
import com.ing.ide.main.mainui.components.testdesign.or.clipboard.ORObjectClipboard;
import com.ing.ide.main.utils.keys.Keystroke;
import com.ing.ide.util.Canvas;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

/**
 * Context (right-click) popup menu for Object Repository (OR) tree nodes in the Test Design UI.
 * <p>
 * This menu provides OR maintenance actions such as adding/renaming/deleting pages, object groups,
 * and objects, plus utilities like removing unused objects, copying items to Shared OR, opening page dumps,
 * and running impact analysis. It also includes standard clipboard operations (cut/copy/paste) using
 * Swing transfer actions.
 * </p>
 *
 * <p>
 * The available actions are dynamically enabled/disabled based on the type of the current selection
 * (root, page, group, or object) and whether the selected item belongs to a Shared repository
 * (e.g., disabling actions that should not modify shared content).
 * </p>
 */
public class ObjectPopupMenu extends JPopupMenu {
    private JMenuItem addPage;
    private JMenuItem renamePage;
    private JMenuItem deletePage;
    private JMenuItem renameObjectGroup;
    private JMenuItem deleteObjectGroup;
    private JMenuItem addObject;
    private JMenuItem renameObject;
    private JMenuItem deleteObject;
    private JMenuItem removeUnusedObject;
    private JMenuItem moveToShared;

    private JMenuItem openPageDump;

    private JMenuItem impactAnalysis;

    private JMenuItem copy;
    private JMenuItem cut;
    private JMenuItem paste;
    private JMenuItem sort;

    private final ActionListener listener;

    private Object currentSelection;

    public ObjectPopupMenu(ActionListener listener) {
        this.listener = listener;
        init();
    }

    private void init() {
        add(addPage = create("Add Page", Keystroke.NEW));
        add(renamePage = create("Rename Page", Keystroke.RENAME));
        add(deletePage = create("Delete Page", Keystroke.DELETE));
        addSeparator();

        add(renameObjectGroup = create("Rename Object Group", Keystroke.RENAME));
        add(deleteObjectGroup = create("Delete Object Group", Keystroke.DELETE));
        addSeparator();

        add(addObject = create("Add Object", Keystroke.NEW));
        add(renameObject = create("Rename Object", Keystroke.RENAME));
        add(deleteObject = create("Delete Object", Keystroke.DELETE));
        add(removeUnusedObject = create("Remove Unused Object", Keystroke.REMOVE_OBJECT));
        addSeparator();

        moveToShared = create("Move to Shared", null);
        moveToShared.setActionCommand("Move to Shared");
        moveToShared.addActionListener(listener);
        add(moveToShared);
        add(openPageDump = create("Open Page Dump", null));
        add(impactAnalysis = create("Get Impacted TestCases", null));
        addSeparator();

        setCCP();
        addSeparator();

        add(sort = create("Sort", null));
        sort.setIcon(Canvas.EmptyIcon);
    }

    public void togglePopupMenu(Object selected) {
        this.currentSelection = selected;
        copy.setEnabled(false);
        cut.setEnabled(false);
        paste.setEnabled(
            (currentSelection instanceof ORPageInf || currentSelection instanceof ORObjectInf) &&
            ORClipboardManager.hasData()
        );

        if (selected instanceof ORRootInf) {
            forRoot();
            return;
        } else if (selected instanceof ORPageInf) {
            forPage();
        } else if (selected instanceof ORObjectInf) {
            forObject();
        }
        moveToShared.setEnabled(!isSharedSelection(currentSelection));
    }

    private void forPage() {
        addPage.setEnabled(false);
        renamePage.setEnabled(true);
        deletePage.setEnabled(true);

        renameObjectGroup.setEnabled(false);
        deleteObjectGroup.setEnabled(false);

        addObject.setEnabled(true);
        renameObject.setEnabled(false);
        deleteObject.setEnabled(false);
        removeUnusedObject.setEnabled(!isSharedSelection(currentSelection));

        impactAnalysis.setEnabled(false);

        copy.setEnabled(true);
        cut.setEnabled(isSameOR(currentSelection));
        paste.setEnabled(true);

        sort.setEnabled(true);
    }

    private void forObjectGroup() {
        addPage.setEnabled(false);
        renamePage.setEnabled(false);
        deletePage.setEnabled(false);

        renameObjectGroup.setEnabled(true);
        deleteObjectGroup.setEnabled(true);

        addObject.setEnabled(true);
        renameObject.setEnabled(false);
        deleteObject.setEnabled(false);
        removeUnusedObject.setEnabled(false);

        impactAnalysis.setEnabled(true);

        copy.setEnabled(true);
        cut.setEnabled(isSameOR(currentSelection));
        paste.setEnabled(true);

        sort.setEnabled(true);
    }

    private void forObject() {
        addPage.setEnabled(false);
        renamePage.setEnabled(false);
        deletePage.setEnabled(false);

        renameObjectGroup.setEnabled(false);
        deleteObjectGroup.setEnabled(false);

        addObject.setEnabled(false);
        renameObject.setEnabled(true);
        deleteObject.setEnabled(true);
        removeUnusedObject.setEnabled(false);

        impactAnalysis.setEnabled(true);

        copy.setEnabled(true);
        cut.setEnabled(isSameOR(currentSelection));
        paste.setEnabled(true);

        sort.setEnabled(false);
    }

    private void forRoot() {
        boolean hasClipboard = ORClipboardManager.hasData();

        addPage.setEnabled(true);
        renamePage.setEnabled(false);
        deletePage.setEnabled(false);

        renameObjectGroup.setEnabled(false);
        deleteObjectGroup.setEnabled(false);

        addObject.setEnabled(false);
        renameObject.setEnabled(false);
        deleteObject.setEnabled(false);
        removeUnusedObject.setEnabled(false);

        impactAnalysis.setEnabled(false);

        cut.setEnabled(isSameOR(currentSelection));
        paste.setEnabled(
            hasClipboard && ORClipboardManager.get().getType() == ORObjectClipboard.Type.PAGE
        );
        sort.setEnabled(true);
    }

    private JMenuItem create(String name, KeyStroke keyStroke) {
        JMenuItem menuItem = new JMenuItem(name);
        menuItem.setActionCommand(name);
        menuItem.setAccelerator(keyStroke);
        menuItem.addActionListener(listener);
        return menuItem;
    }

    private void setCCP() {
        cut = new JMenuItem("Cut");
        cut.setActionCommand("Cut"); // for Pages
        cut.setAccelerator(Keystroke.CUT);
        cut.setMnemonic(KeyEvent.VK_T);
        cut.addActionListener(listener);
        add(cut);

        copy = new JMenuItem("Copy");
        copy.setActionCommand("Copy"); // for Pages
        copy.setAccelerator(Keystroke.COPY);
        copy.setMnemonic(KeyEvent.VK_C);
        copy.addActionListener(listener);
        add(copy);

        paste = new JMenuItem("Paste");
        paste.setActionCommand("Paste"); // for Pages
        paste.setAccelerator(Keystroke.PASTE);
        paste.setMnemonic(KeyEvent.VK_P);
        paste.addActionListener(listener);
        add(paste);
    }

    private boolean isSharedSelection(Object selected) {
        ORPageInf page = null;
        if (selected instanceof ORPageInf) {
            page = (ORPageInf) selected;
        } else if (selected instanceof ORObjectInf) {
            page = ((ORObjectInf) selected).getPage();
        }

        if (page != null && page.getRoot() instanceof WebOR) {
            WebOR root = (WebOR) page.getRoot();
            return root.isShared();
        }
        if (page != null && page.getRoot() instanceof MobileOR) {
            MobileOR root = (MobileOR) page.getRoot();
            return root.isShared();
        }
        if (page != null && page.getRoot() instanceof StructuredDataOR) {
            StructuredDataOR root = (StructuredDataOR) page.getRoot();
            return root.isShared();
        }
        if (page != null && page.getRoot() instanceof SapOR) {
            SapOR root = (SapOR) page.getRoot();
            return root.isShared();
        }
        return false;
    }

    private boolean isSameOR(Object selected) {
        if (selected instanceof ORPageInf) {
            return ((ORPageInf) selected).getParent() == getCurrentRoot();
        }
        if (selected instanceof ORObjectInf) {
            return ((ORObjectInf) selected).getPage().getParent() == getCurrentRoot();
        }
        return false;
    }

    private ORRootInf getCurrentRoot() {
        if (currentSelection instanceof ORRootInf) {
            return (ORRootInf) currentSelection;
        }
        if (currentSelection instanceof ORPageInf) {
            return (ORRootInf) ((ORPageInf) currentSelection).getParent();
        }
        if (currentSelection instanceof ORObjectInf) {
            return (ORRootInf) ((ORObjectInf) currentSelection).getPage().getParent();
        }
        return null;
    }
}
