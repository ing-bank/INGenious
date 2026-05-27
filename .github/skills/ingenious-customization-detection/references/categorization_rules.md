# Categorization Rules

## Category Definitions and Patterns

### 1. Feature Enhancement
**Description:** New functionality added, extended capabilities, custom actions or commands

**Detection Patterns:**
- `new class`
- `new method`
- `implements`
- `extends`
- `@Action`
- `@Command`
- `Added functionality`
- `Enhanced`

**Examples:**
- New action methods in Engine module
- Custom command implementations
- Extended driver capabilities

---

### 2. Bug Fix
**Description:** Error handling improvements, null pointer fixes, exception handling

**Detection Patterns:**
- `fix`
- `fixed`
- `bug`
- `issue`
- `null check`
- `exception`
- `try-catch`
- `validate`
- `NullPointerException`

**Examples:**
- Added null checks
- Improved exception handling
- Fixed race conditions

---

### 3. Configuration
**Description:** POM modifications, dependency updates, build configuration

**Detection Patterns:**
- `pom.xml`
- `dependency`
- `plugin`
- `<version>`
- `.properties`
- `configuration`
- `settings`

**Examples:**
- Updated Maven dependencies
- Modified build plugins
- Changed property files

---

### 4. Integration
**Description:** External tool integrations, API modifications, plugin system changes

**Detection Patterns:**
- `API`
- `REST`
- `external`
- `third-party`
- `integration`
- `client`
- `service`

**Examples:**
- REST API integrations
- Third-party library usage
- External service clients

---

### 5. Performance
**Description:** Code optimizations, caching improvements, resource management

**Detection Patterns:**
- `optimize`
- `optimized`
- `cache`
- `performance`
- `faster`
- `efficient`
- `memory`
- `speed`

**Examples:**
- Caching mechanisms
- Algorithm optimizations
- Memory management improvements

---

### 6. UI/Reporting
**Description:** Report template modifications, dashboard customizations, log formatting

**Detection Patterns:**
- `report`
- `template`
- `HTML`
- `dashboard`
- `display`
- `UI`
- `render`
- `view`

**Examples:**
- Custom report templates
- Dashboard modifications
- Enhanced logging output

---

### 7. Framework Core
**Description:** Core engine changes, API contract modifications, architecture changes

**Detection Patterns:**
- `engine`
- `core`
- `framework`
- `architecture`
- `Plugin`
- `driver`
- `executor`

**Examples:**
- Engine class modifications
- Plugin system changes
- Architecture refactoring

---

## Impact Assessment Guidelines

### High Impact
- Changes to core engine classes
- Modifications to API contracts
- Plugin system architecture changes
- Breaking changes to public APIs

### Medium Impact
- Feature additions
- Integration points
- Configuration changes affecting behavior
- Non-breaking API modifications

### Low Impact
- Bug fixes
- Minor enhancements
- Code formatting
- Documentation updates
- Internal refactoring without API changes
