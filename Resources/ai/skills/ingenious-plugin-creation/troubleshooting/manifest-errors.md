# Invalid Manifest Format

## Symptom
```
invalid manifest format (line 12)
```

## Cause
The `pluginEntryClasses` manifest entry is formatted across multiple lines, which violates JAR manifest format requirements.

## Solution

Keep entry classes on a single line in your `pom.xml`:

### ✅ Correct - Single Line

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <version>3.3.0</version>
    <configuration>
        <archive>
            <manifestEntries>
                <pluginEntryClasses>com.ing.plugin.A,com.ing.plugin.B,com.ing.plugin.C</pluginEntryClasses>
                <Implementation-Version>${project.version}</Implementation-Version>
            </manifestEntries>
        </archive>
    </configuration>
</plugin>
```

### ❌ Wrong - Multi-line

```xml
<manifestEntries>
    <pluginEntryClasses>
        com.ing.plugin.A,
        com.ing.plugin.B,
        com.ing.plugin.C
    </pluginEntryClasses>
</manifestEntries>
```

## Multiple Entry Classes

For multiple classes, use comma-separated values on one line:

```xml
<pluginEntryClasses>com.example.BrowserPlugin,com.example.DatabasePlugin,com.example.UtilityPlugin</pluginEntryClasses>
```

## Verification

After fixing, check the generated manifest:

```bash
# Build plugin
mvn clean package

# Extract and view manifest
unzip -p target/my-plugin.jar META-INF/MANIFEST.MF
```

Look for:
```
pluginEntryClasses: com.ing.plugin.A,com.ing.plugin.B
```

All entry classes should be on the same line (may wrap at 72 characters with continuation).

## Alternative Format

If you have many classes, consider creating a properties file or using concatenation:

```xml
<properties>
    <plugin.entries>com.ing.plugin.A,com.ing.plugin.B,com.ing.plugin.C</plugin.entries>
</properties>

<manifestEntries>
    <pluginEntryClasses>${plugin.entries}</pluginEntryClasses>
</manifestEntries>
```
