package sabaylakbay_1.ui.components;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Round heart button to save a trip ("Saved" trips are in My Groups > Saved).
 * Click (or Enter/Space) toggles it. Used on trip cards and on the trip page.
 * Backend: isSaved, toggleSaved
 */
public class HeartButton extends JButton {

    private static final Color HEART = new Color(0xE11D48);
    private final int groupId;
    private boolean saved;
    private Runnable onChange = () -> { };

    public HeartButton(int groupId, int size) {
        this.groupId = groupId;
        // BACKEND: Backend.isSaved() reads from MySQL
        saved = Backend.isSaved(groupId, Backend.currentUserId());
        setPreferredSize(new Dimension(size, size));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        updateText();
        addActionListener(e -> toggle());
    }

    /** Runs after the heart changes (e.g. to refresh a Saved list). */
    public HeartButton onChange(Runnable r) { onChange = r; return this; }

    private void toggle() {
        try {
            // BACKEND: Backend.toggleSaved() saves to MySQL
            saved = Backend.toggleSaved(groupId, Backend.currentUserId());
            Toast.show(saved ? "Saved. Find it in My Groups > Saved." : "Removed from saved trips");
            updateText();
            repaint();
            onChange.run();
        } catch (RuntimeException ex) {
            Theme.error(this, ex.getMessage());
        }
    }

    private void updateText() {
        String t = saved ? "Remove from saved trips" : "Save this trip";
        setToolTipText(t);
        getAccessibleContext().setAccessibleName(t);
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        int s = Math.min(getWidth(), getHeight());
        float x = (getWidth() - s) / 2f, y = (getHeight() - s) / 2f;
        // soft shadow + white circle, so it shows on any photo
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fill(new Ellipse2D.Float(x, y + 1, s, s));
        g2.setColor(getModel().isRollover() ? new Color(0xFFF1F3) : Color.WHITE);
        g2.fill(new Ellipse2D.Float(x, y, s - 1, s - 1));
        if (Theme.showFocus(this)) {
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(Theme.GREEN);
            g2.draw(new Ellipse2D.Float(x + 1, y + 1, s - 3, s - 3));
        }
        int is = Math.round(s * 0.5f);
        Icon ic = Icons.get(saved ? "heartFilled" : "heart", is, saved ? HEART : Theme.TEXT_DARK);
        ic.paintIcon(this, g2, (int) (x + (s - is) / 2f), (int) (y + (s - is) / 2f + 1));
        g2.dispose();
    }
}
