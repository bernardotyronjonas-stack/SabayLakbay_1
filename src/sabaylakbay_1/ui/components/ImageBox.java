package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Shows a photo scaled to fill the box with rounded corners.
 * imagePath can be a resource in the project ("/images/mayon.jpg") or a file on the computer ("C:\...\photo.jpg").
 * If the image is missing it shows a colored placeholder, so the app never crashes because of a picture.
 */
public class ImageBox extends JComponent {

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();
    private static final Color[][] PALETTES = {
            {new Color(0x1D6B57), new Color(0x7DB9A0)}, {new Color(0x1F5F8B), new Color(0x7CC3D8)},
            {new Color(0x8A5A2B), new Color(0xE0B27A)}, {new Color(0x3E5C2F), new Color(0xA9C77E)},
            {new Color(0x4B3F8C), new Color(0xA79BE0)}, {new Color(0x0F6F7A), new Color(0x8ED1C6)}};

    private String path;
    private final String seed;
    private final int radius;
    private final boolean roundBottom;

    public ImageBox(String path, String seed, int radius, boolean roundBottom) {
        this.path = path;
        this.seed = seed == null ? "" : seed;
        this.radius = radius;
        this.roundBottom = roundBottom;
    }

    public void setPath(String p) { path = p; repaint(); }

    /** Loads (and caches) an image. Returns null if it can't be found or read. */
    public static BufferedImage load(String path) {
        if (path == null || path.isBlank()) return null;
        if (CACHE.containsKey(path)) return CACHE.get(path);
        BufferedImage img = null;
        try {
            URL url = ImageBox.class.getResource(path.startsWith("/") ? path : "/" + path);
            if (url != null) img = ImageIO.read(url);
            if (img == null) {
                File f = new File(path);
                if (f.isFile()) img = ImageIO.read(f);
            }
        } catch (Exception ignored) { }
        CACHE.put(path, img);
        return img;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        int w = getWidth(), h = getHeight();
        BufferedImage img = load(path);
        paintRounded(g2, w, h, radius, roundBottom, b -> {
            if (img != null) drawCover(b, img, w, h);
            else drawPlaceholder(b, seed, w, h);
        });
        g2.dispose();
    }

    public static void drawCover(Graphics2D g, BufferedImage img, int w, int h) {
        double s = Math.max((double) w / img.getWidth(), (double) h / img.getHeight());
        int iw = (int) Math.ceil(img.getWidth() * s), ih = (int) Math.ceil(img.getHeight() * s);
        g.drawImage(img, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
    }

    /** Colored gradient with simple mountains, used when there is no photo yet. */
    public static void drawPlaceholder(Graphics2D g, String seed, int w, int h) {
        Color[] p = PALETTES[Math.abs(seed.hashCode()) % PALETTES.length];
        g.setPaint(new GradientPaint(0, 0, p[1], 0, h, p[0]));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(255, 255, 255, 60));
        g.fillOval((int) (w * 0.7), (int) (h * 0.15), (int) (h * 0.22), (int) (h * 0.22));
        Path2D back = new Path2D.Float();
        back.moveTo(0, h); back.lineTo(w * 0.25, h * 0.45); back.lineTo(w * 0.45, h * 0.7);
        back.lineTo(w * 0.62, h * 0.35); back.lineTo(w, h * 0.8); back.lineTo(w, h); back.closePath();
        g.setColor(new Color(255, 255, 255, 45));
        g.fill(back);
        Path2D front = new Path2D.Float();
        front.moveTo(0, h); front.lineTo(0, h * 0.8); front.lineTo(w * 0.3, h * 0.62); front.lineTo(w * 0.55, h * 0.85);
        front.lineTo(w * 0.8, h * 0.6); front.lineTo(w, h * 0.75); front.lineTo(w, h); front.closePath();
        g.setColor(new Color(0, 0, 0, 45));
        g.fill(front);
        if (h >= 80) Icons.get("image", 22, new Color(255, 255, 255, 170)).paintIcon(null, g, 12, h - 34);
    }

    /** Paints with smooth rounded corners (works with Windows display scaling). */
    public static void paintRounded(Graphics2D g, int w, int h, float arc, boolean roundBottom, Consumer<Graphics2D> painter) {
        if (w <= 0 || h <= 0) return;
        double scale = g.getTransform().getScaleX();
        int bw = (int) Math.ceil(w * scale), bh = (int) Math.ceil(h * scale);
        BufferedImage buf = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D b = buf.createGraphics();
        Theme.aa(b);
        b.scale(scale, scale);
        Shape shape = new RoundRectangle2D.Float(0, 0, w, h, arc, arc);
        if (!roundBottom) {
            Area a = new Area(shape);
            a.add(new Area(new Rectangle2D.Float(0, h / 2f, w, h / 2f)));
            shape = a;
        }
        painter.accept(b);
        // cut away everything outside the rounded shape
        Area outside = new Area(new Rectangle2D.Float(-1, -1, w + 2, h + 2));
        outside.subtract(new Area(shape));
        b.setComposite(AlphaComposite.DstOut);
        b.setColor(Color.BLACK);
        b.fill(outside);
        b.dispose();
        g.drawImage(buf, 0, 0, w, h, null);
    }
}
