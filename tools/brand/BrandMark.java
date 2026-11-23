import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Previews the brand from BrandGeometry: the launcher icon on both candidate tiles at every size
 * down to 16 px, the one-colour themed icon, the lockups, and the palette with WCAG contrast.
 * Run from the repository root.
 *
 * Args: outDir
 */
public class BrandMark extends BrandGeometry {
    // Interface tones of carmine, and the canvas-only next-frame onion skin.
    static final Color MUTED = new Color(0x775652);
    static final Color MUTED_LIGHT = new Color(0xF5DDDA);
    static final Color ONION_NEXT = new Color(0x2E6BD1);

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        BufferedImage sheet = new BufferedImage(1500, 520, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        hints(g);
        g.setColor(new Color(0xE9E6DF));
        g.fillRect(0, 0, 1500, 520);
        int row = 20;
        for (Scheme s : new Scheme[]{ON_PAPER}) {
            g.drawImage(masked(icon(512, s)), 20, row, 220, 220, null);
            int x = 270;
            for (int size : new int[]{96, 48, 32, 24, 16}) {
                g.drawImage(masked(icon(size * 4, s)), x, row + 20, size, size, null);
                // The same icon on a dark home screen.
                g.setColor(new Color(0x111111));
                g.fillRect(x - 6, row + 130, size + 12, size + 12);
                g.drawImage(masked(icon(size * 4, s)), x, row + 136, size, size, null);
                x += size + 30;
            }
            row += 240;
        }
        // Android 13 themed icons: one shape, tinted by the system.
        g.drawImage(masked(themed(512, new Color(0x1F2A44), new Color(0xD8E2FF))), 20, 290, 110, 110, null);
        g.drawImage(masked(themed(512, new Color(0xD8E2FF), new Color(0x1F2A44))), 150, 290, 110, 110, null);
        drawLockup(g, 300, 270, PAPER, GRAPHITE, ON_PAPER);
        drawLockup(g, 900, 270, GRAPHITE, PAPER, ON_PAPER);
        ImageIO.write(sheet, "png", new File(out, "sheet.png"));
        ImageIO.write(icon(512, ON_PAPER), "png", new File(out, "icon-paper.png"));
        BufferedImage palette = palette();
        ImageIO.write(palette, "png", new File(out, "palette.png"));
        BufferedImage board = new BufferedImage(1500, 960, BufferedImage.TYPE_INT_RGB);
        Graphics2D b = board.createGraphics();
        b.drawImage(sheet, 0, 0, null);
        b.setColor(Color.WHITE);
        b.fillRect(0, 520, 1500, 440);
        b.drawImage(palette, 150, 530, null);
        b.dispose();
        ImageIO.write(board, "png", new File(out, "brand-board.png"));
    }

    static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    /** What a launcher shows of an adaptive icon: the central 72 of 108 dp, in a squircle. */
    static BufferedImage masked(BufferedImage icon) {
        int s = icon.getWidth();
        double inset = s * 18 / 108.0;
        BufferedImage m = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = m.createGraphics();
        hints(g);
        g.setClip(new RoundRectangle2D.Double(0, 0, s, s, s * 0.46, s * 0.46));
        g.drawImage(icon, (int) -inset, (int) -inset, (int) (s + 2 * inset), (int) (s + 2 * inset), null);
        g.dispose();
        return m;
    }

    /** The full 108 dp adaptive canvas, as the launcher receives it. */
    static BufferedImage icon(int size, Scheme s) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        hints(g);
        g.scale(size / 512.0, size / 512.0);
        g.setColor(s.tile());
        g.fillRect(0, 0, 512, 512);
        for (Piece p : mark(s)) {
            g.setColor(p.colour());
            g.fill(fit(p.shape(), LAUNCHER_SCALE));
        }
        g.dispose();
        return img;
    }

    static BufferedImage themed(int size, Color tile, Color ink) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        hints(g);
        g.scale(size / 512.0, size / 512.0);
        g.setColor(tile);
        g.fillRect(0, 0, 512, 512);
        g.setColor(ink);
        g.fill(fit(monochrome(), LAUNCHER_SCALE));
        g.dispose();
        return img;
    }

    static void drawLockup(Graphics2D g, int x, int y, Color background, Color ink, Scheme scheme) {
        g.setColor(background);
        g.fillRoundRect(x, y, 580, 220, 24, 24);
        g.drawImage(masked(icon(512, scheme)), x + 30, y + 50, 120, 120, null);
        Wordmark w = wordmark();
        double scale = 370 / w.width();
        AffineTransform saved = g.getTransform();
        g.translate(x + 180, y + 110 - w.height() * scale / 2);
        g.scale(scale, scale);
        g.setColor(ink);
        g.fill(w.letters());
        g.setColor(ink.getRed() > 128 ? CARMINE_LIGHT : CARMINE);
        g.fill(w.dots());
        g.setTransform(saved);
    }

    // ---------- palette with WCAG contrast ----------
    static double luminance(Color c) {
        double[] v = {c.getRed() / 255.0, c.getGreen() / 255.0, c.getBlue() / 255.0};
        for (int i = 0; i < 3; i++) v[i] = v[i] <= 0.03928 ? v[i] / 12.92 : Math.pow((v[i] + 0.055) / 1.055, 2.4);
        return 0.2126 * v[0] + 0.7152 * v[1] + 0.0722 * v[2];
    }

    static double contrast(Color a, Color b) {
        double la = luminance(a), lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    static BufferedImage palette() {
        Object[][] swatches = {
            {"Carmine", CARMINE, Color.WHITE}, {"Carmine 70 (dark)", CARMINE_LIGHT, GRAPHITE},
            {"Muted carmine", MUTED, Color.WHITE}, {"Muted carmine 90", MUTED_LIGHT, GRAPHITE},
            {"Graphite", GRAPHITE, PAPER}, {"Paper", PAPER, GRAPHITE},
        };
        BufferedImage img = new BufferedImage(1200, 420, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        hints(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 1200, 420);
        for (int i = 0; i < swatches.length; i++) {
            Color c = (Color) swatches[i][1];
            Color text = (Color) swatches[i][2];
            int x = 20 + i * 195;
            g.setColor(c);
            g.fillRoundRect(x, 20, 180, 220, 18, 18);
            g.setColor(text);
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            g.drawString((String) swatches[i][0], x + 12, 52);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            g.drawString(String.format("#%06X", c.getRGB() & 0xFFFFFF), x + 12, 80);
            g.drawString(String.format("text %.1f:1", contrast(c, text)), x + 12, 225);
            g.setColor(GRAPHITE);
            g.drawString(String.format("on paper %.1f:1", contrast(c, PAPER)), x + 4, 270);
            g.drawString(String.format("on graphite %.1f:1", contrast(c, GRAPHITE)), x + 4, 295);
        }
        // Onion skins as the editor will draw them, normal and red-green colour-blind views.
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.drawString("Canvas onion skins only: past carmine, next blue (normal, deuteranopia simulation)", 20, 340);
        onion(g, 20, 350, false);
        onion(g, 320, 350, true);
        return img;
    }

    static void onion(Graphics2D g, int x, int y, boolean deutan) {
        Color past = deutan ? deuteranopia(CARMINE) : CARMINE;
        Color future = deutan ? deuteranopia(ONION_NEXT) : ONION_NEXT;
        g.setColor(PAPER);
        g.fillRect(x, y, 280, 60);
        g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(past.getRed(), past.getGreen(), past.getBlue(), 140));
        g.drawArc(x + 20, y + 10, 70, 40, 0, 180);
        g.setColor(GRAPHITE);
        g.drawArc(x + 105, y + 10, 70, 40, 0, 180);
        g.setColor(new Color(future.getRed(), future.getGreen(), future.getBlue(), 140));
        g.drawArc(x + 190, y + 10, 70, 40, 0, 180);
    }

    /** Machado et al. 2009 deuteranopia matrix (severity 1), in linear RGB. */
    static Color deuteranopia(Color c) {
        double[] v = {c.getRed() / 255.0, c.getGreen() / 255.0, c.getBlue() / 255.0};
        for (int i = 0; i < 3; i++) v[i] = v[i] <= 0.04045 ? v[i] / 12.92 : Math.pow((v[i] + 0.055) / 1.055, 2.4);
        double r = 0.367322 * v[0] + 0.860646 * v[1] - 0.227968 * v[2];
        double gg = 0.280085 * v[0] + 0.672501 * v[1] + 0.047413 * v[2];
        double b = -0.011820 * v[0] + 0.042940 * v[1] + 0.968881 * v[2];
        double[] o = {r, gg, b};
        int[] rgb = new int[3];
        for (int i = 0; i < 3; i++) {
            double t = Math.max(0, Math.min(1, o[i]));
            t = t <= 0.0031308 ? t * 12.92 : 1.055 * Math.pow(t, 1 / 2.4) - 0.055;
            rgb[i] = (int) Math.round(t * 255);
        }
        return new Color(rgb[0], rgb[1], rgb[2]);
    }
}
