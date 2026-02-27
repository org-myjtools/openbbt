package org.myjtools.openbbt.core.plan;

import java.util.List;

public record Project(
	String name,
	String description,
	String organization,
	List<TestSuite> testSuites
) {


}
