package org.myjtools.openbbt.cli;

import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.util.Log;
import picocli.CommandLine;

@CommandLine.Command(
	name = "show-config",
	description = "Show the available configuration options and their current values"
)
public final class ShowConfigCommand extends AbstractCommand {

	private static final Log log = Log.of();


	@Override
	protected void execute() {
		log.debug("Showing configuration options...");
		OpenBBTContext context = getContext();
		OpenBBTRuntime cm = new OpenBBTRuntime(context.configuration());
		log.info(cm.configuration().toString());
		log.info("Available configuration options:\n");
		log.info(cm.configuration().getDefinitionsToString());

	}


}
