# Version Compatibility Reference

## Current Framework Versions

As of the latest INGenious Playwright Framework release:

- **Java**: 17
- **Playwright**: 1.50.0  
- **INGenious API**: 3.0
- **Maven**: 3.6.3 or higher

## Compatibility Requirements

### Java Version

| Requirement | Value | Why |
|-------------|-------|-----|
| Java Compiler | ≤ 17 | Framework JVM runs Java 17 |
| Recommended | 17 | Best compatibility |
| Minimum | 11 | Lower versions work but deprecated |

### Playwright Version

| Requirement | Value | Why |
|-------------|-------|-----|
| Playwright | 1.50.0 | Must match framework exactly |
| Scope | `provided` | Critical - prevents ClassCastException |

### API Version

| Requirement | Value | Why |
|-------------|-------|-----|
| ingenious-api | 3.0 | Current framework API |
| Scope | `provided` | Critical - load from parent classloader |

## Compatibility Matrices

### Java Compatibility Matrix

| Plugin Java Version | Framework Java Version | Result | Notes |
|---------------------|------------------------|--------|-------|
| 17 | 17 | ✅ **Recommended** | Perfect match |
| 11 | 17 | ✅ Works | Forward compatible |
| 8 | 17 | ✅ Works | Forward compatible (deprecated) |
| 21 | 17 | ❌ **UnsupportedClassVersionError** | Plugin compiled with newer Java |
| 19 | 17 | ❌ **UnsupportedClassVersionError** | Plugin compiled with newer Java |

**Rule:** Plugin's compiled Java version MUST be ≤ Framework's Java version

**Error Example:**
```
java.lang.UnsupportedClassVersionError: 
com/ing/plugin/MyPlugin has been compiled by a more recent version of the Java Runtime (class file version 61.0), 
this version of the Java Runtime only recognizes class file versions up to 61.0
```

**Fix:** Set `maven.compiler.target` to 17 or lower in your POM:
```xml
<properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

### Playwright Compatibility Matrix

| Plugin Version | Framework Version | Result | Notes |
|----------------|-------------------|--------|-------|
| 1.50.0 | 1.50.0 | ✅ **Recommended** | Exact match |
| 1.50.0 | 1.48.0 | ✅ Works | Plugin can use 1.48-1.50 APIs |
| 1.55.0 | 1.50.0 | ❌ **NoSuchMethodError** | Plugin uses APIs not in framework |
| 1.40.0 | 1.50.0 | ✅ Works | Limited to 1.40 APIs |
| 1.30.0 | 1.50.0 | ⚠️ May work | Very old, not tested |

**Rule:** Plugin's Playwright version SHOULD match framework's version exactly

**Error Example:**
```
java.lang.NoSuchMethodError: 
com.microsoft.playwright.Page.getByTestId(Ljava/lang/String;)Lcom/microsoft/playwright/Locator;
```

**Fix:** Use exact Playwright version:
```xml
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.50.0</version>
    <scope>provided</scope>
</dependency>
```

### INGenious API Compatibility Matrix

| Plugin API Version | Framework API Version | Result | Notes |
|--------------------|----------------------|--------|-------|
| 3.0 | 3.0 | ✅ **Recommended** | Perfect match |
| 2.x | 3.0 | ❌ **Incompatible** | Major version change |
| 3.0 | 2.x | ❌ **Incompatible** | Framework too old |

**Rule:** Plugin API major version MUST match framework API major version

## Dependency Scope Requirements

### Critical: Use `provided` Scope

**ALWAYS use `provided` scope for:**
- `ingenious-api`
- `playwright` (for browser plugins)
- `appium-java-client` (for mobile plugins)

**Why `provided` is critical:**

1. **Prevents ClassCastException**
   - Framework loads these classes from parent classloader
   - Plugin's copy would create different class instances
   - Cast operations would fail

2. **Keeps Plugin JAR Small**
   - `provided`: ~10KB plugin JAR
   - `compile`: ~10MB plugin JAR (includes all dependencies)

3. **Version Control**
   - Framework controls versions for compatibility
   - Plugin inherits correct versions automatically

**Example POM:**
```xml
<dependencies>
    <!-- ✅ CORRECT - provided scope -->
    <dependency>
        <groupId>com.ing</groupId>
        <artifactId>ingenious-api</artifactId>
        <version>3.0</version>
        <scope>provided</scope>
    </dependency>
    
    <dependency>
        <groupId>com.microsoft.playwright</groupId>
        <artifactId>playwright</artifactId>
        <version>1.50.0</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- ✅ CORRECT - compile scope for plugin-specific dependencies -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
        <scope>compile</scope>
    </dependency>
</dependencies>
```

## Upgrade Guidelines

### Upgrading Framework Version

When INGenious framework is upgraded:

1. **Check Release Notes**
   - Playwright version changes?
   - API version changes?
   - Breaking changes?

2. **Update Plugin POM**
   ```xml
   <properties>
       <!-- Match framework Java version -->
       <maven.compiler.target>17</maven.compiler.target>
   </properties>
   
   <dependencies>
       <!-- Match framework Playwright version -->
       <dependency>
           <groupId>com.microsoft.playwright</groupId>
           <artifactId>playwright</artifactId>
           <version>1.50.0</version>
           <scope>provided</scope>
       </dependency>
       
       <!-- Match framework API version -->
       <dependency>
           <groupId>com.ing</groupId>
           <artifactId>ingenious-api</artifactId>
           <version>3.0</version>
           <scope>provided</scope>
       </dependency>
   </dependencies>
   ```

3. **Test Plugin**
   - Rebuild: `mvn clean package`
   - Deploy to test environment
   - Run regression tests

4. **Check for Deprecated APIs**
   - Review compiler warnings
   - Update to new API methods if available

### Upgrading Java Version

If framework upgrades to Java 21:

1. **Update POM**
   ```xml
   <properties>
       <maven.compiler.source>21</maven.compiler.source>
       <maven.compiler.target>21</maven.compiler.target>
   </properties>
   ```

2. **Test Compatibility**
   - Rebuild plugin
   - Check for any compilation errors
   - Test runtime behavior

3. **Optional: Use New Java Features**
   - Pattern matching
   - Records
   - Text blocks
   - etc.

## Troubleshooting Version Issues

### Issue: UnsupportedClassVersionError

**Symptom:**
```
java.lang.UnsupportedClassVersionError: com/ing/plugin/MyPlugin 
has been compiled by a more recent version of the Java Runtime
```

**Cause:** Plugin compiled with Java > 17, framework runs Java 17

**Fix:** Lower Java version in POM:
```xml
<maven.compiler.target>17</maven.compiler.target>
```

### Issue: NoSuchMethodError

**Symptom:**
```
java.lang.NoSuchMethodError: com.microsoft.playwright.Page.newMethod()
```

**Cause:** Plugin uses Playwright API not available in framework's Playwright version

**Fix:** Use framework's Playwright version:
```xml
<version>1.50.0</version> <!-- Match framework -->
```

### Issue: ClassCastException

**Symptom:**
```
java.lang.ClassCastException: 
com.microsoft.playwright.Page cannot be cast to com.microsoft.playwright.Page
```

**Cause:** Plugin has `compile` scope for Playwright (should be `provided`)

**Fix:** Use `provided` scope:
```xml
<scope>provided</scope>
```

## Version Detection

### Check Framework Versions

**Via INGenious Installation:**
```bash
# Check Engine JAR manifest
unzip -p Engine/target/Engine.jar META-INF/MANIFEST.MF | grep Implementation-Version

# Check pom.xml
grep -A 1 "<artifactId>playwright</artifactId>" Engine/pom.xml
```

**Via Runtime:**
```java
// In plugin code
String playwrightVersion = Page.class.getPackage().getImplementationVersion();
System.out.println("Playwright: " + playwrightVersion);
```

### Check Plugin Versions

**Via Plugin JAR:**
```bash
# Check compiled Java version
javap -verbose MyPlugin.class | grep "major version"

# Major version 61 = Java 17
# Major version 65 = Java 21
```

## Best Practices

1. **Always Match Versions**
   - Use exact Playwright version as framework
   - Use exact API version as framework
   - Use Java version ≤ framework

2. **Use `provided` Scope**
   - For ingenious-api
   - For playwright/appium
   - For any framework-provided library

3. **Test After Upgrades**
   - Rebuild plugin
   - Run full test suite
   - Check for warnings/errors

4. **Document Version Requirements**
   - Add README to plugin project
   - List compatible framework versions
   - Note any version-specific behaviors

5. **Monitor Framework Releases**
   - Subscribe to release notifications
   - Review release notes
   - Test plugins with new versions promptly
