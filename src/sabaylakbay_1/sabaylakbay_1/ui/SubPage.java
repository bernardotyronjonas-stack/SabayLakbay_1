package sabaylakbay_1.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A detail screen that opens INSIDE the main window (no separate window):
 * it covers the page area next to the sidebar, with a "Back" bar on top.
 * Used by GroupDetailsForm, TravelerProfileForm, ReportForm, AdminUserDetailForm, ReportReviewForm.
 *
 * It keeps the same methods those forms used before (setContentPane, setVisible, dispose...),
 * so their code stays simple:   new GroupDetailsForm(app, id).setVisible(true);
 * setVisible(true) waits until the page is closed (Back, Esc or dispose()), like the old windows did.
 */
public class SubPage extends JPanel {

    private static final Deque<SubPage> OPEN = new ArrayDeque<>();
    private static JComponent area;        // the dashboard's page area (set by MainDashboard / AdminDashboard)

    private String title;
    private Container content = new JPanel();
    private JPanel host;
    private SecondaryLoop loop;
    private ComponentAdapter follow;

    /** Same arguments as the old JDialog constructors; owner and modal are not needed anymore. */
    protected SubPage(Object owner, String title, Object modal) {
        super(new BorderLayout());
        this.title = title;
    }

    /** The dashboard tells us where pages go (right of the sidebar). */
    public static void setArea(JComponent pageArea) { area = pageArea; }

    /** Closes every open detail page (used when the sidebar switches pages). */
    public static void closeAll() {
        while (!OPEN.isEmpty()) OPEN.peek().dispose();
    }

    public static boolean anyOpen() { return !OPEN.isEmpty(); }

    /** Lines open pages up again (e.g. when the message bar on top opens or closes). */
    public static void relayout() {
        for (SubPage p : OPEN) p.place();
    }

    // ===== the old JDialog methods =====
    public void setTitle(String t) { title = t; }
    public String getTitle() { return title; }
    public void setContentPane(Container c) { content = c; }
    public Container getContentPane() { return content; }
    public void setLocationRelativeTo(Component c) { }
    public void setResizable(boolean b) { }
    public void pack() { }
    @Override public void setSize(int w, int h) { }

    @Override
    public void setVisible(boolean show) {
        if (show) open();
        else dispose();
    }

    // ===== showing =====
    private void open() {
        if (host != null) return;
        AppWindow w = AppWindow.get();
        if (w == null) return;
        JLayeredPane layers = w.getLayeredPane();

        host = new JPanel(new BorderLayout());
        host.setBackground(Theme.BG);
        host.add(bar(), BorderLayout.NORTH);
        JPanel center = new JPanel(new CenterColumn());
        center.setBackground(Theme.BG);
        if (content instanceof JComponent) ((JComponent) content).setBorder(
                BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER), ((JComponent) content).getBorder()));
        center.add(content);
        host.add(center, BorderLayout.CENTER);

        // Esc = back
        host.registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        // clicks don't go through to the page underneath
        host.addMouseListener(new java.awt.event.MouseAdapter() { });

        layers.add(host, Integer.valueOf(JLayeredPane.PALETTE_LAYER + OPEN.size() + 1));
        place();
        follow = new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { place(); }
            @Override public void componentMoved(ComponentEvent e) { place(); }
        };
        if (area != null) area.addComponentListener(follow);
        w.getRootPane().addComponentListener(follow);
        OPEN.push(this);
        host.revalidate();
        host.repaint();
        SwingUtilities.invokeLater(() -> host.requestFocusInWindow());

        // wait here until the page is closed (the rest of the app keeps working)
        if (SwingUtilities.isEventDispatchThread()) {
            loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
            loop.enter();
        }
    }

    /** Covers the page area (or the whole window when there is no dashboard). */
    private void place() {
        if (host == null) return;
        AppWindow w = AppWindow.get();
        JLayeredPane layers = w.getLayeredPane();
        Rectangle r;
        if (area != null && area.isShowing()) r = SwingUtilities.convertRectangle(area.getParent(), area.getBounds(), layers);
        else r = new Rectangle(0, 0, layers.getWidth(), layers.getHeight());
        host.setBounds(r);
        host.revalidate();
    }

    public void dispose() {
        if (host == null) return;
        AppWindow w = AppWindow.get();
        if (area != null) area.removeComponentListener(follow);
        if (w != null) {
            w.getRootPane().removeComponentListener(follow);
            w.getLayeredPane().remove(host);
            w.getLayeredPane().repaint();
        }
        host = null;
        OPEN.remove(this);
        if (loop != null) { loop.exit(); loop = null; }
    }

    /** "< Back   Title" bar. */
    private JComponent bar() {
        JPanel b = new JPanel(new BorderLayout(14, 0));
        b.setBackground(Color.WHITE);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER), new EmptyBorder(12, 24, 12, 24)));
        sabaylakbay_1.ui.components.RoundButton back =
                new sabaylakbay_1.ui.components.RoundButton("Back", sabaylakbay_1.ui.components.RoundButton.Style.GHOST).icon("arrowLeft").small();
        back.addActionListener(e -> dispose());
        b.add(back, BorderLayout.WEST);
        b.add(Theme.label(title, Font.BOLD, 16, Theme.TEXT_DARK), BorderLayout.CENTER);
        return b;
    }

    /** Puts the content in a centered column (max 1040 px wide) that fills the height. */
    private static final class CenterColumn implements LayoutManager {
        public void addLayoutComponent(String n, Component c) { }
        public void removeLayoutComponent(Component c) { }
        public Dimension preferredLayoutSize(Container p) { return new Dimension(800, 600); }
        public Dimension minimumLayoutSize(Container p) { return new Dimension(300, 200); }
        public void layoutContainer(Container p) {
            if (p.getComponentCount() == 0) return;
            int w = p.getWidth(), h = p.getHeight();
            int cw = Math.min(1040, w - 48);
            p.getComponent(0).setBounds((w - cw) / 2, 20, cw, h - 40);
        }
    }
}
