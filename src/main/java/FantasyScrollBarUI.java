import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

// Custom scrollbar UI that matches the game's ornate fantasy theme.
// Draws a stone-textured track with a brass/gold thumb and subtle decorative grooves.

public class FantasyScrollBarUI extends BasicScrollBarUI {

    // Track colours
    private Color trackTop, trackBottom;

    // Thumb colours
    private Color thumbBody, thumbHighlight, thumbShadow, thumbBorder;

    // Groove accent
    private Color grooveLight, grooveDark;

    public FantasyScrollBarUI() {
        applyThemeColours();
    }

    private void applyThemeColours() {
        boolean dark = ThemeManager.get().getCurrent() == Theme.DARK;

        if (dark) {
            trackTop     = new Color(35, 32, 28);
            trackBottom   = new Color(28, 25, 22);
            thumbBody     = new Color(110, 90, 55);
            thumbHighlight = new Color(155, 130, 80);
            thumbShadow   = new Color(70, 55, 35);
            thumbBorder   = new Color(90, 75, 45);
            grooveLight   = new Color(255, 255, 255, 12);
            grooveDark    = new Color(0, 0, 0, 40);
        } else {
            trackTop     = new Color(210, 200, 180);
            trackBottom   = new Color(190, 180, 160);
            thumbBody     = new Color(165, 140, 90);
            thumbHighlight = new Color(200, 180, 130);
            thumbShadow   = new Color(120, 100, 60);
            thumbBorder   = new Color(140, 118, 72);
            grooveLight   = new Color(255, 255, 255, 50);
            grooveDark    = new Color(0, 0, 0, 30);
        }
    }

    // Remove arrow buttons entirely
    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createInvisibleButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createInvisibleButton();
    }

    private static JButton createInvisibleButton() {
        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(0, 0));
        btn.setMinimumSize(new Dimension(0, 0));
        btn.setMaximumSize(new Dimension(0, 0));
        return btn;
    }

    // Track
    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        applyThemeColours();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Gradient track (stone channel)
        GradientPaint gp;
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            gp = new GradientPaint(trackBounds.x, trackBounds.y, trackTop,
                    trackBounds.x + trackBounds.width, trackBounds.y, trackBottom);
        } else {
            gp = new GradientPaint(trackBounds.x, trackBounds.y, trackTop,
                    trackBounds.x, trackBounds.y + trackBounds.height, trackBottom);
        }
        g2.setPaint(gp);
        g2.fillRoundRect(trackBounds.x, trackBounds.y,
                trackBounds.width, trackBounds.height, 6, 6);

        // Inset border to make the track look carved
        g2.setColor(grooveDark);
        g2.drawRoundRect(trackBounds.x, trackBounds.y,
                trackBounds.width - 1, trackBounds.height - 1, 6, 6);

        g2.dispose();
    }

    // Thumb
    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int x = thumbBounds.x;
        int y = thumbBounds.y;
        int w = thumbBounds.width;
        int h = thumbBounds.height;

        boolean hovered = isThumbRollover();
        Color body = hovered
                ? brighter(thumbBody, 20)
                : (isDragging ? darker(thumbBody, 15) : thumbBody);

        // Main body, vertical gradient gives it a rounded-metal look
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            g2.setPaint(new GradientPaint(x, y, thumbHighlight, x + w, y, thumbShadow));
        } else {
            g2.setPaint(new GradientPaint(x, y, thumbHighlight, x, y + h, thumbShadow));
        }
        g2.fillRoundRect(x + 1, y + 1, w - 2, h - 2, 8, 8);

        // Overlay the body colour with partial transparency for richness
        g2.setColor(new Color(body.getRed(), body.getGreen(), body.getBlue(), 160));
        g2.fillRoundRect(x + 1, y + 1, w - 2, h - 2, 8, 8);

        // Border
        g2.setColor(thumbBorder);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(x + 1, y + 1, w - 3, h - 3, 8, 8);

        // Decorative grooves in the centre of the thumb
        int grooveCount = 3;
        g2.setStroke(new BasicStroke(1f));
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            int cx = x + w / 2;
            int totalH = grooveCount * 4;
            int startY = y + (h - totalH) / 2;
            for (int i = 0; i < grooveCount; i++) {
                int gy = startY + i * 4;
                g2.setColor(grooveDark);
                g2.drawLine(cx - 4, gy, cx + 4, gy);
                g2.setColor(grooveLight);
                g2.drawLine(cx - 4, gy + 1, cx + 4, gy + 1);
            }
        } else {
            int cy = y + h / 2;
            int totalW = grooveCount * 4;
            int startX = x + (w - totalW) / 2;
            for (int i = 0; i < grooveCount; i++) {
                int gx = startX + i * 4;
                g2.setColor(grooveDark);
                g2.drawLine(gx, cy - 4, gx, cy + 4);
                g2.setColor(grooveLight);
                g2.drawLine(gx + 1, cy - 4, gx + 1, cy + 4);
            }
        }

        g2.dispose();
    }

    // Preferred width (thin but touchable)
    @Override
    public Dimension getPreferredSize(JComponent c) {
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            return new Dimension(12, super.getPreferredSize(c).height);
        } else {
            return new Dimension(super.getPreferredSize(c).width, 12);
        }
    }

    // Utility: install on any JScrollPane
    // Applies the fantasy scrollbar to the given scroll pane.
    public static void install(JScrollPane sp) {
        if (sp == null) return;
        sp.getVerticalScrollBar().setUI(new FantasyScrollBarUI());
        sp.getHorizontalScrollBar().setUI(new FantasyScrollBarUI());
        sp.getVerticalScrollBar().setOpaque(false);
        sp.getHorizontalScrollBar().setOpaque(false);
    }

    // Colour helpers
    private static Color brighter(Color c, int amount) {
        return new Color(
                Math.min(255, c.getRed() + amount),
                Math.min(255, c.getGreen() + amount),
                Math.min(255, c.getBlue() + amount));
    }

    private static Color darker(Color c, int amount) {
        return new Color(
                Math.max(0, c.getRed() - amount),
                Math.max(0, c.getGreen() - amount),
                Math.max(0, c.getBlue() - amount));
    }
}
