import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.List;
import java.util.ArrayList;

public class LoadGamePanel extends JPanel implements ThemeAware {

    private static final String ORNATE_FRAME =
            "/kenney_fantasy-ui-borders/PNG/Default/Transparent center/panel-transparent-center-030.png";

    private BufferedImage backgroundImage;
    private LoadGameListener listener;

    private final JPanel card = new JPanel(new BorderLayout(12, 12));
    private final JLabel title = new JLabel("Load Game");
    private final JPanel listPanel = new JPanel();
    private final JScrollPane scrollPane;
    private final JButton backBtn;
    private final JButton playBtn;
    private final OrnateFramePanel framedCard;

    private final java.util.List<ProfileRowPanel> rows = new ArrayList<>();
    private ThemedConfirmOverlay confirmOverlay;
    // Currently selected profile name, or null if none.
    // Mirrors the jigsaw-selection pattern in ProgressPanel,
    // one click toggles, a dedicated Play button commits.
    private String selectedProfile = null;

    public interface LoadGameListener {
        void onProfileSelected(String profileName);
        void onBackToMenu();
    }

    public void setLoadGameListener(LoadGameListener listener) {
        this.listener = listener;
    }

    public LoadGamePanel() {
        setPreferredSize(new Dimension(600, 400));
        setOpaque(false);
        setLayout(new GridBagLayout());

        title.setHorizontalAlignment(SwingConstants.CENTER);

        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        scrollPane = new JScrollPane(listPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        FantasyScrollBarUI.install(scrollPane);

        card.setOpaque(true);
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        card.add(title, BorderLayout.NORTH);

        scrollPane.setPreferredSize(new Dimension(380, 400));
        card.add(scrollPane, BorderLayout.CENTER);

        backBtn = makeMenuButton("\u2190 Back");
        backBtn.addActionListener(e -> {
            if (listener != null) listener.onBackToMenu();
        });

        playBtn = makeMenuButton("Play");
        playBtn.addActionListener(e -> {
            if (selectedProfile != null && listener != null) {
                listener.onProfileSelected(selectedProfile);
            }
        });
        playBtn.setEnabled(false);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 48, 0));
        bottom.setOpaque(false);
        bottom.add(backBtn);
        bottom.add(playBtn);
        card.add(bottom, BorderLayout.SOUTH);

        framedCard = new OrnateFramePanel(card, 12, 16, 2, ORNATE_FRAME);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(24, 24, 24, 24);
        add(framedCard, gbc);

        setupProfileKeyboardNavigation();

        ThemeManager.get().register(this);
        refreshTheme();
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

    // Toggle the highlighted row and enable/disable the Play button.
    // Single source of truth for which profile the Play button will load.
    private void selectProfile(String name) {
        selectedProfile = name;
        for (ProfileRowPanel r : rows) {
            r.setSelected(name != null && name.equals(r.profileName));
        }
        if (playBtn != null) playBtn.setEnabled(name != null);
    }

    // Call this every time you navigate to this screen.
    public void refreshProfiles() {
        selectedProfile = null;
        if (playBtn != null) playBtn.setEnabled(false);
        listPanel.removeAll();
        rows.clear();

        List<String> profiles = SaveManager.listProfiles();

        if (profiles.isEmpty()) {
            JLabel emptyLabel = new JLabel("No saved profiles found.");
            emptyLabel.setFont(new Font("Serif", Font.ITALIC, 16));
            emptyLabel.setForeground(ThemeManager.get().palette().base.mutedText);
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            listPanel.add(Box.createVerticalStrut(24));
            listPanel.add(emptyLabel);
        } else {
            for (String name : profiles) {
                GameState gs = SaveManager.loadProfile(name);
                ProfileRowPanel row = new ProfileRowPanel(name, gs);
                rows.add(row);
                listPanel.add(row);
                listPanel.add(Box.createVerticalStrut(4));
            }
        }

        refreshRowThemes();
        listPanel.revalidate();
        listPanel.repaint();
    }

    private void refreshRowThemes() {
        Theme.Palette.Base b = ThemeManager.get().palette().base;
        Theme.Palette.Fonts f = ThemeManager.get().palette().fonts;
        for (ProfileRowPanel r : rows) {
            r.refreshRowTheme(b, f);
        }
    }

    // HE-20 fix: arrow-key navigation through the profile rows.
    // Up/Down move the highlighted profile (with wrap-around), Home/End jump to the ends,
    // and Enter loads the selected profile, equivalent to clicking Play.
    // Enter is suppressed while the delete-confirm overlay is visible
    // so the two key handlers don't both fire on the same keystroke.

    private void setupProfileKeyboardNavigation() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        bindProfileKey(im, am, "DOWN",  "profile-down",  () -> moveProfileSelection(1));
        bindProfileKey(im, am, "UP",    "profile-up",    () -> moveProfileSelection(-1));
        bindProfileKey(im, am, "HOME",  "profile-home",  () -> selectProfileIndex(0));
        bindProfileKey(im, am, "END",   "profile-end",   () -> selectProfileIndex(rows.size() - 1));
        bindProfileKey(im, am, "ENTER", "profile-play",  this::playSelectedProfile);
    }

    private void playSelectedProfile() {
        // Defer to the confirm overlay's Enter handler when it's up.
        if (confirmOverlay != null && confirmOverlay.isVisible()) return;
        if (playBtn != null && playBtn.isEnabled()) playBtn.doClick();
    }

    private void bindProfileKey(InputMap im, ActionMap am, String keyStroke, String name, Runnable run) {
        im.put(KeyStroke.getKeyStroke(keyStroke), name);
        am.put(name, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!isShowing()) return;
                run.run();
            }
        });
    }

    private void moveProfileSelection(int delta) {
        if (rows.isEmpty()) return;
        int current = -1;
        if (selectedProfile != null) {
            for (int i = 0; i < rows.size(); i++) {
                if (selectedProfile.equals(rows.get(i).profileName)) {
                    current = i;
                    break;
                }
            }
        }
        int n = rows.size();
        int next;
        if (current < 0) {
            next = (delta >= 0) ? 0 : n - 1;
        } else {
            // Wrap around so Down on the last row jumps to the first, and vice versa
            next = ((current + delta) % n + n) % n;
        }
        selectProfileIndex(next);
    }

    private void selectProfileIndex(int index) {
        if (index < 0 || index >= rows.size()) return;
        ProfileRowPanel row = rows.get(index);
        selectProfile(row.profileName);
        SwingUtilities.invokeLater(() -> row.scrollRectToVisible(row.getBounds()));
    }

    @Override
    public void refreshTheme() {
        Theme theme = ThemeManager.get().getCurrent();
        Theme.Palette.Base b = ThemeManager.get().palette().base;
        Theme.Palette.Fonts f = ThemeManager.get().palette().fonts;

        String path = BackgroundCatalog.backgroundFor("/Rome/Artifacts/", theme);
        try (var in = getClass().getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Missing resource: " + path);
            backgroundImage = ImageIO.read(in);
        } catch (Exception e) {
            e.printStackTrace();
            backgroundImage = null;
        }

        Color cardBg = new Color(b.controlBg.getRed(), b.controlBg.getGreen(), b.controlBg.getBlue(), 200);
        card.setBackground(cardBg);

        title.setFont(f.heading);
        title.setForeground(b.text);

        if (backBtn != null) backBtn.setForeground(b.text);
        if (playBtn != null) playBtn.setForeground(b.text);

        refreshRowThemes();
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

    private void showDeleteConfirm(String profileName) {
        JRootPane root = SwingUtilities.getRootPane(this);
        if (root == null) return;

        if (confirmOverlay == null) confirmOverlay = new ThemedConfirmOverlay();

        confirmOverlay.show(
                "Delete Profile",
                "Delete profile '" + profileName + "'? This cannot be undone.",
                new ThemedConfirmOverlay.ConfirmListener() {
                    @Override public void onConfirmed() {
                        dismissConfirm();
                        SaveManager.deleteProfile(profileName);
                        refreshProfiles();
                    }
                    @Override public void onCancelled() {
                        dismissConfirm();
                    }
                }
        );

        root.getLayeredPane().add(confirmOverlay, JLayeredPane.MODAL_LAYER);
        confirmOverlay.setBounds(0, 0, root.getWidth(), root.getHeight());
        confirmOverlay.revalidate();
        confirmOverlay.repaint();
    }

    private void dismissConfirm() {
        if (confirmOverlay != null && confirmOverlay.getParent() != null) {
            Container parent = confirmOverlay.getParent();
            parent.remove(confirmOverlay);
            parent.repaint();
        }
    }

    // Profile Row
    private class ProfileRowPanel extends JPanel {
        final String profileName;
        private final JLabel avatarLabel;
        private final JLabel nameLabel;
        private final JLabel levelLabel;
        private final JProgressBar xpBar;
        private final JButton deleteBtn;
        private boolean selected = false;

        ProfileRowPanel(String profileName, GameState gs) {
            this.profileName = profileName;

            setLayout(new BorderLayout(8, 0));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
            setPreferredSize(new Dimension(340, 64));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(6, 8, 6, 6));
            setOpaque(true);

            // Left: avatar icon
            avatarLabel = new JLabel();
            avatarLabel.setPreferredSize(new Dimension(44, 44));
            avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
            avatarLabel.setVerticalAlignment(SwingConstants.CENTER);
            if (gs != null) {
                BufferedImage avatar = AvatarChooserPanel.loadCachedAvatar(gs.avatarImagePath);
                if (avatar != null) {
                    avatarLabel.setIcon(new ImageIcon(avatar.getScaledInstance(40, 40, Image.SCALE_SMOOTH)));
                }
            }
            add(avatarLabel, BorderLayout.WEST);

            // Center: name + level + xp bar
            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setOpaque(false);

            // Top line: name and level
            JPanel topLine = new JPanel(new BorderLayout(6, 0));
            topLine.setOpaque(false);
            topLine.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            topLine.setAlignmentX(Component.LEFT_ALIGNMENT);

            nameLabel = new JLabel(profileName);
            topLine.add(nameLabel, BorderLayout.CENTER);

            int level = gs != null ? Math.max(1, gs.level) : 1;
            levelLabel = new JLabel("Lv. " + level);
            topLine.add(levelLabel, BorderLayout.EAST);

            center.add(topLine);
            center.add(Box.createVerticalStrut(3));

            // XP progress bar
            int xpForNext = AchievementManager.xpForLevel(level);
            int xpInLevel = gs != null ? gs.xp - AchievementManager.totalXpForLevel(level) : 0;
            if (xpInLevel < 0) xpInLevel = 0;

            xpBar = new JProgressBar(0, Math.max(1, xpForNext));
            // Force cross-platform UI so track/fill colours apply on macOS
            xpBar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI());
            xpBar.setValue(Math.min(xpInLevel, xpForNext));
            xpBar.setStringPainted(true);
            xpBar.setString(xpInLevel + " / " + xpForNext + " XP");
            xpBar.setPreferredSize(new Dimension(200, 14));
            xpBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
            xpBar.setAlignmentX(Component.LEFT_ALIGNMENT);
            xpBar.setFont(new Font("SansSerif", Font.PLAIN, 10));
            xpBar.setBorderPainted(false);
            center.add(xpBar);

            add(center, BorderLayout.CENTER);

            // Delete button (wax seal)
            deleteBtn = new JButton();
            deleteBtn.setPreferredSize(new Dimension(28, 28));
            deleteBtn.setOpaque(false);
            deleteBtn.setContentAreaFilled(false);
            deleteBtn.setBorderPainted(false);
            deleteBtn.setFocusPainted(false);
            deleteBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            deleteBtn.setToolTipText("Delete profile");
            deleteBtn.setRolloverEnabled(true);
            deleteBtn.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { deleteBtn.repaint(); }
                @Override public void mouseExited(MouseEvent e)  { deleteBtn.repaint(); }
            });
            deleteBtn.addActionListener(e -> showDeleteConfirm(profileName));
            add(deleteBtn, BorderLayout.EAST);

            // Single click toggles selection (reclick deselects), mirroring the
            // jigsaw-selection row behaviour in ProgressPanel. A Play button at
            // the bottom of the panel commits the load, instant-load on click
            // was too eager (cf. Participant feedback noted in Update 26 for the
            // collections row).
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getSource() == deleteBtn) return;
                    if (profileName.equals(selectedProfile)) {
                        selectProfile(null);
                    } else {
                        selectProfile(profileName);
                    }
                    // Double-click keeps the fast-path for confident users.
                    if (e.getClickCount() >= 2 && listener != null) {
                        listener.onProfileSelected(profileName);
                    }
                }
            });
        }

        void setSelected(boolean sel) {
            this.selected = sel;
            refreshRowTheme(ThemeManager.get().palette().base,
                            ThemeManager.get().palette().fonts);
            repaint();
        }

        void refreshRowTheme(Theme.Palette.Base b, Theme.Palette.Fonts f) {
            setBackground(selected
                    ? new Color(b.text.getRed(), b.text.getGreen(), b.text.getBlue(), 40)
                    : new Color(b.controlBg.getRed(), b.controlBg.getGreen(), b.controlBg.getBlue(), 120));

            setBorder(selected
                    ? BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(255, 215, 0), 2, true),
                        new EmptyBorder(4, 6, 4, 4))
                    : new EmptyBorder(6, 8, 6, 6));

            nameLabel.setFont(f.bodyBold);
            nameLabel.setForeground(b.text);

            levelLabel.setFont(f.caption);
            levelLabel.setForeground(b.mutedText);

            xpBar.setForeground(new Color(180, 150, 80));
            xpBar.setBackground(new Color(b.controlBg.getRed(), b.controlBg.getGreen(), b.controlBg.getBlue(), 80));
            xpBar.setFont(f.caption.deriveFont(10f));
            xpBar.setForeground(new Color(180, 150, 80));

            deleteBtn.repaint();
        }

        @Override
        protected void paintChildren(Graphics g) {
            super.paintChildren(g);

            // Draw wax seal on the delete button position
            if (deleteBtn != null) {
                Rectangle r = deleteBtn.getBounds();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int seal = 22;
                int x = r.x + (r.width - seal) / 2;
                int y = r.y + (r.height - seal) / 2;
                paintWaxSeal(g2, x, y, seal, deleteBtn.getModel().isRollover());
                g2.dispose();
            }
        }
    }

    // Paints a wax-seal style circle with a stamped * impression.
    static void paintWaxSeal(Graphics2D g2, int x, int y, int size, boolean hover) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        int r = size / 2;

        // Outer shadow
        g2.setColor(new Color(60, 10, 10, 80));
        g2.fillOval(x + 1, y + 2, size, size);

        // Main body, radial gradient from bright centre to dark rim
        Color centre = hover ? new Color(210, 60, 50) : new Color(170, 40, 35);
        Color rim     = hover ? new Color(130, 25, 20) : new Color(100, 20, 15);
        g2.setPaint(new RadialGradientPaint(cx, cy, r,
                new float[]{0f, 0.65f, 1f},
                new Color[]{centre, centre, rim}));
        g2.fillOval(x, y, size, size);

        // Raised rim highlight (top-left arc)
        g2.setColor(new Color(255, 255, 255, hover ? 60 : 40));
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawArc(x + 1, y + 1, size - 2, size - 2, 45, 135);

        // Shadow on bottom-right arc
        g2.setColor(new Color(0, 0, 0, 50));
        g2.drawArc(x + 1, y + 1, size - 2, size - 2, 225, 135);

        // Stamped * impression
        g2.setColor(new Color(80, 15, 10, 180));
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int inset = size / 4;
        g2.drawLine(x + inset, y + inset, x + size - inset, y + size - inset);
        g2.drawLine(x + size - inset, y + inset, x + inset, y + size - inset);

        // Light impression edge (offset by 1px for depth)
        g2.setColor(new Color(255, 200, 180, 50));
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x + inset + 1, y + inset + 1, x + size - inset + 1, y + size - inset + 1);
        g2.drawLine(x + size - inset + 1, y + inset + 1, x + inset + 1, y + size - inset + 1);
    }
}
