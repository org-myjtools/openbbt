package org.myjtools.openbbt.core.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.SQLDialect;
import org.myjtools.jexten.Extension;
import javax.sql.DataSource;


@Extension(extensionPoint = "org.myjtools.openbbt.core.PlanNodeRepository")
public class HsqldbPlanNodeRepository extends JooqPlanNodeRepository {

	public HsqldbPlanNodeRepository() {
		init();
	}

	@Override
	protected DataSource createDataSource() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:hsqldb:mem:openbbt;DB_CLOSE_DELAY=-1");
		config.setUsername("sa");
		config.setPassword("");
		config.setMaximumPoolSize(10);
		return new HikariDataSource(config);
	}

	@Override
	protected SQLDialect dialect() {
		return SQLDialect.HSQLDB;
	}

	@Override
	protected String migrationLocation() {
		return "classpath:org/myjtools/openbbt/core/persistence/migration/hsqldb";
	}

}
