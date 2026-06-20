# INGenious Authoring Skill

This document teaches an AI model how to author **INGenious** test artifacts correctly.
It is injected as system context for chat and agent turns to ground the model in real
INGenious conventions and reduce hallucinated steps, actions, and locators.

## 1. Project model

An INGenious project is organised as a tree:

- **Project** — the root. Contains a *Test Plan* and *Reusable Components*.
- **Scenario** — a folder grouping related test cases. Lives under either the Test Plan
  (`Scenario.Source.TEST_PLAN`) or Reusable Components (`Scenario.Source.REUSABLE_COMPONENTS`).
- **TestCase** — an ordered list of test steps. Persisted as a CSV file inside its scenario.
- **TestStep** — a single instruction row.
- **Object Repository (OR)** — named UI elements grouped into *pages*. Steps reference
  elements by name, never by raw locator.

## 2. TestStep shape

Every step is a row with these columns (the CSV header is exactly):

```
Step,ObjectName,Description,Action,Input,Condition,Reference
```

| Column      | Meaning                                                                 |
|-------------|-------------------------------------------------------------------------|
| Step        | Auto-numbered sequence (leave generation to INGenious).                 |
| ObjectName  | `Page.ElementName` from the Object Repository, or blank if not needed.  |
| Description | Human-readable description (auto-filled from the action template).      |
| Action      | A **valid action keyword** (see section 4). Required.                   |
| Input       | The data the action consumes (`<Data>`), or blank.                      |
| Condition   | Optional run condition.                                                 |
| Reference   | Optional reference (e.g. data column or reusable name).                 |

Rules:
- An action's template uses `[<Object>]` and `[<Data>]` placeholders. If the template
  references `[<Object>]`, the step **must** set `ObjectName` to a real OR element.
  If it references `[<Data>]`, the step **must** set `Input`.
- Object names must follow `Page.Element` and the page + element must exist in the OR.
- Do not invent action keywords. Only use keywords that exist in the action catalog.

## 3. Object Repository conventions

- Elements are grouped under a **page** (e.g. a `WebORPage`). Reference an element as
  `PageName.ElementName`.
- Page and element names are matched case-insensitively but should be created in a
  consistent CamelCase or human-readable form (e.g. `LoginPage.UsernameField`).
- A new web element needs a **locator** (XPath or CSS). Prefer stable locators:
  id > name > stable CSS > text-based XPath. Avoid brittle absolute XPaths and
  auto-generated index-based paths.

## 4. Common action keywords

These are real INGenious web actions (subset of `Configuration/StepMap.csv`). Use the exact
lowercase keyword.

**Navigation & browser**
- `navigate` — open a URL (Input = URL).
- `back` — go to the previous page.
- `close` — close the current browser session.
- `changewaittime` — change implicit wait (Input = seconds).

**Interaction**
- `click` — click `[<Object>]`.
- `enter` / `type` — type `[<Data>]` into `[<Object>]`.
- `clear` — clear text from `[<Object>]`.
- `check` / `uncheck` — toggle a checkbox `[<Object>]`.
- `doubleclickelement`, `clickandholdelement`, `draganddropelement` — advanced mouse actions.
- `selectbyvisibletext` / `selectbyvalue` / `selectbyindex` — choose a dropdown option (Input = value).

**Assertions** (all assertion actions start with `assert`)
- `asserttitle` — assert page title equals `[<Data>]`.
- `asserttext` — assert `[<Object>]` has text `[<Data>]`.
- `assertvalue` — assert `[<Object>]` contains value `[<Data>]`.
- `assertelementdisplayed` — assert `[<Object>]` is displayed.
- `assertelementenabled` — assert `[<Object>]` is enabled.
- `assertcurrenturl` — assert URL equals `[<Data>]`.
- `asserttextpresentinpage` — assert text `[<Data>]` is present on the page.

**Alerts & cookies**
- `acceptalert`, `dismissalert`, `answeralert` (Input = answer).
- `addcookie`, `deletecookie` (Input = cookie name).

> The complete catalog lives in `Configuration/StepMap.csv`. When unsure whether a keyword
> exists, prefer a known keyword above or ask, rather than inventing one. The INGenious MCP
> tool layer validates action keywords server-side and will reject unknown ones.

## 5. Reusable components

- A reusable scenario/test case is invoked from another test case with the `Execute` action,
  with the `Reference` column naming the reusable.
- Prefer extracting repeated sequences (login, setup) into a reusable rather than duplicating
  steps.

## 6. Authoring guidance

- Keep each step atomic: one action per row.
- Create OR elements before the steps that reference them.
- Use assertions to make tests meaningful — a flow that only clicks proves little.
- When the user describes a flow in plain English, map it to the smallest correct sequence of
  valid actions, creating only the OR elements you actually reference.
- Never fabricate an `ObjectName` that has not been created in the Object Repository.
