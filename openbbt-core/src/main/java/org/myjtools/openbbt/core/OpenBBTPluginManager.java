package org.myjtools.openbbt.core;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Version;
import org.myjtools.jexten.maven.artifactstore.MavenArtifactStore;
import org.myjtools.jexten.plugin.PluginID;
import org.myjtools.jexten.plugin.PluginManager;
import org.myjtools.mavenfetcher.MavenFetcherProperties;
import org.myjtools.openbbt.core.util.Log;
import java.nio.file.Path;
import java.util.Properties;

public class OpenBBTPluginManager {


	private static final Log log = Log.of();

	private final PluginManager pluginManager;

	public OpenBBTPluginManager(Config config) {

		Path envPath = config.get(OpenBBTConfig.ENV_PATH, Path::of).orElse(OpenBBTConfig.ENV_DEFAULT_PATH);
		this.pluginManager = new PluginManager(
			"org.myjtools.openbbt",
			getClass().getClassLoader(),
			envPath.resolve(OpenBBTConfig.PLUGINS_PATH)
		);
		Properties mavenFetcherProperties = computeMavenFetcherProperties(config, envPath);
		this.pluginManager.setArtifactStore(new MavenArtifactStore().configure(mavenFetcherProperties));
	}


	public boolean installPlugin(String pluginName) {
		if (!pluginName.contains(":")) {
			pluginName = "org.myjtools.openbbt.plugins:" + pluginName;
		}
		String[] parts = pluginName.split(":");
		String groupId = parts[0];
		String artifactId = parts[1];
		Version version = (parts.length > 2) ? Version.of(parts[2]) : null;
		PluginID pluginID = new PluginID(groupId, artifactId);
		if (pluginManager.plugins().contains(pluginID)) {
			log.info("Plugin {} is already installed.", pluginName);
			return true;
		}
		log.info("Installing plugin {} from artifact store...", pluginName);
		try {
			pluginManager.installPluginFromArtifactStore(pluginID, version);
			log.info("Plugin {} installed successfully.", pluginName);
			return true;
		} catch (Exception e) {
			log.error(e,"Failed to resolve plugin {}", pluginName);
			return false;
		}
	}



	private static Properties computeMavenFetcherProperties(Config config, Path envPath) {
		Properties artifactStoreProperties = new Properties();
		artifactStoreProperties.setProperty(
			MavenFetcherProperties.LOCAL_REPOSITORY,
			envPath.resolve(OpenBBTConfig.ARTIFACTS_PATH).toAbsolutePath().toString()
		);
		config.getString(OpenBBTConfig.ARTIFACTS_REPOSITORY_PROXY_URL).ifPresent(
			url -> artifactStoreProperties.setProperty(MavenFetcherProperties.PROXY_URL, url)
		);
		config.getString(OpenBBTConfig.ARTIFACTS_REPOSITORY_PROXY_USERNAME).ifPresent(
			username -> artifactStoreProperties.setProperty(MavenFetcherProperties.PROXY_USERNAME, username)
		);
		config.getString(OpenBBTConfig.ARTIFACTS_REPOSITORY_PROXY_PASSWORD).ifPresent(
			password -> artifactStoreProperties.setProperty(MavenFetcherProperties.PROXY_PASSWORD, password)
		);
		config.getString(OpenBBTConfig.ARTIFACTS_REPOSITORY_URL).ifPresent(url -> {
			String username = config.getString(OpenBBTConfig.ARTIFACTS_REPOSITORY_USERNAME).orElse(null);
			String password = config.getString(OpenBBTConfig.ARTIFACTS_REPOSITORY_PASSWORD).orElse(null);
			String urlWithCredentials = "remote=" + url;
			if (username != null && password != null) {
				urlWithCredentials = urlWithCredentials + "[" + username + ":" + password + "]";
			}
			artifactStoreProperties.setProperty(MavenFetcherProperties.REMOTE_REPOSITORIES, urlWithCredentials);
			artifactStoreProperties.setProperty(MavenFetcherProperties.USE_DEFAULT_REMOTE_REPOSITORY, "false");
		});
		return artifactStoreProperties;
	}



}
