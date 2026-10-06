# User Story 9917379: [SAP] Support SAP Job Status Retrieval and Time-Based Validation

Source: `SAP Ingenious Issues 2026.xlsx` — sheet `SAP Requests` (Item #12), sheet `US`.

## User Story

As an INGenious engineer, I want to retrieve SAP job execution details and compare execution
durations, so that automated health checks can validate system performance and job execution
behavior.

## Acceptance Criteria (ATDD)

### Scenario 1: Retrieve SAP job details

**Given** an SAP job exists

**When** the execution retrieves job information

**Then** the job status and execution details are returned

### Scenario 2: Compare execution duration

**Given** start and end timestamps are available

**When** a duration comparison is executed

**Then** the calculated duration is compared against the configured threshold

### Scenario 3: Fail duration validation

**Given** a maximum execution duration is configured

**When** the actual duration exceeds the configured threshold

**Then** the test execution fails

### Scenario 4: Health-check the first N rows of a job table

**Given** a job status table/grid showing multiple job runs, and a configured row count, status
column, expected status, time column and interval

**When** the row health-check assertion is executed

**Then** it fails if any of the first N rows' status column does not equal the expected status
(e.g. a "Failed" job among "Finished" ones), or if the time gap between any two consecutive
rows' time column exceeds the configured interval (e.g. jobs expected every 2, 3 or 5 minutes)

## Related SAP Request Items

### Item #12

**Description:** Comparison of time - Compare time between each job is 3 minutes or Transaction
start time - end time and then compare end - start time > 1 minute.

**Resolution note (from tracker):** POC - possibility of Customization and Action to get status
details of Jobs in SAP.

## Context

Users need to retrieve SAP job execution details and calculate time differences between events
or jobs. The retrieved information should be available for comparisons against configurable
thresholds.

## Goal

Provide actions that retrieve job status information, execution timestamps, and allow
duration-based validation.

## Value

- Supports SAP health check automation.
- Reduces manual validation effort.
- Enables performance monitoring within automated tests.

## Scope

Affected Components:

- SAP Monitoring Actions
- Job Status Utilities
- Validation Framework
- Workflow Actions
- Shared Utilities

## Current State (gap analysis)

Job/status details in SAP (e.g. the `SM37` job overview) are only reachable through generic,
already-existing table/grid actions — there is nothing SAP-job-specific:

| Action | What it does | Gap |
|---|---|---|
| `sapStoreCellValueByIndexInVariable` / `sapStoreGridValueByIndexInVariable` | Reads a `row,column` cell (e.g. a job's `Start time`/`End time`/`Status` column) into a variable | Generic — works for a job list, but there's no purpose-built "get job status" action, and no concept of a job at all (just rows/columns). |
| `sapSelectTableRowByText` / `sapSelectGridRowByText` | Locates a row by a column's text (e.g. job name) | Covers "find this job's row", but the matched row **index is never exposed** - `findMatchingRow`/`selectRowByText`
  (`Engine/src/main/java/com/ing/engine/commands/SAP/SAPActions.java`) compute it internally and immediately
  call `SelectRow`, without an `addVar`. So there is no way to then read/assert a *different* column
  (Status, End time) of that same matched row without already knowing its numeric index - the by-text
  lookup can't be chained into a follow-up `GetCellValue`/assert today. |
| `sapExecuteTransaction` / `sapEndTransaction` | Start/end a transaction | Neither captures a timestamp - there is no automatic start/end time tracking around a transaction, so "transaction start time - end time" (Item #12) can't be measured without manually capturing text-based timestamps from the screen. |

Timestamp/duration plumbing exists but isn't exposed as a reusable action:

| Existing code | What it does | Gap |
|---|---|---|
| `GeneralOperations.storeEpochTimestampInVariable` | Captures the *current test-execution machine's* time as an epoch value | Captures "now" on the client, not a SAP-side timestamp read from a screen/job log, and doesn't compute a difference between two captured values. |
| `Command.before` / `Command.after` / `Command.duration` (static maps in `Engine/src/main/java/com/ing/engine/commands/browser/Command.java`) | Internally times webservice/queue request round-trips (`Instant.now()` before/after, `Duration.between(...).toMillis()`) | Not exposed as a user-facing action - it's wired only into the webservice/queue call path, so it can't be reused to time "job A end minus job B start" or "transaction start minus end" from SAP screen text. |
| `NumberConditionEvaluator` (`Engine/src/main/java/com/ing/engine/util/NumberConditionEvaluator.java`) | Compares a number against an operator + threshold, and now extracts embedded numeric tokens from text | Only understands numbers, not timestamps/durations (e.g. `"14:32:10"` or `"08.15.2026 14:32:10"`) - there is no timestamp-difference calculation anywhere in the codebase. |

## Proposed Actions

1. **Job status retrieval** — `SM37` and equivalent job/status lists are standard in every SAP
   system and render as either `GuiTableControl` or `GuiGridView`, both already handled by the
   generic row/cell actions built this session. Retrieving *multiple columns of the same matched
   row* (Status, Start time, End time) needed the row-index gap fixed first - **done**:
   - **`sapStoreCellValueByMatchInVariable`/`...Contains`** (table) and
     **`sapStoreGridValueByMatchInVariable`/`...Contains`** (grid) — find a row by one
     column (`matchColumn,matchText`, e.g. job name), then read a *different* `targetColumn`'s
     value from that same row into a variable, mirroring how `sapSetCheckBoxInMatchingRow`
     already finds-then-acts-on-another-column instead of just selecting the row.
   - **`sapStoreCellValueByMatchInDataSheet`/`...Contains`** (table) and
     **`sapStoreGridValueByMatchInDataSheet`/`...Contains`** (grid) — same lookup, storing
     into a datasheet cell instead of a variable.
   - Assertion is deliberately *not* a new one-step action: once the target column's value is in
     a variable, the existing `GeneralOperations.assertVariable` (equality) and
     `assertNumericValue` (operators, via `NumberConditionEvaluator`) already assert it -
     avoiding doubling the store actions into store+assert combinatorics. Resolves Scenario 1.

2. **`storeTimeDifferenceInVariable`** (`GeneralOperations`, general-purpose, not SAP-specific)
   — parses two timestamp strings (`Data` = `startTime,endTime` in a configurable format) and
   stores the difference (in seconds, matching `storeEpochTimestampInVariable`'s epoch-based
   plumbing) into a variable via `Condition`. Resolves Scenario 2's "calculated duration".

3. **`assertDurationCondition`** (`GeneralOperations`) — asserts a duration value against an
   operator + threshold expression (`Data` = e.g. `>60` for "more than 1 minute", `<=180` for
   "3 minutes or less"), reusing `NumberConditionEvaluator` for the comparison so the same
   `<>`/`>`/`<`/`>=`/`<=` vocabulary applies to durations as it does to other numeric
   assertions. Directly resolves Item #12 ("end - start time > 1 min", "each job is 3 minutes")
   and Scenario 3 (fails when the threshold is exceeded).

4. No new SAP-GUI-specific API surface is needed for reading job data - only the two new
   general-purpose time actions above, which slot in after job details are captured via the
   existing row/cell actions from item 1.

5. **`sapAssertTableRowsHealthy` / `sapAssertGridRowsHealthy`** (new, implemented) — a combined
   health check over the first N rows that resolves Scenario 4 directly, built from a request
   refinement during this session (compare the first 10 rows' status, and flag a run-time gap
   larger than a configurable interval):
   - `Data` = `statusColumn,expectedStatus,timeColumn,rowCount` — e.g.
     `Status,Finished,Last Run Time,10`. Columns are addressed by name via the existing
     `GetCellValue(row, column)` pattern, so differently-named or differently-ordered columns
     across SAP systems still resolve correctly, per the explicit requirement that column
     names/order aren't guaranteed to match.
   - `Condition` = `intervalMinutes` — e.g. `3`; a single value, consistent with how `Condition`
     is used everywhere else in this file. User-configurable (2, 3, 5 minutes, or any value), not
     hard-coded to the "3 minutes" in Item #12's wording. (`rowCount` moved from `Condition` into
     `Data` during refinement so `Condition` stays single-valued.)
   - Checks every row's status column against the expected value (a mismatch anywhere - e.g. a
     "Failed" row among "Finished" ones - fails the whole assertion, per "if there is failed in
     between then it should fail the assert"), and separately checks that the gap between each
     consecutive pair of rows' time column does not exceed the configured interval. All
     mismatches/gap violations are collected and reported together, not just the first one.
   - Time values are parsed trying `HH:mm:ss` then `HH:mm` (SAP's typical time-only column
     display, no date) - **confirmed against a real sample**: a job monitor screen (custom
     `ZF_GRID2SAP` report, not standard `SM37`) shows a `Start Time` column formatted exactly
     as `HH:MM:SS` (e.g. `00:00:55`), a separate `Start date` column, and a `Status` column with
     the literal values `Finished` (green)/`Released` - all matching this design's assumptions
     exactly, no changes needed.
   - This action reuses existing building blocks only (`RowCount`/`GetCellValue`, the
     `parseIntSafely` helper, `java.time.Duration`) - no change to the generic store actions from
     item 1.
   - **Report-specific shortcut spotted in the same sample**: that particular custom report
     already pre-computes a `Delay (sec.)` column per row (how far off each run was from its
     expected schedule). Where such a column exists, reading it directly via the existing
     `sapStoreCellValueByIndexInVariable`/`assertNumericValue` is cheaper than recomputing gaps
     - but standard `SM37` has no equivalent column, so `sapAssertTableRowsHealthy`/
     `sapAssertGridRowsHealthy`'s own gap computation remains the general-purpose path that
     works on any job table regardless of whether it happens to pre-compute delay itself.

## Open Questions

- What timestamp format(s) should `storeTimeDifferenceInVariable` accept - SAP's default
  `HH:MM:SS` / `DD.MM.YYYY` display formats, ISO-8601, epoch seconds, or a configurable pattern?
- Should the "job vs job" comparison (comparing two different jobs' timestamps) and the
  "transaction start vs end" comparison (one job/transaction's own two timestamps) be the same
  action, or does the two-jobs case need its own action to keep the `Data` format simple?
- Should a negative/zero duration (end time before start time, e.g. crossing midnight) be treated
  as a data error (`FAILNS`) or handled by wrapping the calculation into the next day?
- **`sapAssertTableRowsHealthy`/`sapAssertGridRowsHealthy` interval semantics** - implemented as
  "gap must not exceed the configured interval" (a maximum-allowed-gap health check). Not
  confirmed against a real screen whether "3 minutes interval" should instead mean an *exact*
  match (e.g. `== 3`, flagging gaps that are too small as well as too large) rather than a
  maximum - needs a real SM37-style sample to validate against.
- **Time format assumption** - **resolved for at least one real screen**: a sample `ZF_GRID2SAP`
  job monitor confirms `HH:mm:ss` (no date, no locale variation) for the time column. Still not
  confirmed whether every SAP job table (including standard `SM37` itself) uses the same format,
  so the `HH:mm`/`HH:mm:ss` fallback pair is kept rather than assuming only one.
- **Midnight rollover** - `Duration.between(previous, current)` on two `LocalTime` values doesn't
  account for a run crossing midnight (e.g. `23:59` → `00:01` would compute a large negative/huge
  gap, not a small one); not yet handled - same class of edge case flagged for
  `storeTimeDifferenceInVariable` above.
