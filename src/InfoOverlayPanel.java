import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URI;

public class InfoOverlayPanel extends JPanel {

    // layout / sizing constants
    private static final int MARGIN = 20;

    // starting size
    private static final int START_W = 480;
    private static final int START_H = 420;

    // minimum size
    private static final int MIN_W = 320;
    private static final int MIN_H = 220;

    // preferred preview height before clamping
    private static final int PREVIEW_H = 160;

    // preview box shouldn't be taller than 30% of panel height
    private static final double PREVIEW_MAX_FRACTION = 0.30;
    // preview box shouldn't shrink below this
    private static final int PREVIEW_MIN_PX = 100;

    // how close to an edge counts as resize grab
    private static final int RESIZE_MARGIN = 10;

    // UI components
    private JLabel imageLabel;
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

        // IMAGE PREVIEW (top)
        imageLabel = new JLabel();
        imageLabel.setOpaque(true);
        imageLabel.setBackground(new Color(30, 30, 30));
        imageLabel.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        add(imageLabel);

        // TITLE (under image)
        titleLabel = new JLabel("Artifact Title");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 22));
        add(titleLabel);

        // DESCRIPTION (scrollable)
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

        // LEARN MORE BUTTON (bottom-left)
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
                    // 1:1 cursor movement using SCREEN coordinates (no lag, no shake)
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
                        case RESIZE_E -> {
                            newBounds.width = startBounds.width + dx;
                        }
                        case RESIZE_S -> {
                            newBounds.height = startBounds.height + dy;
                        }
                        case RESIZE_SE -> {
                            newBounds.width = startBounds.width + dx;
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

        // initial layout
        layoutChildrenForCurrentSize();
        lastW = getWidth();
        lastH = getHeight();
    }

    /**
     * Decide where everything sits given current panel size.
     * Rules:
     * - We cap the image height so it can't eat the whole panel.
     * - We always reserve space for the "Learn more" button at the bottom.
     * - The scrollPane fills ALL the vertical space between title and button.
     * - If the text overflows, the scroll bar appears. So text is never lost.
     */
    private void layoutChildrenForCurrentSize() {
        int w = getWidth();
        int h = getHeight();

        // --- image box height logic ---
        int maxImgHByFraction = (int) Math.round(h * PREVIEW_MAX_FRACTION);

        int imgH = PREVIEW_H;
        imgH = Math.min(imgH, maxImgHByFraction);
        imgH = Math.max(imgH, PREVIEW_MIN_PX);
        imgH = Math.min(imgH, h - 140); // never let it take literally everything in tiny panels

        int imgX = MARGIN;
        int imgY = MARGIN;
        int imgW = w - 2 * MARGIN;
        imageLabel.setBounds(imgX, imgY, imgW, imgH);

        // title below image
        int titleX = MARGIN;
        int titleY = imgY + imgH + 10;
        int titleW = w - 2 * MARGIN;
        int titleH = 32;
        titleLabel.setBounds(titleX, titleY, titleW, titleH);

        // learn more button at bottom-left
        int buttonW = 200;
        int buttonH = 40;
        int buttonX = MARGIN;
        int buttonY = h - MARGIN - buttonH;
        if (buttonY < titleY + titleH + 10) {
            // in a super-short panel, don't let the button overlap title
            buttonY = titleY + titleH + 10;
        }
        learnMoreButton.setBounds(buttonX, buttonY, buttonW, buttonH);

        // scrollPane fills from under title down to just above the button
        int scrollX = MARGIN;
        int scrollY = titleY + titleH + 10;
        int scrollW = w - 2 * MARGIN;
        int scrollH = buttonY - 10 - scrollY; // stop slightly above button
        if (scrollH < 40) {
            scrollH = 40; // always leave at least some visible text area
        }
        scrollPane.setBounds(scrollX, scrollY, scrollW, scrollH);
    }

    /**
     * Scale the preview image to fit cleanly in its box while keeping aspect ratio.
     */
    private ImageIcon createScaledIconKeepAspect(Image srcImage, int maxW, int maxH) {
        int imgW = srcImage.getWidth(null);
        int imgH = srcImage.getHeight(null);

        if (imgW <= 0 || imgH <= 0) {
            return new ImageIcon(srcImage);
        }

        double scaleW = (double) maxW / imgW;
        double scaleH = (double) maxH / imgH;
        double scale = Math.min(scaleW, scaleH);

        int newW = (int) Math.round(imgW * scale);
        int newH = (int) Math.round(imgH * scale);

        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resized.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(srcImage, 0, 0, newW, newH, null);
        g2.dispose();

        return new ImageIcon(resized);
    }

    /**
     * Update the overlay's content.
     * descArea ALWAYS gets full text. No truncation.
     * If it's long, the JScrollPane scrolls.
     */
    public void updateContent(ImageIcon fullImageIcon,
                              String title,
                              String description,
                              String url) {

        // title
        titleLabel.setText(title != null ? title : "Artifact Information");

        // full description (cleaned from HTML-ish blocks)
        if (description == null) description = "";
        descArea.setText(cleanDescription(description));
        descArea.setCaretPosition(0);

        // url for learn more
        moreInfoUrl = url;

        // scaled image into the preview box
        if (fullImageIcon != null && fullImageIcon.getImage() != null) {
            int boxW = imageLabel.getWidth();
            int boxH = imageLabel.getHeight();
            if (boxW <= 0 || boxH <= 0) {
                // if updateContent is called before first layout,
                // fall back to intended dimensions
                boxW = getWidth() - 2 * MARGIN;
                boxH = PREVIEW_H;
            }

            ImageIcon scaled = createScaledIconKeepAspect(
                    fullImageIcon.getImage(),
                    boxW - 4,
                    boxH - 4
            );
            imageLabel.setIcon(scaled);
        } else {
            imageLabel.setIcon(null);
        }

        // relayout just in case size changed earlier
        layoutChildrenForCurrentSize();
        lastW = getWidth();
        lastH = getHeight();

        repaint();
    }

    /**
     * Convert museum HTML-ish description into readable multiline plain text.
     */
    private String cleanDescription(String raw) {
        String s = raw;

        // <br> → newline
        s = s.replaceAll("(?i)<br\\s*/?>", "\n");

        // block-ish tags → newline boundaries
        s = s.replaceAll("(?i)</?(div|p|ul|ol|li|h1|h2|h3|h4|h5|h6)[^>]*>", "\n");

        // strip any other <...>
        s = s.replaceAll("<[^>]+>", "");

        // normalize
        s = s.replace("\r", "");

        // collapse 3+ newlines -> 2
        s = s.replaceAll("\n{3,}", "\n\n");

        // trim
        s = s.trim();

        return s;
    }

    /**
     * Open "Learn more" URL in browser.
     */
    private void openMoreInfoUrl() {
        if (moreInfoUrl == null || moreInfoUrl.isEmpty()) return;
        try {
            Desktop.getDesktop().browse(new URI(moreInfoUrl));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Unable to open link.");
        }
    }

    /**
     * Show the overlay on the given glass pane at (x,y).
     * If it's not already a child of that glass pane, add it.
     */
    public void open(JComponent glassPane, int x, int y) {
        if (getParent() != glassPane) {
            glassPane.add(this);
        }

        setLocation(x, y);

        glassPane.setVisible(true);
        setVisible(true);
        glassPane.repaint();
    }

    /**
     * Hide overlay without destroying it.
     */
    public void close() {
        setVisible(false);
        Container p = getParent();
        if (p != null) {
            p.repaint();
        }
    }
}
