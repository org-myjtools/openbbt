import org.myjtools.openbbt.core.contributors.StepProvider;
import org.myjtools.openbbt.core.contributors.SuiteAssembler;
import org.myjtools.openbbt.core.messages.MessageProvider;

module org.myjtools.openbbt.it {
	requires org.myjtools.openbbt.cli;
	requires org.myjtools.openbbt.core;
	requires org.myjtools.openbbt.persistence;
	requires org.myjtools.imconfig;
	requires org.myjtools.jexten;
	requires info.picocli;
	requires org.junit.jupiter.api;
	requires org.assertj.core;

	opens org.myjtools.openbbt.it to org.junit.platform.commons, org.myjtools.jexten, org.myjtools.openbbt.core;

	provides StepProvider    with org.myjtools.openbbt.it.TestValidationStepProvider;
	provides SuiteAssembler  with org.myjtools.openbbt.it.TestTreeSuiteAssembler;
	provides MessageProvider with org.myjtools.openbbt.it.TestValidationStepProviderMessageProvider;
}
