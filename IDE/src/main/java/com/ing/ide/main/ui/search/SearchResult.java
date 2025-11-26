package com.ing.ide.main.ui.search;

import javax.swing.Icon;

public class SearchResult {

    public enum FileType {
        TEST_CASE("Test Case"),
        REUSABLE("Reusable"),
        TEST_DATA("Test Data"),
        OBJECT_REPOSITORY("Object Repository");

        private final String displayName;

        FileType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final String name;
    private final String path;
    private final FileType type;
    private final Icon icon;

    public SearchResult(String name, String path, FileType type, Icon icon) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public FileType getType() {
        return type;
    }

    public Icon getIcon() {
        return icon;
    }

    @Override
    public String toString() {
        return name + " (" + type.getDisplayName() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SearchResult that = (SearchResult) obj;
        return path.equals(that.path) && type == that.type;
    }

    @Override
    public int hashCode() {
        return path.hashCode() * 31 + type.hashCode();
    }
}
