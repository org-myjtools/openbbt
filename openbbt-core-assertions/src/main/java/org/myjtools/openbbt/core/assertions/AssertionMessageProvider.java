package org.myjtools.openbbt.core.assertions;

import org.myjtools.jexten.Extension;
import org.myjtools.openbbt.core.messages.MessageAdapter;
import org.myjtools.openbbt.core.messages.MessageProvider;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
@Extension(name = AssertionMessageProvider.NAME)
public class AssertionMessageProvider extends MessageAdapter implements MessageProvider {

	public static final String NAME = "assertions";

	public AssertionMessageProvider() {
		super(NAME);
	}

}
