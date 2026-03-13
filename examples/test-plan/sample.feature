
Feature: Mi Feature

  Background:
    * I make a DELETE request to 'pepe'
    And I make a POST request to 'pepe' with body:
      """json
      { 
        "nombre": "Luis",
        "apellidos": "Iñesta Gelabert"
      }
      """

  Scenario: My Scenario
    Given the response body is:
      """json
      { 
        "nombre": "Luis",
        "apellidos": "Iñesta Gelabert"
      }
      """

    And the response body contains:
      | tabla | x |
      | a     | 3 |
      | b     | 4 |
