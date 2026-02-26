import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyEvent;

final class SplitChooserOverlay extends JPanel implements ThemeAware {

    interface ChoiceListener { void onChoice(int rows, int cols); }

    private ChoiceListener listener;
    private Runnable onCustom;

    private final JPanel card;
    private final JLabel title;
    private final JButton easy;
    private final JButton medium;
    private final JButton hard;
    private final JButton custom;

    SplitChooserOverlay() {
        setOpaque(false);
        setLayout(new GridBagLayout());

        card = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Theme.Palette p = ThemeManager.get().palette();
                Theme.Palette.Overlay o = p.overlay;

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Fill
                g2.setColor(o.cardFill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);

                // Stroke
                g2.setColor(o.cardStroke);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 18, 18);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        title = new JLabel("Select puzzle split difficulty");
        title.setFont(new Font("Serif", Font.BOLD, 20));

        easy   = makeChoiceButton("Easy (2×2)",   2, 2);
        medium = makeChoiceButton("Medium (3×3)", 3, 3);
        hard   = makeChoiceButton("Hard (4×4)",   4, 4);
        custom = new JButton("Custom…");

        custom.addActionListener(evt -> {
            if (onCustom != null) {
                onCustom.run();
            } else {
                int rows = askInt("Enter number of rows:");
                int cols = (rows > 0) ? askInt("Enter number of columns:") : -1;
                if (rows > 0 && cols > 0) fireChoice(rows, cols);
            }
        });

        // Layout inside card
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.weightx = 1; c.insets = new Insets(0, 0, 12, 0);
        card.add(title, c);

        JPanel row = new JPanel(new GridLayout(1, 0, 12, 0));
        row.setOpaque(false);
        row.add(easy);
        row.add(medium);
        row.add(hard);
        row.add(custom);

        c.gridy = 1; c.insets = new Insets(0, 0, 0, 0);
        card.add(row, c);

        // Place card in bottom-right of overlay
        GridBagConstraints shell = new GridBagConstraints();
        shell.gridx = 0; shell.gridy = 0;
        shell.weightx = 1; shell.weighty = 1;
        shell.anchor = GridBagConstraints.SOUTHEAST;
        shell.insets = new Insets(0, 0, 24, 24);
        add(card, shell);

        // ESC closes
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getActionMap().put("close", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { close(); }
        });

        // Register for theme updates + apply initial theme
        ThemeManager.get().register(this);
        refreshTheme();
    }

    @Override
    public void refreshTheme() {
        Theme.Palette.Overlay o = ThemeManager.get().palette().overlay;

        title.setForeground(o.text);

        // Buttons in this overlay should use overlay palette (not base palette)
        styleOverlayButton(easy, o);
        styleOverlayButton(medium, o);
        styleOverlayButton(hard, o);
        styleOverlayButton(custom, o);

        repaint();
    }

    private void styleOverlayButton(JButton b, Theme.Palette.Overlay o) {
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 16));
        b.setForeground(o.text);
        b.setBackground(o.buttonBg);
        b.setOpaque(true);
        b.setBorder(new LineBorder(o.buttonBorder));
    }

    private JButton makeChoiceButton(String text, int r, int c) {
        JButton b = new JButton(text);
        b.addActionListener(e -> fireChoice(r, c));
        return b;
    }

    private int askInt(String prompt) {
        String s = JOptionPane.showInputDialog(this, prompt);
        try { return (s == null) ? -1 : Integer.parseInt(s.trim()); }
        catch (Exception ex) { return -1; }
    }

    private void fireChoice(int rows, int cols) {
        if (listener != null) listener.onChoice(rows, cols);
    }

    void openBottom(JComponent glassPane, ChoiceListener l) {
        this.listener = l;
        if (getParent() != glassPane) glassPane.add(this);
        setBounds(0, 0, glassPane.getWidth(), glassPane.getHeight());
        setVisible(true);
        glassPane.setVisible(true);
        glassPane.repaint();
        refreshTheme(); // in case theme changed while hidden
    }

    void close() {
        setVisible(false);
        Container p = getParent();
        if (p != null) p.repaint();
    }

    void setOnCustom(Runnable r) {
        this.onCustom = r;
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Scrim behind the card
        Theme.Palette.Overlay o = ThemeManager.get().palette().overlay;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(o.scrim);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}
