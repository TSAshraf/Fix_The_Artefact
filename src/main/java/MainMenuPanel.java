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
    }

    public void setMainMenuListener(MainMenuListener listener) {
        this.listener = listener;
        if (framePanel != null) framePanel.setMainMenuListener(listener);
    }

    public MainMenuPanel() {
        setLayout(new GridBagLayout());
        setOpaque(false);

        JButton newGameButton = new JButton("New Game");
        JButton loadButton = new JButton("Load");

        newGameButton.setFont(new Font("Serif", Font.BOLD, 28));
        newGameButton.setFocusPainted(false);
        newGameButton.setFocusable(false);
        loadButton.setFont(new Font("Serif", Font.BOLD, 28));
        loadButton.setFocusPainted(false);
        loadButton.setFocusable(false);

        // Hover effect, increases button font size when hovered, for both play and load button
        newGameButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                newGameButton.setFont(newGameButton.getFont().deriveFont(32f));
                newGameButton.revalidate();
                newGameButton.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                newGameButton.setFont(newGameButton.getFont().deriveFont(28f));
                newGameButton.revalidate();
                newGameButton.repaint();
            }
        });
        loadButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                loadButton.setFont(loadButton.getFont().deriveFont(32f));
                loadButton.revalidate();
                loadButton.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                loadButton.setFont(loadButton.getFont().deriveFont(28f));
                loadButton.revalidate();
                loadButton.repaint();
            }
        });

        // Action listeners for both buttons
        newGameButton.addActionListener(e -> {
            if (listener != null) listener.onNewGameClicked();
        });
        loadButton.addActionListener(e -> {
            if (listener != null) listener.onLoadClicked();
        });

        // Make both button's "menu-style": transparent, no chunky Swing background
        newGameButton.setOpaque(false);
        newGameButton.setContentAreaFilled(false);
        newGameButton.setBorderPainted(false);
        loadButton.setOpaque(false);
        loadButton.setContentAreaFilled(false);
        loadButton.setBorderPainted(false);

        // Stack buttons vertically in one content panel
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        newGameButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(Box.createVerticalStrut(8));
        content.add(newGameButton);
        content.add(Box.createVerticalStrut(12));
        content.add(loadButton);
        content.add(Box.createVerticalStrut(8));

        // One shared ornate frame around both buttons
        OrnateFramePanel framedContent = new OrnateFramePanel(
                content, 8, 10, 1, ORNATE_FRAME
        );

        // FramePanel hosts the single framed content
        framePanel = new FramePanel(framedContent, newGameButton, loadButton);
        framePanel.setMainMenuListener(listener);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(24, 24, 24, 24);
        add(framePanel, gbc);

        ThemeManager.get().register(this);
        refreshTheme();
    }



    /** Optional: call this if you want main menu background to match chosen collection */
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
        JButton btn = new JButton(text);
        btn.setFont(new Font("Serif", Font.BOLD, 28));
        btn.setFocusPainted(false);
        btn.setFocusable(false);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setFont(btn.getFont().deriveFont(32f));
                btn.revalidate();
                btn.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setFont(btn.getFont().deriveFont(28f));
                btn.revalidate();
                btn.repaint();
            }
        });
        return btn;
    }

    private static class FramePanel extends JPanel implements ThemeAware {
        private final JComponent framed;
        private final JButton newGameButton;
        private final JButton loadButton;
        private MainMenuListener menuListener;

        FramePanel(JComponent framed, JButton newGameButton, JButton loadButton) {
            this.framed = framed;
            this.newGameButton = newGameButton;
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
            loadButton.setForeground(b.text);
            repaint();
        }
    }

}
