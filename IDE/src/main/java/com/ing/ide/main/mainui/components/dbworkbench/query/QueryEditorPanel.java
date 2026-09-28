package com.ing.ide.main.mainui.components.dbworkbench.query;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.dbworkbench.DBQuery;
import com.ing.ide.main.mainui.components.dbworkbench.DBWorkbench;
import com.ing.ide.main.mainui.components.dbworkbench.DBWorkbenchUI;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * Top pane of the Database Workbench: connection selector, SQL editor, and the
 * Run / Save / {@code ⇢ Automation} toolbar. Mirrors the API Workbench
 * {@code RequestPanel} (URL bar + body + Automation button).
 */
public class QueryEditorPanel extends JPanel {
    private static final String TYPE_TEST_CASE = "Test Case";
    private static final String TYPE_USER_INTENT = "User Intent (Reusable)";

    private final DBWorkbenchUI parent;
    private final JComboBox<String> connectionCombo = new JComboBox<>();
    private final JTextField nameField = new JTextField("Query 1");
    private final RSyntaxTextArea editor = new RSyntaxTextArea(10, 60);
    private final DefaultCompletionProvider completionProvider = new DefaultCompletionProvider();
    private final java.util.Set<String> knownCompletions = new java.util.HashSet<>();

    private static final String[] SQL_KEYWORDS = {
        "SELECT",
        "FROM",
        "WHERE",
        "INSERT INTO",
        "UPDATE",
        "DELETE FROM",
        "VALUES",
        "SET",
        "JOIN",
        "LEFT JOIN",
        "INNER JOIN",
        "GROUP BY",
        "ORDER BY",
        "HAVING",
        "LIMIT",
        "DISTINCT",
        "COUNT",
        "AND",
        "OR",
        "NOT",
        "NULL",
        "AS",
        "ON"
    };

    public QueryEditorPanel(DBWorkbenchUI parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        add(buildToolbar(), BorderLayout.NORTH);

        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
        editor.setCodeFoldingEnabled(true);
        editor.setText("SELECT * FROM ");
        add(new RTextScrollPane(editor), BorderLayout.CENTER);

        for (String kw : SQL_KEYWORDS) {
            completionProvider.addCompletion(new BasicCompletion(completionProvider, kw));
        }
        AutoCompletion ac = new AutoCompletion(completionProvider);
        ac.setAutoActivationEnabled(true);
        ac.install(editor);
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        bar.setBorder(new EmptyBorder(2, 4, 2, 4));

        bar.add(new JLabel("Connection:"));
        connectionCombo.setPreferredSize(new Dimension(180, 28));
        bar.add(connectionCombo);

        bar.add(new JLabel("Name:"));
        nameField.setPreferredSize(new Dimension(160, 28));
        bar.add(nameField);

        JButton runBtn = new JButton("Run ▶");
        runBtn.setToolTipText("Execute the SQL, or just the selected text");
        runBtn.addActionListener(e -> parent.runCurrentQuery());
        bar.add(runBtn);

        JButton saveBtn = new JButton("Save");
        saveBtn.setToolTipText("Save this query in the project");
        saveBtn.addActionListener(e -> parent.saveCurrentQuery());
        bar.add(saveBtn);

        JButton openBtn = new JButton("Open");
        openBtn.setToolTipText("Open a saved query");
        openBtn.addActionListener(e -> parent.openSavedQuery());
        bar.add(openBtn);

        JButton commitBtn = new JButton("Commit");
        commitBtn.setToolTipText("Commit the current transaction on this connection");
        commitBtn.addActionListener(e -> parent.commitCurrent());
        bar.add(commitBtn);

        JButton rollbackBtn = new JButton("Rollback");
        rollbackBtn.setToolTipText("Roll back the current transaction on this connection");
        rollbackBtn.addActionListener(e -> parent.rollbackCurrent());
        bar.add(rollbackBtn);

        JButton automationBtn = new JButton("⇢ Automation");
        automationBtn.setToolTipText("Convert to INGenious Test Case or User Intent (Reusable)");
        automationBtn.addActionListener(e -> showConvertToAutomationDialog());
        bar.add(automationBtn);

        return bar;
    }

    public void refreshConnections(List<String> aliases) {
        Object selected = connectionCombo.getSelectedItem();
        connectionCombo.setModel(new DefaultComboBoxModel<>(aliases.toArray(new String[0])));
        if (selected != null && aliases.contains(selected.toString())) {
            connectionCombo.setSelectedItem(selected);
        }
    }

    public String getSelectedAlias() {
        Object a = connectionCombo.getSelectedItem();
        return a == null ? null : a.toString();
    }

    public String getSql() {
        String selected = editor.getSelectedText();
        String sql = (selected != null && !selected.trim().isEmpty()) ? selected : editor.getText();
        sql = sql.trim();
        while (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        return sql;
    }

    /** Adds table/column identifiers discovered by the schema browser to autocomplete. */
    public void addSchemaCompletions(List<String> identifiers) {
        if (identifiers == null) return;
        for (String id : identifiers) {
            if (id != null && knownCompletions.add(id)) {
                completionProvider.addCompletion(new BasicCompletion(completionProvider, id));
            }
        }
    }

    public String getQueryName() {
        String n = nameField.getText().trim();
        return n.isEmpty() ? "Query" : n;
    }

    public void setSql(String sql) {
        editor.setText(sql);
    }

    /** Loads a saved query into the editor (name, connection, SQL). */
    public void loadQuery(DBQuery q) {
        if (q == null) return;
        nameField.setText(q.getName() == null ? "Query" : q.getName());
        if (q.getConnectionAlias() != null) {
            connectionCombo.setSelectedItem(q.getConnectionAlias());
        }
        editor.setText(q.getSql() == null ? "" : q.getSql());
    }

    /** True when the statement looks like DML (INSERT/UPDATE/DELETE/MERGE). */
    public boolean isDml() {
        String sql = getSql().toUpperCase();
        return (
            sql.startsWith("INSERT") ||
            sql.startsWith("UPDATE") ||
            sql.startsWith("DELETE") ||
            sql.startsWith("MERGE")
        );
    }

    /** Builds a {@link DBQuery} from the current editor state + grid validations. */
    public DBQuery buildQuery() {
        DBQuery q = new DBQuery(getQueryName());
        q.setConnectionAlias(getSelectedAlias());
        q.setSql(getSql());
        q.setDml(isDml());
        q.setValidations(new java.util.ArrayList<>(parent.getResultPanel().getValidations()));
        return q;
    }

    private void showConvertToAutomationDialog() {
        DBWorkbench controller = parent.getController();
        if (getSelectedAlias() == null) {
            JOptionPane.showMessageDialog(
                this,
                "Select a database connection first.",
                "No Connection",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        if (getSql().isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Enter a SQL query first.",
                "No SQL",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        final List<Scenario> testPlanScenarios = controller.getAvailableScenarios();
        final List<Scenario> reusableScenarios = controller.getAvailableReusableScenarios();
        if (testPlanScenarios.isEmpty() && reusableScenarios.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "No scenarios available. Please open a project and create a scenario first.",
                "No Scenarios",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        JPanel panel = new JPanel(new java.awt.GridLayout(3, 2, 10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Automation Type:"));
        final JComboBox<String> typeCombo = new JComboBox<>(
            new String[] { TYPE_TEST_CASE, TYPE_USER_INTENT }
        );
        panel.add(typeCombo);

        panel.add(new JLabel("Target Scenario:"));
        final JComboBox<Scenario> scenarioCombo = new JComboBox<>();
        scenarioCombo.setRenderer(
            new javax.swing.DefaultListCellRenderer() {

                @Override
                public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
                ) {
                    super.getListCellRendererComponent(
                        list,
                        value,
                        index,
                        isSelected,
                        cellHasFocus
                    );
                    if (value instanceof Scenario) {
                        setText(((Scenario) value).getName());
                    }
                    return this;
                }
            }
        );
        panel.add(scenarioCombo);

        final JLabel nameLabel = new JLabel("Test Case Name:");
        panel.add(nameLabel);
        final JTextField tcNameField = new JTextField(getQueryName());
        panel.add(tcNameField);

        typeCombo.addActionListener(
            e -> {
                boolean reusable = TYPE_USER_INTENT.equals(typeCombo.getSelectedItem());
                List<Scenario> list = reusable ? reusableScenarios : testPlanScenarios;
                scenarioCombo.setModel(new DefaultComboBoxModel<>(list.toArray(new Scenario[0])));
                nameLabel.setText(reusable ? "User Intent Name:" : "Test Case Name:");
            }
        );
        scenarioCombo.setModel(
            new DefaultComboBoxModel<>(testPlanScenarios.toArray(new Scenario[0]))
        );

        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Convert to Automation",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) return;

        boolean reusable = TYPE_USER_INTENT.equals(typeCombo.getSelectedItem());
        Scenario scenario = (Scenario) scenarioCombo.getSelectedItem();
        if (scenario == null) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a scenario.",
                "No Scenario",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        String tcName = tcNameField.getText().trim();
        if (tcName.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Please enter a name.",
                "Invalid Name",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        DBQuery query = buildQuery();
        TestCase created = reusable
            ? controller.convertQueryToReusable(query, scenario, tcName)
            : controller.convertQueryToTestCase(query, scenario, tcName);

        if (created != null) {
            int nav = JOptionPane.showConfirmDialog(
                this,
                "Successfully created " +
                (reusable ? "user intent '" : "test case '") +
                tcName +
                "' in scenario '" +
                scenario.getName() +
                "'.\n\nWould you like to open it in Test Design?",
                "Conversion Successful",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
            );
            if (nav == JOptionPane.YES_OPTION) {
                controller.navigateToTestCase(created);
            }
        } else {
            JOptionPane.showMessageDialog(
                this,
                "Failed to convert query to automation. Check the logs for details.",
                "Conversion Failed",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
