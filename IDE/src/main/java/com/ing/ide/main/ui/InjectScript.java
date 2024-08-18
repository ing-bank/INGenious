
package com.ing.ide.main.ui;

import com.ing.engine.support.methodInf.MethodInfoManager;
import com.ing.ide.main.help.Help;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.util.Canvas;
import com.ing.ide.util.Notification;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.apache.commons.io.FileUtils;

/**
 *
 * 
 */
public class InjectScript extends javax.swing.JFrame {

    private static final Logger LOG = Logger.getLogger(InjectScript.class.getName());
    private static final JavaCompiler JAVA_COMPILER = ToolProvider.getSystemJavaCompiler();

    private String sampleCode;

    private String javacPath;

    private final DefaultListModel javaListModel;

    private final OutputStream consoleOutputStream;

    /**
     * Creates new form InjectScript
     */
    public InjectScript() {
        initComponents();

        setIconImage(((ImageIcon) Utils.getIconByResourceName("/ui/resources/main/InjectScript")).getImage());

        loadSampleScript();

        clearConsole.setIcon(Canvas.EmptyIcon);

        tryToLoadJavac();
        javaListModel = new DefaultListModel();
        javaList.setModel(javaListModel);
        javaList.setTransferHandler(new FileTransferHandler());

        consoleOutputStream = new OutputStream() {
            @Override
            public void write(byte[] buffer, int offset, int length) throws IOException {
                final String text = new String(buffer, offset, length);
                SwingUtilities.invokeLater(() -> {
                    console.append(text);
                });
            }

            @Override
            public void write(int b) throws IOException {
                write(new byte[]{(byte) b}, 0, 1);
            }

        };
        javaList.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DELETE"), "Delete");
        javaList.getActionMap().put("Delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                int[] indices = javaList.getSelectedIndices();
                if (indices != null && indices.length > 0) {
                    for (int i = indices.length - 1; i >= 0; i--) {
                        javaListModel.removeElementAt(i);
                    }
                }
            }
        });
    }

    private void loadSampleScript() {
        try {
            File file = new File("Configuration" + File.separator + "SampleScript.java");
            if (file.exists()) {
                sampleCode = FileUtils.readFileToString(file, Charset.defaultCharset());
            } else {
                sampleCode = "";
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private void tryToLoadJavac() {
        if (JAVA_COMPILER == null) {
            javacPath = Utils.getJavacPath();
        }
    }

    public void load() throws IOException {
        if (JAVA_COMPILER == null) {
            if (javacPath == null) {
                Notification.show("JDK is not available in path.Please select a jdk");
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select JDK bin path");
                fileChooser.setSelectedFile(new File(new File(System.getenv("JAVA_HOME")).getCanonicalPath()));
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                File selected = promptFile(fileChooser);
                while (selected != null && !Utils.getJavaCFilter().accept(selected)) {
                    Notification.show("Please select a valid jdk/bin");
                    selected = promptFile(fileChooser);
                }
                if (selected != null) {
                    javacPath = selected.getAbsolutePath() + File.separator + "javac";
                } else {
                    Notification.show("Compile java file/s manually and "
                            + "place class file/s inside [app.root]/userdefined");
                    return;
                }
            }
        }
        console.setText("Add Files by using load button or Drag and Drop java files to the box above\n"
                + "Remove files by using DELETE key");
        setSize(500, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private File promptFile(JFileChooser fileChooser) {
        int val = fileChooser.showOpenDialog(this);
        if (val == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }

    private void addToList(String path) {
        if (!javaListModel.contains(path)) {
            javaListModel.addElement(path);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javaFileChooser = new javax.swing.JFileChooser();
        jPopupMenu1 = new javax.swing.JPopupMenu();
        clearConsole = new javax.swing.JMenuItem();
        jPanel1 = new javax.swing.JPanel();
        loadFiles = new javax.swing.JButton();
        inject = new javax.swing.JButton();
        help = new javax.swing.JButton();
        showSample = new javax.swing.JButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        javaList = new javax.swing.JList<>();
        jScrollPane4 = new javax.swing.JScrollPane();
        console = new javax.swing.JTextArea();

        javaFileChooser.setDialogTitle("Select Java Files");
        javaFileChooser.setFileFilter(new FileNameExtensionFilter("Java Files","java")
        );

        clearConsole.setText("Clear Console");
        clearConsole.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearConsoleActionPerformed(evt);
            }
        });
        jPopupMenu1.add(clearConsole);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Inject Script");

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        loadFiles.setText("Load Files");
        loadFiles.setToolTipText("Add Java Files");
        loadFiles.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadFilesActionPerformed(evt);
            }
        });
        jPanel1.add(loadFiles);

        inject.setText("Inject Script");
        inject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                injectActionPerformed(evt);
            }
        });
        jPanel1.add(inject);

        help.setText("Open Help");
        help.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpActionPerformed(evt);
            }
        });
        jPanel1.add(help);

        showSample.setText("Get Sample");
        showSample.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showSampleActionPerformed(evt);
            }
        });
        jPanel1.add(showSample);

        getContentPane().add(jPanel1, java.awt.BorderLayout.NORTH);

        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setOneTouchExpandable(true);

        jScrollPane1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Java Files", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jScrollPane1.setViewportView(javaList);

        jSplitPane1.setLeftComponent(jScrollPane1);

        jScrollPane4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Console", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        console.setEditable(false);
        console.setBackground(java.awt.Color.black);
        console.setColumns(20);
        console.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
        console.setForeground(java.awt.Color.white);
        console.setRows(5);
        console.setText("Add Files by using load button or Drag and Drop java files to the box above\n");
        console.setComponentPopupMenu(jPopupMenu1);
        jScrollPane4.setViewportView(console);

        jSplitPane1.setRightComponent(jScrollPane4);

        getContentPane().add(jSplitPane1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void injectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_injectActionPerformed
        if (injectScript()) {
            MethodInfoManager.load();
            this.dispose();
        }
    }//GEN-LAST:event_injectActionPerformed

    private void loadFilesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadFilesActionPerformed
        int val = javaFileChooser.showOpenDialog(this);
        if (val == JFileChooser.APPROVE_OPTION) {
            addToList(javaFileChooser.getSelectedFile().getAbsolutePath());
        }
    }//GEN-LAST:event_loadFilesActionPerformed

    private void helpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpActionPerformed
        Help.openHelp();
    }//GEN-LAST:event_helpActionPerformed

    private void showSampleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showSampleActionPerformed
        Utils.copyTextToClipboard(sampleCode);
        JOptionPane.showMessageDialog(this, "Sample code has been copied to clipboard");
    }//GEN-LAST:event_showSampleActionPerformed

    private void clearConsoleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearConsoleActionPerformed
        console.setText("");
    }//GEN-LAST:event_clearConsoleActionPerformed

    private Boolean injectScript() {
        if (javaListModel.isEmpty()) {
            console.append("\n");
            console.append("Please Load some files and then inject");
        } else {
            Boolean flag = true;
            for (Object object : javaListModel.toArray()) {
                console.append("\n");
                if (injectScript(object.toString()) == 0) {
                    console.append(object + " - compiled successfully");
                    try {
                        FileUtils.copyFileToDirectory(new File(object.toString()
                                .replace(".java", ".class")),
                                new File("userdefined"));
                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                } else {
                    console.append(object + " - compiled with errors");
                    flag = false;
                }
                console.append("\n");
            }
            return flag;
        }
        return false;
    }

    private int injectScript(String path) {
        if (JAVA_COMPILER != null) {
            return JAVA_COMPILER.run(null, consoleOutputStream,
                    consoleOutputStream, path);
        }
        return compileClass(javacPath, path);
    }

    private int compileClass(String jdkPath, String file) {
        try {
            String[] cmd = {jdkPath, "-cp", System.getProperty("java.class.path"), file};
            Process pro = Runtime.getRuntime().exec(cmd, null, Utils.getAppRoot());
            printLines(pro.getErrorStream());
            pro.waitFor();
            return pro.exitValue();
        } catch (IOException | InterruptedException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return -1;
    }

    private void printLines(InputStream ins) throws IOException {
        int line;
        BufferedReader in = new BufferedReader(
                new InputStreamReader(ins));
        while ((line = in.read()) != -1) {
            consoleOutputStream.write(line);
        }
    }

    class FileTransferHandler extends TransferHandler {

        private final DataFlavor INDICES = new DataFlavor(int[].class, "Selected Indices");

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            JList list = (JList) c;
            return new JListTransferable(list.getSelectedIndices());
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            if (support.getTransferable().isDataFlavorSupported(INDICES)) {
                return reorderProjects(support);
            } else {
                return dropProjects(support);
            }
        }

        private Boolean reorderProjects(TransferHandler.TransferSupport support) {
            JList list = (JList) support.getComponent();
            try {
                int[] selectedIndices = (int[]) support.getTransferable().getTransferData(INDICES);
                DefaultListModel model = (DefaultListModel) list.getModel();
                JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
                if (dl.getIndex() != -1) {
                    for (int selectedIndex : selectedIndices) {
                        Object value = model.get(selectedIndex);
                        model.removeElement(value);
                        model.add(dl.getIndex(), value);
                    }
                    return true;
                } else {
                    LOG.warning("Invalid Drop Location");
                }
            } catch (UnsupportedFlavorException | IOException ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
            return false;
        }

        private Boolean dropProjects(TransferHandler.TransferSupport support) {
            List files;
            try {
                files = (List) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                for (Object dfile : files) {
                    File file = (File) dfile;
                    if (file.isDirectory()) {
                        if (file.listFiles() != null) {
                            for (File file1 : file.listFiles()) {
                                if (file1.getName().matches(".*\\.java")) {
                                    addToList(file1.getAbsolutePath());
                                }
                            }
                        }
                    } else if (file.getName().matches(".*\\.java")) {
                        addToList(file.getAbsolutePath());
                    }
                }
                return true;
            } catch (UnsupportedFlavorException | IOException ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
            LOG.warning("Invalid files.The files are not Projects");
            return false;
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                    || support.isDataFlavorSupported(INDICES);
        }

        class JListTransferable implements Transferable {

            private final int[] selectedValues;

            public JListTransferable(int[] selectedValues) {
                this.selectedValues = selectedValues;
            }

            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{INDICES};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return INDICES.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                if (flavor.equals(INDICES)) {
                    return selectedValues;
                }
                throw new UnsupportedFlavorException(flavor);
            }

        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem clearConsole;
    private javax.swing.JTextArea console;
    private javax.swing.JButton help;
    private javax.swing.JButton inject;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JFileChooser javaFileChooser;
    private javax.swing.JList<String> javaList;
    private javax.swing.JButton loadFiles;
    private javax.swing.JButton showSample;
    // End of variables declaration//GEN-END:variables
}
