package org.myjtools.openbbt.cli;

import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.tui.TuiApp;
import picocli.CommandLine;

@CommandLine.Command(
	name = "tui",
	description = "Launch the interactive TUI"
)
public final class TuiCommand extends AbstractCommand {

	@Override
	protected void execute() {
		OpenBBTContext context = getContext();
		OpenBBTRuntime runtime = new OpenBBTRuntime(context.configuration()).withProfile(profile(parent.profile));
		try {
			TestPlan testPlan = runtime.buildTestPlan(context);
			TuiApp.launch(runtime, testPlan);
		} catch (Exception e) {
			throw new RuntimeException("Failed to launch TUI: " + e.getMessage(), e);
		}
	}
}