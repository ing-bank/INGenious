# NoSuchMethodError

## Symptom
```
java.lang.NoSuchMethodError: com.microsoft.playwright.Page.someMethod()
```

## Cause
Your plugin is using a Playwright API method that doesn't exist in the framework's Playwright version (1.50.0).

This happens when:
- Plugin uses newer Playwright version (e.g., 1.55.0) with new methods
- Framework runs older version (1.50.0) without those methods

## Solution

Update your plugin's Playwright version to match the framework:

```xml
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.50.0</version>  <!-- Must match framework -->
    <scope>provided</scope>
</dependency>
```

Then rebuild:
```bash
mvn clean install package
```

## Version Compatibility Matrix

| Plugin Playwright | Framework Playwright | Result |
|------------------|---------------------|--------|
| 1.50.0 | 1.50.0 | ✅ Recommended |
| 1.55.0 | 1.50.0 | ❌ NoSuchMethodError at runtime |
| 1.40.0 | 1.50.0 | ✅ Works (limited to 1.40.0 APIs) |

## Prevention

Always check the framework's Playwright version and match it exactly in your plugin's POM.

Current framework version: **1.50.0**

## Finding Framework Version

Check the framework's lib directory:
```bash
ls -l /path/to/INGenious/lib/playwright*.jar
```
