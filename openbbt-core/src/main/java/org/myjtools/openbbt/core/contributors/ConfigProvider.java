package org.myjtools.openbbt.core.contributors;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.openbbt.core.Contributor;


/**
 * Interface for contributors that provide configuration.
 * <p>
 * Implementations of this interface should provide a method to retrieve
 * the configuration object.

 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
@ExtensionPoint(version = "1.0")
public interface ConfigProvider extends Contributor {

	/**
	 * @return a configuration object
	 */
	Config config();


}
