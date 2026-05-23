# References Directory

This directory contains detailed reference documentation for the ingenious-customization-detection skill.

## Contents

### [version-detection-reference.md](./version-detection-reference.md)
Detailed strategies for detecting INGenious version from multiple sources.

**Covers:**
- 5 detection strategies (pom.xml, JAR manifests, properties, Git tags, source constants)
- Common version patterns
- Priority order
- Troubleshooting version detection issues

**Use when:** Need to understand how version detection works or troubleshoot version issues

---

### [copy-type-reference.md](./copy-type-reference.md)
Complete guide to distinguishing between source code and build installations.

**Covers:**
- Source code copy vs build copy characteristics
- Module structure comparison
- Detection logic and criteria
- Edge cases (partial copies, modified builds)
- Module descriptions

**Use when:** Need to understand installation types or troubleshoot type detection

---

### [comparison-strategy-reference.md](./comparison-strategy-reference.md)
Comprehensive guide to comparing installations and detecting customizations.

**Covers:**
- diff and git diff techniques
- Exclusion patterns (build outputs, IDE files, etc.)
- Output formats (unified diff, quick summary)
- Module-by-module comparison strategies
- Handling large diffs
- Diff interpretation

**Use when:** Need to understand comparison process or optimize diff generation

---

### [categorization-patterns.md](./categorization-patterns.md)
Pattern-matching logic for automatically categorizing customizations.

**Covers:**
- 7 customization categories (Feature, Bug Fix, Configuration, Integration, Performance, UI/Reporting, Framework Core)
- Pattern matching rules
- Impact assessment (High/Medium/Low)
- Categorization algorithm
- Examples by category

**Use when:** Understanding how changes are categorized or customizing categorization logic

---

### [plugin-extraction-reference.md](./plugin-extraction-reference.md)
Criteria and process for identifying plugin candidates from customizations.

**Covers:**
- Good plugin candidate criteria (4 criteria)
- Plugin type detection (Browser, Mobile, Webservice, Database, General)
- Action method detection patterns
- Plugin specifications structure
- Validation checklist
- Examples of good and bad candidates

**Use when:** Evaluating if customizations should be extracted as plugins

---

### [examples.md](./examples.md)
Complete end-to-end workflow examples and usage patterns.

**Covers:**
- Basic customization detection example
- Plugin creation workflow
- Upgrade preservation scenario
- Team collaboration use case
- Common usage patterns

**Use when:** Learning how to use the skill or seeing complete workflows

---

### [troubleshooting.md](./troubleshooting.md)
Common issues and solutions for all aspects of customization detection.

**Covers:**
- Installation detection issues
- Version detection problems
- Download failures
- Comparison issues
- Permission problems
- Report generation issues
- Diagnostic commands

**Use when:** Encountering errors or unexpected behavior

---

## How to Use These References

### During Workflow Execution

References are loaded **on-demand** when:
- Agent follows a link from SKILL.md
- User asks for detailed explanation
- Troubleshooting specific issue

### For Learning

Read in this order:
1. [examples.md](./examples.md) - See complete workflows
2. [version-detection-reference.md](./version-detection-reference.md) - Understand version detection
3. [copy-type-reference.md](./copy-type-reference.md) - Understand installation types
4. [comparison-strategy-reference.md](./comparison-strategy-reference.md) - Deep dive on comparison
5. [categorization-patterns.md](./categorization-patterns.md) - How changes are categorized
6. [plugin-extraction-reference.md](./plugin-extraction-reference.md) - Plugin candidate identification

### For Troubleshooting

1. Start with [troubleshooting.md](./troubleshooting.md)
2. Find your specific issue
3. Follow diagnostic steps
4. Refer to detailed reference if needed

---

## Progressive Loading

These references support the skill's progressive loading approach:

**Phase 1: Discovery**
- Skill description only (~100 tokens)

**Phase 2: Workflow**
- Main SKILL.md loaded (~1,750 tokens)
- References not loaded yet

**Phase 3: Details (on-demand)**
- Specific reference loaded when needed
- User can deep-dive into topics

**Phase 4: Troubleshooting (reactive)**
- Troubleshooting guide loaded if issues occur

This keeps initial context small while providing deep information when needed.

---

## Reference File Sizes

| File | Approx Lines | Purpose |
|------|-------------|---------|
| version-detection-reference.md | ~150 | Version strategies |
| copy-type-reference.md | ~250 | Installation types |
| comparison-strategy-reference.md | ~300 | Diff strategies |
| categorization-patterns.md | ~250 | Change categorization |
| plugin-extraction-reference.md | ~300 | Plugin criteria |
| examples.md | ~450 | Usage examples |
| troubleshooting.md | ~400 | Issue resolution |

**Total:** ~2,100 lines of reference documentation  
(Not loaded into context unless specifically referenced)

---

## Updating References

When updating references:

1. **Maintain backward compatibility** - Don't break existing links
2. **Update examples** - Keep examples current with latest patterns
3. **Test links** - Verify all relative links work
4. **Update this README** - Keep index accurate
5. **Version control** - Document changes in commit messages

---

## Related Resources

**Main Skill:**
- [../SKILL.md](../SKILL.md) - Main workflow

**Scripts:**
- [../scripts/](../scripts/) - Executable scripts used by workflow

**Templates:**
- [../assets/templates/](../assets/templates/) - Report and spec templates
