package com.ing.datalib.api.importer.spi;

import com.ing.datalib.api.importer.ImportException;
import com.ing.datalib.api.importer.ImportSource;
import com.ing.datalib.api.importer.ImportWarning;
import com.ing.datalib.api.importer.NormalizedCollection;

import java.io.File;
import java.util.List;

/**
 * SPI for parsing third-party API collection formats (Postman, Bruno, ...)
 * into INGenious's {@link NormalizedCollection} model.
 */
public interface CollectionImporter {

    /** Identifies which format this importer handles. */
    ImportSource source();

    /** Quick, cheap test (extension / sentinel file) of whether the given file/dir is supported. */
    boolean supports(File fileOrDir);

    /**
     * Parse the collection. Implementations append non-fatal issues to {@code warnings}
     * and throw {@link ImportException} only on hard failures.
     */
    NormalizedCollection parse(File fileOrDir, List<ImportWarning> warnings) throws ImportException;
}
