package org.myjtools.openbbt.core.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import javax.sql.DataSource;
import java.nio.file.Path;

public class DataSourceProvider {

	public enum DatabaseType {
		HSQLDB("hsqldb"),
		POSTGRESQL("postgresql");

		private final String migrationFolder;

		DatabaseType(String migrationFolder) {
			this.migrationFolder = migrationFolder;
		}
	}

	interface JdbcUrlProvider {
		String jdbcUrl();
		String username();
		String password();
		DatabaseType databaseType();
	}

	public static class HsqldbDataSource implements JdbcUrlProvider {

		private final Path file;
		public HsqldbDataSource(Path file) {
			this.file = file;
		}

		@Override
		public String jdbcUrl() {
			return "jdbc:hsqldb:file:" + file.toAbsolutePath() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
		}
		@Override
		public String username() {
			return "sa";
		}
		@Override
		public String password() {
			return "";
		}
		@Override
		public DatabaseType databaseType() {
			return DatabaseType.HSQLDB;
		}
	}

	public static class PostgresqlDataSource implements JdbcUrlProvider {

		private final String jdbcUrl;
		private final String username;
		private final String password;

		public PostgresqlDataSource(String jdbcUrl, String username, String password) {
			this.jdbcUrl = jdbcUrl;
			this.username = username;
			this.password = password;
		}

		@Override
		public String jdbcUrl() {
			return jdbcUrl;
		}
		@Override
		public String username() {
			return username;
		}
		@Override
		public String password() {
			return password;
		}
		@Override
		public DatabaseType databaseType() {
			return DatabaseType.POSTGRESQL;
		}
	}


	public static DataSourceProvider hsqldb(Path file) {
		return new DataSourceProvider(new HsqldbDataSource(file));
	}

	public static DataSourceProvider postgresql(String jdbcUrl, String username, String password) {
		return new DataSourceProvider(new PostgresqlDataSource(jdbcUrl, username, password));
	}


	DataSourceProvider(JdbcUrlProvider jdbcUrlProvider) {
		this.jdbcUrlProvider = jdbcUrlProvider;
	}


	private final JdbcUrlProvider jdbcUrlProvider;


	public DataSource obtainDataSource() {

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(jdbcUrlProvider.jdbcUrl());
		config.setUsername(jdbcUrlProvider.username());
		config.setPassword(jdbcUrlProvider.password());
		config.setMaximumPoolSize(10);
		DataSource dataSource = new HikariDataSource(config);

		String migrationLocation = "classpath:org/myjtools/openbbt/core/persistence/migration/"
			+ jdbcUrlProvider.databaseType().migrationFolder;

		Flyway flyway = Flyway.configure(getClass().getClassLoader())
			.dataSource(dataSource)
			.locations(migrationLocation)
			.load();
		flyway.migrate();

		return dataSource;
	}

}
