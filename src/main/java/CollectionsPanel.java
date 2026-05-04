import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class CollectionsPanel extends JPanel implements ThemeAware {
    private JButton ancientCyprusButton;
    private JButton ancientEgyptButton;
    private JButton ancientNearEastButton;
    private JButton ancientGreeceButton;
    private JButton romeButton;

    private JButton backButton;

    private JLabel profileNameLabel;
    private JLabel avatarLabel;
    private JLabel levelLabel;
    private JProgressBar levelBar;
    private JButton heartButton;

    // The clickable profile card container, held as a field so the shared
    // group-hover listener can check whether the cursor is still within the
    // card when a child fires mouseExited.
    private JPanel profileCard;
    private boolean profileCardHovered = false;

    private BufferedImage backgroundImage;
    private CollectionsListener listener;

    private String currentCollection = "/Rome/Artifacts/";

    private static final String ORNATE_FRAME =
            "/kenney_fantasy-ui-borders/PNG/Default/Transparent center/panel-transparent-center-030.png";

    public interface CollectionsListener {
        void onCollectionSelected(String collectionPath);
        void onFavouritesClicked();
        void onBackToMenu();
        void onProfileClicked();
    }

    public void setCollectionsListener(CollectionsListener listener) {
        this.listener = listener;
    }

    public CollectionsPanel() {
        setPreferredSize(new Dimension(600, 400));
        setOpaque(true);
        setLayout(new GridBagLayout());

        GameState s = SaveManager.loadOrDefault();
        if (s != null && s.currentCollection != null && !s.currentCollection.isBlank()) {
            currentCollection = s.currentCollection;
        }

        // Collection Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        buttonPanel.setOpaque(false);

        final Dimension fixedSize = new Dimension(260, 44);
        final float normalFont = 18f;
        final float hoverFont  = 22f;

        ancientCyprusButton = makeMenuButton("Ancient Cyprus", fixedSize, normalFont, hoverFont);
        ancientCyprusButton.addActionListener(e -> {
            if (listener != null) listener.onCollectionSelected("/Ancient Cyprus/Artifacts/");
        });
        buttonPanel.add(ancientCyprusButton);

        ancientGreeceButton = makeMenuButton("Ancient Greece", fixedSize, normalFont, hoverFont);
        ancientGreeceButton.addActionListener(e -> {
            if (listener != null) listener.onCollectionSelected("/Ancient Greece/Artifacts/");
        });
        buttonPanel.add(ancientGreeceButton);

        ancientEgyptButton = makeMenuButton("Ancient Egypt", fixedSize, normalFont, hoverFont);
        ancientEgyptButton.addActionListener(e -> {
            if (listener != null) listener.onCollectionSelected("/Ancient Egypt/Artifacts/");
        });
        buttonPanel.add(ancientEgyptButton);

        ancientNearEastButton = makeMenuButton("Ancient Near East", fixedSize, normalFont, hoverFont);
        ancientNearEastButton.addActionListener(e -> {
            if (listener != null) listener.onCollectionSelected("/Ancient Near East/Artifacts/");
        });
        buttonPanel.add(ancientNearEastButton);

        romeButton = makeMenuButton("Ancient Rome", fixedSize, normalFont, hoverFont);
        romeButton.addActionListener(e -> {
            if (listener != null) listener.onCollectionSelected("/Rome/Artifacts/");
        });
        buttonPanel.add(romeButton);

        backButton = makeMenuButton("\u2190 Back", fixedSize, normalFont, hoverFont);
        backButton.addActionListener(e -> {
            if (listener != null) listener.onBackToMenu();
        });
        buttonPanel.add(backButton);

        OrnateFramePanel framedButtons = new OrnateFramePanel(
                buttonPanel, 8, 10, 1, ORNATE_FRAME
        );

        // Profile Card
        JPanel profilePanel = new JPanel();
        profilePanel.setOpaque(false);
        profilePanel.setLayout(new BoxLayout(profilePanel, BoxLayout.Y_AXIS));

        // One shared mouse listener, every element in the top half navigates to
        // the profile page. Larger click target = more forgiving, following the
        // same pattern as clickable card components in web/mobile UIs.
        MouseAdapter toProfile = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (listener != null) listener.onProfileClicked();
            }
        };
        Cursor handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

        // profileCard wraps the avatar/name/level/XP group.
        // It paints a rounded-rect halo behind its children so the four elements read as a
        // single clickable surface, same pattern as the menu buttons.
        profileCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, profileCardHovered ? 180 : 150));
                int inset = 4;
                g2.fillRoundRect(inset, inset, getWidth() - 2 * inset, getHeight() - 2 * inset, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        profileCard.setOpaque(false);
        profileCard.setLayout(new BoxLayout(profileCard, BoxLayout.Y_AXIS));
        profileCard.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        profileCard.setCursor(handCursor);
        profileCard.setToolTipText("View profile");
        profileCard.addMouseListener(toProfile);

        // Avatar
        avatarLabel = new JLabel();
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatarLabel.setPreferredSize(new Dimension(100, 100));
        avatarLabel.setCursor(handCursor);
        avatarLabel.setToolTipText("View profile");
        avatarLabel.addMouseListener(toProfile);
        profileCard.add(avatarLabel);

        profileCard.add(Box.createVerticalStrut(8));

        // Name, also clickable
        profileNameLabel = new JLabel("");
        profileNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        profileNameLabel.setCursor(handCursor);
        profileNameLabel.setToolTipText("View profile");
        profileNameLabel.addMouseListener(toProfile);
        profileCard.add(profileNameLabel);

        profileCard.add(Box.createVerticalStrut(6));

        // Level label, also clickable
        levelLabel = new JLabel("Level 1");
        levelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        levelLabel.setCursor(handCursor);
        levelLabel.setToolTipText("View profile");
        levelLabel.addMouseListener(toProfile);
        profileCard.add(levelLabel);

        profileCard.add(Box.createVerticalStrut(4));

        // XP progress bar, also clickable. JProgressBar normally swallows mouse
        // events; we attach the listener directly so clicks navigate the same way.
        levelBar = new JProgressBar(0, 100);

        // Force the cross-platform UI so track/fill colours actually take effect on
        // macOS (Aqua's native progress bar ignores setBackground/setForeground).
        levelBar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI());
        levelBar.setValue(0);
        levelBar.setStringPainted(true);
        levelBar.setString("0 / 100 XP");
        levelBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        levelBar.setMaximumSize(new Dimension(160, 18));
        levelBar.setPreferredSize(new Dimension(160, 18));
        levelBar.setBorder(BorderFactory.createLineBorder(new Color(120, 90, 30), 1));
        levelBar.setCursor(handCursor);
        levelBar.setToolTipText("View profile");
        levelBar.addMouseListener(toProfile);
        profileCard.add(levelBar);

        // Group-hover: all four children (and the card itself) share a listener
        // that treats the card as one unit. Child mouseExited events can fire
        // spuriously when moving between siblings, so we re-check against the
        // card's bounds before clearing the hovered flag.
        MouseAdapter groupHover = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { setProfileCardHovered(true); }
            @Override public void mouseExited(MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (profileCard == null || !profileCard.isShowing()) return;
                    Point mouse = MouseInfo.getPointerInfo().getLocation();
                    Point origin = profileCard.getLocationOnScreen();
                    Rectangle bounds = new Rectangle(origin, profileCard.getSize());
                    if (!bounds.contains(mouse)) setProfileCardHovered(false);
                });
            }
        };
        profileCard.addMouseListener(groupHover);
        avatarLabel.addMouseListener(groupHover);
        profileNameLabel.addMouseListener(groupHover);
        levelLabel.addMouseListener(groupHover);
        levelBar.addMouseListener(groupHover);

        // Fix the slot size to the card's hovered maximum so the card grows
        // inside it on hover, otherwise the size change propagates up through
        // profilePanel and reflows the ornate frame every time the cursor lands.
        JPanel profileCardSlot = new JPanel(new GridBagLayout());
        profileCardSlot.setOpaque(false);
        Dimension cardSlotSize = new Dimension(210, 220);
        profileCardSlot.setPreferredSize(cardSlotSize);
        profileCardSlot.setMinimumSize(cardSlotSize);
        profileCardSlot.setMaximumSize(cardSlotSize);
        profileCardSlot.setAlignmentX(Component.CENTER_ALIGNMENT);
        profileCardSlot.add(profileCard);
        profilePanel.add(profileCardSlot);

        profilePanel.add(Box.createVerticalStrut(10));

        // "Favourites" button, reuses makeMenuButton so font weight, halo,
        // hover-grow, and proportions match the civilisation buttons on the
        // right frame exactly. Slightly narrower than 260 so it fits inside
        // the profile column (~210 wide) while keeping the same 44px height
        // and the shared 18f to 22f hover font scale.
        heartButton = makeMenuButton("Favourites \u2665", new Dimension(200, 44), normalFont, hoverFont);
        heartButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        heartButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        heartButton.setToolTipText("View your favourite jigsaws");
        heartButton.addActionListener(e -> {
            if (listener != null) listener.onFavouritesClicked();
        });
        profilePanel.add(heartButton);

        profilePanel.add(Box.createVerticalStrut(6));

        OrnateFramePanel framedProfile = new OrnateFramePanel(
                profilePanel, 8, 10, 1, ORNATE_FRAME
        );

        // Layout: profile on left, buttons on right
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;

        gbc.gridx = 0;
        gbc.insets = new Insets(18, 18, 18, 8);
        add(framedProfile, gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(18, 8, 18, 18);
        add(framedButtons, gbc);

        ThemeManager.get().register(this);
        refreshProfile();
        refreshTheme();
    }

    // Reload profile info labels from the active save state.
    public void refreshProfile() {
        GameState s = SaveManager.loadOrDefault();
        String name = (s.profileName != null && !s.profileName.isBlank())
                ? s.profileName : "Guest";
        profileNameLabel.setText(name);

        // Avatar, size depends on current hover state so a refresh during
        // hover doesn't snap the avatar back to its resting size.
        BufferedImage avatar = AvatarChooserPanel.loadCachedAvatar(s.avatarImagePath);
        if (avatar != null) {
            int sz = profileCardHovered ? 104 : 90;
            Image scaled = avatar.getScaledInstance(sz, sz, Image.SCALE_SMOOTH);
            avatarLabel.setIcon(new ImageIcon(scaled));
            avatarLabel.setText("");
        } else {
            avatarLabel.setIcon(null);
            avatarLabel.setText("No Avatar");
            avatarLabel.setFont(new Font("Serif", Font.ITALIC, 12));
        }

        // Level + XP bar
        int level = Math.max(1, s.level);
        int xpForNext = AchievementManager.xpForLevel(level);
        int xpInLevel = s.xp - AchievementManager.totalXpForLevel(level);
        if (xpInLevel < 0) xpInLevel = 0;

        levelLabel.setText("Level " + level);
        levelBar.setMaximum(Math.max(1, xpForNext));
        levelBar.setValue(Math.min(xpInLevel, xpForNext));
        levelBar.setString(xpInLevel + " / " + xpForNext + " XP");

        // Apply colors
        Color text = ThemeManager.get().palette().base.text;
        profileNameLabel.setForeground(text);
        avatarLabel.setForeground(text);
        levelLabel.setForeground(text);
    }


    // Toggle the hover state of the profile card, bumps avatar size, name
    // and level fonts, and the XP bar so the group visibly grows together.
    // The halo behind the card redraws automatically via repaint.
    private void setProfileCardHovered(boolean hovered) {
        if (profileCardHovered == hovered) return;
        profileCardHovered = hovered;

        // Re-scale avatar from the cached avatar image, at the new size.
        GameState s = SaveManager.loadOrDefault();
        BufferedImage avatar = AvatarChooserPanel.loadCachedAvatar(s != null ? s.avatarImagePath : null);
        if (avatar != null) {
            int sz = hovered ? 104 : 90;
            Image scaled = avatar.getScaledInstance(sz, sz, Image.SCALE_SMOOTH);
            avatarLabel.setIcon(new ImageIcon(scaled));
        }

        Theme.Palette.Fonts f = ThemeManager.get().palette().fonts;
        if (profileNameLabel != null) {
            float size = f.heading.getSize2D() + (hovered ? 2f : 0f);
            profileNameLabel.setFont(f.heading.deriveFont(size));
        }
        if (levelLabel != null) {
            float size = f.caption.getSize2D() + (hovered ? 1f : 0f);
            levelLabel.setFont(f.caption.deriveFont(size));
        }
        if (levelBar != null) {
            Dimension d = hovered ? new Dimension(176, 22) : new Dimension(160, 18);
            levelBar.setPreferredSize(d);
            levelBar.setMaximumSize(d);
        }

        if (profileCard != null) {
            profileCard.revalidate();
            profileCard.repaint();
        }
    }

    private JButton makeMenuButton(
            String text,
            Dimension fixedSize,
            float normalFont,
            float hoverFont
    ) {
        // Anonymous subclass overrides paintComponent to draw a luminance-adaptive
        // halo behind the text. Usability testing (P2 observed, P4 mentioned) showed
        // dark text was unreadable against certain dark background images in light
        // mode, the halo guarantees contrast regardless of what's behind the button.
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                drawTextHalo(this, g);
                super.paintComponent(g);
            }
        };
        b.setFont(b.getFont().deriveFont(Font.BOLD, normalFont));
        b.setPreferredSize(fixedSize);
        b.setMinimumSize(fixedSize);
        b.setMaximumSize(fixedSize);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFocusable(false);

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

    // Draw a soft pill behind the button's text so it reads cleanly on any background.
    // The pill colour is chosen to contrast with the text colour (dark text gets a
    // light pill, light text gets a dark pill), so this works in both themes.
    private static void drawTextHalo(JButton btn, Graphics g) {
        String text = btn.getText();
        if (text == null || text.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color textColor = btn.getForeground();
        double lum = 0.299 * textColor.getRed()
                   + 0.587 * textColor.getGreen()
                   + 0.114 * textColor.getBlue();
        Color halo = lum > 128
                ? new Color(0, 0, 0, 150) // dark pill under light text
                : new Color(255, 255, 255, 150); // light pill under dark text

        FontMetrics fm = g2.getFontMetrics(btn.getFont());
        int textW = fm.stringWidth(text);
        int textH = fm.getAscent();
        int padX = 18, padY = 8;
        int pillW = textW + padX * 2;
        int pillH = textH + padY * 2;
        int pillX = (btn.getWidth() - pillW) / 2;
        int pillY = (btn.getHeight() - pillH) / 2;

        g2.setColor(halo);
        g2.fillRoundRect(pillX, pillY, pillW, pillH, 16, 16);
        g2.dispose();
    }

    public void setCurrentCollection(String collectionPath) {
        this.currentCollection = collectionPath;
        refreshTheme();
    }

    // Target component for the tutorial's "pick a collection" step.
    // Returns the first collection card so the pointer lands on something familiar.
    public JComponent getTutorialTargetCard() {
        if (ancientCyprusButton != null) return ancientCyprusButton;
        if (ancientGreeceButton != null) return ancientGreeceButton;
        if (romeButton != null) return romeButton;
        return null;
    }

    @Override
    public void refreshTheme() {
        Theme theme = ThemeManager.get().getCurrent();
        boolean dark = theme == Theme.DARK;
        String path = BackgroundCatalog.backgroundFor(currentCollection, theme);

        try (var in = getClass().getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Missing resource: " + path);
            backgroundImage = ImageIO.read(in);
        } catch (Exception e) {
            e.printStackTrace();
            backgroundImage = null;
        }

        Theme.Palette.Base b = ThemeManager.get().palette().base;
        Theme.Palette.Fonts f = ThemeManager.get().palette().fonts;
        Color text = b.text;

        if (ancientCyprusButton != null) ancientCyprusButton.setForeground(text);
        if (ancientGreeceButton != null) ancientGreeceButton.setForeground(text);
        if (ancientEgyptButton != null) ancientEgyptButton.setForeground(text);
        if (ancientNearEastButton != null) ancientNearEastButton.setForeground(text);
        if (romeButton != null) romeButton.setForeground(text);
        if (backButton != null) backButton.setForeground(text);

        if (profileNameLabel != null) {
            profileNameLabel.setFont(f.heading);
            profileNameLabel.setForeground(text);
        }
        if (levelLabel != null) {
            levelLabel.setFont(f.caption);
            levelLabel.setForeground(b.mutedText);
        }
        if (levelBar != null) {
            levelBar.setBackground(dark ? new Color(40, 40, 40) : new Color(220, 220, 220));
            levelBar.setForeground(new Color(180, 160, 60)); // gold XP bar
            levelBar.setFont(new Font(f.caption.getFamily(), Font.PLAIN, 10));
        }
        if (avatarLabel != null) {
            avatarLabel.setForeground(b.mutedText);
        }
        if (heartButton != null) {
            heartButton.setForeground(text);
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(ThemeManager.get().palette().base.appBg);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
