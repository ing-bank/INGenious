# Reusable Components Directory Implementation Plan

## Goal
Replace the ReusableComponent.xml mechanism with a filesystem-based approach:
- TestPlan/ holds main scenarios and test cases.
- ReusableComponents/ holds reusable scenarios and test cases.
- Reusable status is inferred from the directory location.

## Scope
In scope:
- Datalib: scenario loading, test case loading, project model changes.
- IDE: tree models, UI actions, reusable creation, auto-suggest.
- Engine: runtime lookup for reusable steps.
- Migration of existing projects.

Out of scope:
- Changing CSV schema.
- Changing the reusable step format (Scenario:TestCase).

## High-Level Design
- Add a new top-level directory: ReusableComponents/.
- Introduce a dedicated reusable scenario list in Project:
  - main scenarios: TestPlan/.
  - reusable scenarios: ReusableComponents/.
- Keep scenario and test case CSV formats unchanged.
- Resolve reusable steps by searching only reusable scenarios.
- Enforce globally unique reusable scenario + test case combinations.

## Data Model Changes (Datalib)
1. Project
   - Add a new collection: reusableScenarios.
   - Add load method: loadScenariosFromReusableComponents().
   - Update loadProject(): load both TestPlan and ReusableComponents.
   - Add getReusableScenarios(), getReusableScenarioByName().
    - Update getScenarioByName() behavior to support the new rule:
       - reusable scenario + test case must be globally unique across the project.
2. Scenario
   - Add a constructor flag or field for source type: TESTPLAN or REUSABLE.
   - Update getLocation() to use the source directory.
   - Update rename/delete to apply within the correct directory.
3. TestCase
   - Keep existing isReusable() for now, but implement based on scenario source.
   - De-emphasize toggleAsReusable() and replace with move-based operations.

## File System Changes
- New directory created per project:
  - <project>/ReusableComponents/
- Migration moves CSV files from TestPlan/ into ReusableComponents/ when they are marked reusable in ReusableComponent.xml.
- If a scenario contains mixed reusable and non-reusable test cases, split into:
  - TestPlan/<Scenario>/ (non-reusable)
  - ReusableComponents/<Scenario>/ (reusable)

## IDE Changes
1. Tree Models
   - TestPlan tree should only use main scenarios.
   - Reusable tree should load from reusable scenarios.
   - Remove any XML usage in ReusableTreeModel.save().
2. Toggle Actions
   - Replace Make As Reusable with a move operation:
     - Move CSV from TestPlan/<Scenario>/ to ReusableComponents/<Scenario>/.
     - Update in-memory structures and reload trees.
   - Replace Make As TestCase with the inverse move.
3. Reusable Grouping
   - Remove XML-based grouping.
   - Reusable grouping will be by scenario folder only.
4. BDD Parser
   - Create StepDefinitions test cases under ReusableComponents instead of TestPlan.
5. Auto-Suggest
   - Suggest reusables from reusable scenarios only.

## Engine Changes
- Update reusable step resolution:
   - When parsing Scenario:TestCase, lookup only in reusable scenarios.
   - If not found, throw a clear error indicating missing reusable.

## Constants and Paths
- Add new reusable path in Engine:
  - FilePath.getReusableComponentsPath().
- Replace hardcoded "TestPlan" paths where reusable scenarios should use the new directory.

## Migration Plan
1. Detect legacy projects by presence of ReusableComponent.xml.
2. Parse XML once (existing parser can remain only in migration code).
3. For each reusable entry:
   - Move CSV into ReusableComponents/<Scenario>/.
4. If TestPlan/<Scenario>/ becomes empty, optionally remove it.
5. Rename ReusableComponent.xml to ReusableComponent.xml.bak.
6. Log a migration summary (counts of moved files).

## Validation Rules
- Enforce globally unique reusable scenario + test case combinations at creation time.
- On load, if duplicates exist, surface a clear error message and prevent execution.

## Risks and Mitigations
- Risk: scenario name collisions.
  - Mitigation: enforce uniqueness at create/rename and fail-fast on load.
- Risk: migration moves large numbers of files.
  - Mitigation: back up XML as .bak and log all file moves.
- Risk: IDE toggle behavior becomes slower.
  - Mitigation: perform file moves asynchronously or show progress if needed.

## Testing Plan
1. Unit tests (Datalib)
   - Project.load() loads scenarios from both directories.
   - Scenario.getLocation() uses correct directory.
   - Reusable lookup uses reusable scenarios first.
2. Unit tests (IDE)
   - Toggle Make As Reusable moves CSV correctly.
   - Reusable tree reflects file system after reload.
3. Migration tests
   - Project with ReusableComponent.xml migrates successfully.
   - Mixed scenario split is handled correctly.
4. Manual tests
   - Create new reusable flow in IDE.
   - Execute test case that calls a reusable step.
   - Run a TestSet and verify execution of reusable steps.

## Rollout Steps
1. Implement core data model changes in Datalib.
2. Update Engine reusable step lookup.
3. Update IDE tree models and toggle actions.
4. Add migration logic and run migration against sample projects.
5. Validate with existing sample projects (Tutorial, Mobile, ING Mortgage Calculator).

## Open Decisions
- Resolved: reusable scenario + test case combination must be globally unique across the project.
- Resolved: reuse references are restricted to ReusableComponents only.
