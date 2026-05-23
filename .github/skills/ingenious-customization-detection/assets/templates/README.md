# Templates Directory

This directory contains templates used by the customization detection and reporting scripts.

## Templates

### [customization-report-template.md](./customization-report-template.md)

Markdown template for generating customization reports.

**Purpose:** Human-readable comprehensive report of all customizations

**Used by:** `generate-report.py` script (Step 6)

**Placeholder Format:** `{{VARIABLE_NAME}}` (Mustache-style)

**Key Sections:**
- Summary (installation details, statistics)
- Customizations by Category (grouped changes)
- Risk Assessment (impact levels)
- Recommendations (upgrade path, plugin opportunities)
- Plugin Extraction Opportunities
- Detailed Diff Files reference

**Variables:**

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `{{ANALYSIS_DATE}}` | Date/time of analysis | `2026-05-23 14:30:00` |
| `{{USER_PATH}}` | User's installation path | `/path/to/user/ingenious` |
| `{{VERSION}}` | Detected version | `2.3.0` |
| `{{COPY_TYPE}}` | Installation type | `BUILD_COPY` or `SOURCE_CODE_COPY` |
| `{{MODULES_COUNT}}` | Number of modules analyzed | `5` |
| `{{FILES_MODIFIED}}` | Total files changed | `45` |
| `{{LINES_ADDED}}` | Total lines added | `2340` |
| `{{CATEGORIES}}` | Array of category objects | (nested structure) |
| `{{PLUGINS}}` | Array of plugin objects | (nested structure) |

**Nested Structures:**

Categories:
```
{{#CATEGORIES}}
  {{CATEGORY_NAME}}
  {{FILE_COUNT}}
  {{#FILES}}
    {{FILE_PATH}}
    {{IMPACT_LEVEL}}
  {{/FILES}}
{{/CATEGORIES}}
```

Plugins:
```
{{#PLUGINS}}
  {{PLUGIN_NAME}}
  {{PLUGIN_TYPE}}
  {{#ACTIONS}}
    {{ACTION_NAME}}
  {{/ACTIONS}}
{{/PLUGINS}}
```

---

## Using Templates

### In Python Scripts

```python
from string import Template

# Load template
with open('template.md', 'r') as f:
    template = Template(f.read())

# Substitute values
output = template.substitute(
    USER_PATH='/path/to/user',
    VERSION='2.3.0',
    FILES_MODIFIED='45'
)

# Write output
with open('report.md', 'w') as f:
    f.write(output)
```

### In Bash Scripts

```bash
# Simple substitution
sed "s/{{USER_PATH}}/$USER_PATH/g" template.md > output.md

# Multiple substitutions
sed -e "s/{{USER_PATH}}/$USER_PATH/g" \
    -e "s/{{VERSION}}/$VERSION/g" \
    template.md > output.md
```

### With Mustache Libraries

For complex nested structures, use a Mustache library:

```python
import pystache

template = open('template.md').read()
data = {
    'USER_PATH': '/path',
    'CATEGORIES': [
        {
            'CATEGORY_NAME': 'Feature Enhancement',
            'FILES': [...]
        }
    ]
}
output = pystache.render(template, data)
```

---

## Template Best Practices

### 1. Use Clear Placeholder Names

✅ Good: `{{USER_INSTALLATION_PATH}}`  
❌ Bad: `{{PATH}}` (ambiguous)

### 2. Document All Placeholders

Every template should have a table listing all variables.

### 3. Provide Example Values

Show what actual substituted values look like.

### 4. Handle Missing Values

**In code:**
```python
value = data.get('OPTIONAL_FIELD', 'Not available')
```

**In template:**
```
{{#HAS_PLUGINS}}
  Plugin section
{{/HAS_PLUGINS}}
{{^HAS_PLUGINS}}
  No plugins section
{{/HAS_PLUGINS}}
```

### 5. Format Consistently

- Use uppercase for placeholders: `{{USER_PATH}}`
- Use underscores for multi-word: `{{LINES_ADDED}}`
- Match naming to code variables when possible

---

## Extending Templates

To add new sections:

1. **Define new placeholders** in comments
2. **Document in this README**
3. **Update generator scripts** to provide values
4. **Test with sample data**
5. **Update examples**

Example:
```markdown
<!-- New section in template -->

## {{NEW_SECTION_TITLE}}

{{#NEW_ITEMS}}
- {{ITEM_NAME}}: {{ITEM_VALUE}}
{{/NEW_ITEMS}}
```

Then update `generate-report.py`:
```python
template_data['NEW_SECTION_TITLE'] = 'Custom Section'
template_data['NEW_ITEMS'] = [
    {'ITEM_NAME': 'Item 1', 'ITEM_VALUE': 'Value 1'},
    ...
]
```

---

## Template Versions

Templates should be versioned in footer:

```markdown
**Template version:** 1.0
```

When making breaking changes:
1. Increment version
2. Document changes
3. Update generator scripts
4. Maintain backward compatibility when possible

---

## Related Files

**Scripts using templates:**
- [../../scripts/generate-report.py](../../scripts/generate-report.py) - Uses customization-report-template.md
- [../../scripts/identify-plugins.py](../../scripts/identify-plugins.py) - Uses plugin-spec-template.json

**Reference documentation:**
- [../../references/](../../references/) - Detailed reference docs
