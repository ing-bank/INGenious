# Plugin Development Best Practices

## Constructor Pattern

**⚠️ CRITICAL: Always use the COMPLETE constructor pattern**

See [constructor-pattern.md](./constructor-pattern.md) for:
- Complete initialization sequence
- All plugin type examples
- Common mistakes to avoid

**Quick Rule:** Initialize ALL fields from API contract, even if unused initially. This ensures extensibility.

## Action Naming Conventions

### Storage Actions

Use format: `store<Data>In<Destination>`

```java
@Action(object = "Database", desc = "Store DB value in data sheet")
public void storeDBValueInDataSheet() { ... }

@Action(object = "Webservice", desc = "Store result in variable")
public void storeResultInVariable() { ... }

@Action(object = "Browser", desc = "Store element text in global variable")
public void storeValueInGlobalVariable() { ... }
```

### Assertion Actions

Use format: `assert<Object><Condition>`

```java
@Action(object = "Webservice", desc = "Assert response body contains text")
public void assertResponseBodyContains() { ... }

@Action(object = "XML", desc = "Assert XML element equals value")
public void assertXMLElementEquals() { ... }

@Action(object = "Browser", desc = "Assert element is visible")
public void assertElementIsVisible() { ... }
```

### General Actions

Use descriptive verb-noun format:

```java
@Action(object = "Browser", desc = "Click and wait for navigation")
public void clickAndWait() { ... }

@Action(object = "Browser", desc = "Fill form field with data")
public void fillFormField() { ... }

@Action(object = "Browser", desc = "Scroll to element")
public void scrollToElement() { ... }
```

### Naming Guidelines

✅ **Good:**
- `clickWithHighlight()` - descriptive, clear intent
- `waitForElementAndClick()` - sequence is clear
- `storeTextInVariable()` - format consistent

❌ **Avoid:**
- `doClick()` - vague "do" prefix
- `clk()` - abbreviations
- `action1()` - generic numbering
- `test()` - non-descriptive

## Object Type Naming

### Descriptive Nouns

```java
// ✅ Good - clear and specific
@Action(object = "Webservice", ...)
@Action(object = "Database", ...)
@Action(object = "Text Assertions", ...)
@Action(object = "XML Validation", ...)
```

### Avoid Vague Names

```java
// ❌ Avoid - unclear or abbreviated
@Action(object = "WS", ...)        // Use "Webservice"
@Action(object = "DB", ...)        // Use "Database"
@Action(object = "Test", ...)      // Use specific type
@Action(object = "Utils", ...)     // Be more specific
```

### Grouping Strategy

Group related actions under same object type:

```java
// All custom browser actions under one type
@Action(object = "Custom Browser", desc = "Click with highlight")
public void clickWithHighlight() { ... }

@Action(object = "Custom Browser", desc = "Hover and wait")
public void hoverAndWait() { ... }

@Action(object = "Custom Browser", desc = "Scroll to bottom")
public void scrollToBottom() { ... }
```

## Error Handling

### Always Catch and Report

```java
@Action(object = "Browser", desc = "Navigate to URL")
public void navigateToURL() {
    try {
        Page.navigate(Data);
        Report.updateTestLog(Action, "Navigated to: " + Data, Status.DONE);
    } catch (PlaywrightException e) {
        Report.updateTestLog(Action, 
            "Navigation failed: " + e.getMessage(), 
            Status.FAIL);
    } catch (Exception e) {
        Logger.getLogger(getClass().getName()).log(Level.OFF, null, e);
        Report.updateTestLog(Action, 
            "Unexpected error: " + e.getMessage(), 
            Status.FAIL);
    }
}
```

### Specific Exception Handling

Catch specific exceptions first:

```java
try {
    // Action logic
    Report.updateTestLog(Action, "Success", Status.DONE);
} catch (TimeoutException e) {
    Report.updateTestLog(Action, 
        "Timeout: Element not found within " + timeout + "ms", 
        Status.FAIL);
} catch (PlaywrightException e) {
    Report.updateTestLog(Action, 
        "Playwright error: " + e.getMessage(), 
        Status.FAIL);
} catch (Exception e) {
    Logger.getLogger(getClass().getName()).log(Level.OFF, null, e);
    Report.updateTestLog(Action, 
        "Unexpected error: " + e.getMessage(), 
        Status.FAIL);
}
```

### Error Message Guidelines

✅ **Good Error Messages:**
- Include context: "Failed to click button 'Submit'"
- Include values: "Expected 'Success' but got 'Error'"
- Include hints: "Element not visible, check page load state"

❌ **Poor Error Messages:**
- Generic: "Error"
- No context: "Failed"
- Just exception: e.getMessage()

## Null Safety

### Check Before Use

```java
@Action(...)
public void myAction() {
    // Always check for null
    Page page = (Page) gen.getPage();
    if (page == null) {
        Report.updateTestLog(Action, 
            "Page not available - ensure browser is open", 
            Status.FAIL);
        return;
    }
    
    // Safe to use
    page.navigate(Data);
}
```

### Check Data Fields

```java
@Action(...)
public void processData() {
    if (Data == null || Data.trim().isEmpty()) {
        Report.updateTestLog(Action, 
            "Data is required but was empty", 
            Status.FAIL);
        return;
    }
    
    // Process data
    String processed = Data.trim().toLowerCase();
    // ...
}
```

### Optional Parameters

```java
@Action(...)
public void waitAndClick() {
    // Default timeout if Condition not provided
    int timeout = 5000; // default
    
    if (Condition != null && !Condition.isEmpty()) {
        try {
            timeout = Integer.parseInt(Condition);
        } catch (NumberFormatException e) {
            Report.updateTestLog(Action, 
                "Invalid timeout: " + Condition + ", using default 5000ms", 
                Status.DEBUG);
        }
    }
    
    // Use timeout...
}
```

## Playwright Locator Best Practices

### Prioritize User-Facing Attributes

Playwright's built-in locators are more resilient:

```java
// ✅ Best - user-facing attributes (auto-waiting, retry-able)
Page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit"));
Page.getByLabel("Email");
Page.getByPlaceholder("Enter your name");
Page.getByText("Welcome");
Page.getByTestId("submit-button");

// ⚠️ Use when necessary - less resilient
Page.locator("#submit-btn");  // CSS selector
Page.locator("//button[@id='submit']");  // XPath
```

### Chain Locators for Precision

```java
// Find product 2 in a list
Locator product = Page.getByRole(AriaRole.LISTITEM)
    .filter(new Locator.FilterOptions().setHasText("Product 2"));

// Find button within a specific row
Locator row = Page.getByRole(AriaRole.ROW)
    .filter(new Locator.FilterOptions().setHasText("John"));
Locator button = row.getByRole(AriaRole.BUTTON);
button.click();
```

### Filter by Text or Locator

```java
// Filter by text
Locator filteredRows = Page.getByRole(AriaRole.ROW)
    .filter(new Locator.FilterOptions().setHasText("Active"));

// Filter by another locator
Locator rowsWithButton = Page.getByRole(AriaRole.ROW)
    .filter(new Locator.FilterOptions()
        .setHas(Page.getByRole(AriaRole.BUTTON, 
            new Page.GetByRoleOptions().setName("Delete"))));
```

### Auto-Waiting Benefits

Playwright locators automatically:
- Wait for element to be visible
- Wait for element to be enabled
- Retry on failure
- Check actionability before interaction

```java
// No explicit waits needed - Playwright handles it
Page.getByRole(AriaRole.BUTTON).click();  // Auto-waits for visible & enabled
Page.getByLabel("Email").fill(Data);      // Auto-waits for input to be ready
```

## Variable Management

### Storing Values

```java
@Action(desc = "Store element text in variable")
public void storeTextInVariable() {
    try {
        String text = Locator.textContent();
        
        // Store in variable using ObjectName as variable name
        gen.addVar("%" + ObjectName + "%", text);
        
        Report.updateTestLog(Action, 
            "Stored '" + text + "' in %" + ObjectName + "%", 
            Status.DONE);
    } catch (Exception e) {
        Report.updateTestLog(Action, 
            "Failed to store text: " + e.getMessage(), 
            Status.FAIL);
    }
}
```

### Using Variables

```java
@Action(desc = "Use stored variable value")
public void useVariableValue() {
    // Get variable value
    String value = gen.getVar("%myVariable%");
    
    if (value == null) {
        Report.updateTestLog(Action, 
            "Variable %myVariable% not found", 
            Status.FAIL);
        return;
    }
    
    // Use the value
    Page.getByLabel("Email").fill(value);
    Report.updateTestLog(Action, 
        "Used variable value: " + value, 
        Status.DONE);
}
```

### Variable Naming

```java
// ✅ Good - descriptive names
gen.addVar("%userEmail%", email);
gen.addVar("%orderTotal%", total);
gen.addVar("%loginToken%", token);

// ❌ Avoid - cryptic names
gen.addVar("%var1%", email);
gen.addVar("%x%", total);
```

## Logging and Reporting

### Use Appropriate Status

```java
Status.PASS     // Action passed (with screenshot)
Status.FAIL     // Action failed (with screenshot)
Status.DONE     // Action completed successfully
Status.PASSNS   // Pass without screenshot (faster)
Status.FAILNS   // Fail without screenshot
Status.DEBUG    // Debug message (no screenshot)
Status.SKIP     // Action skipped
```

### Report at Key Points

```java
@Action(...)
public void complexAction() {
    try {
        // Report start (optional, for debugging)
        Report.updateTestLog(Action, "Starting complex action", Status.DEBUG);
        
        // Step 1
        Page.getByLabel("Email").fill(Data);
        Report.updateTestLog(Action, "Entered email", Status.DEBUG);
        
        // Step 2
        Page.getByRole(AriaRole.BUTTON).click();
        Report.updateTestLog(Action, "Clicked submit", Status.DEBUG);
        
        // Final success
        Page.waitForURL("**/success");
        Report.updateTestLog(Action, "Complex action completed", Status.PASS);
        
    } catch (Exception e) {
        Report.updateTestLog(Action, 
            "Complex action failed at step: " + e.getMessage(), 
            Status.FAIL);
    }
}
```

### Informative Messages

```java
// ✅ Good - provides context
Report.updateTestLog(Action, 
    "Clicked button 'Submit' and navigated to confirmation page", 
    Status.DONE);

// ❌ Poor - too vague
Report.updateTestLog(Action, "Done", Status.DONE);
```

## Performance Considerations

### Cache Locators When Reusing

```java
@Action(...)
public void fillMultipleFields() {
    // Don't: Locate same element multiple times
    // Page.getByLabel("Email").fill(email);
    // Page.getByLabel("Email").click();
    
    // Do: Cache locator if using multiple times
    Locator emailField = Page.getByLabel("Email");
    emailField.fill(email);
    emailField.click();
}
```

### Avoid Unnecessary Screenshots

```java
// For frequent operations, use PASSNS/FAILNS to avoid screenshot overhead
Report.updateTestLog(Action, message, Status.PASSNS);

// Use PASS/FAIL only when screenshot is valuable
Report.updateTestLog(Action, message, Status.PASS);
```

### Batch Operations

```java
// Don't: Make multiple separate calls
for (String value : values) {
    Page.getByLabel(value).fill(value);
    Report.updateTestLog(Action, "Filled " + value, Status.DEBUG);
}

// Do: Batch operations, single report
for (String value : values) {
    Page.getByLabel(value).fill(value);
}
Report.updateTestLog(Action, "Filled " + values.size() + " fields", Status.DONE);
```

## Code Organization

### One Plugin Per Functionality Domain

```java
// ✅ Good - focused plugin
BrowserTestPlugin     // Browser automation actions
WebserviceTestPlugin  // API testing actions
DatabaseTestPlugin    // Database operations

// ❌ Avoid - mixed responsibilities
MixedPlugin          // Has browser, API, and database actions
```

### Group Related Actions

```java
public class BrowserTestPlugin {
    // Navigation actions
    public void navigateToURL() { ... }
    public void goBack() { ... }
    public void refresh() { ... }
    
    // Form actions
    public void fillField() { ... }
    public void selectOption() { ... }
    public void checkCheckbox() { ... }
    
    // Assertion actions
    public void assertTextEquals() { ... }
    public void assertElementVisible() { ... }
}
```

### Helper Methods

```java
// Private helpers for common logic
private boolean waitForElement(Locator locator, int timeout) {
    try {
        locator.waitFor(new Locator.WaitForOptions().setTimeout(timeout));
        return true;
    } catch (TimeoutException e) {
        return false;
    }
}

// Use in actions
@Action(...)
public void clickWhenReady() {
    if (waitForElement(Locator, 5000)) {
        Locator.click();
        Report.updateTestLog(Action, "Clicked element", Status.DONE);
    } else {
        Report.updateTestLog(Action, "Element not ready", Status.FAIL);
    }
}
```

## Testing Your Plugin

### Unit Testing

Create test class in `src/test/java`:

```java
public class BrowserTestPluginTest {
    @Test
    public void testActionLogic() {
        // Test action logic with mocked API
        BrowserPluginApi mockApi = mock(BrowserPluginApi.class);
        when(mockApi.getData()).thenReturn("test-data");
        
        BrowserTestPlugin plugin = new BrowserTestPlugin(mockApi);
        // Assert behavior...
    }
}
```

### Integration Testing

1. Build plugin: `mvn clean package`
2. Deploy to test INGenious installation
3. Create test case using your actions
4. Run and verify results
5. Check logs for errors

### Regression Testing

After changes:
1. Rebuild plugin
2. Run existing test cases
3. Verify no regressions
4. Test new functionality

## Documentation

### JavaDoc for Actions

```java
/**
 * Clicks an element after highlighting it for visual feedback.
 * 
 * @Action annotation specifies this as a browser action
 * @param None - uses Locator from test case
 * @requires Locator must be set in test case
 * @reports DONE on success, FAIL on error
 */
@Action(object = ObjectType.BROWSER, 
        desc = "Click element with highlight effect")
public void clickWithHighlight() {
    // Implementation...
}
```

### README for Plugin

Include in your plugin project:

```markdown
# Custom Browser Actions Plugin

## Purpose
Provides enhanced browser automation actions with visual feedback.

## Actions
- `clickWithHighlight` - Clicks element after highlighting
- `hoverAndWait` - Hovers over element with configurable wait time

## Installation
1. Build: `mvn clean package`
2. Copy `target/custom-browser-actions.jar` and `target/lib/` to INGenious `plugins/custom-browser-actions/`
3. Restart INGenious

## Requirements
- INGenious Playwright 2.3+
- Java 17
- Playwright 1.50.0

## Usage
See examples/ directory for sample test cases.
```

## Maintenance

### Version Your Plugin

Use semantic versioning in POM:

```xml
<version>1.0.0</version>  <!-- Initial release -->
<version>1.1.0</version>  <!-- New feature added -->
<version>1.0.1</version>  <!-- Bug fix -->
<version>2.0.0</version>  <!-- Breaking change -->
```

### Changelog

Maintain CHANGELOG.md:

```markdown
# Changelog

## [1.1.0] - 2026-05-23
### Added
- New `scrollToBottom` action
- Support for timeout configuration via Condition field

### Fixed
- Highlight effect not clearing on Firefox

## [1.0.0] - 2026-05-01
### Added
- Initial release
- clickWithHighlight action
- hoverAndWait action
```

### Backward Compatibility

When updating:
- Don't remove existing actions (deprecate instead)
- Don't change action signatures
- Document breaking changes clearly
- Provide migration guide

```java
/**
 * @deprecated Use clickWithOptions() instead
 * Will be removed in version 2.0
 */
@Action(...)
public void oldClickMethod() {
    // Keep for backward compatibility
    clickWithOptions();
}
```

## Common Pitfalls to Avoid

1. **❌ Forgetting `provided` scope** → ClassCastException
2. **❌ Incomplete constructor initialization** → NullPointerException
3. **❌ Not catching exceptions** → Unhandled errors crash tests
4. **❌ Using Java > 17** → UnsupportedClassVersionError
5. **❌ Mismatched Playwright versions** → NoSuchMethodError
6. **❌ Not reporting failures** → Silent failures confuse users
7. **❌ Hardcoding values** → Use Data, Input, Condition fields
8. **❌ Poor action names** → Users can't find your actions
9. **❌ Missing null checks** → Runtime crashes
10. **❌ No documentation** → Users don't know how to use plugin

Follow these best practices to create robust, maintainable, and user-friendly plugins!
