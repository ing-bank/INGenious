# Reusable Component CSV (validate created customer)

File: `Projects/<ProjectName>/ReusableComponents/API Common/Validate Customer.csv`
```csv
Step,ObjectName,Description,Action,Input,Condition,Reference
1,Webservice,Set End Point,setEndPoint,@http://localhost:3000/customers?id={API:CustomerID},,
2,Webservice,Add Header,addHeader,@Content-Type=application/json,,
3,Webservice,GET Rest Request,getRestRequest,,,
4,Webservice,Assert Response Code,assertResponseCode,@200,,
5,Webservice,Assert JSON Element Equals,assertJSONelementEquals,API:CustomerName,$.[0].name,
6,Webservice,"Close the connection ",closeConnection,,,
```

- Always add a closeConnection action at the end of every API reusable components
