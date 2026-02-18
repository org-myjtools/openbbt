package org.myjtools.openbbt.core.cli;

import picocli.CommandLine;

@CommandLine.Command(
	name = "openbbt",
	description = "OpenBBT Command Line Interface",
	subcommands = {
		CleanCommand.class,
		VersionCommand.class,
		CommandLine.HelpCommand.class
	}
)
public class OpenBBTCli implements Runnable {

	public static void main(String[] args) {
		int exitCode = new CommandLine(new OpenBBTCli()).execute(args);
		System.exit(exitCode);
	}

	@Override
	public void run() {
		new CommandLine(this).execute("help");
	}

}
