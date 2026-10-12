# INGenious Coding Instructions

This file contains coding standards, conventions, and best practices for the INGenious Playwright Studio test automation framework.

## Project Overview

INGenious is a multi-module Maven project that provides a no-code/low-code test automation solution leveraging Playwright-Java, Appium, and JavaFX. The framework enables everyone—from engineers to business analysts—to create high-quality automated tests.

### Module Structure

The project consists of the following modules:
- **Common** – Helper functions executed during Maven build lifecycle for project-wide tasks and configurations
- **Dist** – Finalizes package structure and prepares distribution artifacts
- **Datalib** – Facilitates access to backend data sources and manages data retrieval/transformation logic
- **Engine** – Implements core business logic and processing workflows
- **IDE** – Code and utilities for the integrated development environment
- **StoryWriter** – Components and templates for building BDD scenarios and test cases
- **TestData** – Manages test plan interactions and orchestrates test data retrieval from backend systems
- **Ingenious-api** – Provides an contract for the INGenious API, enabling plugins to interact with the framework

## Code Style and Formatting

### Indentation and Spacing
- Use **4 spaces** for indentation (no tabs)
- Add space after keywords: `if`, `for`, `while`, `catch`
- No trailing whitespace

### Braces and Blocks
- Always use braces `{}` for `if`, `else`, `for`, `while`, and `do` blocks, even for single statements
- Place opening brace on same line as declaration

```java
// Correct
if (condition) {
    doSomething();
}

// Incorrect
if (condition)
    doSomething();
```

### Naming Conventions
- **Classes and Interfaces**: `PascalCase`
- **Methods and Variables**: `camelCase`
- **Constants**: `UPPER_SNAKE_CASE`
- **Packages**: lowercase, dot-separated (e.g., `com.ing.ingenious.engine`)

### Line Length and Wrapping
- Keep lines under **120 characters**
- Break long method calls or expressions into multiple lines with proper indentation
- Align continuation lines with the start of the expression

### Imports
- **Never use wildcard imports** (`import java.util.*`)
- Import only what is needed
- Group imports: standard Java → third-party libraries → project-specific imports
- Use static imports sparingly and only when they improve readability

### Annotations
- Place annotations on their own line above the declaration
- Use `@Override`, `@Deprecated`, and `@SuppressWarnings` appropriately

```java
@Override
@SuppressWarnings("unchecked")
public void process() {
    // implementation
}
```

### Comments
- Use `//` for single-line comments and `/* */` for multi-line comments
- Write meaningful comments that explain **why**, not what
- Keep comments synchronized with code changes

### Modern Java Features
- Use `var` for local variables when the type is obvious from context
- Use **Records** for immutable data carriers
- Use **text blocks** (triple quotes `"""`) for multiline strings
- Favor **immutability** for POJOs and data classes
- Use `final` keyword wherever applicable

```java
// Use text blocks for multiline strings
var jsonPayload = """
    {
        "name": "test",
        "value": 123
    }
    """;

// Use records for data carriers
public record TestResult(String name, boolean passed, String message) {}
```

### Collections and Streams
- Use **interfaces** (`List`, `Map`) over concrete implementations (`ArrayList`, `HashMap`)
- Prefer **Stream API** for filtering, mapping, and collecting operations

```java
// Correct
List<String> names = users.stream()
    .map(User::getName)
    .filter(name -> name.startsWith("A"))
    .collect(Collectors.toList());

// Incorrect
ArrayList<String> names = new ArrayList<>();
for (User user : users) {
    if (user.getName().startsWith("A")) {
        names.add(user.getName());
    }
}
```

### Clean Code Principles
- Write **small, focused methods** (typically under 20 lines)
- Follow **Single Responsibility Principle** (SRP)
- Avoid magic numbers and hard-coded values—use named constants
- Extract complex conditions into well-named methods

## INGenious-Specific Conventions

### Test Object and Action Naming

#### Object Naming
- Use descriptive nouns that clearly represent the testing domain concept (e.g., `Webservice`, `Database`, `XMLDocument`)
- Objects can represent items with associated actions (e.g., `XMLDocument` for XML operations)
- Avoid abbreviations unless widely recognized (e.g., `Api`, `Id`)

#### Storage Action Naming
Use the format: `store<Data>In<TargetDestination>`

Examples:
- `storeDBValueInDataSheet`
- `storeResultInVariable`
- `storeValueInGlobalVariable`

#### Assert Action Naming
Use the format: `assert<ObjectOfAssertion><Condition>`

Examples:
- `assertResponseBodyContains`
- `assertXMLElementEquals`
- `assertDatabaseRecordExists`

#### Assert Action Structure
- **Input/Data**: Contains the target of assertion
- **Condition**: Includes expected values, variables containing expected values, or literals influencing assertion behavior

## Build and Configuration

### Maven POM Management
- Update parent POM version and child's parent version when starting new release development
- Set Java version explicitly in `pom.xml`
- Use consistent dependency versions across modules (defined in parent POM properties)

### Directory Layout
Follow standard Maven directory structure:
- `src/main/java` – Application source code
- `src/main/resources` – Configuration files and resources
- `src/test/java` – Unit and integration tests
- `src/test/resources` – Test resources

## Documentation

### Javadoc Requirements
- Document **all public classes and methods** using Javadoc
- Include `@param`, `@return`, and `@throws` tags where applicable
- Provide examples for complex methods

```java
/**
 * Stores database value into the specified data sheet.
 *
 * @param query the SQL query to execute
 * @param dataSheet the target data sheet name
 * @param columnName the column name for storing the value
 * @throws DatabaseException if query execution fails
 */
public void storeDBValueInDataSheet(String query, String dataSheet, String columnName) {
    // implementation
}
```

## Testing Best Practices

### Test Requirements
- New features must pass agreed Acceptance Test Cases (unless explicitly disregarded)
- All changes must pass regression tests
- Create or update sample projects in the INGenious pipeline repository (if applicable)
- Update the Requirements Traceability Matrix (RTM) masterlist (if applicable)

## Exception Handling and Logging

- Handle exceptions gracefully with meaningful log messages
- Create custom exceptions for domain-specific errors
- Use appropriate logging levels (ERROR, WARN, INFO, DEBUG)

```java
try {
    executeQuery(query);
} catch (SQLException e) {
    logger.error("Failed to execute query: {}", query, e);
    throw new DatabaseException("Query execution failed", e);
}
```

## Security Best Practices

- **Never hardcode secrets**, credentials, or sensitive data
- Use environment variables or secure configuration management
- Keep dependencies updated to avoid known vulnerabilities
- Run Checkmarx scans for vulnerability detection

## Version Control

### Branching Strategy
- All developments must be pushed first to the **current release branch**
- Use meaningful, descriptive commit messages
- Follow the team's code review checklist before merging

### Commit Message Format
```
[Module] Brief description of change

Detailed explanation of what was changed and why.

Refs: #issue-number
```

## CI/CD

- Use Maven goals (`clean`, `install`) in pipelines
- Ensure all tests pass before merging
- Keep build times reasonable by optimizing test execution

## When Working with INGenious

1. **Module-Aware Development**: Always identify which module you're working in and follow module-specific patterns
2. **Framework Extensions**: When adding new actions or objects, follow the naming conventions strictly
3. **Backward Compatibility**: Consider existing tests and maintain API stability
4. **Performance**: INGenious is used for test automation—ensure changes don't significantly impact test execution time
5. **User Experience**: Remember that non-technical users may interact with your code through the IDE—keep APIs simple and intuitive

## Quick Reference

- Java Version: Check parent POM for project's Java version
- Code Style: 4 spaces, 120 char line limit, braces always
- Naming: PascalCase classes, camelCase methods, UPPER_SNAKE_CASE constants
- Actions: `store<Data>In<Target>`, `assert<Object><Condition>`
- No wildcards in imports
- Prefer immutability, use modern Java features
- Document all public APIs with Javadoc