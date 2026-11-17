package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import java.awt.*;

/** Shared parts of the charts: grow-in animation and number formatting. */
public abstract class ChartBase extends JComponent {

    protected float grow = 0f;   // 0..1, bars/lines grow when the chart appears
    private Timer timer;

    protected ChartBase() {
        setFont(Theme.font(Font.PLAIN, 11));
    }

    @Override
    public void addNotify() {
        super.addNotify();
        grow = 0f;
        long start = System.currentTimeMillis();
        if (timer != null) timer.stop();
        timer = new Timer(15, e -> {
            float t = Math.min(1f, (System.currentTimeMillis() - start) / 650f);
            grow = 1f - (1f - t) * (1f - t) * (1f - t);
            if (t >= 1f) ((Timer) e.getSource()).stop();
            repaint();
        });
        timer.start();
    }

    protected static final int STEPS = 5;

    /** Top of the axis: a round number that splits into 5 whole steps (e.g. 163 -> 200, 41 -> 50, 9 -> 10). */
    protected static double niceMax(double max) {
        if (max <= 0) return STEPS;
        double need = max / STEPS;
        double p = Math.pow(10, Math.floor(Math.log10(need)));
        for (double m : new double[]{1, 2, 2.5, 4, 5, 10}) {
            double step = m * p;
            if (step >= need && step == Math.floor(step)) return step * STEPS;
        }
        return Math.ceil(need) * STEPS;
    }

    protected static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format("%.1f", v);
    }

    protected static void legend(Graphics2D g, int x, int y, Color c, String text) {
        g.setColor(c);
        g.fillRoundRect(x, y - 9, 10, 10, 3, 3);
        g.setColor(Theme.TEXT_GRAY);
        g.drawString(text, x + 15, y);
    }
}
