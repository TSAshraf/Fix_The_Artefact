import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

// Rendering helpers for text drawn directly via Graphics2D (i.e. not via Swing
// components that can use the CollectionsPanel pill-halo approach).
// The map panels paint their title, subtitle, back-label, and view-all-label
// straight onto the background image, where contrast can swing dramatically
// across the canvas (sky to pyramids to desert). A luminance-adaptive outline
// around each glyph keeps the text legible regardless of what's behind it.

public final class TextRender {
    private TextRender() {}

    // Draw a string with a soft contrasting outline ("halo") so it reads cleanly
    // against any background image. The halo colour is chosen from the text
    // colour's perceived luminance: dark text gets a light halo, light text
    // gets a dark halo.

    public static void drawStringWithHalo(Graphics2D g2, String text, int x, int y, Color textColor) {
        drawStringWithHalo(g2, text, x, y, textColor, 140);
    }

    // Variant with an explicit halo alpha so callers can tune the intensity.
    // Titles typically benefit from stronger halos (180), small legend /
    // caption text looks less bloated with lighter halos (110-130).

    public static void drawStringWithHalo(Graphics2D g2, String text, int x, int y,
                                          Color textColor, int haloAlpha) {
        if (text == null || text.isEmpty()) return;

        // Rec. 601 luminance
        double lum = 0.299 * textColor.getRed()
                   + 0.587 * textColor.getGreen()
                   + 0.114 * textColor.getBlue();
        int a = Math.max(0, Math.min(255, haloAlpha));
        Color halo = lum > 128
                ? new Color(0, 0, 0, a)
                : new Color(255, 255, 255, a);

        // 4-direction cardinal outline, lighter than 8-direction, still covers
        // contrasting-background cases. The diagonals tended to produce visible
        // corner artefacts on thin strokes, making letters look bolder than they are.
        g2.setColor(halo);
        g2.drawString(text, x - 1, y);
        g2.drawString(text, x + 1, y);
        g2.drawString(text, x,     y - 1);
        g2.drawString(text, x,     y + 1);

        // Main text on top
        g2.setColor(textColor);
        g2.drawString(text, x, y);
    }

    // Draw text with a filled rounded-pill behind it, matches the button halos
    // used elsewhere in the app (main menu, profile card, new-profile overlay,
    // avatar chooser). Use this for directly-painted clickable labels so they
    // share the same visual vocabulary as Swing buttons.
    // The pill colour contrasts with the text colour (dark pill under light text,
    // light pill under dark text), mirroring drawStringWithHalo.

    public static void drawStringWithPill(Graphics2D g2, String text, int x, int y, Color textColor) {
        drawStringWithPill(g2, text, x, y, textColor, 150);
    }

    public static void drawStringWithPill(Graphics2D g2, String text, int x, int y,
                                          Color textColor, int pillAlpha) {
        if (text == null || text.isEmpty()) return;

        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(text);
        int ascent = fm.getAscent();
        int height = fm.getHeight();
        // Pill padding mirrors the click-bounds pattern used by the map panels
        // (x - 8, y - ascent - 4, textW + 16, height + 8), so the visible pill
        // coincides with the actual clickable region.
        int padX = 8, padY = 4;
        int pillX = x - padX;
        int pillY = y - ascent - padY;
        int pillW = textW + padX * 2;
        int pillH = height + padY * 2;

        double lum = 0.299 * textColor.getRed()
                   + 0.587 * textColor.getGreen()
                   + 0.114 * textColor.getBlue();
        int a = Math.max(0, Math.min(255, pillAlpha));
        Color pill = lum > 128 ? new Color(0, 0, 0, a) : new Color(255, 255, 255, a);

        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(pill);
        g2.fillRoundRect(pillX, pillY, pillW, pillH, 18, 18);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);

        g2.setColor(textColor);
        g2.drawString(text, x, y);
    }
}
