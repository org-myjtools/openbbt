package org.myjtools.openbbt.tui.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.LayoutData;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.execution.PlanExecutor;
import org.myjtools.openbbt.core.execution.ExecutionResult;
import org.myjtools.openbbt.core.persistence.TestPlanRepository;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.tui.mock.MockData;
import org.myjtools.openbbt.tui.model.PlanNode;
import org.myjtools.openbbt.tui.model.PlanNodeAdapter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainWindow extends BasicWindow {

    private enum ViewMode { FILES, PLAN, EXECUTION }

    // ─── State ───────────────────────────────────────────────────────────────

    private ViewMode currentMode = ViewMode.FILES;
    private volatile boolean running = false;

    // Backend (may be null for mock/demo mode)
    private final OpenBBTRuntime runtime;
    private final TestPlan storedTestPlan;

    // Plan view
    private PlanNode planRoot;
    private final PlanTreeComponent planTreeComponent;
    private final DetailPanel planDetailPanel;
    private Panel planViewPanel;

    // Execution view
    private PlanNode execRoot;
    private final PlanTreeComponent execTreeComponent;
    private final DetailPanel execDetailPanel;
    private Panel execViewPanel;

    // Files view
    private final FileListComponent fileList;
    private final FileViewerComponent fileViewer;
    private final Label pathLabel;
    private Panel filesViewPanel;

    // Layout anchors
    private Panel viewport;
    private Label tabFiles;
    private Label tabPlan;
    private Label tabExec;
    private Label statusLabel;

    // ─── Construction ────────────────────────────────────────────────────────

    public MainWindow() {
        this(null, null);
    }

    public MainWindow(OpenBBTRuntime runtime, TestPlan testPlan) {
        super(" OpenBBT ");
        setHints(List.of(Hint.FULL_SCREEN));

        this.runtime        = runtime;
        this.storedTestPlan = testPlan;

        // Build plan root from real data or mock
        if (runtime != null && testPlan != null) {
            var repo = (TestPlanRepository) runtime.getRepository(TestPlanRepository.class);
            planRoot = PlanNodeAdapter.adaptForPlanView(testPlan.planNodeRoot(), repo);
        } else {
            planRoot = MockData.createMockPlan();
        }
        PlanNode.collapseBelow(planRoot, 2);

        // Plan view
        planTreeComponent = new PlanTreeComponent(planRoot);
        planDetailPanel   = new DetailPanel(DetailPanel.Mode.PLAN);
        planTreeComponent.setOnSelectionChange(planDetailPanel::showNode);
        planDetailPanel.showNode(planRoot);

        // Execution view (structural copy, statuses = PENDING)
        execRoot          = PlanNodeAdapter.copyForExecution(planRoot);
        PlanNode.collapseBelow(execRoot, 2);
        execTreeComponent = new PlanTreeComponent(execRoot);
        execDetailPanel   = new DetailPanel(DetailPanel.Mode.EXECUTION);
        execTreeComponent.setOnSelectionChange(execDetailPanel::showNode);
        execDetailPanel.showNode(execRoot);

        // Files view
        fileList   = new FileListComponent(Path.of(".").toAbsolutePath().normalize());
        fileViewer = new FileViewerComponent();
        pathLabel  = new Label("");
        fileList.setOnDirChanged(this::onDirChanged);
        fileList.setOnFileSelected(this::onFileSelected);
        updatePathLabel();

        // Build sub-panels
        planViewPanel  = buildPlanViewPanel();
        execViewPanel  = buildExecViewPanel();
        filesViewPanel = buildFilesViewPanel();

        // Status + tab labels
        tabFiles   = new Label("");
        tabPlan    = new Label("");
        tabExec    = new Label("");
        statusLabel = new Label(" Ready");

        setComponent(buildRootLayout());
        updateTabBar();
        updateStatusBar();
    }

    // ─── Root layout ─────────────────────────────────────────────────────────

    private static LayoutData gridFixed() {
        return GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.BEGINNING, true, false);
    }

    private static LayoutData gridGrow() {
        return GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, true, true);
    }

    private Panel buildRootLayout() {
        // Use GridLayout(1) so the viewport row can grow to fill remaining height
        var root = new Panel(new GridLayout(1));

        // Tab bar row (fixed height)
        var tabBar = new Panel(new LinearLayout(Direction.HORIZONTAL));
        tabBar.addComponent(tabFiles);
        tabBar.addComponent(tabPlan);
        tabBar.addComponent(tabExec);
        root.addComponent(tabBar, gridFixed());
        root.addComponent(new Separator(Direction.HORIZONTAL), gridFixed());

        // Viewport (grows to fill all remaining vertical space)
        viewport = new Panel(new GridLayout(1));
        viewport.addComponent(filesViewPanel, gridGrow());
        root.addComponent(viewport, gridGrow());

        // Status bar row (fixed height)
        root.addComponent(new Separator(Direction.HORIZONTAL), gridFixed());
        var statusBar = new Panel(new LinearLayout(Direction.HORIZONTAL));
        statusBar.addComponent(statusLabel);
        root.addComponent(statusBar, gridFixed());

        return root;
    }

    private Panel buildPlanViewPanel() {
        var panel = new Panel(new GridLayout(2));
        panel.addComponent(
            planTreeComponent.withBorder(Borders.singleLine(" Plan ")),
            GridLayout.createLayoutData(
                GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, false, true));
        panel.addComponent(
            planDetailPanel.getPanel().withBorder(Borders.singleLine(" Detail ")),
            GridLayout.createLayoutData(
                GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, true, true));
        return panel;
    }

    private Panel buildExecViewPanel() {
        var panel = new Panel(new GridLayout(2));
        panel.addComponent(
            execTreeComponent.withBorder(Borders.singleLine(" Execution ")),
            GridLayout.createLayoutData(
                GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, false, true));
        panel.addComponent(
            execDetailPanel.getPanel().withBorder(Borders.singleLine(" Detail ")),
            GridLayout.createLayoutData(
                GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, true, true));
        return panel;
    }

    private Panel buildFilesViewPanel() {
        var panel = new Panel(new GridLayout(2));
        panel.addComponent(
            buildFileListColumn(),
            GridLayout.createLayoutData(
                GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, false, true));
        panel.addComponent(
            fileViewer.withBorder(Borders.singleLine(" Content ")),
            GridLayout.createLayoutData(
                GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, true, true));
        return panel;
    }

    private Panel buildFileListColumn() {
        var col = new Panel(new LinearLayout(Direction.VERTICAL));
        col.addComponent(pathLabel);
        col.addComponent(
            fileList.withBorder(Borders.singleLine(" Files ")),
            LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        return col;
    }

    // ─── Mode switching ──────────────────────────────────────────────────────

    private void switchMode(ViewMode mode) {
        if (currentMode == mode) return;
        currentMode = mode;
        viewport.removeAllComponents();
        Panel panel = switch (mode) {
            case FILES     -> filesViewPanel;
            case PLAN      -> planViewPanel;
            case EXECUTION -> execViewPanel;
        };
        viewport.addComponent(panel, gridGrow());
        updateTabBar();
        updateStatusBar();
        switch (mode) {
            case FILES     -> fileList.takeFocus();
            case PLAN      -> planTreeComponent.takeFocus();
            case EXECUTION -> execTreeComponent.takeFocus();
        }
    }

    private void updateTabBar() {
        setTab(tabFiles, "  [1] Files ", currentMode == ViewMode.FILES);
        setTab(tabPlan,  "  [2] Plan ",  currentMode == ViewMode.PLAN);
        setTab(tabExec,  "  [3] Exec ",  currentMode == ViewMode.EXECUTION);
    }

    private void setTab(Label lbl, String text, boolean active) {
        lbl.setText(text);
        if (active) {
            lbl.setForegroundColor(TextColor.ANSI.CYAN_BRIGHT);
            lbl.addStyle(SGR.BOLD);
        } else {
            lbl.setForegroundColor(TextColor.ANSI.WHITE);
            lbl.removeStyle(SGR.BOLD);
        }
    }

    private void updateStatusBar() {
        statusLabel.setText(switch (currentMode) {
            case FILES     -> " [↑↓] Navigate  [Enter] Open  [Tab] Switch panel  [1/2/3] Switch view  [Q] Quit";
            case PLAN      -> " [↑↓] Navigate  [Enter] Expand  [V] Validate  [F] Filter  [1/2/3] Switch view  [Q] Quit";
            case EXECUTION -> " [↑↓] Navigate  [Enter] Expand  [R] Run  [1/2/3] Switch view  [Q] Quit";
        });
    }

    // ─── Input handling ──────────────────────────────────────────────────────

    @Override
    public boolean handleInput(KeyStroke key) {
        if (key.getKeyType() == KeyType.Character) {
            return switch (key.getCharacter()) {
                case 'q', 'Q' -> { close(); yield true; }
                case '1'      -> { switchMode(ViewMode.FILES);     yield true; }
                case '2'      -> { switchMode(ViewMode.PLAN);      yield true; }
                case '3'      -> { switchMode(ViewMode.EXECUTION); yield true; }
                case 'r', 'R' -> {
                    if (currentMode == ViewMode.EXECUTION) { startRun(); yield true; }
                    yield false;
                }
                case 'v', 'V' -> {
                    if (currentMode == ViewMode.PLAN) { rebuildPlanView(); yield true; }
                    yield false;
                }
                case 'f', 'F' -> {
                    if (currentMode == ViewMode.PLAN) { openFilterDialog(); yield true; }
                    yield false;
                }
                default -> super.handleInput(key);
            };
        }
        if (key.getKeyType() == KeyType.Escape) { close(); return true; }
        return super.handleInput(key);
    }

    // ─── File view callbacks ─────────────────────────────────────────────────

    private void onDirChanged(Path newDir) {
        updatePathLabel();
        fileViewer.clear();
    }

    private void onFileSelected(Path file) {
        fileViewer.loadFile(file);
    }

    private void updatePathLabel() {
        String path = fileList.getCurrentDir().toString();
        pathLabel.setText(" " + path);
        pathLabel.setForegroundColor(TextColor.ANSI.BLACK_BRIGHT);
    }

    // ─── Plan re-evaluation ──────────────────────────────────────────────────

    private void rebuildPlanView() {
        if (runtime == null) {
            setStatus(" Demo mode — validation is static");
            return;
        }
        setStatus(" Re-evaluating plan...");
        var repo = (TestPlanRepository) runtime.getRepository(TestPlanRepository.class);
        planRoot = PlanNodeAdapter.adaptForPlanView(storedTestPlan.planNodeRoot(), repo);
        PlanNode.collapseBelow(planRoot, 2);
        planTreeComponent.reload(planRoot);
        planDetailPanel.showNode(planTreeComponent.getSelectedNode());

        execRoot = PlanNodeAdapter.copyForExecution(planRoot);
        PlanNode.collapseBelow(execRoot, 2);
        execTreeComponent.reload(execRoot);
        execDetailPanel.showNode(execTreeComponent.getSelectedNode());
        setStatus(" Plan re-evaluated");
    }

    // ─── Plan execution ──────────────────────────────────────────────────────

    private void startRun() {
        if (running) { setStatus(" Already running..."); return; }
        running = true;
        execRoot.resetStatus();
        refreshExecUi(" Running...");

        if (runtime != null) {
            var repo     = (TestPlanRepository) runtime.getRepository(TestPlanRepository.class);
            var executor = new PlanExecutor(runtime);
            Thread.ofVirtual().name("run-executor").start(() -> {
                executor.setUp();
                try {
                    runNode(execRoot, executor, repo);
                    refreshExecUi(" Done");
                } catch (Exception e) {
                    refreshExecUi(" Error: " + e.getMessage());
                } finally {
                    executor.tearDown();
                    running = false;
                }
            });
        } else {
            Thread.ofVirtual().name("run-simulation").start(() -> {
                try {
                    runNode(execRoot, null, null);
                    refreshExecUi(" Done");
                } finally {
                    running = false;
                }
            });
        }
    }

    private boolean runNode(PlanNode node, PlanExecutor executor, TestPlanRepository repo) {
        return switch (node.getType()) {
            case STEP -> {
                executeStep(node, executor, repo);
                yield node.getStatus() == PlanNode.Status.PASS;
            }
            case SCENARIO, STEP_GROUP -> {
                node.setStatus(PlanNode.Status.RUNNING);
                refreshExecUi(null);
                List<PlanNode> leafSteps = findLeafSteps(node);
                boolean failed = false;
                for (var step : leafSteps) {
                    if (failed) {
                        step.setStatus(PlanNode.Status.SKIPPED);
                        refreshExecUi(null);
                    } else {
                        executeStep(step, executor, repo);
                        if (step.getStatus() != PlanNode.Status.PASS) failed = true;
                    }
                }
                propagateStatus(node);
                refreshExecUi(null);
                yield !failed;
            }
            case FEATURE, PROJECT -> {
                node.setStatus(PlanNode.Status.RUNNING);
                refreshExecUi(null);
                for (var child : node.getChildren()) {
                    runNode(child, executor, repo);
                }
                node.setStatus(aggregateStatus(node.getChildren()));
                refreshExecUi(null);
                yield node.getStatus() == PlanNode.Status.PASS;
            }
        };
    }

    private void executeStep(PlanNode step, PlanExecutor executor, TestPlanRepository repo) {
        step.setStatus(PlanNode.Status.RUNNING);
        refreshExecUi(null);

        if (executor == null) {
            sleep(100);
            if (Math.random() > 0.15) {
                step.setStatus(PlanNode.Status.PASS);
            } else {
                step.setStatus(PlanNode.Status.FAIL);
                step.setValidationMessage("AssertionError: expected condition was not met");
            }
            sleep(50);
            return;
        }

        UUID id;
        try {
            id = UUID.fromString(step.getId());
        } catch (IllegalArgumentException e) {
            // Mock data — simulate
            sleep(80);
            if (Math.random() > 0.15) {
                step.setStatus(PlanNode.Status.PASS);
            } else {
                step.setStatus(PlanNode.Status.FAIL);
                step.setValidationMessage("AssertionError: expected condition was not met");
            }
            return;
        }

        var coreNode = repo.getNodeData(id).orElse(null);
        if (coreNode == null) {
            step.setStatus(PlanNode.Status.UNDEFINED);
            step.setValidationMessage("Step node not found in repository");
            return;
        }

        try {
            var pair = executor.submitExecution(coreNode).get();
            step.setStatus(mapResult(pair.left()));
            if (pair.right() != null) {
                step.setValidationMessage(pair.right().getMessage());
            }
        } catch (Exception e) {
            step.setStatus(PlanNode.Status.FAIL);
            step.setValidationMessage(e.getMessage());
        }
    }

    private static List<PlanNode> findLeafSteps(PlanNode node) {
        if (!node.hasChildren()) return List.of(node);
        var result = new ArrayList<PlanNode>();
        for (var child : node.getChildren()) result.addAll(findLeafSteps(child));
        return result;
    }

    private static void propagateStatus(PlanNode node) {
        if (!node.hasChildren()) return;
        for (var child : node.getChildren()) propagateStatus(child);
        node.setStatus(aggregateStatus(node.getChildren()));
    }

    private static PlanNode.Status aggregateStatus(List<PlanNode> children) {
        if (children.isEmpty()) return PlanNode.Status.PENDING;
        boolean anyFail    = children.stream().anyMatch(c -> c.getStatus() == PlanNode.Status.FAIL);
        boolean anyUndef   = children.stream().anyMatch(c -> c.getStatus() == PlanNode.Status.UNDEFINED);
        boolean anySkip    = children.stream().anyMatch(c -> c.getStatus() == PlanNode.Status.SKIPPED);
        boolean anyPending = children.stream().anyMatch(c ->
            c.getStatus() == PlanNode.Status.PENDING || c.getStatus() == PlanNode.Status.RUNNING);
        if (anyFail)    return PlanNode.Status.FAIL;
        if (anyUndef)   return PlanNode.Status.UNDEFINED;
        if (anySkip)    return PlanNode.Status.SKIPPED;
        if (anyPending) return PlanNode.Status.PENDING;
        return PlanNode.Status.PASS;
    }

    private static PlanNode.Status mapResult(ExecutionResult result) {
        return switch (result) {
            case PASSED    -> PlanNode.Status.PASS;
            case FAILED    -> PlanNode.Status.FAIL;
            case SKIPPED   -> PlanNode.Status.SKIPPED;
            case ERROR     -> PlanNode.Status.FAIL;
            case UNDEFINED -> PlanNode.Status.UNDEFINED;
        };
    }

    // ─── Filter dialog ───────────────────────────────────────────────────────

    private void openFilterDialog() {
        String tag = TextInputDialog.showDialog(getTextGUI(), "Filter by tag", "Enter tag expression:", "");
        if (tag != null && !tag.isBlank()) {
            setStatus(" Filter: " + tag + "  (not yet connected to backend)");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void refreshExecUi(String newStatus) {
        getTextGUI().getGUIThread().invokeLater(() -> {
            execTreeComponent.refresh();
            execDetailPanel.showNode(execTreeComponent.getSelectedNode());
            if (newStatus != null) statusLabel.setText(newStatus);
        });
    }

    private void setStatus(String text) {
        getTextGUI().getGUIThread().invokeLater(() -> statusLabel.setText(text));
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
