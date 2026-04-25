package org.myjtools.openbbt.cli.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.cli.MainCommand;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTFile;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.persistence.TestExecutionRepository;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.testplan.NodeType;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.core.testplan.TestPlanNode;
import org.myjtools.openbbt.core.testplan.TestProject;
import picocli.CommandLine;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeleteExecutionCommandTest {

    static final String ENV_PATH = "target/.openbbt-deleteexec";
    static final String[] BASE_ARGS = {
        "-f", "src/test/resources/openbbt.yaml",
        "-D" + OpenBBTConfig.ENV_PATH + "=" + ENV_PATH,
        "-D" + OpenBBTConfig.PERSISTENCE_MODE + "=" + OpenBBTConfig.PERSISTENCE_MODE_FILE
    };

    static String executionId;

    @BeforeAll
    static void setup() throws Exception {
        deleteDirectory(Path.of(ENV_PATH));
        new CommandLine(new MainCommand()).execute(
            "install",
            "-f", "src/test/resources/openbbt.yaml",
            "-D" + OpenBBTConfig.ENV_PATH + "=" + ENV_PATH
        );

        try (var reader = new FileReader("src/test/resources/openbbt.yaml")) {
            OpenBBTFile file = OpenBBTFile.read(reader);
            var context = file.createContext(
                Config.ofMap(Map.of(
                    OpenBBTConfig.ENV_PATH, ENV_PATH,
                    OpenBBTConfig.PERSISTENCE_MODE, OpenBBTConfig.PERSISTENCE_MODE_FILE
                )),
                List.of()
            );
            OpenBBTRuntime runtime = new OpenBBTRuntime(context.configuration());
            TestPlanRepository planRepo = runtime.getRepository(TestPlanRepository.class);
            TestExecutionRepository execRepo = runtime.getRepository(TestExecutionRepository.class);

            UUID projectId = planRepo.persistProject(new TestProject("P", null, "Org", List.of()));
            UUID root = planRepo.persistNode(new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("root"));
            TestPlan plan = planRepo.persistPlan(new TestPlan(null, projectId, Instant.now(), "h", "c", root, 0, null));
            var ex = execRepo.newExecution(plan.planID(), Instant.now(), null);
            executionId = ex.executionID().toString();
        }
    }

    @Test
    void showHelp() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("delete-execution", "--help", "--execution-id", UUID.randomUUID().toString())
        );
        assertEquals(0, exitCode);
    }

    @Test
    void deleteExecution() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("delete-execution", "--execution-id", executionId)
        );
        assertEquals(0, exitCode);
    }

    static String[] args(String... extra) {
        List<String> all = new ArrayList<>(Arrays.asList(extra));
        all.addAll(Arrays.asList(BASE_ARGS));
        return all.toArray(String[]::new);
    }

    static void deleteDirectory(Path dir) throws IOException {
        if (!dir.toFile().exists()) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }
}