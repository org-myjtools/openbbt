package org.myjtools.openbbt.persistence;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.contributors.RepositoryFactory;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.persistence.Repository;
import org.myjtools.openbbt.persistence.plan.JooqPlanRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import static org.myjtools.openbbt.core.OpenBBTConfig.*;

@Extension
public class JooqRepositoryFactory implements RepositoryFactory {

	@Inject
	Config config;


	@Override
	@SuppressWarnings("unchecked")
	public <T extends Repository> T createRepository(Class<T> type) {
		String mode = config.get(PERSISTENCE_MODE,String.class).orElse(PERSISTENCE_MODE_TRANSIENT);
		switch (mode) {
			case PERSISTENCE_MODE_TRANSIENT -> {
				try {
					return (T) createFileRepository(type, Files.createTempFile("openbbt", "db"));
				} catch (IOException e) {
					throw new OpenBBTException(e);
				}
			}
			case PERSISTENCE_MODE_FILE -> {
				Path envPath = config.get(ENV_PATH, Path::of).orElseThrow(
						() -> new OpenBBTException("Repository environment path not configured {}: ", ENV_PATH)
				);
				Path filePath = config.get(PERSISTENCE_FILE, Path::of).orElseThrow(
						() -> new OpenBBTException("Repository file path not configured {}: ", PERSISTENCE_FILE)
				);
				return (T) createFileRepository(type,envPath.resolve(filePath));
			}
			case PERSISTENCE_MODE_REMOTE -> {
				String url = config.get(PERSISTENCE_DB_URL, String::toString).orElseThrow(
						() -> new OpenBBTException("Repository remote URL not configured {}: ", PERSISTENCE_DB_URL)
				);
				String username = config.get(PERSISTENCE_DB_USERNAME, String::toString).orElseThrow(
						() -> new OpenBBTException("Repository remote username not configured {}: ", PERSISTENCE_DB_USERNAME)
				);
				String password = config.get(PERSISTENCE_DB_PASSWORD, String::toString).orElseThrow(
						() -> new OpenBBTException("Repository remote password not configured {}: ", PERSISTENCE_DB_PASSWORD)
				);
				return (T) createRemoteRepository(type, url, username, password);
			}
			default -> throw new OpenBBTException("Unsupported repository mode: {}, expected: {}",
					mode,
					List.of(PERSISTENCE_MODE_FILE, PERSISTENCE_MODE_TRANSIENT, PERSISTENCE_MODE_REMOTE)
			);
		}

	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends Repository> T createReadOnlyRepository(Class<T> type) {
		String mode = config.get(PERSISTENCE_MODE, String.class).orElse(PERSISTENCE_MODE_FILE);
		try {
			DataSourceProvider provider = switch (mode) {
				case PERSISTENCE_MODE_FILE -> {
					Path envPath = config.get(ENV_PATH, Path::of).orElseThrow(
						() -> new OpenBBTException("Repository environment path not configured: {}", ENV_PATH)
					);
					Path filePath = config.get(PERSISTENCE_FILE, Path::of).orElseThrow(
						() -> new OpenBBTException("Repository file path not configured: {}", PERSISTENCE_FILE)
					);
					yield DataSourceProvider.hsqldb(envPath.resolve(filePath));
				}
				case PERSISTENCE_MODE_REMOTE -> {
					String url = config.get(PERSISTENCE_DB_URL, String::toString).orElseThrow(
						() -> new OpenBBTException("Repository remote URL not configured: {}", PERSISTENCE_DB_URL)
					);
					String username = config.get(PERSISTENCE_DB_USERNAME, String::toString).orElseThrow(
						() -> new OpenBBTException("Repository remote username not configured: {}", PERSISTENCE_DB_USERNAME)
					);
					String password = config.get(PERSISTENCE_DB_PASSWORD, String::toString).orElseThrow(
						() -> new OpenBBTException("Repository remote password not configured: {}", PERSISTENCE_DB_PASSWORD)
					);
					yield DataSourceProvider.postgresql(url, username, password);
				}
				default -> throw new OpenBBTException("Unsupported repository mode for read-only access: {}", mode);
			};
			Connection connection = provider.openConnection();
			if (type.equals(TestPlanRepository.class)) {
				return (T) new JooqPlanRepository(connection, provider.dialect());
			}
			throw new OpenBBTException("Unsupported repository type: {}", type.getName());
		} catch (OpenBBTException e) {
			throw e;
		} catch (Exception e) {
			throw new OpenBBTException(e, "Failed to open read-only repository connection");
		}
	}

	private Object createRemoteRepository(Class<?> type, String url, String username, String password) {
		DataSourceProvider provider = DataSourceProvider.postgresql(url, username, password);
		if (type.equals(TestPlanRepository.class)) {
			return new JooqPlanRepository(provider);
		}
		throw new OpenBBTException("Unsupported repository type for remote mode: {}", type.getName());
	}


	private static Object createFileRepository(Class<?> type, Path filePath) {
		DataSourceProvider provider = DataSourceProvider.hsqldb(filePath);
		if (type.equals(TestPlanRepository.class)) {
			return new JooqPlanRepository(provider);
		}
		throw new OpenBBTException("Unsupported repository type for file mode: {}", type.getName());
	}

}
