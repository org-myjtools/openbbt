package org.myjtools.openbbt.core.execution;

import java.time.Instant;
import java.util.UUID;

public class ExecutionNode {

	private UUID execID;
	private Instant startTime;
	private Instant endTime;
	private ExecutionResult result;
	private String message;

	public ExecutionNode(UUID execID, Instant startTime, Instant endTime, ExecutionResult result, String message) {
		this.execID = execID;
		this.startTime = startTime;
		this.endTime = endTime;
		this.result = result;
		this.message = message;
	}

	public UUID execID() {
		return execID;
	}

	public long duration() {
		return endTime.toEpochMilli() - startTime.toEpochMilli();
	}

	public Instant startTime() {
		return startTime;
	}

	public Instant endTime() {
		return endTime;
	}

}
