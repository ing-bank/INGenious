package com.ing.ide.main.mainui.components.testdesign.testcase;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestData;
import com.ing.datalib.component.TestStep;

import static com.ing.datalib.component.TestStep.HEADERS.Action;
import static com.ing.datalib.component.TestStep.HEADERS.Condition;
import static com.ing.datalib.component.TestStep.HEADERS.Description;
import static com.ing.datalib.component.TestStep.HEADERS.Input;
import static com.ing.datalib.component.TestStep.HEADERS.ObjectName;
import static com.ing.datalib.component.TestStep.HEADERS.Reference;
import com.ing.datalib.or.mobile.ResolvedMobileObject;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.datalib.testdata.model.Record;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.engine.support.methodInf.MethodInfoManager;
import com.ing.engine.support.methodInf.ObjectType;
import com.ing.engine.util.data.fx.FParser;
import com.ing.ide.main.utils.table.SQLTextArea;
import com.ing.ide.main.utils.table.WebservicePayloadArea;
import com.ing.ide.main.utils.table.EndPointTextArea;
import com.ing.ide.main.utils.table.StringOperationsPayloadArea;
import com.ing.ide.main.utils.table.autosuggest.AutoSuggest;
import com.ing.ide.main.utils.table.autosuggest.InputMainAutoSuggest;
import com.ing.ide.main.utils.table.autosuggest.AutoSuggestCellEditor;
import com.ing.ide.main.utils.table.autosuggest.InputAutoSuggestCellEditor;
import com.ing.ide.main.utils.table.autosuggest.ComboSeparatorsRenderer;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Auto-suggest controller for the Test Case table, providing intelligent
 * suggestions for Object, Action, Condition, and Input columns.
 *
 * Updated to support Mobile OR separation (Project + Shared) and scoped
 * reference tokens ("[Project]" / "[Shared]") when detecting object type.
 */
public class TestCaseAutoSuggest {
    private final Project sProject;
    final JTable table;

    private AutoSuggest objAutoSuggest;
    private AutoSuggest conditionAutoSuggest;
    private AutoSuggest actionAutoSuggest;
    private InputAutoSuggest inputAutoSuggest;

    public TestCaseAutoSuggest(Project sProject, JTable table) {
        this.sProject = sProject;
        this.table = table;
        initAutoSuggest();
        installMouseListener();
    }

    private void initAutoSuggest() {
        objAutoSuggest = new AutoSuggest().withSearchList(getObjectList())
                .withOnHide(stopEditingOnFocusLost());

        conditionAutoSuggest = (ConditionAutoSuggest) new ConditionAutoSuggest()
                .withOnHide(stopEditingOnFocusLost());
        conditionAutoSuggest.setRenderer(
                new ComboSeparatorsRenderer(conditionAutoSuggest.getRenderer()) {
                    @Override
                    protected boolean addSeparatorAfter(JList list, Object value, int index) {
                        return "End Param:@n".equals(value)
                                || "End Loop:@n".equals(value)
                                || "GlobalObject".equals(value);
                    }
                });

        actionAutoSuggest = new ActionAutoSuggest()
                .withOnHide(stopEditingOnFocusLost());

        inputAutoSuggest = (InputAutoSuggest) new InputAutoSuggest()
                .withOnHide(stopEditingOnFocusLost());
    }

    private boolean isStringOpsEditor(){
        int row = table.getSelectedRow();
        String value = "";
        if(row >= 0) value = table.getModel().getValueAt(row, 1).toString();
        if(!value.matches("String Operations")) return false;
        return true;
    }

    private Action stopEditingOnFocusLost() {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (table.isEditing()) {
                    table.getCellEditor().stopCellEditing();
                }
            }
        };
    }

    public void installForTestCase() {
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(ObjectName.getIndex()).setCellEditor(new AutoSuggestCellEditor(objAutoSuggest));
        table.getColumnModel().getColumn(Action.getIndex()).setCellEditor(new AutoSuggestCellEditor(actionAutoSuggest));
        table.getColumnModel().getColumn(Condition.getIndex()).setCellEditor(new AutoSuggestCellEditor(conditionAutoSuggest));
        table.getColumnModel().getColumn(Input.getIndex()).setCellEditor(new InputAutoSuggestCellEditor(inputAutoSuggest));
    }

    private List getObjectList() {
        List objectList = new ArrayList<>();
        objectList.add("Browser");
        objectList.add("Mobile");
        objectList.add("Webservice");
        objectList.add("Database");
        objectList.add("Kafka");
        objectList.add("Queue");
        objectList.add("Synthetic Data");
        objectList.add("File");
        objectList.add("General");
        objectList.add("Execute");
        objectList.add("String Operations");
        return objectList;
    }

    public List getContextAliasList() {
        List values = sProject.getProjectSettings().getContextSettings().getContextList();
        List newList = new ArrayList<>();
        for (Object string : values) {
            newList.add("#"+string);
        }
        return newList;
    }

    public List getDatabaseAliasList() {
        List values = sProject.getProjectSettings().getDatabaseSettings().getDbList();
        List newList = new ArrayList<>();
        for (Object string : values) {
            newList.add("#"+string);
        }
        return newList;
    }

    public List getAPIAliasList() {
        List values = sProject.getProjectSettings().getDriverSettings().getAPIList();
        List newList = new ArrayList<>();
        for (Object string : values) {
            newList.add("#"+string);
        }
        return newList;
    }

    private void startEditing(final AutoSuggest suggest) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!table.isEditing()) {
                    table.editCellAt(table.getSelectedRow(), table.getSelectedColumn());
                    boolean isStringOpsEditor = isStringOpsEditor();
                    if(!isStringOpsEditor){
                        suggest.getTextField().setText(suggest.getText() + ":");
                        suggest.getTextField().requestFocusInWindow();
                        suggest.updateList();
                    }
                }
            }
        });
    }

    private void startEditing(final InputMainAutoSuggest suggest) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (!table.isEditing()) {
                    table.editCellAt(table.getSelectedRow(), table.getSelectedColumn());
                    boolean isStringOpsEditor = isStringOpsEditor();
                    if(!isStringOpsEditor){
                        suggest.getTextField().setText(suggest.getText() + ":");
                        suggest.getTextField().requestFocusInWindow();
                        suggest.updateList();
                    }
                }
            }
        });
    }

    private void installMouseListener() {
        table.addMouseListener(new MouseAdapterImpl());
        table.addMouseMotionListener(new MouseMotionAdapterImpl());
    }

    private TestCase getTestCase(JTable table) {
        if (table.getModel() instanceof TestCase) {
            return (TestCase) table.getModel();
        }
        return null;
    }

    private boolean isDataBaseQueryStep(TestStep step) {
        return step != null && step.isDatabaseStep() && (step.getAction().contains("execute")
                || step.getAction().contains("storeResult"));
    }

    private boolean isProtractorjsStep(TestStep step) {
        return step != null && (step.getAction().contains("protractor_customSpec"));
    }

    private boolean isRestWebservicePostStep(TestStep step) {
        return step != null && step.isWebserviceStep() && (step.getAction().contains("postRest")
                || step.getAction().contains("putRest")
                || step.getAction().contains("patchRest")
                || step.getAction().contains("deleteWithPayload"));
    }

    private boolean isSetEndPointStep(TestStep step) {
        return step != null && step.isWebserviceStep() && (step.getAction().contains("setEndPoint"));
    }

    private boolean isSOAPWebservicePostStep(TestStep step) {
        return step != null && step.isWebserviceStep() && step.getAction().contains("postSoap");
    }

    private boolean isFileStep(TestStep step) {
        return step != null && step.isFileStep() && step.getAction().contains("populateData");
    }

    private boolean isMessageStep(TestStep step) {
        return step != null && step.isMessageStep() && (step.getAction().contains("setText")
                || step.getAction().contains("produceMessage"));
    }

    private boolean isRouteFulfillEndpointStep(TestStep step) {
        return step != null && step.isBrowserStep() && (step.getAction().contains("RouteFulfillEndpoint"));
    }

    private boolean isRouteFulfillSetBodyStep(TestStep step) {
        return step != null && step.isBrowserStep() && step.getAction().contains("RouteFulfillSetBody");
    }

    private boolean isStringOperationsStep(TestStep step) {
        return step != null && step.isStringOperationsStep();
    }

    class ConditionAutoSuggest extends AutoSuggest {

        private List getConditionBasedOnText(String value) {
            String objectName = Objects.toString(table.getValueAt(
                    table.getSelectedRow(), ObjectName.getIndex()), "");
            if ("Webservice".equals(objectName)){
                return getAPIAliasList();
            } else {
                return getContextAliasList();
            }
        }

        @Override
        public void beforeSearch(String text) {
            if (text.isEmpty()) {
                setSearchList(getConditionList());
            } else {
                if (text.startsWith("#")) {
                    setSearchList(getConditionBasedOnText(text));
                }
            }
        }

        private List getConditionList() {
            List conditionList = new ArrayList<>();
            conditionList.add("Start Param");
            conditionList.add("End Param");
            conditionList.add("End Param:@n");
            conditionList.add("Start Loop");
            conditionList.add("End Loop:@n");
            conditionList.add("GlobalObject");
            conditionList.add("screen");
            conditionList.add("viewport");
            return conditionList;
        }
    }

    class ActionAutoSuggest extends AutoSuggest {

        private List getActionBasedOnObject() {
            String objectName = Objects.toString(table.getValueAt(
                    table.getSelectedRow(), ObjectName.getIndex()), "");
            String pageToken = Objects.toString(table.getValueAt(
                    table.getSelectedRow(), Reference.getIndex()), "");

            switch (objectName) {
                case "Execute":
                    return getReusables();
                case "Browser":
                    return MethodInfoManager.getMethodListFor(ObjectType.BROWSER, ObjectType.ANY);
                case "Mobile":
                    return MethodInfoManager.getMethodListFor(ObjectType.MOBILE, ObjectType.MOBILE);
                case "Database":
                    return MethodInfoManager.getMethodListFor(ObjectType.DATABASE, ObjectType.DATABASE);
                case "ProtractorJS":
                    return MethodInfoManager.getMethodListFor(ObjectType.PROTRACTORJS, ObjectType.PROTRACTORJS);
                case "Webservice":
                    return MethodInfoManager.getMethodListFor(ObjectType.WEBSERVICE, ObjectType.WEBSERVICE);
                case "Synthetic Data":
                    return MethodInfoManager.getMethodListFor(ObjectType.DATA, ObjectType.DATA);
                case "Queue":
                    return MethodInfoManager.getMethodListFor(ObjectType.QUEUE, ObjectType.QUEUE);
                case "Kafka":
                    return MethodInfoManager.getMethodListFor(ObjectType.KAFKA, ObjectType.KAFKA);
                case "File":
                    return MethodInfoManager.getMethodListFor(ObjectType.FILE, ObjectType.FILE);
                case "General":
                    return MethodInfoManager.getMethodListFor(ObjectType.GENERAL, ObjectType.GENERAL);
                case "String Operations":
                    return MethodInfoManager.getMethodListFor(ObjectType.STRINGOPERATIONS, ObjectType.STRINGOPERATIONS);
                default:
                    if (isWebObject(objectName, pageToken)) {
                        return MethodInfoManager.getMethodListFor(ObjectType.PLAYWRIGHT, ObjectType.WEB, ObjectType.ANY);
                    } else if (isMobileObject(objectName, pageToken)) {
                        return MethodInfoManager.getMethodListFor(ObjectType.APP);
                    }
            }
            return new ArrayList<>();
        }

        private List getReusables() {
            List reusableList = new ArrayList<>();
            for (Scenario scenario : sProject.getScenarios()) {
                int rcount = scenario.getReusableCount();
                for (int i = 0; i < rcount; i++) {
                    reusableList.add(scenario.getName() + ":" + scenario.getReusableAt(i).getName());
                }
            }
            return reusableList;
        }

        private boolean isWebObject(String objectName, String pageToken) {
            if (pageToken == null
                    || pageToken.isBlank()
                    || objectName == null
                    || objectName.isBlank()) {
                return false;
            }
            var repo = sProject.getObjectRepository();
            ResolvedWebObject.PageRef ref = ResolvedWebObject.PageRef.parse(pageToken);
            ResolvedWebObject r = (ref != null && ref.name != null && ref.scope != null)
                    ? repo.resolveWebObject(ref, objectName)
                    : repo.resolveWebObjectWithScope(pageToken, objectName);
            return r != null && r.isPresent();
        }

        /**
         * Detect Mobile objects via Mobile resolver (supports scoped refs + shared)
         * instead of directly accessing getMobileOR()/pages.
         */
        private boolean isMobileObject(String objectName, String pageToken) {
            if (pageToken == null
                    || pageToken.isBlank()
                    || objectName == null
                    || objectName.isBlank()) {
                return false;
            }
            var repo = sProject.getObjectRepository();
            ResolvedMobileObject.PageRef ref = ResolvedMobileObject.PageRef.parse(pageToken);
            ResolvedMobileObject r = (ref != null && ref.name != null && ref.scope != null)
                    ? repo.resolveMobileObject(ref, objectName)
                    : repo.resolveMobileObjectWithScope(pageToken, objectName);
            return r != null && r.isPresent();
        }

        @Override
        public void beforeSearch(String text) {
            setSearchList(getActionBasedOnObject());
        }

        @Override
        public void afterReset() {
            String val = Objects.toString(
                    table.getValueAt(table.getSelectedRow(), Description.getIndex()), "");
            if (val.trim().isEmpty()) {
                String desc = MethodInfoManager.getDescriptionFor(getText());
                table.setValueAt(desc, table.getSelectedRow(), Description.getIndex());
            }
        }
    }

    class InputAutoSuggest extends InputMainAutoSuggest {
        Boolean isPending = false;
        private String prevText;

        public InputAutoSuggest(){
            super.setTable(TestCaseAutoSuggest.this.table);
        }

        private List getInputBasedOnText(String value) {
            if (value.startsWith("%")) {
                return getUserDefinedList();
            } else if (value.startsWith("=")) {
                return getFunctionList();
            } else if(value.startsWith("#")) {
                return getDatabaseAliasList();
            }
            return setupTestData(value);
        }

        public List getUserDefinedList() {
            Set udSet = sProject.getProjectSettings().getUserDefinedSettings().stringPropertyNames();
            List values = new ArrayList<>();
            for (Object string : udSet) {
                values.add("%".concat((String) string).concat("%"));
            }
            return values;
        }

        private List getFunctionList() {
            List newFList = new ArrayList<>();
            for (String function : FParser.getFuncList()) {
                newFList.add("=" + function);
            }
            return newFList;
        }

        private List setupTestData(String value) {
            if (value != null && value.contains(":")) {
                prevText = value.substring(0, value.indexOf(':'));
                isPending = true;
                Set colList = new LinkedHashSet<>();
                String tdName = value.substring(0, value.indexOf(':'));
                for (TestData sTestData : sProject.getTestData().getAllEnvironments()) {
                    for (TestDataModel stdList : sTestData.getTestDataList()) {
                        if (stdList.getName().equals(tdName)) {
                            colList.addAll(stdList.getColumns());
                        }
                    }
                }
                colList.removeAll(Arrays.asList(Record.HEADERS));
                return new ArrayList<>(colList);
            } else {
                Set tdList = new LinkedHashSet<>();
                for (TestData sTestData : sProject.getTestData().getAllEnvironments()) {
                    for (TestDataModel stdList : sTestData.getTestDataList()) {
                        tdList.add(stdList.getName());
                    }
                }
                return new ArrayList<>(tdList);
            }
        }

        public List getTestData() {
            List retList = new ArrayList<>();
            Set tdList = new LinkedHashSet<>();
            sProject.getTestData().getAllEnvironments().stream().forEach((sTestData) -> {
                for (TestDataModel stdList : sTestData.getTestDataList()) {
                    tdList.add(stdList.getName());
                }
            });
            tdList.stream().forEach((string) -> {
                List tdCols = setupTestData(string + ":");
                tdCols.stream().forEach((tdCol) -> {
                    retList.add(string + ":" + tdCol);
                });
            });
            return retList;
        }

        @Override
        public void setSelectedItem(Object o) {
            boolean isStringOpsEditor = isStringOpsEditor();
            if (o != null && !o.toString().matches("(@.+)\n(=.+)\n(%.+%)") && !o.toString().contains(":")) {
                if (isPending && prevText != null && !isStringOpsEditor) {
                    o = prevText + ":" + o.toString();
                } else if (isPending && prevText != null && isStringOpsEditor) {
                    o = prevText + o.toString();
                }
            }
            super.setSelectedItem(o);
        }

        @Override
        public String preReset(String val) {
            if (!val.isEmpty() && !val.equals(getText()) && !val.contains(":")) {
                val = getText().split(":")[0] + ":" + val;
                table.setValueAt(val, table.getSelectedRow(), table.getSelectedColumn());
                return val;
            }
            return val;
        }

        @Override
        public void beforeSearch(String text) {
            prevText = null;
            isPending = false;
            if (text.startsWith("@")) {
                clearSearchList();
            } else {
                setSearchList(getInputBasedOnText(text));
            }
        }

        @Override
        public void afterReset() {
            prevText = null;
            isPending = false;
            if (!getText().isEmpty() && !getText().matches("^[\\%\\@].*") && !getText().contains(":")) {
                startEditing(this);
            }
        }

        public void setPrevText(String prevText) {
            this.prevText = prevText;
        }

        @Override
        public void beforeShow() {
            String val = Objects.toString(getSelectedItem(), "");
            if (!val.isEmpty() && !val.matches("(@.+)\n(=.+)\n(%.+%)") && val.contains(":")) {
                setPrevText(val.substring(0, val.indexOf(':')));
                isPending = true;
            }
        }

        @Override
        public String getSearchString() {
            String text = super.getSearchString();
            if (isPending && prevText != null) {
                return text.substring(text.indexOf(':') + 1);
            }
            return text;
        }
    }

    class MouseAdapterImpl extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent me) {
            boolean isInputclicked = table.columnAtPoint(me.getPoint()) == Input.getIndex();
            if (me.isAltDown()) {
                if (table.rowAtPoint(me.getPoint()) != -1 && getTestCase(table) != null) {
                    TestStep step = getTestCase(table).getTestSteps().get(table.rowAtPoint(me.getPoint()));
                    if ((isDataBaseQueryStep(step) && table.columnAtPoint(me.getPoint()) == Input.getIndex())
                            || (isProtractorjsStep(step) && table.columnAtPoint(me.getPoint()) == Input.getIndex())) {
                        new SQLTextArea(null, step, getInputs());
                    }
                    if ((isRestWebservicePostStep(step) && isInputclicked)) {
                        new WebservicePayloadArea(null, step, "REST", getInputs());
                    }
                    if ((isSOAPWebservicePostStep(step) && isInputclicked)) {
                        new WebservicePayloadArea(null, step, "SOAP", getInputs());
                    }
                    if ((isSetEndPointStep(step) && isInputclicked)) {
                        new EndPointTextArea(null, step, getInputs());
                    }
                    if ((isFileStep(step) && isInputclicked)) {
                        new WebservicePayloadArea(null, step, "SOAP", getInputs());
                    }
                    if ((isMessageStep(step) && isInputclicked)) {
                        new WebservicePayloadArea(null, step, "SOAP", getInputs());
                    }
                    if ((isRouteFulfillSetBodyStep(step) && isInputclicked)) {
                        new WebservicePayloadArea(null, step, "REST", getInputs());
                    }
                    if ((isRouteFulfillEndpointStep(step) && isInputclicked)) {
                        new EndPointTextArea(null, step, getInputs());
                    }
                    if ((isStringOperationsStep(step) && isInputclicked)) {
                        new StringOperationsPayloadArea(null, step, getInputs());
                    }
                }
            }
        }
    }

    public List getInputs() {
        List auto = inputAutoSuggest.getUserDefinedList();
        auto.addAll(inputAutoSuggest.getTestData());
        return auto;
    }

    class MouseMotionAdapterImpl extends MouseMotionAdapter {

        Point hintCell;
        Timer showTimerp;
        Timer showTimerd;
        Timer showTimerw;
        Timer showTimerf;
        Timer showTimerm;
        Timer showTimers;

        Timer disposeTimerp;
        Timer disposeTimerd;
        Timer disposeTimerw;
        Timer disposeTimerf;
        Timer disposeTimerm;
        Timer disposeTimers;

        JPopupMenu popupp;
        JPopupMenu popupd;
        JPopupMenu popupw;
        JPopupMenu popupf;
        JPopupMenu popupm;
        JPopupMenu popups;

        TestStep step;

        public MouseMotionAdapterImpl() {
            popupp = new JPopupMenu();
            popupd = new JPopupMenu();
            popupw = new JPopupMenu();
            popupf = new JPopupMenu();
            popupm = new JPopupMenu();
            popups = new JPopupMenu();

            final JMenuItem jMenuItemp = new JMenuItem("Click to open ProtractorJS command editor");
            final JMenuItem jMenuItemd = new JMenuItem("Click to Open SQL Query Editor ");
            final JMenuItem jMenuItemw = new JMenuItem("Click to Open Webservice Editor ");
            final JMenuItem jMenuItemf = new JMenuItem("Click to Open File Editor ");
            final JMenuItem jMenuItemm = new JMenuItem("Click to Open Message Editor ");
            final JMenuItem jMenuItems = new JMenuItem("Click to Open String Operations Editor ");

            popupp.add(jMenuItemp);
            popupd.add(jMenuItemd);
            popupw.add(jMenuItemw);
            popupf.add(jMenuItemf);
            popupm.add(jMenuItemm);
            popups.add(jMenuItems);

            jMenuItemp.addActionListener((ActionEvent ae) -> {
                if (step != null && (isProtractorjsStep(step))) {
                    new SQLTextArea(null, step, getInputs());
                }
            });

            jMenuItemd.addActionListener((ActionEvent ae) -> {
                if (step != null && (isDataBaseQueryStep(step))) {
                    new SQLTextArea(null, step, getInputs());
                }
            });

            jMenuItemw.addActionListener((ActionEvent ae) -> {
                if (step.isWebserviceStep() && step.getAction().contains("postSoap")) {
                    new WebservicePayloadArea(null, step, "SOAP", getInputs());
                }
                if (isRestWebservicePostStep(step)) {
                    new WebservicePayloadArea(null, step, "REST", getInputs());
                }
                if (isSetEndPointStep(step)) {
                    new EndPointTextArea(null, step, getInputs());
                }
                if (step.isBrowserStep() && step.getAction().contains("RouteFulfillSetBody")) {
                    new WebservicePayloadArea(null, step, "REST", getInputs());
                }
                if (isRouteFulfillEndpointStep(step)) {
                    new EndPointTextArea(null, step, getInputs());
                }
            });

            jMenuItemf.addActionListener((ActionEvent ae) -> {
                if (step != null && (isFileStep(step))) {
                    new WebservicePayloadArea(null, step, "SOAP", getInputs());
                }
            });

            jMenuItemm.addActionListener((ActionEvent ae) -> {
                if (step != null && (isMessageStep(step))) {
                    new WebservicePayloadArea(null, step, "SOAP", getInputs());
                }
            });

            jMenuItems.addActionListener((ActionEvent ae) -> {
                if (step != null && (isStringOperationsStep(step))) {
                    new StringOperationsPayloadArea(null, step, getInputs());
                }
            });

            // Timer p
            showTimerp = new Timer(1000, (ActionEvent ae) -> {
                if (hintCell != null) {
                    disposeTimerp.stop();
                    popupp.setVisible(false);
                    Rectangle bounds = table.getCellRect(hintCell.y, hintCell.x, true);
                    int x = bounds.x;
                    int y = bounds.y + bounds.height;
                    popupp.show(table, x, y);
                    disposeTimerp.start();
                }
            });
            showTimerp.setRepeats(false);
            showTimerp.setCoalesce(true);
            disposeTimerp = new Timer(2000, (ActionEvent ae) -> {
                popupp.setVisible(false);
            });
            disposeTimerp.setRepeats(false);
            disposeTimerp.setCoalesce(true);

            // Timer D
            showTimerd = new Timer(1000, (ActionEvent ae) -> {
                if (hintCell != null) {
                    disposeTimerd.stop();
                    popupd.setVisible(false);
                    Rectangle bounds = table.getCellRect(hintCell.y, hintCell.x, true);
                    int x = bounds.x;
                    int y = bounds.y + bounds.height;
                    popupd.show(table, x, y);
                    disposeTimerd.start();
                }
            });
            showTimerd.setRepeats(false);
            showTimerd.setCoalesce(true);
            disposeTimerd = new Timer(2000, (ActionEvent ae) -> {
                popupd.setVisible(false);
            });
            disposeTimerd.setRepeats(false);
            disposeTimerd.setCoalesce(true);

            //Timer w
            showTimerw = new Timer(1000, (ActionEvent ae) -> {
                if (hintCell != null) {
                    disposeTimerw.stop();
                    popupw.setVisible(false);
                    Rectangle bounds = table.getCellRect(hintCell.y, hintCell.x, true);
                    int x = bounds.x;
                    int y = bounds.y + bounds.height;
                    popupw.show(table, x, y);
                    disposeTimerw.start();
                }
            });
            showTimerw.setRepeats(false);
            showTimerw.setCoalesce(true);
            disposeTimerw = new Timer(2000, (ActionEvent ae) -> {
                popupw.setVisible(false);
            });
            disposeTimerw.setRepeats(false);
            disposeTimerw.setCoalesce(true);

            //Timer f
            showTimerf = new Timer(1000, (ActionEvent ae) -> {
                if (hintCell != null) {
                    disposeTimerf.stop();
                    popupf.setVisible(false);
                    Rectangle bounds = table.getCellRect(hintCell.y, hintCell.x, true);
                    int x = bounds.x;
                    int y = bounds.y + bounds.height;
                    popupf.show(table, x, y);
                    disposeTimerf.start();
                }
            });
            showTimerf.setRepeats(false);
            showTimerf.setCoalesce(true);
            disposeTimerf = new Timer(2000, (ActionEvent ae) -> {
                popupf.setVisible(false);
            });
            disposeTimerf.setRepeats(false);
            disposeTimerf.setCoalesce(true);

            //Timer m
            showTimerm = new Timer(1000, (ActionEvent ae) -> {
                if (hintCell != null) {
                    disposeTimerm.stop();
                    popupm.setVisible(false);
                    Rectangle bounds = table.getCellRect(hintCell.y, hintCell.x, true);
                    int x = bounds.x;
                    int y = bounds.y + bounds.height;
                    popupm.show(table, x, y);
                    disposeTimerm.start();
                }
            });
            showTimerm.setRepeats(false);
            showTimerm.setCoalesce(true);
            disposeTimerm = new Timer(2000, (ActionEvent ae) -> {
                popupm.setVisible(false);
            });
            disposeTimerm.setRepeats(false);
            disposeTimerm.setCoalesce(true);

            //Timer s
            showTimers = new Timer(1000, (ActionEvent ae) -> {
                if (hintCell != null) {
                    disposeTimers.stop();
                    popups.setVisible(false);
                    Rectangle bounds = table.getCellRect(hintCell.y, hintCell.x, true);
                    int x = bounds.x;
                    int y = bounds.y + bounds.height;
                    popups.show(table, x, y);
                    disposeTimerf.start();
                }
            });
            showTimers.setRepeats(false);
            showTimers.setCoalesce(true);
            disposeTimers = new Timer(2000, (ActionEvent ae) -> {
                popups.setVisible(false);
            });
            disposeTimers.setRepeats(false);
            disposeTimers.setCoalesce(true);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            Point p = e.getPoint();
            int row = table.rowAtPoint(p);
            int col = table.columnAtPoint(p);
            if (row != -1 && getTestCase(table) != null) {
                step = getTestCase(table).getTestSteps().get(row);
                if (isDataBaseQueryStep(step) && col == Input.getIndex()) {
                    if (hintCell == null
                            || (hintCell.x != col
                            || hintCell.y != row)) {
                        hintCell = new Point(col, row);
                        showTimerd.restart();
                    }
                } else if (isProtractorjsStep(step) && col == Input.getIndex()) {
                    if (hintCell == null
                            || (hintCell.x != col
                            || hintCell.y != row)) {
                        hintCell = new Point(col, row);
                        showTimerp.restart();
                    }
                } else if ((isSOAPWebservicePostStep(step) && col == Input.getIndex())
                        || (isRestWebservicePostStep(step) && col == Input.getIndex())) {
                    if (hintCell == null
                            || (hintCell.x != col
                            || hintCell.y != row)) {
                        hintCell = new Point(col, row);
                        showTimerw.restart();
                    }
                } else if ((isSetEndPointStep(step) && col == Input.getIndex())) {
                    if (hintCell == null
                            || (hintCell.x != col
                            || hintCell.y != row)) {
                        hintCell = new Point(col, row);
                        showTimerw.restart();
                    }
                } else if ((isFileStep(step) && col == Input.getIndex())) {
                    if (hintCell == null
                            || (hintCell.x != col
                            || hintCell.y != row)) {
                        hintCell = new Point(col, row);
                        showTimerf.restart();
                    }
                } else if ((isRouteFulfillEndpointStep(step) && col == Input.getIndex())) {
                    if (hintCell == null
                            || (hintCell.x != col
                            || hintCell.y != row)) {
                        hintCell = new Point(col, row);
                        showTimerw.restart();
                    }
                } else if ((isRouteFulfillSetBodyStep(step) && col == Input.getIndex())) {
                    if (hintCell == null
                            || (hintCell.x != col
                            || hintCell.y != row)) {
                        hintCell = new Point(col, row);
                        showTimerw.restart();
                    }
                } else if ((isMessageStep(step) && col == Input.getIndex())) {
                    if (hintCell == null
                            || (hintCell.x != col
                            || hintCell.y != row)) {
                        hintCell = new Point(col, row);
                        showTimerm.restart();
                    }
                } else if ((isStringOperationsStep(step) && col == Input.getIndex())) {
                    if (hintCell == null
                            || (hintCell.x != col
                            || hintCell.y != row)) {
                        hintCell = new Point(col, row);
                        showTimers.restart();
                    }
                } else {
                    hintCell = null;
                    if (popupp.isVisible()
                            || popupd.isVisible()
                            || popupw.isVisible()
                            || popupf.isVisible()
                            || popupm.isVisible()
                            || popups.isVisible()) {
                        popupp.setVisible(false);
                        popupd.setVisible(false);
                        popupw.setVisible(false);
                        popupf.setVisible(false);
                        popupm.setVisible(false);
                        popups.setVisible(false);
                    }
                }
            }
        }
    }
}