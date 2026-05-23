# Main TestPlan CSV (orchestration)

File: `Projects/<ProjectName>/TestPlan/<Scenario>/<TestCase>.csv`
```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
1,Execute,,API Common:Create Customer,,,
2,Execute,,API Common:Create Account,,,
3,Execute,,API Common:Create Transaction,,,
4,Execute,,API Common:Validate Transaction,,,
```
- `ObjectName` must be `Execute` for all orchestration rows.
- `Action` must be `<ReusableScenario>:<ReusableName>`.
- `Input`, `Condition`, `Reference` must be blank.
