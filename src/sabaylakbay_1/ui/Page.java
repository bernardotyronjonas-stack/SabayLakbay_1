package sabaylakbay_1.ui;

import sabaylakbay_1.MainDashboard;

import javax.swing.*;
import java.awt.*;

/** Base for every screen inside the main window. */
public abstract class Page extends JPanel {
    protected final MainDashboard app;

    /** Pages of MainDashboard. */
    protected Page(MainDashboard app) {
        super(new BorderLayout());
        this.app = app;
        setBackground(Theme.BG);
    }

    /** Pages of AdminDashboard (they keep their own reference to the admin dashboard). */
    protected Page() {
        this(null);
    }

    /** Called every time the page is shown. Reload data from the services here. */
    public abstract void refresh();

    /** Standard page header: big title + gray subtitle, with an optional component on the right. */
    protected JPanel header(String title, String subtitle, JComponent right) {
        JPanel h = Theme.clear(new BorderLayout());
        h.setBorder(Theme.pad(28, 32, 16, 32));
        JPanel text = Theme.vbox();
        text.add(Theme.left(Theme.title(title)));
        text.add(Theme.gap(4));
        text.add(Theme.left(Theme.subtitle(subtitle)));
        h.add(text, BorderLayout.WEST);
        if (right != null) {
            JPanel r = Theme.clear(new GridBagLayout());
            r.add(right);
            h.add(r, BorderLayout.EAST);
        }
        return h;
    }

    /** A centered gray message for empty lists. */
    protected static JComponent empty(String icon, String text) {
        JPanel p = Theme.vbox();
        p.setBorder(Theme.pad(40, 0, 40, 0));
        JLabel i = new JLabel(Icons.get(icon, 36, Theme.HINT));
        i.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel t = Theme.label(text, Font.PLAIN, 13, Theme.TEXT_GRAY);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(i);
        p.add(Theme.gap(10));
        p.add(t);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }
}
