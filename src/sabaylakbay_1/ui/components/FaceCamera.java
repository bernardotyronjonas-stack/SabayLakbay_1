package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Face check with the laptop camera (like the selfie step in other apps).
 * Shows the live camera with an oval guide and tells the traveler what to fix:
 * "Move closer", "Center your face", "Too dark"... When one face is in the oval and well lit
 * for about 2 seconds, the photo is taken by itself (or press Take photo).
 *
 * Uses OpenCV (free). Setup: see "HOW TO SET UP MYSQL.txt" > FACE CHECK.
 * The photo must come from the live camera (no picking a file), so it can be trusted.
 * If the camera can't start, it says why and shows "Try again".
 *
 *   FaceCamera cam = new FaceCamera();   ...   File photo = cam.getPhoto();   (null until taken)
 *   cam.stop() when leaving the screen (also done automatically when it's removed).
 */
public class FaceCamera extends JPanel {

    private static final int GOOD_FRAMES_TO_SHOOT = 20;     // about 2 seconds of a good face

    private BufferedImage frame;           // latest camera picture, mirrored (what the traveler sees)
    private BufferedImage rawFrame;        // the same picture not mirrored (what gets saved)
    private Rectangle face;                // face found in the frame (frame coordinates)
    private String hint = "Starting the camera...";
    private boolean good;
    private int goodFrames;
    private File photo;                    // the taken photo
    private BufferedImage taken;
    private volatile boolean running;
    private volatile int generation;     // each start() gets a new number, so an old camera loop can't interfere
    private Thread worker;
    private final RoundButton shoot = new RoundButton("Take photo").icon("check");
    private final RoundButton retake = new RoundButton("Retake", RoundButton.Style.OUTLINE);
    private final RoundButton retry = new RoundButton("Try again", RoundButton.Style.OUTLINE).small();
    private final Screen screen = new Screen();
    private Runnable onChange = () -> { };

    public FaceCamera() {
        super(new BorderLayout(0, 12));
        setOpaque(false);
        screen.setPreferredSize(new Dimension(560, 340));
        add(screen, BorderLayout.CENTER);
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(retry, BorderLayout.WEST);
        JPanel right = Theme.row(10, retake, shoot);
        bar.add(right, BorderLayout.EAST);
        add(bar, BorderLayout.SOUTH);
        shoot.setEnabled(false);
        retake.setVisible(false);
        retry.setVisible(false);
        shoot.addActionListener(e -> takePhoto());
        retake.addActionListener(e -> { photo = null; taken = null; retake.setVisible(false); shoot.setVisible(true); start(); onChange.run(); });
        retry.addActionListener(e -> { retry.setVisible(false); Cv.retry(); start(); });
    }

    /** The face photo (a JPG file), or null until one is taken. */
    public File getPhoto() { return photo; }

    /** Runs when a photo is taken or cleared. */
    public void onChange(Runnable r) { onChange = r; }

    // ================= camera =================

    /** Starts the camera (safe to call again). */
    public void start() {
        if (running || photo != null) return;
        running = true;
        goodFrames = 0;
        hint = "Starting the camera...";
        int my = ++generation;
        worker = new Thread(() -> loop(my), "face-camera");
        worker.setDaemon(true);
        worker.start();
    }

    /** Turns the camera off. */
    public void stop() {
        running = false;
    }

    @Override public void removeNotify() { stop(); super.removeNotify(); }

    private void loop(int my) {
        Object cap = null;
        try {
            if (!Cv.load()) { fail(Cv.problem); return; }
            cap = Cv.openCamera();
            if (cap == null) { fail("No camera found. Close other apps using it, then Try again."); return; }
            Object mat = Cv.newMat();
            while (running && generation == my) {
                BufferedImage img = Cv.read(cap, mat);
                if (img == null) { Thread.sleep(30); continue; }
                BufferedImage raw = img;
                SwingUtilities.invokeLater(() -> rawFrame = raw);
                img = mirror(img);
                Rectangle[] faces = Cv.findFaces(img);
                evaluate(img, faces);
                Thread.sleep(40);
            }
        } catch (Throwable ex) {
            fail("The camera stopped (" + ex.getClass().getSimpleName() + "). Press Try again.");
        } finally {
            Cv.release(cap);
            if (generation == my) running = false;
        }
    }

    /** Decides what to tell the traveler for this frame, and takes the photo when it's been good for a while. */
    private void evaluate(BufferedImage img, Rectangle[] faces) {
        int w = img.getWidth(), h = img.getHeight();
        String msg;
        boolean ok = false;
        Rectangle f = faces.length == 1 ? faces[0] : null;
        if (faces.length == 0) msg = "Looking for your face... look straight at the camera";
        else if (faces.length > 1) msg = "Only one face, please";
        else {
            double size = f.width / (double) w;
            double cx = (f.x + f.width / 2.0) / w, cy = (f.y + f.height / 2.0) / h;
            int light = brightness(img, f);
            if (size < 0.22) msg = "Move a little closer";
            else if (size > 0.62) msg = "Move back a little";
            else if (Math.abs(cx - 0.5) > 0.12 || Math.abs(cy - 0.47) > 0.15) msg = "Center your face in the oval";
            else if (light < 60) msg = "Too dark. Face a light or a window";
            else if (light > 225) msg = "Too bright. Move away from the light";
            else { msg = "Great, hold still..."; ok = true; }
        }
        goodFrames = ok ? goodFrames + 1 : 0;
        show(img, f, msg);
        boolean ready = ok;
        SwingUtilities.invokeLater(() -> {
            good = ready;
            shoot.setEnabled(ready);
            if (goodFrames >= GOOD_FRAMES_TO_SHOOT && photo == null) takePhoto();
        });
    }

    /** The camera can't be used: say why and offer Try again. */
    private void fail(String msg) {
        show(null, null, msg);
        SwingUtilities.invokeLater(() -> retry.setVisible(true));
    }

    private void show(BufferedImage img, Rectangle f, String msg) {
        SwingUtilities.invokeLater(() -> {
            if (img != null) frame = img;
            face = f;
            hint = msg;
            if (img == null && msg != null) { good = false; shoot.setEnabled(false); }
            screen.repaint();
        });
    }

    private void takePhoto() {
        if (frame == null || photo != null) return;
        try {
            File f = File.createTempFile("sabaylakbay-face-", ".jpg");
            f.deleteOnExit();
            // saved the right way round (not mirrored), so the admin can compare it with the ID
            ImageIO.write(rawFrame != null ? rawFrame : mirror(frame), "jpg", f);
            photo = f;
            taken = frame;
            stop();
            hint = "Face photo taken";
            shoot.setVisible(false);
            retake.setVisible(true);
            screen.repaint();
            onChange.run();
        } catch (Exception ex) {
            hint = "Couldn't save the photo. Try again.";
        }
    }

    private static BufferedImage mirror(BufferedImage src) {
        BufferedImage m = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = m.createGraphics();
        g.drawImage(src, src.getWidth(), 0, -src.getWidth(), src.getHeight(), null);
        g.dispose();
        return m;
    }

    /** Average brightness (0-255) inside the face box. */
    private static int brightness(BufferedImage img, Rectangle f) {
        long sum = 0; int n = 0;
        for (int y = Math.max(0, f.y); y < Math.min(img.getHeight(), f.y + f.height); y += 4)
            for (int x = Math.max(0, f.x); x < Math.min(img.getWidth(), f.x + f.width); x += 4) {
                int rgb = img.getRGB(x, y);
                sum += ((rgb >> 16 & 255) * 299 + (rgb >> 8 & 255) * 587 + (rgb & 255) * 114) / 1000;
                n++;
            }
        return n == 0 ? 0 : (int) (sum / n);
    }

    // ================= drawing =================

    /** Camera picture with the oval guide and the hint at the bottom. */
    private final class Screen extends JComponent {
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            int w = getWidth(), h = getHeight();
            Shape box = new RoundRectangle2D.Float(0, 0, w, h, 18, 18);
            g2.clip(box);           // keep the parent's clip (so it never draws outside the scroll area)
            g2.setColor(new Color(0x101828));
            g2.fill(box);
            BufferedImage img = taken != null ? taken : frame;
            if (img != null) ImageBox.drawCover(g2, img, w, h);

            // oval guide (outside the oval is darker)
            int ow = (int) (h * 0.62), oh = (int) (h * 0.80);
            Shape oval = new Ellipse2D.Float((w - ow) / 2f, h * 0.47f - oh / 2f, ow, oh);
            if (taken == null) {
                Area dark = new Area(new Rectangle(0, 0, w, h));
                dark.subtract(new Area(oval));
                g2.setColor(new Color(16, 24, 40, img == null ? 0 : 120));
                g2.fill(dark);
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                        good ? null : new float[]{10, 8}, 0));
                g2.setColor(good ? new Color(0x34D399) : new Color(255, 255, 255, 200));
                g2.draw(oval);
                if (good && goodFrames > 0) {      // ring fills up while holding still
                    float p = Math.min(1f, goodFrames / (float) GOOD_FRAMES_TO_SHOOT);
                    g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.draw(new Arc2D.Float(((Ellipse2D) oval).getBounds2D(), 90, -360 * p, Arc2D.OPEN));
                }
            }
            if (img == null) {
                Icon cam = Icons.get("user", 42, new Color(255, 255, 255, 120));
                cam.paintIcon(this, g2, w / 2 - 21, h / 2 - 50);
            }

            // hint pill at the bottom
            if (hint != null) {
                g2.setFont(Theme.font(Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                String t = hint;
                while (fm.stringWidth(t) > w - 80 && t.length() > 10) t = t.substring(0, t.length() - 2);
                int pw = fm.stringWidth(t) + (taken != null || good ? 52 : 32), ph = 34;
                int px = (w - pw) / 2, py = h - ph - 14;
                g2.setColor(taken != null || good ? new Color(0x0B4F3A) : new Color(16, 24, 40, 200));
                g2.fill(new RoundRectangle2D.Float(px, py, pw, ph, ph, ph));
                int tx = px + 16;
                if (taken != null || good) {
                    Icons.get("check", 16, Color.WHITE).paintIcon(this, g2, px + 16, py + 9);
                    tx += 22;
                }
                g2.setColor(Color.WHITE);
                g2.drawString(t, tx, py + (ph - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }

    // ================= OpenCV (loaded only if installed) =================

    /**
     * Talks to OpenCV without needing it to build the project: if the OpenCV jar isn't in Libraries,
     * the app still runs and the face step explains how to set the camera up.
     * It looks for the OpenCV files in the project's lib/opencv folder and in C:\opencv\build.
     */
    static final class Cv {
        static String problem = "The camera isn't set up on this computer yet (OpenCV, see the setup guide).";
        private static boolean loaded, tried;
        private static Object classifier;
        private static Class<?> matC, sizeC, matOfRectC, imgproc, rectC;

        /** Lets load() try again (e.g. after the OpenCV files were added). */
        static synchronized void retry() { if (!loaded) tried = false; }

        static synchronized boolean load() {
            if (tried) return loaded;
            tried = true;
            try {
                Class<?> core = Class.forName("org.opencv.core.Core");
                String lib = (String) core.getField("NATIVE_LIBRARY_NAME").get(null);     // e.g. opencv_java4130
                File dll = find(lib + ".dll", lib + ".so", "lib" + lib + ".so", "lib" + lib + ".dylib");
                if (Boolean.getBoolean("sabaylakbay.cv.skipNative")) { /* test only */ }
                else if (dll != null) System.load(dll.getAbsolutePath());
                else System.loadLibrary(lib);
                File xml = find("haarcascade_frontalface_default.xml");
                if (xml == null) {
                    problem = "Face guide file missing (haarcascade_frontalface_default.xml), see the setup guide.";
                    return false;
                }
                matC = Class.forName("org.opencv.core.Mat");
                sizeC = Class.forName("org.opencv.core.Size");
                matOfRectC = Class.forName("org.opencv.core.MatOfRect");
                rectC = Class.forName("org.opencv.core.Rect");
                imgproc = Class.forName("org.opencv.imgproc.Imgproc");
                classifier = Class.forName("org.opencv.objdetect.CascadeClassifier").getConstructor(String.class)
                        .newInstance(xml.getAbsolutePath());
                if ((Boolean) classifier.getClass().getMethod("empty").invoke(classifier)) {
                    problem = "The face guide file couldn't be read. See the setup guide.";
                    return false;
                }
                loaded = true;
            } catch (ClassNotFoundException e) {
                problem = "OpenCV isn't added to the project yet. See the setup guide.";
            } catch (Throwable e) {
                problem = "OpenCV couldn't start (" + e.getClass().getSimpleName() + "). See the setup guide.";
            }
            return loaded;
        }

        /** Looks for a file in lib/opencv, lib, the project folder and the usual OpenCV folders. */
        private static File find(String... names) {
            String[] dirs = {"lib/opencv", "lib", ".", "C:/opencv/build/java/x64", "C:/opencv/build/etc/haarcascades",
                    "C:/opencv/opencv/build/java/x64", "C:/opencv/opencv/build/etc/haarcascades",
                    System.getProperty("user.home") + "/opencv/build/java/x64",
                    System.getProperty("user.home") + "/opencv/build/etc/haarcascades"};
            for (String d : dirs)
                for (String n : names) {
                    File f = new File(d, n);
                    if (f.isFile()) return f;
                }
            return null;
        }

        static Object openCamera() throws Exception {
            Class<?> vc = Class.forName("org.opencv.videoio.VideoCapture");
            Object cap = null;
            try {   // DirectShow opens laptop cameras faster on Windows
                cap = vc.getConstructor(int.class, int.class).newInstance(0, 700);
            } catch (Throwable ignored) { }
            if (cap == null || !(Boolean) vc.getMethod("isOpened").invoke(cap)) {
                release(cap);
                cap = vc.getConstructor(int.class).newInstance(0);
            }
            if (!(Boolean) vc.getMethod("isOpened").invoke(cap)) { release(cap); return null; }
            Method set = vc.getMethod("set", int.class, double.class);
            set.invoke(cap, 3, 960.0);     // CAP_PROP_FRAME_WIDTH
            set.invoke(cap, 4, 540.0);     // CAP_PROP_FRAME_HEIGHT
            return cap;
        }

        static Object newMat() throws Exception { return matC.getConstructor().newInstance(); }

        /** Reads one camera frame as a picture (null if none yet). */
        static BufferedImage read(Object cap, Object mat) throws Exception {
            if (!(Boolean) cap.getClass().getMethod("read", matC).invoke(cap, mat)) return null;
            int rows = (Integer) matC.getMethod("rows").invoke(mat), cols = (Integer) matC.getMethod("cols").invoke(mat);
            int ch = (Integer) matC.getMethod("channels").invoke(mat);
            if (rows == 0 || cols == 0 || ch != 3) return null;
            BufferedImage img = new BufferedImage(cols, rows, BufferedImage.TYPE_3BYTE_BGR);
            byte[] data = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
            matC.getMethod("get", int.class, int.class, byte[].class).invoke(mat, 0, 0, data);
            return img;
        }

        /** Finds faces in a picture (looks at a small gray copy, so it stays fast). */
        static Rectangle[] findFaces(BufferedImage img) throws Exception {
            int sw = 320, sh = img.getHeight() * sw / img.getWidth();
            BufferedImage small = new BufferedImage(sw, sh, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g = small.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, sw, sh, null);
            g.dispose();
            byte[] px = ((DataBufferByte) small.getRaster().getDataBuffer()).getData();
            Object gray = matC.getConstructor(int.class, int.class, int.class).newInstance(sh, sw, 0);   // CV_8UC1
            matC.getMethod("put", int.class, int.class, byte[].class).invoke(gray, 0, 0, px);
            imgproc.getMethod("equalizeHist", matC, matC).invoke(null, gray, gray);
            Object found = matOfRectC.getConstructor().newInstance();
            Constructor<?> size = sizeC.getConstructor(double.class, double.class);
            classifier.getClass().getMethod("detectMultiScale", matC, matOfRectC, double.class, int.class, int.class, sizeC, sizeC)
                    .invoke(classifier, gray, found, 1.15, 5, 0, size.newInstance(40.0, 40.0), sizeC.getConstructor().newInstance());
            Object[] rects = (Object[]) matOfRectC.getMethod("toArray").invoke(found);
            double k = img.getWidth() / (double) sw;
            Rectangle[] out = new Rectangle[rects.length];
            for (int i = 0; i < rects.length; i++) {
                Object r = rects[i];
                out[i] = new Rectangle((int) (rectC.getField("x").getInt(r) * k), (int) (rectC.getField("y").getInt(r) * k),
                        (int) (rectC.getField("width").getInt(r) * k), (int) (rectC.getField("height").getInt(r) * k));
            }
            matC.getMethod("release").invoke(gray);
            return out;
        }

        static void release(Object cap) {
            if (cap == null) return;
            try { cap.getClass().getMethod("release").invoke(cap); } catch (Throwable ignored) { }
        }
    }
}
