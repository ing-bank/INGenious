package com.ing.ide.main.mainui.components.testdesign.testcase;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import static com.ing.datalib.component.TestStep.HEADERS.Description;
import com.ing.datalib.component.utils.SaveListener;
import com.ing.engine.constants.SystemDefaults;
import com.ing.engine.core.RunManager;
import com.ing.engine.support.methodInf.MethodInfoManager;
import com.ing.ide.main.mainui.EngineConfig;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.playwrightrecording.PlaywrightSpinner;
import com.ing.ide.main.utils.ConsolePanel;
import com.ing.ide.main.utils.MenuScroller;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.keys.Keystroke;
import com.ing.ide.main.utils.table.TableColumnManager;
import com.ing.ide.main.utils.table.XTable;
import com.ing.ide.util.Canvas;
import com.ing.ide.util.Notification;
import com.ing.ide.util.WindowMover;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 *
 *
 */
public class TestCaseComponent extends JPanel implements ActionListener {

    private final TestDesign testDesign;

    private final TestCaseToolBar toolBar;

    private final ConsoleDialog consoleDialog;

    private final DebugDialog debugDialog;

    private final RecorderDialog recorderDialog;

    private final TestCasePopupMenu popupMenu;

    private final TestCaseValidator validator;

    private TestCaseAutoSuggest tcAutoSuggest;

    private final XTable testCaseTable;

    private SaveListener saveListener;

    private Thread runner;

    TableColumnManager tableColumnManager;

    private final TCHistory testCaseHistory;

    public TestCaseComponent(TestDesign testDesign) {
        this.testDesign = testDesign;
        toolBar = new TestCaseToolBar(this);
        popupMenu = new TestCasePopupMenu(this);
        testCaseTable = new XTable();
        tableColumnManager = new TableColumnManager(testCaseTable);
        consoleDialog = new ConsoleDialog();
        debugDialog = new DebugDialog();
        recorderDialog = new RecorderDialog(testDesign);
        testCaseHistory = new TCHistory();
        validator = new TestCaseValidator(testCaseTable);
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(new JScrollPane(testCaseTable), BorderLayout.CENTER);
        testCaseTable.setComponentPopupMenu(popupMenu);
        initTableListeners();
        initRunner();
    }

    public void loadTableModelForSelection(Object obj) {
        if (obj != null && obj instanceof TestCase) {
            testCaseHistory.log();
            TestCase tc = (TestCase) obj;
            tc.setSaveListener(saveListener);
            getTestCaseTable().setModel(testDesign.getProject().getTableModelFor(tc));
            tcAutoSuggest.installForTestCase();
            validator.initValidations();
            changeSave(tc.isSaved());
            refreshTitle();
        }
    }

    public void resetTable() {
        getTestCaseTable().setModel(new DefaultTableModel());
        changeSave(true);
        toolBar.setPlaceHolderText("", null);
    }

    public void refreshTitle() {
        String scText = getCurrentTestCase().getScenario().getName();
        if (scText.length() > 20) {
            scText = scText.substring(0, 20) + "...";
        }
        String tcText = getCurrentTestCase().getName();
        if (tcText.length() > 20) {
            tcText = tcText.substring(0, 20) + "...";
        }
//        String toolTip
//                = getCurrentTestCase().getScenario().getName()
//                + " - "
//                + getCurrentTestCase().getName();
        toolBar.setPlaceHolderText(scText + " - " + tcText, null);
    }

    public void load() {
        tcAutoSuggest = new TestCaseAutoSuggest(testDesign.getProject(), testCaseTable);
        testCaseHistory.clear();
        loadBrowsers();
    }

    public void loadBrowsers() {
        toolBar.loadBrowsers(testDesign.getProject().getProjectSettings().getEmulators().getEmulatorNames());
    }

    private void initTableListeners() {
        testCaseTable.setActionFor("Comment", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleComment();
            }
        });

        testCaseTable.setActionFor("BreakPoint", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleBreakPoint();
            }
        });

        testCaseTable.setActionFor("Insert", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertRow();
            }
        });
        testCaseTable.setActionFor("Add", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addRow();
            }
        });
        testCaseTable.setActionFor("Delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedRows();
            }
        });

        testCaseTable.setActionFor("Clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                clearValues();
            }
        });

        testCaseTable.setActionFor("Replicate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                replicateRow();
            }
        });
        testCaseTable.setActionFor("Save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });
        testCaseTable.setActionFor("Reload", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reload();
            }
        });
        testCaseTable.setActionFor("Open", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openWithSystemEditor();
            }

        });
        testCaseTable.setActionFor("Search", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toolBar.focusSearch();
            }
        });

        testCaseTable.setActionFor("Copy Above", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyAbove();
            }
        });

        testCaseTable.setActionFor("MoveUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveRowUp();
            }
        });
        testCaseTable.setActionFor("MoveDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveRowDown();
            }
        });

        testCaseTable.setKeyStrokeFor("RunTestCase", Keystroke.F6);
        testCaseTable.setActionFor("RunTestCase", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                run();
            }
        });
        testCaseTable.setKeyStrokeFor("DebugTestCase", Keystroke.CTRLF6);
        testCaseTable.setActionFor("DebugTestCase", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                debug();
            }
        });

        saveListener = new SaveListener() {
            @Override
            public void onSave(Boolean bln) {
                changeSave(bln);
            }
        };

        testCaseTable.setTransferHandler(new TestCaseTableDnD());

        testCaseTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent me) {

                if (SwingUtilities.isLeftMouseButton(me) && me.isAltDown()) {
                    goToSelectedReusable();
                } else if (SwingUtilities.isLeftMouseButton(me) && me.isShiftDown()) {
                    goToObject();
                } else if (SwingUtilities.isLeftMouseButton(me)) {
                    addLastRow();
                }
            }

        });
    }

    private void initRunner() {
        runner = new Thread(() -> {
            toolBar.setConsoleVisible(true);
            toolBar.stopMode();
            consoleDialog.start();
            RunManager.getGlobalSettings().setFor(getCurrentTestCase(), toolBar.getSelectedBrowser());
            EngineConfig.runProject(testDesign.getProject());
            debugDialog.setVisible(false);
            toolBar.startMode();
        });
    }

    private void changeSave(Boolean bln) {
        toolBar.setSave(!bln);
        popupMenu.setSave(!bln);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        switch (ae.getActionCommand()) {
            case "Record": {
                try {
                    record();
                } catch (IOException ex) {
                    Logger.getLogger(TestCaseComponent.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            break;

            case "Open with System Editor":
                openWithSystemEditor();
                break;
            case "Add Row":
                insertRowBelow();
                break;
            case "Delete Rows":
                deleteSelectedRows();
                break;
            case "Save":
                save();
                break;
            case "Reload":
                reload();
                break;
            case "Search":
                testCaseTable.searchFor(((JTextField) ae.getSource()).getText());
                break;
            case "GoToNextSearch":
                testCaseTable.goToNextSearch();
                break;
            case "GoToPrevoiusSearch":
                testCaseTable.goToPrevoiusSearch();
                break;
            case "Cut":
            case "Copy":
            case "Paste":
                ccp(ae.getActionCommand());
                break;
            case "Create Reusable":
                createReusable();
                break;
            case "Move Rows Up":
                moveRowUp();
                break;
            case "Move Rows Down":
                moveRowDown();
                break;
            case "Run":
                run();
                break;
            case "Debug":
                debug();
                break;
            case "StopRun":
                stopExecution();
                break;
            case "Toggle BreakPoint":
                toggleBreakPoint();
                break;
            case "Toggle Comment":
                toggleComment();
                break;
            case "Console":
                consoleDialog.showConsole();
                break;
            case "Go To Reusable":
                goToSelectedReusable();
                break;
            case "Go To Object":
                goToObject();
                break;
            case "Go To TestData":
                goToTestData();
                break;
            case "Toggle Validation":
                validator.toggleValidation();
                break;
            case "Paramterize":
                parameterizeSelectedSteps();
                break;
            case "Up One Level":
                loadTableModelForSelection(testCaseHistory.visit());
                break;
            default:
                throw new UnsupportedOperationException(ae.getActionCommand());
        }
    }

    public TestCase getCurrentTestCase() {
        if (getTestCaseTable().getModel() instanceof TestCase) {
            return (TestCase) getTestCaseTable().getModel();
        }
        return null;
    }

    public void record() throws IOException {
        CompletableFuture<Void> launchPlaywright = CompletableFuture.runAsync(() -> {
            try {
                launchPlaywright();
            } catch (IOException ex) {
                Logger.getLogger(TestCaseComponent.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        CompletableFuture<Void> playwrightLoading = CompletableFuture.runAsync(() -> {
            playwrightLoading();
        });
        CompletableFuture<Void> playwright = CompletableFuture.allOf(launchPlaywright, playwrightLoading);
    }

    public Process startPlaywrightProcess(String processName){
        try{
            String[] command = new String[0];
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("windows")) {
                // Windows command
                command = new String[]{"cmd", "/c", "mvn initialize -f engine/pom.xml ^&^& mvn exec:java -f engine/pom.xml -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args=" + processName};
            } else if (osName.contains("mac")) {
                // Mac command
              command = new String[]{"bash", "-l", "-c", "mvn initialize -f engine/pom.xml && mvn exec:java -f engine/pom.xml -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args=" + processName};
            } 
            Process process = Runtime.getRuntime().exec(command);
            return process;
       }catch (Exception ex){
         System.out.println(ex.getMessage());
       }

        return null;
    }
    
     public void initialization(){
        try{
            String[] command = new String[0];
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("windows")) {
                // Windows command
                
                command = new String[]{"cmd", "/c", "mvn initialize -f engine/pom.xml"};
            } else if (osName.contains("mac")) {
                // Mac command
                command = new String[]{"bash", "-l", "-c", "mvn initialize -f engine/pom.xml"};
            } 
           Runtime.getRuntime().exec(command);
       }catch (Exception ex){
         System.out.println(ex.getMessage());
       }
    }

    public void launchPlaywright() throws IOException {
        System.out.println("============================== Playwright Log Started ==============================");
        initialization();
        Process launchRecorder = startPlaywrightProcess("codegen");
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(launchRecorder.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(launchRecorder.getErrorStream()));
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }
        while ((s = stdError.readLine()) != null) {
            System.out.println(s);
            if (s.contains("mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args=\"install\"")) {
                System.out.println("");
                System.out.println("--> mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args=\"install\" --> Got executed");
                Process playwrightInstall = startPlaywrightProcess("install");
                BufferedReader stdInput1 = new BufferedReader(new InputStreamReader(playwrightInstall.getInputStream()));
                BufferedReader stdError1 = new BufferedReader(new InputStreamReader(playwrightInstall.getErrorStream()));
                String s1 = null;
                while ((s1 = stdInput1.readLine()) != null) {
                    System.out.println(s1);
                }
                while ((s1 = stdError1.readLine()) != null) {
                    System.out.println(s1);
                }
                try {
                    playwrightInstall.waitFor();
                } catch (InterruptedException ex) {
                    Logger.getLogger(TestCaseComponent.class.getName()).log(Level.SEVERE, null, ex);
                }
                startPlaywrightProcess("codegen");
                break;
            }
        }
        System.out.println("============================== Playwright Log Ended ==============================");

    }

    public void playwrightLoading() {

        PlaywrightSpinner a = new PlaywrightSpinner();
        a.setAlwaysOnTop(true);
        a.setVisible(true);

    }

    private void stopCellEditing() {
        if (testCaseTable.getCellEditor() != null) {
            testCaseTable.getCellEditor().stopCellEditing();
        }
    }

    private void insertRow() {
        stopCellEditing();
        if (testCaseTable.getSelectedRow() != -1) {
            getCurrentTestCase().addNewStepAt(testCaseTable.getSelectedRow());
        }
    }

    public TestStep getSelectedStep() {
        if (testCaseTable.getSelectedRow() != -1) {
            return getCurrentTestCase().getTestSteps().get(testCaseTable.getSelectedRow());
        }
        if (testCaseTable.getRowCount() > 0) {
            return getCurrentTestCase().getTestSteps().get(testCaseTable.getRowCount() - 1);
        }
        return null;
    }

    public TestStep getLastStep() {
        if (testCaseTable.getRowCount() > 0) {
            return getCurrentTestCase().getTestSteps().get(testCaseTable.getRowCount() - 1);
        }
        return null;
    }

    public TestStep insertRowBelow() {
        stopCellEditing();
        if (testCaseTable.getSelectedRow() != -1
                && testCaseTable.getSelectedRow() + 1 < testCaseTable.getRowCount()) {
            return getCurrentTestCase().addNewStepAt(testCaseTable.getSelectedRow() + 1);
        } else {
            return getCurrentTestCase().addNewStep();
        }
    }

    private void addLastRow() {
        int row = testCaseTable.getSelectedRow();
        int column = testCaseTable.getSelectedColumn();
        if (row == testCaseTable.getRowCount() - 1
                && column == testCaseTable.getColumnCount() - 1) {
            addRow();
        }
    }

    public TestStep addRow() {
        stopCellEditing();
        return getCurrentTestCase().addNewStep();
    }

    private void replicateRow() {
        stopCellEditing();
        if (testCaseTable.getSelectedRow() != -1) {
            getCurrentTestCase().replicateStepAt(testCaseTable.getSelectedRow());
        }
    }

    private void copyAbove() {
        stopCellEditing();
        int row = testCaseTable.getSelectedRow();
        if (row > 0) {
            for (int col : testCaseTable.getSelectedColumns()) {
                String value = Objects.toString(testCaseTable.getValueAt(row - 1, col), "");
                testCaseTable.setValueAt(value, row, col);
            }
        }
    }

    private void moveRowUp() {
        stopCellEditing();
        if (testCaseTable.getSelectedRows().length > 0) {
            List<Integer> rows = Utils.getSorted(testCaseTable.getSelectedRows());
            int from = rows.get(0);
            int to = rows.get(rows.size() - 1);
            if (getCurrentTestCase().moveRowsUp(from, to)) {
                testCaseTable.getSelectionModel().setSelectionInterval(from - 1, to - 1);
            }
        }
    }

    private void moveRowDown() {
        stopCellEditing();
        if (testCaseTable.getSelectedRows().length > 0) {
            List<Integer> rows = Utils.getSorted(testCaseTable.getSelectedRows());
            int from = rows.get(0);
            int to = rows.get(rows.size() - 1);
            if (getCurrentTestCase().moveRowsDown(from, to)) {
                testCaseTable.getSelectionModel().setSelectionInterval(from + 1, to + 1);
            }
        }
    }

    private void clearValues() {
        stopCellEditing();
        if (testCaseTable.getSelectedRowCount() > 0) {
            getCurrentTestCase().clearValues(
                    testCaseTable.getSelectedRows(),
                    testCaseTable.getSelectedColumns());
        }
    }

    private void deleteSelectedRows() {
        stopCellEditing();
        if (testCaseTable.getSelectedRows().length > 0) {
            getCurrentTestCase().removeSteps(Utils.getReverseSorted(testCaseTable.getSelectedRows()));
        }
    }

    private void parameterizeSelectedSteps() {
        stopCellEditing();
        if (testCaseTable.getSelectedRows().length > 0) {
            List<Integer> rows = Utils.getSorted(testCaseTable.getSelectedRows());
            int from = rows.get(0);
            int to = rows.get(rows.size() - 1);
            TestStep fstep = getCurrentTestCase().getTestSteps().get(from);
            TestStep tstep = getCurrentTestCase().getTestSteps().get(to);
            if (fstep.getCondition().isEmpty()) {
                fstep.setCondition("Start Param");
            } else if (!fstep.getCondition().equals("Start Param")) {
                insertFiller(from).setCondition("Start Param");
                to++;
            }
            if (tstep.getCondition().isEmpty()) {
                tstep.setCondition("End Param");
            } else if (!tstep.getCondition().contains("End Param")) {
                insertFiller(++to).setCondition("End Param");
            }
        }
    }

    private TestStep insertFiller(int row) {
        return getCurrentTestCase().addNewStepAt(row).setObject("Browser").setAction("filler");
    }

    private void toggleComment() {
        stopCellEditing();
        if (testCaseTable.getSelectedRows().length > 0) {
            getCurrentTestCase().toggleComment(testCaseTable.getSelectedRows());
        }
    }

    private void toggleBreakPoint() {
        stopCellEditing();
        if (testCaseTable.getSelectedRows().length > 0) {
            getCurrentTestCase().toggleBreakPoint(testCaseTable.getSelectedRows());
        }
    }

    private void openWithSystemEditor() {
        save();
        Utils.openWithSystemEditor(getCurrentTestCase().getLocation());
    }

    private void save() {
        stopCellEditing();
        populateDescription();
        getCurrentTestCase().save();
    }

    private void populateDescription() {
        int i = 0;
        for (TestStep testStep : getCurrentTestCase().getTestSteps()) {

            if (!testStep.getAction().isEmpty()
                    && testStep.getDescription().isEmpty()) {
                String desc = MethodInfoManager.getDescriptionFor(testStep.getAction());
                testCaseTable.setValueAt(desc, i, Description.getIndex());
            }
            i++;
        }
    }

    public void reload() {
        stopCellEditing();
        getCurrentTestCase().reload();
        tableColumnManager.reset();
        tcAutoSuggest.installForTestCase();
        validator.initValidations();
    }

    private void ccp(String operation) {
        switch (operation) {
            case "Cut":
                testCaseTable.cut();
                break;
            case "Copy":
                testCaseTable.copy();
                break;
            case "Paste":
                testCaseTable.paste();
                break;
        }
    }

    private void createReusable() {
        if (testCaseTable.getSelectedRowCount() > 0) {
            int from = testCaseTable.getSelectedRows()[0];
            int to = testCaseTable.getSelectedRows()[testCaseTable.getSelectedRowCount() - 1];
            String name = JOptionPane.showInputDialog("Enter the Reusable Name");
            if (name != null && !name.trim().isEmpty()) {
                TestCase reusable = getCurrentTestCase().
                        createAsReusable(name, from, to);
                if (reusable != null) {
                    testDesign.getReusableTree().getTreeModel().addTestCase(reusable);
                } else {
                    Notification.show("Couldn't Create Reusable - " + name);
                }
            }
        }
    }

    public XTable getTestCaseTable() {
        return testCaseTable;
    }

    private void debug() {
        run(true);
    }

    private void run() {
        run(false);
    }

    private void run(Boolean debugMode) {
        if (!runner.isAlive()) {
            save();
            getCurrentTestCase().getProject().save();
            stopCellEditing();
            SystemDefaults.debugMode.set(debugMode);
            initRunner();
            runner.start();
            if (debugMode) {
                debugDialog.showDebugDialog();
            }
        } else {
            JOptionPane.showMessageDialog(null, "Already Running");
        }
    }

    private void stopExecution() {
        if (runner.isAlive()) {
            SystemDefaults.pauseExecution.set(false);
            SystemDefaults.stopCurrentIteration.set(true);
            SystemDefaults.stopExecution.set(true);
        }
    }

    private void pauseExecution() {
        if (runner.isAlive()) {
            SystemDefaults.pauseExecution.set(true);
        }
    }

    private void continueExecution() {
        if (runner.isAlive()) {
            SystemDefaults.pauseExecution.set(false);
        }
    }

    private void nextStepExecution() {
        if (runner.isAlive()) {
            SystemDefaults.nextStepflag.set(false);
        }
    }

    private void goToSelectedReusable() {
        if (testCaseTable.getSelectedRow() != -1) {
            TestStep tStep = getCurrentTestCase().getTestSteps().get(testCaseTable.getSelectedRow());
            String[] reusableData = tStep.getReusableData();
            if (reusableData != null) {
                Scenario scenario = testDesign.getProject().getScenarioByName(reusableData[0]);
                if (scenario != null) {
                    TestCase testCase = scenario.getTestCaseByName(reusableData[1]);
                    if (testCase != null) {
                        loadTableModelForSelection(testCase);
                    } else {
                        Notification.show("TestCase [" + reusableData[1]
                                + "] not present in the Scenario [" + reusableData[0] + "]");
                    }
                } else {
                    Notification.show("Scenario [" + reusableData[0]
                            + "] not present in the project");
                }
            }
        }
    }

    private void goToTestData() {
        if (testCaseTable.getSelectedRow() != -1) {
            TestStep tStep = getCurrentTestCase().getTestSteps().get(testCaseTable.getSelectedRow());
            String[] tdFromInput = tStep.getTestDataFromInput();
            if (tdFromInput != null) {
                testDesign.getTestDatacomp().navigateToTestData(tdFromInput[0], tdFromInput[1]);
            }
        }
    }

    private void goToObject() {
        if (testCaseTable.getSelectedRow() != -1) {
            TestStep tStep = getCurrentTestCase().getTestSteps().get(testCaseTable.getSelectedRow());
            String[] objectPage = tStep.getPageObject();
            if (objectPage != null) {
                if (!testDesign.getObjectRepo().navigateToObject(objectPage[0], objectPage[1])) {
                    Notification.show(objectPage[0] + " - Object not found in Object Repository");
                }
            }
        }
    }

    public String getDefaultBrowser() {
        return toolBar.getSelectedBrowser();
    }

    public TCHistory getTestCaseHistory() {
        return testCaseHistory;
    }

    public TestCaseToolBar getToolBar() {
        return toolBar;
    }

    public TestDesign getTestDesign() {
        return testDesign;
    }

    class ConsoleDialog extends JDialog {

        private final ConsolePanel cPanel;

        public ConsoleDialog() {
            super(new JFrame());
            setAlwaysOnTop(true);
            setLayout(new BorderLayout());
            cPanel = new ConsolePanel();
            add(cPanel, BorderLayout.CENTER);
            setTitle("Console");
            setIconImage(new ImageIcon(getClass().getResource("/ui/resources/favicon.png")).getImage());
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
        }

        public void showConsole() {
            if (!isVisible()) {
                pack();
                setSize(600, 400);
                setLocationRelativeTo(null);
                setVisible(true);
            } else {
                toFront();
            }
        }

        public void start() {
            cPanel.start();
        }

    }

    class DebugDialog extends JDialog implements ActionListener {

        public DebugDialog() {
            super(new JFrame());
            init();
            setUndecorated(true);
        }

        private void init() {
            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);
            JButton drag = new JButton("   ");
            toolBar.add(drag);
            registerDrag(drag);
            toolBar.add(create("Show Console", "cmd"));
            toolBar.add(create("Continue Execution", "continue"));
            toolBar.add(create("Go to Next Step", "next"));
            toolBar.add(create("Pause the Execution", "pause"));
            toolBar.add(create("Stop the Execution", "stop"));
            add(toolBar);
        }

        private JButton create(String tooltip, String icon) {
            JButton button = new JButton();
            button.setActionCommand(tooltip);
            button.setToolTipText(tooltip);
            button.setIcon(Utils.getIconByResourceName("/ui/resources/testdesign/debug/" + icon));
            button.addActionListener(this);
            return button;
        }

        private void registerDrag(JButton drag) {
            drag.setContentAreaFilled(false);
            drag.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            WindowMover.register(this, drag, WindowMover.MOVE_BOTH);
        }

        void showDebugDialog() {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
            Rectangle rect = defaultScreen.getDefaultConfiguration().getBounds();
            pack();
            setLocation((int) rect.getCenterX(), Canvas.Window.winStart.y);
            setAlwaysOnTop(true);
            setVisible(true);
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            switch (ae.getActionCommand()) {
                case "Show Console":
                    consoleDialog.showConsole();
                    break;
                case "Continue Execution":
                    continueExecution();
                    break;
                case "Go to Next Step":
                    nextStepExecution();
                    break;
                case "Pause the Execution":
                    pauseExecution();
                    break;
                case "Stop the Execution":
                    stopExecution();
                    break;

            }
        }

    }

    class TCHistory extends JMenu implements ActionListener {

        private final LinkedList<String> historyList = new LinkedList<>();

        private final int max = 20;

        private Boolean allowed = false;

        public TCHistory() {
            setText("Recent TestCases");
            MenuScroller.setScrollerFor(this, 10);
        }

        public void log() {
            if (getCurrentTestCase() != null) {
                String val = getCurrentTestCase().getScenario().getName()
                        + ":"
                        + getCurrentTestCase().getName();
                log(val);
            }
        }

        public void log(String val) {
            if (allowed) {
                if (historyList.contains(val)) {
                    int index = historyList.indexOf(val);
                    historyList.remove(index);
                    remove(index);
                }
                if (historyList.size() == max) {
                    historyList.removeLast();
                    remove(getItemCount() - 1);
                }
                historyList.push(val);
                insert(val, 0);
            } else {
                allowed = true;
            }
        }

        @Override
        public void insert(String string, int i) {
            super.insert(string.split(":")[1], i);
            getItem(i).setToolTipText(string);
            getItem(i).setActionCommand(string);
            getItem(i).addActionListener(this);
        }

        public TestCase visit() {
            if (!historyList.isEmpty()) {
                String[] val = historyList.peek().split(":");
                Scenario scenario = testDesign.getProject().getScenarioByName(val[0]);
                if (scenario != null) {
                    return scenario.getTestCaseByName(val[1]);
                }
            }
            return null;
        }

        public void clear() {
            historyList.clear();
            allowed = false;
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            log(ae.getActionCommand());
            loadTableModelForSelection(visit());
        }
    }

}
