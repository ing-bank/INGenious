package com.ing.ide.main.mainui.components.apitester.importing;

import com.ing.datalib.api.importer.ImportException;
import com.ing.datalib.api.importer.ImportOptions;
import com.ing.datalib.api.importer.ImportResult;
import com.ing.datalib.api.importer.ImportSource;
import com.ing.datalib.api.importer.ImportWarning;
import com.ing.datalib.api.importer.NormalizedCollection;
import com.ing.datalib.api.importer.bruno.BrunoImporter;
import com.ing.datalib.api.importer.postman.PostmanImporter;
import com.ing.datalib.api.importer.spi.CollectionImporter;
import com.ing.ide.main.mainui.AppMainFrame;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 * Orchestrates the "Tools → Import Collection" flow: opens the wizard, parses the
 * chosen source via an {@link CollectionImporter}, maps it through
 * {@link ReusableImportEngine}, and writes an HTML report with audit trail.
 */
public class ImportCollectionAction {
    private static final Logger LOG = Logger.getLogger(ImportCollectionAction.class.getName());

    private final AppMainFrame mainFrame;

    public ImportCollectionAction(AppMainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public void openWizard(ImportSource initial) {
        if (mainFrame.getProject() == null) {
            JOptionPane.showMessageDialog(
                mainFrame,
                "Please open a project before importing a collection.",
                "Import Collection",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        ImportCollectionWizard wiz = new ImportCollectionWizard(mainFrame, initial);
        wiz.setVisible(true);
        if (!wiz.isConfirmed()) return;

        final File file = wiz.getSelectedFile();
        final ImportSource source = wiz.getSource();
        final ImportOptions opts = wiz.getOptions();

        new SwingWorker<ImportResult, Void>() {
            NormalizedCollection nc;
            File report;
            Throwable failure;
            final List<ImportWarning> parseWarnings = new ArrayList<>();
            LocalDateTime startTime;
            LocalDateTime endTime;

            @Override
            protected ImportResult doInBackground() {
                startTime = LocalDateTime.now();
                try {
                    CollectionImporter importer = (source == ImportSource.BRUNO)
                        ? new BrunoImporter()
                        : new PostmanImporter();
                    nc = importer.parse(file, parseWarnings);
                    nc.setSource(source);
                    ReusableImportEngine engine = new ReusableImportEngine(
                        mainFrame.getAPITester(),
                        mainFrame.getProject()
                    );
                    ImportResult res = engine.importAsReusables(nc, opts);
                    res.getWarnings().addAll(0, parseWarnings);
                    endTime = LocalDateTime.now();
                    try {
                        report =
                            ImportReportWriter.write(
                                mainFrame.getProject().getLocation(),
                                nc,
                                res,
                                opts,
                                startTime,
                                endTime
                            );
                    } catch (Exception ex) {
                        LOG.log(Level.WARNING, "Failed to write import report", ex);
                    }
                    return res;
                } catch (ImportException ex) {
                    failure = ex;
                    return null;
                } catch (Exception ex) {
                    failure = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (failure != null) {
                    LOG.log(Level.SEVERE, "Collection import failed", failure);
                    JOptionPane.showMessageDialog(
                        mainFrame,
                        "Import failed: " + failure.getMessage(),
                        "Import Collection",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
                try {
                    ImportResult res = get();
                    if (res == null) return;

                    // Refresh all trees immediately after import
                    refreshAllTrees(opts.isImportEnvironments());

                    ImportCollectionWizard.showResult(mainFrame, nc, res, report);
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Import post-processing failed", ex);
                }
            }
        }
        .execute();
    }

    /**
     * Refreshes all project trees (Test Cases, Reusables, and Test Data)
     * to show newly imported items without requiring IDE restart.
     *
     * @param includeTestData whether to force refresh of Test Data environments
     */
    private void refreshAllTrees(boolean includeTestData) {
        javax.swing.SwingUtilities.invokeLater(
            () -> {
                try {
                    // Refresh project tree (Test Cases/Scenarios)
                    if (mainFrame.getTestDesign() != null) {
                        mainFrame.getTestDesign().getProjectTree().load();
                        mainFrame.getTestDesign().getReusableTree().load();

                        // Force reload of Test Data environments from disk
                        if (includeTestData) {
                            reloadTestDataFromDisk();
                        }
                        mainFrame.getTestDesign().getTestDatacomp().load();
                    }
                    // Refresh API Tester collection tree
                    if (
                        mainFrame.getAPITester() != null &&
                        mainFrame.getAPITester().getAPITesterUI() != null
                    ) {
                        mainFrame.getAPITester().getAPITesterUI().refreshCollectionsTree();
                    }
                    LOG.info("Successfully refreshed all project trees after import");
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Could not refresh trees after import", ex);
                }
            }
        );
    }

    /**
     * Forces a complete reload of the TestData component from disk.
     * This ensures newly created data environments are visible in the UI.
     */
    private void reloadTestDataFromDisk() {
        try {
            // Re-read the environment configuration from disk
            var project = mainFrame.getProject();
            if (project != null && project.getTestData() != null) {
                // Force the EnvTestData to reload its environment list from disk
                project.getTestData().reloadEnvironments();
                LOG.info("Reloaded test data environments from disk");
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to reload test data from disk", ex);
        }
    }
}
