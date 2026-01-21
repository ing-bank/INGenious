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

