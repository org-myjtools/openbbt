module org.myjtools.openbbt.core.assertions.test {
	requires org.junit.jupiter.api;
	requires org.junit.jupiter.params;
	requires org.assertj.core;
	requires org.hamcrest;
	requires org.myjtools.openbbt.core;
	requires org.myjtools.openbbt.core.assertions;
	requires org.myjtools.openbbt.core.datatypes;

	opens org.myjtools.openbbt.core.assertions.test to org.junit.platform.commons;
}