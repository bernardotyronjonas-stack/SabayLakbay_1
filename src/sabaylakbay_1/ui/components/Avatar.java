package sabaylakbay_1.ui.components;

import sabaylakbay_1.model.User;
import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/** Round profile picture. Shows the photo if avatarPath loads, otherwise colored initials. */
public class Avatar extends JComponent {
    private static final Color[] COLORS = {
            new Color(0x0E7C66), new Color(0xD9822B), new Color(0x2B6CB0), new Color(0x9B3D8F),
            new Color(0xC2453A), new Color(0x4F7A28), new Color(0x6B5BD6), new Color(0x0F8FA6)};
    private User user;
    private final int size;

    public Avatar(User user, int size) {
        this.user = user;
        this.size = size;
        Dimension d = new Dimension(size, size);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
    }

    /** Shows another person (or the same person after a new photo). */
    public void setUser(User u) { user = u; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        BufferedImage img = user == null ? null : ImageBox.load(user.getAvatarPath());
        if (img != null) {
            ImageBox.paintRounded(g2, size, size, size, true, b -> ImageBox.drawCover(b, img, size, size));
        } else {
            int id = user == null ? 0 : Math.abs(user.getId());
            g2.setColor(COLORS[id % COLORS.length]);
            g2.fill(new Ellipse2D.Float(0, 0, size, size));
            g2.setColor(Color.WHITE);
            g2.setFont(Theme.font(Font.BOLD, Math.max(9, size * 2 / 5)));
            String s = user == null ? "?" : user.getInitials();
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, (size - fm.stringWidth(s)) / 2, (size - fm.getHeight()) / 2 + fm.getAscent());
        }
        g2.dispose();
    }
}
