package org.myjtools.openbbt.tui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import org.myjtools.openbbt.tui.ui.MainWindow;

import java.io.IOException;

public class TuiApp {

    public static void launch() throws IOException {
        var factory = new DefaultTerminalFactory();
        try (var screen = factory.createScreen()) {
            screen.startScreen();
            var gui = new MultiWindowTextGUI(
                screen,
                new DefaultWindowManager(),
                new EmptySpace(TextColor.ANSI.BLACK)
            );
            gui.addWindowAndWait(new MainWindow());
        }
    }
}