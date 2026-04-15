package org.myjtools.openbbt.cli;

import com.google.gson.JsonObject;
import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTException;
import org.myjtools.openbbt.core.OpenBBTPluginManager;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.execution.ExecutionResult;
import org.myjtools.openbbt.core.execution.TestExecution;
import org.myjtools.openbbt.core.execution.TestPlanExecutor;
import org.myjtools.openbbt.core.persistence.TestExecutionRepository;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.core.util.Log;
import picocli.CommandLine;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@CommandLine.Command(
    name = "exec",
    description = "Install plugins, mount the test plan and execute it"
)
public final class ExecCommand extends AbstractCommand {

    private static final Log log = Log.of();

    @CommandLine.Option(
        names = {"--detach"},
        description = "Print executionID immediately and run execution in background",
        defaultValue = "false"
    )
    boolean detach;

    @CommandLine.Option(
        names = {"--json"},
        description = "Output result as JSON",
        defaultValue = "false"
    )
    boolean json;

    @Override
    protected void execute() {
        OpenBBTContext context = getContext();

        if (detach) {
            executeDetached(context);
        } else {
            executeAttached(context);
        }
    }

    private void executeAttached(OpenBBTContext context) {
        OpenBBTRuntime runtime = buildRuntime(context);
        TestPlan plan = buildPlan(context, runtime);
        TestExecution execution = new TestPlanExecutor(runtime).execute(plan.planID(), null);

        // TODO: step 4 - reports

        Optional<ExecutionResult> result = Optional.empty();
        if (execution.executionRootNodeID() != null) {
            TestExecutionRepository execRepo = runtime.getRepository(TestExecutionRepository.class);
            result = execRepo.getExecutionNodeResult(execution.executionRootNodeID());
        }
        String resultName = result.map(ExecutionResult::name).orElse("-");

        if (json) {
            JsonObject obj = new JsonObject();
            obj.addProperty("executionId", execution.executionID().toString());
            obj.addProperty("result", resultName);
            System.out.println(obj);
        } else {
            System.out.println(execution.executionID() + " " + resultName);
        }
    }

    private void executeDetached(OpenBBTContext context) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<UUID> executionIdRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        Thread bgThread = new Thread(() -> {
            try {
                OpenBBTRuntime runtime = buildRuntime(context);
                TestPlan plan = buildPlan(context, runtime);
                new TestPlanExecutor(runtime).execute(plan.planID(), id -> {
                    executionIdRef.set(id);
                    latch.countDown();
                });
                // TODO: step 4 - reports
            } catch (Throwable t) {
                errorRef.set(t);
                latch.countDown();
            }
        }, "openbbt-exec");
        bgThread.setDaemon(false);
        bgThread.start();

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OpenBBTException("Interrupted while waiting for execution to start");
        }

        if (errorRef.get() != null) {
            Throwable t = errorRef.get();
            throw new OpenBBTException("Execution failed to start: {}", t.getMessage());
        }

        // Prevent System.exit() so the background thread can finish
        MainCommand.detachModeActive = true;

        if (json) {
            JsonObject obj = new JsonObject();
            obj.addProperty("executionId", executionIdRef.get().toString());
            System.out.println(obj);
        } else {
            System.out.println(executionIdRef.get());
        }
    }

    private OpenBBTRuntime buildRuntime(OpenBBTContext context) {
        // Step 1: install plugins
        if (!context.plugins().isEmpty()) {
            OpenBBTPluginManager pluginManager = new OpenBBTPluginManager(context.configuration());
            for (String plugin : context.plugins()) {
                try {
                    boolean result = pluginManager.installPlugin(plugin);
                    if (!result) {
                        throw new OpenBBTException("Failed to install plugin {}", plugin);
                    }
                } catch (Exception e) {
                    log.error(e, "Failed to install plugin {}", plugin);
                }
            }
        }
        return new OpenBBTRuntime(context.configuration()).withProfile(profile(parent.profile));
    }

    private TestPlan buildPlan(OpenBBTContext context, OpenBBTRuntime runtime) {
        // Step 2: mount plan
        try {
            return runtime.buildTestPlan(context, getSelectedSuites());
        } catch (Exception e) {
            throw new OpenBBTException(e, "Failed to build test plan: {}", e.getMessage());
        }
    }
}
