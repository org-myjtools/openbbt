package org.myjtools.openbbt.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.execution.ExecutionResult;
import org.myjtools.openbbt.core.execution.TestExecution;
import org.myjtools.openbbt.core.execution.TestPlanExecutor;
import org.myjtools.openbbt.core.persistence.TestExecutionRepository;
import org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.testplan.*;
import org.myjtools.openbbt.persistence.execution.JooqExecutionRepository;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TestPlanExecutorTest {

	@Test
	void execute_passingStep_recordsPassedResult(@TempDir Path tempDir) {
		var ctx = setup("execPassingStep", tempDir);
		TestExecution execution = new TestPlanExecutor(ctx.runtime()).execute(ctx.plan().planID());

		UUID stepNodeID = findNodeOfType(ctx, NodeType.STEP);
		UUID execNodeID = ctx.execRepo().getExecutionNodeByPlanNode(execution.executionID(), stepNodeID).orElseThrow();

		assertThat(ctx.execRepo().getExecutionNodeResult(execNodeID)).contains(ExecutionResult.PASSED);
		assertThat(ctx.execRepo().getExecutionNodeStartedAt(execNodeID)).isPresent();
		assertThat(ctx.execRepo().getExecutionNodeFinishedAt(execNodeID)).isPresent();
	}

	@Test
	void execute_failingStep_recordsFailedResultAndMessage(@TempDir Path tempDir) {
		var ctx = setup("execFailingStep", tempDir);
		TestExecution execution = new TestPlanExecutor(ctx.runtime()).execute(ctx.plan().planID());

		UUID stepNodeID = findNodeOfType(ctx, NodeType.STEP);
		UUID execNodeID = ctx.execRepo().getExecutionNodeByPlanNode(execution.executionID(), stepNodeID).orElseThrow();

		assertThat(ctx.execRepo().getExecutionNodeResult(execNodeID)).contains(ExecutionResult.FAILED);
		assertThat(ctx.execRepo().getExecutionNodeMessage(execNodeID)).isPresent().get().asString().isNotBlank();
	}

	@Test
	void execute_errorStep_recordsErrorResultAndAttachment(@TempDir Path tempDir) {
		var ctx = setup("execErrorStep", tempDir);
		TestExecution execution = new TestPlanExecutor(ctx.runtime()).execute(ctx.plan().planID());

		UUID stepNodeID = findNodeOfType(ctx, NodeType.STEP);
		UUID execNodeID = ctx.execRepo().getExecutionNodeByPlanNode(execution.executionID(), stepNodeID).orElseThrow();

		assertThat(ctx.execRepo().getExecutionNodeResult(execNodeID)).contains(ExecutionResult.ERROR);
		assertThat(ctx.execRepo().getExecutionNodeMessage(execNodeID)).isPresent().get().asString().isNotBlank();
	}

	@Test
	void execute_virtualStep_recordsPassedResultWithoutExecutingStep(@TempDir Path tempDir) {
		var ctx = setup("execVirtualStep", tempDir);
		TestExecution execution = new TestPlanExecutor(ctx.runtime()).execute(ctx.plan().planID());

		UUID virtualStepNodeID = findNodeOfType(ctx, NodeType.VIRTUAL_STEP);
		UUID execNodeID = ctx.execRepo().getExecutionNodeByPlanNode(execution.executionID(), virtualStepNodeID).orElseThrow();

		assertThat(ctx.execRepo().getExecutionNodeResult(execNodeID)).contains(ExecutionResult.PASSED);
	}

	@Test
	void execute_allNodesHaveTimestamps(@TempDir Path tempDir) {
		var ctx = setup("execPassingStep", tempDir);
		TestExecution execution = new TestPlanExecutor(ctx.runtime()).execute(ctx.plan().planID());

		ctx.planRepo().getNodeDescendants(ctx.plan().planNodeRoot())
			.forEach(nodeID -> {
				UUID execNodeID = ctx.execRepo()
					.getExecutionNodeByPlanNode(execution.executionID(), nodeID)
					.orElseThrow(() -> new AssertionError("Missing execution node for plan node " + nodeID));
				assertThat(ctx.execRepo().getExecutionNodeStartedAt(execNodeID))
					.as("startedAt for node %s", nodeID).isPresent();
				assertThat(ctx.execRepo().getExecutionNodeFinishedAt(execNodeID))
					.as("finishedAt for node %s", nodeID).isPresent();
			});
	}

	@Test
	void execute_twoTestCases_allNodesRecorded(@TempDir Path tempDir) {
		var ctx = setup("execTwoTestCases", tempDir);
		TestExecution execution = new TestPlanExecutor(ctx.runtime()).execute(ctx.plan().planID());

		long testCaseCount = ctx.planRepo().getNodeDescendants(ctx.plan().planNodeRoot())
			.map(id -> ctx.planRepo().getNodeData(id).orElseThrow())
			.filter(n -> n.nodeType() == NodeType.TEST_CASE)
			.count();
		assertThat(testCaseCount).isEqualTo(2);

		ctx.planRepo().getNodeDescendants(ctx.plan().planNodeRoot())
			.forEach(nodeID -> assertThat(
				ctx.execRepo().getExecutionNodeByPlanNode(execution.executionID(), nodeID)
			).as("execution node for plan node %s", nodeID).isPresent());
	}

	@Test
	void execute_twoTestCases_bothPass(@TempDir Path tempDir) {
		var ctx = setup("execTwoTestCases", tempDir);
		TestExecution execution = new TestPlanExecutor(ctx.runtime()).execute(ctx.plan().planID());

		ctx.planRepo().searchNodes(TestPlanNodeCriteria.and(
			TestPlanNodeCriteria.descendantOf(ctx.plan().planNodeRoot()),
			TestPlanNodeCriteria.withNodeType(NodeType.TEST_CASE)
		)).forEach(testCaseNodeID -> {
			UUID execNodeID = ctx.execRepo()
				.getExecutionNodeByPlanNode(execution.executionID(), testCaseNodeID)
				.orElseThrow();
			assertThat(ctx.execRepo().getExecutionNodeResult(execNodeID))
				.as("result for test case %s", testCaseNodeID)
				.contains(ExecutionResult.PASSED);
		});
	}


	// ---- helpers ----

	private record ExecutionContext(
		OpenBBTRuntime runtime,
		TestPlan plan,
		TestPlanRepository planRepo,
		JooqExecutionRepository execRepo
	) {}

	private ExecutionContext setup(String suiteName, Path tempDir) {
		Config config = Config.ofMap(Map.of(
			OpenBBTConfig.ENV_PATH,          tempDir.toString(),
			OpenBBTConfig.PERSISTENCE_MODE,  OpenBBTConfig.PERSISTENCE_MODE_FILE,
			OpenBBTConfig.PERSISTENCE_FILE,  tempDir.resolve("test.db").toString()
		));
		OpenBBTRuntime runtime = new OpenBBTRuntime(config);
		TestPlan plan = runtime.buildTestPlan(createContext(suiteName, config));
		TestPlanRepository planRepo = runtime.getRepository(TestPlanRepository.class);
		JooqExecutionRepository execRepo = (JooqExecutionRepository) runtime.getRepository(TestExecutionRepository.class);
		return new ExecutionContext(runtime, plan, planRepo, execRepo);
	}

	private OpenBBTContext createContext(String suiteName, Config config) {
		TestSuite suite = new TestSuite(suiteName, "", null);
		TestProject project = new TestProject("Test Project", "", "Test Org", List.of(suite));
		return new OpenBBTContext(project, config, List.of(suiteName), List.of());
	}

	private UUID findNodeOfType(ExecutionContext ctx, NodeType type) {
		return ctx.planRepo().searchNodes(TestPlanNodeCriteria.and(
			TestPlanNodeCriteria.descendantOf(ctx.plan().planNodeRoot()),
			TestPlanNodeCriteria.withNodeType(type)
		)).findFirst().orElseThrow(() -> new AssertionError("No node of type " + type + " found"));
	}

}
