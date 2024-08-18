Feature: Payment Operations

User should be able to perform credit and debit transactions.


@Pay
Scenario: Pay money to an existing contact
Given User is logged in 
When User creates a new transaction
And chooses to pay
Then user should be able to complete the operation

@Request
Scenario: Request money from existing contact
Given User is logged in 
When User creates a new transaction
And chooses to request
Then user should be able to complete the operation