module org.myjtools.openbbt.plugins.markdownplan.test {

	requires org.junit.jupiter.api;
	requires org.myjtools.openbbt.core;
	requires org.assertj.core;
	requires org.myjtools.openbbt.plugins.markdownplan;
	requires org.myjtools.openbbt.persistence;
	requires org.myjtools.jexten;
	requires org.myjtools.imconfig;
	requires org.myjtools.jexten.maven.artifact.store;

	opens org.myjtools.openbbt.plugins.markdownplan.test to org.junit.platform.commons;

}