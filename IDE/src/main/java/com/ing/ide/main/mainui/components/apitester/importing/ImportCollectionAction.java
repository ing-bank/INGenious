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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 * Orchestrates the "Tools → Import Collection" flow: opens the wizard, parses the
 * chosen source via an {@link CollectionImporter}, maps it through
 * {@link ReusableImportEngine}, and writes a Markdown report.
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

            @Override
            protected ImportResult doInBackground() {
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
                    try {
                        report =
                            ImportReportWriter.write(mainFrame.getProject().getLocation(), nc, res);
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
                    ImportCollectionWizard.showResult(mainFrame, nc, res, report);
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Import post-processing failed", ex);
                }
            }
        }
        .execute();
    }
}
