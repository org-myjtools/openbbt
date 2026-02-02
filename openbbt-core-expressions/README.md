# OpenBBT Core Expressions

This module provides the expression parsing and matching engine for OpenBBT, enabling
natural language pattern matching for BDD-style test definitions.

## Overview

The `openbbt-core-expressions` module implements a flexible expression language that allows
defining step patterns with support for:

- **Literal text** - Exact text matching
- **Optional parts** - Text that may or may not be present `(optional)`
- **Choices** - Alternative words or phrases `word1|word2` or `(phrase one|phrase two)`
- **Negation** - Match anything except a specific word `^word` or phrase `^[phrase]`
- **Wildcards** - Match any text `*`
- **Arguments** - Typed parameters `{number}` or named `{count:number}`
- **Assertions** - Validation patterns `{{number-assertion}}`

## Dependencies

- `openbbt-core` - Core interfaces (`DataType`, `DataTypes`, `Assertion`, `AssertionFactory`)
- `openbbt-core-datatypes` - Data type implementations
- `openbbt-core-assertions` - Assertion factory implementations

## Architecture

```
						 Expression String
								│
								▼
					┌───────────────────────┐
					│  ExpressionTokenizer  │  Lexical analysis
					└───────────────────────┘
								│
						 List<ExpressionToken>
								│
								▼
					┌───────────────────────┐
					│  ExpressionASTBuilder │  Syntax analysis
					└───────────────────────┘
								│
						 ExpressionASTNode (tree)
								│
								▼
					┌───────────────────────┐
					│ExpressionMatcherBuilder│  Code generation
					└───────────────────────┘
								│
						 ExpressionMatcher
								│
					┌───────────┴───────────┐
					│    FragmentMatchers   │
					├───────────────────────┤
					│ PatternFragmentMatcher│  Regex patterns
					│ArgumentFragmentMatcher│  Typed arguments
					│AssertionFactoryFragment│  Assertions
					└───────────────────────┘
```

## Expression Syntax

### Literal Text
Plain text matches exactly (whitespace is normalized):
```
this is a simple expression
```

### Optional Parts
Parentheses make content optional:
```
click (the) button         # matches "click button" or "click the button"
the value is (equal to) 5  # matches "the value is 5" or "the value is equal to 5"
```

### Choices
Pipe `|` separates alternatives:
```
click|press the button     # word choice: matches "click the button" or "press the button"
(one|two|three)            # optional choice in parentheses
[first|second|third]       # required choice in brackets
```

### Negation
Caret `^` negates matching:
```
^invalid word              # matches any word except "invalid"
^[invalid phrase]          # matches any text except "invalid phrase"
```

### Wildcards
Asterisk `*` matches any text:
```
the user * clicks          # matches "the user John clicks", "the user clicks", etc.
```

### Arguments
Curly braces define typed arguments:
```
the count is {number}              # argument with type as name
wait {seconds:number} seconds      # named argument with explicit type
the date is {date}                 # date type argument
```

### Assertions
Double curly braces define assertion patterns:
```
the value {{number-assertion}}     # e.g., "the value is greater than 10"
the name {{text-assertion}}        # e.g., "the name contains 'John'"
```

### Escaping
Backslash escapes special characters:
```
the price is \$100                 # literal $
use parentheses \(like this\)      # literal parentheses
```

## Main Classes

### Tokenization Layer

| Class | Description |
|-------|-------------|
| `ExpressionTokenType` | Enum of token types (TEXT, NEGATION, START_OPTIONAL, etc.) |
| `ExpressionToken` | Token with type, value, and position information |
| `ExpressionTokenizer` | Converts expression string into token list |

### AST Layer

| Class | Description |
|-------|-------------|
| `ExpressionASTNode` | AST node with type (LITERAL, CHOICE, OPTIONAL, etc.) and children |
| `ExpressionASTBuilder` | Builds AST from token stream using state machine |

### Matching Layer

| Class | Description |
|-------|-------------|
| `ExpressionMatcher` | Main matcher that combines fragment matchers |
| `ExpressionMatcherBuilder` | Builds ExpressionMatcher from expression string |
| `FragmentMatcher` | Interface for matching expression fragments |
| `PatternFragmentMatcher` | Matches using regex patterns |
| `ArgumentFragmentMatcher` | Matches typed arguments |
| `AssertionFactoryFragmentMatcher` | Matches assertion patterns |

### Value Types

| Class | Description |
|-------|-------------|
| `Match` | Result of matching with extracted arguments and assertions |
| `ArgumentValue` | Sealed interface for argument values |
| `LiteralValue` | Literal value extracted from expression |
| `VariableValue` | Variable reference `${varName}` |

### Exceptions

| Class | Description |
|-------|-------------|
| `ExpressionException` | Thrown for expression parsing or matching errors |

## Java Module

```java
module org.myjtools.openbbt.core.expressions {
	requires org.myjtools.openbbt.core;
	requires org.myjtools.openbbt.core.assertions;
	exports org.myjtools.openbbt.core.expressions;
}
```

## Usage

### Building an Expression Matcher

```java
// Create dependencies
DataTypes dataTypes = DataTypes.of(CoreDataTypes.ALL);
AssertionFactories assertions = AssertionFactories.of(CoreAssertionFactories.ALL);

// Build the matcher
ExpressionMatcherBuilder builder = new ExpressionMatcherBuilder(dataTypes, assertions);
ExpressionMatcher matcher = builder.buildExpressionMatcher(
	"the user {name:text} has {count:number} items"
);

// Match against input
Match match = matcher.matches("the user John has 5 items", Locale.ENGLISH);

if (match.matched()) {
	ArgumentValue name = match.argument("name");   // LiteralValue("John")
	ArgumentValue count = match.argument("count"); // LiteralValue("5")
}
```

### Using Assertions

```java
ExpressionMatcher matcher = builder.buildExpressionMatcher(
	"the count {{number-assertion}}"
);

Match match = matcher.matches("the count is greater than 10", Locale.ENGLISH);

if (match.matched()) {
	Assertion assertion = match.assertion("number-assertion");
	boolean passed = assertion.test(15); // true
	boolean failed = assertion.test(5);  // false
}
```

### Working with the AST

```java
// Build AST directly
ExpressionASTBuilder astBuilder = new ExpressionASTBuilder(
	"click (the) button"
);
ExpressionASTNode tree = astBuilder.buildTree();

// Tree structure:
// SEQUENCE [
//   LITERAL<click >
//   OPTIONAL [
//     LITERAL<the>
//   ]
//   LITERAL< button>
// ]
```

### Tokenizing Expressions

```java
ExpressionTokenizer tokenizer = new ExpressionTokenizer(
	"the value is {number}"
);
List<ExpressionToken> tokens = tokenizer.tokens();

// Tokens:
// Token[TEXT 'the value is ' 0-12]
// Token[START_ARGUMENT 13-13]
// Token[TEXT 'number' 14-19]
// Token[END_ARGUMENT 20-20]
```

## Special Characters

| Character | Meaning | Escape |
|-----------|---------|--------|
| `(` `)` | Optional group | `\(` `\)` |
| `[` `]` | Required group | `\[` `\]` |
| `{` `}` | Argument | `\{` `\}` |
| `{{` `}}` | Assertion | N/A |
| `\|` | Choice separator | `\\|` |
| `^` | Negation | `\^` |
| `*` | Wildcard | `\*` |
| `\` | Escape | `\\` |

## License

This module is part of the OpenBBT project.
