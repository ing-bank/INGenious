package com.ing.datalib.component.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists and restores the user-defined sort order for scenarios and test cases.
 * <p>
 * A hidden {@code .sort_order} file (one name per line) is stored inside each directory that
 * the user has sorted. On load, the file is read and the in-memory list is reordered to match.
 * Names added after the last sort (new scenarios / test cases) are appended at the end.
 * Names removed from disk are silently ignored.
 * </p>
 */
public class SortOrderStore {
    private static final Logger LOGGER = Logger.getLogger(SortOrderStore.class.getName());
    static final String SORT_ORDER_FILE = ".sort_order";

    private SortOrderStore() {}

    /**
     * Saves the given ordered list of names to a {@code .sort_order} file inside
     * {@code directory}.  Creates the file if it does not exist; overwrites it otherwise.
     *
     * @param directory    the directory to write the sort file in (must be an existing directory)
     * @param orderedNames the names in the desired order
     */
    public static void save(File directory, List<String> orderedNames) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }
        File sortFile = new File(directory, SORT_ORDER_FILE);
        try {
            Files.write(sortFile.toPath(), orderedNames, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save sort order in " + directory.getPath(), e);
        }
    }

    /**
     * Applies the persisted sort order from {@code directory/.sort_order} to {@code names}.
     * <ul>
     *   <li>Names found in the sort file are placed first, in saved order.</li>
     *   <li>Names present in {@code names} but absent from the sort file (i.e. newly added
     *       items) are appended at the end, preserving their original relative order.</li>
     *   <li>Names in the sort file that no longer exist on disk are silently skipped.</li>
     * </ul>
     * Returns the original list unchanged when no sort file exists.
     *
     * @param directory the directory that may contain a {@code .sort_order} file
     * @param names     the current list of names (as loaded from disk)
     * @return a new list with the persisted order applied, or {@code names} if no sort file
     */
    public static List<String> apply(File directory, List<String> names) {
        if (directory == null) {
            return names;
        }
        File sortFile = new File(directory, SORT_ORDER_FILE);
        if (!sortFile.exists()) {
            return names;
        }
        try {
            List<String> savedOrder = Files.readAllLines(sortFile.toPath(), StandardCharsets.UTF_8);
            List<String> result = new ArrayList<>(names.size());
            for (String name : savedOrder) {
                if (names.contains(name)) {
                    result.add(name);
                }
            }
            for (String name : names) {
                if (!result.contains(name)) {
                    result.add(name);
                }
            }
            return result;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read sort order in " + directory.getPath(), e);
            return names;
        }
    }
}
