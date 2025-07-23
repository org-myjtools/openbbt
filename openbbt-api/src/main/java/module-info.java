module org.myjtools.openbbt.api {
    requires com.github.benmanes.caffeine;
    requires org.slf4j;
    requires org.myjtools.jexten;
    requires org.myjtools.imconfig;
    requires static lombok;
    exports org.myjtools.openbbt.api;
    exports org.myjtools.openbbt.api.util;
    exports org.myjtools.openbbt.api.plan;
    exports org.myjtools.openbbt.api.expressions;
    exports org.myjtools.openbbt.api.contributors;
    exports org.myjtools.openbbt.api.persistence;

}