package org.myjtools.openbbt.core;

import org.myjtools.openbbt.core.contributors.SuiteAssembler;
import org.myjtools.openbbt.core.persistence.PlanRepository;
import org.myjtools.openbbt.core.plan.*;
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
	public Plan buildTestPlan(OpenBBTContext context) {


		PlanRepository planRepository = runtime.getRepository(PlanRepository.class);

		// create project if not exists
		UUID projectID = planRepository.persistProject(context.project());

		String resourceSetHash = runtime.resourceSet().hash();
		String configurationHash = Hash.of(runtime.configuration().toString());

		Plan plan = planRepository.getPlan(context.project(), resourceSetHash, configurationHash).orElse(null);
		if (plan == null) {
			// No existing plan found, assemble a new one
			var rootNodeID = assembleTestPlanNodes(context).orElseThrow(
				() -> new OpenBBTException("Failed to assemble test plan for project: {}", context.project().name())
			);
			plan = new Plan(
				null,
				projectID,
				runtime.clock().now(),
				resourceSetHash,
				configurationHash,
				rootNodeID
			);
			plan = planRepository.persistPlan(plan);
			log.debug("Registered new test plan: {}", plan.planID());
		} else {
			log.debug("Reusing existing test plan: {}", plan.planID());
		}
		return plan;
	}



	/*
	 * Assembles the test plan for the given context by invoking all registered SuiteAssemblers.
	 */
	private Optional<UUID> assembleTestPlanNodes(OpenBBTContext context) {
		PlanRepository planNodeRepository = runtime.getRepository(PlanRepository.class);
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
		PlanNode root = new PlanNode(NodeType.TEST_PLAN);
		root.name("Test Plan");
		var rootID = planNodeRepository.persistNode(root);
		for (UUID nodeId : nodes) {
			planNodeRepository.attachChildNodeLast(rootID, nodeId);
		}
		return Optional.ofNullable(rootID);
	}


}
