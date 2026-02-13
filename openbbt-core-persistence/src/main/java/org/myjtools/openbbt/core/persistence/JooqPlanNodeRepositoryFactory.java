package org.myjtools.openbbt.core.persistence;

import org.myjtools.jexten.Extension;
import org.myjtools.openbbt.core.PlanNodeRepository;
import org.myjtools.openbbt.core.contributors.PlanNodeRepositoryFactory;
import java.nio.file.Path;

@Extension
public class JooqPlanNodeRepositoryFactory implements PlanNodeRepositoryFactory {

	@Override
	public PlanNodeRepository create(Mode mode, Object...args) {
		return new JooqRepository(dataSourceProvider(mode,args));
	}

	private DataSourceProvider dataSourceProvider(Mode mode,Object...args) {
		return switch (mode) {
			case FILE -> DataSourceProvider.hsqldb(Path.of(args[0].toString()));
			case REMOTE -> DataSourceProvider.postgresql(args[0].toString(),args[1].toString(),args[2].toString());
			case IN_MEMORY -> DataSourceProvider.hsqldb();
		};
	}

}
