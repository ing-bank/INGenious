# INGenious Java Best Practices Instructions

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