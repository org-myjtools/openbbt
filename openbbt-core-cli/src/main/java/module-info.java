module org.myjtools.openbbt.core.cli {
	exports org.myjtools.openbbt.core.cli;
	requires org.myjtools.openbbt.core;
	requires info.picocli;
	requires org.myjtools.jexten.plugin;
	requires com.google.common;
	requires org.myjtools.imconfig;

	opens org.myjtools.openbbt.core.cli to info.picocli;

}