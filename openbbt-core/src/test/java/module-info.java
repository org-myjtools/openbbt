import org.myjtools.openbbt.core.contributors.StepProvider;
import org.myjtools.openbbt.core.messages.MessageProvider;

module org.myjtools.openbbt.core.test {
	requires org.myjtools.jexten;
	requires org.myjtools.imconfig;
	requires org.myjtools.openbbt.core;
	requires org.junit.jupiter.api;
	requires org.junit.jupiter.params;
	requires org.assertj.core;
	requires com.google.common;
	requires org.hamcrest;
	requires java.xml.crypto;

	opens org.myjtools.openbbt.core.test to org.junit.platform.commons;
	opens org.myjtools.openbbt.core.test.backend to org.junit.platform.commons;
	opens org.myjtools.openbbt.core.test.util to org.junit.platform.commons;
	opens org.myjtools.openbbt.core.test.assertions to org.junit.platform.commons;
	opens org.myjtools.openbbt.core.test.datatypes to org.junit.platform.commons;
	opens org.myjtools.openbbt.core.test.expressions to org.junit.platform.commons;
	opens org.myjtools.openbbt.core.test.docgen to org.junit.platform.commons;
	opens org.myjtools.openbbt.core.test.messages to org.junit.platform.commons;

	exports org.myjtools.openbbt.core.test.backend to org.myjtools.openbbt.core, org.myjtools.jexten;

	provides StepProvider with org.myjtools.openbbt.core.test.backend.TestStepProvider;
	provides MessageProvider with org.myjtools.openbbt.core.test.backend.TestStepProviderMessageProvider;

}