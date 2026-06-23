# INGenious — assertVariable + Parameterization Bug Analysis

## Summary

This analysis covers how `assertVariable` (a `General`-object action) interacts with the IDE's inline validation/rendering system, specifically the bug where the Input column renders red even when the user has provided a complete, valid input value. The project is a Java-based test automation framework (INGenious by ING Bank) with an IDE built on Swing, an Engine that executes test steps, and a model layer that manages test cases/steps in memory.

## Root Cause

The bug is in **`IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/validation/InputRenderer.java`**, in the `isInputValid()` method.

### Tracing the problem for `assertVariable` with `General`:

**Configuration**: ObjectName=`General`, Action=`assertVariable`, Input=`%myVar%=expected`

**`InputRenderer.render()` validation chain**:

1. **Empty check**: Input is not empty → skip ✅
2. **`isNotNeeded(step)`**: The `@Action` annotation has `input = InputType.YES`, so `isNotNeeded()` returns `false` → skip ✅
3. **`isTestDataStep()` check**: `TestStep.isTestDataStep()` uses `getInput().matches("(?!(@|=|%)).+:.+")`. The input `%myVar%=expected` has no colon → returns `false` → skip ✅
4. **`isInputValid(value, step.getObject())`**: **THIS IS WHERE THE BUG IS** ❌

The `isInputValid()` method checks against these regexes:

```java
val.matches("(@.+)|(=.+)|(%.+%)|(#.+)")  // FALSE — "%myVar%=expected" starts with % but doesn't end with %
val.startsWith("<") || val.startsWith("{") || val.startsWith("[")  // FALSE
// → returns FALSE → RED ERROR
```

**Result**: The cell is rendered red with the tooltip "Syntax error. Input should be one of [@val , #val, %var% ,=Function ,Sheet:Column]".

### Why `assertVariable` expects `key=value`

Looking at `Engine/src/main/java/com/ing/engine/commands/general/GeneralOperations.java` → `assertVariable()` (line ~54):

```java
@Action(object = ObjectType.GENERAL, desc = "Assert if Key:Value -> [<Data>] is valid", input = InputType.YES)
public void assertVariable() {
    String strObj = Data;             // The Input column value
    String[] strTemp = strObj.split("=", 2);  // Splits on "="
    String strAns = strTemp[0].matches("%.+%") ? getVar(strTemp[0]) : strTemp[0];
    if (strAns.equals(strTemp[1])) {
        // PASS — variable value matches
    } else {
        // FAIL — no match
    }
}
```

The input format is explicitly `key=value` — something like:
- `%myVar%=expectedValue` — check that runtime variable `%myVar%` equals `expectedValue`  
- `myVar=42` — check that literal string `myVar` equals `42`

## THE FIX

### File to modify
`IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/validation/InputRenderer.java`

### Method to modify
`isInputValid(Object value, String objectName)` (around line 80)

### Change

Add a case to accept `key=value` format before the final `return false`:

```java
// In InputRenderer.isInputValid(), add this before the final return false:
if (val.contains("=") && !val.startsWith("=")) {
    return true;  // Accept key=value format used by assertVariable
}
```

This handles:
- `%myVar%=expected` — runtime variable comparison (most common for assertVariable)
- `myVar=expected` — literal comparison
- `%variable%=Sheet:Column` — test data reference comparison (when parameterized)

### Why not fix the other methods?

Fixing `isTestDataStep()` on `TestStep` would be riskier — it would change behavior for ALL actions, not just `assertVariable`. The localized fix in `InputRenderer.isInputValid()` is the safest approach.

## Parameterization Context

When a user parameterizes steps and `assertVariable` is between `Start Param` / `End Param`:

1. The parameterization (via `TestCaseComponent.parameterizeSelectedSteps()`) sets the Condition column on start/end rows — it does NOT modify Input values
2. The Input column for `assertVariable` may contain:
   - A static value like `%myVar%=expected` (no parameterization involvement)
   - A test data reference like `%myVar%=DataSheet:ColumnName` — this WILL contain a colon, causing `isTestDataStep()` to return `true`
3. If the input is `%myVar%=DataSheet:ColumnName`, `isTestDataPresent()` splits on `:` and gets `["%myVar%", "DataSheet", "ColumnName"]` — it tries to find a test data sheet named `%myVar%`, which doesn't exist → fails → **RED AGAIN**

So there are actually **two edge cases** where `assertVariable` shows spurious red with parameterization:
1. Simple `key=value` without a colon (falls through to `isInputValid()` → fails)
2. `key=Sheet:Column` with colon (fails `isTestDataPresent()`)

The fix above (accepting `key=value` format in `isInputValid()`) resolves case 1. For case 2, we need to ensure `isTestDataStep()` doesn't match the `key=` portion — but the fix should distinguish between `key=Sheet:Column` (test data) and `key=value` (simple assertion). 

**The complete fix would be**: in `isInputValid()`, also handle the case where the input contains a `=`, as `assertVariable` is the only action (other than equals-comparison actions) that uses this format.

## Detailed report saved

The full analysis report has been saved as `project_info__1.md` in the project root with complete code references, trace paths, alternative fix approaches, and the reasoning behind the recommended fix.