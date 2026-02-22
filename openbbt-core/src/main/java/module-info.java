import org.myjtools.openbbt.core.contributors.*;

module org.myjtools.openbbt.core {

	requires com.github.benmanes.caffeine;
	requires org.slf4j;
	requires org.myjtools.jexten;
	requires org.myjtools.imconfig;
	requires static lombok;
	requires java.sql;
	requires com.github.f4b6a3.ulid;
	requires org.myjtools.jexten.plugin;
	requires org.myjtools.mavenfetcher;
	requires org.myjtools.jexten.maven.artifact.store;
	requires org.jspecify;
	requires org.yaml.snakeyaml;
	requires org.hamcrest;

	exports org.myjtools.openbbt.core;
	exports org.myjtools.openbbt.core.util;
	exports org.myjtools.openbbt.core.plan;
	exports org.myjtools.openbbt.core.messages;
	exports org.myjtools.openbbt.core.backend;
	exports org.myjtools.openbbt.core.project;
	exports org.myjtools.openbbt.core.contributors;
	exports org.myjtools.openbbt.core.expressions;
	exports org.myjtools.openbbt.core.datatypes;
	exports org.myjtools.openbbt.core.assertions;

	opens org.myjtools.openbbt.core to org.myjtools.jexten;
	opens org.myjtools.openbbt.core.messages to org.myjtools.jexten;
	opens org.myjtools.openbbt.core.plan to org.myjtools.jexten;

	opens org.myjtools.openbbt.core.contributors to org.myjtools.jexten;

	uses AssertionFactoryProvider;
	uses DataTypeProvider;
	uses org.myjtools.openbbt.core.PlanNodeRepository;
	uses ConfigProvider;
	uses org.myjtools.openbbt.core.messages.MessageProvider;
	uses SuiteAssembler;
	uses org.myjtools.openbbt.core.contributors.StepProvider;
	uses PlanNodeRepositoryFactory;

	provides ConfigProvider with org.myjtools.openbbt.core.OpenBBTConfig;
	provides org.myjtools.openbbt.core.contributors.DataTypeProvider with org.myjtools.openbbt.core.datatypes.CoreDataTypes;
	provides org.myjtools.openbbt.core.messages.MessageProvider with org.myjtools.openbbt.core.assertions.AssertionMessageProvider;
	provides org.myjtools.openbbt.core.contributors.AssertionFactoryProvider with org.myjtools.openbbt.core.assertions.CoreAssertionFactories;

}