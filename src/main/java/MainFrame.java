import javax.swing.*;
import java.awt.*;

// MainFrame is the main window of the application
// It uses a CardLayout to switch between the main menu, collections, and game screens
public class MainFrame extends JFrame
        implements MainMenuPanel.MainMenuListener,
        CollectionsPanel.CollectionsListener,
        FixThePotGamePanel.GamePanelListener {

    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final MainMenuPanel menuPanel;
    private final CollectionsPanel collectionsPanel;
    private final FixThePotGamePanel gamePanel;

    public MainFrame() {
        setTitle("Fix the Pot");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

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

        // Put the card panel into the frame
        setContentPane(cardPanel);

        // IMPORTANT: don't rely on pack() (preferred sizes can be unreliable across panels)
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null); // Center the window
    }

    // MainMenuPanel callback: when Play is clicked
    @Override
    public void onPlayClicked() {
        cardLayout.show(cardPanel, "Collections");
    }

    // CollectionsPanel callback: when a collection is selected
    @Override
    public void onCollectionSelected(String collectionName) {
        gamePanel.loadCollection(collectionName);
        cardLayout.show(cardPanel, "Game");
    }

    // CollectionsPanel callback: back to main menu
    @Override
    public void onBackToMenu() {
        cardLayout.show(cardPanel, "Menu");
    }

    // Game panel callback: back to collections
    @Override
    public void onBackToCollections() {
        cardLayout.show(cardPanel, "Collections");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
