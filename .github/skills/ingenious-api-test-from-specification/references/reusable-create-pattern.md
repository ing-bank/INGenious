# Reusable Component CSV (create customer)

File: `Projects/<ProjectName>/ReusableComponents/API Common/Create Customer.csv`
```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
1,Webservice,Set End Point,setEndPoint,@http://localhost:3000/customers,,
2,Webservice,Add Header,addHeader,@Content-Type=application/json,,
3,Webservice,POST Rest Request,postRestRequest,"{ \"name\": \"{API:CustomerName}\", \"country\": \"{API:Country}\" }",,
4,Webservice,Assert Response Code,assertResponseCode,@201,,
5,Webservice,Store JSON Element In DataSheet,storeJSONelementInDataSheet,API:CustomerID,$.id,
6,Webservice,"Close the connection ",closeConnection,,,
```
- This example uses **Pattern 3A (inline JSON with embedded variables)** from [input-field-patterns.md](./input-field-patterns.md).
- For large or complex payloads, use **Pattern 3B** with `{API:RequestBody}` instead.
- API step rows use `ObjectName=Webservice`.
- IDs can be persisted using `storeJSONelementInDataSheet`.
- Always add a closeConnection action at the end of every API reusable components