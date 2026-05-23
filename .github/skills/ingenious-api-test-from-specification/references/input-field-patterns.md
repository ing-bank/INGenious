# Input Field Patterns

This document explains all input field syntax patterns supported by INGenious API test framework.

## Overview

The `Input` column in CSV files supports three primary patterns for specifying values:
1. **Literal values** with `@` prefix
2. **Datasheet column references** 
3. **Embedded variable substitution** in JSON/XML/text

## Pattern 1: Literal Values

Use the `@` prefix for hard-coded literal values that do not change across test iterations.

### Examples
```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
1,Webservice,Set End Point,setEndPoint,@http://localhost:3000/customers,,
2,Webservice,Add Header,addHeader,@Content-Type=application/json,,
3,Webservice,Assert Response Code,assertResponseCode,@201,,
```

**When to use:**
- Fixed endpoints that don't vary by environment
- Static header values
- Expected status codes
- Static query parameters

## Pattern 2: Datasheet Column References

Reference a column from a TestData sheet to inject dynamic values at runtime.

### Syntax
- `{SheetName:ColumnName}` — with braces (recommended for consistency)
- `SheetName:ColumnName` — without braces (also supported)

### Examples
```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
1,Webservice,Set End Point,setEndPoint,@http://localhost:3000/customers?id={API:CustomerID},,
2,Webservice,Store JSON Element In DataSheet,storeJSONelementInDataSheet,API:CustomerID,$.id,
```

**When to use:**
- Simple value injection from test data
- Storing response values back to test data
- Query parameter values
- Single field references

## Pattern 3: Embedded Variable Substitution in JSON/XML

Embed datasheet references within JSON payloads, XML documents, or text strings using `{SheetName:ColumnName}` placeholder syntax.

### Pattern 3A: Inline JSON with Embedded Variables

Build the JSON structure inline with variable placeholders that get substituted at runtime.

#### JSON Example
```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
3,Webservice,POST Rest Request,postRestRequest,"{ \"name\": \"{API:CustomerName}\", \"country\": \"{API:Country}\" }",,
```

**Corresponding TestData CSV:**
```csv
Scenario,Flow,Iteration,SubIteration,CustomerName,Country
API Testing,ConnectedFlow,1,1,Phillip Matthews,New Zealand
```

**When to use:**
- Small, simple payloads (1-5 fields)
- Payload structure is visible in test flow
- Easy to review and maintain inline
- Template-based payloads with few variables

#### XML Example
```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
3,Webservice,POST Rest Request,postRestRequest,"<customer><name>{API:CustomerName}</name><country>{API:Country}</country></customer>",,
```

### Pattern 3B: Full Payload from Datasheet Column

Store the entire JSON/XML payload in a datasheet column, enabling complex or large payloads to be maintained separately from test flow logic.

#### JSON Example
```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
3,Webservice,POST Rest Request,postRestRequest,{API:RequestBody},,
```

**Corresponding TestData CSV:**
```csv
Scenario,Flow,Iteration,SubIteration,RequestBody
API Testing,ConnectedFlow,1,1,"{ \"name\": \"Phillip Matthews\", \"country\": \"New Zealand\", \"address\": { \"street\": \"123 Main St\", \"city\": \"Auckland\" }, \"preferences\": [\"email\", \"sms\"] }"
```

**When to use:**
- Large, complex payloads (6+ fields, nested structures)
- Multiple test iterations with completely different payload shapes
- Payloads managed by non-technical users in spreadsheets
- When payload readability in CSV flow would suffer

#### XML Example
```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
3,Webservice,POST Rest Request,postRestRequest,{API:RequestBodyXML},,
```

**Corresponding TestData CSV:**
```csv
Scenario,Flow,Iteration,SubIteration,RequestBodyXML
API Testing,ConnectedFlow,1,1,"<customer><name>Phillip Matthews</name><country>New Zealand</country><address><street>123 Main St</street><city>Auckland</city></address></customer>"
```

## Choosing Between Pattern 3A and 3B

| Criteria | Pattern 3A (Inline) | Pattern 3B (Datasheet) |
|----------|---------------------|------------------------|
| Payload size | Small (1-5 fields) | Large (6+ fields) |
| Readability | Flow shows structure | Flow shows reference |
| Maintenance | Edit flow CSV | Edit TestData CSV |
| Complexity | Simple flat objects | Nested/array structures |
| Variation | Few fields vary | Entire payload varies |
| User type | Technical testers | Business analysts |

## Advanced: Combining Patterns

You can combine Pattern 1 and Pattern 2 in URL construction:

```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
1,Webservice,Set End Point,setEndPoint,@http://localhost:3000/customers?id={API:CustomerID}&status={API:Status},,
```

## Common Mistakes

❌ **Incorrect:** Missing braces in embedded JSON
```csv
3,Webservice,POST Rest Request,postRestRequest,"{ \"name\": \"API:CustomerName\" }",,
```

✅ **Correct:** Braces around datasheet reference
```csv
3,Webservice,POST Rest Request,postRestRequest,"{ \"name\": \"{API:CustomerName}\" }",,
```

---

❌ **Incorrect:** Using `@` prefix with datasheet reference
```csv
1,Webservice,Set End Point,setEndPoint,@{API:BaseURL}/customers,,
```

✅ **Correct:** No `@` prefix when using datasheet references
```csv
1,Webservice,Set End Point,setEndPoint,{API:BaseURL}/customers,,
```

---

❌ **Incorrect:** Datasheet reference without braces in Pattern 3B
```csv
3,Webservice,POST Rest Request,postRestRequest,API:RequestBody,,
```

✅ **Correct:** Use braces for consistency
```csv
3,Webservice,POST Rest Request,postRestRequest,{API:RequestBody},,
```

## Summary

- **Literal values:** Use `@` prefix for static content
- **Simple references:** Use `{Sheet:Column}` for single values
- **Inline JSON/XML:** Embed `{Sheet:Column}` within JSON/XML strings (Pattern 3A)
- **Full payload:** Reference entire payload via `{Sheet:Column}` (Pattern 3B)
- **Keep braces:** Always use `{Sheet:Column}` format for consistency
