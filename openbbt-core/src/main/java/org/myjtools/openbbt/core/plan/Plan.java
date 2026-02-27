package org.myjtools.openbbt.core.plan;

import java.time.Instant;

public record Plan(
	PlanID planID,
	Instant createdAt,
	String resourceSetHash,
	String configurationHash,
	PlanNodeID planNodeRoot
) {

}