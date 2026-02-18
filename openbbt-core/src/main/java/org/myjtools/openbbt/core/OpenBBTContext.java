package org.myjtools.openbbt.core;

import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.project.Project;
import java.util.List;

public record OpenBBTContext(
	Project project,
	Config configuration,
	List<String> testSuites,
	String profile,
	List<String> plugins
){

}
