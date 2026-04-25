module org.myjtools.openbbt.lsp.test {
    requires org.myjtools.openbbt.lsp;
    requires org.myjtools.openbbt.core;
    requires org.myjtools.gherkinparser;
    requires org.myjtools.imconfig;
    requires org.eclipse.lsp4j;
    requires org.eclipse.lsp4j.jsonrpc;
    requires org.junit.jupiter.api;
    requires org.assertj.core;
    opens org.myjtools.openbbt.lsp.test to org.junit.platform.commons;
}