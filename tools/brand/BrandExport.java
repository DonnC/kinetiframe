import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Writes the brand from BrandGeometry: Android vectors (launcher foreground, themed monochrome,
 * splash), Compose path data (designsystem BrandArt.kt), SVGs for docs, and the store PNG.
 * Every piece is a filled outline, so all targets draw the same pixels. Run from the repo root.
 *
 * Args: repoRoot
 */
public class BrandExport extends BrandGeometry {
    static String path(Shape s) {
        StringBuilder b = new StringBuilder();
        double[] c = new double[6];
        // An area never overlaps itself, so even-odd and non-zero filling agree.
        for (PathIterator it = new Area(s).getPathIterator(null); !it.isDone(); it.next()) {
            switch (it.currentSegment(c)) {
                case PathIterator.SEG_MOVETO -> b.append('M').append(n(c[0])).append(' ').append(n(c[1]));
                case PathIterator.SEG_LINETO -> b.append('L').append(n(c[0])).append(' ').append(n(c[1]));
                case PathIterator.SEG_QUADTO -> b.append('Q').append(n(c[0])).append(' ').append(n(c[1])).append(' ')
                    .append(n(c[2])).append(' ').append(n(c[3]));
                case PathIterator.SEG_CUBICTO -> b.append('C').append(n(c[0])).append(' ').append(n(c[1])).append(' ')
                    .append(n(c[2])).append(' ').append(n(c[3])).append(' ').append(n(c[4])).append(' ').append(n(c[5]));
                case PathIterator.SEG_CLOSE -> b.append('Z');
            }
        }
        return b.toString();
    }

    static String n(double v) {
        String s = String.format(Locale.ROOT, "%.1f", v);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }

    static String hex(Color c) { return String.format("#%06X", c.getRGB() & 0xFFFFFF); }

    static String vector(double sizeDp, List<String[]> paths) {
        StringBuilder b = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        b.append("<!-- Generated from tools/brand/BrandExport.java: the bouncing-ball mark. Do not edit by hand. -->\n");
        b.append("<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n");
        b.append("    android:width=\"").append(n(sizeDp)).append("dp\" android:height=\"").append(n(sizeDp)).append("dp\"\n");
        b.append("    android:viewportWidth=\"512\" android:viewportHeight=\"512\">\n");
        for (String[] p : paths) {
            b.append("    <path android:fillColor=\"").append(p[1]).append("\" android:fillType=\"evenOdd\"\n");
            b.append("        android:pathData=\"").append(p[0]).append("\" />\n");
        }
        return b.append("</vector>\n").toString();
    }

    static List<String[]> colourPaths(double scale) {
        List<String[]> out = new ArrayList<>();
        for (Piece p : mark(ON_PAPER)) out.add(new String[]{path(fit(p.shape(), scale)), hex(p.colour())});
        return out;
    }

    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args[0]);
        Path drawable = root.resolve("app/src/main/res/drawable");
        // Adaptive icons: the mark already fills the central 66 dp safe circle of the 108 dp canvas.
        Files.writeString(drawable.resolve("ic_launcher_foreground.xml"), vector(108, colourPaths(LAUNCHER_SCALE)));
        Files.writeString(drawable.resolve("ic_launcher_monochrome.xml"),
            vector(108, List.<String[]>of(new String[]{path(fit(monochrome(), LAUNCHER_SCALE)), "#FFFFFFFF"})));
        // The splash icon, on a paper icon background, must fit a 160 dp circle of its 240 dp.
        Files.writeString(drawable.resolve("ic_splash_mark.xml"), vector(240, colourPaths(SPLASH_SCALE)));
        writeKotlin(root.resolve("modules/designsystem/src/main/java/com/donnclab/kinetiframe/designsystem/theme/BrandArt.kt"));
        writeDocs(root.resolve("docs/brand"));
    }

    /** The safe circle is 66 of 108 dp for launchers and 160 of 240 dp for the splash. */
    static final double SPLASH_SCALE = (160 / 240.0) / (66 / 108.0) * 0.82;

    // ---------- Compose path data ----------
    static String colourName(Color c) {
        if (c.equals(CARMINE)) return "CARMINE";
        if (c.equals(GRAPHITE)) return "GRAPHITE";
        throw new IllegalArgumentException(hex(c));
    }

    static void writeKotlin(Path file) throws Exception {
        StringBuilder b = new StringBuilder();
        b.append("// SPDX-License-Identifier: Apache-2.0\n");
        b.append("// Generated from tools/brand/BrandExport.java. Do not edit by hand.\n");
        b.append("package com.donnclab.kinetiframe.designsystem.theme\n\n");
        b.append("/**\n * KinetiFrame's bouncing-ball mark and Nunito wordmark as SVG path data, on a 512 grid for the\n");
        b.append(" * mark and a 0-based grid for the wordmark, so every screen draws the same vector art.\n */\n");
        b.append("object BrandArt {\n");
        b.append("    /** One piece of the mark: its outline and which brand colour fills it. */\n");
        b.append("    data class Piece(val pathData: String, val colour: BrandArtColour)\n\n");
        b.append("    enum class BrandArtColour { GRAPHITE, CARMINE }\n\n");
        b.append("    /** The mark sits on a paper tile of this size. */\n");
        b.append("    const val MARK_SIZE = 512f\n\n");
        b.append("    /** How much to enlarge the mark when a whole tile shows, not a launcher's crop. */\n");
        b.append("    const val TILE_SCALE = ").append(String.format(Locale.ROOT, "%.3f", ICON_SCALE)).append("f\n\n");
        b.append("    val mark = listOf(\n");
        for (Piece p : mark(ON_PAPER)) {
            b.append("        Piece(\n            \"").append(path(p.shape())).append("\",\n            BrandArtColour.")
                .append(colourName(p.colour())).append(",\n        ),\n");
        }
        b.append("    )\n\n");
        Wordmark w = wordmark();
        b.append("    const val WORDMARK_WIDTH = ").append(n(w.width())).append("f\n");
        b.append("    const val WORDMARK_HEIGHT = ").append(n(w.height())).append("f\n\n");
        b.append("    /** The lettering, drawn in the surface's ink colour. */\n");
        b.append("    const val WORDMARK_LETTERS =\n        \"").append(path(w.letters())).append("\"\n\n");
        b.append("    /** The dots of the two i's: bouncing balls, drawn in carmine. */\n");
        b.append("    const val WORDMARK_DOTS =\n        \"").append(path(w.dots())).append("\"\n");
        b.append("}\n");
        Files.writeString(file, b.toString());
    }

    // ---------- docs ----------
    static void writeDocs(Path dir) throws Exception {
        Files.createDirectories(dir);
        StringBuilder icon = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 512 512\">\n");
        icon.append(tile());
        for (String[] p : colourPaths(ICON_SCALE)) icon.append(svgPath(p));
        Files.writeString(dir.resolve("kinetiframe-icon.svg"), icon.append("</svg>\n").toString());

        Wordmark w = wordmark();
        double markSize = 120, gap = 28, scale = markSize * 0.5 / w.height();
        double width = markSize + gap + w.width() * scale;
        StringBuilder lockup = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ")
            .append(n(width)).append(" ").append(n(markSize)).append("\">\n");
        lockup.append("  <g transform=\"scale(").append(String.format(Locale.ROOT, "%.5f", markSize / 512)).append(")\">\n");
        lockup.append("  ").append(tile());
        for (String[] p : colourPaths(ICON_SCALE)) lockup.append("  ").append(svgPath(p));
        lockup.append("  </g>\n");
        lockup.append("  <g transform=\"translate(").append(n(markSize + gap)).append(" ")
            .append(n((markSize - w.height() * scale) / 2)).append(") scale(")
            .append(String.format(Locale.ROOT, "%.5f", scale)).append(")\">\n");
        lockup.append("    <path fill-rule=\"evenodd\" fill=\"").append(hex(GRAPHITE)).append("\" d=\"").append(path(w.letters())).append("\"/>\n");
        lockup.append("    <path fill-rule=\"evenodd\" fill=\"").append(hex(CARMINE)).append("\" d=\"").append(path(w.dots())).append("\"/>\n");
        lockup.append("  </g>\n</svg>\n");
        Files.writeString(dir.resolve("kinetiframe-lockup.svg"), lockup.toString());

        // The Play store icon: a full-bleed 512 PNG (the store applies its own mask).
        BufferedImage store = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = store.createGraphics();
        BrandMark.hints(g);
        g.setColor(PAPER);
        g.fillRect(0, 0, 512, 512);
        for (Piece p : mark(ON_PAPER)) {
            g.setColor(p.colour());
            g.fill(fit(p.shape(), ICON_SCALE));
        }
        g.dispose();
        ImageIO.write(store, "png", dir.resolve("app-icon-512.png").toFile());
    }

    /** Docs and store icons show the whole tile, not a launcher's 72 of 108 dp, so the mark grows. */
    static final double ICON_SCALE = 108 / 72.0 * 0.92;

    static String tile() {
        // A hairline keeps the paper tile visible on a white page.
        return "  <rect x=\"2\" y=\"2\" width=\"508\" height=\"508\" rx=\"116\" fill=\"" + hex(PAPER)
            + "\" stroke=\"#D9D6CF\" stroke-width=\"4\"/>\n";
    }

    static String svgPath(String[] p) {
        return "  <path fill-rule=\"evenodd\" fill=\"" + p[1] + "\" d=\"" + p[0] + "\"/>\n";
    }
}
