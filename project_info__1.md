# INGenious — assertVariable + Parameterization Bug Analysis

## Summary

This analysis covers how `assertVariable` (a `General`-object action) interacts with the IDE's inline validation/rendering system, specifically the bug where the Input column renders red even when the user has provided a complete, valid input value. The project is a Java-based test automation framework (INGenious by ING Bank) with an IDE built on Swing, an Engine that executes test steps, and a model layer that manages test cases/steps in memory.

## Architecture

The system follows a 3-module architecture:

1. **Datalib** (`Datalib/`) — Data model: `TestStep`, `TestCase`, `Scenario`, `Project`, `ReusableRef`, etc.
2. **Engine** (`Engine/`) — Execution runtime: discovers `@Action`-annotated methods, dispatches them, manages browser/web service/mobile drivers.
3. **IDE** (`IDE/`) — Swing-based UI: table editor for test cases, auto-suggest, validation renderers, Playwright recorder.

The validation/rendering subsystem lives entirely in the IDE module and consists of:

- `TestCaseValidator` — wires per-column cell renderers to the JTable
- `AbstractRenderer` — base class for all column renderers; tracks `errorState`
- `ActionRenderer` — validates the Action column
- `InputRenderer` — validates the Input column (THE BUG IS HERE)
- `ObjectRenderer` — validates the ObjectName column
- `ConditionRenderer` — validates the Condition column
- `ReferenceRenderer` — validates the Reference column
- `TestCaseValidation` — aggregates all renderers to validate full test cases and scenarios (used to color tree nodes red)

## How Parameterization Works

When the user selects rows and clicks "Parameterize" in the context menu:

1. `TestCaseComponent.parameterizeSelectedSteps()` is called
2. The first selected row gets its Condition set to `"Start Param"` (or a new filler row is inserted)
3. The last selected row gets its Condition set to `"End Param"` (or a new filler row is inserted)
4. The `Start Param` step's Input column typically holds a `Sheet:Column` reference to a test data sheet

During execution, the Engine iterates over the data rows between Start/End Param markers, substituting values for each iteration. This is the **data-driven parameterization** mechanism.

## Key Abstractions

### `Action` Annotation
- **File**: `ingenious-api/src/main/java/com/ing/ingenious/api/annotation/Action.java`
- **Responsibility**: Marks a method as an executable test action
- **Key fields**: `object` (which ObjectType it belongs to), `input` (InputType: YES/NO/OPTIONAL), `condition` (InputType: YES/NO/OPTIONAL), `desc`
- **Used by**: `MethodInfoManager` to discover all actions; renderers to look up whether input/condition is mandatory

### `InputType` Enum
- **File**: `ingenious-api/src/main/java/com/ing/ingenious/api/types/InputType.java`
- **Values**: `YES` (mandatory), `NO` (not needed → should be empty), `OPTIONAL`
- **Methods**: `isMandatory()`, `isOptional()`, `isNotNeeded()`
- **Used by**: `InputRenderer.isOptional()`, `InputRenderer.isNotNeeded()`, `ConditionRenderer.isOptional()`

### `GeneralOperations.assertVariable()`
- **File**: `Engine/src/main/java/com/ing/engine/commands/general/GeneralOperations.java` (line ~54)
- **Annotation**: `@Action(object = ObjectType.GENERAL, desc = "Assert if Key:Value -> [<Data>] is valid", input = InputType.YES)`
- **Input format**: Expects `key=value` in the Input column (e.g., `%myVar%=expected` or `myVar=42`)
- **Condition**: NOT annotated → defaults to `InputType.NO` (condition not needed)
- **Logic**: Splits input on `=`, validates that the left side's value matches the right side

### `InputRenderer`
- **File**: `IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/validation/InputRenderer.java`
- **Responsibility**: Renders the Input column with appropriate colors; sets error state (red) when input is invalid
- **Validation chain**:
  1. If empty → check `isOptional()`; if not optional → RED
  2. If `isNotNeeded()` → RED (input should be empty for this action)
  3. If `isTestDataStep()` and `!isTestDataPresent()` → RED
  4. If `isInputValid()` returns false → RED

### `AbstractRenderer`
- **File**: `IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/validation/AbstractRenderer.java`
- **Key method**: `hasError(TestStep)` — runs the render logic on a scratch component to determine if the step has a validation error
- **Error states**: `setEmpty()` (red background + red border), `setNotPresent()` (red foreground text)

## Root Cause of the Bug

The bug is in `InputRenderer.render()`, specifically in its validation chain for the `assertVariable` action. Here's the exact path through the code:

### Scenario
- **ObjectName**: `General`
- **Action**: `assertVariable`
- **Input**: `%myVar%=expected` (valid format for this action)

### Tracing `InputRenderer.render()`:

1. **Empty check**: Input is not empty → skip
2. **`isNotNeeded(step)`**: Calls `MethodInfoManager.getActionFor("assertVariable").input().isNotNeeded()`. The annotation has `input = InputType.YES`, so `isNotNeeded()` returns `false` → skip
3. **`isTestDataStep()` check**: 
   - `step.isTestDataStep()` calls `getInput().matches("(?!(@|=|%)).+:.+")` 
   - The input `%myVar%=expected` does NOT contain a colon (`:`), so this returns `false` → skip
4. **`isInputValid(value, step.getObject())`**:
   ```java
   String val = "%myVar%=expected";
   val.matches("(@.+)|(=.+)|(%.+%)|(#.+)")  // FALSE — doesn't end with %
   val.startsWith("<") || val.startsWith("{") || val.startsWith("[")  // FALSE
   // → returns FALSE
   ```
5. **Result**: `isInputValid` returns `false` → `setNotPresent(comp, inValidInput)` → **RED text with tooltip "Syntax error..."**

### Why `isInputValid()` fails

The `isInputValid()` regex pattern `(@.+)|(=.+)|(%.+%)|(#.+)` expects input to be ONE of these formats:
- `@value` — literal value
- `=function()` — function call
- `%variable%` — runtime variable
- `#alias` — database/API alias

OR start with `<`, `{`, or `[` for JSON/XML/HTML payloads.

But `assertVariable` uses the format `key=value` (e.g., `%myVar%=expected`), which:
- Starts with `%` → matches `%.+` but NOT `%.+%` (doesn't end with `%`)
- Is NOT any other format

### The parameterization dimension

The user mentions "this is when ObjectName is General and Action is assertVariable" in context of parameterization. When this step is placed between `Start Param` and `End Param`, the Input column might also reference test data like `%variable%=Sheet:Column` or `%variable%=%TestData:Column%`. In the `Sheet:Column` case, the input contains a colon, which makes `isTestDataStep()` return `true`, and THEN the test fails because `isTestDataPresent()` tries to look up `%variable%` as a test data sheet name (it splits on `:` and gets `["%variable%", "Sheet", "Column"]` — since data[0] is `%variable%`, which is not a valid sheet name).

## THE ACTUAL FIX NEEDED

### Option A (Recommended): Special-case `assertVariable` in `InputRenderer`

In `InputRenderer.isInputValid()`, before the final `return false`, add a check: if the action is `assertVariable` and the input matches the `key=value` format, return true.

```java
private Boolean isInputValid(Object value, String objectName) {
    String val = Objects.toString(value, "").trim();
    if (objectName.matches("String Operations")) {
        return true;
    } else {
        if (val.matches("(@.+)|(=.+)|(%.+%)|(#.+)")) return true;
        else if (val.startsWith("<") || val.startsWith("{") || val.startsWith("[")) return true;
        // ADD: Accept assertVariable format (key=value)
        else if (val.matches(".*=.+")) return true;  // generic key=value pattern
        else return false;
    }
}
```

**More targeted version**: The fix should be aware of the specific action context. Since `assertVariable` expects `key=value`, accept any input with an `=` sign:

```java
else if (val.contains("=")) return true;  // assertVariable / key=value format
```

### Option B: Fix `isTestDataStep()` on `TestStep`

The regex `(?!(@|=|%)).+:.+` is too greedy. It matches any non-prefixed string containing a colon. For `assertVariable` format like `%variable%=Sheet:Column`, the fix could be:

```java
public Boolean isTestDataStep() {
    String input = getInput();
    if (input.startsWith("<") || input.startsWith("{") || input.startsWith("[")) return false;
    // Must look like "SheetName:ColumnName" — no = signs, not starting with @ or % or =
    if (input.matches("(?!(@|=|%|.*=)).+:.+")) return true;
    return false;
}
```

**However**, Option B would affect ALL code that calls `isTestDataStep()`, not just the renderer. Option A is safer and more localized.

### Option C: Add condition annotation to assertVariable

If the assertion format should be `key=value` where `key` comes from Condition and `value` from Input (like `assertVariableFromDataSheet` already does), then the fix would be to change the `@Action` annotation:

```java
@Action(
    object = ObjectType.GENERAL,
    desc = "Assert if Key:Value -> [<Data>] is valid",
    input = InputType.YES,
    condition = InputType.YES  // ADD THIS
)
public void assertVariable() { ... }
```

But this changes the action's semantics, which is more invasive.

## Recommended Fix Location

**File**: `IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/validation/InputRenderer.java`

**Method**: `isInputValid(Object value, String objectName)`

**Change**: Add a case for `key=value` format inputs. The most maintainable approach:

```java
// Accept key=value pattern used by assertVariable and similar actions
if (val.contains("=") && !val.startsWith("=")) {
    return true;
}
```

This should be added just before the final `return false` line.

## Data Flow Summary

```
IDE Table Cell
  ↓ (user edits Input column)
  ↓
InputRenderer.getTableCellRendererComponent()
  ↓
InputRenderer.render(comp, step, value)
  ├─ isEmpty() → setEmpty() if mandatory → RED
  ├─ isNotNeeded() → setNotPresent() → RED
  ├─ isTestDataStep() + !isTestDataPresent() → RED
  └─ isInputValid() → false → RED ◄── BUG IS HERE
  ↓
AbstractRenderer.setNotPresent(comp, inValidInput)
  ↓
Cell shown with RED foreground + tooltip "Syntax error..."
```

## Non-Obvious Design Decisions

1. **Validation uses real renderers, not separate validators**: The `TestCaseValidation` helper reuses the `AbstractRenderer` subclasses but runs them on a scratch `JLabel` instead of the live table cell. `errorState` is a mutable field on the renderer, saved/restored around recursive calls to handle reusable step validation.

2. **`isTestDataStep()` is purely heuristic**: The method uses a regex `(?!(@|=|%)).+:.+` to detect test data references. This regex has no awareness of action-specific input formats. Any input that contains a colon and doesn't start with `@`, `=`, or `%` is assumed to be a test data reference.

3. **Action annotations drive validation state**: The `InputType` enum (YES/NO/OPTIONAL) directly controls whether the IDE shows red markers. An action annotated with `input = InputType.YES` means the Input column MUST have a value that passes `isInputValid()`.

4. **No per-action input format validation**: There is no mechanism for an action to declare "my input format is `key=value`" or "my input is a JSON path." Every action's input is validated against the same hardcoded regex patterns in `isInputValid()`.

## Suggested Reading Order

1. **`IDE/.../validation/InputRenderer.java`** — The file with the bug; read `render()` and `isInputValid()` methods
2. **`Engine/.../commands/general/GeneralOperations.java`** — The `assertVariable()` method; read the annotation and its input format
3. **`ingenious-api/.../types/InputType.java`** — Understand YES/NO/OPTIONAL semantics
4. **`ingenious-api/.../annotation/Action.java`** — See how annotations are structured
5. **`IDE/.../validation/AbstractRenderer.java`** — Base rendering logic and error state tracking
6. **`IDE/.../validation/TestCaseValidation.java`** — How validation propagates through reusable references and caches results
7. **`Datalib/.../component/TestStep.java`** — The `isTestDataStep()` method that interacts with the rendering logic
