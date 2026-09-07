# Complete End-to-End Workflow Example

## Scenario: User Wants to Upgrade INGenious with Customizations

This example demonstrates a complete workflow from customization detection through plugin creation to safe upgrade.

---

### Initial User Request

**User:** "I need to upgrade INGenious but I have customizations. How can I preserve them?"

---

### Step 1: Installation Identification

**Agent:**
```
I'll help you analyze your customizations and convert them to plugins so they're preserved during the upgrade.

Step 1: Let me detect your customizations first.

I've detected the following potential INGenious installations in your workspace:

Option 1: /Users/qs01nn/ing_code_repos/INGenious_project/Neil-ingenious-playwright-2.3
  └─ Contains: Configuration/, Engine/, Projects/

Option 2: /Users/qs01nn/ing_code_repos/INGenious_project/Github-Plugins-sys-INGenious
  └─ Contains: Common/, Datalib/, Engine/, IDE/

Which installation would you like to analyze for customizations?
(Or provide a different path if needed)
```

**User:** "Option 1"

**Agent:** 
```
✓ Confirmed: Analyzing /Users/qs01nn/ing_code_repos/INGenious_project/Neil-ingenious-playwright-2.3

Validating installation...
✓ Found: Configuration/conf.js
✓ Found: Engine/pom.xml
✓ Found: Projects/
✓ Found: Run.command

Valid INGenious installation detected.

Proceeding with version detection...
```

---

### Step 2: Version Detection

**Agent:**
```
Detecting version...

Found version 2.3 in Engine/pom.xml
✓ Version detected: 2.3
```

---

### Step 3: Copy Type Detection

**Agent:**
```
Determining installation type...

Checking for modules: Common, Datalib, IDE, StoryWriter...
Only Engine module found

✓ Installation type: BUILD_COPY (Engine module only)
```

---

### Step 4: Download Official Release

**Agent:**
```
Downloading official INGenious v2.3 for comparison...

Creating temporary directory: /tmp/ingenious-official-abc123
Cloning repository: https://github.com/ing-bank/INGenious.git
Checking out tag: v2.3

✓ Official release downloaded successfully
```

---

### Step 5: Comparison and Analysis

**Agent:**
```
Comparing your installation against official v2.3...

Analyzing Engine module...
Detecting changes...

Progress:
├─ Files scanned: 156
├─ Files modified: 23
├─ Files added: 5
└─ Files deleted: 0

Categorizing customizations...
├─ Feature Enhancements: 12 files
├─ Bug Fixes: 8 files
└─ Configuration: 3 files

✓ Analysis complete!
```

---

### Step 6: Report Generation

**Agent:**
```
Analysis complete! 

Summary:
- Files Modified: 23
- Files Added: 5
- Lines Added: 456
- Lines Removed: 89
- Customization Categories: 3

Report saved to: customization_report.md

Key Findings:
1. Feature Enhancements (12 files) - Custom browser actions added
2. Bug Fixes (8 files) - Null pointer exception handling
3. Configuration (3 files) - Modified POM dependencies

Would you like me to:
1. Open the detailed report?
2. Identify plugin extraction opportunities?
```

**User:** "Identify plugin opportunities"

---

### Step 7: Plugin Candidate Identification

**Agent:**
```
Analyzing customizations for plugin extraction...

Scanning for new action methods...
Analyzing imports and dependencies...
Detecting plugin types...

🔌 Plugin Extraction Analysis Complete!

I've identified 2 plugins that can be extracted from your customizations:

---

### Plugin 1: Custom Browser Actions ✅ High Priority
**Type:** Browser Plugin | **Complexity:** Moderate

**Actions Identified:**
1. clickWithHighlight() - Highlights element before clicking
2. hoverAndWait() - Hover with configurable wait time
3. scrollToBottom() - Scroll page to bottom with animation

**Source Files:**
- Engine/src/main/java/com/ing/engine/commands/WebCommands.java (lines 145-235)

**Dependencies:** None (uses only Playwright API)
**Estimated Effort:** 2-3 hours

---

### Plugin 2: Excel Data Helpers ✅ Medium Priority  
**Type:** General Plugin | **Complexity:** Simple

**Actions Identified:**
1. readExcelColumnValues() - Read all values from a column
2. compareExcelSheets() - Compare two Excel sheets

**Source Files:**
- Engine/src/main/java/com/ing/engine/utils/ExcelHelper.java (lines 89-156)

**Dependencies:** 
- Apache POI 5.2.3

**Estimated Effort:** 1-2 hours

---

I've analyzed your customizations and identified 2 that can be converted to plugins.
This will make upgrading much safer - your customizations will remain separate from the core framework.

Plugin 1: Custom Browser Actions (3 actions identified)
Plugin 2: Excel Data Helpers (2 actions identified)

Should I create these plugins so you can upgrade safely?
```

**User:** "Yes, create both"

---

### Plugin Creation (Plugin 1)

**Agent:**
```
Creating Plugin 1: Custom Browser Actions...

Loading ingenious-plugin-creation skill...

Creating browser plugin structure...
✓ Created directory: custom-browser-actions/

Generating pom.xml...
✓ Added dependency: BrowserPluginApi
✓ Configured maven-jar-plugin
✓ Configured maven-dependency-plugin

Creating plugin entry class...
✓ Created: CustomBrowserActionsPlugin.java

Implementing action methods...
✓ Implemented: clickWithHighlight()
✓ Implemented: hoverAndWait()
✓ Implemented: scrollToBottom()

✓ Plugin 1 created successfully!

Location: /Users/qs01nn/ing_code_repos/custom-browser-actions/
```

---

### Plugin Creation (Plugin 2)

**Agent:**
```
Creating Plugin 2: Excel Data Helpers...

Creating general plugin structure...
✓ Created directory: excel-data-helpers/

Generating pom.xml...
✓ Added dependency: CommandPluginApi
✓ Added dependency: Apache POI 5.2.3
✓ Configured build plugins

Creating plugin entry class...
✓ Created: ExcelDataHelpersPlugin.java

Implementing action methods...
✓ Implemented: readExcelColumnValues()
✓ Implemented: compareExcelSheets()

✓ Plugin 2 created successfully!

Location: /Users/qs01nn/ing_code_repos/excel-data-helpers/
```

---

### Final Summary and Next Steps

**Agent:**
```
Both plugins are ready! Here's your upgrade path:

✅ Customizations Extracted to Plugins:
   1. custom-browser-actions/ (3 actions)
   2. excel-data-helpers/ (2 actions)

📋 Upgrade Steps:

1. Build the plugins:
   cd custom-browser-actions && mvn clean package
   cd excel-data-helpers && mvn clean package

2. Download new INGenious version (e.g., v2.4)
   
3. Install plugins in new version:
   - Copy custom-browser-actions/target/*.jar → INGenious-2.4/plugins/custom-browser-actions/
   - Copy custom-browser-actions/target/lib/ → INGenious-2.4/plugins/custom-browser-actions/lib/
   - Copy excel-data-helpers/target/*.jar → INGenious-2.4/plugins/excel-data-helpers/
   - Copy excel-data-helpers/target/lib/ → INGenious-2.4/plugins/excel-data-helpers/lib/

4. Test in new version:
   - Launch INGenious 2.4
   - Verify actions appear in UI
   - Run your existing test scenarios

Your customizations are now safe and portable! 🎉

Would you like me to build these plugins now?
```

**User:** "Yes, build them"

---

### Building Plugins

**Agent:**
```
Building plugins...

Building custom-browser-actions...
$ cd custom-browser-actions && mvn clean package
[INFO] Scanning for projects...
[INFO] Building custom-browser-actions 1.0.0
[INFO] 
[INFO] --- maven-clean-plugin:3.1.0:clean ---
[INFO] --- maven-compiler-plugin:3.11.0:compile ---
[INFO] --- maven-jar-plugin:3.3.0:jar ---
[INFO] Building jar: custom-browser-actions/target/custom-browser-actions-1.0.0.jar
[INFO] --- maven-dependency-plugin:3.6.0:copy-dependencies ---
[INFO] BUILD SUCCESS

✓ custom-browser-actions built successfully!

Building excel-data-helpers...
$ cd excel-data-helpers && mvn clean package
[INFO] Scanning for projects...
[INFO] Building excel-data-helpers 1.0.0
[INFO] BUILD SUCCESS

✓ excel-data-helpers built successfully!

All plugins built and ready for deployment!

Artifacts:
- custom-browser-actions/target/custom-browser-actions-1.0.0.jar
- custom-browser-actions/target/lib/ (dependencies)
- excel-data-helpers/target/excel-data-helpers-1.0.0.jar
- excel-data-helpers/target/lib/ (dependencies including Apache POI)

You can now proceed with upgrading INGenious. Your customizations are safe! 🚀
```

---

## Summary

This workflow demonstrated:

1. ✅ **Interactive installation selection** with user confirmation
2. ✅ **Automatic version detection** from multiple sources
3. ✅ **Copy type determination** (Build vs Source)
4. ✅ **Official release download** for comparison
5. ✅ **Comprehensive diff analysis** with categorization
6. ✅ **Plugin candidate identification** with priorities
7. ✅ **Automated plugin creation** using integrated skill
8. ✅ **Build automation** for immediate deployment
9. ✅ **Clear upgrade path** with step-by-step instructions

**Key Benefits:**
- Customizations preserved as portable plugins
- Safe upgrade path without losing enhancements
- Reusable plugins across versions
- Clear documentation of what was customized
- Automated extraction and creation process
