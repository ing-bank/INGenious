# INGenious MCP Startup Diagnostics

Complete diagnostic suite for troubleshooting INGenious MCP (Model Context Protocol) server startup failures.

## 📋 Documentation Index

### 1. **[MCP-TROUBLESHOOTING-QUICK-GUIDE.md](MCP-TROUBLESHOOTING-QUICK-GUIDE.md)** ⭐ START HERE
   - **For:** End users experiencing MCP startup issues
   - **Time:** 5-10 minutes
   - **Contents:**
     - Quick diagnostic checklist (1 minute)
     - 7 common issues with step-by-step solutions
     - VS Code settings reference
     - When to get help

### 2. **[MCP-ROOT-CAUSE-ANALYSIS.md](MCP-ROOT-CAUSE-ANALYSIS.md)** 🔍 DETAILED ANALYSIS
   - **For:** Developers and support engineers
   - **Time:** 20-30 minutes
   - **Contents:**
     - 6 root causes with detailed explanations
     - Why each issue fails on some machines
     - Architecture diagrams
     - File references with line numbers
     - Suggested code fixes

### 3. **[MCP-CODE-ISSUES-INDEX.md](MCP-CODE-ISSUES-INDEX.md)** 🛠️ FOR DEVELOPERS
   - **For:** Contributors implementing fixes
     - Exact file paths and line numbers
     - Code snippets showing the bug
     - Before/after code fixes
     - Priority ranking
     - Testing instructions

### 4. **[copilot-mcp-diagnostic.md](copilot-mcp-diagnostic.md)** 📚 COMPREHENSIVE REFERENCE
   - **For:** In-depth technical documentation
   - **Time:** 30-60 minutes
   - **Contents:**
     - Complete MCP architecture overview
     - Diagnostic checklist (Level 1-4)
     - Solutions for each root cause
     - Startup flow diagrams
     - CLI diagnostic commands

---

## 🚀 Quick Start

### For End Users

1. **Run the diagnostic script:**
   ```bash
   bash ingenious-mcp-diagnostic.sh
   ```

2. **Check the output** for failed items

3. **Read [MCP-TROUBLESHOOTING-QUICK-GUIDE.md](MCP-TROUBLESHOOTING-QUICK-GUIDE.md)** for your specific issue

### For Developers

1. **Read [MCP-ROOT-CAUSE-ANALYSIS.md](MCP-ROOT-CAUSE-ANALYSIS.md)** for overview

2. **Check [MCP-CODE-ISSUES-INDEX.md](MCP-CODE-ISSUES-INDEX.md)** for specific issues to fix

3. **Review and apply suggested code fixes**

4. **Test with:**
   ```bash
   echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"0.1.0"}}}' | ingenious server mcp
   ```

---

## 🔴 The 6 Root Causes

| # | Issue | Severity | Component | Files |
|---|-------|----------|-----------|-------|
| 1 | CLI not found | 🔴 CRITICAL | VS Code Extension | `paths.ts` |
| 2 | Classpath expansion fails | 🔴 CRITICAL | Java Engine | `CopilotSdkProvider.java` |
| 3 | Silent process crashes | 🔴 CRITICAL | Java CLI | `ServerCommand.java` |
| 4 | Configuration errors | 🟠 HIGH | VS Code Extension | `paths.ts` |
| 5 | Stderr not captured | 🟠 HIGH | Java Engine | `CopilotSdkProvider.java` |
| 6 | Timeout too short | 🟡 MEDIUM | VS Code Extension | `mcp.ts` |

**See [MCP-ROOT-CAUSE-ANALYSIS.md](MCP-ROOT-CAUSE-ANALYSIS.md) for details**

---

## 📁 Files in This Suite

```
INGenious-git/
├── README-MCP-DIAGNOSTICS.md          ← You are here
├── MCP-TROUBLESHOOTING-QUICK-GUIDE.md ← Quick fixes for users
├── MCP-ROOT-CAUSE-ANALYSIS.md         ← Detailed technical analysis
├── MCP-CODE-ISSUES-INDEX.md           ← Code fixes for developers
├── copilot-mcp-diagnostic.md          ← Complete reference
└── ingenious-mcp-diagnostic.sh        ← Automated diagnostic script
```

---

## 🧪 Diagnostic Script

Automated checks for all 6 root causes:

```bash
# Run with default checks
bash ingenious-mcp-diagnostic.sh

# Run with verbose output
bash ingenious-mcp-diagnostic.sh --verbose

# What it checks:
# ✓ CLI availability and executability
# ✓ Java installation and version
# ✓ INGenious installation structure
# ✓ MCP server startup and response
# ✓ Java classpath validity
# ✓ Network connectivity
# ✓ Platform-specific issues
# ✓ VS Code configuration
```

---

## 🎯 Troubleshooting Flowchart

```
MCP Not Starting?
    ↓
[Run diagnostic script]
    ↓
[Checks fail?]
    ├─ YES → [Read MCP-TROUBLESHOOTING-QUICK-GUIDE.md]
    │         ├─ CLI not found? → Issue #1
    │         ├─ Java not found? → Install Java 17+
    │         ├─ ClassNotFoundException? → Issue #2
    │         ├─ Silent crash? → Issue #3
    │         ├─ Timeout? → Issue #6
    │         └─ Wrong settings? → Issue #4
    └─ NO → [Still have issues?] → [Post on support]
```

---

## 📊 Issue Severity and Impact

### 🔴 CRITICAL (Blocks MCP startup completely)

**Issues 1, 2, 3** prevent the MCP server from starting at all:

```
User's Impact:
  - AI Chat unavailable
  - Cannot use Copilot integration
  - MCP status: "error"
  - Exit code: 1
```

**Fix Priority:** Highest - Fix immediately

### 🟠 HIGH (Causes startup failures on some machines)

**Issues 4, 5** cause failures on specific configurations:

```
User's Impact:
  - Works on developer's machine
  - Fails on CI/CD or customer machines
  - Intermittent failures
  - Incomplete error messages
```

**Fix Priority:** High - Fix before release

### 🟡 MEDIUM (Degrades user experience)

**Issue 6** causes unnecessary delays:

```
User's Impact:
  - Slow machines experience timeout
  - Retry mechanism kicks in
  - Network lag affects responsiveness
```

**Fix Priority:** Medium - Improve but not blocking

---

## 🔧 Implementation Roadmap

| Phase | Issue | Task | Effort | Status |
|-------|-------|------|--------|--------|
| 1 | #1 | Add CLI path validation | 1h | Ready |
| 1 | #2 | Add classpath error handling | 2h | Ready |
| 1 | #3 | Add error logging | 1.5h | Ready |
| 2 | #4 | Add config validation | 1h | Ready |
| 2 | #6 | Make timeout configurable | 0.5h | Ready |
| 3 | #5 | Implement stderr capture | 1h | Ready |

**Total Estimated Effort:** 7.5 hours

**Testing Time:** 2-3 hours

---

## ✅ Verification Checklist

After applying fixes:

- [ ] Run `ingenious server mcp --verbose` and see no errors
- [ ] Verify MCP server responds to initialize request
- [ ] Test on slow machine (should complete initialization)
- [ ] Test with missing CLI (should show clear error)
- [ ] Test with invalid JAVA_HOME (should show clear error)
- [ ] Test with broken classpath (should show clear error)
- [ ] Run diagnostic script and all checks pass
- [ ] VS Code Output > INGenious > MCP shows "MCP server connected"

---

## 📞 Getting Help

### If Diagnostic Script Passes But MCP Still Doesn't Work

1. **Enable verbose logging:**
   ```json
   {
       "ingenious.debug": true,
       "ingenious.mcp.verbose": true
   }
   ```

2. **Check VS Code Output:**
   - View > Output > INGenious > MCP
   - Copy all logs

3. **Run manual test:**
   ```bash
   ingenious server mcp --verbose < /dev/null
   ```

4. **Share with support team:**
   - Diagnostic output
   - VS Code logs
   - Error messages
   - OS and Java version

---

## 📝 Document Versions

- **MCP-TROUBLESHOOTING-QUICK-GUIDE.md** - v1.0 (2026-11-13)
- **MCP-ROOT-CAUSE-ANALYSIS.md** - v1.0 (2026-11-13)
- **MCP-CODE-ISSUES-INDEX.md** - v1.0 (2026-11-13)
- **copilot-mcp-diagnostic.md** - v1.0 (2026-11-13)
- **ingenious-mcp-diagnostic.sh** - v1.0 (2026-11-13)

---

## 🔗 Related Resources

- [INGenious GitHub Repository](https://github.com/ing-bank/INGenious)
- [MCP Specification](https://spec.modelcontextprotocol.io/)
- [Copilot SDK Java](https://github.com/github/copilot-sdk-java)
- [VS Code Extension Documentation](https://code.visualstudio.com/docs/editor/extension-gallery)

---

**Need help?** Start with [MCP-TROUBLESHOOTING-QUICK-GUIDE.md](MCP-TROUBLESHOOTING-QUICK-GUIDE.md)
