module org.myjtools.openbbt.core.datatypes.test {
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;
    requires org.myjtools.openbbt.core;
    requires org.myjtools.openbbt.core.datatypes;
    opens org.myjtools.openbbt.core.datatypes.test to org.junit.platform.commons;
}