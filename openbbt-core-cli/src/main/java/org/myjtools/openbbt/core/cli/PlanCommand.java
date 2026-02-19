package org.myjtools.openbbt.core.cli;

import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.util.Log;
import picocli.CommandLine;

@CommandLine.Command(
	name = "plan",
	description = "Analyze the test plan"
)
public final class PlanCommand extends AbstractCommand {

	private static final Log log = Log.of();

	@CommandLine.Option(
		names = {"--detail"},
		description = "Show detailed analysis of the test plan",
		defaultValue = "false"
	)
	boolean detail;


	@Override
	protected void execute() {

		OpenBBTContext context = getContext();



	}


}
