module org.myjtools.openbbt.core {
	requires com.github.benmanes.caffeine;
	requires org.slf4j;
	requires org.myjtools.jexten;
	requires org.myjtools.imconfig;
	requires static lombok;
	requires java.sql;
	requires com.github.f4b6a3.ulid;



	exports org.myjtools.openbbt.core;
	exports org.myjtools.openbbt.core.util;
	exports org.myjtools.openbbt.core.plan;
	exports org.myjtools.openbbt.core.contributors;
	exports org.myjtools.openbbt.core.messages;
	exports org.myjtools.openbbt.core.resources;
	exports org.myjtools.openbbt.core.step;
	exports org.myjtools.openbbt.core.backend;
	exports org.myjtools.openbbt.core.config;


}