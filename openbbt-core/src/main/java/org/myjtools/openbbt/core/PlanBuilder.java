package org.myjtools.openbbt.core;

import org.myjtools.openbbt.core.contributors.SuiteAssembler;
import org.myjtools.openbbt.core.persistence.PlanNodeRepository;
import org.myjtools.openbbt.core.persistence.ProjectRepository;
import org.myjtools.openbbt.core.plannode.NodeType;
import org.myjtools.openbbt.core.plannode.PlanNode;
import org.myjtools.openbbt.core.plannode.PlanNodeID;
import org.myjtools.openbbt.core.project.Plan;
import org.myjtools.openbbt.core.project.PlanID;
import org.myjtools.openbbt.core.project.TestSuite;
import org.myjtools.openbbt.core.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlanBuilder {

	private static final Log log = Log.of();

	private final OpenBBTRuntime contextManager;

	public PlanBuilder(OpenBBTRuntime contextManager) {
		this.contextManager = contextManager;
	}


	/**
	 * Builds a test plan for the given context by assembling the test plan nodes and
	 * registering the plan in the repository.
	 * @param context the context for which the test plan should be built
	 * @return the generated PlanID of the registered test plan
	 * @throws OpenBBTException if the test plan could not be assembled or registered
	 */
	public Plan buildTestPlan(OpenBBTContext context) {
		var rootNodeID = assembleTestPlanNodes(context).orElseThrow(
			() -> new OpenBBTException("Failed to assemble test plan for project: {}", context.project().name())
		);
		return registerPlan(context, rootNodeID);
	}


	/*
	 * Registers the assembled test plan in the repository and returns the generated PlanID.
	 */
	private Plan registerPlan(OpenBBTContext context, PlanNodeID rootNodeID) {
		ProjectRepository projectRepository = contextManager.getRepository(ProjectRepository.class);
		projectRepository.persistProject(context.project());
		Plan plan = new Plan(
			null,
			contextManager.clock().now(),
			contextManager.resourceSet().hash(),
			null,
			rootNodeID
		);
		PlanID planID = projectRepository.persistPlan(context.project(), plan);
		projectRepository.deleteTestSuites(planID);
		for (String suiteName : context.testSuites()) {
			TestSuite testSuite = context.testSuite(suiteName).orElseThrow(
				() -> new OpenBBTException("Test suite not found in project: {}", suiteName)
			);
			projectRepository.persistTestSuite(context.project(), plan, testSuite);
		}
		return projectRepository.getPlan(planID).orElseThrow(
			() -> new OpenBBTException("Failed to retrieve registered plan with ID: {}", planID)
		);
	}

	/*
	 * Assembles the test plan for the given context by invoking all registered SuiteAssemblers.
	 */
	private Optional<PlanNodeID> assembleTestPlanNodes(OpenBBTContext context) {
		PlanNodeRepository planNodeRepository = contextManager.getRepository(PlanNodeRepository.class);
		List<SuiteAssembler> assemblers = contextManager.getExtensions(SuiteAssembler.class).toList();
		if (assemblers.isEmpty()) {
			log.warn("No SuiteAssembler found, cannot assemble test plan");
			return Optional.empty();
		}
		List<PlanNodeID> nodes = new ArrayList<>();
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
		for (PlanNodeID nodeId : nodes) {
			planNodeRepository.attachChildNodeLast(rootID, nodeId);
		}
		return Optional.ofNullable(rootID);
	}

}
