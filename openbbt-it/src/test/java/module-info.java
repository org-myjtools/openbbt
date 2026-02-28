module org.myjtools.openbbt.it {
	requires org.myjtools.openbbt.core;
	requires org.myjtools.openbbt.persistence;
	requires org.myjtools.imconfig;
	requires org.junit.jupiter.api;
	requires org.assertj.core;

	opens org.myjtools.openbbt.it to org.junit.platform.commons;
}