/*
 * Copyright 2014 - 2025 ING Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ing.ide.main.sapscript;

import java.util.Set;

/**
 * Constants used across the SAP script parser system.
 * Centralizes magic numbers, property names, and configuration values.
 */
public final class SapParserConstants {
    
    // Property indices (must match SapOR.OBJECT_PROPS order)
    public static final int PROP_INDEX_ID = 1;
    public static final int PROP_INDEX_NAME = 2;
    public static final int PROP_INDEX_TEXT = 3;
    
    // Property names
    public static final String PROP_ID = "id";
    public static final String PROP_NAME = "name";
    public static final String PROP_TEXT = "Text";
    
    // Action-related properties (excluded from generic property capture)
    // These are values being set/retrieved, not object identification properties
    public static final Set<String> ACTION_PROPERTIES = Set.of(
        "text", "selected", "key", "value", "caretposition", "currentcellrow",
        "caretPosition", "currentCellRow" // Include both case variations
    );
    
    // SAP control prefixes kept in object names for clarity
    // Reference list for documentation purposes
    public static final String[] OBJECT_PREFIXES = {
        "txt",      // Text field
        "btn",      // Button
        "cbo",      // Combo box
        "chk",      // Checkbox
        "tbl",      // Table
        "tab",      // Tab
        "usr",      // User area
        "wnd",      // Window
        "sub",      // Sub-screen
        "ctxt",     // Context
        "cmbBox",   // Combo box (alternative)
        "rad"       // Radio button
    };
    
    // Object name rules
    public static final boolean KEEP_PREFIX_IN_NAMES = true; // Keep SAP control type prefix for clarity
    
    // Special prefix for static value reference in test cases
    public static final String STATIC_VALUE_PREFIX = "@";
    
    // File validation limits
    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    public static final String MAX_FILE_SIZE_DISPLAY = "10MB";
    
    // Object Repository structure
    public static final String OR_OBJECT_GROUP_ELEMENT = "ObjectGroup";
    public static final String OR_OBJECT_ELEMENT = "Object";
    public static final String OR_PROPERTY_ELEMENT = "Properties";
    
    // Test Case structure
    public static final int TC_COLUMN_ACTION = 0;
    public static final int TC_COLUMN_DESCRIPTION = 1;
    public static final int TC_COLUMN_INPUT = 2;
    public static final int TC_COLUMN_CONDITION = 3;
    public static final int TC_COLUMN_OBJECTNAME = 4;
    public static final int TC_COLUMN_REFERENCE = 5;
    public static final String TC_PAGE_PREFIX = "SAP_";
    
    // Action type names
    public static final String ACTION_SET = "Set";
    public static final String ACTION_CLICK = "Click";
    public static final String ACTION_PRESS = "Press";
    public static final String ACTION_SEND_VKEY = "sendVKey";
    public static final String ACTION_DOUBLE_CLICK = "doubleClick";
    public static final String ACTION_SELECT = "select";
    
    // Logging
    public static final String LOG_PARSE_START = "Parsing SAP script file: ";
    public static final String LOG_OBJECT_CREATED = "Created SAP object: ";
    public static final String LOG_ACTION_PARSED = "Parsed SAP action: ";
    
    private SapParserConstants() {
        // Prevent instantiation
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }
}
