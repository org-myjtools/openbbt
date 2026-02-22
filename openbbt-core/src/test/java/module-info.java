module org.myjtools.openbbt.core.test {
	requires org.junit.jupiter.api;
	requires org.myjtools.openbbt.core;
	requires org.assertj.core;
	requires org.junit.jupiter.params;
	requires org.myjtools.imconfig;
	requires com.google.common;
	requires org.myjtools.jexten;
	requires org.hamcrest;

	opens org.myjtools.openbbt.core.test to org.junit.platform.commons;
	opens org.myjtools.openbbt.core.test.backend to org.junit.platform.commons;
	opens org.myjtools.openbbt.core.test.util to org.junit.platform.commons;
	opens org.myjtools.openbbt.core.test.assertions to org.junit.platform.commons;
	opens org.myjtools.openbbt.core.test.datatypes to org.junit.platform.commons;
	opens org.myjtools.openbbt.core.test.expressions to org.junit.platform.commons;
	exports org.myjtools.openbbt.core.test.backend to org.myjtools.openbbt.core;

	provides org.myjtools.openbbt.core.contributors.StepProvider with org.myjtools.openbbt.core.test.backend.TestStepProvider;

}