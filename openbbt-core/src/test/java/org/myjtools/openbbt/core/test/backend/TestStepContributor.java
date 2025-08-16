package org.myjtools.openbbt.core.test.backend;

import org.myjtools.openbbt.core.assertions.Assertion;
import org.myjtools.openbbt.core.plan.DataTable;
import org.myjtools.openbbt.core.plan.Document;
import org.myjtools.openbbt.core.step.SetUp;
import org.myjtools.openbbt.core.step.Step;
import org.myjtools.openbbt.core.step.StepContributor;
import org.myjtools.openbbt.core.step.TearDown;

public class TestStepContributor implements StepContributor {


    @SetUp(order = 1)
    public void setup() {
        // This method is intentionally left empty for testing purposes.
    }


    @TearDown(order = 2)
    public void tearDown() {
        // This method is intentionally left empty for testing purposes.
    }


   @Step("stepWithoutParameters")
   public void stepWithoutParameters() {
      // This method is intentionally left empty for testing purposes.
   }

    @Step("stepWithOneParameter")
    public void stepWithOneParameter(Integer number) {
        // This method is intentionally left empty for testing purposes.
    }

    @Step(value = "stepWithTwoParameters", args = { "number1:number", "number2:number" })
    public void stepWithTwoParameters(Integer number1, Integer number2) {
        // This method is intentionally left empty for testing purposes.
    }

    @Step(value = "stepWithOneParameterAndAssertion")
    public void stepWithOneParameterAndAssertion(Integer number, Assertion assertion) {
        Assertion.assertThat(number, assertion);
    }

    @Step(value = "stepWithOnlyDataTable")
    public void stepWithOnlyDataTable(DataTable dataTable) {

    }


    @Step(value = "stepWithOnlyDocument")
    public void stepWithOnlyDocument(Document document) {

    }

}
