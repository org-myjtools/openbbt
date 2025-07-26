package org.myjtools.openbbt.core.contributors;

import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.openbbt.core.Resource;

import java.util.List;


/**
 * Common interface for OpenBBT resource providers.
 *
 * Resource providers are used to provide resources that can be used in the
 * OpenBBT application, such as images, icons, or other files.
 */
@ExtensionPoint(version = "1.0")
public interface ResourceProvider {

	/**
	 * Returns a list of resources provided by this provider.
	 *
	 * @return a list of resources
	 */
	List<Resource> resources();
	
}
