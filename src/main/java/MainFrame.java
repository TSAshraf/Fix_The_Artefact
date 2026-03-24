import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame
        implements MainMenuPanel.MainMenuListener,
        CollectionsPanel.CollectionsListener,
        FixThePotGamePanel.GamePanelListener,
        ProgressPanel.ProgressListener,
        LoadGamePanel.LoadGameListener,
        FavouritesPanel.FavouritesListener {

    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final MainMenuPanel menuPanel;
    private final CollectionsPanel collectionsPanel;
    private final ProgressPanel progressPanel;
    private final FixThePotGamePanel gamePanel;
    private final LoadGamePanel loadGamePanel;
    private final FavouritesPanel favouritesPanel;
    private String selectedCollection = "/Rome/Artifacts/";

    public MainFrame() {
        setTitle("Fix the Pot");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        menuPanel = new MainMenuPanel();
        menuPanel.setMainMenuListener(this);
        cardPanel.add(menuPanel, "Menu");

        collectionsPanel = new CollectionsPanel();
        collectionsPanel.setCollectionsListener(this);
        cardPanel.add(collectionsPanel, "Collections");

        progressPanel = new ProgressPanel();
        progressPanel.setProgressListener(this);
        cardPanel.add(progressPanel, "Progress");

        loadGamePanel = new LoadGamePanel();
        loadGamePanel.setLoadGameListener(this);
        cardPanel.add(loadGamePanel, "LoadGame");

        favouritesPanel = new FavouritesPanel();
        favouritesPanel.setFavouritesListener(this);
        cardPanel.add(favouritesPanel, "Favourites");

        gamePanel = new FixThePotGamePanel();
        gamePanel.setGamePanelListener(this);
        cardPanel.add(gamePanel, "Game");

        setContentPane(cardPanel);

        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
    }

    // ---------- Menu buttons ----------

    @Override
    public void onNewGameClicked() {
        String profileName = JOptionPane.showInputDialog(
                this,
                "Enter a name for your new profile:",
                "New Game",
                JOptionPane.PLAIN_MESSAGE
        );

        if (profileName == null || profileName.trim().isEmpty()) {
            return;
        }
        profileName = profileName.trim();

        if (SaveManager.profileExists(profileName)) {
            int overwrite = JOptionPane.showConfirmDialog(
                    this,
                    "Profile '" + profileName + "' already exists. Overwrite?",
                    "Profile Exists",
                    JOptionPane.YES_NO_OPTION
            );
            if (overwrite != JOptionPane.YES_OPTION) return;
        }

        GameState newState = new GameState();
        newState.profileName = profileName;
        SaveManager.setActiveProfile(profileName);
        SaveManager.save(newState);

        collectionsPanel.refreshProfile();
        cardLayout.show(cardPanel, "Collections");
    }

    @Override
    public void onLoadClicked() {
        loadGamePanel.refreshProfiles();
        cardLayout.show(cardPanel, "LoadGame");
    }

    @Override
    public void onProfileSelected(String profileName) {
        if (!SaveManager.profileExists(profileName)) {
            JOptionPane.showMessageDialog(this, "Profile not found.");
            return;
        }

        SaveManager.setActiveProfile(profileName);
        GameState state = SaveManager.loadOrDefault();
        selectedCollection = state.currentCollection;
        collectionsPanel.refreshProfile();
        cardLayout.show(cardPanel, "Collections");
    }

    // ---------- Collections ----------
    @Override
    public void onCollectionSelected(String collectionPath) {
        selectedCollection = collectionPath;

        GameState state = SaveManager.loadOrDefault();

        progressPanel.setCollection(selectedCollection);
        progressPanel.loadFrom(state);
        cardLayout.show(cardPanel, "Progress");
    }

    @Override
    public void onFavouritesClicked() {
        favouritesPanel.refreshFavourites();
        cardLayout.show(cardPanel, "Favourites");
    }

    @Override
    public void onBackToMenu() {
        cardLayout.show(cardPanel, "Menu");
    }

    // ---------- Favourites ----------
    @Override
    public void onFavouriteSelected(String jigsawPath) {
        // Determine which collection this jigsaw belongs to
        String collection = collectionForPath(jigsawPath);
        selectedCollection = collection;

        gamePanel.loadCollection(selectedCollection);

        // Select the specific jigsaw in the combo box
        gamePanel.selectJigsaw(jigsawPath);

        cardLayout.show(cardPanel, "Game");
    }

    @Override
    public void onBackToCollections() {
        collectionsPanel.refreshProfile();
        cardLayout.show(cardPanel, "Collections");
    }

    // ---------- Progress ----------
    @Override
    public void onPlaySelectedCollection(String collectionPath) {
        selectedCollection = collectionPath;

        gamePanel.loadCollection(selectedCollection);
        cardLayout.show(cardPanel, "Game");
    }

    // ---------- Helpers ----------

    /** Infer collection path from a full jigsaw resource path. */
    private String collectionForPath(String jigsawPath) {
        if (jigsawPath == null) return "/Rome/Artifacts/";
        // Paths look like "/Ancient Cyprus/Artifacts/jug-1.jpg"
        // We want "/Ancient Cyprus/Artifacts/"
        int artifactsIdx = jigsawPath.indexOf("/Artifacts/");
        if (artifactsIdx >= 0) {
            return jigsawPath.substring(0, artifactsIdx + "/Artifacts/".length());
        }
        return "/Rome/Artifacts/";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
