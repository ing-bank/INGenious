# Complete Constructor Pattern Reference

## Critical Requirement

**⚠️ ALWAYS use the COMPLETE constructor pattern - never simplify**

The constructor pattern shown below is the canonical implementation that MUST be followed for all plugin types. Every field must be initialized even if not immediately used.

## Why This Pattern Matters

1. **Future-proofs plugin** - Adding new actions won't require constructor changes
2. **Prevents NullPointerException** - All fields guaranteed initialized
3. **Framework consistency** - Matches INGenious conventions
4. **Enables all API features** - Report, variables, userData all available

## Required Initialization Sequence

**MANDATORY order:**
1. Store API contract: `this.gen = gen;`
2. Initialize ALL test data fields: `Data`, `Action`, `Input`, `Condition`, `ObjectName`
3. Initialize `Report` API
4. Initialize `userData` (if available in the API contract)
5. Cast Playwright/Appium objects once

## Browser Plugin Constructor

```java
package com.ing.plugin.browser;

import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.contract.BrowserPluginApi;
import com.ing.ingenious.api.contract.reports.TestCaseReportApi;
import com.ing.ingenious.api.types.ObjectType;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.status.Status;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.PlaywrightException;

public class BrowserTestPlugin {
    
    BrowserPluginApi gen;
    
    public String Data;
    public String Action;
    public String Input;
    public String Condition;
    public TestCaseReportApi Report;
    public String ObjectName;
    
    // Cast Playwright objects once in constructor
    public Page Page;
    public Locator Locator;

    public BrowserTestPlugin(BrowserPluginApi gen) {
        this.gen = gen;
        this.Data = gen.getData();
        this.Action = gen.getAction();
        this.Input = gen.getInput();
        this.Condition = gen.getCondition();
        this.Report = gen.getReport();
        this.ObjectName = gen.getObjectName();
        
        // Cast once for type safety and IDE autocomplete
        this.Page = (Page) gen.getPage();
        this.Locator = (Locator) gen.getLocator();
    }

    @Action(object = ObjectType.BROWSER, 
            desc = "Open the Url [<Data>] in the Browser", 
            input = InputType.YES)
    public void Open() {
        try {
            Page.navigate(Data);
            Report.updateTestLog(Action, "Opened " + Data, Status.DONE);
        } catch (PlaywrightException e) {
            Report.updateTestLog(Action, 
                "Error: " + e.getMessage(), 
                Status.FAIL);
        }
    }
}
```

## Mobile Plugin Constructor

```java
import com.ing.ingenious.api.contract.MobilePluginApi;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;

public class MobileTestPlugin {
    
    MobilePluginApi gen;
    
    public String Data;
    public String Action;
    public String Input;
    public String Condition;
    public TestCaseReportApi Report;
    public String ObjectName;
    
    public AppiumDriver Driver;

    public MobileTestPlugin(MobilePluginApi gen) {
        this.gen = gen;
        this.Data = gen.getData();
        this.Action = gen.getAction();
        this.Input = gen.getInput();
        this.Condition = gen.getCondition();
        this.Report = gen.getReport();
        this.ObjectName = gen.getObjectName();
        
        // Cast once
        this.Driver = (AppiumDriver) gen.getDriver();
    }
    
    // Action methods...
}
```

## Webservice Plugin Constructor

```java
import com.ing.ingenious.api.contract.WebservicePluginApi;

public class WebserviceTestPlugin {
    
    WebservicePluginApi gen;
    
    public String Data;
    public String Action;
    public String Input;
    public String Condition;
    public TestCaseReportApi Report;
    public String ObjectName;
    public String userData;

    public WebserviceTestPlugin(WebservicePluginApi gen) {
        this.gen = gen;
        this.Data = gen.getData();
        this.Action = gen.getAction();
        this.Input = gen.getInput();
        this.Condition = gen.getCondition();
        this.Report = gen.getReport();
        this.ObjectName = gen.getObjectName();
        this.userData = gen.getUserData();  // Available in webservice API
    }
    
    // Action methods...
}
```

## Database Plugin Constructor

```java
import com.ing.ingenious.api.contract.DatabasePluginApi;
import java.sql.Connection;
import java.sql.ResultSet;

public class DatabaseTestPlugin {
    
    DatabasePluginApi gen;
    
    public String Data;
    public String Action;
    public String Input;
    public String Condition;
    public TestCaseReportApi Report;
    public String ObjectName;
    
    public Connection Connection;
    public ResultSet ResultSet;

    public DatabaseTestPlugin(DatabasePluginApi gen) {
        this.gen = gen;
        this.Data = gen.getData();
        this.Action = gen.getAction();
        this.Input = gen.getInput();
        this.Condition = gen.getCondition();
        this.Report = gen.getReport();
        this.ObjectName = gen.getObjectName();
        
        // Cast database objects
        this.Connection = (Connection) gen.getConnection();
        this.ResultSet = (ResultSet) gen.getResultSet();
    }
    
    // Action methods...
}
```

## General/Command Plugin Constructor

```java
import com.ing.ingenious.api.contract.CommandPluginApi;

public class GeneralTestPlugin {
    
    CommandPluginApi gen;
    
    public String Data;
    public String Action;
    public String Input;
    public String Condition;
    public TestCaseReportApi Report;
    public String ObjectName;

    public GeneralTestPlugin(CommandPluginApi gen) {
        this.gen = gen;
        this.Data = gen.getData();
        this.Action = gen.getAction();
        this.Input = gen.getInput();
        this.Condition = gen.getCondition();
        this.Report = gen.getReport();
        this.ObjectName = gen.getObjectName();
    }
    
    // Action methods...
}
```

## Common Mistakes to Avoid

### ❌ WRONG - Incomplete Initialization

```java
// Missing fields - will break if you add actions that need them
public BrowserTestPlugin(BrowserPluginApi gen) {
    this.gen = gen;
    this.Page = (Page) gen.getPage();  // Only what you need now
}
```

**Why wrong:** Future actions will get NullPointerException for Data, Report, etc.

### ❌ WRONG - Selective Initialization

```java
// Only initializing fields you think you'll use
public BrowserTestPlugin(BrowserPluginApi gen) {
    this.gen = gen;
    this.Data = gen.getData();
    this.Page = (Page) gen.getPage();
    // Missing: Action, Input, Condition, Report, ObjectName
}
```

**Why wrong:** Makes plugin fragile and non-extensible.

### ❌ WRONG - Late Casting

```java
public void myAction() {
    // Casting on every method call - inefficient
    Page page = (Page) gen.getPage();
    page.navigate(Data);
}
```

**Why wrong:** Performance overhead, no type safety, no IDE autocomplete.

## ✅ CORRECT Pattern

Always follow the complete initialization shown in the examples above:
- Store API contract
- Initialize ALL data fields
- Initialize Report
- Initialize userData (if available)
- Cast and store typed objects once

This ensures your plugin is robust, extensible, and follows framework conventions.
