package sabaylakbay_1.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Line icons drawn in code (no image files needed).
 * Usage: Icons.get("calendar", 16, Theme.TEXT_GRAY)
 */
public final class Icons {

    private Icons() { }

    public static Icon get(String name, int size, Color color) {
        return new VectorIcon(name, size, color);
    }

    private static class VectorIcon implements Icon {
        private final String name;
        private final int size;
        private final Color color;

        VectorIcon(String name, int size, Color color) {
            this.name = name;
            this.size = size;
            this.color = color;
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            g2.translate(x, y);
            g2.scale(size / 24.0, size / 24.0);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            draw(g2, name);
            g2.dispose();
        }
    }

    private static void line(Graphics2D g, double x1, double y1, double x2, double y2) {
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void dot(Graphics2D g, double cx, double cy, double r) {
        g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
    }

    private static Path2D star() {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? 10 : 4.3;
            double a = Math.toRadians(-90 + i * 36);
            double px = 12 + r * Math.cos(a), py = 12.8 + r * Math.sin(a);
            if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
        }
        p.closePath();
        return p;
    }

    private static Path2D heart() {
        Path2D p = new Path2D.Double();
        p.moveTo(12, 20);
        p.curveTo(5, 15, 2.5, 11.5, 2.5, 8.5);
        p.curveTo(2.5, 5.5, 4.8, 3.5, 7.5, 3.5);
        p.curveTo(9.5, 3.5, 11, 4.5, 12, 6);
        p.curveTo(13, 4.5, 14.5, 3.5, 16.5, 3.5);
        p.curveTo(19.2, 3.5, 21.5, 5.5, 21.5, 8.5);
        p.curveTo(21.5, 11.5, 19, 15, 12, 20);
        p.closePath();
        return p;
    }

    private static void draw(Graphics2D g, String name) {
        Path2D p = new Path2D.Double();
        switch (name) {
            case "home":
                p.moveTo(3, 11); p.lineTo(12, 3.5); p.lineTo(21, 11);
                p.moveTo(5.5, 9.5); p.lineTo(5.5, 20.5); p.lineTo(18.5, 20.5); p.lineTo(18.5, 9.5);
                p.moveTo(10, 20.5); p.lineTo(10, 15); p.lineTo(14, 15); p.lineTo(14, 20.5);
                g.draw(p);
                break;
            case "users":
                g.draw(new Ellipse2D.Double(5.5, 4.5, 7, 7));
                p.moveTo(2.5, 20); p.curveTo(2.5, 15.8, 5.3, 14, 9, 14); p.curveTo(12.7, 14, 15.5, 15.8, 15.5, 20);
                p.moveTo(15, 4.8); p.curveTo(18.5, 5.5, 18.5, 10.5, 15, 11.2);
                p.moveTo(17.5, 14.3); p.curveTo(20, 15, 21.5, 16.8, 21.5, 20);
                g.draw(p);
                break;
            case "plusCircle":
                g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                line(g, 12, 8, 12, 16);
                line(g, 8, 12, 16, 12);
                break;
            case "plus":
                line(g, 12, 5, 12, 19);
                line(g, 5, 12, 19, 12);
                break;
            case "user":
                g.draw(new Ellipse2D.Double(8, 3.5, 8, 8));
                p.moveTo(4, 21); p.curveTo(4, 16.5, 7.5, 14, 12, 14); p.curveTo(16.5, 14, 20, 16.5, 20, 21);
                g.draw(p);
                break;
            case "calendar":
                g.draw(new RoundRectangle2D.Double(3.5, 5, 17, 15.5, 4, 4));
                line(g, 3.5, 10, 20.5, 10);
                line(g, 8, 3, 8, 7);
                line(g, 16, 3, 16, 7);
                break;
            case "pin":
                p.moveTo(12, 21.5); p.curveTo(7, 16, 5, 13, 5, 9.5); p.curveTo(5, 5.6, 8.1, 2.5, 12, 2.5);
                p.curveTo(15.9, 2.5, 19, 5.6, 19, 9.5); p.curveTo(19, 13, 17, 16, 12, 21.5); p.closePath();
                g.draw(p);
                g.draw(new Ellipse2D.Double(9.5, 7, 5, 5));
                break;
            case "search":
                g.draw(new Ellipse2D.Double(4, 4, 13, 13));
                line(g, 15.5, 15.5, 20.5, 20.5);
                break;
            case "heart":
                g.draw(heart());
                break;
            case "heartFilled":
                g.fill(heart());
                g.draw(heart());
                break;
            case "filter":
                line(g, 4, 7, 20, 7);
                line(g, 4, 12, 20, 12);
                line(g, 4, 17, 20, 17);
                dot(g, 9, 7, 2.3);
                dot(g, 15, 12, 2.3);
                dot(g, 7.5, 17, 2.3);
                break;
            case "chevronDown":
                p.moveTo(6, 9); p.lineTo(12, 15); p.lineTo(18, 9);
                g.draw(p);
                break;
            case "chevronRight":
                p.moveTo(9, 6); p.lineTo(15, 12); p.lineTo(9, 18);
                g.draw(p);
                break;
            case "arrowRight":
                line(g, 4, 12, 20, 12);
                p.moveTo(14, 6); p.lineTo(20, 12); p.lineTo(14, 18);
                g.draw(p);
                break;
            case "arrowLeft":
                line(g, 4, 12, 20, 12);
                p.moveTo(10, 6); p.lineTo(4, 12); p.lineTo(10, 18);
                g.draw(p);
                break;
            case "chat":
                p.moveTo(7, 17); p.lineTo(4.5, 17); p.quadTo(3, 17, 3, 15.5); p.lineTo(3, 5.5); p.quadTo(3, 4, 4.5, 4);
                p.lineTo(19.5, 4); p.quadTo(21, 4, 21, 5.5); p.lineTo(21, 15.5); p.quadTo(21, 17, 19.5, 17);
                p.lineTo(12, 17); p.lineTo(7, 21); p.closePath();
                g.draw(p);
                break;
            case "bell":
                p.moveTo(6, 16.5); p.lineTo(6, 11); p.curveTo(6, 7.5, 8.7, 5, 12, 5); p.curveTo(15.3, 5, 18, 7.5, 18, 11);
                p.lineTo(18, 16.5);
                g.draw(p);
                line(g, 4, 16.5, 20, 16.5);
                line(g, 12, 3, 12, 5);
                Path2D q = new Path2D.Double();
                q.moveTo(10, 19.5); q.quadTo(12, 21.8, 14, 19.5);
                g.draw(q);
                break;
            case "suitcase":
                g.draw(new RoundRectangle2D.Double(3, 7, 18, 13, 4, 4));
                p.moveTo(9, 7); p.lineTo(9, 5); p.quadTo(9, 4, 10, 4); p.lineTo(14, 4); p.quadTo(15, 4, 15, 5); p.lineTo(15, 7);
                g.draw(p);
                line(g, 3, 12.5, 21, 12.5);
                break;
            case "star":
                g.draw(star());
                break;
            case "starFilled":
                g.fill(star());
                g.draw(star());
                break;
            case "logout":
                p.moveTo(14, 4); p.lineTo(19, 4); p.lineTo(19, 20); p.lineTo(14, 20);
                p.moveTo(8, 7); p.lineTo(3, 12); p.lineTo(8, 17);
                g.draw(p);
                line(g, 3, 12, 14, 12);
                break;
            case "flag":
                line(g, 5, 21, 5, 3.5);
                p.moveTo(5, 4); p.lineTo(18.5, 4); p.lineTo(15.5, 8.5); p.lineTo(18.5, 13); p.lineTo(5, 13);
                g.draw(p);
                break;
            case "block":
                g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                line(g, 6.5, 6.5, 17.5, 17.5);
                break;
            case "clock":
                g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                p.moveTo(12, 7); p.lineTo(12, 12); p.lineTo(15.5, 14);
                g.draw(p);
                break;
            case "wallet":
                g.draw(new RoundRectangle2D.Double(2.5, 6, 19, 13, 4, 4));
                p.moveTo(5, 6); p.lineTo(16, 3); p.lineTo(17, 6);
                g.draw(p);
                dot(g, 17, 12.5, 1.4);
                break;
            case "bus":
                g.draw(new RoundRectangle2D.Double(5, 3, 14, 15, 4, 4));
                line(g, 5, 11, 19, 11);
                dot(g, 8.5, 14.5, 1.2);
                dot(g, 15.5, 14.5, 1.2);
                line(g, 7.5, 18, 7.5, 21);
                line(g, 16.5, 18, 16.5, 21);
                break;
            case "note":
                g.draw(new RoundRectangle2D.Double(5, 3, 14, 18, 3, 3));
                line(g, 8.5, 8, 15.5, 8);
                line(g, 8.5, 12, 15.5, 12);
                line(g, 8.5, 16, 12.5, 16);
                break;
            case "edit":
                p.moveTo(4, 20); p.lineTo(4.6, 16); p.lineTo(15.5, 5); p.lineTo(19, 8.5); p.lineTo(8, 19.4); p.closePath();
                g.draw(p);
                line(g, 13, 7.5, 16.5, 11);
                break;
            case "x":
                line(g, 6, 6, 18, 18);
                line(g, 18, 6, 6, 18);
                break;
            case "check":
                p.moveTo(5, 12.5); p.lineTo(10, 17.5); p.lineTo(19, 7);
                g.draw(p);
                break;
            case "send":
                p.moveTo(3, 11); p.lineTo(21, 3); p.lineTo(14, 21); p.lineTo(11, 13); p.closePath();
                g.draw(p);
                line(g, 11, 13, 21, 3);
                break;
            case "megaphone":
                p.moveTo(3, 10); p.lineTo(3, 14); p.lineTo(6.5, 14); p.lineTo(14, 19); p.lineTo(14, 5); p.lineTo(6.5, 10); p.closePath();
                g.draw(p);
                Path2D w = new Path2D.Double();
                w.moveTo(17.5, 9); w.quadTo(19.5, 12, 17.5, 15);
                g.draw(w);
                break;
            case "info":
                g.draw(new Ellipse2D.Double(3, 3, 18, 18));
                line(g, 12, 11, 12, 16.5);
                dot(g, 12, 7.8, 1.2);
                break;
            case "trash":
                line(g, 4, 7, 20, 7);
                p.moveTo(6.5, 7); p.lineTo(7.5, 20); p.lineTo(16.5, 20); p.lineTo(17.5, 7);
                p.moveTo(9.5, 7); p.lineTo(9.5, 4.5); p.lineTo(14.5, 4.5); p.lineTo(14.5, 7);
                g.draw(p);
                break;
            case "map":
                p.moveTo(3, 6); p.lineTo(9, 3.5); p.lineTo(15, 6); p.lineTo(21, 3.5); p.lineTo(21, 18); p.lineTo(15, 20.5);
                p.lineTo(9, 18); p.lineTo(3, 20.5); p.closePath();
                g.draw(p);
                line(g, 9, 3.5, 9, 18);
                line(g, 15, 6, 15, 20.5);
                break;
            case "image":
                g.draw(new RoundRectangle2D.Double(3, 4, 18, 16, 4, 4));
                dot(g, 8.5, 9, 1.6);
                p.moveTo(3.5, 18); p.lineTo(9.5, 12.5); p.lineTo(13.5, 16); p.lineTo(16, 13.5); p.lineTo(20.5, 17.5);
                g.draw(p);
                break;
            case "mountain":
                p.moveTo(2, 20); p.lineTo(9, 6); p.lineTo(13.5, 14); p.lineTo(16, 10); p.lineTo(22, 20); p.closePath();
                g.draw(p);
                line(g, 9, 6, 11.5, 20);
                break;
            case "chart":
                line(g, 4, 20.5, 20.5, 20.5);
                g.draw(new RoundRectangle2D.Double(5.5, 12, 3.5, 8.5, 1.5, 1.5));
                g.draw(new RoundRectangle2D.Double(10.5, 7, 3.5, 13.5, 1.5, 1.5));
                g.draw(new RoundRectangle2D.Double(15.5, 3.5, 3.5, 17, 1.5, 1.5));
                break;
            case "grid":
                g.draw(new RoundRectangle2D.Double(3.5, 3.5, 7, 7, 2, 2));
                g.draw(new RoundRectangle2D.Double(13.5, 3.5, 7, 7, 2, 2));
                g.draw(new RoundRectangle2D.Double(3.5, 13.5, 7, 7, 2, 2));
                g.draw(new RoundRectangle2D.Double(13.5, 13.5, 7, 7, 2, 2));
                break;
            case "download":
                line(g, 12, 3.5, 12, 15);
                p.moveTo(7, 10.5); p.lineTo(12, 15.5); p.lineTo(17, 10.5);
                p.moveTo(4, 17); p.lineTo(4, 20.5); p.lineTo(20, 20.5); p.lineTo(20, 17);
                g.draw(p);
                break;
            case "shield":
                p.moveTo(12, 2.8); p.lineTo(19.5, 5.8); p.lineTo(19.5, 11.5); p.curveTo(19.5, 16.5, 16, 19.8, 12, 21.3);
                p.curveTo(8, 19.8, 4.5, 16.5, 4.5, 11.5); p.lineTo(4.5, 5.8); p.closePath();
                g.draw(p);
                Path2D ck = new Path2D.Double();
                ck.moveTo(8.5, 12); ck.lineTo(11, 14.5); ck.lineTo(15.5, 9.5);
                g.draw(ck);
                break;
            case "trend":
                p.moveTo(3.5, 17.5); p.lineTo(9.5, 11.5); p.lineTo(13, 15); p.lineTo(20.5, 7.5);
                p.moveTo(15, 7.5); p.lineTo(20.5, 7.5); p.lineTo(20.5, 13);
                g.draw(p);
                break;
            case "lock":
                g.draw(new RoundRectangle2D.Double(4.5, 10.5, 15, 10, 3, 3));
                p.moveTo(8, 10.5); p.lineTo(8, 7.5); p.curveTo(8, 2.5, 16, 2.5, 16, 7.5); p.lineTo(16, 10.5);
                g.draw(p);
                break;
            default:
                g.draw(new Ellipse2D.Double(4, 4, 16, 16));
        }
    }
}
