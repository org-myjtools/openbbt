package org.myjtools.openbbt.core.project;

import java.util.List;

public record Project(
	String name,
	String description,
	String organization,
	List<TestSuite> testSuites
) {


}
