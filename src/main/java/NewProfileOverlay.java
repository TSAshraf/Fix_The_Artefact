import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

// Themed overlay for creating a new profile name.
// Replaces the plain JOptionPane with a styled panel matching the game's theme.
// Includes a random name generator based on historical/mythological figures.

public class NewProfileOverlay extends JPanel implements ThemeAware {

    public interface ProfileCreationListener {
        void onProfileCreated(String profileName);
        void onCancelled();
    }

    private ProfileCreationListener listener;
    private final JTextField nameField;
    private final JLabel titleLabel;
    private final JLabel promptLabel;
    private final JLabel errorLabel;
    private final JButton createButton;
    private final JButton cancelButton;
    private final JButton randomButton;
    private final JPanel card;

    // Historical/mythological name fragments for the generator
    private static final String[] PREFIXES = {
            "Apollo", "Athena", "Bastet", "Cybele", "Daedalus",
            "Echo", "Freya", "Gudea", "Horus", "Isis",
            "Janus", "Kore", "Leto", "Minos", "Nefertiti",
            "Orpheus", "Perses", "Rhea", "Selene", "Thoth",
            "Ur", "Vulcan", "Xerxes", "Zagreus", "Aeneas",
            "Brutus", "Clio", "Dido", "Electra", "Galatea",
            "Helios", "Icarus", "Jasper", "Kronos", "Lyra",
            "Medea", "Nyx", "Odin", "Pyxis", "Sable"
    };

    private static final String[] SUFFIXES = {
            "the Bold", "the Wise", "of Ur", "of Knossos",
            "the Seeker", "the Restorer", "of Cyprus", "the Keeper",
            "the Wanderer", "of Thebes", "the Curator", "the Mender",
            "of Delphi", "the Careful", "of Memphis", "the Patient"
    };

    private final Random rng = new Random();

    public NewProfileOverlay() {
        setOpaque(true);
        setLayout(new GridBagLayout());

        // Semi-transparent dark background
        setBackground(new Color(0, 0, 0, 160));

        // Card panel
        card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(28, 36, 28, 36));
        card.setOpaque(true);

        // Title
        titleLabel = new JLabel("New Adventure");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(16));

        // Prompt
        promptLabel = new JLabel("Choose a name for your profile:");
        promptLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(promptLabel);
        card.add(Box.createVerticalStrut(12));

        // Name field + random button row
        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        inputRow.setMaximumSize(new Dimension(340, 38));
        inputRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        nameField = new JTextField(20);
        nameField.setFont(new Font("Serif", Font.PLAIN, 16));
        nameField.setHorizontalAlignment(JTextField.CENTER);
        inputRow.add(nameField, BorderLayout.CENTER);

        randomButton = new JButton("\u2684"); // dice unicode
        randomButton.setToolTipText("Generate a random name");
        randomButton.setFont(new Font("Serif", Font.PLAIN, 20));
        randomButton.setFocusPainted(false);
        randomButton.setPreferredSize(new Dimension(42, 38));
        randomButton.addActionListener(e -> generateRandomName());
        inputRow.add(randomButton, BorderLayout.EAST);

        card.add(inputRow);
        card.add(Box.createVerticalStrut(6));

        // Error label
        errorLabel = new JLabel(" ");
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setForeground(new Color(220, 60, 60));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(12));

        // Buttons row
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        cancelButton = makeStyledButton("\u2190 Back");
        cancelButton.addActionListener(e -> {
            if (listener != null) listener.onCancelled();
        });

        createButton = makeStyledButton("Begin");
        createButton.addActionListener(e -> attemptCreate());

        // Back on the left, Begin on the right, matches every other screen in the
        // app (Collections, Profile, Favourites) so the navigational vocabulary is consistent.
        buttonRow.add(cancelButton);
        buttonRow.add(createButton);
        card.add(buttonRow);

        // Enter key triggers create
        nameField.addActionListener(e -> attemptCreate());

        // Escape key triggers cancel
        nameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (listener != null) listener.onCancelled();
                }
            }
        });

        add(card);

        // Keep the overlay sized to its parent (layered pane) on window resize so
        // the semi-transparent backdrop always covers the full window.
        addHierarchyBoundsListener(new java.awt.event.HierarchyBoundsAdapter() {
            @Override public void ancestorResized(java.awt.event.HierarchyEvent e) {
                Container p = getParent();
                if (p != null) setBounds(0, 0, p.getWidth(), p.getHeight());
            }
        });

        ThemeManager.get().register(this);
        refreshTheme();
    }

    // Halo-styled button: draws a rounded pill behind the text that contrasts with the text colour,
    // and grows the font on hover. Same pattern as the main-menu and profile-card buttons so every
    // clickable in the app shares the same visual vocabulary.
    private JButton makeStyledButton(String text) {
        final float normalFont = 16f;
        final float hoverFont = 20f;
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                drawTextHalo(this, g);
                super.paintComponent(g);
            }
        };
        b.setFont(b.getFont().deriveFont(Font.BOLD, normalFont));
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Fixed box so the hover-grow changes only the halo/text inside and the
        // surrounding button row doesn't reflow on every cursor pass.
        Dimension size = new Dimension(140, 44);
        b.setPreferredSize(size);
        b.setMinimumSize(size);
        b.setMaximumSize(size);

        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                b.setFont(b.getFont().deriveFont(Font.BOLD, hoverFont));
                b.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                b.setFont(b.getFont().deriveFont(Font.BOLD, normalFont));
                b.repaint();
            }
        });
        return b;
    }

    private static void drawTextHalo(JButton btn, Graphics g) {
        String text = btn.getText();
        if (text == null || text.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color tc = btn.getForeground();
        double lum = 0.299 * tc.getRed() + 0.587 * tc.getGreen() + 0.114 * tc.getBlue();
        Color halo = lum > 128 ? new Color(0, 0, 0, 150) : new Color(255, 255, 255, 150);
        FontMetrics fm = g2.getFontMetrics(btn.getFont());
        int textW = fm.stringWidth(text);
        int textH = fm.getAscent();
        int padX = 14, padY = 6;
        int pillW = textW + padX * 2;
        int pillH = textH + padY * 2;
        int pillX = (btn.getWidth() - pillW) / 2;
        int pillY = (btn.getHeight() - pillH) / 2;
        g2.setColor(halo);
        g2.fillRoundRect(pillX, pillY, pillW, pillH, 20, 20);
        g2.dispose();
    }

    public void setListener(ProfileCreationListener listener) {
        this.listener = listener;
    }

    public void reset() {
        nameField.setText("");
        errorLabel.setText(" ");
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
    }

    private void generateRandomName() {
        String prefix = PREFIXES[rng.nextInt(PREFIXES.length)];
        // 40% chance of adding a suffix
        if (rng.nextDouble() < 0.4) {
            String suffix = SUFFIXES[rng.nextInt(SUFFIXES.length)];
            nameField.setText(prefix + " " + suffix);
        } else {
            nameField.setText(prefix);
        }
        nameField.selectAll();
        nameField.requestFocusInWindow();
    }

    private void attemptCreate() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setText("Please enter a name.");
            return;
        }
        if (name.length() > 30) {
            errorLabel.setText("Name must be 30 characters or fewer.");
            return;
        }

        if (SaveManager.profileExists(name)) {
            errorLabel.setText("Profile '" + name + "' already exists. Choose another.");
            return;
        }

        errorLabel.setText(" ");
        if (listener != null) listener.onProfileCreated(name);
    }

    @Override
    public void refreshTheme() {
        Theme.Palette p = ThemeManager.get().palette();
        Theme.Palette.Base b = p.base;
        Theme.Palette.Fonts f = p.fonts;

        setBackground(new Color(0, 0, 0, 160));

        card.setBackground(new Color(b.controlBg.getRed(), b.controlBg.getGreen(),
                b.controlBg.getBlue(), 230));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(b.controlBorder, 1),
                new EmptyBorder(28, 36, 28, 36)));

        titleLabel.setFont(f.title);
        titleLabel.setForeground(b.text);

        promptLabel.setFont(f.body);
        promptLabel.setForeground(b.mutedText);

        errorLabel.setFont(f.caption);

        nameField.setFont(f.body);
        nameField.setBackground(b.inputBg);
        nameField.setForeground(b.text);
        nameField.setCaretColor(b.text);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(b.inputBorder, 1),
                new EmptyBorder(4, 8, 4, 8)));

        randomButton.setBackground(b.controlBg);
        randomButton.setForeground(b.text);
        randomButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(b.controlBorder, 1),
                new EmptyBorder(2, 6, 2, 6)));

        createButton.setForeground(b.text);
        cancelButton.setForeground(b.text);

        repaint();
    }

    // Consume mouse clicks on the background so they don't pass through
    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        e.consume();
    }
}
