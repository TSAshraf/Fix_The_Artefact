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
    private JButton favouritesButton;
    private JButton backButton;

    private JLabel profileNameLabel;
    private JLabel profileStatsLabel;

    private BufferedImage backgroundImage;
    private CollectionsListener listener;

    private String currentCollection = "/Rome/Artifacts/";

    private static final String ORNATE_FRAME =
            "/kenney_fantasy-ui-borders/PNG/Default/Transparent center/panel-transparent-center-030.png";

    public interface CollectionsListener {
        void onCollectionSelected(String collectionPath);
        void onFavouritesClicked();
        void onBackToMenu();
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

        // ---------- Buttons (content) ----------
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

        romeButton = makeMenuButton("Rome", fixedSize, normalFont, hoverFont);
        romeButton.addActionListener(e -> {
            if (listener != null) listener.onCollectionSelected("/Rome/Artifacts/");
        });
        buttonPanel.add(romeButton);

        favouritesButton = makeMenuButton("Favourites", fixedSize, normalFont, hoverFont);
        favouritesButton.addActionListener(e -> {
            if (listener != null) listener.onFavouritesClicked();
        });
        buttonPanel.add(favouritesButton);

        backButton = makeMenuButton("Back to Main Menu", fixedSize, normalFont, hoverFont);
        backButton.addActionListener(e -> {
            if (listener != null) listener.onBackToMenu();
        });
        buttonPanel.add(backButton);

        // ---------- Wrap buttons in ornate frame ----------
        OrnateFramePanel framedButtons = new OrnateFramePanel(
                buttonPanel, 8, 10, 1, ORNATE_FRAME
        );

        // ---------- Profile info panel ----------
        JPanel profilePanel = new JPanel();
        profilePanel.setOpaque(false);
        profilePanel.setLayout(new BoxLayout(profilePanel, BoxLayout.Y_AXIS));

        profileNameLabel = new JLabel("");
        profileNameLabel.setFont(new Font("Serif", Font.BOLD, 22));
        profileNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        profileStatsLabel = new JLabel("");
        profileStatsLabel.setFont(new Font("Serif", Font.PLAIN, 14));
        profileStatsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        profilePanel.add(Box.createVerticalStrut(8));
        profilePanel.add(profileNameLabel);
        profilePanel.add(Box.createVerticalStrut(6));
        profilePanel.add(profileStatsLabel);
        profilePanel.add(Box.createVerticalStrut(8));

        OrnateFramePanel framedProfile = new OrnateFramePanel(
                profilePanel, 8, 10, 1, ORNATE_FRAME
        );

        // ---------- Layout: profile on left, buttons on right ----------
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

    /** Reload profile info labels from the active save state. */
    public void refreshProfile() {
        GameState s = SaveManager.loadOrDefault();
        String name = (s.profileName != null && !s.profileName.isBlank())
                ? s.profileName : "Guest";
        profileNameLabel.setText(name);

        int completed = 0;
        int total = 0;
        for (GameState.ProgressEntry pe : s.progress.values()) {
            total++;
            if (pe.completed) completed++;
        }
        int favCount = (s.favourites != null) ? s.favourites.size() : 0;
        profileStatsLabel.setText("<html><center>"
                + completed + " completed<br>"
                + favCount + " favourites"
                + "</center></html>");

        Color text = ThemeManager.get().palette().base.text;
        profileNameLabel.setForeground(text);
        profileStatsLabel.setForeground(text);
    }

    private JButton makeMenuButton(
            String text,
            Dimension fixedSize,
            float normalFont,
            float hoverFont
    ) {
        JButton b = new JButton(text);
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

    public void setCurrentCollection(String collectionPath) {
        this.currentCollection = collectionPath;
        refreshTheme();
    }

    @Override
    public void refreshTheme() {
        Theme theme = ThemeManager.get().getCurrent();
        String path = BackgroundCatalog.backgroundFor(currentCollection, theme);

        try (var in = getClass().getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Missing resource: " + path);
            backgroundImage = ImageIO.read(in);
        } catch (Exception e) {
            e.printStackTrace();
            backgroundImage = null;
        }

        Color text = ThemeManager.get().palette().base.text;
        if (ancientCyprusButton != null) ancientCyprusButton.setForeground(text);
        if (ancientGreeceButton != null) ancientGreeceButton.setForeground(text);
        if (ancientEgyptButton != null) ancientEgyptButton.setForeground(text);
        if (ancientNearEastButton != null) ancientNearEastButton.setForeground(text);
        if (romeButton != null) romeButton.setForeground(text);
        if (favouritesButton != null) favouritesButton.setForeground(text);
        if (backButton != null) backButton.setForeground(text);
        if (profileNameLabel != null) profileNameLabel.setForeground(text);
        if (profileStatsLabel != null) profileStatsLabel.setForeground(text);

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
