package org.myjtools.openbbt.cli;

import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.util.Log;
import picocli.CommandLine;
import java.io.IOException;
import java.nio.file.Files;
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
		Path envPath = context.configuration().get(OpenBBTConfig.ENV_PATH, Path::of).orElse(OpenBBTConfig.ENV_DEFAULT_PATH);
		if (envPath.toFile().exists()) {
			try (var stream = Files.list(envPath)) {
				stream.forEach(Util::deleteDirectory);
			} catch (IOException e) {
				log.error(e, "Failed to purge OpenBBT data: {}", e.getMessage());
			}
		}
	}




}
