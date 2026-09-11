# SAP Connections Redesign

**Design proposal & implementation plan**

Turn SAP from a single hard-wired browser into named, driverless connections that mix
freely with other archetypes in one test case — with concurrent SAP sessions scoped as a
later, additive phase.

| | |
|---|---|
| **Branch** | `task/sap-multi-conn-session-win` |
| **Status** | Phases 1–2 implemented, tested and pushed. Phase 3 not started. |
| **Scope** | Engine · Datalib · IDE |

---

## Contents

1. [Summary — what this changes](#summary--what-this-changes)
2. [Context — how SAP works today](#context--how-sap-works-today)
3. [The core idea — SAP is a connection, not a driver](#the-core-idea--sap-is-a-connection-not-a-driver)
4. [Request 1 — multiple SAP connection configurations](#request-1--multiple-sap-connection-configurations)
5. [Request 2 — driverless SAP, mixed with other actions](#request-2--driverless-sap-mixed-with-other-actions)
6. [SAP Scripting Tracker import](#sap-scripting-tracker-import)
7. [Guardrails — what may share a SAP test case](#guardrails--what-may-share-a-sap-test-case)
8. [Deferred (Phase 4) — concurrent SAP sessions](#deferred-phase-4--concurrent-sap-sessions)
9. [Dependency — JACOB](#dependency--jacob)
10. [Testing without a SAP environment](#testing-without-a-sap-environment)
11. [Known limitation — environment prerequisites](#known-limitation--environment-prerequisites)
12. [Compatibility — migration](#compatibility--migration)
13. [Roadmap — phased plan](#roadmap--phased-plan)
    - [Phase 1 — delivered](#phase-1--delivered)
    - [Phase 2 — delivered](#phase-2--delivered)
    - [Phase 3](#phase-3)
    - [Follow-up — legacy project rewrite (separate request)](#follow-up--legacy-project-rewrite-separate-request)
    - [Phase 4 — deferred](#phase-4--deferred)
14. [Verification — test list](#verification--test-list)
15. [Open decisions](#open-decisions)

---

## Summary — what this changes

| | |
|---|---|
| **Request 1** | Multiple SAP connection configurations, managed in Settings like Databases and API aliases — `SAP_DEV`, `SAP_QA`, `SAP_PROD`. |
| **Request 2** | SAP stops owning the run. It becomes a connection opened on demand, so any non-SAP action that needs no second live device — General, Browser/Playwright, String Ops, Synthetic Data, Database, API — runs in the same test case, inline or through a reusable. |
| **Deferred** | Multiple concurrent SAP *sessions* (transaction windows) addressed by alias. Additive on top of Phases 1–3; needs one seam built now (see below). |
| **Unchanged** | Every other archetype's behaviour. Mobile, Kafka, Queue, LambdaTest/Grid and ProtractorJS stay out of a SAP test case — each needs its own real device, broker or remote driver. |

---

## Context — how SAP works today

The SAP identity is the literal string `"SAP"`, everywhere:

- `Task.isSAPExecution()` is `runContext.BrowserName.equals("SAP")`. Each iteration in
  `Task.runIteration` launches **exactly one** of Playwright, SAP or WebDriver.
- `SAPSessionCreation` re-checks the same literal and additionally requires an
  `Emulators.json` row named `SAP`, auto-created by `Emulators.ensureDefaultEmulators()`.
- `SAPSessionFactory` reads one file — `Capabilities/SAP.properties` — and calls
  `OpenConnection(connectionName)`. One system, chosen in the Run button or assigned as the
  Browser.
- `TestCaseToolBar.loadBrowsers` and `DriverSettings.getTotalBrowserList` both special-case
  the `"SAP"` string into the browser list.

Mixing already *half* works: `CommandControl` (around line 142) skips object-finding for a
step whose Object/Reference is not in the SAP Object Repository, so driverless actions —
General, String Ops, Synthetic Data, Structured Data, Database, Webservice, File — already
run inside a SAP test case. What cannot run is anything needing a second live session:
Browser/Playwright, Mobile, Kafka, Grid.

The pattern to copy already exists in the same codebase: `database/General.java` holds
`public static Connection dbconnection`; `Database.initDBConnection` opens it from
`Settings/Databases/*` by `#alias`, later actions reuse it, `closeDBConnection` tears it
down — and `Task` has no Database branch at all.

---

## The core idea — SAP is a connection, not a driver

SAP GUI Scripting is a COM attachment to a desktop app you launch or adopt — the same shape
as a JDBC connection, not a browser driver that owns the iteration. Once SAP is modelled
that way, multiple connections, action mixing and reusable calls all fall out of one change.

### Today — one driver per iteration

```
Run target — BrowserName
        |
        v   pick exactly one
  [ Playwright ]   [ SAP ]   [ WebDriver ]
        |
        v
  CommandControl
```

Choosing SAP excludes every browser action. Other systems? A second config that doesn't
exist.

### Proposed — browser + SAP connections, side by side

```
Run target — Browser | "No Browser"    SAP connections
                                     SAP_QA · SAP_PROD
        |                                   |
        +----------------+------------------+
                         v
   CommandControl — routes each step by Object type
        |
        +--  Web OR      -> Playwright
        +--  SAP OR      -> SAP
        +--  driverless  -> run
```

SAP is opened by an explicit `SAP.initConnection` step — aliased and thread-scoped, exactly
like `Database.initDBConnection`. The browser is whatever the test needs, or nothing.

---

## Request 1 — multiple SAP connection configurations

### Connection store

A dedicated store, `Settings/SAP/<name>.properties` — one file per connection, exactly like
`Settings/Databases/`. No `type=SAP` marker inside a shared Capabilities folder, no
dependency on `Emulators.json`.

```properties
# Settings/SAP/SAP_QA.properties
connectionName = QAS [PUBLIC]          # SAP Logon entry (SNC-configured for SSO), or a connection string
app            = C:\Program Files\SAP\FrontEnd\SAPGUI\saplogon.exe
multiLogon     = keepOthers            # keepOthers (default) | endOthers | terminateThis | fail
                                      # auto-answers the "Multiple Logon" dialog — never blocks

# Interactive logon -- OMIT ALL FOUR for SSO / SNC users
client         = 200
user           = ${env:SAP_QA_USER}       # resolved via the existing param/env resolver
password       = ${env:SAP_QA_PASSWORD}
language       = EN
```

> `user`, `password`, `client` and `language` apply **only** when the framework reaches the
> SAP Logon screen itself. SSO / SNC users omit all four — SAP authenticates from the
> Windows identity and no logon screen appears. An already-open, signed-in session ignores
> them too. See [Logon — SSO/SNC and interactive](#logon--ssosnc-and-interactive).

No `dllPath` / `libraryPath` — the JACOB native bridge is Maven-managed, see
[Dependency — JACOB](#dependency--jacob). The pre-Maven `lib/jacob-1.21/*` keys are removed.

### Registry and selection

- **New `SapConfigRegistry`** (Datalib) — `listNames()` and `get(name)` over the store,
  wired into `ProjectSettings`.
- **Run button and Test Set assignment** no longer carry a `"SAP"` entry (see Request 2).
  Connections are chosen with an explicit `SAP.initConnection` step — `#alias` for a named
  connection, blank for the project default.
- **CLI** — `-setEnv "sap.SAP_QA.connectionName=…"` overrides via the same path the
  Capabilities override dispatcher already uses.

### Settings UI — "SAP Connections"

A tab in Driver Settings beside Databases, API, Devices: a list with Add / Duplicate /
Rename / Delete, a "Set as default" toggle, and a form for the keys above — password masked
and stored as a `${…}` reference by default, `multiLogon` as a dropdown
(`keepOthers` / `endOthers` / `terminateThis` / `fail`). A **Test Connection** button runs a
short-lived attach-or-launch and reports back. Persistence reuses the `Capabilities`-style
load/save/rename/delete already in Datalib.

---

## Request 2 — driverless SAP, mixed with other actions

### SAP leaves driver selection

Remove the SAP branch from `Task` entirely — `isSAPExecution()`, `getSAPSession()`,
`launchSap()`, the `finally` special-case, `report.setSapSession()`. `"SAP"` also comes out
of the Run-button lists (`TestCaseToolBar.loadBrowsers`, `DriverSettings.getTotalBrowserList`,
`Repl`).

The run target is then a normal browser, or the **existing `"No Browser"` target** for
browserless test cases. `"No Browser"` is already a first-class value — `BrowserNames.NO_BROWSER`
(aliases `NoBrowser`, `no-browser`), `PlaywrightDriverFactory.Browser.Empty`, an
`EmptyDriver` from `WebDriverFactory`, `WebDriverCreation.isNoBrowserExecution()` — so
nothing new is needed and it's the same option API-only tests already use. A pure SAP test
case picks `"No Browser"`; a SAP + web test case picks a browser.

### SapSessionManager

`SAPSessionCreation` becomes a manager keyed by a caller-chosen alias:

```
ThreadLocal<LinkedHashMap<String alias, SapTarget>>   + currentAlias pointer

SapTarget { GuiConnection conn; GuiSession session;
            boolean ownsConn; boolean ownsSession; }
```

Thread-scoped because COM STA affinity ties a session to the thread that called
`ComThread.InitSTA()`. A test case and its inline reusables run on one runner thread, so a
reusable sees every alias its caller bound — unlike the driver fields copied by value in
`TestCaseRunner.createControl`. The map has one entry until the multi-session phase; that
entry behaves exactly like `database/General.dbconnection` does today.

> **Seam to build now.** `CommandControl` must resolve the SAP session and `SAPObject`
> through `sapSessionManager.current()` — never a bare field passed by `Task`. And the SAP
> Object Repository page schema reserves an optional, nullable `session` attribute now,
> ignored until Phase 4. Both cost nothing in v1 and remove a migration later.

### Connection vs session

Two distinct SAP GUI Scripting concepts, and this design turns on the difference:

| | Connection (`GuiConnection`) | Session (`GuiSession`) |
|---|---|---|
| What it is | One **logon** to one SAP system — a SID + client + user, authenticated once | One **window / mode inside a connection** — an independent transaction context |
| How many | One per SAP Logon entry you open; several if you open several systems | Up to **6 per connection**; created with "New Session" (the `/o` prefix) |
| Auth | Each is its own logon (password or SNC/SSO) | Shares the connection's logon — no re-auth |
| In this design | `initConnection` / `closeConnection`; `#alias` → `Settings/SAP/<alias>.properties` | Phase 4 only — `openSession` / `switchSession`; runtime `@label`, no config file |

Phases 1–3 deal only with **connections** (one per alias). Concurrent **sessions** are the
[deferred Phase 4](#deferred-phase-4--concurrent-sap-sessions) enhancement.

**Where element ids sit.** The full SAP GUI Scripting path is hierarchical:

```
/app  /con[0]  /ses[0]  /wnd[0]      /usr/txtRSYST-BNAME
 app   conn.    session   window       control
                          /wnd[1]     modal popup on top of wnd[0]
                          /wnd[2]     popup on top of that popup
```

`wnd[0]` is the session's main window; `wnd[1]` / `wnd[2]` are modal dialogs stacked inside
**one session** at that moment — not connections or sessions. The engine resolves elements
with `session.FindById(id)` ([`SAPObject.java`](../Engine/src/main/java/com/ing/engine/drivers/SAPObject.java)),
so stored OR ids and Scripting Tracker ids are **session-relative** — they start at
`wnd[...]` and omit `/app/con[x]/ses[y]/`. Which connection (and, in Phase 4, which session)
a `wnd[...]` id binds to is supplied by `sapSessionManager.current()`; a `wnd[1]` in an id
just means the step touches a popup and says nothing about connection/session identity.

### initConnection / closeConnection

SAP mirrors `Database` exactly: the connection is **explicit and required**. Every SAP test
case (or reusable) opens with `SAP.initConnection` before any SAP action. A SAP action with
no open connection fails fast — *"No SAP connection — add a `SAP.initConnection` step."*

| Action | Input | Effect |
|---|---|---|
| `initConnection` | *blank* | Open or adopt the **default** connection, add a claim, make it current. |
| `initConnection` | `#QA`, `#Staging`, `#default`, … | Open or adopt the named connection (`#alias` → `Settings/SAP/<alias>.properties`), add a claim, make it current. Already open → just add the claim and set current. |
| `switchConnection` | `#QA` | Make an already-opened connection current — no claim change. |
| `closeConnection` | *blank* | The mirror of `initConnection` blank — resolves to the **default** connection: release this runner's claim on it; physically close only when no claims remain and the connection is owned. |
| `closeConnection` | `#QA` | Same, for that alias only. |
| `closeAllConnection` | — | Last-resort teardown: force-close **every** connection this run owns, ignoring claims. Adopted sessions are released but not closed. |

- **Blank input = the project default.** The default is the connection flagged in the SAP
  Connections panel (`defaultSap=<name>` in project settings); if exactly one connection is
  defined it is the default implicitly. Blank input with no default set **and** more than
  one connection defined → fail fast.
- **`#alias` = that connection**, where `alias` is the config file name (`#QA` →
  `Settings/SAP/QA.properties`). A config may itself be named `default`.
- **Auto-close backstop.** Anything still owned at iteration end is closed by
  `SapSessionManager` regardless of leftover claims, so an unbalanced or forgotten
  `closeConnection` never leaks a launched client.

#### Surfacing in the IDE — two object types, like Browser vs. Web

Typing `Object = "SAP"` didn't work out of the box, and once it did it mixed two different
kinds of action together. Fixed by splitting SAP into **two `ObjectType`s**, mirroring the
existing `BROWSER` (session-level, typed literally) vs. `WEB` (element-level, reached only by
resolving a real Object Repository element) split:

| `ObjectType` | Value | Reached by | Actions |
|---|---|---|---|
| `SAP` | `"SAP"` | Typing `Object = "SAP"` literally — registered in `ObjectTypeUtil.getAllTypesForIDE()`, same list `"Browser"`/`"Database"` are in | The 9 object-less actions: `initConnection` / `switchConnection` / `closeConnection` / `closeAllConnection` and the legacy session/process actions `sapExecuteTransaction` / `sapEndTransaction` / `sapRefreshSession` / `sapCloseLogonScreen` / `sapSetglobalObjectProperty` |
| `SAP_OBJECT` | `"SAP Object"` | Resolving a real SAP OR element via `isSapObject()` (an `Object`/`Reference` pair that matches a page in the SAP Object Repository) — **not** in `getAllTypesForIDE()`, exactly like `WEB`/`APP` aren't | The ~59 element-level actions: `sapClick`, `sapFill`, every window/text-field/button/checkbox/combobox/tab/table/grid/menu/tree/status-bar action |

So `Object = "SAP"` now shows exactly the connection/session actions — no element-level noise
— and the element actions only ever appear once a step actually references a SAP OR element,
the same way `sapClick` on a real object never shows up under `Object = "Browser"` today.

Getting there took three fixes, all in Phase 1:

- **Registered `ObjectType.SAP`** in `ObjectTypeUtil.getAllTypesForIDE()` (it was missing
  entirely — SAP had no literal-object entry point before this redesign).
- **Added `ObjectType.SAP_OBJECT`** as a new core constant (`ingenious-api`'s `ObjectType`,
  in `initialObjectTypes` so it's never mistaken for a plugin type) and re-annotated the ~59
  element-level `@Action` methods from `SAP` to `SAP_OBJECT`; repointed the two IDE lookups
  that resolve a SAP OR element to an action list — `TestCaseAutoSuggest.getActionBasedOnObject()`
  and `ActionRenderer.isActionValid()` — from `getMethodListFor(ObjectType.SAP)` to
  `getMethodListFor(ObjectType.SAP_OBJECT)`.
- **`input = InputType.YES` blocked the documented blank-input case.** The IDE's
  `InputRenderer.isOptional()` reads the action's `InputType` directly to decide whether an
  empty Input cell is valid — independently of `ArgType.ALIAS_SAP`'s validator (which is
  `null`, i.e. "blank is fine"). `InputType.YES` means *mandatory*, so it flagged a blank
  `initConnection`/`switchConnection`/`closeConnection` step as an error despite blank being
  exactly how "use the project default" is meant to be written. Corrected all three to
  `InputType.OPTIONAL`. `closeAllConnection` is unaffected — it already took no input
  (`InputType.NO`).

**Legacy session/process actions reclassified too.** The five pre-existing actions
(`sapExecuteTransaction`, `sapEndTransaction`, `sapRefreshSession`, `sapCloseLogonScreen`,
`sapSetglobalObjectProperty`) were annotated `object = ObjectType.BROWSER` — the only way to
make an object-less SAP action appear in the IDE before `ObjectType.SAP` existed. Left as
`BROWSER` they would keep showing SAP-only actions under a real Browser/Playwright test
case's `Object = "Browser"` list, exactly the mixing this redesign exists to prevent.
Reclassified all five to `object = ObjectType.SAP` (not `SAP_OBJECT` — they take no element).
No stored test case or sample project referenced them under `Object = "Browser"`, so this was
a same-branch, no-migration change.

**Fallout: `SAPProcess` wiring.** `sapCloseLogonScreen` calls `SAPProcess.destroy()`. In the
driverless model `CommandControl.SAPProcess` was never populated (only `SAPObject` was bound
lazily), so the action would silently fail. `SapSessionManager` gained `currentProcess()`
(returns the owned `Process` for the current alias, or `null` for an adopted/opened-on-existing-engine
connection with nothing to kill), and `CommandControl.bindSapSession()` now sets `SAPProcess`
from it alongside `SAPObject`. This also means `sapCloseLogonScreen` naturally respects the
ownership model: it only ever kills a process this run launched, never a connection it
adopted or merely opened on someone else's already-running client.

#### Nested `init` / `close` — claim counting

Each connection carries a stack of **claims**, one per `initConnection` call, tagged with
the `TestCaseRunner` that made it. `closeConnection` pops the most recent claim **made by
the same runner**; the connection is physically torn down only when its claim stack is empty
*and* the run owns it (launched or opened it — an adopted connection is released but never
closed). This makes the parent + reusable sequence behave:

| Step | Runner | Claims on `C` | Physical action |
|---|---|---|---|
| `initConnection` | parent `R0` | `[R0]` | open/adopt `C` |
| ↳ reusable `initConnection` | reusable `R1` | `[R0, R1]` | none — already open |
| ↳ reusable `closeConnection` | `R1` | `[R0]` | none — `R0` still claims `C` |
| ↳ *(reusable returns)* | | `[R0]` | none |
| `closeConnection` | `R0` | `[]` | close `C` (owned, unclaimed) |

Both `closeConnection` steps here take blank input, resolving to the default alias — the
same alias both `initConnection` blank calls opened.

Fallbacks: a `closeConnection` from a runner that never `init`'d that alias has no claim to
pop → design-time warning, runtime no-op (the parent's claim is safe). An unbalanced
reusable that `init`s without `close` leaves its claim on the stack; the iteration-end
backstop clears it. `closeAllConnection` ignores the stack entirely — an explicit "tear
everything down now", meant as a last resort.

#### Calling `initConnection` more than once

It happens routinely — parent + an imported reusable both carry a step 1; a defensive repeat
after branching; step 1 firing again on every data-driven iteration; a step retry. The
manager resolves the input to an **alias** (blank → the default connection's name) and keys
on that:

| Situation | Behaviour |
|---|---|
| Alias **not yet open** on this thread | Run adopt-or-launch, register it, add a claim, set current. |
| Alias **already open** on this thread | Add a claim, set current, log at debug. No re-launch, no re-adopt, the session is **not** reset to the menu. |
| A **different** alias | Not a repeat — opens that connection too and makes it current; the first stays open (multi-connection). |
| Alias was open, all claims released, then `initConnection` again | Fully re-opened. |

So `initConnection` blank and `initConnection #QA` unify when `QA` is the default — the
second just adds a claim. Across iterations, the auto-close backstop fires per iteration, so
step 1 genuinely re-opens each iteration (cheap — the adopt path reuses the running SAP
GUI). To force a fresh session within an iteration, `closeConnection` (until the stack
empties) then `initConnection`.

### Adopt-or-launch — automatic, no flag

Runs on every `initConnection`:

| State at initConnection time | Action | Owns process | Owns connection |
|---|---|---|---|
| Scripting engine running, matching connection already open | Adopt that connection (use its active session) | no | no |
| Engine running, no matching connection | `OpenConnection` on the existing engine | no | yes |
| No engine at all | Launch `saplogon.exe`, poll the ROT, `OpenConnection` | yes | yes |

Teardown honours the flags: an adopted connection is left untouched; an
opened-on-existing-engine connection is closed but the client stays; a fully launched client
is closed and its process terminated. This replaces the fixed `Thread.sleep(7000)` — only the launch path waits, and
it polls the Running Object Table instead of sleeping blindly.

### Logon — SSO/SNC and interactive

Many users authenticate to SAP by SSO (SNC / Kerberos / Windows integrated), with no
username or password to supply. After `OpenConnection` the `initConnection` flow **inspects
the session** rather than assuming a logon screen:

| Session state after `OpenConnection` | Behaviour |
|---|---|
| No logon screen — already on SAP Easy Access (SSO/SNC completed, or an adopted session) | Proceed. Any configured `client` / `user` / `password` / `language` are ignored. |
| Logon screen present, credentials configured | Fill and submit. |
| Logon screen present, no credentials (SSO expected) | Fail fast: *"SAP logon screen appeared but no credentials are configured and SSO did not complete — check the SAP Logon entry's SNC settings."* |
| *"License Information for Multiple Logon"* dialog | Resolve per `multiLogon` (below). |

SSO users typically already have SAP GUI open and authenticated, so the **adopt** path is
their normal case and no logon screen is ever shown. SNC is configured on the SAP Logon
entry itself, workstation-side — the framework does not set it.

#### The multiple-logon dialog

This is a **connection-level** event, not a session one. SAP raises *"License Information
for Multiple Logon"* when a **new logon** (a new connection) is opened for a user who is
**already logged on to that same system + client** — from their interactive SAP GUI, another
PC, a background job, etc. Opening a second *session* inside an already-open connection never
triggers it (same logon). SSO users hit it constantly because they keep SAP GUI open all day
and the framework's own `OpenConnection` (adopt-or-launch cases 2–3) counts as another
logon.

Unlike the scripting-notification popups, this dialog **is script-addressable** — a normal
`GuiModalWindow` with radio buttons and an OK button. So the framework detects it and
**auto-answers it from `multiLogon`** every time it appears: the dialog is never a blocker,
never hangs the run, never leaves a half-open connection. The value is read from the
connection's `Settings/SAP/<alias>.properties`; resolution order is CLI override
(`-setEnv "sap.<alias>.multiLogon=…"`) → config file → built-in default `keepOthers`.

| `multiLogon` | Dialog choice taken | Effect |
|---|---|---|
| `keepOthers` *(default)* | "Continue with this logon, without ending any other logons" | The user's existing SAP session is untouched. Framework proceeds on its own logon. |
| `endOthers` | "Continue with this logon and end any other logons in the system" | **Logs the user out of their own interactive SAP GUI.** Only for dedicated automation accounts on a machine with no interactive use. |
| `terminateThis` | Cancel | The new logon is abandoned; `initConnection` then falls back to adopting the user's existing connection, or fails fast if there is none. |
| `fail` | — (not answered) | `initConnection` errors immediately: *"Multiple-logon dialog appeared; set `multiLogon` on this connection."* Opt-in, for teams that want an explicit per-environment decision. |

`keepOthers` is the default and almost always correct — with adopt-or-launch the framework
usually reuses the user's existing connection and never sees the dialog; it only appears when
it opens its own, and ending the user's session then would be destructive. Set `endOthers`
per-connection for dedicated runners. Note some systems disable multiple logon entirely
(`login/disable_multi_gui_login`), whitelisting only named service accounts — there the
dialog never shows and a non-whitelisted logon simply fails.

### Reusables

A reusable's `CommandControl` copies driver references from `getRoot().getControl()`, so it
inherits the browser the root test case launched. SAP connections it inherits through the
thread-scoped `SapSessionManager` — not a copied reference.

- **The parent's connection flows in automatically.** If the parent test case has already
  run `SAP.initConnection`, a reusable called inside it runs on the same runner thread and
  sees that open connection and the current-alias pointer. The reusable needs **no**
  `initConnection` of its own — its SAP steps just use the parent's connection.
- **A reusable that carries its own `initConnection` is still fine** (imported reusables do).
  Same alias already open → adds this runner's claim and makes it current, no re-open;
  a different alias → opens that one too, alongside the parent's.
- **`switchConnection` inside a reusable is visible to its caller** — a copied field could
  not do this.
- **Fail-fast only when nothing in the call chain connected** — neither the parent nor the
  reusable. Same as calling a `Database` reusable with no `initDBConnection` anywhere.
- **`closeConnection` is claim-scoped, so a reusable can't close the parent's connection.**
  It only pops the reusable runner's own claim (see [Nested `init` /
  `close`](#nested-init--close--claim-counting)); the parent's claim keeps the connection
  alive for its trailing steps. A balanced `init`…`close` inside a reusable is always safe.

---

## SAP Scripting Tracker import

INGenious imports [SAP GUI Scripting Tracker][tracker] recordings (PowerShell and Java
today) and converts them to a test case plus a SAP Object Repository page —
`SapScriptParser.generateTestCase()` / `generateSapORPage()`, reached from
**Import SAP Recording** in `AppActionListener.handleSapImport`.

[tracker]: https://community.sap.com/t5/technology-blog-posts-by-members/scripting-tracker-development-tool-for-sap-gui-scripting/ba-p/13282144

What the importer produces today: a `TestPlan/<file>/<page>.csv` with columns
`Step,ObjectName,Description,Action,Input,Condition,Reference`; transaction steps as
`ObjectName = SAP_SYSTEM`, `Action = executeTransaction`; element steps referencing
`[Project] <page>`; and the SAP OR page built from the recording's object ids. **No run
target and no connection are written** — the imported test case relies on the user picking
`SAP` in the Run button.

The redesign removes that `SAP` button entry, so the importer changes with it:

| Change | Detail |
|---|---|
| Emit `SAP.initConnection` … `SAP.closeConnection` | `initConnection` as **step 1**, `closeConnection` as the **last step** — a balanced pair, both with **blank `Input`** so they use the project default. No connection prompt at import; the user edits step 1 to `#alias` afterwards if they want a specific system. Claim-scoping makes the pair safe when this case is later called as a reusable — its `init` just adds a claim to the parent's connection and its `close` pops only that claim. |
| Capture from the recording *(optional, same phase)* | The language parsers (`SapParserLangJava`, `SapParserLangPowerShell`) already track `findById` / `invoke`; extend them to pull the `OpenConnection "<entry>"` argument and pre-create `Settings/SAP/<name>.properties` with `connectionName` = that entry and **no credentials** (the SSO default), setting it as the project default when none exists. The generated step 1 stays blank regardless. |
| Run target | The generated test case runs against **`"No Browser"`** — a pure SAP recording has no browser. |
| `SAP_SYSTEM` transaction steps | Unchanged. Session-level SAP actions resolve through `sapSessionManager.current()`, populated by the `initConnection` step. |
| Generated SAP OR objects | Carry the reserved (empty) `session` attribute once the Phase 1 schema change lands — no importer code change. |
| `initConnection` is claim-counted | An imported test case reused inside another SAP test case does not double-open: `initConnection` on an already-open alias just adds a claim; the trailing `closeConnection` pops only that claim. |
| Multi-window (`wnd[0..n]`) — **fix in Phase 3** | Reported issue: only `wnd[0]` elements come through. The parser regexes do capture `wnd[1]/...` ids, but `generateObjectName` keys off the **last path segment only**, so `wnd[0]/.../txtFOO` and a popup's `wnd[1]/.../txtFOO` collapse to one name — the modal-dialog element is lost or mis-referenced. Nearly every transaction has an F4 / confirmation / "save changes?" popup, so this hits ordinary single-session recordings. See the graceful-handling list below. |
| Single-session only — Phase 4 | The parsers assume one `session` variable, so a recording that opened multiple *sessions* imports broken. Multi-session import (`openSession` / `switchSession` emission, absolute-id handling) is [Phase 4 scope](#deferred-phase-4--concurrent-sap-sessions); it reuses the Phase 3 window-id groundwork. Interim: record each session separately. |

Already-imported test cases in existing projects carry no `initConnection` step and depend
on `Browser = "SAP"`. They are covered by the **same Phase 3 migration** as hand-built SAP
cases — `Browser` set to `"No Browser"` and a blank `SAP.initConnection` injected as step 1 (default
connection). No separate import-migration path.

**Graceful multi-window (`wnd[0..n]`) handling — Phase 3:**

- **Keep every `wnd[N]` id verbatim** through parse → OR; never normalise to `wnd[0]` or drop
  higher windows.
- **Disambiguate names by window scope** — objects under `wnd[1]`, `wnd[2]`… get a scope
  prefix (`w1_`, `w2_`) or land in a per-window group ("Main window", "Popup 1"), so a
  popup's `txtFOO` never collides with the main screen's.
- **Runtime modal wait** — before resolving a `wnd[N>0]` object, briefly poll for that modal
  window to exist (popups render async) and retry rather than hard-fail.
- **Both id forms** — relative `wnd[1]/...` and absolute `/app/con[x]/ses[y]/wnd[1]/...`.
- This is a bug fix to the existing importer, independent of multi-*session*; the Phase 4
  multi-session work builds on the same window-id groundwork.

---

## Guardrails — what may share a SAP test case

The rule is one line: an archetype is blocked when it needs its own second live device,
broker or remote driver — SAP has nothing to do with it, which is why the same list would
block those archetypes from each other.

| Object type | In a SAP test case | Why |
|---|---|---|
| General, String Operations, Synthetic Data, Structured Data, File | works today | Driverless — no session at all. |
| Database, Webservice / API | works today | Per-action connection, opened from Settings like SAP now is. |
| Browser / Playwright / Web | **enabled** | The browser is the run target and launches the normal way; SAP no longer displaces it. |
| Image | **enabled** | Rides the browser driver when one is present. |
| Mobile / App | **blocked** | Needs a real device or emulator and an Appium session. |
| Kafka, Queue | **blocked** | Needs a live broker connection with its own lifecycle. |
| LambdaTest / Grid execution | **blocked** | The remote grid owns the driver; SAP's COM session is local-only. |
| ProtractorJS | **blocked** | Runs in a separate process. |

**Enforcement** — design-time: a non-blocking warning marker on a blocked step (reusing the
validation renderers) when the test has SAP steps. Run-time: a blocked step resolves to
`Status.FAILNS` with a message naming the fix, instead of a null-pointer. Grid: rejected at
launch when SAP steps are present.

---

## Deferred (Phase 4) — concurrent SAP sessions

**Later, additive.** One connection holds up to six sessions — independent transaction
contexts (create the order in one, check stock in another; see
[Connection vs session](#connection-vs-session)). This phase exposes them; it does not
restructure anything from Phases 1–3.

- **New actions** — `openSession` (`connection.createSession()` on the current connection,
  bound to a new label), `switchSession`, `closeSession`.
- **Session labels are runtime-only.** Unlike `#connection` aliases (which resolve to a
  `Settings/SAP/` config file), a session label is invented by the `openSession` step and
  lives only in `SapSessionManager` for the iteration. It takes the ordinary value sigils,
  not `#`: `openSession @stock` (literal), `openSession ${name}` (variable),
  `openSession stock` (datasheet column) — whatever it resolves to becomes the label.
  `initConnection` also labels its primary session (default: the connection name).
- **Step targeting** — the current label by default; an optional per-step or per-OR-page
  label pins a step/page to a session regardless of the current pointer (the OR `session`
  attribute reserved in Phase 1 holds that string).
- **Why it stays additive** — the manager is already an alias map and `CommandControl`
  already routes through `current()`. Phase 4 adds entries and switch actions; the ~64
  existing SAP actions are untouched.

**Constraints to hold the line on**: one STA thread means sessions are interleaved, not
truly concurrent — fine for test steps, not a parallelism feature. Parallel *test cases*
still need separate connections or systems, since two threads adopting one connection would
fight over its sessions. Six sessions per connection is a hard cap; `openSession` fails
gracefully past it. Modal dialogs are per-session.

**Scripting Tracker multi-session import** (Phase 4 scope). The current parsers
([`SapLanguageParser`](../IDE/src/main/java/com/ing/ide/main/sapscript/parser/SapLanguageParser.java))
assume a single `session` variable — every pattern is `session.findById("wnd[...]")`, so a
recording that used multiple sessions imports broken: absolute ids
(`/app/con[0]/ses[1]/wnd[0]/...`) don't match the `wnd[...]` patterns, and `createSession()`
lands in the "unrecognised method" branch. Phase 4 extends the parsers to:

- recognise `createSession()`, `session`-variable reassignment, and `con[x]/ses[y]/`
  prefixes;
- emit `SAP.openSession @s1` / `SAP.switchSession @s0` at each switch point, labels derived
  from the `ses[y]` index or `createSession` order;
- strip the `/app/con[x]/ses[y]/` prefix, store ids session-relative as today, and set each
  generated OR page's `session` attribute to the label current at record time.

Until then: record each session's flow as a **separate** recording → separate test case, and
compose them by hand with `openSession` + reusable calls; or keep recordings single-session.

---

## Dependency — JACOB

The COM bridge is already a managed Maven dependency in `Engine/pom.xml`:

```xml
<dependency>
    <groupId>io.github.osobolev</groupId>
    <artifactId>jacob</artifactId>
    <version>1.21</version>
</dependency>
```

That fork **bundles the native DLLs (x86 + x64) inside the jar and auto-extracts and loads
them at runtime**. There is no `.dll` anywhere in the repo. Consequences:

- **Users provide nothing** — no jacob jar, no DLL, no manual `lib/` folder. `mvn` pulls it
  transitively.
- `SAPSessionFactory` currently still sets `System.setProperty("jacob.dll.path", …)` and
  `java.library.path` at `lib/jacob-1.21/…` — paths that do not exist. This is dead
  pre-Maven wiring; **Phase 2 removes it** along with the `dllPath` / `libraryPath` config
  keys.
- Optional follow-up: a single `nativeExtractDir` override for locked-down machines that
  block writes to the default temp extraction directory.
- The native load still only works on Windows x64 with SAP GUI installed — the reason the
  test strategy below exists.

---

## Testing without a SAP environment

CI and dev machines here have neither SAP GUI nor the COM runtime, so almost all coverage
runs against a fake. The seam that makes this possible is a small interface layer that
confines every `com.jacob.*` import to one class.

| Interface | Real implementation | Fake (test) |
|---|---|---|
| `SapGuiSession` — `findById`, `startTransaction`, `sendVKey`, `statusBar()`, window / connection lifecycle | `JacobSapGuiSession` — wraps `ActiveXComponent` / `Dispatch`; the **only** file importing `com.jacob.*` | `FakeSapGuiSession` — in-memory element tree, no native code |
| `SapElement` — `setText`, `press`, `select`, `getProperty` / `setProperty` | jacob `Dispatch` wrapper | `FakeSapElement { id, type, text, props, children }` |
| `SapEngineLocator` — `GetROTEntry("SAPGUI")`, enumerate `GuiApplication.Children` | `JacobSapEngineLocator` | `FakeSapEngineLocator` — canned open connections / sessions to drive the whole adopt-or-launch decision table |

- `SapSessionManager` holds a `SapGuiSession`; tests inject the fake. `CommandControl` and
  `SAPActions` code against `SapGuiSession` / `SapElement`, never `Dispatch`.
- **Fake screen fixtures reuse the SAP OR page format** — a fake screen is loaded from a
  generated OR page (e.g. from a Scripting Tracker import), so import tests and action tests
  share one fixture shape. The fake simulates element-not-found, status-bar S/W/E messages,
  modal popups and transaction changes.
- **Effort** — `SAPObject` and all ~64 `SAPActions` use `Dispatch` directly today, so this
  is a real but mechanical refactor. Introduce the interfaces + `JacobSapGuiSession` in
  Phase 1 (the manager needs them anyway) with a `session.raw()` escape hatch; migrate
  actions off `raw()` through Phases 2–3.
- **Non-Windows safety** — nothing constructs `ActiveXComponent` at class-load (it is lazy
  inside `createSAPSession()` today and moves into `JacobSapGuiSession`). Any test that does
  exercise the real implementation self-skips with `assumeTrue(IS_OS_WINDOWS)`.
- **CI split** — fake-backed unit tests (routing, adopt-or-launch table, each action's
  effect on the fake tree, importer output) run on every build. Real-SAP tests are tagged
  `@Tag("sap-live")` behind a `-Psap-live` profile, excluded by default, run only on a
  self-hosted Windows + SAP runner if one exists.

The existing `SAPTestHelper.createMockSAPSession` (sets `session = null`) stays useful for
pure routing checks but is superseded by `FakeSapGuiSession` for anything touching elements;
the print-only `SAPActionRoutingTest` is rewritten against the fake.

---

## Known limitation — environment prerequisites

Admin policy locks down the SAP GUI registry on most managed estates, so the framework
**does not** write those keys. Enabling scripting is a one-time user/Basis task, documented
in the setup guide and the SAP Connections panel help text:

- **Server** — `sapgui/user_scripting = TRUE` (Basis team).
- **Each workstation** — SAP GUI Options → Accessibility & Scripting → Scripting: enable
  scripting; clear *"Notify when a script attaches to SAP GUI"* and *"Notify when a script
  opens a connection"*.
- **SSO users** — the SAP Logon entry for the connection must be SNC-configured (SNC name
  set, SSO library selected) so `OpenConnection` authenticates without a logon screen. This
  is workstation configuration the framework does not manage.

Because those notify dialogs can't be dismissed by a script, the engine **fails fast
instead of hanging**: if `GetScriptingEngine` reports disabled, `initConnection` errors
immediately with the fix; after `initConnection` / `openSession` a bounded poll (~10 s)
times out with *"No response from SAP GUI — a scripting notification dialog may be
blocking."*

---

## Compatibility — migration

**Decision: Option A — rewrite legacy projects to the new model.** The actual rewrite ships
as its **own follow-up feature**, not inside Phases 1–3. What these phases do is make the
code *ready* for it and keep legacy projects running until it lands.

### What Phases 1–3 provide

- **Runtime shim (Phases 1–3).** `Browser = "SAP"` is translated at load/run time to
  `"No Browser"` + an implicit `initConnection` to the default before the first SAP step.
  Nothing on disk changes. Every existing SAP project and test case keeps working unmodified.
- **A project-load migration extension point.** A `ProjectMigration` SPI run during
  `loadProject` — ordered, idempotent units of `appliesTo(project)` + `migrate(project)`,
  gated by a `sap.model` marker in project settings (`legacy` → `connection`) so a migration
  runs once and is skipped thereafter. The follow-up feature registers its unit here; no
  restructuring needed then.
- **Rewrite helpers.** Shared read/write utilities for the two artifact shapes a SAP
  migration touches — test-case CSV steps (inject `SAP.initConnection`) and per-test-set /
  per-test-case `Browser` assignments (`"SAP"` → `"No Browser"`) — so the migration unit is
  small and every migration reuses the same safe IO.
- **`Capabilities/SAP.properties`** is migrated to `Settings/SAP/SAP.properties` on project
  open (this one is cheap and in-scope now) and marked the project **default** connection.
  Legacy root `Settings/SAP.properties` is read as a fallback with a deprecation log line.
- The `Emulators.json` `SAP` row is left in place but no longer consulted.
- Feature flag `sap.connectionModel.enabled` gates the whole change for rollback.

### The follow-up feature (separate request)

Registers a `ProjectMigration` unit that, on first open of a `sap.model=legacy` project:
sets every `Browser = "SAP"` to `"No Browser"`, injects a blank `SAP.initConnection` as
step 1 of each affected test case, writes `sap.model=connection`, and reports *"N SAP test
cases updated"*. Once a project is migrated the shim no longer touches it; the shim itself is
removed after enough of the estate has converted. Scope for that request: read-only-project
guard, backup/undo, dry-run report, and CLI (`--migrate-sap`) for headless/bulk runs.

---

## Roadmap — phased plan

Phases 1–3 deliver both original requests; legacy projects keep running on the shim. Two
separate requests follow: the legacy project rewrite (Option A) and the Phase 4
multi-session enhancement — both additive, no rework of 1–3.

**Where we are:** Phases 1 and 2 are implemented, unit-tested against fakes, and pushed to
`task/sap-multi-conn-session-win`. Phase 3 (removing the legacy shim, guardrails, the
`ProjectMigration` SPI, and the Scripting Tracker importer changes) has not been started.

### Phase 1 — delivered

- **Delivers:** Multiple SAP connections selectable; SAP runs driverless; driverless actions
  already mix.
- **Key components:** `Settings/SAP` store · `SapConfigRegistry` + default resolution ·
  `SapSessionManager` (single entry) · `SapGuiSession` / `SapElement` / `SapEngineLocator`
  seam + `FakeSapGuiSession` · `initConnection` / `switchConnection` / `closeConnection` /
  `closeAllConnection` actions + claim counting + uninitialised-step fail-fast · `"SAP"`
  out of Run-button lists · legacy-`"SAP"` shim · `ObjectType.SAP` (session-level, registered
  for the IDE dropdown) split from new `ObjectType.SAP_OBJECT` (element-level, OR-resolved
  only, ~59 actions reclassified) · `InputType.OPTIONAL` on the three blank-capable connection
  actions · legacy `sapExecuteTransaction`/`sapEndTransaction`/`sapRefreshSession`/
  `sapCloseLogonScreen`/`sapSetglobalObjectProperty` reclassified from `BROWSER` to `SAP` ·
  `SapSessionManager.currentProcess()` restoring `CommandControl.SAPProcess` for
  `sapCloseLogonScreen`.
- **Risk:** Low — engine-local, flag-gated, shim keeps old projects running.

### Phase 2 — delivered

- **Delivers:** Connections managed in the IDE; the full adopt-or-launch table incl.
  session-only adoption; logon-screen fill; multi-logon auto-answer; dead JACOB wiring gone.
- **Key components delivered:**
  - **SAP Connections tab** in `DriverSettings` (pulled forward from its original slot;
    hand-built like `buildDevicesTab()` — not part of the generated form). Add / rename /
    delete a connection, edit its properties, set the project default. *Not yet built:* a
    "Test Connection" button (needs a background-thread COM call from the IDE — more risk,
    left for later) and `multiLogon`/`sessionMode` as dedicated dropdowns (both already work
    today as plain property rows in the same table).
  - **Full adopt-or-launch table with session-only adoption** — `SapEngineLocator.SapGuiEngine.findConnection()`
    / `AdoptedConnection` (`firstSession` / `firstIdleSession` / `createSession` /
    `sessionCount`) replace the Phase 1 "always open our own" shortcut.
    `SapSessionManager.open()` now: no match → open our own (owned connection + session);
    match found → `sessionMode` decides (`newSession` default: `createSession()` on the
    shared connection, we own only the session; `shareExisting`: drive the first idle
    session, own nothing; `requireOwn`: refuse and fail fast). 6-session cap fails fast
    pointing at `shareExisting`. Teardown gained `SapGuiSession.closeSessionOnly()` (ends
    just our session, `GuiConnection.CloseSession`) alongside the existing whole-connection
    `close()`.
  - **Logon-screen fill** — `SapSessionManager.attemptLogon()` detects the standard
    `RSYST-BNAME`/`RSYST-BCODE`/`RSYST-MANDT`/`RSYST-LANGU` logon fields via `findById`, fills
    them from the connection config when present, and is a no-op when they're absent
    (SSO/SNC already authenticated, or a session inherited from an already-logged-in
    connection). No credentials configured but the logon screen shows anyway → the
    documented SSO fail-fast.
  - **Multi-logon dialog auto-answer** — `SapSessionManager.handleMultiLogonDialog()`,
    checked independently of whether a logon screen appeared (SSO can still trigger it),
    detects the `MULTI_LOGON_OPT1/2/3` radio buttons and answers per `multiLogon`
    (`keepOthers` default / `endOthers` / `terminateThis` / `fail`). Field and dialog control
    ids are the standard SAP GUI Scripting ones documented across versions — verify against
    the target system if a client has customised them.
  - **Dead JACOB wiring removed** — `Capabilities.java` no longer seeds a
    `Capabilities/SAP.properties` file or exposes `ensureSAPCapabilitiesExist()`; the legacy
    `SAPSessionCreation` / `SAPSessionFactory` classes are deleted, along with the
    `CommandControl` ctor's now-unused 5th parameter and the print-only `SAPActionRoutingTest`
    / `SAPTestHelper` (superseded by the fakes).
  - **`sap-live` Maven profile** — `Engine/pom.xml` excludes `**/*LiveTest.java` from the
    default `mvn test`; `mvn test -pl Engine -Psap-live` re-includes them.
    `JacobSapEngineLocatorLiveTest` is the first: self-skips (not fails) off-Windows or
    without a reachable, scripting-enabled SAP GUI. Writing it surfaced a real bug fixed
    alongside it — `isRunningObjectTablePresent()` / `isScriptingEnabled()` caught `Exception`
    only, so a missing/mismatched jacob native binary raised an uncaught
    `UnsatisfiedLinkError` (an `Error`, not an `Exception`) instead of degrading to "no
    engine". Both now catch `Throwable`.
- **Risk:** Medium — the session-only-adoption and dialog-handling code paths are
  unit-tested against fakes (`FakeSap.Connection` / `.Element`) but not yet exercised against
  a real SAP GUI; the field/dialog ids are standard but worth confirming on first live run.

### Phase 3

- **Delivers:** Browser/Playwright + SAP in one case; guardrails; migration-ready.
- **Key components:** remove `Task` SAP branch · `CommandControl` routing via `current()` ·
  compatibility matrix · design-time warnings · runtime fail-fast · Grid guard ·
  `ProjectMigration` SPI on project load + `sap.model` marker + CSV / test-set rewrite
  helpers (the `Browser = "SAP"` rewrite unit itself is a separate follow-up) · runtime shim
  retained · Scripting Tracker importer emits `SAP.initConnection` / `SAP.closeConnection`
  pair, `"No Browser"` target, **multi-window (`wnd[1..n]`) import fix** — keep every window
  id, disambiguate names by window scope, runtime modal wait (optional: connection capture
  from the recording) · routing tests.
- **Risk:** Medium — touches shared execution path; needs regression coverage.

### Follow-up — legacy project rewrite (separate request)

- **Delivers:** `Browser = "SAP"` projects permanently converted to the connection model;
  shim removable.
- **Key components:** a `ProjectMigration` unit using the Phase 3 SPI and helpers · read-only
  guard · backup / dry-run report · `--migrate-sap` CLI · shim removal once the estate has
  converted.
- **Risk:** Low — isolated to one migration unit; guarded and reversible.

### Phase 4 — deferred

- **Delivers:** Concurrent SAP sessions addressed by alias.
- **Key components:** `openSession` / `switchSession` / `closeSession` · `createSession` +
  6-session cap · OR-page session binding · multi-session tests & docs.
- **Risk:** Low against 1–3 — additive; contained in the manager and new actions.

---

## Verification — test list

See [Testing without a SAP environment](#testing-without-a-sap-environment) for the fake
architecture these run on.

- `SapConfigRegistryTest` — store load, listing, alias resolution, default resolution
  (explicit flag, single-connection fallback, ambiguous → error), legacy-file migration.
- `SapSessionManagerTest` — `initConnection` blank vs `#alias`, adopt vs open vs launch
  decision, ownership flags, `closeConnection` blank (→ default) / `#alias`,
  `closeAllConnection`, auto-close backstop at iteration end, over a mocked Running Object
  Table.
- Claim counting — the `init → (reusable: init, close) → close` sequence: connection stays
  up after the reusable's `close`, is torn down on the parent's `close`; a reusable `close`
  with no matching `init` is a no-op; an unbalanced reusable `init` is swept by the backstop;
  an adopted connection is released but never physically closed.
- SAP action with no open connection → fail-fast *"add a `SAP.initConnection` step"*.
- `CommandControl` routing — real `sync()` cases replacing the print-only
  `SAPActionRoutingTest`: SAP step, Playwright step, Synthetic Data step and a blocked Kafka
  step in one test case; the same via a reusable (reusable carries its own claim-counted
  `initConnection` / `closeConnection`).
- Compatibility shim — a project with the legacy `Capabilities/SAP.properties` and
  `Browser = "SAP"` cases opens and runs unmodified: `"SAP"` is treated as `"No Browser"`
  with an implicit `initConnection` to the migrated default; nothing on disk changes.
- `ProjectMigration` SPI — units run in order on `loadProject`, are idempotent, and are
  skipped once the `sap.model` marker flips; the CSV / test-set rewrite helpers round-trip a
  file without collateral edits. (The `Browser = "SAP"` rewrite unit itself is tested with
  its own follow-up.)
- Scripting Tracker import — a sample `.ps1` / `.java` recording imports to a test case with
  a blank-input `SAP.initConnection` step 1 and blank-input `SAP.closeConnection` last step,
  run target `"No Browser"`, SAP OR page created; it executes standalone against the fake
  session on the default connection, and when called as a reusable inside an
  already-connected parent it adds/pops only its own claim.
- Multi-window import — a recording that drives a `wnd[1]` (and `wnd[2]`) popup imports with
  those elements present, uniquely named per window scope, and executes against a fake screen
  whose popup opens a step later (modal-wait retry, not fail).
- IDE — SAP Connections panel CRUD; the browser list no longer offers `SAP`; `"No Browser"`
  runs a browserless case.

---

## Open decisions

- **Adopt policy** — when a matching connection has several open sessions, adopt session 0,
  the active one, or always `createSession()`? (Pins down Phase 4 behaviour too.)

---

*Proposal for review — INGenious SAP testing. File and class names refer to the current
`tasks/sap-enhancement-supp-browser-actions` branch.*
