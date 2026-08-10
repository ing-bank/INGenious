# INGenious 3.0 — Grouping and Persistent Sort

Generated: 2026-06-28

## Table of Contents

1. [Test Plan Scenario Groups and Persistent Ordering](#1-test-plan-scenario-groups-and-persistent-ordering)
2. [Test Design Tree and DnD Group Management](#2-test-design-tree-and-dnd-group-management)
3. [Reusable Trees Keep Group Controls Hidden](#3-reusable-trees-keep-group-controls-hidden)
4. [Workbench UI Refresh](#4-workbench-ui-refresh)
5. [Scenario and Sort Order Storage](#5-scenario-and-sort-order-storage)
6. [Scenario Group Tutorial](#6-scenario-group-tutorial)

---

---

## 1. Test Plan Scenario Groups and Persistent Ordering

### What changed

The Test Plan tree now supports real scenario groups instead of only a flat scenario list. Scenarios can be organized into named groups, a built-in `(Ungrouped)` bucket keeps unassigned scenarios visible, and the tree restores that structure when a project is reopened. Group and scenario ordering is also persisted so the Test Plan keeps the same shape after restart.

---

### Root cause

Before this branch, Test Plan scenarios were loaded directly from disk in flat order and there was no persistence layer for group membership or group ordering. The tree model had no intermediate group node, so the IDE could not represent named group buckets, could not remember the last arrangement, and could not restore a grouped hierarchy from project files.

---

### Fix

A new Test Plan grouping model was added in the IDE and Datalib layers:

1. `ScenarioGroup` now represents a named group and its ordered list of scenario names.
2. `ScenarioGroupStore` persists those groups to `<project>/TestPlan/.groups` and reloads them on startup.
3. `TestPlanNode` now reads the stored groups and builds either a flat tree or a grouped tree with `TestPlanGroupNode` children.
4. `TestPlanTreeModel` manages group creation, rename, delete, and scenario moves, while keeping the `.groups` file in sync.
5. `ProjectTree` exposes the new group actions, persists sort order after edits, and routes drag-and-drop into the grouped Test Plan model.

When grouping is active, every ungrouped scenario stays visible under `(Ungrouped)`, and the tree remains backward-compatible for projects that do not yet have a `.groups` file.

---

### Files changed

| File | Status |
|---|---|
| [Datalib/src/main/java/com/ing/datalib/component/ScenarioGroup.java](Datalib/src/main/java/com/ing/datalib/component/ScenarioGroup.java) | Added |
| [Datalib/src/main/java/com/ing/datalib/component/utils/ScenarioGroupStore.java](Datalib/src/main/java/com/ing/datalib/component/utils/ScenarioGroupStore.java) | Added |
| [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/model/TestPlanNode.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/model/TestPlanNode.java) | Added |
| [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/model/TestPlanGroupNode.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/model/TestPlanGroupNode.java) | Added |
| [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/model/TestPlanTreeModel.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/model/TestPlanTreeModel.java) | Added |
| [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ProjectTree.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ProjectTree.java) | Modified |
| [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ProjectDnD.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ProjectDnD.java) | Modified |

**Breaking changes**: none.

---

## 2. Test Design Tree and DnD Group Management

### What changed

The Test Design tree now understands Test Plan groups as first-class nodes. Users can create a new group from the project root, rename or delete groups, move scenarios between groups, and drag scenarios directly onto a target group. The context menu, icons, selection logic, and persistence flow were updated to support those actions.

---

### Root cause

The old tree code treated every Test Plan scenario as a direct child of the project root. That meant there was no intermediate group node to select, no group-specific context menu, no way to move scenarios into a named bucket, and no persistence hook to save the new ordering after a drag, rename, or delete.

---

### Fix

`ProjectTree` and `ProjectDnD` were extended to work with the new grouped model:

1. The Test Plan popup menu now offers `New Group`, `Rename Group`, and `Delete Group` when a group node is selected.
2. Drag-and-drop recognizes a Test Plan group as a valid drop target and moves selected scenarios into it.
3. Sort order is persisted after scenario creation, deletion, rename, and rearrangement so the tree reopens in the same order.
4. The renderer now uses a dedicated Test Plan group icon so grouped nodes are visually distinct from scenarios.
5. Group actions are routed through `TestPlanTreeModel`, which updates both the tree and the on-disk `.groups` file.

The result is a complete grouped Test Plan workflow rather than a one-off visual change.

---

### Files changed

| File | Status |
|---|---|
| [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ProjectTree.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ProjectTree.java) | Modified |
| [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ProjectDnD.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ProjectDnD.java) | Modified |
| [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/model/TestPlanGroupNode.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/model/TestPlanGroupNode.java) | Added |
| [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/model/TestPlanTreeModel.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/model/TestPlanTreeModel.java) | Added |
| [IDE/src/main/java/com/ing/ide/main/fx/INGIcons.java](IDE/src/main/java/com/ing/ide/main/fx/INGIcons.java) | Modified |

**Breaking changes**: none.

---

## 3. Reusable Trees Keep Group Controls Hidden

### What changed

Reusable and Shared Reusable trees were updated so they keep their existing grouping affordances hidden while still benefiting from the new sort-order persistence. Creating scenarios and test cases in those trees now saves the current order immediately, but the Test Plan group UI is not exposed there.

---

### Root cause

The branch introduced group-related UI and persistence hooks at the shared `ProjectTree` level, which meant the reusable trees needed an explicit opt-out. Without that guard, the new group controls could have appeared in views where the product still expects the classic reusable-folder behavior.

---

### Fix

`ReusableTree` was updated to persist scenario and test case ordering after edits, and it now forces the group-related menu items off in every relevant selection state. That keeps the reusable views aligned with their existing workflow while still benefiting from the sort-order storage work introduced in this branch.

---

### Files changed

| File | Status |
|---|---|
| [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ReusableTree.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ReusableTree.java) | Modified |

**Breaking changes**: none.

---

## 4. Workbench UI Refresh

### What changed

The old API Workbench entry was turned into a more prominent Workbench experience across the menu bar, dock, toolbar, and status bar. The button label, icon treatment, and visual styling were refreshed, the dock now includes a dedicated API Workbench tile, and the UI uses a more branded appearance for the new workflow.

---

### Root cause

The previous UI used the same general styling as the rest of the shell, which made the API testing entry feel secondary and inconsistent with the rest of the app chrome. There was also no dedicated dock or status treatment for the new workbench view, so the navigation experience did not line up with the branch’s new test-design grouping work.

---

### Fix

The shell UI was restyled in several places:

1. `AppMenuBar` now renders a custom Workbench menu item with a stronger visual treatment, icon, hover state, and accelerator binding.
2. `SimpleDock` was redesigned with larger, colored tiles and now includes a dedicated `APIWorkbench` button.
3. `FXToolBar` renamed the API entry to Workbench and switched it to a neutral black icon treatment.
4. `FXStatusBar` now recognizes `API Workbench` as a distinct view and applies a matching status style.
5. `INGIcons` adds the dedicated icons used by the new workbench and Test Plan group nodes.
6. The AI Assistant toolbar/menu entry was commented out in the shell chrome, reflecting the current branch’s navigation focus.

This is primarily a UI rework, but it also ties the workbench entry to the same navigation language used by the new grouped Test Plan workflow.

---

### Files changed

| File | Status |
|---|---|
| [IDE/src/main/java/com/ing/ide/main/mainui/AppMenuBar.java](IDE/src/main/java/com/ing/ide/main/mainui/AppMenuBar.java) | Modified |
| [IDE/src/main/java/com/ing/ide/main/mainui/SimpleDock.java](IDE/src/main/java/com/ing/ide/main/mainui/SimpleDock.java) | Modified |
| [IDE/src/main/java/com/ing/ide/main/fx/FXToolBar.java](IDE/src/main/java/com/ing/ide/main/fx/FXToolBar.java) | Modified |
| [IDE/src/main/java/com/ing/ide/main/fx/FXStatusBar.java](IDE/src/main/java/com/ing/ide/main/fx/FXStatusBar.java) | Modified |
| [IDE/src/main/java/com/ing/ide/main/fx/FXMenuBar.java](IDE/src/main/java/com/ing/ide/main/fx/FXMenuBar.java) | Modified |
| [IDE/src/main/java/com/ing/ide/main/fx/INGIcons.java](IDE/src/main/java/com/ing/ide/main/fx/INGIcons.java) | Modified |
| [IDE/src/main/resources/fx/ing-theme.css](IDE/src/main/resources/fx/ing-theme.css) | Modified |

**Breaking changes**: none.

---

## 5. Scenario and Sort Order Storage

### What changed

Scenario loading now respects saved sort order when reading Test Plan scenarios, reusable scenarios, shared reusable scenarios, and test cases. The branch also adds explicit persistence of sort order whenever scenarios or test cases are created, renamed, deleted, sorted, or moved, so the tree and on-disk ordering stay aligned.

---

### Root cause

Before this work, loading order came directly from the filesystem and did not preserve the user’s last arrangement. After edits, the model had no common storage for the chosen order, so sorting in the UI was not durable and could drift after a restart.

---

### Fix

Two lightweight persistence helpers were introduced and wired into the loaders and tree actions:

1. `SortOrderStore` writes and reads a hidden `.sort_order` file for a directory.
2. `Project` and `Scenario` now apply that saved order when loading scenarios and test cases.
3. `ProjectTree` and `ReusableTree` call persistence hooks after operations that change membership or order.
4. The Test Plan branch reuses the `.groups` file to persist group order and scenario order within groups.

The practical result is that the application now reloads with the same ordering the user last set, rather than reconstructing a fresh alphabetical or filesystem-based view.

---

### Files changed

| File | Status |
|---|---|
| [Datalib/src/main/java/com/ing/datalib/component/Project.java](Datalib/src/main/java/com/ing/datalib/component/Project.java) | Modified |
| [Datalib/src/main/java/com/ing/datalib/component/Scenario.java](Datalib/src/main/java/com/ing/datalib/component/Scenario.java) | Modified |
| [Datalib/src/main/java/com/ing/datalib/component/utils/SortOrderStore.java](Datalib/src/main/java/com/ing/datalib/component/utils/SortOrderStore.java) | Added |
| [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ProjectTree.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ProjectTree.java) | Modified |
| [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ReusableTree.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ReusableTree.java) | Modified |

**Breaking changes**: none.

---

## 6. Scenario Group Tutorial

### What changed

A new user-facing tutorial was added to document how Test Scenario Groups work, how to create and rename them, how drag-and-drop behaves, and where the `.groups` file lives on disk.

---

### Root cause

The code changes introduced a new persistence model and a new grouped Test Plan workflow, so the branch also adds a dedicated guide to explain the feature and reduce confusion around the new `(Ungrouped)` behavior and storage format.

---

### Fix

The new documentation describes:

1. The meaning of groups, membership, and the implicit `(Ungrouped)` bucket.
2. How to create, rename, delete, and move scenarios between groups.
3. How ordering works and why it now persists.
4. The exact on-disk `.groups` format and where it lives.

---

### Files changed

| File | Status |
|---|---|
| [docs/Test-Scenario-Groups.md](docs/Test-Scenario-Groups.md) | Added |

**Breaking changes**: none.
