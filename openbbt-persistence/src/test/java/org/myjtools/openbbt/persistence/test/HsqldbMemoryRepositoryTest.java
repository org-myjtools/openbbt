package org.myjtools.openbbt.persistence.test;

import org.myjtools.openbbt.persistence.DataSourceProvider;

class HsqldbMemoryRepositoryTest extends AbstractRepositoryTest {

	@Override
	protected DataSourceProvider dataSourceProvider() {
		return DataSourceProvider.hsqldb();
	}

}