import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class CollectionsPanel extends JPanel implements ThemeAware {
    private JButton ancientCyprusButton;
    private JButton ancientEgyptButton;
    private JButton ancientNearEastButton;
    private JButton ancientGreeceButton;
    private JButton romeButton;
    private JButton backButton;
    private BufferedImage backgroundImage;
    private CollectionsListener listener;
    private String currentCollection = "/Rome";     // Collections screen can have a “default” collection background

    public interface CollectionsListener {
        void onCollectionSelected(String collectionPath);
        void onBackToMenu();
    }

    public void setCollectionsListener(CollectionsListener listener) {
        this.listener = listener;
    }

    public CollectionsPanel() {
        setPreferredSize(new Dimension(600, 400));
        setOpaque(true);
        setLayout(new GridBagLayout());

        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        buttonPanel.setOpaque(false);

        ancientCyprusButton = new JButton("Ancient Cyprus");
        ancientCyprusButton.addActionListener(e -> {
            if (listener != null) listener.onCollectionSelected("/Ancient Cyprus/Artifacts/");
        });
        buttonPanel.add(ancientCyprusButton);

        ancientGreeceButton = new JButton("Ancient Greece");
        ancientGreeceButton.addActionListener(e -> {
            if (listener != null) listener.onCollectionSelected("/Ancient Greece/Artifacts/");
        });
        buttonPanel.add(ancientGreeceButton);

        ancientEgyptButton = new JButton("Ancient Egypt");
        ancientEgyptButton.addActionListener(e -> {
            if (listener != null) listener.onCollectionSelected("/Ancient Egypt/Artifacts/");
        });
        buttonPanel.add(ancientEgyptButton);

        ancientNearEastButton = new JButton("Ancient Near East");
        ancientNearEastButton.addActionListener(e -> {
            if (listener != null) listener.onCollectionSelected("/Ancient Near East/Artifacts/");
        });
        buttonPanel.add(ancientNearEastButton);

        romeButton = new JButton("Rome");
        romeButton.addActionListener(e -> {
            if (listener != null) listener.onCollectionSelected("/Rome/Artifacts/");
        });
        buttonPanel.add(romeButton);

        backButton = new JButton("Back to Main Menu");
        backButton.addActionListener(e -> {
            if (listener != null) listener.onBackToMenu();
        });
        buttonPanel.add(backButton);

        add(buttonPanel, new GridBagConstraints());

        // Register + apply theme-driven background immediately
        ThemeManager.get().register(this);
        refreshTheme();
    }

    /** Optional: if you later want the collections screen bg to match a hovered/selected item */
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

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            // fallback
            g.setColor(ThemeManager.get().palette().base.appBg);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}
