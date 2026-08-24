package com.ing.ide.main.mainui.components.testdesign.testdata;

import com.ing.datalib.component.Project;
import com.ing.engine.util.data.fx.FParser;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.table.ReadOnlyCellEditor;
import com.ing.ide.main.utils.table.autosuggest.AutoSuggest;
import com.ing.ide.main.utils.table.autosuggest.AutoSuggestCellEditor;
import com.ing.ide.main.utils.table.autosuggest.ComboSeparatorsRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

/**
 *
 *
 */
public class TestDataAutoSuggest {
    private final Project sProject;
    private final JTable table;

    private AutoSuggest scenarioSugg;
    private AutoSuggest testCaseSugg;
    private AutoSuggest functionSugg;

    public TestDataAutoSuggest(Project sProject, JTable table) {
        this.sProject = sProject;
        this.table = table;
        init();
    }

    private void init() {
        functionSugg =
            new AutoSuggest() {

                @Override
                public void beforeSearch(String text) {
                    if (text.startsWith("=")) {
                        setSearchList(getFunctionList());
                    } else {
                        clearSearchList();
                    }
                }

                private List<String> getFunctionList() {
                    List<String> functions = new ArrayList<>();
                    for (String func : FParser.getFuncList()) {
                        functions.add("=".concat(func));
                    }
                    return functions;
                }
            }
            .withOnHide(stopEditingOnFocusLost());
        scenarioSugg = new AutoSuggest().withOnHide(stopEditingOnFocusLost());
        // Use ComboSeparatorsRenderer to show headers and color-code shared/project entries
        scenarioSugg.setRenderer(
            (ListCellRenderer) new ComboSeparatorsRenderer(scenarioSugg.getRenderer()) {

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
                    String raw = removeOrderingPrefix(value.toString());
                    // show scenario name without scope prefix in the dropdown
                    lbl.setText(
                        raw.startsWith("[Project]") ||
                            raw.startsWith("[Shared]") ||
                            raw.startsWith("[TestPlan]")
                            ? raw.substring(raw.indexOf(' ') + 1)
                            : raw
                    );
                    if (raw.startsWith("[Shared]")) {
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
                    } else if (raw.startsWith("[TestPlan]")) {
                        // Test plan entries use default colors but keep selection opaque
                        if (isSelected) {
                            lbl.setOpaque(true);
                        }
                        lbl.setForeground(Color.DARK_GRAY);
                    }
                }

                @Override
                protected boolean addHeaderBefore(JList list, Object value, int index) {
                    if (value == null) return false;
                    String current = removeOrderingPrefix(value.toString());
                    if (current.startsWith("[TestPlan]")) {
                        if (index == 0) return true;
                        String prev = Objects.toString(list.getModel().getElementAt(index - 1), "");
                        if (prev.matches("^[123]-.*")) {
                            prev = prev.substring(2);
                        }
                        return !prev.startsWith("[TestPlan]");
                    }
                    if (current.startsWith("[Project]")) {
                        if (index == 0) return true;
                        String prev = removeOrderingPrefix(
                            Objects.toString(list.getModel().getElementAt(index - 1), "")
                        );
                        return !prev.startsWith("[Project]");
                    }
                    if (current.startsWith("[Shared]")) {
                        if (index == 0) return true;
                        String prev = removeOrderingPrefix(
                            Objects.toString(list.getModel().getElementAt(index - 1), "")
                        );
                        return !prev.startsWith("[Shared]");
                    }
                    return false;
                }

                @Override
                protected boolean addSeparatorBefore(JList list, Object value, int index) {
                    if (value == null) return false;
                    String current = removeOrderingPrefix(value.toString());
                    // Add a separator before the TestPlan group (i.e., when first TestPlan item appears)
                    if (current.startsWith("[TestPlan]")) {
                        if (index == 0) return true;
                        String prev = removeOrderingPrefix(
                            Objects.toString(list.getModel().getElementAt(index - 1), "")
                        );
                        return !prev.startsWith("[TestPlan]");
                    }
                    return false;
                }

                @Override
                protected String getHeaderLabel(JList list, Object value, int index) {
                    if (value == null) return "";
                    String current = removeOrderingPrefix(value.toString());
                    if (current.startsWith("[TestPlan]")) return "Test Plan";
                    if (current.startsWith("[Project]")) return "Project Reusable Components";
                    if (current.startsWith("[Shared]")) return "Shared Reusable Components";
                    return "";
                }

                @Override
                protected Color getHeaderForeground(
                    JList list,
                    Object value,
                    int index,
                    java.awt.Component comp
                ) {
                    if (value == null) return Color.DARK_GRAY;
                    String current = removeOrderingPrefix(value.toString());
                    if (current.startsWith("[Shared]")) return new Color(0, 128, 0);
                    if (current.startsWith("[TestPlan]")) return Color.DARK_GRAY;
                    return Color.BLACK;
                }

                @Override
                protected boolean addSeparatorAfter(JList list, Object value, int index) {
                    if (value == null) return false;
                    String val = removeOrderingPrefix(value.toString());
                    if (index < list.getModel().getSize() - 1) {
                        Object nextValue = list.getModel().getElementAt(index + 1);
                        if (nextValue != null) {
                            String next = removeOrderingPrefix(nextValue.toString());
                            // separator between TestPlan -> Project and Project -> Shared
                            if (
                                val.startsWith("[TestPlan]") && next.startsWith("[Project]")
                            ) return true;
                            if (
                                val.startsWith("[Project]") && next.startsWith("[Shared]")
                            ) return true;
                        }
                    }
                    return false;
                }
            }
        );
        // When a scenario is selected from the autosuggest, populate the Scope column and apply row coloring
        scenarioSugg.addActionListener(
            (ActionListener) ae -> {
                Object sel = scenarioSugg.getSelectedItem();
                String s = removeOrderingPrefix(Objects.toString(sel, "").trim());
                // If a header was selected, try to move selection to the next real item
                if (s.startsWith("__HEADER__:")) {
                    // find next non-header item in the popup model
                    for (int i = 0; i < scenarioSugg.getItemCount(); i++) {
                        String it = removeOrderingPrefix(
                            Objects.toString(scenarioSugg.getItemAt(i), "")
                        );
                        if (!it.startsWith("__HEADER__:")) {
                            scenarioSugg.setSelectedIndex(i);
                            sel = scenarioSugg.getSelectedItem();
                            s = removeOrderingPrefix(Objects.toString(sel, "").trim());
                            break;
                        }
                    }
                }
                if (s.isEmpty() || s.startsWith("__HEADER__:")) {
                    return;
                }
                String scopeVal = extractScope(s);
                String normalized = normalizeName(s);
                int row = table.getSelectedRow();
                if (row != -1) {
                    try {
                        // determine current normalized scenario to compare
                        Object currentObj = table.getModel().getValueAt(row, 0);
                        String currentStr = Objects.toString(currentObj, "");
                        String currentNormalized = normalizeName(currentStr);

                        // set Scenario cell to the selected normalized name (without prefix)
                        table.getModel().setValueAt(normalized, row, 0);
                        // set Scope: clear for TestPlan, set for Project/Shared
                        try {
                            if (s.startsWith("[TestPlan] ")) {
                                table.getModel().setValueAt("", row, 2);
                            } else if (!scopeVal.isEmpty()) {
                                table.getModel().setValueAt(scopeVal, row, 2);
                            }
                        } catch (Exception ex) {
                            // ignore if scope column not present
                        }

                        // if a different scenario was selected, reset Flow column to 'no selected'
                        if (!normalized.equals(currentNormalized)) {
                            try {
                                table.getModel().setValueAt("", row, 1);
                            } catch (Exception ex) {
                                // ignore if flow column not present
                            }
                        }
                    } catch (Exception ex) {
                        // ignore model errors
                    }
                }
            }
        );
        // install a default renderer once to color full row when scope is [Shared]
        table.setDefaultRenderer(
            Object.class,
            new DefaultTableCellRenderer() {

                @Override
                public Component getTableCellRendererComponent(
                    javax.swing.JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
                ) {
                    Component c = super.getTableCellRendererComponent(
                        table,
                        value,
                        isSelected,
                        hasFocus,
                        row,
                        column
                    );
                    try {
                        Object sc = table.getModel().getValueAt(row, 2);
                        if (Objects.toString(sc, "").equals("[Shared]")) {
                            c.setForeground(new Color(0, 128, 0));
                        } else {
                            c.setForeground(Color.BLACK);
                        }
                    } catch (Exception ex) {
                        // ignore out-of-bounds or model errors
                    }
                    return c;
                }
            }
        );
        testCaseSugg = new AutoSuggest().withOnHide(stopEditingOnFocusLost());
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

    /**
     * Removes the ordering prefix (1-, 2-, or 3-) if present.
     * Example: "2-[Project] Scenario" → "[Project] Scenario"
     */
    private String removeOrderingPrefix(String value) {
        return value != null && value.matches("^[123]-.*") ? value.substring(2) : value;
    }

    /**
     * Extracts the scope label ([Project], [Shared], or [TestPlan]) from a prefixed string.
     * Example: "2-[Project] Scenario" → "[Project]"
     * Returns empty string if no scope prefix found.
     */
    private String extractScope(String value) {
        String normalized = removeOrderingPrefix(value);
        if (normalized.startsWith("[Project] ")) return "[Project]";
        if (normalized.startsWith("[Shared] ")) return "[Shared]";
        if (normalized.startsWith("[TestPlan] ")) return "[TestPlan]";
        return "";
    }

    /**
     * Normalizes a string by removing both ordering prefix and scope label.
     * Example: "2-[Project] Scenario" → "Scenario"
     */
    private String normalizeName(String value) {
        String normalized = removeOrderingPrefix(value);
        if (normalized.startsWith("[Project] ")) return normalized.substring("[Project] ".length());
        if (normalized.startsWith("[Shared] ")) return normalized.substring("[Shared] ".length());
        if (normalized.startsWith("[TestPlan] ")) return normalized.substring(
            "[TestPlan] ".length()
        );
        return normalized;
    }

    private void updateScenarios() {
        List<String> allScenarios = new ArrayList<>();

        // Include test plan scenarios first, then project-scoped reusable scenarios, then shared-scoped
        List<String> testPlanList = new ArrayList<>();
        if (sProject.getScenarios() != null) {
            for (Object s : sProject.getScenarios()) {
                try {
                    // Scenario may be a domain object; attempt to get name via toString or reflection
                    String name = Objects.toString(s, "");
                    // If it's a Scenario object with getName, try to invoke it
                    try {
                        java.lang.reflect.Method m = s.getClass().getMethod("getName");
                        Object nm = m.invoke(s);
                        if (nm != null) name = nm.toString();
                    } catch (Exception ex) {
                        // ignore
                    }
                    if (!name.trim().isEmpty()) testPlanList.add(name);
                } catch (Exception ex) {
                    // ignore bad entries
                }
            }
        }
        Collections.sort(testPlanList, String.CASE_INSENSITIVE_ORDER);
        // Prefix with "1-" to ensure Test Plan appears first after alphabetical sort
        for (String sc : testPlanList) {
            allScenarios.add("1-[TestPlan] " + sc);
        }

        // Add Project-scoped Reusable Component Test Scenarios to the autosuggest list (prefixed)
        List<String> reusableScenarioList = Utils.asStringList(sProject.getReusableScenarios());
        Collections.sort(reusableScenarioList, String.CASE_INSENSITIVE_ORDER);
        if (!reusableScenarioList.isEmpty()) {
            // Prefix with "2-" to ensure Project appears second after alphabetical sort
            for (String sc : reusableScenarioList) {
                allScenarios.add("2-[Project] " + sc);
            }
        }

        // Add Shared-scoped Reusable Component Test Scenarios to the autosuggest list (prefixed)
        List<String> sharedReusableScenarioList = Utils.asStringList(sProject.getSharedScenarios());
        Collections.sort(sharedReusableScenarioList, String.CASE_INSENSITIVE_ORDER);
        if (!sharedReusableScenarioList.isEmpty()) {
            // Prefix with "3-" to ensure Shared appears third after alphabetical sort
            for (String sc : sharedReusableScenarioList) {
                allScenarios.add("3-[Shared] " + sc);
            }
        }

        scenarioSugg.setSearchList(allScenarios);
    }

    private void updateTestCases() {
        testCaseSugg.clearSearchList();
        if (table.getSelectedRow() != -1) {
            // Use model directly to get scenario value (column 0 in model)
            // This works regardless of whether we're in the fixed table or main table
            String scenario = Objects.toString(
                table.getModel().getValueAt(table.getSelectedRow(), 0),
                ""
            );
            if (!scenario.trim().isEmpty()) {
                Set<String> allTestCases = new LinkedHashSet<>();

                // Normalize scenario name by stripping known scope prefixes or test plan prefix
                String normalized = normalizeName(scenario);

                // Read scope column (index 2) to decide which source to use
                String scope = Objects.toString(
                    table.getModel().getValueAt(table.getSelectedRow(), 2),
                    ""
                );

                if ("[Project]".equals(scope)) {
                    // Project-scoped reusable testcases only
                    if (sProject.getReusableScenarioByName(normalized) != null) {
                        List<String> reusableTestCaseList = Utils.asStringList(
                            sProject.getReusableScenarioByName(normalized).getTestCases()
                        );
                        Collections.sort(reusableTestCaseList, String.CASE_INSENSITIVE_ORDER);
                        allTestCases.addAll(reusableTestCaseList);
                    }
                } else if ("[Shared]".equals(scope)) {
                    // Shared-scoped reusable testcases only
                    if (sProject.getSharedReusableScenarioByName(normalized) != null) {
                        List<String> sharedTestCaseList = Utils.asStringList(
                            sProject.getSharedReusableScenarioByName(normalized).getTestCases()
                        );
                        Collections.sort(sharedTestCaseList, String.CASE_INSENSITIVE_ORDER);
                        allTestCases.addAll(sharedTestCaseList);
                    }
                } else {
                    // Default to Test Plan scenarios
                    if (sProject.getScenarioByName(normalized) != null) {
                        List<String> testCaseList = Utils.asStringList(
                            sProject.getScenarioByName(normalized).getTestCases()
                        );
                        Collections.sort(testCaseList, String.CASE_INSENSITIVE_ORDER);
                        allTestCases.addAll(testCaseList);
                    }
                }

                testCaseSugg.setSearchList(new ArrayList<String>(allTestCases));
            }
        }
    }

    public TableCellEditor getCellEditorFor(int column, TableCellEditor cellEditor) {
        switch (column) {
            case 0:
                updateScenarios();
                return new AutoSuggestCellEditor(scenarioSugg);
            case 1:
                updateTestCases();
                return new AutoSuggestCellEditor(testCaseSugg);
            case 2:
                // Scope column (column 2) is read-only and auto-populated from Scenario/TestCase selection
                return new ReadOnlyCellEditor();
            case 3:
                return cellEditor;
            default:
                return new AutoSuggestCellEditor(functionSugg);
        }
    }
}
