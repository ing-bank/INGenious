# Reusable Component CSV - Login Flow

File: `Projects/<ProjectName>/ReusableComponents/Common/Login.csv`

```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
1,Browser,Open the Url [<Data>] in the Browser,Open,@https://www.saucedemo.com/,,
2,Username,Enter the value [<Data>] in the Field [<Object>],Fill,Login:Username,,[Project] Login
3,Password,Enter the value [<Data>] in the Field [<Object>],Fill,Login:Password,,[Project] Login
4,Login [Button],Click the [<Object>],Click,,,[Project] Login
```

- `Reference` column uses `[Project] <PageName>` to point at the ObjectRepository page
- `Input` for data-driven steps uses `<Sheet>:<Column>` syntax
- `Open` with `@<url>` hardcodes the URL inline; use a data reference (`<Sheet>:URL`) for parameterised URLs
