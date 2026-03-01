# language: en
Feature:
  Example feature file for testing the REST OpenBBT plugin.

Scenario: Perform a sample API call
  When I make a GET request to "/sample-endpoint"
  Then I should receive a 200 OK response
  And the response body should contain "success"