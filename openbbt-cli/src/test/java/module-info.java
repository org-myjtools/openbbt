import org.myjtools.openbbt.core.contributors.SuiteAssembler;

module org.myjtools.openbbt.cli.test {
	requires org.myjtools.openbbt.core;
	requires org.myjtools.imconfig;
	requires info.picocli;
	requires org.junit.jupiter.api;
	requires org.assertj.core;
	requires org.myjtools.openbbt.cli;
	requires org.myjtools.jexten;

	opens org.myjtools.openbbt.cli.test to org.junit.platform.commons, info.picocli, org.myjtools.jexten, org.myjtools.openbbt.core;

	provides SuiteAssembler with org.myjtools.openbbt.cli.test.TestSuiteAssembler;
}
