# User Interaction Flow

This document shows typical user interactions and expected agent responses for each step of the customization detection workflow.

---

## Step 1: Installation Identification (MANDATORY INTERACTION)

### Pattern 1: Multiple Installations Detected

**Agent presents options:**
```
I've detected the following potential INGenious installations in your workspace:

Option 1: /Users/qs01nn/ing_code_repos/INGenious_project/Neil-ingenious-playwright-2.3
  └─ Contains: Configuration/, Engine/, Projects/

Option 2: /Users/qs01nn/ing_code_repos/INGenious_project/Github-Plugins-sys-INGenious
  └─ Contains: Common/, Datalib/, Engine/, IDE/

Which installation would you like to analyze for customizations?
(Or provide a different path if needed)
```

**Valid User Responses:**
- "Option 1"
- "1"
- "Use Neil-ingenious-playwright-2.3"
- "The first one"
- "/Users/qs01nn/custom/path/to/ingenious"

**Agent validates and proceeds:**
```
✓ Confirmed: Analyzing /Users/qs01nn/.../Neil-ingenious-playwright-2.3

Validating installation...
[validation results]

Proceeding with Step 2: Detecting version...
```

---

### Pattern 2: Single Installation Detected

**Agent confirms:**
```
I've detected one INGenious installation:

Path: /Users/qs01nn/ing_code_repos/INGenious_project/INGenious-2.3
  └─ Contains: Configuration/, Engine/, Projects/, Common/, Datalib/

Is this the correct installation to analyze?
```

**Valid User Responses:**
- "Yes"
- "Correct"
- "That's right"
- "Yes, analyze that one"

**Agent proceeds:**
```
✓ Confirmed

Proceeding with version detection...
```

---

### Pattern 3: No Installation Auto-Detected

**Agent asks for path:**
```
I couldn't auto-detect an INGenious installation in the current workspace.

Please provide the path to your INGenious installation root directory:
```

**User provides path:**
```
/Users/qs01nn/my-custom-ingenious
```

**Agent validates:**
```
Validating path: /Users/qs01nn/my-custom-ingenious

✓ Found: Configuration/conf.js
✓ Found: Engine/pom.xml
✓ Found: Projects/

Valid INGenious installation detected.

Proceeding with version detection...
```

---

### Pattern 4: Invalid Path Provided

**User provides invalid path:**
```
/some/invalid/path
```

**Agent handles error:**
```
⚠️  Warning: Only 0 INGenious markers found in /some/invalid/path

This doesn't appear to be a valid INGenious installation.

Please confirm:
1. Is this the correct path?
2. Or provide an alternative path?
```

---

## Step 2-5: Automatic Processing

These steps run automatically after user confirms installation:

**Agent shows progress:**
```
✓ Step 1: Installation confirmed
⏳ Step 2: Detecting version... ✓ Version 2.3
⏳ Step 3: Determining copy type... ✓ BUILD_COPY
⏳ Step 4: Downloading official v2.3... ✓ Downloaded
⏳ Step 5: Comparing files... ✓ Complete
```

---

## Step 6: Report Review

**Agent presents summary:**
```
Analysis complete!

Summary:
- Files Modified: 23
- Lines Added: 456
- Lines Removed: 89
- Customization Categories: 3

Report saved to: customization_report.md

Key Findings:
1. Feature Enhancements (12 files) - Custom browser actions
2. Bug Fixes (8 files) - Null pointer exception handling
3. Configuration (3 files) - Modified POM dependencies

Would you like me to:
1. Open the detailed report?
2. Identify plugin extraction opportunities?
```

### User Response Pattern 1: View Report

**User:** "Open the report" or "Show me the report" or "1"

**Agent opens file and summarizes:**
```
[Opens customization_report.md]

Here are the key customizations found:

[Detailed breakdown...]

Would you like me to identify which of these can be extracted as plugins?
```

### User Response Pattern 2: Skip to Plugins

**User:** "Identify plugin opportunities" or "Can these be plugins?" or "2"

**Agent proceeds to Step 7:**
```
Analyzing customizations for plugin extraction...

[Continues to Step 7]
```

---

## Step 7: Plugin Identification

**Agent presents plugin candidates:**
```
🔌 Plugin Extraction Analysis Complete!

I've identified 2 plugins that can be extracted:

### Plugin 1: Custom Browser Actions ✅ High Priority
[Details...]

### Plugin 2: Excel Data Helpers ✅ Medium Priority
[Details...]

Would you like me to create these plugins? Reply with:
- "Create Plugin 1" for Custom Browser Actions
- "Create Plugin 2" for Excel Data Helpers
- "Create both plugins"
- "No, just show me the report"
```

### User Response Pattern 1: Create Specific Plugin

**User:** "Create Plugin 1" or "Create Custom Browser Actions"

**Agent creates plugin:**
```
Creating Custom Browser Actions plugin...

[Plugin creation process...]

✓ Plugin created successfully!

Would you like me to:
1. Create Plugin 2 as well?
2. Build Plugin 1?
3. Stop here?
```

### User Response Pattern 2: Create All Plugins

**User:** "Create both" or "Create all plugins" or "Create both plugins"

**Agent creates all:**
```
Creating all recommended plugins...

Creating Plugin 1: Custom Browser Actions...
✓ Created

Creating Plugin 2: Excel Data Helpers...
✓ Created

All plugins created successfully!

Would you like me to build them now?
```

### User Response Pattern 3: Review Details First

**User:** "Show me more details about Plugin 1" or "Tell me more about the browser actions plugin"

**Agent provides details:**
```
## Plugin 1: Custom Browser Actions

**Full Specification:**

Type: Browser Plugin
API Contract: BrowserPluginApi
Complexity: Moderate

Actions:
1. clickWithHighlight()
   - Purpose: Highlights element before clicking for visual debugging
   - Implementation: Uses Playwright's highlight() followed by click()
   - Parameters: Standard Object and Data columns
   - Source: Engine/src/main/java/com/ing/engine/commands/WebCommands.java:145-167

[More details...]

Should I create this plugin?
```

---

## Error Handling Interactions

### Error: Version Not Found

**Agent reports issue:**
```
⚠️  Warning: Could not detect version automatically

I checked:
- Engine/pom.xml
- Configuration/*.properties
- JAR manifests

Please specify the INGenious version you're using (e.g., 2.3, 2.4):
```

**User provides version:**
```
2.3
```

**Agent proceeds:**
```
✓ Using version 2.3

Proceeding with download of official release...
```

---

### Error: Git Not Available

**Agent reports issue:**
```
❌ Error: Git is not installed

To download the official release for comparison, Git is required.

Options:
1. Install Git and run this again
2. Provide a local path to official INGenious v2.3
3. Skip comparison (will only show what files are different from standard structure)

What would you like to do?
```

**User chooses option:**
```
I'll provide a local path: /Downloads/INGenious-2.3
```

**Agent proceeds:**
```
✓ Using local official copy at /Downloads/INGenious-2.3

Proceeding with comparison...
```

---

### Error: No Internet Connection

**Agent reports issue:**
```
⚠️  Warning: No internet connection detected

Cannot download official release from GitHub.

Options:
1. Check internet connection and try again
2. Provide local path to official INGenious
3. Continue without comparison (limited analysis)

What would you like to do?
```

---

## Quick Commands

Users can skip steps with direct commands:

**User:** "Analyze /path/to/ingenious and create all plugins"

**Agent:** 
```
✓ Analyzing /path/to/ingenious
[Runs all steps automatically]
✓ Analysis complete
✓ Identified 2 plugins
✓ Created all plugins

Summary: [Results]
```

**User:** "Just tell me what version I have"

**Agent:**
```
Detecting version in current workspace...

Found version 2.3 in Engine/pom.xml
```

**User:** "Compare my installation to official 2.3"

**Agent:**
```
Comparing to official v2.3...
[Runs Steps 4-6]
[Shows report]
```
