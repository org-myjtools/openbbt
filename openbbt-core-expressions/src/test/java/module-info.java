module org.myjtools.openbbt.core.expressions.test {
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;
    requires org.assertj.core;
    requires org.hamcrest;
    requires org.myjtools.openbbt.core;
    requires org.myjtools.openbbt.core.expressions;
    requires org.myjtools.openbbt.core.datatypes;
    requires org.myjtools.openbbt.core.assertions;

    opens  org.myjtools.openbbt.core.expressions.test to org.junit.platform.commons;
}