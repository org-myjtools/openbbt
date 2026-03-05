package org.myjtools.openbbt.core.contributors;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.ExtensionPoint;

@ExtensionPoint
public interface StepProvider {

	void init(Config config);

}
