package org.myjtools.openbbt.core.contributors;

import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.openbbt.core.backend.StepProviderBackend;
import org.myjtools.openbbt.core.testplan.TestPlan;

@ExtensionPoint
public interface TestPlanValidator {

	/**
	 * Validates the given test plan and updates its validation status and messages.
	 * This method should check each node in the test plan for correctness
	 * @param plan The test plan to validate
	 * @param backend The backend to use for validating steps
	 */
	void validate(TestPlan plan, StepProviderBackend backend);

}
