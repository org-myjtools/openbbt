package org.myjtools.openbbt.cli;

import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.persistence.TestPlanRepositoryWriter;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.core.util.Log;
import picocli.CommandLine;

@CommandLine.Command(
	name = "plan",
	description = "Analyze the test plan"
)
public final class PlanCommand extends AbstractCommand {

	private static final Log log = Log.of();

	@CommandLine.Option(
		names = {"--detail"},
		description = "Show detailed analysis of the test plan",
		defaultValue = "false"
	)
	boolean detail;


	@Override
	protected void execute() {

		OpenBBTContext context = getContext();
		OpenBBTRuntime runtime = new OpenBBTRuntime(context.configuration());
		try {
			TestPlan testPlan = runtime.buildTestPlan(context);
			log.info("{}", testPlan.planID());
			if (detail) {
				TestPlanRepositoryWriter writer = new TestPlanRepositoryWriter(
						runtime.getRepository(TestPlanRepository.class)
				);
				writer.write(testPlan.planNodeRoot(), System.out::print);
			}
		} catch (Exception e) {
			log.warn("No test plan assembled");
		}
	}


}
