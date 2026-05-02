package org.myjtools.openbbt.plugins.db.jooq;

import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.testplan.DataTable;
import org.myjtools.openbbt.plugins.db.DataSet;

import java.util.List;

public class DataTableRecordLoader {

	private final int maxRows;

	public DataTableRecordLoader(int maxRows) {
		this.maxRows = maxRows;
	}

	public DataSet load(String table, DataTable dataTable) {
		var columns = dataTable.values().getFirst();
		var rows = dataTable.values().stream().skip(1).toList();
		if (rows.size() > maxRows) {
			throw new OpenBBTException(
				"Data table has more than {} rows, which exceeds the configured limit for assertions",
				maxRows
			);
		}
		return new DataSet(table, columns, rows);
	}
}