Feature: Contact Us

 User should be able to send a request towards ING


Scenario: User should be able to send a question on Sustainability
    
    Given User navigates to the Contact-Us page
    When  User fills up personal details
    And submits relevant question
    Then the question should be sent


Scenario: User should be able to send a question on Innovation
    
    Given User navigates to the Contact-Us page
    When  User fills up personal details
    And submits relevant question
    Then the question should be sent    