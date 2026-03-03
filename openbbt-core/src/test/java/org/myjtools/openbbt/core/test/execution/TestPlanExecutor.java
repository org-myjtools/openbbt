package org.myjtools.openbbt.core.test.execution;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.execution.PlanExecutor;
import org.myjtools.openbbt.core.execution.Result;
import org.myjtools.openbbt.core.plan.NodeType;
import org.myjtools.openbbt.core.plan.PlanNode;
import org.myjtools.openbbt.core.util.Pair;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import static org.assertj.core.api.Assertions.assertThat;

class TestPlanExecutor {

	static final Config TEST_CONFIG = Config.ofMap(Map.of(
			OpenBBTConfig.RESOURCE_PATH, "src/test/resources",
			OpenBBTConfig.ENV_PATH, "target/.openbbt"
	));

	@Nested
	class TestsEnglish {

	}
		@Test
		void testRunStepWithOneParameter() throws ExecutionException, InterruptedException {
			var cm = new OpenBBTRuntime(TEST_CONFIG);
			var executor = new PlanExecutor(cm);
			PlanNode node = new PlanNode(NodeType.TEST_CASE)
					.name("Step with one parameter: 5")
					.language("en");
			var future = executor.submitExecution(node);
			assertThat(future.get()).isEqualTo(Pair.of(Result.PASSED,null));
		}


	@Test
	void testRunStepWithInvalidStep() throws ExecutionException, InterruptedException {
		var cm = new OpenBBTRuntime(TEST_CONFIG);
		var executor = new PlanExecutor(cm);
		PlanNode node = new PlanNode(NodeType.TEST_CASE)
				.name("XX Step with one parameter: 5")
				.language("en");
		var future = executor.submitExecution(node);
		assertThat(future.get().left()).isEqualTo(Result.UNDEFINED);
	}


	@Test
	void testStepThatAlwaysFails() throws ExecutionException, InterruptedException {
		var cm = new OpenBBTRuntime(TEST_CONFIG);
		var executor = new PlanExecutor(cm);
		PlanNode node = new PlanNode(NodeType.TEST_CASE)
				.name("stepThatAlwaysFails")
				.language("en");
		var future = executor.submitExecution(node);
		assertThat(future.get().left()).isEqualTo(Result.FAILED);
		assertThat(future.get().right()).hasMessage("This step is designed to always fail");
	}


	@Test
	void stepWithUnexpectedError() throws ExecutionException, InterruptedException {
		var cm = new OpenBBTRuntime(TEST_CONFIG);
		var executor = new PlanExecutor(cm);
		PlanNode node = new PlanNode(NodeType.TEST_CASE)
				.name("stepWithUnexpectedError")
				.language("en");
		var future = executor.submitExecution(node);
		assertThat(future.get().left()).isEqualTo(Result.ERROR);
		assertThat(future.get().right()).hasMessage("java.lang.IllegalArgumentException: This step is designed to throw an unexpected error");
	}


}
