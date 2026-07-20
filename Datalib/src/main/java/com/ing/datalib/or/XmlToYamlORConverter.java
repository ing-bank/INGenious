package com.ing.datalib.or;

import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.mobile.MobileORPage;
import com.ing.datalib.or.sap.SapOR;
import com.ing.datalib.or.sap.SapORPage;
import com.ing.datalib.or.structureddata.StructuredDataOR;
import com.ing.datalib.or.structureddata.StructuredDataORPage;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebORPage;
import com.ing.datalib.or.yaml.YamlORWriter;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Converts in-memory XML Object Repositories to YAML.
 *
 * Scope is determined by WHICH OR is passed:
 * - Project OR → ObjectRepository
 * - Shared OR  → Shared
 *
 * Pages do not carry scope.
 */
public class XmlToYamlORConverter {
    private static final Logger LOG = Logger.getLogger(XmlToYamlORConverter.class.getName());

    private final YamlORWriter yamlWriter;

    public XmlToYamlORConverter(YamlORWriter yamlWriter) {
        this.yamlWriter = yamlWriter;
    }

    /**
     * Convert all ORs to YAML with correct folder separation.
     */
    public void convertAll(
        WebOR projectWeb,
        WebOR sharedWeb,
        MobileOR projectMobile,
        MobileOR sharedMobile,
        StructuredDataOR projectStructuredData,
        StructuredDataOR sharedStructuredData,
        SapOR sapProjectOR,
        SapOR sapSharedOR,
        File projectRoot,
        File sharedRoot
    )
        throws IOException {
        writeProjectWeb(projectWeb, projectRoot);
        writeSharedWeb(sharedWeb, sharedRoot);

        writeProjectMobile(projectMobile, projectRoot);
        writeSharedMobile(sharedMobile, sharedRoot);

        writeProjectStructuredData(projectStructuredData, projectRoot);
        writeSharedStructuredData(sharedStructuredData, sharedRoot);

        writeProjectSap(sapProjectOR, projectRoot);
        writeSharedSap(sapSharedOR, sharedRoot);
    }

    private void writeProjectWeb(WebOR projectOR, File projectRoot) throws IOException {
        if (projectOR == null) return;

        File pagesDir = new File(projectRoot, "Web");
        pagesDir.mkdirs();

        LOG.info("Writing PROJECT Web OR to " + pagesDir.getAbsolutePath());

        for (WebORPage page : projectOR.getPages()) {
            yamlWriter.writeWebPage(page, pagesDir);
        }
    }

    private void writeProjectMobile(MobileOR projectOR, File projectRoot) throws IOException {
        if (projectOR == null) return;

        File pagesDir = new File(projectRoot, "Mobile");
        pagesDir.mkdirs();

        LOG.info("Writing PROJECT Mobile OR to " + pagesDir.getAbsolutePath());

        for (MobileORPage page : projectOR.getPages()) {
            yamlWriter.writeMobilePage(page, pagesDir);
        }
    }

    private void writeProjectStructuredData(StructuredDataOR projectOR, File projectRoot)
        throws IOException {
        if (projectOR == null) return;

        File pagesDir = new File(projectRoot, "StructuredData");
        pagesDir.mkdirs();

        LOG.info("Writing PROJECT Structured Data OR to " + pagesDir.getAbsolutePath());

        for (StructuredDataORPage page : projectOR.getPages()) {
            yamlWriter.writeStructuredDataPage(page, pagesDir);
        }
    }

    private void writeProjectSap(SapOR projectOR, File projectRoot) throws IOException {
        if (projectOR == null) return;

        File pagesDir = new File(projectRoot, "SAP");
        pagesDir.mkdirs();

        LOG.info("Writing PROJECT SAP OR to " + pagesDir.getAbsolutePath());

        for (SapORPage page : projectOR.getPages()) {
            yamlWriter.writeSapPage(page, pagesDir);
        }
    }

    private void writeSharedWeb(WebOR sharedOR, File sharedRoot) throws IOException {
        if (sharedOR == null) return;

        File pagesDir = new File(sharedRoot, "Web");
        pagesDir.mkdirs();

        LOG.info("Writing SHARED Web OR to " + pagesDir.getAbsolutePath());

        for (WebORPage page : sharedOR.getPages()) {
            yamlWriter.writeWebPage(page, pagesDir);
        }
    }

    private void writeSharedMobile(MobileOR sharedOR, File sharedRoot) throws IOException {
        if (sharedOR == null) return;

        File pagesDir = new File(sharedRoot, "Mobile");
        pagesDir.mkdirs();

        LOG.info("Writing SHARED Mobile OR to " + pagesDir.getAbsolutePath());

        for (MobileORPage page : sharedOR.getPages()) {
            yamlWriter.writeMobilePage(page, pagesDir);
        }
    }

    private void writeSharedStructuredData(StructuredDataOR sharedOR, File sharedRoot)
        throws IOException {
        if (sharedOR == null) return;

        File pagesDir = new File(sharedRoot, "StructuredData");
        pagesDir.mkdirs();

        LOG.info("Writing SHARED Structured Data OR to " + pagesDir.getAbsolutePath());

        for (StructuredDataORPage page : sharedOR.getPages()) {
            yamlWriter.writeStructuredDataPage(page, pagesDir);
        }
    }

    private void writeSharedSap(SapOR sharedOR, File sharedRoot) throws IOException {
        if (sharedOR == null) return;

        File pagesDir = new File(sharedRoot, "SAP");
        pagesDir.mkdirs();

        LOG.info("Writing SHARED SAP OR to " + pagesDir.getAbsolutePath());

        for (SapORPage page : sharedOR.getPages()) {
            yamlWriter.writeSapPage(page, pagesDir);
        }
    }
}
