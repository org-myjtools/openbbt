module org.myjtools.openbbt.plugins.rest.test {
	requires org.junit.jupiter.api;
	requires org.myjtools.openbbt.core;
	requires org.myjtools.imconfig;

	opens org.myjtools.plugins.rest.test to org.junit.platform.commons;
}