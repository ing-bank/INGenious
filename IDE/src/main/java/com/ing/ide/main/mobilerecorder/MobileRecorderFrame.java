package com.ing.ide.main.mobilerecorder;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.mobile.MobileORPage;
import com.ing.datalib.or.mobile.MobilePlatform;
import com.ing.ide.main.fx.INGIcons;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.mobilerecorder.MobileStepRecorder.RecordResult;
import com.ing.ide.main.playwrightrecording.RecordingTargetDialog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

/**
 * Live Appium mobile recorder window. Mirrors the connected device's screen and
 * turns taps, swipes and text entry into native INGenious test steps backed by
 * Mobile Object Repository objects (persisted as YAML). Android-first; the
 * session/locator layers are platform-aware so iOS works with the same UI.
 */
public final class MobileRecorderFrame extends JFrame {
    private static final Logger LOG = Logger.getLogger(MobileRecorderFrame.class.getName());
    private static final long FRAME_INTERVAL_MS = 300;
    private static final int DISPLAY_WIDTH = 360;
    private static final double TAP_THRESHOLD_PX = 12.0;

    // INGenious brand palette (mirrors DriverSettings / brand accent #7724FF).
    private static final Color ING_PURPLE = new Color(119, 36, 255);
    private static final Color ING_ORANGE_DARK = new Color(255, 102, 0);
    private static final Color ING_BURGUNDY = Color.decode("#4D0020");
    private static final Color WARM_BG = Color.decode("#FAFAF8");
    private static final Color PURPLE_VERY_LIGHT = Color.decode("#F5F0FF");
    private static final Color PURPLE_LIGHT = Color.decode("#E5D6FF");
    private static final Color DARK_BG = Color.decode("#1E1E1E");
    private static final Color DARK_PANEL = Color.decode("#2A2A2A");
    private static final Color DARK_BORDER = Color.decode("#3A3A3A");
    private static final Color DARK_TEXT = new Color(220, 215, 225);

    private final transient AppMainFrame mainFrame;
    private final transient MobileSessionManager session = new MobileSessionManager();
    private final transient ScreenMirrorPoller poller = new ScreenMirrorPoller();
    private transient MobileStepRecorder recorder;
    private transient MobileORPage recordingPage;
    private transient TestCase targetCase;

    private volatile boolean recording = false;
    private volatile double displayScale = 1.0;
    private volatile int lastImageWidth;
    private volatile int lastImageHeight;
    private int pressX;
    private int pressY;

    private final JComboBox<String> deviceCombo = new JComboBox<>();
    private final JButton startButton = new JButton("Start Session");
    private final JButton stopButton = new JButton("Stop Session");
    private final JButton clearButton = new JButton("Clear Steps");
    private final JButton saveButton = new JButton("Finish & Save");
    private final JLabel statusLabel = new JLabel("Idle");
    private final JLabel mirrorLabel = new JLabel("", JLabel.CENTER);
    private final DefaultListModel<String> stepsModel = new DefaultListModel<>();
    private final JList<String> stepsList = new JList<>(stepsModel);

    public MobileRecorderFrame(AppMainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setTitle("Mobile Recorder");
        Icon frameIcon = INGIcons.swingColored("or.Mobile", 32);
        if (frameIcon != null) {
            setIconImage(INGIcons.toImage(frameIcon));
        }
        buildUi();
        populateDevices();
        setSize(940, 800);
        setLocationRelativeTo(mainFrame);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(
            new java.awt.event.WindowAdapter() {

                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    shutdown();
                }
            }
        );
    }

    private void buildUi() {
        JComponent content = (JComponent) getContentPane();
        content.setLayout(new BorderLayout());
        content.setOpaque(true);
        content.setBackground(bg());

        stopButton.setEnabled(false);
        clearButton.setEnabled(false);
        saveButton.setEnabled(false);
        startButton.addActionListener(e -> onStart());
        stopButton.addActionListener(e -> onStop());
        clearButton.addActionListener(e -> onClear());
        saveButton.addActionListener(e -> onSave());
        styleButton(startButton, true, "recorder");
        styleButton(stopButton, false, "record_stop");
        styleButton(clearButton, false, "delete");
        styleButton(saveButton, true, "save");

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(buildHeader(), BorderLayout.NORTH);
        north.add(buildControlBar(), BorderLayout.CENTER);

        content.add(north, BorderLayout.NORTH);
        content.add(buildCenter(), BorderLayout.CENTER);
        content.add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(true);
        header.setBackground(accent());
        header.setBorder(new EmptyBorder(12, 18, 12, 18));

        Icon mobile = INGIcons.swingColored("or.Mobile", 26);
        if (mobile != null) {
            JLabel iconLabel = new JLabel(mobile);
            iconLabel.setBorder(new EmptyBorder(0, 0, 0, 12));
            header.add(iconLabel, BorderLayout.WEST);
        }

        JLabel title = new JLabel("Mobile Recorder");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 17f));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel(
            "Tap, drag to scroll, or tap a field to type \u2014 gestures become native INGenious steps"
        );
        subtitle.setFont(subtitle.getFont().deriveFont(11.5f));
        subtitle.setForeground(new Color(255, 255, 255, 205));

        JPanel textPanel = new JPanel(new GridBagLayout());
        textPanel.setOpaque(false);
        GridBagConstraints tg = new GridBagConstraints();
        tg.gridx = 0;
        tg.gridy = 0;
        tg.anchor = GridBagConstraints.WEST;
        tg.weightx = 1;
        tg.fill = GridBagConstraints.HORIZONTAL;
        textPanel.add(title, tg);
        tg.gridy = 1;
        textPanel.add(subtitle, tg);
        header.add(textPanel, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildControlBar() {
        JPanel bar = new JPanel(new GridBagLayout());
        bar.setOpaque(true);
        bar.setBackground(panelBg());
        bar.setBorder(new EmptyBorder(10, 16, 10, 16));

        GridBagConstraints g = new GridBagConstraints();
        g.gridy = 0;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(0, 0, 0, 8);

        JLabel dev = new JLabel("Device");
        dev.setForeground(text());
        dev.setFont(dev.getFont().deriveFont(Font.BOLD));
        g.gridx = 0;
        bar.add(dev, g);

        deviceCombo.setBackground(inputBg());
        deviceCombo.setForeground(text());
        g.gridx = 1;
        bar.add(deviceCombo, g);

        JPanel filler = new JPanel();
        filler.setOpaque(false);
        g.gridx = 2;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        bar.add(filler, g);

        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        g.insets = new Insets(0, 4, 0, 0);
        g.gridx = 3;
        bar.add(startButton, g);
        g.gridx = 4;
        bar.add(stopButton, g);
        g.gridx = 5;
        bar.add(clearButton, g);
        g.gridx = 6;
        bar.add(saveButton, g);
        return bar;
    }

    private JComponent buildCenter() {
        mirrorLabel.setHorizontalAlignment(JLabel.CENTER);
        mirrorLabel.setVerticalAlignment(JLabel.CENTER);
        mirrorLabel.setText("Waiting for device\u2026");
        mirrorLabel.setForeground(textSecondary());
        mirrorLabel.setFont(mirrorLabel.getFont().deriveFont(13f));
        mirrorLabel.addMouseListener(
            new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    pressX = e.getX();
                    pressY = e.getY();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    onMirrorReleased(e.getX(), e.getY());
                }
            }
        );

        JScrollPane mirrorScroll = new JScrollPane(mirrorLabel);
        mirrorScroll.setBorder(BorderFactory.createEmptyBorder());
        mirrorScroll.getViewport().setBackground(surface());

        stepsList.setBackground(surface());
        stepsList.setForeground(text());
        stepsList.setFixedCellHeight(30);
        stepsList.setBorder(new EmptyBorder(4, 4, 4, 4));
        stepsList.setCellRenderer(new StepRenderer());
        JScrollPane stepsScroll = new JScrollPane(stepsList);
        stepsScroll.setBorder(BorderFactory.createEmptyBorder());
        stepsScroll.getViewport().setBackground(surface());

        JPanel mirrorCard = buildCard("Device Mirror", mirrorScroll);
        JPanel stepsCard = buildCard("Recorded Steps", stepsScroll);

        javax.swing.JSplitPane split = new javax.swing.JSplitPane(
            javax.swing.JSplitPane.HORIZONTAL_SPLIT,
            mirrorCard,
            stepsCard
        );
        split.setDividerLocation(460);
        split.setOpaque(false);
        split.setBorder(new EmptyBorder(12, 12, 8, 12));
        return split;
    }

    private JPanel buildCard(String title, JComponent body) {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(true);
        card.setBackground(surface());
        card.setBorder(BorderFactory.createLineBorder(borderColor(), 1, true));

        JLabel header = new JLabel(title);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 12f));
        header.setForeground(accent());
        header.setBorder(new EmptyBorder(8, 10, 8, 10));

        JPanel headPanel = new JPanel(new BorderLayout());
        headPanel.setOpaque(true);
        headPanel.setBackground(panelBg());
        headPanel.setBorder(new MatteBorder(0, 0, 1, 0, borderColor()));
        headPanel.add(header, BorderLayout.WEST);

        card.add(headPanel, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(true);
        bar.setBackground(panelBg());
        bar.setBorder(
            BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, borderColor()),
                new EmptyBorder(0, 0, 0, 0)
            )
        );
        statusLabel.setForeground(text());
        statusLabel.setBorder(
            BorderFactory.createCompoundBorder(
                new MatteBorder(0, 4, 0, 0, accent()),
                new EmptyBorder(7, 12, 7, 12)
            )
        );
        bar.add(statusLabel, BorderLayout.CENTER);
        return bar;
    }

    private void styleButton(JButton b, boolean primary, String iconKey) {
        Color acc = accent();
        b.setFocusPainted(false);
        b.setForeground(primary ? Color.WHITE : acc);
        b.setBackground(primary ? acc : inputBg());
        b.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(acc, 1, true),
                new EmptyBorder(7, 16, 7, 16)
            )
        );
        b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        if (iconKey != null) {
            Icon ic = INGIcons.swingColored(iconKey, 15);
            if (ic != null) {
                b.setIcon(ic);
            }
        }
    }

    // ── Theme palette (dark-mode aware, mirrors DriverSettings) ──

    private boolean isDark() {
        return com.ing.ide.main.Main.isDarkMode();
    }

    private Color accent() {
        return isDark() ? ING_ORANGE_DARK : ING_PURPLE;
    }

    private Color bg() {
        return isDark() ? DARK_BG : WARM_BG;
    }

    private Color panelBg() {
        return isDark() ? DARK_PANEL : PURPLE_VERY_LIGHT;
    }

    private Color surface() {
        return isDark() ? DARK_PANEL : Color.WHITE;
    }

    private Color borderColor() {
        return isDark() ? DARK_BORDER : PURPLE_LIGHT;
    }

    private Color text() {
        return isDark() ? DARK_TEXT : ING_BURGUNDY;
    }

    private Color textSecondary() {
        return isDark() ? new Color(150, 150, 150) : new Color(140, 120, 150);
    }

    private Color inputBg() {
        return isDark() ? Color.decode("#333333") : Color.WHITE;
    }

    /** Colours each recorded step row and prefixes a mobile glyph. */
    private final class StepRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean selected,
            boolean focused
        ) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                list,
                value,
                index,
                selected,
                focused
            );
            label.setBorder(new EmptyBorder(4, 10, 4, 8));
            Icon ic = INGIcons.swingColored("or.Mobile", 14);
            if (ic != null) {
                label.setIcon(ic);
            }
            if (selected) {
                label.setBackground(accent());
                label.setForeground(Color.WHITE);
            } else {
                label.setBackground(index % 2 == 0 ? surface() : panelBg());
                label.setForeground(text());
            }
            return label;
        }
    }

    private void populateDevices() {
        deviceCombo.removeAllItems();
        Project project = mainFrame.getProject();
        if (project == null) {
            startButton.setEnabled(false);
            statusLabel.setText("Open a project first");
            return;
        }
        List<String> names = new ArrayList<>(
            project.getProjectSettings().getEmulators().getAppiumEmulatorNames()
        );
        for (String d : project.getProjectSettings().getDevices().getDeviceNames()) {
            if (!names.contains(d)) {
                names.add(d);
            }
        }
        for (String n : names) {
            deviceCombo.addItem(n);
        }
        if (names.isEmpty()) {
            startButton.setEnabled(false);
            statusLabel.setText("No emulators/devices configured. Add one under Settings.");
        }
    }

    // ===================================================================
    // Session lifecycle
    // ===================================================================

    private void onStart() {
        Project project = mainFrame.getProject();
        if (project == null) {
            return;
        }
        Object selected = deviceCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a device to record on.");
            return;
        }
        RecordingTargetDialog.Selection selection = RecordingTargetDialog.showDialog(
            this,
            project,
            mainFrame.getTestDesign().getTestCaseComp().getCurrentTestCase()
        );
        if (selection == null) {
            return;
        }
        TestCase target = resolveTarget(project, selection);
        if (target == null) {
            JOptionPane.showMessageDialog(this, "Unable to resolve recording target.");
            return;
        }
        this.targetCase = target;
        this.recordingPage = createRecordingPage(project, target.getName());
        String reference = "[Project] " + recordingPage.getName();

        startButton.setEnabled(false);
        statusLabel.setText("Starting session...");
        String deviceName = selected.toString();

        Thread worker = new Thread(
            () -> {
                try {
                    session.start(project, deviceName);
                    MobilePlatform platform = session.getPlatform();
                    recorder =
                        new MobileStepRecorder(
                            session.getDriver(),
                            platform,
                            target,
                            recordingPage,
                            reference
                        );
                    poller.start(session, this::onFrame, FRAME_INTERVAL_MS);
                    recording = true;
                    SwingUtilities.invokeLater(
                        () -> {
                            statusLabel.setText(
                                "Recording (" +
                                platform +
                                ") \u2192 " +
                                target.getScenario().getName() +
                                " / " +
                                target.getName() +
                                "  \u2014 tap, drag to scroll, or tap a field to type"
                            );
                            stopButton.setEnabled(true);
                            clearButton.setEnabled(true);
                            saveButton.setEnabled(true);
                            refreshSteps();
                        }
                    );
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Failed to start recording session", ex);
                    SwingUtilities.invokeLater(
                        () -> {
                            statusLabel.setText("Failed to start session");
                            startButton.setEnabled(true);
                            JOptionPane.showMessageDialog(
                                this,
                                "Could not start Appium session:\n" + ex.getMessage(),
                                "Mobile Recorder",
                                JOptionPane.ERROR_MESSAGE
                            );
                        }
                    );
                }
            },
            "mobile-recorder-start"
        );
        worker.setDaemon(true);
        worker.start();
    }

    private void onStop() {
        recording = false;
        stopButton.setEnabled(false);
        statusLabel.setText("Stopping session...");
        Thread worker = new Thread(
            () -> {
                poller.stop();
                session.stop();
                SwingUtilities.invokeLater(
                    () -> {
                        statusLabel.setText("Session stopped. Steps preserved.");
                        startButton.setEnabled(true);
                        mirrorLabel.setIcon(null);
                        mirrorLabel.setText("Waiting for device\u2026");
                        mirrorLabel.setForeground(textSecondary());
                    }
                );
            },
            "mobile-recorder-stop"
        );
        worker.setDaemon(true);
        worker.start();
    }

    private void onClear() {
        if (targetCase != null) {
            for (int i = targetCase.getTestSteps().size() - 1; i >= 0; i--) {
                targetCase.removeRow(i);
            }
            refreshSteps();
        }
    }

    private void onSave() {
        if (targetCase == null) {
            return;
        }
        try {
            targetCase.save();
            if (recordingPage != null && recordingPage.getRoot() != null) {
                recordingPage.getRoot().getObjectRepository().saveMobilePageNow(recordingPage);
            }
            registerInTree(targetCase);
            for (TestStep step : targetCase.getTestSteps()) {
                step.setNewlyRecorded(false);
            }
            statusLabel.setText("Saved to " + targetCase.getName());
            JOptionPane.showMessageDialog(
                this,
                "Recorded " +
                targetCase.getTestSteps().size() +
                " step(s) into '" +
                targetCase.getScenario().getName() +
                " / " +
                targetCase.getName() +
                "'.\nMobile objects saved to OR page '" +
                (recordingPage == null ? "" : recordingPage.getName()) +
                "'.",
                "Mobile Recorder",
                JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Failed to save recording", ex);
            JOptionPane.showMessageDialog(
                this,
                "Could not save recording:\n" + ex.getMessage(),
                "Mobile Recorder",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // ===================================================================
    // Mirror interaction
    // ===================================================================

    private void onFrame(byte[] png) {
        try {
            java.awt.image.BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
            if (img == null) {
                return;
            }
            lastImageWidth = img.getWidth();
            lastImageHeight = img.getHeight();
            displayScale = (double) img.getWidth() / DISPLAY_WIDTH;
            int displayHeight = (int) Math.round(img.getHeight() / displayScale);
            Image scaled = img.getScaledInstance(DISPLAY_WIDTH, displayHeight, Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(scaled);
            SwingUtilities.invokeLater(
                () -> {
                    mirrorLabel.setText(null);
                    mirrorLabel.setIcon(icon);
                }
            );
        } catch (Exception e) {
            LOG.log(Level.FINE, "Failed to render frame", e);
        }
    }

    private void onMirrorReleased(int releaseX, int releaseY) {
        if (!recording || recorder == null || mirrorLabel.getIcon() == null) {
            return;
        }
        double scale = displayScale;
        int shotW = lastImageWidth;
        int shotH = lastImageHeight;
        int startShotX = (int) Math.round(pressX * scale);
        int startShotY = (int) Math.round(pressY * scale);
        int endShotX = (int) Math.round(releaseX * scale);
        int endShotY = (int) Math.round(releaseY * scale);
        double distance = Math.hypot(releaseX - pressX, releaseY - pressY);

        if (distance < TAP_THRESHOLD_PX) {
            recordTap(startShotX, startShotY, shotW, shotH);
        } else {
            recordSwipe(startShotX, startShotY, endShotX, endShotY, shotW, shotH);
        }
    }

    private void recordTap(int x, int y, int shotW, int shotH) {
        statusLabel.setText("Resolving tap target...");
        Thread worker = new Thread(
            () -> {
                RecordResult result = recorder.recordTapAt(x, y, shotW, shotH);
                SwingUtilities.invokeLater(
                    () -> {
                        statusLabel.setText(result.getMessage());
                        refreshSteps();
                        if (result.isOk() && result.isEditable()) {
                            promptForText(result);
                        }
                    }
                );
            },
            "mobile-recorder-tap"
        );
        worker.setDaemon(true);
        worker.start();
    }

    private void promptForText(RecordResult tap) {
        String text = JOptionPane.showInputDialog(
            this,
            "Text to type into this field:",
            "Enter Text",
            JOptionPane.QUESTION_MESSAGE
        );
        if (text == null || text.isEmpty()) {
            return;
        }
        statusLabel.setText("Entering text...");
        Thread worker = new Thread(
            () -> {
                RecordResult result = recorder.recordTextInput(tap, text);
                SwingUtilities.invokeLater(
                    () -> {
                        statusLabel.setText(result.getMessage());
                        refreshSteps();
                    }
                );
            },
            "mobile-recorder-text"
        );
        worker.setDaemon(true);
        worker.start();
    }

    private void recordSwipe(int sx, int sy, int ex, int ey, int shotW, int shotH) {
        statusLabel.setText("Recording swipe...");
        Thread worker = new Thread(
            () -> {
                RecordResult result = recorder.recordSwipe(sx, sy, ex, ey, shotW, shotH);
                SwingUtilities.invokeLater(
                    () -> {
                        statusLabel.setText(result.getMessage());
                        refreshSteps();
                    }
                );
            },
            "mobile-recorder-swipe"
        );
        worker.setDaemon(true);
        worker.start();
    }

    private void refreshSteps() {
        stepsModel.clear();
        if (targetCase == null) {
            return;
        }
        int i = 1;
        for (TestStep step : targetCase.getTestSteps()) {
            String action = step.getAction();
            if (action == null || action.trim().isEmpty()) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(i++).append(". ").append(action);
            if (!step.getObject().isEmpty()) {
                sb.append("  [").append(step.getObject()).append("]");
            }
            if (!step.getInput().isEmpty()) {
                sb.append("  ").append(step.getInput());
            }
            stepsModel.addElement(sb.toString());
        }
    }

    // ===================================================================
    // Target + page resolution
    // ===================================================================

    private TestCase resolveTarget(Project project, RecordingTargetDialog.Selection selection) {
        switch (selection.getMode()) {
            case CURRENT_OPEN_TEST_CASE:
                return mainFrame.getTestDesign().getTestCaseComp().getCurrentTestCase();
            case NEW_TEST_SCENARIO:
                return createOrResolve(
                    project,
                    selection.getScenarioName(),
                    selection.getTestCaseName(),
                    false
                );
            case NEW_REUSABLE_SCENARIO:
                return createOrResolve(
                    project,
                    selection.getScenarioName(),
                    selection.getTestCaseName(),
                    true
                );
            case EXISTING_TEST_CASE:
                return findExisting(
                    project,
                    selection.getExistingScenarioName(),
                    selection.getTestCaseName(),
                    selection.isExistingReusable()
                );
            default:
                return null;
        }
    }

    private TestCase createOrResolve(
        Project project,
        String scenarioName,
        String testCaseName,
        boolean reusable
    ) {
        Scenario scenario = reusable
            ? project.getReusableScenarioByName(scenarioName)
            : project.getScenarioByName(scenarioName);
        if (scenario == null) {
            scenario =
                reusable
                    ? project.addReusableScenario(scenarioName)
                    : project.addScenario(scenarioName);
        }
        if (scenario == null) {
            return null;
        }
        TestCase testCase = scenario.getTestCaseByName(testCaseName);
        if (testCase == null) {
            testCase = scenario.addTestCase(testCaseName);
        }
        return testCase;
    }

    private TestCase findExisting(
        Project project,
        String scenarioName,
        String testCaseName,
        boolean reusable
    ) {
        Scenario scenario = reusable
            ? project.getReusableScenarioByName(scenarioName)
            : project.getScenarioByName(scenarioName);
        return scenario == null ? null : scenario.getTestCaseByName(testCaseName);
    }

    private MobileORPage createRecordingPage(Project project, String basePageName) {
        MobileOR mobileOR = project.getObjectRepository().getMobileOR();
        String pageName = basePageName;
        if (mobileOR.getPageByName(pageName) != null) {
            int counter = 1;
            while (mobileOR.getPageByName(basePageName + "_" + counter) != null) {
                counter++;
            }
            pageName = basePageName + "_" + counter;
        }
        return mobileOR.addPage(pageName);
    }

    private void registerInTree(TestCase testCase) {
        try {
            boolean reusable =
                testCase.getScenario() != null &&
                testCase.getScenario().getSource() != Scenario.Source.TEST_PLAN;
            if (reusable) {
                mainFrame.getTestDesign().getReusableTree().getTreeModel().addTestCase(testCase);
            } else {
                mainFrame.getTestDesign().getProjectTree().getTreeModel().addTestCase(testCase);
            }
        } catch (Exception ex) {
            LOG.log(Level.FINE, "Could not register recording target in tree", ex);
        }
    }

    private void shutdown() {
        recording = false;
        poller.shutdown();
        session.stop();
    }
}
