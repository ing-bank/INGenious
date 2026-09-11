# INGenious Customization Detection Skill - Documentation

## Overview

This skill detects and analyzes customizations made to INGenious framework installations. It compares user installations against official releases, generates comprehensive reports, and identifies opportunities to extract customizations as portable plugins.

**Version:** 2.0  
**Last Updated:** May 2026  
**Refactored:** Modular structure with scripts, references, and examples

---

## Directory Structure

```
ingenious-customization-detection/
├── SKILL.md                    # Main skill file (393 lines)
├── SKILL.md.backup             # Backup of original (1331 lines)
├── README.md                   # This file
├── scripts/                    # Executable scripts (8 bash, 2 python)
│   ├── validate_ingenious_root.sh
│   ├── detect_version.sh
│   ├── detect_copy_type.sh
│   ├── download_official_release.sh
│   ├── compare_modules.sh
│   ├── generate_diff.sh
│   ├── identify_new_methods.sh
│   ├── error_handling_utils.sh
│   ├── categorize_changes.py
│   └── generate_plugin_spec.py
├── references/                 # Templates and schemas (5 files)
│   ├── customization_report_template.md
│   ├── plugin_specification_template.json
│   ├── plugin_recommendation_template.md
│   ├── statistics_schema.json
│   └── categorization_rules.md
└── examples/                   # Usage examples and documentation (3 files)
    ├── workflow_example.md
    ├── user_interaction_flow.md
    └── module_structures.md
```

---

## Quick Start

### For Users

**To analyze an INGenious installation for customizations:**

1. Invoke the skill: `/ingenious-customization-detection`
2. When prompted, select your installation or provide path
3. Skill will automatically detect version, compare against official release, and generate report
4. Optionally identify plugin candidates for extraction

**Example:**
```
User: "Analyze my INGenious installation for customizations"
Agent: [Presents detected installations]
User: "Option 1"
Agent: [Runs analysis and generates customization_report.md]
```

See [examples/workflow_example.md](examples/workflow_example.md) for complete walkthrough.

---

### For Developers

**To run scripts manually:**

```bash
# Navigate to skill directory
cd .github/skills/ingenious-customization-detection

# Validate INGenious installation
./scripts/validate_ingenious_root.sh /path/to/ingenious

# Detect version
./scripts/detect_version.sh /path/to/ingenious

# Determine copy type (source vs build)
./scripts/detect_copy_type.sh /path/to/ingenious

# Download official release for comparison
./scripts/download_official_release.sh 2.3 /tmp/official

# Compare modules
./scripts/compare_modules.sh /user/path /official/path BUILD_COPY

# Generate detailed diff
./scripts/generate_diff.sh /user/path /official/path Engine

# Identify new methods
./scripts/identify_new_methods.sh /user/path /official/path Engine

# Categorize changes (Python)
python3 scripts/categorize_changes.py diff_Engine.patch

# Generate plugin spec (Python)
python3 scripts/generate_plugin_spec.py customization_data.json
```

---

## File Reference

### SKILL.md (Main Skill File)

**Lines:** 393 (reduced from 1,331)  
**Content:** Core workflow, decision flow, step-by-step instructions  
**References:** All scripts, templates, and examples via relative paths

**Sections:**
1. When to Use This Skill
2. Key Principles (mandatory interactive workflow)
3. Decision Flow (7 steps)
4. Workflow Steps (condensed with references)
5. Implementation Guidelines
6. Error Handling
7. Best Practices
8. Reference Files Index
9. Quick Start Example
10. Integration with Plugin Creation Skill

---

### Scripts Directory

#### Bash Scripts (8 files)

| Script | Purpose | Usage |
|--------|---------|-------|
| `validate_ingenious_root.sh` | Validate installation markers | `./validate_ingenious_root.sh <path>` |
| `detect_version.sh` | Multi-strategy version detection | `./detect_version.sh <path>` |
| `detect_copy_type.sh` | Source vs build copy detection | `./detect_copy_type.sh <path>` |
| `download_official_release.sh` | Clone and checkout official | `./download_official_release.sh <version> [dir]` |
| `compare_modules.sh` | Compare user vs official | `./compare_modules.sh <user> <official> <type>` |
| `generate_diff.sh` | Generate detailed diffs | `./generate_diff.sh <user> <official> <module>` |
| `identify_new_methods.sh` | Find new action methods | `./identify_new_methods.sh <user> <official> <module>` |
| `error_handling_utils.sh` | Common validation functions | `source error_handling_utils.sh` |

All bash scripts include:
- Header with purpose, usage, and description
- Parameter validation
- Error handling
- Exit codes (0 = success, 1 = failure)

**Make executable:**
```bash
chmod +x scripts/*.sh
```

#### Python Scripts (2 files)

| Script | Purpose | Dependencies | Usage |
|--------|---------|--------------|-------|
| `categorize_changes.py` | Categorize customizations | Python 3.6+ | `python3 categorize_changes.py <diff_file>` |
| `generate_plugin_spec.py` | Generate plugin specs | Python 3.6+, json | `python3 generate_plugin_spec.py <data.json>` |

Both Python scripts:
- Include docstrings and usage help
- Accept command-line arguments
- Output results to stdout and files
- Use standard library only (no external dependencies)

---

### References Directory

#### Templates (3 markdown, 2 JSON)

| File | Type | Purpose |
|------|------|---------|
| `customization_report_template.md` | Markdown | Main report structure with all sections |
| `plugin_recommendation_template.md` | Markdown | Plugin extraction recommendations format |
| `categorization_rules.md` | Markdown | Category definitions, patterns, impact levels |
| `plugin_specification_template.json` | JSON | Machine-readable plugin spec schema |
| `statistics_schema.json` | JSON | Statistics output format |

**Usage:**
- Copy templates when generating reports
- Reference categorization rules for pattern matching
- Use JSON schemas for structured data output

---

### Examples Directory

#### Documentation (3 markdown files)

| File | Content |
|------|---------|
| `workflow_example.md` | Complete end-to-end scenario with full dialogue |
| `user_interaction_flow.md` | Detailed interaction patterns for each workflow step |
| `module_structures.md` | Source vs build copy structure comparison |

**Purpose:**
- **workflow_example.md**: Shows complete workflow from detection through plugin creation
- **user_interaction_flow.md**: Reference for expected user responses and agent prompts
- **module_structures.md**: Explains differences between source and build installations

---

## Workflow Steps Reference

### Step 1: Identify Repository (INTERACTIVE)
- **Script:** `validate_ingenious_root.sh`
- **Example:** `user_interaction_flow.md` § Step 1
- **Principle:** ALWAYS confirm with user before proceeding

### Step 2: Detect Version
- **Script:** `detect_version.sh`
- **Strategies:** pom.xml, properties, JAR manifest, Git tags

### Step 3: Determine Copy Type
- **Script:** `detect_copy_type.sh`
- **Reference:** `module_structures.md`
- **Types:** SOURCE_CODE_COPY or BUILD_COPY

### Step 4: Download Official Release
- **Script:** `download_official_release.sh`
- **Validation:** `error_handling_utils.sh`
- **Source:** https://github.com/ing-bank/INGenious.git

### Step 5: Compare and Detect Changes
- **Scripts:** `compare_modules.sh`, `generate_diff.sh`, `categorize_changes.py`
- **Reference:** `categorization_rules.md`
- **Output:** Diff files, categorized changes

### Step 6: Generate Report
- **Template:** `customization_report_template.md`
- **Schema:** `statistics_schema.json`
- **Output:** `customization_report.md`, `stats.json`, diff patches

### Step 7: Identify Plugin Candidates (Optional)
- **Scripts:** `identify_new_methods.sh`, `generate_plugin_spec.py`
- **Templates:** `plugin_recommendation_template.md`, `plugin_specification_template.json`
- **Integration:** Invokes `ingenious-plugin-creation` skill

---

## Customization Categories

Defined in [references/categorization_rules.md](references/categorization_rules.md):

1. **Feature Enhancement** - New functionality, custom actions
2. **Bug Fix** - Error handling, null checks, exception fixes
3. **Configuration** - POM changes, dependencies, build config
4. **Integration** - External tools, API modifications
5. **Performance** - Optimizations, caching, resource management
6. **UI/Reporting** - Report templates, dashboard, logging
7. **Framework Core** - Engine changes, architecture modifications

Each category includes:
- Description
- Detection patterns (regex)
- Examples
- Impact assessment guidelines

---

## Plugin Type Detection

Defined in [scripts/generate_plugin_spec.py](scripts/generate_plugin_spec.py):

| Import Pattern | Plugin Type | API Contract |
|---------------|-------------|--------------|
| `com.microsoft.playwright.*` | Browser | BrowserPluginApi |
| `io.appium.*` | Mobile | MobilePluginApi |
| `java.net.http.*` | Webservice | WebservicePluginApi |
| `java.sql.*` | Database | DatabasePluginApi |
| None specific | General | CommandPluginApi |

---

## Integration with Other Skills

### ingenious-plugin-creation Skill

After Step 7 identifies plugin candidates:

1. **Detection skill** generates plugin specifications
2. **User confirms** which plugins to create
3. **Detection skill invokes** plugin creation skill with:
   - Plugin name and type
   - Action method specifications
   - Source code snippets
   - Required dependencies
4. **Plugin creation skill** generates complete plugin project

**Reference:** [examples/workflow_example.md](examples/workflow_example.md) § Plugin Creation

---

## Error Handling

### Common Issues

Handled in [scripts/error_handling_utils.sh](scripts/error_handling_utils.sh):

- **Insufficient disk space** - Check with `check_disk_space`
- **Git not available** - Check with `check_git_available`
- **No internet** - Check with `check_internet`
- **Invalid path** - Check with `validate_path`

### Error Recovery Patterns

| Error | Detection | Recovery |
|-------|-----------|----------|
| Version tag not found | `git checkout` fails | List all tags, ask user |
| Git not installed | `command not found` | Request local official copy |
| Large diff output | File size >100MB | Summarize, provide file list |
| Permission denied | Access error | Check permissions, request sudo |
| Module mismatch | Structure differs | Document differences |

---

## Best Practices

1. **Interactive Confirmation** - Always confirm installation path with user
2. **Validation** - Use validation scripts before proceeding
3. **Error Handling** - Check prerequisites (git, disk space, internet)
4. **Backup** - Keep original files (SKILL.md.backup)
5. **Documentation** - Reference external files for details
6. **Modularity** - Use scripts for reusable operations
7. **Templates** - Copy templates, don't modify originals
8. **Examples** - Refer to examples for guidance

---

## Maintenance

### Adding New Categories

1. Edit [references/categorization_rules.md](references/categorization_rules.md)
2. Update pattern list in [scripts/categorize_changes.py](scripts/categorize_changes.py)
3. Add examples to rules documentation

### Adding New Scripts

1. Create script in `scripts/` directory
2. Add header with purpose, usage, description
3. Include error handling and validation
4. Make executable: `chmod +x scripts/script_name.sh`
5. Update SKILL.md § Reference Files
6. Update this README § Scripts Directory

### Updating Templates

1. Modify template file in `references/` directory
2. Preserve template structure (use placeholders: [value])
3. Update SKILL.md if workflow changes
4. Test with actual data

---

## Version History

### v2.0 (May 2026) - Modular Refactoring
- **Reduced SKILL.md from 1,331 to 393 lines** (70% reduction)
- Created modular structure with scripts, references, examples
- Extracted 8 bash scripts for automation
- Extracted 2 Python scripts for analysis
- Created 5 reference templates
- Created 3 example documentation files
- All functionality preserved, improved maintainability

### v1.0 (Original)
- Monolithic SKILL.md (1,331 lines)
- All content inline
- No separate scripts or templates

---

## Contributing

To improve this skill:

1. Test scripts with various INGenious installations
2. Add new detection patterns to categorization rules
3. Improve error handling and validation
4. Add more examples and use cases
5. Enhance plugin type detection accuracy
6. Update templates based on user feedback

---

## Support

For issues or questions:
1. Check [examples/workflow_example.md](examples/workflow_example.md) for usage
2. Review [examples/user_interaction_flow.md](examples/user_interaction_flow.md) for expected interactions
3. Consult script headers for specific tool usage
4. Refer to [references/categorization_rules.md](references/categorization_rules.md) for category definitions

---

## License

Part of INGenious framework skill collection. See main project LICENSE.

---

## Related Skills

- **ingenious-plugin-creation** - Creates INGenious plugins from specifications
- **java-lsp-tools** - Compiler-accurate Java code navigation

---

**README Generated:** May 2026  
**Skill Version:** 2.0  
**Maintained by:** INGenious Skills Team
