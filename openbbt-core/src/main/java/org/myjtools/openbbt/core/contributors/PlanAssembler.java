package org.myjtools.openbbt.core.contributors;

import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.openbbt.core.plan.PlanNodeID;
import java.util.Optional;


/**
 * Interface for contributors that assembles plans based on resources.
 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
@ExtensionPoint
public interface PlanAssembler extends Contributor {

	/**
	 * Assemble a plan for the given configuration. It should be stored in the OpenBBT
	 * persistence module.
	 * @return an Optional containing the UUID of the plan if available, or an empty Optional if no plan can be provided
	 */
	Optional<PlanNodeID> assemblePlan();

}
