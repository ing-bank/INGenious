# Page Object YAML

File: `Projects/<ProjectName>/ObjectRepository/Web/<Page>.yaml`

```yaml
page: Login
elements:
  Username:
    css: "[data-test=\"username\"]"
  Password:
    css: "[data-test=\"password\"]"
  Login [Button]:
    css: "[data-test=\"login-button\"]"
```

Role-based example (preferred for interactive elements):

```yaml
page: Mortgage - Plans
elements:
  Within 3 Months [Radio]:
    role: Radio;Within 3 months
    exact:
      - role
  Energy Label [Dropdown]:
    css: "select[aria-label*='energy label']"
  Next [Button]:
    role: Button;Next
  Guest [Tab]:
    role: Tab;Guest
```

> **Rule:** Role type and element name are always written as `Type;Name` in a single `role:` value.
> Never split into `role: Tab` + `text: Guest` — `text:` is not a valid INGenious YAML key.
