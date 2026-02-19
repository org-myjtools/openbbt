package org.myjtools.openbbt.core.cli.test;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.cli.MainCommand;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ShowConfigCommandTest {

	@Test
	void showHelp() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"show-config", "--help",
			"-f","src/test/resources/openbbt.yaml",
			"-D"+OpenBBTConfig.ENV_PATH+"=target/.openbbt"
		);
		assertEquals(0, exitCode);
	}

	@Test
	void showConfig() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"show-config",
			"--debug",
			"-f","src/test/resources/openbbt.yaml",
			"-D"+OpenBBTConfig.ENV_PATH+"=target/.openbbt",
			"-Dparam1=value1"
		);
		assertEquals(0, exitCode);
	}


}