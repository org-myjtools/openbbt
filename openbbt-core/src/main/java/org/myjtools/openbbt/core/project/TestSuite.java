package org.myjtools.openbbt.core.project;

import org.myjtools.openbbt.core.plannode.TagExpression;

public record TestSuite (
	String name,
	String description,
	TagExpression tagExpression
) {


}
