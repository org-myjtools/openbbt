package org.myjtools.openbbt.it;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.myjtools.openbbt.core.contributors.StepExpression;
import org.myjtools.openbbt.core.contributors.StepProvider;

@Extension(scope = Scope.TRANSIENT)
public class TestValidationStepProvider implements StepProvider {

	@Override
	public void init(Config config) {}

	@StepExpression("a valid step")
	public void aValidStep() {}

	@StepExpression("a failing step")
	public void aFailingStep() {
		throw new AssertionError("step assertion failed");
	}

	@StepExpression("an error step")
	public void anErrorStep() {
		throw new RuntimeException("step unexpected error");
	}

}
