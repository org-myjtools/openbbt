package org.myjtools.openbbt.it;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.cli.MainCommand;
import org.myjtools.openbbt.core.OpenBBTConfig;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnvFileCommandIntegrationTest {

	private static final String FIXTURE_DIR = "src/test/resources/env-cli";
	private static final String CONFIG_FILE = FIXTURE_DIR + "/openbbt.yaml";
	private static final String ENV_LOADED_TAG = "env-loaded-definition-tag";

	@Test
	void showConfig_readsSiblingEnvFileAndMergesItsValues() {
		var out = captureStdout(() -> new CommandLine(new MainCommand()).execute(
			args("show-config")
		));

		assertThat(out.exitCode()).isZero();
		assertThat(out.text()).contains(ENV_LOADED_TAG);
	}

	private static String[] args(String... extra) {
		List<String> all = new ArrayList<>(Arrays.asList(extra));
		all.addAll(Arrays.asList(
			"-f", CONFIG_FILE,
			"-D" + OpenBBTConfig.RESOURCE_PATH + "=" + FIXTURE_DIR,
			"-D" + OpenBBTConfig.RESOURCE_FILTER + "=openbbt.yaml",
			"-D" + OpenBBTConfig.PERSISTENCE_MODE + "=" + OpenBBTConfig.PERSISTENCE_MODE_TRANSIENT
		));
		return all.toArray(String[]::new);
	}

	private record CapturedOutput(int exitCode, String text) {}

	@FunctionalInterface
	private interface IntSupplier {
		int get();
	}

	private static CapturedOutput captureStdout(IntSupplier action) {
		PrintStream original = System.out;
		var buffer = new ByteArrayOutputStream();
		var tee = new PrintStream(new java.io.OutputStream() {
			@Override
			public void write(int b) {
				buffer.write(b);
				original.write(b);
			}

			@Override
			public void write(byte[] b, int off, int len) {
				buffer.write(b, off, len);
				original.write(b, off, len);
			}
		});
		System.setOut(tee);
		try {
			int code = action.get();
			return new CapturedOutput(code, buffer.toString());
		} finally {
			System.setOut(original);
			tee.close();
		}
	}
}
