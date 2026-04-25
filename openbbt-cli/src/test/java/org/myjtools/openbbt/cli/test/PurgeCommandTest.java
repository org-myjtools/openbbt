package org.myjtools.openbbt.cli.test;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.cli.MainCommand;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PurgeCommandTest {

	private static final String ENV_PATH = "target/.openbbt-purge";

	@Test
	void showHelp() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"purge", "--help",
			"-D"+ OpenBBTConfig.ENV_PATH+"="+ENV_PATH,
			"-f","src/test/resources/openbbt.yaml"
		);
		assertEquals(0, exitCode);
	}

	@Test
	void purgeTest() {
		int exitCode = new CommandLine(new MainCommand()).execute(
			"purge",
			"-D"+ OpenBBTConfig.ENV_PATH+"="+ENV_PATH,
			"-f","src/test/resources/openbbt.yaml"
		);
		assertEquals(0, exitCode);
	}


}
