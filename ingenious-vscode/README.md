# INGenious Test Automation — VS Code Extension

Author, run, and report [INGenious](https://ing-bank.github.io/ingenious-doc/) tests
directly inside VS Code — no Swing IDE required. The extension delegates all
execution to the `ingenious` engine via its MCP server (data plane) and CLI
(run/control plane); it never re-implements engine logic.

## Requirements

- Java 17+ on `PATH` (or set `ingenious.javaHome`).
- A working `ingenious` launcher — on `PATH`, or set `ingenious.cliPath`, or the
  bundled `Resources/ingenious.command` / `Resources/ingenious.bat` in the
  workspace. Build it from source with `mvn -pl Engine -am package -DskipTests`.

## Features

- **Activity bar**: Projects, Test Plan, Reusables, Test Sets, Object Repository,
  Test Data, API Collections, Reports.
- **Visual test-case editor** for `TestPlan/**/*.csv` and `ReusableComponents/**/*.csv`
  with action/object auto-complete.
- **YAML Object Repository editor** for `ObjectRepository/**/*.yaml`.
- **Language features** on step CSVs: completion, hover, validation diagnostics,
  and CodeLens (Run / Debug / Dry-run / Validate / Last report).
- **Run tests** via the task provider (terminal output) with automatic report
  opening and a status-bar summary.
- **Report webview** rendering `summary-v2.html`.
- **Doctor**, **MCP restart**, **AI assistant terminal** (`ingenious ai`), and
  **browser object discovery** commands.

## Build & run locally

```bash
npm install
npm run compile
# Press F5 in VS Code to launch an Extension Development Host,
# or package with: npx vsce package
```

## Configuration

| Setting | Default | Description |
|---|---|---|
| `ingenious.cliPath` | `ingenious` | Path to the launcher. |
| `ingenious.javaHome` | `` | Optional JAVA_HOME override. |
| `ingenious.defaultBrowser` | `Chromium` | Browser used for runs. |
| `ingenious.headless` | `false` | Run headless by default. |
| `ingenious.mcp.autoStart` | `true` | Auto-start the MCP server. |
| `ingenious.report.openOnRunEnd` | `true` | Open the report when a run ends. |

> This is the authoring/execution/reporting client. It is complementary to the
> separate `vscode-ingenious-bridge` extension (which bridges Copilot models into
> INGenious's own AI assistant).
