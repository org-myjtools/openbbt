package org.myjtools.openbbt.plugins.db;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.contributors.StepProvider;
import org.myjtools.openbbt.plugins.db.jooq.JooqDbEngine;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Extension(
	name = "Database Step Provider",
	extensionPointVersion = "1.0",
	scope = Scope.TRANSIENT
)
public class DbStepProvider implements StepProvider {

	private Map<String, ConnectionParameters> connectionParameters = new HashMap<>();

	private DbEngine engine;

	@Override
	public void init(Config config) {
		Map<String, ConnectionParameters> connections = fillConnectionParameters(config);
		engine = new JooqDbEngine(connections);
	}

	private Map<String, ConnectionParameters> fillConnectionParameters(Config config) {
		Map<String, ConnectionParameters> connections = new HashMap<>();
		Config datasourcesConfig = config.inner(DbConfigProvider.DATASOURCES);
		for (String datasource : datasourcesConfig.innerKeys().toList()) {
			Config datasourceConfig = datasourcesConfig.inner(datasource);
			String url = datasourceConfig.getString(DbConfigProvider.DATASOURCE_URL).orElseThrow(
				() -> new OpenBBTException("Missing URL for datasource {}",datasource)
			);
			String username = datasourceConfig.getString(DbConfigProvider.DATASOURCE_USERNAME).orElseThrow(
				() -> new OpenBBTException("Missing username for datasource {}",datasource)
			);
			String password = datasourceConfig.getString(DbConfigProvider.DATASOURCE_PASSWORD).orElseThrow(
				() -> new OpenBBTException("Missing password for datasource {}",datasource)
			);
			String driver = datasourceConfig.getString(DbConfigProvider.DATASOURCE_DRIVER).orElseThrow(
				() -> new OpenBBTException("Missing driver for datasource {}",datasource)
			);
			String dialect = datasourceConfig.getString(DbConfigProvider.DATASOURCE_DIALECT).orElseThrow(
				() -> new OpenBBTException("Missing dialect for datasource {}",datasource)
			);
			String schema = datasourceConfig.getString(DbConfigProvider.DATASOURCE_SCHEMA).orElse(null);
			String catalog = datasourceConfig.getString(DbConfigProvider.DATASOURCE_CATALOG).orElse(null);

			connections.put(
				datasource,
				new ConnectionParameters(url, username, password, driver, schema, catalog, dialect)
			);
		}
		return connections;
	}

}
