package org.myjtools.openbbt.plugins.db;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.myjtools.openbbt.core.Assertion;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.contributors.StepExpression;
import org.myjtools.openbbt.core.contributors.StepProvider;
import org.myjtools.openbbt.core.contributors.TearDown;
import org.myjtools.openbbt.core.testplan.Document;
import org.myjtools.openbbt.plugins.db.jooq.JooqDbEngine;
import java.util.HashMap;
import java.util.Map;

@Extension(
	name = "Database Step Provider",
	extensionPointVersion = "1.0",
	scope = Scope.TRANSIENT
)
public class DbStepProvider implements StepProvider {

	private DbEngine engine;
	private String alias;

	@Override
	public void init(Config config) {
		String nullValue = config.getString(DbConfigProvider.NULL_VALUE).orElse("");
		engine = new JooqDbEngine(fillConnectionParameters(config), nullValue);
	}

	@TearDown
	public void tearDown() {
		try {
			engine.close();
		} catch (Exception e) {
			throw new OpenBBTException(e, "Failed to close database engine");
		}
	}

	private Map<String, ConnectionParameters> fillConnectionParameters(Config config) {
		Map<String, ConnectionParameters> connections = new HashMap<>();
		Config datasourcesConfig = config.inner(DbConfigProvider.DATASOURCES);
		for (String datasource : datasourcesConfig.innerKeys().toList()) {
			Config datasourceConfig = datasourcesConfig.inner(datasource);
			String url = datasourceConfig.getString(DbConfigProvider.DATASOURCE_URL).orElseThrow(
				() -> new OpenBBTException("Missing URL for datasource {}",datasource)
			);
			String username = datasourceConfig.getString(DbConfigProvider.DATASOURCE_USERNAME).orElse("");
			String password = datasourceConfig.getString(DbConfigProvider.DATASOURCE_PASSWORD).orElse("");
			String driver = datasourceConfig.getString(DbConfigProvider.DATASOURCE_DRIVER).orElseThrow(
				() -> new OpenBBTException("Missing driver for datasource {}",datasource)
			);
			String dialect = datasourceConfig.getString(DbConfigProvider.DATASOURCE_DIALECT).orElseThrow(
				() -> new OpenBBTException("Missing dialect for datasource {}",datasource)
			);
			String schema = datasourceConfig.getString(DbConfigProvider.DATASOURCE_SCHEMA).orElse(null);
			String catalog = datasourceConfig.getString(DbConfigProvider.DATASOURCE_CATALOG).orElse(null);
			boolean quoteIdentifiers = datasourceConfig.get(DbConfigProvider.DATASOURCE_QUOTE_IDENTIFIERS, Boolean.class).orElse(true);

			connections.put(
				datasource,
				new ConnectionParameters(url, username, password, driver, schema, catalog, dialect, quoteIdentifiers)
			);
		}
		return connections;
	}



	@StepExpression(value = "db.define.alias", args = {"alias:text"})
	public void defineAlias(String alias) {
		this.alias = alias;
	}

	@StepExpression(value = "db.assert.count", args = {"table:word"})
	public void assertCountSql(String table, Assertion assertion) {
		Integer count = engine.executeCountQueryFromTable(alias, table);
		Assertion.assertThat(count, assertion);
	}

}
