package org.myjtools.openbbt.api.contributors;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.openbbt.api.Contributor;


/**
 * Interface for contributors that provide configuration.
 * <p>
 * Implementations of this interface should provide a method to retrieve
 * the configuration object.
 */
@ExtensionPoint(version = "1.0")
public interface ConfigProvider extends Contributor {

	/**
	 * @return a configuration object
	 */
	Config config();


}
