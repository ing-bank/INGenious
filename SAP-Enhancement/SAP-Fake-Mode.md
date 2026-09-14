# SAP fake mode — running the real app without SAP GUI

Companion to `SAP-Connections-Redesign.md` §"Testing without a SAP environment", which
covers the **unit-test** fake seam. This doc covers the separate, build-time toggle that
lets you run the **actual INGenious app** (the real IDE/UI, or a CLI run) against that same
fake — no SAP GUI, no COM, no JACOB — so you can exercise SAP test cases end-to-end on a
machine that doesn't have SAP installed.

## TL;DR

```
mvn clean install -Dsap.fakeMode=true
```

then launch normally — `Dist\release\ingenious.bat`, no args, no extra flags. That build's
SAP actions now run against an in-memory fake instead of a real SAP GUI connection.

A plain `mvn clean install` (no property) always produces a normal, real-SAP build. There is
**no runtime flag** — you cannot toggle this on an already-built jar; the behavior is fixed
at the moment it was compiled.

## Why build-time, not a launch flag

The toggle is deliberately baked in at build time rather than read from a JVM system
property at launch (e.g. `java -Dsap.fakeMode=true`). That means:

- A normal `mvn clean install` — what any release/CI pipeline runs by default — can **never**
  accidentally ship a fake-mode build, because nothing reads an env var or launch flag to
  decide.
- Whoever wants a fake-mode build has to explicitly ask for it at build time. There is no
  ambiguity about which mode a given jar is in once it exists.
- `ingenious.bat` / `ingenious.command` need zero changes — they just launch whatever was
  built.

## How it works

| Piece | What it does |
|---|---|
| `Engine/pom.xml` — `<sap.fakeMode>false</sap.fakeMode>` property | Default `false`. Overridden per-build with `-Dsap.fakeMode=true`. |
| `Engine/src/main/resources/sap/sap-build.properties` | `sap.fakeMode=${sap.fakeMode}` — filtered by Maven at `process-resources` time (the `src/main/resources` tree is already filtered project-wide), so the real value is baked into the packaged jar. |
| `SapSessionManager.loadFakeModeFlag()` | Reads `/sap/sap-build.properties` off the classpath once, at class-init, via `getResourceAsStream`. Missing/unreadable resource → `false`. |
| `SapSessionManager.defaultLocatorFactory()` | Returns `FakeSap.Locator::new` when the baked flag is `true`, otherwise `JacobSapEngineLocator::new` (the real COM-based implementation) — unchanged from before. |
| `FakeSap` (`Engine/src/main/java/com/ing/engine/drivers/sap/FakeSap.java`) | The same in-memory fake used by `SapSessionManagerTest` / `CommandControlSapGuardrailTest`. It moved from `src/test` to `src/main` (same package, so the tests needed no changes) specifically so production code — and now fake-mode app runs — can reach it. It stays inert in a normal build: nothing selects it unless `FAKE_MODE` is `true`. |

`SapSessionManager` logs a loud warning the moment fake mode is detected:

```
WARNING: ⚠ SAP FAKE MODE ACTIVE - this build was made with -Dsap.fakeMode=true.
SAP actions will run against an in-memory fake, not a real SAP GUI connection.
```

Look for that line in the console/log on startup to confirm which mode a given build is in.

## Building and running

```
# From the repo root — builds every module, including Dist/release
mvn clean install -Dsap.fakeMode=true

# Or scoped to just what you need rebuilt
mvn clean install -pl Engine,IDE,Dist -am -Dsap.fakeMode=true
```

Then launch exactly as you normally would:

- **Windows**: double-click `Dist\release\ingenious.bat`, or run it with no args from a
  terminal, to open the IDE.
- **IntelliJ**: if you'd rather iterate without a full `mvn install` each time, you can run
  `Main` directly — but note the flag only takes effect through the *build*, not a VM option.
  The simplest IntelliJ workflow is still: run `mvn clean install -Dsap.fakeMode=true` once
  from a terminal, then point your run configuration's classpath at the resulting
  `Engine`/`IDE` module output (which now contains the baked `true` resource).

To go back to a real build: `mvn clean install` (no property), or explicitly
`-Dsap.fakeMode=false`.

## What you get

Once running against the fake:

- `SAP.initConnection`, `SAP.openSession`, `SAP.switchSession`, `SAP.closeSession`,
  `SAP.closeConnection` and the transaction/element actions in `SAPActions.java` all route
  through `FakeSap.Locator` / `FakeSap.Session` / `FakeSap.Element` — no SAP GUI process, no
  COM, no scripting registry keys needed.
- By default the fake's `Locator.rotPresent = false` and no elements are preset, so
  `initConnection` "launches" (a no-op in the fake) and opens a session with an empty element
  tree — `findById` on any id returns `null` unless you've preset it.
- `Session.connectionInfo()` reports `"FAKE/000/TESTER"`, a quick visual tell in reports/logs
  that you're on the fake.
- Guardrail routing (which archetypes can share a SAP test case — see
  `CommandControlSapGuardrailTest`) still applies exactly as with a real connection, since
  it's driven by `SapSessionManager.hasConnection()`, not by which locator is behind it.

## Limitations

- The fake has no real screens. `SAP.findById` / element-based actions against fake ids that
  were never preset in code just return `null` / no-ops — there's no logon screen, no actual
  transaction UI, and no verification of GUI element ids beyond what
  `SapSessionManagerTest`-style presets would configure programmatically. It's meant for
  exercising **routing, session/connection lifecycle, and guardrails**, not for validating
  real transaction screens or real element ids from a Scripting Tracker recording.
- It's a single build-time switch for the whole jar — you can't mix real and fake connections
  in the same running app.
- CI/release pipelines should never pass `-Dsap.fakeMode=true`; verify your pipeline config
  doesn't set it if you're relying on the "never ships in fake mode by accident" guarantee.

## See also

- `SAP-Enhancement/SAP-Connections-Redesign.md` — "Testing without a SAP environment" (unit
  tests), "Dependency — JACOB", "Known limitation — environment prerequisites".
- `Engine/src/test/java/com/ing/engine/drivers/sap/SapSessionManagerTest.java` and
  `Engine/src/test/java/com/ing/engine/core/CommandControlSapGuardrailTest.java` — the unit
  tests exercising the same `FakeSap` seam this mode reuses.
