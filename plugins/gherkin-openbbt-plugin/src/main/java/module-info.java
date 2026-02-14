import org.myjtools.openbbt.plugins.gherkin.GherkinSuiteAssembler;

module org.myjtools.openbbt.plugins.gherkin {

	exports org.myjtools.openbbt.plugins.gherkin;
	
	requires org.myjtools.jexten;
	requires org.myjtools.openbbt.core;
	requires org.myjtools.imconfig;
	requires org.myjtools.gherkinparser;


	provides org.myjtools.openbbt.core.contributors.SuiteAssembler with GherkinSuiteAssembler;
	provides org.myjtools.openbbt.core.contributors.ConfigProvider with org.myjtools.openbbt.plugins.gherkin.GherkinConfig;

	opens org.myjtools.openbbt.plugins.gherkin to org.myjtools.jexten;

}