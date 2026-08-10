package com.ing.ide.main.mainui.components.testdesign.testcase;

import static com.ing.datalib.component.TestStep.HEADERS.Action;
import static com.ing.datalib.component.TestStep.HEADERS.Condition;
import static com.ing.datalib.component.TestStep.HEADERS.Description;
import static com.ing.datalib.component.TestStep.HEADERS.Input;
import static com.ing.datalib.component.TestStep.HEADERS.ObjectName;
import static com.ing.datalib.component.TestStep.HEADERS.Reference;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestData;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.mobile.ResolvedMobileObject;
import com.ing.datalib.or.sap.ResolvedSapObject;
import com.ing.datalib.or.structureddata.ResolvedStructuredDataObject;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.datalib.testdata.model.Record;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.engine.support.ObjectTypeUtil;
import com.ing.engine.support.methodInf.MethodInfoManager;
import com.ing.engine.util.data.fx.FParser;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.utils.table.EndPointTextArea;
import com.ing.ide.main.utils.table.SQLTextArea;
import com.ing.ide.main.utils.table.StringOperationsPayloadArea;
import com.ing.ide.main.utils.table.WebservicePayloadArea;
import com.ing.ide.main.utils.table.autosuggest.AutoSuggest;
import com.ing.ide.main.utils.table.autosuggest.AutoSuggestCellEditor;
import com.ing.ide.main.utils.table.autosuggest.ComboSeparatorsRenderer;
import com.ing.ide.main.utils.table.autosuggest.InputAutoSuggestCellEditor;
import com.ing.ide.main.utils.table.autosuggest.InputMainAutoSuggest;
import com.ing.ingenious.api.types.ObjectType;
import java.awt.Color;
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
 * Updated to support Web, Mobile and Structured Data OR separation (Project + Shared) and scoped
 * Updated to support Web, Mobile and Structured Data OR separation (Project + Shared) and scoped
 * reference tokens ("[Project]" / "[Shared]") when detecting object type.
 */
public class TestCaseAutoSuggest {
    private final Project sProject;
    private final TestDesign testDesign;
    final JTable table;

    private AutoSuggest objAutoSuggest;
    private AutoSuggest conditionAutoSuggest;
    private AutoSuggest actionAutoSuggest;
    private InputAutoSuggest inputAutoSuggest;

    public TestCaseAutoSuggest(Project sProject, JTable table, TestDesign testDesign) {
        this.sProject = sProject;
        this.table = table;
        this.testDesign = testDesign;
        initAutoSuggest();
        installMouseListener();
    }

    private void initAutoSuggest() {
        objAutoSuggest =
            new AutoSuggest().withSearchList(getObjectList()).withOnHide(stopEditingOnFocusLost());

        conditionAutoSuggest =
            (ConditionAutoSuggest) new ConditionAutoSuggest().withOnHide(stopEditingOnFocusLost());
        conditionAutoSuggest.setRenderer(
            new ComboSeparatorsRenderer(conditionAutoSuggest.getRenderer()) {

                @Override
                protected boolean addSeparatorAfter(JList list, Object value, int index) {
                    return (
                        "End Param:@n".equals(value) ||
                        "End Loop:@n".equals(value) ||
                        "GlobalObject".equals(value)
                    );
                }
            }
        );

        actionAutoSuggest = new ActionAutoSuggest().withOnHide(stopEditingOnFocusLost());

        // Phase 5.4: Add custom renderer for scope prefix display with separators
        actionAutoSuggest.setRenderer(
            new ComboSeparatorsRenderer(actionAutoSuggest.getRenderer()) {

                @Override
                protected void customizeListItemComponent(
                    java.awt.Component comp,
                    JList list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
                ) {
                    if (!(comp instanceof javax.swing.JLabel) || value == null) {
                        return;
                    }
                    javax.swing.JLabel lbl = (javax.swing.JLabel) comp;
                    String raw = value.toString();
                    lbl.setText(removeScopePrefix(raw));
                    if (raw.startsWith("[Shared]")) {
                        // Keep shared items readable on hover/selection by using shared-specific shades.
                        if (isSelected) {
                            lbl.setOpaque(true);
                            lbl.setBackground(new Color(213, 238, 220));
                            lbl.setForeground(new Color(0, 83, 0));
                        } else {
                            lbl.setForeground(new Color(0, 128, 0));
                        }
                    } else if (raw.startsWith("[Project]")) {
                        if (isSelected) {
                            lbl.setOpaque(true);
                        }
                        lbl.setForeground(Color.BLACK);
                    }
                }

                @Override
                protected boolean addHeaderBefore(JList list, Object value, int index) {
                    if (value == null) {
                        return false;
                    }
                    String current = value.toString();
                    if (current.startsWith("[Project]")) {
                        return (
                            index == 0 ||
                            !Objects
                                .toString(list.getModel().getElementAt(index - 1), "")
                                .startsWith("[Project]")
                        );
                    }
                    if (current.startsWith("[Shared]")) {
                        return (
                            index == 0 ||
                            !Objects
                                .toString(list.getModel().getElementAt(index - 1), "")
                                .startsWith("[Shared]")
                        );
                    }
                    return false;
                }

                @Override
                protected String getHeaderLabel(JList list, Object value, int index) {
                    if (value == null) {
                        return "";
                    }
                    String current = value.toString();
                    if (current.startsWith("[Project]")) {
                        return "Project Reusables";
                    }
                    if (current.startsWith("[Shared]")) {
                        return "Shared Reusables";
                    }
                    return "";
                }

                @Override
                protected Color getHeaderForeground(
                    JList list,
                    Object value,
                    int index,
                    java.awt.Component comp
                ) {
                    if (value == null) {
                        return Color.DARK_GRAY;
                    }
                    String current = value.toString();
                    if (current.startsWith("[Shared]")) {
                        return new Color(0, 128, 0);
                    }
                    return Color.BLACK;
                }

                @Override
                protected boolean addSeparatorAfter(JList list, Object value, int index) {
                    if (value == null) return false;
                    String val = value.toString();
                    // Add separator after last [Project] item before [Shared] items
                    if (index < list.getModel().getSize() - 1) {
                        Object nextValue = list.getModel().getElementAt(index + 1);
                        if (nextValue != null) {
                            String current = val;
                            String next = nextValue.toString();
                            return current.startsWith("[Project]") && next.startsWith("[Shared]");
                        }
                    }
                    return false;
                }
            }
        );

        inputAutoSuggest =
            (InputAutoSuggest) new InputAutoSuggest().withOnHide(stopEditingOnFocusLost());
    }

    private boolean isStringOpsEditor() {
        int row = table.getSelectedRow();
        String value = "";
        if (row >= 0) value = table.getModel().getValueAt(row, 1).toString();
        if (!value.matches("String Operations")) return false;
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
        table.getColumnModel().getColumn(Action.getIndex()).setPreferredWidth(250);
        table
            .getColumnModel()
            .getColumn(ObjectName.getIndex())
            .setCellEditor(new AutoSuggestCellEditor(objAutoSuggest));
        table
            .getColumnModel()
            .getColumn(Action.getIndex())
            .setCellEditor(new AutoSuggestCellEditor(actionAutoSuggest));
        table
            .getColumnModel()
            .getColumn(Condition.getIndex())
            .setCellEditor(new AutoSuggestCellEditor(conditionAutoSuggest));
        table
            .getColumnModel()
            .getColumn(Input.getIndex())
            .setCellEditor(new InputAutoSuggestCellEditor(inputAutoSuggest));
    }

    /**
     * Retrieves a list of all object types available in the IDE.
     * <p>
     * The object types are sourced from {@link ObjectTypeUtil#getAllTypesForIDE()}
     * and are used to populate the ObjectName dropdown in test case design.
     * </p>
     *
     * @return a List of String containing all object type names available in the IDE
     * @see ObjectTypeUtil#getAllTypesForIDE()
     */
    private List<String> getObjectList() {
        List<String> objectList = new ArrayList<>(ObjectTypeUtil.getAllTypesForIDE()); // arraylist to allow ordered list
        return objectList;
    }

    public List getContextAliasList() {
        List values = sProject.getProjectSettings().getContextSettings().getContextList();
        List newList = new ArrayList<>();
        for (Object string : values) {
            newList.add("#" + string);
        }
        return newList;
    }

    public List getDatabaseAliasList() {
        List values = sProject.getProjectSettings().getDatabaseSettings().getDbList();
        List newList = new ArrayList<>();
        for (Object string : values) {
            newList.add("#" + string);
        }
        return newList;
    }

    public List getAPIAliasList() {
        List values = sProject.getProjectSettings().getDriverSettings().getAPIList();
        List newList = new ArrayList<>();
        for (Object string : values) {
            newList.add("#" + string);
        }
        return newList;
    }

    private void startEditing(final AutoSuggest suggest) {
        SwingUtilities.invokeLater(
            new Runnable() {

                @Override
                public void run() {
                    if (!table.isEditing()) {
                        table.editCellAt(table.getSelectedRow(), table.getSelectedColumn());
                        boolean isStringOpsEditor = isStringOpsEditor();
                        if (!isStringOpsEditor) {
                            suggest.getTextField().setText(suggest.getText() + ":");
                            suggest.getTextField().requestFocusInWindow();
                            suggest.updateList();
                        }
                    }
                }
            }
        );
    }

    private void startEditing(final InputMainAutoSuggest suggest) {
        SwingUtilities.invokeLater(
            new Runnable() {

                @Override
                public void run() {
                    if (!table.isEditing()) {
                        table.editCellAt(table.getSelectedRow(), table.getSelectedColumn());
                        boolean isStringOpsEditor = isStringOpsEditor();
                        if (!isStringOpsEditor) {
                            suggest.getTextField().setText(suggest.getText() + ":");
                            suggest.getTextField().requestFocusInWindow();
                            suggest.updateList();
                        }
                    }
                }
            }
        );
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
        return (
            step != null &&
            step.isDatabaseStep() &&
            (step.getAction().contains("execute") || step.getAction().contains("storeResult"))
        );
    }

    private boolean isProtractorjsStep(TestStep step) {
        return step != null && (step.getAction().contains("protractor_customSpec"));
    }

    private boolean isRestWebservicePostStep(TestStep step) {
        return (
            step != null &&
            step.isWebserviceStep() &&
            (
                step.getAction().contains("postRest") ||
                step.getAction().contains("putRest") ||
                step.getAction().contains("patchRest") ||
                step.getAction().contains("deleteWithPayload")
            )
        );
    }

    private boolean isSetEndPointStep(TestStep step) {
        return (
            step != null && step.isWebserviceStep() && (step.getAction().contains("setEndPoint"))
        );
    }

    private boolean isSOAPWebservicePostStep(TestStep step) {
        return step != null && step.isWebserviceStep() && step.getAction().contains("postSoap");
    }

    private boolean isFileStep(TestStep step) {
        return step != null && step.isFileStep() && step.getAction().contains("populateData");
    }

    private boolean isMessageStep(TestStep step) {
        return (
            step != null &&
            step.isMessageStep() &&
            (step.getAction().contains("setText") || step.getAction().contains("produceMessage"))
        );
    }

    private boolean isRouteFulfillEndpointStep(TestStep step) {
        return (
            step != null &&
            step.isBrowserStep() &&
            (step.getAction().contains("RouteFulfillEndpoint"))
        );
    }

    private boolean isRouteFulfillSetBodyStep(TestStep step) {
        return (
            step != null && step.isBrowserStep() && step.getAction().contains("RouteFulfillSetBody")
        );
    }

    private boolean isStringOperationsStep(TestStep step) {
        return step != null && step.isStringOperationsStep();
    }

    class ConditionAutoSuggest extends AutoSuggest {

        private List getConditionBasedOnText(String value) {
            String objectName = Objects.toString(
                table.getValueAt(table.getSelectedRow(), ObjectName.getIndex()),
                ""
            );
            if ("Webservice".equals(objectName)) {
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

        @Override
        public void setSelectedItem(Object o) {
            if (o instanceof String) {
                String selected = (String) o;
                if (selected.startsWith("[Project]") || selected.startsWith("[Shared]")) {
                    String scopeToken = selected.startsWith("[Shared]") ? "[Shared]" : "[Project]";
                    String actionValue = removeScopePrefix(selected);
                    if (
                        table.getSelectedRow() >= 0 &&
                        "Execute".equalsIgnoreCase(
                                Objects.toString(
                                    table.getValueAt(table.getSelectedRow(), ObjectName.getIndex()),
                                    ""
                                )
                            )
                    ) {
                        table.setValueAt(scopeToken, table.getSelectedRow(), Reference.getIndex());
                    }
                    super.setSelectedItem(actionValue);
                    return;
                }
            }
            super.setSelectedItem(o);
        }

        /**
         * Retrieves available actions for the currently selected object in the test design table.
         * <p>
         * Returns action methods appropriate for the object type (Browser, Database, Webservice, etc.)
         * or reusable components for Execute objects. For unrecognized object names, attempts to
         * resolve them as custom types, web objects, or mobile objects from the object repository.
         * </p>
         *
         * @return list of action method names for the selected object, or an empty list if unrecognized
         * @see MethodInfoManager#getMethodListFor(String...)
         * @see ObjectTypeUtil#isKnownType(String)
         */
        private List<String> getActionBasedOnObject() {
            String objectName = Objects.toString(
                table.getValueAt(table.getSelectedRow(), ObjectName.getIndex()),
                ""
            );
            String pageToken = Objects.toString(
                table.getValueAt(table.getSelectedRow(), Reference.getIndex()),
                ""
            );

            if ("Execute".equalsIgnoreCase(objectName)) {
                return getReusables();
            }

            if ("Browser".equals(objectName)) {
                return MethodInfoManager.getMethodListFor(ObjectType.BROWSER, ObjectType.ANY);
            }

            if (ObjectTypeUtil.isKnownType(objectName)) {
                return MethodInfoManager.getMethodListFor(objectName);
            }

            if (isWebObject(objectName, pageToken)) {
                return MethodInfoManager.getMethodListFor(
                    ObjectType.PLAYWRIGHT,
                    ObjectType.WEB,
                    ObjectType.ANY
                );
            }

            if (isMobileObject(objectName, pageToken)) {
                return MethodInfoManager.getMethodListFor(ObjectType.APP);
            }

            if (isStructuredDataObject(objectName, pageToken)) {
                return MethodInfoManager.getMethodListFor(ObjectType.STRUCTUREDDATA);
            }

            if (isSapObject(objectName, pageToken)) {
                return MethodInfoManager.getMethodListFor(ObjectType.SAP);
            }

            return new ArrayList<>();
        }

        private List<String> getReusables() {
            List<String> reusableList = new ArrayList<>();

            // Add [Project] scoped reusables
            for (Scenario scenario : sProject.getReusableScenarios()) {
                for (TestCase testCase : scenario.getTestCases()) {
                    reusableList.add("[Project] " + scenario.getName() + ":" + testCase.getName());
                }
            }

            // Add [Shared] scoped reusables
            for (Scenario scenario : sProject.getSharedScenarios()) {
                for (TestCase testCase : scenario.getTestCases()) {
                    reusableList.add("[Shared] " + scenario.getName() + ":" + testCase.getName());
                }
            }

            return reusableList;
        }

        private boolean isWebObject(String objectName, String pageToken) {
            if (
                pageToken == null ||
                pageToken.isBlank() ||
                objectName == null ||
                objectName.isBlank()
            ) {
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
            if (
                pageToken == null ||
                pageToken.isBlank() ||
                objectName == null ||
                objectName.isBlank()
            ) {
                return false;
            }
            var repo = sProject.getObjectRepository();
            ResolvedMobileObject.PageRef ref = ResolvedMobileObject.PageRef.parse(pageToken);
            ResolvedMobileObject r = (ref != null && ref.name != null && ref.scope != null)
                ? repo.resolveMobileObject(ref, objectName)
                : repo.resolveMobileObjectWithScope(pageToken, objectName);
            return r != null && r.isPresent();
        }

        /**
         * Detect Structured Data objects via Structured Data resolver (supports scoped refs + shared)
         * instead of directly accessing getStructuredDataOR()/pages.
         */
        private boolean isStructuredDataObject(String objectName, String pageToken) {
            if (
                pageToken == null ||
                pageToken.isBlank() ||
                objectName == null ||
                objectName.isBlank()
            ) {
                return false;
            }
            var repo = sProject.getObjectRepository();
            ResolvedStructuredDataObject.PageRef ref = ResolvedStructuredDataObject.PageRef.parse(
                pageToken
            );
            ResolvedStructuredDataObject r = (ref != null && ref.name != null && ref.scope != null)
                ? repo.resolveStructuredDataObject(ref, objectName)
                : repo.resolveStructuredDataObjectWithScope(pageToken, objectName);
            return r != null && r.isPresent();
        }

        /**
         * Detect SAP objects via SAP resolver (supports scoped refs + shared)
         * instead of directly accessing getSapOR()/pages.
         */
        private boolean isSapObject(String objectName, String pageToken) {
            if (
                pageToken == null ||
                pageToken.isBlank() ||
                objectName == null ||
                objectName.isBlank()
            ) {
                return false;
            }
            var repo = sProject.getObjectRepository();
            ResolvedSapObject.PageRef ref = ResolvedSapObject.PageRef.parse(pageToken);
            ResolvedSapObject r = (ref != null && ref.name != null && ref.scope != null)
                ? repo.resolveSapObject(ref, objectName)
                : repo.resolveSapObjectWithScope(pageToken, objectName);
            return r != null && r.isPresent();
        }

        @Override
        public void beforeSearch(String text) {
            setSearchList(getActionBasedOnObject());
        }

        @Override
        public void afterReset() {
            String actionText = getText();
            if (actionText.startsWith("[Project]") || actionText.startsWith("[Shared]")) {
                String scopeToken = actionText.startsWith("[Shared]") ? "[Shared]" : "[Project]";
                String actionValue = removeScopePrefix(actionText);
                table.setValueAt(scopeToken, table.getSelectedRow(), Reference.getIndex());
                table.setValueAt(actionValue, table.getSelectedRow(), Action.getIndex());
            }
            // Set description if empty
            String val = Objects.toString(
                table.getValueAt(table.getSelectedRow(), Description.getIndex()),
                ""
            );
            if (val.trim().isEmpty()) {
                String desc = MethodInfoManager.getDescriptionFor(getText());
                table.setValueAt(desc, table.getSelectedRow(), Description.getIndex());
            }
        }
    }

    class InputAutoSuggest extends InputMainAutoSuggest {
        Boolean isPending = false;
        private String prevText;

        public InputAutoSuggest() {
            super.setTable(TestCaseAutoSuggest.this.table);
        }

        private List getInputBasedOnText(String value) {
            if (value.startsWith("%")) {
                return getUserDefinedList();
            } else if (value.startsWith("=")) {
                return getFunctionList();
            } else if (value.startsWith("#")) {
                return getDatabaseAliasList();
            }
            return setupTestData(value);
        }

        public List getUserDefinedList() {
            Set udSet = sProject
                .getProjectSettings()
                .getUserDefinedSettings()
                .stringPropertyNames();
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
            sProject
                .getTestData()
                .getAllEnvironments()
                .stream()
                .forEach(
                    sTestData -> {
                        for (TestDataModel stdList : sTestData.getTestDataList()) {
                            tdList.add(stdList.getName());
                        }
                    }
                );
            tdList
                .stream()
                .forEach(
                    string -> {
                        List tdCols = setupTestData(string + ":");
                        tdCols
                            .stream()
                            .forEach(
                                tdCol -> {
                                    retList.add(string + ":" + tdCol);
                                }
                            );
                    }
                );
            return retList;
        }

        @Override
        public void setSelectedItem(Object o) {
            boolean isStringOpsEditor = isStringOpsEditor();
            if (
                o != null &&
                !o.toString().matches("(@.+)\n(=.+)\n(%.+%)") &&
                !o.toString().contains(":")
            ) {
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
            if (
                !getText().isEmpty() &&
                !getText().matches("^[\\%\\@].*") &&
                !getText().contains(":")
            ) {
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
            boolean isActionClicked = table.columnAtPoint(me.getPoint()) == Action.getIndex();

            if (me.isAltDown()) {
                if (table.rowAtPoint(me.getPoint()) != -1 && getTestCase(table) != null) {
                    TestStep step = getTestCase(table)
                        .getTestSteps()
                        .get(table.rowAtPoint(me.getPoint()));
                    if (
                        (
                            isDataBaseQueryStep(step) &&
                            table.columnAtPoint(me.getPoint()) == Input.getIndex()
                        ) ||
                        (
                            isProtractorjsStep(step) &&
                            table.columnAtPoint(me.getPoint()) == Input.getIndex()
                        )
                    ) {
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
            // Double-click on reusable steps allows editing; navigation is disabled
            // Use context menu "Go To Reusable" for navigation instead
        }
    }

    private String removeScopePrefix(String text) {
        if (text == null) {
            return "";
        }
        if (text.startsWith("[Project]")) {
            return text.substring("[Project]".length()).trim();
        }
        if (text.startsWith("[Shared]")) {
            return text.substring("[Shared]".length()).trim();
        }
        return text;
    }

    public List getInputs() {
        List auto = inputAutoSuggest.getUserDefinedList();
        auto.addAll(inputAutoSuggest.getTestData());
        return auto;
    }

    class MouseMotionAdapterImpl extends MouseMotionAdapter {
        private Point hintCell = new Point(-1, -1);
        private Timer showTimer;
        private Timer hideTimer;

        private JPopupMenu popup;
        private JMenuItem actionItem;
        private TestStep currentStep;

        public MouseMotionAdapterImpl() {
            popup = new JPopupMenu();
            actionItem = new JMenuItem();
            popup.add(actionItem);

            actionItem.addActionListener(
                (ActionEvent ae) -> {
                    if (currentStep == null) return;

                    if (isProtractorjsStep(currentStep) || isDataBaseQueryStep(currentStep)) {
                        new SQLTextArea(null, currentStep, getInputs());
                    } else if (
                        isRestWebservicePostStep(currentStep) ||
                        (
                            currentStep.isWebserviceStep() &&
                            currentStep.getAction().contains("postSoap")
                        ) ||
                        (
                            currentStep.isBrowserStep() &&
                            currentStep.getAction().contains("RouteFulfillSetBody")
                        )
                    ) {
                        new WebservicePayloadArea(
                            null,
                            currentStep,
                            isRestWebservicePostStep(currentStep) ? "REST" : "SOAP",
                            getInputs()
                        );
                    } else if (
                        isSetEndPointStep(currentStep) || isRouteFulfillEndpointStep(currentStep)
                    ) {
                        new EndPointTextArea(null, currentStep, getInputs());
                    } else if (isFileStep(currentStep) || isMessageStep(currentStep)) {
                        new WebservicePayloadArea(null, currentStep, "SOAP", getInputs());
                    } else if (isStringOperationsStep(currentStep)) {
                        new StringOperationsPayloadArea(null, currentStep, getInputs());
                    }

                    hidePopupNow();
                }
            );

            showTimer =
                new Timer(
                    650,
                    (ActionEvent ae) -> {
                        if (hintCell.x != -1 && hintCell.y != -1) {
                            Rectangle bounds = table.getCellRect(hintCell.y, hintCell.x, true);
                            popup.show(table, bounds.x, bounds.y + bounds.height);
                        }
                    }
                );
            showTimer.setRepeats(false);

            hideTimer =
                new Timer(
                    250,
                    (ActionEvent ae) -> {
                        hidePopupNow();
                    }
                );
            hideTimer.setRepeats(false);

            MouseAdapter popupMouseAdapter = new MouseAdapter() {

                @Override
                public void mouseEntered(MouseEvent e) {
                    cancelHideTimer();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    scheduleHidePopup();
                }
            };

            popup.addMouseListener(popupMouseAdapter);
            actionItem.addMouseListener(popupMouseAdapter);

            table.addMouseListener(
                new MouseAdapter() {

                    @Override
                    public void mouseExited(MouseEvent e) {
                        if (isMouseOverPopup(e)) {
                            cancelHideTimer();
                            return;
                        }

                        scheduleHidePopup();
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        cancelHideTimer();
                    }
                }
            );
        }

        private void scheduleHidePopup() {
            showTimer.stop();
            hideTimer.restart();
        }

        private void cancelHideTimer() {
            if (hideTimer != null) {
                hideTimer.stop();
            }
        }

        private void hidePopupNow() {
            showTimer.stop();
            cancelHideTimer();

            if (popup.isVisible()) {
                popup.setVisible(false);
            }

            hintCell = new Point(-1, -1);
        }

        private boolean isMouseOverPopup(MouseEvent e) {
            if (!popup.isVisible() || !popup.isShowing()) {
                return false;
            }

            try {
                Rectangle popupRect = new Rectangle(popup.getLocationOnScreen(), popup.getSize());
                popupRect.grow(4, 4);
                return popupRect.contains(e.getLocationOnScreen());
            } catch (Exception ex) {
                return false;
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            cancelHideTimer();

            if (isMouseOverPopup(e)) {
                return;
            }

            Point p = e.getPoint();
            int row = table.rowAtPoint(p);
            int col = table.columnAtPoint(p);

            if (row == hintCell.y && col == hintCell.x) {
                if (!popup.isVisible()) {
                    showTimer.restart();
                }

                return;
            }

            if (row != -1 && col == Input.getIndex() && getTestCase(table) != null) {
                currentStep = getTestCase(table).getTestSteps().get(row);

                if (updateMenuTextForStep(currentStep)) {
                    cancelHideTimer();

                    if (hintCell.x != col || hintCell.y != row) {
                        hidePopupNow();
                        hintCell = new Point(col, row);
                        cancelHideTimer();
                        showTimer.restart();
                    }

                    return;
                }
            }

            scheduleHidePopup();
        }

        private boolean updateMenuTextForStep(TestStep step) {
            if (isDataBaseQueryStep(step)) {
                actionItem.setText("Click to Open SQL Query Editor");
                return true;
            } else if (isProtractorjsStep(step)) {
                actionItem.setText("Click to open ProtractorJS command editor");
                return true;
            } else if (
                isSOAPWebservicePostStep(step) ||
                isRestWebservicePostStep(step) ||
                isSetEndPointStep(step) ||
                isRouteFulfillEndpointStep(step) ||
                isRouteFulfillSetBodyStep(step)
            ) {
                actionItem.setText("Click to Open Webservice Editor");
                return true;
            } else if (isFileStep(step)) {
                actionItem.setText("Click to Open File Editor");
                return true;
            } else if (isMessageStep(step)) {
                actionItem.setText("Click to Open Message Editor");
                return true;
            } else if (isStringOperationsStep(step)) {
                actionItem.setText("Click to Open String Operations Editor");
                return true;
            }

            return false;
        }
    }
}
