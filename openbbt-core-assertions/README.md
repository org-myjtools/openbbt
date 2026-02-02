# OpenBBT Core Assertions

This module provides the assertion framework for OpenBBT, offering integration
with Hamcrest Matchers and support for different data types.

## Overview

The `openbbt-core-assertions` module implements a flexible assertion system that validates
values at runtime using localizable expression patterns. Assertions are based on Hamcrest
Matchers, providing descriptive error messages and an extensible API.

## Dependencies

- `openbbt-core` - Base interfaces (`Assertion`, `AssertionFactory`, `AssertionFactoryProvider`)
- `openbbt-core-datatypes` - Supported data types (`CoreDataTypes`)
- `org.hamcrest:hamcrest` - Matcher framework for validations

## Architecture

```
					+-------------------------+
					| AssertionFactoryProvider|  (from openbbt-core)
					+-------------------------+
							   ^
							   |
					+-------------------------+
					| CoreAssertionFactories  |  Main provider
					+-------------------------+
							   |
		  +--------------------+--------------------+
		  |                    |                    |
+---------v--------+  +--------v--------+  +-------v---------+
|ComparableAssertion|  |StringAssertion  |  |TemporalAssertion|
|     Factory      |  |   Factory       |  |    Factory      |
+------------------+  +-----------------+  +-----------------+
		  |                    |                    |
		  +--------------------+--------------------+
							   |
					+----------v----------+
					|AssertionFactoryAdapter|  Abstract base class
					+----------------------+
							   |
					+----------v----------+
					|  AssertionAdapter   |  Adapts Hamcrest Matcher
					+---------------------+
```

## Main Classes

### AssertionAdapter

Adapter that wraps an `org.hamcrest.Matcher` to implement the OpenBBT `Assertion` interface.

```java
Assertion assertion = new AssertionAdapter("my-assertion", Matchers.equalTo(42));
boolean result = assertion.test(42); // true
String failure = assertion.describeFailure(40); // failure description
```

### AssertionFactoryAdapter

Abstract base class for implementing assertion factories. Provides:

- Localized pattern management by locale
- Parameter parsing from text expressions
- Assertion creation from patterns

Subclasses must implement `fillSuppliers()` to register supported assertions.

### ComparableAssertionFactory

Assertion factory for `Comparable` types (numbers, decimals). Supports:

| Message Key | Description |
|-------------|-------------|
| `assertion.number.equals` | Equal to |
| `assertion.number.greater` | Greater than |
| `assertion.number.less` | Less than |
| `assertion.number.greater.equals` | Greater than or equal to |
| `assertion.number.less.equals` | Less than or equal to |
| `assertion.number.not.equals` | Not equal to |
| `assertion.number.not.greater` | Not greater than |
| `assertion.number.not.less` | Not less than |
| `assertion.generic.null` | Is null |
| `assertion.generic.not.null` | Is not null |

### StringAssertionFactory

Assertion factory for text strings. Supports:

| Message Key | Description |
|-------------|-------------|
| `assertion.string.equals` | Equal to |
| `assertion.string.not.equals` | Not equal to |
| `assertion.string.equals.ignore.case` | Equal ignoring case |
| `assertion.string.equals.ignore.whitespace` | Equal ignoring whitespace |
| `assertion.string.starts.with` | Starts with |
| `assertion.string.ends.with` | Ends with |
| `assertion.string.contains` | Contains |
| `assertion.string.not.starts.with` | Does not start with |
| `assertion.string.not.ends.with` | Does not end with |
| `assertion.string.not.contains` | Does not contain |
| (+ ignore.case variants) | |

### TemporalAssertionFactory

Assertion factory for temporal types (`LocalDate`, `LocalTime`, `LocalDateTime`). Supports:

| Message Key | Description |
|-------------|-------------|
| `assertion.temporal.equals` | Equal to |
| `assertion.temporal.after` | After |
| `assertion.temporal.before` | Before |
| `assertion.temporal.after.equals` | After or equal to |
| `assertion.temporal.before.equals` | Before or equal to |
| `assertion.temporal.not.equals` | Not equal to |
| `assertion.temporal.not.after` | Not after |
| `assertion.temporal.not.before` | Not before |

### CoreAssertionFactories

Main provider that registers the default assertion factories:

- `number-assertion` - Assertions for integers
- `decimal-assertion` - Assertions for decimals (`BigDecimal`)
- `date-assertion` - Assertions for dates (`LocalDate`)
- `time-assertion` - Assertions for times (`LocalTime`)
- `datetime-assertion` - Assertions for date-time (`LocalDateTime`)
- `text-assertion` - Assertions for text

### AssertionFactories

Registry that groups multiple `AssertionFactory` instances and allows lookup by name.

```java
AssertionFactories factories = AssertionFactories.CORE_ENGLISH;
AssertionFactory<?> numberFactory = factories.byName("number-assertion");
```

## Java Module

```java
module org.myjtools.openbbt.core.assertions {
	requires org.hamcrest;
	requires org.myjtools.openbbt.core;
	requires org.myjtools.openbbt.core.datatypes;
	requires org.myjtools.jexten;
	exports org.myjtools.openbbt.core.assertions;
}
```

## Usage

### Using predefined assertions

```java
// Get the factory registry
AssertionFactories factories = AssertionFactories.CORE_ENGLISH;

// Get a specific factory
AssertionFactory<?> factory = factories.byName("number-assertion");

// Get patterns for a locale
List<AssertionPattern<?>> patterns = factory.patterns(Locale.ENGLISH);

// Create an assertion from a pattern
Assertion assertion = factory.assertion(pattern, "is greater than 10");

// Evaluate the assertion
boolean passed = assertion.test(15); // true
```

### Creating a custom factory

```java
public class CustomAssertionFactory extends AssertionFactoryAdapter<MyType> {

	public CustomAssertionFactory(Messages messages) {
		super("custom-assertion", MyType::parse, myDataType, messages);
	}

	@Override
	protected void fillSuppliers() {
		suppliers.put("assertion.custom.valid",
			it -> new AssertionAdapter(name, myCustomMatcher(it)));
	}
}
```

## Extension

To add new assertion factories, implement `AssertionFactoryProvider`:

```java
@Extension(scope = Scope.SINGLETON)
public class MyAssertionFactories implements AssertionFactoryProvider {

	@Inject("assertions")
	Messages messages;

	@Override
	public Stream<AssertionFactory<?>> assertionFactories() {
		return Stream.of(
			new MyCustomAssertionFactory(messages)
		);
	}
}
```

## License

This module is part of the OpenBBT project.