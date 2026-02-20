package org.myjtools.openbbt.core.cli;

import com.google.common.io.Files;
import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.OpenBBTFile;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract sealed class AbstractCommand implements Callable<Integer> permits InstallCommand, PlanCommand, PurgeCommand, ShowConfigCommand {

	@CommandLine.ParentCommand
	MainCommand parent;

	protected abstract void execute();


	protected OpenBBTContext getContext() {
		Map<String, String> params = parent.params == null ? Map.of() : parent.params;
		return readConfigurationFile().createContext(
			Config.ofMap(params),
			parent.suites == null ? List.of() : parent.suites,
			parent.profile,
			Config.ofMap(params).append(Config.env())
		);
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
		if (parent.debugMode) {
			try {
				var cl = Thread.currentThread().getContextClassLoader();
				var levelClass = Class.forName("ch.qos.logback.classic.Level", true, cl);
				var debugLevel = levelClass.getField("DEBUG").get(null);
				// Set debug for org.myjtools (includes jexten and openbbt)
				var root = LoggerFactory.getLogger("org.myjtools");
				root.getClass().getMethod("setLevel", levelClass).invoke(root, debugLevel);
			} catch (ReflectiveOperationException ignored) {}
		}
		try {
			if (parent.showHelp) {
				CommandLine.usage(this, System.out);
				return 0;
			}
			execute();
			return 0;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace(System.err);
			return 1;
		}
	}
}
