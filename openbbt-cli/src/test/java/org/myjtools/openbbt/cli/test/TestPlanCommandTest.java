package org.myjtools.openbbt.cli.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.cli.MainCommand;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TestPlanCommandTest {

	private static final String ENV_PATH = "target/.openbbt-testplan";

	@BeforeAll
	static void installPlugins() {
		new CommandLine(new MainCommand()).execute(
			"install",
			"-f", "src/test/resources/openbbt.yaml",
			"-D" + OpenBBTConfig.ENV_PATH + "=" + ENV_PATH
		);
	}

	@Test
	void showHelp() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"plan", "--help",
			"-f","src/test/resources/openbbt.yaml",
			"-D"+OpenBBTConfig.ENV_PATH+"="+ENV_PATH
		);
		assertEquals(0, exitCode);
	}

	@Test
	void assembleTestPlan() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"plan",
			"-f","src/test/resources/openbbt.yaml",
			"-D"+OpenBBTConfig.ENV_PATH+"="+ENV_PATH,
			"--suite", "suiteA",
			"-D"+OpenBBTConfig.PERSISTENCE_MODE+"="+OpenBBTConfig.PERSISTENCE_MODE_TRANSIENT,
			"-D"+OpenBBTConfig.RESOURCE_PATH+"=src/test/resources/test-features"
		);
		assertEquals(0, exitCode);
	}


	@Test
	void assembleTestPlanWithDetails() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"plan", "--detail",
			"-f","src/test/resources/openbbt.yaml",
			"-D"+OpenBBTConfig.ENV_PATH+"="+ENV_PATH,
			"--suite", "suiteA",
			"-D"+OpenBBTConfig.PERSISTENCE_MODE+"="+OpenBBTConfig.PERSISTENCE_MODE_TRANSIENT,
			"-D"+OpenBBTConfig.RESOURCE_PATH+"=src/test/resources/test-features"
		);
		assertEquals(0, exitCode);
	}


}
