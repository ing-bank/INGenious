
package com.ing.ide.main.utils;

import com.ing.engine.support.DesktopApi;
import static com.ing.ide.main.utils.INGeniousFileChooser.OPEN_PROJECT;
import com.ing.ide.main.utils.filters.JavaCFilter;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 *
 */
public class Utils {

    public static final Image FAVICON = new ImageIcon(Utils.class.getResource("/ui/resources/favicon.png")).getImage();
    private static final FileFilter JAVAC_FILTER = new JavaCFilter();

    public static List<Integer> getReverseSorted(int[] array) {
        List<Integer> arrayList = new ArrayList();
        if (array != null) {
            for (int val : array) {
                arrayList.add(val);
            }
            Collections.sort(arrayList, Collections.reverseOrder());
        }
        return arrayList;
    }

    public static List<Integer> getSorted(int[] array) {
        List<Integer> arrayList = new ArrayList();
        if (array != null) {
            for (int val : array) {
                arrayList.add(val);
            }
            Collections.sort(arrayList);
        }
        return arrayList;
    }

    public static void openWithSystemEditor(String location) {
        File file = new File(location);
        if (file.exists() && !DesktopApi.open(file)) {
            JOptionPane.showMessageDialog(null, "Couldn't Open Testdata " + file);
        }
    }

    public static Icon getIconByResourceName(String name) {
        // Try INGIcons (web font icon) first — with semantic color
        String iconKey = extractIconKey(name);
        if (iconKey != null) {
            Icon webIcon = com.ing.ide.main.fx.INGIcons.swingColored(iconKey, 16);
            if (webIcon != null) {
                return webIcon;
            }
        }
        // Fallback to PNG resource
        URL url = Utils.class.getResource(name + ".png");
        if (url != null) {
            return new ImageIcon(url);
        }
        return null;
    }

    /**
     * Extracts a logical icon key from a resource path.
     * E.g. "/ui/resources/toolbar/add" → "add",
     *      "/ui/resources/main/NewProject" → "NewProject",
     *      "/ui/resources/testdesign/debug/stepover" → "debug.stepover"
     */
    private static String extractIconKey(String resourcePath) {
        if (resourcePath == null) return null;
        String path = resourcePath.replace('\\', '/');
        // Try direct filename first
        int lastSlash = path.lastIndexOf('/');
        String filename = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        if (com.ing.ide.main.fx.INGIcons.has(filename)) {
            return filename;
        }
        // Try parent.filename pattern (e.g. debug.stepover)
        if (lastSlash > 0) {
            int prevSlash = path.lastIndexOf('/', lastSlash - 1);
            if (prevSlash >= 0) {
                String parent = path.substring(prevSlash + 1, lastSlash);
                String compound = parent + "." + filename;
                if (com.ing.ide.main.fx.INGIcons.has(compound)) {
                    return compound;
                }
            }
        }
        return filename; // Return filename — INGIcons.swing will return null if not mapped
    }

    public static JButton createButton(String action, ActionListener actionlistener) {
        JButton btn = new JButton();
        btn.setActionCommand(action);
        btn.addActionListener(actionlistener);
        return btn;
    }

    public static JButton createButton(String action, String icon, String tooltip, ActionListener actionlistener) {
        JButton btn = new JButton();
        btn.setActionCommand(action);
        btn.setIcon(getIconByResourceName("/ui/resources/toolbar/" + icon));
        if (btn.getIcon() == null) {
            btn.setText(action);
        }
        btn.setToolTipText("<html>" + action + (tooltip != null ? (" [" + tooltip + "]") : "") + "</html>");
        btn.addActionListener(actionlistener);
        return btn;
    }
    
    public static JButton createLRButton(String action, String icon, ActionListener actionlistener) {
        JButton btn = new JButton();
        btn.setActionCommand(action);
        btn.setIcon(getIconByResourceName("/ui/resources/toolbar/" + icon));
        if (btn.getIcon() == null) {
            btn.setText(action);
        }
        btn.addActionListener(actionlistener);
        return btn;
    }

    public static JMenuItem createMenuItem(String action, String tooltip, KeyStroke keyStroke, ActionListener actionlistener) {
        JMenuItem btn = new JMenuItem();
        btn.setActionCommand(action);
        btn.setText(action);
        btn.setToolTipText("<html>" + action + (tooltip != null ? (" [" + tooltip + "]") : "") + "</html>");
        btn.setAccelerator(keyStroke);
        btn.addActionListener(actionlistener);
        btn.setFont(UIManager.getFont("TableMenu.font"));
        return btn;
    }

    public static JMenuItem createMenuItem(String action, ActionListener actionlistener) {
        JMenuItem btn = new JMenuItem();
        btn.setActionCommand(action);
        btn.setText(action);
        btn.addActionListener(actionlistener);
        btn.setFont(UIManager.getFont("TableMenu.font"));
        return btn;
    }

    public static File openINGeniousProject() {
        int option = OPEN_PROJECT.showOpenDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            return OPEN_PROJECT.getSelectedFile();
        }
        return null;
    }

    public static File saveDialog() throws IOException {
        return saveDialog("");
    }

    public static File saveDialog(String fileName) throws IOException {
        JFileChooser fileChooser = createFileChooser();
        fileChooser.setCurrentDirectory(new File(new File(System.getProperty("user.dir")).getCanonicalPath()));
        fileChooser.setSelectedFile(new File(fileName));
        int option = fileChooser.showSaveDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }

    public static File openDialog() throws IOException {
        return openDialog("Open File");
    }

    public static File openDialog(String desc, String... fileFormat) throws IOException {
        JFileChooser fileChooser = createFileChooser();
        fileChooser.setCurrentDirectory(new File(new File(System.getProperty("user.dir")).getCanonicalPath()));
        if (fileFormat != null && fileFormat.length > 0) {
            fileChooser.setFileFilter(new FileNameExtensionFilter(desc, fileFormat));
        }
        int option = fileChooser.showOpenDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }

    public static Image getFavIcon() {
        return AppIcon.getAppIcon();
    }

    private static JFileChooser createFileChooser() {
        JFileChooser jf = new JFileChooser() {
            @Override
            protected JDialog createDialog(Component parent) throws HeadlessException {
                JDialog dialog = super.createDialog(parent);
                AppIcon.applyTo(dialog);
                return dialog;
            }
        };
        return jf;
    }

    public static List<String> asStringList(List<?> list) {
        List<String> arrayList = new ArrayList<>();
        for (Object object : list) {
            arrayList.add(Objects.toString(object, null));
        }
        return arrayList;
    }

    public static void copyTextToClipboard(String text) {
        StringSelection stringSelection = new StringSelection(text);
        Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
        clpbrd.setContents(stringSelection, null);
    }

    public static String getJavacPath() {
        try {
            Process proc = Runtime.getRuntime().exec("where javac");
            proc.waitFor();
            BufferedReader stdInput = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));

            List<String> jdkPaths = new ArrayList<>();
            String s;
            while ((s = stdInput.readLine()) != null) {
                if (!s.trim().isEmpty()
                        && s.contains("1.8")) {
                    jdkPaths.add(s);
                }
            }
            if (!jdkPaths.isEmpty()) {
                return jdkPaths.get(0);

            }
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(Utils.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static FileFilter getJavaCFilter() {
        return JAVAC_FILTER;
    }

    public static File getAppRoot() throws IOException {
        return new File(new File(System.getProperty("user.dir")).getCanonicalPath());
    }
}
