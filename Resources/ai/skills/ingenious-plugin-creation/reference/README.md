# Reference Documentation Index

This directory contains detailed reference documentation loaded on-demand by the skill.

## Files

### api-methods-quick-ref.md
Quick reference for API contract methods across all plugin types (Browser, Mobile, Webservice, Database, General).

**Use when:** Need to know which methods are available on each API contract.

### best-practices.md
Comprehensive development best practices including:
- Constructor pattern (links to constructor-pattern.md)
- Action naming conventions
- Object type naming
- Error handling patterns
- Null safety guidelines
- Playwright locator best practices
- Variable management
- Logging and reporting
- Performance considerations
- Code organization
- Testing strategies
- Documentation guidelines
- Maintenance and versioning
- Common pitfalls to avoid

**Use when:** Need guidance on coding standards, patterns, or best practices.

### constructor-pattern.md
Complete constructor initialization pattern for all 5 plugin types with examples and common mistakes.

**Use when:** Creating a new plugin class or troubleshooting constructor-related issues.

### pom-complete-template.xml
Complete Maven POM configuration template including:
- Java 17 compiler settings
- ingenious-api dependency (`provided` scope)
- Playwright dependency (`provided` scope)
- maven-dependency-plugin (copy dependencies)
- maven-jar-plugin (manifest configuration)
- maven-antrun-plugin (auto-deployment)

**Use when:** Creating a new plugin project or fixing build configuration.

### step0-directory-confirmation.md
Detailed workflow for confirming source code and deployment directories before creating plugin files.

**Use when:** Starting plugin creation process, need to understand two-directory pattern, or troubleshooting directory-related issues.

### version-compatibility.md
Version compatibility matrices and requirements:
- Current versions (Java 17, Playwright 1.50.0, API 3.0)
- Java compatibility matrix
- Playwright compatibility matrix
- API compatibility matrix
- Dependency scope requirements
- Upgrade guidelines
- Troubleshooting version issues

**Use when:** Encountering ClassCastException, UnsupportedClassVersionError, NoSuchMethodError, or need to determine compatible versions.

## Progressive Loading Pattern

These reference files are **not loaded automatically**. The main SKILL.md provides concise summaries and references specific files when detailed information is needed.

This keeps the main skill file under 500 lines while providing comprehensive documentation on-demand.

## Related Directories

- `../examples/` - Pattern files demonstrating plugin implementation
- `../templates/` - File templates for quick plugin creation
- `../troubleshooting/` - Error-specific troubleshooting guides
