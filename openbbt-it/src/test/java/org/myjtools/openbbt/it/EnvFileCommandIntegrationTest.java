package org.myjtools.openbbt.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.openbbt.cli.MainCommand;
import org.myjtools.openbbt.core.OpenBBTConfig;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnvFileCommandIntegrationTest {

	private static final String FIXTURE_DIR = "src/test/resources/env-cli";
	private static final String CONFIG_FILE = FIXTURE_DIR + "/openbbt.yaml";
	private static final String FORMAT_ENV_FIXTURE_DIR = "src/test/resources/env-format-cli";
	private static final String FORMAT_ENV_CONFIG_FILE = FORMAT_ENV_FIXTURE_DIR + "/openbbt.yaml";
	private static final String NO_ENV_FIXTURE_DIR = "src/test/resources/no-env-cli";
	private static final String NO_ENV_CONFIG_FILE = NO_ENV_FIXTURE_DIR + "/openbbt.yaml";
	private static final String ENV_LOADED_TAG = "env-loaded-definition-tag";
	private static final String ENV_SECRET_VALUE = "env-secret-value";
	private static final String PROCESS_ENV_KEY = "USER";
	private static final String ENV_FILE_PRECEDENCE_VALUE = "from-env-file";
	private static final String CLI_PRECEDENCE_VALUE = "from-cli";

	@Test
	void showConfig_readsSiblingEnvFileWithoutPrintingSecretsEvenInDebugMode() {
		var out = captureStdout(() -> new CommandLine(new MainCommand()).execute(
			args("show-config", "--debug")
		));

		assertThat(out.exitCode()).isZero();
		assertThat(out.text()).contains(ENV_LOADED_TAG);
		assertThat(out.text()).contains("core.artifacts.repository.password : <masked>");
		assertThat(out.text()).doesNotContain(ENV_SECRET_VALUE);
	}

	@Test
	void showConfig_envFileOverridesProcessEnvironment() {
		assertThat(System.getenv(PROCESS_ENV_KEY)).isNotBlank().isNotEqualTo(ENV_FILE_PRECEDENCE_VALUE);

		var out = captureStdout(() -> new CommandLine(new MainCommand()).execute(
			args("show-config")
		));

		assertThat(out.exitCode()).isZero();
		assertThat(out.text()).contains(PROCESS_ENV_KEY + " : " + ENV_FILE_PRECEDENCE_VALUE);
	}

	@Test
	void showConfig_cliOverridesEnvFile() {
		var out = captureStdout(() -> new CommandLine(new MainCommand()).execute(
			args("show-config", "-D" + PROCESS_ENV_KEY + "=" + CLI_PRECEDENCE_VALUE)
		));

		assertThat(out.exitCode()).isZero();
		assertThat(out.text()).contains(PROCESS_ENV_KEY + " : " + CLI_PRECEDENCE_VALUE);
	}

	@Test
	void showConfig_withoutSiblingEnv_usesDefaultAndDoesNotLeakOtherFixtureValues() {
		var out = captureOutput(() -> new CommandLine(new MainCommand()).execute(
			argsForConfig(NO_ENV_FIXTURE_DIR, NO_ENV_CONFIG_FILE, "show-config")
		));

		assertThat(out.exitCode()).isZero();
		assertThat(out.stdout()).contains("core.definitionTag : definition");
		assertThat(out.stdout()).doesNotContain(ENV_LOADED_TAG);
		assertThat(out.stdout()).doesNotContain(PROCESS_ENV_KEY + " : " + ENV_FILE_PRECEDENCE_VALUE);
	}

	@Test
	void showConfig_withUnreadableEnvFile_returnsClearError(@TempDir Path tempDir) throws Exception {
		Path configFile = tempDir.resolve("openbbt.yaml");
		Files.writeString(configFile, Files.readString(Path.of(CONFIG_FILE)), StandardCharsets.UTF_8);
		Files.createDirectory(tempDir.resolve(".env"));

		var out = captureOutput(() -> new CommandLine(new MainCommand()).execute(
			argsForConfig(tempDir.toString(), configFile.toString(), "show-config")
		));

		assertThat(out.exitCode()).isEqualTo(1);
		assertThat(out.stderr()).contains("Failed to read env file");
	}

	@Test
	void showConfig_parsesCommentsBlankLinesAndSpacesAroundEquals() {
		var out = captureOutput(() -> new CommandLine(new MainCommand()).execute(
			argsForConfig(FORMAT_ENV_FIXTURE_DIR, FORMAT_ENV_CONFIG_FILE, "show-config")
		));

		assertThat(out.exitCode()).isZero();
		assertThat(out.stdout()).contains("core.definitionTag : env-format-tag");
		assertThat(out.stdout()).contains(PROCESS_ENV_KEY + " : from-format-env");
	}

	private static String[] args(String... extra) {
		return argsForConfig(FIXTURE_DIR, CONFIG_FILE, extra);
	}

	private static String[] argsForConfig(String fixtureDir, String configFile, String... extra) {
		List<String> all = new ArrayList<>(Arrays.asList(extra));
		all.addAll(Arrays.asList(
			"-f", configFile,
			"-D" + OpenBBTConfig.RESOURCE_PATH + "=" + fixtureDir,
			"-D" + OpenBBTConfig.RESOURCE_FILTER + "=openbbt.yaml",
			"-D" + OpenBBTConfig.PERSISTENCE_MODE + "=" + OpenBBTConfig.PERSISTENCE_MODE_TRANSIENT
		));
		return all.toArray(String[]::new);
	}

	private record CapturedOutput(int exitCode, String stdout, String stderr) {
		String text() {
			return stdout;
		}
	}

	@FunctionalInterface
	private interface IntSupplier {
		int get();
	}

	private static CapturedOutput captureStdout(IntSupplier action) {
		CapturedOutput output = captureOutput(action);
		return new CapturedOutput(output.exitCode(), output.stdout(), output.stderr());
	}

	private static CapturedOutput captureOutput(IntSupplier action) {
		PrintStream originalOut = System.out;
		PrintStream originalErr = System.err;
		var stdoutBuffer = new ByteArrayOutputStream();
		var stderrBuffer = new ByteArrayOutputStream();
		var stdoutTee = new PrintStream(new java.io.OutputStream() {
			@Override
			public void write(int b) {
				stdoutBuffer.write(b);
				originalOut.write(b);
			}

			@Override
			public void write(byte[] b, int off, int len) {
				stdoutBuffer.write(b, off, len);
				originalOut.write(b, off, len);
			}
		});
		var stderrTee = new PrintStream(new java.io.OutputStream() {
			@Override
			public void write(int b) {
				stderrBuffer.write(b);
				originalErr.write(b);
			}

			@Override
			public void write(byte[] b, int off, int len) {
				stderrBuffer.write(b, off, len);
				originalErr.write(b, off, len);
			}
		});
		System.setOut(stdoutTee);
		System.setErr(stderrTee);
		try {
			int code = action.get();
			return new CapturedOutput(code, stdoutBuffer.toString(), stderrBuffer.toString());
		} finally {
			System.setOut(originalOut);
			System.setErr(originalErr);
			stdoutTee.close();
			stderrTee.close();
		}
	}
}
