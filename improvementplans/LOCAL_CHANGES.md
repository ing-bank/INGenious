# Changes

**Date:** February 14, 2026  
**Branch:** `main`  
**Remote:** `origin/main` → https://github.com/ghoshasish99/INGenious_LT.git  
**Last remote commit:** `611fb38` — _feat: Add Lambda Test cloud execution support_

---

## Summary

| Category | Count |
|----------|-------|
| Modified files | 16 |
| New files (untracked) | 76 |
| Deleted files | 1 |
| **Total** | **93** |

---

## 1. Modified Files

### 1.1 Build Configuration

| File | Change |
|------|--------|
| `pom.xml` | Pinned `LATEST` versions to fixed values (`playwright` → 1.58.0, `extentreport` → 5.1.2, `playwright.axe` → 4.11.1, `appium` → 10.0.0, `javafaker` → 1.0.2, `kafka-clients` → 3.9.0, `avro` → 1.12.0, `kafka-avro-serializer` → 7.8.0, `ibmmq` → 9.4.5.0). Added `mockito-core` 5.14.2 and `assertj-core` 3.26.3 to `<dependencyManagement>`. Added JaCoCo 0.8.12 plugin. Added OWASP dependency-check-maven 11.1.1 plugin. Centralized `maven-compiler-plugin` in `<pluginManagement>`. Added `<flatlaf.version>3.5.4</flatlaf.version>` property. |
| `Datalib/pom.xml` | Added `mockito-core` and `assertj-core` test dependencies. Removed duplicate compiler plugin config (now inherited). |
| `Engine/pom.xml` | Added `mockito-core` and `assertj-core` test dependencies. Removed Appium `java-client` dependency. Pinned `ojdbc11` to 23.26.1.0.0 (was `LATEST`). |
| `StoryWriter/pom.xml` | Added `testng`, `mockito-core`, and `assertj-core` test dependencies. |
| `TestData - Csv/pom.xml` | Added `testng`, `mockito-core`, and `assertj-core` test dependencies. Removed duplicate compiler plugin config. |
| `IDE/pom.xml` | Removed duplicate compiler plugin config (now inherited from parent). Added `com.formdev:flatlaf:${flatlaf.version}` (3.5.4) dependency for modern flat Look & Feel. |
| `Common/pom.xml` | Removed duplicate compiler plugin config (now inherited from parent). |
| `Resources/Engine/pom.xml` | Pinned `LATEST` versions to fixed values (same as parent pom.xml). |
| `Dist/pom.xml` | Added `unix-permissions` Maven profile with `maven-antrun-plugin` 3.1.0 — runs `chmod 755` on `Run.command` during `install` phase. Activated only on non-Windows OS (`<os><family>!windows</family></os>`). Ensures `Dist/release/Run.command` is executable after build without manual `chmod`. |
| `Resources/Run.command` | Set git executable bit (`100644` → `100755`) so the file is executable after clone/pull without manual `chmod`. |

### 1.2 CI/CD

| File | Change |
|------|--------|
| `.github/workflows/maven.yml` | Added: Publish Test Results step (EnricoMi/publish-unit-test-result-action@v2), Upload JaCoCo Coverage Reports artifact, Upload Surefire Reports artifact. |

### 1.3 Source Code Fixes

| File | Change |
|------|--------|
| `Engine/src/main/java/com/ing/engine/core/Task.java` | Added `isLocalExecution()` check to `closePlaywrightInstance()` call in iteration loop (line 79) and in `closePlaywrightDriver()` (line 190). Prevents closing browser on grid/cloud executions. |
| `Engine/src/main/java/com/ing/engine/drivers/PlaywrightDriverCreation.java` | Reordered `closeBrowser()`: now calls `page.close()` before `closeBrowserContext()` (was the reverse). |
| `IDE/src/main/java/com/ing/ide/main/Main.java` | **UI Modernization**: Replaced Nimbus L&F with FlatLaf 3.5.4. Removed `setUpUI()`, `tweakNimbusUI()`, `tweakNimbusDarkUI()`, and `FillPainter1` inner class (~320 lines of Nimbus-specific painters). Added `setUpFlatLafUI()`, `applyCustomColors()`, and `registerCustomFont()`. Preserves all app-specific UIManager keys (`control`, `execTableColor`, `text`, `Table.font`, `TableMenu.font`). ING Orange (#FF6200) accent theming via native FlatLaf properties. |

### 1.4 Deleted Files

| File | Reason |
|------|--------|
| `CODE_CHANGES_SUMMARY.md` | Moved to `improvementplans/` folder. |

---

## 2. New Files — Unit Tests (72 test classes, 973 total tests)

### 2.1 Datalib Module — Model Tests (9 files)

| File | Tests |
|------|-------|
| `Datalib/src/test/java/com/ing/datalib/model/AttributeTest.java` | Factory method, getters, JSON serialization |
| `Datalib/src/test/java/com/ing/datalib/model/AttributesTest.java` | add, find, update, contains, stream logic |
| `Datalib/src/test/java/com/ing/datalib/model/DataTest.java` | List wrapper operations |
| `Datalib/src/test/java/com/ing/datalib/model/DataItemTest.java` | Constructor, getters, tag association |
| `Datalib/src/test/java/com/ing/datalib/model/MetaTest.java` | Constructor, attribute/tag management |
| `Datalib/src/test/java/com/ing/datalib/model/MetaListTest.java` | filter, findByTypeName, collection ops |
| `Datalib/src/test/java/com/ing/datalib/model/ProjectInfoTest.java` | Getters, nested object graph |
| `Datalib/src/test/java/com/ing/datalib/model/TagTest.java` | Value getter/setter |
| `Datalib/src/test/java/com/ing/datalib/model/TagsTest.java` | Collection operations |

### 2.2 Datalib Module — OR Tests (2 files)

| File | Tests |
|------|-------|
| `Datalib/src/test/java/com/ing/datalib/or/common/ORAttributeTest.java` | Constructor, clone, getters/setters |
| `Datalib/src/test/java/com/ing/datalib/or/common/ObjectGroupTest.java` | TreeNode operations, child management |

### 2.3 Datalib Module — Component Tests (6 files)

| File | Tests |
|------|-------|
| `Datalib/src/test/java/com/ing/datalib/component/ExecutionStepTest.java` | HEADERS enum, data field get/set |
| `Datalib/src/test/java/com/ing/datalib/component/TestStepTest.java` | HEADERS enum, step data parsing |
| `Datalib/src/test/java/com/ing/datalib/component/ScenarioTest.java` | Test case management, child node logic |
| `Datalib/src/test/java/com/ing/datalib/component/TestCaseTest.java` | Step ordering, save listeners |
| `Datalib/src/test/java/com/ing/datalib/component/ReleaseTest.java` | Test set management |
| `Datalib/src/test/java/com/ing/datalib/component/TestSetTest.java` | Execution step management |

### 2.4 Datalib Module — Settings Tests (17 files)

| File | Tests |
|------|-------|
| `Datalib/src/test/java/com/ing/datalib/settings/RunSettingsTest.java` | Typed getters (string→int, string→bool), defaults |
| `Datalib/src/test/java/com/ing/datalib/settings/ExecutionSettingsTest.java` | Composition, getter delegation |
| `Datalib/src/test/java/com/ing/datalib/settings/AbstractPropSettingsTest.java` | Load/save properties files (temp dir) |
| `Datalib/src/test/java/com/ing/datalib/settings/PropUtilsTest.java` | loadProperties, saveProperties (temp files) |
| `Datalib/src/test/java/com/ing/datalib/settings/CapabilitiesTest.java` | Per-browser .properties loading |
| `Datalib/src/test/java/com/ing/datalib/settings/ContextOptionsTest.java` | Named option set CRUD |
| `Datalib/src/test/java/com/ing/datalib/settings/DBPropertiesTest.java` | Multi-DB property management |
| `Datalib/src/test/java/com/ing/datalib/settings/DriverPropertiesTest.java` | API config loading |
| `Datalib/src/test/java/com/ing/datalib/settings/EmulatorsTest.java` | JSON load/save with Jackson |
| `Datalib/src/test/java/com/ing/datalib/settings/MailSettingsTest.java` | Default values, load/save |
| `Datalib/src/test/java/com/ing/datalib/settings/TestMgmtModuleTest.java` | JSON module definition parsing |
| `Datalib/src/test/java/com/ing/datalib/settings/LambdaTestCapsTest.java` | Property defaults, load/save |
| `Datalib/src/test/java/com/ing/datalib/settings/UserDefinedSettingsTest.java` | Custom property handling |
| `Datalib/src/test/java/com/ing/datalib/settings/ExtentReportSettingsTest.java` | Report config defaults |
| `Datalib/src/test/java/com/ing/datalib/settings/KafkaSSLConfigurationsTest.java` | Kafka SSL config |
| `Datalib/src/test/java/com/ing/datalib/settings/ReportPortalSettingsTest.java` | ReportPortal defaults |

### 2.5 Datalib Module — Utility Tests (1 file)

| File | Tests |
|------|-------|
| `Datalib/src/test/java/com/ing/datalib/util/data/LinkedPropertiesTest.java` | Insertion-order properties |

### 2.6 Engine Module — Execution Data Tests (7 files)

| File | Tests |
|------|-------|
| `Engine/src/test/java/com/ing/engine/execution/data/ParameterTest.java` | resolveMaxIter, resolveStartIter — regex parsing |
| `Engine/src/test/java/com/ing/engine/execution/data/DataIteratorTest.java` | Iteration counting, max computation |
| `Engine/src/test/java/com/ing/engine/execution/data/StepSetTest.java` | Loop from/to, counter increment, break flag |
| `Engine/src/test/java/com/ing/engine/execution/data/DataProcessorStaticTest.java` | trimFirst, isInputPatternDynamic, isInputPatternDataSheet |
| `Engine/src/test/java/com/ing/engine/execution/data/DataProcessorResolveTest.java` | resolve(raw, context, subIter) with mocked runner |
| `Engine/src/test/java/com/ing/engine/execution/data/DataAccessTest.java` | getData, putData with mocked context |
| `Engine/src/test/java/com/ing/engine/execution/data/DataAccessInternalTest.java` | Iteration helpers with mocked TestDataModel |

### 2.7 Engine Module — Command Tests (10 files)

| File | Tests |
|------|-------|
| `Engine/src/test/java/com/ing/engine/commands/browser/DynamicObjectTest.java` | setProperty map manipulation |
| `Engine/src/test/java/com/ing/engine/commands/browser/PerformanceTest.java` | escapeName via reflection + DataProvider |
| `Engine/src/test/java/com/ing/engine/commands/browser/RequestFulfillTest.java` | Static Command maps (mockEndPoints, headers, etc.) |
| `Engine/src/test/java/com/ing/engine/commands/browser/CookiesTest.java` | storeCookiesInVariable, clearCookies |
| `Engine/src/test/java/com/ing/engine/commands/browser/StorageStateTest.java` | BrowserContext delegation |
| `Engine/src/test/java/com/ing/engine/commands/stringOperations/StringOpsStaticTest.java` | isNumeric, countCharOccurrences |
| `Engine/src/test/java/com/ing/engine/commands/stringOperations/StringOperationsActionTest.java` | @Action methods with mocked CommandControl |
| `Engine/src/test/java/com/ing/engine/commands/webservice/WebserviceRequestMethodTest.java` | RequestMethod enum |
| `Engine/src/test/java/com/ing/engine/commands/webservice/WebserviceJsonPathTest.java` | JSONPath extraction |
| `Engine/src/test/java/com/ing/engine/commands/webservice/WebserviceXPathTest.java` | XPath extraction |
| `Engine/src/test/java/com/ing/engine/commands/webservice/WebserviceHttpTest.java` | HTTP call construction |

### 2.8 Engine Module — Driver Tests (4 files)

| File | Tests |
|------|-------|
| `Engine/src/test/java/com/ing/engine/drivers/BrowserEnumTest.java` | Browser enum fromString, getValuesAsList |
| `Engine/src/test/java/com/ing/engine/drivers/PlaywrightDriverFactoryTest.java` | getPropertyValueAsDesiredType, viewport, geolocation |
| `Engine/src/test/java/com/ing/engine/drivers/AutomationObjectLocatorTest.java` | FindType, chainLocatorMapping, getRuntimeValue |
| `Engine/src/test/java/com/ing/engine/drivers/ChromeEmulatorsExpandedTest.java` | getPrefLocation, JSON round-trip |

### 2.9 Engine Module — Core & Support Tests (6 files)

| File | Tests |
|------|-------|
| `Engine/src/test/java/com/ing/engine/support/StatusTest.java` | Enum toString, getValue, all variants |
| `Engine/src/test/java/com/ing/engine/support/StepTest.java` | create() factory methods |
| `Engine/src/test/java/com/ing/engine/support/AnnontationUtilTest.java` | detect() with MockedStatic |
| `Engine/src/test/java/com/ing/engine/constants/SystemDefaultsTest.java` | Debug flag, wait times |
| `Engine/src/test/java/com/ing/engine/constants/AppResourcePathTest.java` | ~30 static path construction methods |
| `Engine/src/test/java/com/ing/engine/util/data/KeyMapExpandedTest.java` | Nested vars, edge cases, missing keys |

### 2.10 Engine Module — Reporting Tests (3 files)

| File | Tests |
|------|-------|
| `Engine/src/test/java/com/ing/engine/reporting/sync/BasicHttpClientMockTest.java` | Mocked HTTP; auth, proxy, SSL config |
| `Engine/src/test/java/com/ing/engine/reporting/sync/DefectModulesTest.java` | getDecoded, checkServer logic |
| `Engine/src/test/java/com/ing/engine/reporting/sync/UnknownSyncTest.java` | No-op contract verification |

### 2.11 Engine Module — Core Tests (3 files)

| File | Tests |
|------|-------|
| `Engine/src/test/java/com/ing/engine/core/RunContextTest.java` | Context data management |
| `Engine/src/test/java/com/ing/engine/core/ThreadPoolTest.java` | Error handling in afterExecute |
| `Engine/src/test/java/com/ing/engine/core/TMIntegrationTest.java` | Factory logic, decrypt method |

### 2.12 StoryWriter Module (8 files)

| File | Tests |
|------|-------|
| `StoryWriter/src/test/java/com/ing/storywriter/util/ValidatorTest.java` | isValidName — valid/invalid filenames |
| `StoryWriter/src/test/java/com/ing/storywriter/bdd/data/DSTest.java` | update, updateV, step type constants |
| `StoryWriter/src/test/java/com/ing/storywriter/bdd/data/StoryTest.java` | toJSON, update, string parsing |
| `StoryWriter/src/test/java/com/ing/storywriter/util/ToolsDateTest.java` | today, after(days), toDate, getMillisNow |
| `StoryWriter/src/test/java/com/ing/storywriter/util/UtilityTest.java` | isEmpty, getValue, getDays, date formatting |
| `StoryWriter/src/test/java/com/ing/storywriter/util/ToolsFileTest.java` | readFile, writeFile with temp files |
| `StoryWriter/src/test/java/com/ing/storywriter/bdd/data/StoryParserTest.java` | Parse .feature files |
| `StoryWriter/src/test/java/com/ing/storywriter/bdd/data/BDDProjectTest.java` | Load/save BDD project JSON |

### 2.13 TestData-Csv Module (2 files)

| File | Tests |
|------|-------|
| `TestData - Csv/src/test/java/com/ing/testdata/csv/CsvGlobalDataTest.java` | Global data model operations |
| `TestData - Csv/src/test/java/com/ing/testdata/csv/CsvTestDataTest.java` | Sheet data model operations |

---

## 3. New Files — Documentation (4 files)

| File | Description |
|------|-------------|
| `improvementplans/CODE_CHANGES_SUMMARY.md` | Summary of all code changes (moved from root) |
| `improvementplans/MODERNIZATION_PLAN.md` | Modernization plan |
| `improvementplans/TEST_IMPROVEMENT_PLAN.md` | Test improvement plan (4 sprints, all completed) |
| `improvementplans/UI_MODERNIZATION_PLAN.md` | UI modernization recommendation — FlatLaf migration plan |

---

## Test Results Summary

| Module | Tests | Failures | Errors | Skipped |
|--------|-------|----------|--------|---------|
| StoryWriter | 92 | 0 | 0 | 0 |
| Datalib | 458 | 0 | 0 | 0 |
| TestData-Csv | 17 | 0 | 0 | 0 |
| Engine | 406 | 0 | 0 | 0 |
| **Total** | **973** | **0** | **0** | **0** |

**BUILD SUCCESS**
