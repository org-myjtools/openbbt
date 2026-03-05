package org.myjtools.openbbt.core.testplan;

import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.extensions.SuiteAssembler;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.util.Hash;
import org.myjtools.openbbt.core.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlanBuilder {

	private static final Log log = Log.of();

	private final OpenBBTRuntime runtime;

	public PlanBuilder(OpenBBTRuntime runtime) {
		this.runtime = runtime;
	}


	/**
	 * Builds a test plan for the given context by assembling the test plan nodes and
	 * registering the plan in the repository.
	 * If a plan with the same resource set and configuration already exists, it will be reused.
	 * @param context the context for which the test plan should be built
	 * @return the generated PlanID of the registered test plan
	 * @throws OpenBBTException if the test plan could not be assembled or registered
	 */
	public TestPlan buildTestPlan(OpenBBTContext context) {


		TestPlanRepository testPlanRepository = runtime.getRepository(TestPlanRepository.class);

		// create project if not exists
		UUID projectID = testPlanRepository.persistProject(context.testProject());

		String resourceSetHash = runtime.resourceSet().hash();
		String configurationHash = Hash.of(runtime.configuration().toString());

		TestPlan testPlan = testPlanRepository.getPlan(context.testProject(), resourceSetHash, configurationHash).orElse(null);
		if (testPlan == null) {
			// No existing plan found, assemble a new one
			var rootNodeID = assembleTestPlanNodes(context).orElseThrow(
				() -> new OpenBBTException("Failed to assemble test plan for project: {}", context.testProject().name())
			);
			testPlan = new TestPlan(
				null,
				projectID,
				runtime.clock().now(),
				resourceSetHash,
				configurationHash,
				rootNodeID
			);
			testPlan = testPlanRepository.persistPlan(testPlan);
			log.debug("Registered new test plan: {}", testPlan.planID());
		} else {
			log.debug("Reusing existing test plan: {}", testPlan.planID());
		}
		return testPlan;
	}



	/*
	 * Assembles the test plan for the given context by invoking all registered SuiteAssemblers.
	 */
	private Optional<UUID> assembleTestPlanNodes(OpenBBTContext context) {
		TestPlanRepository planNodeRepository = runtime.getRepository(TestPlanRepository.class);
		List<SuiteAssembler> assemblers = runtime.getExtensions(SuiteAssembler.class).toList();
		if (assemblers.isEmpty()) {
			log.warn("No SuiteAssembler found, cannot assemble test plan");
			return Optional.empty();
		}
		List<UUID> nodes = new ArrayList<>();
		for (String suiteName : context.testSuites()) {
			TestSuite testSuite = context.testSuite(suiteName).orElseThrow(
					() -> new OpenBBTException("Test suite not found in project: {}", suiteName)
			);
			for (SuiteAssembler assembler : assemblers) {
				assembler.assembleSuite(testSuite).ifPresent(nodes::add);
			}
		}
		if (nodes.isEmpty()) {
			log.warn("No test plan nodes assembled for test suites: {}", context.testSuites());
			return Optional.empty();
		}
		TestPlanNode root = new TestPlanNode(NodeType.TEST_PLAN);
		root.name("Test Plan");
		var rootID = planNodeRepository.persistNode(root);
		for (UUID nodeId : nodes) {
			planNodeRepository.attachChildNodeLast(rootID, nodeId);
		}
		return Optional.ofNullable(rootID);
	}


}
