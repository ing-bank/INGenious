
package com.ing.ide.main.mainui.components.testdesign.or;

import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.datalib.or.common.ORRootInf;
import com.ing.ide.main.mainui.components.testdesign.or.clipboard.ORClipboardManager;
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
    private JMenuItem addObject;
    private JMenuItem renameObject;
    private JMenuItem deleteObject;
    private JMenuItem removeUnusedObject;
    private JMenuItem copyToShared;

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
        add(addObject = create("Add Object", Keystroke.NEW));
        add(renameObject = create("Rename Object", Keystroke.RENAME));
        add(deleteObject = create("Delete Object", Keystroke.DELETE));
        add(removeUnusedObject = create("Remove Unused Object",Keystroke.REMOVE_OBJECT));
        addSeparator();
        copyToShared = create("Copy to Shared", null);
        add(copyToShared);
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
        paste.setEnabled((currentSelection instanceof ORPageInf || currentSelection instanceof ORObjectInf)&& ORClipboardManager.hasData());

        if (selected instanceof ORRootInf) {
            forRoot();
            return;
        } else if (selected instanceof ORPageInf) {
            forPage();
        } else if (selected instanceof ORObjectInf) {
            forObject();
        }
        copyToShared.setEnabled(!isSharedSelection(selected));
        removeUnusedObject.setEnabled(!isSharedSelection(selected));
    }

    private void forPage() {
        renamePage.setEnabled(true);
        deletePage.setEnabled(true);

        addPage.setEnabled(false);

        addObject.setEnabled(true);
        renameObject.setEnabled(false);
        deleteObject.setEnabled(false);
        removeUnusedObject.setEnabled(true);
        
        impactAnalysis.setEnabled(false);

        sort.setEnabled(true);
    }

    private void forObject() {
        addPage.setEnabled(false);
        renamePage.setEnabled(false);
        deletePage.setEnabled(false);

        addObject.setEnabled(false);
        renameObject.setEnabled(true);
        deleteObject.setEnabled(true);

        impactAnalysis.setEnabled(true);

        copy.setEnabled(true);
        cut.setEnabled(!isSharedSelection(currentSelection));
        paste.setEnabled(true);

        sort.setEnabled(false);
    }

    private void forRoot() {
        addPage.setEnabled(true);
        removeUnusedObject.setEnabled(false);

        renamePage.setEnabled(false);
        deletePage.setEnabled(false);

        addObject.setEnabled(false);
        renameObject.setEnabled(false);
        deleteObject.setEnabled(false);

        impactAnalysis.setEnabled(false);

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
        cut.setAccelerator(Keystroke.CUT);
        cut.setMnemonic(KeyEvent.VK_T);
        cut.addActionListener(e -> handleCut());
        add(cut);

        copy = new JMenuItem("Copy");
        copy.setAccelerator(Keystroke.COPY);
        copy.setMnemonic(KeyEvent.VK_C);
        copy.addActionListener(e -> handleCopy());
        add(copy);

        paste = new JMenuItem("Paste");
        paste.setAccelerator(Keystroke.PASTE);
        paste.setMnemonic(KeyEvent.VK_P);
        paste.setActionCommand("Paste Object");
        paste.addActionListener(listener);
        add(paste);
    }

    private void handleCopy() {
        if (!(currentSelection instanceof ORObjectInf)) {
            return;
        }
        ORObjectInf object = (ORObjectInf) currentSelection;
        ORClipboardManager.copy(object);
    }

    private void handleCut() {
        if (!(currentSelection instanceof ORObjectInf)) {
            return;
        }
        if (isSharedSelection(currentSelection)) {
            return;
        }
        ORObjectInf object = (ORObjectInf) currentSelection;
        ORClipboardManager.cut(object);
    }

    private boolean isSharedSelection(Object selected) {
        ORPageInf page = null;
        if (selected instanceof ORPageInf) {
            page = (ORPageInf) selected;
        } else if (selected instanceof ORObjectInf) {
            page = ((ORObjectInf) selected).getPage();
        }
        
        if (page != null && page.getRoot() instanceof com.ing.datalib.or.web.WebOR) {
            com.ing.datalib.or.web.WebOR root = (com.ing.datalib.or.web.WebOR) page.getRoot();
            return root.isShared();
        }
        if (page != null && page.getRoot() instanceof com.ing.datalib.or.mobile.MobileOR) {
            com.ing.datalib.or.mobile.MobileOR root = (com.ing.datalib.or.mobile.MobileOR) page.getRoot();
            return root.isShared();
        }
        return false;
    }
}