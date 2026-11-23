package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/** The SabayLakbay mountain logo. */
public class LogoIcon extends JComponent {
    private final Color color;

    public LogoIcon(Color color, int w, int h) {
        this.color = color;
        setPreferredSize(new Dimension(w, h));
        setMaximumSize(new Dimension(w, h));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        g2.scale(getWidth() / 34.0, getHeight() / 30.0);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D m = new Path2D.Float();
        m.moveTo(2, 27); m.lineTo(12, 5); m.lineTo(19, 18); m.lineTo(24, 11); m.lineTo(32, 27); m.closePath();
        g2.draw(m);
        g2.draw(new Line2D.Float(12, 5, 16, 27));
        g2.dispose();
    }
}
