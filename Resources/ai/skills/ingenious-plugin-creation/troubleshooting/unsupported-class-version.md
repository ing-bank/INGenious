# UnsupportedClassVersionError

## Symptom
```
java.lang.UnsupportedClassVersionError: has been compiled by a more recent version
```

## Cause
Your plugin was compiled with a newer Java version than the framework supports (Java 17).

## Solution

Update your `pom.xml` to use Java 17 or lower:

```xml
<properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

Then rebuild your plugin:
```bash
mvn clean install package
```

## Java Version Compatibility

| Plugin Compiled With | Framework Runs On | Result |
|---------------------|-------------------|--------|
| Java 17 | Java 17 | ✅ Works |
| Java 11 | Java 17 | ✅ Works |
| Java 21 | Java 17 | ❌ UnsupportedClassVersionError |

## Verification

Check your Maven compiler version:
```bash
mvn -version
```

Ensure it's Java 17 or configure Maven to use Java 17 explicitly in `pom.xml`.
