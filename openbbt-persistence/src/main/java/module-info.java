import org.myjtools.openbbt.core.contributors.RepositoryFactory;
import org.myjtools.openbbt.persistence.JooqRepositoryFactory;

module org.myjtools.openbbt.persistence {
	requires org.myjtools.openbbt.core;
	requires java.sql;
	requires com.github.f4b6a3.ulid;
	requires org.jooq;
	requires org.myjtools.jexten;
	requires com.zaxxer.hikari;
	requires flyway.core;
	requires org.myjtools.imconfig;
	requires org.jspecify;

	exports org.myjtools.openbbt.persistence;

	opens org.myjtools.openbbt.persistence;
	opens org.myjtools.openbbt.persistence.migration.hsqldb;
	opens org.myjtools.openbbt.persistence.migration.postgresql;
	exports org.myjtools.openbbt.persistence.plannode;
	opens org.myjtools.openbbt.persistence.plannode;
	exports org.myjtools.openbbt.persistence.project;
	opens org.myjtools.openbbt.persistence.project;

	provides RepositoryFactory
			with JooqRepositoryFactory;
}
