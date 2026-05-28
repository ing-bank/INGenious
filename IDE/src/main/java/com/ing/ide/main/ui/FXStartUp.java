package com.ing.ide.main.ui;

import com.ing.datalib.testdata.TestDataFactory;
import com.ing.ide.main.fx.FXTheme;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.utils.INGeniousFileChooser;
import com.ing.ide.main.utils.recentItem.RecentItem;
import com.ing.ide.settings.AppSettings;
import com.ing.ide.util.Validator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;

import javax.swing.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Beautiful JavaFX-based StartUp screen for INGenious Playwright Studio.
 * Replaces the old Swing StartUp dialog with a modern, visually appealing UI.
 * <p>
 * Features a sidebar navigation with 4 views:
 * Recent, Application (default projects location), System (file chooser), New.
 */
public class FXStartUp extends JDialog {

    private static final Logger LOG = Logger.getLogger(FXStartUp.class.getName());

    private final AppMainFrame sMainFrame;
    private boolean recentChanged = false;

    // JavaFX content
    private JFXPanel fxPanel;
    private ToggleGroup navGroup;
    private StackPane contentArea;

    // Recent tab
    private ListView<RecentItem> recentList;

    // Application tab
    private ListView<String> appList;

    // New tab
    private TextField projNameField;
    private TextField projLocationField;
    private ComboBox<String> testDataTypeCombo;
    private Label errorLabel;

    public FXStartUp(AppMainFrame sMainFrame) {
        super(new JFrame());
        this.sMainFrame = sMainFrame;
        setModal(true);
        setTitle("INGenious Playwright Studio");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                sMainFrame.quit();
            }
        });

        fxPanel = new JFXPanel();
        getContentPane().add(fxPanel);
        // Build UI synchronously: block until FX thread has set the scene.
        // Prevents macOS NSTrackingRectTag crash when showIt() triggers
        // a JFXPanel resize before Glass tracking rects are initialised.
        CountDownLatch sceneReady = new CountDownLatch(1);
        Platform.runLater(() -> {
            buildUI();
            sceneReady.countDown();
        });
        try {
            sceneReady.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void showIt() {
        setSize(720, 480);
        sMainFrame.setVisible(false);
        setLocationRelativeTo(null);
        Platform.runLater(this::loadData);
        setVisible(true);
    }

    private void buildUI() {
        // ── Sidebar ──
        VBox sidebar = buildSidebar();

        // ── Content area ──
        contentArea = new StackPane();
        contentArea.getStyleClass().add("startup-content");

        // ── Root layout ──
        HBox root = new HBox();
        root.getStyleClass().add("startup-root");
        HBox.setHgrow(contentArea, Priority.ALWAYS);
        root.getChildren().addAll(sidebar, contentArea);

        Scene scene = new Scene(root, 720, 480);
        FXTheme.registerScene(scene);
        fxPanel.setScene(scene);
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(4);
        sidebar.getStyleClass().add("startup-sidebar");
        sidebar.setPrefWidth(180);
        sidebar.setMinWidth(180);
        sidebar.setPadding(new Insets(20, 12, 20, 12));

        // Brand header
        Label brandLabel = new Label("INGenious");
        brandLabel.getStyleClass().add("startup-brand");

        Label subtitleLabel = new Label("Playwright Studio");
        subtitleLabel.getStyleClass().add("startup-subtitle");

        VBox brandBox = new VBox(2, brandLabel, subtitleLabel);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setPadding(new Insets(0, 0, 20, 4));

        // Navigation buttons
        navGroup = new ToggleGroup();

        ToggleButton recentBtn = createNavButton("Recent", MaterialDesignH.HISTORY, "startup-nav-recent");
        ToggleButton appBtn = createNavButton("Projects", MaterialDesignA.APPLICATION, "startup-nav-app");
        ToggleButton systemBtn = createNavButton("Browse", MaterialDesignF.FOLDER_SEARCH, "startup-nav-system");
        ToggleButton newBtn = createNavButton("New Project", MaterialDesignP.PLUS_CIRCLE, "startup-nav-new");

        recentBtn.setToggleGroup(navGroup);
        appBtn.setToggleGroup(navGroup);
        systemBtn.setToggleGroup(navGroup);
        newBtn.setToggleGroup(navGroup);

        // Prevent deselecting all buttons
        navGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) oldVal.setSelected(true);
        });

        // Handle navigation
        recentBtn.setOnAction(e -> showPanel("Recent"));
        appBtn.setOnAction(e -> showPanel("Application"));
        systemBtn.setOnAction(e -> showSystemChooser());
        newBtn.setOnAction(e -> showPanel("New"));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Auto-open checkbox
        CheckBox autoOpen = new CheckBox("Auto-open last project");
        autoOpen.getStyleClass().add("startup-auto-open");
        autoOpen.setSelected(AppSettings.canOpenRecentProjects());
        autoOpen.selectedProperty().addListener((obs, ov, nv) -> {
            AppSettings.openRecentProjectsOnLaunch(nv);
            recentChanged = true;
        });

        sidebar.getChildren().addAll(
                brandBox,
                recentBtn, appBtn, systemBtn, newBtn,
                spacer,
                autoOpen
        );

        return sidebar;
    }

    private ToggleButton createNavButton(String text, org.kordamp.ikonli.Ikon ikon, String styleClass) {
        FontIcon icon = new FontIcon(ikon);
        icon.setIconSize(18);
        icon.getStyleClass().add("startup-nav-icon");

        ToggleButton btn = new ToggleButton(text);
        btn.setGraphic(icon);
        btn.getStyleClass().add("startup-nav-btn");
        if (styleClass != null) btn.getStyleClass().add(styleClass);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setGraphicTextGap(10);
        return btn;
    }

    private void showPanel(String panelName) {
        contentArea.getChildren().clear();
        switch (panelName) {
            case "Recent":
                contentArea.getChildren().add(buildRecentPanel());
                break;
            case "Application":
                contentArea.getChildren().add(buildApplicationPanel());
                break;
            case "New":
                contentArea.getChildren().add(buildNewProjectPanel());
                break;
        }
    }

    // ── Recent Panel ──

    private VBox buildRecentPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(24, 28, 24, 28));
        panel.getStyleClass().add("startup-panel");

        Label title = new Label("Recent Projects");
        title.getStyleClass().add("startup-panel-title");

        FontIcon titleIcon = new FontIcon(MaterialDesignH.HISTORY);
        titleIcon.setIconSize(22);
        titleIcon.setIconColor(Color.web("#FF6200"));
        title.setGraphic(titleIcon);
        title.setGraphicTextGap(8);

        Label hint = new Label("Double-click a project to open it");
        hint.getStyleClass().add("startup-hint");

        // Reuse existing ListView if it was already populated with data
        if (recentList == null) {
            recentList = new ListView<>();
        }
        // Ensure styling and event handlers are set (may have been created by loadRecentData)
        if (!recentList.getStyleClass().contains("startup-list")) {
            recentList.getStyleClass().add("startup-list");
        }
        recentList.setCellFactory(lv -> new RecentItemCell());
        recentList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && recentList.getSelectionModel().getSelectedItem() != null) {
                loadProject(recentList.getSelectionModel().getSelectedItem().getLocation());
            }
        });
        VBox.setVgrow(recentList, Priority.ALWAYS);

        panel.getChildren().addAll(title, hint, recentList);
        return panel;
    }

    // ── Application Panel (Default Projects Location) ──

    private VBox buildApplicationPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(24, 28, 24, 28));
        panel.getStyleClass().add("startup-panel");

        Label title = new Label("Projects");
        title.getStyleClass().add("startup-panel-title");

        FontIcon titleIcon = new FontIcon(MaterialDesignA.APPLICATION);
        titleIcon.setIconSize(22);
        titleIcon.setIconColor(Color.web("#7724FF"));
        title.setGraphic(titleIcon);
        title.setGraphicTextGap(8);

        Label loc = new Label("Location: " + new File("Projects").getAbsolutePath());
        loc.getStyleClass().add("startup-hint");

        // Reuse existing ListView if it was already populated with data
        if (appList == null) {
            appList = new ListView<>();
        }
        // Ensure styling and event handlers are set (may have been created by loadAppProjectsData)
        if (!appList.getStyleClass().contains("startup-list")) {
            appList.getStyleClass().add("startup-list");
        }
        appList.setCellFactory(lv -> new ProjectItemCell());
        appList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && appList.getSelectionModel().getSelectedItem() != null) {
                String projName = appList.getSelectionModel().getSelectedItem();
                loadProject(new File("Projects" + File.separator + projName).getAbsolutePath());
            }
        });
        VBox.setVgrow(appList, Priority.ALWAYS);

        panel.getChildren().addAll(title, loc, appList);
        return panel;
    }

    // ── New Project Panel ──

    private VBox buildNewProjectPanel() {
        VBox panel = new VBox(16);
        panel.setPadding(new Insets(24, 40, 24, 40));
        panel.getStyleClass().add("startup-panel");
        panel.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Create New Project");
        title.getStyleClass().add("startup-panel-title");

        FontIcon titleIcon = new FontIcon(MaterialDesignP.PLUS_CIRCLE);
        titleIcon.setIconSize(22);
        titleIcon.setIconColor(Color.web("#349651"));
        title.setGraphic(titleIcon);
        title.setGraphicTextGap(8);

        // Error label
        errorLabel = new Label();
        errorLabel.getStyleClass().add("startup-error");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);

        // Project Name
        Label nameLabel = new Label("Project Name");
        nameLabel.getStyleClass().add("startup-field-label");
        projNameField = new TextField();
        projNameField.setPromptText("Enter project name...");
        projNameField.getStyleClass().add("startup-field");
        projNameField.setMaxWidth(400);

        // Project Location
        Label locLabel = new Label("Project Location");
        locLabel.getStyleClass().add("startup-field-label");
        projLocationField = new TextField(new File("Projects").getAbsolutePath());
        projLocationField.setEditable(false);
        projLocationField.getStyleClass().addAll("startup-field", "startup-field-readonly");
        projLocationField.setMaxWidth(400);

        // Test Data Type
        Label tdLabel = new Label("Test Data Type");
        tdLabel.getStyleClass().add("startup-field-label");
        testDataTypeCombo = new ComboBox<>(FXCollections.observableArrayList(
                TestDataFactory.getDATA_PROVIDER_NAMES()));
        testDataTypeCombo.getStyleClass().add("startup-field");
        testDataTypeCombo.setMaxWidth(400);
        if (!testDataTypeCombo.getItems().isEmpty()) {
            testDataTypeCombo.getSelectionModel().selectFirst();
        }

        // Create Button
        Button createBtn = new Button("Create Project");
        createBtn.getStyleClass().add("startup-create-btn");

        FontIcon rocketIcon = new FontIcon(MaterialDesignR.ROCKET_LAUNCH);
        rocketIcon.setIconSize(16);
        rocketIcon.setIconColor(Color.WHITE);
        createBtn.setGraphic(rocketIcon);

        createBtn.setOnAction(e -> onCreateProject());
        projNameField.setOnAction(e -> onCreateProject());

        VBox formBox = new VBox(8);
        formBox.setAlignment(Pos.CENTER_LEFT);
        formBox.setMaxWidth(400);
        formBox.getChildren().addAll(
                nameLabel, projNameField,
                new Region(),
                locLabel, projLocationField,
                new Region(),
                tdLabel, testDataTypeCombo
        );

        HBox btnBox = new HBox(createBtn);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(12, 0, 0, 0));

        panel.getChildren().addAll(title, errorLabel, formBox, btnBox);
        return panel;
    }

    // ── System File Chooser ──

    private void showSystemChooser() {
        // Run Swing file chooser on EDT
        SwingUtilities.invokeLater(() -> {
            INGeniousFileChooser.OPEN_PROJECT.afterFileSelected = this::loadProject;
            int result = INGeniousFileChooser.OPEN_PROJECT.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selected = INGeniousFileChooser.OPEN_PROJECT.getSelectedFile();
                if (selected != null) {
                    loadProject(selected.getAbsolutePath());
                }
            }
            INGeniousFileChooser.OPEN_PROJECT.afterFileSelected = null;
        });
    }

    // ── Data Loading ──

    private void loadData() {
        loadRecentData();
        loadAppProjectsData();

        // Select default nav
        ToggleButton defaultBtn;
        if (recentList != null && !recentList.getItems().isEmpty()) {
            defaultBtn = (ToggleButton) navGroup.getToggles().get(0);
            showPanel("Recent");
        } else {
            defaultBtn = (ToggleButton) navGroup.getToggles().get(1);
            showPanel("Application");
        }
        defaultBtn.setSelected(true);
    }

    private void loadRecentData() {
        ObservableList<RecentItem> items = FXCollections.observableArrayList();
        for (RecentItem ri : sMainFrame.getRecentItems().getRECENT_ITEMS()) {
            items.add(ri);
        }
        if (recentList == null) {
            recentList = new ListView<>();
        }
        recentList.setItems(items);
    }

    private void loadAppProjectsData() {
        ObservableList<String> items = FXCollections.observableArrayList();
        File projects = new File("Projects");
        if (projects.exists()) {
            File[] files = projects.listFiles();
            if (files != null) {
                for (File project : files) {
                    if (project.isDirectory()) {
                        File[] dotProject = project.listFiles((dir, name) -> name.endsWith(".project"));
                        if (dotProject != null && dotProject.length > 0) {
                            items.add(project.getName());
                        }
                    }
                }
            }
        }
        if (appList == null) {
            appList = new ListView<>();
        }
        appList.setItems(items);
    }

    // ── Actions ──

    private void onCreateProject() {
        String name = projNameField.getText().trim();
        if (!Validator.isValidName(name)) {
            showError("Invalid project name. Use only letters, numbers, spaces and underscores.");
            return;
        }
        String location = System.getProperty("user.dir") + File.separator + "Projects";
        File file = new File(location + File.separator + sanitizePathTraversal(name));
        if (file.exists()) {
            showError("Project already exists at this location.");
            return;
        }
        if (recentChanged) {
            AppSettings.store("Options Changed");
        }
        SwingUtilities.invokeLater(() -> {
            sMainFrame.createProject(name, location,
                    testDataTypeCombo.getSelectionModel().getSelectedItem());
            closeWindow();
            sMainFrame.adjustUI();
            sMainFrame.setVisible(true);
        });
    }

    private void loadProject(String location) {
        if (new File(location).exists()) {
            SwingUtilities.invokeLater(() -> {
                sMainFrame.loadProject(location);
                closeWindow();
                sMainFrame.adjustUI();
                sMainFrame.setVisible(true);
                if (recentChanged) {
                    AppSettings.store("Options Changed");
                }
            });
        }
    }

    private void closeWindow() {
        SwingUtilities.invokeLater(() -> {
            INGeniousFileChooser.OPEN_PROJECT.afterFileSelected = null;
            setVisible(false);
        });
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private static String sanitizePathTraversal(String filename) {
        Path p = Paths.get(filename);
        return p.getFileName().toString();
    }

    // ── Custom List Cells ──

    /**
     * A beautifully styled list cell for recent project items.
     */
    private static class RecentItemCell extends ListCell<RecentItem> {
        @Override
        protected void updateItem(RecentItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            FontIcon icon = new FontIcon(MaterialDesignF.FOLDER_COG);
            icon.setIconSize(20);
            icon.getStyleClass().add("startup-list-icon");

            Label nameLabel = new Label(item.getProjectName());
            nameLabel.getStyleClass().add("startup-list-name");

            Label pathLabel = new Label(item.getLocation());
            pathLabel.getStyleClass().add("startup-list-path");

            VBox textBox = new VBox(2, nameLabel, pathLabel);
            textBox.setAlignment(Pos.CENTER_LEFT);

            HBox cell = new HBox(12, icon, textBox);
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.setPadding(new Insets(6, 12, 6, 12));

            setGraphic(cell);
            setText(null);
        }
    }

    /**
     * A styled list cell for application/project directory items.
     */
    private static class ProjectItemCell extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            FontIcon icon = new FontIcon(MaterialDesignF.FOLDER);
            icon.setIconSize(20);
            icon.getStyleClass().add("startup-list-icon");

            Label nameLabel = new Label(item);
            nameLabel.getStyleClass().add("startup-list-name");

            HBox cell = new HBox(12, icon, nameLabel);
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.setPadding(new Insets(8, 12, 8, 12));

            setGraphic(cell);
            setText(null);
        }
    }
}
