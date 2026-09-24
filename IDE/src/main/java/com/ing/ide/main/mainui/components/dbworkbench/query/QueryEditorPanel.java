package com.ing.ide.main.mainui.components.dbworkbench.query;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.dbworkbench.DBQuery;
import com.ing.ide.main.mainui.components.dbworkbench.DBWorkbench;
import com.ing.ide.main.mainui.components.dbworkbench.DBWorkbenchUI;
import com.ing.ide.main.mainui.components.dbworkbench.util.SqlScript;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
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
        saveBtn.setToolTipText("Save the whole query in the project");
        saveBtn.addActionListener(e -> parent.saveCurrentQuery());
        bar.add(saveBtn);

        JButton openBtn = new JButton("Open");
        openBtn.setToolTipText("Open a saved query");
        openBtn.addActionListener(e -> parent.openSavedQuery());
        bar.add(openBtn);

        JButton importBtn = new JButton("Import SQL");
        importBtn.setToolTipText("Load a .sql file into the editor");
        importBtn.addActionListener(e -> importSqlFile());
        bar.add(importBtn);

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

    /** Replaces the editor content with a {@code .sql} file from disk. */
    private void importSqlFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import SQL File");
        chooser.setFileFilter(new FileNameExtensionFilter("SQL files (*.sql)", "sql"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        java.io.File file = chooser.getSelectedFile();
        try {
            String sql = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            editor.setText(sql);
            editor.setCaretPosition(0);
            nameField.setText(stripExtension(file.getName()));
            parent
                .getResultPanel()
                .showMessage(
                    "Imported " +
                    file.getName() +
                    " (" +
                    SqlScript.split(sql).size() +
                    " statement(s))."
                );
        } catch (IOException ex) {
            parent
                .getResultPanel()
                .showError("Could not read " + file.getName() + ": " + ex.getMessage());
        }
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
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

    /**
     * Points the editor at a connection, e.g. after a table is picked in the
     * schema browser, so Run targets the database the table came from.
     *
     * @param alias the connection alias to select
     */
    public void setSelectedAlias(String alias) {
        if (alias == null) return;
        for (int i = 0; i < connectionCombo.getItemCount(); i++) {
            if (alias.equals(connectionCombo.getItemAt(i))) {
                connectionCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * The full script as typed, semicolons included. Saving and automation use
     * this so nothing is silently dropped.
     *
     * @return the editor content, trimmed
     */
    public String getSql() {
        return editor.getText().trim();
    }

    /**
     * @return the selected text when there is a selection, otherwise the whole
     *         script; this is what Run executes
     */
    public String getSqlToRun() {
        String selected = editor.getSelectedText();
        return (selected != null && !selected.trim().isEmpty())
            ? selected.trim()
            : editor.getText().trim();
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

    /** True when the script contains at least one statement that writes. */
    public boolean isDml() {
        for (String statement : SqlScript.split(getSql())) {
            if (SqlScript.classify(statement).isWrite()) {
                return true;
            }
        }
        return false;
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
        if (controller.getMainFrame().getProject() == null) {
            JOptionPane.showMessageDialog(
                this,
                "Open a project first.",
                "No Project",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        final List<Scenario> testPlanScenarios = controller.getAvailableScenarios();
        final List<Scenario> reusableScenarios = controller.getAvailableReusableScenarios();

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("Automation Type:"));
        final JComboBox<String> typeCombo = new JComboBox<>(
            new String[] { TYPE_TEST_CASE, TYPE_USER_INTENT }
        );
        panel.add(typeCombo);

        panel.add(new JLabel("Target Scenario:"));
        // Editable so a brand-new scenario can be typed straight into the combo.
        final JComboBox<String> scenarioCombo = new JComboBox<>();
        scenarioCombo.setEditable(true);
        panel.add(scenarioCombo);

        final JLabel nameLabel = new JLabel("Test Case Name:");
        panel.add(nameLabel);
        final JTextField tcNameField = new JTextField(getQueryName());
        panel.add(tcNameField);

        panel.add(new JLabel(" "));
        JLabel hint = new JLabel("Type a new name in either field to create it.");
        hint.setForeground(java.awt.Color.GRAY);
        panel.add(hint);

        typeCombo.addActionListener(
            e -> {
                boolean reusable = TYPE_USER_INTENT.equals(typeCombo.getSelectedItem());
                List<Scenario> list = reusable ? reusableScenarios : testPlanScenarios;
                scenarioCombo.setModel(new DefaultComboBoxModel<>(scenarioNames(list)));
                nameLabel.setText(reusable ? "User Intent Name:" : "Test Case Name:");
            }
        );
        scenarioCombo.setModel(new DefaultComboBoxModel<>(scenarioNames(testPlanScenarios)));

        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Convert to Automation",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) return;

        boolean reusable = TYPE_USER_INTENT.equals(typeCombo.getSelectedItem());
        Object scenarioItem = scenarioCombo.getEditor().getItem();
        String scenarioName = scenarioItem == null ? "" : scenarioItem.toString().trim();
        if (scenarioName.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Please select or enter a scenario name.",
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

        Scenario scenario = controller.findOrCreateScenario(scenarioName, reusable);
        if (scenario == null) {
            JOptionPane.showMessageDialog(
                this,
                "Could not create scenario '" + scenarioName + "'.",
                "Conversion Failed",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        DBWorkbench.ExistingCasePolicy policy = DBWorkbench.ExistingCasePolicy.CREATE;
        if (controller.testCaseExists(scenario, tcName)) {
            policy = askExistingCasePolicy(tcName, reusable);
            if (policy == null) return;
        }

        DBQuery query = buildQuery();
        TestCase created = controller.convertQueryToAutomation(
            query,
            scenario,
            tcName,
            reusable,
            policy
        );

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

    /**
     * @param tcName the conflicting name
     * @param reusable whether the target is a user intent
     * @return the chosen policy, or {@code null} when the user cancelled
     */
    private DBWorkbench.ExistingCasePolicy askExistingCasePolicy(String tcName, boolean reusable) {
        String[] options = { "Overwrite steps", "Append steps", "Cancel" };
        int choice = JOptionPane.showOptionDialog(
            this,
            (reusable ? "User intent '" : "Test Case '") +
            tcName +
            "' already exists.\n\nOverwrite its steps, or append the new steps at the end?",
            "Already Exists",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[1]
        );
        if (choice == 0) return DBWorkbench.ExistingCasePolicy.OVERWRITE;
        if (choice == 1) return DBWorkbench.ExistingCasePolicy.APPEND;
        return null;
    }

    private static String[] scenarioNames(List<Scenario> scenarios) {
        String[] names = new String[scenarios.size()];
        for (int i = 0; i < scenarios.size(); i++) {
            names[i] = scenarios.get(i).getName();
        }
        return names;
    }
}
