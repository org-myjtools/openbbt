package org.myjtools.openbbt.plugins.gherkin.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.gherkinparser.DefaultKeywordMapProvider;
import org.myjtools.gherkinparser.GherkinParser;
import org.myjtools.junit5.memorycheck.MemoryExtension;
import org.myjtools.openbbt.core.persistence.PlanRepository;
import org.myjtools.openbbt.persistence.DataSourceProvider;
import org.myjtools.openbbt.persistence.plan.JooqPlanRepository;
import org.myjtools.openbbt.core.plan.TagExpression;
import org.myjtools.openbbt.plugins.gherkin.FeaturePlanAssembler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;


class FeaturePlanAssemblerMemoryTest {

	static void processFeatures(PlanRepository repository) throws IOException {
		GherkinParser parser = new GherkinParser(new DefaultKeywordMapProvider());
		try (var paths = Files.walk(Path.of("src/test/resources/test-memory"))) {
			for (Path path : paths.filter(p -> p.toString().endsWith(".feature")).toList()) {
				var gherkinDocument = parser.parse(Files.newInputStream(path));
				var feature = gherkinDocument.feature();
				FeaturePlanAssembler assembler = new FeaturePlanAssembler(
					feature,
					path.toString(),
					new DefaultKeywordMapProvider(),
					"ID-(\\w+)",
					repository,
					TagExpression.parse("")
				);
				assertThat(assembler.createTestPlan()).isPresent();
			}
		}
	}

	@Nested
	class FileDatabase {

		@RegisterExtension
		MemoryExtension memoryCheck = new MemoryExtension(20_000_000);

		@TempDir
		private Path tempDir;

		private PlanRepository repository;

		@BeforeEach
		void setUp() {
			repository = new JooqPlanRepository(DataSourceProvider.hsqldb(tempDir.resolve("testdb")));
		}

		@Test
		void test() throws IOException {
			processFeatures(repository);
		}
	}


}