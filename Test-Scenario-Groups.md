# Test Scenario Groups — Tutorial

Scenario Groups let you organize the scenarios in your **Test Plan** into named,
persistent buckets — for example *Payment Initiation* and *Payment Processing* —
so a large project stays readable and related scenarios can be managed together.

Groups are:

- **Visual** — scenarios are nested under a group folder in the Test Plan tree,
  shown with a distinct stacked-folder icon (different from the single-folder
  scenario icon).
- **Persistent** — grouping is saved to disk and is restored automatically every
  time you reopen the project or restart the IDE.
- **Optional & backward-compatible** — projects with no groups keep showing the
  classic flat scenario list. Groups only appear once you create one.

---

## 1. Concepts

| Term | Meaning |
| --- | --- |
| **Group** | A named container for one or more scenarios (e.g. *Payment Initiation*). |
| **(Ungrouped)** | An implicit group that automatically holds every scenario not assigned to a named group. It is always shown last and cannot be renamed or deleted. |
| **Membership** | Which scenarios belong to which group. |

When a project has at least one group, the Test Plan tree gains an extra level:

```
MyProject                     (project root)
├─ Payment Initiation         (group)
│  ├─ Login                   (scenario)
│  │  └─ ValidCredentials     (test case)
│  └─ CreatePayment
├─ Payment Processing         (group)
│  └─ ProcessPayment
└─ (Ungrouped)                (implicit catch-all group)
   └─ Miscellaneous
```

---

## 2. Creating a group

1. In the **Test Plan** tree, right-click the **project root** (top node).
2. Choose **New Group**.
3. Enter a name (e.g. `Payment Initiation`) and confirm.

The first time you create a group, every existing scenario is automatically moved
under **(Ungrouped)**, and your new group appears above it. From there you can
move scenarios into the group.

> Group names must be unique and cannot be `(Ungrouped)`.

---

## 3. Moving scenarios into a group

You can move scenarios in two ways:

**A. Context menu**
1. Right-click a scenario (or select several scenarios first).
2. Open the **Move to Group ▶** submenu.
3. Pick the destination group.

**B. Drag and drop**
1. Drag the scenario node and drop it onto the target group folder.

Scenarios that you move out of every named group fall back into **(Ungrouped)**.

---

## 4. Renaming a group

1. Right-click the group → **Rename Group** (or select it and press the rename
   shortcut).
2. Type the new name and press **Enter**.

Renaming updates the group on disk immediately. Scenario memberships are kept.

---

## 5. Deleting a group

1. Right-click the group → **Delete Group**.
2. Confirm.

Deleting a group **does not delete its scenarios** — they are moved to
**(Ungrouped)**. Only the grouping is removed.

---

## 6. Adding new scenarios

When grouping is active, any scenario you create lands in **(Ungrouped)** by
default. Move it into the group you want using the steps in section 3.

---

## 7. Ordering

Ordering is fully persistent and survives restarts:

- **Group order** — sort or rearrange the groups under the project root.
- **Scenario order within a group** — sort or rearrange scenarios inside a group.
- **Test case order within a scenario** — unchanged from before.

Use the **Sort** context-menu action on any node to alphabetically order its
children; the chosen order is then remembered.

---

## 8. Where the grouping is stored

Grouping is saved in a hidden file inside the project's Test Plan folder:

```
<project>/TestPlan/.groups
```

Example contents:

```json
{
  "groups": [
    { "name": "Payment Initiation", "scenarios": ["Login", "CreatePayment"] },
    { "name": "Payment Processing", "scenarios": ["ProcessPayment"] }
  ]
}
```

Notes:

- Only **named** groups are stored. The **(Ungrouped)** bucket is recomputed each
  time the project loads as *every scenario that is not listed in a named group*.
  This means the file never goes stale if scenarios are added or removed outside
  the IDE.
- The file is plain JSON, so it is safe to commit to version control and review
  in pull requests.

---

## 9. FAQ / Troubleshooting

**My groups disappeared after editing files outside the IDE.**
Make sure the `<project>/TestPlan/.groups` file is intact. If a scenario named in
the file no longer exists on disk, it is simply skipped.

**A scenario shows up under (Ungrouped) that I expected in a group.**
The scenario name in `.groups` must exactly match the scenario folder name. Rename
inside the IDE so both stay in sync automatically.

**I don't see any group nodes.**
Groups only appear after you create at least one. Projects with no `.groups` file
show the classic flat scenario list.

**Reusable / Shared Reusable trees.**
These trees have their own separate grouping mechanism and are not affected by
Test Plan scenario groups.
