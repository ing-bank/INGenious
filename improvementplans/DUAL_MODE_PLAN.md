# INGenious Dual-Mode Strategy: Loved by BAs and Developers

**Created:** February 16, 2026  
**Goal:** Make INGenious equally appealing to Business Analysts (GUI-first) and Developers (Code-first)  
**Principle:** One source of truth, multiple interfaces

---

## Executive Summary

The keyword-driven approach excels at democratizing test automation but creates friction for developers who want:
- IDE autocomplete and refactoring
- Debuggability with breakpoints
- Type safety and compile-time checks
- Git-friendly diffs and merge resolution
- Extensibility through code composition

**Solution:** Implement bidirectional synchronization between CSV/GUI and generated Java code, giving each persona their preferred interface while maintaining a single canonical source.

---

## Architecture Vision

```
┌─────────────────────────────────────────────────────────────────┐
│                     INGenious IDE (Swing)                       │
├────────────────────────┬────────────────────────────────────────┤
│   BA Mode (Default)    │         Developer Mode                 │
│                        │                                        │
│  ┌──────────────────┐  │  ┌──────────────────────────────────┐  │
│  │ Visual Test      │  │  │ Fluent Java DSL Editor           │  │
│  │ Designer (Grid)  │◄─┼──┤                                  │  │
│  │                  │  │  │ test("Login Flow")               │  │
│  │ Step │ Action    │──┼─►│   .open(loginPage)               │  │
│  │ ─────┼─────────  │  │  │   .type(username, "user")        │  │
│  │ 1    │ Click     │  │  │   .type(password, "pass")        │  │
│  │ 2    │ Fill      │  │  │   .click(submitBtn)              │  │
│  └──────────────────┘  │  │   .assertVisible(dashboard);     │  │
│                        │  └──────────────────────────────────┘  │
│  ┌──────────────────┐  │                                        │
│  │ Natural Language │  │  ┌──────────────────────────────────┐  │
│  │ Step Suggestions │  │  │ Code → CSV Sync                  │  │
│  │                  │  │  │ (Reverse engineer for BA view)   │  │
│  └──────────────────┘  │  └──────────────────────────────────┘  │
└────────────────────────┴────────────────────────────────────────┘
                              │
                              ▼
              ┌───────────────────────────────┐
              │   Unified Test Repository     │
              │   (CSV as source OR Java)     │
              │                               │
              │   Project Settings:           │
              │   source_mode: CSV | JAVA     │
              └───────────────────────────────┘
```

---

## Phase 1 — Fluent Java DSL (Week 1-3)

**Goal:** Allow developers to write tests in Java using a fluent API that mirrors the CSV structure.

### 1.1 Core DSL Classes

Create `Engine/src/main/java/com/ing/engine/dsl/`:

```java
// TestBuilder.java
public class TestBuilder {
    private final String testName;
    private final List<StepDefinition> steps = new ArrayList<>();
    
    public static TestBuilder test(String name) {
        return new TestBuilder(name);
    }
    
    public TestBuilder on(String objectName) {
        return new StepContext(this, objectName);
    }
    
    public TestBuilder on(PageObject page, String element) {
        return on(page.getName() + "." + element);
    }
    
    // ... fluent methods: click(), fill(), select(), assertText(), etc.
    
    public void execute(TestContext context) {
        // Convert to internal Step objects and run
    }
    
    public String toCsv() {
        // Export as CSV for BA review
    }
}
```

```java
// Usage Example
public class LoginTests {
    @Test
    public void successfulLogin() {
        test("Successful Login")
            .on(LoginPage.USERNAME).click().fill("testuser")
            .on(LoginPage.PASSWORD).fill("secret123")
            .on(LoginPage.SUBMIT).click()
            .on(DashboardPage.WELCOME_MSG).assertContains("Welcome")
            .execute();
    }
}
```

### 1.2 Page Object Generator

Auto-generate Java Page Object classes from Object Repository:

```java
// Generated: LoginPage.java
public class LoginPage {
    public static final String PAGE = "Login";
    public static final String USERNAME = "Username";
    public static final String PASSWORD = "Password";
    public static final String SUBMIT = "Submit Button";
    public static final String ERROR_MSG = "Error Message";
}
```

**IDE Menu:** `Tools → Generate Page Objects` → Creates Java constants for all OR entries.

### 1.3 Action Method Mapping

Map CSV Action names to fluent methods:

| CSV Action | DSL Method | Notes |
|------------|------------|-------|
| `Click` | `.click()` | |
| `Fill` | `.fill(value)` | |
| `SelectSingleByText` | `.select(text)` | |
| `AssertElementText` | `.assertText(expected)` | |
| `WaitForElement` | `.waitFor()` | |
| `ExecuteJS` | `.executeScript(js)` | |
| `StoreVariable` | `.storeAs(varName)` | |
| `CallReusable` | `.include(reusableName)` | |

---

## Phase 2 — Bidirectional Sync (Week 3-5)

### 2.1 CSV → Java Generation

**IDE Feature:** Right-click StepDefinition CSV → "Generate Java Test"

```java
// Input: User fills up personal details.csv
// Output: UserFillsUpPersonalDetails.java

@INGeniousTest(source = "User fills up personal details.csv")
public class UserFillsUpPersonalDetails extends BaseTest {
    
    @Test
    public void execute() {
        test("User fills up personal details")
            .on("Contact Us", "First name").click().fill("John")
            .on("Contact Us", "Last name").click().fill("Doe")
            .on("Contact Us", "Email Address").click().fill("John.Doe@email.com")
            .on("Contact Us", "Country").select("Austria")
            .execute(context);
    }
}
```

### 2.2 Java → CSV Sync

When developer edits Java, sync back to CSV for BA visibility:

```java
// Annotation-driven sync
@INGeniousTest(
    source = "User fills up personal details.csv",
    syncMode = SyncMode.JAVA_TO_CSV  // or CSV_TO_JAVA, BIDIRECTIONAL
)
```

**Build Plugin:** `mvn ingenious:sync` scans annotated tests and updates CSVs.

### 2.3 Conflict Resolution UI

When both CSV and Java are modified:

```
┌────────────────────────────────────────────────────────────┐
│  Sync Conflict Detected                                    │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  CSV (modified by BA):          Java (modified by Dev):   │
│  ┌────────────────────────┐    ┌────────────────────────┐ │
│  │ Step 3: Fill "Jane"    │    │ .fill("Jonathan")      │ │
│  └────────────────────────┘    └────────────────────────┘ │
│                                                            │
│  [Keep CSV]  [Keep Java]  [Merge Manually]                │
└────────────────────────────────────────────────────────────┘
```

---

## Phase 3 — Enhanced BA Experience (Week 5-7)

### 3.1 Natural Language Step Input

Let BAs type in natural language, auto-suggest matching actions:

```
┌─────────────────────────────────────────────────────────────┐
│ Type what you want to do...                                 │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ click the login button                                  │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ Suggestions:                                                │
│ ├─ Click on [Login Button] from [Login Page]              │
│ ├─ Click on [Login] from [Header Menu]                    │
│ └─ Click on [Submit Login] from [Login Modal]             │
└─────────────────────────────────────────────────────────────┘
```

**Implementation:** 
- Fuzzy match user input against Object Repository names
- Use action keywords: "click", "type", "enter", "select", "check", "verify"
- Store common patterns in `StepSuggestions.json`

### 3.2 Visual Flow Designer

Drag-and-drop test flow visualization:

```
┌─────────────────────────────────────────────────────────────┐
│  Test: User fills up personal details                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐ │
│  │ Open    │───►│ Fill    │───►│ Fill    │───►│ Select  │ │
│  │ Page    │    │ Name    │    │ Email   │    │ Country │ │
│  └─────────┘    └─────────┘    └─────────┘    └─────────┘ │
│       │                                             │       │
│       └─────────────────────────────────────────────┘       │
│                    (Loop with test data)                    │
│                                                             │
│  [+Add Step]  [Add Branch]  [Add Loop]  [Add Assertion]    │
└─────────────────────────────────────────────────────────────┘
```

### 3.3 Live Preview Panel

Show real-time browser preview as BA builds test:

```
┌────────────────────────────────────────────────────────────────┐
│ Step Designer                │ Live Preview                   │
├──────────────────────────────┼─────────────────────────────────┤
│ ObjectName: [First name    ▼]│  ┌─────────────────────────┐   │
│ Action:     [Fill          ▼]│  │  Contact Us Form        │   │
│ Input:      [John           ]│  │                         │   │
│                              │  │  First name: [John    ] │◄──│
│ [▶ Preview Step]             │  │  Last name:  [        ] │   │
│                              │  │  Email:      [        ] │   │
│                              │  │                         │   │
│                              │  └─────────────────────────┘   │
└──────────────────────────────┴─────────────────────────────────┘
```

---

## Phase 4 — Developer Power Features (Week 7-9)

### 4.1 Inline Code Blocks

Allow embedding Java/Groovy in CSV for complex logic:

```csv
Step,ObjectName,Action,Input,Condition
1,Browser,Open,https://example.com,
2,*CODE*,Execute,```
  // Complex validation logic
  String price = context.getVariable("price");
  double numericPrice = Double.parseDouble(price.replace("$", ""));
  if (numericPrice > 100) {
      context.setVariable("discount", "10%");
  }
```,
3,Discount Field,AssertText,%discount%,
```

**GUI Representation:**
```
┌─────────────────────────────────────────────────────────────┐
│ Step 2: Custom Code Block                            [Edit]│
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────┐│
│ │ // Complex validation logic                             ││
│ │ String price = context.getVariable("price");            ││
│ │ ...                                                     ││
│ └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 4.2 Custom Action Registration

Let developers create reusable actions without modifying Engine source:

```java
// In project's custom actions package
@INGeniousAction(name = "ValidatePrice", category = "Custom")
public class ValidatePriceAction extends Command {
    
    @ActionParameter(name = "minPrice", required = true)
    private double minPrice;
    
    @ActionParameter(name = "maxPrice", required = false, defaultValue = "1000")
    private double maxPrice;
    
    @Override
    public void execute() {
        String priceText = getElement().getText();
        double price = parsePrice(priceText);
        
        if (price < minPrice || price > maxPrice) {
            Report.fail("Price " + price + " out of range [" + minPrice + ", " + maxPrice + "]");
        } else {
            Report.pass("Price " + price + " is within range");
        }
    }
}
```

**Auto-discovered in IDE:** Shows in Action dropdown with parameter hints.

### 4.3 Debugger Integration

Add breakpoints in both CSV and Java views:

- **CSV View:** Click row number to set breakpoint
- **Java View:** Standard IDE breakpoint
- **Debug Panel:** Step through, inspect variables, evaluate expressions

```
┌─────────────────────────────────────────────────────────────┐
│ Debug: User fills up personal details                       │
├─────────────────────────────────────────────────────────────┤
│ ● Step 3: Fill | Email Address | John.Doe@email.com        │
│   ────────────────────────────────────────────────────────  │
│ Variables:                                                  │
│   %firstName% = "John"                                      │
│   %lastName% = "Doe"                                        │
│   %currentUrl% = "https://example.com/contact"              │
│                                                             │
│ [▶ Continue] [⏭ Step Over] [⏹ Stop] [📷 Screenshot]        │
└─────────────────────────────────────────────────────────────┘
```

### 4.4 External IDE Integration

**VSCode Extension:**
- Syntax highlighting for CSV test files
- Go-to-definition for object references
- Autocomplete for actions and objects
- Run/debug tests from VSCode

**IntelliJ Plugin:**
- Similar functionality
- Integration with Java DSL
- Refactoring support (rename object → updates all tests)

---

## Phase 5 — Collaboration Features (Week 9-11)

### 5.1 Test Review Workflow

```
┌─────────────────────────────────────────────────────────────┐
│ Test Review: PR #142 - Add checkout flow tests              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ Changes by: dev@company.com                                 │
│ Reviewer: ba@company.com                                    │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐│
│ │ + Step 5: Click | Add to Cart                          ││
│ │ + Step 6: AssertText | Cart Count | 1                  ││
│ │ ~ Step 7: Changed input from "$99" to "$99.00"         ││
│ └─────────────────────────────────────────────────────────┘│
│                                                             │
│ BA View: [Show as Table]  Dev View: [Show as Code]         │
│                                                             │
│ Comments:                                                   │
│ ├─ ba@company.com: Step 6 should also verify item name    │
│ └─ dev@company.com: Added in commit abc123                 │
│                                                             │
│ [Approve] [Request Changes]                                 │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 Shared Component Library

```
┌─────────────────────────────────────────────────────────────┐
│ Component Library                                           │
├─────────────────────────────────────────────────────────────┤
│ 🔍 Search components...                                     │
│                                                             │
│ ├─ 📁 Authentication                                       │
│ │   ├─ Login with valid credentials                        │
│ │   ├─ Login with SSO                                      │
│ │   └─ Logout                                              │
│ ├─ 📁 Navigation                                           │
│ │   ├─ Navigate to Dashboard                               │
│ │   └─ Navigate via Menu                                   │
│ └─ 📁 Data Entry                                           │
│     ├─ Fill Personal Details                     ⭐ 12 uses │
│     └─ Fill Address Form                                   │
│                                                             │
│ [+ Create Component]  [Import from Team]                   │
└─────────────────────────────────────────────────────────────┘
```

### 5.3 Usage Analytics

Track which tests/components are used most, identify redundancy:

```
┌─────────────────────────────────────────────────────────────┐
│ Test Analytics                                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ Most Used Components:           Potential Duplicates:       │
│ 1. Login (47 tests)             "Fill Contact Form" and    │
│ 2. Navigate to Cart (32)        "Enter Contact Details"    │
│ 3. Add Product (28)             are 87% similar            │
│                                                             │
│ Unused Objects:                 Flaky Tests:               │
│ - OldHeader.SearchBox          - Checkout Flow (3 fails)   │
│ - DeprecatedPage.*             - Payment Test (2 fails)    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## Phase 6 — AI-Assisted Test Creation (Week 11-13)

### 6.1 Record → Optimize → Suggest

When recording a test, AI suggests optimizations:

```
┌─────────────────────────────────────────────────────────────┐
│ 🤖 AI Suggestions for: Checkout Flow Test                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ ⚡ Performance:                                             │
│    Steps 3-5 can be combined using "FillForm" action       │
│    [Apply] [Dismiss]                                        │
│                                                             │
│ 🔄 Reusability:                                             │
│    Steps 1-4 match existing "Login Flow" - reuse it?       │
│    [Convert to CallReusable] [Keep as-is]                  │
│                                                             │
│ ✅ Assertions:                                              │
│    No assertions found. Add verification after:            │
│    - Step 7 (Payment submitted)                            │
│    - Step 9 (Order confirmed)                              │
│    [Add Suggested Assertions] [Skip]                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 Natural Language → Test Generation

```
┌─────────────────────────────────────────────────────────────┐
│ 🤖 Generate Test from Description                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ Describe what you want to test:                             │
│ ┌─────────────────────────────────────────────────────────┐│
│ │ User should be able to add a product to cart, apply a  ││
│ │ discount code "SAVE10", and verify the total is reduced││
│ │ by 10%                                                  ││
│ └─────────────────────────────────────────────────────────┘│
│                                                             │
│ [Generate Test]                                             │
│                                                             │
│ Generated Steps:                                            │
│ 1. Navigate to Product Page                                │
│ 2. Click Add to Cart                                        │
│ 3. Navigate to Cart                                         │
│ 4. Store original total as %originalTotal%                 │
│ 5. Enter "SAVE10" in Discount Code field                   │
│ 6. Click Apply Discount                                     │
│ 7. Store new total as %newTotal%                           │
│ 8. Assert %newTotal% equals %originalTotal% * 0.9          │
│                                                             │
│ [Accept] [Edit] [Regenerate]                               │
└─────────────────────────────────────────────────────────────┘
```

---

## Implementation Priority Matrix

| Feature | BA Value | Dev Value | Effort | Priority |
|---------|----------|-----------|--------|----------|
| Fluent Java DSL | Low | ⭐⭐⭐⭐⭐ | Medium | P1 |
| CSV → Java Generation | Low | ⭐⭐⭐⭐ | Medium | P1 |
| Page Object Generator | Low | ⭐⭐⭐⭐ | Low | P1 |
| Natural Language Input | ⭐⭐⭐⭐⭐ | Low | Medium | P2 |
| Visual Flow Designer | ⭐⭐⭐⭐⭐ | ⭐⭐ | High | P2 |
| Inline Code Blocks | ⭐⭐ | ⭐⭐⭐⭐⭐ | Low | P1 |
| Custom Action Registration | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Low | P1 |
| Debugger Integration | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | High | P2 |
| Bidirectional Sync | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | High | P3 |
| External IDE Plugins | Low | ⭐⭐⭐⭐⭐ | High | P3 |
| AI Suggestions | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | High | P4 |
| Live Preview | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | High | P3 |

---

## Quick Wins (Implement This Week)

### 1. Export Test as Java (1-2 days)

Add context menu: Right-click test → "Export as Java"
Generates standalone Java file using existing Engine APIs.

### 2. Show Code View Toggle (1 day)

In test editor, add toggle: `[Table View] [Code View]`
Code view shows equivalent Java DSL (read-only initially).

### 3. Custom Action Discovery (1-2 days)

Scan classpath for `@INGeniousAction` annotations.
Show in Action dropdown under "Custom" category.

### 4. Object Repository → Java Constants (0.5 day)

Menu: Tools → Generate Page Constants
Creates simple Java class with static strings for all objects.

---

## Success Metrics

| Metric | Current | Target (6 months) |
|--------|---------|-------------------|
| Developer adoption (using Java DSL) | 0% | 40% |
| Test creation time (new users) | 45 min | 20 min |
| Code review participation | Low | 80% of tests reviewed |
| Custom actions in use | 0 | 15+ per project |
| Test reusability (shared components) | 10% | 50% |

---

## Appendix: File Structure

```
Engine/src/main/java/com/ing/engine/
├── dsl/                          # NEW: Fluent DSL
│   ├── TestBuilder.java
│   ├── StepContext.java
│   ├── ActionMethods.java
│   └── PageObjectGenerator.java
├── annotations/                   # NEW: Custom action support
│   ├── INGeniousAction.java
│   ├── INGeniousTest.java
│   └── ActionParameter.java
├── sync/                         # NEW: Bidirectional sync
│   ├── CsvToJavaConverter.java
│   ├── JavaToCsvConverter.java
│   └── ConflictResolver.java
└── commands/                     # Existing
    └── ... (current actions)

IDE/src/main/java/com/ing/ide/main/mainui/components/
├── testdesign/                   # Existing
│   └── testcase/
│       └── TestCaseComponent.java  # Add code view toggle
├── codeview/                     # NEW
│   ├── JavaCodePanel.java
│   └── SyntaxHighlighter.java
└── nlp/                          # NEW: Natural language
    ├── StepSuggester.java
    └── NaturalLanguageParser.java
```

---

## Conclusion

This dual-mode strategy respects both personas:

- **BAs get:** Visual designers, natural language, live preview, no-code experience
- **Developers get:** Type-safe DSL, IDE integration, debugger, custom extensibility
- **Both get:** Single source of truth, collaborative workflows, AI assistance

The key is **interoperability** — never force one group to abandon their preferred workflow. Tests created in either mode are fully compatible and can be maintained by either persona.
