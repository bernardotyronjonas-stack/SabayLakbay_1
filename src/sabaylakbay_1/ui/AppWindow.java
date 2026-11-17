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
        // message bar on top (slides open for a few seconds), screens below it
        JPanel frame = new JPanel(new BorderLayout());
        frame.add(sabaylakbay_1.ui.components.Toast.create(), BorderLayout.NORTH);
        frame.add(root, BorderLayout.CENTER);
        setContentPane(frame);
        setLocationRelativeTo(null);
        setExtendedState(MAXIMIZED_BOTH);

        setIconImages(appIcons());

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
        SubPage.closeAll();
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

    /** Taskbar / title bar icon: white mountain on a green rounded square. */
    private static java.util.List<Image> appIcons() {
        java.util.List<Image> list = new java.util.ArrayList<>();
        for (int size : new int[]{16, 24, 32, 48, 64, 128}) {
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            Theme.aa(g2);
            g2.setColor(Theme.GREEN);
            g2.fillRoundRect(0, 0, size, size, size / 3, size / 3);
            g2.translate(size * 0.18, size * 0.2);
            g2.scale(size * 0.64 / 34.0, size * 0.6 / 30.0);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(size < 32 ? 3.2f : 2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            java.awt.geom.Path2D m = new java.awt.geom.Path2D.Float();
            m.moveTo(2, 27); m.lineTo(12, 5); m.lineTo(19, 18); m.lineTo(24, 11); m.lineTo(32, 27); m.closePath();
            g2.draw(m);
            g2.dispose();
            list.add(img);
        }
        return list;
    }
}
