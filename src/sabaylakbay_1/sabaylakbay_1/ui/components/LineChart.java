package sabaylakbay_1.ui.components;

import sabaylakbay_1.model.ChartPoint;
import sabaylakbay_1.ui.Theme;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/** Line with a soft filled area (e.g. new registrations per month). Shows the value of the last point. */
public class LineChart extends ChartBase {

    private List<ChartPoint> data = new ArrayList<>();
    private final Color color;

    public LineChart(Color color) {
        this.color = color;
        setPreferredSize(new Dimension(500, 240));
    }

    public void setData(List<ChartPoint> d) { data = d; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        if (data.size() < 2) return;
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int top = 18, bottom = 22, left = 34, right = 16;
        int w = getWidth() - left - right, h = getHeight() - top - bottom;
        double max = niceMax(data.stream().mapToDouble(ChartPoint::getValue).max().orElse(1));

        for (int i = 0; i <= STEPS; i++) {
            int y = top + h - h * i / STEPS;
            g2.setColor(new Color(0xEDF0EF));
            g2.drawLine(left, y, left + w, y);
            g2.setColor(Theme.HINT);
            String t = fmt(max * i / STEPS);
            g2.drawString(t, left - 8 - fm.stringWidth(t), y + 4);
        }
        int n = data.size();
        float[] xs = new float[n], ys = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = left + w * i / (float) (n - 1);
            ys[i] = (float) (top + h - h * data.get(i).getValue() / max * grow);
            g2.setColor(Theme.TEXT_GRAY);
            String m = data.get(i).getLabel();
            g2.drawString(m, xs[i] - fm.stringWidth(m) / 2f, getHeight() - 4);
        }
        Path2D line = new Path2D.Float();
        line.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            float mx = (xs[i - 1] + xs[i]) / 2;
            line.curveTo(mx, ys[i - 1], mx, ys[i], xs[i], ys[i]);
        }
        Path2D area = new Path2D.Float(line);
        area.lineTo(xs[n - 1], top + h);
        area.lineTo(xs[0], top + h);
        area.closePath();
        g2.setPaint(new GradientPaint(0, top, new Color(color.getRed(), color.getGreen(), color.getBlue(), 70),
                0, top + h, new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)));
        g2.fill(area);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(line);
        // last point
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Float(xs[n - 1] - 5, ys[n - 1] - 5, 10, 10));
        g2.setColor(color);
        g2.draw(new Ellipse2D.Float(xs[n - 1] - 5, ys[n - 1] - 5, 10, 10));
        g2.dispose();
    }
}
