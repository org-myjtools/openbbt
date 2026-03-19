package org.myjtools.openbbt.test;

import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTContext;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.execution.TestExecution;
import org.myjtools.openbbt.core.execution.TestPlanExecutor;
import org.myjtools.openbbt.core.testplan.TagExpression;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.core.testplan.TestProject;
import org.myjtools.openbbt.core.testplan.TestSuite;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test fixture injected by {@link OpenBBTExtension} into JUnit 5 test methods.
 * <p>
 * Holds the resolved feature directory and temp directory, accepts plugin-specific
 * configuration, and drives a full {@link TestPlanExecutor} run when {@link #execute()}
 * is called.
 *
 * <pre>{@code
 * @Test
 * @FeatureDir("get-200")
 * void myTest(JUnitOpenBBTPlan plan) {
 *     plan.withConfig("rest.baseURL", "http://localhost:" + port)
 *         .execute()
 *         .assertAllPassed();
 * }
 * }</pre>
 */
public class JUnitOpenBBTPlan {

    private final Path featureDirPath;
    private final Path tempDir;
    private final Map<String, String> extraConfig = new LinkedHashMap<>();

    JUnitOpenBBTPlan(Path featureDirPath, Path tempDir) {
        this.featureDirPath = featureDirPath;
        this.tempDir = tempDir;
    }

    public JUnitOpenBBTPlan withConfig(String key, String value) {
        extraConfig.put(key, value);
        return this;
    }

    public JUnitOpenBBTResult execute() {
        Map<String, String> configMap = new LinkedHashMap<>();
        configMap.put(OpenBBTConfig.ENV_PATH,         tempDir.toString());
        configMap.put(OpenBBTConfig.PERSISTENCE_MODE, OpenBBTConfig.PERSISTENCE_MODE_FILE);
        configMap.put(OpenBBTConfig.PERSISTENCE_FILE, tempDir.resolve("test.db").toString());
        configMap.put(OpenBBTConfig.RESOURCE_PATH,    featureDirPath.toString());
        configMap.put(OpenBBTConfig.RESOURCE_FILTER,  "**/*");
        configMap.putAll(extraConfig);

        Config config = Config.ofMap(configMap);
        OpenBBTRuntime runtime = new OpenBBTRuntime(config);

        String suiteName = featureDirPath.getFileName().toString();
        TestSuite suite = new TestSuite(suiteName, "", TagExpression.EMPTY);
        TestProject project = new TestProject("OpenBBT Test", "", "", List.of(suite));
        OpenBBTContext context = new OpenBBTContext(project, config, List.of(suiteName), "", List.of());

        TestPlan plan = runtime.buildTestPlan(context);
        TestExecution execution = new TestPlanExecutor(runtime).execute(plan.planID());

        return new JUnitOpenBBTResult(runtime, plan, execution);
    }
}
