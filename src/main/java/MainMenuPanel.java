import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class MainMenuPanel extends JPanel implements ThemeAware {
    private FramePanel framePanel;
    private BufferedImage backgroundImage;
    private MainMenuListener listener;

    // If you want the main menu to follow the last played collection,
    // make this settable later (e.g., from your app controller).
    private String currentCollection = "/Rome";

    public interface MainMenuListener {
        void onPlayClicked();
    }

    public void setMainMenuListener(MainMenuListener listener) {
        this.listener = listener;
        if (framePanel != null) {
            framePanel.setMainMenuListener(listener);
        }
    }

    public MainMenuPanel() {
        setLayout(new GridBagLayout());
        setOpaque(false);

        // Load ornate frame once
        Image ornateFrame = null;
        try (var in = MainMenuPanel.class.getResourceAsStream("/Rome/Artifacts/Ornate-1-Photoroom.png")) {
            if (in == null) throw new RuntimeException("Missing resource: /Rome/Artifacts/Ornate-1-Photoroom.png");
            ornateFrame = ImageIO.read(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        framePanel = new FramePanel(ornateFrame);
        if (listener != null) framePanel.setMainMenuListener(listener);
        add(framePanel, new GridBagConstraints());
        ThemeManager.get().register(this);
        refreshTheme();
    }

    /** Optional: call this if you want main menu background to match chosen collection */
    public void setCurrentCollection(String collection) {
        this.currentCollection = collection;
        refreshTheme();
    }

    @Override
    public void refreshTheme() {
        // 1) Swap background based on theme + collection
        Theme theme = ThemeManager.get().getCurrent();
        String path = BackgroundCatalog.backgroundFor(currentCollection, theme);
        try (var in = MainMenuPanel.class.getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Missing resource: " + path);
            backgroundImage = ImageIO.read(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 2) Force Play button readable in both modes
        if (framePanel != null) framePanel.refreshTheme();

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    private class FramePanel extends JPanel implements ActionListener, ThemeAware {
        private final Image frameImage;
        private final JButton playButton;
        private MainMenuListener menuListener;
        private int offsetX = 0;
        private int offsetY = -5;
        public FramePanel(Image frameImage) {
            this.frameImage = frameImage;
            setOpaque(false);
            setLayout(new GridBagLayout());

            if (frameImage != null) {
                setPreferredSize(new Dimension(frameImage.getWidth(null), frameImage.getHeight(null)));
            } else {
                setPreferredSize(new Dimension(300, 200));
            }
            playButton = new JButton("Play");
            playButton.setFont(new Font("Serif", Font.BOLD, 28));
            // text-only look
            playButton.setOpaque(false);
            playButton.setContentAreaFilled(false);
            playButton.setBorderPainted(false);
            playButton.setFocusPainted(false);
            playButton.setFocusable(false);
            playButton.addActionListener(this);
            playButton.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    playButton.setFont(playButton.getFont().deriveFont(32f));
                    playButton.revalidate();
                    playButton.repaint();
                }
                @Override public void mouseExited(MouseEvent e) {
                    playButton.setFont(playButton.getFont().deriveFont(28f));
                    playButton.revalidate();
                    playButton.repaint();
                }
            });

            add(playButton, new GridBagConstraints());
        }

        @Override
        public void refreshTheme() {
            Theme t = ThemeManager.get().getCurrent();
            boolean dark = (t == Theme.DARK);
            playButton.setForeground(dark ? Color.WHITE : Color.BLACK);
            repaint();
        }

        public void setMainMenuListener(MainMenuListener listener) {
            this.menuListener = listener;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (frameImage != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                int padding = 10;
                Rectangle btnBounds = playButton.getBounds();
                int frameX = btnBounds.x - padding + offsetX;
                int frameY = btnBounds.y - padding + offsetY;
                int frameW = btnBounds.width + 2 * padding;
                int frameH = btnBounds.height + 2 * padding;
                g2.drawImage(frameImage, frameX, frameY, frameW, frameH, this);
                g2.dispose();
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (menuListener != null) menuListener.onPlayClicked();
        }
    }
}
