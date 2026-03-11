package org.myjtools.openbbt.core.persistence;


import org.myjtools.openbbt.core.execution.ExecutionResult;
import org.myjtools.openbbt.core.execution.TestExecution;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface TestExecutionRepository extends Repository {

	TestExecution newExecution(UUID planID, Instant executedAt);

	UUID newExecutionNode(UUID executionID, UUID testPlanNodeID);

	Optional<UUID> getExecutionNodeByPlanNode(UUID executionID, UUID testPlanNodeID);

	void updateExecutionNodeStart(UUID executionNodeID, Instant startedAt);

	void updateExecutionNodeFinish(UUID executionNodeID, ExecutionResult result, Instant finishedAt);

	void updateExecutionNodeMessage(UUID executionNodeID, String message);

	UUID newAttachment(UUID executionNodeID);
}
