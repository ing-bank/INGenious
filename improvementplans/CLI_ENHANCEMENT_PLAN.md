# INGenious CLI Enhancement Plan

## Executive Summary

Transform INGenious CLI from a basic test runner into a **comprehensive, interactive command-line tool** that supports full project management, test authoring, execution, reporting, and AI/Copilot integration.

---

## Current State Analysis

### Existing CLI Capabilities (Limited)

| Category | Current Options |
|----------|----------------|
| **Execution** | `-run`, `-rerun`, `-project_location`, `-scenario`, `-testcase`, `-browser`, `-release`, `-testset`, `-tags` |
| **Results** | `-latest_exe_*` family (status, location, data, logs, performance) |
| **Config** | `-setVar`, `-setEnv`, `-debug`, `-standalone_report`, `-dont_launch_report` |
| **Info** | `-v`, `-version`, `-bDate`, `-bTime`, `-help` |

### Limitations

1. **No project management** - cannot list/create/modify projects, scenarios, test cases
2. **No object repository management** - cannot query or modify page objects
3. **No test data operations** - cannot view/edit test data
4. **Not interactive** - single command execution only
5. **No JSON/structured output** - hard to integrate with other tools
6. **No watch/live modes** - cannot monitor executions
7. **No AI/MCP integration** - not usable by GitHub Copilot

---

## Phase 1: Enhanced Command Structure (Foundation)

### 1.1 Adopt Subcommand Pattern

Replace flat options with hierarchical commands using **Picocli** for modern CLI features:

```
ingenious <command> <subcommand> [options]
```

### 1.2 New Command Groups

```
ingenious
├── project
│   ├── list                    # List all projects
│   ├── info <path>             # Show project details
│   ├── create <name>           # Create new project
│   ├── validate <path>         # Validate project structure
│   └── export <path>           # Export project as ZIP
│
├── scenario
│   ├── list                    # List scenarios in project
│   ├── create <name>           # Create new scenario
│   ├── delete <name>           # Delete scenario
│   └── info <name>             # Show scenario details
│
├── testcase
│   ├── list [--scenario]       # List test cases
│   ├── create <name>           # Create new test case
│   ├── show <scenario/tc>      # Display test steps
│   ├── add-step                # Add step to test case
│   └── validate <name>         # Validate test case
│
├── testset
│   ├── list [--release]        # List test sets
│   ├── create <name>           # Create new test set
│   ├── add <scenario/tc>       # Add test case to set
│   └── remove <scenario/tc>    # Remove from set
│
├── object
│   ├── list [--page]           # List objects in OR
│   ├── show <page/object>      # Show object properties
│   ├── search <query>          # Search objects
│   ├── create                  # Create new object
│   └── validate                # Validate object locators
│
├── data
│   ├── list                    # List data sheets
│   ├── show <sheet>            # Display data
│   ├── get <sheet:col:row>     # Get specific value
│   ├── set <sheet:col:row>     # Set value
│   └── import <file>           # Import CSV/Excel
│
├── action
│   ├── list [--category]       # List available actions
│   ├── search <query>          # Search actions
│   ├── info <action>           # Show action details
│   └── categories              # List action categories
│
├── run
│   ├── testcase <sc/tc>        # Run single test case
│   ├── testset <rel/ts>        # Run test set
│   ├── tags <tag1,tag2>        # Run by tags
│   ├── rerun                   # Rerun last execution
│   └── watch <path>            # Watch mode (re-run on change)
│
├── report
│   ├── latest                  # Show latest execution summary
│   ├── history [--limit N]     # Show execution history
│   ├── show <execution-id>     # Show specific report
│   ├── export <format>         # Export (html/json/junit)
│   └── compare <id1> <id2>     # Compare two runs
│
├── config
│   ├── show                    # Show current configuration
│   ├── get <key>               # Get config value
│   ├── set <key> <value>       # Set config value
│   ├── drivers                 # Show browser/driver info
│   └── reset                   # Reset to defaults
│
├── server
│   ├── start [--port]          # Start MCP/REST server
│   ├── stop                    # Stop server
│   └── status                  # Show server status
│
└── shell                       # Interactive REPL mode
```

---

## Phase 2: Output Formats & Interactivity

### 2.1 Structured Output Formats

```bash
# Default: Human-readable
ingenious testcase list

# JSON output for scripting/AI
ingenious testcase list --json

# YAML output
ingenious testcase list --yaml

# Table format with customizable columns
ingenious testcase list --format table --columns name,status,steps

# Quiet mode (minimal output)
ingenious run testcase Login/ValidLogin -q
```

### 2.2 Interactive Shell (REPL)

```bash
$ ingenious shell
INGenious CLI v2.3.1 - Type 'help' for commands

ingenious> project open /path/to/MyProject
✓ Project loaded: MyProject (15 scenarios, 47 test cases)

ingenious> scenario list
┌────────────────────┬────────────┬──────────┐
│ Name               │ Test Cases │ Status   │
├────────────────────┼────────────┼──────────┤
│ Login              │ 5          │ ✓ Valid  │
│ Registration       │ 8          │ ✓ Valid  │
│ Checkout           │ 12         │ ⚠ Warning│
└────────────────────┴────────────┴──────────┘

ingenious> run testcase Login/ValidLogin --browser chrome
🚀 Starting execution...
├─ Step 1: Open Browser        ✓ PASS (1.2s)
├─ Step 2: Navigate to URL     ✓ PASS (0.8s)
├─ Step 3: Enter Username      ✓ PASS (0.3s)
├─ Step 4: Enter Password      ✓ PASS (0.2s)
└─ Step 5: Click Submit        ✓ PASS (0.5s)

✓ PASSED in 3.0s

ingenious> exit
```

### 2.3 Progress & Live Updates

```bash
# Progress bars for long operations
ingenious run testset Regression/Smoke --progress

[████████████░░░░░░░░] 60% │ 12/20 tests │ 10 pass, 2 fail │ ETA: 2m 30s

# Live tail of execution logs
ingenious run testcase Login/ValidLogin --tail

# Watch mode
ingenious run watch Login/* --browser chrome
# Re-runs tests when files change
```

---

## Phase 3: AI/Copilot Integration (MCP Server)

### 3.1 MCP Server Mode

```bash
# Start as MCP server for GitHub Copilot
ingenious server start --mcp --stdio

# Start as REST API server
ingenious server start --rest --port 8080
```

### 3.2 MCP Tools Exposed

| Tool Name | Description | Parameters |
|-----------|-------------|------------|
| `ingenious_list_projects` | List available projects | `path?` |
| `ingenious_list_scenarios` | List scenarios | `project` |
| `ingenious_list_testcases` | List test cases | `project`, `scenario?` |
| `ingenious_list_actions` | List available actions | `category?`, `search?` |
| `ingenious_list_objects` | List page objects | `project`, `page?` |
| `ingenious_run_test` | Execute a test | `project`, `scenario`, `testcase`, `browser?` |
| `ingenious_run_testset` | Execute a test set | `project`, `release`, `testset` |
| `ingenious_get_results` | Get execution results | `executionId?` |
| `ingenious_create_testcase` | Create new test case | `project`, `scenario`, `name`, `steps` |
| `ingenious_create_object` | Create page object | `project`, `page`, `name`, `locator` |
| `ingenious_validate_project` | Validate project | `project` |
| `ingenious_get_test_data` | Get test data | `project`, `sheet`, `column?`, `row?` |
| `ingenious_set_test_data` | Set test data | `project`, `sheet`, `column`, `row`, `value` |

### 3.3 MCP Resource Access

```
ingenious://project/{path}           # Project metadata
ingenious://scenario/{project}/{name} # Scenario details
ingenious://testcase/{project}/{scenario}/{name} # Test case with steps
ingenious://object/{project}/{page}/{name} # Object definition
ingenious://data/{project}/{sheet}   # Test data sheet
ingenious://report/{executionId}     # Execution report
```

---

## Phase 4: Advanced Features

### 4.1 Parallel Execution Management

```bash
# Run with parallel execution
ingenious run testset Regression/Full --parallel 4

# Distributed execution across nodes
ingenious run testset Regression/Full --nodes node1,node2,node3
```

### 4.2 Test Generation & Recording

```bash
# Start recorder
ingenious record --project MyProject --scenario NewScenario --browser chrome

# Generate tests from OpenAPI spec
ingenious generate api --spec openapi.yaml --output MyProject/API

# Generate tests from Playwright trace
ingenious import trace --file trace.zip --output MyProject/Imported
```

### 4.3 Environment Management

```bash
# List environments
ingenious env list

# Switch environment
ingenious env use staging

# Compare environments
ingenious env diff dev staging
```

### 4.4 Execution Scheduling

```bash
# Schedule execution
ingenious schedule add --testset Regression/Nightly --cron "0 2 * * *"

# List scheduled runs
ingenious schedule list

# Run scheduled item now
ingenious schedule run nightly-regression
```

---

## Phase 5: Implementation Architecture

### 5.1 Technology Stack

| Component | Technology | Reason |
|-----------|------------|--------|
| **CLI Framework** | [Picocli](https://picocli.info/) | Modern, annotation-based, supports subcommands, auto-completion, ANSI colors |
| **Interactive Shell** | [JLine3](https://github.com/jline/jline3) | Rich terminal handling, history, auto-complete |
| **JSON Processing** | Jackson (existing) | Already in project |
| **Progress Display** | [Progressbar](https://github.com/ctongfei/progressbar) | Non-blocking progress bars |
| **MCP Protocol** | Custom JSON-RPC over stdio | Standard MCP specification |
| **Table Formatting** | [ASCII Table](https://github.com/vdmeer/asciitable) | Beautiful table output |

### 5.2 Module Structure

```
Engine/src/main/java/com/ing/engine/cli/
├── INGeniousCLI.java           # Main entry point (@Command)
├── commands/
│   ├── ProjectCommand.java      # project subcommands
│   ├── ScenarioCommand.java     # scenario subcommands
│   ├── TestCaseCommand.java     # testcase subcommands
│   ├── TestSetCommand.java      # testset subcommands
│   ├── ObjectCommand.java       # object subcommands
│   ├── DataCommand.java         # data subcommands
│   ├── ActionCommand.java       # action subcommands
│   ├── RunCommand.java          # run subcommands
│   ├── ReportCommand.java       # report subcommands
│   ├── ConfigCommand.java       # config subcommands
│   └── ServerCommand.java       # server subcommands
├── interactive/
│   ├── ShellCommand.java        # REPL shell
│   ├── Completer.java           # Auto-completion
│   └── Highlighter.java         # Syntax highlighting
├── output/
│   ├── OutputFormatter.java     # Format selection
│   ├── JsonFormatter.java       # JSON output
│   ├── TableFormatter.java      # Table output
│   └── ProgressHandler.java     # Progress display
├── mcp/
│   ├── MCPServer.java           # MCP protocol handler
│   ├── MCPToolRegistry.java     # Tool definitions
│   └── MCPResourceProvider.java # Resource access
└── util/
    ├── ProjectContext.java      # Current project state
    └── CLIConfig.java           # CLI configuration
```

### 5.3 Dependencies to Add (Engine/pom.xml)

```xml
<!-- CLI Framework -->
<dependency>
    <groupId>info.picocli</groupId>
    <artifactId>picocli</artifactId>
    <version>4.7.5</version>
</dependency>

<!-- Interactive Shell -->
<dependency>
    <groupId>org.jline</groupId>
    <artifactId>jline</artifactId>
    <version>3.25.0</version>
</dependency>

<!-- Progress Bars -->
<dependency>
    <groupId>me.tongfei</groupId>
    <artifactId>progressbar</artifactId>
    <version>0.10.0</version>
</dependency>

<!-- ASCII Tables -->
<dependency>
    <groupId>de.vandermeer</groupId>
    <artifactId>asciitable</artifactId>
    <version>0.3.2</version>
</dependency>
```

---

## Phase 6: Implementation Roadmap

| Phase | Timeline | Deliverables |
|-------|----------|--------------|
| **Phase 1** | Week 1-2 | Picocli migration, basic subcommands (project, scenario, testcase, run) |
| **Phase 2** | Week 3-4 | Output formatters (JSON/Table), interactive shell |
| **Phase 3** | Week 5-6 | MCP server implementation, Copilot integration |
| **Phase 4** | Week 7-8 | Advanced features (watch mode, parallel execution) |
| **Phase 5** | Week 9-10 | Documentation, testing, shell completions |

---

## Example Usage After Enhancement

```bash
# AI/Copilot can ask:
$ ingenious action search "click button"
┌───────────────────────┬──────────────┬────────────────────────────────────────────┐
│ Action                │ Category     │ Description                                │
├───────────────────────┼──────────────┼────────────────────────────────────────────┤
│ Click                 │ Browser      │ Click on [<Object>]                        │
│ ClickByText           │ Browser      │ Click element containing text [<Data>]     │
│ DoubleClick           │ Browser      │ Double-click on [<Object>]                 │
│ clickElement          │ Mobile       │ Click on mobile element [<Object>]         │
└───────────────────────┴──────────────┴────────────────────────────────────────────┘

$ ingenious action info Click --json
{
  "name": "Click",
  "category": "Browser",
  "objectType": "PLAYWRIGHT",
  "description": "Click on [<Object>]",
  "input": "NO",
  "condition": "NO"
}

$ ingenious run testcase Login/ValidLogin --browser chrome --json
{
  "executionId": "exec-2026-02-16-143022",
  "status": "PASSED",
  "duration": "3.2s",
  "steps": [
    {"name": "Open Browser", "status": "PASS", "duration": "1.2s"},
    {"name": "Navigate to URL", "status": "PASS", "duration": "0.8s"}
  ]
}
```

---

## Backward Compatibility

The legacy CLI options will continue to work via a compatibility layer:

```bash
# Old style (still works)
java -jar ingenious-engine.jar -run -project_location /path -scenario Login -testcase ValidLogin -browser chrome

# New style (preferred)
ingenious run testcase Login/ValidLogin --project /path --browser chrome
```

---

## Success Metrics

1. **Developer Experience**: Reduce time to execute common tasks by 50%
2. **AI Integration**: Full Copilot compatibility via MCP
3. **Automation**: 100% CLI coverage for all IDE features
4. **Adoption**: New users can run first test within 5 minutes

---

## Next Steps

1. ☐ Review and approve plan
2. ☐ Add Picocli dependency to Engine/pom.xml
3. ☐ Create INGeniousCLI.java main entry point
4. ☐ Implement ProjectCommand as first subcommand
5. ☐ Add JSON output formatter
6. ☐ Implement MCP server mode
