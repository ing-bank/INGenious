# .project Metadata File Pattern

File: `Projects/<ProjectName>/.project`
```json
{
  "id": "<ProjectName>",
  "name": "<ProjectName>",
  "version": "2.4",
  "attributes": [],
  "tags": [],
  "_meta": [
    { "type": "scenario", "name": "API Testing", "ref": "com.ing.datalib.model.Attribute", "attributes": [], "tags": [] }
  ],
  "data": [
    {
      "id": "API Testing#<TestCaseName>",
      "name": "<TestCaseName>",
      "tags": [],
      "attributes": [
        { "name": "type", "value": "testcase" },
        { "name": "scenario", "value": "API Testing" }
      ]
    },
    {
      "id": "API Common#Create Customer",
      "name": "Create Customer",
      "tags": [],
      "attributes": [
        { "name": "type", "value": "reusable" },
        { "name": "scenario", "value": "API Common" }
      ]
    }
  ]
}
```
- Every new scenario, reusable, and testcase may need registration in `_meta` and `data` depending on project behavior.
- Use id format `<Scenario>#<Name>`.
