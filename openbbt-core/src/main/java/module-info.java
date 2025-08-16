module org.myjtools.openbbt.core {
    requires com.github.benmanes.caffeine;
    requires org.slf4j;
    requires org.myjtools.jexten;
    requires org.myjtools.imconfig;
    requires static lombok;
    requires java.sql;
    requires com.h2database;
    requires com.github.f4b6a3.ulid;
    requires org.hamcrest;
    requires org.myjtools.openbbt.core;
    exports org.myjtools.openbbt.core;
    exports org.myjtools.openbbt.core.util;
    exports org.myjtools.openbbt.core.plan;
    exports org.myjtools.openbbt.core.expressions;
    exports org.myjtools.openbbt.core.contributors;
    exports org.myjtools.openbbt.core.persistence;
    exports org.myjtools.openbbt.core.persistence.h2;
    exports org.myjtools.openbbt.core.adapters;
    exports org.myjtools.openbbt.core.messages;
    exports org.myjtools.openbbt.core.datatypes;
    exports org.myjtools.openbbt.core.assertions;
    exports org.myjtools.openbbt.core.resources;


}