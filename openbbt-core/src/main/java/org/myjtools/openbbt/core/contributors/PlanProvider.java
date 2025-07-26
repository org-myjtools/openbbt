package org.myjtools.openbbt.core.contributors;

import java.util.*;

import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.openbbt.core.Contributor;
import org.myjtools.openbbt.core.PlanNodeID;
import org.myjtools.openbbt.core.Resource;


/**
 * Interface for contributors that provide plans based on resources.
 * <p>
 * Implementations of this interface should provide a method to check if they
 * can handle a given resource and a method to provide a plan for a list of
 * resources.
 */
@ExtensionPoint
public interface PlanProvider extends Contributor {


	/**
	 * Checks if the provider accepts the given resource.
	 * @param resource the resource to check
	 * @return true if the provider can handle the resource, false otherwise
	 */
	boolean accept(Resource resource);

	/**
	 * Provides a plan for the given resources. It should be stored in the OpenBBT
	 * persistence module.
	 * @param resources the list of resources to provide a plan for
	 * @return an Optional containing the UUID of the plan if available, or an empty Optional if no plan can be provided
	 */
	Optional<PlanNodeID> providePlan(List<Resource> resources);

}
