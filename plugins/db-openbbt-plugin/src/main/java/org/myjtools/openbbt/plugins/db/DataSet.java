package org.myjtools.openbbt.plugins.db;

import java.util.List;

public record DataSet(String table, List<String> columns, List<List<String>> rows) {

}
