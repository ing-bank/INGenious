# Main TestPlan CSV (Orchestration)

File: `Projects/<ProjectName>/TestPlan/<Scenario>/<TestCase>.csv`

```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
1,Execute,,Common:Login,,,
2,Execute,,Purchase Flow:Add item to cart,,,
3,Execute,,Purchase Flow:Fill Checkout Info,,,
4,Execute,,Purchase Flow:Finish Checkout,,,
```

- `ObjectName` must be `Execute` for all orchestration rows.
- `Action` must be `<ReusableScenario>:<ReusableName>`.
- `Input`, `Condition`, `Reference` must all be blank.
