package org.myjtools.openbbt.core;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.maven.artifactstore.MavenArtifactStore;
import org.myjtools.jexten.plugin.PluginID;
import org.myjtools.jexten.plugin.PluginManager;
import org.myjtools.mavenfetcher.MavenFetcherProperties;
import org.myjtools.openbbt.core.util.Log;
import java.nio.file.Path;
import java.util.Properties;

public class Plugins {


	private static final Log log = Log.of();

	private final PluginManager pluginManager;
	private final MavenArtifactStore artifactStore;

	public Plugins(Config config) {

		Path envPath = config.get(OpenBBTConfig.ENV_PATH, Path::of).orElse(OpenBBTConfig.ENV_DEFAULT_PATH);
		Properties mavenFetcherProperties = computeMavenFetcherProperties(config, envPath);

		this.artifactStore = new MavenArtifactStore();
		this.artifactStore.configure(mavenFetcherProperties);

		this.pluginManager = new PluginManager(
			"org.myjtools.openbbt",
			getClass().getClassLoader(),
			envPath.resolve(OpenBBTConfig.PLUGINS_PATH)
		);
		this.pluginManager.setArtifactStore(new MavenArtifactStore());


	}


	public void installPlugin(String pluginName) {
		String normalizedPluginName = normalizePluginName(pluginName);
		String groupId = normalizedPluginName.substring(0, normalizedPluginName.indexOf(":"));
		String artifactId = normalizedPluginName.substring(normalizedPluginName.indexOf(":") + 1);
		PluginID pluginID = new PluginID(groupId, artifactId);
		if (pluginManager.plugins().contains(pluginID)) {
			log.info("Plugin {} is already installed.", normalizedPluginName);
			return;
		}
		pluginManager.installPluginFromArtifactStore(new PluginID(groupId, artifactId));
		log.info("Installing plugin {} from artifact store...", normalizedPluginName);
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



	private String normalizePluginName(String name) {
		int groupNameIndex = name.indexOf(":");
		if (groupNameIndex == -1) {
			return "org.myjtools.openbbt.plugins:" + name;
		}
		return name;
	}

}
