package org.myjtools.openbbt.core.cli;

import org.myjtools.openbbt.core.OpenBBTContext;
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
		log.info(context.configuration().toString());
		log.info(context.configuration().getDefinitionsToString());

	}


}
