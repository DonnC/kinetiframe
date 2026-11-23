import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextAttribute;
import java.awt.geom.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * The one geometry every brand output is drawn from.
 *
 * The mark is the bouncing ball, the first exercise an animator learns (Williams, The Animator's
 * Survival Kit) and the clearest picture of squash and stretch, the first of the twelve principles
 * (Thomas and Johnston, The Illusion of Life). One brand colour, carmine, with the logo's two
 * neutrals: earlier frames in carmine like onion skins, the landed ball and ground in graphite,
 * on a paper tile. Most leading brands use one or two colours, and the app derives every
 * interface colour from this carmine.
 *
 * The wordmark is Nunito ExtraBold, the app's interface font, with the i dots replaced by
 * carmine balls.
 */
public class BrandGeometry {
    static final Color CARMINE = new Color(0xC9372C);
    static final Color CARMINE_LIGHT = new Color(0xFF8A7B);
    static final Color GRAPHITE = new Color(0x22201E);
    static final Color PAPER = new Color(0xF6F3EC);

    record Piece(String name, Shape shape, Color colour) {}

    /** Colours for the earlier frames, the landed ball and the ground on a given tile. */
    record Scheme(Color tile, Color past, Color now, Color ground) {}

    static final Scheme ON_PAPER = new Scheme(PAPER, CARMINE, GRAPHITE, GRAPHITE);

    static Shape ellipse(double cx, double cy, double rx, double ry, double tilt) {
        return AffineTransform.getRotateInstance(Math.toRadians(tilt), cx, cy)
            .createTransformedShape(new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2));
    }

    static Shape stroked(Shape s, float width) {
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(s);
    }

    // On a 512 grid. One falling diagonal, never a face: the ball is round near the top of its
    // arc, where it moves slowly, then stretched and further on as it speeds up (the spacing
    // chart animators draw), and squashed where it lands on the ground.
    // The smallest shape is still about 10 px on a 48 dp launcher icon.
    static Shape top() { return ellipse(164, 134, 40, 40, 0); }
    static Shape falling() { return ellipse(232, 246, 38, 60, -32); }
    static Shape landed() { return ellipse(326, 372, 84, 44, 0); }
    static Shape ground() { return stroked(new Line2D.Double(240, 436, 412, 436), 26); }

    /** The mark, centred on the grid so it sits inside the launcher's safe circle. */
    static List<Piece> mark(Scheme s) {
        List<Piece> raw = List.of(
            new Piece("top", top(), s.past()),
            new Piece("falling", falling(), s.past()),
            new Piece("landed", landed(), s.now()),
            new Piece("ground", ground(), s.ground())
        );
        AffineTransform centre = centring(raw.stream().map(Piece::shape).toList());
        List<Piece> out = new ArrayList<>();
        for (Piece p : raw) out.add(new Piece(p.name(), centre.createTransformedShape(p.shape()), p.colour()));
        return out;
    }

    /** One shape for Android's themed icons, which the system tints a single colour. */
    static Shape monochrome() {
        Area all = new Area();
        for (Piece p : mark(ON_PAPER)) all.add(new Area(p.shape()));
        return all;
    }

    /** Radius on the 512 grid that maps to the adaptive icon's 66 dp safe circle (of 108 dp). */
    static final double SAFE_RADIUS = 256 * 66 / 108.0;

    /** Centres the shapes on the grid and scales them to fill the safe circle, with a margin. */
    static AffineTransform centring(List<Shape> shapes) {
        Rectangle2D b = null;
        for (Shape s : shapes) b = b == null ? s.getBounds2D() : b.createUnion(s.getBounds2D());
        AffineTransform move = AffineTransform.getTranslateInstance(256 - b.getCenterX(), 256 - b.getCenterY());
        double far = 0;
        double[] c = new double[6];
        for (Shape s : shapes) {
            for (PathIterator it = move.createTransformedShape(s).getPathIterator(null, 0.5); !it.isDone(); it.next()) {
                if (it.currentSegment(c) != PathIterator.SEG_CLOSE) far = Math.max(far, Math.hypot(c[0] - 256, c[1] - 256));
            }
        }
        AffineTransform t = new AffineTransform();
        t.translate(256, 256);
        t.scale(SAFE_RADIUS * 0.96 / far, SAFE_RADIUS * 0.96 / far);
        t.translate(-256, -256);
        t.concatenate(move);
        return t;
    }

    /** The adaptive icon safe zone is a 66 dp circle on the 108 dp canvas. */
    static final double LAUNCHER_SCALE = 1.0;

    /** Scales the mark about the grid centre. */
    static Shape fit(Shape s, double scale) {
        AffineTransform t = new AffineTransform();
        t.translate(256, 256);
        t.scale(scale, scale);
        t.translate(-256, -256);
        return t.createTransformedShape(s);
    }

    // ---------- the wordmark ----------
    static final String FONT = "modules/designsystem/src/main/res/font/nunito_extrabold.ttf";

    record Wordmark(Shape letters, Shape dots, double width, double height) {}

    /** "KinetiFrame" in Nunito ExtraBold at 100 px, with carmine balls for the i dots. */
    static Wordmark wordmark() {
        try {
            Map<TextAttribute, Object> kern = Map.of(TextAttribute.KERNING, TextAttribute.KERNING_ON);
            Font font = Font.createFont(Font.TRUETYPE_FONT, new File(FONT)).deriveFont(100f).deriveFont(kern);
            FontRenderContext frc = new FontRenderContext(null, true, true);
            char[] text = "KınetıFrame".toCharArray();
            GlyphVector gv = font.layoutGlyphVector(frc, text, 0, text.length, Font.LAYOUT_LEFT_TO_RIGHT);
            Shape letters = gv.getOutline(0, 0);
            double xHeight = 48.4;
            Path2D dots = new Path2D.Double();
            for (int i = 0; i < text.length; i++) {
                if (text[i] != 'ı') continue;
                Rectangle2D stem = gv.getGlyphVisualBounds(i).getBounds2D();
                // A ball a little wider than the stem, so it reads as the brand's ball, not a dot.
                double r = stem.getWidth() * 0.66;
                double cy = -xHeight - stem.getWidth() * 0.42 - r;
                dots.append(new Ellipse2D.Double(stem.getCenterX() - r, cy - r, r * 2, r * 2), false);
            }
            Rectangle2D b = letters.getBounds2D().createUnion(dots.getBounds2D());
            AffineTransform move = AffineTransform.getTranslateInstance(2 - b.getX(), 2 - b.getY());
            return new Wordmark(move.createTransformedShape(letters), move.createTransformedShape(dots),
                b.getWidth() + 4, b.getHeight() + 4);
        } catch (Exception e) {
            throw new IllegalStateException("Run from the repository root with " + FONT + " present", e);
        }
    }
}
