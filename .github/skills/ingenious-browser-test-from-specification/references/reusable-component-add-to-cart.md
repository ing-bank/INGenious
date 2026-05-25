# Reusable Component CSV - Add Item to Cart

File: `Projects/<ProjectName>/ReusableComponents/Purchase Flow/Add Item to Cart.csv`

```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
1,Add to Cart [button],Click the [<Object>],Click,,,[Project] Product
2,Shopping Cart [icon],Click the [<Object>],Click,,,[Project] Product
3,Item Name,Assert if [<Object>] has text [<Data>],assertElementTextMatches,Purchase Details:Product,,[Project] Your Cart
```

- `Reference` column uses `[Project] <PageName>` to point at the ObjectRepository page