package org.myjtools.openbbt.plugins.rest;

import org.myjtools.jexten.Extension;
import org.myjtools.openbbt.core.messages.MessageProvider;
import org.myjtools.openbbt.core.messages.StepDocMessageAdapter;

@Extension
public class RestMessageProvider extends StepDocMessageAdapter implements MessageProvider {


	public RestMessageProvider() {
		super("rest-steps.yaml");
	}

	@Override
	public boolean providerFor(String category) {
		return RestStepProvider.class.getSimpleName().equals(category);
	}

}
