# Scripts Directory

This directory contains executable scripts used by the ingenious-customization-detection skill.

## Scripts

### detect-version.sh
Detect INGenious version from multiple sources (pom.xml, manifests, properties, Git tags).

**Usage:**
```bash
./detect-version.sh /path/to/ingenious/root
```

**Output:** Version string (e.g., "2.3") or "UNKNOWN"

**Exit Codes:**
- 0: Version detected successfully
- 1: Version not found or error

---

### detect-copy-type.sh
Determine if installation is source code copy or build copy.

**Usage:**
```bash
./detect-copy-type.sh /path/to/ingenious/root
```

**Output:** `SOURCE_CODE_COPY`, `BUILD_COPY`, or `UNKNOWN`

**Exit Codes:**
- 0: Type detected successfully
- 1: Type unknown or error

---

### download-official.sh
Download official INGenious release from GitHub.

**Usage:**
```bash
./download-official.sh <version> <output-directory>
```

**Example:**
```bash
./download-official.sh 2.3 /tmp/ingenious-official
```

**Exit Codes:**
- 0: Successfully downloaded and checked out exact version
- 1: Git not available or other error
- 2: Version tag not found (fell back to main branch)

---

### compare-installations.sh
Compare user's installation against official release.

**Usage:**
```bash
./compare-installations.sh <user-path> <official-path> <copy-type> <output-dir>
```

**Example:**
```bash
./compare-installations.sh /path/to/user /tmp/official SOURCE_CODE_COPY ./diffs
```

**Output Files:**
- `diff_<module>.patch` - Detailed diff for each module
- `files_<module>.txt` - Quick summary of changed files

---

### generate-report.py
Generate customization report from diff files.

**Usage:**
```bash
./generate-report.py <diff-dir> <user-path> <version> <copy-type> <output-file>
```

**Example:**
```bash
./generate-report.py ./diffs /path/to/user 2.3 BUILD_COPY report.md
```

**Features:**
- Categorizes changes (Feature, Bug Fix, Configuration, etc.)
- Assesses impact (High, Medium, Low)
- Generates statistics and risk assessment

---

### identify-plugins.py
Identify plugin candidates from customizations.

**Usage:**
```bash
./identify-plugins.py <diff-dir> <user-path> <output-file>
```

**Example:**
```bash
./identify-plugins.py ./diffs /path/to/user plugins.md
```

**Output Files:**
- `plugins.md` - Human-readable plugin recommendations
- `plugins.json` - Machine-readable plugin specifications

---

### validate-installation.sh
Unified validation for INGenious installations.

**Usage:**
```bash
./validate-installation.sh --check <type> --path <path>
```

**Check Types:**
- `markers` - Verify INGenious indicators present
- `version` - Check if version can be detected
- `modules` - Validate module structure

**Example:**
```bash
./validate-installation.sh --check markers --path /path/to/ingenious
```

**Output:** `VALID` or `INVALID` with reason

---

## Making Scripts Executable

After cloning, make scripts executable:

```bash
chmod +x *.sh *.py
```

## Dependencies

**Required:**
- Bash 4.0+ (for .sh scripts)
- Python 3.6+ (for .py scripts)
- Git (for download-official.sh)
- diff/grep/sed (standard Unix tools)

**Optional:**
- unzip (for JAR manifest inspection)

## Testing Scripts

Test individual scripts:

```bash
# Test version detection
./detect-version.sh /path/to/test/installation

# Test validation
./validate-installation.sh --check markers --path /path/to/test
```
