---
name: ingenious-plugin-creation
description: 'Expert guidance for creating INGenious Playwright Framework plugins. USE FOR: creating new plugins, fixing plugin errors, configuring maven POMs, implementing action methods, working with Playwright objects, troubleshooting classloader issues, version compatibility problems, extending the framework with custom actions, converting customizations into plugins. INCLUDES: complete templates, architecture patterns, dependency management, best practices. INTEGRATES WITH: ingenious-customization-detection skill for extracting customizations as plugins.'
argument-hint: 'Describe the plugin to create or issue to fix'
allowed-tools: shell
---

# INGenious Plugin Creation Skill

## When to Use This Skill

Use this skill when you need to:
- Create a new plugin for the INGenious Playwright
- Fix plugin compilation or runtime errors
- Configure Maven POM for plugins
- Implement action methods with proper annotations
- Work with Playwright objects through the API
- Troubleshoot ClassCastException, NoSuchMethodError, or classloader issues
- Ensure version compatibility between plugin and framework
- Debug duplicate action names or manifest issues
- **Convert customizations into plugins** (see Skill Integration below)

## Key Principles

**🔴 MANDATORY: This is an INTERACTIVE workflow**

- **NEVER assume** where to create the plugin
- **ALWAYS confirm** the target directory with the user before creating files
- **STOP before creating** and present location options
- **Validate** the user's choice before proceeding
- **Allow custom paths** - don't force predefined locations

This ensures plugins are created in the correct location and prevents accidental file creation in wrong directories.

## Skill Integration: From Customizations to Plugins

**🔗 Works with:** `ingenious-customization-detection` skill

If you have customized INGenious framework code, you can:

1. **First:** Use `/ingenious-customization-detection` to analyze your customizations
2. **Then:** Use this skill to convert detected customizations into plugins

**Benefits of Converting Customizations to Plugins:**
- ✅ Easier to upgrade INGenious (no merge conflicts)
- ✅ Can be shared across multiple INGenious installations
- ✅ Isolated from core framework changes
- ✅ Easier to maintain and test independently
- ✅ Can be version-controlled separately

**Example Workflow:**
```
User: "Analyze my INGenious customizations"
→ Uses: ingenious-customization-detection skill
→ Output: Plugin specifications identified

User: "Create Plugin 1 from the analysis"
→ Uses: ingenious-plugin-creation skill (this skill)
→ Output: Fully functional plugin created
```

**When Receiving Plugin Specifications:**

This skill can accept structured input from the customization detection skill:
- Plugin name and type
- Action method specifications
- Source code snippets to convert
- Dependencies to include
- API contract to implement

## How to Ensure This Skill is Used

**For Creating New Plugins:**
This skill is automatically available when working in the plugin repository. To ensure it's used when creating new modules:

1. **Ask to create the new plugin** in the same conversation
2. **Reference the skill explicitly**: Say "Using the plugin skill, create..." or "@PLUGIN-CREATION-SKILL"

**For Editing Existing Plugins:**
The skill auto-activates when you open files in:
- Any directory containing `plugin` in the path
- Any Java file with `Plugin` in the name

**File Pattern Triggers:**
```
✅ Matches (skill will activate):
- browser-test-plugin/pom.xml
- mobile-test-plugin/src/main/java/.../*.java

```

## Plugin Architecture Overview

The INGenious Framework uses a sophisticated plugin architecture:

- **API Module** (`ingenious-api`): Stable interfaces between framework and plugins
- **Custom Classloaders**: Each plugin runs in isolated classloader environment
- **Type Erasure Pattern**: Playwright objects passed as `Object` type for version independence
- **Parent-First Delegation**: Critical packages (Playwright, API) loaded from parent classloader

This ensures plugins can be developed independently while maintaining compatibility.

## Step 0: Confirm Directories

**🔴 CRITICAL: ALWAYS confirm TWO directories before creating any files**

### Why Two Directories?

| Directory | Purpose | Contains | Example |
|-----------|---------|----------|---------|
| **Source Code** | Development | pom.xml, src/, target/ | `/path/to/Github-Plugins/my-plugin/` |
| **Deployment** | Runtime | my-plugin.jar, lib/ | `/path/to/INGenious/plugins/my-plugin/` |

### Question 1: Source Code Location

**Ask:** "Where should I save the plugin SOURCE CODE?"

**Steps:**
1. List workspace folders and identify plugin development repositories
2. Present options (e.g., Github-Plugins-sys-INGenious, custom path)
3. **STOP and wait** for explicit user confirmation
4. Validate path exists/writable, no conflicts

**Example:**
```
STEP 1/2: Where should I save the plugin SOURCE CODE?

Option 1: /path/to/Github-Plugins-sys-INGenious/[plugin-name]/
Option 2: Specify a custom directory

Which option?
```

### Question 2: INGenious Installation (Deployment Target)

**Ask:** "Where is your INGenious installation?"

**Steps:**
1. Detect INGenious installations (folders with Configuration/, Engine/, Projects/)
2. Present options or allow "Skip auto-deployment"
3. **STOP and wait** for explicit confirmation
4. Validate INGenious structure, plugins/ accessible

**Example:**
```
STEP 2/2: Where is your INGenious installation (deployment target)?

Option 1: /path/to/Neil-ingenious-playwright-2.3/
  └─ Plugin will deploy to: [path]/plugins/[plugin-name]/

Option 2: Specify custom path
Option 3: Skip auto-deployment (I'll deploy manually)

Which option?
```

### After Confirmation: Show Summary

```
✓ Configuration Confirmed:

Source Code Location:
  └─ /path/to/Github-Plugins-sys-INGenious/custom-actions/

Deployment Target:
  └─ /path/to/Neil-ingenious-playwright-2.3/plugins/custom-actions/

Proceeding with plugin creation...
```

### Critical Rules

**❌ Do NOT:**
- Assume workspace root is source code location
- Assume deployment target from source code location
- Create files without confirming BOTH directories
- Skip asking about INGenious installation
- Proceed if validation fails

**✅ ALWAYS:**
- Ask Question 1 (source code) and wait for confirmation
- Ask Question 2 (deployment) and wait for confirmation
- Validate both paths before proceeding
- Allow user to skip auto-deployment
- Show summary before creating files

## Quick Start: Creating a Plugin

**⚠️ IMPORTANT:** Before following these steps, ensure you've completed **Step 0** above:
- ✅ Confirmed source code directory (where to create the plugin)
- ✅ Confirmed INGenious installation directory (where to deploy the plugin)

Once both directories are confirmed, proceed with creating the plugin files:

### 1. Maven POM Configuration

**Generate pom.xml with deployment path from Step 0**

Location: `${SOURCE_CODE_DIR}/${plugin-name}/pom.xml`

**Critical Requirements:**
- Java 17 compiler target
- ingenious-api dependency with `provided` scope
- Playwright dependency with `provided` scope (browser plugins)
- maven-dependency-plugin for dependency management
- maven-jar-plugin for manifest configuration
- maven-antrun-plugin for auto-deployment (optional)

**⚠️ Why `provided` scope is critical:**
- Prevents ClassCastException by ensuring classes load from parent classloader
- Keeps plugin JAR small (~10KB vs ~10MB)

**Complete template:** See [reference/pom-complete-template.xml](reference/pom-complete-template.xml)

### 2. Plugin Entry Class Pattern

**🔴 MANDATORY: Use COMPLETE constructor pattern - do not simplify**

See [reference/constructor-pattern.md](reference/constructor-pattern.md) for:
- Complete initialization sequence
- All 5 plugin type examples (Browser, Mobile, Webservice, Database, General)
- Common mistakes to avoid

**Quick Rules:**
1. Constructor injection: Receive API contract via constructor
2. Complete initialization: Initialize ALL fields from API contract
3. Cast once: Cast Playwright/Appium objects in constructor
4. @Action annotation: Mark methods as plugin actions
5. Error handling: Always catch exceptions and report via Report API

## Plugin API Contracts

Different plugin types use different API contracts:

| Plugin Type | API Contract | Constructor Parameter |
|-------------|--------------|----------------------|
| Browser | `BrowserPluginApi` | `BrowserPluginApi gen` |
| Mobile | `MobilePluginApi` | `MobilePluginApi gen` |
| Webservice | `WebservicePluginApi` | `WebservicePluginApi gen` |
| Database | `DatabasePluginApi` | `DatabasePluginApi gen` |
| General | `CommandPluginApi` | `CommandPluginApi gen` |

**Common API Methods:**
```java
// Test data
String data = gen.getData();
String action = gen.getAction();
String input = gen.getInput();
String condition = gen.getCondition();
String objectName = gen.getObjectName();

// Reporting
TestCaseReportApi report = gen.getReport();
report.updateTestLog(action, message, Status.DONE);

// Variable management
gen.addVar("%myVar%", "value");
String value = gen.getVar("%myVar%");
```

**For complete API reference** including Browser (Playwright), Mobile (Appium), Webservice, Database methods, usage examples, and common patterns, see [reference/api-methods-quick-ref.md](reference/api-methods-quick-ref.md)

## Common Patterns

Load specific pattern examples as needed from `examples/` directory:

**Available Patterns:**
- **Element Interaction:** [examples/pattern-element-interaction.java](examples/pattern-element-interaction.java) - Click with highlighting
- **Variable Storage:** [examples/pattern-variable-storage.java](examples/pattern-variable-storage.java) - Store text in variables
- **Timeout Handling:** [examples/pattern-timeout-handling.java](examples/pattern-timeout-handling.java) - Optional timeout with navigation
- **Webservice Request:** [examples/pattern-webservice-request.java](examples/pattern-webservice-request.java) - POST request handling
- **Mobile Interaction:** [examples/pattern-mobile-interaction.java](examples/pattern-mobile-interaction.java) - Tap with validation

**Agent Loading Instructions:** Load pattern files only when user requests specific functionality. Each pattern is self-contained with complete working code.

## Troubleshooting Guide

Common errors with quick solutions. Load detailed guides from `troubleshooting/` for specific issues:

| Error | Symptom | Quick Fix | Details |
|-------|---------|-----------|---------|  
| ClassCastException | Cannot cast Playwright.Page | Use `provided` scope | [Guide](troubleshooting/classcastexception.md) |
| UnsupportedClassVersionError | Compiled by newer Java | Set Java 17 in POM | [Guide](troubleshooting/unsupported-class-version.md) |
| NoSuchMethodError | Method not found at runtime | Match Playwright 1.50.0 | [Guide](troubleshooting/nosuchmethoderror.md) |
| Duplicate Actions | Action name already exists | Rename with unique suffix | [Guide](troubleshooting/duplicate-actions.md) |
| Invalid Manifest | manifest format error | Single-line entry classes | [Guide](troubleshooting/manifest-errors.md) |

**Agent Loading:** Load specific troubleshooting guide only when user reports matching error.

## Version Compatibility

**Current Versions:** Java 17, Playwright 1.50.0, API 3.0

**Critical Rules:**
- Java compiler: ≤ 17 (framework JVM runs Java 17)
- Playwright: Must match framework version exactly (1.50.0)
- Dependency scope: `provided` for ingenious-api and playwright

**See [reference/version-compatibility.md](reference/version-compatibility.md) for:**
- Complete compatibility matrices
- Upgrade guidelines
- Troubleshooting version issues

## Best Practices

**See [reference/best-practices.md](reference/best-practices.md) for comprehensive guidance on:**

- ⚠️ Constructor pattern (CRITICAL - links to constructor-pattern.md)
- Action naming conventions (storage/assertion/general formats)
- Object type naming (descriptive nouns)
- Error handling patterns (always catch and report)
- Null safety (check before use)
- Playwright locator best practices (user-facing attributes, chaining, filtering)
- Variable management (storing/using values)
- Logging and reporting (appropriate Status usage)
- Performance considerations (cache locators, avoid unnecessary screenshots)
- Code organization (grouping, helper methods)
- Testing strategies (unit, integration, regression)
- Documentation (JavaDoc, README)
- Maintenance (versioning, changelog, backward compatibility)
- Common pitfalls to avoid (10+ anti-patterns)

## Build and Deploy

```bash
# Build plugin
mvn clean install package

# Plugin structure created:
target/
  ├── my-plugin.jar          # Plugin JAR
  └── lib/                   # Dependencies
      └── gson-2.10.1.jar
      
# If using maven-antrun-plugin, files auto-copy to:
/path/to/INGenious/plugins/my-plugin/
  ├── my-plugin.jar
  └── lib/
      └── gson-2.10.1.jar
```

## Testing Your Plugin

1. **Build**: `mvn clean install package`
2. **Deploy**: Copy JAR and lib to `INGenious/plugins/my-plugin/`
3. **Launch**: Start INGenious Playwright Studio
4. **Verify**: Your actions appear in the Object type dropdown
5. **Test**: Create test case using your plugin actions

## Example: Complete Plugin Templates

Load production-ready templates as needed from `templates/` directory:

| Template | Use Case | Features | File |
|----------|----------|----------|------|
| **Browser** | Playwright web automation | Navigation, assertions, variable storage, highlighting | [templates/BrowserTestPlugin.java](templates/BrowserTestPlugin.java) |
| **Database** | SQL operations | Query execution, variable substitution, result extraction | [templates/DatabasePlugin.java](templates/DatabasePlugin.java) |
| **General** | Utility operations | Text validation, custom assertions | [templates/TextAsserts.java](templates/TextAsserts.java) |
| **Mobile** | Appium mobile testing | Tap, scroll (Android/iOS), element validation | [templates/MobileTestPlugin.java](templates/MobileTestPlugin.java) |
| **Webservice** | REST API testing | GET/POST/PUT, JSON parsing, headers, assertions | [templates/WebserviceTestPlugin.java](templates/WebserviceTestPlugin.java) |

**Agent Loading Instructions:** Load specific template only when user requests code for that plugin type. Each template includes complete constructor pattern, action examples, and helper methods.

## Quick Reference: Status Values

```java
Status.PASS     // Action passed (with screenshot)
Status.FAIL     // Action failed (with screenshot)
Status.DONE     // Action completed
Status.PASSNS   // Pass without screenshot
Status.FAILNS   // Fail without screenshot
Status.DEBUG    // Debug message
Status.SKIP     // Action skipped
```

## When to Use Which API Contract

- **BrowserPluginApi**: Web browser automation with Playwright
- **MobilePluginApi**: Mobile app testing with Appium
- **WebservicePluginApi**: REST API and web service testing
- **DatabasePluginApi**: Database SQL operations
- **CommandPluginApi**: General purpose utilities and operations

## Additional Resources

- Complete POM template with all plugins configured
- Working plugin examples in the repository
- Full documentation in `how-to-create-plugin.md`
- Plugin samples: browser-test-plugin, mobile-test-plugin, webservice-test-plugin

---

**Critical Reminders:**

1. **Constructor Pattern**: Always use the COMPLETE constructor pattern - initialize ALL fields from the API contract (Data, Action, Input, Condition, Report, ObjectName, userData, etc.). DO NOT simplify even if some fields aren't used immediately. This ensures plugin extensibility and compatibility.

2. **Dependency Scope**: Always use `provided` scope for `ingenious-api` and `playwright` dependencies. This is critical to avoid ClassCastException and version conflicts.

3. **Java Version**: Plugin must be compiled with Java 17 or lower to match the framework's Java version.
