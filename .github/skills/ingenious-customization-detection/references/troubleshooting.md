# Troubleshooting Guide

Common issues and solutions when using the ingenious-customization-detection skill.

## Installation Detection Issues

### Cannot Detect INGenious Installation

**Problem:** Script doesn't recognize directory as INGenious installation

**Symptoms:**
- "No INGenious markers found"
- Validation fails at Step 1

**Possible Causes:**
1. Incomplete installation (missing key directories)
2. Wrong directory selected
3. Non-standard installation structure

**Solutions:**

1. **Verify key directories exist:**
```bash
ls -la Configuration/ Engine/ Projects/
```

2. **Check for indicators manually:**
```bash
# These should exist for valid installation:
[ -d "Configuration" ] && echo "✓ Configuration"
[ -d "Engine" ] && echo "✓ Engine"  
[ -f "Run.bat" ] || [ -f "Run.command" ] && echo "✓ Launcher"
```

3. **Use absolute path:**
```bash
# Instead of: ./my-ingenious
# Use: /Users/username/full/path/to/my-ingenious
```

4. **Check permissions:**
```bash
ls -ld /path/to/ingenious
# Should be readable by current user
```

---

## Version Detection Issues

### Version Not Found

**Problem:** `detect-version.sh` returns "UNKNOWN"

**Symptoms:**
- No version in pom.xml
- No manifest files
- Git tags not available

**Possible Causes:**
1. Custom build without version information
2. Stripped/modified installation
3. Very old version (different structure)

**Solutions:**

1. **Manual search for version:**
```bash
# Search all files for version pattern
grep -r "2\.[0-9]" Configuration/ Engine/ | grep -i version
```

2. **Check release notes:**
```bash
cat README.md RELEASE_NOTES.txt
```

3. **Ask user directly:**
```
Which version of INGenious do you have?
```

4. **Use approximate version:**
```bash
# If you know it's "around 2.3", use that for comparison
./download-official.sh 2.3 /tmp/official
```

---

### Multiple Versions Found

**Problem:** Different version numbers in different files

**Symptoms:**
- pom.xml shows 2.3.0
- manifest shows 2.3.1
- Git tag shows v2.3

**Solution:**
- Use Engine/pom.xml as authoritative source
- Document version discrepancy in report
- Compare against closest official version

---

## Download Issues

### Git Clone Fails

**Problem:** Cannot download official repository

**Symptoms:**
- "fatal: unable to access 'https://github.com/...'"
- Network timeout
- SSL certificate errors

**Possible Causes:**
1. No internet connection
2. Firewall blocking GitHub
3. Proxy configuration needed
4. Git not installed

**Solutions:**

1. **Check internet connectivity:**
```bash
ping -c 3 github.com
```

2. **Verify Git installation:**
```bash
git --version
```

3. **Configure proxy (if behind corporate firewall):**
```bash
git config --global http.proxy http://proxy.company.com:8080
git config --global https.proxy https://proxy.company.com:8080
```

4. **Try manual download:**
```bash
# Download ZIP instead of git clone
curl -L https://github.com/ing-bank/INGenious/archive/refs/tags/v2.3.zip -o ingenious.zip
unzip ingenious.zip
```

---

### Version Tag Not Found

**Problem:** Git checkout fails - tag doesn't exist

**Symptoms:**
- "error: pathspec 'v2.3' did not match any file(s)"
- Available tags don't match version

**Possible Causes:**
1. Version number doesn't match tag format
2. Internal/custom version not in public repository
3. Typo in version

**Solutions:**

1. **List all available tags:**
```bash
cd official-ingenious
git tag -l | sort -V
```

2. **Search for similar tags:**
```bash
git tag -l | grep "2.3"
```

3. **Use closest match:**
```bash
# If you have 2.3.1 but only 2.3.0 and 2.4.0 exist
# Use 2.3.0 as baseline
git checkout v2.3.0
```

4. **Fall back to main branch:**
```bash
git checkout main
# Note: May have significant differences
```

---

## Comparison Issues

### Diff Files Too Large

**Problem:** Diff output is enormous (>100MB)

**Symptoms:**
- Comparison takes very long
- Diff files consume too much disk
- Cannot open diff in editor

**Possible Causes:**
1. Comparing different major versions
2. Many generated files included
3. Binary files being diffed

**Solutions:**

1. **Use summary mode:**
```bash
diff -qr official/ user/ > summary.txt
# Quick summary only, not full diff
```

2. **Add more exclusions:**
```bash
diff -Naur \
    --exclude='target' \
    --exclude='*.class' \
    --exclude='*.jar' \
    --exclude='generated' \
    official/ user/
```

3. **Compare modules individually:**
```bash
# Split into smaller chunks
diff -Naur official/Engine user/Engine > diff_Engine.patch
diff -Naur official/Datalib user/Datalib > diff_Datalib.patch
```

---

### Diff Shows Too Many Changes

**Problem:** Thousands of files differ

**Symptoms:**
- 1000+ files changed
- Nearly every file shows differences
- Clearly not all customizations

**Possible Causes:**
1. Comparing different major versions (e.g., 2.3 vs 3.0)
2. Different build configurations
3. File encoding differences
4. Line ending differences (Windows vs Unix)

**Solutions:**

1. **Verify version match:**
```bash
# User version
./detect-version.sh /user/path

# Official version
cd official-ingenious && git describe --tags
```

2. **Normalize line endings:**
```bash
# Convert to Unix line endings before comparing
find user/ -type f -name "*.java" -exec dos2unix {} \;
```

3. **Check if comparing correct copies:**
```bash
# Ensure both are source OR both are build
./detect-copy-type.sh /user/path
./detect-copy-type.sh /official/path
```

---

## Permission Issues

### Cannot Access Directories

**Problem:** Permission denied when reading files

**Symptoms:**
- "Permission denied"
- "Cannot open file"
- Scripts fail with access errors

**Solutions:**

1. **Check file permissions:**
```bash
ls -la /path/to/ingenious
```

2. **Run with appropriate user:**
```bash
# Don't use sudo unless necessary
# Instead, ensure user owns the files
```

3. **Fix permissions:**
```bash
chmod -R u+r /path/to/ingenious
```

---

### Cannot Write Output Files

**Problem:** Cannot create reports or diff files

**Symptoms:**
- "Permission denied" when writing
- Reports not generated

**Solutions:**

1. **Check output directory permissions:**
```bash
ls -ld /tmp/customization-analysis
```

2. **Use user-writable directory:**
```bash
# Instead of system directory, use home
OUTPUT_DIR=~/customization-analysis
mkdir -p "$OUTPUT_DIR"
```

---

## Script Execution Issues

### Script Not Executable

**Problem:** "Permission denied" when running script

**Symptoms:**
- `bash: ./detect-version.sh: Permission denied`

**Solution:**
```bash
chmod +x scripts/*.sh scripts/*.py
```

---

### Python Script Fails

**Problem:** Python scripts don't run

**Symptoms:**
- "python3: command not found"
- "ModuleNotFoundError"

**Solutions:**

1. **Check Python installation:**
```bash
python3 --version
```

2. **Install Python 3.6+:**
```bash
# macOS
brew install python3

# Ubuntu/Debian
sudo apt-get install python3
```

3. **No external dependencies needed:**
```bash
# Scripts use only standard library
# No pip install required
```

---

## Report Generation Issues

### Empty Report

**Problem:** Report generated but contains no customizations

**Symptoms:**
- Report shows 0 files changed
- "No customizations detected"

**Possible Causes:**
1. User version exactly matches official (no customizations)
2. Comparison failed silently
3. Wrong directories compared

**Solutions:**

1. **Verify diff files exist:**
```bash
ls -lh /tmp/diffs/
```

2. **Check diff content:**
```bash
head -50 /tmp/diffs/diff_Engine.patch
```

3. **Manual spot check:**
```bash
# Pick a file you know you customized
diff official/Engine/src/.../MyFile.java user/Engine/src/.../MyFile.java
```

---

### Report Categories Wrong

**Problem:** Customizations categorized incorrectly

**Symptoms:**
- Bug fix labeled as feature
- Configuration shown as framework core

**Solution:**
- Review categorization patterns in [categorization-patterns.md](./categorization-patterns.md)
- Categorization is automatic and best-effort
- Manually review and reclassify if needed
- Report categories are guidance, not definitive

---

## Disk Space Issues

### Out of Disk Space

**Problem:** Script fails due to insufficient disk space

**Symptoms:**
- "No space left on device"
- Clone or diff fails partway through

**Solutions:**

1. **Check available space:**
```bash
df -h .
```

2. **Clean up temp files:**
```bash
rm -rf /tmp/ingenious-official-*
```

3. **Use different temp directory:**
```bash
export TMPDIR=~/tmp
mkdir -p ~/tmp
```

4. **Download smaller scope:**
```bash
# Clone with limited depth
git clone --depth=1 --branch=v2.3 https://github.com/ing-bank/INGenious.git
```

---

## Module Structure Mismatch

### Different Module Layout

**Problem:** User's modules don't match official structure

**Symptoms:**
- Modules in different directories
- Additional custom modules
- Missing standard modules

**Solution:**
- Document structural differences separately
- Compare only matching modules
- Note custom modules as 100% customizations
- Focus comparison on standard modules

---

## Quick Diagnostic Commands

### Check Installation Health

```bash
#!/bin/bash
echo "=== Installation Diagnostic ==="

echo "1. Checking markers..."
[ -d "Configuration" ] && echo "✓ Configuration" || echo "✗ Configuration"
[ -d "Engine" ] && echo "✓ Engine" || echo "✗ Engine"
[ -d "Projects" ] && echo "✓ Projects" || echo "✗ Projects"

echo ""
echo "2. Detecting version..."
./scripts/detect-version.sh .

echo ""
echo "3. Detecting type..."
./scripts/detect-copy-type.sh .

echo ""
echo "4. Checking permissions..."
[ -r "Engine/pom.xml" ] && echo "✓ Can read files" || echo "✗ Permission issues"

echo ""
echo "5. Checking tools..."
command -v git >/dev/null && echo "✓ Git installed" || echo "✗ Git missing"
command -v python3 >/dev/null && echo "✓ Python 3 installed" || echo "✗ Python 3 missing"
command -v diff >/dev/null && echo "✓ diff available" || echo "✗ diff missing"
```

---

## Getting Help

If issues persist:

1. **Check script README:**
   - [scripts/README.md](../scripts/README.md)

2. **Review reference docs:**
   - [version-detection-reference.md](./version-detection-reference.md)
   - [copy-type-reference.md](./copy-type-reference.md)
   - [comparison-strategy-reference.md](./comparison-strategy-reference.md)

3. **Provide diagnostic info:**
   - OS and version
   - INGenious version
   - Error messages
   - Output of diagnostic commands above

4. **Manual alternative:**
   - You can always manually compare directories using GUI tools
   - Generate report manually from diff output
   - Script automation is a convenience, not a requirement
