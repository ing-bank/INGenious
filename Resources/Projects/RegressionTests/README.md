# RegressionTests — Test Data scope reference regression

Exercises the **full sweep** that made every Test Data reference parser accept
`Sheet:Column` **and** `[Project] Sheet:Column` **and** `[Shared] Sheet:Column`
(braced or bare), routed through the single
[`TestDataToken`](../../Engine/src/main/java/com/ing/engine/execution/data/TestDataToken.java)
helper. See `TESTDATA_SCOPE_REMAINING_GAPS.md` at the repo root for the audit that found the
393 call sites (18 hand-migrated + 375 in `SyntheticDataGenerator.java`) fixed by
`SyntheticDataWriteBack` and `PreviousTestCaseWriteBack` below.

Runs **headless** (no browser / network / DB) and is deterministic.

## Scenario `TestDataScope`

| Test case | What it covers | Self-checks? |
|---|---|---|
| **AllReferenceForms** | String Operations (`Concat`, `ToUpper`, `GetLength`, `Substring`, `Trim`) resolving `{Sheet:Col}` / `{[Project] Sheet:Col}` / `{[Shared] Sheet:Col}` via `CommandControl.getDataSheetValue` → `TestDataToken`; whole-input refs (bare / braced / `[Project]` / `[Shared]`) via `DataProcessor` + `assertVariableFromDataSheet`. | ✅ `assertVariable` / `assertVariableFromDataSheet` — a regression turns steps **FAIL** |
| **WriteBack** | `General.storeVariableInDataSheet` write path (`TestDataToken.parse` instead of `Input.split(":")`). Its Input *is* the whole destination ref, so **bare `Sheet:Column` is canonical**; covers bare untagged, bare `[Project]`, and braced `{[Project] …}` (the last used to fail as sheet `"{DataScope"`). | ✅ writes then reads back with `assertVariableFromDataSheet` |
| **RenderTemplate** | `FileOperations.populateData` → `TestDataToken.resolveEmbeddedTokens` — the **same** shared resolver now used by webservice payloads/endpoints/headers, `RequestFulfill`, DB query text, `Switch` context values and MQ message bodies. If embedded-token substitution works here it works for all of them. | ⚠️ artifact `regtest-rendered.txt` |
| **DbConnectionConfig** | **Settings / config** can carry datasheet refs, and a single property value may **mix** literal text + `%runtimeVar%` + `{testdata}`. `Settings/Databases/regdb.properties` `connectionString = jdbc:regmark:{[Project] DataScope:DbConn}-%dbEnv%-{[Shared] RegShared:SVal}`; `General.verifyDbConnection` → `resolveAllVariables` (`handleDataSheetVariables`→`TestDataToken`, then `%var%`). | ⚠️ step FAILs by design (no JDBC driver); the composed marker `jdbc:regmark:proj-ok-LIVE-SHRVAL` in the error proves literal + `[Project]` + `%var%` + `[Shared]` all resolved |
| **SyntheticDataWriteBack** | `SyntheticDataGenerator` write path — the 375-site gap (`numerify`/`regexify`/`streetAddress`/… all ended with the same raw `strObj.split(":", 2)` → `userData.putData`), now `TestDataToken.parse`. Covers bare, braced, braced+`[Project]`, braced+`[Shared]` destinations. | ✅ `numerify("###")`/`regexify("[A-Z]{5}")` give a fixed-length result even though the value is random, so `String Operations.GetLength` + `assertVariable` proves the write landed in the *correct* cell without needing to predict Faker's output |
| **PreviousTestCaseWriteBack** | `General.storeDataFromPreviousTestCaseData` — **both** the source (`Condition`) and target (`Input`) sides used raw `split(":", 2)`. Covers bare/braced/`[Project]`/`[Shared]` on both sides in the same step. | ✅ writes a known literal (`PREVVAL`/`SHRVAL`), reads it back with `String Operations.Concat` + `assertVariable` against the exact expected string |

### Data

- Project: `TestData/DataScope.csv` (`PVal=PROJVAL`, `PLower=proj`, `PPadded=" pad "`, `Scratch`,
  `SrcVal=PREVVAL`, plus the `Dst*`/`Gen*` write-back columns the two newer test cases target).
- Shared: `<workspace>/Shared/SharedTestData/RegShared.csv` (`SVal=SHRVAL`, plus `DstShared`/
  `GenShared`).

## Run

Requires a **built** distribution (JDK 17) — the reference parsers live in
`ingenious-engine`. From a `Dist/release` that contains this project:

```bash
cd Dist/release
J='/c/Program Files/Java/jdk-17/bin/java'                     # or `java` if 17 is on PATH
CP='lib/*;lib/clib/*'
for tc in AllReferenceForms WriteBack RenderTemplate DbConnectionConfig \
          SyntheticDataWriteBack PreviousTestCaseWriteBack; do
  "$J" -cp "$CP" com.ing.engine.core.Control run "RegressionTests/TestDataScope/$tc"
done
```

or the bundled wrapper (runs all six + validates both artefacts + sets exit code):

```bash
Projects/RegressionTests/run-regression.sh        # from Dist/release
```

### No `Dist/release` handy? Run straight from a Maven build

The engine doesn't need to be packaged as a full distribution — `com.ing.engine.core.Control`
only needs three things next to the app-root (the directory containing `Projects/` and
`Shared/`, here `testProjects/`), none of which are checked into git:

1. **`plugins/`** — an empty directory is enough (`PluginLoader` just needs it to exist).
2. **`Configuration/`** — copy wholesale from `Resources/Configuration/` in this repo (report
   templates, the `.enc` key, etc.).
3. **`lib/`** — a flat directory containing `ingenious-engine-<version>.jar` (from
   `mvn -pl Engine package` after `mvn -pl Datalib install`) plus every dependency jar (from
   `mvn -pl Engine dependency:build-classpath`); `AppResourcePath.getEngineJarPath()` scans
   `<appRoot>/lib` for `ingenious-engine*.jar` to discover `@Action` methods, so it must be a
   real jar, not `target/classes`.

Once those three exist:

```bash
J='/c/Program Files/Java/jdk-17/bin/java'
cd testProjects   # the app-root: contains Projects/, Shared/, plugins/, Configuration/, lib/
"$J" -Xms64m -Xmx768m -Dfile.encoding=UTF-8 -cp "lib/*" \
  com.ing.engine.core.Control run "RegressionTests/TestDataScope/SyntheticDataWriteBack"
```

(On Windows, prefer PowerShell for this over Git Bash — Bash's automatic path mangling of a
long `;`-separated Windows classpath can make `-cp` fail to find the main class.)

### Expected — pass

```
TestDataScope:AllReferenceForms          | Status: PASS   (24/24 steps)
TestDataScope:WriteBack                  | Status: PASS   ( 7/7  steps)
TestDataScope:RenderTemplate             | Status: PASS   ( 3/3  steps)
TestDataScope:DbConnectionConfig         | Status: FAIL   (by design - no JDBC driver; see below)
TestDataScope:SyntheticDataWriteBack     | Status: PASS   (12/12 steps)
TestDataScope:PreviousTestCaseWriteBack  | Status: PASS   (12/12 steps)
REGRESSION: PASS
```

`RenderTemplate` writes `<working-dir>/regtest-rendered.txt`; with the sweep in place it must
contain exactly:

```
line1 PROJVAL | line2 PROJVAL | line3 SHRVAL
```

`DbConnectionConfig` step 2 FAILs on purpose (no JDBC driver on the classpath); the error must
read `No suitable driver found for jdbc:regmark:proj-ok-LIVE-SHRVAL`. The wrapper keys off that
marker, not the step status.

### Regression signatures

| Broken area | Symptom |
|---|---|
| `getDataSheetValue` / String Operations tag handling | `AllReferenceForms` steps 5–6, 9–10, 21–22 **FAIL** (`[Shared]` refs resolve empty) |
| `DataProcessor` whole-input scoped refs | `AllReferenceForms` steps 16–22 **FAIL** |
| `storeVariableInDataSheet` parse | `WriteBack` steps 6–7 **FAIL** (braced ref → sheet `"{DataScope"`); bare forms in steps 2–5 stay green either way |
| `TestDataToken.resolveEmbeddedTokens` (the 7 payload sites) | `regtest-rendered.txt` shows literal `line2 {[Project] DataScope:PVal} \| line3 {[Shared] RegShared:SVal}` |
| DB / Settings config resolution (`resolveAllVariables`) | `DbConnectionConfig` error message contains a literal `{[Project] …}` / `{[Shared] …}` / `%dbEnv%` fragment instead of `jdbc:regmark:proj-ok-LIVE-SHRVAL` |
| `SyntheticDataGenerator` write path (375 sites) | `SyntheticDataWriteBack` steps 4–6/7–9/10–12 **FAIL** (braced/tagged destination parses to the wrong sheet name, e.g. `"{DataScope"`, so the real cell is never written and `GetLength` throws or reads stale data) |
| `storeDataFromPreviousTestCaseData` source+target parse | `PreviousTestCaseWriteBack` steps 4–6/7–9/10–12 **FAIL** the same way, on both the read side (`Condition`) and the write side (`Input`) |

## Notes

- `WriteBack` rewrites `DataScope.csv`'s `Scratch` cell each run to the same value (idempotent).
  `SyntheticDataWriteBack` and `PreviousTestCaseWriteBack` likewise rewrite their `Dst*`/`Gen*`
  cells each run — harmless, the assertions don't depend on the previous run's values.
- A Chromium instance is launched and immediately closed (engine default); the steps
  themselves need no browser.
- Authoritative unit coverage for `resolveEmbeddedTokens` (incl. JSON-payload safety) is
  `Engine/src/test/java/com/ing/engine/execution/data/TestDataTokenTest.java`.
- `.project` is matched by the repo's `.gitignore` (Eclipse rule). Commit it explicitly, the
  way `Resources/Projects/Tutorial/.project` is tracked:
  `git add -f Resources/Projects/RegressionTests/.project`
- The `Shared/SharedTestData/RegShared.csv` sheet must sit at the workspace root
  (`<user.dir>/Shared/SharedTestData/`) - packaged from `Resources/Shared/`.
- **Out of scope for this project (needs a live target, not just headless data):** the other
  16 of the 393 fixed call sites — `Basic.java` (5), `Webservice.java` (5),
  `StructuredData.java` (3), `QueueOperations.java` (2), `CommonMethods.java` (1) — read from a
  live browser element, HTTP response, XML document, or queue message before writing to the
  data sheet, so they can't be exercised headlessly the way the `General`/`Synthetic Data`
  actions above can. They were fixed with the exact same `TestDataToken.parse` + null-guard
  mechanical replacement, verified by `mvn compile`/`test-compile`, and use the identical
  call-site pattern already proven correct here.
