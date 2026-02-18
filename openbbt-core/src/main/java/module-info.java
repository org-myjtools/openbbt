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

	exports org.myjtools.openbbt.core;
	exports org.myjtools.openbbt.core.util;
	exports org.myjtools.openbbt.core.plan;
	exports org.myjtools.openbbt.core.messages;
	exports org.myjtools.openbbt.core.step;
	exports org.myjtools.openbbt.core.backend;
	exports org.myjtools.openbbt.core.project;

	opens org.myjtools.openbbt.core to org.myjtools.jexten;
	opens org.myjtools.openbbt.core.messages to org.myjtools.jexten;
	opens org.myjtools.openbbt.core.plan to org.myjtools.jexten;
	opens org.myjtools.openbbt.core.step to org.myjtools.jexten;
	exports org.myjtools.openbbt.core.contributors;
	opens org.myjtools.openbbt.core.contributors to org.myjtools.jexten;

	uses AssertionFactoryProvider;
	uses DataTypeProvider;
	uses org.myjtools.openbbt.core.PlanNodeRepository;
	uses ConfigProvider;
	uses org.myjtools.openbbt.core.messages.MessageProvider;
	uses SuiteAssembler;
	uses org.myjtools.openbbt.core.step.StepContributor;
	uses PlanNodeRepositoryFactory;

}