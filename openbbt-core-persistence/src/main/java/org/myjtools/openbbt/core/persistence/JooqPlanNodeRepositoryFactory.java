package org.myjtools.openbbt.core.persistence;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.PlanNodeRepository;
import org.myjtools.openbbt.core.contributors.PlanNodeRepositoryFactory;
import java.nio.file.Path;
import java.util.List;
import static org.myjtools.openbbt.core.OpenBBTConfig.*;

@Extension
public class JooqPlanNodeRepositoryFactory implements PlanNodeRepositoryFactory {

	@Inject
	Config config;

	@Override
	public PlanNodeRepository createPlanNodeRepository() {
		String mode = config.get(PERSISTENCE_MODE,String.class).orElse(PERSISTENCE_MODE_MEMORY);
		switch (mode) {
			case PERSISTENCE_MODE_MEMORY -> {
				return new JooqRepository(DataSourceProvider.hsqldb());
			}
			case PERSISTENCE_MODE_FILE -> {
				Path filePath = config.get(PERSISTENCE_FILE, Path::of).orElseThrow(
						() -> new OpenBBTException("Repository file path not configured {}: ", PERSISTENCE_FILE)
				);
				return new JooqRepository(DataSourceProvider.hsqldb(filePath));
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
				return new JooqRepository(DataSourceProvider.postgresql(url, username, password));
			}
			default -> throw new OpenBBTException("Unsupported repository mode: {}, expected: {}",
					mode,
					List.of(PERSISTENCE_MODE_FILE, PERSISTENCE_MODE_MEMORY, PERSISTENCE_MODE_REMOTE)
			);
		}

	}
}
