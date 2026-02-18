package org.myjtools.openbbt.core.cli.test;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.cli.MainCommand;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InstallCommandTest {

	@Test
	void installTest() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"install",
			"-f","src/test/resources/openbbt.yaml",
			"-D"+OpenBBTConfig.ENV_PATH+"=target/.openbbt"
		);
		assertEquals(0, exitCode);
	}

	@Test
	void installTestWithClean() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"install",
			"-D"+OpenBBTConfig.ENV_PATH+"=target/.openbbt",
			"-f","src/test/resources/openbbt.yaml",
			"--clean"
		);
		assertEquals(0, exitCode);
	}

}