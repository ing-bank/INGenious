# Usage Examples

Complete end-to-end workflow examples demonstrating the customization detection skill.

## Example 1: Basic Customization Detection

### User Request
```
"Analyze my INGenious installation for customizations"
```

### Agent Response - Step 1

```
I'll help you detect customizations in your INGenious installation.

I've detected the following potential INGenious installations in your workspace:

Option 1: /Users/qs01nn/ing_code_repos/INGenious_project/Neil-ingenious-playwright-2.3
  └─ Contains: Configuration/, Engine/, Projects/
  
Option 2: /Users/qs01nn/ing_code_repos/INGenious_project/Github-Plugins-sys-INGenious
  └─ Contains: Common/, Datalib/, Engine/, IDE/

Which installation would you like to analyze for customizations?
(Or provide a different path if needed)
```

### User Response
```
"Option 1"
```

### Agent - Validation & Progression

```
✓ Confirmed: Analyzing /Users/qs01nn/ing_code_repos/INGenious_project/Neil-ingenious-playwright-2.3

Running validation...
✓ INGenious markers found: Configuration/, Engine/, Projects/

Proceeding with Step 2: Detecting version...
```

### Agent - Step 2

```
Running version detection script...
✓ Version detected: 2.3.0 (from Engine/pom.xml)
```

### Agent - Step 3

```
Determining installation type...
✓ Type detected: BUILD_COPY
  (Engine/ only, no Datalib or Common modules)
```

### Agent - Step 4

```
Downloading official INGenious v2.3.0 from GitHub...
Cloning repository...
✓ Successfully cloned and checked out tag v2.3.0
Official version saved to: /tmp/ingenious-official-20260523/
```

### Agent - Step 5

```
Comparing installations...
Module: Engine
  Files changed: 23
  Lines added: 456
  Lines removed: 89

Diff files saved to: /tmp/customization-analysis/diffs/
```

### Agent - Step 6

```
Generating customization report...

✓ Report generated: customization_report.md

Summary:
- Files Modified: 23
- Lines Added: +456
- Lines Removed: -89
- Categories Identified: 4

Key Findings:
1. Feature Enhancements (12 files) - Custom browser actions added
2. Bug Fixes (8 files) - Null pointer exception handling
3. Configuration (3 files) - Modified POM dependencies

Would you like me to:
1. Show the detailed report?
2. Identify plugin extraction opportunities?
```

### User Response
```
"Identify plugin opportunities"
```

### Agent - Step 7

```
Analyzing customizations for plugin extraction...

✓ Plugin analysis complete!

🔌 Plugin Extraction Recommendations:

I've identified 2 plugins that can be extracted from your customizations:

---

### Plugin 1: Custom Browser Actions
✅ **High Priority** | **Type:** Browser | **Complexity:** Moderate

**Actions Identified:**
1. `clickWithHighlight()` - Highlights element before clicking
2. `hoverAndWait()` - Hover with configurable wait time  
3. `scrollToBottom()` - Scroll page to bottom

**Source:** Engine/src/main/java/com/ing/engine/commands/WebCommands.java

**Dependencies:** None (uses Playwright API only)

**Estimated Effort:** 2-3 hours

---

### Plugin 2: Excel Data Helpers
⚠️ **Medium Priority** | **Type:** General | **Complexity:** Simple

**Actions Identified:**
1. `readColumnValues()` - Read all values from Excel column
2. `compareSheets()` - Compare two Excel sheets

**Source:** Engine/src/main/java/com/ing/engine/utils/ExcelHelper.java

**Dependencies:** Apache POI 5.2.3

**Estimated Effort:** 1-2 hours

---

Would you like me to create these plugins?
Reply with:
- "Create Plugin 1"
- "Create Plugin 2"
- "Create both plugins"
```

---

## Example 2: Plugin Creation Workflow

### Continuing from Example 1...

### User Request
```
"Create Plugin 1"
```

### Agent Response

```
Creating Custom Browser Actions plugin...

Loading ingenious-plugin-creation skill...

Step 1: Creating project structure...
✓ Created: custom-browser-actions/
✓ Created: custom-browser-actions/src/main/java/com/custom/plugins/

Step 2: Generating POM file...
✓ Generated: pom.xml with BrowserPluginApi dependency

Step 3: Creating plugin entry class...
✓ Created: CustomBrowserActionsPlugin.java

Step 4: Implementing action methods...
✓ Implemented: clickWithHighlight()
✓ Implemented: hoverAndWait()
✓ Implemented: scrollToBottom()

Step 5: Configuring build plugins...
✓ Configured: maven-jar-plugin with manifest
✓ Configured: maven-dependency-plugin for lib/

✓ Plugin created successfully!

Location: /Users/qs01nn/ing_code_repos/INGenious_project/custom-browser-actions/

Next steps:
1. Build plugin: cd custom-browser-actions && mvn clean package
2. Deploy: Copy target/*.jar and target/lib/ to INGenious/plugins/custom-browser-actions/
3. Test: Launch INGenious and verify actions appear in Object dropdown

Would you like me to build the plugin now?
```

---

## Example 3: Upgrade Preservation Scenario

### User Request
```
"I need to upgrade INGenious but I have customizations. How can I preserve them?"
```

### Agent Response - Initial

```
I'll help you analyze your customizations and convert them to plugins so they're preserved during the upgrade.

Step 1: Let me detect your customizations first.

Which INGenious installation should I analyze?

Option 1: /Users/qs01nn/ing_code_repos/INGenious_project/My-INGenious-2.3
Option 2: /Users/qs01nn/ing_code_repos/INGenious_project/Test-INGenious
```

### User Response
```
"Option 1"
```

### Agent - Running Full Analysis

```
✓ Confirmed: /Users/qs01nn/ing_code_repos/INGenious_project/My-INGenious-2.3

Analyzing installation...
✓ Version: 2.3.0
✓ Type: BUILD_COPY
✓ Downloaded official v2.3.0
✓ Comparison complete

Generating customization report...
✓ Report generated

Identifying plugin candidates...
✓ Analysis complete

---

📊 Customization Analysis Summary:

**Total Changes:**
- 45 files modified
- 2,340 lines added
- 456 lines removed

**Categories:**
- Feature Enhancements: 18 files
- Bug Fixes: 12 files
- Configuration: 10 files
- Performance: 5 files

**Risk Assessment:**
- High Impact: 5 files (core framework changes)
- Medium Impact: 15 files (feature additions)
- Low Impact: 25 files (minor changes)

---

🔌 Plugin Opportunities:

I've identified 3 customizations that can be converted to plugins:

1. **Custom Browser Actions** (High Priority)
   - 4 new action methods
   - Browser plugin type
   
2. **API Testing Extensions** (Medium Priority)
   - 3 webservice actions
   - Webservice plugin type

3. **Database Validators** (Medium Priority)
   - 2 database actions
   - Database plugin type

**Recommendations for Upgrade:**

✅ **Convert to Plugins:** Plugins 1, 2, and 3
  → These are self-contained and work independently

⚠️ **Keep as Customizations:** 5 high-impact core framework changes
  → These modify framework internals and cannot be pluginized
  → You'll need to reapply these after upgrade

Should I create these plugins so you can upgrade safely?
```

### User Response
```
"Yes, create all three plugins"
```

### Agent - Creating Plugins

```
Creating plugins...

[1/3] Creating Custom Browser Actions plugin...
✓ Plugin created: custom-browser-actions/

[2/3] Creating API Testing Extensions plugin...
✓ Plugin created: api-testing-extensions/

[3/3] Creating Database Validators plugin...
✓ Plugin created: database-validators/

---

✓ All plugins created successfully!

📦 Your Upgrade Plan:

**Phase 1: Build Plugins**
```bash
cd custom-browser-actions && mvn clean package
cd api-testing-extensions && mvn clean package
cd database-validators && mvn clean package
```

**Phase 2: Backup Current Installation**
```bash
cp -r My-INGenious-2.3 My-INGenious-2.3-backup
```

**Phase 3: Upgrade INGenious**
1. Download INGenious v3.0 (or desired version)
2. Extract to new directory
3. Copy your Configuration/ folder
4. Copy your Projects/ folder

**Phase 4: Deploy Plugins**
```bash
cp -r custom-browser-actions/target/custom-browser-actions-1.0.jar \
     new-ingenious/plugins/custom-browser-actions/
cp -r custom-browser-actions/target/lib \
     new-ingenious/plugins/custom-browser-actions/
# Repeat for other plugins
```

**Phase 5: Reapply High-Impact Customizations** ⚠️
- 5 core framework changes identified
- See: high_impact_customizations.md
- These must be manually ported to new version

**Phase 6: Test**
- Run your test suite
- Verify plugin actions work
- Validate high-impact customizations

Would you like me to:
1. Build all plugins now?
2. Generate detailed migration guide?
3. Show the high-impact customizations that need manual porting?
```

---

## Example 4: Team Collaboration Use Case

### User Request
```
"Generate a report of our team's INGenious customizations for documentation"
```

### Agent Response

```
I'll analyze your installation and generate a comprehensive customization report.

Detected: /Users/qs01nn/team-shared/INGenious-Team (current directory)

Proceed with analysis? (yes/no)
```

### User Response
```
"yes"
```

### Agent - Analysis & Report Generation

```
Running customization analysis...

✓ Version: 2.3.1
✓ Type: SOURCE_CODE_COPY (full multi-module project)
✓ Downloaded official v2.3.1
✓ Comparing all modules: Common, Datalib, Engine, IDE, StoryWriter

Generating comprehensive report...

✓ Report generated: team_customizations_report.md
✓ Detailed diffs: /tmp/diffs/ (7 modules)
✓ Statistics: customization_stats.json

---

📊 Team Customization Report Generated

**Summary:**
- 78 files customized across 5 modules
- 4,567 lines added
- 892 lines removed

**By Module:**
- Engine: 45 files (primary customizations)
- Datalib: 15 files (data handling enhancements)
- Common: 10 files (utility additions)
- IDE: 5 files (UI improvements)
- StoryWriter: 3 files (BDD enhancements)

**By Category:**
- Feature Enhancements: 35 files
- Bug Fixes: 20 files
- Performance Optimizations: 10 files
- Configuration: 8 files
- Framework Core: 5 files

**Plugin Opportunities:** 6 potential plugins identified

---

The report includes:
✓ Functional grouping of customizations
✓ Risk assessment for each change
✓ Plugin extraction recommendations
✓ Upgrade impact analysis
✓ Full diff files for code review

Report location: team_customizations_report.md

Would you like me to:
1. Share the report summary in markdown format?
2. Generate plugin specifications?
3. Create a migration guide for next version upgrade?
```

---

## Common Usage Patterns

### Quick Version Check

```
User: "What version of INGenious is this?"
Agent: [Runs detect-version.sh]
Agent: "Version 2.3.0 detected from Engine/pom.xml"
```

### Installation Type Check

```
User: "Is this a source copy or build copy?"
Agent: [Runs detect-copy-type.sh]
Agent: "SOURCE_CODE_COPY - contains Datalib, Common, Engine, IDE modules"
```

### Comparison Only

```
User: "Compare my installation against official 2.3"
Agent: [Runs Steps 1-5, skips Step 6-7]
Agent: "Comparison complete. 23 files differ. See: /tmp/diffs/"
```

### Plugin Analysis Only

```
User: "Can I extract plugins from my customizations?"
Agent: [Runs Steps 1-7, focuses on Step 7]
Agent: "2 plugin candidates identified: [details]"
```
