package sabaylakbay_1.ui.components;

import sabaylakbay_1.model.MonthlyCount;
import sabaylakbay_1.ui.Theme;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Two columns per month (e.g. trips created vs completed), with grid lines and a legend. */
public class ColumnChart extends ChartBase {

    private List<MonthlyCount> data = new ArrayList<>();
    private final String firstName, secondName;
    private final Color firstColor, secondColor;

    public ColumnChart(String firstName, Color firstColor, String secondName, Color secondColor) {
        this.firstName = firstName;
        this.firstColor = firstColor;
        this.secondName = secondName;
        this.secondColor = secondColor;
        setPreferredSize(new Dimension(500, 260));
    }

    public void setData(List<MonthlyCount> d) { data = d; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        if (data.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int top = 26, bottom = 22, left = 34;
        int w = getWidth() - left, h = getHeight() - top - bottom;
        double max = niceMax(data.stream().mapToInt(m -> Math.max(m.getFirst(), m.getSecond())).max().orElse(1));

        legend(g2, left, 12, firstColor, firstName);
        legend(g2, left + fm.stringWidth(firstName) + 40, 12, secondColor, secondName);

        for (int i = 0; i <= STEPS; i++) {
            int y = top + h - h * i / STEPS;
            g2.setColor(new Color(0xEDF0EF));
            g2.drawLine(left, y, getWidth(), y);
            g2.setColor(Theme.HINT);
            String t = fmt(max * i / STEPS);
            g2.drawString(t, left - 8 - fm.stringWidth(t), y + 4);
        }
        float slot = w / (float) data.size();
        float bw = Math.min(16, slot * 0.3f);
        for (int i = 0; i < data.size(); i++) {
            MonthlyCount m = data.get(i);
            float cx = left + slot * i + slot / 2;
            float h1 = (float) (h * m.getFirst() / max * grow), h2 = (float) (h * m.getSecond() / max * grow);
            g2.setColor(firstColor);
            g2.fill(new RoundRectangle2D.Float(cx - bw - 1, top + h - h1, bw, h1 + 3, 5, 5));
            g2.setColor(secondColor);
            g2.fill(new RoundRectangle2D.Float(cx + 1, top + h - h2, bw, h2 + 3, 5, 5));
            g2.setColor(Color.WHITE);
            g2.fillRect(left, top + h + 1, getWidth(), 3);
            g2.setColor(Theme.TEXT_GRAY);
            g2.drawString(m.getMonth(), cx - fm.stringWidth(m.getMonth()) / 2f, getHeight() - 4);
        }
        g2.dispose();
    }
}
