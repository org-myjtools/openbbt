module org.myjtools.openbbt.plugins.rest.test {

	requires org.myjtools.openbbt.plugins.rest;

	requires org.junit.jupiter.api;
	requires org.myjtools.openbbt.persistence;
	requires org.myjtools.openbbt.plugins.gherkin;
	requires org.myjtools.imconfig;
	requires org.myjtools.openbbt.core;
	requires openbbt.test.support;

	opens org.myjtools.plugins.rest.test to org.junit.platform.commons;

}
