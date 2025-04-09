import javax.swing.*;
import java.awt.*;

//  MainFrame is the main window of the application
// It uses a cardLayout to switch between the main menu, collections, and game screens
public class MainFrame extends JFrame
        implements MainMenuPanel.MainMenuListener, CollectionsPanel.CollectionsListener, FixThePotGamePanel.GamePanelListener {

    private CardLayout cardLayout; // Layout manager to switch between different screens
    private JPanel cardPanel; // Panel that holds the different screens
    private MainMenuPanel menuPanel; // The Starting screen
    private CollectionsPanel collectionsPanel; // The screen for selecting collections
    private FixThePotGamePanel gamePanel; // The screen where the puzzle is played

    public MainFrame() {
        setTitle("Fix the Pot");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Exits the application when the window is closed

        // Use CardLayout to switch between screens.
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // 1) Starting screen (main menu)
        menuPanel = new MainMenuPanel();
        menuPanel.setMainMenuListener(this);
        cardPanel.add(menuPanel, "Menu");

        // 2) Collections screen
        collectionsPanel = new CollectionsPanel();
        collectionsPanel.setCollectionsListener(this);
        cardPanel.add(collectionsPanel, "Collections");

        // 3) Game panel
        gamePanel = new FixThePotGamePanel();
        gamePanel.setGamePanelListener(this); // Connect the back-to-collections listener
        cardPanel.add(gamePanel, "Game");

        add(cardPanel); // Add the card panel to the frame
        pack(); // Size the frame to fit its contents
        setLocationRelativeTo(null); // Center the window on the screen
    }

    // MainMenuPanel callback, when play button is clicked
    @Override
    public void onPlayClicked() {
        // Show the collections screen
        cardLayout.show(cardPanel, "Collections");
    }

    // CollectionsPanel callbacks, when a collection is selected
    @Override
    public void onCollectionSelected(String collectionName) {
        // Load the chosen collection in the game panel
        gamePanel.loadCollection(collectionName);
        // Switch to the game screen
        cardLayout.show(cardPanel, "Game");
    }

    // Callback from Collections panel, when back is selected, brings it back to starting screen
    @Override
    public void onBackToMenu() {
        cardLayout.show(cardPanel, "Menu");
    }

    // New callback for the Game Panel to go back to collections.
    @Override
    public void onBackToCollections() {
        cardLayout.show(cardPanel, "Collections");
    }

    // Main method, launches the application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}