module org.myjtools.openbbt.cli {
	exports org.myjtools.openbbt.cli;
	requires org.myjtools.openbbt.core;
	requires org.myjtools.openbbt.persistence;
	requires info.picocli;
	requires org.myjtools.jexten.plugin;
	requires com.google.common;
	requires org.myjtools.imconfig;
	requires org.slf4j;
	requires org.myjtools.openbbt.tui;

	opens org.myjtools.openbbt.cli to info.picocli;

}