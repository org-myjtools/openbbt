package org.myjtools.openbbt.persistence.test.project;

import org.myjtools.openbbt.persistence.DataSourceProvider;

class HsqldbMemoryProjectRepositoryTest extends AbstractProjectRepositoryTest {

	@Override
	protected DataSourceProvider dataSourceProvider() {
		return DataSourceProvider.hsqldb();
	}

}
