package org.myjtools.openbbt.it;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.myjtools.openbbt.core.messages.MessageAdapter;
import org.myjtools.openbbt.core.messages.MessageProvider;

@Extension(scope = Scope.SINGLETON)
public class TestValidationStepProviderMessageProvider extends MessageAdapter implements MessageProvider {

	public TestValidationStepProviderMessageProvider() {
		super(TestValidationStepProvider.class.getSimpleName());
	}

}