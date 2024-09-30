
package com.ing.ide.main.mainui.components.testdesign.or;

import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.datalib.or.common.ORRootInf;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.ide.main.utils.dnd.TransferActionListener;
import com.ing.ide.main.utils.keys.Keystroke;
import com.ing.ide.util.Canvas;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;

/**
 *
 * 
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

    private JMenuItem openPageDump;

    private JMenuItem impactAnalysis;

    private JMenuItem copy;
    private JMenuItem cut;
    private JMenuItem paste;
    private JMenuItem sort;

    private final ActionListener listener;

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
        add(removeUnusedObject = create("Remove Unused Object",Keystroke.REMOVE_OBJECT));
        addSeparator();

        
        add(openPageDump = create("Open Page Dump", null));
        add(impactAnalysis = create("Get Impacted TestCases", null));

        addSeparator();

        setCCP();
        addSeparator();
        add(sort = create("Sort", null));
        sort.setIcon(Canvas.EmptyIcon);
    }

    public void togglePopupMenu(Object selected) {
        if (selected instanceof ORRootInf) {
            forRoot();
        } else if (selected instanceof ORPageInf) {
            forPage();
        } else if (selected instanceof ObjectGroup) {
            forObjectGroup();
        } else if (selected instanceof ORObjectInf) {
            forObject();
        }
    }

    private void forPage() {
        renamePage.setEnabled(true);
        deletePage.setEnabled(true);

        addPage.setEnabled(false);
        renameObjectGroup.setEnabled(false);
        deleteObjectGroup.setEnabled(false);

        addObject.setEnabled(true);
        renameObject.setEnabled(false);
        deleteObject.setEnabled(false);
        removeUnusedObject.setEnabled(true);
        
        impactAnalysis.setEnabled(false);

        copy.setEnabled(true);
        cut.setEnabled(true);
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

        impactAnalysis.setEnabled(true);

        copy.setEnabled(true);
        cut.setEnabled(true);
        paste.setEnabled(true);

        sort.setEnabled(false);
    }

    private void forObject() {
        addPage.setEnabled(false);
        renamePage.setEnabled(false);
        deletePage.setEnabled(false);

        renameObjectGroup.setEnabled(false);
        deleteObjectGroup.setEnabled(false);

        addObject.setEnabled(true);
        renameObject.setEnabled(true);
        deleteObject.setEnabled(true);

        impactAnalysis.setEnabled(true);

        copy.setEnabled(true);
        cut.setEnabled(true);
        paste.setEnabled(true);

        sort.setEnabled(false);
    }

    private void forRoot() {
        addPage.setEnabled(true);
        removeUnusedObject.setEnabled(false);

        renamePage.setEnabled(false);
        deletePage.setEnabled(false);

        renameObjectGroup.setEnabled(false);
        deleteObjectGroup.setEnabled(false);

        addObject.setEnabled(false);
        renameObject.setEnabled(false);
        deleteObject.setEnabled(false);

        impactAnalysis.setEnabled(false);

        copy.setEnabled(false);
        cut.setEnabled(false);
        paste.setEnabled(false);

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
        TransferActionListener actionListener = new TransferActionListener();
        cut = new JMenuItem("Cut");
        cut.setActionCommand((String) TransferHandler.getCutAction().getValue(Action.NAME));
        cut.addActionListener(actionListener);
        cut.setAccelerator(Keystroke.CUT);
        cut.setMnemonic(KeyEvent.VK_T);
        add(cut);

        copy = new JMenuItem("Copy");
        copy.setActionCommand((String) TransferHandler.getCopyAction().getValue(Action.NAME));
        copy.addActionListener(actionListener);
        copy.setAccelerator(Keystroke.COPY);
        copy.setMnemonic(KeyEvent.VK_C);
        add(copy);

        paste = new JMenuItem("Paste");
        paste.setActionCommand((String) TransferHandler.getPasteAction().getValue(Action.NAME));
        paste.addActionListener(actionListener);
        paste.setAccelerator(Keystroke.PASTE);
        paste.setMnemonic(KeyEvent.VK_P);
        add(paste);
    }

}
