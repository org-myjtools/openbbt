package org.myjtools.openbbt.cli.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.cli.MainCommand;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTFile;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.plan.Plan;
import picocli.CommandLine;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PlanBuilderTest {

	@BeforeAll
	static void installPlugins() {
		new CommandLine(new MainCommand()).execute(
			"install",
			"-f", "src/test/resources/openbbt.yaml",
			"-D" + OpenBBTConfig.ENV_PATH + "=target/.openbbt"
		);
	}

	@Test
	void buildTestPlan_reusesExistingPlanOnSecondCall(@TempDir Path tempDir) throws IOException {
		OpenBBTFile file = OpenBBTFile.read(new FileReader("src/test/resources/openbbt.yaml"));
		var context = file.createContext(
			Config.ofMap(Map.of(
				OpenBBTConfig.ENV_PATH, "target/.openbbt",
				OpenBBTConfig.RESOURCE_PATH, "src/test/resources/test-features",
				OpenBBTConfig.PERSISTENCE_MODE, OpenBBTConfig.PERSISTENCE_MODE_FILE,
				OpenBBTConfig.PERSISTENCE_FILE, tempDir.resolve("plan.db").toString()
			)),
			List.of("suiteA"),
			"",
			Config.empty()
		);

		// Step 1: first call creates a new plan
		Plan plan1 = new OpenBBTRuntime(context.configuration()).buildTestPlan(context);
		assertThat(plan1.planID()).isNotNull();

		// Step 2: second call reuses the existing plan
		Plan plan2 = new OpenBBTRuntime(context.configuration()).buildTestPlan(context);
		assertThat(plan2.planID()).isEqualTo(plan1.planID());
	}

}