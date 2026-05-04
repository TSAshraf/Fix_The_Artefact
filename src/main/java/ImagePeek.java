import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImagePeek extends JPanel implements ThemeAware {
    private final JLabel pic = new JLabel();
    private final JPanel card = new JPanel(new BorderLayout());
    private BufferedImage src;
    private int maxW = 220, maxH = 160;

    // Theme-derived colours
    private Color shadowColor;
    private Color cardFill;
    private Color cardStroke;

    public ImagePeek() {
        setOpaque(false);
        setLayout(new BorderLayout());

        card.setOpaque(true);
        card.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        card.add(pic, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
        setVisible(false);

        // Theme hook
        ThemeManager.get().register(this);
        refreshTheme();
    }

    @Override
    public void refreshTheme() {
        Theme.Palette.Overlay o = ThemeManager.get().palette().overlay;

        cardFill = o.cardFill;
        cardStroke = o.cardStroke;

        // shadow: derived from scrim, toned down
        shadowColor = new Color(
                o.scrim.getRed(), o.scrim.getGreen(), o.scrim.getBlue(),
                Math.min(70, o.scrim.getAlpha())
        );

        card.setBackground(cardFill);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cardStroke, 1, true),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));

        pic.setForeground(o.text);

        repaint();
    }

    // Set the maximum size the preview should fit within.
    public void setPeekSize(int maxW, int maxH) {
        this.maxW = Math.max(32, maxW);
        this.maxH = Math.max(32, maxH);
        if (src != null) updateIcon();
    }

    // Set/refresh the image and fit it inside the current max size.
    public void setImage(BufferedImage img, int maxW, int maxH) {
        this.src = img;
        this.maxW = Math.max(32, maxW);
        this.maxH = Math.max(32, maxH);
        updateIcon();
    }

    // Convenience: place the peek at (x,y) in parent coords (e.g., layered pane).
    public void showAt(int x, int y) {
        Dimension ps = card.getPreferredSize();
        setBounds(x, y, ps.width, ps.height);
        setVisible(true);
        revalidate();
        repaint();
    }

    private void updateIcon() {
        if (src == null) { pic.setIcon(null); return; }

        double s = Math.min(maxW / (double) src.getWidth(), maxH / (double) src.getHeight());
        int w = Math.max(1, (int) Math.round(src.getWidth() * s));
        int h = Math.max(1, (int) Math.round(src.getHeight() * s));

        // High-quality scale (better than getScaledInstance)
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();

        pic.setIcon(new ImageIcon(scaled));

        // Size the card to content
        card.setPreferredSize(new Dimension(scaled.getWidth() + 12, scaled.getHeight() + 12));
        revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // drop shadow around the card
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (shadowColor != null) {
            g2.setColor(shadowColor);
            Rectangle b = card.getBounds();
            g2.fillRoundRect(b.x - 4, b.y - 4, b.width + 8, b.height + 8, 12, 12);
        }

        g2.dispose();
    }
}
