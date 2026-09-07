# ClassCastException

## Symptom
```
java.lang.ClassCastException: cannot cast com.microsoft.playwright.Page to com.microsoft.playwright.Page
```

## Causes & Solutions

### 1. Wrong Dependency Scope
**Problem:** Playwright dependency not using `provided` scope

**Solution:**
```xml
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.50.0</version>
    <scope>provided</scope>  <!-- MUST be provided -->
</dependency>
```

### 2. Missing Dependency
**Problem:** Playwright dependency not declared in POM

**Solution:** Add Playwright with `provided` scope to your plugin's `pom.xml`

### 3. Version Mismatch
**Problem:** Plugin uses different Playwright version than framework

**Solution:** Use exact version 1.50.0 to match framework
```xml
<version>1.50.0</version>  <!-- Must match framework -->
```

## Why This Happens

When dependencies are not `provided`, Maven bundles them in your plugin JAR. This creates two different `Page` classes:
- One from framework's classloader
- One from plugin's classloader

Even though they're the same class, Java considers them different because they're loaded by different classloaders.

## Prevention

Always use `provided` scope for:
- `ingenious-api`
- `playwright`
- Any framework-provided dependencies
