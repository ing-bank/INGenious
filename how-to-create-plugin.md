## Table of Contents

- [INGenious Plugin system](#ingenious-plugin-system)
- [Plugin Directory Structure](#plugin-directory-structure)
- [How to Create Your Plugin](#how-to-create-your-plugin)
- [INGenious Object types and Actions](#ingenious-object-types-and-actions)
    - [Best Practice](#best-practice)
    - [Troubleshooting](#troubleshooting)
- [Plugin Dependencies](#plugin-dependencies)



---

## INGenious Plugin system
This guide explains how to create a custom plugin for the INGenious Playwright Framework. Plugins allow you to extend the platform with new automation actions.

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

1. **Set Up Your Maven Project**
    - Create a generic Maven Java project (no main class required).
    - In your `pom.xml`, add the following dependency to access the core engine APIs:
        ```xml
        <dependency>
            <groupId>com.ing</groupId>
            <artifactId>ingenious-engine</artifactId>
            <version>2.4</version>
            <type>jar</type>
        </dependency>
        ```
    - Add any additional dependencies your plugin requires.

2. **Configure Dependency Packaging**
    - To package your plugin dependencies (excluding `ingenious-engine`), add the following Maven plugin to the `<build>` section:
        ```xml
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-dependency-plugin</artifactId>
            <version>3.6.0</version>
            <executions>
                <execution>
                    <id>copy-direct-deps-except-engine</id>
                    <phase>package</phase>
                    <goals>
                        <goal>copy-dependencies</goal>
                    </goals>
                    <configuration>
                        <outputDirectory>${project.build.directory}/lib</outputDirectory>
                        <excludeArtifactIds>ingenious-engine</excludeArtifactIds>
                        <includeScope>compile</includeScope>
                        <excludeTransitive>true</excludeTransitive>
                    </configuration>
                </execution>
            </executions>
        </plugin>
        ```

3. **Declare Plugin Entry Classes**
    - Entry classes contain your action methods and are dynamically instantiated by INGenious. Specify them in the JAR manifest using the Maven JAR plugin:
        ```xml
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jar-plugin</artifactId>
            <version>3.3.0</version>
            <configuration>
                <archive>
                    <manifestEntries>
                        <pluginEntryClasses>
                            com.ing.plugin2.DatabasePlugin,com.ing.plugin2.Plugin2
                        </pluginEntryClasses>
                        <Implementation-Version>${project.version}</Implementation-Version>
                    </manifestEntries>
                </archive>
            </configuration>
        </plugin>
        ```
    - List fully qualified class names, separated by commas.

4. **Automate Deployment (Optional)**
    - To automatically copy your JAR and dependencies to the plugin directory, use the Maven Antrun plugin. Update `deploy.dir` to your target plugin folder:
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
                            <property name="deploy.dir" value="/path/to/INGenious/plugins/plugin2"/>
                            <copy file="${project.build.directory}/${project.build.finalName}.jar"
                                  tofile="${deploy.dir}/plugin-2.jar"/>
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

5. **Implement Entry Classes**
    - Create your java class and extend `com.ing.engine.commands.browser.General` (or the appropriate General class).
    - Annotate your action methods with `@Action`.

6. **Build and Deploy**
    - Run `mvn clean install package`.
    - If you configured automated deployment, your plugin JAR and `lib` folder will be copied to the INGenious plugin directory. Otherwise, copy them manually.

7. **Use Your Plugin**
    - Launch INGenious Playwright Studio. Your plugin actions will appear in the suggested actions list under the relevant Object type.

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
***Duplicate Action*** 

If you define multiple actions with the same name and object type, INGenious will report a duplicate action error during plugin loading and the application will exit. Ensure that each action method within an object type has a unique name to avoid this issue.

Below is an example of the error you might encounter.
```
Duplicate action 'assertOddNumberDataSheet' for object type 'Numeric Assert' detected:
  - Original found in: text-assertion-plugin (class: com.ing.plugin2.Plugin2)
  - Duplicate found in: sample-plugin (class: com.ing.plugin.cloader.PluginCloader)
Duplicate action 'GetOccurence' for object type 'String Operations' detected:
  - Original found in: core (class: com.ing.engine.commands.stringOperations.StringOperations)
  - Duplicate found in: sample-plugin (class: com.ing.plugin.cloader.PluginCloader)
Duplicate action 'print' for object type 'General' detected:
  - Original found in: core (class: com.ing.engine.commands.general.GeneralOperations)
  - Duplicate found in: sample-plugin (class: com.ing.plugin.cloader.PluginCloader)
Duplicate method names detected in the loaded actions. Please resolve the conflicts.
```


## Plugin Dependencies

Plugins can include their own dependencies and use different versions of libraries as needed. The only mandatory dependency for all plugins is `ingenious-engine`, which provides access to the core INGenious APIs and must be declared in your `pom.xml` (see How to Create Your Plugin, Step 1).

**Note:** INGenious uses a custom classloader for each plugin and for the main application. This design isolates dependencies, so plugins can safely use different versions of libraries without causing conflicts with the core application or other plugins.

**Best Practices:**
- Use the Maven Dependency Plugin to package your plugin’s dependencies separately from the core engine (see step 2 for configuration).
- Avoid including `ingenious-engine` in your plugin’s `lib` folder to prevent conflicts with the main application.
- Ensure that all plugin dependencies are included in the plugin’s lib folder, as INGenious does not rebuild your plugin.

**Example:**
```xml
<dependency>
    <groupId>com.ing</groupId>
    <artifactId>ingenious-engine</artifactId>
    <version>2.4</version>
    <type>jar</type>
</dependency>
```