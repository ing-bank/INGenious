# Plugin Recommendation Template

## 🔌 Plugin Extraction Recommendations

Based on the customization analysis, **[N] plugins** can be extracted:

---

### Plugin [N]: [Plugin Name]

**[Priority Icon] [Priority Level]** | **Plugin Type:** [Browser/Mobile/Webservice/Database/General] | **Complexity:** [Simple/Moderate/Complex]

**Purpose:** [Brief description of what the plugin does]

**Actions Identified:**
1. `actionName()` - [Description]
   - Source: [file_path]:[line_range]
   - Object Type: [PLAYWRIGHT/APPIUM/HTTP/JDBC/GENERIC]
   - Input Required: [Yes/No] [optional: (description)]
   
2. `anotherAction()` - [Description]
   - Source: [file_path]:[line_range]
   - Object Type: [type]
   - Input Required: [Yes/No]

**Dependencies Required:**
- [dependency-name] version [x.x.x]
- Or: None (uses only framework APIs)

**API Contract:** `[ApiContractName]`

**Estimated Effort:** [time estimate]

**Code Ready:** [Yes/No - explanation]

**Next Step:**
```
Use the ingenious-plugin-creation skill with this specification:
"Create a [type] plugin named '[plugin-name]' with the following actions:
1. [action1] - [description]
2. [action2] - [description]"
```

---

## 📋 Plugin Specifications (Machine-Readable)

Saved to: `plugin_specifications.json`

Use this file with automated plugin generation workflows.

---

## ⚙️ How to Proceed

**Option 1: Create plugins one at a time**
Ask: "Create Plugin 1" or "Create the [Plugin Name] plugin"

**Option 2: Create all recommended plugins**
Ask: "Create all recommended plugins from the customization analysis"

**Option 3: Review a specific plugin in detail**
Ask: "Show me the full specification for Plugin [N]"

**Option 4: Ask the plugin creation skill directly**
Type: `/ingenious-plugin-creation` and provide the specification above
