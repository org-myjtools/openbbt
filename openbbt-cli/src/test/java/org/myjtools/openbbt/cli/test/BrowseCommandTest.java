package org.myjtools.openbbt.cli.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.myjtools.imconfig.Config;
import org.myjtools.openbbt.cli.MainCommand;
import org.myjtools.openbbt.core.OpenBBTConfig;
import org.myjtools.openbbt.core.OpenBBTFile;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.testplan.TestPlan;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BrowseCommandTest {

    static String planID;
    static String rootNodeID;

    static final String[] BASE_ARGS = {
        "-f", "src/test/resources/openbbt.yaml",
        "-D" + OpenBBTConfig.ENV_PATH + "=target/.openbbt",
        "-D" + OpenBBTConfig.PERSISTENCE_MODE + "=" + OpenBBTConfig.PERSISTENCE_MODE_FILE
    };

    @BeforeAll
    static void setup() throws Exception {
        new CommandLine(new MainCommand()).execute(
            "install",
            "-f", "src/test/resources/openbbt.yaml",
            "-D" + OpenBBTConfig.ENV_PATH + "=target/.openbbt"
        );

        try (var reader = new FileReader("src/test/resources/openbbt.yaml")) {
            OpenBBTFile file = OpenBBTFile.read(reader);
            Map<String, String> params = Map.of(
                OpenBBTConfig.ENV_PATH, "target/.openbbt",
                OpenBBTConfig.RESOURCE_PATH, "src/test/resources/test-features",
                OpenBBTConfig.PERSISTENCE_MODE, OpenBBTConfig.PERSISTENCE_MODE_FILE
            );
            var context = file.createContext(
                Config.ofMap(params),
                List.of("suiteA"),
                "",
                Config.ofMap(params).append(Config.env())
            );
            OpenBBTRuntime runtime = new OpenBBTRuntime(context.configuration());
            TestPlan testPlan = runtime.buildTestPlan(context);
            planID = testPlan.planID().toString();

            // grab the first child of root to use in --node tests
            TestPlanRepository repository = runtime.getRepository(TestPlanRepository.class);
            rootNodeID = repository.getNodeChildren(testPlan.planNodeRoot())
                .findFirst()
                .map(UUID::toString)
                .orElse(testPlan.planNodeRoot().toString());
        }
    }

    @Test
    void showHelp() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("browse", "--help", "--plan", UUID.randomUUID().toString())
        );
        assertEquals(0, exitCode);
    }

    @Test
    void browseShowsPlanSummaryWithoutNodes() {
        var out = captureStdout(() -> new CommandLine(new MainCommand()).execute(
            args("browse", "--plan", planID)
        ));
        assertEquals(0, out.exitCode);
        assertTrue(out.text.contains("Plan ID:"));
        assertTrue(out.text.contains("Project ID:"));
        assertTrue(out.text.contains("Created at:"));
        assertFalse(out.text.contains("nodes"), "Default output should not contain nodes");
    }

    @Test
    void browseJsonShowsHierarchicalNodes() {
        var out = captureStdout(() -> new CommandLine(new MainCommand()).execute(
            args("browse", "--plan", planID, "--json")
        ));
        assertEquals(0, out.exitCode);
        assertTrue(out.text.contains("\"planID\""));
        assertTrue(out.text.contains("\"nodes\""));
    }

    @Test
    void browseJsonWithDepthLimitsTree() {
        var fullOut = captureStdout(() -> new CommandLine(new MainCommand()).execute(
            args("browse", "--plan", planID, "--json")
        ));
        var depthOut = captureStdout(() -> new CommandLine(new MainCommand()).execute(
            args("browse", "--plan", planID, "--json", "--depth", "1")
        ));
        assertEquals(0, fullOut.exitCode);
        assertEquals(0, depthOut.exitCode);
        assertTrue(depthOut.text.length() < fullOut.text.length(),
            "--depth 1 should produce less output than unlimited");
    }

    @Test
    void browseNodeShowsHierarchicalText() {
        var out = captureStdout(() -> new CommandLine(new MainCommand()).execute(
            args("browse", "--node", rootNodeID)
        ));
        assertEquals(0, out.exitCode);
        assertTrue(out.text.contains("[TEST_SUITE]") || out.text.contains("[TEST_PLAN]")
            || out.text.contains("[TEST_FEATURE]") || out.text.contains("[TEST_CASE]"));
    }

    @Test
    void browseNodeJsonShowsSubtree() {
        var out = captureStdout(() -> new CommandLine(new MainCommand()).execute(
            args("browse", "--node", rootNodeID, "--json")
        ));
        assertEquals(0, out.exitCode);
        assertTrue(out.text.contains("\"nodeID\""));
        assertTrue(out.text.contains("\"nodes\""));
    }

    @Test
    void browseNodeJsonWithDepthLimitsSubtree() {
        var fullOut = captureStdout(() -> new CommandLine(new MainCommand()).execute(
            args("browse", "--node", rootNodeID, "--json")
        ));
        var depthOut = captureStdout(() -> new CommandLine(new MainCommand()).execute(
            args("browse", "--node", rootNodeID, "--json", "--depth", "1")
        ));
        assertEquals(0, fullOut.exitCode);
        assertEquals(0, depthOut.exitCode);
        assertTrue(depthOut.text.length() < fullOut.text.length(),
            "--depth 1 should produce less output than unlimited");
    }

    @Test
    void browseNonExistentPlan() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("browse", "--plan", UUID.randomUUID().toString())
        );
        assertEquals(1, exitCode);
    }

    @Test
    void browseNonExistentNode() {
        int exitCode = new CommandLine(new MainCommand()).execute(
            args("browse", "--node", UUID.randomUUID().toString())
        );
        assertEquals(1, exitCode);
    }

    // --- helpers ---

    static String[] args(String... extra) {
        List<String> all = new ArrayList<>(Arrays.asList(extra));
        all.addAll(Arrays.asList(BASE_ARGS));
        return all.toArray(String[]::new);
    }

    record CapturedOutput(int exitCode, String text) {}

    interface IntSupplier { int get(); }

    static CapturedOutput captureStdout(IntSupplier action) {
        PrintStream original = System.out;
        var buffer = new ByteArrayOutputStream();
        var tee = new PrintStream(new java.io.OutputStream() {
            @Override public void write(int b) {
                buffer.write(b);
                original.write(b);
            }
            @Override public void write(byte[] b, int off, int len) {
                buffer.write(b, off, len);
                original.write(b, off, len);
            }
        });
        System.setOut(tee);
        try {
            int code = action.get();
            return new CapturedOutput(code, buffer.toString());
        } finally {
            System.setOut(original);
        }
    }
}