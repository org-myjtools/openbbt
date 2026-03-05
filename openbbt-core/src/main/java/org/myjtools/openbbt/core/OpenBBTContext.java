package org.myjtools.openbbt.core;

import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.testplan.TestProject;
import org.myjtools.openbbt.core.testplan.TestSuite;
import java.util.List;
import java.util.Optional;

public record OpenBBTContext(
	TestProject testProject,
	Config configuration,
	List<String> testSuites,
	String profile,
	List<String> plugins
){

	public Optional<TestSuite> testSuite(String name) {
		return testProject.testSuites().stream().filter(suite -> suite.name().equals(name)).findFirst();
	}

}
