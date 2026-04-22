package org.myjtools.openbbt.plugins.db;

import org.myjtools.openbbt.core.testplan.DataTable;
import org.myjtools.openbbt.core.testplan.Document;
import java.nio.file.Path;

public interface DbEngine extends AutoCloseable {


	void insertDataTable(String datasource, String table, DataTable data);

	void insertCSVFile(String datasource, String table, Path csvFile);

	void insertSubselect(String datasource, String table, String subselect);

	void insertXLSFile(String datasource, String table, Path xlsFile);

	Integer executeCountQueryFromTable(String datasource, String table);
}
