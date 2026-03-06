package org.myjtools.openbbt.it;

import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.openbbt.core.contributors.SuiteAssembler;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.testplan.*;
import java.util.Optional;
import java.util.UUID;

/**
 * Test-only SuiteAssembler that builds specific plan trees based on the suite name,
 * allowing validation tests to control the tree structure without needing real test resources.
 */
@Extension
public class TestTreeSuiteAssembler implements SuiteAssembler {

	@Inject
	TestPlanRepository repository;

	@Override
	public Optional<UUID> assembleSuite(TestSuite testSuite) {
		return switch (testSuite.name()) {
			case "validStep"                    -> suiteWithStep("a valid step");
			case "invalidStep"                  -> suiteWithStep("this step does not exist");
			case "testCaseNoSteps"              -> suiteWithTestCaseNoSteps();
			case "stepAggregatorNoStepChildren" -> suiteWithEmptyAggregator();
			default                             -> Optional.empty();
		};
	}

	// TEST_SUITE → TEST_FEATURE → TEST_CASE → STEP_AGGREGATOR → STEP(stepName)
	private Optional<UUID> suiteWithStep(String stepName) {
		UUID suite      = node(NodeType.TEST_SUITE,       "suite");
		UUID feature    = node(NodeType.TEST_FEATURE,     "feature");
		UUID testCase   = node(NodeType.TEST_CASE,        "test case");
		UUID aggregator = node(NodeType.STEP_AGGREGATOR,  "steps");
		UUID step       = node(NodeType.STEP,             stepName);

		repository.attachChildNodeLast(suite, feature);
		repository.attachChildNodeLast(feature, testCase);
		repository.attachChildNodeLast(testCase, aggregator);
		repository.attachChildNodeLast(aggregator, step);
		return Optional.of(suite);
	}

	// TEST_SUITE → TEST_FEATURE → TEST_CASE  (no steps at all)
	private Optional<UUID> suiteWithTestCaseNoSteps() {
		UUID suite    = node(NodeType.TEST_SUITE,   "suite");
		UUID feature  = node(NodeType.TEST_FEATURE, "feature");
		UUID testCase = node(NodeType.TEST_CASE,    "test case with no steps");

		repository.attachChildNodeLast(suite, feature);
		repository.attachChildNodeLast(feature, testCase);
		return Optional.of(suite);
	}

	// TEST_SUITE → TEST_FEATURE → TEST_CASE → STEP_AGGREGATOR  (aggregator has no STEP children)
	private Optional<UUID> suiteWithEmptyAggregator() {
		UUID suite      = node(NodeType.TEST_SUITE,      "suite");
		UUID feature    = node(NodeType.TEST_FEATURE,    "feature");
		UUID testCase   = node(NodeType.TEST_CASE,       "test case");
		UUID aggregator = node(NodeType.STEP_AGGREGATOR, "empty aggregator");

		repository.attachChildNodeLast(suite, feature);
		repository.attachChildNodeLast(feature, testCase);
		repository.attachChildNodeLast(testCase, aggregator);
		return Optional.of(suite);
	}

	private UUID node(NodeType type, String name) {
		return repository.persistNode(new TestPlanNode(type).name(name));
	}
}