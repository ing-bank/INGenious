# API Methods Quick Reference

## API Contracts by Plugin Type

| Plugin Type | API Contract | Constructor Parameter |
|-------------|--------------|----------------------|
| Browser | `BrowserPluginApi` | `BrowserPluginApi gen` |
| Mobile | `MobilePluginApi` | `MobilePluginApi gen` |
| Webservice | `WebservicePluginApi` | `WebservicePluginApi gen` |
| Database | `DatabasePluginApi` | `DatabasePluginApi gen` |
| General | `CommandPluginApi` | `CommandPluginApi gen` |

## Common Methods (All Contracts)

### Test Data Access
```java
String data = gen.getData();
String action = gen.getAction();
String input = gen.getInput();
String condition = gen.getCondition();
String objectName = gen.getObjectName();
```

### Test Reporting
```java
TestCaseReportApi report = gen.getReport();
report.updateTestLog(action, "Message", Status.DONE);
report.updateTestLog(action, "Pass", Status.PASS);      // With screenshot
report.updateTestLog(action, "Fail", Status.FAIL);      // With screenshot
report.updateTestLog(action, "Pass", Status.PASSNS);    // No screenshot
report.updateTestLog(action, "Fail", Status.FAILNS);    // No screenshot
```

### Variable Management
```java
// Runtime variables
gen.addVar("%myVar%", "value");
String value = gen.getVar("%myVar%");

// Global variables
gen.addGlobalVar("%globalVar%", "value");
String globalValue = gen.getGlobalVar("%globalVar%");
```

## Browser Plugin API (BrowserPluginApi)

### Playwright Objects
```java
Page page = (Page) gen.getPage();
Locator locator = (Locator) gen.getLocator();

// Null safety
if (page == null) {
    report.updateTestLog(action, "Page not available", Status.FAIL);
    return;
}
```

### Data Sheet Access
```java
UserDataAccessApi userData = gen.getUserData();

// Read
String value = userData.getData("SheetName", "ColumnName");

// Write
userData.putData("SheetName", "ColumnName", "Value");
```

## Mobile Plugin API (MobilePluginApi)

### Appium Objects
```java
AppiumDriver driver = (AppiumDriver) gen.getDriver();
AndroidDriver androidDriver = (AndroidDriver) gen.getDriver();
IOSDriver iosDriver = (IOSDriver) gen.getDriver();
WebElement element = (WebElement) gen.getElement();
```

## Webservice Plugin API (WebservicePluginApi)

### HTTP Operations
```java
String url = gen.getData();
String requestBody = gen.getInput();

// Use Java HTTP client or library of choice
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(url))
    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
    .header("Content-Type", "application/json")
    .build();

HttpResponse<String> response = client.send(request, 
    HttpResponse.BodyHandlers.ofString());

gen.addVar("%responseBody%", response.body());
gen.addVar("%statusCode%", String.valueOf(response.statusCode()));
```

## Database Plugin API (DatabasePluginApi)

### SQL Execution
```java
String query = gen.getData();

// Variable substitution
String resolvedQuery = query
    .replace("%userId%", gen.getVar("%userId%"))
    .replace("%email%", gen.getVar("%email%"));

// Execute with JDBC
Connection conn = DriverManager.getConnection(jdbcUrl, "user", "password");
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery(resolvedQuery);

while (rs.next()) {
    String value = rs.getString("columnName");
    gen.addVar("%dbValue%", value);
}
```

## Status Values

```java
Status.PASS     // Pass with screenshot
Status.FAIL     // Fail with screenshot
Status.DONE     // Action completed
Status.PASSNS   // Pass without screenshot
Status.FAILNS   // Fail without screenshot
Status.DEBUG    // Debug message
Status.SKIP     // Action skipped
```

## Common Patterns

### Null Safety
```java
Object pageObj = gen.getPage();
if (pageObj == null) {
    Report.updateTestLog(Action, "Page not available", Status.FAIL);
    return;
}
Page page = (Page) pageObj;
```

### Error Handling
```java
try {
    // Action logic
    Report.updateTestLog(Action, "Success", Status.DONE);
} catch (SpecificException e) {
    Report.updateTestLog(Action, "Error: " + e.getMessage(), Status.FAIL);
} catch (Exception e) {
    Logger.getLogger(getClass().getName()).log(Level.OFF, null, e);
    Report.updateTestLog(Action, "Unexpected: " + e.getMessage(), Status.FAIL);
}
```

### Variable Substitution
```java
String input = gen.getInput();  // May be "%myVar%" or literal
if (input != null && input.startsWith("%") && input.endsWith("%")) {
    String resolved = gen.getVar(input);
    if (resolved != null) {
        input = resolved;
    }
}
```
