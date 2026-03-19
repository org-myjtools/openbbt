package org.myjtools.openbbt.cli;

import org.myjtools.openbbt.cli.serve.JsonRpcServer;
import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.OpenBBTPluginManager;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.execution.TestPlanExecutor;
import org.myjtools.openbbt.core.persistence.AttachmentRepository;
import org.myjtools.openbbt.core.persistence.TestExecutionRepository;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.core.util.Log;
import java.util.UUID;
import java.util.function.Consumer;
import picocli.CommandLine;

@CommandLine.Command(
    name = "serve",
    description = "Start OpenBBT in server mode, serving JSON-RPC 2.0 requests over stdio"
)
public final class ServeCommand extends AbstractCommand {

    private static final Log log = Log.of();

    @Override
    protected void execute() {
        OpenBBTContext context = getContext();

        // Single full-mode runtime shared by both the repository factory and the
        // exec handler. Using repositoryOnly() would open HSQLDB in read-only mode,
        // which would prevent exec from writing execution records.
        OpenBBTRuntime runtime = new OpenBBTRuntime(context.configuration());

        JsonRpcServer.ExecHandler execHandler = onExecutionCreated -> {
            if (!context.plugins().isEmpty()) {
                OpenBBTPluginManager pluginManager = new OpenBBTPluginManager(context.configuration());
                for (String plugin : context.plugins()) {
                    try {
                        pluginManager.installPlugin(plugin);
                    } catch (Exception e) {
                        log.error(e, "Failed to install plugin {}", plugin);
                    }
                }
            }
            TestPlan plan;
            try {
                plan = runtime.buildTestPlan(context);
            } catch (Exception e) {
                throw new OpenBBTException(e, "Failed to build test plan: {}", e.getMessage());
            }
            final var planId = plan.planID();
            Consumer<UUID> cb = onExecutionCreated != null
                ? id -> onExecutionCreated.accept(id, planId)
                : null;
            return new TestPlanExecutor(runtime).execute(planId, cb);
        };

        new JsonRpcServer(System.in, System.out, new JsonRpcServer.RepositoryFactory() {
            @Override public TestPlanRepository open() {
                return runtime.getRepository(TestPlanRepository.class);
            }
            @Override public TestExecutionRepository openExecution() {
                return runtime.getRepository(TestExecutionRepository.class);
            }
            @Override public AttachmentRepository openAttachment() {
                return runtime.getRepository(AttachmentRepository.class);
            }
        }, execHandler, runtime::getContributors).run();
    }
}
