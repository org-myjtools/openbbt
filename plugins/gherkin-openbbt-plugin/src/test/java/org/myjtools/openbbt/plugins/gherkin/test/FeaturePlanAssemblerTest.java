package org.myjtools.openbbt.plugins.gherkin.test;

import org.junit.jupiter.api.Test;
import org.myjtools.gherkinparser.DefaultKeywordMapProvider;
import org.myjtools.gherkinparser.GherkinParser;
import org.myjtools.openbbt.core.persistence.PlanRepository;
import org.myjtools.openbbt.core.persistence.PlanRepositoryWriter;
import org.myjtools.openbbt.persistence.DataSourceProvider;
import org.myjtools.openbbt.persistence.plan.JooqPlanRepository;
import org.myjtools.openbbt.core.plan.PlanNodeID;
import org.myjtools.openbbt.core.plan.TagExpression;
import org.myjtools.openbbt.plugins.gherkin.FeaturePlanAssembler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

class FeaturePlanAssemblerTest {

	@Test
	void testAssemble() throws IOException {

		PlanRepository repository = new JooqPlanRepository(DataSourceProvider.hsqldb());
		var writer = new PlanRepositoryWriter(repository);
		Optional<PlanNodeID> testPlan = assembleFeature(repository,"");
		assertThat(testPlan).isPresent();
		StringBuilder output = new StringBuilder();
		writer.write(testPlan.orElseThrow(), output::append);

		assertThat(output).hasToString("""
			[TEST_FEATURE] Test 1 - Simple Scenario
			  [TEST_CASE] (Test1_Scenario1) Test Scenario
			    [STEP_AGGREGATOR] Background
			      [STEP] Given the set of real numbers
			    [STEP] Given a number with value 8.02 and another number with value 9
			    [STEP] When both numbers are multiplied
			    [STEP] Then the matchResult is equals to 72.18
			  [TEST_FEATURE] (Test1_ScenarioOutline) Test Scenario Outline
			    [TEST_CASE] (Test1_ScenarioOutline_1) Test Scenario Outline [1]
			      [STEP_AGGREGATOR] Background
			        [STEP] Given the set of real numbers
			      [STEP] Given a number with value 8.02 and another number with value 9
			      [STEP] When both numbers are multiplied
			      [STEP] Then the matchResult is equals to 72.18
			    [TEST_CASE] (Test1_ScenarioOutline_2) Test Scenario Outline [2]
			      [STEP_AGGREGATOR] Background
			        [STEP] Given the set of real numbers
			      [STEP] Given a number with value 5 and another number with value 4
			      [STEP] When both numbers are multiplied
			      [STEP] Then the matchResult is equals to 20
			""");

	}

	@Test
	void testAssembleScenarioWithTags() throws IOException {

		PlanRepository repository = new JooqPlanRepository(DataSourceProvider.hsqldb());
		var writer = new PlanRepositoryWriter(repository);
		Optional<PlanNodeID> testPlan = assembleFeature(repository,"ScenarioA");
		assertThat(testPlan).isPresent();
		StringBuilder output = new StringBuilder();
		writer.write(testPlan.orElseThrow(), output::append);

		assertThat(output).hasToString("""
			[TEST_FEATURE] Test 1 - Simple Scenario
			  [TEST_CASE] (Test1_Scenario1) Test Scenario
			    [STEP_AGGREGATOR] Background
			      [STEP] Given the set of real numbers
			    [STEP] Given a number with value 8.02 and another number with value 9
			    [STEP] When both numbers are multiplied
			    [STEP] Then the matchResult is equals to 72.18
			""");

	}


	@Test
	void testAssembleScenarioOutlineWithTags() throws IOException {

		PlanRepository repository = new JooqPlanRepository(DataSourceProvider.hsqldb());
		var writer = new PlanRepositoryWriter(repository);
		Optional<PlanNodeID> testPlan = assembleFeature(repository,"ScenarioB");
		assertThat(testPlan).isPresent();
		StringBuilder output = new StringBuilder();
		writer.write(testPlan.orElseThrow(), output::append);

		assertThat(output).hasToString("""
			[TEST_FEATURE] Test 1 - Simple Scenario
			  [TEST_FEATURE] (Test1_ScenarioOutline) Test Scenario Outline
			    [TEST_CASE] (Test1_ScenarioOutline_1) Test Scenario Outline [1]
			      [STEP_AGGREGATOR] Background
			        [STEP] Given the set of real numbers
			      [STEP] Given a number with value 8.02 and another number with value 9
			      [STEP] When both numbers are multiplied
			      [STEP] Then the matchResult is equals to 72.18
			    [TEST_CASE] (Test1_ScenarioOutline_2) Test Scenario Outline [2]
			      [STEP_AGGREGATOR] Background
			        [STEP] Given the set of real numbers
			      [STEP] Given a number with value 5 and another number with value 4
			      [STEP] When both numbers are multiplied
			      [STEP] Then the matchResult is equals to 20
			""");

	}

	@Test
	void testAssembleScenarioWithInvalidTags() throws IOException {
		PlanRepository repository = new JooqPlanRepository(DataSourceProvider.hsqldb());
		Optional<PlanNodeID> testPlan = assembleFeature(repository,"InvalidTag");
		assertThat(testPlan).isEmpty();
	}


	private Optional<PlanNodeID> assembleFeature (PlanRepository planNodeRepository, String tagExpression) throws IOException {
		GherkinParser parser = new GherkinParser(new DefaultKeywordMapProvider());
		var gherkinDocument = parser.parse(Files.newInputStream(Path.of("src/test/resources/test1/simpleScenario.feature")));
		var feature = gherkinDocument.feature();

		FeaturePlanAssembler assembler = new FeaturePlanAssembler(
			feature,
			"/path/to/feature",
			new DefaultKeywordMapProvider(),
			"ID-(\\w+)",
			planNodeRepository,
			TagExpression.parse(tagExpression)
		);

		return assembler.createTestPlan();
	}

}
