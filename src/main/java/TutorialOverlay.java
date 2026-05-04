import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Themed tutorial overlay: dims the whole window, punches a transparent spotlight
// over a target rectangle, and displays a callout card with an arrow tail that
// points at the target.
// Mouse clicks inside the spotlight pass through to the underlying component,
// this is what makes "click the collection card" steps work naturally.
// Clicks outside the spotlight are absorbed so the user can't stray off-path.
// Owned by TutorialController; this class is layout + rendering only.

public class TutorialOverlay extends JPanel {

    public interface ActionListener {
        void onNext();
        void onSkip();
    }

    private ActionListener listener;

    // Target area for the spotlight, in this overlay's own coordinate space.
    // null = no spotlight (full dim, centred callout).
    private Rectangle spotlight;

    /** Current callout content. */
    private final JPanel card;
    private final JLabel titleLabel;
    private final JLabel messageLabel;
    private final JButton nextButton;
    private final JButton skipButton;

    // If true, the next button is hidden (step advances only when the user performs
    // the gated action, e.g. clicking the spotlighted collection).
    private boolean actionGated = false;

    public TutorialOverlay() {
        setOpaque(false);
        setLayout(null); // we'll position the card manually

        card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(16, 20, 16, 20));
        card.setOpaque(true);

        titleLabel = new JLabel("Welcome!");
        titleLabel.putClientProperty("tutorial-role", "title");
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(10));

        messageLabel = new JLabel();
        messageLabel.putClientProperty("tutorial-role", "message");
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(messageLabel);
        card.add(Box.createVerticalStrut(14));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        skipButton = makeButton("Skip tutorial", false);
        skipButton.addActionListener(e -> { if (listener != null) listener.onSkip(); });
        buttonRow.add(skipButton);

        // Primary action, styled distinctly so it's obviously clickable
        nextButton = makeButton("Next", true);
        nextButton.addActionListener(e -> { if (listener != null) listener.onNext(); });
        buttonRow.add(nextButton);

        card.add(buttonRow);

        add(card);

        // Absorb clicks that are NOT inside the spotlight hole.
        // Clicks inside the hole reach the underlying component because contains(x,y)
        // returns false for those points (see below).
        addMouseListener(new MouseAdapter() {});

        // ESC skips the tutorial
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "tutorial-skip");
        getActionMap().put("tutorial-skip", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (listener != null) listener.onSkip();
            }
        });
    }

    public void setListener(ActionListener l) { this.listener = l; }

    public void configure(String title, String message, Rectangle spotlight,
                          boolean actionGated, boolean isFinalStep) {
        this.spotlight = spotlight != null ? new Rectangle(spotlight) : null;
        this.actionGated = actionGated;
        titleLabel.setText(title);
        // Body width chosen so a serif word like "profile" at line-end doesn't
        // have its last glyph clipped by the HTML layout; the surrounding card
        // maxes at 360 so 290 leaves room for padding and border.
        messageLabel.setText("<html><body style='width:290px'>" + escapeHtml(message) + "</body></html>");
        nextButton.setText(isFinalStep ? "Finish" : "Next");
        nextButton.setVisible(!actionGated);
        // On the final step the tutorial is effectively over, Skip and Finish
        // would do the same thing, so collapse to just Finish to reduce noise.
        skipButton.setVisible(!isFinalStep);
        layoutCard();
        repaint();
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        layoutCard();
    }

    // Position the callout card near the spotlight (or centre-screen if no spotlight).
    // Prefers the side with the most space to avoid clipping.
    private void layoutCard() {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        Dimension cs = card.getPreferredSize();
        // Clamp to a sensible maximum
        int cardW = Math.min(cs.width, 360);
        int cardH = Math.min(cs.height, 220);

        int cx, cy;
        if (spotlight == null) {
            // Centred
            cx = (w - cardW) / 2;
            cy = (h - cardH) / 2;
        } else {
            int gap = 24;
            int spotRight = spotlight.x + spotlight.width;
            int spotBottom = spotlight.y + spotlight.height;

            // Try below first
            if (spotBottom + gap + cardH + 12 <= h) {
                cy = spotBottom + gap;
                cx = Math.max(12, Math.min(spotlight.x + (spotlight.width - cardW) / 2, w - cardW - 12));
            }
            // Then above
            else if (spotlight.y - gap - cardH - 12 >= 0) {
                cy = spotlight.y - gap - cardH;
                cx = Math.max(12, Math.min(spotlight.x + (spotlight.width - cardW) / 2, w - cardW - 12));
            }
            // Then right
            else if (spotRight + gap + cardW + 12 <= w) {
                cx = spotRight + gap;
                cy = Math.max(12, Math.min(spotlight.y + (spotlight.height - cardH) / 2, h - cardH - 12));
            }
            // Then left
            else if (spotlight.x - gap - cardW - 12 >= 0) {
                cx = spotlight.x - gap - cardW;
                cy = Math.max(12, Math.min(spotlight.y + (spotlight.height - cardH) / 2, h - cardH - 12));
            }
            // Last resort: centred
            else {
                cx = (w - cardW) / 2;
                cy = (h - cardH) / 2;
            }
        }

        card.setBounds(cx, cy, cardW, cardH);
    }

    // Route mouse events that fall inside the spotlight hole through to the
    // underlying component, EXCEPT when the point is inside the callout card,
    // the card always absorbs events so the Next / Skip buttons work even when
    // the card overlaps the spotlight area (e.g. step 5, where the whole puzzle
    // panel is the target and the card sits on top of it).
    @Override
    public boolean contains(int x, int y) {
        // Always absorb clicks on the callout card so its buttons fire reliably
        if (card != null && card.getBounds().contains(x, y)) return true;
        // Otherwise, let clicks through the spotlight hole to the target component
        if (spotlight != null && spotlight.contains(x, y)) return false;
        return super.contains(x, y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        if (spotlight == null) {
            // No spotlight,  just a uniform dim across the whole overlay
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, w, h);
        } else {
            // Paint the dim as a complex shape: the full panel MINUS the spotlight.
            // This leaves the spotlight region completely unpainted, so the content
            // pane below shows through naturally. AlphaComposite.CLEAR doesn't work
            // reliably in JLayeredPane.MODAL_LAYER, it leaves a black rectangle
            // instead of a transparent hole.
            int pad = 8;
            int sx = spotlight.x - pad;
            int sy = spotlight.y - pad;
            int sw = spotlight.width + pad * 2;
            int sh = spotlight.height + pad * 2;

            java.awt.geom.Area dim = new java.awt.geom.Area(new Rectangle(0, 0, w, h));
            dim.subtract(new java.awt.geom.Area(
                    new java.awt.geom.RoundRectangle2D.Double(sx, sy, sw, sh, 16, 16)));

            g2.setColor(new Color(0, 0, 0, 170));
            g2.fill(dim);

            // Golden border around the spotlight to draw the eye
            g2.setColor(new Color(255, 215, 0, 220));
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(new java.awt.geom.RoundRectangle2D.Double(sx, sy, sw, sh, 16, 16));

            // Arrow tail pointing from the callout toward the spotlight
            drawArrowTail(g2, sx, sy, sw, sh);
        }

        g2.dispose();
    }

    // Draw a small triangular arrow from the card edge toward the spotlight. */
    private void drawArrowTail(Graphics2D g2, int sx, int sy, int sw, int sh) {
        Rectangle cb = card.getBounds();
        if (cb.width <= 0) return;

        int spotCx = sx + sw / 2;
        int spotCy = sy + sh / 2;
        int cardCx = cb.x + cb.width / 2;
        int cardCy = cb.y + cb.height / 2;

        // Determine which side of the card is closest to the spotlight
        int[] tipX = new int[3];
        int[] tipY = new int[3];
        int tipLen = 14;

        if (cb.y + cb.height <= sy) {
            // Card is above the spotlight, tail from bottom-centre of card, pointing down
            int ax = cardCx;
            int ay = cb.y + cb.height;
            tipX[0] = ax - 8; tipY[0] = ay;
            tipX[1] = ax + 8; tipY[1] = ay;
            tipX[2] = ax;     tipY[2] = ay + tipLen;
        } else if (cb.y >= sy + sh) {
            // Card is below, tail from top-centre, pointing up
            int ax = cardCx;
            int ay = cb.y;
            tipX[0] = ax - 8; tipY[0] = ay;
            tipX[1] = ax + 8; tipY[1] = ay;
            tipX[2] = ax;     tipY[2] = ay - tipLen;
        } else if (cb.x + cb.width <= sx) {
            // Card is to the left, tail from right edge, pointing right
            int ax = cb.x + cb.width;
            int ay = cardCy;
            tipX[0] = ax; tipY[0] = ay - 8;
            tipX[1] = ax; tipY[1] = ay + 8;
            tipX[2] = ax + tipLen; tipY[2] = ay;
        } else if (cb.x >= sx + sw) {
            // Card is to the right, tail from left edge, pointing left
            int ax = cb.x;
            int ay = cardCy;
            tipX[0] = ax; tipY[0] = ay - 8;
            tipX[1] = ax; tipY[1] = ay + 8;
            tipX[2] = ax - tipLen; tipY[2] = ay;
        } else {
            return; // overlapping, skip the tail
        }

        g2.setColor(new Color(255, 215, 0, 230));
        g2.fillPolygon(tipX, tipY, 3);
    }

    // Apply theme-aware styling to the card. Called when the overlay is shown.
    public void refreshTheme() {
        Theme.Palette.Base b = ThemeManager.get().palette().base;
        Theme.Palette.Fonts f = ThemeManager.get().palette().fonts;

        Color cardBg = new Color(b.controlBg.getRed(), b.controlBg.getGreen(), b.controlBg.getBlue(), 240);
        card.setBackground(cardBg);

        titleLabel.setFont(f.bodyBold.deriveFont(Font.BOLD, 16f));
        titleLabel.setForeground(b.text);

        messageLabel.setFont(f.body.deriveFont(13f));
        messageLabel.setForeground(b.text);

        nextButton.setFont(f.button.deriveFont(13f));
        nextButton.setForeground(b.text);
        skipButton.setFont(f.button.deriveFont(12f));
        skipButton.setForeground(b.mutedText);

        repaint();
    }

    private JButton makeButton(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty("tutorial-primary", primary);

        if (primary) {
            // Persistent rounded border so it reads as a button even when idle
            b.setBorderPainted(true);
            b.setBorder(BorderFactory.createCompoundBorder(
                    new javax.swing.border.LineBorder(
                            ThemeManager.get().palette().base.text, 1, true),
                    BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        } else {
            b.setBorderPainted(false);
            b.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        }

        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                Color line = ThemeManager.get().palette().base.text;
                if (primary) {
                    b.setBorder(BorderFactory.createCompoundBorder(
                            new javax.swing.border.LineBorder(line, 2, true),
                            BorderFactory.createEmptyBorder(3, 11, 3, 11)));
                } else {
                    b.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 0, line),
                            BorderFactory.createEmptyBorder(3, 7, 3, 7)));
                }
                b.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                Color line = ThemeManager.get().palette().base.text;
                if (primary) {
                    b.setBorder(BorderFactory.createCompoundBorder(
                            new javax.swing.border.LineBorder(line, 1, true),
                            BorderFactory.createEmptyBorder(4, 12, 4, 12)));
                } else {
                    b.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                }
                b.repaint();
            }
        });
        return b;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
