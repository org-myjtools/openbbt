package org.myjtools.openbbt.core.assertions.test;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.AssertionFactories;
import org.myjtools.openbbt.core.AssertionFactory;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.assertions.CoreAssertionFactories;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link AssertionFactories}.
 */
class TestAssertionFactories {

    static AssertionFactories factories = AssertionFactories.of(CoreAssertionFactories.ALL);

    @Test
    void coreEnglish_shouldContainAllDefaultFactories() {
        assertThat(factories.byName("number-assertion")).isNotNull();
        assertThat(factories.byName("decimal-assertion")).isNotNull();
        assertThat(factories.byName("date-assertion")).isNotNull();
        assertThat(factories.byName("time-assertion")).isNotNull();
        assertThat(factories.byName("datetime-assertion")).isNotNull();
        assertThat(factories.byName("text-assertion")).isNotNull();
    }

    @Test
    void byName_withValidName_shouldReturnFactory() {
        AssertionFactory<?> factory = factories.byName("number-assertion");

        assertThat(factory).isNotNull();
        assertThat(factory.name()).isEqualTo("number-assertion");
    }

    @Test
    void byName_withInvalidName_shouldThrowException() {
        assertThatThrownBy(() -> factories.byName("non-existent-assertion"))
            .isInstanceOf(OpenBBTException.class)
            .hasMessageContaining("Unknown assertion")
            .hasMessageContaining("non-existent-assertion");
    }

    @Test
    void of_withVarargs_shouldCreateRegistry() {
        AssertionFactory<?> numberFactory = factories.byName("number-assertion");
        AssertionFactory<?> textFactory = factories.byName("text-assertion");

        AssertionFactories custom = AssertionFactories.of(numberFactory, textFactory);

        assertThat(custom.byName("number-assertion")).isNotNull();
        assertThat(custom.byName("text-assertion")).isNotNull();
        assertThatThrownBy(() -> custom.byName("date-assertion"))
            .isInstanceOf(OpenBBTException.class);
    }
}