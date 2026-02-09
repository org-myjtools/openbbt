package org.myjtools.openbbt.plugins.gherkin.test;

import org.junit.jupiter.api.Test;
import org.myjtools.gherkinparser.DefaultKeywordMapProvider;
import org.myjtools.gherkinparser.GherkinParser;
import org.myjtools.openbbt.core.PlanNodeRepository;
import org.myjtools.openbbt.core.persistence.DataSourceProvider;
import org.myjtools.openbbt.core.persistence.JooqRepository;
import org.myjtools.openbbt.core.plan.PlanNodeID;
import org.myjtools.openbbt.plugins.gherkin.FeaturePlanAssembler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class FeaturePlanAssemblerTest {

	@Test
	void testAssemble() throws IOException {

		PlanNodeRepository repository = new JooqRepository(DataSourceProvider.hsqldb());

		GherkinParser parser = new GherkinParser(new DefaultKeywordMapProvider());
		var gherkinDocument = parser.parse(Files.newInputStream(Path.of("src/test/resources/simpleScenario.feature")));
		var feature = gherkinDocument.feature();

		FeaturePlanAssembler assembler = new FeaturePlanAssembler(
			feature,
			"/path/to/feature",
			new DefaultKeywordMapProvider(),
			"ID-(\\d+)",
			repository
		);

		PlanNodeID testPlan = assembler.createTestPlan();
		assertThat(testPlan).isNotNull();

	}
}
