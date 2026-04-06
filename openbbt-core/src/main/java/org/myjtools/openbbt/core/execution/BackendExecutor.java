package org.myjtools.openbbt.core.execution;

import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.backend.StepProviderBackend;
import org.myjtools.openbbt.core.testplan.NodeArgument;
import org.myjtools.openbbt.core.testplan.TestPlanNode;
import org.myjtools.openbbt.core.util.Log;
import org.myjtools.openbbt.core.util.Pair;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BackendExecutor {

	private static final Log log  = Log.of();
	private static final long NO_TIMEOUT = Long.MAX_VALUE;

	private final StepProviderBackend backend;
	private final ExecutorService executor;

	private boolean testCaseFailed = false;


	public BackendExecutor(OpenBBTRuntime runtime) {
		this.backend = new StepProviderBackend(runtime);
		this.executor = Executors.newSingleThreadExecutor(); // TODO: make this configurable for parallel execution in the future
	}

	public void setUp(UUID executionID, UUID executionNodeID, Map<String,String> properties) {
		runInExecutor(()-> backend.setUp(executionID, executionNodeID, properties));
	}

	public void tearDown() {
		runInExecutor(backend::tearDown);
	}

	private void runInExecutor(Runnable task) {
		try {
			executor.submit(task).get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new OpenBBTException(e, "Interrupted while running task in executor");
		} catch (ExecutionException e) {
			throw new OpenBBTException(e.getCause(), "Task failed in executor");
		}
	}


	public Future<Pair<ExecutionResult,Throwable>> submitStepExecution(TestPlanNode node) {
		return submitStepExecution(node, null, NO_TIMEOUT);
	}


	public Future<Pair<ExecutionResult,Throwable>> submitStepExecution(
			TestPlanNode node,
			UUID executionNodeID,
			long timeoutSec
	) {
		if (timeoutSec == 0 || timeoutSec == -1) {
			throw new OpenBBTException(
				"Invalid timeout value: {}. Step timeout must be a positive number of seconds.", timeoutSec
			);
		}
		Future<Pair<ExecutionResult,Throwable>> future = this.executor.submit(() -> {
			try {
				if (testCaseFailed) {
					return Pair.of(ExecutionResult.SKIPPED, null);
				}
				backend.run(node.name(), locale(node.language()), nodeArgument(node), executionNodeID);
				return Pair.of(ExecutionResult.PASSED, null);
			} catch (AssertionError e) {
				testCaseFailed = true;
				return Pair.of(ExecutionResult.FAILED, e);
			} catch (NoMatchingStepException e) {
				testCaseFailed = true;
				return Pair.of(ExecutionResult.UNDEFINED, e);
			} catch (Exception e) {
				testCaseFailed = true;
				log.error(e);
				return Pair.of(ExecutionResult.ERROR, e);
			}
		});
		try {
			Pair<ExecutionResult,Throwable> result = timeoutSec == NO_TIMEOUT
				? future.get()
				: future.get(timeoutSec, TimeUnit.SECONDS);
			return CompletableFuture.completedFuture(result);
		} catch (TimeoutException e) {
			future.cancel(true);
			throw new OpenBBTException("Step execution timed out after {} seconds: {}", timeoutSec, node.name());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new OpenBBTException(e, "Interrupted while waiting for step execution");
		} catch (ExecutionException e) {
			throw new OpenBBTException(e.getCause(), "Unexpected error in step execution");
		}
	}


	private NodeArgument nodeArgument(TestPlanNode node) {
		NodeArgument nodeArgument = null;
		if (node.document() != null) {
			nodeArgument = node.document();
		} else if (node.dataTable() != null) {
			nodeArgument = node.dataTable();
		}
		return nodeArgument;
	}


	private Locale locale(String language) {
		if (language == null || language.isBlank()) {
			return Locale.ENGLISH;
		}
		return Locale.forLanguageTag(language);
	}

}
