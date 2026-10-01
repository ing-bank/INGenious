# Getting Started with INGenious AI + MCP

INGenious ships with an AI assistant that can build, run, and debug your tests for
you. You describe what you want in plain English; the assistant calls the same
**`ingenious_*` tools** the engine uses internally, so everything it produces is a
real, valid INGenious artefact — not hand-written guesswork.

This guide gets you from zero to your first AI-authored test.

---

## 1. Which one should I use?

The same tools are reachable three ways. Pick whichever fits how you work.

| | What it is | Use it when |
|---|---|---|
| **A. `ingenious ai`** | A conversational assistant in your terminal | You want the fastest, cheapest option — especially for big jobs like a whole API suite |
| **B. INGenie** | The AI panel inside the INGenious IDE | You're already authoring in the IDE and want help in place |
| **C. MCP client** | VS Code Copilot, Claude Desktop, Cursor, Continue… | You'd rather stay in the editor you already use |

> **Note:** Options A and B talk to the tools **in-process** — there is no server to
> start and nothing to configure. Only option C needs the MCP server running.

---

## 2. Before you start

You need the standard INGenious prerequisites (see [`Readme.md`](./Readme.md)), plus
an AI backend.

### Recommended: GitHub Copilot CLI

This is the smoothest option — no API key to manage.

```bash
copilot --version     # must be installed and on your PATH
copilot auth login    # sign in once
```

If `copilot` isn't found, install the GitHub Copilot CLI first.

### Alternatives

| Backend | What it is | How you sign in |
|---|---|---|
| `copilot-sdk` | The GitHub Copilot CLI above. **Recommended.** | `copilot auth login` |
| `copilot` | GitHub Models | `/login` inside the assistant (device-code flow) |
| `openai` | Any OpenAI-compatible endpoint — OpenAI, Azure OpenAI, Ollama, or a corporate gateway | API key read from an environment variable |

---

## 3. Option A — the `ingenious ai` assistant

### Start it

From your INGenious folder:

```bash
# macOS / Linux
./ingenious ai

# Windows
ingenious.bat ai
```

Handy flags:

```bash
./ingenious ai --project CLIDemo       # start with a project already selected
./ingenious ai --mode attended         # pause at checkpoints to ask you
./ingenious ai --no-banner             # skip the welcome banner
```

### Choose how autonomous it should be

On first launch it asks:

- **unattended** — works your request through to completion on its own, correcting
  itself when something fails.
- **attended** — the same, but stops at sensible checkpoints (and on the first
  failure) to show you what happened and ask how to proceed.

Start with **attended** while you're learning what it does. Change it anytime:

```
/mode unattended
```

### Pick your model

```
/model list
```

This fetches the models your account can actually use and lets you pick one. With
the `copilot`/`copilot-sdk` backends the list comes **straight from the GitHub
Copilot CLI**.

Other model commands:

```
/model                                  show the current backend and model
/model provider copilot-sdk             switch backend
/model <name>                           set the model directly
/model url https://my-gateway/v1        endpoint for the `openai` backend
```

Your choice is remembered in `~/.ingenious/ai.json`.

### Say hello

```
/project CLIDemo
What scenarios and test cases are in this project?
```

Anything that doesn't start with `/` is treated as a request. Try:

```
Create a test case called "PingAPI" in a new scenario "HealthChecks" that
calls https://jsonplaceholder.typicode.com/users/1, checks the response
code is 200, and verifies the JSON field "name" equals "Leanne Graham".
```

Then run it:

```
Run HealthChecks/PingAPI and tell me whether it passed.
```

### Commands worth knowing

| Command | What it does |
|---|---|
| `/help` | Everything available |
| `/project <name>` | Set the active project |
| `/tools` | Browse the tools; `/tools run <tool> {json}` calls one directly |
| `/workflows` | Ready-made workflows that don't need AI at all |
| `/plan` · `/approve` | Review a proposed plan, then run it |
| `/undo` · `/redo` | Roll back (or redo) the last set of file changes |
| `/status` · `/context` · `/history` | Where things stand, session memory, recent turns |
| `/clear` | Start the conversation over (`/clear --all` also forgets session facts) |
| `/login` | Sign in to GitHub Models (the `copilot` backend) |
| `/exit` | Quit |

---

## 4. Option B — INGenie in the IDE

1. Launch the INGenious IDE (`ingenious.command`, `ingenious.bat`, or `./ingenious`
   with no arguments).
2. Click **INGenie** in the toolbar to open the assistant panel.
3. Connect:
   - Turn on **Copilot SDK** in settings to use the GitHub Copilot CLI, **or**
   - Leave it off and click **Connect** to sign in to GitHub Models.
4. The bulb next to the model selector turns **green** when the backend is ready.
5. Tick **Attended** if you want it to pause and check in with you — the same idea
   as `/mode attended` in the CLI.

Then just type what you want in the chat box.

---

## 5. Option C — connect your own editor (MCP)

Start the MCP server and point your AI client at it.

```bash
# macOS / Linux
./ingenious server mcp --project CLIDemo

# Windows
ingenious.bat server mcp --project CLIDemo
```

Add `--verbose` if you need to see what it's doing.

Then configure your client to launch that command. For example, VS Code
(`.vscode/mcp.json`):

```jsonc
{
  "servers": {
    "ingenious": {
      "type": "stdio",
      "command": "/absolute/path/to/ingenious",
      "args": ["server", "mcp", "--project", "CLIDemo"]
    }
  }
}
```

Claude Desktop, Cursor, and Continue follow the same shape — a command, its
arguments, and stdio transport.

Once connected, your editor's AI gains all the `ingenious_*` tools, and INGenious
automatically teaches it the authoring conventions during the connection
handshake. Nothing else to copy or configure.

---

## 6. Getting good results

**Let it look things up.** Never dictate action names from memory — ask it to
discover them:

> *"Look up the correct action name first, then create the test case."*

**Preview before you commit.** Most write operations support a dry run:

> *"Show me what would be created without actually writing any files."*

**Be specific about the outcome, not the mechanics.** Describe what the test should
prove; let the assistant work out the steps.

**Use attended mode when it matters.** On an unfamiliar project, checkpoints let you
catch a wrong turn early instead of reviewing 30 steps afterwards.

**It can undo itself.** If a run goes sideways, `/undo` reverts the file changes
from the last plan.

---

## 7. If something goes wrong

**"Could not list Copilot models"**
The Copilot CLI isn't installed or isn't signed in. Run `copilot --version`, then
`copilot auth login`.

**"Could not fetch models from http://127.0.0.1:…"**
You're on a stale configuration from an older build. Run `/model provider
copilot-sdk`, then `/model list`. (Newer builds migrate this automatically.)

**The editor's AI can't see any `ingenious_*` tools**
The MCP server didn't start. Check the command path in your client's config and
confirm the INGenious folder is built and runnable.

**It wrote something that doesn't look right**
Ask it to validate and fix rather than editing files by hand:

> *"Validate that test case and fix whatever's wrong."*

---

## Where to go next

- [`Readme.md`](./Readme.md) — installing and running INGenious itself
- [`ai/Readme.md`](./ai/Readme.md) — AI assets for tools not connected over MCP
- [`ai/skills/README.md`](./ai/skills/README.md) — task-specific skills (browser
  tests, API tests, migration, plugins)
