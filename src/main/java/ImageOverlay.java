import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class ImageOverlay extends JPanel implements ThemeAware {
    private final JLabel pic = new JLabel();
    private BufferedImage src;

    // Theme-derived paint colours
    private Color shadowColor;
    private Color cardFill;
    private Color cardStroke;

    // main card that floats; we size/move this component itself
    // Remove layout gaps to avoid white borders
    private final JPanel card = new JPanel(new BorderLayout()) {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // soft shadow behind the rounded card
            if (shadowColor != null) {
                g2.setColor(shadowColor);
                g2.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 16, 16);
            }

            // NOTE: card background (fill) is handled by card.setBackground(cardFill)
            // and border via card.setBorder(...) in refreshTheme().

            g2.dispose();
        }
    };

    // content holder (no scrollbars; image always fits)
    private final JScrollPane scroller = new JScrollPane(
            pic,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    );

    // top-right close (wax seal) above the image
    private final JButton closeButton = new JButton() {
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 4;
            int x = (getWidth()  - size) / 2;
            int y = (getHeight() - size) / 2;
            LoadGamePanel.paintWaxSeal(g2, x, y, size, getModel().isRollover());
            g2.dispose();
        }
    };
    private final JPanel topBar = new JPanel(new BorderLayout());
    private static final int TOP_BAR_H = 32;

    // drag / resize state
    private static final int RESIZE_MARGIN = 16; // bottom-right resize grip
    private boolean resizing = false;
    private Point dragAnchor = null;
    private Dimension startSize = null;
    private double aspect = 4.0 / 5.0; // updated when image is set

    public ImageOverlay() {
        setOpaque(false);
        setLayout(null); // we position the card directly

        // Ensure card is non-opaque; we rely on its background alpha
        card.setOpaque(true);

        // make scroller/viewport fully transparent; no stray white
        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        pic.setOpaque(false);
        pic.setBorder(null);

        // Configure the close button (paints itself as a wax seal)
        closeButton.setFocusPainted(false);
        closeButton.setOpaque(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setMargin(new Insets(0, 0, 0, 0));
        closeButton.setPreferredSize(new Dimension(TOP_BAR_H, TOP_BAR_H));
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.setToolTipText("Close");
        closeButton.addActionListener(e -> close());

        topBar.setOpaque(false);
        topBar.add(closeButton, BorderLayout.EAST);
        topBar.setPreferredSize(new Dimension(0, TOP_BAR_H));

        card.add(topBar, BorderLayout.NORTH);
        card.add(scroller, BorderLayout.CENTER);

        // default size & add to overlay
        card.setSize(new Dimension(520, 680));
        add(card);

        // drag & diagonal resize on the card
        MouseAdapter dragResize = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                if (isInResizeZone(e.getPoint())) {
                    card.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
                } else {
                    card.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }

            @Override public void mousePressed(MouseEvent e) {
                dragAnchor = e.getPoint();
                if (isInResizeZone(dragAnchor)) {
                    resizing = true;
                    startSize = card.getSize();
                } else {
                    resizing = false;
                }
            }

            @Override public void mouseDragged(MouseEvent e) {
                if (dragAnchor == null) return;

                if (resizing) {
                    int dx = e.getX() - dragAnchor.x;
                    int newW = Math.max(260, startSize.width + dx);           // min size
                    int newH = Math.max(200, (int) Math.round(newW * aspect));// keep aspect

                    // keep inside parent
                    Dimension parent = getParentSize();
                    newW = Math.min(newW, parent.width - card.getX());
                    newH = Math.min(newH, parent.height - card.getY());

                    card.setSize(newW, newH);
                    ImageOverlay.this.setSize(card.getSize()); // overlay matches card
                    fitImageToCard();
                    revalidate();
                    repaint();
                } else {
                    // move the overlay window itself
                    Point p = getLocation();
                    int nx = p.x + e.getX() - dragAnchor.x;
                    int ny = p.y + e.getY() - dragAnchor.y;

                    // clamp to parent
                    Dimension parent = getParentSize();
                    nx = Math.max(0, Math.min(nx, parent.width - card.getWidth()));
                    ny = Math.max(0, Math.min(ny, parent.height - card.getHeight()));
                    setLocation(nx, ny);
                }
            }

            @Override public void mouseReleased(MouseEvent e) {
                resizing = false;
                dragAnchor = null;
                startSize = null;
            }
        };
        card.addMouseListener(dragResize);
        card.addMouseMotionListener(dragResize);

        // when card resizes (programmatically), keep image fitted
        card.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                fitImageToCard();
            }
        });

        // ESC to hide
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "hide");
        getActionMap().put("hide", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { setVisible(false); }
        });

        // Theme hook
        ThemeManager.get().register(this);
        refreshTheme();
    }

    @Override
    public void refreshTheme() {
        Theme.Palette.Overlay o = ThemeManager.get().palette().overlay;

        // Shadow: derive from scrim (subtle). We don't want full scrim intensity for shadow.
        // If your scrim is already light, this still looks fine.
        shadowColor = new Color(o.scrim.getRed(), o.scrim.getGreen(), o.scrim.getBlue(),
                Math.min(80, o.scrim.getAlpha()));

        cardFill = o.cardFill;
        cardStroke = o.cardStroke;

        // Apply to components
        card.setBackground(cardFill);

        // Keep the inset padding + themed stroke
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cardStroke, 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        // Not strictly necessary for the image, but keeps consistency if you add text later
        pic.setForeground(o.text);

        // Seal paints its own palette, no themed foreground needed.

        repaint();
    }

    private boolean isInResizeZone(Point p) {
        return p.x >= card.getWidth() - RESIZE_MARGIN && p.y >= card.getHeight() - RESIZE_MARGIN;
    }

    private Dimension getParentSize() {
        Container parent = getParent();
        return (parent != null) ? parent.getSize() : Toolkit.getDefaultToolkit().getScreenSize();
    }

    // Sets the image and refreshes sizing/fit.
    public void setImage(BufferedImage img) {
        this.src = img;
        if (src != null && src.getWidth() > 0) {
            aspect = (double) src.getHeight() / (double) src.getWidth();
        }
        fitImageToCard();
    }

    // Fits the image to current card, preserving aspect and hugging edges (no gaps).
    private void fitImageToCard() {
        if (src == null) {
            pic.setIcon(null);
            return;
        }

        // Calculate drawable area inside the card (exclude insets and the close-X bar)
        Insets in = card.getInsets();
        int availW = card.getWidth()  - in.left - in.right;
        int availH = card.getHeight() - in.top  - in.bottom - TOP_BAR_H;

        // If card hasn't been laid out yet, use its current size as a hint
        if (availW <= 0 || availH <= 0) {
            Dimension s = card.getSize();
            availW = Math.max(1, s.width  - in.left - in.right);
            availH = Math.max(1, s.height - in.top  - in.bottom - TOP_BAR_H);
        }

        // Scale to fit while keeping aspect ratio
        double s = Math.min(availW / (double) src.getWidth(),
                availH / (double) src.getHeight());
        int w = Math.max(1, (int) Math.round(src.getWidth()  * s));
        int h = Math.max(1, (int) Math.round(src.getHeight() * s));

        // Apply scaled image
        Image scaled = src.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        pic.setIcon(new ImageIcon(scaled));
        pic.setPreferredSize(new Dimension(w, h));
        pic.revalidate();

        // Make the card wrap tightly around the image (no letterboxing).
        // Reserve TOP_BAR_H for the close-X strip so the image isn't squashed by it.
        int cardW = w + in.left + in.right;
        int cardH = h + in.top + in.bottom + TOP_BAR_H;
        card.setPreferredSize(new Dimension(cardW, cardH));
        card.setSize(cardW, cardH);

        // overlay matches card
        setSize(card.getSize());

        revalidate();
        repaint();
    }

    // Center within a container (e.g., layered pane size).
    public void centerIn(Dimension containerSize) {
        int x = (containerSize.width - card.getWidth()) / 2;
        int y = (containerSize.height - card.getHeight()) / 2;
        setLocation(Math.max(8, x), Math.max(8, y));
    }

    public void close() {
        setVisible(false);
        Container p = getParent();
        if (p != null) p.repaint();
    }
}
