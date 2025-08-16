package org.myjtools.openbbt.core.messages;

import org.myjtools.jexten.Extension;

@Extension(name = AssertionMessageProvider.NAME)
public class AssertionMessageProvider extends MessageAdapter implements MessageProvider {

    public static final String NAME = "org.myjtools.openbbt.core.assertions";

    public AssertionMessageProvider() {
        super(NAME);
    }

}
