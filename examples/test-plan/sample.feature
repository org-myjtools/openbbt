# language: en
Feature: Mi Feature

  Scenario: My Scenario
    Given the response body is:    
      """json
      { "nombre": "hola" }
      """

    And the response body contains:
      | tabla | x |
      | a     | 3 |
