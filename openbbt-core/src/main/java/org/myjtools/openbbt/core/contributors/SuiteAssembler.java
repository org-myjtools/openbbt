package org.myjtools.openbbt.core.contributors;

import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.openbbt.core.plannode.PlanNodeID;
import org.myjtools.openbbt.core.project.TestSuite;
import java.util.Optional;


/**
 * Interface for contributors that assembles plans based on resources.
 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
@ExtensionPoint
public interface SuiteAssembler extends Contributor {


	Optional<PlanNodeID> assembleSuite(TestSuite testSuite);

}
