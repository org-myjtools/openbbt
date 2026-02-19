module org.myjtools.openbbt.core.cli.test {
	requires org.myjtools.openbbt.core;
	requires info.picocli;
	requires org.junit.jupiter.api;
	requires org.assertj.core;
	requires org.myjtools.openbbt.core.cli;
	requires org.myjtools.openbbt.plugins.gherkin;


	opens org.myjtools.openbbt.core.cli.test to org.junit.platform.commons, info.picocli;
}
