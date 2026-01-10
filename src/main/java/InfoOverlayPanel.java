import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URI;

public class InfoOverlayPanel extends JPanel {

    // ---- layout / sizing constants ----
    private static final int MARGIN = 20;

    // starting size
    private static final int START_W = 480;
    private static final int START_H = 420;

    // minimum size
    private static final int MIN_W = 320;
    private static final int MIN_H = 220;

    // preferred preview height before clamping (fallback when no image yet)
    private static final int PREVIEW_H = 160;

    // preview box shouldn't be taller than 30% of panel height
    private static final double PREVIEW_MAX_FRACTION = 0.30;
    // preview box shouldn't shrink below this
    private static final int PREVIEW_MIN_PX = 100;

    // how close to an edge counts as resize grab
    private static final int RESIZE_MARGIN = 10;

    // responsive layout breakpoints / metrics
    private static final int SIDE_BY_SIDE_BREAKPOINT = 720; // px width to switch to two columns
    private static final double SIDE_IMAGE_FRACTION = 0.42; // fraction of width for image column
    private static final int TITLE_H = 32;
    private static final int BUTTON_W = 200, BUTTON_H = 40;

    // === UI components ===
    private BannerImage banner;     // aspect-preserving banner
    private JLabel titleLabel;
    private JTextArea descArea;
    private JScrollPane scrollPane;
    private JButton learnMoreButton;

    // external link
    private String moreInfoUrl;

    // drag / resize state
    private enum DragMode {
        MOVE,
        RESIZE_N, RESIZE_S, RESIZE_E, RESIZE_W,
        RESIZE_NE, RESIZE_NW, RESIZE_SE, RESIZE_SW,
        NONE
    }

    private DragMode dragMode = DragMode.NONE;

    // for resizing math (component-relative deltas)
    private Point pressPoint;          // mouse point inside panel at mousePressed
    private Rectangle startBounds;     // panel bounds at mousePressed

    // for smooth MOVE drag (screen-space deltas)
    private Point pressMouseScreen;    // mouse screen coords at mousePressed
    private Point pressPanelScreen;    // panel screen coords at mousePressed

    // track last size so we only relayout children when size actually changes
    private int lastW = -1;
    private int lastH = -1;

    public InfoOverlayPanel() {
        setLayout(null);
        setSize(START_W, START_H);

        setBackground(new Color(0, 0, 0, 200));
        setBorder(new LineBorder(new Color(220, 220, 220, 180), 2));
        setOpaque(true);

        // === IMAGE BANNER ===
        banner = new BannerImage();
        banner.setOpaque(false);
        banner.setAllowUpscale(true); // ✅ allow enlarging to fill the column (still aspect-correct)
        add(banner);

        // === TITLE ===
        titleLabel = new JLabel("Artifact Title");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 22));
        add(titleLabel);

        // === DESCRIPTION ===
        descArea = new JTextArea();
        descArea.setEditable(false);
        descArea.setOpaque(false);
        descArea.setForeground(Color.WHITE);
        descArea.setFont(new Font("Serif", Font.PLAIN, 18));
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);

        scrollPane = new JScrollPane(
                descArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane);

        // === LEARN MORE BUTTON (bottom-right) ===
        learnMoreButton = new JButton("Learn more");
        learnMoreButton.setFocusPainted(false);
        learnMoreButton.setFont(new Font("SansSerif", Font.BOLD, 18));
        learnMoreButton.setBackground(Color.BLACK);
        learnMoreButton.setForeground(Color.WHITE);
        learnMoreButton.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        learnMoreButton.addActionListener(e -> openMoreInfoUrl());
        add(learnMoreButton);

        // drag + resize behavior
        MouseAdapter dragAndResize = new MouseAdapter() {

            private DragMode getDragModeForPoint(Point p) {
                boolean left   = p.x < RESIZE_MARGIN;
                boolean right  = p.x > getWidth()  - RESIZE_MARGIN;
                boolean top    = p.y < RESIZE_MARGIN;
                boolean bottom = p.y > getHeight() - RESIZE_MARGIN;

                if (top && left)    return DragMode.RESIZE_NW;
                if (top && right)   return DragMode.RESIZE_NE;
                if (bottom && left) return DragMode.RESIZE_SW;
                if (bottom && right)return DragMode.RESIZE_SE;
                if (top)            return DragMode.RESIZE_N;
                if (bottom)         return DragMode.RESIZE_S;
                if (left)           return DragMode.RESIZE_W;
                if (right)          return DragMode.RESIZE_E;

                return DragMode.MOVE;
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                DragMode mode = getDragModeForPoint(e.getPoint());
                switch (mode) {
                    case RESIZE_N, RESIZE_S ->
                            setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                    case RESIZE_E, RESIZE_W ->
                            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                    case RESIZE_NE, RESIZE_SW ->
                            setCursor(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
                    case RESIZE_NW, RESIZE_SE ->
                            setCursor(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
                    case MOVE ->
                            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    default ->
                            setCursor(Cursor.getDefaultCursor());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                dragMode = getDragModeForPoint(e.getPoint());

                // record state for resizing math
                pressPoint  = e.getPoint();     // mouse position inside panel at press
                startBounds = getBounds();      // panel bounds at press

                // record state for MOVE math (screen coords)
                pressMouseScreen = e.getLocationOnScreen();      // mouse screen pos at press
                Point panelScreenLoc = getLocationOnScreen();    // panel screen pos at press
                pressPanelScreen = new Point(panelScreenLoc);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragMode == DragMode.NONE) return;
                Container parent = getParent();
                if (parent == null) return;

                Rectangle newBounds = new Rectangle(startBounds);

                if (dragMode == DragMode.MOVE) {
                    // 1:1 cursor movement using SCREEN coordinates
                    Point curMouseScreen = e.getLocationOnScreen();
                    int dxScreen = curMouseScreen.x - pressMouseScreen.x;
                    int dyScreen = curMouseScreen.y - pressMouseScreen.y;

                    int newScreenX = pressPanelScreen.x + dxScreen;
                    int newScreenY = pressPanelScreen.y + dyScreen;

                    // convert screen coords back into parent's coordinate space
                    Point parentScreenLoc = parent.getLocationOnScreen();
                    newBounds.x = newScreenX - parentScreenLoc.x;
                    newBounds.y = newScreenY - parentScreenLoc.y;

                } else {
                    // resizing math uses component-relative delta
                    int dx = e.getX() - pressPoint.x;
                    int dy = e.getY() - pressPoint.y;

                    switch (dragMode) {
                        case RESIZE_E -> newBounds.width  = startBounds.width  + dx;
                        case RESIZE_S -> newBounds.height = startBounds.height + dy;
                        case RESIZE_SE -> {
                            newBounds.width  = startBounds.width  + dx;
                            newBounds.height = startBounds.height + dy;
                        }
                        case RESIZE_W -> {
                            newBounds.x = startBounds.x + dx;
                            newBounds.width = startBounds.width - dx;
                        }
                        case RESIZE_N -> {
                            newBounds.y = startBounds.y + dy;
                            newBounds.height = startBounds.height - dy;
                        }
                        case RESIZE_NW -> {
                            newBounds.x = startBounds.x + dx;
                            newBounds.width = startBounds.width - dx;
                            newBounds.y = startBounds.y + dy;
                            newBounds.height = startBounds.height - dy;
                        }
                        case RESIZE_NE -> {
                            newBounds.y = startBounds.y + dy;
                            newBounds.height = startBounds.height - dy;
                            newBounds.width = startBounds.width + dx;
                        }
                        case RESIZE_SW -> {
                            newBounds.x = startBounds.x + dx;
                            newBounds.width = startBounds.width - dx;
                            newBounds.height = startBounds.height + dy;
                        }
                        default -> { /* no-op */ }
                    }
                }

                // enforce min size
                if (newBounds.width  < MIN_W) newBounds.width  = MIN_W;
                if (newBounds.height < MIN_H) newBounds.height = MIN_H;

                // clamp so we don't fully leave the glassPane
                if (newBounds.x < 0) newBounds.x = 0;
                if (newBounds.y < 0) newBounds.y = 0;
                if (newBounds.x + newBounds.width > parent.getWidth()) {
                    newBounds.x = Math.max(0, parent.getWidth() - newBounds.width);
                }
                if (newBounds.y + newBounds.height > parent.getHeight()) {
                    newBounds.y = Math.max(0, parent.getHeight() - newBounds.height);
                }

                // apply new position/size
                setBounds(newBounds);

                // only redo children if size actually changed
                if (newBounds.width != lastW || newBounds.height != lastH) {
                    layoutChildrenForCurrentSize();
                    lastW = newBounds.width;
                    lastH = newBounds.height;
                }

                parent.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragMode = DragMode.NONE;
            }
        };

        addMouseListener(dragAndResize);
        addMouseMotionListener(dragAndResize);

        // re-layout + re-render when the panel itself resizes (programmatically)
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                layoutChildrenForCurrentSize();   // ✅ ensure bounds update
                banner.repaint();
            }
        });

        // initial layout
        layoutChildrenForCurrentSize();
        lastW = getWidth();
        lastH = getHeight();
    }

    /**
     * Decide where everything sits given current panel size.
     */
    private void layoutChildrenForCurrentSize() {
        int w = getWidth();
        int h = getHeight();

        // Common margins
        int x0 = MARGIN;
        int y0 = MARGIN;
        int x1 = w - MARGIN;
        int y1 = h - MARGIN;

        // Title height
        int titleH = TITLE_H;

        // Button (bottom-right)
        int btnW = BUTTON_W, btnH = BUTTON_H;
        int btnX = x1 - btnW;
        int btnY = y1 - btnH;

        if (w >= SIDE_BY_SIDE_BREAKPOINT) {
            // --- SIDE-BY-SIDE LAYOUT ---
            int imageW = (int) Math.round(w * SIDE_IMAGE_FRACTION);
            int imageH = (btnY - y0); // fills down to just above the button

            // Left column: image fills column; letterboxed (no crop / no warp)
            banner.setBounds(x0, y0, imageW, imageH); // ✅ use full column (no "- x0" / "- y0")

            // Right column
            int textX = x0 + imageW + MARGIN;
            int textW = x1 - textX;
            titleLabel.setBounds(textX, y0, textW, titleH);

            int scrollY = y0 + titleH + 10;
            int scrollH = (btnY - 10) - scrollY;
            if (scrollH < 40) scrollH = 40;
            scrollPane.setBounds(textX, scrollY, textW, scrollH);

            learnMoreButton.setBounds(btnX, btnY, btnW, btnH);
        } else {
            // --- STACKED LAYOUT ---
            int maxImgHByFraction = (int) Math.round(h * PREVIEW_MAX_FRACTION);
            int imgH = PREVIEW_H;
            imgH = Math.min(imgH, maxImgHByFraction);
            imgH = Math.max(imgH, PREVIEW_MIN_PX);
            imgH = Math.min(imgH, h - 140);

            banner.setBounds(x0, y0, x1 - x0, imgH);

            int titleY = y0 + imgH + 10;
            titleLabel.setBounds(x0, titleY, x1 - x0, titleH);

            learnMoreButton.setBounds(btnX, btnY, btnW, btnH);

            int scrollY = titleY + titleH + 10;
            int scrollH = btnY - 10 - scrollY;
            if (scrollH < 40) scrollH = 40;
            scrollPane.setBounds(x0, scrollY, x1 - x0, scrollH);
        }
    }

    /**
     * Update the overlay's content.
     * descArea ALWAYS gets full text. No truncation.
     */
    public void updateContent(ImageIcon fullImageIcon,
                              String title,
                              String description,
                              String url) {
        titleLabel.setText(title != null ? title : "Artifact Information");

        if (description == null) description = "";
        descArea.setText(cleanDescription(description));
        descArea.setCaretPosition(0);

        moreInfoUrl = url;

        // Let the banner do the aspect-preserving fit. No pre-scaling needed.
        if (fullImageIcon != null && fullImageIcon.getImage() != null) {
            banner.setImage(toBuffered(fullImageIcon.getImage()));
        } else {
            banner.setImage(null);
        }

        layoutChildrenForCurrentSize();
        lastW = getWidth();
        lastH = getHeight();
        repaint();
    }

    private BufferedImage toBuffered(Image img) {
        if (img instanceof BufferedImage bi) return bi;
        int w = Math.max(1, img.getWidth(null));
        int h = Math.max(1, img.getHeight(null));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        return out;
    }

    private String cleanDescription(String raw) {
        String s = raw == null ? "" : raw;
        s = s.replaceAll("(?i)<br\\s*/?>", "\n");
        s = s.replaceAll("(?i)</?(div|p|ul|ol|li|h1|h2|h3|h4|h5|h6)[^>]*>", "\n");
        s = s.replaceAll("<[^>]+>", "");
        s = s.replace("\r", "");
        s = s.replaceAll("\n{3,}", "\n\n");
        return s.trim();
    }

    private void openMoreInfoUrl() {
        if (moreInfoUrl == null || moreInfoUrl.isEmpty()) return;
        try {
            Desktop.getDesktop().browse(new URI(moreInfoUrl));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Unable to open link.");
        }
    }

    /** Open at current location. */
    public void open(JComponent glassPane, int x, int y) {
        if (getParent() != glassPane) glassPane.add(this);
        setLocation(x, y);
        glassPane.setVisible(true);
        setVisible(true);
        glassPane.repaint();
    }

    /** Convenience: open sized to trigger side-by-side and right-aligned. */
    public void openRightAligned(JComponent glassPane) {
        if (getParent() != glassPane) glassPane.add(this);

        int gpW = glassPane.getWidth();
        int gpH = glassPane.getHeight();

        int targetW = Math.max(SIDE_BY_SIDE_BREAKPOINT + 40, (int)(gpW * 0.70)); // ensure breakpoint
        int targetH = Math.min(gpH - 2 * MARGIN, (int)(gpH * 0.75));

        setSize(Math.min(targetW, gpW - 2 * MARGIN), Math.max(MIN_H, targetH));
        setLocation(gpW - getWidth() - MARGIN, MARGIN);

        glassPane.setVisible(true);
        setVisible(true);
        glassPane.repaint();
    }

    public void close() {
        setVisible(false);
        Container p = getParent();
        if (p != null) p.repaint();
    }

    // === Aspect-preserving banner component ===
    private static final class BannerImage extends JComponent {
        private BufferedImage img;
        private boolean allowUpscale = false;

        void setImage(BufferedImage b) { this.img = b; revalidate(); repaint(); }
        void setAllowUpscale(boolean allow) { this.allowUpscale = allow; }

        @Override public Dimension getPreferredSize() { return new Dimension(480, 160); }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int cw = getWidth(), ch = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();

            // If you want NO letterbox fill, comment the next two lines.
            // g2.setColor(new Color(0,0,0,120));
            // g2.fillRect(0, 0, cw, ch);

            if (img != null) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                int iw = img.getWidth();
                int ih = img.getHeight();
                double sx = cw / (double) iw;
                double sy = ch / (double) ih;
                double s = Math.min(sx, sy);
                if (!allowUpscale) s = Math.min(1.0, s);

                int w = Math.max(1, (int)Math.round(iw * s));
                int h = Math.max(1, (int)Math.round(ih * s));
                int x = (cw - w) / 2;
                int y = (ch - h) / 2;

                g2.drawImage(img, x, y, w, h, null);
            }
            g2.dispose();
        }
    }
}
