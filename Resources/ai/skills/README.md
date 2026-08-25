# INGenious AI Skills

These skills teach an AI assistant to author and migrate INGenious 3.1.x test artifacts
by **orchestrating the `ingenious_*` MCP tools** rather than hand-writing files. The
engine owns every file format (YAML test cases, YAML Object Repository, CSV data) and
validates against the live action catalog, so the outcomes are deterministic at low token
cost.

They ship inside the distributable at `Dist/release/ai/skills/` (copied verbatim from
`Resources/ai/skills/` on every build).

## Skills

| Skill | Use it for |
|---|---|
| `ingenious-browser-test-from-specification` | Turn a browser business flow into a YAML scenario/test case + reusable components + YAML Object Repository + data sheets, via tools. |
| `ingenious-api-test-from-specification` | Collection-first API testing: ingest APIs as a collection → run them against a live environment → convert the observed run into a YAML INGenious test (with a documented one-shot fallback). |
| `ingenious-ui-migrator` | Migrate Selenium/Gherkin UI tests into INGenious 3.1.x (YAML), routing all writes through tools. |
| `ingenious-plugin-creation` | Build/fix INGenious Playwright plugins (Java/Maven). |
| `ingenious-customization-detection` | Detect user customizations vs the official release and extract them as plugin specs. |

## Prerequisite

Point your MCP client at the INGenious MCP server so the `ingenious_*` tools are available:

```
cd Dist/release
./ingenious server mcp -p Projects/<YourProject>
```

The server advertises the tool set and the authoring conventions on `initialize`. The
`INGenious.skill.md` and `copilot-instructions.md` files (one level up in `ai/`) carry the
same conventions for agents that are not connected to MCP.

## Installing a skill into another agent

Copy a skill folder into that agent's skills location, e.g.:

```
cp -R ingenious-browser-test-from-specification \
  <repo>/.github/skills/ingenious-browser-test-from-specification
```

Keep the folder contents together — the `SKILL.md` plus any `references/`, `templates/`,
`examples/`, or `scripts/` it ships with.
