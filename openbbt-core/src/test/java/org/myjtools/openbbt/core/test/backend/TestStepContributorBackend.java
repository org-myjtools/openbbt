package org.myjtools.openbbt.core.test.backend;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.AssertionFactories;
import org.myjtools.openbbt.core.DataTypes;
import org.myjtools.openbbt.core.assertions.CoreAssertionFactories;
import org.myjtools.openbbt.core.backend.StepProviderService;
import org.myjtools.openbbt.core.datatypes.CoreDataTypes;
import org.myjtools.openbbt.core.messages.Messages;
import org.myjtools.openbbt.core.test.TestMessages;
import java.util.Locale;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class TestStepContributorBackend {


    static final DataTypes dataTypes = DataTypes.of(CoreDataTypes.ALL);
    static final AssertionFactories assertionFactories = AssertionFactories.of(CoreAssertionFactories.ALL);

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
    void testStepNoMatch() throws Throwable {
        TestStepProvider stepProvider = new TestStepProvider();
        StepProviderService service = new StepProviderService(
            stepProvider,
            dataTypes,
            assertionFactories,
            stepMessages
        );
        var matchingStep = service.matchingStep("Step with one parameter: 5x", Locale.ENGLISH);
        assertThat(matchingStep).isEmpty();
    }


    @Test
    void testStepWithOneParameterLiteralValue() throws Throwable {
        TestStepProvider stepProvider = new TestStepProvider();
	    StepProviderService service = new StepProviderService(
			stepProvider,
		    dataTypes,
		    assertionFactories,
		    stepMessages
	    );
		var matchingStep = service.matchingStep("Step with one parameter: 5", Locale.ENGLISH)
            .orElseThrow();
		var stepMethod = matchingStep.left();
        var match = matchingStep.right();

        assertThat(stepMethod.stepKey()).isEqualTo("stepWithOneParameter");

        Map<String,Object> arguments = match.interpolateArguments(Map.of());
        assertThat(stepProvider.runnedStepWithOneParameter).isNull();
        stepMethod.run(arguments,null);
        assertThat(stepProvider.runnedStepWithOneParameter).isEqualTo(5);
      }



    @Test
    void testStepWithOneParameter() throws Throwable {
        TestStepProvider stepProvider = new TestStepProvider();
        StepProviderService service = new StepProviderService(
            stepProvider,
            dataTypes,
            assertionFactories,
            stepMessages
        );
        var matchingStep = service.matchingStep("Step with one parameter: 5", Locale.ENGLISH)
                .orElseThrow();
        var stepMethod = matchingStep.left();
        var match = matchingStep.right();

        assertThat(stepMethod.stepKey()).isEqualTo("stepWithOneParameter");

        assertThat(stepProvider.runnedStepWithOneParameter).isNull();
        stepMethod.run(match.interpolateArguments(Map.of()),null);
        assertThat(stepProvider.runnedStepWithOneParameter).isEqualTo(5);
    }



//    @Test
//    void stepWithOneParameterAndAssertion() throws Throwable {
//        StepContributor stepContributor = new TestStepContributor();
//        var stepContributorBackend = new StepContributorBackend(stepContributor, dataTypes, assertionFactories, stepMessages);
//        stepContributorBackend.setUp();
//        var step = stepContributorBackend.matchingStep("Step with one parameter and assertion: 5 is greater than 2", Locale.ENGLISH).orElseThrow();
//        var runnableStep = step.left();
//        var match = step.right();
//        assertThat(runnableStep.stepKey()).isEqualTo("stepWithOneParameterAndAssertion");
//        assertThat(match.matched()).isTrue();
//        assertThat(match.argument("number")).isInstanceOf(LiteralValue.class);
//        assertThat(((LiteralValue) match.argument("number")).value()).isEqualTo(5);
//        assertThat(match.assertion("number-assertion")).isInstanceOf(Assertion.class);
//        Assertion assertion = (Assertion) match.assertion("number-assertion");
//        runnableStep.run(Map.of("number",5),assertion);
//        stepContributorBackend.tearDown();
//    }
}
