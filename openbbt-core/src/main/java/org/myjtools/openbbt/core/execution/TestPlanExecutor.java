package org.myjtools.openbbt.core.execution;

import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.persistence.AttachmentRepository;
import org.myjtools.openbbt.core.persistence.TestExecutionRepository;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.testplan.NodeType;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.core.testplan.TestPlanNode;
import org.myjtools.openbbt.core.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class TestPlanExecutor {

	private record Result (ExecutionResult result, String message, Throwable error) {}


	private static final Log log = Log.of();

	private final OpenBBTRuntime runtime;
	private final TestPlanRepository testPlanRepository;
	private final TestExecutionRepository testExecutionRepository;
	private final AttachmentRepository attachmentRepository;
	private final String parallelTag;
	private final ExecutorService parallelExecutor = Executors.newCachedThreadPool();
	private final ExecutorService mainExecutor = Executors.newSingleThreadExecutor();

	public TestPlanExecutor(OpenBBTRuntime runtime) {
		this.runtime = runtime;
		this.testPlanRepository = runtime.getRepository(TestPlanRepository.class);
		this.testExecutionRepository = runtime.getRepository(TestExecutionRepository.class);
		this.attachmentRepository = runtime.getRepository(AttachmentRepository.class);
		this.parallelTag = runtime.configuration().getString(OpenBBTConfig.PARALLEL_EXECUTION_TAG).orElseThrow(
			() -> new OpenBBTException("Configuration key {} not found", OpenBBTConfig.PARALLEL_EXECUTION_TAG)
		);
	}

	public TestExecution execute(UUID planID) {
		return execute(planID, null);
	}

	public TestExecution execute(UUID planID, Consumer<UUID> onExecutionCreated) {
		TestPlan testPlan = testPlanRepository.getPlan(planID).orElseThrow(
			() -> new OpenBBTException("Test plan with ID {} not found", planID)
		);
		TestPlanNode planRoot = testPlanRepository.getNodeData(testPlan.planNodeRoot()).orElseThrow(
			() -> new OpenBBTException("Test plan root node with ID {} not found", testPlan.planNodeRoot())
		);
		if (planRoot.hasIssues()) {
			throw new OpenBBTException("Test plan has issues, cannot be executed");
		}
		TestExecution execution = testExecutionRepository.newExecution(planID, runtime.clock().now());
		if (onExecutionCreated != null) {
			onExecutionCreated.accept(execution.executionID());
		}
		UUID rootExecutionNodeID = createExecutionNodes(execution.executionID(), planRoot.nodeID());
		execution.executionRootNodeID(rootExecutionNodeID);

		executeTestPlanNode(execution.executionID(), planRoot.nodeID(), null);
		return execution;
	}


	private ExecutionResult executeTestPlanNode(UUID executionID, UUID testPlanNodeID, BackendExecutor backendExecutor) {
		UUID executionNodeID = testExecutionRepository.getExecutionNodeByPlanNode(executionID, testPlanNodeID)
		.orElseThrow(
			() -> new OpenBBTException("Execution node for test plan node with ID {} not found", testPlanNodeID)
		);
		testExecutionRepository.updateExecutionNodeStart(executionNodeID, runtime.clock().now());
		ExecutionResult result = doExecuteTestPlanNode(executionID, executionNodeID, testPlanNodeID, backendExecutor);
		testExecutionRepository.updateExecutionNodeFinish(executionNodeID, result, runtime.clock().now());
		return result;
	}


	private ExecutionResult doExecuteTestPlanNode(
		UUID executionID,
		UUID executionNodeID,
		UUID testPlanNodeID,
		BackendExecutor backendExecutor
	) {
		TestPlanNode node = testPlanRepository.getNodeData(testPlanNodeID).orElseThrow(
			() -> new OpenBBTException("Test plan node with ID {} not found", testPlanNodeID)
		);

		if (node.nodeType() == NodeType.VIRTUAL_STEP) {
			return ExecutionResult.PASSED;
		}

		ExecutionResult ownResult = ExecutionResult.PASSED;
		if (node.nodeType() == NodeType.STEP) {
			ownResult = recordStepExecution(executionID, executionNodeID, backendExecutor, node);
		} else if (node.nodeType() == NodeType.TEST_CASE) {
			backendExecutor = new BackendExecutor(runtime);
			backendExecutor.setUp(executionID, executionNodeID, node.properties());
		}

		ExecutionResult childrenResult = executeChildren(executionID, testPlanNodeID, backendExecutor);

		if (node.nodeType() == NodeType.TEST_CASE) {
			backendExecutor.tearDown();
		}

		return merge(ownResult, childrenResult);
	}


	private ExecutionResult executeChildren(UUID executionID, UUID testPlanNodeID, BackendExecutor backendExecutor) {
		List<UUID> children = testPlanRepository.getNodeChildren(testPlanNodeID).toList();
		if (children.isEmpty()) {
			return ExecutionResult.PASSED;
		}
		if (backendExecutor == null && children.size() > 1) {
			return executeChildrenParallel(executionID, children);
		}
		return executeChildrenSequential(executionID, children, backendExecutor);
	}


	private ExecutorService determineExecutor(TestPlanNode node) {
		return node.hasTag(parallelTag) ? parallelExecutor : mainExecutor;
	}


	private ExecutionResult executeChildrenParallel(UUID executionID, List<UUID> children) {
		List<CompletableFuture<ExecutionResult>> futures = children.stream()
			.map(childNodeID -> {
				TestPlanNode childNode = testPlanRepository.getNodeData(childNodeID).orElseThrow(
					() -> new OpenBBTException("Test plan node with ID {} not found", childNodeID)
				);
				return CompletableFuture.supplyAsync(
					() -> executeTestPlanNode(executionID, childNodeID, null),
					determineExecutor(childNode)
				);
			})
			.toList();
		ExecutionResult finalResult = ExecutionResult.PASSED;
		for (CompletableFuture<ExecutionResult> future : futures) {
			finalResult = merge(finalResult, future.join());
		}
		return finalResult;
	}


	private ExecutionResult executeChildrenSequential(UUID executionID, List<UUID> children, BackendExecutor backendExecutor) {
		ExecutionResult finalResult = ExecutionResult.PASSED;
		for (UUID childNodeID : children) {
			finalResult = merge(finalResult, executeTestPlanNode(executionID, childNodeID, backendExecutor));
		}
		return finalResult;
	}


	private ExecutionResult recordStepExecution(
		UUID executionID, UUID executionNodeID, BackendExecutor backendExecutor, TestPlanNode node
	) {
		Result stepResult = executeTestCaseStep(backendExecutor, node, executionNodeID);
		if (stepResult.message() != null) {
			testExecutionRepository.updateExecutionNodeMessage(executionNodeID, stepResult.message());
		}
		if (stepResult.error() != null) {
			storeStackTraceAttachment(executionID, executionNodeID, stepResult.error());
		}
		return stepResult.result();
	}


	private Result executeTestCaseStep(BackendExecutor backendExecutor, TestPlanNode node, UUID executionNodeID) {
		try {
			var stepResult = backendExecutor.submitStepExecution(node, executionNodeID).get();
			if (stepResult.left() == ExecutionResult.PASSED) {
				return new Result(ExecutionResult.PASSED,null,null);
			} else if (stepResult.left() == ExecutionResult.FAILED) {
				return new Result(ExecutionResult.FAILED, stepResult.right().getMessage(), null);
			} else if (stepResult.left() == ExecutionResult.UNDEFINED) {
				log.error("Step execution failed with no matching step definition: {}", stepResult.right().getMessage());
				return new Result(ExecutionResult.UNDEFINED, node.name(), null);
			} else if (stepResult.left() == ExecutionResult.SKIPPED) {
				return new Result(ExecutionResult.SKIPPED, null, null);
			}else {
				return new Result(ExecutionResult.ERROR, stepResult.right().getMessage(), stepResult.right());
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error(e);
			return new Result(ExecutionResult.ERROR, e.getMessage(), e);
		} catch (Exception e) {
			log.error(e);
			return new Result(ExecutionResult.ERROR, e.getMessage(), e);
		}
	}


	private UUID createExecutionNodes(UUID executionID, UUID planNodeID) {
		UUID executionNodeID = testExecutionRepository.newExecutionNode(executionID, planNodeID);
		testPlanRepository.getNodeChildren(planNodeID)
			.forEach(childNodeID -> createExecutionNodes(executionID, childNodeID));
		return executionNodeID;
	}


	private void storeStackTraceAttachment(UUID executionID, UUID executionNodeID, Throwable error) {
		UUID attachmentID = testExecutionRepository.newAttachment(executionNodeID);
		String stackTrace = getStackTraceAsString(error);
		attachmentRepository.storeAttachment(
			executionID,
			executionNodeID,
			attachmentID,
			stackTrace.getBytes(StandardCharsets.UTF_8),
			"text/plain"
		);
	}


	private String getStackTraceAsString(Throwable error) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		error.printStackTrace(pw);
		return sw.toString();
	}


	private static ExecutionResult merge(ExecutionResult a, ExecutionResult b) {
		return mergePriority(a) >= mergePriority(b) ? a : b;
	}

	private static int mergePriority(ExecutionResult result) {
		return switch (result) {
			case ERROR    -> 4;
			case FAILED   -> 3;
			case SKIPPED  -> 2;
			case PASSED   -> 1;
			default       -> 4; // UNDEFINED treated as ERROR
		};
	}

}