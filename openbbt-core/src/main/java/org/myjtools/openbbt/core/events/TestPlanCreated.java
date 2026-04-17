package org.myjtools.openbbt.core.events;

import java.time.Instant;
import java.util.UUID;

public record TestPlanCreated(
	Instant instant,
	UUID projectID,
	UUID planID,
	boolean hasIssues
) implements Event {
}
