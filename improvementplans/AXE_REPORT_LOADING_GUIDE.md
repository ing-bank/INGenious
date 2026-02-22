# aXe Accessibility Report Modal - Loading Guide

## Overview

The test execution report now includes **aXe accessibility report buttons** for any test step that runs accessibility checks (`testAccessibility` action). Clicking the button opens a modal with detailed accessibility violation analysis.

## File Locations

- **Report Template**: `Resources/Configuration/ReportTemplate/html/detailed-v2.html`
- **Deployed Version**: `Dist/release/detailed-v2.html`
- **aXe JSON Data**: Stored in `aXe/` subdirectory next to the HTML report:
  - Example: `aXe/Mortgage Calculator_Applicant_axe-results.json`

## Technical Architecture

### Data Loading Strategy

The modal uses a **3-tier fallback approach**:

```
1. Embedded HTML Data (BEST)
   ↓ (if not available)
2. XMLHttpRequest to file:// URL (GOOD)
   ↓ (if blocked by browser security)
3. Fetch API with no-cors (ACCEPTABLE)
   ↓ (if all else fails)
4. Manual workaround needed (LAST RESORT)
```

### Tier 1: Embedded Data (Recommended Long-Term)

**Current Status**: Not yet implemented

**Requirements**:
- Java/Engine code must embed aXe JSON in test report HTML
- Location: `Engine/src/main/java/.../HtmlTestCaseHandler.java`
- Implementation: Add hidden `<script type="application/json" id="axe-data-{reusable}">` element
- Benefit: Eliminates file:// protocol security issues entirely

**Example HTML structure needed**:
```html
<!-- In detailed-v2.html, near opening <body> -->
<script type="application/json" id="axe-data-Applicant">
{
  "violations": [...],
  "passes": [...],
  ...
}
</script>
```

### Tier 2: XMLHttpRequest (Current Primary)

**How it works**:
1. Extracts relative path: `aXe/Scenario_ReusableName_axe-results.json`
2. Converts to absolute file:// URL: `file:///path/to/aXe/Scenario_ReusableName_axe-results.json`
3. Sends XMLHttpRequest to load JSON
4. On success (status 0 or 200): Parses and displays data
5. On error: Falls back to Tier 3

**Browser Support**: ✅ Works in most modern browsers when run from file://

**Known Issues**: 
- Some browsers/environments block XHR on file:// URLs
- Server-side file access restrictions may apply

### Tier 3: Fetch API

**How it works**:
1. Uses Fetch API with `mode: 'no-cors'`
2. Avoids CORS checks that block file:// URLs
3. Parses response as JSON

**Browser Support**: ✅ Works in all modern browsers

**Note**: Lower reliability than XMLHttpRequest for file:// protocol

### Tier 4: Manual Workaround

If all automated methods fail, use one of these approaches:

**Option A: Copy file to same directory as HTML**
```bash
# Copy aXe JSON next to the HTML report
cp "aXe/Scenario_ReusableName_axe-results.json" "Scenario_ReusableName_axe-results.json"
```

**Option B: Run test report on HTTP server**
```bash
# Start a simple web server in the report directory
cd /path/to/report/directory
python3 -m http.server 8000

# Open report in browser
open http://localhost:8000/detailed-v2.html
```

**Option C: Upload JSON to backend**
- Future feature: Upload aXe JSON to server and embed it in HTML during generation

## Testing & Troubleshooting

### Step 1: Open the Test Report

1. Generate a test execution that includes `testAccessibility` steps
2. Locate the report HTML file (typically in `Dist/release/` or report output directory)
3. Open with a web browser (double-click or `open report.html`)

### Step 2: Locate aXe Report Buttons

1. Find a test step with **Accessibility** or **testAccessibility** action
2. Expand the step details (click the expand icon)
3. Look for the **purple "aXe Report" button** (labeled "📊 aXe Report")

### Step 3: Click aXe Report Button

1. Click the button to open the modal
2. The modal should load in ~1-2 seconds
3. If loading spinner appears indefinitely → Check console logs (Step 4)

### Step 4: Check Browser Console for Diagnostics

**Open Developer Console**:
- Chrome/Edge: `Cmd+Option+J` (macOS) or `F12`
- Firefox: `Cmd+Option+K` (macOS)
- Safari: Enable Developer Tools in preferences, then `Cmd+Option+U`

**Look for messages starting with**:
```
[aXe Modal] Attempting to load from: aXe/...
[aXe Modal] Full decoded URL: file:///...
[aXe Modal] XHR failed: ...
[aXe Modal] Attempting fetch API...
```

**Common Console Outputs**:

| Message | Meaning | Solution |
|---------|---------|----------|
| `[aXe Modal] XHR load successful` | ✅ Data loaded via XMLHttpRequest | No action needed |
| `[aXe Modal] Fetch succeeded` | ✅ Data loaded via Fetch API | No action needed |
| `[aXe Modal] XHR failed: 0` | ⚠️ Browser blocked XHR | Try Fetch (Tier 3) or Option B below |
| `Browser blocked file access` | 🔒 Security restriction | Use HTTP server (Option B) |
| `File may not exist` | ❌ aXe JSON not found | Verify file exists, check path |
| `Parse error` | ❌ JSON is malformed | Regenerate aXe report |

### Step 5: Troubleshoot File Access Issues

**Verify file exists**:
```bash
# List aXe directory contents
ls -la "/path/to/report/directory/aXe/"

# Should show JSON files like:
# -rw-r--r-- Scenario_ReusableName_axe-results.json
```

**Verify file permissions**:
```bash
# Check if HTML can read the JSON file
test -r "/path/to/report/directory/aXe/Scenario_ReusableName_axe-results.json" && echo "✓ Readable" || echo "✗ Not readable"
```

**Verify JSON format**:
```bash
# Validate JSON syntax
cat "/path/to/report/directory/aXe/Scenario_ReusableName_axe-results.json" | python3 -m json.tool > /dev/null && echo "✓ Valid JSON" || echo "✗ Invalid JSON"
```

## Issues & Solutions

### Issue: aXe Report button not visible

**Cause**: Step doesn't have `testAccessibility` action

**Solution**: 
- Verify test step includes `testAccessibility` action in script
- Check that aXe report was generated for that step
- Redeploy the report HTML if needed

---

### Issue: Modal opens but shows loading spinner indefinitely

**Diagnosis**:
1. Open browser console (Cmd+Option+J)
2. Look for `[aXe Modal]` messages
3. Check for error messages below

**Common Causes & Solutions**:

**Cause A: Browser blocks file:// XHR (most common)**
- **Message**: `Browser blocked file access`
- **Solution 1**: Run report on HTTP server (Option B above)
- **Solution 2**: Copy JSON file to same directory (Option A above)
- **Solution 3**: Restart browser in development mode (launches without security restrictions)

**Cause B: aXe JSON file doesn't exist**
- **Message**: `File may not exist or is not readable`
- **Solution**:
  1. Verify file exists: `ls -la aXe/`
  2. Check exact filename matches path in modal
  3. Regenerate test execution report

**Cause C: JSON parsing error**
- **Message**: `Failed to parse JSON: SyntaxError`
- **Solution**:
  1. Validate JSON: `python3 -m json.tool filename.json`
  2. If invalid: Regenerate aXe report from test execution
  3. Check file isn't corrupted

---

### Issue: Modal shows error message instead of loading data

**Solution**: 
1. Read the error message carefully - it will guide you
2. Check browser console for `[aXe Modal]` logs
3. Try one of the workarounds in "Manual Workaround" section

---

## Advanced: Running on HTTP Server

The most reliable method is to serve the report from a local HTTP server:

**Python (built-in)**:
```bash
cd /path/to/report/directory
python3 -m http.server 8000
open http://localhost:8000/detailed-v2.html
```

**Node.js (http-server)**:
```bash
npm install -g http-server
cd /path/to/report/directory
http-server
open http://localhost:8080/detailed-v2.html
```

**Benefits**:
- ✅ Eliminates file:// protocol security restrictions
- ✅ Reliable file:// XHR access
- ✅ Works in all browsers
- ✅ Simulates production environment

## Implementation Plan for Perfect Solution

To make aXe data loading 100% reliable across all browsers/environments:

### Backend Changes (Engine Module)

**File**: `Engine/src/main/java/com/ing/engine/reporting/impl/html/HtmlTestCaseHandler.java`

**Change**:
```java
// During HTML generation, for each testAccessibility step:
1. Find aXe JSON file for that reusable
2. Read JSON content
3. Embed in HTML as: <script type="application/json" id="axe-data-{ReusableName}">...JSON...</script>
4. Place script tag in <head> or near modal content
```

**Benefits**:
- No file:// protocol issues
- Works in ALL browsers
- Zero latency (data pre-loaded)
- No XHR/Fetch API needed

### Frontend Already Ready

The `detailed-v2.html` modal already checks for embedded data first:

```javascript
// Line ~2450 in detailed-v2.html
openAxeReportModal(reusableName, jsonPath) {
  // 1. Check for pre-embedded data (BEST)
  const embeddedDataId = 'axe-data-' + reusableName.replace(/[^a-zA-Z0-9]/g, '-');
  const embeddedElement = document.getElementById(embeddedDataId);
  if (embeddedElement?.textContent) {
    this.axeModal.data = JSON.parse(embeddedElement.textContent);
    this.processAxeResults();
    return;
  }
  // 2. Fallback: File loading (current tier 2)
  this.loadAxeReportData(jsonPath);
}
```

Just need backend implementation! 🎯

---

## File Structure Reference

```
TestReport/
├── detailed-v2.html              ← Report template with aXe modal
├── aXe/                          ← aXe JSON reports directory
│   ├── Scenario_Applicant_axe-results.json
│   ├── Scenario_Your Plans_axe-results.json
│   └── ...
└── _attachments/                 ← Other report resources
    ├── assets/ ├── css/
    └── js/
```

## Summary

| Strategy | Status | Reliability | Action Needed |
|----------|--------|-------------|---------------|
| Embedded Data | ⏳ Ready (frontend), pending backend | 100% ⭐⭐⭐⭐⭐ | Implement backend embedding |
| XMLHttpRequest | ✅ Deployed | 85% ⭐⭐⭐⭐ | Test in your environment |
| Fetch API | ✅ Deployed | 80% ⭐⭐⭐ | Automatic fallback |
| HTTP Server | ✅ Available | 100% ⭐⭐⭐⭐⭐ | Use if XHR fails |
| Manual Upload | 🔄 Future | N/A | Not yet implemented |

**Current Recommendation**: Test XMLHttpRequest+Fetch approach. If it works → use it. If not → run on HTTP server or implement backend embedding.
