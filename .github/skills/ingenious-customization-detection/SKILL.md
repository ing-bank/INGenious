---
name: ingenious-customization-detection
description: 'Detect and analyze customizations made to INGenious framework by users. USE FOR: detecting code modifications, identifying custom enhancements, comparing user version against official release, analyzing build vs source code copies, generating customization reports, identifying version differences, tracking user modifications, extracting customizations as plugins. INCLUDES: version detection, repository comparison, git diff analysis, customization reporting with functional grouping, plugin candidate identification. OUTPUTS: structured plugin specifications for use with ingenious-plugin-creation skill.'
argument-hint: 'Describe which INGenious installation to analyze or confirm the root folder'
---

# INGenious Customization Detection Skill

## When to Use This Skill

Use this skill when you need to:
- Detect customizations or modifications made to INGenious framework
- Compare a user's INGenious installation against the official release
- Identify version and type of INGenious installation (build vs source)
- Generate reports of enhancements grouped by functionality
- Analyze differences between user code and official release
- Track custom modifications across modules (Engine, Datalib, Common, etc.)
- Extract customizations as plugins for safer upgrades

## Skill Structure

This skill uses **progressive loading** with organized assets:

- **[scripts/](./scripts/)** - 7 executable scripts for detection, comparison, and analysis
- **[references/](./references/)** - 7 detailed reference documents (loaded on-demand)
- **[assets/templates/](./assets/templates/)** - 1 report template for customization reports

See [scripts/README.md](./scripts/README.md) for script usage and [references/README.md](./references/README.md) for reference documentation index.

## Key Principles

**🔴 MANDATORY: This is an INTERACTIVE workflow**

- **NEVER assume** which folder to analyze
- **ALWAYS confirm** with the user before proceeding
- **STOP at Step 1** until user explicitly confirms the target installation
- **Present clear options** when multiple installations are detected
- **Validate** user's choice before moving forward

Each step requires validation before proceeding to the next step. This ensures accuracy and prevents analyzing the wrong codebase.

## Decision Flow

This skill follows a systematic 7-step workflow:

```
1. Identify Repository → 2. Detect Version → 3. Determine Copy Type → 
4. Download Official Release → 5. Compare & Detect Changes → 6. Generate Report →
7. Identify Plugin Candidates (Optional)
```

**Skill Integration:** This skill can invoke the `ingenious-plugin-creation` skill to help convert customizations into plugins.

## Workflow Steps

### Step 1: Identify INGenious Repository

**CRITICAL: ALWAYS verify with user before proceeding to Step 2**

**Actions:**
1. **Detect** - List workspace folders and identify INGenious installations
2. **Present** - Show options to user with clear numbering
3. **STOP** - Wait for explicit user confirmation
4. **Validate** - Run [validate-installation.sh](./scripts/validate-installation.sh) to verify INGenious markers

**User Interaction:**
```
I've detected the following potential INGenious installations:

Option 1: /path/to/installation-1
  └─ Contains: Configuration/, Engine/, Projects/
  
Option 2: /path/to/installation-2
  └─ Contains: Common/, Datalib/, Engine/, IDE/

Which installation would you like to analyze?
(Or provide a different path)
```

**Required User Response:**
- Option number ("Option 1", "1", "The first one")
- Absolute path ("/path/to/my-ingenious")
- Confirmation ("Yes", "Correct", "That's right")

**Validation:**
```bash
./scripts/validate-installation.sh --check markers --path "$USER_PATH"
```

**If validation fails:**
- Show warning to user
- Ask for confirmation or alternative path
- Do NOT proceed until validation passes

**Key Requirements:**
- ✅ ALWAYS present options (never assume)
- ✅ ALWAYS wait for confirmation
- ✅ ALWAYS validate before Step 2
- ❌ NEVER auto-select or guess

See [references/troubleshooting.md](./references/troubleshooting.md#installation-detection-issues) for common issues.

### Step 2: Detect Version

**Action:** Run [detect-version.sh](./scripts/detect-version.sh) to identify INGenious version

**Script:**
```bash
./scripts/detect-version.sh "$USER_PATH"
```

**Detection Strategy:** Multi-source search across:
1. pom.xml files (`<version>` tags)
2. JAR manifest files (Implementation-Version)
3. Property files (VERSION keys)
4. Git tags (if .git/ exists)
5. Source code constants

**Common Patterns:** `2.3`, `2.3.0`, `v2.3`, `release-2.3`

**Output:** Version string (e.g., "2.3.0") or "UNKNOWN"

For detailed strategies, see [version-detection-reference.md](./references/version-detection-reference.md).

### Step 3: Determine Copy Type

**Action:** Run [detect-copy-type.sh](./scripts/detect-copy-type.sh) to determine installation type

**Script:**
```bash
./scripts/detect-copy-type.sh "$USER_PATH"
```

**Types:**
- **SOURCE_CODE_COPY** - Full Maven multi-module project (Datalib/, Common/, Engine/, IDE/, StoryWriter/)
- **BUILD_COPY** - Runtime distribution with Engine/ only

**Detection:** Counts modules with `src/` subdirectories. If ≥2 modules found → Source Code Copy.

**Why It Matters:**
- **Source Code Copy:** Compare all modules (comprehensive analysis)
- **Build Copy:** Compare Engine/ only (focused analysis)

For module structures and edge cases, see [copy-type-reference.md](./references/copy-type-reference.md).

### Step 4: Download Official INGenious Release

**Action:** Run [download-official.sh](./scripts/download-official.sh) to clone and checkout matching version

**Script:**
```bash
./scripts/download-official.sh "$VERSION" "/tmp/ingenious-official"
```

**Prerequisites:**
- Git installed
- Internet connection
- ~100-500MB disk space

**Process:**
1. Clone official repository from GitHub
2. Try version tags: `v{VERSION}`, `{VERSION}`, `release-{VERSION}`, etc.
3. Checkout exact tag or fall back to main branch
4. Verify checkout successful

**Exit Codes:**
- `0` - Success (exact version found)
- `1` - Git error or network failure
- `2` - Version tag not found (using main branch)

For troubleshooting clone failures, see [troubleshooting.md](./references/troubleshooting.md#download-issues).

### Step 5: Compare and Detect Changes

**Action:** Run [compare-installations.sh](./scripts/compare-installations.sh) to generate diffs

**Script:**
```bash
./scripts/compare-installations.sh \
    "$USER_PATH" \
    "$OFFICIAL_PATH/official-ingenious" \
    "$COPY_TYPE" \
    "/tmp/customization-diffs"
```

**Process:**
- **Source Code Copy:** Compares all modules (Common, Datalib, Engine, IDE, ingenious-api, StoryWriter)
- **Build Copy:** Compares Engine module only

**Exclusions:** Automatically excludes `target/`, `.git/`, `.idea/`, `*.class`, log files

**Output Files:**
- `diff_<module>.patch` - Detailed unified diff for each module
- `files_<module>.txt` - Quick summary of changed files
- Statistics: files changed, lines added/removed

For comparison strategies and handling large diffs, see [comparison-strategy-reference.md](./references/comparison-strategy-reference.md).

### Step 6: Generate Customization Report

**Action:** Run [generate-report.py](./scripts/generate-report.py) to create comprehensive report

**Script:**
```bash
./scripts/generate-report.py \
    "/tmp/customization-diffs" \
    "$USER_PATH" \
    "$VERSION" \
    "$COPY_TYPE" \
    "customization_report.md"
```

**Report Includes:**
- Summary (installation details, version, copy type)
- Statistics (modules, files, lines changed)
- Customizations grouped by category (Feature Enhancement, Bug Fix, Configuration, Integration, Performance, UI/Reporting, Framework Core)
- Risk assessment (High/Medium/Low impact)
- Plugin extraction opportunities (if applicable)
- Detailed diff file references

**Template:** Uses [customization-report-template.md](./assets/templates/customization-report-template.md)

**Categorization:** Automatic pattern-matching based on file paths, imports, and code patterns. See [categorization-patterns.md](./references/categorization-patterns.md) for logic details.

### Step 7: Identify Plugin Candidates (Optional)

**Action:** Analyze customizations and identify which ones should be extracted as plugins.

**When to Execute This Step:**
- After completing Step 6 (report generation)
- When user asks to "extract customizations to plugins"
- When user wants to upgrade INGenious and preserve customizations
- When customizations are significant enough to warrant plugin extraction

**Action:** Run [identify-plugins.py](./scripts/identify-plugins.py) to analyze plugin candidates

**Script:**
```bash
./scripts/identify-plugins.py \
    "/tmp/customization-diffs" \
    "$USER_PATH" \
    "plugin_recommendations.md"
```

**Output:**
- `plugin_recommendations.md` - Human-readable recommendations with priority, complexity, action lists

**Plugin Criteria:** See [plugin-extraction-reference.md](./references/plugin-extraction-reference.md) for:
- Good candidate criteria (new functionality, self-contained, reusable, appropriate location)
- Plugin type detection (Browser, Mobile, Webservice, Database, General)
- Action method patterns
- Validation checklist

**Cross-Skill Integration:**
When user confirms plugin creation, invoke the `ingenious-plugin-creation` skill with plugin specifications from this step.

---

## Usage Examples

For complete end-to-end workflow examples, see [references/examples.md](./references/examples.md):
- Basic customization detection
- Plugin extraction workflow
- Upgrade preservation scenario
- Team collaboration use case

**Quick Start:**
```
User: "Analyze my INGenious installation for customizations"
Agent: Guides through Steps 1-6 interactively, then offers Step 7 for plugin extraction
```

---

## Troubleshooting

For common issues and solutions, see [references/troubleshooting.md](./references/troubleshooting.md):
- Installation detection issues
- Version detection problems
- Download failures
- Comparison issues
- Permission problems
- Report generation issues

**Quick Diagnostic:**
```bash
# Check installation health
./scripts/validate-installation.sh --check markers --path "$PATH"
./scripts/detect-version.sh "$PATH"
./scripts/detect-copy-type.sh "$PATH"
```

---

**Version:** 2.0 (Refactored)  
**Last Updated:** May 2026  
**Structure:** Progressive loading with modular assets
