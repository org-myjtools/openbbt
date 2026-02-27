module org.myjtools.openbbt.plugins.markdownplan {
	requires org.commonmark;
	requires org.commonmark.ext.gfm.tables;
	requires org.myjtools.imconfig;
	requires org.myjtools.jexten;
	requires org.myjtools.openbbt.core;
	exports org.myjtools.openbbt.plugins.markdownplan;
	provides org.myjtools.openbbt.core.contributors.SuiteAssembler with org.myjtools.openbbt.plugins.markdownplan.MarkdownSuiteAssembler;
}