package org.myjtools.openbbt.core.messages;

import org.myjtools.jexten.Extension;
import org.myjtools.openbbt.core.adapters.MessageAdapter;
import org.myjtools.openbbt.core.contributors.MessageProvider;

@Extension(name = AssertionMessageProvider.NAME)
public class AssertionMessageProvider extends MessageAdapter implements MessageProvider {

    public static final String NAME = "assertions";

    public AssertionMessageProvider() {
        super(NAME);
    }

}
