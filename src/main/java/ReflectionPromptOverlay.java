import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

// A themed overlay that displays reflection / prediction prompts.
// Used for the learning-theory PoC (Ancient Cyprus collection).
// Two modes:
// PRE, prediction question with soft choices, shown before play.
// POST, factual reveal + reflection question + "Open more info" button.

public class ReflectionPromptOverlay extends JPanel implements ThemeAware {

    public enum Mode { PRE, POST }

    // Callback when the player dismisses the overlay.
    public interface DismissListener {
        void onDismiss();
    }

    // Callback when "Open more info" is clicked (post mode only).
    public interface MoreInfoListener {
        void onMoreInfo();
    }

    private DismissListener dismissListener;
    private MoreInfoListener moreInfoListener;

    // UI components
    private final JLabel titleLabel;
    private final JTextArea questionArea;
    private final JPanel choicesPanel;
    private final JTextArea revealArea;
    private final JTextArea reflectionArea;
    private final JLabel xpLabel;
    private final JPanel achievementsPanel;
    private final JButton continueButton;
    private final JButton moreInfoButton;
    private final JButton closeSeal;

    private Mode currentMode = Mode.PRE;

    public ReflectionPromptOverlay() {
        setLayout(new BorderLayout());
        setOpaque(false); // transparent, we draw our own card

        // Card panel (the visible box)
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.GRAY, 1, true),
                new EmptyBorder(24, 28, 24, 28)
        ));

        // Title row, title on the left, wax seal close on the right
        titleLabel = new JLabel();

        closeSeal = makeCloseButton();
        closeSeal.addActionListener(e -> {
            if (dismissListener != null) dismissListener.onDismiss();
        });

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerRow.add(titleLabel, BorderLayout.CENTER);
        headerRow.add(closeSeal, BorderLayout.EAST);
        card.add(headerRow);
        card.add(Box.createVerticalStrut(12));

        // Question
        questionArea = createTextArea();
        card.add(questionArea);
        card.add(Box.createVerticalStrut(14));

        // Choices (pre mode)
        choicesPanel = new JPanel();
        choicesPanel.setLayout(new BoxLayout(choicesPanel, BoxLayout.Y_AXIS));
        choicesPanel.setOpaque(false);
        choicesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(choicesPanel);

        // Reveal text (post mode)
        revealArea = createTextArea();
        card.add(revealArea);
        card.add(Box.createVerticalStrut(8));

        // Reflection question (post mode)
        reflectionArea = createTextArea();
        card.add(reflectionArea);
        card.add(Box.createVerticalStrut(12));

        // XP earned
        xpLabel = new JLabel();
        xpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(xpLabel);
        card.add(Box.createVerticalStrut(4));

        // Achievements
        achievementsPanel = new JPanel();
        achievementsPanel.setLayout(new BoxLayout(achievementsPanel, BoxLayout.Y_AXIS));
        achievementsPanel.setOpaque(false);
        achievementsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(achievementsPanel);
        card.add(Box.createVerticalStrut(18));

        // Button row
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        moreInfoButton = new JButton("Open more info");
        moreInfoButton.setFocusable(false);
        moreInfoButton.addActionListener(e -> {
            if (moreInfoListener != null) moreInfoListener.onMoreInfo();
        });
        buttonRow.add(moreInfoButton);

        continueButton = new JButton("Continue");
        continueButton.setFocusable(false);
        continueButton.addActionListener(e -> {
            if (dismissListener != null) dismissListener.onDismiss();
        });
        buttonRow.add(continueButton);

        card.add(buttonRow);

        // Centre the card in the overlay with max width
        card.setMaximumSize(new Dimension(480, 600));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Wrap in a centering box
        JPanel centerer = new JPanel();
        centerer.setLayout(new BoxLayout(centerer, BoxLayout.Y_AXIS));
        centerer.setOpaque(false);
        centerer.add(Box.createVerticalGlue());
        centerer.add(card);
        centerer.add(Box.createVerticalGlue());

        add(centerer, BorderLayout.CENTER);

        // Stay sized to the parent (layered pane) on window resize, otherwise the
        // backdrop doesn't cover the full window if the user resizes while visible.
        addHierarchyBoundsListener(new java.awt.event.HierarchyBoundsAdapter() {
            @Override public void ancestorResized(java.awt.event.HierarchyEvent e) {
                Container p = getParent();
                if (p != null) setBounds(0, 0, p.getWidth(), p.getHeight());
            }
        });

        // ESC key dismisses the overlay, matching the consistent dismissal pattern used
        // by every other overlay in the application. Without this binding, users who
        // learn ESC elsewhere find the reflection prompt unresponsive (HE-12).
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "reflection-dismiss");
        getActionMap().put("reflection-dismiss", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (isVisible() && dismissListener != null) {
                    dismissListener.onDismiss();
                }
            }
        });

        // Apply initial theme
        refreshTheme();
    }

    // Public API

    public void setDismissListener(DismissListener l) { this.dismissListener = l; }
    public void setMoreInfoListener(MoreInfoListener l) { this.moreInfoListener = l; }

    // Configure for pre-puzzle prediction prompt.
    public void showPre(ReflectionPromptData.Prompts prompts) {
        currentMode = Mode.PRE;
        titleLabel.setText("Before you begin\u2026");
        questionArea.setText(prompts.predictionQuestion);
        questionArea.setVisible(true);

        // Build choice labels
        choicesPanel.removeAll();
        if (prompts.predictionChoices != null) {
            for (String choice : prompts.predictionChoices) {
                JLabel choiceLabel = new JLabel("\u2022  " + choice);
                choiceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                choicesPanel.add(choiceLabel);
                choicesPanel.add(Box.createVerticalStrut(4));
            }
        }
        choicesPanel.setVisible(true);

        revealArea.setVisible(false);
        reflectionArea.setVisible(false);
        xpLabel.setVisible(false);
        achievementsPanel.setVisible(false);
        moreInfoButton.setVisible(false);
        continueButton.setText("Start puzzle");

        refreshTheme();
        revalidate();
        repaint();
    }

    // Configure for post-puzzle reflection / reveal with XP and achievements.
    public void showPost(ReflectionPromptData.Prompts prompts, int xpEarned,
                         java.util.List<String> achievementNames) {
        currentMode = Mode.POST;
        titleLabel.setText("Puzzle complete!");
        questionArea.setVisible(false);
        choicesPanel.setVisible(false);

        if (prompts != null) {
            revealArea.setText(prompts.revealText);
            revealArea.setVisible(true);
            reflectionArea.setText(prompts.reflectionQuestion);
            reflectionArea.setVisible(true);
        } else {
            revealArea.setVisible(false);
            reflectionArea.setVisible(false);
        }
        // "Open more info" is always available, it opens the artefact information
        // overlay, which exists for every collection, not just the Cyprus PoC.
        moreInfoButton.setVisible(true);

        // XP line
        if (xpEarned > 0) {
            xpLabel.setText("+" + xpEarned + " XP");
            xpLabel.setVisible(true);
        } else {
            xpLabel.setVisible(false);
        }

        // Achievement badges
        achievementsPanel.removeAll();
        if (achievementNames != null && !achievementNames.isEmpty()) {
            for (String name : achievementNames) {
                JLabel badge = new JLabel("\u2B50 " + name);
                badge.setAlignmentX(Component.LEFT_ALIGNMENT);
                achievementsPanel.add(badge);
                achievementsPanel.add(Box.createVerticalStrut(2));
            }
            achievementsPanel.setVisible(true);
        } else {
            achievementsPanel.setVisible(false);
        }

        continueButton.setText("Continue");

        refreshTheme();
        revalidate();
        repaint();
    }

    // Painting (scrim)
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Theme.Palette.Overlay o = ThemeManager.get().getCurrent().palette.overlay;
        g.setColor(o.scrim);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    // Theme
    @Override
    public void refreshTheme() {
        Theme t = ThemeManager.get().getCurrent();
        Theme.Palette.Overlay o = t.palette.overlay;
        Theme.Palette.Fonts f = t.palette.fonts;

        // Card
        for (Component c : getComponents()) {
            applyCardTheme(c, o, f);
        }

        titleLabel.setFont(f.heading);
        titleLabel.setForeground(o.text);

        questionArea.setFont(f.body);
        questionArea.setForeground(o.text);
        questionArea.setBackground(o.cardFill);

        revealArea.setFont(f.body);
        revealArea.setForeground(o.text);
        revealArea.setBackground(o.cardFill);

        reflectionArea.setFont(f.bodyBold);
        reflectionArea.setForeground(o.text);
        reflectionArea.setBackground(o.cardFill);

        // Choice labels
        for (Component c : choicesPanel.getComponents()) {
            if (c instanceof JLabel) {
                c.setFont(f.body);
                c.setForeground(o.text);
            }
        }

        // XP label
        xpLabel.setFont(f.heading);
        xpLabel.setForeground(new Color(255, 215, 0)); // gold

        // Achievement badges
        for (Component c : achievementsPanel.getComponents()) {
            if (c instanceof JLabel) {
                c.setFont(f.bodyBold);
                c.setForeground(o.text);
            }
        }

        // Buttons
        styleButton(continueButton, o, f);
        styleButton(moreInfoButton, o, f);
    }

    // Helpers
    private JTextArea createTextArea() {
        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setOpaque(false);
        ta.setAlignmentX(Component.LEFT_ALIGNMENT);
        ta.setBorder(null);
        ta.setFocusable(false);
        return ta;
    }

    private JButton makeCloseButton() {
        JButton b = new JButton() {
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
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setPreferredSize(new Dimension(30, 30));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setToolTipText("Close");
        return b;
    }

    private void styleButton(JButton btn, Theme.Palette.Overlay o, Theme.Palette.Fonts f) {
        btn.setFont(f.button);
        btn.setForeground(o.text);
        btn.setBackground(o.buttonBg);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(o.buttonBorder, 1, true),
                new EmptyBorder(6, 16, 6, 16)
        ));
        btn.setOpaque(true);
    }

    private void applyCardTheme(Component comp, Theme.Palette.Overlay o, Theme.Palette.Fonts f) {
        if (comp instanceof JPanel) {
            JPanel p = (JPanel) comp;
            p.setBackground(o.cardFill);
            if (p.getBorder() instanceof javax.swing.border.CompoundBorder) {
                p.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(o.cardStroke, 1, true),
                        new EmptyBorder(24, 28, 24, 28)
                ));
            }
            for (Component child : p.getComponents()) {
                applyCardTheme(child, o, f);
            }
        }
    }
}
