package org.myjtools.openbbt.core.cli;

import picocli.CommandLine;
import java.util.List;
import java.util.Map;

@CommandLine.Command(
	name = "openbbt",
	description = "OpenBBT CLI utility",
	subcommands = {
		InstallCommand.class,
		VersionCommand.class,
		PurgeCommand.class,
		PlanCommand.class,
		ShowConfigCommand.class
	}
)
public class MainCommand implements Runnable {

	@CommandLine.Option(
		names = {"-f", "--file"},
		description = "Configuration file",
		scope = CommandLine.ScopeType.INHERIT,
		defaultValue = "openbbt.yaml"
	)
	String configurationFile;

	@CommandLine.Option(
		names = {"-d", "--debug"},
		description = "Enable debug mode",
		scope = CommandLine.ScopeType.INHERIT,
		defaultValue = "false"
	)
	boolean debugMode;

	@CommandLine.Option(
		names = {"-s", "--suite"},
		description = "Test suite names",
		scope = CommandLine.ScopeType.INHERIT
	)
	List<String> suites;

	@CommandLine.Option(
		names = "-D",
		description = "Parameters in key=value format",
		scope = CommandLine.ScopeType.INHERIT
	)
	Map<String, String> params;

	@CommandLine.Option(
		names = {"-p","--profile"},
		description = "Profile name",
		scope = CommandLine.ScopeType.INHERIT,
		defaultValue = ""
	)
	String profile;


	@CommandLine.Option(
		names = {"--help"},
		description = "Show command help",
		scope = CommandLine.ScopeType.INHERIT
	)
	boolean showHelp;


	public static void main(String[] args) {
		int exitCode = new CommandLine(new MainCommand()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public void run() {
		new CommandLine(this).usage(System.out);
	}

}
