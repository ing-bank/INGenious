package com.ing.ide.main.mainui.components.dbworkbench.connections;

import com.ing.ide.main.mainui.components.dbworkbench.util.JdbcExecutor;
import com.ing.util.encryption.Encryption;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/**
 * Modal add/edit form for a single database connection, with a Test Connection
 * button. Reads and writes the same {@code Settings/Databases/<alias>.properties}
 * store used by the engine, so a connection saved here drives generated tests too.
 */
public class ConnectionDialog extends JDialog {
    private static final String ENC_SUFFIX = " Enc";

    /** Extra (workbench-only) property keys persisted alongside the engine's keys. */
    public static final String VENDOR = "vendor";
    public static final String READ_ONLY = "readOnly";
    public static final String GROUP = "group";

    /** Aliases are file names in {@code Settings/Databases}; keep them path-safe. */
    private static final String ALIAS_PATTERN = "[A-Za-z0-9][A-Za-z0-9 ._-]*";
    /** Fully qualified Java class name. */
    private static final String DRIVER_PATTERN =
        "([A-Za-z_$][A-Za-z\\d_$]*\\.)*[A-Za-z_$][A-Za-z\\d_$]*";
    private static final int MAX_TIMEOUT_SECONDS = 86400;

    /** Vendor presets: label -> {driver, connection-string template}. */
    private static final String[][] VENDORS = {
        { "Custom", "", "" },
        { "PostgreSQL", "org.postgresql.Driver", "jdbc:postgresql://<host>:5432/<database>" },
        { "MySQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql://<host>:3306/<database>" },
        { "MariaDB", "org.mariadb.jdbc.Driver", "jdbc:mariadb://<host>:3306/<database>" },
        {
            "SQL Server",
            "com.microsoft.sqlserver.jdbc.SQLServerDriver",
            "jdbc:sqlserver://<host>:1433;databaseName=<database>"
        },
        { "Oracle", "oracle.jdbc.OracleDriver", "jdbc:oracle:thin:@<host>:1521:<sid>" },
        { "H2", "org.h2.Driver", "jdbc:h2:mem:testdb" },
        { "SQLite", "org.sqlite.JDBC", "jdbc:sqlite:<path-to-file>.db" },
        { "DB2", "com.ibm.db2.jcc.DB2Driver", "jdbc:db2://<host>:50000/<database>" }
    };

    private final JdbcExecutor executor;
    private final boolean editMode;
    private final String originalAlias;
    private final List<String> existingAliases;

    private final JTextField aliasField = new JTextField();
    private final JComboBox<String> vendorCombo = new JComboBox<>();
    private final JTextField driverField = new JTextField();
    private final JTextField connStringField = new JTextField();
    private final JTextField userField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JTextField timeoutField = new JTextField("30");
    private final JTextField groupField = new JTextField();
    private final JCheckBox commitCheck = new JCheckBox("Auto-commit");
    private final JCheckBox readOnlyCheck = new JCheckBox(
        "Read-only (block writes from workbench)"
    );

    private boolean saved = false;

    public ConnectionDialog(
        Frame owner,
        JdbcExecutor executor,
        String existingAlias,
        Properties existing,
        List<String> existingAliases
    ) {
        super(owner, existingAlias == null ? "New Connection" : "Edit Connection", true);
        this.executor = executor;
        this.editMode = existingAlias != null;
        this.originalAlias = existingAlias;
        this.existingAliases =
            existingAliases == null ? new ArrayList<>() : new ArrayList<>(existingAliases);
        buildUi();
        if (editMode) {
            populate(existingAlias, existing);
        }
        setSize(600, 480);
        setLocationRelativeTo(owner);
    }

    private void buildUi() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(14, 16, 8, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        for (String[] v : VENDORS) {
            vendorCombo.addItem(v[0]);
        }
        vendorCombo.addActionListener(e -> applyVendorTemplate());

        int row = 0;
        addRow(form, g, row++, "Alias (name):", aliasField, true);
        addRow(form, g, row++, "Vendor:", vendorCombo, false);
        addRow(form, g, row++, "Driver class:", driverField, true);
        addRow(form, g, row++, "Connection string:", connStringField, true);
        addRow(form, g, row++, "User:", userField, false);
        addRow(form, g, row++, "Password:", passwordField, false);
        addRow(form, g, row++, "Query timeout (s):", timeoutField, true);
        addRow(form, g, row++, "Group (optional):", groupField, false);
        addRow(form, g, row++, "", commitCheck, false);
        addRow(form, g, row++, "", readOnlyCheck, false);

        JLabel legend = new JLabel("* required");
        legend.setForeground(Color.GRAY);
        g.gridx = 1;
        g.gridy = row;
        form.add(legend, g);

        groupField.setToolTipText("Connections sharing a group are listed under a common folder");
        aliasField.setToolTipText(
            "Letters, digits, spaces, dot, dash and underscore; used as the configuration file name"
        );
        timeoutField.setToolTipText("Whole number of seconds, 1 to " + MAX_TIMEOUT_SECONDS);

        JButton testBtn = new JButton("Test Connection");
        testBtn.addActionListener(e -> doTest());
        JButton saveBtn = new JButton(editMode ? "Save" : "Add");
        saveBtn.addActionListener(e -> onSave());
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.add(testBtn);
        buttons.add(cancelBtn);
        buttons.add(saveBtn);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(saveBtn);
        installEscapeToClose();
    }

    /** Esc closes the dialog without saving, from any focused field. */
    private void installEscapeToClose() {
        getRootPane()
            .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "close-connection-dialog");
        getRootPane()
            .getActionMap()
            .put(
                "close-connection-dialog",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        dispose();
                    }
                }
            );
    }

    private void addRow(
        JPanel form,
        GridBagConstraints g,
        int row,
        String label,
        java.awt.Component field,
        boolean required
    ) {
        g.gridx = 0;
        g.gridy = row;
        g.weightx = 0;
        JLabel l = new JLabel(required ? label + " *" : label);
        form.add(l, g);
        g.gridx = 1;
        g.weightx = 1;
        if (field instanceof JTextField) {
            ((JTextField) field).setPreferredSize(new Dimension(340, 26));
        }
        form.add(field, g);
    }

    /**
     * Fills in the driver for the chosen vendor. The connection string is only
     * replaced when it is still empty or is another vendor's untouched template,
     * so switching the vendor never discards a real URL.
     */
    private void applyVendorTemplate() {
        int idx = vendorCombo.getSelectedIndex();
        if (idx <= 0) return; // Custom
        String[] v = VENDORS[idx];
        driverField.setText(v[1]);
        if (isTemplateOrEmpty(connStringField.getText())) {
            connStringField.setText(v[2]);
        }
    }

    private static boolean isTemplateOrEmpty(String connString) {
        String current = connString == null ? "" : connString.trim();
        if (current.isEmpty()) return true;
        for (String[] v : VENDORS) {
            if (!v[2].isEmpty() && v[2].equals(current)) return true;
        }
        return false;
    }

    /** Best-effort vendor detection for connections saved before vendor was stored. */
    private static String detectVendor(String driver, String connString) {
        String d = driver == null ? "" : driver.trim();
        String c = connString == null ? "" : connString.trim().toLowerCase(Locale.ROOT);
        for (String[] v : VENDORS) {
            if (!v[1].isEmpty() && v[1].equals(d)) return v[0];
        }
        if (c.startsWith("jdbc:postgresql")) return "PostgreSQL";
        if (c.startsWith("jdbc:mysql")) return "MySQL";
        if (c.startsWith("jdbc:mariadb")) return "MariaDB";
        if (c.startsWith("jdbc:sqlserver")) return "SQL Server";
        if (c.startsWith("jdbc:oracle")) return "Oracle";
        if (c.startsWith("jdbc:h2")) return "H2";
        if (c.startsWith("jdbc:sqlite")) return "SQLite";
        if (c.startsWith("jdbc:db2")) return "DB2";
        return "Custom";
    }

    private void populate(String alias, Properties p) {
        aliasField.setText(alias);
        driverField.setText(p.getProperty(JdbcExecutor.DRIVER, ""));
        connStringField.setText(p.getProperty(JdbcExecutor.CONN_STR, ""));
        userField.setText(p.getProperty(JdbcExecutor.USER, ""));
        passwordField.setText(decryptForDisplay(p.getProperty(JdbcExecutor.PASSWORD, "")));
        timeoutField.setText(p.getProperty(JdbcExecutor.TIMEOUT, "30"));
        groupField.setText(p.getProperty(GROUP, ""));
        commitCheck.setSelected(Boolean.parseBoolean(p.getProperty(JdbcExecutor.COMMIT, "false")));
        readOnlyCheck.setSelected(Boolean.parseBoolean(p.getProperty(READ_ONLY, "false")));

        String vendor = p.getProperty(VENDOR, "");
        if (vendor.isEmpty()) {
            vendor = detectVendor(driverField.getText(), connStringField.getText());
        }
        // Restore the stored URL afterwards: selecting the vendor fires the
        // template listener, which must never win over what the user saved.
        String storedConnString = connStringField.getText();
        vendorCombo.setSelectedItem(vendor);
        connStringField.setText(storedConnString);
    }

    private void doTest() {
        final Properties props = collectRuntimeProps();
        final JDialog self = this;
        new SwingWorker<String, Void>() {

            @Override
            protected String doInBackground() throws Exception {
                return executor.testConnection(props);
            }

            @Override
            protected void done() {
                try {
                    String info = get();
                    JOptionPane.showMessageDialog(
                        self,
                        info,
                        "Connection OK",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(
                        self,
                        describeFailure(cause, driverField.getText().trim()),
                        "Connection Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }
        .execute();
    }

    /**
     * Turns a raw JDBC failure into a message that names the likely cause. A
     * missing driver surfaces as a bare {@code ClassNotFoundException} whose
     * message is only the class name, which reads like a truncated error.
     *
     * @param cause the failure thrown while connecting
     * @param driver the configured driver class name
     * @return a message suitable for a dialog
     */
    public static String describeFailure(Throwable cause, String driver) {
        if (cause instanceof ClassNotFoundException) {
            return (
                "Connection failed: Error encountered connecting with the specified driver '" +
                driver +
                "'.\nThe driver class was not found. Check the class name and make sure the " +
                "JDBC driver jar is installed in the Resources/lib folder."
            );
        }
        String message = cause.getMessage();
        return "Connection failed:\n" + (message == null ? cause.toString() : message);
    }

    private void onSave() {
        String error = validateForm();
        if (error != null) {
            JOptionPane.showMessageDialog(
                this,
                error,
                "Invalid Connection Details",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        saved = true;
        dispose();
    }

    /**
     * @return the first validation failure, or {@code null} when the form is valid
     */
    private String validateForm() {
        String alias = getAlias();
        if (alias.isEmpty()) {
            return "Alias is required.";
        }
        if (!alias.matches(ALIAS_PATTERN)) {
            return (
                "Alias must start with a letter or digit and may only contain letters, " +
                "digits, spaces, dots, dashes and underscores."
            );
        }
        if (isDuplicateAlias(alias)) {
            return "A connection named '" + alias + "' already exists. Choose another alias.";
        }
        String driver = driverField.getText().trim();
        if (driver.isEmpty()) {
            return "Driver class is required.";
        }
        if (!driver.matches(DRIVER_PATTERN)) {
            return (
                "Driver class must be a fully qualified Java class name, " +
                "e.g. org.postgresql.Driver."
            );
        }
        String connString = connStringField.getText().trim();
        if (connString.isEmpty()) {
            return "Connection string is required.";
        }
        if (!connString.toLowerCase(Locale.ROOT).startsWith("jdbc:")) {
            return (
                "Connection string must be a JDBC URL starting with 'jdbc:', " +
                "e.g. jdbc:postgresql://localhost:5432/mydb."
            );
        }
        if (connString.contains("<") || connString.contains(">")) {
            return "Replace the <placeholders> in the connection string with real values.";
        }
        String timeout = timeoutField.getText().trim();
        if (timeout.isEmpty()) {
            return "Query timeout is required.";
        }
        int seconds;
        try {
            seconds = Integer.parseInt(timeout);
        } catch (NumberFormatException e) {
            return "Query timeout must be a whole number of seconds.";
        }
        if (seconds < 1 || seconds > MAX_TIMEOUT_SECONDS) {
            return "Query timeout must be between 1 and " + MAX_TIMEOUT_SECONDS + " seconds.";
        }
        return null;
    }

    private boolean isDuplicateAlias(String alias) {
        for (String existing : existingAliases) {
            if (!existing.equalsIgnoreCase(alias)) continue;
            // Keeping the same alias while editing is not a duplicate.
            return !(editMode && existing.equalsIgnoreCase(originalAlias));
        }
        return false;
    }

    /** Runtime properties (plaintext password) for a Test Connection attempt. */
    private Properties collectRuntimeProps() {
        Properties p = new Properties();
        p.setProperty(JdbcExecutor.DRIVER, driverField.getText().trim());
        p.setProperty(JdbcExecutor.CONN_STR, connStringField.getText().trim());
        p.setProperty(JdbcExecutor.USER, userField.getText().trim());
        p.setProperty(JdbcExecutor.PASSWORD, new String(passwordField.getPassword()));
        p.setProperty(JdbcExecutor.TIMEOUT, timeoutOrDefault());
        p.setProperty(JdbcExecutor.COMMIT, String.valueOf(commitCheck.isSelected()));
        return p;
    }

    /** Persisted properties (encrypted password with the {@code " Enc"} suffix). */
    public Properties toPersistedProps() {
        Properties p = new Properties();
        p.setProperty("db.alias", getAlias());
        p.setProperty(JdbcExecutor.USER, userField.getText().trim());
        p.setProperty(
            JdbcExecutor.PASSWORD,
            encryptForStore(new String(passwordField.getPassword()))
        );
        p.setProperty(JdbcExecutor.DRIVER, driverField.getText().trim());
        p.setProperty(JdbcExecutor.CONN_STR, connStringField.getText().trim());
        p.setProperty(JdbcExecutor.TIMEOUT, timeoutOrDefault());
        p.setProperty(JdbcExecutor.COMMIT, String.valueOf(commitCheck.isSelected()));
        p.setProperty(READ_ONLY, String.valueOf(readOnlyCheck.isSelected()));
        p.setProperty(VENDOR, String.valueOf(vendorCombo.getSelectedItem()));
        p.setProperty(GROUP, groupField.getText().trim());
        return p;
    }

    private String timeoutOrDefault() {
        String timeout = timeoutField.getText().trim();
        return timeout.isEmpty() ? "30" : timeout;
    }

    public String getAlias() {
        return aliasField.getText().trim();
    }

    /** @return the alias the dialog was opened with, or {@code null} when adding. */
    public String getOriginalAlias() {
        return originalAlias;
    }

    /** @return true when the user changed the alias of an existing connection. */
    public boolean isRenamed() {
        return editMode && !getAlias().equals(originalAlias);
    }

    public boolean isSaved() {
        return saved;
    }

    private static String decryptForDisplay(String stored) {
        if (stored == null) return "";
        if (stored.endsWith(ENC_SUFFIX)) {
            String cipher = stored.substring(0, stored.length() - ENC_SUFFIX.length());
            String plain = Encryption.getInstance().decrypt(cipher);
            return plain != null ? plain : "";
        }
        return stored;
    }

    private static String encryptForStore(String plain) {
        if (plain == null || plain.isEmpty()) return "";
        String cipher = Encryption.getInstance().encrypt(plain);
        return cipher != null ? cipher + ENC_SUFFIX : "";
    }
}
