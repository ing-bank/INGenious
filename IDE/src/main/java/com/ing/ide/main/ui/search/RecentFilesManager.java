package com.ing.ide.main.ui.search;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class RecentFilesManager {

    private static final int MAX_RECENT_FILES = 12;
    private final LinkedHashSet<SearchResult> recentFiles = new LinkedHashSet<>();

    public void addRecentFile(SearchResult result) {

        recentFiles.remove(result);

        LinkedHashSet<SearchResult> newSet = new LinkedHashSet<>();
        newSet.add(result);
        newSet.addAll(recentFiles);

        recentFiles.clear();

        // Keep only the last MAX_RECENT_FILES items
        int count = 0;
        for (SearchResult r : newSet) {
            if (count >= MAX_RECENT_FILES) {
                break;
            }
            recentFiles.add(r);
            count++;
        }
    }

    public List<SearchResult> getRecentFiles() {
        return new ArrayList<>(recentFiles);
    }

    public void clear() {
        recentFiles.clear();
    }

    public boolean hasRecentFiles() {
        return !recentFiles.isEmpty();
    }
}
