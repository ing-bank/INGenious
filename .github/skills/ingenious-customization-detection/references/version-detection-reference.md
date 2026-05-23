# Version Detection Reference

## Overview

INGenious version can be detected from multiple sources within an installation. This document details all detection strategies.

## Detection Strategies

### Strategy 1: POM.xml Files

**Primary source** for Maven-based installations.

**Location:** `Engine/pom.xml`

**Pattern:**
```xml
<version>2.3.0</version>
```

**Command:**
```bash
grep -m 1 "<version>" Engine/pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/'
```

---

### Strategy 2: JAR Manifest Files

**Source:** Build artifacts in target directories.

**Location:** `Engine/target/*.jar`

**Pattern:**
```
Implementation-Version: 2.3.0
```

**Command:**
```bash
unzip -p Engine/target/*.jar META-INF/MANIFEST.MF | grep Implementation-Version
```

---

### Strategy 3: Property Files

**Locations:**
- `Configuration/package.properties`
- `Configuration/Global Settings.Properties`
- `Configuration/*.properties`

**Pattern:**
```
VERSION=2.3.0
```

**Command:**
```bash
grep -r "VERSION" Configuration/*.properties | head -1
```

---

### Strategy 4: Git Tags

**Source:** Git repository metadata (if installation is git-enabled).

**Command:**
```bash
cd /path/to/ingenious && git describe --tags --always
```

**Note:** Requires `.git/` directory to exist.

---

### Strategy 5: Source Code Constants

**Location:** Java source files, typically in Engine module.

**Pattern:**
```java
public static final String VERSION = "2.3.0";
```

**Command:**
```bash
grep -r "VERSION\s*=\s*" Engine/src/ --include="*.java"
```

---

## Common Version Patterns

| Pattern | Example | Source |
|---------|---------|--------|
| `X.Y` | `2.3` | pom.xml, properties |
| `X.Y.Z` | `2.3.0` | pom.xml, manifest |
| `vX.Y` | `v2.3` | Git tags |
| `vX.Y.Z` | `v2.3.0` | Git tags |
| `release-X.Y` | `release-2.3` | Git tags |
| `Release-X.Y` | `Release-2.3` | Git tags (capitalized) |

## Priority Order

The detect-version.sh script checks sources in this order:

1. ✅ **pom.xml** (most reliable for source copies)
2. ✅ **JAR manifests** (most reliable for build copies)
3. ✅ **Property files** (configuration-based)
4. ✅ **Git tags** (if repository exists)
5. ✅ **Source constants** (fallback)

## Troubleshooting

### Version Not Found

**Symptoms:**
- Script returns "UNKNOWN"
- No version information in standard locations

**Solutions:**
1. Check if installation is complete (Engine module present)
2. Look for custom version files in Configuration/
3. Ask user directly which version they have
4. Check release notes or documentation in installation

### Multiple Versions Found

**Symptoms:**
- Different versions in different files
- Parent POM version differs from module version

**Solution:**
- Use Engine/pom.xml version as authoritative
- Document discrepancy in report

### Version Format Inconsistency

**Symptoms:**
- Version has unusual format (e.g., "2.3-SNAPSHOT")
- Version includes build metadata (e.g., "2.3.0-b123")

**Solution:**
- Extract core version number (2.3)
- Note special flags in report (snapshot, build)
