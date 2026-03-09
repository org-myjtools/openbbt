module org.myjtools.openbbt.cli.test {
	requires org.myjtools.openbbt.core;
	requires org.myjtools.imconfig;
	requires info.picocli;
	requires org.junit.jupiter.api;
	requires org.assertj.core;
	requires org.myjtools.openbbt.cli;

	opens org.myjtools.openbbt.cli.test to org.junit.platform.commons, info.picocli;
}
