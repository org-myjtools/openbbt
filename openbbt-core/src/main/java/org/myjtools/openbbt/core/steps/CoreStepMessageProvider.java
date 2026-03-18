package org.myjtools.openbbt.core.steps;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.myjtools.openbbt.core.messages.MessageProvider;
import org.myjtools.openbbt.core.messages.StepDocMessageAdapter;

@Extension(
	name = "Core Step Message Provider",
	extensionPointVersion = "1.0",
	scope = Scope.SINGLETON
)
public class CoreStepMessageProvider extends StepDocMessageAdapter implements MessageProvider {

	public CoreStepMessageProvider() {
		super("core-steps.yaml");
	}

	@Override
	public boolean providerFor(String category) {
		return CoreStepProvider.class.getSimpleName().equals(category);
	}

}
