package org.myjtools.openbbt.core.contributors;

import java.util.UUID;
import org.myjtools.jexten.ExtensionPoint;
import org.myjtools.openbbt.core.testplan.TestSuite;
import java.util.Optional;


/**
 * Interface for contributors that assembles plans based on resources.
 * @author Luis Iñesta Gelabert - luiinge@gmail.com */
@ExtensionPoint
public interface SuiteAssembler extends Contributor {


	Optional<UUID> assembleSuite(TestSuite testSuite);

}
