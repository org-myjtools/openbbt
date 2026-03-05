package org.myjtools.openbbt.core.execution;

import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.backend.StepProviderBackend;
import org.myjtools.openbbt.core.testplan.NodeArgument;
import org.myjtools.openbbt.core.testplan.TestPlanNode;
import org.myjtools.openbbt.core.util.Log;
import org.myjtools.openbbt.core.util.Pair;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PlanExecutor {

	private final static Log log  = Log.of();

	private final StepProviderBackend backend;
	private final ExecutorService executor;

	public PlanExecutor(OpenBBTRuntime runtime) {
		this.backend = new StepProviderBackend(runtime);
		this.executor = Executors.newSingleThreadExecutor(); // TODO: make this configurable for parallel execution in the future
	}

	public void setUp() {
		this.backend.setUp();
	}

	public void tearDown() {
		this.backend.tearDown();
	}


	public Future<Pair<Result,Throwable>> submitExecution(TestPlanNode node) {
		return this.executor.submit(() -> {
			try {
				backend.run(node.name(), locale(node.language()), nodeArgument(node));
				return Pair.of(Result.PASSED, null);
			} catch (AssertionError e) {
				return Pair.of(Result.FAILED, e);
			} catch (NoMatchingStepException e) {
				return Pair.of(Result.UNDEFINED, e);
			} catch (Exception e) {
				log.error(e);
				// TODO save the stack trace in the execution repository for reporting
				return Pair.of(Result.ERROR, e);
			}
		});
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
