module org.myjtools.openbbt.api.test {
    requires org.junit.jupiter.api;
    requires org.myjtools.openbbt.api;
    requires org.assertj.core;
    requires org.junit.jupiter.params;
    opens org.myjtools.openbbt.api.test.expressions to org.junit.platform.commons;
    opens org.myjtools.openbbt.api.test.contributors to org.junit.platform.commons;
}