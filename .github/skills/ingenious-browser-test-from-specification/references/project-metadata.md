# .project Metadata File

File: `Projects/<ProjectName>/.project`

```json
{
  "id": "<ProjectName>",
  "name": "<ProjectName>",
  "version": "2.4",
  "attributes": [],
  "tags": [],
  "_meta": [
    { "type": "scenario", "name": "Common", "ref": "com.ing.datalib.model.Attribute", "attributes": [], "tags": [] },
    { "type": "scenario", "name": "<YourScenario>", "ref": "com.ing.datalib.model.Attribute", "attributes": [], "tags": [] }
  ],
  "data": [
    {
      "id": "<Scenario>#<TestCaseName>",
      "name": "<TestCaseName>",
      "tags": [],
      "attributes": [
        { "name": "type", "value": "testcase" },
        { "name": "scenario", "value": "<Scenario>" }
      ]
    },
    {
      "id": "<FlowGroup>#<ReusableName>",
      "name": "<ReusableName>",
      "tags": [],
      "attributes": [
        { "name": "type", "value": "reusable" },
        { "name": "scenario", "value": "<FlowGroup>" }
      ]
    }
  ]
}
```

- Every new scenario, reusable, and testcase must be registered in `_meta` and `data` respectively.
- Use the same id format `<Scenario>#<Name>` for all entries.
