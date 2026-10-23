package com.ing.ide.main.mainui.components.dbworkbench.connections;

import com.ing.ide.main.mainui.components.dbworkbench.util.JdbcExecutor;
import com.ing.util.encryption.Encryption;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

/**
 * Modal add/edit form for a single database connection, with a Test Connection
 * button. Reads and writes the same {@code Settings/Databases/<alias>.properties}
 * store used by the engine, so a connection saved here drives generated tests too.
 */
public class ConnectionDialog extends JDialog {
    private static final String ENC_SUFFIX = " Enc";

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

    private final JTextField aliasField = new JTextField();
    private final JComboBox<String> vendorCombo = new JComboBox<>();
    private final JTextField driverField = new JTextField();
    private final JTextField connStringField = new JTextField();
    private final JTextField userField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JTextField timeoutField = new JTextField("30");
    private final JCheckBox commitCheck = new JCheckBox("Auto-commit");
    private final JCheckBox readOnlyCheck = new JCheckBox("Read-only (block DML from workbench)");

    private boolean saved = false;

    public ConnectionDialog(
        Frame owner,
        JdbcExecutor executor,
        String existingAlias,
        Properties existing
    ) {
        super(owner, existingAlias == null ? "New Connection" : "Edit Connection", true);
        this.executor = executor;
        this.editMode = existingAlias != null;
        buildUi();
        if (editMode) {
            populate(existingAlias, existing);
        }
        setSize(560, 420);
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
        addRow(form, g, row++, "Alias (name):", aliasField);
        addRow(form, g, row++, "Vendor:", vendorCombo);
        addRow(form, g, row++, "Driver class:", driverField);
        addRow(form, g, row++, "Connection string:", connStringField);
        addRow(form, g, row++, "User:", userField);
        addRow(form, g, row++, "Password:", passwordField);
        addRow(form, g, row++, "Query timeout (s):", timeoutField);
        addRow(form, g, row++, "", commitCheck);
        addRow(form, g, row++, "", readOnlyCheck);

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

        aliasField.setEnabled(!editMode); // alias is the file name; don't rename in place
    }

    private void addRow(
        JPanel form,
        GridBagConstraints g,
        int row,
        String label,
        java.awt.Component field
    ) {
        g.gridx = 0;
        g.gridy = row;
        g.weightx = 0;
        JLabel l = new JLabel(label);
        form.add(l, g);
        g.gridx = 1;
        g.weightx = 1;
        if (field instanceof JTextField) {
            ((JTextField) field).setPreferredSize(new Dimension(320, 26));
        }
        form.add(field, g);
    }

    private void applyVendorTemplate() {
        int idx = vendorCombo.getSelectedIndex();
        if (idx <= 0) return; // Custom
        String[] v = VENDORS[idx];
        driverField.setText(v[1]);
        if (
            connStringField.getText().trim().isEmpty() ||
            connStringField.getText().startsWith("jdbc:")
        ) {
            connStringField.setText(v[2]);
        }
    }

    private void populate(String alias, Properties p) {
        aliasField.setText(alias);
        driverField.setText(p.getProperty(JdbcExecutor.DRIVER, ""));
        connStringField.setText(p.getProperty(JdbcExecutor.CONN_STR, ""));
        userField.setText(p.getProperty(JdbcExecutor.USER, ""));
        passwordField.setText(decryptForDisplay(p.getProperty(JdbcExecutor.PASSWORD, "")));
        timeoutField.setText(p.getProperty(JdbcExecutor.TIMEOUT, "30"));
        commitCheck.setSelected(Boolean.parseBoolean(p.getProperty(JdbcExecutor.COMMIT, "false")));
        readOnlyCheck.setSelected(Boolean.parseBoolean(p.getProperty("readOnly", "false")));
        vendorCombo.setSelectedIndex(0);
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
                        "Connection failed:\n" + cause.getMessage(),
                        "Connection Failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }
        .execute();
    }

    private void onSave() {
        if (aliasField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Alias is required.",
                "Missing Alias",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        if (connStringField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "Connection string is required.",
                "Missing URL",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        saved = true;
        dispose();
    }

    /** Runtime properties (plaintext password) for a Test Connection attempt. */
    private Properties collectRuntimeProps() {
        Properties p = new Properties();
        p.setProperty(JdbcExecutor.DRIVER, driverField.getText().trim());
        p.setProperty(JdbcExecutor.CONN_STR, connStringField.getText().trim());
        p.setProperty(JdbcExecutor.USER, userField.getText().trim());
        p.setProperty(JdbcExecutor.PASSWORD, new String(passwordField.getPassword()));
        p.setProperty(
            JdbcExecutor.TIMEOUT,
            timeoutField.getText().trim().isEmpty() ? "30" : timeoutField.getText().trim()
        );
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
        p.setProperty(
            JdbcExecutor.TIMEOUT,
            timeoutField.getText().trim().isEmpty() ? "30" : timeoutField.getText().trim()
        );
        p.setProperty(JdbcExecutor.COMMIT, String.valueOf(commitCheck.isSelected()));
        p.setProperty("readOnly", String.valueOf(readOnlyCheck.isSelected()));
        return p;
    }

    public String getAlias() {
        return aliasField.getText().trim();
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
