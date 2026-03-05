import org.myjtools.openbbt.plugins.rest.RestConfigProvider;
import org.myjtools.openbbt.plugins.rest.RestMessageProvider;
import org.myjtools.openbbt.plugins.rest.RestStepProvider;

module org.myjtools.openbbt.plugins.rest {

    requires org.myjtools.jexten;
    requires org.myjtools.openbbt.core;
	requires org.myjtools.imconfig;
	requires rest.assured;

	provides org.myjtools.openbbt.core.extensions.StepProvider with RestStepProvider;
    provides org.myjtools.openbbt.core.extensions.ConfigProvider with RestConfigProvider;
    provides org.myjtools.openbbt.core.messages.MessageProvider with RestMessageProvider;

    exports org.myjtools.openbbt.plugins.rest;
    opens org.myjtools.openbbt.plugins.rest to org.myjtools.jexten;

}