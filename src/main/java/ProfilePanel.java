import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class ProfilePanel extends JPanel implements ThemeAware {

    private JLabel avatarLabel;
    private JPanel avatarCard;
    private boolean avatarHovered = false;
    private JLabel nameLabel;
    private JLabel levelLabel;
    private JProgressBar xpBar;
    private JLabel achTitle;
    private JPanel achievementsPanel;
    private JButton backButton;

    private static final int AVATAR_REST_PX = 120;
    private static final int AVATAR_HOVER_PX = 140;

    private BufferedImage backgroundImage;

    private ProfileListener listener;

    private static final String ORNATE_FRAME =
            "/kenney_fantasy-ui-borders/PNG/Default/Transparent center/panel-transparent-center-030.png";

    // Match ProgressPanel / FavouritesPanel: the left frame holds the profile,
    // the right frame holds the achievements, visually split, narrower than
    // the old edge-to-edge BorderLayout.WEST/CENTER version.
    // Heights are kept equal so the two frames read as a matched pair.
    private static final int FRAME_HEIGHT = 360;
    private static final int LEFT_FRAME_WIDTH = 230;
    // HE-22: the achievements panel now sizes responsively to the window width.
    // RIGHT_FRAME_WIDTH_MIN is the floor for narrow windows (still enough for icon + text column + progress bar).
    // RIGHT_FRAME_WIDTH_MAX is the cap for very wide windows so a single column of achievements
    // doesn't sprawl beyond a comfortable reading width.
    private static final int RIGHT_FRAME_WIDTH_MIN = 380;
    private static final int RIGHT_FRAME_WIDTH_MAX = 700;

    public interface ProfileListener {
        void onChangeAvatarClicked();
        void onBackClicked();
    }

    public void setProfileListener(ProfileListener listener) {
        this.listener = listener;
    }

    public ProfilePanel() {
        setLayout(new GridBagLayout());
        setOpaque(false);

        OrnateFramePanel framedLeft = buildLeftFrame();
        OrnateFramePanel framedRight = buildRightFrame();

        backButton = makeMenuButton("\u2190 Back");
        backButton.addActionListener(e -> {
            if (listener != null) listener.onBackClicked();
        });
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottom.setOpaque(false);
        bottom.add(backButton);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;

        gbc.gridx = 0;
        gbc.insets = new Insets(18, 18, 10, 8);
        add(framedLeft, gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(18, 8, 10, 18);
        add(framedRight, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 18, 18, 18);
        add(bottom, gbc);

        ThemeManager.get().register(this);
    }

    private OrnateFramePanel buildLeftFrame() {
        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        leftPanel.setPreferredSize(new Dimension(LEFT_FRAME_WIDTH, FRAME_HEIGHT));

        leftPanel.add(Box.createVerticalGlue());

        // Avatar card: the avatar itself is the "Change avatar" button.
        // Paints a rounded halo behind its contents (darker on hover) to match the other
        // clickable cards in the app, and grows the avatar on hover.
        // Wrapped in a fixed-size slot so the hover-grow doesn't reflow the ornate frame.
        avatarCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, avatarHovered ? 180 : 150));
                int inset = 4;
                g2.fillRoundRect(inset, inset, getWidth() - 2 * inset, getHeight() - 2 * inset, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatarCard.setOpaque(false);
        avatarCard.setLayout(new GridBagLayout());
        Dimension cardSize = new Dimension(AVATAR_HOVER_PX + 24, AVATAR_HOVER_PX + 24);
        avatarCard.setPreferredSize(cardSize);
        avatarCard.setMinimumSize(cardSize);
        avatarCard.setMaximumSize(cardSize);
        avatarCard.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatarCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatarCard.setToolTipText("Change avatar");

        avatarLabel = new JLabel();
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatarLabel.setToolTipText("Change avatar");

        MouseAdapter avatarMouse = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (listener != null) listener.onChangeAvatarClicked();
            }
            @Override public void mouseEntered(MouseEvent e) { setAvatarHovered(true); }
            @Override public void mouseExited(MouseEvent e) {
                // Re-check against the card's bounds so moving between the card
                // and the label doesn't flicker the hover state off.
                SwingUtilities.invokeLater(() -> {
                    if (!avatarCard.isShowing()) return;
                    Point mouse = MouseInfo.getPointerInfo().getLocation();
                    Point origin = avatarCard.getLocationOnScreen();
                    Rectangle bounds = new Rectangle(origin, avatarCard.getSize());
                    if (!bounds.contains(mouse)) setAvatarHovered(false);
                });
            }
        };
        avatarCard.addMouseListener(avatarMouse);
        avatarLabel.addMouseListener(avatarMouse);
        avatarCard.add(avatarLabel);
        leftPanel.add(avatarCard);

        leftPanel.add(Box.createVerticalStrut(12));

        nameLabel = new JLabel("Profile");
        nameLabel.setFont(new Font("Serif", Font.BOLD, 22));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(nameLabel);

        leftPanel.add(Box.createVerticalStrut(4));

        levelLabel = new JLabel("Level 1");
        levelLabel.setFont(new Font("Serif", Font.PLAIN, 15));
        levelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(levelLabel);

        leftPanel.add(Box.createVerticalStrut(8));

        // Force BasicProgressBarUI so fill/track colours apply on macOS Aqua.
        xpBar = new JProgressBar(0, 100);
        xpBar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI());
        xpBar.setStringPainted(true);
        xpBar.setString("0 / 100 XP");
        Dimension xpSize = new Dimension(190, 20);
        xpBar.setPreferredSize(xpSize);
        xpBar.setMinimumSize(xpSize);
        xpBar.setMaximumSize(xpSize);
        xpBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        xpBar.setBorder(BorderFactory.createLineBorder(new Color(120, 90, 30), 1));
        leftPanel.add(xpBar);

        leftPanel.add(Box.createVerticalGlue());

        return new OrnateFramePanel(leftPanel, 8, 10, 1, ORNATE_FRAME);
    }

    private OrnateFramePanel buildRightFrame() {
        // HE-22: the right panel computes its preferred width from the parent ProfilePanel's current width
        // on every layout pass, so the achievements panel grows on wider windows and shrinks on narrower ones
        // rather than sitting at a fixed size. Clamped to [MIN, MAX] so it stays readable at extreme window sizes.
        JPanel rightPanel = new JPanel(new BorderLayout(0, 8)) {
            @Override
            public Dimension getPreferredSize() {
                int parentW = ProfilePanel.this.getWidth();
                // Subtract the left frame and the inter-frame / outer insets
                // budgeted by the GridBagLayout so the result is the share of
                // window width the right frame can claim.
                int avail = parentW - LEFT_FRAME_WIDTH - 80;
                int target = Math.max(RIGHT_FRAME_WIDTH_MIN,
                                      Math.min(RIGHT_FRAME_WIDTH_MAX, avail));
                return new Dimension(target, FRAME_HEIGHT);
            }
        };
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        rightPanel.setMinimumSize(new Dimension(RIGHT_FRAME_WIDTH_MIN, FRAME_HEIGHT));

        achTitle = new JLabel("Achievements", SwingConstants.CENTER);
        achTitle.setFont(new Font("Serif", Font.BOLD, 20));
        achTitle.setHorizontalAlignment(SwingConstants.CENTER);
        rightPanel.add(achTitle, BorderLayout.NORTH);

        achievementsPanel = new JPanel();
        achievementsPanel.setOpaque(false);
        achievementsPanel.setLayout(new BoxLayout(achievementsPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(achievementsPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        FantasyScrollBarUI.install(scroll);
        rightPanel.add(scroll, BorderLayout.CENTER);

        return new OrnateFramePanel(rightPanel, 8, 10, 1, ORNATE_FRAME);
    }

    private JButton makeMenuButton(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                drawTextHalo(this, g);
                super.paintComponent(g);
            }
        };
        final float normalFont = 18f;
        final float hoverFont  = 22f;
        b.setFont(b.getFont().deriveFont(Font.BOLD, normalFont));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFocusable(false);
        Dimension btnSize = new Dimension(200, 44);
        b.setPreferredSize(btnSize);
        b.setMinimumSize(btnSize);
        b.setMaximumSize(btnSize);

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
        Color textColor = btn.getForeground();
        double lum = 0.299 * textColor.getRed() + 0.587 * textColor.getGreen() + 0.114 * textColor.getBlue();
        Color halo = lum > 128 ? new Color(0, 0, 0, 150) : new Color(255, 255, 255, 150);
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

    // Toggle the avatar card's hover state, scales the avatar image up/down and repaints the halo.
    // The halo alpha is picked up from avatarHovered inside avatarCard.paintComponent.
    private void setAvatarHovered(boolean hovered) {
        if (avatarHovered == hovered) return;
        avatarHovered = hovered;

        GameState s = SaveManager.loadOrDefault();
        BufferedImage avatar = AvatarChooserPanel.loadCachedAvatar(s != null ? s.avatarImagePath : null);
        if (avatar != null) {
            int sz = hovered ? AVATAR_HOVER_PX : AVATAR_REST_PX;
            Image scaled = avatar.getScaledInstance(sz, sz, Image.SCALE_SMOOTH);
            avatarLabel.setIcon(new ImageIcon(scaled));
        }
        avatarCard.repaint();
    }

    public void refresh() {
        GameState state = SaveManager.loadOrDefault();

        String name = (state.profileName != null && !state.profileName.isBlank())
                ? state.profileName : "Guest";
        nameLabel.setText(name);

        levelLabel.setText("Level " + state.level);

        int currentLevelXp = AchievementManager.totalXpForLevel(state.level);
        int nextLevelXp = AchievementManager.totalXpForLevel(state.level + 1);
        int range = nextLevelXp - currentLevelXp;
        int progress = state.xp - currentLevelXp;
        xpBar.setMaximum(Math.max(range, 1));
        xpBar.setValue(Math.max(progress, 0));
        xpBar.setString(progress + " / " + range + " XP");

        BufferedImage avatar = AvatarChooserPanel.loadCachedAvatar(state.avatarImagePath);
        if (avatar != null) {
            int sz = avatarHovered ? AVATAR_HOVER_PX : AVATAR_REST_PX;
            Image scaled = avatar.getScaledInstance(sz, sz, Image.SCALE_SMOOTH);
            avatarLabel.setIcon(new ImageIcon(scaled));
            avatarLabel.setText("");
        } else {
            avatarLabel.setIcon(null);
            avatarLabel.setText("No Avatar");
        }

        achievementsPanel.removeAll();
        for (var entry : AchievementManager.ALL.entrySet()) {
            String id = entry.getKey();
            AchievementManager.Achievement ach = entry.getValue();
            boolean unlocked = state.achievements.contains(id);
            int prog = AchievementManager.getProgress(state, id);

            JPanel row = createAchievementCard(ach, unlocked, prog);
            row.setAlignmentX(Component.CENTER_ALIGNMENT);
            achievementsPanel.add(row);
            achievementsPanel.add(Box.createVerticalStrut(6));
        }

        refreshTheme();
        revalidate();
        repaint();
    }

    private JPanel createAchievementCard(AchievementManager.Achievement ach,
                                         boolean unlocked, int progress) {
        Theme.Palette.Base b = ThemeManager.get().palette().base;
        Theme.Palette.Fonts f = ThemeManager.get().palette().fonts;

        JPanel row = new JPanel(new BorderLayout(10, 4));
        row.setOpaque(false);
        // Cards fill the scroll viewport width so they read as a tidy column.
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        row.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JLabel icon = new JLabel(unlocked ? "\u2705" : "\uD83D\uDD12");
        icon.setFont(new Font("Serif", Font.PLAIN, 20));
        row.add(icon, BorderLayout.WEST);

        JPanel centre = new JPanel();
        centre.setOpaque(false);
        centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));

        JLabel nameL = new JLabel(ach.name);
        nameL.setFont(f.bodyBold);
        nameL.setForeground(unlocked ? b.text : b.mutedText);
        nameL.setAlignmentX(Component.LEFT_ALIGNMENT);
        centre.add(nameL);

        JLabel descL = new JLabel(ach.description);
        descL.setFont(f.caption);
        descL.setForeground(b.mutedText);
        descL.setAlignmentX(Component.LEFT_ALIGNMENT);
        centre.add(descL);

        JProgressBar bar = new JProgressBar(0, ach.target);
        bar.setValue(Math.min(progress, ach.target));
        bar.setStringPainted(true);
        bar.setString(Math.min(progress, ach.target) + " / " + ach.target);
        bar.setPreferredSize(new Dimension(240, 14));
        bar.setMaximumSize(new Dimension(320, 14));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (unlocked) {
            bar.setForeground(new Color(76, 175, 80));
        }
        centre.add(bar);

        row.add(centre, BorderLayout.CENTER);

        return row;
    }

    @Override
    public void refreshTheme() {
        Theme theme = ThemeManager.get().getCurrent();
        Theme.Palette.Base b = ThemeManager.get().palette().base;
        Color text = b.text;

        // Load the collection background so the profile screen matches the
        // visual context the player was in (Favourites / Progress / Collections all do the same).
        GameState s = SaveManager.loadOrDefault();
        String collection = (s != null && s.currentCollection != null && !s.currentCollection.isBlank())
                ? s.currentCollection : "/Rome/Artifacts/";
        String bgPath = BackgroundCatalog.backgroundFor(collection, theme);
        try (var in = getClass().getResourceAsStream(bgPath)) {
            if (in == null) throw new RuntimeException("Missing resource: " + bgPath);
            backgroundImage = ImageIO.read(in);
        } catch (Exception ex) {
            ex.printStackTrace();
            backgroundImage = null;
        }

        nameLabel.setForeground(text);
        levelLabel.setForeground(text);
        if (achTitle != null) achTitle.setForeground(text);

        // XP bar: gold fill, track colour adapts to theme so the bar itself is always visible even with 0 progress.
        boolean dark = ThemeManager.get().getCurrent() == Theme.DARK;
        xpBar.setForeground(new Color(200, 170, 70));
        xpBar.setBackground(dark ? new Color(50, 50, 50) : new Color(220, 215, 200));

        if (backButton != null) {
            backButton.setForeground(text);
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
