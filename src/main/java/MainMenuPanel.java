import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class MainMenuPanel extends JPanel implements ThemeAware {

    private FramePanel framePanel;
    private BufferedImage backgroundImage;
    private MainMenuListener listener;

    // main menu background follows last played collection if you call setCurrentCollection()
    private String currentCollection = "/Rome/Artifacts/";

    // Kenney ornate frame (same one you used elsewhere)
    private static final String ORNATE_FRAME =
            "/kenney_fantasy-ui-borders/PNG/Default/Transparent center/panel-transparent-center-030.png";

    public interface MainMenuListener {
        void onNewGameClicked();
        void onLoadClicked();
        // Resume the most recently played profile at its last jigsaw. Maybe a no-op if no profile exists.
        void onResumeClicked();
    }

    private JButton resumeButton;

    public void setMainMenuListener(MainMenuListener listener) {
        this.listener = listener;
        if (framePanel != null) framePanel.setMainMenuListener(listener);
    }

    // Re-check whether any saved profile exists and show/hide the Resume button accordingly.
    public void refreshResumeAvailability() {
        if (resumeButton == null) return;
        boolean hasProfile = SaveManager.findMostRecentProfile() != null;
        resumeButton.setVisible(hasProfile);
        revalidate();
        repaint();
    }

    public MainMenuPanel() {
        setLayout(new GridBagLayout());
        setOpaque(false);

        JButton newGameButton = createMenuButton("New Game");
        resumeButton = createMenuButton("Continue");
        JButton loadButton = createMenuButton("Load");

        // Action listeners
        newGameButton.addActionListener(e -> {
            if (listener != null) listener.onNewGameClicked();
        });
        resumeButton.addActionListener(e -> {
            if (listener != null) listener.onResumeClicked();
        });
        loadButton.addActionListener(e -> {
            if (listener != null) listener.onLoadClicked();
        });

        // Resume button is only meaningful when at least one profile exists.
        // Hidden by default; refreshResumeAvailability() flips it on if a profile is present.
        resumeButton.setVisible(false);

        // Stack buttons vertically in one content panel
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        newGameButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resumeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(resumeButton);
        content.add(Box.createVerticalStrut(4));
        content.add(newGameButton);
        content.add(Box.createVerticalStrut(4));
        content.add(loadButton);

        // One shared ornate frame around both buttons
        OrnateFramePanel framedContent = new OrnateFramePanel(
                content, 0, 10, 1, ORNATE_FRAME
        );

        // FramePanel hosts the single framed content
        framePanel = new FramePanel(framedContent, newGameButton, resumeButton, loadButton);
        framePanel.setMainMenuListener(listener);

        // Show Resume immediately if a profile already exists from a previous session
        refreshResumeAvailability();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(24, 24, 24, 24);
        add(framePanel, gbc);

        ThemeManager.get().register(this);
        refreshTheme();
    }

    public void setCurrentCollection(String collection) {
        this.currentCollection = (collection == null || collection.isBlank())
                ? "/Rome/Artifacts/"
                : collection;
        refreshTheme();
    }

    @Override
    public void refreshTheme() {
        Theme theme = ThemeManager.get().getCurrent();
        String path = BackgroundCatalog.backgroundFor(currentCollection, theme);

        try (var in = MainMenuPanel.class.getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Missing resource: " + path);
            backgroundImage = ImageIO.read(in);
        } catch (Exception e) {
            e.printStackTrace();
            backgroundImage = null;
        }
        if (framePanel != null) framePanel.refreshTheme();
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

    private JButton createMenuButton(String text) {
        // Anonymous subclass draws a luminance-adaptive halo behind the text
        // so it stays readable against any background image.
        // Same pattern as CollectionsPanel, keeps the main menu and collections menu visually consistent.
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                drawTextHalo(this, g);
                super.paintComponent(g);
            }
        };
        final float normalFont = 28f;
        final float hoverFont = 32f;
        btn.setFont(new Font("Serif", Font.BOLD, (int) normalFont));
        btn.setFocusPainted(false);
        btn.setFocusable(false);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        // Fixed size so the hover font change grows only the text+halo inside,
        // the button box stays put and the surrounding frame doesn't jump.
        // Sized so the hover-state pill nearly fills the button box horizontally,
        // for a properly tight hug on the longest label ("New Game").
        Dimension btnSize = new Dimension(200, 54);
        btn.setPreferredSize(btnSize);
        btn.setMinimumSize(btnSize);
        btn.setMaximumSize(btnSize);

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setFont(btn.getFont().deriveFont(hoverFont));
                btn.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setFont(btn.getFont().deriveFont(normalFont));
                btn.repaint();
            }
        });
        return btn;
    }

    // Soft pill behind the button text so it reads cleanly on any background.
    // The pill colour contrasts with the text colour (dark pill under light text,
    // light pill under dark text) so it works in both themes.
    // Mirrors CollectionsPanel.drawTextHalo for visual consistency.

    private static void drawTextHalo(JButton btn, Graphics g) {
        String text = btn.getText();
        if (text == null || text.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color textColor = btn.getForeground();
        // Rec. 601 luminance, matches perceived brightness.
        double lum = 0.299 * textColor.getRed()
                   + 0.587 * textColor.getGreen()
                   + 0.114 * textColor.getBlue();
        Color halo = lum > 128
                ? new Color(0, 0, 0, 150)
                : new Color(255, 255, 255, 150);

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

    private static class FramePanel extends JPanel implements ThemeAware {
        private final JComponent framed;
        private final JButton newGameButton;
        private final JButton resumeButton;
        private final JButton loadButton;
        private MainMenuListener menuListener;

        FramePanel(JComponent framed, JButton newGameButton, JButton resumeButton, JButton loadButton) {
            this.framed = framed;
            this.newGameButton = newGameButton;
            this.resumeButton = resumeButton;
            this.loadButton = loadButton;

            setOpaque(false);
            setLayout(new GridBagLayout());
            add(framed, new GridBagConstraints());
        }

        public void setMainMenuListener(MainMenuListener listener) {
            this.menuListener = listener;
        }

        @Override
        public void refreshTheme() {
            Theme.Palette.Base b = ThemeManager.get().palette().base;
            newGameButton.setForeground(b.text);
            resumeButton.setForeground(b.text);
            loadButton.setForeground(b.text);
            repaint();
        }
    }

}
