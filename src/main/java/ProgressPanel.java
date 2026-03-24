import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.imageio.ImageIO;

public class ProgressPanel extends JPanel implements ThemeAware {

    public interface ProgressListener {
        void onBackToCollections();
        void onPlaySelectedCollection(String collectionPath);
    }

    private final DefaultTableModel model;
    private final JTable table;
    private ProgressListener listener;

    private String currentCollectionFilter = "/Rome/Artifacts/";
    private BufferedImage backgroundImage; // collection + theme background

    // “Card” UI bits (your actual content)
    private final JPanel card = new JPanel(new BorderLayout(12, 12));
    private final JLabel title = new JLabel("Progress");
    private final JScrollPane scroll;

    // Bottom buttons (store as fields so refreshTheme can style them)
    private final JButton backBtn;
    private final JButton playBtn;

    // Ornate frame wrapper around the card
    private final OrnateFramePanel framedCard;

    // Kenney ornate frame (single file; OrnateFramePanel tints in DARK mode)
    private static final String ORNATE_FRAME =
            "/kenney_fantasy-ui-borders/PNG/Default/Transparent center/panel-transparent-center-030.png";

    public ProgressPanel() {
        // Use GridBagLayout so the framed card can be centred
        setLayout(new GridBagLayout());
        setOpaque(false); // we paint our own background image

        // Center title
        title.setHorizontalAlignment(SwingConstants.CENTER);

        // ---------- Table ----------
        model = new DefaultTableModel(
                new Object[]{"Jigsaw", "Completed", "Best Difficulty", "Best Time (s)"},
                0
        ) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setOpaque(false); // plays nicer with translucent backgrounds

        scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);

        // ---------- Card (content that goes INSIDE the ornate frame) ----------
        card.setOpaque(true); // we’ll set a semi-transparent background in refreshTheme()
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        card.add(title, BorderLayout.NORTH);

        // sensible size for the table area
        scroll.setPreferredSize(new Dimension(350, 720));
        card.add(scroll, BorderLayout.CENTER);

        // ---------- Buttons (Civ-style: transparent + hover underline + font bump, no layout resize) ----------
        backBtn = makeMenuButton("Back");
        backBtn.addActionListener(e -> {
            if (listener != null) listener.onBackToCollections();
        });

        playBtn = makeMenuButton("Play Collection");
        playBtn.addActionListener(e -> {
            if (listener != null) listener.onPlaySelectedCollection(currentCollectionFilter);
        });

        // Bottom row: center the PAIR as a group, and don't lock sizes (no clipping/jank)
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 48, 0));
        bottom.setOpaque(false);
        bottom.add(backBtn);
        bottom.add(playBtn);
        card.add(bottom, BorderLayout.SOUTH);

        // ---------- Wrap the card in an ornate 9-slice frame ----------
        framedCard = new OrnateFramePanel(
                card,
                12,         // paddingPx
                16,         // slicePx
                2,          // scale
                ORNATE_FRAME
        );

        // Centre framed card
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(24, 24, 24, 24);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        add(framedCard, gbc);

        ThemeManager.get().register(this);
        refreshTheme();
    }

    /**
     * "Menu style" button:
     * - transparent (no Swing fill/border)
     * - fixed clickable area (so hover font doesn't resize layout)
     * - hover: font bump + underline (Civ-ish)
     * - colour set by refreshTheme()
     */
    private JButton makeMenuButton(String text) {
        JButton b = new JButton(text);

        final float normalFont = 18f;
        final float hoverFont  = 20f;

        b.setFont(b.getFont().deriveFont(Font.BOLD, normalFont));

        // Transparent “Civ-style”
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFocusable(false);

        // Fixed clickable area (prevents layout shifts)
        // Make this wide enough for "Play Collection"
        Dimension btnSize = new Dimension(240, 44);
        b.setPreferredSize(btnSize);
        b.setMinimumSize(btnSize);
        b.setMaximumSize(btnSize);

        // Comfortable padding even when border is empty/underline
        b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                b.setFont(b.getFont().deriveFont(Font.BOLD, hoverFont));

                // underline in current theme text colour
                Color line = ThemeManager.get().palette().base.text;
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, line),
                        BorderFactory.createEmptyBorder(6, 12, 6, 12)
                ));

                b.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                b.setFont(b.getFont().deriveFont(Font.BOLD, normalFont));
                b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
                b.repaint();
            }
        });

        return b;
    }

    public void setProgressListener(ProgressListener listener) {
        this.listener = listener;
    }

    public void setCollection(String collectionPath) {
        this.currentCollectionFilter = (collectionPath == null || collectionPath.isBlank())
                ? "/Rome/Artifacts/"
                : collectionPath;
        refreshTheme();
    }

    public void loadFrom(GameState state) {
        model.setRowCount(0);

        if (state == null || state.progress == null) return;

        for (Map.Entry<String, GameState.ProgressEntry> e : state.progress.entrySet()) {
            String jigsawPath = e.getKey();
            GameState.ProgressEntry p = e.getValue();
            if (p == null) continue;

            String entryCollection = (p.collectionPath != null && !p.collectionPath.isBlank())
                    ? p.collectionPath
                    : guessCollectionFromJigsawPath(jigsawPath);

            if (!currentCollectionFilter.equals(entryCollection)) continue;

            model.addRow(new Object[]{
                    ArtifactCatalog.displayName(jigsawPath),
                    p.completed ? "Yes" : "No",
                    p.bestDifficulty == null ? "" : p.bestDifficulty,
                    p.bestTimeSeconds <= 0 ? "" : p.bestTimeSeconds
            });
        }

        table.revalidate();
        table.repaint();
    }

    private String guessCollectionFromJigsawPath(String jigsawPath) {
        if (jigsawPath == null) return "";
        int idx = jigsawPath.indexOf("/Artifacts/");
        if (idx < 0) return "";
        return jigsawPath.substring(0, idx + "/Artifacts/".length());
    }

    @Override
    public void refreshTheme() {
        Theme theme = ThemeManager.get().getCurrent();
        Theme.Palette.Base b = ThemeManager.get().palette().base;

        // 1) Background image (collection + theme)
        String bgPath = BackgroundCatalog.backgroundFor(currentCollectionFilter, theme);
        try (var in = getClass().getResourceAsStream(bgPath)) {
            if (in == null) throw new RuntimeException("Missing resource: " + bgPath);
            backgroundImage = ImageIO.read(in);
        } catch (Exception ex) {
            ex.printStackTrace();
            backgroundImage = null;
        }

        // 2) Card styling (semi-transparent overlay inside the ornate frame)
        Color cardBg = new Color(b.controlBg.getRed(), b.controlBg.getGreen(), b.controlBg.getBlue(), 200);
        card.setBackground(cardBg);

        // 3) Title / table colours
        title.setForeground(b.text);

        boolean dark = ThemeManager.get().getCurrent() == Theme.DARK;

        // More contrast than before so the table doesn't “vanish”
        table.setBackground(dark
                ? new Color(15, 15, 15, 160)
                : new Color(255, 255, 255, 160));
        table.setForeground(dark ? Color.WHITE : Color.BLACK);
        table.setGridColor(b.controlBorder);

        table.getTableHeader().setBackground(dark
                ? new Color(25, 25, 25, 220)
                : new Color(245, 245, 245, 220));
        table.getTableHeader().setForeground(dark ? Color.WHITE : Color.BLACK);

        // 4) Buttons match theme
        if (backBtn != null) backBtn.setForeground(b.text);
        if (playBtn != null) playBtn.setForeground(b.text);

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
}