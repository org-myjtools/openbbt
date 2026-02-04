package org.myjtools.openbbt.core.persistence.test;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.junit.jupiter.api.condition.EnabledIf;
import org.myjtools.openbbt.core.persistence.DataSourceProvider;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

@Testcontainers
@EnabledIf("isDockerAvailable")
class PostgresqlRepositoryTest extends AbstractRepositoryTest {

	static boolean isDockerAvailable() {
		try {
			DockerClientFactory.instance().client();
			return true;
		} catch (Throwable ex) {
			return false;
		}
	}

	@Container
	private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Override
	protected DSLContext createDSLContext() {
		DataSourceProvider dataSourceProvider = DataSourceProvider.postgresql(
			postgres.getJdbcUrl(),
			postgres.getUsername(),
			postgres.getPassword()
		);
		DataSource dataSource = dataSourceProvider.obtainDataSource();
		return DSL.using(new DataSourceConnectionProvider(dataSource), SQLDialect.POSTGRES);
	}

}