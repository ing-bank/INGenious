# INGenious ↔ Test Manager Integration — Detailed Implementation Plan

> **Purpose**  
Integrate **INGenious** test set executions with **LambdaTest Test Manager** so that:
- required entities (Scenario, Test Case, Release, Test Set) are **found or created** idempotently,
- a **Test Run** is created under the correct Test Set,
- test cases are **added** to that run,
- **test case instance statuses** are updated from INGenious execution results,
- the **overall run** is marked Passed/Failed,
- the workflow is supported with **clean logging, pretty reporting, retries, and circuit breakers**.

---

## 0. Terminology & IDs (Quick Reference)

- **Scenario** (INGenious) → **Folder** (Test Manager `entity_type=project`) → `scenarioFolderId`
- **Test Case** (INGenious) → **Test Case** (Test Manager) → `testCaseId`
- **Release** (INGenious) → **Test Run Folder** (Test Manager `entity_type=test_run`) → `releaseFolderId`
- **Test Set** (INGenious) → **Child folder under Release folder** → `testSetFolderId`
- **Test Run** (Test Manager execution container) → `testRunId`
- **Test Case Instance** (a test case row inside a given run) → `testCaseInstanceId`

> ⚠️ Important distinction: in Step **3b** you fetch **testCaseInstanceId** from `test_run_instances.data[].id`.

---

## 1. Execution Context & Inputs from INGenious

When a test set execution is triggered from INGenious, you have:

- **Release Name**
- **Test Set Name**
- As each test case executes:
  - **Scenario Name**
  - **Test Case Name**
  - **Execution result** (Passed/Failed)
  - (Optional) **Tags** for metadata alignment in Test Manager

---

## 2. Step 1 — Verify / Create Required Entities (Idempotent Pre-flight)

### Goal
Ensure these pairs exist:
- **Scenario + Test Case**
- **Release + Test Set**

> **Idempotency rule**: Always **lookup before create**. Do not create duplicates on re-run.

---

### 2.1 Step 1a — Check if Scenario Exists (Folder Lookup & Create)

#### API: List folders under a project
**GET**
```http
https://test-manager-api.lambdatest.com/api/v1/folder/entity/{projectID}
```

#### Matching rules
- Traverse `data[]` objects.
- Match if `name == INGenious Scenario Name`.
- Also check `children[]` (and recursively if nested).

#### If Scenario found
- Store:
  - `scenarioFolderId = matched.id`

#### If Scenario not found → create
**POST**
```http
https://test-manager-api.lambdatest.com/api/v1/folder
```

Payload:
```json
{
  "folders": [
    {
      "name": "{INGenious Scenario Name}",
      "entity_id": "{projectID}",
      "entity_type": "project"
    }
  ]
}
```

Response (example):
```json
{
  "message": "Folders created successfully",
  "type": "Success",
  "id": "01KSG6XSAPPBPH901ZAECJ0WFS"
}
```

Store:
- `scenarioFolderId = $.id`

---

### 2.2 Step 1b — Check if Test Case Exists (Folder-Level Lookup & Create)

> `{folderID}` is the same as `scenarioFolderId`.

#### API: List test cases in a folder
**GET**
```http
https://test-manager-api.lambdatest.com/api/v1/projects/{projectID}/folder/{folderID}/test-cases
```

#### Matching rules
- Iterate `data[]`
- Match if `title == INGenious Test Case Name`

#### If Test Case found
- Store:
  - `testCaseId = matched.test_case_id`

#### If Test Case not found → create
**POST**
```http
https://test-manager-api.lambdatest.com/api/v1/test-cases
```

Payload:
```json
{
  "project_id": "{projectID}",
  "folder_id": "{folderID}",
  "test_cases": [
    {
      "title": "{Test Case Name in INGenious}"
    }
  ]
}
```

Response (example):
```json
{
  "message": "Test cases created successfully",
  "type": "Success",
  "id": [
    "01KSG7TM0C3KZGYK2J1BA8N1AP"
  ]
}
```

Store:
- `testCaseId = $.id[0]`

---

### 2.3 Step 1b.1 — Get Test Case Snapshot (`snapshot_id`)

#### Objective
Retrieve current `snapshot_id` for the test case. Required for v2 update (optimistic concurrency).

#### API: Get test case (v2)
**GET**
```http
https://test-manager-api.lambdatest.com/api/v2/test-cases/{testCaseId}
```

Store:
- `testCaseSnapshotId = $.data.snapshot_id`

---

### 2.4 Step 1b.2 — Update Test Case Using `snapshot_id` (Metadata Alignment)

#### Objective
Update test case metadata (status, automation status, tags, commit message) using `snapshot_id`.

#### API: Update test case (v2)
**PUT**
```http
https://test-manager-api.lambdatest.com/api/v2/test-cases
```

Payload:
```json
{
  "title": "{Same as the TestCase Name}",
  "id": "{testCaseId}",
  "project_id": "{projectID}",
  "status": "Ready",
  "automation_status": "Automated",
  "tags": [
    "{INGenious tag1}",
    "{INGenious tag2}",
    "{INGenious tag3}"
  ],
  "commit_message": "Updated test case",
  "snapshot_id": "{testCaseSnapshotId}",
  "override": false
}
```

Response (example):
```json
{
  "message": "Test case updated successfully",
  "type": "Success",
  "data": {
    "snapshot_id": "01KSGD868657WN4EQYX1FZQSBA"
  }
}
```

Store:
- `updatedTestCaseSnapshotId = $.data.snapshot_id`

**Guardrail (concurrency)**  
If update fails due to stale snapshot:
1) Re-run Step **1b.1** to fetch a fresh snapshot  
2) Retry update once

**Guardrail (tags overwrite vs merge)**  
If you want to merge tags instead of overwrite:
- Read existing tags from Step 1b.1 response and union them with INGenious tags before PUT.

---

### 2.5 Step 1c — Check if Release Exists (Test Run Folder Lookup & Create)

#### API: List test run folders (Release folders)
**GET**
```http
https://test-manager-api.lambdatest.com/api/v1/folder/test-run/entity/{projectID}
```

#### Matching rules
- Iterate `data[]`
- Match if `name == INGenious Release Name`

#### If Release found
- Store:
  - `releaseFolderId = matched.id`

#### If Release not found → create
**POST**
```http
https://test-manager-api.lambdatest.com/api/v1/folder/test-run
```

Payload:
```json
{
  "folders": [
    {
      "name": "{INGenious Release Name}",
      "description": "",
      "entity_id": "{projectID}"
    }
  ]
}
```

Response:
```json
{
  "message": "Folders created successfully",
  "type": "Success",
  "id": "01KSG8BKPJS01WRMFSYRZKKERZ"
}
```

Store:
- `releaseFolderId = $.id`

---

### 2.6 Step 1d — Check if Test Set Exists (Child Folder Under Release)

#### API: List test run folders (same endpoint as Step 1c)
**GET**
```http
https://test-manager-api.lambdatest.com/api/v1/folder/test-run/entity/{projectID}
```

#### Matching rules
1. Find the release object in `data[]` where:
   - `id == releaseFolderId`
2. Check `children[]` for a child where:
   - `child.name == INGenious Test Set Name`

#### If Test Set found
- Store:
  - `testSetFolderId = child.id`

#### If Test Set not found → create under release
**POST**
```http
https://test-manager-api.lambdatest.com/api/v1/folder/test-run
```

Payload:
```json
{
  "folders": [
    {
      "name": "{TestSet Name of INGenious}",
      "entity_id": "{projectID}",
      "parent_id": "{releaseFolderId}"
    }
  ]
}
```

Response:
```json
{
  "message": "Folders created successfully",
  "type": "Success",
  "id": "01KSGATW5TQ9HW01G9QDBE5MEJ"
}
```

Store:
- `testSetFolderId = $.id`

---

## 3. Step 2 — Create a Test Run under the Test Set

#### API: Create test run
**POST**
```http
https://test-manager-api.lambdatest.com/api/v1/test-run
```

Payload:
```json
{
  "title": "Run_<prettyformat_date_time_stamp>",
  "project_id": "{projectID}",
  "folder_id": "{testSetFolderId}"
}
```

Response:
```json
{
  "message": "Test run created successfully",
  "type": "Success",
  "id": "01KSGB5X4V72PW1WDJEBYF4XSC"
}
```

Store:
- `testRunId = $.id`

---

## 4. Step 3a — Add Test Cases to the Test Run (Folder Selections)

#### Objective
Bind discovered/created test cases into the run, grouped by scenario folders.

#### API: Update test run selections
**PUT**
```http
https://test-manager-api.lambdatest.com/api/v1/test-run/{testRunId}
```

Payload:
```json
{
  "title": "Run_<prettyformat_date_time_stamp same as used to create the Test Run>",
  "objective": "",
  "folder_selections": {
    "{scenarioFolderId1}": {
      "selected_testIds": [
        "{testCaseId1}",
        "{testCaseId2}"
      ]
    },
    "{scenarioFolderId2}": {
      "selected_testIds": [
        "{testCaseId3}",
        "{testCaseId4}"
      ]
    }
  },
  "project_id": "{projectID}"
}
```

Response:
```json
{
  "message": "Test run updated successfully",
  "type": "Success"
}
```

**Guardrail**
- Title must match the one used in Step 2.
- Treat as desired-state update (idempotent).

---

## 4.1 Step 3b — Fetch Test Case Instance IDs from the Test Run

#### Objective
Obtain **test case instance IDs** for per-instance status updates.

#### API: Get instances for a test run
**GET**
```http
https://test-manager-api.lambdatest.com/api/v1/test-run/instances/{testRunId}
```

#### Extraction
Iterate:
- `test_run_instances.data[]`

For each item store:
- `testCaseInstanceId = data[i].id`
- `testCaseId = data[i].test_case_id`

Suggested mapping:
- `testCaseInstanceIdByTestCaseId[testCaseId] = testCaseInstanceId`

If a test case appears multiple times (multi-environment), store list:
- `testCaseInstanceIdsByTestCaseId[testCaseId] = [id1, id2, ...]`

---

## 4.2 Step 3c — Update Each Test Case Instance Status (Passed/Failed)

#### Objective
For each `testCaseInstanceId`, update execution status to match INGenious.

#### API: Update test instance
**PUT**
```http
https://test-manager-api.lambdatest.com/api/v1/test-run/instance/{testCaseInstanceId}
```

Payload:
```json
{
  "status": "Passed"
}
```

Response:
```json
{
  "message": "Test instance updated successfully",
  "type": "Success"
}
```

#### Processing logic
For each `testCaseInstanceId`:
- determine INGenious result,
- map it to Test Manager values,
- PUT status update.

Minimum mapping:
- INGenious Pass → `Passed`
- INGenious Fail → `Failed`

---

## 4.3 Step 4 — Mark the Overall Test Run Status (Final Step)

#### Objective
Mark run as:
- **Passed** if **all** test case instances are Passed
- **Failed** if **any** test case instance is Failed

#### API: Update test run status
**PUT**
```http
https://test-manager-api.lambdatest.com/api/v1/test-run/status/{testRunId}
```

Payload:
```json
{
  "status": "Passed"
}
```

Response:
```json
{
  "message": "Test run status updated successfully",
  "type": "Success"
}
```

---

## 5. End-to-End State to Track (In-Memory / Per Execution)

At minimum keep these keys:

- `projectID`
- `correlationId` (one per INGenious execution)
- `releaseFolderId`
- `testSetFolderId`
- `testRunId`
- For each scenario:
  - `scenarioName → scenarioFolderId`
- For each test case:
  - `(scenarioFolderId, testCaseName) → testCaseId`
  - `testCaseId → testCaseSnapshotId` (and updated snapshot)
- For publishing:
  - `testCaseId → testCaseInstanceId(s)`
  - `testCaseInstanceId → status`

---

# 6. Observability & Resilience (Logging, Pretty Reporting, Retries, Circuit Breakers)

This workflow touches multiple APIs and must be **operationally robust**. Implement:

- **Structured logging** (machine readable)
- **Pretty reporting** (human readable)
- **Retries** for transient failures
- **Circuit breakers** for upstream issues
- **Step-level failure handling** (abort vs continue policy)

---

## 6.1 Clean Structured Logging (Actionable + Consistent)

### Logging goals
- Trace a single execution end-to-end
- Answer “found vs created vs updated” quickly
- Diagnose failures fast without exposing secrets

### Required correlation fields
- `correlationId` (one per INGenious test set execution)
- `runTitle` (generated)
- `testRunId` (once created)
- `releaseFolderId`, `testSetFolderId`
- `scenarioFolderId`, `testCaseId`, `testCaseInstanceId` (as discovered)

### Standard event naming
- `tm.step.start`
- `tm.api.request`
- `tm.api.response`
- `tm.step.success`
- `tm.step.fail`
- `tm.step.warn`
- `tm.circuit.open`

### Example structured log event (recommended JSON)
```json
{
  "timestamp": "2026-05-25T19:59:00Z",
  "level": "INFO",
  "event": "tm.step.success",
  "correlationId": "<guid>",
  "step": "1b",
  "action": "testcase.lookup",
  "outcome": "FOUND",
  "timingMs": 143,
  "entity": {
    "projectId": "<projectID>",
    "scenario": "<ScenarioName>",
    "scenarioFolderId": "<scenarioFolderId>",
    "testCase": "<TestCaseName>",
    "testCaseId": "<testCaseId>"
  },
  "message": "Test case exists; using existing testCaseId"
}
```

### Redaction
- Never log tokens/headers
- Mask sensitive identifiers if required by policy

---

## 6.2 Pretty Reporting (End-of-Run “CI Artifact”)

Generate a single **Markdown or HTML report** per execution for humans.

### Suggested report structure
1. **Header**
   - correlationId, start/end time, duration
   - pipeline link, commit SHA, environment
2. **Step outcomes table**
   - step, outcome (FOUND/CREATED/UPDATED/SKIPPED/FAILED), timing, key IDs
3. **Entity map**
   - releaseFolderId, testSetFolderId, testRunId
4. **Results rollup**
   - passed/failed/skipped counts + optionally link to Test Manager UI
5. **Failures**
   - failing step, endpoint, HTTP code, error message, retry count, recommended action

### Example “console-friendly summary”
```text
INGenious → Test Manager publish: SUCCESS
CorrelationId: 9f2e... | Run: Run_20260525_2014 | testRunId: 01KSGB...
Entities: Release=FOUND, TestSet=CREATED, Scenarios=3 (2 found/1 created), TestCases=12 (10 found/2 created)
Results: Passed=11 Failed=1 Skipped=0 | Duration: 00:01:42
```

---

## 6.3 Retry Policy (Per API Call)

Retries should be used only for **transient** failures.

### Defaults
- **Timeouts**
  - connect: 2–5s
  - read: 10–30s (tune per endpoint)
- **Retries**: 2–3 attempts with exponential backoff + jitter
- Retry on:
  - `429`, `408`, and most `5xx`
- Do not retry on:
  - `400/401/403` (fix config/auth)
  - most `404` (unless you expect eventual consistency)
  - schema/validation errors

---

## 6.4 Circuit Breakers (Fail Fast and Protect Dependencies)

Implement a circuit breaker per upstream host (Test Manager API) or per endpoint group.

### Suggested breaker config
- Window: 30–60 seconds
- Open when:
  - failure rate ≥ 50% with at least N=5 calls in window, OR
  - consecutive failures ≥ 5
- Open duration: 60 seconds
- Half-open: allow 1–3 trial requests

### When breaker is OPEN
- Skip further calls for this execution
- Mark report as **INCOMPLETE**
- Emit one high-signal event: `tm.circuit.open`

---

## 6.5 Step-Level Failure Handling Policy

### General rule
If a step fails and the next steps depend on its IDs → **abort** publishing.

### Recommended criticality
- **Critical (abort workflow)**:
  - Step 1a, 1b, 1c, 1d
  - Step 2, Step 3a, Step 3b
- **Per-instance critical**:
  - Step 3c: allow partial updates but record failures; final run status should reflect them
- **Tunable / optional**:
  - Step 1b.2 (metadata update): can be `WARN` and continue if metadata alignment is not mandatory

---

## 6.6 Where to Log in Each Step (Checklist)

For every step:
- `tm.step.start` (inputs + correlationId)
- `tm.api.request` (method/path/attempt)
- `tm.api.response` (status code + timing)
- `tm.step.success|fail` (outcome + ids)

For lookups:
- `outcome = FOUND | NOT_FOUND`

For creates:
- `outcome = CREATED` + returned IDs

For status updates:
- include `testCaseInstanceId`, `oldStatus` (if known), `newStatus`, and INGenious source result

---

## 7. Open Items / Optional Extensions

- Support more statuses (Skipped / Blocked / Not Run) with explicit mapping
- Attach evidence (logs/screenshots/videos) if Test Manager supports an attachment API
- Parallelization strategy for large runs (with rate limiting and backpressure)
- Persistent caching layer for entity lookups to reduce API calls across executions

---

*Document maintained incrementally. Last updated: 2026-05-25T21:09:11Z*
