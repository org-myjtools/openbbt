package org.myjtools.openbbt.core.assertions.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.assertions.AssertionAdapter;

/**
 * Tests for {@link AssertionAdapter}.
 */
class TestAssertionAdapter {

	@Test
	void testWithEqualTo_shouldPassWhenValuesMatch() {
		var assertion = new AssertionAdapter("equals-test", equalTo(42));

		assertThat(assertion.test(42)).isTrue();
		assertThat(assertion.name()).isEqualTo("equals-test");
	}

	@Test
	void testWithEqualTo_shouldFailWhenValuesDontMatch() {
		var assertion = new AssertionAdapter("equals-test", equalTo(42));

		assertThat(assertion.test(40)).isFalse();
	}

	@Test
	void describeFailure_shouldDescribeMismatch() {
		var assertion = new AssertionAdapter("equals-test", equalTo(42));

		String failure = assertion.describeFailure(40);
		assertThat(failure).contains("40");
	}

	@Test
	void testWithGreaterThan_shouldWork() {
		var assertion = new AssertionAdapter("greater-test", greaterThan(10));

		assertThat(assertion.test(15)).isTrue();
		assertThat(assertion.test(10)).isFalse();
		assertThat(assertion.test(5)).isFalse();
	}

	@Test
	void testWithLessThan_shouldWork() {
		var assertion = new AssertionAdapter("less-test", lessThan(10));

		assertThat(assertion.test(5)).isTrue();
		assertThat(assertion.test(10)).isFalse();
		assertThat(assertion.test(15)).isFalse();
	}

	@Test
	void testWithContainsString_shouldWork() {
		var assertion = new AssertionAdapter("contains-test", containsString("hello"));

		assertThat(assertion.test("hello world")).isTrue();
		assertThat(assertion.test("goodbye world")).isFalse();
	}

	@Test
	void testWithNullValue_shouldWork() {
		var assertion = new AssertionAdapter("null-test", nullValue());

		assertThat(assertion.test(null)).isTrue();
		assertThat(assertion.test("not null")).isFalse();
	}

	@Test
	void testWithNotNullValue_shouldWork() {
		var assertion = new AssertionAdapter("not-null-test", notNullValue());

		assertThat(assertion.test("something")).isTrue();
		assertThat(assertion.test(null)).isFalse();
	}

	@Test
	void describeFailure_withNullMatcher_shouldDescribeMismatch() {
		var assertion = new AssertionAdapter("null-test", nullValue());

		String failure = assertion.describeFailure("actual value");
		assertThat(failure).contains("actual value");
	}
}