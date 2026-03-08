# language: es
Característica: Mi Feature

  Escenario: My Scenario
    Dado que el cuerpo de la respuesta es:
      """json
      { "nombre": "hola" }
      """

    Y el cuerpo de la respuesta es:
      | tabla | x |
      | a     | 3 |
