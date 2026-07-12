# INGenious AI assets

These files ship with the INGenious distributable so that **any** AI assistant
working against an INGenious project follows the same authoring conventions the
engine enforces.

You normally do **not** need them:

* The **INGenious IDE AI assistant** and the **`ingenious ai` REPL** embed the
  conventions automatically.
* Any **MCP client** (VS Code, Claude Desktop, Cursor, ...) connected to
  `ingenious server mcp` receives the conventions automatically through the
  MCP `initialize.instructions` handshake, and can read the full reference at
  the `ingenious://docs/conventions` resource.

Use these copies only for AI tooling that is **not** connected to the MCP
server:

| File | Use it for |
|---|---|
| `INGenious.skill.md` | Drop into a skills folder (e.g. `.github/skills/ingenious-authoring/SKILL.md`) for skill-capable agents. |
| `copilot-instructions.md` | Append to your repository's `.github/copilot-instructions.md` (or equivalent custom-instructions file). |

> These files mirror `com.ing.engine.mcp.ConventionCatalog` — the single source
> of truth also used by the write tools and `ingenious_testcase_validate`.
> If you change conventions, change the catalog first.
