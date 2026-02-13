module org.myjtools.openbbt.plugins.gherkin.test {

	requires org.junit.jupiter.api;
	requires org.myjtools.openbbt.core;
	requires org.assertj.core;
	requires org.myjtools.openbbt.plugins.gherkin;
	requires org.myjtools.gherkinparser;
	requires org.myjtools.openbbt.core.persistence;
	requires junit5.memory.check;
	requires org.myjtools.jexten;
	requires org.myjtools.imconfig;

	opens org.myjtools.openbbt.plugins.gherkin.test to org.junit.platform.commons;


}