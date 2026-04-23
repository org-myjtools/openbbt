module org.myjtools.openbbt.jsonrpc.test {
    requires org.myjtools.openbbt.core;
    requires org.myjtools.openbbt.jsonrpc;
    requires com.google.gson;
    requires org.junit.jupiter.api;
    requires org.assertj.core;
    opens org.myjtools.openbbt.jsonrpc.serve.test to org.junit.platform.commons;
}