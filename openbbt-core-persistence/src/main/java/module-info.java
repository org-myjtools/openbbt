module org.myjtools.openbbt.core.persistence {
	requires org.myjtools.openbbt.core;
	requires java.sql;
	requires com.github.f4b6a3.ulid;
	requires org.jooq;
	requires org.myjtools.jexten;
	requires com.zaxxer.hikari;
	requires flyway.core;
	requires org.myjtools.imconfig;

	exports org.myjtools.openbbt.core.persistence;

	opens org.myjtools.openbbt.core.persistence;
	opens org.myjtools.openbbt.core.persistence.migration.hsqldb;
	opens org.myjtools.openbbt.core.persistence.migration.postgresql;

	provides org.myjtools.openbbt.core.contributors.PlanNodeRepositoryFactory
			with org.myjtools.openbbt.core.persistence.JooqPlanNodeRepositoryFactory;
}
