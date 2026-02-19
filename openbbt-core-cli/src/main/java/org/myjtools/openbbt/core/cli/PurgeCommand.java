package org.myjtools.openbbt.core.cli;

import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.util.Log;
import picocli.CommandLine;
import java.nio.file.Path;

@CommandLine.Command(
	name = "purge",
	description = "Delete all local OpenBBT data"
)
public final class PurgeCommand extends AbstractCommand {

	private static final Log log = Log.of();


	@Override
	protected void execute() {

	log.info("Purging OpenBBT data...");
		OpenBBTContext context = getContext();
		Path envPath = context.configuration().get(OpenBBTConfig.ENV_PATH, Path::of).orElseThrow();
		Util.deleteDirectory(envPath);
	}




}
