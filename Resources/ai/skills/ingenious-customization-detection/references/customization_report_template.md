# INGenious Customization Report Template

## Summary
- **Analyzed Installation:** [path]
- **Version Detected:** [version]
- **Copy Type:** [Source Code / Build]
- **Official Version Compared:** [tag]
- **Analysis Date:** [date]

## Overview Statistics
- **Modules Analyzed:** [count]
- **Files Modified:** [count]
- **Files Added:** [count]
- **Files Deleted:** [count]
- **Total Lines Changed:** [+lines / -lines]

## Detailed Changes by Module

### Module: [Module Name]
**Files Changed:** [count]

#### Customization Category: [Category Name]
**Intent:** [Inferred purpose]
**Impact:** [High/Medium/Low]

**Files Affected:**
- [file path] (+X lines, -Y lines)
- [file path] (+X lines, -Y lines)

**Description:**
[Detailed description of changes]

**Code Sample:**
```java
[Representative code snippet]
```

---

### Module: [Next Module]
[repeat structure]

## Customization Categories

Based on analysis, customizations are grouped into:

### 1. Feature Enhancements
- New functionality added
- Extended capabilities
- Custom actions or commands

### 2. Bug Fixes & Patches
- Error handling improvements
- Null pointer fixes
- Exception handling

### 3. Configuration Changes
- POM modifications
- Dependency updates
- Build configuration

### 4. Integration Customizations
- External tool integrations
- API modifications
- Plugin system changes

### 5. Performance Optimizations
- Code optimizations
- Caching improvements
- Resource management

### 6. UI/Reporting Changes
- Report template modifications
- Dashboard customizations
- Log formatting

### 7. Framework Modifications
- Core engine changes
- API contract modifications
- Architecture changes

## Risk Assessment

| Change Type | Risk Level | Reason |
|-------------|-----------|--------|
| [Type] | [High/Med/Low] | [Explanation] |

## Recommendations

1. **Upgrade Path:** [Suggestions for maintaining customizations during upgrades]
2. **Plugin Candidates:** [Changes that could be extracted to plugins]
3. **Contribution Opportunities:** [Changes that could be contributed upstream]

## Plugin Extraction Opportunities

### Recommended Plugins to Create

Based on the customizations detected, the following plugins are recommended:

#### Plugin 1: [Plugin Name]
**Type:** Browser/Mobile/Webservice/Database/General
**Priority:** High/Medium/Low
**Complexity:** Simple/Moderate/Complex

**Actions to Implement:**
1. `actionName1` - [Description]
2. `actionName2` - [Description]

**Required Dependencies:**
- [dependency-name] version [x.x.x]

**Affected Files:**
- [file path] - [lines modified]

**Code Snippet:**
```java
[Key customization code that would become plugin action]
```

**Next Steps:**
To create this plugin, use: `/ingenious-plugin-creation` and provide this specification.

## Detailed Diff Files

Full diff files available at:
- [path to diff files]
