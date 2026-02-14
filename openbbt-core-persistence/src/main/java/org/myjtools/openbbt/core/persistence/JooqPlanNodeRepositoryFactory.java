package org.myjtools.openbbt.core.persistence;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.PlanNodeRepository;
import org.myjtools.openbbt.core.contributors.PlanNodeRepositoryFactory;
import java.nio.file.Path;
import java.util.List;

@Extension
public class JooqPlanNodeRepositoryFactory implements PlanNodeRepositoryFactory {

	@Inject
	Config config;

	@Override
	public PlanNodeRepository createPlanNodeRepository() {
		String mode = config.get(OpenBBTConfig.REPOSITORY_MODE,String.class).orElse(OpenBBTConfig.REPOSITORY_MODE_MEMORY);
		switch (mode) {
			case OpenBBTConfig.REPOSITORY_MODE_MEMORY -> {
				return new JooqRepository(DataSourceProvider.hsqldb());
			}
			case OpenBBTConfig.REPOSITORY_MODE_FILE -> {
				Path filePath = config.get(OpenBBTConfig.REPOSITORY_FILE, Path::of).orElseThrow(
						() -> new OpenBBTException("Repository file path not configured {}: ", OpenBBTConfig.REPOSITORY_FILE)
				);
				return new JooqRepository(DataSourceProvider.hsqldb(filePath));
			}
			case OpenBBTConfig.REPOSITORY_MODE_REMOTE -> {
				String url = config.get(OpenBBTConfig.REPOSITORY_URL, String::toString).orElseThrow(
						() -> new OpenBBTException("Repository remote URL not configured {}: ", OpenBBTConfig.REPOSITORY_URL)
				);
				String username = config.get(OpenBBTConfig.REPOSITORY_USERNAME, String::toString).orElseThrow(
						() -> new OpenBBTException("Repository remote username not configured {}: ", OpenBBTConfig.REPOSITORY_USERNAME)
				);
				String password = config.get(OpenBBTConfig.REPOSITORY_PASSWORD, String::toString).orElseThrow(
						() -> new OpenBBTException("Repository remote password not configured {}: ", OpenBBTConfig.REPOSITORY_PASSWORD)
				);
				return new JooqRepository(DataSourceProvider.postgresql(url, username, password));
			}
			default -> throw new OpenBBTException("Unsupported repository mode: {}, expected: {}",
					mode,
					List.of(OpenBBTConfig.REPOSITORY_MODE_FILE, OpenBBTConfig.REPOSITORY_MODE_MEMORY, OpenBBTConfig.REPOSITORY_MODE_REMOTE)
			);
		}

	}
}
