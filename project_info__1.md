# INGenious — `assertVariable` Action Deep Dive

## Summary

This report explains how the `assertVariable` action works in INGenious (v3.1.0), including its parameterization variants that accept datasheet values, runtime variables, and datasheet variable references. There are **three related actions**: `assertVariable`, `assertVariableFromDataSheet`, and the legacy `verifyVariable`. All live in `Engine/src/main/java/com/ing/engine/commands/general/GeneralOperations.java`.

---

## 1. `assertVariable` — Direct key=value assertion

**File:** `Engine/src/main/java/com/ing/engine/commands/general/GeneralOperations.java` (line 66)

**Java method signature:**
```java
@Action(
    object = ObjectType.GENERAL,
    desc = "Assert if Key:Value -> [<Data>] is valid",
    input = InputType.YES
)
public void assertVariable() throws RuntimeException
```

**StepMap.csv entry:**
```
assertvariable,Assert if Key:Value -> [<Data>] is valid,The [<Data>] should be valid
```

### How the method works (step by step)

1. Reads the **Data** column from the test step — this is the full string like `%myVar%=expectedValue` or `myVar=42`
2. Splits on `=` with a limit of 2 → `["%myVar%", "expectedValue"]` or `["myVar", "42"]`
3. For the **key** part (left side):
   - If it matches the pattern `%.+%`, calls `getVar(strTemp[0])` to resolve the actual runtime variable value
   - If it does NOT match `%...%`, uses it **as-is** (treats the literal string as the value)
4. Compares the resolved actual value (`strAns`) with the expected value (`strTemp[1]`)
   - **Equal** → logs `PASSNS` with message: `"Variable value matches with provided data <expected>"`
   - **Not equal** → logs `FAILNS` with message: `"Variable value is <actual> but expected value is <expected>"`
5. Wraps everything in a try/catch; on exception, throws `ForcedException("assertVariable", <message>)`

### What the Data column should contain

The **Data** column format is: `<variable_reference>=<expected_value>`

| Variable reference | Expected value | Behavior |
|---|---|---|
| `%myVar%` | `hello` | Resolves runtime variable `%myVar%`, compares with literal string `hello` |
| `%myVar%` | `%otherVar%` | Resolves runtime variable `%myVar%`, compares with literal **string** `%otherVar%` (does NOT resolve `%otherVar%`) |
| `myVar` | `hello` | Compares the literal string `myVar` with `hello` (no variable resolution on key side) |

### Execution pipeline detail

Before `assertVariable()` is called, the test framework processes the step's Input/Data through `DataProcessor.resolve()`:

1. `TestStepRunner.executeStep()` → calls `context.getControl().sync(step, subIter)`
2. `CommandControl.sync(Step, String)` calls `DataProcessor.resolve(curr.Input, context, subIter)`
3. `DataProcessor.resolve()` handles:
   - **`@` prefix** → strips it, treats the rest as a literal
   - **`%...%` patterns** → resolves runtime variables inline
   - **`SheetName:Column` patterns** → resolves datasheet values
   - **`#...`** → resolves global data
   - **`=...`** → evaluates as an expression via `FParser.eval()`
   - **`>...`** → evaluates as JavaScript via `FParser.evaljs()`

This means you can put **datasheet references** in the Data column, and by the time `assertVariable()` runs, the datasheet value is already resolved.

### Concrete test examples

| Step Data column | What happens |
|---|---|
| `@%myVar%=hello` | Data becomes `%myVar%=hello` (DataProcessor strips the `@`), then `assertVariable` splits on `=`, resolves `%myVar%` → actual value, compares with `hello` |
| `@%myVar%=Sheet1:Expected` | DataProcessor sees `@` prefix, strips it → `%myVar%=Sheet1:Expected`. The `Sheet1:Expected` is NOT resolved by DataProcessor because it's after the `@`-stripping. `assertVariable` compares `%myVar%` resolved value against the literal string `Sheet1:Expected` |
| `%myVar%=Sheet1:Expected` (no `@`) | DataProcessor tries to resolve the whole string. Since it contains `%...%`, it resolves `%myVar%` → actual value. The `=Sheet1:Expected` part remains. The resolved string becomes something like `actualValue=Sheet1:Expected`. Then `assertVariable` splits on `=`, gets `["actualValue", "Sheet1:Expected"]` — compares. The left side is now the resolved value, not a variable name. This is likely NOT what you want. |

---

## 2. `assertVariableFromDataSheet` — Two-column assertion

**File:** `Engine/src/main/java/com/ing/engine/commands/general/GeneralOperations.java` (line 84)

**Java method signature:**
```java
@Action(
    object = ObjectType.GENERAL,
    desc = "Assert if the variable value matches with given value from datasheet(variable:datasheet-> [<Data>])",
    input = InputType.YES,
    condition = InputType.YES
)
public void assertVariableFromDataSheet() throws RuntimeException
```

**StepMap.csv entry:**
```
assertvariablefromdatasheet,Assert if the variable value matches with given value from datasheet(variable:datasheet-> [<Data>]),The assertion of variable [<Data>] from given datasheet should be done
```

### How it differs from `assertVariable`

This version uses **two columns** of the test step:

| Column | Field | What it holds |
|---|---|---|
| **Condition** | `Condition` variable name to resolve (e.g., `%myVar%`) |
| **Data** | `Data` (or `Input`) | The expected value — resolved from a datasheet |

### How the method works

1. Calls `getVar(Condition)` — resolves the runtime variable named by the Condition column
2. Calls `Data` — the Data column value, which was **already resolved by the DataProcessor** before this method ran
3. Compares: if `getVar(Condition).equals(Data)`:
   - **Equal** → logs `DONE` with message: `"Variable is matched with the expected result"`
   - **Not equal** → logs `FAIL` with message: `"Variable is matched with the expected result"` — throws `ForcedException`
4. Note: `Condition` value is treated as a variable reference **only** if it matches `%...%`. But `getVar()` handles both forms (with or without `%`)

### Concrete test examples

| Condition column | Data column | What happens |
|---|---|---|
| `%actualUser%` | `Sheet1:ExpectedUser` | DataProcessor resolves `Sheet1:ExpectedUser` → e.g. `"John"`. Then `assertVariableFromDataSheet` resolves `%actualUser%` → runtime value, compares with `"John"` |
| `%actualUser%` | `@John` | DataProcessor strips the `@` → `"John"`. Then `getVar("%actualUser%")` resolves the runtime variable, compares with literal `"John"` |
| `actualUser` | `Sheet1:ExpectedUser` | `getVar("actualUser")` resolves the runtime variable (without `%`), compares with datasheet value |

### How to set up a test

In the INGenious IDE test step editor:

| Field | Value |
|---|---|
| Object | `General` |
| Action | `assertVariableFromDataSheet` |
| Condition | `%runtimeVariableName%` |
| Data | `SheetName:ColumnName` |
| Description | (your description) |

---

## 3. How parameterization works across both actions

The user mentioned three parameterization modes. Here is how each maps:

### 3a. Datasheet parameterization

**What it means:** Using a datasheet column to supply the expected value.

**How it works for `assertVariable`:**
- Put the expected value in the Data column as a datasheet reference: `%actualVar%=Sheet1:ExpectedColumn`
- Wait — this is **tricky**. The DataProcessor runs **before** `assertVariable()`. If the whole Data string is `%actualVar%=Sheet1:ExpectedColumn`, the DataProcessor sees `%actualVar%` and tries to resolve it. The resolved value might not be what you want.
- **Recommended approach:** Prefix with `@` to tell DataProcessor to treat it as a literal: `@%actualVar%=Sheet1:ExpectedColumn`
- But then `Sheet1:ExpectedColumn` won't be resolved because the `@` prevents resolution of the whole string.
- **Best approach:** Use `assertVariableFromDataSheet` instead, which cleanly separates the variable reference (Condition column) from the datasheet-resolved expected value (Data column).

### 3b. Runtime variable parameterization

**What it means:** The variable name itself is resolved from a runtime variable.

**How it works:** In `assertVariable()`:
```java
String strAns = strTemp[0].matches("%.+%") ? getVar(strTemp[0]) : strTemp[0];
```
If the left side of the `=` in Data matches `%...%`, it calls `getVar()` to resolve the runtime variable. So `%myVar%=expected` resolves the **key** side as a runtime variable and compares its value to the literal `expected`.

### 3c. Datasheet variable reference

**What it means:** The variable name (Condition column) is resolved from a datasheet.

**How it works:** The Condition column can also hold `%...%` references, or it can be a datasheet reference like `SheetName:ColumnName`. But note: `getVar()` in `assertVariableFromDataSheet` only resolves runtime variables and user-defined data. For datasheet resolution on the Condition column:
- The DataProcessor resolves the **Data** column from datasheets
- The **Condition** column is NOT automatically resolved by DataProcessor before the method runs — it's passed as-is
- If your Condition contains `SheetName:ColumnName`, you'd need to manually resolve it via `getDatasheet()` from inside the command
- But the existing `assertVariableFromDataSheet` code does NOT call `getDatasheet()` on the Condition — it calls `getVar()`

---

## 4. Variable resolution chain (how `getVar` works)

Defined in `CommandControl.java` (line ~120-155):

1. `getVar(String key)` calls `getDynamicValue(key)`
2. `getDynamicValue()`:
   - First checks `runTimeVars` HashMap (runtime variables set via `addVar()`)
   - If not found, strips `%` signs if present, then calls `getUserDefinedData(key)` 
   - `getUserDefinedData()` looks up the project's user-defined settings properties
3. If the variable still isn't found → returns empty string `""` and logs a WARNING

Key methods in `Command.java`:
- `addVar(key, val)` → stores in runtime map
- `addGlobalVar(key, val)` → stores in project's user-defined settings and persists to disk
- `getDatasheet(key)` → looks up `{SheetName:ColumnName}` in test data
- `resolveAllRuntimeVars(str)` → replaces every `%var%` in a string with its resolved value
- `isVarExist(key)` → checks if a variable exists without returning empty string

---

## 5. How to test `assertVariable`

### Test 1: Basic variable assertion

Setup:
1. Use `AddVar` action (Object: General) to create: Condition=`%testVar%`, Data=`hello`
2. Then `assertVariable` (Object: General) with Data=`@%testVar%=hello`
3. Expected: PASSNS — "Variable value matches with provided data hello"

### Test 2: Failed assertion

Setup:
1. Use `AddVar` action to create: Condition=`%testVar%`, Data=`hello`
2. Then `assertVariable` with Data=`@%testVar%=world`
3. Expected: FAILNS — "Variable value is hello but expected value is world"

### Test 3: Using `assertVariableFromDataSheet` with a datasheet

Setup:
1. Create a datasheet (e.g. "TestData") with a column "ExpectedValue" containing "world"
2. Use `AddVar` to set `%myVar%` = `hello`
3. Use `assertVariableFromDataSheet` with Condition=`%myVar%`, Data=`TestData:ExpectedValue`
4. Since `hello` != `world`, expected: FAIL with exception

### Test 4: Using runtime variable as the expected value

Setup:
1. Use `AddVar` to set `%expectedVar%` = `someValue`
2. Use `AddVar` to set `%actualVar%` = `someValue`
3. Use `assertVariable` with Data=`@%actualVar%=someValue` 
4. Expected: PASSNS

Note: You **cannot** do `%actualVar%=%expectedVar%` because `assertVariable` treats the right side of `=` as a literal string, not as a variable reference.

---

## 6. The `verifyVariable` legacy action

There's also a legacy `verifyVariable` in the same file (line ~210-225) annotated with `object = ObjectType.MOBILE`. It works identically to `assertVariable` but uses `PASS`/`FAIL` statuses instead of `PASSNS`/`FAILNS` and does not throw an exception on failure.

---

## 7. Key files reference

| File | Role |
|---|---|
| `Engine/.../commands/general/GeneralOperations.java` | Contains `assertVariable()` (line 66), `assertVariableFromDataSheet()` (line 84), `verifyVariable()` (line 210) |
| `Engine/.../core/CommandControl.java` | Contains `getVar()`, `getDynamicValue()`, `addVar()`, `getRuntimeVar()`, `resolveAllRuntimeVars()`, `isVarExist()`, `runTimeVars` HashMap |
| `Engine/.../execution/data/DataProcessor.java` | Resolves `%...%`, `@`, `=`, `>`, `SheetName:Column`, `#...` before step execution |
| `Engine/.../execution/run/TestStepRunner.java` | Orchestrates step execution; calls `DataProcessor.resolve()` then `sync()` |
| `Engine/.../commands/browser/Command.java` | Base class exposing `getVar()`, `addVar()`, `addGlobalVar()`, `getDatasheet()`, `resolveAllRuntimeVars()` |
| `ingenious-api/.../annotation/Action.java` | `@Action` annotation definition with `input`, `condition`, `object`, `desc` attributes |
| `Resources/Configuration/StepMap.csv` | Maps action names to descriptions for the IDE |
