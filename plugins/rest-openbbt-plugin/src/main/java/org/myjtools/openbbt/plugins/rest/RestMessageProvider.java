package org.myjtools.openbbt.plugins.rest;

import org.myjtools.jexten.Extension;
import org.myjtools.openbbt.core.messages.MessageAdapter;
import org.myjtools.openbbt.core.messages.MessageProvider;

@Extension
public class RestMessageProvider extends MessageAdapter implements MessageProvider {

	public RestMessageProvider() {
		super(RestStepProvider.class.getSimpleName(), "rest-messages");
	}

	
}
