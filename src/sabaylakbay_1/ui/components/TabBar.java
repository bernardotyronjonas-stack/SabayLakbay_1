package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Underlined tabs like the Log In / Register tabs. */
public class TabBar extends JPanel {
    private final List<JLabel> tabs = new ArrayList<>();
    private int selected;
    private IntConsumer listener;

    public TabBar(String... names) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            JLabel l = new JLabel(names[i]) {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (idx == selected) {
                        g.setColor(Theme.GREEN);
                        g.fillRect(0, getHeight() - 3, getWidth(), 3);
                    }
                }
            };
            l.setBorder(new EmptyBorder(8, 16, 12, 16));
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            l.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { setSelected(idx); }
            });
            tabs.add(l);
            add(l);
        }
        setSelected(0);
    }

    public void onChange(IntConsumer l) { listener = l; }

    public int getSelected() { return selected; }

    public void setTitle(int index, String text) { tabs.get(index).setText(text); }

    public void setSelected(int idx) {
        selected = idx;
        for (int i = 0; i < tabs.size(); i++) {
            JLabel l = tabs.get(i);
            l.setFont(Theme.font(i == idx ? Font.BOLD : Font.PLAIN, 14));
            l.setForeground(i == idx ? Theme.TEXT_DARK : Theme.TEXT_GRAY);
        }
        repaint();
        if (listener != null) listener.accept(idx);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Theme.BORDER);
        g.fillRect(0, getHeight() - 1, getWidth(), 1);
    }
}
