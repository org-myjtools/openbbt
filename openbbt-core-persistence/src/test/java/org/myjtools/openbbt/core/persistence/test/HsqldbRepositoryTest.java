package org.myjtools.openbbt.core.persistence.test;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.openbbt.core.persistence.DataSourceProvider;

import javax.sql.DataSource;
import java.nio.file.Path;

class HsqldbRepositoryTest extends AbstractRepositoryTest {

	@TempDir
	private Path tempDir;

	@Override
	protected DSLContext createDSLContext() {
		DataSourceProvider dataSourceProvider = DataSourceProvider.hsqldb(tempDir.resolve("testdb"));
		DataSource dataSource = dataSourceProvider.obtainDataSource();
		return DSL.using(new DataSourceConnectionProvider(dataSource), SQLDialect.HSQLDB);
	}

}