package org.myjtools.openbbt.plugins.db;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.myjtools.openbbt.core.messages.MessageProvider;
import org.myjtools.openbbt.core.messages.StepDocMessageAdapter;

@Extension(
	name = "Database Message Provider",
	extensionPointVersion = "1.0",
	scope = Scope.SINGLETON
)
public class DbMessageProvider extends StepDocMessageAdapter implements MessageProvider {

	public DbMessageProvider() {
		super("steps.yaml");
	}

	@Override
	public boolean providerFor(String category) {
		return DbStepProvider.class.getSimpleName().equals(category);
	}

}
