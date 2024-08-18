
package com.ing.ide.main.utils;

import static com.ing.ide.main.utils.Utils.getFavIcon;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.function.Consumer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

public class INGeniousFileChooser extends JFileChooser implements PropertyChangeListener {

    public static final FileFilter PROJECT_SELECTOR = new FileFilter() {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                File[] files = f.listFiles((File file, String string) -> string.endsWith(".project"));
                return files != null && files.length > 0;
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Automation Project";
        }
    };
    public static final FileFilter PROJECT_FILTER = new FileNameExtensionFilter("Automation Project", "project");

    public enum Selection {
        MULTI, SINGLE
    }

    public enum SelectionMode {
        FILE, DIR, BOTH

    }

    private final ImageIcon ICON_14;

    public enum Dir {

        PROJECTS(new File(System.getProperty("user.dir"), "Projects"));

        private final File f;

        Dir(File f) {
            this.f = f;
        }

        public File value() {
            return f;
        }
    }

    public static final INGeniousFileChooser OPEN_PROJECT = new INGeniousFileChooser(Utils.FAVICON,
            "Open Project", SelectionMode.DIR, Dir.PROJECTS.value(),
            PROJECT_SELECTOR, PROJECT_FILTER);

    private final FileFilter selector;

    public Consumer<String> afterFileSelected;

    public INGeniousFileChooser(Image i, String title, SelectionMode selMode, File startIn, FileFilter selector) {
        super(startIn);
        ICON_14 = new ImageIcon(i.getScaledInstance(13, 13, 4));
        this.selector = selector;
        setDialogTitle(title);
        setMultiSelectionEnabled(false);
        setFileSelectionMode(selMode.ordinal());
        setAcceptAllFileFilterUsed(false);
        if (JFileChooser.FILES_ONLY != selMode.ordinal()) {
            addPropertyChangeListener((PropertyChangeListener) this);
        }
    }

    public INGeniousFileChooser(Image i, String title, SelectionMode selMode, File startIn, FileFilter selector,
            FileFilter fltr) {
        this(i, title, selMode, startIn, selector);
        setFileFilter(fltr);
    }

    public INGeniousFileChooser(Image i, String title, SelectionMode selMode,
            File startIn, FileFilter selector,
            FileFilter fltr, Selection selType) {
        this(i, title, selMode, startIn, selector, fltr);
        setMultiSelectionEnabled(selType.equals(Selection.MULTI));

    }

    @Override
    protected JDialog createDialog(Component parent) throws HeadlessException {
        JDialog dialog = super.createDialog(parent);
        dialog.setIconImage(getFavIcon());
        return dialog;
    }

    public boolean approve(File f) {
        return selector != null && selector.accept(f);
    }

    @Override
    public Icon getIcon(File f) {
        if (approve(f)) {
            return ICON_14;
        } else {
            return FileSystemView.getFileSystemView().getSystemIcon(f);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(evt.getPropertyName())) {
            File proj = (File) evt.getNewValue();
            if (approve(proj)) {
                setSelectedFile(proj);
                approveSelection();
                setCurrentDirectory(proj.getParentFile());
            }
        }
    }

    @Override
    public void approveSelection() {
        if (approve(getSelectedFile())) {
            super.approveSelection();
            if (afterFileSelected != null) {
                afterFileSelected.accept(getSelectedFile().getAbsolutePath());
            }
        }
    }
}
