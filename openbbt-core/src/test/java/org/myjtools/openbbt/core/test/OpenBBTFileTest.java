package org.myjtools.openbbt.core.test;

import com.google.common.io.Files;
import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTFile;
import org.myjtools.openbbt.core.plan.TagExpression;
import org.myjtools.openbbt.core.project.Project;
import org.myjtools.openbbt.core.project.TestSuite;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class OpenBBTFileTest {

	private final Config INPUT_ENV = Config.ofMap(Map.of(OpenBBTConfig.ENV_PATH, "target/.openbbt"));

	@Test
	void testReadFile() throws IOException {
		try (var reader = Files.newReader(new File("src/test/resources/openbbt.yaml"), StandardCharsets.UTF_8)) {
			var file = OpenBBTFile.read(reader);
			assertThat(file).isNotNull();
			assertThat(file.project()).extracting(Project::name).isEqualTo("My Project");
			assertThat(file.project().testSuites()).containsExactlyInAnyOrder(
				new TestSuite("suiteA", "Suite A", TagExpression.parse("A or Aa")),
				new TestSuite("suiteB", "Suite B", TagExpression.parse("B or Bb"))
			);
			assertThat(file.plugins()).containsExactlyInAnyOrder(
				"gherkin",
				"org.myjtools.openbbt.plugins:gherkin-openbbt-plugin",
				"org.myjtools.openbbt.plugins:gherkin-openbbt-plugin:1.0.0"
			);
		}
	}

	@Test
	void testCreateContext() throws IOException {
		try (var reader = Files.newReader(new File("src/test/resources/openbbt.yaml"), StandardCharsets.UTF_8)) {
			var file = OpenBBTFile.read(reader);
			Config env = Config.ofMap(Map.of(
				"ENV_VAR_ONE","value1",
				"param1","valueA"
			));
			var context = file.createContext(INPUT_ENV, List.of("suiteA"), "profileA", env);
			assertThat(context).isNotNull();
			assertThat(context.project()).extracting(Project::name).isEqualTo("My Project");
			assertThat(context.testSuites()).containsExactlyInAnyOrder(
				"suiteA"
			);
			System.out.println(context.configuration().toString());
			assertThat(context.configuration().getString("gherkin.gherkin-prop")).hasValue("value1");
			assertThat(context.configuration().getString("gherkin.gherkin-param")).hasValue("valueA");
			assertThat(context.configuration().getString("rest.rest-prop")).hasValue("A");
			assertThat(context.plugins()).containsExactlyInAnyOrder(
				"org.myjtools.openbbt.plugins:gherkin-openbbt-plugin",
				"org.myjtools.openbbt.plugins:gherkin-openbbt-plugin",
				"org.myjtools.openbbt.plugins:gherkin-openbbt-plugin:1.0.0"
			);
		}
	}

}
