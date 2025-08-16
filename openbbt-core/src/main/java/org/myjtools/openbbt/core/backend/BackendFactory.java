package org.myjtools.openbbt.core.backend;

import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.plan.NodeType;
import org.myjtools.openbbt.core.plan.PlanNode;
import org.myjtools.openbbt.core.step.StepContributor;
import org.myjtools.openbbt.core.util.Log;

public class BackendFactory {

    private static final Log log = Log.of("core");

    public Backend createBackend(PlanNode testCase) {
        if (testCase.nodeType() != NodeType.TEST_CASE) {
            throw new IllegalArgumentException("Plan node must be of type TEST_CASE");
        }
        log.debug("Creating backend for Test Case {}::'{}'",testCase.source(),testCase.displayName());
        return doCreateBackend(testCase, configuration);
    }


    private Backend doCreateBackend(PlanNode testCase) {

        Config testCaseConfig = Config.ofMap(testCase.properties());

        List<StepContributor> stepContributors = createStepContributors(
                restrictedModules,
                configuration,
                !runnableBackend
        );

        Stream<DataTypeContributor> dataTypeContributors = resolveDataTypeContributors(
                restrictedModules
        );

        WakamitiDataTypeRegistry typeRegistry = loadTypes(dataTypeContributors);
        List<RunnableStep> steps = createSteps(stepContributors, typeRegistry);
        Clock clock = Clock.systemUTC();
        if (runnableBackend) {
            return new RunnableBackend(
                    testCase,
                    configuration,
                    typeRegistry,
                    steps,
                    getSetUpOperations(stepContributors),
                    getTearDownOperations(stepContributors),
                    clock
            );
        } else {
            return new NonRunnableBackend(configuration, typeRegistry, steps);
        }
    }
}
