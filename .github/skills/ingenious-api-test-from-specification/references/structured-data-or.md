# Structured Data

File: `Projects/<ProjectName>/ObjectRespository/StructuredData/<page>.yaml`

## Pattern A Example (With json path query)
```yaml
page: JsonPaths
scope: PROJECT
elements:
  email:
    jsonPath: $.customer.name
```

## Pattern 3B Example (with xml path)
```yaml
page: XmlPaths
scope: PROJECT
elements:
  title:
    xpath: //title/text()
```
