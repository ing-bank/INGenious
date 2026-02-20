## Table of Contents

- [INGenious Plugin system](#ingenious-plugin-system)
- [Plugin Directory Structure](#plugin-directory-structure)
- [How to Create Your Plugin](#how-to-create-your-plugin)
- [Working with Playwright Objects](#working-with-playwright-objects)
- [INGenious Object types and Actions](#ingenious-object-types-and-actions)
    - [Best Practice](#best-practice)
    - [Troubleshooting](#troubleshooting)
- [Plugin Dependencies](#plugin-dependencies)
- [Version Compatibility](#version-compatibility)
- [Complete Plugin Template](#complete-plugin-template)



---

## INGenious Plugin system

This guide explains how to create a custom plugin for the INGenious Playwright Framework. Plugins allow you to extend the platform with new automation actions and integrate with Playwright's browser automation capabilities.

### Architecture Overview

The INGenious Framework uses a sophisticated plugin architecture with:
- **API Module** (`ingenious-api`): Provides stable interfaces between framework and plugins
- **Custom Classloaders**: Each plugin runs in isolated classloader environment
- **Type Erasure Pattern**: Playwright objects passed as `Object` type for version independence
- **Parent-First Delegation**: Critical packages (Playwright, API) loaded from parent classloader

This architecture ensures plugins can be developed independently while maintaining compatibility with the framework.

## Plugin Directory Structure

Your plugins should be organized in the following directory structure:

```
plugins/
    ├── pluginA/
    │   ├── plugin-a.jar
    │   └── lib/
    └── pluginB/
        ├── plugin-b.jar
        └── lib/
```

Each plugin resides in its own subfolder under `plugins`, containing the plugin JAR and a `lib` directory for its dependencies.


## How to Create Your Plugin

Follow these steps to build and deploy a custom plugin for the INGenious Playwright Framework:

### 1. Set Up Your Maven Project

Create a generic Maven Java project (no main class required).

**Configure Java Version** - The framework runs on **Java 17**, so plugins must be compiled with Java 17 or lower:

```xml
<properties>
    <!-- REQUIRED: Match framework Java version -->
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

**Add Required Dependencies** - In your `pom.xml`:

```xml
<dependencies>
    <!-- REQUIRED: API module with provided scope -->
    <dependency>
        <groupId>com.ing.ingenious</groupId>
        <artifactId>ingenious-api</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- REQUIRED: Playwright with provided scope -->
    <dependency>
        <groupId>com.microsoft.playwright</groupId>
        <artifactId>playwright</artifactId>
        <version>1.50.0</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- Optional: Add your plugin-specific dependencies here -->
</dependencies>
```

**Important**: Both dependencies MUST use `<scope>provided</scope>`. This tells Maven not to bundle these libraries in your plugin JAR, as they will be provided by the framework. This prevents classloader conflicts and keeps your plugin JAR small.

### 2. Configure Dependency Packaging

If your plugin has additional dependencies (beyond `ingenious-api` and `playwright`), configure the Maven Dependency Plugin to copy them to the `lib` folder:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <version>3.6.0</version>
    <executions>
        <execution>
            <id>copy-dependencies</id>
            <phase>package</phase>
            <goals>
                <goal>copy-dependencies</goal>
            </goals>
            <configuration>
                <outputDirectory>${project.build.directory}/lib</outputDirectory>
                <includeScope>compile</includeScope>
                <excludeTransitive>true</excludeTransitive>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Note**: Since `ingenious-api` and `playwright` use `provided` scope, they are automatically excluded from the `lib` folder. Only `compile` scoped dependencies will be copied.

**Optional**: You can skip this step entirely if your plugin has no additional dependencies beyond the required `provided` ones.

### 3. Declare Plugin Entry Classes

Entry classes contain your action methods and are dynamically instantiated by INGenious. Specify them in the JAR manifest using the Maven JAR plugin:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <version>3.3.0</version>
    <configuration>
        <archive>
            <manifestEntries>
                <pluginEntryClasses>
                    com.example.plugin.BrowserActions,com.example.plugin.CustomActions
                </pluginEntryClasses>
                <Implementation-Version>${project.version}</Implementation-Version>
            </manifestEntries>
        </archive>
    </configuration>
</plugin>
```

List fully qualified class names, separated by commas.

### 4. Automate Deployment (Optional)

To automatically copy your JAR and dependencies to the plugin directory, use the Maven Antrun plugin. Update `deploy.dir` to your target plugin folder:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-antrun-plugin</artifactId>
    <version>3.1.0</version>
    <executions>
        <execution>
            <id>copy-artifacts</id>
            <phase>package</phase>
            <configuration>
                <target>
                    <property name="deploy.dir" value="/path/to/INGenious/plugins/my-plugin"/>
                    <copy file="${project.build.directory}/${project.build.finalName}.jar"
                          tofile="${deploy.dir}/${project.artifactId}.jar"/>
                    <copy todir="${deploy.dir}/lib">
                        <fileset dir="${project.build.directory}/lib"/>
                    </copy>
                </target>
            </configuration>
            <goals>
                <goal>run</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 5. Implement Entry Classes

Create your entry class(es) with action methods. Entry classes receive the contract interface (such as `GeneralBrApi` or `GeneralDbApi`) via constructor injection. All framework functionalities are accessed through this contract instance, which is provided to your entry class by the framework at runtime.

**Basic Plugin Example:**

```java
package com.ing.plugin.browser;

import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.contract.GeneralBrApi;
import com.ing.ingenious.api.contract.reports.TestCaseReportApi;
import com.ing.ingenious.api.types.ObjectType;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.status.Status;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.NavigateOptions;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PageAssertions;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.microsoft.playwright.options.AriaRole;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.opentest4j.AssertionFailedError;
import java.util.regex.Pattern;

import com.ing.samp.dependency.SampDependency;

/**
 *
 * @author qs01nn
 */
public class BrowserTestPlugin {

    GeneralBrApi gen;

    public String Data;
    public String Action;
    public String Input;
    public String Condition;
    public TestCaseReportApi report;

    public String ObjectName;

    // Playwright objects
    public Page Page;
    public Locator Locator;

    public BrowserTestPlugin(GeneralBrApi gen) {
        System.out.println("BrowserTestPlugin initialized with GeneralBrApi: " + gen);
        this.gen = gen;
        this.Data = gen.getData();
        this.Action = gen.getAction();
        this.Input = gen.getInput();
        this.Condition = gen.getCondition();
        this.report = gen.getReport();
        
        this.ObjectName = gen.getObjectName();
        
        this.Page = (Page) gen.getPage();
        this.Locator = (Locator) gen.getLocator();
        
    }

    @Action(object = ObjectType.BROWSER, desc = "Open the Url [<Data>] in the Browser", input = InputType.YES, condition = InputType.OPTIONAL)
    public void OpenTest() {
        
        Boolean pageTimeOut = false;
        NavigateOptions navigateOptions = new NavigateOptions();
        try {
            if (Condition.matches("[0-9]+")) {
                navigateOptions.setTimeout(Double.parseDouble(Condition));
            }
            Page.navigate(Data, navigateOptions);
            report.updateTestLog("Open", "Opened Url: " + Data, Status.DONE);
        } catch (TimeoutError e) {
            report.updateTestLog("Open",
                    "Opened Url: " + Data + " and cancelled page load after " + Condition + " seconds", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            report.updateTestLog("Open", e.getMessage(), Status.FAIL);
        }
        if (pageTimeOut) {
            setPageTimeOut(300);
        }
    }
}
```

### 6. Build and Deploy

Run the following Maven command:

```bash
mvn clean install package
```

If you configured automated deployment (Step 4), your plugin JAR and `lib` folder will be copied to the INGenious plugin directory. Otherwise, copy them manually to:

```
plugins/
    └── my-plugin/
        ├── my-plugin.jar
        └── lib/
```

### 7. Use Your Plugin

Launch INGenious Playwright Studio. Your plugin actions will appear in the suggested actions list under the relevant Object type.

---

## Working with Playwright Objects

The framework provides access to Playwright objects (Page, Locator, BrowserContext, etc.) through the `GeneralBrApi` interface. Due to the classloader architecture, these objects are returned as `Object` type to maintain version independence.

### Accessing Playwright Objects

Refer to the **BrowserTestPlugin** example in [Step 5](#5-implement-entry-classes) above for a complete, working implementation. 

Key points demonstrated in that example:

1. **Constructor Pattern**: Accept `GeneralBrApi` as a constructor parameter
   ```java
   public BrowserTestPlugin(GeneralBrApi gen) {
       this.gen = gen;
       this.Data = gen.getData();
       this.report = gen.getReport();
       // Cast Playwright objects once in constructor
       this.Page = (Page) gen.getPage();
       this.Locator = (Locator) gen.getLocator();
   }
   ```

2. **Cast Once in Constructor**: Get Playwright objects from the API and store them as typed fields
   ```java
   public Page Page;        // Cast once, use many times
   public Locator Locator;  // Full IDE autocomplete support
   ```

3. **Use in Action Methods**: Use the typed fields directly with full Playwright API
   ```java
   @Action(object = ObjectType.BROWSER, desc = "Open the Url")
   public void OpenTest() {
       Page.navigate(Data, navigateOptions);
       report.updateTestLog("Open", "Opened Url: " + Data, Status.DONE);
   }
   ```

### Best Practices

**1. Cast-Once in Constructor Pattern** (as shown in BrowserTestPlugin):
```java
// ✅ Best Practice - Cast once in constructor, use everywhere
public BrowserTestPlugin(GeneralBrApi gen) {
    this.Page = (Page) gen.getPage();      // Cast once
    this.Locator = (Locator) gen.getLocator(); // Cast once
}

@Action(...)
public void someAction() {
    Page.navigate(Data);  // Use typed field with autocomplete
}
```

**2. Null Checking for Safety**:
```java
Page page = (Page) gen.getPage();
if (page == null) {
    report.updateTestLog(Action, "Page not available", Status.FAIL);
    return;
}
```

**3. Error Handling** (as shown in BrowserTestPlugin OpenTest method):
```java
try {
    Page.navigate(Data, navigateOptions);
    report.updateTestLog("Open", "Opened Url: " + Data, Status.DONE);
} catch (TimeoutError e) {
    report.updateTestLog("Open", "Timeout occurred", Status.DONE);
} catch (Exception e) {
    report.updateTestLog("Open", e.getMessage(), Status.FAIL);
}
```

### Additional GeneralBrApi Methods

Beyond Playwright objects, `GeneralBrApi` provides access to:
- `getData()` - Test data input
- `getAction()` - Current action name
- `getInput()` - Input parameter
- `getCondition()` - Condition parameter  
- `getReport()` - Returns `TestCaseReportApi` for logging test results
- `getObjectName()` - Current object name

See the BrowserTestPlugin constructor for how to initialize these in your plugin.

### Using the Test Report API

The `getReport()` method returns a `TestCaseReportApi` instance for logging test results. This is essential for providing feedback in the test execution reports.

**Common reporting methods:**

```java
// Get the report instance (typically in constructor)
TestCaseReportApi report = gen.getReport();

// Log test results with different statuses
report.updateTestLog(action, message, Status.PASS);   // Success
report.updateTestLog(action, message, Status.FAIL);   // Failure
report.updateTestLog(action, message, Status.DONE);   // Completed
report.updateTestLog(action, message, Status.PASSNS); // Pass (no screenshot)
report.updateTestLog(action, message, Status.FAILNS); // Fail (no screenshot)
```

**Example from BrowserTestPlugin:**

```java
try {
    Page.navigate(Data, navigateOptions);
    report.updateTestLog("Open", "Opened Url: " + Data, Status.DONE);
} catch (TimeoutError e) {
    report.updateTestLog("Open",
        "Opened Url: " + Data + " and cancelled page load after " + Condition + " seconds", 
        Status.DONE);
} catch (Exception e) {
    report.updateTestLog("Open", e.getMessage(), Status.FAIL);
}
```

**Available Status values:**
- `Status.PASS` - Action passed (with screenshot)
- `Status.FAIL` - Action failed (with screenshot)
- `Status.DONE` - Action completed
- `Status.PASSNS` - Pass without screenshot
- `Status.FAILNS` - Fail without screenshot
- `Status.SKIP` - Action skipped

---

## INGenious Object types and Actions
Object types in INGenious categorize automation actions, helping organize and group related functionalities within the platform. Actions are the operations or commands you define for each object type. By creating plugins, you can introduce new object types and their associated actions, making them available in the INGenious UI alongside built-in types. This extensibility allows you to tailor the automation framework to your specific testing needs.

Multiple new object types can also be added in a single entry class. Ensure object types and actions are defined inside your entry class-otherwise they will not be detected by INGenious. Note that object type names are case sensitive (e.g., xml and XML are treated as different types).

To add new Object Type, declare it inside the @Action annotation as the object. See example below. 

``` java
@Action(object = "Numeric Assert", desc = "Assert if input is even number", input = InputType.YES, condition = InputType.NO)
    public void assertEvenNumber(){
        String var = getVar(Input);
        System.out.println("Input is " + var);
        int number = Integer.parseInt(var);
        if(number % 2 != 0){
            Report.updateTestLog(Action, "The input " + Data + " is not an even number.", Status.FAILNS);
        } else { 
            Report.updateTestLog(Action, "The input " + Data + " is an even number.", Status.PASSNS);
        }
        
    }
```

### Best Practice
**Object Naming**
- Use descriptive nouns that clearly represent the testing domain concept (e.g., `Webservice`, `Database`).
- Objects can also represent items that have associated actions, such as `XMLDocument` for XML-related operations (e.g., create XML document, add child nodes).
- Avoid abbreviations unless they are widely recognized (e.g., Api, Id).

**Storage Action Naming**
- Use format `store<Data>In<TargetDestination>` (e.g., `storeDBValueInDataSheet`, `storeResultInVariable`, `storeValueInGlobalVariable`).

**Assert Action Naming**
- Use format `assert<ObjectOfAssertion><Condition>` (e.g., `assertResponseBodyContains`, `assertXMLElementEquals`).

### Troubleshooting

#### Duplicate Action Error

If you define multiple actions with the same name and object type, INGenious will report a duplicate action error during plugin loading and the application will exit. Ensure that each action method within an object type has a unique name to avoid this issue.

Below is an example of the error you might encounter:

```
Duplicate action 'assertOddNumberDataSheet' for object type 'Numeric Assert' detected:
  - Original found in: text-assertion-plugin (class: com.ing.plugin2.Plugin2)
  - Duplicate found in: sample-plugin (class: com.ing.plugin.cloader.PluginCloader)
Duplicate action 'GetOccurence' for object type 'String Operations' detected:
  - Original found in: core (class: com.ing.engine.commands.stringOperations.StringOperations)
  - Duplicate found in: sample-plugin (class: com.ing.plugin.cloader.PluginCloader)
Duplicate method names detected in the loaded actions. Please resolve the conflicts.
```

#### ClassCastException Error

If you encounter a `ClassCastException` when casting Playwright objects, check the following:

1. **Dependency Scope**: Ensure Playwright dependency uses `<scope>provided</scope>` in your pom.xml
2. **Correct Type**: Verify you're casting to the correct Playwright type
3. **Null Check**: Always check if the object is null before casting

```java
// ✅ Correct pattern
Page page = (Page) getPage();
if (page != null) {
    page.navigate("https://example.com");
}

// ❌ Wrong - missing null check
Page page = (Page) getPage();
page.navigate("https://example.com"); // NullPointerException if null
```

#### Java Version Error (UnsupportedClassVersionError)

If you see an error like:

```
java.lang.UnsupportedClassVersionError: 
com/example/plugin/MyAction has been compiled by a more recent version of the Java Runtime
```

**Cause**: Your plugin was compiled with a newer Java version than the framework supports.

**Solution**: Update your `pom.xml` to use Java 17 or lower:

```xml
<properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

Then rebuild your plugin with `mvn clean install package`.

#### NoSuchMethodError

If you encounter `NoSuchMethodError` at runtime:

```
java.lang.NoSuchMethodError: com.microsoft.playwright.Page.someNewMethod()
```

**Cause**: Your plugin is trying to use a Playwright API method that doesn't exist in the framework's Playwright version.

**Solution**: 
1. Check the framework's Playwright version (currently 1.50.0)
2. Update your plugin's Playwright dependency to match:

```xml
<dependency>
    <groupId>com.microsoft.playwright</groupId>
    <artifactId>playwright</artifactId>
    <version>1.50.0</version>
    <scope>provided</scope>
</dependency>
```

3. Use only APIs available in Playwright 1.50.0

#### NoClassDefFoundError for Playwright Classes

If you see:

```
java.lang.NoClassDefFoundError: com/microsoft/playwright/Page
```

**Cause**: Missing Playwright dependency in your plugin's `pom.xml`.

**Solution**: Add Playwright dependency with `provided` scope (see Step 1 of [How to Create Your Plugin](#how-to-create-your-plugin)).

---

## Plugin Dependencies

### Why `provided` Scope is Critical

The `<scope>provided</scope>` for `ingenious-api` and `playwright` is essential:

- **Prevents ClassCastException**: Ensures Playwright classes are loaded from the parent classloader, not duplicated in your plugin
- **Smaller JAR Size**: Plugin JAR remains ~10KB instead of ~10MB with bundled Playwright  
- **Version Consistency**: Framework controls Playwright version, ensuring compatibility

### Optional Plugin Dependencies

Add plugin-specific dependencies with `compile` scope - these will be packaged in your `lib` folder:

```xml
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
    <scope>compile</scope>
</dependency>
```

### Dependency Isolation

Each plugin runs in its own isolated classloader:
- Different plugins can use different versions of the same library without conflicts
- Plugin dependencies don't affect the core framework or other plugins
- If your plugin has no `compile` dependencies, your `lib` folder may be empty (which is fine)

---

## Version Compatibility

### Framework Versions

The INGenious Framework currently runs on:
- **Java Version**: Java 17
- **Playwright Version**: 1.50.0

### Plugin Compatibility Requirements

| Component | Requirement | Why |
|-----------|-------------|-----|
| **Java Compiler** | Java 17 or lower | Framework JVM runs Java 17; cannot load bytecode from newer Java versions |
| **Playwright Version** | 1.50.0 (same as framework) | Must use same version to avoid `NoSuchMethodError` |
| **API Version** | Match framework's API version | Ensures interface compatibility |

### Compatibility Matrix

| Plugin Java | Plugin Playwright | Result | Notes |
|-------------|------------------|--------|-------|
| Java 17 | 1.50.0 | ✅ **Recommended** | Perfect match with framework |
| Java 11 | 1.50.0 | ✅ Works | Limited to Java 11 features |
| Java 21 | 1.50.0 | ❌ **UnsupportedClassVersionError** | Cannot load on Java 17 JVM |
| Java 17 | 1.55.0 | ❌ **NoSuchMethodError** | Newer APIs not available in framework |
| Java 17 | 1.40.0 | ✅ Works | Backward compatible |

### Common Configuration Mistakes

| Mistake | Symptom | Fix |
|---------|---------|-----|
| Plugin compiled with Java 21 | `UnsupportedClassVersionError` | Change `maven.compiler.target` to `17` |
| API scope is `compile` | `ClassCastException` or conflicts | Change to `<scope>provided</scope>` |
| Playwright scope is `compile` | Large JAR (~10MB), potential conflicts | Change to `<scope>provided</scope>` |
| Playwright version is 1.55 | `NoSuchMethodError` at runtime | Change to framework version `1.50.0` |
| No Playwright dependency | `NoClassDefFoundError` | Add Playwright with `provided` scope |

---

## Complete Plugin Template

Here is a complete, working `pom.xml` template for creating a plugin:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.example</groupId>
    <artifactId>my-plugin</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <name>My INGenious Plugin</name>
    
    <properties>
        <!-- REQUIRED: Match framework Java version -->
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    
    <dependencies>
        <!-- REQUIRED: API with provided scope -->
        <dependency>
            <groupId>com.ing.ingenious</groupId>
            <artifactId>ingenious-api</artifactId>
            <version>1.0.0</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- REQUIRED: Playwright with provided scope -->
        <dependency>
            <groupId>com.microsoft.playwright</groupId>
            <artifactId>playwright</artifactId>
            <version>1.50.0</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- Optional: Add your plugin-specific dependencies with compile scope -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.10.1</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <!-- Package plugin-specific dependencies (only needed if you have compile-scoped dependencies) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <version>3.6.0</version>
                <executions>
                    <execution>
                        <id>copy-dependencies</id>
                        <phase>package</phase>
                        <goals>
                            <goal>copy-dependencies</goal>
                        </goals>
                        <configuration>
                            <outputDirectory>${project.build.directory}/lib</outputDirectory>
                            <includeScope>compile</includeScope>
                            <excludeTransitive>true</excludeTransitive>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            
            <!-- Configure JAR manifest with entry classes -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <archive>
                        <manifestEntries>
                            <pluginEntryClasses>
                                com.ing.plugin.browser.BrowserTestPlugin
                            </pluginEntryClasses>
                            <Implementation-Version>${project.version}</Implementation-Version>
                        </manifestEntries>
                    </archive>
                </configuration>
            </plugin>
            
            <!-- Optional: Auto-deploy to plugin folder -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-antrun-plugin</artifactId>
                <version>3.1.0</version>
                <executions>
                    <execution>
                        <id>copy-artifacts</id>
                        <phase>package</phase>
                        <configuration>
                            <target>
                                <!-- Update this path to your INGenious plugins directory -->
                                <property name="deploy.dir" 
                                          value="/path/to/INGenious/plugins/my-plugin"/>
                                <copy file="${project.build.directory}/${project.build.finalName}.jar"
                                      tofile="${deploy.dir}/${project.artifactId}.jar"/>
                                <copy todir="${deploy.dir}/lib">
                                    <fileset dir="${project.build.directory}/lib"/>
                                </copy>
                            </target>
                        </configuration>
                        <goals>
                            <goal>run</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### Example Plugin Action Class

See the complete **BrowserTestPlugin** example in [Step 5: Implement Entry Classes](#5-implement-entry-classes) for a full, working plugin implementation.

That example demonstrates:
- ✅ Proper constructor with `GeneralBrApi` parameter
- ✅ Casting Playwright objects once in the constructor
- ✅ Using typed fields for full IDE support
- ✅ Real-world error handling with TimeoutError
- ✅ Proper use of NavigateOptions for configuration
- ✅ Integration with the reporting API

**Find the source code**: Working plugin examples are available in the `P33148-INGenious-Playwright-Framework-Plugins` repository under the `browser-test-plugin` directory.

### Quick Start Checklist

- [ ] Java 17 configured in `pom.xml`
- [ ] `ingenious-api` dependency with `provided` scope
- [ ] `playwright` dependency (version 1.50.0) with `provided` scope
- [ ] Plugin entry classes listed in JAR manifest
- [ ] Maven Dependency Plugin configured (only if you have additional compile-scoped dependencies)
- [ ] Action methods annotated with `@Action`
- [ ] Playwright objects cast from `Object` with null checks
- [ ] Built with `mvn clean install package`
- [ ] JAR and `lib` folder deployed to plugins directory

---

**For more information:**
- **Sample Plugins**: See `browser-test-plugin` and `sample-plugin` in the `P33148-INGenious-Playwright-Framework-Plugins` repository for complete working examples
- **API Documentation**: Check the `ingenious-api` module for interface contracts and annotations
- **Architecture Details**: Refer to the architecture design document for classloader architecture and version compatibility