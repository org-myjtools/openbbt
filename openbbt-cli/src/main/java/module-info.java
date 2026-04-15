module org.myjtools.openbbt.cli {
	exports org.myjtools.openbbt.cli;
	requires org.myjtools.openbbt.core;
	requires org.myjtools.openbbt.persistence;
	requires info.picocli;
	requires org.myjtools.jexten.plugin;
	requires com.google.common;
	requires org.myjtools.imconfig;
	requires org.slf4j;
	requires ch.qos.logback.classic;
	requires ch.qos.logback.core;
	requires org.myjtools.openbbt.tui;
	requires org.myjtools.openbbt.lsp;
	requires org.myjtools.openbbt.jsonrpc;
	requires com.google.gson;

	opens org.myjtools.openbbt.cli to info.picocli;

}