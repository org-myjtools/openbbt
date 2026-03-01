package org.myjtools.openbbt.tui.ui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialog;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import org.myjtools.openbbt.tui.mock.MockData;
import org.myjtools.openbbt.tui.model.PlanNode;

import java.nio.file.Path;
import java.util.List;

public class MainWindow extends BasicWindow {

    private enum ViewMode { PLAN, FILES }

    // ─── State ───────────────────────────────────────────────────────────────

    private ViewMode currentMode = ViewMode.PLAN;
    private volatile boolean running = false;

    // Plan view
    private final PlanNode plan;
    private final PlanTreeComponent treeComponent;
    private final DetailPanel detailPanel;
    private Panel planViewPanel;

    // Files view
    private final FileListComponent fileList;
    private final FileViewerComponent fileViewer;
    private final Label pathLabel;
    private Panel filesViewPanel;

    // Layout anchors
    private Panel viewport;
    private Label tabPlan;
    private Label tabFiles;
    private Label statusLabel;

    // ─── Construction ────────────────────────────────────────────────────────

    public MainWindow() {
        super(" OpenBBT ");
        setHints(List.of(Hint.FULL_SCREEN));

        // Plan view
        plan          = MockData.createMockPlan();
        treeComponent = new PlanTreeComponent(plan);
        detailPanel   = new DetailPanel();
        treeComponent.setOnSelectionChange(detailPanel::showNode);
        detailPanel.showNode(plan);

        // Files view
        fileList   = new FileListComponent(Path.of(".").toAbsolutePath().normalize());
        fileViewer = new FileViewerComponent();
        pathLabel  = new Label("");
        fileList.setOnDirChanged(this::onDirChanged);
        fileList.setOnFileSelected(this::onFileSelected);
        updatePathLabel();

        // Build sub-panels
        planViewPanel  = buildPlanViewPanel();
        filesViewPanel = buildFilesViewPanel();

        // Status + tab labels
        tabPlan    = new Label("");
        tabFiles   = new Label("");
        statusLabel = new Label(" Ready");

        setComponent(buildRootLayout());
        updateTabBar();
    }

    // ─── Root layout ─────────────────────────────────────────────────────────

    private Panel buildRootLayout() {
        var root = new Panel(new LinearLayout(Direction.VERTICAL));

        // Tab bar
        var tabBar = new Panel(new LinearLayout(Direction.HORIZONTAL));
        tabBar.addComponent(tabPlan);
        tabBar.addComponent(tabFiles);
        root.addComponent(tabBar);
        root.addComponent(new Separator(Direction.HORIZONTAL));

        // Viewport (swappable content area)
        viewport = new Panel(new LinearLayout(Direction.VERTICAL));
        viewport.addComponent(planViewPanel,
            LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        root.addComponent(viewport,
            LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));

        // Status bar
        root.addComponent(new Separator(Direction.HORIZONTAL));
        var statusBar = new Panel(new LinearLayout(Direction.HORIZONTAL));
        statusBar.addComponent(statusLabel);
        root.addComponent(statusBar);

        return root;
    }

    private Panel buildPlanViewPanel() {
        var panel = new Panel(new GridLayout(2));
        panel.addComponent(
            treeComponent.withBorder(Borders.singleLine(" Plan ")),
            GridLayout.createLayoutData(
                GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, false, true));
        panel.addComponent(
            detailPanel.getPanel().withBorder(Borders.singleLine(" Detail ")),
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
        var panel = (mode == ViewMode.PLAN) ? planViewPanel : filesViewPanel;
        viewport.addComponent(panel, LinearLayout.createLayoutData(LinearLayout.Alignment.Fill));
        updateTabBar();
        updateStatusBar();
    }

    private void updateTabBar() {
        boolean planActive  = currentMode == ViewMode.PLAN;
        setTab(tabPlan,  "  [1] Plan ",  planActive);
        setTab(tabFiles, "  [2] Files ", !planActive);
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
        statusLabel.setText(currentMode == ViewMode.PLAN
            ? " [↑↓] Navigate  [Enter] Expand  [R] Run  [F] Filter  [1/2] Switch view  [Q] Quit"
            : " [↑↓] Navigate  [Enter] Open  [Tab] Switch panel  [1/2] Switch view  [Q] Quit");
    }

    // ─── Input handling ──────────────────────────────────────────────────────

    @Override
    public boolean handleInput(KeyStroke key) {
        if (key.getKeyType() == KeyType.Character) {
            return switch (key.getCharacter()) {
                case 'q', 'Q' -> { close(); yield true; }
                case '1'      -> { switchMode(ViewMode.PLAN);  yield true; }
                case '2'      -> { switchMode(ViewMode.FILES); yield true; }
                case 'r', 'R' -> {
                    if (currentMode == ViewMode.PLAN) { startRun(); yield true; }
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

    // ─── Plan run simulation ─────────────────────────────────────────────────

    private void startRun() {
        if (running) { setStatus(" Already running..."); return; }
        running = true;
        plan.resetStatus();
        refreshPlanUi(" Running...");

        Thread.ofVirtual().name("run-simulation").start(() -> {
            try {
                for (var feature : plan.getChildren()) {
                    for (var scenario : feature.getChildren()) {
                        runScenario(scenario);
                    }
                }
                updateFeatureStatuses();
                refreshPlanUi(" Done");
            } finally {
                running = false;
            }
        });
    }

    private void runScenario(PlanNode scenario) {
        scenario.setStatus(PlanNode.Status.RUNNING);
        refreshPlanUi(null);

        boolean passed = true;
        boolean failedEarly = false;
        for (var step : scenario.getChildren()) {
            if (failedEarly) break;
            step.setStatus(PlanNode.Status.RUNNING);
            refreshPlanUi(null);
            sleep(120);
            boolean ok = Math.random() > 0.15;
            step.setStatus(ok ? PlanNode.Status.PASS : PlanNode.Status.FAIL);
            if (!ok) { passed = false; failedEarly = true; }
            refreshPlanUi(null);
            sleep(60);
        }
        scenario.setStatus(passed ? PlanNode.Status.PASS : PlanNode.Status.FAIL);
        refreshPlanUi(null);
    }

    private void updateFeatureStatuses() {
        for (var feature : plan.getChildren()) {
            boolean anyFail    = feature.getChildren().stream().anyMatch(s -> s.getStatus() == PlanNode.Status.FAIL);
            boolean anyPending = feature.getChildren().stream().anyMatch(s -> s.getStatus() == PlanNode.Status.PENDING);
            feature.setStatus(anyFail ? PlanNode.Status.FAIL : anyPending ? PlanNode.Status.PENDING : PlanNode.Status.PASS);
        }
        boolean anyFail    = plan.getChildren().stream().anyMatch(f -> f.getStatus() == PlanNode.Status.FAIL);
        boolean anyPending = plan.getChildren().stream().anyMatch(f -> f.getStatus() == PlanNode.Status.PENDING);
        plan.setStatus(anyFail ? PlanNode.Status.FAIL : anyPending ? PlanNode.Status.PENDING : PlanNode.Status.PASS);
    }

    // ─── Filter dialog ───────────────────────────────────────────────────────

    private void openFilterDialog() {
        String tag = TextInputDialog.showDialog(getTextGUI(), "Filter by tag", "Enter tag expression:", "");
        if (tag != null && !tag.isBlank()) {
            setStatus(" Filter: " + tag + "  (not yet connected to backend)");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void refreshPlanUi(String newStatus) {
        getTextGUI().getGUIThread().invokeLater(() -> {
            treeComponent.refresh();
            detailPanel.showNode(treeComponent.getSelectedNode());
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