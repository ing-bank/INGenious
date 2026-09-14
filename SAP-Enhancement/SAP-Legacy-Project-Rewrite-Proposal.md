# SAP Legacy Project Rewrite

**Follow-up proposal — for team discussion, not started**

| | |
|---|---|
| **Branch** | `task/sap-multi-conn-session-win` |
| **Status** | Proposal only — deliberately not implemented. Needs team input before work starts. |
| **Depends on** | [SAP-Connections-Redesign.md](SAP-Connections-Redesign.md) (Phases 1–4, delivered) |
| **Scope** | Datalib · Engine · IDE/CLI |

---

## Why this is a separate doc

[SAP-Connections-Redesign.md](SAP-Connections-Redesign.md) turned SAP from a per-iteration
"driver" into a named, explicit connection (`SAP.initConnection`). Every existing project that
still has `Browser = "SAP"` on a test case keeps working today through a **runtime shim** —
nothing on disk changes; the shim translates `"SAP"` to `"No Browser"` + an implicit
`initConnection` at run time.

That shim is a bridge, not the destination. The redesign doc already named the real fix —
**rewrite `Browser = "SAP"` projects on disk to the connection model, then retire the shim** —
and deliberately scoped it out as its own follow-up feature, because unlike everything in
Phases 1–4 it **writes to a user's project files**, which is a different risk profile and
warrants its own sign-off before any code is written. This doc is that proposal.

---

## What already exists (built in Phases 1–3, ready to use)

Nothing below needs to be built — the follow-up unit plugs into it:

| Piece | Where | What it does |
|---|---|---|
| Runtime shim | `Task.java` (`sapLegacyShim` + `SystemDefaults.sapConnectionModelEnabled`) | Legacy `Browser = "SAP"` cases keep running unmodified, today, indefinitely if we never do this rewrite. |
| `sap.model` marker | `SapDefaults.KEY_MODEL` — `"legacy"` (default) / `"connection"` | Per-project flag a migration checks/flips so it runs once. |
| `ProjectMigration` SPI | `com.ing.datalib.component.migration.ProjectMigration` + `ProjectMigrationRegistry`, wired into `Project.loadProject()` | Generic, ordered, idempotent migration-unit registry. **Currently empty** — no unit registered. A failing unit is logged and skipped, never aborts project load. |
| Rewrite helpers | `com.ing.datalib.sap.SapProjectRewriteHelper` | `rewriteBrowserAssignment()` (`"SAP"` → `"No Browser"`) and `ensureInitCloseConnectionSteps()` (idempotent — injects a blank-input `SAP.initConnection` as step 1 / `SAP.closeConnection` as the last step if not already present). Unit-tested already. |

So the follow-up's actual code is small: one `ProjectMigration` implementation that finds every
`Browser = "SAP"` assignment and every affected test case, calls the two helpers above, flips
`sap.model` to `"connection"`, and reports what it did. The scaffolding, the safe IO, and the
"don't touch this project again" bookkeeping are already done.

---

## What the follow-up needs to decide and build

### 1. What "rewrite" touches

- Every `Browser = "SAP"` assignment — per-test-set and/or per-test-case, wherever that's
  stored (needs a codebase check — the redesign doc treats this as "the two artifact shapes",
  but the exact persisted locations should be re-confirmed against current `TestSet`/`TestCase`
  storage before writing the migration unit).
- Every affected test case's steps — inject `SAP.initConnection` (blank input) as step 1 and
  `SAP.closeConnection` (blank input) as the last step, via `SapProjectRewriteHelper`.
- Imported-via-Scripting-Tracker test cases are **not** a separate case — Phase 3 already made
  the importer itself emit the init/close pair and skip writing `"SAP"` anywhere, so only
  *pre-existing*, already-imported-and-saved test cases need the rewrite.

### 2. Safety — this is the part that needs the most discussion

Every other phase of this redesign only touched engine/IDE code. This one **writes into a
user's project on disk**. Open questions the team should weigh in on:

- **When does it run?** Automatically on project open (once, gated by `sap.model`), or only
  ever via an explicit CLI command? Automatic is more convenient but means a project owner
  who didn't expect a rewrite gets one the next time anyone opens their project — including,
  e.g., a CI job or a teammate just browsing it.
- **Backup.** Should the migration snapshot the affected files before rewriting (a `.bak`
  copy, a zip, a git-aware "just don't run if the tree is dirty" check)? What's the rollback
  story if a rewrite goes wrong partway through a large project?
- **Dry-run.** A `--migrate-sap --dry-run` (or equivalent IDE affordance) that reports what
  *would* change without touching anything — proposed in the original doc, not designed yet.
- **Read-only guard.** `Project`'s existing `readOnlyMode` already suppresses all migrations
  (including this one, for free, since it goes through the same `ProjectMigrationRegistry`
  gate) — confirm that's sufficient, or whether this migration needs an additional,
  more visible "are you sure" gate given it's a bulk rewrite rather than a small settings
  migration like the ones that already run silently today (`EmulatorToDeviceMigration`, CSV→YAML).
- **Partial failure.** If the migration touches 50 test cases and fails on the 30th (disk full,
  file locked, whatever), what state is the project left in? `sap.model` shouldn't flip to
  `"connection"` unless the whole pass succeeded, but that needs an explicit transactional (or
  at least idempotent-and-resumable) design, not just "loop and hope."
- **Reporting.** The doc's original sketch was *"N SAP test cases updated"* — is a console/log
  line enough, or does this need a proper report artifact (which files, what changed, what to
  review) given it's an unattended, automatic rewrite of user content?

### 3. Rollout / shim retirement

- The shim stays until "enough of the estate has converted" — undefined today. Is that a time
  box, a percentage, a per-project opt-in period, or does it just stay forever as a safety net
  once the automatic migration exists?
- `--migrate-sap` CLI for headless/bulk runs (e.g. a fleet of CI-checked-out projects) — worth
  building in the same pass, or a later addition once the interactive path is validated?

### 4. Scope confirmation

Everything above is scoped to `Browser = "SAP"` → connection-model conversion only. It does
**not** touch:
- Phase 4 sessions (`openSession`/`switchSession`) — nothing to migrate, those are new.
- The guardrails, compatibility matrix, or any Phase 1–4 behavior — unaffected.
- Non-SAP archetypes — out of scope entirely.

---

## Suggested next step

Get sign-off from the team on the safety questions in section 2 (automatic vs. CLI-only,
backup strategy, dry-run requirement) before writing the `ProjectMigration` unit itself — the
unit is small and mechanical once those decisions are made; the decisions are the actual work
here.
