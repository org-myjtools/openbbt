package org.myjtools.openbbt.core.events;

import org.myjtools.openbbt.core.execution.ExecutionResult;
import java.time.Instant;
import java.util.UUID;

public record ExecutionFinished(
	Instant instant,
	UUID executionID,
	UUID planID,
	String profile,
	ExecutionResult result
) implements Event {
}
