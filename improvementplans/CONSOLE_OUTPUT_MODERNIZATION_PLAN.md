# Console Output Modernization Plan

## Overview

This plan outlines the modernization of console output during test execution in INGenious_LT. The goal is to transform the scattered, inconsistent console output into a modern, visually appealing, and informative display.

## Current State Analysis

### Existing Console Output Locations

| Location | Current Output Style |
|----------|---------------------|
| `TestCaseReport.java` | `[PASS] \| Step description ✅` followed by `===` separator |
| `MessageConsole.java` | Colors lines based on `[PASS]`/`[FAIL]` prefix |
| Various Engine commands | Scattered `System.out.println` (100+ locations) |
| `SummaryReport.java` | Basic `=== [UPDATING SUMMARY] ===` style headers |

### Issues with Current Approach

1. **Inconsistent formatting** - Each class formats output differently
2. **No step progress tracking** - Users don't know how many steps remain
3. **Missing timing information** - No visibility into step or total execution time
4. **No visual hierarchy** - Hard to distinguish scenarios, test cases, and steps
5. **Basic status indicators** - Minimal use of colors and symbols

## Proposed Solution

### New Architecture

```
Engine/src/main/java/com/ing/engine/reporting/console/
├── ConsoleLogger.java      # Main logging facade
└── ConsoleFormatter.java   # Formatting utilities (box-drawing, colors)
```

### Example Output Format

```
╔══════════════════════════════════════════════════════════════════╗
║  INGenious Test Execution                                        ║
║  Project: Tutorial  |  Browser: Chrome  |  Started: 14:32:05     ║
╚══════════════════════════════════════════════════════════════════╝

┌─ Scenario: LoginTests
│  └─ TestCase: ValidLogin [Iteration 1]
│
│  [1/5]  ✅ PASS   OpenBrowser          → Browser launched (0.8s)
│  [2/5]  ✅ PASS   navigateToApplication → Navigated to https://... (1.2s)
│  [3/5]  ✅ PASS   setUsername          → Entered "testuser" (0.1s)
│  [4/5]  ✅ PASS   setPassword          → Entered "****" (0.1s)
│  [5/5]  ✅ PASS   clickLogin           → Element clicked (0.3s)
│
│  ├─ Reusable: VerifyHomepage
│  │  [1/2]  ✅ PASS   verifyElementPresent → Dashboard visible (0.2s)
│  │  [2/2]  ✅ PASS   verifyTitle          → Title matches (0.1s)
│  └─ End Reusable
│
└─ TestCase Complete: PASSED ✅  [5 passed, 0 failed]  Duration: 2.8s

═══════════════════════════════════════════════════════════════════
 EXECUTION SUMMARY
═══════════════════════════════════════════════════════════════════
 Total: 2 test cases  |  Passed: 2  |  Failed: 0  |  Duration: 5.4s
═══════════════════════════════════════════════════════════════════
```

### Status Icons

| Status | Icon | Color |
|--------|------|-------|
| PASS | ✅ | Green |
| FAIL | ❌ | Red |
| DONE | 🟢 | Green |
| WARNING | ⚠️ | Yellow |
| DEBUG | 🔴 | Red |
| SCREENSHOT | 📸 | Blue |
| INFO | ℹ️ | Blue |

## Implementation Tasks

### Task 1: Create ConsoleFormatter.java

Utility class for formatting:
- Box-drawing characters (╔, ═, ╗, ║, ╚, ╝, ┌, ─, ┐, │, └, ┘, ├, ┤)
- Color codes (ANSI for terminal, handled by MessageConsole for IDE)
- Padding and alignment helpers
- Time formatting utilities

### Task 2: Create ConsoleLogger.java

Centralized logging facade:
- `printHeader(project, browser, startTime)` - Execution header
- `printScenarioStart(scenario)` - Scenario section start
- `printTestCaseStart(testCase, iteration)` - Test case section start
- `printStep(stepNum, totalSteps, status, action, description, duration)` - Step output
- `printReusableStart(name)` - Reusable component start
- `printReusableEnd(name)` - Reusable component end
- `printTestCaseEnd(passed, failed, duration)` - Test case summary
- `printSummary(total, passed, failed, duration)` - Execution summary

### Task 3: Update TestCaseReport.java

- Replace direct `System.out.println` with ConsoleLogger calls
- Track step count for progress display
- Capture timing for each step

### Task 4: Update SummaryReport.java

- Use ConsoleLogger for summary output
- Aggregate results from all test cases

### Task 5: Integration with MessageConsole.java

- MessageConsole already handles color based on `[PASS]`/`[FAIL]` prefix
- ConsoleLogger output will work with existing color detection

## Files to Modify

| File | Changes |
|------|---------|
| `ConsoleLogger.java` | **NEW** - Centralized logging |
| `ConsoleFormatter.java` | **NEW** - Formatting utilities |
| `TestCaseReport.java` | Replace System.out.println with ConsoleLogger |
| `SummaryReport.java` | Use ConsoleLogger for summary |

## Benefits

1. **Improved readability** - Clear visual hierarchy
2. **Progress visibility** - Step counter shows progress
3. **Timing information** - Duration for each step and total
4. **Consistent formatting** - Single source of truth for output
5. **Easy maintenance** - All console output in one place

## Timeline

| Phase | Description | Estimate |
|-------|-------------|----------|
| 1 | Create ConsoleFormatter and ConsoleLogger | 30 min |
| 2 | Update TestCaseReport | 20 min |
| 3 | Update SummaryReport | 10 min |
| 4 | Testing and refinement | 15 min |

## Future Enhancements

- Progress bar for long-running tests
- Real-time test dashboard output
- Configurable verbosity levels
- JSON output mode for CI/CD integration
