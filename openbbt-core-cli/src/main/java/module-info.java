module org.myjtools.openbbt.core.cli {
	exports org.myjtools.openbbt.core.cli;
	requires org.myjtools.openbbt.core;
	requires info.picocli;
	requires org.myjtools.jexten.plugin;

	opens org.myjtools.openbbt.core.cli to info.picocli;

}