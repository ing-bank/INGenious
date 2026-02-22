# Feature File to Test Case Conversion Plan

## Overview

This plan describes how INGenious will parse Gherkin/Cucumber feature files and automatically generate:
1. **Test Cases** with automation steps
2. **Page Object Model (POM)** - Object Repository entries
3. **Step Definitions** mapped to INGenious actions

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Feature File Import Pipeline                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌───────────┐ │
│  │   Feature    │───▶│   Gherkin    │───▶│    Step      │───▶│  Action   │ │
│  │    File      │    │   Parser     │    │  Analyzer    │    │  Mapper   │ │
│  │  (.feature)  │    │              │    │              │    │           │ │
│  └──────────────┘    └──────────────┘    └──────────────┘    └─────┬─────┘ │
│                                                                     │       │
│                      ┌──────────────────────────────────────────────┘       │
│                      │                                                      │
│                      ▼                                                      │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                  │
│  │   OR.object  │◀───│   POM        │◀───│   Element    │                  │
│  │   (XML)      │    │  Generator   │    │  Extractor   │                  │
│  └──────────────┘    └──────────────┘    └──────────────┘                  │
│                                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                  │
│  │  TestCase    │◀───│  TestCase    │◀───│   Scenario   │                  │
│  │   (.csv)     │    │  Generator   │    │   Converter  │                  │
│  └──────────────┘    └──────────────┘    └──────────────┘                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Phase 1: Core Feature Parsing

### 1.1 Gherkin Parser Integration

**Technology**: Use existing `io.cucumber:gherkin:5.0.0` dependency (already in project)

```java
// Existing in IDE module - adapt for Engine CLI
import gherkin.Parser;
import gherkin.AstBuilder;
import gherkin.ast.*;

Parser<GherkinDocument> parser = new Parser<>(new AstBuilder());
GherkinDocument document = parser.parse(featureFileContent);
Feature feature = document.getFeature();
```

### 1.2 Feature Structure Extraction

| Gherkin Element | INGenious Mapping |
|-----------------|-------------------|
| `Feature` | Scenario folder |
| `Scenario` | TestCase CSV file |
| `Scenario Outline` | TestCase + TestData sheet |
| `Given/When/Then` | Step Definition reference |
| `And/But` | Additional step |
| `Examples` | TestData iterations |
| `@tag` | Test metadata/tags |
| `Background` | Common scenario (reusable) |

---

## Phase 2: Step Pattern Recognition

### 2.1 Natural Language Processing for Steps

Steps in feature files follow patterns that can be mapped to actions:

#### Browser Interaction Patterns

| Step Pattern | INGenious Action | Object Type |
|--------------|------------------|-------------|
| `I navigate to {url}` | `Browser:Open` | Browser |
| `I click on {element}` | `Click` | Page Object |
| `I enter {text} in {field}` | `Fill` | Input Field |
| `I select {value} from {dropdown}` | `SelectByText` | Dropdown |
| `I should see {text}` | `AssertElementText` | Any Element |
| `I wait for {element}` | `WaitForElement` | Any Element |
| `I scroll to {element}` | `ScrollIntoView` | Any Element |
| `I check {checkbox}` | `Check` | Checkbox |
| `I upload {file} to {element}` | `SetFile` | File Input |
| `I take a screenshot` | `TakePageScreenshot` | Browser |
| `the page title should be {title}` | `AssertTitle` | Browser |

#### API Interaction Patterns

| Step Pattern | INGenious Action | Object Type |
|--------------|------------------|-------------|
| `I send a GET request to {endpoint}` | `GET` | Webservice |
| `I send a POST request to {endpoint}` | `POST` | Webservice |
| `I send a PUT request to {endpoint}` | `PUT` | Webservice |
| `I send a DELETE request to {endpoint}` | `DELETE` | Webservice |
| `the response status should be {code}` | `AssertResponseCode` | Webservice |
| `the response contains {json_path}` | `storeJSONelementValue` | Webservice |
| `I set header {name} to {value}` | `AddHeader` | Webservice |
| `the response body contains {text}` | `AssertResponseBodyContains` | Webservice |

#### Database Patterns

| Step Pattern | INGenious Action | Object Type |
|--------------|------------------|-------------|
| `I execute query {sql}` | `executeSelectQuery` | Database |
| `the result should contain {value}` | `assertQueryResultContains` | Database |
| `I insert into {table}` | `executeUpdate` | Database |

### 2.2 Pattern Matching Engine

```java
public class StepPatternMatcher {
    
    // Regex patterns for step recognition
    private static final Map<Pattern, ActionMapping> BROWSER_PATTERNS = Map.of(
        Pattern.compile("(?:I |user )?navigate(?:s)? to [\"']?(.+?)[\"']?", CASE_INSENSITIVE),
            new ActionMapping("Browser", "Open", "$1", null),
            
        Pattern.compile("(?:I |user )?click(?:s)? (?:on |the )?[\"']?(.+?)[\"']?", CASE_INSENSITIVE),
            new ActionMapping("$1", "Click", null, "PageRef"),
            
        Pattern.compile("(?:I |user )?enter(?:s)? [\"']?(.+?)[\"']? (?:in|into) [\"']?(.+?)[\"']?", CASE_INSENSITIVE),
            new ActionMapping("$2", "Fill", "$1", "PageRef"),
            
        Pattern.compile("(?:I |user )?(?:should )?see(?:s)? [\"']?(.+?)[\"']?", CASE_INSENSITIVE),
            new ActionMapping("Browser", "AssertElementText", "$1", null)
    );
    
    private static final Map<Pattern, ActionMapping> API_PATTERNS = Map.of(
        Pattern.compile("(?:I )?send (?:a )?GET request to [\"']?(.+?)[\"']?", CASE_INSENSITIVE),
            new ActionMapping("Webservice", "GET", "$1", null),
            
        Pattern.compile("(?:I )?send (?:a )?POST request to [\"']?(.+?)[\"']?", CASE_INSENSITIVE),
            new ActionMapping("Webservice", "POST", "$1", null),
            
        Pattern.compile("(?:the )?response (?:status )?(?:code )?(?:should be|is) (\\d+)", CASE_INSENSITIVE),
            new ActionMapping("Webservice", "AssertResponseCode", "$1", null)
    );
}
```

---

## Phase 3: Page Object Model (POM) Generation

### 3.1 Element Extraction from Steps

When a step references an element, we need to:
1. Extract the element name from the step text
2. Determine the likely locator strategy
3. Generate an OR.object entry

### 3.2 OR.object XML Structure

```xml
<Root type="OR" ref="ProjectName">
  <Page title="" ref="LoginPage">
    <ObjectGroup ref="Username">
      <Object frame="" ref="Username">
        <Property value="" ref="Role" pref="1"/>
        <Property value="" ref="Text" pref="2"/>
        <Property value="Username" ref="Label" pref="3"/>
        <Property value="Username" ref="Placeholder" pref="4"/>
        <Property value="" ref="xpath" pref="5"/>
        <Property value="" ref="css" pref="6"/>
        <Property value="" ref="AltText" pref="7"/>
        <Property value="" ref="Title" pref="8"/>
        <Property value="username" ref="TestId" pref="9"/>
        <Property value="" ref="ChainedLocator" pref="10"/>
      </Object>
    </ObjectGroup>
  </Page>
</Root>
```

### 3.3 Smart Locator Generation

| Element Name Pattern | Generated Locator |
|---------------------|-------------------|
| `login button` | `Label: "Login"`, `Role: "button"` |
| `username field` | `Label: "Username"`, `Placeholder: "Username"` |
| `submit-btn` | `TestId: "submit-btn"`, `css: "[data-test='submit-btn']"` |
| `Email input` | `Label: "Email"`, `Placeholder: "Email"` |

---

## Phase 4: Test Case Generation

### 4.1 CSV File Structure

```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
1,Browser,Open the Url [<Data>] in the Browser,Open,@https://example.com,,
2,Username,Click the [<Object>],Click,,,LoginPage
3,Username,Enter the value [<Data>] in the Field [<Object>],Fill,%username%,,LoginPage
4,Password,Enter the value [<Data>] in the Field [<Object>],Fill,%password%,,LoginPage
5,LoginButton,Click the [<Object>],Click,,,LoginPage
```

### 4.2 Generation Strategy

1. **Parse Feature** → Extract scenarios and steps
2. **Analyze Steps** → Match to action patterns
3. **Extract Elements** → Identify page objects
4. **Generate POM** → Create OR.object entries
5. **Generate TestCase** → Create CSV with action mappings
6. **Generate TestData** → For Scenario Outlines with Examples

---

## Phase 5: CLI Integration

### 5.1 New CLI Commands

```bash
# Import a single feature file
./ingenious feature import login.feature --project ./Projects/MyApp

# Import all features from a directory
./ingenious feature import ./features/ --project ./Projects/MyApp

# Import with options
./ingenious feature import login.feature \
  --project ./Projects/MyApp \
  --page LoginPage \
  --generate-pom \
  --type browser

# Dry-run to see what would be generated
./ingenious feature import login.feature --dry-run

# Import API feature
./ingenious feature import api-tests.feature \
  --project ./Projects/MyApp \
  --type api
```

### 5.2 Command Options

| Option | Description |
|--------|-------------|
| `--project, -p` | Target project path |
| `--page` | Default page name for elements |
| `--generate-pom` | Auto-generate Page Object entries |
| `--type` | Test type: `browser`, `api`, `database`, `mobile` |
| `--dry-run` | Show what would be generated without saving |
| `--interactive, -i` | Interactive mode for element mapping |
| `--ai-assist` | Use AI to improve step mapping (future) |

---

## Phase 6: Technology Stack

### 6.1 Browser Interactions

**Framework**: Playwright (already integrated)

INGenious uses Playwright for all browser automation:
- `com.ing.engine.commands.browser.*` - Browser commands
- `com.ing.engine.commands.playwright.*` - Playwright-specific commands

**Actions for Browser Steps**:
| Action Class | Method | Purpose |
|-------------|--------|---------|
| `Basic.java` | `Open` | Navigate to URL |
| `Click.java` | `Click` | Click element |
| `Fill.java` | `Fill` | Enter text |
| `Select.java` | `SelectByText` | Select dropdown option |
| `Assertions.java` | `AssertElementText` | Verify text |
| `Wait.java` | `WaitForElement` | Wait for element |

### 6.2 API Interactions

**Framework**: Java HTTP Client + REST Assured (integrated)

INGenious uses these for API testing:
- `com.ing.engine.commands.webservice.*` - Webservice commands

**Actions for API Steps**:
| Action Class | Method | Purpose |
|-------------|--------|---------|
| `RestClient.java` | `GET`, `POST`, `PUT`, `DELETE` | HTTP methods |
| `Assertions.java` | `AssertResponseCode` | Check status code |
| `Store.java` | `storeJSONelementValue` | Extract JSON value |
| `Headers.java` | `AddHeader` | Set request headers |

### 6.3 Database Interactions

**Framework**: JDBC (integrated)

- `com.ing.engine.commands.database.*` - Database commands

### 6.4 Mobile Interactions

**Framework**: Appium (planned integration)

---

## Phase 7: Implementation Roadmap

### Week 1: Core Parser
- [ ] Create `FeatureImportCommand.java` CLI command
- [ ] Create `FeatureParser.java` - Gherkin parsing wrapper
- [ ] Create `StepPatternMatcher.java` - Pattern recognition

### Week 2: Action Mapping
- [ ] Create `ActionMapper.java` - Map steps to INGenious actions
- [ ] Create `BrowserActionMapper.java` - Browser-specific patterns
- [ ] Create `ApiActionMapper.java` - API-specific patterns

### Week 3: Generators
- [ ] Create `TestCaseGenerator.java` - Generate CSV test cases
- [ ] Create `PomGenerator.java` - Generate OR.object entries
- [ ] Create `TestDataGenerator.java` - Handle Scenario Outlines

### Week 4: Integration & Testing
- [ ] Integrate with existing CLI
- [ ] Add dry-run mode
- [ ] Add interactive mode
- [ ] Test with sample feature files

---

## Example Conversion

### Input: login.feature

```gherkin
@login @smoke
Feature: User Login

  As a user
  I want to login to the application
  So that I can access my account

  Background:
    Given I am on the login page

  @happy-path
  Scenario: Successful login with valid credentials
    When I enter "testuser" in the username field
    And I enter "password123" in the password field
    And I click on the login button
    Then I should see the dashboard

  @negative
  Scenario Outline: Login with invalid credentials
    When I enter "<username>" in the username field
    And I enter "<password>" in the password field
    And I click on the login button
    Then I should see "<error_message>"

    Examples:
      | username  | password    | error_message           |
      | invalid   | password123 | Invalid username        |
      | testuser  | wrongpass   | Invalid password        |
      |           | password123 | Username is required    |
```

### Output: Generated Files

#### 1. TestPlan/User Login/Successful login with valid credentials.csv

```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
1,Execute,Background: I am on the login page,User Login:Background,,,
2,username field,Enter the value [<Data>] in the Field [<Object>],Fill,@testuser,,LoginPage
3,password field,Enter the value [<Data>] in the Field [<Object>],Fill,@password123,,LoginPage
4,login button,Click the [<Object>],Click,,,LoginPage
5,dashboard,Assert Element [<Object>] is Visible,AssertElementVisible,,,DashboardPage
```

#### 2. TestPlan/User Login/Background.csv

```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
1,Browser,Open the Url [<Data>] in the Browser,Open,@https://app.example.com/login,,
```

#### 3. OR.object (additions)

```xml
<Page title="" ref="LoginPage">
  <ObjectGroup ref="username field">
    <Object frame="" ref="username field">
      <Property value="" ref="Label" pref="3"/>
      <Property value="username" ref="Placeholder" pref="4"/>
      <Property value="username" ref="TestId" pref="9"/>
    </Object>
  </ObjectGroup>
  <ObjectGroup ref="password field">
    <Object frame="" ref="password field">
      <Property value="" ref="Label" pref="3"/>
      <Property value="password" ref="Placeholder" pref="4"/>
    </Object>
  </ObjectGroup>
  <ObjectGroup ref="login button">
    <Object frame="" ref="login button">
      <Property value="button" ref="Role" pref="1"/>
      <Property value="Login" ref="Text" pref="2"/>
    </Object>
  </ObjectGroup>
</Page>
```

#### 4. TestData/Successful login with valid credentials.csv

```csv
Scenario,TestCase,Iteration,SubIteration,username,password,error_message
User Login,Login with invalid credentials,1,1,invalid,password123,Invalid username
User Login,Login with invalid credentials,1,2,testuser,wrongpass,Invalid password
User Login,Login with invalid credentials,1,3,,password123,Username is required
```

---

## Future Enhancements

### AI-Assisted Step Mapping
- Use LLM to understand complex natural language steps
- Suggest element locators based on step context
- Auto-detect test type (browser/API/database)

### Visual Element Mapping
- Launch browser to capture element locators
- Screenshot-based element identification
- Record element interactions

### Bidirectional Sync
- Export test cases back to feature files
- Keep feature files and test cases in sync

---

## File Structure

```
Engine/src/main/java/com/ing/engine/
├── cli/
│   └── commands/
│       └── FeatureCommand.java          # CLI entry point
├── feature/
│   ├── FeatureParser.java               # Gherkin parsing
│   ├── StepPatternMatcher.java          # Pattern recognition
│   ├── ActionMapper.java                # Step to action mapping
│   │   ├── BrowserActionMapper.java     # Browser patterns
│   │   ├── ApiActionMapper.java         # API patterns
│   │   └── DatabaseActionMapper.java    # Database patterns
│   ├── TestCaseGenerator.java           # CSV generation
│   ├── PomGenerator.java                # OR.object generation
│   └── TestDataGenerator.java           # Test data generation
```

---

## Summary

| Aspect | Technology/Approach |
|--------|---------------------|
| **Feature Parsing** | Gherkin Parser (io.cucumber:gherkin:5.0.0) |
| **Browser Automation** | Playwright (existing integration) |
| **API Testing** | Java HTTP Client + JSON Path |
| **Database Testing** | JDBC (existing integration) |
| **Element Locators** | Label, Placeholder, TestId, CSS, XPath |
| **Test Case Format** | CSV (existing INGenious format) |
| **Object Repository** | OR.object XML (existing format) |
| **CLI Framework** | Picocli (existing integration) |

