module org.myjtools.openbbt.core.test {
    requires org.junit.jupiter.api;
    requires org.myjtools.openbbt.core;
    requires org.assertj.core;
    requires org.junit.jupiter.params;
    opens org.myjtools.openbbt.core.test.expressions to org.junit.platform.commons;
    opens org.myjtools.openbbt.core.test.contributors to org.junit.platform.commons;

}