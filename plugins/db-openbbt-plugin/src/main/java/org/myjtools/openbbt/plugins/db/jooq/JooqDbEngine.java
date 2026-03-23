package org.myjtools.openbbt.plugins.db.jooq;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.Select;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.testplan.DataTable;
import org.myjtools.openbbt.plugins.db.ConnectionParameters;
import org.myjtools.openbbt.plugins.db.DbEngine;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JooqDbEngine implements DbEngine {

	private final Map<String, DSLContext> dslContexts = new HashMap<>();
	private final String nullValue;

	public JooqDbEngine(Map<String, ConnectionParameters> connections, String nullValue) {
		this.nullValue = nullValue;
		for (Map.Entry<String, ConnectionParameters> entry : connections.entrySet()) {
			String datasource = entry.getKey();
			ConnectionParameters params = entry.getValue();
			DSLContext dslContext = createDSLContext(params);
			dslContexts.put(datasource, dslContext);
		}
	}

	private DSLContext createDSLContext(ConnectionParameters params) {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(params.url());
		config.setUsername(params.username());
		config.setPassword(params.password());
		config.setMaximumPoolSize(10);
		config.setCatalog(params.catalog());
		config.setSchema(params.schema());
		config.setDriverClassName(params.driver());
		DataSource dataSource = new HikariDataSource(config);
		SQLDialect dialect = SQLDialect.valueOf(params.dialect().toUpperCase());
		return DSL.using(new DataSourceConnectionProvider(dataSource), dialect);
	}

	private DSLContext dslContext(String datasource) {
		DSLContext dslContext = dslContexts.get(datasource);
		if (dslContext == null) {
			throw new OpenBBTException("Unknown datasource {}",datasource);
		}
		return dslContext;
	}


	@Override
	public void insertDataTable(String datasource, String table, DataTable data) {
		List<String> columns = data.values().getFirst();
		List<Field<Object>> fields = columns.stream().map(DSL::field).toList();
		List<List<String>> values = data.values().stream().skip(1).toList();
		dslContext(datasource)
			.insertInto(DSL.table(table))
			.columns(fields)
			.values(values)
			.execute();
	}


	@Override
	public void insertCSVFile(String datasource, String table, java.nio.file.Path csvFile) {
		dslContext(datasource).loadInto(DSL.table(table)).loadCSV(csvFile.toFile());

	}

	@Override
	public void insertSubselect(String datasource, String table, String subselect) {
		dslContext(datasource)
			.insertInto(DSL.table(table))
			.select((Select<?>) DSL.resultQuery(subselect))
			.execute();
	}

	@Override
	public void insertXLSFile(String datasource, String table, java.nio.file.Path xlsFile) {
		try {
			new ExcelRecordLoader(dslContext(datasource), nullValue).loadExcel(xlsFile.toString());
		} catch (IOException e) {
			throw new OpenBBTException(e, "Failed to load Excel file {}", xlsFile);
		}
	}

}
