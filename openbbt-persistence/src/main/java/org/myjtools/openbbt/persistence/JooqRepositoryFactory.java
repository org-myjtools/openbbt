package org.myjtools.openbbt.persistence;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.contributors.RepositoryFactory;
import org.myjtools.openbbt.core.persistence.PlanNodeRepository;
import org.myjtools.openbbt.core.persistence.ProjectRepository;
import org.myjtools.openbbt.persistence.plannode.JooqPlanNodeRepository;
import org.myjtools.openbbt.persistence.project.JooqProjectRepository;
import java.nio.file.Path;
import java.util.List;
import static org.myjtools.openbbt.core.OpenBBTConfig.*;

@Extension
public class JooqRepositoryFactory implements RepositoryFactory {

	@Inject
	Config config;

	@Override
	public PlanNodeRepository createPlanNodeRepository() {
		return (PlanNodeRepository) createRepository(PlanNodeRepository.class);
	}


	@Override
	public ProjectRepository createProjectRepository() {
		return (ProjectRepository) createRepository(ProjectRepository.class);
	}


	private Object createRepository(Class<?> type) {
		String mode = config.get(PERSISTENCE_MODE,String.class).orElse(PERSISTENCE_MODE_MEMORY);
		switch (mode) {
			case PERSISTENCE_MODE_MEMORY -> {
				return createInMemoryRepository(type);
			}
			case PERSISTENCE_MODE_FILE -> {
				Path filePath = config.get(PERSISTENCE_FILE, Path::of).orElseThrow(
						() -> new OpenBBTException("Repository file path not configured {}: ", PERSISTENCE_FILE)
				);
				return createFileRepository(type,filePath);
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
				return createRemoteRepository(type, url, username, password);
			}
			default -> throw new OpenBBTException("Unsupported repository mode: {}, expected: {}",
					mode,
					List.of(PERSISTENCE_MODE_FILE, PERSISTENCE_MODE_MEMORY, PERSISTENCE_MODE_REMOTE)
			);
		}

	}

	private Object createRemoteRepository(Class<?> type, String url, String username, String password) {
		DataSourceProvider provider = DataSourceProvider.postgresql(url, username, password);
		if (type.equals(PlanNodeRepository.class)) {
			return new JooqPlanNodeRepository(provider);
		}
		if (type.equals(ProjectRepository.class)) {
			return new JooqProjectRepository(provider);
		}
		throw new OpenBBTException("Unsupported repository type for remote mode: {}", type.getName());
	}


	private static Object createFileRepository(Class<?> type, Path filePath) {
		DataSourceProvider provider = DataSourceProvider.hsqldb(filePath);
		if (type.equals(PlanNodeRepository.class)) {
			return new JooqPlanNodeRepository(provider);
		}
		if (type.equals(ProjectRepository.class)) {
			return new JooqProjectRepository(provider);
		}
		throw new OpenBBTException("Unsupported repository type for file mode: {}", type.getName());
	}


	private static Object createInMemoryRepository(Class<?> type) {
		DataSourceProvider provider = DataSourceProvider.hsqldb();
		if (type.equals(PlanNodeRepository.class)) {
			return new JooqPlanNodeRepository(provider);
		}
		if (type.equals(ProjectRepository.class)) {
			return new JooqProjectRepository(provider);
		}
		throw new OpenBBTException("Unsupported repository type for in-memory mode: {}", type.getName());
	}
}
