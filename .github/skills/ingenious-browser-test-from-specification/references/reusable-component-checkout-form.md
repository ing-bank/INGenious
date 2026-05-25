# Reusable Component CSV - Checkout Form Fill

File: `Projects/<ProjectName>/ReusableComponents/Purchase Flow/Fill Checkout Info.csv`

```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
1,Checkout [button],Click the [<Object>],Click,,,[Project] Your Cart
2,First Name,Enter the value [<Data>] in the Field [<Object>],Fill,Purchase Details:FirstName,,[Project] Checkout - Your Information
3,Last Name,Enter the value [<Data>] in the Field [<Object>],Fill,Purchase Details:LastName,,[Project] Checkout - Your Information
4,Postal Code,Enter the value [<Data>] in the Field [<Object>],Fill,Purchase Details:PostCode,,[Project] Checkout - Your Information
5,Continue [button],Click the [<Object>],Click,,,[Project] Checkout - Your Information
```

- `Reference` column uses `[Project] <PageName>` to point at the ObjectRepository page