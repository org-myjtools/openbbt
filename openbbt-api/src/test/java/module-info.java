module org.myjtools.openbbt.api.test {
    requires org.junit.jupiter.api;
    requires org.myjtools.openbbt.api;
    requires org.assertj.core;
    requires org.junit.jupiter.params;
    opens org.myjtools.openbbt.api.test to org.junit.platform.commons;
}