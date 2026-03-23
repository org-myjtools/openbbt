package org.myjtools.openbbt.plugins.db;

import org.myjtools.openbbt.core.testplan.DataTable;
import java.nio.file.Path;

public interface DbEngine {


	void insertDataTable(String datasource, String table, DataTable data);

	void insertCSVFile(String datasource, String table, Path csvFile);

	void insertSubselect(String datasource, String table, String subselect);

	void insertXLSFile(String datasource, String table, Path xlsFile);
}
