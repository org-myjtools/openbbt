package org.myjtools.openbbt.core.cli.test;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.cli.MainCommand;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PurgeCommandTest {

	@Test
	void showHelp() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"purge", "--help",
			"-D"+ OpenBBTConfig.ENV_PATH+"=target/.openbbt",
			"-f","src/test/resources/openbbt.yaml"
		);
		assertEquals(0, exitCode);
	}

	@Test
	void purgeTest() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"purge",
			"-D"+ OpenBBTConfig.ENV_PATH+"=target/.openbbt",
			"-f","src/test/resources/openbbt.yaml"
		);
		assertEquals(0, exitCode);
	}


}