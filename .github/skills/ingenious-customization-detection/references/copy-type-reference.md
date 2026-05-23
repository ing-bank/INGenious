# Copy Type Reference

## Overview

INGenious can be distributed in two formats: **source code copy** (for development) or **build copy** (for runtime). This document explains how to distinguish between them.

## Copy Types

### Source Code Copy

**Purpose:** Development, building from source, framework customization

**Characteristics:**
- Full Maven multi-module project
- Contains multiple module directories with source code
- Has parent POM at root
- Typically cloned from Git repository

**Module Structure:**
```
INGenious/
├── pom.xml                    # Parent POM
├── Common/
│   ├── pom.xml
│   └── src/main/java/...
├── Datalib/
│   ├── pom.xml
│   └── src/main/java/...
├── Engine/
│   ├── pom.xml
│   └── src/main/java/...
├── IDE/
│   ├── pom.xml
│   └── src/main/java/...
├── ingenious-api/
│   ├── pom.xml
│   └── src/main/java/...
├── StoryWriter/
│   ├── pom.xml
│   └── src/main/java/...
└── TestData - Csv/
    └── ...
```

**Indicators:**
- Multiple directories: Datalib/, Common/, IDE/, StoryWriter/, ingenious-api/
- Each has `src/main/java/` structure
- Each has individual `pom.xml`
- Root has parent `pom.xml` with `<modules>` section

---

### Build Copy

**Purpose:** Runtime execution, test automation

**Characteristics:**
- Pre-built distribution
- Contains compiled JARs
- Only Engine source code (for plugin development)
- Ready to run without building

**Module Structure:**
```
INGenious/
├── Configuration/             # Runtime configuration
│   ├── conf.js
│   ├── XPLOR_SETTINGS.json
│   └── ...
├── Engine/
│   ├── pom.xml               # For plugin compatibility
│   └── src/main/java/...     # Source for reference
├── lib/                      # Compiled JARs
│   ├── ingenious-engine-2.3.jar
│   ├── ingenious-datalib-2.3.jar
│   └── ...
├── Projects/                 # Test projects
├── Tools/                    # Utilities
├── web/                      # Web dashboard
├── Run.bat                   # Windows launcher
└── Run.command               # Mac/Linux launcher
```

**Indicators:**
- Only Engine/ has source code
- `lib/` directory with compiled JARs
- `Run.bat` or `Run.command` launcher scripts
- No Datalib/, Common/, IDE/ source directories

---

## Detection Logic

### Algorithm

```bash
# Count source modules present
MODULES=("Datalib" "Common" "IDE" "StoryWriter" "ingenious-api")
count=0

for module in MODULES:
    if exists("$module/src"):
        count++

if count >= 2:
    type = "SOURCE_CODE_COPY"
else if exists("Engine/src") and count == 0:
    type = "BUILD_COPY"
else:
    type = "UNKNOWN"
```

### Minimum Criteria

**Source Code Copy:**
- At least 2 module directories with `src/` subdirectories
- Typically: Datalib/, Common/, Engine/

**Build Copy:**
- Engine/src/ exists
- No other module source directories
- lib/ directory with JARs

---

## Why It Matters

### For Customization Detection

**Source Code Copy:**
- Compare **all modules** (Common, Datalib, Engine, IDE, etc.)
- More comprehensive customization detection
- Changes may span multiple modules

**Build Copy:**
- Compare **Engine module only**
- Focused customization detection
- Changes typically limited to Engine/

### For Plugin Extraction

**Source Code Copy:**
- Users may have customized multiple modules
- Plugin candidates can come from any module
- Need to check all modules for action methods

**Build Copy:**
- Customizations typically in Engine/
- Focus plugin detection on Engine/src/
- Limited scope simplifies analysis

---

## Edge Cases

### Partial Source Copy

**Scenario:** Some modules have source, others don't

**Example:**
```
INGenious/
├── Engine/src/...     ✓
├── Datalib/src/...    ✓
└── Common/            ✗ (no src/)
```

**Classification:** SOURCE_CODE_COPY (has at least 2 modules)

---

### Modified Build Copy

**Scenario:** Build copy where user added source modules

**Example:**
```
INGenious/
├── Engine/src/...
├── lib/...
├── MyCustomModule/src/...
```

**Classification:** UNKNOWN (requires manual review)

**Recommendation:** Ask user to clarify installation type

---

### Incomplete Installation

**Scenario:** Missing critical directories

**Example:**
```
INGenious/
├── Engine/          (no src/)
└── lib/
```

**Classification:** UNKNOWN or INVALID

**Action:** Validate installation integrity before proceeding

---

## Module Descriptions

| Module | Purpose | Present In |
|--------|---------|------------|
| **Engine** | Core test execution engine | Both types |
| **Datalib** | Data management and Excel handling | Source only |
| **Common** | Shared utilities and helpers | Source only |
| **IDE** | IDE integration components | Source only |
| **StoryWriter** | BDD story creation tools | Source only |
| **ingenious-api** | Public API interfaces | Source only |
| **TestData - Csv** | CSV test data support | Source only |

---

## Verification Commands

### Check for Source Code Copy

```bash
# Count source modules
count=0
for module in Datalib Common IDE StoryWriter ingenious-api; do
    [ -d "$module/src" ] && ((count++))
done

[ $count -ge 2 ] && echo "SOURCE_CODE_COPY"
```

### Check for Build Copy

```bash
# Check build copy indicators
[ -d "Engine/src" ] && \
[ -d "lib" ] && \
[ ! -d "Datalib/src" ] && \
echo "BUILD_COPY"
```

### List All Modules

```bash
# Find all directories with pom.xml
find . -maxdepth 2 -name "pom.xml" -exec dirname {} \;
```
