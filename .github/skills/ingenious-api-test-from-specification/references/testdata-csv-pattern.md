# TestData CSV

File: `Projects/<ProjectName>/TestData/API.csv`

## Pattern 3A Example (Inline JSON with embedded variables)
```csv
Scenario,Flow,Iteration,SubIteration,CustomerName,Country,CustomerID,AccountID,TransactionID
API Testing,ConnectedFlow,1,1,Phillip Matthews,New Zealand,1de9,c408,567a
```

## Pattern 3B Example (Full payload from datasheet column)
```csv
Scenario,Flow,Iteration,SubIteration,RequestBody,CustomerID,AccountID
API Testing,ConnectedFlow,1,1,"{ \"name\": \"Phillip Matthews\", \"country\": \"New Zealand\", \"address\": { \"street\": \"123 Main St\", \"city\": \"Auckland\" } }",1de9,c408
```

- Use Pattern 3A columns (CustomerName, Country) for small payloads with few fields
- Use Pattern 3B column (RequestBody) for large, complex payloads
- See [input-field-patterns.md](./input-field-patterns.md) for detailed guidance on choosing between patterns
