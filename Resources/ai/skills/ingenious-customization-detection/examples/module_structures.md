# Module Structures: Source Code vs Build Copy

## Overview

INGenious installations come in two types: **Source Code Copy** and **Build Copy**. Understanding the difference is essential for customization detection.

---

## Source Code Copy

### Characteristics
- Contains multiple Maven modules
- Full source code with all modules
- Parent POM at root level
- Used for development and contribution

### Module List
- `Datalib/` - Data library module
- `Common/` - Common utilities module
- `Engine/` - Core engine module
- `IDE/` - IDE integration module
- `ingenious-api/` - API module
- `StoryWriter/` - Story writer module
- `TestData - Csv/` - CSV test data module

### Directory Structure

```
INGenious/
├── pom.xml                      # Parent POM
├── Common/
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   └── java/
│   │   └── test/
│   │       └── java/
│   └── target/
├── Datalib/
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   │       └── java/
│   └── target/
├── Engine/
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   │       └── java/
│   └── target/
├── IDE/
│   ├── pom.xml
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       └── resources/
│   └── target/
├── ingenious-api/
│   ├── pom.xml
│   └── src/
├── StoryWriter/
│   ├── pom.xml
│   └── src/
└── TestData - Csv/
    ├── pom.xml
    └── src/
```

### Detection Command

```bash
./scripts/detect_copy_type.sh /path/to/ingenious
# Output: SOURCE_CODE_COPY
# Modules found: Datalib Common IDE StoryWriter ingenious-api
```

---

## Build Copy

### Characteristics
- Only Engine module with source code
- Includes runtime resources (Configuration/, Projects/, lib/)
- Ready-to-run installation
- Used for test automation execution

### Key Directories
- `Configuration/` - Framework configuration files
- `Engine/` - Engine source code (only module)
- `lib/` - Runtime libraries
- `Projects/` - Test projects
- `plugins/` - Plugin directory
- `web/` - Dashboard resources

### Directory Structure

```
INGenious/
├── Configuration/
│   ├── conf.js
│   ├── XPLOR_SETTINGS.json
│   ├── Global Settings.Properties
│   ├── package.properties
│   └── ReportTemplate/
├── Engine/
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   │       └── java/
│   └── target/
├── lib/
│   └── *.jar
├── plugins/
│   └── [plugin folders]
├── Projects/
│   ├── ING-Public-Web/
│   ├── Mobile/
│   └── Tutorial/
├── Resources/
│   ├── Run.bat
│   └── Run.command
└── web/
    └── dashboard/
```

### Detection Command

```bash
./scripts/detect_copy_type.sh /path/to/ingenious
# Output: BUILD_COPY
# Only Engine module found
```

---

## Comparison Impact

### Customization Detection

**Source Code Copy:**
- Compare all modules against official release
- More comprehensive customization detection
- Longer comparison time
- Example:
  ```bash
  ./scripts/compare_modules.sh /user/path /official/path SOURCE_CODE_COPY
  # Compares: Common, Datalib, Engine, IDE, StoryWriter, etc.
  ```

**Build Copy:**
- Compare only Engine module
- Faster comparison
- Focus on execution-related customizations
- Example:
  ```bash
  ./scripts/compare_modules.sh /user/path /official/path BUILD_COPY
  # Compares: Engine only
  ```

### Typical Customizations

**Source Code Copy:**
- Framework architecture changes
- Core module modifications
- Build system changes
- IDE integration customizations

**Build Copy:**
- Engine command customizations
- Test execution enhancements
- Report template modifications
- Configuration changes

---

## Validation

Use the validation script to confirm installation type:

```bash
./scripts/validate_ingenious_root.sh /path/to/ingenious
```

Expected output for **Source Code Copy**:
```
✓ Found: Configuration/conf.js
✓ Found: Engine/pom.xml
✓ Found: Projects/
✓ Found: Common/pom.xml
✓ Found: Datalib/pom.xml

Total indicators found: 5 out of 6
✓ Valid INGenious installation detected
```

Expected output for **Build Copy**:
```
✓ Found: Configuration/conf.js
✓ Found: Engine/pom.xml
✓ Found: Projects/
✓ Found: Run.command

Total indicators found: 4 out of 6
✓ Valid INGenious installation detected
```
