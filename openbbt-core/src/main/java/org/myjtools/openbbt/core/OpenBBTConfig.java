package org.myjtools.openbbt.core;

import java.nio.file.Path;

/**
 * @author Luis Iñesta Gelabert - luiinge@gmail.com
 */
public class OpenBBTConfig {

	public static final String RESOURCE_PATH = "resourcePath";
	public static final String ENV_PATH = "environmentPath";

	public static final String PERSISTENCE_MODE = "persistence.mode";
	public static final String PERSISTENCE_MODE_MEMORY = "in-memory";
	public static final String PERSISTENCE_MODE_FILE = "file";
	public static final String PERSISTENCE_MODE_REMOTE = "remote";
	public static final String PERSISTENCE_FILE = "persistence.file";
	public static final String PERSISTENCE_DB_URL = "persistence.db.url";
	public static final String PERSISTENCE_DB_USERNAME = "persistence.db.username";
	public static final String PERSISTENCE_DB_PASSWORD = "persistence.db.password";

	public static final String ARTIFACTS_LOCAL_REPOSITORY = "artifacts.local.repository";
	public static final String ARTIFACTS_REPOSITORY_URL = "artifacts.repository.url";
	public static final String ARTIFACTS_REPOSITORY_USERNAME = "artifacts.repository.username";
	public static final String ARTIFACTS_REPOSITORY_PASSWORD = "artifacts.repository.password";
	public static final String ARTIFACTS_REPOSITORY_PROXY_URL = "artifacts.repository.proxy.url";
	public static final String ARTIFACTS_REPOSITORY_PROXY_USERNAME= "artifacts.repository.proxy.host";
	public static final String ARTIFACTS_REPOSITORY_PROXY_PASSWORD= "artifacts.repository.proxy.host";

	public static final Path ENV_DEFAULT_PATH = Path.of(".openbbt");
	public static final Path PLUGINS_PATH = Path.of("plugins");
	public static final Path ARTIFACTS_PATH = Path.of("artifacts");
	public static final Path LOGS_PATH = Path.of("logs");
	public static final Path DATA_PATH = Path.of("data");

}
