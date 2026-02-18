package org.myjtools.openbbt.core.cli;

import com.google.common.io.Files;
import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.OpenBBTFile;
import picocli.CommandLine;
import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

public abstract sealed class AbstractCommand implements Callable<Integer> permits InstallCommand, PlanCommand, PurgeCommand {

	@CommandLine.ParentCommand
	MainCommand parent;

	protected abstract void execute();

	protected Config getConfig(OpenBBTContext context) {
		return context.configuration().append(Config.ofMap(parent.params)).append(Config.env());
	}

	protected OpenBBTContext getContext() {
		return readConfigurationFile().createContext(List.of(),"", Config.env());
	}

	protected OpenBBTFile readConfigurationFile() {
		try (var reader = Files.newReader(new File(parent.configurationFile), java.nio.charset.StandardCharsets.UTF_8)) {
		    return OpenBBTFile.read(reader);
		} catch (Exception e) {
			throw new OpenBBTException(
				e,
				"Failed to read configuration file: {}",
				e.getMessage()
			);
		}
	}

	@Override
	public Integer call() throws Exception {
		try {
			execute();
			return 0;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return 1;
		}
	}
}
