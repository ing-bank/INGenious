# Plugin Extraction Reference

## Overview

This document details the criteria and process for identifying which customizations should be extracted as INGenious plugins.

## Good Plugin Candidates

### Criteria 1: Adds New Functionality

✅ **Good candidate** if customization:
- Adds new action methods
- Implements new commands
- Provides new integrations
- Creates reusable capabilities

❌ **Not suitable** if customization:
- Modifies existing core behavior
- Changes internal framework logic
- Patches bugs in framework code

**Example (Good):**
```java
// New file: CustomBrowserActions.java
public class CustomBrowserActions {
    @Action
    public void clickWithHighlight(String objectName) {
        // New action - perfect for plugin
    }
}
```

**Example (Not Suitable):**
```java
// Modified: ExecutionEngine.java (core framework file)
public void executeStep() {
-   standard();
+   customModified();  // Modifies core - keep as customization
}
```

---

### Criteria 2: Is Self-Contained

✅ **Good candidate** if customization:
- Has minimal dependencies on framework internals
- Uses only public API methods
- Can be isolated into separate class(es)
- Doesn't require modifying core classes

❌ **Not suitable** if customization:
- Tightly coupled with framework internals
- Requires access to private methods/fields
- Depends on modified framework behavior

**Check:**
```bash
# Find internal dependencies
grep -r "import com.ing.engine.core" CustomFile.java
# If found: ⚠️ May have internal dependencies

# Check for public API usage
grep -r "import com.ing.api" CustomFile.java
# If found: ✅ Uses public APIs
```

---

### Criteria 3: Provides Reusable Actions

✅ **Good candidate** if customization:
- Implements generic functionality
- Could be useful across multiple test scenarios
- Not project-specific hacks
- Provides clear action interface

❌ **Not suitable** if customization:
- Hard-coded for specific test case
- Contains company-specific logic
- One-off workarounds

---

### Criteria 4: Located in Appropriate Modules

✅ **High priority candidates** in:
- `Engine/src/main/java/com/ing/engine/commands/`
- New files in `Engine/src/main/java/com/custom/`
- Standalone action classes

❌ **Keep as customizations** if in:
- `Engine/src/main/java/com/ing/engine/core/`
- `Engine/src/main/java/com/ing/engine/execution/`
- Framework internal packages

---

## Plugin Type Detection

### Based on Imports

| Import Pattern | Plugin Type | API Contract |
|---------------|-------------|--------------|
| `com.microsoft.playwright.*` | Browser | BrowserPluginApi |
| `io.appium.*` | Mobile | MobilePluginApi |
| `java.net.http.HttpClient` | Webservice | WebservicePluginApi |
| `java.sql.*`, `javax.sql.*` | Database | DatabasePluginApi |
| No specific imports | General | CommandPluginApi |

### Based on File Location

| Path Pattern | Likely Type |
|--------------|-------------|
| `*/commands/Web*.java` | Browser |
| `*/commands/Mobile*.java` | Mobile |
| `*/commands/API*.java` | Webservice |
| `*/commands/DB*.java` | Database |
| `*/commands/*` | General |

### Based on Method Names

| Method Pattern | Likely Type |
|----------------|-------------|
| `click*`, `navigate*`, `waitFor*` | Browser |
| `tap*`, `swipe*`, `scroll*` | Mobile |
| `sendRequest*`, `validateResponse*` | Webservice |
| `executeQuery*`, `validateData*` | Database |

---

## Action Method Detection

### Valid Action Signatures

```java
// Browser action - operates on Playwright object
public void actionName(String objectName) { }
public void actionName(String objectName, String input) { }
public boolean actionName(String objectName, String condition) { }

// General action - no object interaction
public void actionName() { }
public void actionName(String parameter) { }
```

### Patterns to Match

**New method detection:**
```bash
# Find public methods in new/modified files
grep -E "public (void|boolean|String) \w+\(" NewFile.java
```

**Exclude standard methods:**
```bash
# Skip getters, setters, constructors
grep -v "^(get|set|is|equals|hashCode|toString)"
```

**Extract method signature:**
```regex
public\s+(void|boolean|String)\s+(\w+)\s*\(([^)]*)\)
```

---

## Plugin Specifications

### Specification Structure

```json
{
  "plugin_name": "custom-browser-actions",
  "type": "browser",
  "api_contract": "BrowserPluginApi",
  "priority": "high",
  "complexity": "moderate",
  "actions": [
    {
      "method_name": "clickWithHighlight",
      "description": "Click element after highlighting it",
      "object_type": "PLAYWRIGHT",
      "input_required": false,
      "condition_optional": false,
      "source_file": "Engine/src/.../WebCommands.java",
      "source_lines": "145-167"
    }
  ],
  "dependencies": [
    {
      "groupId": "com.microsoft.playwright",
      "artifactId": "playwright",
      "version": "1.40.0"
    }
  ]
}
```

### Priority Assignment

**High Priority:**
- 4+ action methods
- Core functionality (browser/mobile interactions)
- Well-structured, reusable code
- Minimal dependencies

**Medium Priority:**
- 2-3 action methods
- Utility functions
- Some external dependencies
- Moderate complexity

**Low Priority:**
- 1 action method
- Edge case handling
- Complex dependencies
- Experimental code

### Complexity Assessment

**Simple:**
- 1-2 action methods
- Straightforward logic
- No external dependencies
- <100 lines total

**Moderate:**
- 3-5 action methods
- Some conditional logic
- Standard dependencies (Playwright, Appium)
- 100-300 lines

**Complex:**
- 6+ action methods
- Complex business logic
- Multiple dependencies
- >300 lines
- Requires configuration

---

## Validation Before Extraction

### Checklist

✅ **Is this truly new functionality?**
- Not modifying existing framework methods
- Adds new capabilities

✅ **Can it be isolated?**
- Doesn't depend on modified core classes
- Uses public APIs only

✅ **Is it reusable?**
- Generic enough for different projects
- Not hard-coded for specific tests

✅ **Are dependencies available?**
- External libraries are public/accessible
- Compatible with INGenious version

✅ **Does it fit a plugin type?**
- Matches Browser/Mobile/Webservice/Database/General pattern
- Has appropriate API contract

### Red Flags

❌ **Do NOT extract if:**

1. **Modifies existing methods** in framework classes
2. **Requires framework patches** to work
3. **Accesses private fields** or methods
4. **Tightly coupled** with project-specific code
5. **Hack or workaround** for framework bugs
6. **Incomplete implementation** or experimental

---

## Plugin Extraction Workflow

### Step 1: Identify Candidate

From customization analysis:
- New files with action methods
- Significant additions to existing files
- Reusable functionality

### Step 2: Validate Against Criteria

Apply checklist above:
- ✓ New functionality
- ✓ Self-contained
- ✓ Reusable
- ✓ Appropriate location

### Step 3: Detect Plugin Type

Check imports, file paths, method names:
- Browser, Mobile, Webservice, Database, or General

### Step 4: Extract Specifications

Generate plugin spec:
- Plugin name
- Action methods
- Dependencies
- Source file references

### Step 5: Create Plugin

Use `ingenious-plugin-creation` skill:
- Provide specifications
- Generate plugin structure
- Implement actions
- Test plugin

---

## Examples

### Example 1: Browser Plugin

**Source customization:**
```java
// Added to Engine/.../WebCommands.java
public void clickWithHighlight(String objectName) {
    Locator element = getLocator(objectName);
    element.evaluate("el => el.style.border = '3px solid red'");
    element.click();
}
```

**Analysis:**
- ✅ New method (not modifying existing)
- ✅ Uses public Playwright API
- ✅ Reusable across projects
- ✅ Browser-related

**Plugin Spec:**
```json
{
  "name": "custom-browser-actions",
  "type": "browser",
  "actions": [
    {
      "method_name": "clickWithHighlight",
      "description": "Highlights element before clicking"
    }
  ]
}
```

---

### Example 2: NOT a Plugin Candidate

**Source customization:**
```java
// Modified Engine/.../ExecutionEngine.java
public void executeTestCase(TestCase tc) {
-   standardExecution(tc);
+   if (isCustomProject()) {
+       customExecution(tc);
+   } else {
+       standardExecution(tc);
+   }
}
```

**Analysis:**
- ❌ Modifies core framework method
- ❌ Project-specific logic
- ❌ Changes execution flow
- ❌ Cannot be isolated

**Recommendation:** Keep as customization, document for upgrade planning

---

## Integration with Customization Detection

The `identify-plugins.py` script automatically:

1. **Scans diff files** for new methods
2. **Filters by criteria** (new files, significant additions)
3. **Detects plugin types** (imports, paths, methods)
4. **Generates specifications** (JSON + Markdown)
5. **Outputs recommendations** for user review

Users can then:
- Review plugin candidates
- Select which to create
- Invoke `ingenious-plugin-creation` skill with specs
