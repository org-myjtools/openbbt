package org.myjtools.openbbt.plugins.gherkin.test;

import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.*;
import org.myjtools.openbbt.core.contributors.SuiteAssembler;
import org.myjtools.openbbt.core.plan.PlanNodeID;
import org.myjtools.openbbt.core.plan.TagExpression;
import org.myjtools.openbbt.core.project.TestSuite;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class GherkinSuiteAssemblerTest {

	@Test
	void testEmptySuite() throws IOException {
		OpenBBTDependencyInjection di = new OpenBBTDependencyInjection(Config.ofMap(Map.of(
			OpenBBTConfig.PATH, "src/test/resources/test-empty-suite",
			OpenBBTConfig.REPOSITORY_MODE, OpenBBTConfig.REPOSITORY_MODE_MEMORY
		)));
		var planAssembler = di.getExtensions(SuiteAssembler.class).findFirst().orElseThrow();
		TestSuite testSuite = new TestSuite("Test Suite", "Test Suite", TagExpression.EMPTY);
		assertThat(planAssembler.assembleSuite(testSuite)).isEmpty();
	}

	@Test
	void testSingleFeatureSuite() throws IOException {
		String string = assembleSuite("src/test/resources/test-single-feature");
		assertThat(string).hasToString("""
				[TEST_SUITE] Test Suite
				  [TEST_AGGREGATOR] Test 1 - Simple Scenario
				    [TEST_CASE] (Test1_Scenario1) Test Scenario
				      [STEP_AGGREGATOR] Background
				        [STEP] the set of real numbers
				      [STEP] a number with value 8.02 and another number with value 9
				      [STEP] both numbers are multiplied
				      [STEP] the matchResult is equals to 72.18
				""");
	}


	@Test
	void testComplexTestSuite() throws IOException {
		String string = assembleSuite("src/test/resources/test-features");
		assertThat(string).hasToString("""
				[TEST_SUITE] Test Suite
				  [TEST_AGGREGATOR] Test - Scenario with arguments
				    [TEST_CASE] Test Scenario with document
				      [STEP] a number with value 8.02 and another number with value 9
				      [STEP] both numbers are multiplied
				      [STEP] the matchResult is equals to:
				    [TEST_CASE] Test Scenario with data table
				      [STEP] a number with value 8.02 and another number with value 9
				      [STEP] both numbers are multiplied
				      [STEP] the matchResult is equals to:
				  [TEST_AGGREGATOR] Test 3 - Backgrounds
				    [TEST_CASE] (Test3_Scenario1) Test Scenario 1
				      [STEP_AGGREGATOR] Background
				        [STEP] the set of real numbers ℝ
				      [STEP] a number with value 8.02 and another number with value 9
				      [STEP] both numbers are multiplied
				      [STEP] the matchResult is equals to 72.18
				    [TEST_CASE] (Test3_Scenario2) Test Scenario 2
				      [STEP_AGGREGATOR] Background
				        [STEP] the set of real numbers ℝ
				      [STEP] a number with value 7.02 and another number with value 8
				      [STEP] both numbers are multiplied
				      [STEP] the matchResult is equals to 56.16
				    [TEST_CASE] (Test3_Scenario3) Test Scenario 3
				      [STEP_AGGREGATOR] Background
				        [STEP] the set of real numbers ℝ
				      [STEP] a number with value 8.09 and another number with value 9
				      [STEP] both numbers are multiplied
				      [STEP] the matchResult is equals to 72.81
				  [TEST_AGGREGATOR] Test 2 - Scenario Outline
				    [TEST_AGGREGATOR] (ScenarioOutline1) Test Scenario Outline
				      [TEST_CASE] (ScenarioOutline1_1) Test Scenario Outline [1]
				        [STEP] a number with value 1.0 and another number with value 2
				        [STEP] both numbers are multiplied
				        [STEP] the matchResult is equals to 2.0
				      [TEST_CASE] (ScenarioOutline1_2) Test Scenario Outline [2]
				        [STEP] a number with value 2.0 and another number with value 3
				        [STEP] both numbers are multiplied
				        [STEP] the matchResult is equals to 6.0
				      [TEST_CASE] (ScenarioOutline1_3) Test Scenario Outline [3]
				        [STEP] a number with value 5.0 and another number with value 4
				        [STEP] both numbers are multiplied
				        [STEP] the matchResult is equals to 20.0
				  [TEST_AGGREGATOR] Test 1 - Simple Scenario
				    [TEST_CASE] (Test1_Scenario1) Test Scenario
				      [STEP] a number with value 8.02 and another number with value 9
				      [STEP] both numbers are multiplied
				      [STEP] the matchResult is equals to 72.18
				""");
	}


	private String assembleSuite(String path) throws IOException {
		OpenBBTDependencyInjection di = new OpenBBTDependencyInjection(Config.ofMap(Map.of(
				OpenBBTConfig.PATH, path,
				OpenBBTConfig.REPOSITORY_MODE, OpenBBTConfig.REPOSITORY_MODE_MEMORY
		)));
		var planAssembler = di.getExtensions(SuiteAssembler.class).findFirst().orElseThrow();
		TestSuite testSuite = new TestSuite("Test Suite", "Test Suite", TagExpression.EMPTY);
		PlanNodeID planID = planAssembler.assembleSuite(testSuite).orElseThrow();
		var repository = di.getPlanNodeRepository();
		PlanNodeRepositoryWriter writer = new PlanNodeRepositoryWriter(repository);
		Writer string = new StringWriter();
		writer.write(planID, string);
		return string.toString();
	}

}
