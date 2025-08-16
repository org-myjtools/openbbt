package org.myjtools.openbbt.core.test.backend;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.assertions.Assertion;
import org.myjtools.openbbt.core.assertions.AssertionFactories;
import org.myjtools.openbbt.core.backend.StepContributorBackend;
import org.myjtools.openbbt.core.datatypes.DataTypes;
import org.myjtools.openbbt.core.expressions.LiteralValue;
import org.myjtools.openbbt.core.messages.Messages;
import org.myjtools.openbbt.core.step.StepContributor;
import org.myjtools.openbbt.core.test.TestMessages;
import java.util.Locale;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class TestStepContributorBackend {


    static final DataTypes dataTypes = DataTypes.CORE;
    static final AssertionFactories assertionFactories = AssertionFactories.CORE_ENGLISH;

    static final Messages stepMessages = new TestMessages(
        Map.of(Locale.ENGLISH, Map.of(
            "stepWithoutParameters", "Step without parameters",
            "stepWithOneParameter", "Step with one parameter: {number}",
            "stepWithTwoParameters", "Step with two parameters: {number1:number}, {number2:number}",
            "stepWithOneParameterAndAssertion", "Step with one parameter and assertion: {number} {{number-assertion}}",
            "stepWithOnlyDataTable", "Step with only data table",
            "stepWithOnlyDocument", "Step with only document"
        )
    ));


    @Test
    void testStepWithOneParameter() throws Throwable {
        StepContributor stepContributor = new TestStepContributor();
        var stepContributorBackend = new StepContributorBackend(stepContributor, dataTypes, assertionFactories, stepMessages);
        stepContributorBackend.setUp();
        var step = stepContributorBackend.matchingStep("Step with one parameter: 5", Locale.ENGLISH).orElseThrow();
        var runnableStep = step.left();
        var match = step.right();
        assertThat(runnableStep.stepKey()).isEqualTo("stepWithOneParameter");
        assertThat(match.matched()).isTrue();
        assertThat(match.argument("number")).isInstanceOf(LiteralValue.class);
        assertThat(((LiteralValue) match.argument("number")).value()).isEqualTo(5);
        runnableStep.run(Map.of("number",5),null);
        stepContributorBackend.tearDown();
    }

    @Test
    void stepWithOneParameterAndAssertion() throws Throwable {
        StepContributor stepContributor = new TestStepContributor();
        var stepContributorBackend = new StepContributorBackend(stepContributor, dataTypes, assertionFactories, stepMessages);
        stepContributorBackend.setUp();
        var step = stepContributorBackend.matchingStep("Step with one parameter and assertion: 5 is greater than 2", Locale.ENGLISH).orElseThrow();
        var runnableStep = step.left();
        var match = step.right();
        assertThat(runnableStep.stepKey()).isEqualTo("stepWithOneParameterAndAssertion");
        assertThat(match.matched()).isTrue();
        assertThat(match.argument("number")).isInstanceOf(LiteralValue.class);
        assertThat(((LiteralValue) match.argument("number")).value()).isEqualTo(5);
        assertThat(match.assertion("number-assertion")).isInstanceOf(Assertion.class);
        Assertion assertion = (Assertion) match.assertion("number-assertion");
        runnableStep.run(Map.of("number",5),assertion);
        stepContributorBackend.tearDown();
    }
}
