import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame
        implements MainMenuPanel.MainMenuListener,
        CollectionsPanel.CollectionsListener,
        FixThePotGamePanel.GamePanelListener,
        ProgressPanel.ProgressListener,
        LoadGamePanel.LoadGameListener,
        FavouritesPanel.FavouritesListener,
        CyprusMapPanel.MapListener,
        GreeceMapPanel.MapListener,
        EgyptMapPanel.MapListener,
        RomeMapPanel.MapListener,
        NearEastMapPanel.MapListener {

    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final MainMenuPanel menuPanel;
    private final CollectionsPanel collectionsPanel;
    private final ProgressPanel progressPanel;
    private final FixThePotGamePanel gamePanel;
    private final LoadGamePanel loadGamePanel;
    private final FavouritesPanel favouritesPanel;
    private final ProfilePanel profilePanel;
    private final AvatarChooserPanel avatarChooserPanel;
    private final NewProfileOverlay newProfileOverlay;
    private final CyprusMapPanel cyprusMapPanel;
    private final GreeceMapPanel greeceMapPanel;
    private final EgyptMapPanel egyptMapPanel;
    private final RomeMapPanel romeMapPanel;
    private final NearEastMapPanel nearEastMapPanel;
    private String selectedCollection = "/Rome/Artifacts/";
    private ThemedConfirmOverlay corruptionOverlay;

    // True only during the New Game to name to avatar creation flow. Used by onAvatarCancelled
    // to decide whether to delete the half-built profile or just go back to the profile screen.
    private boolean creatingNewProfile = false;

    // Tracks which card to return to when the Profile screen's Back button is pressed.
    // Set whenever we navigate to the Profile screen so back is context-aware.
    private String profileReturnCard = "Collections";

    // Same idea for the Favourites screen, set to "Game" when opened via the toolbar
    // button so Back returns the player to their puzzle rather than Collections.
    private String favouritesReturnCard = "Collections";
    private TutorialController tutorial;

    public MainFrame() {
        setTitle("Fix the Pot");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Custom app icon, replaces the default Java Duke that shows in the dock/taskbar/title bar.
        // setIconImages covers the window itself; Taskbar.setIconImage covers the macOS dock + Windows taskbar.
        // Both are guarded because some desktop environments reject either call.

        try {
            setIconImages(AppIcon.sizes());
        } catch (Exception ignored) {}
        try {
            if (java.awt.Taskbar.isTaskbarSupported()) {
                java.awt.Taskbar.getTaskbar().setIconImage(AppIcon.create(512));
            }
        } catch (UnsupportedOperationException | SecurityException ignored) {}

        // Tooltips appear faster (300ms vs Swing's default 750ms) so users can discover
        // icon-only button functions without a noticeable wait (HE-13 / HE-16).
        javax.swing.ToolTipManager.sharedInstance().setInitialDelay(300);

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

        profilePanel = new ProfilePanel();
        profilePanel.setProfileListener(new ProfilePanel.ProfileListener() {
            @Override
            public void onChangeAvatarClicked() {
                cardLayout.show(cardPanel, "AvatarChooser");
            }
            @Override
            public void onBackClicked() {
                // Return to whichever screen the user came from (Collections by default,
                // Game when they clicked the profile button in the game toolbar).
                if ("Game".equals(profileReturnCard)) {
                    cardLayout.show(cardPanel, "Game");
                } else {
                    collectionsPanel.refreshProfile();
                    cardLayout.show(cardPanel, "Collections");
                }
            }
        });

        cardPanel.add(profilePanel, "Profile");

        avatarChooserPanel = new AvatarChooserPanel();
        avatarChooserPanel.setAvatarListener(new AvatarChooserPanel.AvatarListener() {
            @Override
            public void onAvatarConfirmed(String style, String seed,
                                          java.util.Map<String, String> options, String cachedPath) {
                boolean wasCreatingNewProfile = creatingNewProfile;
                creatingNewProfile = false;
                GameState state = SaveManager.loadOrDefault();
                state.avatarStyle = style;
                state.avatarSeed = seed;
                state.avatarOptions = options;
                state.avatarImagePath = cachedPath;
                SaveManager.save(state);

                collectionsPanel.refreshProfile();
                cardLayout.show(cardPanel, "Collections");

                // First-run tutorial, only on newly-created profiles and only if it hasn't been seen before on this profile.
                // Deferred to the next EDT pass so the Collections layout is settled before we measure targets.
                if (wasCreatingNewProfile && !state.tutorialShown) {
                    SwingUtilities.invokeLater(() -> tutorial.start());
                }
            }
            @Override
            public void onAvatarCancelled() {
                if (creatingNewProfile) {
                    // Discard the half-built profile created during the naming step so that
                    // cancelling the avatar selection fully abandons the creation flow rather
                    // than leaving an incomplete profile sitting in the save directory.
                    creatingNewProfile = false;
                    String activeName = SaveManager.getActiveProfile();
                    if (activeName != null) {
                        SaveManager.deleteProfile(activeName);
                        SaveManager.setActiveProfile(null);
                    }
                    menuPanel.refreshResumeAvailability();
                    cardLayout.show(cardPanel, "Menu");
                } else {
                    // Existing user changing their avatar and cancelling, just go back
                    // to the profile screen without touching the save file.
                    profilePanel.refresh();
                    cardLayout.show(cardPanel, "Profile");
                }
            }
        });
        cardPanel.add(avatarChooserPanel, "AvatarChooser");

        gamePanel = new FixThePotGamePanel();
        gamePanel.setGamePanelListener(this);
        cardPanel.add(gamePanel, "Game");

        cyprusMapPanel = new CyprusMapPanel();
        cyprusMapPanel.setMapListener(this);
        cardPanel.add(cyprusMapPanel, "CyprusMap");

        greeceMapPanel = new GreeceMapPanel();
        greeceMapPanel.setMapListener(this);
        cardPanel.add(greeceMapPanel, "GreeceMap");

        egyptMapPanel = new EgyptMapPanel();
        egyptMapPanel.setMapListener(this);
        cardPanel.add(egyptMapPanel, "EgyptMap");

        romeMapPanel = new RomeMapPanel();
        romeMapPanel.setMapListener(this);
        cardPanel.add(romeMapPanel, "RomeMap");

        nearEastMapPanel = new NearEastMapPanel();
        nearEastMapPanel.setMapListener(this);
        cardPanel.add(nearEastMapPanel, "NearEastMap");

        // Themed new-profile overlay (replaces JOptionPane)
        newProfileOverlay = new NewProfileOverlay();
        newProfileOverlay.setListener(new NewProfileOverlay.ProfileCreationListener() {
            @Override
            public void onProfileCreated(String profileName) {
                hideNewProfileOverlay();
                creatingNewProfile = true;

                GameState newState = new GameState();
                newState.profileName = profileName;
                SaveManager.setActiveProfile(profileName);
                SaveManager.save(newState);

                avatarChooserPanel.setProfileName(profileName);
                cardLayout.show(cardPanel, "AvatarChooser");
            }

            @Override
            public void onCancelled() {
                hideNewProfileOverlay();
            }
        });

        setContentPane(cardPanel);

        // Tutorial (HE-28 extension)
        tutorial = new TutorialController(this, cardLayout, cardPanel);
        tutorial.setCollectionCardTarget(() -> {
            JComponent c = collectionsPanel.getTutorialTargetCard();
            return TutorialController.boundsInOverlaySpace(c, getTutorialOverlayComponent());
        });
        tutorial.setMapSiteTarget(() -> {
            // Pick the currently visible map panel's View All label
            Rectangle r = null;
            if (cyprusMapPanel.isVisible())     r = cyprusMapPanel.getTutorialTargetBounds();
            else if (greeceMapPanel.isVisible())   r = greeceMapPanel.getTutorialTargetBounds();
            else if (egyptMapPanel.isVisible())    r = egyptMapPanel.getTutorialTargetBounds();
            else if (romeMapPanel.isVisible())     r = romeMapPanel.getTutorialTargetBounds();
            else if (nearEastMapPanel.isVisible()) r = nearEastMapPanel.getTutorialTargetBounds();
            if (r == null) return null;
            // Convert the map-panel-local rectangle into overlay coordinates
            JComponent visibleMap = visibleMapPanel();
            if (visibleMap == null) return null;
            return SwingUtilities.convertRectangle(visibleMap, r, getTutorialOverlayComponent());
        });
        tutorial.setJigsawRowTarget(() ->
                TutorialController.boundsInOverlaySpace(progressPanel.getTutorialTargetRow(),
                        getTutorialOverlayComponent()));
        tutorial.setPuzzleAreaTarget(() ->
                TutorialController.boundsInOverlaySpace(gamePanel.getTutorialTargetPuzzleArea(),
                        getTutorialOverlayComponent()));
        tutorial.setArrowsTarget(() ->
                TutorialController.boundsInOverlaySpace(gamePanel.getTutorialTargetArrows(),
                        getTutorialOverlayComponent()));
        tutorial.setHelpButtonTarget(() ->
                TutorialController.boundsInOverlaySpace(gamePanel.getTutorialTargetHelpButton(),
                        getTutorialOverlayComponent()));
        tutorial.setBackButtonTarget(() ->
                TutorialController.boundsInOverlaySpace(gamePanel.getTutorialTargetBackButton(),
                        getTutorialOverlayComponent()));
        tutorial.setCompletionListener(() -> {
            // Persist tutorialShown so it doesn't auto-run again on this profile
            GameState state = SaveManager.loadOrDefault();
            state.tutorialShown = true;
            SaveManager.save(state);
        });

        setSize(1100, 700);
        // Minimum sized so that (a) all 17 toolbar buttons fit on a single FlowLayout
        // row without wrapping, and (b) the puzzle area retains at least 640*600 of
        // usable space after the toolbar + padding. Below this, controls begin to
        // overlap or the puzzle becomes too small to play comfortably.
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
    }

    // Menu buttons

    @Override
    public void onNewGameClicked() {
        showNewProfileOverlay();
    }

    private void showNewProfileOverlay() {
        newProfileOverlay.reset();
        JLayeredPane layered = getRootPane().getLayeredPane();
        newProfileOverlay.setBounds(0, 0, layered.getWidth(), layered.getHeight());
        layered.add(newProfileOverlay, JLayeredPane.MODAL_LAYER);
        newProfileOverlay.setVisible(true);
        layered.revalidate();
        layered.repaint();
    }

    private void hideNewProfileOverlay() {
        newProfileOverlay.setVisible(false);
        Container parent = newProfileOverlay.getParent();
        if (parent != null) {
            parent.remove(newProfileOverlay);
            parent.revalidate();
            parent.repaint();
        }
    }

    @Override
    public void onLoadClicked() {
        loadGamePanel.refreshProfiles();
        cardLayout.show(cardPanel, "LoadGame");
    }

    @Override
    public void onResumeClicked() {
        // Resume the most recently played profile at its last jigsaw.
        // The button is hidden when no profile exists, but we still null-check defensively.
        String mostRecent = SaveManager.findMostRecentProfile();
        if (mostRecent == null) return;

        // Strict load so corruption is surfaced before we set the active profile and touch the game panel.
        GameState state;
        try {
            state = SaveManager.loadProfileChecked(mostRecent);
        } catch (SaveCorruptException sce) {
            showCorruptionDialog(sce);
            return;
        }
        if (state == null) return;

        SaveManager.setActiveProfile(mostRecent);

        // Critical: re-load the new profile's state into the game panel BEFORE calling
        // loadCollection, otherwise loadCollection's snapshotToSaveState() would write the
        // previously-active profile's stale in-memory state into the new profile's file.
        gamePanel.reloadFromActiveProfile();

        selectedCollection = state.currentCollection != null
                ? state.currentCollection
                : "/Rome/Artifacts/";

        gamePanel.loadCollection(selectedCollection);
        if (state.selectedJigsawPath != null && !state.selectedJigsawPath.isBlank()) {
            gamePanel.selectJigsaw(state.selectedJigsawPath);
        }
        cardLayout.show(cardPanel, "Game");
    }

    @Override
    public void onProfileSelected(String profileName) {
        if (!SaveManager.profileExists(profileName)) {
            JOptionPane.showMessageDialog(this, "Profile not found.");
            return;
        }

        // Strict load, surfaces corruption as an exception instead of silently
        // resurrecting the profile as a blank GameState (HE-26).
        GameState state;
        try {
            state = SaveManager.loadProfileChecked(profileName);
        } catch (SaveCorruptException sce) {
            showCorruptionDialog(sce);
            return;
        }
        if (state == null) {
            JOptionPane.showMessageDialog(this, "Profile not found.");
            return;
        }

        SaveManager.setActiveProfile(profileName);
        selectedCollection = state.currentCollection != null
                ? state.currentCollection
                : "/Rome/Artifacts/";
        collectionsPanel.refreshProfile();
        cardLayout.show(cardPanel, "Collections");
    }

    // Reference component used by TutorialController target providers to convert
    // panel-local bounds into overlay coordinates.
    private JComponent getTutorialOverlayComponent() {
        return tutorial != null ? tutorial.getOverlayComponent() : null;
    }

    // Returns whichever map panel is currently visible, or null.
    private JComponent visibleMapPanel() {
        if (cyprusMapPanel.isVisible())     return cyprusMapPanel;
        if (greeceMapPanel.isVisible())     return greeceMapPanel;
        if (egyptMapPanel.isVisible())      return egyptMapPanel;
        if (romeMapPanel.isVisible())       return romeMapPanel;
        if (nearEastMapPanel.isVisible())   return nearEastMapPanel;
        return null;
    }

    // Start the tutorial publicly so SettingsOverlay can replay it.
    // The first step anchors to the Collections screen, so we switch cards first,
    // otherwise the "Show tutorial" button replays the overlay
    // on top of whatever screen the user was on (usually the game panel),
    // where none of the first-step targets exist.

    public void startTutorial() {
        if (tutorial == null) return;
        collectionsPanel.refreshProfile();
        cardLayout.show(cardPanel, "Collections");
        SwingUtilities.invokeLater(tutorial::start);
    }

    // Called by the game panel when a puzzle is solved. Finishes an in-progress tutorial.
    public void notifyPuzzleSolved() {
        if (tutorial != null) tutorial.onPuzzleSolved();
    }

    // Show a themed overlay explaining that a profile file was corrupt and has been backed up.
    private void showCorruptionDialog(SaveCorruptException sce) {
        if (corruptionOverlay == null) corruptionOverlay = new ThemedConfirmOverlay();

        String backupName = sce.getBackupPath() != null
                ? sce.getBackupPath().getFileName().toString()
                : "(backup creation failed)";
        String message = "The profile '" + sce.getProfileName() + "' could not be loaded "
                + "because its save file is corrupt or unreadable. "
                + "The original file has been moved aside as " + backupName + " "
                + "so it isn't overwritten by the next save. "
                + "You can attempt to recover it manually from the saves folder.";

        corruptionOverlay.showMessage("Save File Corrupt", message, () -> {
            corruptionOverlay.setVisible(false);
            Container parent = corruptionOverlay.getParent();
            if (parent != null) {
                parent.remove(corruptionOverlay);
                parent.revalidate();
                parent.repaint();
            }
            // Refresh the load screen so the corrupt profile no longer appears
            loadGamePanel.refreshProfiles();
            menuPanel.refreshResumeAvailability();
        });

        JLayeredPane layered = getRootPane().getLayeredPane();
        corruptionOverlay.setBounds(0, 0, layered.getWidth(), layered.getHeight());
        layered.add(corruptionOverlay, JLayeredPane.MODAL_LAYER);
        layered.revalidate();
        layered.repaint();
    }

    // Collections
    @Override
    public void onCollectionSelected(String collectionPath) {
        selectedCollection = collectionPath;

        boolean isMapBacked = "/Ancient Cyprus/Artifacts/".equals(collectionPath)
                || "/Ancient Greece/Artifacts/".equals(collectionPath)
                || "/Ancient Egypt/Artifacts/".equals(collectionPath)
                || "/Rome/Artifacts/".equals(collectionPath)
                || "/Ancient Near East/Artifacts/".equals(collectionPath);
        if (tutorial != null && tutorial.isActive()) {
            tutorial.onCollectionSelected(isMapBacked);
        }

        // Collections with interactive maps
        if ("/Ancient Cyprus/Artifacts/".equals(collectionPath)) {
            cardLayout.show(cardPanel, "CyprusMap");
            if (tutorial != null && tutorial.isActive()) tutorial.onScreenSettled();
            return;
        }
        if ("/Ancient Greece/Artifacts/".equals(collectionPath)) {
            cardLayout.show(cardPanel, "GreeceMap");
            if (tutorial != null && tutorial.isActive()) tutorial.onScreenSettled();
            return;
        }
        if ("/Ancient Egypt/Artifacts/".equals(collectionPath)) {
            cardLayout.show(cardPanel, "EgyptMap");
            if (tutorial != null && tutorial.isActive()) tutorial.onScreenSettled();
            return;
        }
        if ("/Rome/Artifacts/".equals(collectionPath)) {
            cardLayout.show(cardPanel, "RomeMap");
            if (tutorial != null && tutorial.isActive()) tutorial.onScreenSettled();
            return;
        }
        if ("/Ancient Near East/Artifacts/".equals(collectionPath)) {
            cardLayout.show(cardPanel, "NearEastMap");
            if (tutorial != null && tutorial.isActive()) tutorial.onScreenSettled();
            return;
        }

        GameState state = SaveManager.loadOrDefault();

        progressPanel.setCollection(selectedCollection);
        progressPanel.loadFrom(state);
        cardLayout.show(cardPanel, "Progress");
        if (tutorial != null && tutorial.isActive()) tutorial.onScreenSettled();
    }

    @Override
    public void onFavouritesClicked() {
        favouritesReturnCard = "Collections";
        favouritesPanel.refreshFavourites();
        cardLayout.show(cardPanel, "Favourites");
    }

    @Override
    public void onBackToMenu() {
        // A profile may have been created or saved since the menu was last shown,
        // so re-evaluate whether the Resume button should be visible.
        menuPanel.refreshResumeAvailability();
        cardLayout.show(cardPanel, "Menu");
    }

    @Override
    public void onJumpToProfile() {
        // Direct route from the game panel to the profile screen (P4 usability feedback,
        // "quick access buttons for profile, favourites from within the game panel").
        // Previously required two clicks (game to collections to avatar).
        profileReturnCard = "Game";
        profilePanel.refresh();
        cardLayout.show(cardPanel, "Profile");
    }

    @Override
    public void onJumpToFavourites() {
        // Counterpart to onJumpToProfile, same P4 feedback. Tracks the origin so
        // Back on the favourites screen returns to the game instead of Collections.
        favouritesReturnCard = "Game";
        favouritesPanel.refreshFavourites();
        cardLayout.show(cardPanel, "Favourites");
    }

    // Favourites
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
        // Determine which panel is currently visible to decide where "back" goes
        for (Component c : cardPanel.getComponents()) {
            if (c.isVisible()) {
                if (c == favouritesPanel && "Game".equals(favouritesReturnCard)) {
                    // Opened from the in-game toolbar, return there, not Collections.
                    favouritesReturnCard = "Collections";
                    cardLayout.show(cardPanel, "Game");
                    return;
                }
                if (c == progressPanel) {
                    // Progress panel's back:
                    // If a map collection, go back to its map; otherwise collections
                    progressPanel.clearHighlights();
                    if ("/Ancient Cyprus/Artifacts/".equals(selectedCollection)) {
                        cardLayout.show(cardPanel, "CyprusMap");
                    } else if ("/Ancient Greece/Artifacts/".equals(selectedCollection)) {
                        cardLayout.show(cardPanel, "GreeceMap");
                    } else if ("/Ancient Egypt/Artifacts/".equals(selectedCollection)) {
                        cardLayout.show(cardPanel, "EgyptMap");
                    } else if ("/Rome/Artifacts/".equals(selectedCollection)) {
                        cardLayout.show(cardPanel, "RomeMap");
                    } else if ("/Ancient Near East/Artifacts/".equals(selectedCollection)) {
                        cardLayout.show(cardPanel, "NearEastMap");
                    } else {
                        collectionsPanel.refreshProfile();
                        cardLayout.show(cardPanel, "Collections");
                    }
                } else if (c == cyprusMapPanel || c == greeceMapPanel || c == egyptMapPanel || c == romeMapPanel || c == nearEastMapPanel) {
                    // Any map panel's back to collections
                    collectionsPanel.refreshProfile();
                    cardLayout.show(cardPanel, "Collections");
                } else {
                    // Game panel's back to jump straight to Collections, skipping the
                    // intermediate Progress screen. Minimises clicks for users who want
                    // to browse other collections without stepping through the timeline.
                    collectionsPanel.refreshProfile();
                    cardLayout.show(cardPanel, "Collections");
                }
                return;
            }
        }
        // Fallback
        collectionsPanel.refreshProfile();
        cardLayout.show(cardPanel, "Collections");
    }

    // Progress
    @Override
    public void onPlaySelectedCollection(String collectionPath) {
        selectedCollection = collectionPath;
        gamePanel.loadCollection(selectedCollection);
        cardLayout.show(cardPanel, "Game");
        if (tutorial != null && tutorial.isActive()) {
            tutorial.onGameScreenEntered();
        }
    }

    @Override
    public void onPlaySpecificJigsaw(String collectionPath, String jigsawPath) {
        selectedCollection = collectionPath;
        gamePanel.loadCollection(selectedCollection);
        gamePanel.selectJigsaw(jigsawPath);
        cardLayout.show(cardPanel, "Game");
        if (tutorial != null && tutorial.isActive()) {
            tutorial.onGameScreenEntered();
        }
    }

    @Override
    public void onProfileClicked() {
        // Collections avatar click to profile should return to Collections on back.
        profileReturnCard = "Collections";
        profilePanel.refresh();
        cardLayout.show(cardPanel, "Profile");
    }

    // Map
    @Override
    public void onSiteSelected(String siteName, java.util.List<String> artefactPaths) {
        // Determine which collection based on which map is visible
        for (Component c : cardPanel.getComponents()) {
            if (c.isVisible()) {
                if (c == greeceMapPanel) {
                    selectedCollection = "/Ancient Greece/Artifacts/";
                } else if (c == egyptMapPanel) {
                    selectedCollection = "/Ancient Egypt/Artifacts/";
                } else if (c == romeMapPanel) {
                    selectedCollection = "/Rome/Artifacts/";
                } else if (c == nearEastMapPanel) {
                    selectedCollection = "/Ancient Near East/Artifacts/";
                } else {
                    selectedCollection = "/Ancient Cyprus/Artifacts/";
                }
                break;
            }
        }
        if (tutorial != null && tutorial.isActive()) {
            tutorial.onSiteSelected();
        }

        GameState state = SaveManager.loadOrDefault();

        progressPanel.setCollection(selectedCollection);
        progressPanel.setSiteContext(siteName);
        progressPanel.loadFrom(state);
        progressPanel.highlightArtefacts(artefactPaths);

        cardLayout.show(cardPanel, "Progress");
        if (tutorial != null && tutorial.isActive()) tutorial.onScreenSettled();
    }

    // Helpers
    // Infer collection path from a full jigsaw resource path.
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
