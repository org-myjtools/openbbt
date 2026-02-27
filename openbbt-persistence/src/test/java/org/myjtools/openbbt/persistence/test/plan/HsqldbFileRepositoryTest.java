package org.myjtools.openbbt.persistence.test.plan;

import org.junit.jupiter.api.io.TempDir;
import org.myjtools.openbbt.persistence.DataSourceProvider;
import java.nio.file.Path;

class HsqldbFileRepositoryTest extends AbstractRepositoryTest {

	@TempDir
	private Path tempDir;

	@Override
	protected DataSourceProvider dataSourceProvider() {
		return DataSourceProvider.hsqldb(tempDir.resolve("testdb"));
	}

}