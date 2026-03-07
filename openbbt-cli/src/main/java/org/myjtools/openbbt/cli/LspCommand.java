package org.myjtools.openbbt.cli;

import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.lsp.LspApp;
import picocli.CommandLine;

@CommandLine.Command(
	name = "lsp",
	description = "Launch the LSP language server (communicates via stdio)"
)
public final class LspCommand extends AbstractCommand {

	@Override
	protected void execute() {
		OpenBBTRuntime runtime = null;
		try {
			OpenBBTContext context = getContext();
			runtime = new OpenBBTRuntime(context.configuration());
		} catch (Exception ignored) {
			// No openbbt.yaml found — start in degraded mode (structural completions only)
		}
		LspApp.launch(runtime);
	}
}
