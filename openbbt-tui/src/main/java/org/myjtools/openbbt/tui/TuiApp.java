package org.myjtools.openbbt.tui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import org.myjtools.openbbt.core.OpenBBTRuntime;
import org.myjtools.openbbt.core.testplan.TestPlan;
import org.myjtools.openbbt.tui.ui.MainWindow;

import java.io.IOException;

public class TuiApp {

    /** Launch in demo mode with mock data. */
    public static void launch() throws IOException {
        launch(null, null);
    }

    /** Launch connected to a real runtime and pre-built test plan. */
    public static void launch(OpenBBTRuntime runtime, TestPlan testPlan) throws IOException {
        var factory = new DefaultTerminalFactory();
        try (var screen = factory.createScreen()) {
            screen.startScreen();
            var gui = new MultiWindowTextGUI(
                screen,
                new DefaultWindowManager(),
                new EmptySpace(TextColor.ANSI.BLACK)
            );
            gui.addWindowAndWait(new MainWindow(runtime, testPlan));
        }
    }
}