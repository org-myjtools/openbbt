package org.myjtools.openbbt.core.cli;

import picocli.CommandLine;

@CommandLine.Command(
	name = "openbbt",
	description = "OpenBBT CLI utility",
	subcommands = {
		CleanCommand.class,
		VersionCommand.class
	}
)
public class MainCommand implements Runnable {

	@CommandLine.Option(names = {"-f", "--file"}, description = "Configuration file", scope = CommandLine.ScopeType.INHERIT)
	boolean configurationFile;

	@CommandLine.Option(names = {"-d", "--debug"}, description = "Enable debug mode", scope = CommandLine.ScopeType.INHERIT)
	boolean debugMode;


	public static void main(String[] args) {
		int exitCode = new CommandLine(new MainCommand()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public void run() {
		new CommandLine(this).usage(System.out);
	}

}
