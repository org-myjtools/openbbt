package org.myjtools.openbbt.core.persistence.test;

import org.myjtools.openbbt.core.persistence.DataSourceProvider;

class HsqldbMemoryRepositoryTest extends AbstractRepositoryTest {

	@Override
	protected DataSourceProvider dataSourceProvider() {
		return DataSourceProvider.hsqldb();
	}

}