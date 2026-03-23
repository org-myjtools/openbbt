package org.myjtools.openbbt.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.testplan.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlanValidatorTest {

	@Test
	void validStep_hasNoValidationIssues(@TempDir Path tempDir) {
		var plan = buildPlan("validStep", tempDir);
		var repo = (TestPlanRepository) plan.runtime().getRepository(TestPlanRepository.class);

		assertThat(repo.planHasIssues(plan.testPlan().planID())).isFalse();

		TestPlanNode stepNode = findNodeOfType(repo, plan.testPlan(), NodeType.STEP);
		assertThat(stepNode.validationStatus()).isEqualTo(ValidationStatus.OK);
		assertThat(stepNode.hasIssues()).isFalse();
	}

	@Test
	void invalidStep_hasValidationError(@TempDir Path tempDir) {
		var plan = buildPlan("invalidStep", tempDir);
		var repo = (TestPlanRepository) plan.runtime().getRepository(TestPlanRepository.class);

		assertThat(repo.planHasIssues(plan.testPlan().planID())).isTrue();

		TestPlanNode stepNode = findNodeOfType(repo, plan.testPlan(), NodeType.STEP);
		assertThat(stepNode.validationStatus()).isEqualTo(ValidationStatus.ERROR);
		assertThat(stepNode.validationMessage()).isNotBlank();
		assertThat(stepNode.hasIssues()).isTrue();

		// HAS_ISSUES debe propagarse hasta el nodo raíz
		TestPlanNode root = repo.getNodeData(plan.testPlan().planNodeRoot()).orElseThrow();
		assertThat(root.hasIssues()).isTrue();
	}

	@Test
	void testCaseWithNoSteps_hasValidationError(@TempDir Path tempDir) {
		var plan = buildPlan("testCaseNoSteps", tempDir);
		var repo = (TestPlanRepository) plan.runtime().getRepository(TestPlanRepository.class);

		assertThat(repo.planHasIssues(plan.testPlan().planID())).isTrue();

		TestPlanNode caseNode = findNodeOfType(repo, plan.testPlan(), NodeType.TEST_CASE);
		assertThat(caseNode.validationStatus()).isEqualTo(ValidationStatus.ERROR);
		assertThat(caseNode.validationMessage()).isNotBlank();
	}

	@Test
	void stepAggregatorWithNoStepChildren_hasValidationError(@TempDir Path tempDir) {
		var plan = buildPlan("stepAggregatorNoStepChildren", tempDir);
		var repo = (TestPlanRepository) plan.runtime().getRepository(TestPlanRepository.class);

		assertThat(repo.planHasIssues(plan.testPlan().planID())).isTrue();

		TestPlanNode aggregatorNode = findNodeOfType(repo, plan.testPlan(), NodeType.STEP_AGGREGATOR);
		assertThat(aggregatorNode.validationStatus()).isEqualTo(ValidationStatus.ERROR);
		assertThat(aggregatorNode.validationMessage()).isNotBlank();
	}

	@Test
	void getNodeDescendantsWithIssues_returnsOnlyFailingNodes(@TempDir Path tempDir) {
		var plan = buildPlan("invalidStep", tempDir);
		var repo = (TestPlanRepository) plan.runtime().getRepository(TestPlanRepository.class);

		List<UUID> failingIds = repo.getNodeDescendantsWithIssues(plan.testPlan().planNodeRoot()).toList();

		// Sólo el STEP tiene error propio; los ancestros propagan HAS_ISSUES pero no tienen VALIDATION_STATUS=ERROR
		assertThat(failingIds).hasSize(1);
		TestPlanNode failing = repo.getNodeData(failingIds.getFirst()).orElseThrow();
		assertThat(failing.nodeType()).isEqualTo(NodeType.STEP);
		assertThat(failing.validationStatus()).isEqualTo(ValidationStatus.ERROR);
	}


	// ---- helpers ----

	private record PlanResult(OpenBBTRuntime runtime, TestPlan testPlan) {}

	private PlanResult buildPlan(String suiteName, Path tempDir) {
		Config config = Config.ofMap(Map.of(
			OpenBBTConfig.ENV_PATH,          "target/.openbbt",
			OpenBBTConfig.PERSISTENCE_MODE,  OpenBBTConfig.PERSISTENCE_MODE_FILE,
			OpenBBTConfig.PERSISTENCE_FILE,  tempDir.resolve("plan.db").toString()
		));
		OpenBBTRuntime runtime = new OpenBBTRuntime(config);
		TestPlan plan = runtime.buildTestPlan(createContext(suiteName, config));
		return new PlanResult(runtime, plan);
	}

	private OpenBBTContext createContext(String suiteName, Config config) {
		TestSuite suite = new TestSuite(suiteName, "", null);
		TestProject project = new TestProject("Test Project", "", "Test Org", List.of(suite));
		return new OpenBBTContext(project, config, List.of(suiteName), List.of());
	}

	private TestPlanNode findNodeOfType(TestPlanRepository repo, TestPlan plan, NodeType type) {
		UUID nodeId = repo.searchNodes(TestPlanNodeCriteria.and(
			TestPlanNodeCriteria.descendantOf(plan.planNodeRoot()),
			TestPlanNodeCriteria.withNodeType(type)
		)).findFirst().orElseThrow();
		return repo.getNodeData(nodeId).orElseThrow();
	}
}