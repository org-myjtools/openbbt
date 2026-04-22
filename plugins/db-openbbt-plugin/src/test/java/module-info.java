module org.myjtools.openbbt.plugins.db.test {

    requires org.myjtools.openbbt.plugins.db;
    requires org.myjtools.openbbt.plugins.gherkin;
    requires org.myjtools.openbbt.persistence;
    requires org.myjtools.openbbt.core;
    requires org.myjtools.imconfig;
    requires openbbt.test.support;
    requires org.junit.jupiter.api;
    requires java.sql;
    requires com.h2database;
    requires org.hsqldb;
    requires org.xerial.sqlitejdbc;
    requires duckdb.jdbc;

    opens org.myjtools.plugins.db.test to org.junit.platform.commons;

}
