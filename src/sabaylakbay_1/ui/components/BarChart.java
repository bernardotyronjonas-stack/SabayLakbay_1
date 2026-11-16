package sabaylakbay_1.ui.components;

import sabaylakbay_1.model.ChartPoint;
import sabaylakbay_1.ui.Theme;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Horizontal bars with the label on the left and the value at the end (e.g. most visited places). */
public class BarChart extends ChartBase {

    private List<ChartPoint> data = new ArrayList<>();
    private final Color color;
    private final String unit;

    public BarChart(Color color, String unit) {
        this.color = color;
        this.unit = unit;
        setPreferredSize(new Dimension(400, 280));
    }

    public void setData(List<ChartPoint> d) { data = d; repaint(); }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, Math.max(120, data.size() * 34 + 10));
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (data.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int labelW = 0;
        for (ChartPoint p : data) labelW = Math.max(labelW, fm.stringWidth(p.getLabel()));
        labelW += 14;
        double max = data.stream().mapToDouble(ChartPoint::getValue).max().orElse(1);
        int valueW = fm.stringWidth(fmt(max) + " " + unit) + 12;
        int barArea = Math.max(40, getWidth() - labelW - valueW);
        int rowH = Math.max(24, Math.min(36, getHeight() / data.size()));
        int barH = 14;
        for (int i = 0; i < data.size(); i++) {
            ChartPoint p = data.get(i);
            int y = i * rowH + (rowH - barH) / 2;
            g2.setColor(Theme.TEXT_DARK);
            g2.drawString(p.getLabel(), 0, y + barH - 2);
            g2.setColor(new Color(0xEEF2F0));
            g2.fill(new RoundRectangle2D.Float(labelW, y, barArea, barH, barH, barH));
            float w = (float) (barArea * (p.getValue() / max) * grow);
            Color c = i == 0 ? color : blend(color, Color.WHITE, Math.min(0.55f, i * 0.09f));
            g2.setColor(c);
            g2.fill(new RoundRectangle2D.Float(labelW, y, Math.max(barH, w), barH, barH, barH));
            g2.setColor(Theme.TEXT_GRAY);
            g2.drawString(fmt(p.getValue()) + " " + unit, labelW + Math.max(barH, w) + 8, y + barH - 2);
        }
        g2.dispose();
    }

    private static Color blend(Color a, Color b, float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
