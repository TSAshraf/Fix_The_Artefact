import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.imageio.ImageIO;

public class ProgressPanel extends JPanel implements ThemeAware {

    public interface ProgressListener {
        void onBackToCollections();
        void onPlaySelectedCollection(String collectionPath);
        void onPlaySpecificJigsaw(String collectionPath, String jigsawPath);
    }

    private ProgressListener listener;
    private String currentCollectionFilter = "/Rome/Artifacts/";
    private String currentSiteName = null; // HE-06: site label when arriving from a map
    private BufferedImage backgroundImage;

    // Card UI
    private final JPanel card = new JPanel(new BorderLayout(12, 12));
    private final JLabel title = new JLabel("Timeline");

    // Timeline entries container (vertical list)
    private final JPanel timelineList = new JPanel();
    private final JScrollPane scroll;

    // Bottom buttons
    private final JButton backBtn;
    private final JButton playBtn;

    // Ornate frame
    private final OrnateFramePanel framedCard;
    private static final String ORNATE_FRAME =
            "/kenney_fantasy-ui-borders/PNG/Default/Transparent center/panel-transparent-center-030.png";

    // Selection state
    private String selectedJigsawPath = null;
    private final java.util.List<TimelineRowPanel> rows = new ArrayList<>();

    // Tutorial pointer target, returns the first jigsaw row so the pointer
    // has something to spotlight on the timeline. Null if the list is empty.
    public JComponent getTutorialTargetRow() {
        return rows.isEmpty() ? null : rows.get(0);
    }

    public ProgressPanel() {
        setLayout(new GridBagLayout());
        setOpaque(false);

        title.setHorizontalAlignment(SwingConstants.CENTER);

        // Timeline list setup
        timelineList.setLayout(new BoxLayout(timelineList, BoxLayout.Y_AXIS));
        timelineList.setOpaque(false);

        scroll = new JScrollPane(timelineList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        FantasyScrollBarUI.install(scroll);

        // Card
        card.setOpaque(true);
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        card.add(title, BorderLayout.NORTH);

        // Preferred size is a target, not a minimum, the BorderLayout.CENTER
        // region will shrink the scroll when the card shrinks (small windows).
        // Kept comfortably under the 650px minimum window height so content
        // doesn't get clipped at small sizes.
        scroll.setPreferredSize(new Dimension(420, 360));
        scroll.setMinimumSize(new Dimension(380, 180));
        card.add(scroll, BorderLayout.CENTER);

        // Buttons
        backBtn = makeMenuButton("\u2190 Back");
        backBtn.addActionListener(e -> {
            if (listener != null) listener.onBackToCollections();
        });

        playBtn = makeMenuButton("Play");
        playBtn.addActionListener(e -> {
            if (selectedJigsawPath != null && listener != null) {
                listener.onPlaySpecificJigsaw(currentCollectionFilter, selectedJigsawPath);
            } else if (listener != null) {
                listener.onPlaySelectedCollection(currentCollectionFilter);
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 48, 0));
        bottom.setOpaque(false);
        bottom.add(backBtn);
        bottom.add(playBtn);
        card.add(bottom, BorderLayout.SOUTH);

        // Frame
        framedCard = new OrnateFramePanel(card, 12, 16, 2, ORNATE_FRAME);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(24, 24, 24, 24);
        // Fill both directions so the card (and its timeline scroll) uses available space.
        // Previously NONE forced the card to its preferred size, on small windows that pushed the buttons out of view.
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(framedCard, gbc);

        setupTimelineKeyboardNavigation();

        ThemeManager.get().register(this);
        refreshTheme();
    }

    private JButton makeMenuButton(String text) {
        // Same halo + font-grow-on-hover as CollectionsPanel's menu buttons so
        // all menu-style buttons behave identically across the app.
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

    // Soft pill behind the button text so it reads cleanly on any background.
    // Mirrors CollectionsPanel.drawTextHalo, duplicated to keep the panels
    // independent of each other's internals.
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
                ? new Color(0, 0, 0, 150)
                : new Color(255, 255, 255, 150);

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

    public void setProgressListener(ProgressListener listener) {
        this.listener = listener;
    }

    public void setCollection(String collectionPath) {
        this.currentCollectionFilter = (collectionPath == null || collectionPath.isBlank())
                ? "/Rome/Artifacts/" : collectionPath;

        // Reset any per-site label, callers that arrive from a map will set
        // it back via setSiteContext after this call.
        this.currentSiteName = null;
        applyTitle();

        // Always populate the timeline entries immediately
        buildTimelineRows(null);
        refreshTheme();
    }

    // HE-06: when the user arrives at the timeline via a map site (rather than directly from the collections grid),
    // append the site name to the panel title so the displayed scope reflects the geographic specificity of the
    // map navigation. Pass null to clear.

    public void setSiteContext(String siteName) {
        this.currentSiteName = (siteName == null || siteName.isBlank()) ? null : siteName;
        applyTitle();
    }

    private void applyTitle() {
        String name = currentCollectionFilter
                .replace("/Artifacts/", "")
                .replace("/Artifacts", "");
        if (name.startsWith("/")) name = name.substring(1);
        // Ensure "Rome" displays as "Ancient Rome" to match other collections
        if ("Rome".equals(name)) name = "Ancient Rome";

        String base = name.isEmpty() ? "Timeline" : name;
        title.setText(currentSiteName != null
                ? base + " \\u2014 " + currentSiteName
                : base);
    }

    public void loadFrom(GameState state) {
        // Rebuild with progress data overlaid
        buildTimelineRows(state);
        refreshTheme();
    }

    private void buildTimelineRows(GameState state) {
        timelineList.removeAll();
        rows.clear();
        selectedJigsawPath = null;

        Map<String, GameState.ProgressEntry> progressMap =
                (state != null && state.progress != null) ? state.progress : Collections.emptyMap();

        // Use TimelineData for chronological ordering
        java.util.List<TimelineData.Entry> entries = TimelineData.forCollection(currentCollectionFilter);

        if (entries.isEmpty()) {
            // Fallback: list items from catalog without timeline dates
            for (String img : ArtifactCatalog.imagesFor(currentCollectionFilter)) {
                GameState.ProgressEntry pe = progressMap.get(img);
                TimelineRowPanel row = new TimelineRowPanel(
                        img, ArtifactCatalog.displayName(img), "", "", pe);
                rows.add(row);
                timelineList.add(row);
                timelineList.add(Box.createVerticalStrut(4));
            }
        } else {
            for (TimelineData.Entry te : entries) {
                GameState.ProgressEntry pe = progressMap.get(te.imagePath);
                TimelineRowPanel row = new TimelineRowPanel(
                        te.imagePath, te.displayName, te.dateLabel, te.microNarrative, pe);
                rows.add(row);
                timelineList.add(row);
                timelineList.add(Box.createVerticalStrut(4));
            }
        }

        timelineList.revalidate();
        timelineList.repaint();
    }

    // Highlight specific artefact rows (e.g. from a map site selection).
    public void highlightArtefacts(java.util.List<String> artefactPaths) {
        if (artefactPaths == null || artefactPaths.isEmpty()) return;
        Set<String> paths = new HashSet<>(artefactPaths);
        for (TimelineRowPanel r : rows) {
            r.setDimmed(!paths.contains(r.imagePath));
        }
        // Scroll to the first highlighted row
        for (TimelineRowPanel r : rows) {
            if (paths.contains(r.imagePath)) {
                SwingUtilities.invokeLater(() ->
                        r.scrollRectToVisible(r.getBounds()));
                break;
            }
        }
    }

    // Clear any dimming applied by highlightArtefacts.
    public void clearHighlights() {
        for (TimelineRowPanel r : rows) {
            r.setDimmed(false);
        }
    }

    private void selectRow(String jigsawPath) {
        selectedJigsawPath = jigsawPath;
        for (TimelineRowPanel r : rows) {
            r.setSelected(r.imagePath.equals(jigsawPath));
        }
    }

    // HE-20 fix: arrow-key navigation through the timeline rows. Up/Down move
    // the selection one row (with wrap-around), Home/End jump to the ends, and
    // Enter plays the currently selected row, equivalent to a double-click.
    // Bindings are guarded by isShowing() so they don't fire while the panel
    // sits inactive in the CardLayout.

    private void setupTimelineKeyboardNavigation() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        bindTimelineKey(im, am, "DOWN",  "timeline-down",  () -> moveTimelineSelection(1));
        bindTimelineKey(im, am, "UP",    "timeline-up",    () -> moveTimelineSelection(-1));
        bindTimelineKey(im, am, "HOME",  "timeline-home",  () -> selectTimelineIndex(0));
        bindTimelineKey(im, am, "END",   "timeline-end",   () -> selectTimelineIndex(rows.size() - 1));
        bindTimelineKey(im, am, "ENTER", "timeline-play",  this::playSelectedRow);
    }

    private void bindTimelineKey(InputMap im, ActionMap am, String keyStroke, String name, Runnable run) {
        im.put(KeyStroke.getKeyStroke(keyStroke), name);
        am.put(name, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (!isShowing()) return;
                run.run();
            }
        });
    }

    private void moveTimelineSelection(int delta) {
        if (rows.isEmpty()) return;
        int current = -1;
        if (selectedJigsawPath != null) {
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).imagePath.equals(selectedJigsawPath)) {
                    current = i;
                    break;
                }
            }
        }
        int n = rows.size();
        int next;
        if (current < 0) {
            // Nothing selected yet, first arrow press lands on the first / last row
            next = (delta >= 0) ? 0 : n - 1;
        } else {
            // Wrap around so Down on the last row jumps to the first, and vice versa
            next = ((current + delta) % n + n) % n;
        }
        selectTimelineIndex(next);
    }

    private void selectTimelineIndex(int index) {
        if (index < 0 || index >= rows.size()) return;
        TimelineRowPanel row = rows.get(index);
        selectRow(row.imagePath);
        SwingUtilities.invokeLater(() -> row.scrollRectToVisible(row.getBounds()));
    }

    private void playSelectedRow() {
        if (selectedJigsawPath != null && listener != null) {
            listener.onPlaySpecificJigsaw(currentCollectionFilter, selectedJigsawPath);
        }
    }

    // Theme

    @Override
    public void refreshTheme() {
        Theme theme = ThemeManager.get().getCurrent();
        Theme.Palette.Base b = ThemeManager.get().palette().base;
        Theme.Palette.Fonts f = ThemeManager.get().palette().fonts;

        String bgPath = BackgroundCatalog.backgroundFor(currentCollectionFilter, theme);
        try (var in = getClass().getResourceAsStream(bgPath)) {
            if (in == null) throw new RuntimeException("Missing resource: " + bgPath);
            backgroundImage = ImageIO.read(in);
        } catch (Exception ex) {
            ex.printStackTrace();
            backgroundImage = null;
        }

        Color cardBg = new Color(b.controlBg.getRed(), b.controlBg.getGreen(), b.controlBg.getBlue(), 200);
        card.setBackground(cardBg);

        title.setFont(f.heading);
        title.setForeground(b.text);

        if (backBtn != null) backBtn.setForeground(b.text);
        if (playBtn != null) playBtn.setForeground(b.text);

        for (TimelineRowPanel r : rows) {
            r.refreshRowTheme(b, f);
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            Theme.Palette.Base b = ThemeManager.get().palette().base;
            g.setColor(b.appBg);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // Timeline Row
    private class TimelineRowPanel extends JPanel {
        final String imagePath;
        final String displayName;
        final String dateLabel;
        final String microNarrative;
        final GameState.ProgressEntry progress;
        private boolean selected = false;
        private boolean dimmed = false;

        private final JLabel thumbLabel;
        private final JLabel nameLabel;
        private final JLabel dateText;
        private final JLabel narrativeLabel;
        private final JPanel statusPanel;

        TimelineRowPanel(String imagePath, String displayName, String dateLabel,
                         String microNarrative, GameState.ProgressEntry progress) {
            this.imagePath = imagePath;
            this.displayName = displayName;
            this.dateLabel = dateLabel;
            this.microNarrative = microNarrative;
            this.progress = progress;

            setLayout(new BorderLayout(8, 0));
            boolean hasNarrative = microNarrative != null && !microNarrative.isEmpty();
            int rowH = hasNarrative ? 88 : 70;
            setMaximumSize(new Dimension(Integer.MAX_VALUE, rowH));
            setPreferredSize(new Dimension(380, rowH));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(4, 6, 4, 6));
            setOpaque(true);

            // Thumbnail (left)
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

            narrativeLabel = new JLabel(hasNarrative ? microNarrative : " ");
            narrativeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (hasNarrative) center.add(narrativeLabel);

            add(center, BorderLayout.CENTER);

            // Right: status icons
            statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 12));
            statusPanel.setOpaque(false);
            statusPanel.setPreferredSize(new Dimension(80, 56));
            buildStatusIcons();
            add(statusPanel, BorderLayout.EAST);

            // Single click toggles selection (reclick deselects); double-click plays.
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (imagePath.equals(selectedJigsawPath)) {
                        // Already selected to reclick to deselect.
                        selectedJigsawPath = null;
                        for (TimelineRowPanel r : rows) r.setSelected(false);
                    } else {
                        selectRow(imagePath);
                    }
                    if (e.getClickCount() >= 2 && listener != null) {
                        listener.onPlaySpecificJigsaw(currentCollectionFilter, imagePath);
                    }
                }
            });

            // Load thumbnail
            loadThumbnail();
        }

        private void buildStatusIcons() {
            statusPanel.removeAll();

            boolean completed = progress != null && progress.completed;

            if (completed) {
                // Checkmark
                JLabel check = new JLabel("\u2713");
                check.setFont(new Font("Serif", Font.BOLD, 18));
                check.setForeground(ThemeManager.get().palette().base.successGreen);
                statusPanel.add(check);

                // Difficulty dots
                if (progress.bestDifficulty != null) {
                    int dots = difficultyToDots(progress.bestDifficulty);
                    statusPanel.add(new DifficultyDots(dots));
                }

                // Time
                if (progress.bestTimeSeconds > 0) {
                    JLabel time = new JLabel(progress.bestTimeSeconds + "s");
                    time.setFont(new Font("Serif", Font.PLAIN, 11));
                    statusPanel.add(time);
                }
            }
        }

        private int difficultyToDots(String diff) {
            if (diff == null) return 0;
            switch (diff.toLowerCase()) {
                case "easy":   return 1;
                case "medium": return 2;
                case "hard":   return 3;
                case "expert": return 4;
                case "custom": return 5;
                default:       return 1;
            }
        }

        void setSelected(boolean sel) {
            this.selected = sel;
            // Re-apply the background/border styling immediately so the selection
            // highlight is visually reflected, repaint() alone doesn't re-run
            // refreshRowTheme(), which is where the colours are actually set.
            refreshRowTheme(ThemeManager.get().palette().base,
                            ThemeManager.get().palette().fonts);
            repaint();
        }

        void setDimmed(boolean dim) {
            this.dimmed = dim;
            setVisible(!dim);
        }

        void refreshRowTheme(Theme.Palette.Base b, Theme.Palette.Fonts f) {
            boolean dark = ThemeManager.get().getCurrent() == Theme.DARK;

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
            narrativeLabel.setForeground(b.narrativeText);

            // Status text colours
            for (Component c : statusPanel.getComponents()) {
                if (c instanceof JLabel && !(c instanceof DifficultyDots)) {
                    JLabel lbl = (JLabel) c;
                    if ("\u2713".equals(lbl.getText())) {
                        lbl.setForeground(ThemeManager.get().palette().base.successGreen);
                    } else {
                        lbl.setForeground(b.mutedText);
                    }
                }
            }
        }

        private void loadThumbnail() {
            new SwingWorker<ImageIcon, Void>() {
                @Override
                protected ImageIcon doInBackground() {
                    try (var in = ProgressPanel.class.getResourceAsStream(imagePath)) {
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

    // Difficulty Dots Component
    private static class DifficultyDots extends JLabel {
        private final int count;
        private static final Color BRONZE = new Color(205, 127, 50);
        private static final Color SILVER = new Color(192, 192, 192);
        private static final Color GOLD   = new Color(255, 215, 0);

        DifficultyDots(int count) {
            this.count = count;
            setPreferredSize(new Dimension(count * 10 + 2, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            for (int i = 0; i < count; i++) {
                Color c;
                if (count <= 1) c = BRONZE;
                else if (count <= 2) c = SILVER;
                else c = GOLD;
                g2.setColor(c);
                g2.fillOval(i * 10, 2, 8, 8);
            }
            g2.dispose();
        }
    }
}
