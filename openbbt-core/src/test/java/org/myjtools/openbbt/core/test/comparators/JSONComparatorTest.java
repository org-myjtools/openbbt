package org.myjtools.openbbt.core.test.comparators;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.assertions.AssertionAdapter;
import org.myjtools.openbbt.core.comparators.JSONComparator;
import org.myjtools.openbbt.core.contributors.ContentComparator.ComparisonMode;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class JSONComparatorTest {

	private final JSONComparator comparator = new JSONComparator();

	// --- accepts ---

	@Test
	void accepts_json_shorthand() {
		assertThat(comparator.accepts("json")).isTrue();
	}

	@Test
	void accepts_applicationJson() {
		assertThat(comparator.accepts("application/json")).isTrue();
	}

	@Test
	void accepts_customPlusJson() {
		assertThat(comparator.accepts("application/vnd.api+json")).isTrue();
	}

	@Test
	void accepts_xml_returnsFalse() {
		assertThat(comparator.accepts("application/xml")).isFalse();
	}


	// --- assertContentEquals STRICT ---

	@Test
	void contentEquals_strict_identical_passes() {
		String json = """
			{"name":"Alice","age":30}
			""";
		assertThatCode(() -> comparator.assertContentEquals(json, json, ComparisonMode.STRICT))
			.doesNotThrowAnyException();
	}

	@Test
	void contentEquals_strict_differentValue_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"""
			{"name":"Alice"}
			""",
			"""
			{"name":"Bob"}
			""",
			ComparisonMode.STRICT
		)).isInstanceOf(AssertionError.class).hasMessageContaining("STRICT");
	}

	@Test
	void contentEquals_strict_differentKeyOrder_passes() {
		// JSON objects are unordered by spec; STRICT compares values, not key order
		assertThatCode(() -> comparator.assertContentEquals(
			"""
			{"a":1,"b":2}
			""",
			"""
			{"b":2,"a":1}
			""",
			ComparisonMode.STRICT
		)).doesNotThrowAnyException();
	}


	// --- assertContentEquals ANY_ORDER ---

	@Test
	void contentEquals_anyOrder_differentArrayOrder_passes() {
		assertThatCode(() -> comparator.assertContentEquals(
			"""
			{"items":[1,2,3]}
			""",
			"""
			{"items":[3,1,2]}
			""",
			ComparisonMode.ANY_ORDER
		)).doesNotThrowAnyException();
	}

	@Test
	void contentEquals_anyOrder_differentValues_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"""
			{"items":[1,2,3]}
			""",
			"""
			{"items":[1,2,4]}
			""",
			ComparisonMode.ANY_ORDER
		)).isInstanceOf(AssertionError.class);
	}


	// --- assertContentEquals LOOSE ---

	@Test
	void contentEquals_loose_subsetFields_passes() {
		assertThatCode(() -> comparator.assertContentEquals(
			"""
			{"name":"Alice"}
			""",
			"""
			{"name":"Alice","age":30,"city":"Madrid"}
			""",
			ComparisonMode.LOOSE
		)).doesNotThrowAnyException();
	}

	@Test
	void contentEquals_loose_missingExpectedField_fails() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"""
			{"name":"Alice","role":"admin"}
			""",
			"""
			{"name":"Alice"}
			""",
			ComparisonMode.LOOSE
		)).isInstanceOf(AssertionError.class);
	}


	// --- assertFragmentEquals ---

	@Test
	void fragmentEquals_existingPath_passes() {
		assertThatCode(() -> comparator.assertFragmentEquals(
			"""
			{"person":{"name":"Alice","age":30}}
			""",
			"$.person.name",
			new AssertionAdapter(Matchers.equalTo("Alice"))
		)).doesNotThrowAnyException();
	}

	@Test
	void fragmentEquals_arrayElement_passes() {
		assertThatCode(() -> comparator.assertFragmentEquals(
			"""
			{"items":[10,20,30]}
			""",
			"$.items[1]",
			new AssertionAdapter(Matchers.equalTo(20))
		)).doesNotThrowAnyException();
	}

	@Test
	void fragmentEquals_wrongValue_fails() {
		assertThatThrownBy(() -> comparator.assertFragmentEquals(
			"""
			{"name":"Alice"}
			""",
			"$.name",
			new AssertionAdapter(Matchers.equalTo("Bob"))
		)).isInstanceOf(AssertionError.class);
	}

	@Test
	void fragmentEquals_missingPath_fails() {
		assertThatThrownBy(() -> comparator.assertFragmentEquals(
			"""
			{"name":"Alice"}
			""",
			"$.missing",
			new AssertionAdapter(Matchers.anything())
		)).isInstanceOf(AssertionError.class).hasMessageContaining("$.missing");
	}


	// --- assertComplyWithSchema ---

	@Test
	void complyWithSchema_validContent_passes() {
		String schema = """
			{
			  "$schema": "https://json-schema.org/draft/2020-12/schema",
			  "type": "object",
			  "properties": {
			    "name": {"type": "string"},
			    "age":  {"type": "integer"}
			  },
			  "required": ["name"]
			}
			""";
		assertThatCode(() -> comparator.assertComplyWithSchema(
			"""
			{"name":"Alice","age":30}
			""",
			schema
		)).doesNotThrowAnyException();
	}

	@Test
	void complyWithSchema_missingRequiredField_fails() {
		String schema = """
			{
			  "$schema": "https://json-schema.org/draft/2020-12/schema",
			  "type": "object",
			  "required": ["name"]
			}
			""";
		assertThatThrownBy(() -> comparator.assertComplyWithSchema(
			"""
			{"age":30}
			""",
			schema
		)).isInstanceOf(AssertionError.class).hasMessageContaining("Schema validation failed");
	}

	@Test
	void complyWithSchema_wrongType_fails() {
		String schema = """
			{
			  "$schema": "https://json-schema.org/draft/2020-12/schema",
			  "type": "object",
			  "properties": {
			    "age": {"type": "integer"}
			  }
			}
			""";
		assertThatThrownBy(() -> comparator.assertComplyWithSchema(
			"""
			{"age":"not-a-number"}
			""",
			schema
		)).isInstanceOf(AssertionError.class);
	}

	@Test
	void contentEquals_invalidJson_throws() {
		assertThatThrownBy(() -> comparator.assertContentEquals(
			"not-json", "{}", ComparisonMode.STRICT
		)).isInstanceOf(AssertionError.class).hasMessageContaining("Invalid JSON");
	}
}
