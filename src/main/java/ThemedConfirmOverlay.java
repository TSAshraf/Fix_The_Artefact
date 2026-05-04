import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

// A themed confirmation dialog that replaces JOptionPane.showConfirmDialog.
// Renders as a semi-transparent overlay with an ornate-framed card.
public class ThemedConfirmOverlay extends JPanel implements ThemeAware {

    public interface ConfirmListener {
        void onConfirmed();
        void onCancelled();
    }

    private ConfirmListener listener;
    private final JLabel titleLabel;
    private final JLabel messageLabel;
    private final JButton yesBtn;
    private final JButton noBtn;
    private final JPanel card;

    public ThemedConfirmOverlay() {
        setOpaque(true);
        setLayout(new GridBagLayout());
        setBackground(new Color(0, 0, 0, 160));

        card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(24, 32, 24, 32));

        titleLabel = new JLabel();
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(16));

        messageLabel = new JLabel();
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(messageLabel);
        card.add(Box.createVerticalStrut(24));

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 0));
        btnRow.setOpaque(false);

        noBtn = makeButton("No");
        yesBtn = makeButton("Yes");

        noBtn.addActionListener(e -> {
            if (listener != null) listener.onCancelled();
        });
        yesBtn.addActionListener(e -> {
            if (listener != null) listener.onConfirmed();
        });

        btnRow.add(noBtn);
        btnRow.add(yesBtn);
        btnRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(btnRow);

        // Wax seal icon centred above the title
        JPanel sealRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        sealRow.setOpaque(false);
        JPanel sealIcon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int seal = 36;
                int x = (getWidth() - seal) / 2;
                int y = (getHeight() - seal) / 2;
                LoadGamePanel.paintWaxSeal(g2, x, y, seal, false);
                g2.dispose();
            }
        };
        sealIcon.setOpaque(false);
        sealIcon.setPreferredSize(new Dimension(44, 44));
        sealRow.add(sealIcon);
        sealRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Re-order: seal on top
        card.removeAll();
        card.add(sealRow);
        card.add(Box.createVerticalStrut(12));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(messageLabel);
        card.add(Box.createVerticalStrut(24));
        card.add(btnRow);

        // Card auto-sizes to its content; minimum width keeps the layout visually consistent
        // for short messages, but height grows freely to accommodate long profile names that
        // wrap onto multiple lines.
        card.setMinimumSize(new Dimension(380, 220));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        add(card, gbc);

        // Keep the overlay sized to its parent (layered pane) on window resize so
        // the semi-transparent backdrop always covers the full window.
        addHierarchyBoundsListener(new java.awt.event.HierarchyBoundsAdapter() {
            @Override public void ancestorResized(java.awt.event.HierarchyEvent e) {
                Container p = getParent();
                if (p != null) setBounds(0, 0, p.getWidth(), p.getHeight());
            }
        });

        // Click outside card to cancel
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!card.getBounds().contains(e.getPoint()) && listener != null) {
                    listener.onCancelled();
                }
            }
        });

        // Keyboard shortcuts: Enter = confirm, Backspace / ESC = cancel
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "confirm");
        getActionMap().put("confirm", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isVisible() && listener != null) listener.onConfirmed();
            }
        });

        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "cancel");
        getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isVisible() && listener != null) listener.onCancelled();
            }
        });

        ThemeManager.get().register(this);
        refreshTheme();
    }

    public void show(String title, String message, ConfirmListener listener) {
        this.listener = listener;
        titleLabel.setText(title);
        // HTML escape and wrap on a fixed width, the card itself grows vertically to fit.
        // width:320 gives enough breathing room for long profile names and still wraps cleanly.
        messageLabel.setText("<html><div style='text-align:center;width:320px'>"
                + escapeHtml(message) + "</div></html>");
        // Restore both buttons in case showMessage() previously hid one
        noBtn.setVisible(true);
        yesBtn.setText("Yes");
        noBtn.setText("No");
        setVisible(true);
        // Force the card to re-measure now that the message has changed, otherwise long
        // names render clipped because the previous layout is cached.
        card.invalidate();
        card.revalidate();
        revalidate();
        repaint();
    }

    // Variant of show() for one-button informational alerts (e.g. "save corrupt, backup created").
    // Hides the No button and renames the Yes button to OK; both hand off to the same callback.
    public void showMessage(String title, String message, Runnable onDismiss) {
        ConfirmListener bridge = new ConfirmListener() {
            @Override public void onConfirmed() { if (onDismiss != null) onDismiss.run(); }
            @Override public void onCancelled() { if (onDismiss != null) onDismiss.run(); }
        };
        this.listener = bridge;
        titleLabel.setText(title);
        messageLabel.setText("<html><div style='text-align:center;width:320px'>"
                + escapeHtml(message) + "</div></html>");
        noBtn.setVisible(false);
        yesBtn.setText("OK");
        setVisible(true);
        card.invalidate();
        card.revalidate();
        revalidate();
        repaint();
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private JButton makeButton(String text) {
        JButton b = new JButton(text);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension sz = new Dimension(100, 36);
        b.setPreferredSize(sz);
        b.setMinimumSize(sz);
        b.setMaximumSize(sz);

        final float normalSize = 16f;
        final float hoverSize = 18f;

        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Theme.Palette.Base base = ThemeManager.get().palette().base;
                b.setFont(b.getFont().deriveFont(Font.BOLD, hoverSize));
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, base.text),
                        BorderFactory.createEmptyBorder(4, 8, 4, 8)));
                b.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                b.setFont(b.getFont().deriveFont(Font.BOLD, normalSize));
                b.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                b.repaint();
            }
        });

        return b;
    }

    @Override
    public void refreshTheme() {
        Theme.Palette.Base b = ThemeManager.get().palette().base;
        Theme.Palette.Fonts f = ThemeManager.get().palette().fonts;

        Color cardBg = new Color(b.controlBg.getRed(), b.controlBg.getGreen(), b.controlBg.getBlue(), 230);
        card.setBackground(cardBg);
        card.setOpaque(true);

        titleLabel.setFont(f.heading);
        titleLabel.setForeground(b.text);

        messageLabel.setFont(f.body);
        messageLabel.setForeground(b.mutedText);

        yesBtn.setFont(f.button);
        yesBtn.setForeground(b.text);
        yesBtn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        noBtn.setFont(f.button);
        noBtn.setForeground(b.text);
        noBtn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Semi-transparent backdrop
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}
