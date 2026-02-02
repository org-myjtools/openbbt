package org.myjtools.openbbt.core.assertions.test;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.AssertionFactory;
import org.myjtools.openbbt.core.assertions.AssertionMessageProvider;
import org.myjtools.openbbt.core.assertions.CoreAssertionFactories;
import org.myjtools.openbbt.core.messages.Messages;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CoreAssertionFactories}.
 */
class TestCoreAssertionFactories {

	@Test
	void assertionFactories_shouldReturnAllCoreFactories() {
		Messages messages = new Messages(List.of(new AssertionMessageProvider()));
		CoreAssertionFactories provider = new CoreAssertionFactories(messages);

		List<AssertionFactory<?>> factories = provider.assertionFactories().toList();

		assertThat(factories).hasSize(6);
	}

	@Test
	void assertionFactories_shouldContainNumberAssertion() {
		Messages messages = new Messages(List.of(new AssertionMessageProvider()));
		CoreAssertionFactories provider = new CoreAssertionFactories(messages);

		List<AssertionFactory<?>> factories = provider.assertionFactories().toList();

		assertThat(factories)
			.extracting(AssertionFactory::name)
			.contains("number-assertion");
	}

	@Test
	void assertionFactories_shouldContainDecimalAssertion() {
		Messages messages = new Messages(List.of(new AssertionMessageProvider()));
		CoreAssertionFactories provider = new CoreAssertionFactories(messages);

		List<AssertionFactory<?>> factories = provider.assertionFactories().toList();

		assertThat(factories)
			.extracting(AssertionFactory::name)
			.contains("decimal-assertion");
	}

	@Test
	void assertionFactories_shouldContainDateAssertion() {
		Messages messages = new Messages(List.of(new AssertionMessageProvider()));
		CoreAssertionFactories provider = new CoreAssertionFactories(messages);

		List<AssertionFactory<?>> factories = provider.assertionFactories().toList();

		assertThat(factories)
			.extracting(AssertionFactory::name)
			.contains("date-assertion");
	}

	@Test
	void assertionFactories_shouldContainTimeAssertion() {
		Messages messages = new Messages(List.of(new AssertionMessageProvider()));
		CoreAssertionFactories provider = new CoreAssertionFactories(messages);

		List<AssertionFactory<?>> factories = provider.assertionFactories().toList();

		assertThat(factories)
			.extracting(AssertionFactory::name)
			.contains("time-assertion");
	}

	@Test
	void assertionFactories_shouldContainDatetimeAssertion() {
		Messages messages = new Messages(List.of(new AssertionMessageProvider()));
		CoreAssertionFactories provider = new CoreAssertionFactories(messages);

		List<AssertionFactory<?>> factories = provider.assertionFactories().toList();

		assertThat(factories)
			.extracting(AssertionFactory::name)
			.contains("datetime-assertion");
	}

	@Test
	void assertionFactories_shouldContainTextAssertion() {
		Messages messages = new Messages(List.of(new AssertionMessageProvider()));
		CoreAssertionFactories provider = new CoreAssertionFactories(messages);

		List<AssertionFactory<?>> factories = provider.assertionFactories().toList();

		assertThat(factories)
			.extracting(AssertionFactory::name)
			.contains("text-assertion");
	}

	@Test
	void assertionFactories_allFactoriesHaveNames() {
		Messages messages = new Messages(List.of(new AssertionMessageProvider()));
		CoreAssertionFactories provider = new CoreAssertionFactories(messages);

		List<AssertionFactory<?>> factories = provider.assertionFactories().toList();

		assertThat(factories)
			.extracting(AssertionFactory::name)
			.containsExactlyInAnyOrder(
				"number-assertion",
				"decimal-assertion",
				"date-assertion",
				"time-assertion",
				"datetime-assertion",
				"text-assertion"
			);
	}
}