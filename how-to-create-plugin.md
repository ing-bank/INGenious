
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
        <groupId>com.ing</groupId>
        <artifactId>ingenious-api</artifactId>
        <version>3.0</version>
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


### Java Version Compatibility Matrix

| Plugin Java Version | Main App Java Version | API Java Version | Result | Notes |
|---------------------|----------------------|------------------|--------|-------|
| 17                  | 17                   | 17               | ✅ **Recommended** | All components match; full compatibility |
| 11                  | 17                   | 17               | ✅ Works | Plugin limited to Java 11 features; runs on Java 17 JVM |
| 21                  | 17                   | 17               | ❌ **UnsupportedClassVersionError** | Plugin compiled with newer Java; cannot load on Java 17 JVM |
| 17                  | 11                   | 11               | ❌ Not Supported | Main app/API compiled with older Java; cannot load Java 17 bytecode |
| 8                   | 17                   | 17               | ✅ Works | Plugin limited to Java 8 features; runs on Java 17 JVM |

### Playwright Version Compatibility Matrix

| Plugin Playwright Version | Main App Playwright Version | Result | Notes |
|--------------------------|-----------------------------|--------|-------|
| 1.50.0                   | 1.50.0                      | ✅ **Recommended** | Versions match; full compatibility |
| 1.55.0                   | 1.50.0                      | ❌ **NoSuchMethodError** | Plugin may compile and reference newer APIs, but any call to a new API on a Playwright object from the main app will fail at runtime |
| 1.40.0                   | 1.50.0                      | ✅ Works | Plugin uses only older APIs, which are present in the main app |
| 1.50.0                   | 1.55.0                      | ✅ Works | Main app provides newer APIs; plugin uses only 1.50.0 APIs |
| 1.40.0                   | 1.40.0                      | ✅ Works | Both use older version; limited to 1.40.0 features |

> **Important Note:**
> During development, your IDE may show and allow you to use new Playwright APIs if your plugin's dependency is set to a newer version. However, at runtime, only the Playwright version loaded by the main app is actually present. If your plugin tries to use a newer API that does not exist in the main app’s Playwright version (even if the code compiles), it will result in runtime errors such as `NoSuchMethodError`.
>
> **Always set your plugin's Playwright dependency version to exactly match the main app, and use `<scope>provided</scope>`. This ensures that what you see in development matches what will work at runtime, and prevents subtle, hard-to-debug errors.**

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

### POM.xml Template

Here is a comprehensive `pom.xml` template for creating plugins:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.ing.plugin</groupId>
    <artifactId>my-custom-plugin</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <name>My INGenious Custom Plugin</name>
    <description>Custom plugin for INGenious Playwright Framework</description>
    
    <properties>
        <!-- CRITICAL: Must match framework's Java version (17) -->
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- Version properties for easier maintenance -->
        <ingenious.api.version>3.0</ingenious.api.version>
        <playwright.version>1.50.0</playwright.version>
    </properties>
    
    <dependencies>
        <!-- ============================================= -->
        <!-- REQUIRED: Framework API (provided scope)     -->
        <!-- ============================================= -->
        <dependency>
            <groupId>com.ing</groupId>
            <artifactId>ingenious-api</artifactId>
            <version>${ingenious.api.version}</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- ============================================= -->
        <!-- REQUIRED FOR BROWSER/MOBILE PLUGINS          -->
        <!-- Playwright with provided scope               -->
        <!-- ============================================= -->
        <dependency>
            <groupId>com.microsoft.playwright</groupId>
            <artifactId>playwright</artifactId>
            <version>${playwright.version}</version>
            <scope>provided</scope>
        </dependency>
        
        <!-- ============================================= -->
        <!-- OPTIONAL: Plugin-specific dependencies       -->
        <!-- Use compile scope - will be packaged in lib  -->
        <!-- ============================================= -->
        
        <!-- Example: Apache Commons for utility functions -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>3.12.0</version>
            <scope>compile</scope>
        </dependency>
        
        <!-- Example: JSON processing -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.10.1</version>
            <scope>compile</scope>
        </dependency>
        
        <!-- Example: JSONPath for webservice plugins -->
        <dependency>
            <groupId>com.jayway.jsonpath</groupId>
            <artifactId>json-path</artifactId>
            <version>2.8.0</version>
            <scope>compile</scope>
        </dependency>
        
        <!-- Appium for mobile plugins (includes Selenium) -->
        <dependency>
            <groupId>io.appium</groupId>
            <artifactId>java-client</artifactId>
            <version>9.1.0</version>
            <scope>compile</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <!-- ============================================= -->
            <!-- Copy plugin dependencies to lib folder       -->
            <!-- Only compile-scoped dependencies are copied  -->
            <!-- ============================================= -->
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
                            <excludeTransitive>false</excludeTransitive>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            
            <!-- ============================================= -->
            <!-- Configure JAR manifest with entry classes    -->
            <!-- List ALL plugin entry classes here           -->
            <!-- ============================================= -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <archive>
                        <manifestEntries>
                            <!-- Comma-separated list of fully qualified class names -->
                            <pluginEntryClasses>com.ing.plugin.browser.BrowserTestPlugin,com.ing.plugin.database.DatabasePlugin,com.ing.plugin.mobile.MobileTestPlugin,com.ing.plugin.webservice.WebserviceTestPlugin</pluginEntryClasses>
                            <Implementation-Version>${project.version}</Implementation-Version>
                            <Implementation-Title>${project.name}</Implementation-Title>
                        </manifestEntries>
                    </archive>
                </configuration>
            </plugin>
            
            <!-- ============================================= -->
            <!-- Optional: Auto-deploy plugin to target dir   -->
            <!-- Update deploy.dir to your INGenious location -->
            <!-- ============================================= -->
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
                                <!-- ⚠️ UPDATE THIS PATH to your INGenious plugins directory -->
                                <property name="deploy.dir" 
                                          value="/path/to/INGenious/plugins/${project.artifactId}"/>
                                
                                <!-- Create plugin directory if it doesn't exist -->
                                <mkdir dir="${deploy.dir}"/>
                                <mkdir dir="${deploy.dir}/lib"/>
                                
                                <!-- Copy plugin JAR -->
                                <copy file="${project.build.directory}/${project.build.finalName}.jar"
                                      tofile="${deploy.dir}/${project.artifactId}.jar"
                                      overwrite="true"/>
                                
                                <!-- Copy dependencies to lib folder -->
                                <copy todir="${deploy.dir}/lib" overwrite="true">
                                    <fileset dir="${project.build.directory}/lib"/>
                                </copy>
                                
                                <echo message="Plugin deployed to: ${deploy.dir}"/>
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

---

### Plugin Class Templates

Below are complete, production-ready plugin templates for different use cases. Each demonstrates proper initialization, error handling, and reporting patterns.

#### 1. Browser Plugin Template

Use this template for browser automation actions with Playwright:

```java
package com.ing.plugin.browser;

import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.contract.BrowserPluginApi;
import com.ing.ingenious.api.contract.data.UserDataAccessApi;
import com.ing.ingenious.api.contract.reports.TestCaseReportApi;
import com.ing.ingenious.api.exception.ForcedException;
import com.ing.ingenious.api.types.ObjectType;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.status.Status;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.assertions.LocatorAssertions;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.opentest4j.AssertionFailedError;

/**
 * Browser automation plugin for Playwright-based actions
 * 
 * @author Your Name
 */
public class BrowserTestPlugin {
    
    BrowserPluginApi gen;
    
    public String Data;
    public String Action;
    public String Input;
    public String Condition;
    public TestCaseReportApi Report;
    public UserDataAccessApi userData;
    public String ObjectName;
    
    public Page Page;
    public Locator Locator;

    public BrowserTestPlugin(BrowserPluginApi gen) {
        System.out.println("BrowserTestPlugin initialized with BrowserPluginApi: " + gen);
        this.gen = gen;
        this.Data = gen.getData();
        this.Action = gen.getAction();
        this.Input = gen.getInput();
        this.Condition = gen.getCondition();
        this.Report = gen.getReport();
        this.userData = gen.getUserData();
        this.ObjectName = gen.getObjectName();
        this.Page = (Page) gen.getPage();
        this.Locator = (Locator) gen.getLocator();
    }

    /**
     * Navigate to URL with timeout support
     */
    @Action(object = ObjectType.BROWSER, desc = "Open the Url [<Data>] in the Browser", input = InputType.YES, condition = InputType.OPTIONAL)
    public void Open() {
        try {
            Page.NavigateOptions options = new Page.NavigateOptions();
            if (Condition != null && !Condition.isEmpty() && Condition.matches("[0-9]+")) {
                options.setTimeout(Double.parseDouble(Condition) * 1000);
            }
            Page.navigate(Data, options);
            Report.updateTestLog(Action, "Opened " + Data + " in the Browser", Status.DONE);
        } catch (TimeoutError e) {
            if (Condition != null && !Condition.isEmpty()) {
                Report.updateTestLog(Action, 
                    "Opened URL: " + Data + " and cancelled page load after " + Condition + " seconds", 
                    Status.DONE);
            } else {
                Report.updateTestLog(Action, "Page load timed out for URL: " + Data, Status.FAIL);
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, e.getMessage(), Status.FAIL);
            throw new ForcedException(Action, e.getMessage());
        }
    }

    /**
     * Assert element contains text with visual feedback
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Assert if [<Object>] contains the text [<Data>]", input = InputType.YES)
    public void assertElementContains() {
        String actualText = "";
        try {
            LocatorAssertions.ContainsTextOptions options = new LocatorAssertions.ContainsTextOptions();
            options.setTimeout(getTimeoutValue());
            actualText = Locator.innerHTML();
            highlightElement();
            assertThat(Locator).containsText(Data, options);
            Report.updateTestLog(Action, "Element [" + ObjectName + "] Contains text '" + Data + "'", Status.PASS);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (AssertionFailedError err) {
            assertionLogging(err, actualText);
        } finally {
            removeHighlightFromElement();
        }
    }

    /**
     * Click on element
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Click on [<Object>]")
    public void Click() {
        try {
            highlightElement();
            Locator.click();
            Report.updateTestLog(Action, "Clicked on '" + ObjectName + "'", Status.DONE);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } finally {
            removeHighlightFromElement();
        }
    }

    /**
     * Fill element with data
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Enter the value [<Data>] in [<Object>]", input = InputType.YES)
    public void Fill() {
        try {
            highlightElement();
            Locator.fill(Data);
            Report.updateTestLog(Action, "Entered '" + Data + "' in '" + ObjectName + "'", Status.DONE);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } finally {
            removeHighlightFromElement();
        }
    }

    /**
     * Store element text in variable
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Store [<Object>] element's text into variable [<Data>]", input = InputType.YES)
    public void storeElementTextinVariable() {
        try {
            String text = Locator.textContent();
            String variableName = Data;
            if (!variableName.matches("%.*%")) {
                Report.updateTestLog(Action, "Variable format incorrect. Expected: %variableName%", Status.FAIL);
                return;
            }
            gen.addVar(variableName, text);
            Report.updateTestLog(Action, "Stored text '" + text + "' in variable " + variableName, Status.DONE);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        }
    }

    /**
     * Store element text in data sheet
     */
    @Action(object = ObjectType.PLAYWRIGHT, desc = "Store [<Object>] element's text into data sheet [<Data>] under column [<Input>]", input = InputType.YES)
    public void storeElementTextinDataSheet() {
        try {
            String text = Locator.textContent();
            String sheetName = Data;
            String columnName = Input;
            userData.putData(sheetName, columnName, text);
            Report.updateTestLog(Action, "Stored text '" + text + "' in data sheet '" + sheetName + "' under column '" + columnName + "'", Status.DONE);
        } catch (PlaywrightException e) {
            PlaywrightExceptionLogging(e);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Error storing text in data sheet: " + e.getMessage(), Status.FAIL);
        }
    }

    // ========== Helper Methods ==========

    private void highlightElement() {
        Locator.scrollIntoViewIfNeeded();
        Locator.evaluate("element => element.style.outline = '2px solid red'");
    }

    private void removeHighlightFromElement() {
        Locator.evaluate("element => element.style.outline = ''");
    }

    private double getTimeoutValue() {
        double timeout = 5000;
        if (StringUtils.isNotBlank(Condition)) {
            try {
                timeout = Double.parseDouble(Condition) * 1000;
            } catch (NumberFormatException e) {
                // Use default timeout
            }
        }
        return timeout;
    }

    private void PlaywrightExceptionLogging(PlaywrightException e) {
        Report.updateTestLog(Action, "Element [" + ObjectName + "] not found. Error: " + e.getMessage(), Status.FAIL);
        Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
    }

    private void assertionLogging(AssertionFailedError err, String actualText) {
        if (err.getMessage().contains("locator resolved to")) {
            Report.updateTestLog(Action, "[" + ObjectName + "] does not contain text '" + Data + "'. Actual text is '" + actualText + "'", Status.FAIL);
        } else {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not found on Page", Status.FAIL);
        }
        Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, err);
    }
}
```

---

#### 2. Database Plugin Template

Use this template for database testing actions:

```java
package com.ing.plugin.database;

import com.ing.ingenious.api.contract.DatabasePluginApi;
import com.ing.ingenious.api.contract.data.UserDataAccessApi;
import com.ing.ingenious.api.contract.reports.TestCaseReportApi;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import com.ing.ingenious.api.annotation.Action;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Database testing plugin for SQL operations
 * 
 * @author Your Name
 */
public class DatabasePlugin {
    
    // API contract instance
    DatabasePluginApi gen;
    
    // Test data fields
    public String Data;
    public String Action;
    public String Input;
    public String Condition;
    public TestCaseReportApi Report;
    public UserDataAccessApi userData;
    public String ObjectName;

    /**
     * Constructor - receives DatabasePluginApi from framework
     */
    public DatabasePlugin(DatabasePluginApi gen) {
        this.gen = gen;
        this.Data = gen.getData();
        this.Action = gen.getAction();
        this.Input = gen.getInput();
        this.Condition = gen.getCondition();
        this.Report = gen.getReport();
        this.userData = gen.getUserData();
        this.ObjectName = gen.getObjectName();
    }

    /**
     * Execute DML query with variable substitution
     * Demonstrates processQuery() helper method
     */
    @Action(object = ObjectType.DATABASE, 
            desc = "Execute DML Query Example [<Data>]", 
            input = InputType.YES)
    public void executeDMLQueryExample() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "Query is empty or null", Status.FAIL);
                return;
            }
            
            // Process query for variable substitution using helper method
            String processedQuery = processQuery(Data);
            String originalData = this.Data;
            this.Data = processedQuery;
            
            // Execute DML through framework API
            gen.executeDML();
            
            // Restore original data
            this.Data = originalData;
            
            Report.updateTestLog(Action, 
                "DML Query executed successfully: " + processedQuery, 
                Status.PASS);
                
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, 
                "SQL Error: " + ex.getMessage(), 
                Status.FAIL);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, 
                "Unexpected error: " + ex.getMessage(), 
                Status.FAIL);
        }
    }

    /**
     * Execute SELECT and store column value from result
     * Demonstrates complex query handling with result extraction
     */
    @Action(object = ObjectType.DATABASE, 
            desc = "Execute Query Example [<Data>] and Store Column [<Input>] in [<Condition>]", 
            input = InputType.YES, 
            condition = InputType.YES)
    public void executeQueryAndStoreValueExample() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "Query is empty or null", Status.FAIL);
                return;
            }
            
            String columnName = Input;
            String variableName = Condition;
            
            if (!variableName.matches("%.*%")) {
                Report.updateTestLog(Action, 
                    "Variable format incorrect. Expected: %variableName%", 
                    Status.FAIL);
                return;
            }
            
            // Process and execute query using helper method
            String processedQuery = processQuery(Data);
            String originalData = this.Data;
            this.Data = processedQuery;
            
            gen.executeSelect();
            ResultSet rs = gen.getResult();
            
            if (rs != null && rs.next()) {
                String value = rs.getString(columnName);
                gen.addVar(variableName, value);
                
                Report.updateTestLog(Action, 
                    "Value [" + value + "] from column [" + columnName + 
                    "] stored in " + variableName, 
                    Status.PASS);
            } else {
                Report.updateTestLog(Action, "Query returned no results", Status.DONE);
            }
            
            this.Data = originalData;
            
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, 
                "SQL Error: " + ex.getMessage(), 
                Status.FAIL);
        }
    }

    // ========== Helper Methods ==========

    /**
     * Process query - handles variable substitution
     * Replaces %variableName% with actual values
     */
    private String processQuery(String query) {
        if (query == null) return null;
        
        // Find all variables in format %variableName%
        Pattern pattern = Pattern.compile("%([^%]+)%");
        Matcher matcher = pattern.matcher(query);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String variableName = "%" + matcher.group(1) + "%";
            String value = gen.getVar(variableName);
            
            if (value == null || value.equals(variableName)) {
                // Variable not found, keep original
                matcher.appendReplacement(result, 
                    Matcher.quoteReplacement(variableName));
            } else {
                // Replace with actual value
                matcher.appendReplacement(result, 
                    Matcher.quoteReplacement(value));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
}
```

---

#### 3. General Purpose Plugin Template

Use this template for general testing actions and utility operations:

```java
package com.ing.plugin.general;

import com.ing.ingenious.api.contract.CommandPluginApi;
import com.ing.ingenious.api.contract.reports.TestCaseReportApi;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import com.ing.ingenious.api.annotation.Action;

/**
 * General purpose plugin for text assertions and utility operations
 * 
 * @author Your Name
 */
public class TextAsserts {
    
    CommandPluginApi gen;
    public String Data;
    public String Action;
    public String Input;
    public TestCaseReportApi Report;

    public TextAsserts(CommandPluginApi gen) {
        System.out.println("TextAsserts Plugin initialized with CommandPluginApi: " + gen);
        this.gen = gen;
        this.Data = gen.getData();
        this.Action = gen.getAction();
        this.Input = gen.getInput();
        this.Report = gen.getReport();
    }

    /**
     * Assert text is in lowercase
     */
    @Action(object = "Text Assertions", desc = "Assert if input is in lower case", input = InputType.YES, condition = InputType.NO)
    public void assertTextInLowerCase() {
        System.out.println("Hello World! This is the assertTextInLowerCase action");
        String var = gen.getVar(Input);
        System.out.println("Input is " + var);
        if (var.equals(var.toLowerCase())) {
            Report.updateTestLog(Action, "The input " + Data + " is in lower case.", Status.PASSNS);
        } else {
            Report.updateTestLog(Action, "The input " + Data + " is not in lower case.", Status.FAILNS);
        }
    }

    /**
     * Assert text is in uppercase
     */
    @Action(object = "Text Assertions", desc = "Assert if input is in upper case", input = InputType.YES, condition = InputType.NO)
    public void assertTextInUpperCase() {
        System.out.println("Hello World! This is the assertTextInUpperCase action");
        String var = gen.getVar(Input);
        System.out.println("Input is " + var);
        if (var.equals(var.toUpperCase())) {
            Report.updateTestLog(Action, "The input " + Data + " is in upper case.", Status.PASSNS);
        } else {
            Report.updateTestLog(Action, "The input " + Data + " is not in upper case.", Status.FAILNS);
        }
    }
    
    /**
     * General action demonstrating ObjectType enum usage
     */
    @Action(object = ObjectType.GENERAL, desc = "General purpose action", input = InputType.YES, condition = InputType.NO)
    public void testObjectypeUsingEnum() {
        String var = gen.getVar(Input);
        System.out.println("This is stored in the variable: " + var);
        Report.updateTestLog(Action, "Status code is : " + Data, Status.DONE);
    }
}
```

---

#### 4. Mobile Plugin Template

Use this template for mobile testing with Appium:

```java
package com.ing.plugin.mobile;

import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.contract.MobilePluginApi;
import com.ing.ingenious.api.contract.data.UserDataAccessApi;
import com.ing.ingenious.api.contract.drivers.MobileObjectApi;
import com.ing.ingenious.api.contract.reports.TestCaseReportApi;
import com.ing.ingenious.api.types.ObjectType;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.exception.mobile.ElementException;
import com.ing.ingenious.api.exception.mobile.ElementException.ExceptionType;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.ios.IOSDriver;

/**
 * Mobile testing plugin for Appium-based mobile automation
 * 
 * @author Your Name
 */
public class MobileTestPlugin {
    
    MobilePluginApi gen;
    
    public String Data;
    public String Action;
    public String Input;
    public String Condition;
    public TestCaseReportApi Report;
    public UserDataAccessApi userData;
    public String ObjectName;
    
    public WebDriver mDriver;
    public WebElement Element;
    public MobileObjectApi mObject;

    public MobileTestPlugin(MobilePluginApi gen) {
        System.out.println("MobileTestPlugin initialized with MobilePluginApi: " + gen);
        this.gen = gen;
        this.Data = gen.getData();
        this.Action = gen.getAction();
        this.Input = gen.getInput();
        this.Condition = gen.getCondition();
        this.Report = gen.getReport();
        this.userData = gen.getUserData();
        this.ObjectName = gen.getObjectName();
        this.Element = (WebElement) gen.getElement();
        this.mDriver = (WebDriver) gen.getMDriver();
        this.mObject = gen.getMObject();
    }

    /**
     * Tap on mobile element
     */
    @Action(object = ObjectType.APP, desc = "Tap on [<Object>]")
    public void Tap() {
        if (gen.elementEnabled()) {
            Element.click();
            Report.updateTestLog(Action, "Tapped on " + ObjectName, Status.DONE);
        } else {
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    /**
     * Set value in mobile element
     */
    @Action(object = ObjectType.APP, desc = "Enter the value [<Data>] in [<Object>]", input = InputType.YES)
    public void Set() {
        if (gen.elementEnabled()) {
            Element.sendKeys(Data);
            Report.updateTestLog(Action, "Entered '" + Data + "' in '" + ObjectName + "'", Status.DONE);
        } else {
            throw new ElementException(ExceptionType.Element_Not_Enabled, ObjectName);
        }
    }

    /**
     * Scroll to text in Android
     */
    @Action(object = ObjectType.MOBILE, desc = "Scroll to Text [<Data>] in Android", input = InputType.YES)
    public void scrollInAndroid() {
        try {
            mDriver.findElement(AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector().scrollable(true))" +
                ".scrollIntoView(new UiSelector().text(\"" + Data + "\").instance(0))"
            ));
            Report.updateTestLog(Action, "Scrolled to '" + Data + "'", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perform [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
        }
    }

    /**
     * Scroll to element in iOS
     */
    @Action(object = ObjectType.MOBILE, desc = "Scroll to Text [<Data>] in IOS", input = InputType.YES)
    public void scrollInIOS() {
        try {
            HashMap<String, String> scrollObject = new HashMap<>();
            scrollObject.put("direction", "down");
            scrollObject.put("name", Data);
            ((IOSDriver) mDriver).executeScript("mobile: scroll", scrollObject);
            Report.updateTestLog(Action, "Scrolled to '" + Data + "'", Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog("Could not perform [" + Action + "] action", "Error: " + e.getMessage(), Status.FAIL);
        }
    }

    /**
     * Assert element is displayed
     */
    @Action(object = ObjectType.APP, desc = "Assert if [<Object>] is displayed", condition = InputType.OPTIONAL)
    public void assertElementDisplayed() {
        try {
            if (Element.isDisplayed()) {
                Report.updateTestLog(Action, "Element [" + ObjectName + "] is displayed", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element [" + ObjectName + "] is not displayed", Status.FAILNS);
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not found. Error: " + e.getMessage(), Status.FAILNS);
        }
    }

    // ========== Helper Methods ==========

    public boolean elementPresent() {
        return gen.checkIfDriverIsAlive() && Element != null;
    }
}
```

---

#### 5. Webservice Plugin Template

Use this template for REST API and web service testing:

```java
package com.ing.plugin.webservice;

import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.contract.WebservicePluginApi;
import com.ing.ingenious.api.contract.data.UserDataAccessApi;
import com.ing.ingenious.api.contract.reports.TestCaseReportApi;
import com.ing.ingenious.api.types.ObjectType;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.RequestMethod;
import com.ing.ingenious.api.status.Status;

import com.jayway.jsonpath.JsonPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Webservice Test Plugin for REST API testing
 * 
 * @author Your Name
 */
public class WebserviceTestPlugin {

    WebservicePluginApi gen;

    public String Data;
    public String Action;
    public String Input;
    public String Condition;
    public TestCaseReportApi Report;
    public UserDataAccessApi userData;
    public String ObjectName;

    // Webservice-specific fields
    public String endpoint;
    public String responseCode;
    public String responseMessage;
    public String responseBody;
    public Object connection;
    public String httpAgent;

    // Shared storage - direct access to framework's static maps
    private String key;
    private Map<String, String> endPoints;
    private Map<String, ArrayList<String>> headers;
    private Map<String, ArrayList<String>> urlParams;
    private Map<String, String> responseBodies;
    private Map<String, String> responseCodes;
    private Map<String, String> responseMessages;

    public WebserviceTestPlugin(WebservicePluginApi gen) {
        System.out.println("WebserviceTestPlugin initialized with WebservicePluginApi: " + gen);
        this.gen = gen;
        this.Data = gen.getData();
        this.Action = gen.getAction();
        this.Input = gen.getInput();
        this.Condition = gen.getCondition();
        this.Report = gen.getReport();
        this.userData = gen.getUserData();
        this.ObjectName = gen.getObjectName();
        
        // Initialize webservice-specific fields from API
        this.endpoint = gen.Endpoint();
        this.responseCode = gen.ResponseCode();
        this.responseMessage = gen.ResponseMessage();
        this.responseBody = gen.ResponseBody();
        this.connection = gen.Connection();
        this.httpAgent = gen.HttpAgent();
        
        // Get references to shared maps from framework
        this.key = gen.getKey();
        this.endPoints = gen.getEndPointsMap();
        this.headers = gen.getHeadersMap();
        this.urlParams = gen.getUrlParamsMap();
        this.responseBodies = gen.getResponseBodiesMap();
        this.responseCodes = gen.getResponseCodesMap();
        this.responseMessages = gen.getResponseMessagesMap();
    }

    /**
     * Execute GET REST request
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "GET Rest Request ", input = InputType.NO, condition = InputType.OPTIONAL)
    public void getRestRequest() {
        try {
            gen.createHttpRequest(RequestMethod.GET);
            
            // Update local fields with response
            this.responseCode = gen.ResponseCode();
            this.responseBody = gen.ResponseBody();
            this.responseMessage = gen.ResponseMessage();
            
            /** 
             * Report here is optional. createHttpRequest already updates the report with request and response details. 
             * This is just an additional log entry if you want to explicitly log the successful execution of the GET request
             */
            // Report.updateTestLog(Action, "GET request executed successfully. Response code: " + responseCode, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the GET request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    /**
     * Store JSON element value in runtime variable
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Store JSON Element", input = InputType.YES, condition = InputType.YES)
    public void storeJSONelement() {
        try {
            String variableName = Condition;
            String jsonpath = Data;
            
            if (variableName.matches("%.*%")) {
                // Get the current response body
                String currentResponseBody = gen.ResponseBody();
                
                if (currentResponseBody != null && !currentResponseBody.isEmpty()) {
                    String value = JsonPath.read(currentResponseBody, jsonpath).toString();
                    gen.addVar(variableName, value);
                    Report.updateTestLog(Action, "JSON element value [" + value + "] stored in variable " + variableName, Status.DONE);
                } else {
                    Report.updateTestLog(Action, "Response body is empty or null", Status.DEBUG);
                }
            } else {
                Report.updateTestLog(Action, "Variable format is not correct. Expected format: %variableName%", Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Add HTTP header to request
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Add Header ", input = InputType.YES)
    public void addHeader() {
        try {
            String headerData = Data;

            // Handle datasheet variable substitution: {sheetName:columnName}
            Pattern datasheetPattern = Pattern.compile("\\{([^:]+):([^}]+)\\}");
            Matcher datasheetMatcher = datasheetPattern.matcher(headerData);
            
            while (datasheetMatcher.find()) {
                String sheetName = datasheetMatcher.group(1);
                String columnName = datasheetMatcher.group(2);
                try {
                    String value = userData.getData(sheetName, columnName);
                    if (value != null) {
                        headerData = headerData.replace("{" + sheetName + ":" + columnName + "}", value);
                    }
                } catch (Exception e) {
                    Report.updateTestLog(Action, "Could not find data for {" + sheetName + ":" + columnName + "}", Status.DEBUG);
                }
            }

            // Handle runtime variable substitution: %variable%
            Pattern variablePattern = Pattern.compile("%\\w+%");
            Matcher variableMatcher = variablePattern.matcher(headerData);

            while (variableMatcher.find()) {
                String variable = variableMatcher.group();
                String value = gen.getVar(variable);
                if (value != null) {
                    headerData = headerData.replaceAll(Pattern.quote(variable), value);
                } else {
                    Report.updateTestLog(Action, "Variable " + variable + " not found", Status.DEBUG);
                }
            }

            // Store the header in shared map
            if (headers.containsKey(key)) {
                headers.get(key).add(headerData);
            } else {
                ArrayList<String> toBeAdded = new ArrayList<>();
                toBeAdded.add(headerData);
                headers.put(key, toBeAdded);
            }
            
            Report.updateTestLog(Action, "Header added [" + headerData + "]", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error adding Header :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * POST REST request
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "POST Rest Request ", input = InputType.YES, condition = InputType.OPTIONAL)
    public void postRestRequest() {
        try {
            gen.createHttpRequest(RequestMethod.POST);
            
            // Update local fields with response
            this.responseCode = gen.ResponseCode();
            this.responseBody = gen.ResponseBody();
            this.responseMessage = gen.ResponseMessage();
            
            /** 
             * Report here is optional. createHttpRequest already updates the report with request and response details. 
             * This is just an additional log entry if you want to explicitly log the successful execution of the POST request
             */
            // Report.updateTestLog(Action, "POST request executed successfully. Response code: " + responseCode, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the POST request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    /**
     * PUT REST request
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "PUT Rest Request ", input = InputType.YES, condition = InputType.OPTIONAL)
    public void putRestRequest() {
        try {
            System.out.println("Executing PUT request with key: " + key);
            gen.createHttpRequest(RequestMethod.PUT);
            
            // Update local fields with response
            this.responseCode = gen.ResponseCode();
            this.responseBody = gen.ResponseBody();
            this.responseMessage = gen.ResponseMessage();
            
            /** 
             * Report here is optional. createHttpRequest already updates the report with request and response details. 
             * This is just an additional log entry if you want to explicitly log the successful execution of the PUT request
             */
            // Report.updateTestLog(Action, "PUT request executed successfully. Response code: " + responseCode, Status.DONE);
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, e);
            Report.updateTestLog(Action,
                    "An unexpected error occurred while executing the PUT request : " + "\n" + e.getMessage(),
                    Status.FAIL);
        }
    }

    /**
     * Assert response code matches expected value
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert Response Code ", input = InputType.YES)
    public void assertResponseCode() {
        try {
            String currentResponseCode = gen.ResponseCode();
            
            if (currentResponseCode != null && currentResponseCode.equals(Data)) {
                Report.updateTestLog(Action, "Status code is : " + Data, Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Status code is : " + currentResponseCode + " but should be " + Data,
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating response code :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Assert response body contains specific text
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert Response Body contains ", input = InputType.YES)
    public void assertResponseBodyContains() {
        try {
            String currentResponseBody = gen.ResponseBody();
            
            if (currentResponseBody != null && currentResponseBody.contains(Data)) {
                Report.updateTestLog(Action, "Response body contains : " + Data, Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Response body does not contain : " + Data, Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating response body :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Assert JSON element equals expected value
     */
    @Action(object = ObjectType.WEBSERVICE, desc = "Assert JSON Element Equals ", input = InputType.YES, condition = InputType.YES)
    public void assertJSONelementEquals() {
        try {
            String currentResponseBody = gen.ResponseBody();
            String jsonpath = Condition;
            String value = JsonPath.read(currentResponseBody, jsonpath).toString();
            
            if (value.equals(Data)) {
                Report.updateTestLog(Action, "Element text [" + value + "] is as expected", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Element text is [" + value + "] but is expected to be [" + Data + "]",
                        Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating JSON element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Get stored headers for current context
     */
    public List<String> getHeaders() {
        return headers.getOrDefault(key, new ArrayList<>());
    }
}
```

---

### Using the Templates

1. **Choose the appropriate template** based on your plugin type:
   - **BrowserTestPlugin**: For web browser automation
   - **DatabasePlugin**: For database testing
   - **TextAssertsPlugin**: For general purpose operations
   - **MobileTestPlugin**: For mobile app testing
   - **WebserviceTestPlugin**: For REST API and web service testing

2. **Customize the package name** to match your project structure

3. **Add your custom actions** following the patterns shown

4. **Update the POM.xml** with your plugin entry classes

5. **Build and deploy**: `mvn clean install package`

### Key Patterns Demonstrated

Each template includes **focused, production-ready examples** showcasing essential patterns:

**Browser Plugin (3 actions):**
- ✅ **Navigation** with timeout handling
- ✅ **Assertions** with visual feedback (highlight/unhighlight helpers)
- ✅ **Variable storage** for dynamic test data

**Database Plugin (2 actions):**
- ✅ **Query execution** with variable substitution helper (`processQuery()`)
- ✅ **Result extraction** and storage from SELECT queries

**General Purpose Plugin (2 actions):**
- ✅ **Validation** with variable retrieval
- ✅ **Data transformation** and storage

**Mobile Plugin (2 actions):**
- ✅ **Element interaction** with state checking
- ✅ **Platform-specific** implementation (Android scrolling)

**Webservice Plugin (3 actions):**
- ✅ **HTTP operations** with response handling
- ✅ **JSON parsing** using JSONPath
- ✅ **Multi-request context** with variable substitution helpers

**Common patterns across all templates:**
- **Proper initialization**: All fields populated in constructor
- **Error handling**: Try-catch with appropriate logging
- **Null checks**: Defensive programming prevents NPE
- **Helper methods**: DRY principle for reusable logic
- **Status reporting**: Clear PASS/FAIL/DONE feedback
- **Type safety**: Cast Playwright/Selenium objects once

**Total: 12 focused examples (reduced from 20+)** demonstrating all essential plugin patterns.  

**Find complete working examples** in the `P33148-INGenious-Playwright-Framework-Plugins` repository.

### Quick Start Checklist

Before deploying your plugin, verify:

**POM Configuration:**
- [ ] Java 17 (or lower) configured: `maven.compiler.source` and `target`
- [ ] `ingenious-api` dependency with `<scope>provided</scope>`
- [ ] `playwright` dependency (version 1.50.0) with `<scope>provided</scope>` (for browser/mobile plugins)
- [ ] Plugin entry classes listed in JAR manifest: `<pluginEntryClasses>`
- [ ] Maven Dependency Plugin configured (only if you have compile-scoped dependencies)
- [ ] Maven Antrun Plugin configured with correct `deploy.dir` path (optional)

**Plugin Class:**
- [ ] Constructor accepts appropriate API interface (`BrowserPluginApi`, `DatabasePluginApi`, etc.)
- [ ] All test data fields initialized in constructor
- [ ] Playwright/Selenium objects cast once in constructor (not in action methods)
- [ ] Action methods annotated with `@Action` including `object`, `desc`, and `input` parameters
- [ ] Proper null checks before using objects
- [ ] Error handling with try-catch blocks
- [ ] Status reporting using `Report.updateTestLog()`
- [ ] Variable format validation for storage actions (`%variableName%`)

**Build and Deploy:**
- [ ] Build successful: `mvn clean install package`
- [ ] JAR file generated in `target/` directory
- [ ] `lib/` folder contains compile-scoped dependencies (if any)
- [ ] Plugin deployed to: `<INGenious>/plugins/<your-plugin>/`
- [ ] Both JAR and `lib/` folder copied to plugin directory

**Testing:**
- [ ] Plugin loads without errors in INGenious Studio
- [ ] Actions appear in suggested actions list
- [ ] Actions execute successfully with test data
- [ ] Reports show correct status (PASS/FAIL/DONE)

---

### Additional Resources

**Sample Plugins:**
- **Browser Plugin**: `browser-test-plugin` in `P33148-INGenious-Playwright-Framework-Plugins`
- **Database Plugin**: `general-test-plugin` (DatabasePlugin class)
- **Mobile Plugin**: `mobile-test-plugin` in `P33148-INGenious-Playwright-Framework-Plugins`
- **Webservice Plugin**: `webservice-test-plugin` in `P33148-INGenious-Playwright-Framework-Plugins`
- **Text Assertions**: `general-test-plugin` (TextAsserts class)

**API Documentation:**
- Check the `ingenious-api` module for interface contracts and annotations
- Review JavaDoc in API classes for method documentation

**Architecture Details:**
- Refer to the architecture design document for classloader architecture
- See version compatibility matrix for Java and Playwright version requirements

**Need Help?**
- Review the [Troubleshooting](#troubleshooting) section for common issues
- Check [Common Configuration Mistakes](#common-configuration-mistakes) table
- Examine working plugin examples in the samples repository