package org.myjtools.openbbt.core;

import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.project.Project;
import org.myjtools.openbbt.core.project.TestSuite;
import java.util.List;
import java.util.Optional;

public record OpenBBTContext(
	Project project,
	Config configuration,
	List<String> testSuites,
	String profile,
	List<String> plugins
){

	public Optional<TestSuite> testSuite(String name) {
		return project.testSuites().stream().filter(suite -> suite.name().equals(name)).findFirst();
	}

}
