import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class OrnateFramePanel extends JPanel implements ThemeAware {
    private BufferedImage frameImg;
    private final JPanel content;
    private final int paddingPx;
    private final int slicePx;
    private final int scale;
    private final String framePath; // single file now

    // Tint settings for DARK mode
    // (tweak these if you want darker/lighter)
    private final Color darkTint = new Color(0, 0, 0, 180); // black overlay
    private final float darkSaturation = 0.2f; // 0 = greyscale-ish, 1 = unchanged

    public OrnateFramePanel(
            JPanel content,
            int paddingPx,
            int slicePx,
            int scale,
            String framePath
    ) {
        this.content = content;
        this.paddingPx = paddingPx;
        this.slicePx = slicePx;
        this.scale = Math.max(1, scale);
        this.framePath = framePath;

        setOpaque(false);
        setLayout(new BorderLayout());
        content.setOpaque(false);

        JPanel inset = new JPanel(new BorderLayout());
        inset.setOpaque(false);

        int borderThickness = slicePx * this.scale;
        inset.setBorder(BorderFactory.createEmptyBorder(
                borderThickness + paddingPx,
                borderThickness + paddingPx,
                borderThickness + paddingPx,
                borderThickness + paddingPx
        ));

        inset.add(content, BorderLayout.CENTER);
        add(inset, BorderLayout.CENTER);

        ThemeManager.get().register(this);
        refreshTheme();
    }

    @Override
    public void refreshTheme() {
        try (var in = getClass().getResourceAsStream(framePath)) {
            if (in == null) throw new RuntimeException("Missing resource: " + framePath);
            frameImg = ImageIO.read(in);
        } catch (Exception e) {
            e.printStackTrace();
            frameImg = null;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (frameImg == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        boolean dark = ThemeManager.get().getCurrent() == Theme.DARK;

        // If dark mode, tint the frame without destroying highlights
        if (dark) {
            // Slight desaturation can help "Civ" vibes
            g2.setComposite(AlphaComposite.SrcOver);
        }

        int w = getWidth();
        int h = getHeight();

        int s = slicePx;
        int ds = slicePx * scale;

        int sw = frameImg.getWidth();
        int sh = frameImg.getHeight();

        if (w < 2 * ds || h < 2 * ds || sw < 2 * s || sh < 2 * s) {
            // fallback: draw stretched
            drawImageWithOptionalTint(g2, frameImg, 0, 0, w, h, dark);
            g2.dispose();
            return;
        }

        // 9-slice draw
        draw9(g2, 0,     0,     s, s,  0,     0,     ds, ds, dark);           // TL
        draw9(g2, sw-s,  0,     s, s,  w-ds,  0,     ds, ds, dark);           // TR
        draw9(g2, 0,     sh-s,  s, s,  0,     h-ds,  ds, ds, dark);           // BL
        draw9(g2, sw-s,  sh-s,  s, s,  w-ds,  h-ds,  ds, ds, dark);           // BR

        draw9(g2, s,     0,     sw-2*s, s,    ds,    0,     w-2*ds, ds, dark);     // top
        draw9(g2, s,     sh-s,  sw-2*s, s,    ds,    h-ds,  w-2*ds, ds, dark);     // bottom
        draw9(g2, 0,     s,     s, sh-2*s,    0,     ds,    ds,     h-2*ds, dark); // left
        draw9(g2, sw-s,  s,     s, sh-2*s,    w-ds,  ds,    ds,     h-2*ds, dark); // right

        draw9(g2, s, s, sw-2*s, sh-2*s, ds, ds, w-2*ds, h-2*ds, dark); // center

        g2.dispose();
    }

    private void draw9(Graphics2D g2,
                       int sx, int sy, int sw, int sh,
                       int dx, int dy, int dw, int dh,
                       boolean dark) {
        // render sub-rect of source into dest
        BufferedImage sub = frameImg.getSubimage(sx, sy, sw, sh);
        drawImageWithOptionalTint(g2, sub, dx, dy, dw, dh, dark);
    }

    private void drawImageWithOptionalTint(Graphics2D g2, BufferedImage img, int x, int y, int w, int h, boolean dark) {
        // draw base
        g2.setComposite(AlphaComposite.SrcOver);
        g2.drawImage(img, x, y, w, h, null);

        if (!dark) return;

        // tint overlay: preserves shape/highlights but darkens overall
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setColor(darkTint);
        g2.fillRect(x, y, w, h);
    }
}
