package org.myjtools.openbbt.core.project;

import org.myjtools.openbbt.core.plannode.PlanNodeID;
import java.time.Instant;

public record Plan(
	PlanID planID,
	Instant createdAt,
	String resourceSetHash,
	String configurationHash,
	PlanNodeID planNodeRoot
) {

}