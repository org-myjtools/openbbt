package org.myjtools.openbbt.core.plan;

import java.util.UUID;
import java.time.Instant;

public record Plan(
	UUID planID,
	UUID projectID,
	Instant createdAt,
	String resourceSetHash,
	String configurationHash,
	UUID planNodeRoot
) {

}