import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;
import java.io.File;

// Panel for displaying collection selection buttons
public class CollectionsPanel extends JPanel {
    private JButton ancientCyprusButton; // Button to select Ancient Cyprus
    private JButton ancientEgyptButton; // Button to select Ancient Egypt
    private JButton ancientNearEastButton; // Button to select Ancient Near East
    private JButton ancientGreeceButton; // Button to select Ancient Greece
    private JButton romeButton; // Button to select Rome
    private JButton backButton; // Button to Return to opening screen
    private Image backgroundImage; // Background image for the panel
    private CollectionsListener listener; // Listener to handle collection selection and back action

    // Interface to notify collection selection and back action
    public interface CollectionsListener {
        void onCollectionSelected(String collectionName);
        void onBackToMenu();
    }

    // Setter for CollectionsListener
    public void setCollectionsListener(CollectionsListener listener) {
        this.listener = listener;
    }

    // Constructor: sets up the layout, loads the background image, and creates the buttons
    public CollectionsPanel() {
        // Set a smaller preferred size for the collections panel.
        setPreferredSize(new Dimension(600, 400));
        // Make this panel non-opaque so the background is visible.
        setOpaque(false);

        // Load the background image.
        try {
            backgroundImage = ImageIO.read(new File(
                    "/Users/taashfeen/Desktop/Jigsaw Game/src/Starting/Fantasy-Background.jpg"
            ));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Use GridBagLayout to center the content.
        setLayout(new GridBagLayout());

        // Create a transparent panel to hold the collection buttons.
        JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        buttonPanel.setOpaque(false);

        // Create and add the Ancient Cyprus button
        ancientCyprusButton = new JButton("Ancient Cyprus");
        ancientCyprusButton.addActionListener(e -> {
            if (listener != null) {
                listener.onCollectionSelected("Ancient Cyprus");
            }
        });
        buttonPanel.add(ancientCyprusButton);

        // Create and add the Ancient Greece button
        ancientGreeceButton = new JButton("Ancient Greece");
        ancientGreeceButton.addActionListener(e -> {
            if (listener != null) {
                listener.onCollectionSelected("Ancient Greece");
            }
        });
        buttonPanel.add(ancientGreeceButton);

        // Create and add the Ancient Egypt button
        ancientEgyptButton = new JButton("Ancient Egypt");
        ancientEgyptButton.addActionListener(e -> {
            if (listener != null) {
                listener.onCollectionSelected("Ancient Egypt");
            }
        });
        buttonPanel.add(ancientEgyptButton);

        // Create and add the Ancient Near East button
        ancientNearEastButton = new JButton("Ancient Near East");
        ancientNearEastButton.addActionListener(e -> {
            if (listener != null) {
                listener.onCollectionSelected("Ancient Near East");
            }
        });
        buttonPanel.add(ancientNearEastButton);

        // Create and add the Rome button
        romeButton = new JButton("Rome");
        romeButton.addActionListener(e -> {
            if (listener != null) {
                listener.onCollectionSelected("Rome");
            }
        });
        buttonPanel.add(romeButton);

        // Create and add the Back button
        backButton = new JButton("Back to Main Menu");
        backButton.addActionListener(e -> {
            if (listener != null) {
                listener.onBackToMenu();
            }
        });
        buttonPanel.add(backButton);

        add(buttonPanel, new GridBagConstraints()); // Add the button panel to the center of this CollectionsPanel
    }

    // Override the paintComponent method to draw the background image
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw the background image scaled to the panel size.
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}