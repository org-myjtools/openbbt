package org.myjtools.openbbt.core.cli;

import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTContextManager;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.PlanNodeRepositoryWriter;
import org.myjtools.openbbt.core.plan.PlanNodeID;
import org.myjtools.openbbt.core.util.Log;
import picocli.CommandLine;
import java.io.IOException;

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
		OpenBBTContextManager cm = new OpenBBTContextManager(context.configuration());
		PlanNodeID plan = cm.assembleTestPlan(context).orElse(null);
		if (plan == null) {
			log.warn("No test plan assembled");
			return;
		}
		log.info(plan.toString());
		if (detail) {
			PlanNodeRepositoryWriter writer = new PlanNodeRepositoryWriter(cm.getPlanNodeRepository());
			try {
				writer.write(plan, System.out::print);
			} catch (IOException e) {
				throw new OpenBBTException(e);
			}
		}


	}


}
