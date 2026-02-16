package com.ing.ide.main.fx;

import com.ing.engine.support.DesktopApi;
import com.ing.ide.main.mainui.components.testexecution.TestExecution;
import com.ing.ide.main.mainui.components.testexecution.tree.model.ReleaseNode;
import com.ing.ide.main.mainui.components.testexecution.tree.model.TestSetNode;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import java.awt.BorderLayout;
import java.io.File;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Full JavaFX DashBoard replacing the old Swing DashBoard.
 * Contains a TreeView (test sets) and WebView (HTML reports)
 * within a styled SplitPane, all wrapped in a JFXPanel.
 */
public class FXDashBoard extends javax.swing.JPanel {

    private static final Logger LOG = Logger.getLogger(FXDashBoard.class.getName());

    private static final String ERR_HTML = "file:///"
            + System.getProperty("user.dir") + File.separator + "Configuration"
            + File.separator + "err.html";

    private final TestExecution testExecution;
    private final JFXPanel fxPanel;

    // JavaFX components (must be accessed on FX thread)
    private TreeView<DashBoardItem> treeView;
    private WebView webView;
    private WebEngine webEngine;
    private javafx.scene.control.ToolBar navToolBar;

    private String release = "";
    private String testSet = "";

    public FXDashBoard(TestExecution testExecution) {
        this.testExecution = testExecution;
        this.fxPanel = new JFXPanel();
        setLayout(new BorderLayout());
        // Don't add fxPanel yet — wait until load() sets the scene
        // to prevent macOS NSTrackingRectTag crash.
    }

    /**
     * Initializes the JavaFX scene. Must be called after the toolkit is ready.
     * Blocks until the scene is set to prevent Glass native crashes on macOS.
     */
    public void load() {
        CountDownLatch sceneReady = new CountDownLatch(1);
        Platform.runLater(() -> {
            initFX();
            sceneReady.countDown();
        });
        try {
            sceneReady.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        add(fxPanel, BorderLayout.CENTER);
    }

    private void initFX() {
        // ── Left: Tree with header ──
        treeView = new TreeView<>();
        treeView.setShowRoot(true);
        treeView.getStyleClass().add("dashboard-tree");
        treeView.setCellFactory(tv -> new DashBoardTreeCell());
        treeView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> onTreeSelectionChanged(newVal));

        Label treeHeader = new Label("Test Lab");
        treeHeader.getStyleClass().add("panel-header-label");

        VBox treeContainer = new VBox(treeHeader, treeView);
        VBox.setVgrow(treeView, Priority.ALWAYS);
        treeContainer.getStyleClass().add("panel-container");

        // ── Right: WebView with navigation toolbar ──
        webView = new WebView();
        webEngine = webView.getEngine();
        webView.getStyleClass().add("dashboard-webview");

        navToolBar = createNavToolBar();

        VBox browserContainer = new VBox(navToolBar, webView);
        VBox.setVgrow(webView, Priority.ALWAYS);
        browserContainer.getStyleClass().add("panel-container");

        // ── SplitPane ──
        SplitPane splitPane = new SplitPane(treeContainer, browserContainer);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.22);
        splitPane.getStyleClass().add("dashboard-split");
        SplitPane.setResizableWithParent(treeContainer, false);

        BorderPane root = new BorderPane(splitPane);
        root.getStyleClass().add("light-theme");

        Scene scene = new Scene(root);
        FXTheme.registerScene(scene);
        fxPanel.setScene(scene);
    }

    private javafx.scene.control.ToolBar createNavToolBar() {
        Button backBtn = createNavButton("Navigate Back", "back");
        backBtn.setOnAction(e -> goBack());

        Button latestBtn = createNavButton("Latest Summary", "latestSummary");
        latestBtn.setOnAction(e -> loadPage(true));
        latestBtn.getStyleClass().add("nav-summary-btn");

        Button detailedBtn = createNavButton("Detailed Summary", "detailedSummary");
        detailedBtn.setOnAction(e -> loadPage(false));
        detailedBtn.getStyleClass().add("nav-summary-btn");

        Button forwardBtn = createNavButton("Navigate Forward", "forward");
        forwardBtn.setOnAction(e -> goForward());

        Button openBtn = createNavButton("Open in Browser", "openInBrowser");
        openBtn.setOnAction(e -> openInBrowser());

        Pane spacerLeft = new Pane();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        Pane spacerRight = new Pane();
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        javafx.scene.control.ToolBar toolbar = new javafx.scene.control.ToolBar(
                spacerLeft, backBtn, new Separator(), latestBtn,
                new Separator(), detailedBtn, new Separator(),
                forwardBtn, spacerRight, openBtn
        );
        toolbar.getStyleClass().add("dashboard-nav-toolbar");
        return toolbar;
    }

    private Button createNavButton(String tooltip, String iconName) {
        Button btn = new Button();
        btn.setTooltip(new Tooltip(tooltip));

        org.kordamp.ikonli.javafx.FontIcon icon = INGIcons.fxColored(iconName, 20);
        if (icon != null) {
            btn.setGraphic(icon);
        } else {
            btn.setText(tooltip);
        }
        return btn;
    }

    // ── Tree Data Loading ──

    /**
     * Loads (or reloads) the tree from the TestSetTree's Swing model.
     * Called after project change.
     */
    public void loadTree() {
        Platform.runLater(() -> {
            TreeModel swingModel = testExecution.getTestSetTree().getTree().getModel();
            TreeItem<DashBoardItem> root = convertSwingTreeModel(swingModel);
            treeView.setRoot(root);
            root.setExpanded(true);
            // Select first test set if available
            selectFirstTestSet(root);
        });
    }

    private TreeItem<DashBoardItem> convertSwingTreeModel(TreeModel model) {
        Object rootObj = model.getRoot();
        DashBoardItem rootItem = new DashBoardItem(
                rootObj.toString(), DashBoardItem.Type.ROOT, null, null);
        TreeItem<DashBoardItem> rootTreeItem = new TreeItem<>(rootItem);
        rootTreeItem.setExpanded(true);

        int childCount = model.getChildCount(rootObj);
        for (int i = 0; i < childCount; i++) {
            Object child = model.getChild(rootObj, i);
            addSwingNodeToFXTree(rootTreeItem, child, model);
        }
        return rootTreeItem;
    }

    private void addSwingNodeToFXTree(TreeItem<DashBoardItem> parent, Object node, TreeModel model) {
        DashBoardItem.Type type;
        String releaseName = null;
        String testSetName = null;

        if (node instanceof ReleaseNode) {
            type = DashBoardItem.Type.RELEASE;
            releaseName = node.toString();
        } else if (node instanceof TestSetNode) {
            type = DashBoardItem.Type.TEST_SET;
            TreeNode parentNode = ((TestSetNode) node).getParent();
            releaseName = parentNode != null ? parentNode.toString() : "";
            testSetName = node.toString();
        } else {
            type = DashBoardItem.Type.ROOT;
        }

        DashBoardItem item = new DashBoardItem(node.toString(), type, releaseName, testSetName);
        TreeItem<DashBoardItem> treeItem = new TreeItem<>(item);
        treeItem.setExpanded(true);
        parent.getChildren().add(treeItem);

        int childCount = model.getChildCount(node);
        for (int i = 0; i < childCount; i++) {
            Object child = model.getChild(node, i);
            addSwingNodeToFXTree(treeItem, child, model);
        }
    }

    private void selectFirstTestSet(TreeItem<DashBoardItem> root) {
        if (root == null) return;
        for (TreeItem<DashBoardItem> release : root.getChildren()) {
            if (!release.getChildren().isEmpty()) {
                TreeItem<DashBoardItem> firstSet = release.getChildren().get(0);
                treeView.getSelectionModel().select(firstSet);
                return;
            }
        }
    }

    // ── Tree Selection ──

    private void onTreeSelectionChanged(TreeItem<DashBoardItem> selected) {
        if (selected == null || selected.getValue() == null) {
            webEngine.load(ERR_HTML);
            return;
        }

        DashBoardItem item = selected.getValue();
        if (item.type == DashBoardItem.Type.TEST_SET) {
            release = item.releaseName != null ? item.releaseName : "";
            testSet = item.testSetName != null ? item.testSetName : "";
            loadPage(true);
        } else {
            webEngine.load(ERR_HTML);
        }
    }

    // ── Page Navigation ──

    private void loadPage(boolean latest) {
        if (webEngine == null) return;
        String url = latest ? getDetailedSummary() : getHistory();
        if (checkFile(url)) {
            webEngine.load(url);
        } else {
            webEngine.load(ERR_HTML);
        }
    }

    private String getDetailedSummary() {
        return getPrefix() + File.separator + release + File.separator + testSet
                + File.separator + "Latest"
                + File.separator + "summary.html";
    }

    private String getHistory() {
        return getPrefix() + File.separator + release + File.separator + testSet
                + File.separator + "ReportHistory.html";
    }

    private String getPrefix() {
        return "file:///"
                + testExecution.getProject().getLocation()
                + File.separator + "Results"
                + File.separator + "TestExecution";
    }

    private void goBack() {
        if (webEngine.getHistory().getCurrentIndex() > 0) {
            webEngine.getHistory().go(-1);
        }
    }

    private void goForward() {
        int current = webEngine.getHistory().getCurrentIndex();
        int size = webEngine.getHistory().getEntries().size();
        if (current < size - 1) {
            webEngine.getHistory().go(1);
        }
    }

    private void openInBrowser() {
        try {
            String url = webEngine.getLocation();
            if (url != null && url.contains(".html")) {
                url = url.substring(0, url.indexOf(".html") + 5);
                if (url.endsWith("detailed.html")) {
                    url = url.replace("detailed.html", "summary.html");
                }
                DesktopApi.browse(new URI(url));
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Failed to open in browser", ex);
        }
    }

    public void closeBrowser() {
        // WebView is managed by JavaFX — no explicit close needed
    }

    private boolean checkFile(String url) {
        String fAddr = url.replace("file:///", "");
        File f = new File(fAddr);
        return f.exists() && f.isFile();
    }

    // ── Data Model ──

    /**
     * Represents a node in the DashBoard tree.
     */
    static class DashBoardItem {
        enum Type { ROOT, RELEASE, TEST_SET }

        final String label;
        final Type type;
        final String releaseName;
        final String testSetName;

        DashBoardItem(String label, Type type, String releaseName, String testSetName) {
            this.label = label;
            this.type = type;
            this.releaseName = releaseName;
            this.testSetName = testSetName;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // ── Tree Cell with Icons ──

    private class DashBoardTreeCell extends TreeCell<DashBoardItem> {

        @Override
        protected void updateItem(DashBoardItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(item.label);
                switch (item.type) {
                    case ROOT:
                        setGraphic(INGIcons.fxColored("testlab.Root", 16));
                        break;
                    case RELEASE:
                        setGraphic(INGIcons.fxColored("testlab.Release", 16));
                        break;
                    case TEST_SET:
                        setGraphic(INGIcons.fxColored("testlab.TestSet", 16));
                        break;
                }
            }
        }
    }
}
