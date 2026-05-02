package org.myjtools.openbbt.plugins.db;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Inject;
import org.myjtools.jexten.Scope;
import org.myjtools.openbbt.core.Assertion;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.ResourceFinder;
import org.myjtools.openbbt.core.backend.ExecutionContext;
import org.myjtools.openbbt.core.contributors.StepExpression;
import org.myjtools.openbbt.core.contributors.StepProvider;
import org.myjtools.openbbt.core.contributors.TearDown;
import org.myjtools.openbbt.core.testplan.DataTable;
import org.myjtools.openbbt.core.util.Log;
import org.myjtools.openbbt.plugins.db.jooq.JooqDbEngine;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Extension(
	name = "Database Step Provider",
	extensionPointVersion = "1.0",
	scope = Scope.TRANSIENT
)
public class DbStepProvider implements StepProvider {

	private static final Log log = Log.of("plugins.db");

	@Inject
	private ResourceFinder resourceFinder;

	private DbEngine engine;
	private String alias;

	@Override
	public void init(Config config) {
		String nullValue = config.getString(DbConfigProvider.NULL_VALUE).orElse("");
		int maxAssertRows = config.get(DbConfigProvider.MAX_ASSERT_ROWS, Integer.class).orElse(100);
		engine = new JooqDbEngine(fillConnectionParameters(config), nullValue, maxAssertRows);
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

	@StepExpression(value = "db.assert.table.contains", args = {"table:word"})
	public void assertTableContents(String table, DataTable dataTable) {
		tryAndLog(table, () -> engine.assertTableContains(alias, table, engine.readTable(table, dataTable)));
	}

	@StepExpression(value = "db.assert.table.is", args = {"table:word"})
	public void assertTableIs(String table, DataTable dataTable) {
		tryAndLog(table, () -> engine.assertTableIs(alias, table, engine.readTable(table,dataTable)));
	}

	@StepExpression(value = "db.assert.table.contains.csv", args = {"table:word", "file:file"})
	public void assertTableContainsCsv(String table, java.nio.file.Path csvFile) {
		tryAndLog(
			table,
			() -> engine.assertTableContains(alias, table, engine.readCsv(table,resourceFinder.resolve(csvFile)))
		);
	}

	@StepExpression(value = "db.assert.table.is.csv", args = {"table:word", "file:file"})
	public void assertTableIsCsv(String table, java.nio.file.Path csvFile) {
		tryAndLog(
			table,
			() -> engine.assertTableIs(alias, table, engine.readCsv(table,resourceFinder.resolve(csvFile)))
		);
	}

	@StepExpression(value = "db.assert.contains.xls", args = {"file:file"})
	public void assertXlsContains(java.nio.file.Path xlsFile) {
		for (DataSet dataSet : engine.readXls(resourceFinder.resolve(xlsFile))) {
			tryAndLog(dataSet.table(), () -> engine.assertTableContains(alias, dataSet.table(), dataSet));
		}
	}


	private void tryAndLog(String table, Runnable assertion) {
		try {
			assertion.run();
		} catch (AssertionError e) {
			logTable(table);
			throw e;
		}
	}

	private void logTable(String table) {
		String tableContent = engine.printTable(alias, table);
		log.debug("{}\n{}", table, tableContent);
		ExecutionContext.current()
			.storeAttachment(tableContent.getBytes(StandardCharsets.UTF_8), "csv");
	}

}
