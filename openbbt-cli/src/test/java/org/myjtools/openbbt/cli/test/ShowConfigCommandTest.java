package org.myjtools.openbbt.cli.test;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.cli.MainCommand;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ShowConfigCommandTest {

	private static final String ENV_PATH = "target/.openbbt-show-config";

	@Test
	void showHelp() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"show-config", "--help",
			"-f","src/test/resources/openbbt.yaml",
			"-D"+OpenBBTConfig.ENV_PATH+"="+ENV_PATH
		);
		assertEquals(0, exitCode);
	}

	@Test
	void showConfig() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"show-config",
			"--debug",
			"-f","src/test/resources/openbbt.yaml",
			"-D"+OpenBBTConfig.ENV_PATH+"="+ENV_PATH,
			"-Dparam1=value1"
		);
		assertEquals(0, exitCode);
	}


}
