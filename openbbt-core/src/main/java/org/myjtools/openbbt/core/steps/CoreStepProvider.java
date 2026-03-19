package org.myjtools.openbbt.core.steps;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.myjtools.openbbt.core.contributors.StepProvider;

@Extension(
name = "Core Step Provider",
extensionPointVersion = "1.0",
scope = Scope.TRANSIENT // A new instance of this class will be created for each execution
)
public class CoreStepProvider implements StepProvider {

	@Override
	public void init(Config config) {

	}

}
