package org.myjtools.openbbt.core.cli;

import picocli.CommandLine;
import java.util.concurrent.Callable;

@CommandLine.Command(
	name = "install",
	description = "Install plugins required by the project"
)
public class InstallCommand implements Callable<Integer> {

	@CommandLine.ParentCommand
	MainCommand parent;

	@Override
	public Integer call() throws Exception {


		System.out.println("Installing plugins...");
		return 0;
	}


}
