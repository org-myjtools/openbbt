package org.myjtools.openbbt.core.contributors;

import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.openbbt.core.events.Event;

@ExtensionPoint(version = "1.0")
public interface EventObserver extends Contributor {


	void onEvent(Event event);

}
