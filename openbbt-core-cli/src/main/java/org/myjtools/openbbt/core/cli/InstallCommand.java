package org.myjtools.openbbt.core.cli;

import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.OpenBBTPluginManager;
import org.myjtools.openbbt.core.util.Log;
import picocli.CommandLine;
import java.nio.file.Path;

@CommandLine.Command(
	name = "install",
	description = "Install plugins required by the project"
)
public final class InstallCommand extends AbstractCommand {

	private static final Log log = Log.of();

	@CommandLine.Option(
		names = {"-c", "--clean"},
		description = "Clean existing plugins before installation",
		defaultValue = "false"
	)
	boolean clean;


	@Override
	protected void execute() {

		OpenBBTContext context = getContext();

		Config config = getConfig(context);

		if (clean) {
			Path envPath = config.get(OpenBBTConfig.ENV_PATH, Path::of).orElseThrow();
			Path pluginsPath = envPath.resolve(OpenBBTConfig.PLUGINS_PATH);
			Util.deleteDirectory(pluginsPath);
			log.info("Existing plugins cleaned.");
		}


		if (context.plugins().isEmpty()) {
			log.info("Nothing to install");
			return;
		}

		OpenBBTPluginManager pluginManager = new OpenBBTPluginManager(config);
		for (String plugin : context.plugins()) {
			try {
				boolean result = pluginManager.installPlugin(plugin);
				if (!result) {
					throw new OpenBBTException("Failed to install plugin {}",plugin);
				}
			} catch (Exception e) {
				log.error(e,"Failed to install plugin {}", plugin);
			}
		}
	}


}
