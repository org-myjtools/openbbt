package org.myjtools.openbbt.core.events;

import java.time.Instant;
import java.util.UUID;

public record ExecutionNodeFinished(
		Instant instant,
		UUID executionID,
		UUID executionNodeID,
		UUID testPlanNodeID,
		org.myjtools.openbbt.core.execution.ExecutionResult result) implements Event {

}