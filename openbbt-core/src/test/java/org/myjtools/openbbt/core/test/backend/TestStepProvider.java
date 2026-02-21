package org.myjtools.openbbt.core.test.backend;

import org.myjtools.jexten.Extension;
import org.myjtools.openbbt.core.Assertion;
import org.myjtools.openbbt.core.contributors.StepProvider;
import org.myjtools.openbbt.core.plan.DataTable;
import org.myjtools.openbbt.core.plan.Document;
import org.myjtools.openbbt.core.contributors.SetUp;
import org.myjtools.openbbt.core.contributors.Step;
import org.myjtools.openbbt.core.contributors.TearDown;

@Extension
public class TestStepProvider implements StepProvider  {


	public boolean runnedStepWithoutParameters = false;
	public Integer runnedStepWithOneParameter = null;
	public Integer runnedStepWithTwoParameters1 = null;
	public Integer runnedStepWithTwoParameters2 = null;
	public Assertion runnedStepWithOnlyAssertion = null;
	public Integer runnedStepWithOneParameterAndAssertionNumber = null;
	public DataTable runnedStepWithOnlyDataTable = null;
	public Integer runnedStepWithOneParameterAndDataTableNumber = null;
	public Document runnedStepWithOnlyDocument = null;

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
	  this.runnedStepWithoutParameters = true;
   }

	@Step("stepWithOneParameter")
	public void stepWithOneParameter(Integer number) {
		this.runnedStepWithOneParameter = number;
	}

	@Step(value = "stepWithTwoParameters", args = { "number1:number", "number2:number" })
	public void stepWithTwoParameters(Integer number1, Integer number2) {
		this.runnedStepWithTwoParameters1 = number1;
		this.runnedStepWithTwoParameters2 = number2;
	}

	@Step(value = "stepWithOneParameterAndAssertion")
	public void stepWithOneParameterAndAssertion(Integer number, Assertion assertion) {
		this.runnedStepWithOneParameterAndAssertionNumber = number;
		Assertion.assertThat(number, assertion);
	}

	@Step(value = "stepWithOnlyDataTable")
	public void stepWithOnlyDataTable(DataTable dataTable) {
		this.runnedStepWithOnlyDataTable = dataTable;
	}

	@Step(value = "stepWithOneParameterAndDataTable")
	public void stepWithOneParameterAndDataTable(Integer number, DataTable dataTable) {
		this.runnedStepWithOneParameterAndDataTableNumber = number;
	}

	@Step(value = "stepWithOnlyDocument")
	public void stepWithOnlyDocument(Document document) {
		this.runnedStepWithOnlyDocument = document;
	}

	@Step(value = "stepWithOnlyAssertion")
	public void stepWithOnlyAssertion(Assertion assertion) {
		this.runnedStepWithOnlyAssertion = assertion;
	}

}
