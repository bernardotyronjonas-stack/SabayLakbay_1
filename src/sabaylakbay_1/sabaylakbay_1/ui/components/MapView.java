package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * Interactive map (OpenStreetMap tiles) with one pin.
 *   Drag = move, mouse wheel / + - = zoom, click = drop the pin (when editable).
 *
 *   MapView map = new MapView(true);
 *   map.setPin(13.2572, 123.6856);   map.getLat();  map.getLng();   map.hasPin();
 *
 * Needs internet. Downloaded map pieces are saved in the "cache/tiles" folder of the project
 * so they load fast next time.
 */
public class MapView extends JComponent {

    // Map picture source. Uses the free OpenStreetMap server (shows "(c) OpenStreetMap contributors" as required).
    private static final String TILES = System.getProperty("sabaylakbay.tiles", "https://tile.openstreetmap.org/{z}/{x}/{y}.png");
    private static final String AGENT = "SabayLakbay/1.0 (student travel app)";
    private static final int TILE = 256, MIN_ZOOM = 3, MAX_ZOOM = 18;
    private static final File DISK = new File("cache/tiles");

    // shared by every map in the app
    private static final Map<String, BufferedImage> memory = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> e) { return size() > 400; }
    });
    private static final Set<String> loading = ConcurrentHashMap.newKeySet();
    private static final Map<String, Long> failed = new ConcurrentHashMap<>();
    private static final ExecutorService pool = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "map-tiles");
        t.setDaemon(true);
        return t;
    });
    private static final Set<MapView> live = Collections.newSetFromMap(new WeakHashMap<>());

    private final boolean editable;
    private int zoom = 5;
    private double cx, cy;                 // center, in world pixels at the current zoom
    private Double pinLat, pinLng;
    private final List<Runnable> listeners = new ArrayList<>();
    private Point pressAt, lastDrag;
    private boolean dragged;
    private int hoverButton = -1;          // 0 = zoom in, 1 = zoom out
    private boolean offline;

    public MapView(boolean editable) {
        this.editable = editable;
        setPreferredSize(new Dimension(600, 320));
        setView(12.3, 122.6, 5);           // the whole Philippines
        synchronized (live) { live.add(this); }
        setCursor(Cursor.getPredefinedCursor(editable ? Cursor.CROSSHAIR_CURSOR : Cursor.HAND_CURSOR));
        setToolTipText(null);

        MouseAdapter m = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                pressAt = lastDrag = e.getPoint();
                dragged = false;
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (lastDrag == null) return;
                if (pressAt.distance(e.getPoint()) > 4) dragged = true;
                cx -= e.getX() - lastDrag.x;
                cy -= e.getY() - lastDrag.y;
                clampCenter();
                lastDrag = e.getPoint();
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                repaint();
            }
            @Override public void mouseReleased(MouseEvent e) {
                setCursor(Cursor.getPredefinedCursor(editable ? Cursor.CROSSHAIR_CURSOR : Cursor.HAND_CURSOR));
                if (dragged || pressAt == null) return;
                int b = buttonAt(e.getPoint());
                if (b == 0) { zoomAt(getWidth() / 2, getHeight() / 2, 1); return; }
                if (b == 1) { zoomAt(getWidth() / 2, getHeight() / 2, -1); return; }
                if (editable && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
                    double[] ll = toLatLng(e.getX(), e.getY());
                    setPin(ll[0], ll[1]);
                }
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && buttonAt(e.getPoint()) < 0) zoomAt(e.getX(), e.getY(), 1);
            }
            @Override public void mouseWheelMoved(MouseWheelEvent e) {
                if (!editable && !e.isControlDown()) {
                    // on a page that scrolls: let the page scroll, Ctrl + wheel zooms the map
                    Container parent = getParent();
                    if (parent != null) parent.dispatchEvent(SwingUtilities.convertMouseEvent(MapView.this, e, parent));
                    return;
                }
                zoomAt(e.getX(), e.getY(), e.getWheelRotation() < 0 ? 1 : -1);
            }
            @Override public void mouseMoved(MouseEvent e) {
                int b = buttonAt(e.getPoint());
                if (b != hoverButton) { hoverButton = b; repaint(); }
                setCursor(Cursor.getPredefinedCursor(b >= 0 ? Cursor.HAND_CURSOR : editable ? Cursor.CROSSHAIR_CURSOR : Cursor.HAND_CURSOR));
            }
            @Override public void mouseExited(MouseEvent e) { hoverButton = -1; repaint(); }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
        addMouseWheelListener(m);
    }

    // ================= public =================

    /** Moves the map to this place. */
    public void setView(double lat, double lng, int z) {
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z));
        cx = lngToX(lng, zoom);
        cy = latToY(lat, zoom);
        repaint();
    }

    /** Puts the pin here (and keeps it in view). */
    public void setPin(double lat, double lng) {
        pinLat = lat;
        pinLng = lng;
        Point2D p = toScreen(lat, lng);
        if (p.getX() < 30 || p.getY() < 40 || p.getX() > getWidth() - 30 || p.getY() > getHeight() - 30) {
            cx = lngToX(lng, zoom);
            cy = latToY(lat, zoom);
        }
        repaint();
        for (Runnable r : listeners) r.run();
    }

    public void clearPin() {
        pinLat = pinLng = null;
        repaint();
        for (Runnable r : listeners) r.run();
    }

    public boolean hasPin() { return pinLat != null; }
    public Double getLat() { return pinLat; }
    public Double getLng() { return pinLng; }
    public int getZoom() { return zoom; }

    /** Runs whenever the pin is dropped, moved or removed. */
    public void onPinChange(Runnable r) { listeners.add(r); }

    // ================= map math (Web Mercator, same as Google / OpenStreetMap) =================

    private static double lngToX(double lng, int z) { return (lng + 180) / 360 * TILE * (1 << z); }

    private static double latToY(double lat, int z) {
        double r = Math.toRadians(Math.max(-85, Math.min(85, lat)));
        return (1 - Math.log(Math.tan(r) + 1 / Math.cos(r)) / Math.PI) / 2 * TILE * (1 << z);
    }

    private static double xToLng(double x, int z) { return x / (TILE * (1 << z)) * 360 - 180; }

    private static double yToLat(double y, int z) {
        double n = Math.PI - 2 * Math.PI * y / (TILE * (1 << z));
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    private double[] toLatLng(int sx, int sy) {
        double wx = cx - getWidth() / 2.0 + sx, wy = cy - getHeight() / 2.0 + sy;
        return new double[]{yToLat(wy, zoom), xToLng(wx, zoom)};
    }

    private Point2D toScreen(double lat, double lng) {
        return new Point2D.Double(lngToX(lng, zoom) - cx + getWidth() / 2.0, latToY(lat, zoom) - cy + getHeight() / 2.0);
    }

    private void zoomAt(int sx, int sy, int dir) {
        int nz = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom + dir));
        if (nz == zoom) return;
        double[] ll = toLatLng(sx, sy);                 // keep the point under the mouse still
        zoom = nz;
        cx = lngToX(ll[1], zoom) - (sx - getWidth() / 2.0);
        cy = latToY(ll[0], zoom) - (sy - getHeight() / 2.0);
        clampCenter();
        repaint();
    }

    private void clampCenter() {
        double world = TILE * (double) (1 << zoom);
        cy = Math.max(0, Math.min(world, cy));
        cx = ((cx % world) + world) % world;
    }

    // ================= drawing =================

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.aa(g2);
        int w = getWidth(), h = getHeight();
        Shape round = new RoundRectangle2D.Float(0, 0, w, h, 14, 14);
        Shape oldClip = g2.getClip();
        g2.clip(round);         // keep the parent's clip
        g2.setColor(new Color(0xE9EEF0));
        g2.fillRect(0, 0, w, h);

        // tiles
        int n = 1 << zoom;
        double left = cx - w / 2.0, top = cy - h / 2.0;
        int tx0 = (int) Math.floor(left / TILE), ty0 = (int) Math.floor(top / TILE);
        int tx1 = (int) Math.floor((left + w) / TILE), ty1 = (int) Math.floor((top + h) / TILE);
        int missing = 0, failedCount = 0, total = 0;
        for (int ty = ty0; ty <= ty1; ty++) {
            if (ty < 0 || ty >= n) continue;
            for (int tx = tx0; tx <= tx1; tx++) {
                int wrapX = ((tx % n) + n) % n;
                int px = (int) Math.round(tx * TILE - left), py = (int) Math.round(ty * TILE - top);
                total++;
                BufferedImage img = tile(zoom, wrapX, ty);
                if (img != null) {
                    g2.drawImage(img, px, py, TILE, TILE, null);
                } else {
                    missing++;
                    if (failed.containsKey(zoom + "/" + wrapX + "/" + ty)) failedCount++;
                    BufferedImage parent = parentTile(zoom, wrapX, ty);     // blurry bigger piece while loading
                    if (parent != null) {
                        int sub = TILE / 2;
                        int ox = (wrapX % 2) * sub, oy = (ty % 2) * sub;
                        g2.drawImage(parent, px, py, px + TILE, py + TILE, ox, oy, ox + sub, oy + sub, null);
                    } else {
                        g2.setColor(new Color(0xDDE4E7));
                        g2.drawRect(px, py, TILE, TILE);
                    }
                }
            }
        }
        offline = total > 0 && failedCount == total;

        // pin
        if (pinLat != null) {
            Point2D p = toScreen(pinLat, pinLng);
            drawPin(g2, (float) p.getX(), (float) p.getY());
        }

        // zoom buttons
        drawZoomButton(g2, 0, "+");
        drawZoomButton(g2, 1, "\u2212");

        // attribution (required by OpenStreetMap)
        g2.setFont(Theme.font(Font.PLAIN, 10));
        String credit = "\u00a9 OpenStreetMap contributors";
        FontMetrics fm = g2.getFontMetrics();
        int cw = fm.stringWidth(credit) + 10;
        g2.setColor(new Color(255, 255, 255, 210));
        g2.fillRect(w - cw, h - 16, cw, 16);
        g2.setColor(Theme.TEXT_GRAY);
        g2.drawString(credit, w - cw + 5, h - 4);

        if (offline) {
            String msg = "Can't load the map. Check your internet connection.";
            g2.setFont(Theme.font(Font.BOLD, 12));
            fm = g2.getFontMetrics();
            int mw = fm.stringWidth(msg) + 28;
            g2.setColor(new Color(255, 255, 255, 235));
            g2.fill(new RoundRectangle2D.Float((w - mw) / 2f, h / 2f - 18, mw, 36, 18, 18));
            g2.setColor(Theme.TEXT_DARK);
            g2.drawString(msg, (w - mw) / 2 + 14, h / 2 + 5);
        } else if (missing > 0) {
            g2.setFont(Theme.font(Font.PLAIN, 11));
            g2.setColor(new Color(255, 255, 255, 220));
            g2.fill(new RoundRectangle2D.Float(10, h - 30, 92, 20, 10, 10));
            g2.setColor(Theme.TEXT_GRAY);
            g2.drawString("Loading map...", 18, h - 16);
        }

        g2.setClip(oldClip);
        g2.setColor(Theme.BORDER);
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, 14, 14));
        g2.dispose();
    }

    private static void drawPin(Graphics2D g2, float x, float y) {
        g2.setColor(new Color(0, 0, 0, 45));
        g2.fill(new Ellipse2D.Float(x - 7, y - 3, 14, 6));
        Path2D pin = new Path2D.Float();
        pin.moveTo(x, y);
        pin.curveTo(x - 4, y - 10, x - 13, y - 16, x - 13, y - 26);
        pin.curveTo(x - 13, y - 34, x - 7, y - 40, x, y - 40);
        pin.curveTo(x + 7, y - 40, x + 13, y - 34, x + 13, y - 26);
        pin.curveTo(x + 13, y - 16, x + 4, y - 10, x, y);
        pin.closePath();
        g2.setColor(Theme.ORANGE);
        g2.fill(pin);
        g2.setColor(new Color(0xB8440A));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(pin);
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Float(x - 5, y - 31, 10, 10));
    }

    private Rectangle buttonBounds(int i) { return new Rectangle(getWidth() - 44, 12 + i * 38, 32, 32); }

    private int buttonAt(Point p) {
        for (int i = 0; i < 2; i++) if (buttonBounds(i).contains(p)) return i;
        return -1;
    }

    private void drawZoomButton(Graphics2D g2, int i, String text) {
        Rectangle r = buttonBounds(i);
        g2.setColor(new Color(0, 0, 0, 30));
        g2.fillRoundRect(r.x, r.y + 1, r.width, r.height, 9, 9);
        g2.setColor(hoverButton == i ? Theme.GREEN_LIGHT : Color.WHITE);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 9, 9);
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 9, 9);
        g2.setColor(Theme.GREEN);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int mx = r.x + r.width / 2, my = r.y + r.height / 2;
        g2.drawLine(mx - 6, my, mx + 6, my);
        if (i == 0) g2.drawLine(mx, my - 6, mx, my + 6);
    }

    // ================= tile loading =================

    private static BufferedImage parentTile(int z, int x, int y) {
        return z > 0 ? memory.get((z - 1) + "/" + (x / 2) + "/" + (y / 2)) : null;
    }

    private static BufferedImage tile(int z, int x, int y) {
        String key = z + "/" + x + "/" + y;
        BufferedImage img = memory.get(key);
        if (img != null) return img;
        Long when = failed.get(key);
        if (when != null && System.currentTimeMillis() - when < 20000) return null;   // try again after 20 s
        if (loading.add(key)) pool.submit(() -> load(z, x, y, key));
        return null;
    }

    private static void load(int z, int x, int y, String key) {
        try {
            File f = new File(DISK, key + ".png");
            BufferedImage img = null;
            if (f.isFile() && System.currentTimeMillis() - f.lastModified() < 30L * 24 * 3600 * 1000) img = ImageIO.read(f);
            if (img == null) {
                String url = TILES.replace("{z}", String.valueOf(z)).replace("{x}", String.valueOf(x)).replace("{y}", String.valueOf(y));
                URLConnection c = URI.create(url).toURL().openConnection();
                c.setRequestProperty("User-Agent", AGENT);
                c.setConnectTimeout(8000);
                c.setReadTimeout(10000);
                if (c instanceof HttpURLConnection && ((HttpURLConnection) c).getResponseCode() != 200)
                    throw new java.io.IOException("HTTP " + ((HttpURLConnection) c).getResponseCode());
                try (InputStream in = c.getInputStream()) {
                    byte[] bytes = in.readAllBytes();
                    img = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                    if (img != null && url.startsWith("http")) {
                        try { f.getParentFile().mkdirs(); java.nio.file.Files.write(f.toPath(), bytes); } catch (Exception ignored) { }
                    }
                }
            }
            if (img == null) throw new java.io.IOException("not an image");
            memory.put(key, img);
            failed.remove(key);
        } catch (Exception e) {
            failed.put(key, System.currentTimeMillis());
        } finally {
            loading.remove(key);
            SwingUtilities.invokeLater(() -> {
                synchronized (live) { for (MapView m : live) if (m.isShowing()) m.repaint(); }
            });
        }
    }

    // ================= Google Maps =================

    /** Opens Google Maps in the browser with a pin at this place. */
    public static void openInGoogleMaps(Component parent, double lat, double lng) {
        browse(parent, "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lng);
    }

    /** Opens Google Maps directions from the user's location to this place. */
    public static void openDirections(Component parent, double lat, double lng) {
        browse(parent, "https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng);
    }

    private static void browse(Component parent, String url) {
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception e) {
            Theme.error(parent, "Couldn't open the browser. Here is the link:\n" + url);
        }
    }
}
