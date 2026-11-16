package sabaylakbay_1.ui;

import sabaylakbay_1.ui.components.FadeCards;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * The ONE window of the app. It opens full screen (maximized) and every screen
 * (LoginForm, RegisterForm, MainDashboard, AdminDashboard) is shown inside it with a fade.
 *
 * Press F11 to switch between full screen (no title bar) and a normal maximized window.
 */
public final class AppWindow extends JFrame {

    private static AppWindow instance;
    private final FadeCards root = new FadeCards();
    private boolean fullScreen;

    private AppWindow() {
        setTitle("SabayLakbay");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1180, 720));
        setSize(1366, 820);
        root.setBackground(Theme.BG);
        setContentPane(root);
        setLocationRelativeTo(null);
        setExtendedState(MAXIMIZED_BOTH);

        getRootPane().registerKeyboardAction(e -> toggleFullScreen(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Opens the window with the first screen. Call once from main(). */
    public static void start(JComponent firstScreen, String title) {
        instance = new AppWindow();
        instance.root.replace(firstScreen);
        instance.setTitle(title);
        instance.setVisible(true);
    }

    /** Shows another screen with a fade, e.g. AppWindow.show(new LoginForm(), "SabayLakbay - Log In"). */
    public static void show(JComponent screen, String title) {
        if (instance == null) { start(screen, title); return; }
        instance.setTitle(title);
        instance.root.replace(screen);
        SwingUtilities.invokeLater(screen::requestFocusInWindow);
    }

    /** The window itself (used as the owner of popups). */
    public static AppWindow get() { return instance; }

    private void toggleFullScreen() {
        fullScreen = !fullScreen;
        dispose();
        setUndecorated(fullScreen);
        setExtendedState(MAXIMIZED_BOTH);
        setVisible(true);
    }
}
