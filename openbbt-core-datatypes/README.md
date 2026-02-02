# OpenBBT Core DataTypes

This module provides the core data type implementations for OpenBBT. Data types are used to parse and validate arguments in test step expressions.

## Overview

The module implements the `DataType` and `DataTypeProvider` interfaces from `openbbt-core`, providing a set of commonly used data types for BDD testing scenarios.

## Available Data Types

### Text Types

| Name | Java Type | Pattern | Example |
|------|-----------|---------|---------|
| `word` | `String` | `[\w-]+` | `hello`, `my-word` |
| `text` | `String` | Quoted string | `'hello world'`, `"text"` |
| `id` | `String` | `\w[\w_-.]+` | `user_123`, `abc.def` |
| `url` | `URI` | URL pattern | `https://example.com/path` |
| `file` | `Path` | Quoted path | `'/path/to/file.txt'` |

### Numeric Types

| Name | Java Type | Description | Example |
|------|-----------|-------------|---------|
| `number` | `Integer` | Integer without decimals | `12345`, `12,345` |
| `big-number` | `Long` | Large integer | `9999999999` |
| `decimal` | `BigDecimal` | Decimal number | `12,345.67` |

### Temporal Types

| Name | Java Type | Format | Example |
|------|-----------|--------|---------|
| `date` | `LocalDate` | ISO date | `2024-01-15` |
| `time` | `LocalTime` | ISO time | `14:30:00` |
| `date-time` | `LocalDateTime` | ISO date-time | `2024-01-15T14:30:00` |
| `duration` | `Duration` | Hours/minutes/seconds | `2h 30m 15s 500ms` |
| `period` | `Period` | Years/months/days | `1y 6m 15d` |

## Architecture

### Class Hierarchy

```
DataType (interface from openbbt-core)
├── DataTypeAdapter<T> (abstract base class)
│   ├── NumberDataTypeAdapter<T>
│   ├── TemporalDataTypeAdapter<T>
│   ├── DurationDataTypeAdapter
│   └── PeriodDataTypeAdapter
└── RegexDataTypeAdapter<T> (simple regex-based adapter)
```

### Core Classes

#### `DataTypeAdapter<T>`

Abstract base class that provides:
- Pattern-based matching via regex
- Automatic validation before parsing
- Error handling with descriptive messages

```java
public abstract class DataTypeAdapter<T> implements DataType {
	protected DataTypeAdapter(
		String name,
		Class<T> javaType,
		String regex,
		String hint,
		Function<String,T> parser
	);
}
```

#### `RegexDataTypeAdapter<T>`

Lightweight adapter for simple regex-based types:

```java
public class RegexDataTypeAdapter<T> implements DataType {
	public RegexDataTypeAdapter(
		String name,
		String regex,
		Class<T> javaType,
		ThrowableFunction<String, T> parser,
		String hint
	);
}
```

#### `CoreDataTypes`

Plugin extension that provides all built-in data types:

```java
@Extension(scope = Scope.SINGLETON)
public class CoreDataTypes implements DataTypeProvider {
	public static final DataType WORD = ...;
	public static final DataType NUMBER = ...;
	// ... other types

	@Override
	public Stream<DataType> dataTypes() { ... }
}
```

## Usage Examples

### Using Built-in Types

```java
import static org.myjtools.openbbt.core.datatypes.CoreDataTypes.*;

// Parsing values
Integer count = (Integer) NUMBER.parse("1,234");
LocalDate date = (LocalDate) DATE.parse("2024-01-15");
Duration timeout = (Duration) DURATION.parse("5m 30s");
Period validity = (Period) PERIOD.parse("1y 6m");

// Pattern matching
boolean isValid = NUMBER.matcher("12345").matches();
```

### Creating Custom Data Types

#### Using RegexDataTypeAdapter

```java
DataType EMAIL = new RegexDataTypeAdapter<>(
	"email",
	"[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}",
	String.class,
	x -> x,
	"user@example.com"
);
```

#### Extending DataTypeAdapter

```java
public class CustomTypeAdapter extends DataTypeAdapter<MyType> {
	public CustomTypeAdapter() {
		super(
			"custom",
			MyType.class,
			"regex-pattern",
			"<hint>",
			MyType::parse
		);
	}
}
```

### Registering Custom Types

Implement `DataTypeProvider` and annotate with `@Extension`:

```java
@Extension(scope = Scope.SINGLETON)
public class MyDataTypes implements DataTypeProvider {

	public static final DataType MY_TYPE = new RegexDataTypeAdapter<>(...);

	@Override
	public Stream<DataType> dataTypes() {
		return Stream.of(MY_TYPE);
	}
}
```

## Duration Format

Durations support flexible combinations:

| Component | Suffix | Example |
|-----------|--------|---------|
| Hours | `h` | `2h` |
| Minutes | `m` | `30m` |
| Seconds | `s` | `45s` |
| Milliseconds | `ms` | `500ms` |

Examples: `2h`, `30m`, `2h 30m`, `1h 15m 30s 250ms`

## Period Format

Periods support date-based components:

| Component | Suffix | Example |
|-----------|--------|---------|
| Years | `y` | `1y` |
| Months | `m` | `6m` |
| Days | `d` | `15d` |

Examples: `1y`, `6m`, `1y 6m`, `2y 3m 15d`

## Module Info

```java
module org.myjtools.openbbt.core.datatypes {
	requires org.myjtools.openbbt.core;
	requires org.myjtools.jexten;
	exports org.myjtools.openbbt.core.datatypes;
}
```

## Dependencies

| Dependency | Purpose |
|------------|---------|
| `openbbt-core` | Core interfaces (`DataType`, `DataTypeProvider`) |
| `jexten` | Plugin framework (`@Extension`) |
