# Comparison Strategy Reference

## Overview

This document details the strategies and techniques for comparing user installations against official releases to detect customizations.

## Comparison Tools

### diff Command

**Primary tool** for file-by-file comparison.

**Basic syntax:**
```bash
diff -Naur official/ user/ > changes.patch
```

**Flags:**
- `-N`: Treat absent files as empty
- `-a`: Treat all files as text
- `-u`: Unified context format
- `-r`: Recursive comparison

---

### git diff

**Alternative approach** using Git for comparison.

**Advantages:**
- Better handling of file moves/renames
- Cleaner output format
- Built-in ignore patterns

**Strategy:**
```bash
# Initialize temp repo with official version
git init
git add -A
git commit -m "Official version"

# Replace with user version
rm -rf *
cp -r /user/installation/* .
git add -A

# Generate diff
git diff --cached > customizations.patch
```

---

## Exclusion Patterns

### Files to Exclude

**Build outputs:**
```
target/
*.class
*.jar (in target/)
```

**IDE files:**
```
.idea/
*.iml
.vscode/
.settings/
```

**Version control:**
```
.git/
.gitignore
```

**OS files:**
```
.DS_Store
Thumbs.db
```

**Logs:**
```
*.log
logs/
```

**User-specific:**
```
Configuration/conf.js (may contain user paths)
Configuration/XPLOR_SETTINGS.json (user preferences)
```

### Exclusion Commands

**Using diff:**
```bash
diff -Naur \
    --exclude='target' \
    --exclude='*.class' \
    --exclude='.git' \
    --exclude='*.iml' \
    --exclude='.idea' \
    --exclude='node_modules' \
    --exclude='*.log' \
    --exclude='.DS_Store' \
    official/ user/
```

**Using rsync:**
```bash
rsync -avc --dry-run \
    --exclude 'target/' \
    --exclude '.git/' \
    official/ user/
```

---

## Output Formats

### Unified Diff (.patch)

**Format:** Standard patch format with context

**Example:**
```diff
--- a/Engine/src/main/java/com/ing/engine/commands/WebCommands.java
+++ b/Engine/src/main/java/com/ing/engine/commands/WebCommands.java
@@ -145,6 +145,12 @@ public class WebCommands {
     public void click(String objectName) {
         element = findElement(objectName);
         element.click();
+        highlightElement(element);  // CUSTOMIZATION: Added highlighting
+    }
+    
+    private void highlightElement(Element e) {
+        // Custom highlighting logic
+        e.executeScript("this.style.border='3px solid red'");
     }
 }
```

### Quick Summary (.txt)

**Format:** File list with change indicators

**Example:**
```
Only in user/Engine/src/main/java/com/ing/custom: CustomActions.java
Files official/Engine/pom.xml and user/Engine/pom.xml differ
Only in official/Datalib/src: OldFile.java
```

**Command:**
```bash
diff -qr official/ user/
```

---

## Module-by-Module Comparison

### For Source Code Copy

**Modules to compare:**
1. Common/
2. Datalib/
3. Engine/
4. IDE/
5. ingenious-api/
6. StoryWriter/

**Strategy:**
```bash
for module in Common Datalib Engine IDE ingenious-api StoryWriter; do
    echo "Comparing $module..."
    diff -Naur official/$module user/$module > diff_$module.patch
done
```

### For Build Copy

**Module to compare:**
- Engine/ only

**Strategy:**
```bash
diff -Naur official/Engine user/Engine > diff_Engine.patch
```

---

## Analyzing Diff Output

### Extracting Statistics

**Count files changed:**
```bash
grep -c "^diff" changes.patch
```

**Count lines added:**
```bash
grep -c "^+" changes.patch
```

**Count lines removed:**
```bash
grep -c "^-" changes.patch
```

**List changed files:**
```bash
grep "^diff" changes.patch | sed 's/.*b\///'
```

### Categorizing Changes

**New files:**
```bash
grep "^Only in user/" summary.txt
```

**Deleted files:**
```bash
grep "^Only in official/" summary.txt
```

**Modified files:**
```bash
grep "^Files.*differ$" summary.txt
```

---

## Handling Large Diffs

### Problem: Diff Too Large

**Symptoms:**
- Diff file > 100MB
- Too many changes to analyze

**Solutions:**

1. **Summary mode:**
```bash
diff -qr official/ user/ > summary.txt
# Analyze summary instead of full diff
```

2. **Split by module:**
```bash
# Compare modules separately
# Easier to process smaller chunks
```

3. **Filter by file type:**
```bash
# Only compare Java files
diff -Naur official/ user/ --include='*.java'
```

4. **Exclude generated code:**
```bash
# Skip auto-generated files
--exclude='*Generated.java'
```

---

## Diff Interpretation

### Reading Unified Diff

**Structure:**
```diff
--- a/path/to/file.java      # Original (official)
+++ b/path/to/file.java      # Modified (user)
@@ -145,6 +145,12 @@         # Change location (line numbers)
 public void method() {       # Context line (unchanged)
-    oldCode();               # Removed line
+    newCode();               # Added line
 }
```

**Symbols:**
- `---` / `+++`: File headers (official vs user)
- `@@`: Change hunk location
- ` ` (space): Context line (unchanged)
- `-`: Line removed from official
- `+`: Line added in user version

### Common Patterns

**New method added:**
```diff
+    public void customAction() {
+        // New functionality
+    }
```

**Method modified:**
```diff
     public void existingMethod() {
-        oldImplementation();
+        newImplementation();
     }
```

**Dependency added:**
```diff
     <dependencies>
+        <dependency>
+            <groupId>com.example</groupId>
+            <artifactId>custom-lib</artifactId>
+        </dependency>
     </dependencies>
```

---

## Verification

### Sanity Checks

**Check diff is not empty:**
```bash
[ -s changes.patch ] || echo "No changes detected"
```

**Verify modules exist:**
```bash
[ -d official/Engine ] && [ -d user/Engine ] || echo "Module missing"
```

**Check for binary files:**
```bash
grep "Binary files differ" changes.patch
```

### Quality Checks

**Ensure sensible change count:**
```bash
changes=$(grep -c "^diff" changes.patch)
if [ $changes -eq 0 ]; then
    echo "WARNING: No differences found"
elif [ $changes -gt 1000 ]; then
    echo "WARNING: Excessive changes ($changes files) - may be comparing different major versions"
fi
```

---

## Best Practices

1. **Always exclude build outputs** - target/, *.class
2. **Compare matching versions** - Verify version match before comparing
3. **Use absolute paths** - Avoid confusion with relative paths
4. **Save both full diff and summary** - Different use cases
5. **Document exclusions** - Note what was excluded in report
6. **Verify module structure first** - Ensure both installations are valid
7. **Handle binary files separately** - Note binary changes without diffing
8. **Preserve context** - Use unified format with sufficient context lines
9. **Split large comparisons** - Module-by-module for maintainability
10. **Automate where possible** - Script comparison for consistency
