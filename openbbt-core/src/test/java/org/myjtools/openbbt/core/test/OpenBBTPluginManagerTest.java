package org.myjtools.openbbt.core.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTPluginManager;
import java.nio.file.Path;
import java.util.Map;

class OpenBBTPluginManagerTest {

	@TempDir
	Path tempDir;

	@Test
	void loadPlugins() {
		OpenBBTPluginManager pluginManager = new OpenBBTPluginManager(Config.ofMap(Map.of(
			OpenBBTConfig.ENV_PATH, tempDir.toAbsolutePath()
		)));
		pluginManager.installPlugin("plugin");
	}
}
