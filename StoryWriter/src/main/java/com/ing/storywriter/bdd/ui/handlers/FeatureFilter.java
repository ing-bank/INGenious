package com.ing.storywriter.bdd.ui.handlers;

import java.io.File;
import java.io.FileFilter;

/**
 *
 */
public class FeatureFilter implements FileFilter {
    private static final FileFilter FEATURE_FILTER = new FeatureFilter();

    public static FileFilter get() {
        return FEATURE_FILTER;
    }

    @Override
    public boolean accept(File file) {
        return file.isFile() && file.getName().endsWith(".feature");
    }
}
