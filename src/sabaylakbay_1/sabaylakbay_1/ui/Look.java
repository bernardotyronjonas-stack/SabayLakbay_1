package sabaylakbay_1.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;

/**
 * App-wide styling for the standard Swing parts (drop-downs, scrollbars, check boxes,
 * radio buttons, number boxes, right-click menus, tooltips, text selection).
 * Theme calls install() once, before any screen is created, so every form gets this look
 * without changing its code. Nothing here talks to the database.
 */
public final class Look {

    private static boolean installed;

    private Look() { }

    static final Color FIELD_BORDER = new Color(0xD9DEE3);
    static final Color FIELD_BORDER_HOVER = new Color(0xB8C1C9);
    static final Color FIELD_DISABLED = new Color(0xF3F5F6);
    static final Color SELECTION = new Color(0xCFE7DB);

    public static void install() {
        if (installed) return;
        installed = true;
        try {
            // Metal is the base. Our own UI classes below replace the parts that look dated.
            UIManager.setLookAndFeel(new javax.swing.plaf.metal.MetalLookAndFeel());
        } catch (Exception ignored) { }

        // one font everywhere
        FontUIResource base = new FontUIResource(Theme.font(Font.PLAIN, 13));
        for (Object key : new ArrayList<>(UIManager.getLookAndFeelDefaults().keySet())) {
            if (key != null && key.toString().endsWith(".font")) UIManager.put(key, base);
        }

        // our widget classes
        use("ComboBoxUI", ComboUI.class);
        use("ScrollBarUI", ScrollUI.class);
        use("SpinnerUI", SpinnerUI.class);

        // text: green caret, soft green selection
        for (String k : new String[]{"TextField", "PasswordField", "TextArea", "TextPane", "EditorPane", "FormattedTextField"}) {
            UIManager.put(k + ".selectionBackground", new ColorUIResource(SELECTION));
            UIManager.put(k + ".selectionForeground", new ColorUIResource(Theme.TEXT_DARK));
            UIManager.put(k + ".caretForeground", new ColorUIResource(Theme.GREEN));
            UIManager.put(k + ".inactiveForeground", new ColorUIResource(Theme.HINT));
        }

        // check boxes and radio buttons
        UIManager.put("CheckBox.icon", new CheckIcon(false));
        UIManager.put("RadioButton.icon", new CheckIcon(true));
        UIManager.put("CheckBox.focus", new ColorUIResource(new Color(0, 0, 0, 0)));
        UIManager.put("RadioButton.focus", new ColorUIResource(new Color(0, 0, 0, 0)));
        UIManager.put("CheckBox.foreground", new ColorUIResource(Theme.TEXT_DARK));
        UIManager.put("RadioButton.foreground", new ColorUIResource(Theme.TEXT_DARK));

        // lists and right-click menus
        UIManager.put("List.selectionBackground", new ColorUIResource(Theme.GREEN_LIGHT));
        UIManager.put("List.selectionForeground", new ColorUIResource(Theme.GREEN));
        UIManager.put("PopupMenu.border", new BorderUIResourceWrap(BorderFactory.createCompoundBorder(
                new RoundLine(Theme.BORDER, 10), new EmptyBorder(6, 6, 6, 6))));
        UIManager.put("PopupMenu.background", new ColorUIResource(Color.WHITE));
        for (String k : new String[]{"MenuItem", "CheckBoxMenuItem", "RadioButtonMenuItem", "Menu"}) {
            UIManager.put(k + ".background", new ColorUIResource(Color.WHITE));
            UIManager.put(k + ".foreground", new ColorUIResource(Theme.TEXT_DARK));
            UIManager.put(k + ".selectionBackground", new ColorUIResource(Theme.GREEN_LIGHT));
            UIManager.put(k + ".selectionForeground", new ColorUIResource(Theme.GREEN));
            UIManager.put(k + ".border", new BorderUIResourceWrap(new EmptyBorder(8, 12, 8, 16)));
            UIManager.put(k + ".borderPainted", Boolean.FALSE);
        }
        UIManager.put("MenuItem.acceleratorForeground", new ColorUIResource(Theme.HINT));

        // dark tooltips
        UIManager.put("ToolTip.background", new ColorUIResource(Theme.TEXT_DARK));
        UIManager.put("ToolTip.foreground", new ColorUIResource(Color.WHITE));
        UIManager.put("ToolTip.border", new BorderUIResourceWrap(new EmptyBorder(6, 10, 6, 10)));
        UIManager.put("ToolTip.font", new FontUIResource(Theme.font(Font.PLAIN, 12)));
        ToolTipManager.sharedInstance().setInitialDelay(350);

        // plain white backgrounds for anything left over
        UIManager.put("Panel.background", new ColorUIResource(Color.WHITE));
        UIManager.put("OptionPane.background", new ColorUIResource(Color.WHITE));
        UIManager.put("Viewport.background", new ColorUIResource(Color.WHITE));
        UIManager.put("ScrollPane.background", new ColorUIResource(Color.WHITE));
        UIManager.put("Separator.foreground", new ColorUIResource(Theme.BORDER));
        UIManager.put("ComboBox.background", new ColorUIResource(Color.WHITE));
        UIManager.put("ComboBox.disabledBackground", new ColorUIResource(FIELD_DISABLED));
        UIManager.put("ComboBox.disabledForeground", new ColorUIResource(Theme.HINT));
        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(Theme.GREEN_LIGHT));
        UIManager.put("ComboBox.selectionForeground", new ColorUIResource(Theme.GREEN));
    }

    private static void use(String key, Class<?> c) {
        UIManager.put(key, c.getName());
        UIManager.put(c.getName(), c);
    }

    // ================= shared painting =================

    /** Rounded field box: white, gray border, green when focused, darker on hover, gray when disabled. */
    static void paintField(Graphics g, JComponent c, boolean focused, boolean hover) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        RoundRectangle2D r = new RoundRectangle2D.Float(0.5f, 0.5f, c.getWidth() - 1, c.getHeight() - 1, 10, 10);
        g2.setColor(c.isEnabled() ? Color.WHITE : FIELD_DISABLED);
        g2.fill(r);
        if (focused && c.isEnabled()) {
            g2.setColor(new Color(11, 79, 58, 40));
            g2.setStroke(new BasicStroke(3f));
            g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, c.getWidth() - 3, c.getHeight() - 3, 9, 9));
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(Theme.GREEN);
        } else {
            g2.setColor(hover && c.isEnabled() ? FIELD_BORDER_HOVER : FIELD_BORDER);
        }
        g2.draw(r);
        g2.dispose();
    }

    /** Small "v" or "^" arrow. */
    static void chevron(Graphics2D g2, int cx, int cy, int size, boolean up, Color color) {
        Path2D p = new Path2D.Float();
        float h = size / 2f;
        if (up) { p.moveTo(cx - h, cy + h / 2); p.lineTo(cx, cy - h / 2); p.lineTo(cx + h, cy + h / 2); }
        else    { p.moveTo(cx - h, cy - h / 2); p.lineTo(cx, cy + h / 2); p.lineTo(cx + h, cy - h / 2); }
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(p);
    }

    /** Repaints the component when the mouse enters or leaves it (and its children). */
    static MouseAdapter hoverRepaint(JComponent c) {
        MouseAdapter m = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { c.putClientProperty("look.hover", true); c.repaint(); }
            @Override public void mouseExited(MouseEvent e) { c.putClientProperty("look.hover", false); c.repaint(); }
        };
        c.addMouseListener(m);
        return m;
    }

    static boolean hover(JComponent c) { return Boolean.TRUE.equals(c.getClientProperty("look.hover")); }

    // ================= drop-down (JComboBox) =================

    public static class ComboUI extends BasicComboBoxUI {

        public static ComponentUI createUI(JComponent c) { return new ComboUI(); }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            comboBox.setOpaque(false);
            comboBox.setBorder(new EmptyBorder(0, 12, 0, 6));
            comboBox.setBackground(Color.WHITE);
            comboBox.setForeground(Theme.TEXT_DARK);
            comboBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            comboBox.setMaximumRowCount(10);
            comboBox.setRenderer(new ItemRenderer());
            hoverRepaint(comboBox);
            comboBox.addPropertyChangeListener("editable", e -> fixEditor());
            fixEditor();
        }

        private void fixEditor() {
            if (comboBox.getEditor() != null && comboBox.getEditor().getEditorComponent() instanceof JTextField) {
                JTextField t = (JTextField) comboBox.getEditor().getEditorComponent();
                t.setBorder(new EmptyBorder(0, 0, 0, 0));
                t.setOpaque(false);
                t.setFont(comboBox.getFont());
                t.setForeground(Theme.TEXT_DARK);
                t.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                t.addFocusListener(new java.awt.event.FocusAdapter() {
                    @Override public void focusGained(java.awt.event.FocusEvent e) { comboBox.repaint(); }
                    @Override public void focusLost(java.awt.event.FocusEvent e) { comboBox.repaint(); }
                });
            }
        }

        @Override protected void configureEditor() { super.configureEditor(); fixEditor(); }

        @Override
        protected JButton createArrowButton() {
            JButton b = new JButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.aa(g2);
                    boolean open = comboBox.isPopupVisible();
                    chevron(g2, getWidth() / 2 - 2, getHeight() / 2, 9, open,
                            comboBox.isEnabled() ? (open ? Theme.GREEN : Theme.TEXT_GRAY) : Theme.HINT);
                    g2.dispose();
                }
            };
            b.setBorder(null);
            b.setContentAreaFilled(false);
            b.setFocusable(false);
            b.setOpaque(false);
            b.setPreferredSize(new Dimension(28, 28));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return b;
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            boolean focus = comboBox.hasFocus() || comboBox.isPopupVisible()
                    || (comboBox.isEditable() && comboBox.getEditor().getEditorComponent().hasFocus());
            paintField(g, c, focus, hover(c));
            if (!comboBox.isEditable()) paintCurrentValue(g, rectangleForCurrentValue(), false);
        }

        @Override
        public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            ListCellRenderer<Object> r = (ListCellRenderer<Object>) comboBox.getRenderer();
            Component c = r.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, false, false);
            c.setFont(comboBox.getFont());
            c.setForeground(comboBox.isEnabled() ? Theme.TEXT_DARK : Theme.HINT);
            if (c instanceof JComponent) ((JComponent) c).setOpaque(false);
            currentValuePane.paintComponent(g, c, comboBox, bounds.x, bounds.y, bounds.width, bounds.height, c instanceof JPanel);
        }

        @Override public void paintCurrentValueBackground(Graphics g, Rectangle b, boolean f) { }

        @Override
        public Dimension getMinimumSize(JComponent c) {
            Dimension d = super.getMinimumSize(c);
            return new Dimension(d.width + 8, Math.max(d.height, 38));
        }

        @Override
        protected ComboPopup createPopup() {
            BasicComboPopup p = new BasicComboPopup(comboBox) {
                @Override protected void configurePopup() {
                    super.configurePopup();
                    setBorder(BorderFactory.createCompoundBorder(new RoundLine(Theme.BORDER, 10), new EmptyBorder(4, 4, 4, 4)));
                    setBackground(Color.WHITE);
                }
                @Override protected void configureList() {
                    super.configureList();
                    list.setSelectionBackground(Theme.GREEN_LIGHT);
                    list.setSelectionForeground(Theme.GREEN);
                    list.setFixedCellHeight(-1);
                }
                @Override protected JScrollPane createScroller() {
                    JScrollPane sp = super.createScroller();
                    sp.setBorder(null);
                    return sp;
                }
                @Override public void show(Component invoker, int x, int y) { super.show(invoker, x, y + 4); }
            };
            return p;
        }

        /** One row of the drop-down list. */
        static class ItemRenderer extends DefaultListCellRenderer {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, false);
                setFont(Theme.font(sel && index >= 0 ? Font.BOLD : Font.PLAIN, 13));
                if (index < 0) {
                    setBorder(new EmptyBorder(0, 0, 0, 0));
                    setOpaque(false);
                } else {
                    setBorder(new EmptyBorder(8, 10, 8, 10));
                    setOpaque(true);
                    setBackground(sel ? Theme.GREEN_LIGHT : Color.WHITE);
                    setForeground(sel ? Theme.GREEN : Theme.TEXT_DARK);
                }
                return this;
            }
        }
    }

    // ================= thin scrollbars =================

    public static class ScrollUI extends BasicScrollBarUI {

        public static ComponentUI createUI(JComponent c) { return new ScrollUI(); }

        private boolean over;

        @Override
        protected void installListeners() {
            super.installListeners();
            scrollbar.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { over = true; scrollbar.repaint(); }
                @Override public void mouseExited(MouseEvent e) { over = false; scrollbar.repaint(); }
            });
            scrollbar.setOpaque(false);
        }

        @Override public Dimension getPreferredSize(JComponent c) {
            return scrollbar.getOrientation() == JScrollBar.VERTICAL ? new Dimension(12, 48) : new Dimension(48, 12);
        }

        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }

        private static JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            b.setBorder(null);
            return b;
        }

        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            boolean active = over || isDragging;
            g2.setColor(active ? new Color(0x9EA8B0) : new Color(0xC7CED4));
            int pad = active ? 2 : 3;
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                int w = r.width - pad * 2;
                g2.fillRoundRect(r.x + pad, r.y + 2, w, r.height - 4, w, w);
            } else {
                int h = r.height - pad * 2;
                g2.fillRoundRect(r.x + 2, r.y + pad, r.width - 4, h, h, h);
            }
            g2.dispose();
        }

        @Override protected Dimension getMinimumThumbSize() { return new Dimension(12, 36); }
    }

    // ================= number box (JSpinner) =================

    public static class SpinnerUI extends BasicSpinnerUI {

        public static ComponentUI createUI(JComponent c) { return new SpinnerUI(); }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            spinner.setOpaque(false);
            spinner.setBorder(new EmptyBorder(0, 10, 0, 4));
            hoverRepaint(spinner);
            fixEditor();
            spinner.addPropertyChangeListener("editor", e -> fixEditor());
        }

        private void fixEditor() {
            JComponent ed = spinner.getEditor();
            if (ed == null) return;
            ed.setOpaque(false);
            ed.setBorder(null);
            if (ed instanceof JSpinner.DefaultEditor) {
                JFormattedTextField t = ((JSpinner.DefaultEditor) ed).getTextField();
                t.setOpaque(false);
                t.setBorder(null);
                t.setHorizontalAlignment(JTextField.LEFT);
                t.setFont(Theme.font(Font.PLAIN, 13));
                t.setForeground(Theme.TEXT_DARK);
                t.addFocusListener(new java.awt.event.FocusAdapter() {
                    @Override public void focusGained(java.awt.event.FocusEvent e) { spinner.repaint(); }
                    @Override public void focusLost(java.awt.event.FocusEvent e) { spinner.repaint(); }
                });
            }
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            boolean focus = false;
            if (spinner.getEditor() instanceof JSpinner.DefaultEditor)
                focus = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().hasFocus();
            paintField(g, c, focus, hover(c));
            super.paint(g, c);
        }

        @Override protected Component createNextButton() { return arrow(true); }
        @Override protected Component createPreviousButton() { return arrow(false); }

        private Component arrow(boolean up) {
            JButton b = new JButton() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.aa(g2);
                    if (getModel().isRollover() && isEnabled()) {
                        g2.setColor(Theme.GREEN_LIGHT);
                        g2.fillRoundRect(2, up ? 3 : 0, getWidth() - 6, getHeight() - 3, 6, 6);
                    }
                    chevron(g2, getWidth() / 2 - 1, getHeight() / 2 + (up ? 1 : -1), 7, up,
                            isEnabled() ? (getModel().isRollover() ? Theme.GREEN : Theme.TEXT_GRAY) : Theme.HINT);
                    g2.dispose();
                }
            };
            b.setBorder(null);
            b.setContentAreaFilled(false);
            b.setFocusable(false);
            b.setRolloverEnabled(true);
            b.setPreferredSize(new Dimension(22, 14));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (up) installNextButtonListeners(b); else installPreviousButtonListeners(b);
            return b;
        }
    }

    // ================= check box / radio icon =================

    static class CheckIcon implements Icon, javax.swing.plaf.UIResource {
        private final boolean radio;
        CheckIcon(boolean radio) { this.radio = radio; }
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel m = b.getModel();
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            g2.translate(x, y);
            boolean on = m.isSelected();
            boolean enabled = b.isEnabled();
            Color fill = on ? (enabled ? Theme.GREEN : Theme.HINT) : (enabled ? Color.WHITE : FIELD_DISABLED);
            Color line = on ? fill : (m.isRollover() || m.isArmed() ? Theme.GREEN : new Color(0xC2CAD1));
            if (radio) {
                g2.setColor(Color.WHITE);
                g2.fillOval(1, 1, 16, 16);
                g2.setColor(line);
                g2.setStroke(new BasicStroke(on ? 1.6f : 1.3f));
                g2.drawOval(1, 1, 16, 16);
                if (on) { g2.setColor(fill); g2.fillOval(5, 5, 8, 8); }
            } else {
                g2.setColor(fill);
                g2.fillRoundRect(1, 1, 16, 16, 6, 6);
                g2.setColor(line);
                g2.setStroke(new BasicStroke(1.3f));
                g2.drawRoundRect(1, 1, 16, 16, 6, 6);
                if (on) {
                    Path2D p = new Path2D.Float();
                    p.moveTo(5, 9.3); p.lineTo(7.8, 12); p.lineTo(13, 6.2);
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.draw(p);
                }
            }
            g2.dispose();
        }
    }

    // ================= borders =================

    /** Thin rounded line border (used for popups). */
    static class RoundLine implements Border {
        private final Color color;
        private final int arc;
        RoundLine(Color color, int arc) { this.color = color; this.arc = arc; }
        @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
        @Override public boolean isBorderOpaque() { return false; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            g2.setColor(color);
            g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
            g2.dispose();
        }
    }

    /** Lets a normal border be used as a UIManager default. */
    static class BorderUIResourceWrap extends javax.swing.plaf.BorderUIResource {
        BorderUIResourceWrap(Border b) { super(b); }
    }
}
