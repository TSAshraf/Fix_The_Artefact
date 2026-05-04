import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public class FavouritesPanel extends JPanel implements ThemeAware {

    private JPanel listPanel;
    private JScrollPane scrollPane;
    private BufferedImage backgroundImage;
    private FavouritesListener listener;

    private final JPanel card = new JPanel(new BorderLayout(12, 12));
    private final JLabel title = new JLabel("Favourites");
    private final JButton backBtn;
    private final OrnateFramePanel framedCard;

    private final java.util.List<FavRowPanel> rows = new ArrayList<>();
    private String selectedFavouritePath = null;

    private static final String ORNATE_FRAME =
            "/kenney_fantasy-ui-borders/PNG/Default/Transparent center/panel-transparent-center-030.png";

    public interface FavouritesListener {
        void onFavouriteSelected(String jigsawPath);
        void onBackToCollections();
    }

    public void setFavouritesListener(FavouritesListener listener) {
        this.listener = listener;
    }

    public FavouritesPanel() {
        setLayout(new GridBagLayout());
        setOpaque(false);

        title.setHorizontalAlignment(SwingConstants.CENTER);

        listPanel = new JPanel();
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

        scrollPane.setPreferredSize(new Dimension(420, 500));
        card.add(scrollPane, BorderLayout.CENTER);

        // Back button
        backBtn = makeMenuButton("\u2190 Back");
        backBtn.addActionListener(e -> {
            if (listener != null) listener.onBackToCollections();
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 48, 0));
        bottom.setOpaque(false);
        bottom.add(backBtn);
        card.add(bottom, BorderLayout.SOUTH);

        framedCard = new OrnateFramePanel(card, 12, 16, 2, ORNATE_FRAME);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(24, 24, 24, 24);
        add(framedCard, gbc);

        setupFavouritesKeyboardNavigation();

        ThemeManager.get().register(this);
        refreshTheme();
    }

    // HE-20 fix: arrow-key navigation through the favourites rows.
    // Up/Down move the highlighted row (with wrap-around), Home/End jump to the ends,
    // and Enter plays the currently selected row, mirroring the timeline panel.

    private void setupFavouritesKeyboardNavigation() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        bindFavKey(im, am, "DOWN",  "fav-down",  () -> moveFavouriteSelection(1));
        bindFavKey(im, am, "UP",    "fav-up",    () -> moveFavouriteSelection(-1));
        bindFavKey(im, am, "HOME",  "fav-home",  () -> selectFavouriteIndex(0));
        bindFavKey(im, am, "END",   "fav-end",   () -> selectFavouriteIndex(rows.size() - 1));
        bindFavKey(im, am, "ENTER", "fav-play",  this::playSelectedFavourite);
    }

    private void playSelectedFavourite() {
        if (selectedFavouritePath != null && listener != null) {
            listener.onFavouriteSelected(selectedFavouritePath);
        }
    }

    private void bindFavKey(InputMap im, ActionMap am, String keyStroke, String name, Runnable run) {
        im.put(KeyStroke.getKeyStroke(keyStroke), name);
        am.put(name, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!isShowing()) return;
                run.run();
            }
        });
    }

    private void moveFavouriteSelection(int delta) {
        if (rows.isEmpty()) return;
        int current = -1;
        if (selectedFavouritePath != null) {
            for (int i = 0; i < rows.size(); i++) {
                if (selectedFavouritePath.equals(rows.get(i).imagePath)) {
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
            next = ((current + delta) % n + n) % n;
        }
        selectFavouriteIndex(next);
    }

    private void selectFavouriteIndex(int index) {
        if (index < 0 || index >= rows.size()) return;
        FavRowPanel row = rows.get(index);
        selectedFavouritePath = row.imagePath;
        for (FavRowPanel r : rows) r.setSelected(r.imagePath.equals(selectedFavouritePath));
        SwingUtilities.invokeLater(() -> row.scrollRectToVisible(row.getBounds()));
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

    // Rebuild the favourites list from the active profile's save state.
    public void refreshFavourites() {
        listPanel.removeAll();
        rows.clear();

        GameState s = SaveManager.loadOrDefault();
        List<String> favs = s.favourites;

        if (favs == null || favs.isEmpty()) {
            JLabel empty = new JLabel("No favourites yet");
            empty.setFont(new Font("Serif", Font.ITALIC, 16));
            empty.setForeground(ThemeManager.get().palette().base.mutedText);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            empty.setHorizontalAlignment(SwingConstants.CENTER);
            listPanel.add(Box.createVerticalStrut(24));
            listPanel.add(empty);
        } else {
            for (String path : favs) {
                // Look up timeline data for this artefact
                String collection = guessCollection(path);
                TimelineData.Entry te = TimelineData.forImage(collection, path);

                String displayName = (te != null) ? te.displayName : ArtifactCatalog.displayName(path);
                String dateLabel = (te != null) ? te.dateLabel : "";
                String narrative = (te != null) ? te.microNarrative : "";

                FavRowPanel row = new FavRowPanel(path, displayName, dateLabel, narrative);
                rows.add(row);
                listPanel.add(row);
                listPanel.add(Box.createVerticalStrut(4));
            }
        }

        refreshRowThemes();
        listPanel.revalidate();
        listPanel.repaint();
    }

    private String guessCollection(String jigsawPath) {
        if (jigsawPath == null) return "";
        int idx = jigsawPath.indexOf("/Artifacts/");
        if (idx < 0) return "";
        return jigsawPath.substring(0, idx + "/Artifacts/".length());
    }

    private void refreshRowThemes() {
        Theme.Palette.Base b = ThemeManager.get().palette().base;
        Theme.Palette.Fonts f = ThemeManager.get().palette().fonts;
        for (FavRowPanel r : rows) {
            r.refreshRowTheme(b, f);
        }
    }

    @Override
    public void refreshTheme() {
        Theme theme = ThemeManager.get().getCurrent();
        Theme.Palette.Base b = ThemeManager.get().palette().base;
        Theme.Palette.Fonts f = ThemeManager.get().palette().fonts;

        String bgPath = BackgroundCatalog.backgroundFor("/Rome/Artifacts/", theme);
        try (var in = getClass().getResourceAsStream(bgPath)) {
            if (in == null) throw new RuntimeException("Missing resource: " + bgPath);
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

    // Favourite Row
    private class FavRowPanel extends JPanel {
        final String imagePath;
        private final JLabel thumbLabel;
        private final JLabel nameLabel;
        private final JLabel dateText;
        private final JLabel narrativeLabel;
        private final JButton unfavBtn;
        private boolean selected = false;

        FavRowPanel(String imagePath, String displayName, String dateLabel, String narrative) {
            this.imagePath = imagePath;

            setLayout(new BorderLayout(8, 0));
            boolean hasNarrative = narrative != null && !narrative.isEmpty();
            int rowH = hasNarrative ? 88 : 70;
            setMaximumSize(new Dimension(Integer.MAX_VALUE, rowH));
            setPreferredSize(new Dimension(380, rowH));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(4, 6, 4, 6));
            setOpaque(true);

            // Thumbnail
            thumbLabel = new JLabel();
            thumbLabel.setPreferredSize(new Dimension(56, 56));
            thumbLabel.setHorizontalAlignment(SwingConstants.CENTER);
            add(thumbLabel, BorderLayout.WEST);

            // Center: name + date + narrative
            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setOpaque(false);
            center.setBorder(new EmptyBorder(4, 4, 4, 4));

            nameLabel = new JLabel(displayName);
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            center.add(nameLabel);

            dateText = new JLabel(dateLabel);
            dateText.setAlignmentX(Component.LEFT_ALIGNMENT);
            center.add(dateText);

            narrativeLabel = new JLabel(hasNarrative ? narrative : " ");
            narrativeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (hasNarrative) center.add(narrativeLabel);

            add(center, BorderLayout.CENTER);

            // Unfavourite button (wax seal)
            unfavBtn = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int seal = 22;
                    int x = (getWidth() - seal) / 2;
                    int y = (getHeight() - seal) / 2;
                    LoadGamePanel.paintWaxSeal(g2, x, y, seal, getModel().isRollover());
                    g2.dispose();
                }
            };
            unfavBtn.setOpaque(false);
            unfavBtn.setContentAreaFilled(false);
            unfavBtn.setBorderPainted(false);
            unfavBtn.setFocusPainted(false);
            unfavBtn.setRolloverEnabled(true);
            unfavBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            unfavBtn.setToolTipText("Remove from favourites");
            unfavBtn.setPreferredSize(new Dimension(28, 28));
            unfavBtn.addActionListener(e -> {
                GameState gs = SaveManager.loadOrDefault();
                gs.toggleFavourite(imagePath);
                SaveManager.save(gs);
                refreshFavourites();
            });
            add(unfavBtn, BorderLayout.EAST);

            // Single click toggles selection (re-click deselects); double-click plays.
            // Mirrors the timeline panel so the keyboard / mouse interaction model
            // is consistent across the two list-based screens.
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (imagePath.equals(selectedFavouritePath)) {
                        // Already selected, reclick to deselect.
                        selectedFavouritePath = null;
                        for (FavRowPanel r : rows) r.setSelected(false);
                    } else {
                        selectedFavouritePath = imagePath;
                        for (FavRowPanel r : rows) r.setSelected(r.imagePath.equals(imagePath));
                    }
                    if (e.getClickCount() >= 2 && listener != null) {
                        listener.onFavouriteSelected(imagePath);
                    }
                }
            });

            loadThumbnail();
        }

        void refreshRowTheme(Theme.Palette.Base b, Theme.Palette.Fonts f) {
            boolean dark = ThemeManager.get().getCurrent() == Theme.DARK;

            // Match the timeline panel's selection styling exactly: gold border
            // around the row plus a subtle text-tinted background fill.
            setBackground(selected
                    ? new Color(b.text.getRed(), b.text.getGreen(), b.text.getBlue(), 40)
                    : new Color(b.controlBg.getRed(), b.controlBg.getGreen(), b.controlBg.getBlue(), 120));

            setBorder(selected
                    ? BorderFactory.createCompoundBorder(
                            new LineBorder(new Color(255, 215, 0), 2, true),
                            new EmptyBorder(2, 4, 2, 4))
                    : new EmptyBorder(4, 6, 4, 6));

            nameLabel.setFont(f.bodyBold);
            nameLabel.setForeground(b.text);

            dateText.setFont(new Font(f.caption.getFamily(), Font.ITALIC, f.caption.getSize()));
            dateText.setForeground(b.mutedText);

            narrativeLabel.setFont(new Font(f.caption.getFamily(), Font.ITALIC, f.caption.getSize() - 1));
            narrativeLabel.setForeground(dark
                    ? new Color(200, 190, 160)
                    : new Color(100, 80, 50));
        }

        void setSelected(boolean sel) {
            if (this.selected == sel) return;
            this.selected = sel;
            refreshRowTheme(ThemeManager.get().palette().base,
                            ThemeManager.get().palette().fonts);
            repaint();
        }

        private void loadThumbnail() {
            new SwingWorker<ImageIcon, Void>() {
                @Override
                protected ImageIcon doInBackground() {
                    try (var in = FavouritesPanel.class.getResourceAsStream(imagePath)) {
                        if (in == null) return null;
                        BufferedImage full = ImageIO.read(in);
                        if (full == null) return null;
                        int tw = 52, th = 52;
                        double scale = Math.min((double) tw / full.getWidth(), (double) th / full.getHeight());
                        int sw = Math.max(1, (int) (full.getWidth() * scale));
                        int sh = Math.max(1, (int) (full.getHeight() * scale));
                        BufferedImage scaled = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2 = scaled.createGraphics();
                        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2.drawImage(full, 0, 0, sw, sh, null);
                        g2.dispose();
                        return new ImageIcon(scaled);
                    } catch (Exception ex) { return null; }
                }
                @Override
                protected void done() {
                    try {
                        ImageIcon ico = get();
                        if (ico != null) thumbLabel.setIcon(ico);
                    } catch (Exception ignore) {}
                }
            }.execute();
        }
    }
}
