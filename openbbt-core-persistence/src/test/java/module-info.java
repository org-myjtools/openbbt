module org.myjtools.openbbt.core.persistence.test {

	requires org.junit.jupiter.api;
	requires org.myjtools.openbbt.core.persistence;
	requires org.assertj.core;
	requires org.junit.jupiter.params;
	requires org.myjtools.openbbt.core;

	opens org.myjtools.openbbt.core.persistence.test to org.junit.platform.commons;

}