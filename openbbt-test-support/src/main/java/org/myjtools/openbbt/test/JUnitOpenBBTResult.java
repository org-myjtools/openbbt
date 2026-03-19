package org.myjtools.openbbt.test;

import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.execution.ExecutionResult;
import org.myjtools.openbbt.core.execution.TestExecution;
import org.myjtools.openbbt.core.persistence.TestExecutionRepository;
import org.myjtools.openbbt.core.persistence.TestPlanNodeCriteria;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.testplan.NodeType;
import org.myjtools.openbbt.core.testplan.TestPlan;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Result of a full OpenBBT test plan execution, returned by {@link JUnitOpenBBTPlan#execute()}.
 * Provides fluent assertion methods over all test cases in the executed plan.
 */
public class JUnitOpenBBTResult {

    private final OpenBBTRuntime runtime;
    private final TestPlan plan;
    private final TestExecution execution;

    JUnitOpenBBTResult(OpenBBTRuntime runtime, TestPlan plan, TestExecution execution) {
        this.runtime = runtime;
        this.plan = plan;
        this.execution = execution;
    }

    public JUnitOpenBBTResult assertAllPassed() {
        return assertAll(ExecutionResult.PASSED);
    }

    public JUnitOpenBBTResult assertAllFailed() {
        return assertAll(ExecutionResult.FAILED);
    }

    public TestExecution execution() {
        return execution;
    }

    public OpenBBTRuntime runtime() {
        return runtime;
    }

    private JUnitOpenBBTResult assertAll(ExecutionResult expected) {
        TestPlanRepository planRepo = runtime.getRepository(TestPlanRepository.class);
        TestExecutionRepository execRepo = runtime.getRepository(TestExecutionRepository.class);

        planRepo.searchNodes(TestPlanNodeCriteria.and(
            TestPlanNodeCriteria.descendantOf(plan.planNodeRoot()),
            TestPlanNodeCriteria.withNodeType(NodeType.TEST_CASE)
        )).forEach(nodeId -> {
            UUID execNodeId = execRepo
                .getExecutionNodeByPlanNode(execution.executionID(), nodeId)
                .orElseThrow(() -> new AssertionError("No execution node found for plan node " + nodeId));
            assertThat(execRepo.getExecutionNodeResult(execNodeId))
                .as("result for test case node %s", nodeId)
                .contains(expected);
        });

        return this;
    }
}
