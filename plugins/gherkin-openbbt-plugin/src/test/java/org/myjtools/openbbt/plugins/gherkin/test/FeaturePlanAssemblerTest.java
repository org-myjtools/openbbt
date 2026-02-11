package org.myjtools.openbbt.plugins.gherkin.test;

import org.junit.jupiter.api.Test;
import org.myjtools.gherkinparser.DefaultKeywordMapProvider;
import org.myjtools.gherkinparser.GherkinParser;
import org.myjtools.openbbt.core.PlanNodeRepository;
import org.myjtools.openbbt.core.PlanNodeRepositoryWriter;
import org.myjtools.openbbt.core.persistence.DataSourceProvider;
import org.myjtools.openbbt.core.persistence.JooqRepository;
import org.myjtools.openbbt.core.plan.PlanNodeID;
import org.myjtools.openbbt.plugins.gherkin.FeaturePlanAssembler;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class FeaturePlanAssemblerTest {

	@Test
	void testAssemble() throws IOException {

		PlanNodeRepository repository = new JooqRepository(DataSourceProvider.hsqldb());
		var writer = new PlanNodeRepositoryWriter(repository);

		GherkinParser parser = new GherkinParser(new DefaultKeywordMapProvider());
		var gherkinDocument = parser.parse(Files.newInputStream(Path.of("src/test/resources/simpleScenario.feature")));
		var feature = gherkinDocument.feature();

		FeaturePlanAssembler assembler = new FeaturePlanAssembler(
			feature,
			"/path/to/feature",
			new DefaultKeywordMapProvider(),
			"ID-(\\w+)",
			repository
		);

		PlanNodeID testPlan = assembler.createTestPlan();
		StringWriter output = new StringWriter();
		writer.write(testPlan, output);

		assertThat(output).hasToString("""
			[TEST_AGGREGATOR] Test 1 - Simple Scenario
			  [TEST_CASE] (Test1_Scenario1) Test Scenario
			    [STEP_AGGREGATOR] Background
			      [STEP] the set of real numbers
			    [STEP] a number with value 8.02 and another number with value 9
			    [STEP] both numbers are multiplied
			    [STEP] the matchResult is equals to 72.18
			  [TEST_AGGREGATOR] (Test1_ScenarioOutline) Test Scenario Outline
			    [TEST_CASE] (Test1_ScenarioOutline_1) Test Scenario Outline [1]
			      [STEP_AGGREGATOR] Background
			        [STEP] the set of real numbers
			      [STEP] a number with value 8.02 and another number with value 9
			      [STEP] both numbers are multiplied
			      [STEP] the matchResult is equals to 72.18
			    [TEST_CASE] (Test1_ScenarioOutline_2) Test Scenario Outline [2]
			      [STEP_AGGREGATOR] Background
			        [STEP] the set of real numbers
			      [STEP] a number with value 5 and another number with value 4
			      [STEP] both numbers are multiplied
			      [STEP] the matchResult is equals to 20
			""");

	}
}
