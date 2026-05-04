import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


public final class AppIcon {

    private AppIcon() {}

    // Shared palette, matches the Kenney ornate frame and the in-game
    // background colours so the icon visually belongs to the same family.
    private static final Color STONE_TOP  = new Color(168, 98, 60);
    private static final Color STONE_BOT  = new Color(72, 38, 24);
    private static final Color GOLD       = new Color(220, 180, 110);
    private static final Color GOLD_DEEP  = new Color(160, 120, 60);
    private static final Color CREAM_TOP  = new Color(250, 232, 192);
    private static final Color CREAM_BOT  = new Color(194, 158, 108);
    private static final Color PIECE_EDGE = new Color(52, 28, 12, 230);

    public static BufferedImage create(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        float s = size;
        int pad = Math.max(1, Math.round(s * 0.03f));
        int w = size - pad * 2;
        int corner = Math.max(4, Math.round(s * 0.22f));

        // Background plaque: terracotta stone with a soft top-left lit highlight
        Shape plaque = new RoundRectangle2D.Float(pad, pad, w, w, corner, corner);

        g2.setPaint(new GradientPaint(pad, pad, STONE_TOP, pad, pad + w, STONE_BOT));
        g2.fill(plaque);

        // Soft radial highlight in the upper-left quadrant.
        Graphics2D hl = (Graphics2D) g2.create();
        hl.setClip(plaque);
        hl.setPaint(new RadialGradientPaint(
                new Point2D.Float(pad + w * 0.30f, pad + w * 0.25f),
                w * 0.70f,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 220, 180, 90), new Color(255, 220, 180, 0)}));
        hl.fill(plaque);
        hl.dispose();

        // Ambient occlusion in the bottom-right, grounds the icon.
        Graphics2D ao = (Graphics2D) g2.create();
        ao.setClip(plaque);
        ao.setPaint(new RadialGradientPaint(
                new Point2D.Float(pad + w * 0.85f, pad + w * 0.90f),
                w * 0.60f,
                new float[]{0f, 1f},
                new Color[]{new Color(0, 0, 0, 110), new Color(0, 0, 0, 0)}));
        ao.fill(plaque);
        ao.dispose();

        // Gold inner border: simple thin frame the corner flourishes hook onto.
        float borderStroke = Math.max(1.2f, s / 90f);
        int insetBorder = Math.max(2, Math.round(s * 0.060f));
        g2.setStroke(new BasicStroke(borderStroke));
        g2.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 180));
        int borderCorner = Math.max(2, corner - insetBorder);
        g2.drawRoundRect(pad + insetBorder, pad + insetBorder,
                         w - insetBorder * 2, w - insetBorder * 2,
                         borderCorner, borderCorner);

        // Ornate corner flourishes
        // Small L-bracket at each corner of the gold frame, echoing the
        // Kenney fantasy-UI panel corners seen in the menu and profile panels.
        if (size >= 48) {
            drawCornerFlourishes(g2, pad + insetBorder, pad + insetBorder,
                    w - insetBorder * 2, w - insetBorder * 2, s);
        }

        // Jigsaw piece (tab top + right, blank bottom + left)
        double pSize = s * 0.56;
        double pX = (s - pSize) / 2.0;
        double pY = (s - pSize) / 2.0;
        Path2D piece = buildClassicPiece(pX, pY, pSize, pSize);
        Rectangle2D pieceBounds = piece.getBounds2D();

        // Drop shadow.
        Graphics2D shadow = (Graphics2D) g2.create();
        shadow.translate(0, Math.max(1, Math.round(s / 90f)));
        shadow.setColor(new Color(0, 0, 0, 150));
        shadow.fill(piece);
        shadow.dispose();

        // Piece body, cream gradient.
        g2.setPaint(new GradientPaint(
                (float) pieceBounds.getMinX(), (float) pieceBounds.getMinY(), CREAM_TOP,
                (float) pieceBounds.getMinX(), (float) pieceBounds.getMaxY(), CREAM_BOT));
        g2.fill(piece);

        // Two-tone bevel: warm highlight top-left, dark wash bottom-right.
        Graphics2D bevel = (Graphics2D) g2.create();
        bevel.setClip(piece);
        bevel.setPaint(new RadialGradientPaint(
                new Point2D.Float(
                        (float) (pieceBounds.getMinX() + pieceBounds.getWidth() * 0.30),
                        (float) (pieceBounds.getMinY() + pieceBounds.getHeight() * 0.25)),
                (float) (pieceBounds.getWidth() * 0.55),
                new float[]{0f, 1f},
                new Color[]{new Color(255, 250, 230, 140), new Color(255, 250, 230, 0)}));
        bevel.fillRect((int) pieceBounds.getMinX() - 2, (int) pieceBounds.getMinY() - 2,
                       (int) pieceBounds.getWidth() + 4, (int) pieceBounds.getHeight() + 4);
        bevel.setPaint(new RadialGradientPaint(
                new Point2D.Float(
                        (float) (pieceBounds.getMinX() + pieceBounds.getWidth() * 0.80),
                        (float) (pieceBounds.getMinY() + pieceBounds.getHeight() * 0.85)),
                (float) (pieceBounds.getWidth() * 0.50),
                new float[]{0f, 1f},
                new Color[]{new Color(60, 36, 14, 95), new Color(60, 36, 14, 0)}));
        bevel.fillRect((int) pieceBounds.getMinX() - 2, (int) pieceBounds.getMinY() - 2,
                       (int) pieceBounds.getWidth() + 4, (int) pieceBounds.getHeight() + 4);
        bevel.dispose();

        // Crisp dark outline.
        g2.setStroke(new BasicStroke(Math.max(1f, s / 110f)));
        g2.setColor(PIECE_EDGE);
        g2.draw(piece);

        g2.dispose();
        return img;
    }

    /** Four small ornate brackets, one per corner of the given rectangle,
     *  drawn in the same two-tone gold used elsewhere in the app's frame motif. */
    private static void drawCornerFlourishes(Graphics2D g2, int x, int y, int w, int h, float s) {
        double armLen = s * 0.11;                   // length of each bracket arm
        float armStroke = Math.max(1.1f, s / 120f); // thickness
        double inset = s * 0.02;                    // offset from the frame line
        double dotSize = Math.max(1.5, s * 0.018);  // small decorative dot at inner end

        // Draw each corner bracket, two line segments forming an L, with a
        // small diamond dot at the inner elbow for a subtle filigree touch.
        int[] cornerX = {x + (int) inset,           x + w - (int) inset,
                         x + (int) inset,           x + w - (int) inset};
        int[] cornerY = {y + (int) inset,           y + (int) inset,
                         y + h - (int) inset,       y + h - (int) inset};
        // Direction multipliers for each corner (TL, TR, BL, BR).
        int[] dx = { +1, -1, +1, -1 };
        int[] dy = { +1, +1, -1, -1 };

        // Shadow pass, slightly darker gold underneath for depth.
        g2.setStroke(new BasicStroke(armStroke * 1.6f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(GOLD_DEEP.getRed(), GOLD_DEEP.getGreen(), GOLD_DEEP.getBlue(), 170));
        for (int i = 0; i < 4; i++) {
            int cx = cornerX[i], cy = cornerY[i];
            g2.drawLine(cx, cy, cx + (int) (dx[i] * armLen), cy);
            g2.drawLine(cx, cy, cx, cy + (int) (dy[i] * armLen));
        }

        // Highlight pass, brighter gold on top.
        g2.setStroke(new BasicStroke(armStroke,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(GOLD);
        for (int i = 0; i < 4; i++) {
            int cx = cornerX[i], cy = cornerY[i];
            g2.drawLine(cx, cy, cx + (int) (dx[i] * armLen), cy);
            g2.drawLine(cx, cy, cx, cy + (int) (dy[i] * armLen));
        }

        // Small diamond at the inner end of each bracket
        for (int i = 0; i < 4; i++) {
            int cx = cornerX[i] + (int) (dx[i] * armLen);
            int cy = cornerY[i] + (int) (dy[i] * armLen);
            Path2D diamond = new Path2D.Double();
            diamond.moveTo(cx, cy - dotSize);
            diamond.lineTo(cx + dotSize, cy);
            diamond.lineTo(cx, cy + dotSize);
            diamond.lineTo(cx - dotSize, cy);
            diamond.closePath();
            g2.setColor(GOLD);
            g2.fill(diamond);
        }
    }

    // Canonical jigsaw-piece silhouette
    private static Path2D buildClassicPiece(double x, double y, double w, double h) {
        double tab = Math.min(w, h) * 0.15;
        // Base square: offset from bounding-box top/right because those are where tabs protrude;
        // flush with bottom/left because blanks carve inward from the edge.
        double bx = x;
        double by = y + tab;
        double bw = w - tab;
        double bh = h - tab;

        double neckStart = 0.35;    // where the feature's neck meets the edge (start)
        double neckEnd   = 0.65;    // where it meets the edge (end)
        double lobeHalf  = 0.13;    // half-width of the lobe at its depth

        Path2D path = new Path2D.Double();
        path.setWindingRule(Path2D.WIND_NON_ZERO);
        path.moveTo(bx, by);

        // Top edge: tab protruding upward
        path.lineTo(bx + bw * neckStart, by);
        // First half: neck then apex. Controls at the apex depth with symmetric
        // horizontal positions produce a clean half-lobe.
        path.curveTo(
                bx + bw * neckStart,           by - tab,
                bx + bw * (0.5 - lobeHalf),    by - tab,
                bx + bw * 0.5,                 by - tab);
        // Second half: apex then neck.
        path.curveTo(
                bx + bw * (0.5 + lobeHalf),    by - tab,
                bx + bw * neckEnd,             by - tab,
                bx + bw * neckEnd,             by);
        path.lineTo(bx + bw, by);

        // Right edge: tab protruding rightward
        path.lineTo(bx + bw, by + bh * neckStart);
        path.curveTo(
                bx + bw + tab, by + bh * neckStart,
                bx + bw + tab, by + bh * (0.5 - lobeHalf),
                bx + bw + tab, by + bh * 0.5);
        path.curveTo(
                bx + bw + tab, by + bh * (0.5 + lobeHalf),
                bx + bw + tab, by + bh * neckEnd,
                bx + bw,       by + bh * neckEnd);
        path.lineTo(bx + bw, by + bh);

        // Bottom edge: blank indenting UP into the body
        // Traversal is right-to-left along the bottom edge. The lobe peaks
        // INSIDE the body at y = by+bh-tab, so the indent reads as a notch
        // carved out of the piece rather than a bump sticking out.
        path.lineTo(bx + bw * neckEnd, by + bh);
        path.curveTo(
                bx + bw * neckEnd,             by + bh - tab,
                bx + bw * (0.5 + lobeHalf),    by + bh - tab,
                bx + bw * 0.5,                 by + bh - tab);
        path.curveTo(
                bx + bw * (0.5 - lobeHalf),    by + bh - tab,
                bx + bw * neckStart,           by + bh - tab,
                bx + bw * neckStart,           by + bh);
        path.lineTo(bx, by + bh);

        // Left edge: blank indenting RIGHT into the body
        path.lineTo(bx, by + bh * neckEnd);
        path.curveTo(
                bx + tab, by + bh * neckEnd,
                bx + tab, by + bh * (0.5 + lobeHalf),
                bx + tab, by + bh * 0.5);
        path.curveTo(
                bx + tab, by + bh * (0.5 - lobeHalf),
                bx + tab, by + bh * neckStart,
                bx,       by + bh * neckStart);
        path.closePath();
        return path;
    }

    /** Multiple sizes so JFrame.setIconImages can pick the best for each context
     *  (title bar 16/32, taskbar 32/64, dock 128+, Retina 512/1024). */
    public static List<Image> sizes() {
        List<Image> list = new ArrayList<>();
        for (int sz : new int[]{16, 32, 64, 128, 256, 512, 1024}) {
            list.add(create(sz));
        }
        return list;
    }
}
