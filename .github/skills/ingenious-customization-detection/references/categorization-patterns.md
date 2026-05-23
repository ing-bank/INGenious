# Change Categorization Patterns

## Overview

This document details the pattern-matching logic used to automatically categorize customizations into functional groups.

## Categories

### 1. Feature Enhancement

**Intent:** New functionality added, extended capabilities

**Patterns:**
- `new class`
- `new method`
- `implements`
- `extends`
- `Added functionality`
- `Enhanced`
- `public class` (when file is new)
- `public void` (new methods)
- Custom action methods
- Plugin additions

**Example:**
```java
+ public class CustomBrowserActions {
+     public void clickWithHighlight(String objectName) {
+         // New functionality
+     }
+ }
```

**Assessment:** Usually Medium-High impact

---

### 2. Bug Fix

**Intent:** Error handling improvements, fixing issues

**Patterns:**
- `fix`
- `fixed`
- `null check`
- `!= null`
- `== null`
- `exception`
- `try-catch`
- `try {`
- `catch (`
- `validate`
- `NullPointerException`
- `throw new`

**Example:**
```java
  public void click(String objectName) {
+     if (objectName == null) {
+         throw new IllegalArgumentException("Object name cannot be null");
+     }
      element = findElement(objectName);
```

**Assessment:** Usually Low-Medium impact

---

### 3. Configuration

**Intent:** POM modifications, dependency updates, build configuration

**Patterns:**
- `pom.xml` (file path)
- `<dependency>`
- `<plugin>`
- `<version>`
- `.properties` (file extension)
- `dependency`
- `maven`
- `.xml` (in Configuration/)

**Example:**
```xml
+ <dependency>
+     <groupId>com.custom</groupId>
+     <artifactId>custom-lib</artifactId>
+     <version>1.0.0</version>
+ </dependency>
```

**Assessment:** Usually Low-Medium impact

---

### 4. Integration

**Intent:** External tool integrations, API modifications

**Patterns:**
- `API`
- `REST`
- `external`
- `third-party`
- `integration`
- `import org.apache.http`
- `import java.net.http`
- `HttpClient`
- `WebClient`
- `RestTemplate`

**Example:**
```java
+ import java.net.http.HttpClient;
+ 
+ public void callExternalAPI(String endpoint) {
+     HttpClient client = HttpClient.newHttpClient();
+     // Integration code
+ }
```

**Assessment:** Usually Medium-High impact

---

### 5. Performance Optimization

**Intent:** Code optimizations, caching, efficiency improvements

**Patterns:**
- `optimize`
- `optimized`
- `cache`
- `cached`
- `performance`
- `faster`
- `efficient`
- `parallel`
- `async`
- `CompletableFuture`
- `ExecutorService`
- `@Async`

**Example:**
```java
+ private final Map<String, Element> elementCache = new HashMap<>();
+ 
  public Element findElement(String objectName) {
+     if (elementCache.containsKey(objectName)) {
+         return elementCache.get(objectName);
+     }
      Element element = locator.find(objectName);
+     elementCache.put(objectName, element);
      return element;
  }
```

**Assessment:** Usually Medium impact

---

### 6. UI/Reporting

**Intent:** Report template modifications, dashboard customizations

**Patterns:**
- `report`
- `template`
- `HTML`
- `html`
- `.html` (file extension)
- `.css` (file extension)
- `dashboard`
- `display`
- `UI`
- `chart`
- `graph`
- `visualization`

**Example:**
```html
+ <div class="custom-report-section">
+     <h2>Custom Metrics</h2>
+     <!-- New reporting section -->
+ </div>
```

**Assessment:** Usually Low impact

---

### 7. Framework Core

**Intent:** Core engine changes, API contract modifications, architecture changes

**Patterns:**
- `engine` (in path)
- `core` (in path)
- `framework`
- `architecture`
- `Plugin`
- `Engine/src/main/java/com/ing/engine/core`
- `Engine/src/main/java/com/ing/engine/execution`
- `Datalib/src`
- API contract changes

**Example:**
```java
  // In com.ing.engine.core.ExecutionEngine
  public void execute(TestCase testCase) {
+     // Custom pre-execution hook
+     customPreExecutionHook(testCase);
      standardExecution(testCase);
  }
```

**Assessment:** Usually High impact (affects upgrade path)

---

## Impact Assessment

### High Impact

**Criteria:**
- Changes to framework core classes
- API contract modifications
- Plugin system changes
- Execution engine modifications
- Significant structural changes (>200 lines)

**Implications:**
- May block framework upgrades
- Requires careful testing
- Consider extracting to plugin

**Examples:**
- Modified `ExecutionEngine.java`
- Changed plugin loading mechanism
- Altered report generation pipeline

---

### Medium Impact

**Criteria:**
- Feature additions (new classes/methods)
- Integration customizations
- Significant bug fixes
- Performance optimizations
- Moderate changes (50-200 lines)

**Implications:**
- May need adjustments during upgrade
- Good plugin candidates
- Should document intent

**Examples:**
- New custom action methods
- External API integrations
- Caching mechanisms

---

### Low Impact

**Criteria:**
- Minor bug fixes
- Configuration changes
- UI/reporting tweaks
- Small changes (<50 lines)
- Localized modifications

**Implications:**
- Unlikely to block upgrades
- Easy to maintain
- May not need pluginization

**Examples:**
- Null checks added
- POM dependency version bumps
- Report template styling

---

## Pattern Matching Algorithm

### Pseudo-code

```python
def categorize_change(file_path, diff_content):
    categories = {
        "Feature Enhancement": [...patterns...],
        "Bug Fix": [...patterns...],
        ...
    }
    
    # Combine file path and content for matching
    combined_text = f"{file_path} {diff_content}".lower()
    
    # Check each category's patterns
    for category, patterns in categories.items():
        for pattern in patterns:
            if re.search(pattern.lower(), combined_text):
                return category
    
    return "Other"
```

### Priority Rules

If multiple categories match:

1. **Framework Core** takes precedence (highest risk)
2. **Integration** over Feature (external dependency)
3. **Bug Fix** over Feature (for simple additions)
4. **First match wins** for equal priority

---

## Examples by Category

### Multi-category Changes

**Example: Added caching to API integration**

```java
+ import java.net.http.HttpClient;
+ private Map<String, Response> cache = new HashMap<>();
+ 
+ public Response callAPI(String endpoint) {
+     if (cache.containsKey(endpoint)) {
+         return cache.get(endpoint);
+     }
+     HttpClient client = HttpClient.newHttpClient();
+     Response response = client.send(request);
+     cache.put(endpoint, response);
+     return response;
+ }
```

**Categories matched:**
- Integration (HttpClient, API)
- Performance (cache)

**Selected:** Integration (higher priority)

---

### Edge Cases

**Configuration that affects framework:**

```xml
<dependency>
    <groupId>com.ing</groupId>
    <artifactId>ingenious-engine</artifactId>
-   <version>2.3.0</version>
+   <version>3.0.0-SNAPSHOT</version>
</dependency>
```

**Categories:**
- Configuration (pom.xml, dependency)
- Framework Core (engine, major version change)

**Selected:** Framework Core (version change affects core)

---

## Custom Rules

### Project-specific Patterns

Teams can extend categorization with project-specific patterns:

**Example: Custom package naming**

```python
# If custom actions are in specific package
if "com.company.customactions" in file_path:
    category = "Feature Enhancement"
    subcategory = "Custom Actions"
```

### Exclusion Patterns

**Auto-generated files** should not be categorized:

```python
excluded_patterns = [
    "*Generated.java",
    "target/",
    "*.class"
]

if matches_excluded(file_path):
    return "Excluded"
```

---

## Reporting Categories

### Report Structure

```markdown
## Customizations by Category

### Feature Enhancement (12 files)
[Details...]

### Bug Fix (8 files)
[Details...]

### Configuration (5 files)
[Details...]
```

### Category Statistics

```json
{
  "categories": {
    "Feature Enhancement": {
      "count": 12,
      "files": [...],
      "lines_added": 456,
      "lines_removed": 23
    },
    ...
  }
}
```
