package org.myjtools.openbbt.persistence.test.execution;

import org.junit.jupiter.api.io.TempDir;
import org.myjtools.openbbt.persistence.DataSourceProvider;
import java.nio.file.Path;

class HsqldbExecutionRepositoryTest extends AbstractExecutionRepositoryTest {

	@TempDir
	private Path tempDir;

	@Override
	protected DataSourceProvider dataSourceProvider() {
		return DataSourceProvider.hsqldb(tempDir.resolve("testdb"));
	}

}