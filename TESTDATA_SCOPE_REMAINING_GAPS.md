# Scoped Test Data — Remaining Gaps (write-destination parsing)

Follow-up audit triggered by `Webservice.java:1259-1260`. Re-checked **every** call site in
`Engine/src/main` that parses a Test Data write-destination (`Sheet:Column`) by hand, cross
-referenced against what `TestDataToken` already covers. **No code changed — list only.**

---

## The pattern being checked

All 17 sites below share the exact shape that `GeneralOperations.storeVariableInDataSheet`,
`GeneralOperations.storeInGlobalDataSheet` and `Database.storeDBValueinDataSheet` had **before**
they were migrated:

```java
if (strObj.matches(".*:.*")) {
    String sheetName = strObj.split(":", 2)[0];
    String columnName = strObj.split(":", 2)[1];
    userData.putData(sheetName, columnName, value);
    ...
}
```

Same consequences as the already-fixed sites:

| Input | Result |
|---|---|
| `Sheet:Column` (bare) | works |
| `[Project] Sheet:Column` / `[Shared] Sheet:Column` (bare) | works — `DataAccess.putData` strips the tag internally regardless of how the caller split the string |
| `{Sheet:Column}` (braced) | **broken** — sheet name becomes `"{Sheet"` → `TestDataNotFoundException` / silent DEBUG log |
| `{[Project] …}` / `{[Shared] …}` (braced, tagged) | **broken**, same reason |
| no colon at all | `ArrayIndexOutOfBoundsException` on some sites (uncaught in the outer `try`) |

The fix, mirroring the 3 already-migrated sites, is mechanical:

```java
String[] sheetDetail = TestDataToken.parse(strObj);
if (sheetDetail == null) {
    Report.updateTestLog(Action, "Incorrect input format; expected Sheet:Column", Status.DEBUG);
    return; // or the method's existing "invalid format" branch
}
userData.putData(sheetDetail[0], sheetDetail[1], value);
```

---

## Gap list — 17 actions across 6 files

### `commands/browser/Basic.java` (5)

| Action | Line(s) | Destination var |
|---|---|---|
| `storeElementTextinDataSheet` | 476–499 | `Input` |
| `storeElementInnerHTMLinDataSheet` | 550–573 | `Input` |
| `storeElementInnerTextinDataSheet` | 624–647 | `Input` |
| `storeElementInputValueinDataSheet` | 698–721 | `Input` |
| `storeElementCSSValueinVariable` | 802–825 | `Input` — **name says "Variable" but it writes to a datasheet**, same as the others |

### `commands/structuredData/StructuredData.java` (3)

| Action | Line(s) | Destination var | Notes |
|---|---|---|---|
| `storeJsonPathResultCountInDataSheet` | 783–806 | `Input` | |
| `storeJsonPathResultInDataSheet` | 911–934 | `Input` | |
| `storeXmlPathResultInDataSheet` | 1732–1756 | `Input` | |

### `commands/webservice/Webservice.java` (5)

| Action | Line(s) | Destination var |
|---|---|---|
| `storeJSONelementInDataSheet` | 544–567 | `Input` |
| `storeXMLelementInDataSheet` | 615–638 | `Input` |
| `storeResponseBodyInDataSheet` | 807–830 | `Input` |
| `storeJsonElementCountInDataSheet` | 1252–1275 | `Input` |
| `storeHeaderByNameInDatasheet` | 1526–1610 (split at ~1598) | `Input` |

*(This is the same finding already flagged as a "known gap" in `TESTDATA_SCOPE_CHANGE_SUMMARY.md`
— it was under-counted there as "5 actions"; it's still 5, now with exact line numbers and names.)*

### `commands/queue/QueueOperations.java` (2)

| Action | Line(s) | Destination var |
|---|---|---|
| `storeQueueXMLtagInDataSheet` | 495–518 | `Input` |
| `storeQueueJSONtagInDataSheet` | 749–772 | `Input` |

*(`QueueOperations.handleDataSheetVariables` — the embedded-token payload resolver — is already
migrated. These two are a different, unmigrated code path in the same file: whole-input write
destinations, not embedded substitution.)*

### `commands/mobile/CommonMethods.java` (1)

| Action | Line(s) | Destination var |
|---|---|---|
| `storeTextinDataSheet` | 315–338 | `Input` |

*(`CommonMethods.dragToAndDropElement`'s `Data.split(":", 2)` at line 79 is a `Page:Object`
element reference, not Test Data — out of scope.)*

### `commands/general/GeneralOperations.java` (1 action, 2 split sites) — **new finding**

| Action | Line(s) | Destination var |
|---|---|---|
| `storeDataFromPreviousTestCaseData` | 490–545 | **both** `Condition` (source, line 521) and `Input` (target, line 538) |

Reads a value from a *previous* test case's datasheet cell and either stores it to a `%var%` or
writes it into another sheet:column — both the source and the target reference are raw
`split(":", 2)`. Same braces/tag caveat as everything else here.

---

## Checked and found **not** to need a change

| Site | Why it's fine |
|---|---|
| `Database.storeResultInDataSheet` (line 431) | `Condition` is used as a **bare sheet name only** (no `:Column` — the column comes from the SQL result set metadata). `userData.putData(Condition, ...)` already routes through `DataAccess.putData`, which strips a `[Project]`/`[Shared]` tag internally regardless of how the caller obtained the sheet name. A tagged `Condition` (e.g. `[Shared] DbSheet`) should already work. |
| `Webservice.storeHeaderByNameInVariable`, `storeResponseCookiesInVariable` | Store to a `%variable%`, not a datasheet — no `Sheet:Column` parsing involved. |
| `mobile/AppiumDeviceCommands.java` (`Condition.split(":", 2)` at 1762, `Data.split(":", 3)` at 1910) | Scroll direction/attempts and `package:permission:grant\|revoke` — not Test Data. |
| `commands/kafka/KafkaOperations.java` (3 occurrences) | Commented-out dead code (`storeKafkaXMLtagInDataSheet` etc.) — not compiled, not active. |
| `commands/syntheticData/SyntheticDataGenerator.java` (819 occurrences) | Format/pattern strings for generated data (dates, IDs, …), unrelated to Test Data references. |
| `execution/data/DataProcessor.java`, `TestCaseRunner.java`, `MCPTools.java`, `GeneralOperations.storeVariableInDataSheet`/`storeInGlobalDataSheet`, `Database.storeDBValueinDataSheet`, `General.resolveVars`/`handleDataSheetVariables` | Already migrated to `TestDataToken` — re-verified current on disk. |

---

## Suggested order (if/when this gets picked up)

1. `GeneralOperations.storeDataFromPreviousTestCaseData` — two sites, one file, same pattern as
   the already-migrated actions in this file; lowest-risk, most consistent with prior work.
2. The 5 `Webservice.java` actions — same file already partly migrated
   (`handleDataSheetVariables`, `addHeader`), so the import and pattern are already established
   there.
3. `Basic.java` (5), `StructuredData.java` (3) — browser/API assertion-adjacent, moderate usage.
4. `QueueOperations.java` (2), `CommonMethods.java` (1) — lower usage surfaces.

Each is a drop-in replacement of the `if (x.matches(".*:.*")) { split → putData }` block with
`TestDataToken.parse(x)` + null-guard, exactly as done for the 3 already-migrated sites — no
behavioural change for any existing bare/`[Scope]`-bare usage, only adds braced + still-tagged
support and turns the no-colon crash into a clean log message.
