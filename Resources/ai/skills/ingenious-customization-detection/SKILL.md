---
name: ingenious-customization-detection
description: 'Detect and analyze customizations made to INGenious framework by users. USE FOR: detecting code modifications, identifying custom enhancements, comparing user version against official release, analyzing build vs source code copies, generating customization reports, identifying version differences, tracking user modifications, extracting customizations as plugins. INCLUDES: version detection, repository comparison, git diff analysis, customization reporting with functional grouping, plugin candidate identification. OUTPUTS: structured plugin specifications for use with ingenious-plugin-creation skill.'
argument-hint: 'Describe which INGenious installation to analyze or confirm the root folder'
user-invocable: true
version: "2.0.0"
requires:
  ingenious: ">=3.1.0 <3.2.0"
metadata:
  author: ingenious-team
  category: code-generation
---
---

> **3.1.x refresh:** detect versions in the `3.1.x` range; the official release lives at
> `https://github.com/ing-bank/INGenious` (match the user's branch/tag). Source-copy
> modules are `Datalib/ Common/ Engine/ IDE/ StoryWriter/ TestData - Csv/ ingenious-api/`.
> Treat `Dist/release/ai/skills/**` (shipped AI skills) and `Dist/release/ai/**` as
> framework assets, NOT user customizations, when diffing.

# INGenious Customization Detection Skill

## When to Use This Skill

Use this skill to detect customizations or modifications made to INGenious framework, compare a user's installation against the official release, identify version and copy type, generate customization reports, track modifications across modules, and extract customizations as plugins for safe upgrades.

## Key Principles

**🔴 MANDATORY: This is an INTERACTIVE workflow**

- **NEVER assume** which folder to analyze - **ALWAYS confirm** with the user first
- **STOP at Step 1** until user explicitly confirms the target installation
- Each step requires validation before proceeding to ensure accuracy

## Decision Flow

```
1. Identify Repository → 2. Detect Version → 3. Determine Copy Type → 
4. Download Official Release → 5. Compare & Detect Changes → 6. Generate Report →
7. Identify Plugin Candidates (Optional)
```

**Skill Integration:** This skill can invoke the `ingenious-plugin-creation` skill to convert customizations into plugins.

---

## Workflow Steps

### Step 1: Identify INGenious Repository

**CRITICAL: ALWAYS verify with user before proceeding to Step 2**

**Workflow:**

1. **Detect Available Options** - List workspace folders and identify those with INGenious markers
   
2. **Present Options to User:**
   ```
   I've detected the following potential INGenious installations:
   
   Option 1: [path1]
   Option 2: [path2]
   
   Which installation would you like to analyze for customizations?
   ```

3. **STOP and Wait for User Response** - Do NOT proceed without explicit confirmation

4. **Validate User's Choice** using `scripts/validate_ingenious_root.sh`

**INGenious Markers to Check:**
- Configuration/ directory with conf.js or XPLOR_SETTINGS.json
- Engine/ directory with pom.xml
- Projects/ directory
- Run.bat or Run.command files

**Expected User Responses:** "Option 1", "Use [name]", "[path]", "Yes, that's correct"

**Reference:** See [examples/user_interaction_flow.md](examples/user_interaction_flow.md) for detailed interaction patterns.

**Script:** Use `scripts/validate_ingenious_root.sh <path>` to validate installation.

---

### Step 2: Detect Version

**Action:** Identify the INGenious version using multi-strategy detection.

**Detection Strategies:**
1. Check Engine/pom.xml for `<version>` tag
2. Check Configuration/package.properties
3. Check JAR manifest files (if built)
4. Check Git tags (if git repository)

**Common Version Patterns:** `2.3`, `2.3.0`, `v2.3`, `release-2.3`

**Script:** Use `scripts/detect_version.sh <path>` for automated detection.

If version cannot be auto-detected, ask user to specify version manually.

---

### Step 3: Determine Copy Type

**Action:** Determine if installation is **Source Code Copy** or **Build Copy**.

| Copy Type | Indicators | Modules |
|-----------|-----------|---------|
| **Source Code Copy** | Multiple Maven modules | Datalib/, Common/, Engine/, IDE/, StoryWriter/ |
| **Build Copy** | Only Engine module | Engine/ only with Configuration/, Projects/, lib/ |

**Script:** Use `scripts/detect_copy_type.sh <path>` for automated detection.

**Reference:** See [examples/module_structures.md](examples/module_structures.md) for detailed structure comparison.

---

### Step 4: Download Official INGenious Release

**Action:** Clone official repository and checkout matching version.

**Prerequisites:** Git installed, internet connection, ~100-500MB disk space

**Process:**
1. Create temporary directory
2. Clone `https://github.com/ing-bank/INGenious.git`
3. Checkout tag matching detected version (try: `v{VERSION}`, `{VERSION}`, `release-{VERSION}`)
4. Verify checkout success

**Script:** Use `scripts/download_official_release.sh <version> [output_dir]`

**Error Handling:**
- If exact tag not found: List available tags, ask user which to use
- If git unavailable: Request local path to official copy or install git
- If no internet: Use local official copy or skip comparison

**Reference:** Use `scripts/error_handling_utils.sh` for validation checks.

---

### Step 5: Compare and Detect Changes

**Action:** Systematically compare user's installation against official release.

**For Source Code Copy:**
- Compare all modules: Common, Datalib, Engine, IDE, StoryWriter, ingenious-api, TestData - Csv

**For Build Copy:**
- Compare Engine module only

**Comparison Process:**
1. Quick file-level comparison: `scripts/compare_modules.sh <user_path> <official_path> <copy_type>`
2. Detailed diff generation: `scripts/generate_diff.sh <user_path> <official_path> <module>`

**Whitespace Handling:**
All comparison and diff operations ignore changes in the amount of whitespace (using the `-b` flag for `diff`). This ensures that only substantive code changes are reported, not formatting-only changes.

**Example diff command:**
```bash
diff -Naur -b --exclude='target' --exclude='*.class' --exclude='.git' ...
```

**Exclusions:** target/, *.class, .git, .idea, *.iml, *.log

**Analysis:**
- Count files modified, added, deleted
- Calculate lines added/removed (excluding whitespace-only changes)
- Categorize changes by pattern matching

**Categorization:** Use `scripts/categorize_changes.py <diff_file>` for automatic categorization.

**Reference:** See [references/categorization_rules.md](references/categorization_rules.md) for category definitions.

---

### Step 6: Generate Customization Report

**Action:** Create comprehensive markdown report of all customizations.

**Report Sections:**
1. **Summary** - Installation path, version, copy type, analysis date
2. **Overview Statistics** - Files changed, lines added/removed, categories
3. **Detailed Changes by Module** - Per-module breakdown with categorization
4. **Customization Categories** - Grouped by: Feature Enhancements, Bug Fixes, Configuration, Integration, Performance, UI/Reporting, Framework Core
5. **Risk Assessment** - Impact levels (High/Medium/Low)
6. **Recommendations** - Upgrade path, plugin candidates, contribution opportunities

**Template:** Use [references/customization_report_template.md](references/customization_report_template.md)

**Output Artifacts:**
- `customization_report.md` - Main report
- `diff_<Module>.patch` - Detailed diffs per module
- `stats.json` - Statistics summary (schema: [references/statistics_schema.json](references/statistics_schema.json))
- `files_added.txt`, `files_modified.txt`, `files_deleted.txt` - File lists

**Impact Assessment:**
- **High Impact:** Core engine changes, API contract modifications, plugin system changes
- **Medium Impact:** Feature additions, integration points, configuration changes
- **Low Impact:** Bug fixes, minor enhancements, formatting

---

### Step 7: Identify Plugin Candidates (Optional)

**Action:** Analyze customizations and identify which should be extracted as plugins.

**When to Execute:**
- After Step 6 completion
- When user asks to "extract customizations to plugins"
- When preparing for INGenious upgrade

**Plugin Candidate Criteria:**
1. ✅ Adds new functionality (not modifies existing core)
2. ✅ Self-contained (minimal dependencies on framework internals)
3. ✅ Provides reusable actions
4. ✅ Located in appropriate modules (new methods in Engine)

**Not Suitable for Plugins:**
- ❌ Modifications to existing framework methods
- ❌ Changes requiring access to private framework internals
- ❌ Project-specific hacks without general utility

**Detection Process:**
1. Identify new methods: `scripts/identify_new_methods.sh <user_path> <official_path> <module>`
2. Analyze imports to determine plugin type
3. Extract action specifications
4. Generate plugin spec: `scripts/generate_plugin_spec.py <customization_data.json>`

**Plugin Type Detection:**

| Import Pattern | Plugin Type | API Contract |
|---------------|-------------|--------------|
| `com.microsoft.playwright.*` | Browser | BrowserPluginApi |
| `io.appium.*` | Mobile | MobilePluginApi |
| `java.net.http.*` | Webservice | WebservicePluginApi |
| `java.sql.*` | Database | DatabasePluginApi |
| None specific | General | CommandPluginApi |

**Output Format:**

Present recommendations using template: [references/plugin_recommendation_template.md](references/plugin_recommendation_template.md)

Generate machine-readable spec using: [references/plugin_specification_template.json](references/plugin_specification_template.json)

**Cross-Skill Integration:**

When user confirms plugin creation:
1. Load the `ingenious-plugin-creation` skill
2. Provide: plugin type, action specifications, source code, dependencies
3. The plugin creation skill will generate complete plugin project

**Reference:** See [examples/workflow_example.md](examples/workflow_example.md) for complete end-to-end example.

---

## Implementation Guidelines

### Recommended Tool Sequence

1. `run_in_terminal` - Execute scripts, git commands, diff operations
2. `read_file` - Read configuration files for version detection
3. `grep_search` - Search for version strings and patterns
4. `create_file` - Generate customization report
5. `file_search` - Find specific files across modules

### Common Operations

**Multi-file Version Search:**
```bash
grep -h -r "VERSION\|version" --include="*.xml" --include="*.properties" \
  Configuration/ Engine/ | grep -o "[0-9]\+\.[0-9]\+\(\.[0-9]\+\)\?" | sort -V | uniq
```

**Module Detection:**
```bash
MODULES_FOUND=()
for module in Common Datalib Engine IDE StoryWriter; do
    [ -d "$module/src" ] && MODULES_FOUND+=("$module")
done
[ ${#MODULES_FOUND[@]} -gt 1 ] && echo "SOURCE_CODE_COPY" || echo "BUILD_COPY"
```

**Diff with Exclusions:**
```bash
diff -Nur -x target -x '*.class' -x .git -x .idea "$OFFICIAL" "$USER" > changes.diff
```

### Validation Before Each Step

Use `scripts/error_handling_utils.sh` functions:
- `check_disk_space <path>` - Verify sufficient disk space
- `check_git_available` - Verify git is installed
- `check_internet` - Check connectivity for downloads
- `validate_path <path>` - Verify path exists and is accessible

---

## Error Handling

### Common Issues and Solutions

| Issue | Symptom | Solution |
|-------|---------|----------|
| Version tag not found | `git checkout` fails | List all tags, ask user which to use |
| Git not available | `command not found` | Ask user to install git or provide local official copy |
| Large diff output | Diff files >100MB | Summarize changes, provide file list only |
| Permission denied | Cannot access files | Check permissions, ask user to run with appropriate access |
| Module mismatch | Different structure | Document structural differences separately |

**Validation Checks:**
```bash
# Check disk space
df -h . | awk 'NR==2 {print $4}'

# Verify path exists
[ -d "$USER_PATH" ] || { echo "Path not found"; exit 1; }

# Verify git is available
command -v git >/dev/null 2>&1 || { echo "Git not installed"; exit 1; }

# Check internet (for clone)
ping -c 1 github.com >/dev/null 2>&1 || { echo "No internet"; exit 1; }
```

---

## Best Practices

1. **Always Confirm Paths** - Never assume; always ask user to verify
2. **Save Official Clone** - Keep for future comparisons
3. **Use Absolute Paths** - Avoid relative path issues
4. **Generate Timestamped Reports** - Include date in filenames
5. **Preserve Context** - Include surrounding code in diffs
6. **Group Related Changes** - Combine related modifications
7. **Provide Examples** - Include code snippets for key customizations
8. **Document Intent** - Infer why changes were made
9. **Risk Assessment** - Identify changes impacting upgrades
10. **Actionable Recommendations** - Suggest concrete next steps

---

## Reference Files

All scripts, templates, and examples are organized in subdirectories:

### Scripts (`scripts/`)
- `validate_ingenious_root.sh` - Validate INGenious installation markers
- `detect_version.sh` - Multi-strategy version detection
- `detect_copy_type.sh` - Determine source vs build copy
- `download_official_release.sh` - Clone and checkout official release
- `compare_modules.sh` - Compare modules between installations
- `generate_diff.sh` - Generate detailed diff patches
- `identify_new_methods.sh` - Find new action method candidates
- `error_handling_utils.sh` - Common validation functions
- `categorize_changes.py` - Categorize customizations by pattern
- `generate_plugin_spec.py` - Generate plugin specifications from customizations

### References (`references/`)
- `customization_report_template.md` - Report structure template
- `plugin_specification_template.json` - Machine-readable plugin spec format
- `plugin_recommendation_template.md` - Plugin extraction recommendation format
- `statistics_schema.json` - Statistics output schema
- `categorization_rules.md` - Category definitions and impact guidelines

### Examples (`examples/`)
- `workflow_example.md` - Complete end-to-end workflow demonstration
- `user_interaction_flow.md` - Detailed interaction patterns for each step
- `module_structures.md` - Source vs build copy structure comparison

---

## Quick Start Example

**User:** "Analyze my INGenious installation for customizations"

**Agent Workflow:**
1. **Step 1:** Present detected installations, wait for user confirmation ✋
2. **User confirms:** "Option 1" ✓
3. **Steps 2-3:** Auto-detect version (2.3) and copy type (BUILD_COPY)
4. **Step 4:** Download official v2.3 from GitHub
5. **Step 5:** Compare Engine module, categorize changes
6. **Step 6:** Generate report with statistics and recommendations
7. **Ask user:** "Would you like me to identify plugin extraction opportunities?"
8. **If yes → Step 7:** Analyze and present plugin candidates
9. **If user confirms:** Invoke `ingenious-plugin-creation` skill to create plugins

**Reference:** See [examples/workflow_example.md](examples/workflow_example.md) for detailed example with full dialogue.

---

## Integration with Plugin Creation Skill

After identifying plugin candidates in Step 7, if user requests plugin creation:

1. **Detection skill** outputs plugin specifications
2. **User confirms** which plugins to create (1, 2, all, etc.)
3. **Detection skill invokes** `ingenious-plugin-creation` skill with:
   - Plugin name and type
   - Action specifications
   - Source code snippets  
   - Required dependencies

The plugin creation skill handles complete plugin project generation, build configuration, and deployment instructions.

---

## Notes

- All scripts include usage instructions in their headers
- Templates can be copied and customized
- Examples demonstrate realistic scenarios
- Categorization rules are extensible for new patterns
- Plugin specifications follow standard JSON schema

For complete documentation, see README.md in this directory.
