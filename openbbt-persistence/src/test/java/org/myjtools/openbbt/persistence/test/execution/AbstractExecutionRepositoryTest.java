package org.myjtools.openbbt.persistence.test.execution;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.execution.ExecutionResult;
import org.myjtools.openbbt.core.execution.TestExecution;
import org.myjtools.openbbt.core.testplan.NodeType;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.core.testplan.TestPlanNode;
import org.myjtools.openbbt.core.testplan.TestProject;
import org.myjtools.openbbt.persistence.DataSourceProvider;
import org.myjtools.openbbt.persistence.execution.JooqExecutionRepository;
import org.myjtools.openbbt.persistence.plan.JooqPlanRepository;
import javax.sql.DataSource;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractExecutionRepositoryTest {

	protected JooqExecutionRepository repo;
	protected JooqPlanRepository planRepo;
	private DataSource dataSource;

	protected abstract DataSourceProvider dataSourceProvider();

	@BeforeEach
	void setUp() {
		DataSourceProvider provider = dataSourceProvider();
		dataSource = provider.obtainDataSource();
		planRepo = new JooqPlanRepository(dataSource, provider.dialect());
		repo = new JooqExecutionRepository(dataSource, provider.dialect());
		repo.clearAllData();
		planRepo.clearAllData();
	}

	@AfterEach
	void tearDown() {
		if (dataSource instanceof HikariDataSource hikari) {
			hikari.close();
		}
	}

	// --- helpers ---

	private UUID persistPlanWithRoot() {
		UUID root = planRepo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
		UUID projectID = planRepo.persistProject(new TestProject("P", "desc", "Org", List.of()));
		TestPlan plan = planRepo.persistPlan(new TestPlan(null, projectID, Instant.now(), "rh", "ch", root));
		return plan.planID();
	}

	private UUID persistPlanNodeUnder(UUID parent, NodeType type, String name) {
		UUID node = planRepo.persistNode(new TestPlanNode().nodeType(type).name(name));
		planRepo.attachChildNodeLast(parent, node);
		return node;
	}

	// --- newExecution ---

	@Test
	void newExecution_returnsPopulatedExecution() {
		UUID planID = persistPlanWithRoot();
		Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		TestExecution execution = repo.newExecution(planID, now);

		assertThat(execution.executionID()).isNotNull();
		assertThat(execution.planID()).isEqualTo(planID);
		assertThat(execution.executedAt()).isEqualTo(now);
	}

	@Test
	void newExecution_eachCallProducesDistinctId() {
		UUID planID = persistPlanWithRoot();

		TestExecution first = repo.newExecution(planID, Instant.now());
		TestExecution second = repo.newExecution(planID, Instant.now());

		assertThat(first.executionID()).isNotEqualTo(second.executionID());
	}

	// --- newExecutionNode ---

	@Test
	void newExecutionNode_returnsNewId() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now());

		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		assertThat(executionNodeID).isNotNull();
	}

	@Test
	void newExecutionNode_multipleNodesForSameExecution_returnDistinctIds() {
		UUID planID = persistPlanWithRoot();
		UUID rootPlanNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		UUID casePlanNodeID = persistPlanNodeUnder(rootPlanNodeID, NodeType.TEST_CASE, "case");
		TestExecution execution = repo.newExecution(planID, Instant.now());

		UUID node1 = repo.newExecutionNode(execution.executionID(), rootPlanNodeID);
		UUID node2 = repo.newExecutionNode(execution.executionID(), casePlanNodeID);

		assertThat(node1).isNotEqualTo(node2);
	}

	// --- getExecutionNodeByPlanNode ---

	@Test
	void getExecutionNodeByPlanNode_returnsCorrectNode() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now());
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		Optional<UUID> found = repo.getExecutionNodeByPlanNode(execution.executionID(), planNodeID);

		assertThat(found).contains(executionNodeID);
	}

	@Test
	void getExecutionNodeByPlanNode_returnsEmptyForUnknownPlanNode() {
		UUID planID = persistPlanWithRoot();
		TestExecution execution = repo.newExecution(planID, Instant.now());

		Optional<UUID> found = repo.getExecutionNodeByPlanNode(execution.executionID(), UUID.randomUUID());

		assertThat(found).isEmpty();
	}

	@Test
	void getExecutionNodeByPlanNode_isolatesAcrossExecutions() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution exec1 = repo.newExecution(planID, Instant.now());
		TestExecution exec2 = repo.newExecution(planID, Instant.now());
		UUID nodeInExec1 = repo.newExecutionNode(exec1.executionID(), planNodeID);
		UUID nodeInExec2 = repo.newExecutionNode(exec2.executionID(), planNodeID);

		assertThat(repo.getExecutionNodeByPlanNode(exec1.executionID(), planNodeID)).contains(nodeInExec1);
		assertThat(repo.getExecutionNodeByPlanNode(exec2.executionID(), planNodeID)).contains(nodeInExec2);
	}

	// --- updateExecutionNodeStart ---

	@Test
	void updateExecutionNodeStart_setsStartedAt() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now());
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);
		Instant startedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		repo.updateExecutionNodeStart(executionNodeID, startedAt);

		assertThat(repo.getExecutionNodeStartedAt(executionNodeID)).contains(startedAt);
	}

	@Test
	void updateExecutionNodeStart_isNullBeforeUpdate() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now());
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		assertThat(repo.getExecutionNodeStartedAt(executionNodeID)).isEmpty();
	}

	// --- updateExecutionNodeFinish ---

	@Test
	void updateExecutionNodeFinish_setsResultAndFinishedAt() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now());
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);
		Instant finishedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);

		repo.updateExecutionNodeFinish(executionNodeID, ExecutionResult.PASSED, finishedAt);

		assertThat(repo.getExecutionNodeResult(executionNodeID)).contains(ExecutionResult.PASSED);
		assertThat(repo.getExecutionNodeFinishedAt(executionNodeID)).contains(finishedAt);
	}

	@Test
	void updateExecutionNodeFinish_allResultValues() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now());

		for (ExecutionResult result : ExecutionResult.values()) {
			UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);
			repo.updateExecutionNodeFinish(executionNodeID, result, Instant.now());
			assertThat(repo.getExecutionNodeResult(executionNodeID)).contains(result);
		}
	}

	@Test
	void updateExecutionNodeFinish_resultIsNullBeforeUpdate() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now());
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		assertThat(repo.getExecutionNodeResult(executionNodeID)).isEmpty();
		assertThat(repo.getExecutionNodeFinishedAt(executionNodeID)).isEmpty();
	}

	// --- updateExecutionNodeMessage ---

	@Test
	void updateExecutionNodeMessage_setsMessage() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now());
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		repo.updateExecutionNodeMessage(executionNodeID, "assertion failed: expected 200 but was 404");

		assertThat(repo.getExecutionNodeMessage(executionNodeID))
			.contains("assertion failed: expected 200 but was 404");
	}

	@Test
	void updateExecutionNodeMessage_isNullBeforeUpdate() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now());
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		assertThat(repo.getExecutionNodeMessage(executionNodeID)).isEmpty();
	}

	@Test
	void updateExecutionNodeMessage_overwritesPreviousMessage() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now());
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		repo.updateExecutionNodeMessage(executionNodeID, "first message");
		repo.updateExecutionNodeMessage(executionNodeID, "second message");

		assertThat(repo.getExecutionNodeMessage(executionNodeID)).contains("second message");
	}

	// --- newAttachment ---

	@Test
	void newAttachment_createsAttachmentRecord() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now());
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		UUID attachmentID = repo.newAttachment(executionNodeID);

		assertThat(attachmentID).isNotNull();
		assertThat(repo.existsAttachment(attachmentID)).isTrue();
	}

	@Test
	void newAttachment_multipleAttachmentsForSameNode_returnDistinctIds() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		TestExecution execution = repo.newExecution(planID, Instant.now());
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		UUID att1 = repo.newAttachment(executionNodeID);
		UUID att2 = repo.newAttachment(executionNodeID);

		assertThat(att1).isNotEqualTo(att2);
		assertThat(repo.existsAttachment(att1)).isTrue();
		assertThat(repo.existsAttachment(att2)).isTrue();
	}

	// --- full lifecycle ---

	@Test
	void fullExecutionLifecycle_singleNode() {
		UUID planID = persistPlanWithRoot();
		UUID planNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		Instant startedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		Instant finishedAt = startedAt.plusSeconds(2);

		TestExecution execution = repo.newExecution(planID, startedAt);
		UUID executionNodeID = repo.newExecutionNode(execution.executionID(), planNodeID);

		repo.updateExecutionNodeStart(executionNodeID, startedAt);
		repo.updateExecutionNodeFinish(executionNodeID, ExecutionResult.FAILED, finishedAt);
		repo.updateExecutionNodeMessage(executionNodeID, "step failed");
		UUID attachmentID = repo.newAttachment(executionNodeID);

		assertThat(repo.getExecutionNodeByPlanNode(execution.executionID(), planNodeID)).contains(executionNodeID);
		assertThat(repo.getExecutionNodeStartedAt(executionNodeID)).contains(startedAt);
		assertThat(repo.getExecutionNodeFinishedAt(executionNodeID)).contains(finishedAt);
		assertThat(repo.getExecutionNodeResult(executionNodeID)).contains(ExecutionResult.FAILED);
		assertThat(repo.getExecutionNodeMessage(executionNodeID)).contains("step failed");
		assertThat(repo.existsAttachment(attachmentID)).isTrue();
	}

	@Test
	void fullExecutionLifecycle_multipleNodes() {
		UUID planID = persistPlanWithRoot();
		UUID rootPlanNodeID = planRepo.searchNodes(
			org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria.withNodeType(NodeType.TEST_PLAN)
		).findFirst().orElseThrow();
		UUID casePlanNodeID = persistPlanNodeUnder(rootPlanNodeID, NodeType.TEST_CASE, "case1");
		UUID stepPlanNodeID = persistPlanNodeUnder(casePlanNodeID, NodeType.STEP, "step1");

		TestExecution execution = repo.newExecution(planID, Instant.now());
		UUID rootExecNodeID = repo.newExecutionNode(execution.executionID(), rootPlanNodeID);
		UUID caseExecNodeID = repo.newExecutionNode(execution.executionID(), casePlanNodeID);
		UUID stepExecNodeID = repo.newExecutionNode(execution.executionID(), stepPlanNodeID);

		repo.updateExecutionNodeStart(rootExecNodeID, Instant.now());
		repo.updateExecutionNodeStart(caseExecNodeID, Instant.now());
		repo.updateExecutionNodeStart(stepExecNodeID, Instant.now());
		repo.updateExecutionNodeFinish(stepExecNodeID, ExecutionResult.PASSED, Instant.now());
		repo.updateExecutionNodeFinish(caseExecNodeID, ExecutionResult.PASSED, Instant.now());
		repo.updateExecutionNodeFinish(rootExecNodeID, ExecutionResult.PASSED, Instant.now());

		assertThat(repo.getExecutionNodeResult(rootExecNodeID)).contains(ExecutionResult.PASSED);
		assertThat(repo.getExecutionNodeResult(caseExecNodeID)).contains(ExecutionResult.PASSED);
		assertThat(repo.getExecutionNodeResult(stepExecNodeID)).contains(ExecutionResult.PASSED);
	}

}