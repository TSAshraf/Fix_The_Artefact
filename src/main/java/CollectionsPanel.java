import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;

public class CollectionsPanel extends JPanel {

    private JButton ancientCyprusButton;
    private JButton ancientEgyptButton;
    private JButton ancientNearEastButton;
    private JButton ancientGreeceButton;
    private JButton romeButton;
    private JButton backButton;

    private Image backgroundImage;
    private CollectionsListener listener;

    public interface CollectionsListener {
        void onCollectionSelected(String collectionPath);
        void onBackToMenu();
    }

    public void setCollectionsListener(CollectionsListener listener) {
        this.listener = listener;
    }

    public CollectionsPanel() {
        setPreferredSize(new Dimension(600, 400));
        setOpaque(false);

        // Load background from classpath resources
        try (var in = getClass().getResourceAsStream("/Starting/Fantasy-Background.jpg")) {
            if (in == null) {
                throw new RuntimeException("Missing resource: /Starting/Fantasy-Background.jpg");
            }
            backgroundImage = ImageIO.read(in);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setLayout(new GridBagLayout());

        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        buttonPanel.setOpaque(false);

        ancientCyprusButton = new JButton("Ancient Cyprus");
        ancientCyprusButton.addActionListener(e -> {
            if (listener != null) listener.onCollectionSelected("/Ancient Cyprus");
        });
        buttonPanel.add(ancientCyprusButton);

        ancientGreeceButton = new JButton("Ancient Greece");
        ancientGreeceButton.addActionListener(e -> {
            if (listener != null) listener.onCollectionSelected("/Ancient Greece");
        });
        buttonPanel.add(ancientGreeceButton);

        ancientEgyptButton = new JButton("Ancient Egypt");
        ancientEgyptButton.addActionListener(e -> {
            if (listener != null) listener.onCollectionSelected("/Ancient Egypt");
        });
        buttonPanel.add(ancientEgyptButton);

        ancientNearEastButton = new JButton("Ancient Near East");
        ancientNearEastButton.addActionListener(e -> {
            if (listener != null) listener.onCollectionSelected("/Ancient Near East");
        });
        buttonPanel.add(ancientNearEastButton);

        romeButton = new JButton("Rome");
        romeButton.addActionListener(e -> {
            if (listener != null) listener.onCollectionSelected("/Rome");
        });
        buttonPanel.add(romeButton);

        backButton = new JButton("Back to Main Menu");
        backButton.addActionListener(e -> {
            if (listener != null) listener.onBackToMenu();
        });
        buttonPanel.add(backButton);

        add(buttonPanel, new GridBagConstraints());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
