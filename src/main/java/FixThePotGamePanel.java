// Imports required for GUI, image handling, and file operations
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import java.io.File;

// This is the main panel for the "Fix the pot game", extension of JPanel
public class FixThePotGamePanel extends JPanel {
    private InfoOverlayPanel infoOverlay;
    private JLayeredPane layeredPane;
    private JComponent glassPaneRef;
    private InfoOverlayPanel persistentOverlay;
    private JButton previousJigsawButton; // Button to move to previous Jigsaw
    private FixThePotGame puzzlePanel; // Puzzle panel where the game is played
    private JButton restartButton; // Button to Restart (reconstruct) the jigsaw
    private JButton showCompletedButton; // Button to show the completed image
    private JButton jigsawSplitButton; // Button to let the user choose Jigsaw Difficulty
    private JButton nextJigsawButton; // Button to move to the next jigsaw
    private JButton extraInfoButton; // Button to open Extra information and view sources
    private JButton musicToggleButton; // Button to the music on/off
    private String currentTrackname; // Name of the current music tracj
    private JComboBox<String> musicComboBox; // Combobox to select images (Jigsaw Puzzles)
    private JButton chooseJigsawButton;  // Button to choose a jigsaw
    private JComboBox<String> imageComboBox; // Combobox to select images (Jigsaw Puzzles)
    private JButton backToCollectionsButton; // Button to return to the collections panel
    private JButton timerButton; // Button for displaying and controlling the timer
    private Timer timer; // Timer instance
    private int elapsedSeconds; // Time elapsed, in seconds
    private boolean timerRunning; // Whether the timer is on or not
    private Map<String, ImageInfo> imageInfoMap; // Map for extra information about each image
    private MusicPlayer musicPlayer; // Music player instance
    private BufferedImage originalBackground; // Original background image (used in play screen and main menU)
    private BufferedImage blurredBackground; // Blurred background for game-screen
    private String musicFolderPath = "/Users/taashfeen/Desktop/Jigsaw Game/src/Music/"; // Music folder path
    private ImageOverlay completedOverlay; // Overlay panel for displaying the full completed image
    private ImagePeek completedPeek; // Small preview panel shown when hovering over the button
    private javax.swing.Timer peekHideTimer; // Timer used to delay hiding the small preview after mouse exit
    private SplitChooserOverlay splitOverlay; // Easy/Med/Hard/Custom popover
    private RowsColsOverlay rowsColsOverlay; // Holds the small rows/cols picker overlay
    private JButton zenModeButton; // Zen mode
    private boolean zenMode = false; // Zen mode
    private JComponent[] zenHideComponents; // Components to hide when Zen mode is on
    private JPanel controlPanel;

    private String[] imageOptions = {
            // File paths for Ancient Cyprus Jigsaw Images
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/jug-1.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/jug-2.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/jug-3.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/jug-4.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/jug-5.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/bowl-1.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/bowl-2.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/bowl-3.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/bowl-4.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/bowl-5.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/Pyxis-Lid.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/Spindle-Whorl.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/Temple-Boy.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/Human-Remains-1.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/VotiveStatueHead.jpg",

            // File paths for Ancient Greece Jigsaw Images
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Amour Helmet.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Wine Flask.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Wine Flask 2.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Amphora.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Drinking Vessel.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Krater.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Kylix.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Aryballos.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Aryballos 2.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Aryballos 3.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Rhyton.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Horse votive offering.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Fragments of deer figurine.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Wreath Shaped votive offering.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Loutrophoros.jpg",

            // File paths for Rome Jigsaw Images
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Serapis with Cerberus.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Ash Chest.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Ash Chest 2.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Sculpture of Cybele .jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Statue of Anchirroe .jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bust of Boy .jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bust of Trajan.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bowl 1 .jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bowl 2.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bowl 3.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bust of a Priest of Isis.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Statue of Apollo.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Statuette of Hermes.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Statue of Bacchus.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Sarcophagus.jpg",

            // File paths for Ancient Near East Jigsaw Images
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Master of Animals Standard.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/South Arabian Statue.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Seated Goddess with a Child .jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Plaque with horned lion-griffins.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Panel with Lion.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Standing Male Worshiper.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Head of a Ruler.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Helmet with Divine figures.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Statue of Gudea.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Enthroned Deity.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Shaft-hole axe head.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Openwork furniture plaque.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Kneeling Bull.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Headdress.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Stag Vessel.jpeg",

            // File paths for  Ancient Egypt Jigsaw Images
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Shabti of Djedkhonsuefankh.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Figurine of a Pygmy Dance Leader.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Schist Statuette Fragment.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Offering Table.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Amulet of Jackal Headed Deity.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Composite Papyrus Capital.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Chariots with Court Ladies.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Wedjat Eye Amulet.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Artist's Sketch of a Sparrow.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Ring, signet.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Ring with Cat and Kittens.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Sarcophagus of Harkhebit.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Kneeling statue of Hatshepsut.jpeg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Inner Coffin Box of Taenty.jpg",
            "/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Game of Hounds and Jackals.jpeg"
    };


    // Listener Interface for Screen Navigation
    public interface GamePanelListener {
        void onBackToCollections();
    }
    private GamePanelListener gamePanelListener; // Listener instance
    // Setter Interface for Screen Navigation
    public void setGamePanelListener(GamePanelListener listener) {
        this.gamePanelListener = listener;
    }

    // Sets up the game panel
    public FixThePotGamePanel() {
        setLayout(new BorderLayout()); // Set layout
        setOpaque(false); // Set opacity
        loadBackgroundImages(); // Load background image and create blurred background.
        createPuzzlePanel(); // Create the puzzle panel.
        buildControlPanel(); // Build the control panel (all buttons and controls in one row)
        initializeExtraInfoMap(); // Initialize the extra info map.
        setupTimer(); // Set up the timer
        // Set initial image
        imageComboBox.setSelectedIndex(0);
        puzzlePanel.setImage(imageOptions[0]);
        puzzlePanel.setPuzzleSolvedListener(() -> nextJigsawButton.setEnabled(true)); // Register puzzle solved listener
        revalidate();
        SwingUtilities.invokeLater(() -> {
            JFrame f = (JFrame) SwingUtilities.getWindowAncestor(FixThePotGamePanel.this);
            if (f != null) {
                JRootPane rp = f.getRootPane();
                glassPaneRef = (JComponent) rp.getGlassPane();
                glassPaneRef.setLayout(null);       // absolute positioning
                glassPaneRef.setVisible(false);     // it's normally hidden

                persistentOverlay = new InfoOverlayPanel();
                // default starting position on screen:
                persistentOverlay.setLocation(40, 40);
                // NOTE: we don't add it yet, Info button will add+show it
            }
        });
    }

    // Call this whenever we switch puzzles
    private void hideInfoOverlay() {
        if (glassPaneRef != null) {
            glassPaneRef.setVisible(false);      // hide the whole overlay layer
            glassPaneRef.remove(persistentOverlay); // make sure the card is not still attached
            glassPaneRef.revalidate();
            glassPaneRef.repaint();
        }
    }

    // Method to load background images, and use BlurUtil class to created blurred version for the game screen
    private void loadBackgroundImages() {
        try {
            originalBackground = ImageIO.read(new File("/Users/taashfeen/Desktop/Jigsaw Game/src/Starting/Fantasy-Background.jpg"));
            blurredBackground = BlurUtil.blurImage(originalBackground, 5);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Create and configure puzzle panel
    private void createPuzzlePanel() {
        puzzlePanel = new FixThePotGame();
        puzzlePanel.setOpaque(false); // Transparent to blend in with background
        puzzlePanel.setPreferredSize(new Dimension(800, 500));

        // Create a layered pane so we can float the info card above the puzzle
        layeredPane = new JLayeredPane();
        layeredPane.setOpaque(false);

        // We'll size/position puzzlePanel manually inside layeredPane
        // Give it an initial bound; we'll also fix this on resize in doLayout()
        puzzlePanel.setBounds(0, 0, 800, 500);

        layeredPane.add(puzzlePanel, JLayeredPane.DEFAULT_LAYER);

        // Add layeredPane (not puzzlePanel) to the main panel center
        add(layeredPane, BorderLayout.CENTER);
    }

    @Override
    public void doLayout() {
        super.doLayout();

        // Make layeredPane fill the center area
        if (layeredPane != null) {
            // Calculate the bounds BorderLayout gave it
            // Since we called add(layeredPane, BorderLayout.CENTER), BorderLayout
            // already sized it during super.doLayout(), so just sync children.
            Dimension lpSize = layeredPane.getSize();

            // Puzzle panel fills the whole layeredPane
            if (puzzlePanel != null) {
                puzzlePanel.setBounds(0, 0, lpSize.width, lpSize.height);
            }

            // If overlay is visible, keep it 24px from bottom-left
            if (infoOverlay != null) {
                int cardW = infoOverlay.getWidth();
                int cardH = infoOverlay.getHeight();
                int x = 24;
                int y = lpSize.height - cardH - 24;
                if (y < 24) y = 24;
                infoOverlay.setLocation(x, y);
            }
        }
    }

    // Creates the overlay and preview components if they don't already exist
    private void initCompletedOverlays() {
        if (completedOverlay != null) return; // already initialized

        // Safely get the window's layered pane
        JRootPane root = SwingUtilities.getRootPane(FixThePotGamePanel.this);
        if (root == null) return; // guard in case panel isn't attached yet
        JLayeredPane layered = root.getLayeredPane();

        // Small, frameless floating overlay (toggled by the button)
        completedOverlay = new ImageOverlay();
        layered.add(completedOverlay, JLayeredPane.POPUP_LAYER);
        completedOverlay.setVisible(false); // size/position handled when shown

        // Small preview shown temporarily on hover
        completedPeek = new ImagePeek();
        layered.add(completedPeek, JLayeredPane.POPUP_LAYER);
        completedPeek.setVisible(false);

        // Keep overlay within bounds if the window resizes while it's visible
        layered.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                if (!completedOverlay.isVisible()) return;
                Dimension s = layered.getSize();
                Point p = completedOverlay.getLocation();
                Dimension d = completedOverlay.getSize();
                int nx = Math.max(0, Math.min(p.x, s.width - d.width));
                int ny = Math.max(0, Math.min(p.y, s.height - d.height));
                completedOverlay.setLocation(nx, ny);
            }
        });

        // Delay hiding the hover preview slightly after mouse exit
        peekHideTimer = new javax.swing.Timer(250, e -> completedPeek.setVisible(false));
        peekHideTimer.setRepeats(false);
    }

    private String prettyTrackName(String file) {
        String name = file.replace(".mp3", "");
        // trim common source tags
        name = name.replace("(chosic.com)", "")
                .replace("(freetouse.com)", "")
                .replace('_', ' ')
                .trim();
        return name;
    }


    // Builds the control panel containing all the buttons and controls
    private void buildControlPanel() {
        controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5)); // New panel with flow layout
        controlPanel.setOpaque(false); // Makes it transparent so that it blends into the background

        // Previous Jigsaw Button
        // Load and scale the Jigsaw icon
        ImageIcon leftIcon = new ImageIcon("/Users/taashfeen/Desktop/Jigsaw Game/src/Starting/left.png");
        Image origLeft = leftIcon.getImage();
        Image scaledLeft = origLeft.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        ImageIcon scaledLeftIcon = new ImageIcon(scaledLeft);
        previousJigsawButton = new JButton(scaledLeftIcon); // Create previous button with scaled image
        previousJigsawButton.setToolTipText("Previous Jigsaw");
        previousJigsawButton.addActionListener(e -> { // Move to the previous image in the combo box
            int itemCount = imageComboBox.getItemCount();
            if (itemCount <= 1) return; // No previous puzzle if there's only 0 or 1 items
            int currentIndex = imageComboBox.getSelectedIndex();
            int prevIndex = (currentIndex - 1 + itemCount) % itemCount;
            imageComboBox.setSelectedIndex(prevIndex);
            String selectedPath = (String) imageComboBox.getItemAt(prevIndex);
            puzzlePanel.setImage(selectedPath);
            hideInfoOverlay();
        });




        // Restart Button
        // Load and scale the Restart icon
        ImageIcon restartIcon = new ImageIcon("/Users/taashfeen/Desktop/Jigsaw Game/src/Starting/restart.png");
        Image origRestart = restartIcon.getImage();
        Image scaledRestart = origRestart.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        ImageIcon scaledRestartIcon = new ImageIcon(scaledRestart);
        restartButton = new JButton(scaledRestartIcon); // Create restart button with scaled image
        restartButton.setToolTipText("Restart Jigsaw");
        restartButton.addActionListener(e -> {
            puzzlePanel.restartGame(); // Restarts the game
            elapsedSeconds = 0; // Resetting the Timer
            timerButton.setText("Time: 0 s"); // Reset the Timer
            nextJigsawButton.setEnabled(false); // Part of resetting the game
        });




        // Show Completed Button
        ImageIcon show_completedIcon = new ImageIcon("/Users/taashfeen/Desktop/Jigsaw Game/src/Starting/show_completed.png");
        Image origShow_completed = show_completedIcon.getImage();
        Image scaledShow_completed = origShow_completed.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        ImageIcon scaledShow_completedIcon = new ImageIcon(scaledShow_completed);
        showCompletedButton = new JButton(scaledShow_completedIcon);
        showCompletedButton.setToolTipText("Show the completed image of the jigsaw");
        // Hover → show small preview
        showCompletedButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                initCompletedOverlays();
                BufferedImage img = puzzlePanel.getPotImage();
                if (img != null) {
                    completedPeek.setImage(img, 220, 160);

                    JRootPane root = SwingUtilities.getRootPane(FixThePotGamePanel.this);
                    if (root == null) return; // not attached to a window yet
                    JLayeredPane layered = root.getLayeredPane();

                    int pad = 12;
                    Dimension pref = completedPeek.getPreferredSize();
                    completedPeek.setBounds(
                            pad,
                            layered.getHeight() - pref.height - pad,
                            pref.width,
                            pref.height
                    );
                    completedPeek.setVisible(true);
                    if (peekHideTimer != null) peekHideTimer.stop();
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (peekHideTimer != null) peekHideTimer.restart();
            }
        });
        showCompletedButton.addActionListener(e -> { // Click → toggle small floating overlay
            initCompletedOverlays();
            BufferedImage completeImage = puzzlePanel.getPotImage();
            if (completeImage == null) {
                JOptionPane.showMessageDialog(FixThePotGamePanel.this, "Image not available");
                return;
            }

            boolean show = !completedOverlay.isVisible();
            if (show) {
                completedOverlay.setImage(completeImage);

                // Center the small overlay within the window
                JRootPane root = SwingUtilities.getRootPane(FixThePotGamePanel.this);
                if (root != null) {
                    JLayeredPane layered = root.getLayeredPane();
                    // ImageOverlay has centerIn(Dimension) in the frameless version
                    completedOverlay.centerIn(layered.getSize());
                }

                completedOverlay.setVisible(true);
                completedOverlay.requestFocusInWindow();
                completedPeek.setVisible(false);
            } else {
                completedOverlay.setVisible(false);
            }
        });

        // Jigsaw Split Button
        ImageIcon jigsawSplitIcon = new ImageIcon("/Users/taashfeen/Desktop/Jigsaw Game/src/Starting/jigsaw_split.jpeg");
        Image scaledJigsawSplit = jigsawSplitIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        jigsawSplitButton = new JButton(new ImageIcon(scaledJigsawSplit));
        jigsawSplitButton.setToolTipText("Pick the amount of Jigsaw Pieces");

        jigsawSplitButton.addActionListener(e -> {
            if (splitOverlay == null) {
                splitOverlay = new SplitChooserOverlay();

                // When "Custom..." is clicked, close the split overlay first, then open the rows/cols panel
                splitOverlay.setOnCustom(() -> {
                    // Close the split overlay before showing the custom dialog
                    splitOverlay.close();
                    if (glassPaneRef != null) glassPaneRef.repaint();

                    if (rowsColsOverlay == null) rowsColsOverlay = new RowsColsOverlay();

                    // Open the rows/cols overlay for custom difficulty
                    rowsColsOverlay.open(glassPaneRef, (rows, cols) -> {
                        puzzlePanel.setDifficulty(rows, cols);
                        rowsColsOverlay.close();
                        if (glassPaneRef != null) glassPaneRef.repaint();
                    });
                });
            }

            // Toggle: close if already open
            if (splitOverlay.isShowing()) {
                splitOverlay.close();
                if (glassPaneRef != null) glassPaneRef.repaint();
                return;
            }

            // Open the overlay picker (Easy / Medium / Hard handled here)
            splitOverlay.openBottom(glassPaneRef, (rows, cols) -> {
                puzzlePanel.setDifficulty(rows, cols);
                splitOverlay.close();
                if (glassPaneRef != null) glassPaneRef.repaint();
            });
        });


        // Information Button
        // Load and scale the Information icon
        ImageIcon informationIcon = new ImageIcon("/Users/taashfeen/Desktop/Jigsaw Game/src/Starting/information.png");
        Image origInformation = informationIcon.getImage();
        Image scaledInformation = origInformation.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        ImageIcon scaledInformationIcon = new ImageIcon(scaledInformation);

        extraInfoButton = new JButton(scaledInformationIcon);
        extraInfoButton.setToolTipText("Extra Information");

        extraInfoButton.addActionListener(e -> {
            // Fallback if overlay not ready
            if (glassPaneRef == null || persistentOverlay == null) {
                String selectedImage = (String) imageComboBox.getSelectedItem();
                if (selectedImage == null || !imageInfoMap.containsKey(selectedImage)) {
                    JOptionPane.showMessageDialog(FixThePotGamePanel.this, "No extra information available.");
                    return;
                }
                ImageInfo info = imageInfoMap.get(selectedImage);
                JOptionPane.showMessageDialog(
                        FixThePotGamePanel.this,
                        info.getDescription(),
                        "Extra Information",
                        JOptionPane.INFORMATION_MESSAGE,
                        new ImageIcon(selectedImage)
                );
                return;
            }

            // Toggle close
            if (persistentOverlay.isVisible()) {
                persistentOverlay.close();
                if (glassPaneRef != null) glassPaneRef.repaint();
                return;
            }

            // Open/refresh
            String selectedImage = (String) imageComboBox.getSelectedItem();
            if (selectedImage == null || !imageInfoMap.containsKey(selectedImage)) {
                JOptionPane.showMessageDialog(FixThePotGamePanel.this, "No extra information available.");
                return;
            }
            ImageInfo info = imageInfoMap.get(selectedImage);

            // IMPORTANT: pass the full-res image; let the banner scale it.
            ImageIcon previewIcon = new ImageIcon(selectedImage);

            String titleText = "Artifact: " + getDisplayName(selectedImage);
            String fullMuseumDescription = info.getDescription();

            persistentOverlay.updateContent(
                    previewIcon,
                    titleText,
                    fullMuseumDescription,
                    info.getUrl()
            );

            if (persistentOverlay.getParent() != glassPaneRef) {
                glassPaneRef.add(persistentOverlay);
                if (persistentOverlay.getWidth() == 0 || persistentOverlay.getHeight() == 0) {
                    persistentOverlay.setSize(480, 420);
                }
            }

            glassPaneRef.setVisible(true);
            persistentOverlay.setVisible(true);
            glassPaneRef.repaint();
        });

        // Image Combo Box Button
        imageComboBox = new JComboBox<>(imageOptions);
        imageComboBox.setToolTipText("Pick a Jigsaw"); // Hover over information
        imageComboBox.addActionListener(e -> { // When a new image is selected, update the puzzle panel
            String selectedImage = (String) imageComboBox.getSelectedItem();
            if (selectedImage != null) {
                puzzlePanel.setImage(selectedImage);
                nextJigsawButton.setEnabled(false);
            }
        });


        // Timer Button Code
        timerButton = new JButton("Time: 0 s");
        timerButton.setPreferredSize(new Dimension(100, 36));
        timerButton.setToolTipText("Start/Stop"); // Hover over information
        timerButton.setFocusPainted(false);
        timerButton.addActionListener(e -> { // start or stop based on its current state
            if (timerRunning) {
                timer.stop();
                timerButton.setText("Paused: " + elapsedSeconds + " s");
                timerRunning = false;
            } else {
                timer.start();
                timerButton.setText("Time: " + elapsedSeconds + " s");
                timerRunning = true;
            }
        });


        // ========== Music UI (overlay popover; non-modal) ==========

        // 1) Track list (same as before)
        String[] musicTracks = {
                "Lukrembo - Bread (freetouse.com).mp3",
                "John-Bartmann-Another-Grappa-Monsieur_(chosic.com).mp3",
                "scott-buckley-permafrost(chosic.com).mp3",
                "John-Bartmann-Allez-Allez(chosic.com).mp3"
        };

        // Start the first track
        currentTrackname = musicTracks[0];
        musicPlayer = new MusicPlayer(musicFolderPath + currentTrackname);
        musicPlayer.play();

        // 2) Mute / Unmute button (unchanged look)
        ImageIcon musicIcon = new ImageIcon("/Users/taashfeen/Desktop/Jigsaw Game/src/Starting/music note.png");
        Image scaledMusic = musicIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        musicToggleButton = new JButton(new ImageIcon(scaledMusic));
        musicToggleButton.setToolTipText("Mute/Unmute");
        musicToggleButton.addActionListener(e -> {
            if (musicPlayer != null) musicPlayer.togglePlayPause();
        });

        // 3) Track chooser button (the “up arrow”)
        ImageIcon trackIcon = new ImageIcon("/Users/taashfeen/Desktop/Jigsaw Game/src/Starting/up.png");
        Image scaledTrack = trackIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        JButton chooseTrackButton = new JButton(new ImageIcon(scaledTrack));
        chooseTrackButton.setToolTipText("Select a music track");

        // Popover field (store on your panel class if you want to reuse)
        final MusicChooserPopover[] musicPopoverRef = new MusicChooserPopover[1];

        chooseTrackButton.addActionListener(e -> {
            if (glassPaneRef == null) return;

            // Toggle behavior
            if (musicPopoverRef[0] != null && musicPopoverRef[0].isShowing()) {
                musicPopoverRef[0].close();
                glassPaneRef.repaint();
                return;
            }

            // (Re)create when needed
            musicPopoverRef[0] = new MusicChooserPopover();

            // Open just above the arrow button
            musicPopoverRef[0].openAbove(glassPaneRef, chooseTrackButton, musicTracks, pickedFile -> {
                // swap track
                if (musicPlayer != null) musicPlayer.stopPlayback();
                currentTrackname = pickedFile;
                musicPlayer = new MusicPlayer(musicFolderPath + pickedFile);
                musicPlayer.play();

                musicPopoverRef[0].close();
                glassPaneRef.repaint();
            });
        });


        // Collection's Button
        // Load and scale the Collections icon
        ImageIcon collectionsIcon = new ImageIcon("/Users/taashfeen/Desktop/Jigsaw Game/src/Starting/collections.png");
        Image origCollections = collectionsIcon.getImage();
        Image scaledCollections = origCollections.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        ImageIcon scaledCollectionsIcon = new ImageIcon(scaledCollections);
        backToCollectionsButton = new JButton(scaledCollectionsIcon); // Menu Icon button
        backToCollectionsButton.setToolTipText("Return to collection selection"); // Hover over information
        backToCollectionsButton.addActionListener(e -> { // Notify the listener to switch back to the collection screen
            if (gamePanelListener != null) {
                gamePanelListener.onBackToCollections();
            }
        });


        // JIGSAW CHOOSER (popover)
        ImageIcon chooseIcon = new ImageIcon("/Users/taashfeen/Desktop/Jigsaw Game/src/Starting/choose.png");
        Image scaledChoose = chooseIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        chooseJigsawButton = new JButton(new ImageIcon(scaledChoose));
        chooseJigsawButton.setToolTipText("Click to select a jigsaw");

        final JigsawChooserPopover[] jigsawPopoverRef = new JigsawChooserPopover[1];

        chooseJigsawButton.addActionListener(e -> {
            if (glassPaneRef == null) return;

            if (jigsawPopoverRef[0] != null && jigsawPopoverRef[0].isShowing()) {
                jigsawPopoverRef[0].close();
                glassPaneRef.repaint();
                return;
            }

            int n = imageComboBox.getItemCount();
            java.util.List<String> paths = new java.util.ArrayList<>();
            java.util.List<String> display = new java.util.ArrayList<>();
            for (int i = 0; i < n; i++) {
                String path = (String) imageComboBox.getItemAt(i);
                paths.add(path);
                display.add(getDisplayName(path));
            }

            jigsawPopoverRef[0] = new JigsawChooserPopover();

            jigsawPopoverRef[0].openAbove(
                    glassPaneRef,
                    chooseJigsawButton,
                    display,
                    pickedIndex -> {
                        if (pickedIndex < 0 || pickedIndex >= paths.size()) return;
                        imageComboBox.setSelectedIndex(pickedIndex);
                        puzzlePanel.setImage(paths.get(pickedIndex));
                        hideInfoOverlay();
                        jigsawPopoverRef[0].close();
                        glassPaneRef.repaint();
                    }
            );
        });

        // Next Jigsaw Button
        // Load and scale the Jigsaw icon
        ImageIcon nextIcon = new ImageIcon("/Users/taashfeen/Desktop/Jigsaw Game/src/Starting/right.png");
        Image origNext = nextIcon.getImage();
        Image scaledNext = origNext.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        ImageIcon scaledNextIcon = new ImageIcon(scaledNext);
        // Create button with scaled image
        nextJigsawButton = new JButton(scaledNextIcon);
        nextJigsawButton.setToolTipText("Next Jigsaw");
        nextJigsawButton.setEnabled(false); // Disable button until the puzzle is solved
        nextJigsawButton.addActionListener(e -> { // Move to the next image in the combo box
            int itemCount = imageComboBox.getItemCount();
            if (itemCount <= 1) return; // No next puzzle if there's only 0 or 1 items
            int currentIndex = imageComboBox.getSelectedIndex();
            int nextIndex = (currentIndex + 1) % itemCount;
            imageComboBox.setSelectedIndex(nextIndex);
            String selectedPath = (String) imageComboBox.getItemAt(nextIndex);
            puzzlePanel.setImage(selectedPath);
            nextJigsawButton.setEnabled(false); // Disable button until the next puzzle is solved
            hideInfoOverlay();
        });

        // Zen mode button
        // Load and scale the Jigsaw icon
        ImageIcon zenIcon = new ImageIcon("/Users/taashfeen/Desktop/Jigsaw Game/src/Starting/zen.png");
        Image origZen = zenIcon.getImage();
        Image scaledZen = origZen.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        ImageIcon scaledZenIcon = new ImageIcon(scaledZen);
        zenModeButton = new JButton(scaledZenIcon);
        zenModeButton.setToolTipText("Toggle Zen mode (distraction-free)");
        zenModeButton.setFocusPainted(false);
        zenModeButton.setBackground(new Color(28, 28, 28));
        zenModeButton.setForeground(Color.WHITE);
        zenModeButton.addActionListener(e -> toggleZenMode());


        // Add all controls to the panel.
        controlPanel.add(previousJigsawButton);
        controlPanel.add(backToCollectionsButton);
        controlPanel.add(musicToggleButton);
        controlPanel.add(chooseTrackButton);
        controlPanel.add(restartButton);
        controlPanel.add(zenModeButton); // or topButtonsPanel.add(zenModeButton);
        controlPanel.add(timerButton);
        controlPanel.add(extraInfoButton);
        controlPanel.add(showCompletedButton);
        controlPanel.add(jigsawSplitButton);
        controlPanel.add(chooseJigsawButton);
        controlPanel.add(nextJigsawButton);
        add(controlPanel, BorderLayout.SOUTH); // Add control panel to the bottom of the screen

        // After all buttons are created (just before adding to controlPanel or right after)
        zenHideComponents = new JComponent[] {
                backToCollectionsButton,
                musicToggleButton,
                chooseTrackButton,
                restartButton,
                timerButton,
                extraInfoButton,
                showCompletedButton,
                jigsawSplitButton,
                chooseJigsawButton,
                // you can add zenModeButton here too if you want it to vanish as well
        };
    }


    // Zen mode toggler
    private void toggleZenMode() {
        zenMode = !zenMode;
        if (zenHideComponents != null) {
            for (JComponent c : zenHideComponents) {
                if (c != null) {
                    c.setVisible(!zenMode);
                }
            }
        }
        if (zenMode) {
            if (persistentOverlay != null) persistentOverlay.close();
            if (completedOverlay != null) completedOverlay.close();
            if (completedPeek != null) completedPeek.setVisible(false);
        }
        if (puzzlePanel != null) {
            if (zenMode) {
                puzzlePanel.setOpaque(true);
                puzzlePanel.setBackground(new Color(28, 28, 28));
            } else {
                puzzlePanel.setOpaque(false);
            }
        }
        // Apply Zen background to the bottom control panel too
        if (controlPanel != null) {
            if (zenMode) {
                controlPanel.setOpaque(true);
                controlPanel.setBackground(new Color(28, 28, 28)); // warm grey
            } else {
                controlPanel.setOpaque(false); // go back to transparent
            }
        }
        if (zenModeButton != null) {
            zenModeButton.setToolTipText(zenMode
                    ? "Exit Zen mode and show controls"
                    : "Toggle Zen mode (distraction-free)");
        }
        revalidate();
        repaint();
    }



    private void setupTimer() {
        elapsedSeconds = 0; // Set to 0
        timerRunning = true; // Marked as running
        // Timer triggers every 1000ms (1s), adds to the counter each time
        timer = new Timer(1000, e -> {
            elapsedSeconds++;
            timerButton.setText("Time: " + elapsedSeconds + " s");
        });
        timer.start(); // starts the timer
    }

    /**
     * Information for the Extra Info Button.
     * The information is extracted from the URL's, which are listed for each image.
     * The information is displayed as Year, Culture, and Description. If any other categories can be made, they have been added.
     */
    private void initializeExtraInfoMap() {
        imageInfoMap = new HashMap<>();

        // Ancient Cyprus Collection 15/15
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/jug-1.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Year: 2250 BC ~ 2150 BC<br>" +
                "Culture: Early Cypriot<br>" +
                "A large jar with a spherical body, a thick neck and one handle from the shoulder to the neck. " +
                "The jar has a conical base and is decorated in Red Polished I type of Red Polished slip all over its body.",
                "https://www.liverpoolmuseums.org.uk/artifact/jug-1"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/jug-2.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Year: 2075 BC ~ 2000 BC<br>" +
                "Culture: Early Cypriot<br>" +
                "A large handmade jug in Red Polished decoration. The jug has a round base, a spherical body and a long cut-away beak spout. " +
                "There is only one handle starting from the top body and finishing in the middle of the cut-away neck." +
                "The handle has a zig zag incised decoration on it and there are two round protruding circles on each side of the neck by the top handle.",
                "https://www.liverpoolmuseums.org.uk/artifact/jug-2"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/jug-3.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Year: 2075 BC ~ 2000 BC<br>" +
                "Culture: Early Cypriot<br>" +
                "A big Red Polished jug of a round and swollen body. " +
                "The jug has a long concave neck with an everted circular rim and a handle from the mid neck to the shoulder. " +
                "The jug has various cracks but it is generally in a good condition. " +
                "The ware has a rich brown surface, the clay is well mixed with a grit straw and fired buff, the core on the body has grey tones.",
                "https://www.liverpoolmuseums.org.uk/artifact/jug-3"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/jug-4.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Year: 2075 BC ~ 2000 BC<br>" +
                "Culture: Early Cypriot<br>" +
                "A medium size jug with an ovoid body and a small flat base, a short neck with a cut away spout and a handle from the spout to the shoulder." +
                "On either side of the handle there is a knob on the neck." +
                "The excavation reports that the jug has potter's mark on the handle but this is not easily discerned as much of the red polished " +
                "slip has fallen off.",
                "https://www.liverpoolmuseums.org.uk/artifact/jug-4"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/jug-5.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Year: 850 BC ~ 750 BC<br>" +
                "Culture: Cypro-Geometric<br>" +
                "A jug with a short neck, one vertical handle from the spout to the shoulder of the jug and a ring base."  +
                "The jug has Black Slip III bucchero type decoration with vertical parallel lines running along the body." +
                "The decoration is flaky and has lost its shine. Half of the flat spout is missing.",
                "https://www.liverpoolmuseums.org.uk/artifact/jug-5"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/bowl-1.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Year: 2150 BC ~ 2100 BC<br>" +
                "Culture: Early Cypriot<br>" +
                "A deep hemispherical bowl with a flat base and two small horizontal ledge lugs. The bowl was hand made. " +
                "The decoration on the body is in Red Polished II type with a rich red surface and purplish stains on the exterior, giving it a mottle effect. " +
                "This type of mottle effect decoration is more common in pottery from the southern sites of Psematismenos rather the northern coastal site of Vounous. " +
                "It is likely the bowl was an import from the south. A few similar pottery imports have been found in the Vounous cemeteries. " +
                "One of the lugs has two holes while the other lug is almost unformed.",
                "https://www.liverpoolmuseums.org.uk/artifact/bowl-1"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/bowl-2.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Year: 2250 BC ~ 2150 BC<br>" +
                "Culture: Early Cypriot<br>" +
                "A tulip shaped bowl with a slightly concave nipple base and vertical concave lugs that are horizontally pierced. " +
                "The bowl has the typical Black topped on Red Polished decoration of the Early Cypriot Bronze Age. " +
                "The bowl would have been made by hand and the decoration would have been the result of planning in the firing process. " +
                "One of the lugs has two holes on each of its side and the appearance of a bull. " +
                "The external upper body of the bowl is decorated with two zones of deeply incised and white filled zig zags and simple line " +
                "patterns at the lower part of each side. " +
                "The black area of the rim also has two thin lines of zig zags running along it. The interior of the bowl is black. " +
                "One of the external sides of the bowl has considerably lost all its red slip. " +
                "Tulip shaped bowls would have been used in funerary ceremonies, and they would have significant ritual significance. " +
                "Their sophisticated designs perhaps indicate for the need of the regional communities of the North to re-enforce and assert " +
                "their regional identity through material culture.",
                "https://www.liverpoolmuseums.org.uk/artifact/bowl-2"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/bowl-3.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Year: 2250 BC ~ 2150 BC<br>" +
                "Culture: Early Cypriot<br>" +
                "A bowl of Black topped Red Polished ware decoration, typical of the Early Cypriot Bronze Age handmade pottery. " +
                "The decoration was the result of planning in the firing process. The bowl has on one side of its body, a handle in the shape of a horn. " +
                "There are two small holes on each of the lower sides of the spout. The middle to low body of the bowl has incised decoration of a band of vertical " +
                "parallel lines. " +
                "In the upper part of the bowl there is an incised line with interrupted lines on each of its sides. " +
                "The black upper part of the bowl is decorated with four semi circles made of incised lines, alternated by a pattern of parallel " +
                "incised horizontal lines. " +
                "The interior of the bowl is black.",
                "https://www.liverpoolmuseums.org.uk/artifact/bowl-3"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/bowl-4.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Year: 2250 BC ~ 2150 BC<br>" +
                "Culture: Early Cypriot<br>" +
                "A large handmade bowl of Red polished decoration with a long spout on one side and a small handle on the other. " +
                "The interior of the bowl is in black. The core of the body is in red clay that is well mixed with straw and grit. " +
                "The colour of the slip at the lower part of the bowl ranges from red to dark brown/red especially in the area close to the spout.",
                "https://www.liverpoolmuseums.org.uk/artifact/bowl-4"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/bowl-5.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Year: 2150 BC ~ 2100 BC<br>" +
                "Culture: Early Cypriot<br>" +
                "A small bowl of Red Polished decoration shading from dark red to brown to black at the top and the interior of the bowl. " +
                "There is a small lug with a two-side hole on one side of the bowl. " +
                "The interior of the bowl is black and there are a few cracks on its body and a small part missing.",
                "https://www.liverpoolmuseums.org.uk/artifact/bowl-5"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/Pyxis-Lid.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Year: 1300 BC<br>" +
                "Culture: Late Cypriot IIB<br>" +
                "Ivory disc or lid from a pyxis with engraved decoration. and a small central hole for the handle, with remains of the peg in the centre. " +
                "The central medallion decoration is of a lozenge formed out of 4 pairs of interlocking volutes or semi spirals, " +
                "resembling the decoration in the gold bezel rings found in Tomb Evreti 8. The central decoration is enclosed within a border " +
                "formed by an outer group of three, " +
                "and an inner of four bands with curved dogtooth in between, forming the tips of the petals of a great flower. " +
                "The disc is probably the lid of a circular box and many of its fragments are missing. " +
                "The disc is in several pieces which have been joined together and fixed to a Perspex mount. " +
                "One piece has come loose of the mount. Large parts of the opposite sides are missing.",
                "https://www.liverpoolmuseums.org.uk/artifact/pyxis-box-lid"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/Spindle-Whorl.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Year: 1300 BC<br>" +
                "Culture: Late Cypriot IIB<br>" +
                "Part of a thick round ivory spindle whorl. It has a hole in the middle and a convex body. " +
                "The piece has engraved decoration of three incised bands around the edge and the central hole. " +
                "In between these two bands there is band of long narrow petals. Half of the circumference is broken. The underside is plain.",
                "https://www.liverpoolmuseums.org.uk/artifact/spindle-whorl-11"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/Human-Remains-1.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Year: 1250 BC - 1190 BC<br>" +
                "Culture: Cypriot<br>" +
                "Various human and animal bones, possibly a goat or a sheep, including a distal metapodial and a shaft. " +
                "From the excavations of Kouklia: Evreti Tomb 3A.",
                "https://www.liverpoolmuseums.org.uk/artifact/human-and-animal-remains-skeletal"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/Temple-Boy.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Year: 400 BC – 300 BC<br>" +
                "Culture: Cypriot<br>" +
                "Body of a boy statuette of the type known as the temple boy. The head is missing. " +
                "The boy is seated on a slanted plinth with the left leg crossed at the front and he has his left arm resting to the side. " +
                "Only part of his right leg which would have been bent and at a right angle to the side has survived. " +
                "The boy's right hand is holding or stroking a bird. " +
                "The boy wears a long and heavily pleated chiton that covers his legs and bracelets on both his wrists. " +
                "Most of the details of the body and the clothing have faded.",
                "https://www.liverpoolmuseums.org.uk/artifact/statuette-of-temple-boy-6"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/VotiveStatueHead.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Year: 600 BC – 500 BC<br>" +
                "Culture:  Cypriot<br>" +
                "A small male head from a votive statue. The face of the male is slightly turned to the right and facing upwards with a faded smile. " +
                "He wears a wreath of leaves and has a thick line of possibly braided hair or arranged in a roll on the forehead. " +
                "The male wears a fillet at the crown of the head. The hair is long at the back of the neck only part of which is surviving. " +
                "The head was split into two with a heavy crack running across the forehead and it has been glued together. Part of the nose is missing. " +
                "The eyes are long and flat and very worn, and it is doubtful whether they had lids.",
                "https://www.liverpoolmuseums.org.uk/artifact/head-votive-statue-28"
        ));

        // Ancient Greece Collection 15/15
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Amour Helmet.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 550 BC – 450 BC<br>" +
                "Culture: Corinthian<br>" +
                "This helmet is of Corinthian type with a flat base line, " +
                "a wide nose and a carination above the forehead that it pointed sharply upwards and is linked with a vertical carination " +
                "to the eyebrows represented in repousse. " +
                "The eyebrows and the band above the carination round the helmet's crown are bordered by thin relief lines. " +
                "The helmet has no nail holes or inscriptions or signs of bending in the cheek guards that would indicate it is from a sanctuary. ",
                "https://www.liverpoolmuseums.org.uk/artifact/armour-helmet"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Wine Flask.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 360 – 330 BC<br>" +
                "Culture: Campanian, Ancient Greek<br>" +
                "Trefoil-mouthed oinochoe, made in Campania, decorated in the red-figure technique. The neck has a laurel wreath, " +
                "and there are large palmettes and a wave pattern underneath the main scene. Two young men ride galloping horses and seem to be " +
                "conversing with each other, " +
                "the young male ahead turning his head towards the man who is following him and who has his left arm raised. " +
                "Details in the horse's neck are in white dots with a yellowish colour on top of it, " +
                "while the youth's head crowns are a detail in one of the males’ waists is also in the white and yellow. " +
                "The oinochoe is broken has a repaired rim and minor abrasions.",
                "https://www.liverpoolmuseums.org.uk/artifact/wine-flask"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Wine Flask 2.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: about 730 BC<br>" +
                "Culture: Attic<br>" +
                "A large Athenian Late Geometric IIA oinochoe with trefoil lip, a conical shaped body with only a thin base, " +
                "a long and thin neck and a long vertical handle extending slightly above the trefoil mouth from the shoulder of the jug. " +
                "The handle is made of the long strip, the central one attached on top of the other two and it is joined with the upper neck " +
                "with a short horizontal strip of clay. " +
                "The decoration is in zones of geometric designs covering the whole body and including chequerboard, swastikas and Maeander. " +
                "In one area of the band just below the neck there is a scene of two seated long figures represented in a schematic way. " +
                "In between them lies a thick rectangular construction, an altar or a tomb. The figures have their long arms raised and are " +
                "holding clappers or rattles. " +
                "To the sides of the scene there are two raised clay roundels. The scene could be a cult or funerary scene. " +
                "Ahlberg (1967) interprets the objects as 'pomegranate-vases', with one point turned up, the other down.",
                "https://www.liverpoolmuseums.org.uk/artifact/wine-flask-1"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Amphora.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 4th Century BC<br>" +
                "Culture: Campanian, Ancient Greek<br>" +
                "Campanian neck amphora decorated in the red-figure technique. Large palmettes under handle and on neck. " +
                "Side 1: Satyr with thyrsus and tambourine. Side 2: draped and diademed female.",
                "https://www.liverpoolmuseums.org.uk/artifact/amphora-15"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Drinking Vessel.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: Late 5th Century BC – Early 4th Century BC<br>" +
                "Culture: Apulian<br>" +
                "South Italian (Apulian) deep cup of the type known as 'skyphos', decorated in the red-figure technique. " +
                "Details of the woman's jewellery are in yellow while other details of the man's head garland and vessel are in thick white. " +
                "The decoration on one side is of a nude male with a garland on his head, moving to his right. " +
                "He has a himation or other drapery over his left arm and holds an offering dish. The object in his right hand may be a mirror. " +
                "Close to his head is one side an ivy shaped leaf and a thick vine/grape brunch to his left directly above the offering dish. " +
                "The other side has a draped female figure moving to her left towards an altar. " +
                "She holds a wreath on her left hand while the right arm is raised and holds perhaps a large tambourine. " +
                "The woman' s head is turned towards the tambourine rather than the altar. " +
                "There is a vertical brunch or stem by the right side of the woman close to her feet. " +
                "While in the upper left area there is an elaborate ribbon. By the rim there is a thin frieze of joined half egg decoration.",
                "https://www.liverpoolmuseums.org.uk/artifact/drinking-vessel-12"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Krater.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 4th Century BC<br>" +
                "Culture: Apulian<br>" +
                "South Italian bell krater, Early Apulian red-figure style. Palmettes under handles. " +
                "Side A: Dionysus seated on throne in form of Ionic column capital, maenad with torch and thyrsus. " +
                "Side B: two draped youths, one (left) with strigil, the other (right) with stick. " +
                "In Greek mythology, maenads were the female followers of the god Dionysus.",
                "https://www.liverpoolmuseums.org.uk/artifact/krater-2"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Kylix.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: about 530 BC<br>" +
                "Culture: Attic<br>" +
                "Kylix (drinking cup), decoration in the black figure technique in the style described as 'Little Master'. " +
                "The frieze depicts lions attacking a deer, a motif which appears on both sides of the vessel. " +
                "In the centre of the interior (tondo) is a Gorgon mask, rendered in the red-figure technique. " +
                "The use of a Gorgon mask was a common feature on the inside of drinking vessels, probably intended to ward off the evil eye.",
                "https://www.liverpoolmuseums.org.uk/artifact/kylix"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Aryballos.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: about 550 BC<br>" +
                "Culture: Corinthian<br>" +
                "A globular aryballos with a thick and wide disk-shaped top, a small handle from the shoulder to the top of the mouth. " +
                "The overall ground is in creamy yellow slip and had distinct decoration of a dark brown quatrefoil decoration. " +
                "Only one side of the body is decorated. The flat disc shaped top has decoration of a flower with its petals all along the small round hole. " +
                "Small brown dots decorate the thick horizontal band of the disc shaped top. " +
                "The handle also has inverted arrows decoration in brown but has significantly faded.",
                "https://www.liverpoolmuseums.org.uk/artifact/aryballos-15"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Aryballos 2.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 665 BC – 620 BC<br>" +
                "Culture: Greek<br>" +
                "Miniature aryballos (oil flask), with an ovoid body, a small ring base, short neck, a disk-shaped top and a small handle from the shoulder to the top. " +
                "The aryballos is decorated in red brown circular bands along the shoulder and the low part of the body. " +
                "In the middle of the body the background is orange red and there are circular bands in thin brown as well " +
                "as vertical lines with a slight tilt which have been scratched onto the surface of the vessel. " +
                "The area underneath the neck has a yellowish creamy band with rhomboid shaped leaves all along it. " +
                "The flat shaped top has a thick brown circular band outlined by thinner red circular bands.",
                "https://www.liverpoolmuseums.org.uk/artifact/aryballos-17"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Aryballos 3.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 626 BC – 600 BC<br>" +
                "Culture: Corinthian<br>" +
                "Small globular aryballos (perfumed oil container), short neck and flat disc shaped rim, a very small handle from the shoulder to the rim. " +
                "The decoration is of a quatrefoil all over the main body. " +
                "This was an Assyrian design typical of the Early Corinthian phase and the small size of this globular aryballos " +
                "indicate that it may be from the Early Corinthian phase.",
                "https://www.liverpoolmuseums.org.uk/artifact/aryballos-25"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Rhyton.jpg", new ImageInfo("<html>" +
                "<div style='width:400px;'>" +
                "Date: 4th Century BC<br>" +
                "Culture: Greek<br>" +
                "A ceremonial vessel known as rhyton in the form of a horse's head. The head was made in a mould, the handle would have been attached after. " +
                "The representation of the horse is very realistic: he has his mouth open, and the tongue is protruding, the teeth can also be seen, " +
                "his nostrils are wide and the eyes wide in a black slip and outlined in white paint. " +
                "Two of the strong facial muscles beneath the eyes and before the nostrils add to the realism of the representation. " +
                "The ears are long hollow cavities at the top of the head. The reins of the horse are also painted in black slip and have white painted dots all along them. " +
                "The overall surface of the head's horse is in pink clay, " +
                "while the upper part of the vessel which would been used for pouring in the wine is cylindrical with the edges opening at the top. " +
                "In between the mould made head of the horse and the cylindrical top of the vessel there is a decorative band of palmettes with a central anthemion (flower) to the front of the band. " +
                "The cylindrical part of the vessel has a decorative band of red figures on a black background." +
                " The figures are all men from the left hand side area a warrior holding a shield and a spear, " +
                "in front of him a man in a local cloak facing to his right and in a dramatic pose with the left leg bent and the right forward, " +
                "right hand raised and pointing to the right of the decorative band. " +
                "In between these first two men there is a decorative cross in red in the background. " +
                "The centre scene of the band in between the ears has a man standing to the left wearing a chlamys and the flat hat petasos on his " +
                "head and with his left hand leaning on a long stick. He may represent Hermes. To his right is a man with a beard and long hair" +
                ", he wears a long cloak and an himation, his left leg is bent and the right leg at the back, his back seems to be bent as he is moving forward," +
                " perhaps dancing, his left hand is raised in front of his head and he is holding a long object or pouring vessel, perhaps he represents Dionysius. " +
                "The right side of the band shows two more standing figures both dressed in long cloaks, " +
                "the first one holding an offering object and the one behind him with bent back and holding a spear in his right hand, " +
                "the left hand also holding an offering. Other details of the horse such as the top edges of the ears and the front groove " +
                "of the face in between the eyes as well as the muzzle also have thick applied white paint. There are several cracks at the sides of the mouth.",
                "https://www.liverpoolmuseums.org.uk/artifact/rhyton-0"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Horse votive offering.jpg", new ImageInfo( "<html>" +
                "<div style='width:300px;'>" +
                "Date: 6th Century BC<br>" +
                "Culture: Laconian<br>" +
                "A horse votive offering from the Sanctuary of Artemis Orthia. The horse is in profile to its right and has all its legs complete.",
                "https://www.liverpoolmuseums.org.uk/artifact/horse-votive-offering"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Fragments of deer figurine.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 6th Century BC to mid 4th Century BC<br>" +
                "Culture: Laconian<br>" +
                "Fragments of the head, body, antlers and one hind leg of a deer, all part of a votive offering from the Sanctuary of Artemis Orthia in Sparta " +
                "(or possibly another sanctuary site in Lakonia). The deer faces to its right. Deer are associated with the hunting goddess Artemis.",
                "https://www.liverpoolmuseums.org.uk/artifact/fragments-of-deer-figurine"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Wreath Shaped votive offering.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 6th Century BC to mid 4th Century BC<br>" +
                "Culture: Laconian<br>" +
                "Votive offering of a wreath type from the Sanctuary of Artemis Orthia in Sparta (or possibly another sanctuary site in Lakonia).",
                "https://www.liverpoolmuseums.org.uk/artifact/wreath-shaped-votive-offering"));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Loutrophoros.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: Late 4th Century BC<br>" +
                "Culture: Apulian<br>" +
                "Late Apulian loutrophoros, decorated in the red figure technique. Attributed to the Helmet Painter. " +
                "In one side there is a seated woman with a phiale and a standing maid with the cista (casket). " +
                "Both women are under a naiskos (shrine). " +
                "To the left of the main scene there is a seated woman holding a wreath in the upper zone and underneath her a woman bending forward and holding a " +
                "flower. To the right of the main scene there are also two women, one in the upper part of the pot is seated and her head is turned towards the main " +
                "scene; underneath her there is another woman with one leg bent close to the naiskos and her left arm, appearing to be leaning on the knee," +
                " the fingers of the hand pointing or perhaps holding something that has been lost from the decoration. " +
                "All women apart from the ones in the main scene are in a bigger scale to the main scene and are dressed in heavy cloaks. " +
                "The details of the drapery of the cloaks are in thick black lines suggesting volume and movement to the drapery. " +
                "The reverse side of the loutrophoros has decoration of: palmettes. On the shoulder of the pot there is a female head in profile, " +
                "much of her face has been lost only the outline of the head survives. The head is turned to the left and in an overall floral setting. " +
                "This type of vessel was associated with wedding rituals.",
                "https://www.liverpoolmuseums.org.uk/artifact/loutrophoros"
        ));

        // Ancient Egypt 15/15
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Shabti of Djedkhonsuefankh.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 1070 – 945 BC<br>" +
                "Culture: Ancient Egypt, Third Intermediate Period<br>" +
                "Description: Shabtis were small magical figures deposited in a tomb to answer for the deceased should he or she be called upon to do labour " +
                "in the afterlife." +
                " In the New Kingdom, shabtis could be placed in a variety of spaces to extend their owners' presence over space and time. " +
                "This shabti belongs to Djedkhonsuefankh, who held priestly and administrative titles in the domain of Amun at Thebes. " +
                "It was found in a group of more than fifty-nine shabtis, some bearing Djedkhonsuefankh's name, others - his name and titles, " +
                "and a few with the common shabti formula.",
                "https://www.metmuseum.org/art/collection/search/561086"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Figurine of a Pygmy Dance Leader.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 1950 – 1885 BC<br>" +
                "Culture: Ancient Egypt, Middle Kingdom<br>" +
                "Description: Pygmies, small in stature but naturally proportioned, lived in Central Africa, " +
                "and ancient Egyptians contacted them through intermediaries on the Upper Nile. " +
                "Egyptians believed the pygmies possessed divine qualities and called their performances \"dances of the gods.\" " +
                "A young Egyptian woman named Hepy was given an automaton consisting of a board with pygmy figures that could be turned by pulling strings" +
                " fastened around their bases. This piece may represent their leader.",
                "https://www.metmuseum.org/art/collection/search/546440"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Schist Statuette Fragment.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 1550 - 1069 BC<br>" +
                "Culture: Ancient Egypt, New Kingdom<br>" +
                "Description: A fragment from a schist statuette, the head and crown of which is all that remains. " +
                "The workmanship is very fine and the detail excellent. " +
                "The figure wears a nemes headdress surmounted by a complicated atef-style crown of ram horns, uraei, feathers and a sun-disc. " +
                "A similar crown was often associated with the god Khnum. There is an uninscribed back pillar. Presented to the museum by the Bavarian Consul," +
                " Charles Stoess, in 1869.",
                "https://www.liverpoolmuseums.org.uk/artifact/figure-3"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Offering Table.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 200 BC<br>" +
                "Culture: Meroitic Period<br>" +
                "Description: Six fragments of an offering table made from green-glazed faience. " +
                "The pattern on the surface is in relief and includes curling lotus flowers. The six pieces fit together to form one corner.",
                "https://www.liverpoolmuseums.org.uk/artifact/offering-table"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Amulet of Jackal Headed Deity.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 664 – 525 BC<br>" +
                "Culture: Ancient Egypt, Late Period<br>" +
                "Description: Composite deity with a human body, jackal's head, wings and tail of a bird, clad in a short kilt and seated on a throne," +
                " drawing a bow held in the left hand. A possible sun disc headdress is broken away but there remains trace of plumes and curled horns." +
                " A loop at the back would have allowed the amulet to be threaded and worn.",
                "https://www.liverpoolmuseums.org.uk/artifact/amulet-of-jackal-headed-deity"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Composite Papyrus Capital.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 380 – 343 BC<br>" +
                "Culture: Ancient Egypt, Late Period<br>" +
                "Description: This capital was set atop one of the ten columns of a kiosk built in front of the temple of Amun at Hibis in Kharga Oasis. " +
                "It is an early example of a composite capital, which included several kinds of plants combined into a design that, with time, " +
                "became increasingly more elaborate and fanciful. Here, the composition is still rather simple, consisting of two Cyperus species: " +
                "eight plants of the common papyrus (Cyperus papyrus, above) alternating with eight foxtail flat sedge plants (Cyperus alopecuroides, below)." +
                " The capital still shows remnants of its original paint.",
                "https://www.metmuseum.org/art/collection/search/551898"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Chariots with Court Ladies.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 1353 – 1336 BC<br>" +
                "Culture: Ancient Egypt, New Kingdom, Amarna Period<br>" +
                "Description:  Court ladies taking part in a royal procession stand in light chariots, " +
                "as their drivers race to keep up with the royal family in their chariots.",
                "https://www.metmuseum.org/art/collection/search/544889"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Wedjat Eye Amulet.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 1070 – 664 BC<br>" +
                "Culture: Ancient Egypt<br>" +
                "Description: Wedjat eye amulets were among the most popular amulets of ancient Egypt. " +
                "The Wedjat eye represents the healed eye of the god Horus and embodies healing power as well as regeneration and protection in general. " +
                "This eye here is an intriguing combination of the regular Wedjat eye with a wing, two uraei, and a lion. " +
                "This combination alludes to various ancient Egyptian stories that involve the eye of the sun god Re.",
                "https://www.metmuseum.org/art/collection/search/561047"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Artist's Sketch of a Sparrow.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 1479 – 1458 BC<br>" +
                "Culture: Ancient Egypt, New Kingdom<br>" +
                "Description: A sketch of a Sparrow on a rock.",
                "https://www.metmuseum.org/art/collection/search/548890"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Ring, signet.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 1353 -1323 BC<br>" +
                "Culture: Ancient Egypt, New Kingdom, Amarna Period<br>" +
                "Description: The horizontal cartouche on this ring encloses the insignia of Tutankhamun's successor, \"The God's Father, Aye, Ruler of Thebes.\"" +
                " Aye was a military officer who became the king's advisor, or \"god's father,\" and this title was incorporated into his nomenclature when he" +
                " ascended the throne.",
                "https://www.metmuseum.org/art/collection/search/549202"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Ring with Cat and Kittens.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 1295 – 664 BC<br>" +
                "Culture: Ancient Egypt, Ramesside/Third Intermediate Period<br>" +
                "Description: This ring depicts a cat, and its kittens perched above a bound bundle of flowering papyrus designed to represent a marsh. " +
                "In all likelihood, these elements symbolize the myth of the \"Faraway Goddess,\" a story in which a feline plays a prominent role as a deity" +
                " that must be coaxed back to the Nile Valley after she flees into the Nubian desert. Her desertion disrupts the ancient Egyptian world of \"maat”, " +
                "and she must be brought back by a variety of personages so that Egypt returns to stability and order and prosperity. " +
                "Elaborately carved faience rings typically date to this period when craftsmen had total mastery of the medium faience; " +
                "this one is a superb example. Such rings were most likely created to celebrate various festivals held in honour of the deities depicted on the rings.",
                "https://www.metmuseum.org/art/collection/search/744564"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Sarcophagus of Harkhebit.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 595 – 526 BC<br>" +
                "Culture: Ancient Egypt, Late Period<br>" +
                "Description: Horkhebit was a \"Royal Seal Bearer, Sole Companion, Chief Priest of the Shrines of Upper and Lower Egypt, and Overseer of the Cabinet\" " +
                "in early Dynasty 26. His tomb was a great shaft over sixty feet deep sunk into the desert and solid limestone bedrock in the Late Period cemetery " +
                "that covers most of the area east of the Djoser complex at Saqqara. In a huge plain chamber at the bottom of the shaft, a rectangular rock core was" +
                " left standing and hollowed out to house this anthropoid sarcophagus. When the tomb was excavated by the Egyptian government in 1902, the sarcophagus" +
                " contained the remains of a badly decomposed gilded cedar coffin, and a mummy that wore a mask of gilded silver, gold finger and toe stalls, and" +
                " numerous small amulets. Other canopic and shabti equipment accompanied the burial. The finds went to the Egyptian Museum, Cairo, while this" +
                " sarcophagus was purchased from the Egyptian government by the Metropolitan Museum.",
                "https://www.metmuseum.org/art/collection/search/548211"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Kneeling statue of Hatshepsut.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 1479 – 1458 BC<br>" +
                "Culture: New Kingdom, Ancient Egypt<br>" +
                "Description: This over life-size kneeling statue and two others in the collection were made to flank the processional pathway along the axis of" +
                " Hatshepsut's temple at Deir el-Bahri. They depict Hatshepsut as the ideal Egyptian king - a young man in the prime of life. Each statue has an" +
                " inscription that includes her personal name, Hatshepsut (literally foremost of noblewomen) and/or a feminine pronoun or verb form, so the masculine" +
                " garb and physique were not intended to trick people into thinking that she was a man." +
                "Although traditionally the Egyptian throne passed from father to son, when the necessity arose, a female ruler was accepted. However, the trappings" +
                " and symbolism associated with kingship were overwhelmingly masculine and the sculptors of this statue were following a tradition that extended back" +
                " more than fifteen hundred years. In this tradition, the public image of the king, whether he was an infant, a frail old man or, in this case, a" +
                " woman, was shown in the most powerful and imposing form – a young, vigorous man, or a human-headed lion-bodied sphinx. In this statue," +
                " Hatshepsut wears the nemes-headcloth, false beard, and shendyt-kilt that are part of the standard regalia of the king. On her chest she also wears" +
                " the same enigmatic amulet suspended on a necklace of tubular beads that is represented on one of the statues representing Hatshepsut as a woman. ",
                "https://www.metmuseum.org/art/collection/search/544447"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Inner Coffin Box of Taenty.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 1069 – 664 BC<br>" +
                "Culture: Third Intermediate Period, Ancient Egypt<br>" +
                "Description: Ta-enty’s small undecorated tomb was just big enough to hold her mummy nested inside two coffins. The box of her inner coffin is " +
                "brightly painted. The central area is dominated by a painting of a large djed-pillar from the top of which issue two arms holding the sun's disc" +
                " over an ankh sign. Ta-enty's intact tomb was found during John Garstang's excavations at Kostamneh (Nubia) in 1906. The rock cut tomb was given" +
                " the number 200K. II 06. It is now lost beneath the waters of Lake Nasser. John Garstang describes the discovery in his handwritten report to the" +
                " excavation committee: “To the north of the chief necropolis were a few tombs of pure Egyptian character of the New Empire, probably the graves of" +
                " the officials concerned in the governing of this site. The above shows the door of the tomb built up with stones: upon removing these there was" +
                " seen the rather good mummy case of one “Antï”, and excellent mummy within which has not been disturbed in any way. Date about XX Dyn.",
                "https://www.liverpoolmuseums.org.uk/artifact/inner-coffin-box-of-taenty"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Game of Hounds and Jackals.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 1814 – 1805 BC<br>" +
                "Culture: Middle Kingdom, Ancient Egypt<br>" +
                "Description: The board rests on four bulls' legs; one is completely restored and another only partially. There is a drawer with a bolt to store" +
                " the playing pieces: five pins with hounds' heads and five with jackals' heads. The board is shaped like an axe-blade, and there are 58 holes" +
                " in the upper surface with an incised palm tree topped by a shen sign in the centre. Howard Carter and the Earl of Carnarvon reconstructed the" +
                " game as follows in their publication of the find (Five Years of Explorations at Thebes, A Record of Work Done 1907-1911, London, Oxford, New" +
                " York, 1912, p. 58): \"Presuming the 'Shen' sign ... to be the goal, we find on either side twenty-nine holes, or including the goal, thirty" +
                " aside. Among these holes, on either side, two are marked ..nefer, 'good'; and four others are linked together by curved lines. If the holes" +
                " marked 'good' incur a gain, it would appear that the others, connected by lines, incur a loss. Now the moves themselves could easily have been" +
                " denoted by the chance cast of knucklebones or dice....and if so we have before us a simple, but exciting, game of chance." +
                "Egyptians likened the intricate voyage through the underworld to a game. " +
                "This made gaming boards and gaming pieces appropriate objects to deposit in tombs.",
                "https://www.metmuseum.org/art/collection/search/543867"
        ));

        // Ancient Near East Collection 15/15
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Master of Animals Standard.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 1000 BC – 600 BC<br>" +
                "Culture: Iron Age<br>" +
                "Region: Lorestan, Iran<br>" +
                "Description: The exact purpose of this object remains a little bit of a mystery. It is unique to the Luristan region, " +
                "a mountainous area in western Iran just east of Mesopotamia. Thousands of decorative bronze artefacts were buried in cemeteries " +
                "and religious sanctuaries. However, none have been found in an undisturbed archaeological context which makes interpretation of what" +
                " they were used for uncertain. They may have been used as protective devices within religious rituals. At the base of the object is a" +
                " hollow socket designed to be fixed at the top of a pole or other vertical support (which is why they are often called finials or standards)." +
                " The object is shaped in the form of a figure wrestling a pair of stylised felines with smaller felines on their backs (an image often called" +
                " ‘master-of-animals’ iconography).",
                "https://www.liverpoolmuseums.org.uk/artifact/master-of-animals-standard"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/South Arabian Statue.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 100 BC – 1 BC<br>" +
                "Culture: Iron Age South Arabian<br>" +
                "Region: Yemen<br>" +
                "Description: Standing male statue lacking only its right hand and forearm with a chip to the nose. Across the front of the base in clear," +
                " sharp letters is incised ancient Arabian text reading \"Ab'alay, he of (the clan) Dharih-il\". Qataban was one of the ancient Yemeni kingdoms." +
                " Several sculptures like this were discovered in the cemetery at Timna, capital of ancient Qataban. They were probably funerary monuments." +
                " The spice trade brought the people of South Arabia into regular contact with the land to the north. Frankincense and myrrh, the two main" +
                " perfume-resins of the ancient world, occur naturally in south Arabia. The trading caravans took these and other spices north, forming the" +
                " basis of a prosperous urban civilisation. From the 10th century BC, the kingdoms of Saba (Biblical ‘Sheba’), Himyar and Qataban were famous" +
                " for their great wealth. The people of South Arabia learnt an alphabet from their northern contacts, and surviving inscriptions show that many" +
                " local dialects developed.",
                "https://www.liverpoolmuseums.org.uk/artifact/south-arabian-statue"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Seated Goddess with a Child .jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 14th – 13th Century BC<br>" +
                "Culture: Hittite<br>" +
                "Description: This tiny pendant was probably intended to be worn round the neck as an amulet. Small gold figures with loops survive from Iran," +
                " Mesopotamia, the Levant, and Egypt, attesting to the widespread use of such objects. Similar objects from Hittite culture suggest that these " +
                "small figures were portable representations of Hittite gods. The figure shown here, cast in gold using the lost-wax process, is of a seated " +
                "goddess in a long gown, with large oval eyes and a thin mouth with creases at the sides. She is wearing simple, looped earrings and a necklace. " +
                "Her disk-like headdress probably represents the sun, which would lead to the conclusion that this may be the sun goddess, Arinna, a major Hittite" +
                " divinity. A loop for suspension protrudes from the back of the headdress. On her lap the goddess holds a naked child, cast separately of solid " +
                "gold and then attached. The chair on which they are seated is backless and has lion paws.",
                "https://www.metmuseum.org/art/collection/search/327401"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Plaque with horned lion-griffins.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 6th – 4th Century BC<br>" +
                "Culture: Achaemenid Persian<br>" +
                "Description: In the sixth century B.C., under the leadership of Cyrus the Great, the Persians established themselves at the head of an empire" +
                " that would eventually extend from eastern Europe and Egypt to India. The Achaemenid Period is well documented by the descriptions of Greek and " +
                "Old Testament writers and by abundant archaeological remains.<br>" +
                "Like the Achaemenid gold vessel decorated with the forepart of a lion also in the Museum's collection, this ornament depicts the winged " +
                "lion-monster but here two creatures are shown rampant. In place of the lion's ears they have those of a bull. Horns curl back over spiky manes" +
                " and the lion's neck is covered with a feather pattern. Sharply stylized wings extend over two of the five bosses and serve as decorative balance" +
                " for the design. Heavy rings attached to the back suggest that the ornament was worn on a leather belt. the similar treatment of the lion motif on" +
                " different types of objects demonstrates decorative conventions of the period.",
                "https://www.metmuseum.org/art/collection/search/324290"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Panel with Lion.jpeg", new ImageInfo("<html>" +
                "<div style='width:400px;'>" +
                "Date: 604 -562 BC<br>" +
                "Culture: Babylonian<br>" +
                "Description: The Assyrian Empire fell before the combined onslaughts of Babylonians and Medes in 614 and 612 B.C. In the empire's final days," +
                " Nabopolassar (r. 625–605 B.C.), who had been in Assyrian service, established a new dynasty with its capital in Babylon. During the reign of" +
                " his son, Nebuchadnezzar II (r. 604–562 B.C.), the Neo-Babylonian empire reached its peak. This was largely attributable to Nebuchadnezzar's" +
                " ability as a statesman and general. He maintained friendly relations with the Medes in the east while vying successfully with Egypt for the" +
                " control of trade on the eastern Mediterranean coast. He is well known as the biblical conqueror who deported the Jews to Babylon after the capture" +
                " of Jerusalem.<br>" +
                "During this period Babylon became the city of splendour described by Herodotus and the Old Testament Book of Daniel. Because stone is rare in" +
                " southern Mesopotamia, moulded glazed bricks were used for building and Babylon became a city of brilliant colour. Relief figures in white, black," +
                " blue, red, and yellow decorated the city's gates and buildings." +
                "The most important street in Babylon was the Processional Way, leading from the inner city through the Ishtar Gate to the Bit Akitu, or" +
                " \"House of the New Year's Festival.\" The Ishtar Gate, built by Nebuchadnezzar II, was a glazed-brick structure decorated with figures of bulls" +
                " and dragons, symbols of the weather god Adad and of Marduk. North of the gate the roadway was lined with glazed figures of striding lions. This" +
                " relief of a lion, the animal associated with Ishtar, goddess of love and war, served to protect the street; its repeated design served as a guide" +
                " for the ritual processions from the city to the temple.",
                "https://www.metmuseum.org/art/collection/search/322585"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Standing Male Worshiper.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 2900 – 2600 BC<br>" +
                "Culture: Sumerian<br>" +
                "Description: In Mesopotamia gods were thought to be physically present in the materials and experiences of daily life. Enlil, considered the most" +
                " powerful Mesopotamian god during most of the third millennium B.C., was a \"raging storm\" or \"wild bull,\" while the goddess Inanna reappeared" +
                " in different guises as the morning and evening star. Deities literally inhabited their cult statues after they had been animated by the proper" +
                " rituals, and fragments of worn statues were preserved within the walls of the temple.<br>" +
                "This standing figure, with clasped hands and a wide-eyed gaze, is a worshiper. It was placed in the \"Square Temple\" at Tell Asmar, perhaps" +
                " dedicated to the god Abu, in order to pray perpetually on behalf of the person it represented. For humans equally were considered to be physically" +
                " present in their statues. Similar statues were sometimes inscribed with the names of rulers and their families.",
                "https://www.metmuseum.org/art/collection/search/323735"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Head of a Ruler.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 2300 – 2200 BC<br>" +
                "Culture: Mesopotamian<br>" +
                "Description: The identity of this life-size head and where it was created remain a mystery. The expert craftsmanship and innovative technology" +
                " involved in shaping it and casting it in copper alloy, a very costly material, indicates that it represents a king or elite person. The nose," +
                " lips, large ears, heavy-lidded eyes, and modelling of the face are rendered in a naturalistic style. The dark, empty spaces of the eyes were" +
                " probably originally inlaid with contrasting materials. Patterns in the elegantly coiffed beard and well-trimmed Mustache and the curving and" +
                " diagonal lines of the figure’s cloth turban can still be seen beneath the corroded copper surface. These aspects of personal appearance further" +
                " support the identification of this image with an elite personage. Furthermore, the head’s unusually individualized features suggest that it might" +
                " be a portrait. Were that to be true, the head would be a rare example of portraiture in ancient Near Eastern art.<br>" +
                "Recent examination has revealed that the head, long thought to be virtually solid, originally contained a clay core held in place by metal supports." +
                " It may be among the earliest known examples of life-size hollow casting in the lost-wax method. A plate across the neck incorporates a square peg" +
                " originally set into a body or other mount, which may have been made of a different material.",
                "https://www.metmuseum.org/art/collection/search/329077"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Helmet with Divine figures.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 1500 – 1100 BC<br>" +
                "Culture: Middle Elamite<br>" +
                "This example of military headgear is elaborately decorated with three figures on the front. The central one is a male water deity who holds a " +
                "flowing vase at his chest. He has a multiple horned crown, a beard, curled hair, and a mountainlike or scale pattern on the lower body like the " +
                "one on the background. The top of the garment criss-crosses his chest. He is flanked by two female deities with horned crowns who hold their hands" +
                " up in supplication. Their robes are flounced, and they wear necklaces and bracelets. Hovering over the figures is a raptor like bird with " +
                "carefully delineated feathers. At the back is a decorated tube that may have held an actual feather plume. All these elements were carved from" +
                " bitumen and overlaid with silver and then gold foil with incised decoration, a technique that, along with the style and types of the figures, " +
                "point to Elam as the source. The water god might be either the Elamite Inshushinak or Napirisha, similar to Ea, the Mesopotamian god of the sweet" +
                " waters.<br>" +
                "Such a helmet would have been worn by a warrior of high rank, and perhaps on special occasions rather than in actual battle. The representations" +
                " of protective and important deities could certainly have been apotropaic for the wearer.",
                "https://www.metmuseum.org/art/collection/search/325584"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Statue of Gudea.jpeg", new ImageInfo("<html>" +
                "<div style='width:400px;'>" +
                "Date: 2090 BC<br>" +
                "Culture: Neo-Sumerian<br>" +
                "Description: The Akkadian Empire collapsed after two centuries of rule, and during the succeeding fifty years, local kings ruled independent" +
                " city-states in southern Mesopotamia. The city-state of Lagash produced a remarkable number of statues of its kings as well as Sumerian literary" +
                " hymns and prayers under the rule of Gudea (ca. 2150–2125 B.C.) and his son Ur-Ningirsu (ca. 2125–2100 B.C.). Unlike the art of the Akkadian " +
                "period, which was characterized by dynamic naturalism, the works produced by this Neo-Sumerian culture are pervaded by a sense of pious reserve" +
                " and serenity.<br>" +
                "This sculpture belongs to a series of diorite statues commissioned by Gudea, who devoted his energies to rebuilding the great temples of Lagash" +
                " and installing statues of himself in them. Many inscribed with his name and divine dedications survive. Here, Gudea is depicted in the seated " +
                "pose of a ruler before his subjects, his hands folded in a traditional gesture of greeting and prayer.<br>" +
                "The Sumerian inscription on his robe reads as follows:<br>" +
                "When Ningirsu, the mighty warrior of Enlil, had established a courtyard in the city for Ningišzida, son of Ninazu, the beloved one among the gods;" +
                " when he had established for him irrigated plots(?) on the agricultural land; (and) when Gudea, ruler of Lagaš, the straightforward one, beloved by" +
                " his (personal) god, had built the Eninnu, the White Thunderbird, and the..., his 'heptagon,' for Ningirsu, his lord, (then) for Nanše, the powerful" +
                " lady, his lady, did he build the Sirara House, her mountain rising out of the waters. He (also) built the individual houses of (other) great gods" +
                " of Lagaš. For Ningišzida, his (personal) god, he built his House of Girsu. Someone (in the future) whom Ningirsu, his god - as my god (addressed" +
                " me) has (directly) addressed within the crowd, let him not, thereafter, be envious(?) with regard to the house of my (personal) god. Let him" +
                " invoke its (the house's) name; let such a person be my friend, and let him (also) invoke my (own) name. (Gudea) fashioned a statue of himself." +
                " \"Let the life of Gudea, who built the house, be long.\" - (this is how) he named (the statue) for his sake, and he brought it to him into (his)" +
                " house.",
                "https://www.metmuseum.org/art/collection/search/329072"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Enthroned Deity.jpeg", new ImageInfo("<html>" +
                "<div style='width:400px;'>" +
                "Date: 14th – 13th Century BC<br>" +
                "Culture: Canaanite<br>" +
                "Description: This small seated figure would have originally sat on a separately made chair or stool. A tang at the feet and another at the buttocks" +
                " would have held it in place. He wears a long gown and a conical hat with a small knob at the top. His large eyes may once have held inlays of" +
                " another material. One arm is missing but both would have been bent from the elbow, with hands most likely held forward in a peaceful gesture." +
                " The bronze body is covered with a fairly thick layer of gold foil, which would have protected the underlying material from weathering, added to" +
                " his radiant appearance, and indicated his divine nature. The seated pose, enlarged features, hat shape, and gold foil overlay ultimately all " +
                "identify him as a deity. The figure may have had a ritual purpose as a votive or cult statue.<br>" +
                "During the second millennium B.C. in the Levant, small statues of Canaanite gods were produced that incorporated elements from a variety of " +
                "cultures. The conical hat worn by many of the seated figures may reflect Egyptian inspiration, although it had become a defining feature of the" +
                " local style by the time it was used in this period. Many of the male figures produced in this tradition take one of two forms: either that of a" +
                " benevolent, mature deity like this one, or that of a youthful deity raising a weapon in his right hand. The latter was called a “smiting god” and" +
                " his dynamic stance derives from Egyptian images of kings triumphing over fallen enemies, another motif borrowed from Egypt and transformed within" +
                " a Canaanite context. These warlike deities probably represent the young Canaanite storm god Baal, while the seated gods, like this one, likely " +
                "represent El, the head of the Canaanite pantheon. Both were the subject of widespread worship in the Levant during the later second millennium B.C.<br>" +
                "While this small figure appears unremarkable today, images of this type aroused great passions in the period when monotheistic worship first " +
                "developed in the Levant. Because of the millennia-long belief that such objects could embody the essence and power of the deities they depicted," +
                " these images presented a challenge to new religious ideas that classified them as pagan gods. The idols destroyed by the early Jewish patriarchs," +
                " who may have lived in the early Iron Age, were created in the same Canaanite cultural context as this piece and perhaps took similar forms.",
                "https://www.metmuseum.org/art/collection/search/322889"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Shaft-hole axe head.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: Late 3rd – Early 2nd millennium BC<br>" +
                "Culture: Bactria-Margiana Archaeological Complex<br>" +
                "Description: Ancient Bactria and Margiana were areas along the Oxus and Murghab rivers in modern Uzbekistan, Tajikistan, and Afghanistan." +
                " While these areas were sparsely inhabited during much of the third millennium B.C., by about 2200 B.C. permanent settlements with distinctive" +
                " forms of architecture, burial practices, and material culture had been established, supported in part by active trade with parts of Iran, " +
                "Mesopotamia, and the Indus Valley.<br>" +
                "This silver-gilt shaft-hole axe is a masterpiece of three-dimensional and relief sculpture. Expertly cast and gilded with foil, it represents" +
                " a bird-headed hero grappling with a wild boar and a winged dragon. The idea of the heroic bird-headed creature probably came from western Iran," +
                " where it is first documented on a cylinder seal impression. The hero's muscular body is human except for the bird talons that replace the hands" +
                " and feet. He is represented twice, once on each side of the axe, and consequently appears to have two heads. On one side, he grasps the boar by" +
                " the belly and on the other, by the tusks. The posture of the boar is contorted so that its bristly back forms the shape of the blade. With his" +
                " other talon, the bird-headed hero grasps the winged dragon by the neck. This creature is distinguished by folded and staggered wings, a feline" +
                " body, and the talons of a bird of prey in the place of his front paws. Its single horn has been broken off and lost.",
                "https://www.metmuseum.org/art/collection/search/329076"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Openwork furniture plaque.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 9th – 8th Century BC<br>" +
                "Culture: Assyrian<br>" +
                "Description: Furniture inlaid with carved ivory plaques was highly prized by the Assyrian kings. During the ninth to seventh centuries B.C.," +
                " vast quantities of luxury goods, often embellished with carved ivory in local, Syrian, and Phoenician styles, accumulated in Assyrian palaces," +
                " much of it as booty or tribute. This object belongs to a group of plaques depicting animals and stylized plants. They were made by master carvers" +
                " in a delicate openwork technique characteristic of Phoenician ivory carving. However, the style and subjects depicted have close parallels on" +
                " stone relief sculptures from Tell Halaf, in northern Syria, and a debate exists over which tradition produced these fine panels.",
                "https://www.metmuseum.org/art/collection/search/324739"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Kneeling Bull.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 3100 – 2900 BC<br>" +
                "Culture: Proto-Elamite<br>" +
                "Description: Soon after the political transformations of the Uruk period in southern Mesopotamia, similar innovations—including writing and" +
                " cylinder seals, the mass production of standardized ceramics, and a figural art style—developed around the city of Susa in southwestern Iran," +
                " an area in which the predominant language was Elamite. While most of these innovations were adapted from Mesopotamian examples, they all took" +
                " on distinctive Elamite characteristics in Iran.<br>" +
                "This small silver bull, clothed in a robe decorated with a stepped pattern and holding a spouted vessel, shows a curious blend of human and animal" +
                " traits. The large neck meets distinctly human shoulders, which taper into arms that end in hooves. Representations of animals in human postures" +
                " were common in Proto-Elamite art, possibly as symbols of natural forces but just as likely as protagonists in myths or fables. The function of " +
                "this small masterpiece remains uncertain. Traces of cloth that were found affixed to the figure suggest that it was intentionally buried, perhaps" +
                " as part of a ritual or ceremony.",
                "https://www.metmuseum.org/art/collection/search/329074"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Headdress.jpeg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 2600 – 2500 BC<br>" +
                "Culture: Sumerian<br>" +
                "Description: Kings and nobles became increasingly powerful and independent of temple authority during the course of the Early Dynastic period" +
                " (2900–2350 B.C.), although the success of a king's reign was considered to depend on support from the gods. A striking measure of royal wealth" +
                " was the cemetery in the city of Ur, in which sixteen royal tombs were excavated in the 1920s and 1930s by Sir Leonard Woolley. These tombs" +
                " consisted of a vaulted burial chamber for the king or queen, an adjoining pit in which as many as seventy-four attendants were buried, and a ramp" +
                " leading into the grave from the ground.<br>" +
                "This delicate chaplet of gold leaves separated by lapis lazuli and carnelian beads adorned the forehead of one of the female attendants in the" +
                " so-called King's Grave. In addition, the entombed attendants wore necklaces of gold and lapis lazuli, gold hair ribbons, and silver hair rings." +
                " Since gold, silver, lapis, and carnelian are not found in Mesopotamia, the presence of these rich adornments in the royal tomb attests to the" +
                " wealth of the Early Dynastic kings as well as to the existence of a complex system of trade that extended far beyond the Mesopotamian River valley.",
                "https://www.metmuseum.org/art/collection/search/322903"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Stag Vessel.jpeg", new ImageInfo("<html>" +
                "<div style='width:400px;'>" +
                "Date: 14th – 13th Century BC<br>" +
                "Culture: Hittite<br>" +
                "Description: By 1700 B.C., people speaking Hittite—an Indo-European language—had founded a capital at Bogazköy (ancient Hattusha) and, under a" +
                " series of powerful kings, established a state in central Anatolia. The Hittite army attacked and partly destroyed Babylon in 1595 B.C., and in" +
                " 1285 B.C. fought a battle against the Egyptian king Ramesses II at Qadesh in Syria.<br>" +
                "This silver drinking vessel in the form of a stag was hammered from one piece that was joined to the head by a checkerboard-patterned ring. Both" +
                " the horns and the handle were attached separately. A frieze depicting a religious ceremony decorates the rim of the cup, suggesting the uses for" +
                " which the cup was intended. A prominent figure, thought to be a goddess, sits on a cross-legged stool, holding a bird of prey in her left hand " +
                "and a small cup in her right. She wears a conical crown and has large ears, typical of Hittite art. A mushroom-shaped incense burner separates her" +
                " from a male god who stands on the back of a stag. He, too, holds a falcon in his left hand, while with his right he grasps a small, curved staff." +
                " Three men are shown in profile, moving to the left and facing the deities. Each holds an offering to the divinities. Behind the men is a tree or" +
                " plant against which rests the collapsed figure of a stag. Hanging from the tree is a quiver with arrows and an object that appears to be a bag. " +
                "Two vertical spears complete the frieze and separate the stag from the goddess.<br>" +
                "Cult scenes or religious processions are commonly represented in the art of the Hittite Empire, and texts make frequent reference to trees and" +
                " plants associated with rituals or festivals. The texts also tell us that spears were venerated objects, so it is possible that the stag, killed" +
                " in hunt, as is suggested by the quiver and bag, was being dedicated to the stag god. Hittite texts also mention that animal-shaped vessels made " +
                "of gold, silver, stone, and wood, in the appropriate animal form, were given to the gods for their own use. Though the precise meaning of the " +
                "frieze on this vessel remains a matter of conjecture, it is possible that it was intended to be the personal property of the stag god.",
                "https://www.metmuseum.org/art/collection/search/327399"
        ));

        // Rome 15/15
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Serapis with Cerberus.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Culture: Roman Imperial<br>" +
                "Description: Statuette of the god Serapis in a throne with Cerberus, his three-headed dog. It is like many other statues, derived from the" +
                " cult statue of Serapis from Alexandria, and attributed to 3rd century BC sculptor, Bryaxis. Such typical features include the frontal pose," +
                " the left arm raised and right outstretched, the costume of the sleeved chiton, the himation and the sandals, the large face with the beard," +
                " the long hair, the arrangement of hair on the forehead, and the modius (grain measure) on his head. The three headed dog Cerberus is also like" +
                " the typology attributed to the Bryaxis statue. The god has the character of an oracle as symbolised by its open mouth. The statue was probably " +
                "intended for a private setting, a domestic shrine and many similar ones have been found in Rome. The base with the overhanging lip would make it " +
                "easy to display in a domestic setting. Serapis was a god associated with healing and death and had a strong personal appeal. Blundell identified " +
                "the statue as Pluto because of the presence of Cerberus.",
                "https://www.liverpoolmuseums.org.uk/artifact/statue-of-serapis-cerberus"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Ash Chest.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 150 BC – 70 AD<br>" +
                "Culture: Roman Imperial; Antonine<br>" +
                "Description: Double rectangular ash chest of Lepidia Privata and M. Lepidius Epigonus. Despite the double inscription there is only one cavity" +
                " in the chest. The decoration is only at the front while at the side corners there are the remnants of Ammon heads and eagles in a recessed panel." +
                " The Ammon heads are too big and the sculptor may have found it difficult to represent them in a convincing way.  The heads have some detailed" +
                " modelling, and their beards and hair are stylised but of regular curls of possibly the 2nd century AD. The eagles are also schematically" +
                " represented and smaller than the Ammon heads, their feathers made in rough chisel strokes. A laurel garland slungs from the horns to the lower " +
                "part of the front. The garland is small and rope like and follows the shape of the inscription panel rather than in an arc. Details of leaves and " +
                "berries are on the garland but in a very low relief. Above the inscription is an abstract pattern of pairs of wavy lines, possibly a ribbon " +
                "(taeniae). Above the bottom at the front there is a moulding with a single line and the traces of similar moulding at the top are heavily eroded." +
                " The back is roughly worked and so is the interior cavity. The top edge is not shaped for a lid and there are no clamp holes. The sides are" +
                " partially finished.",
                "https://www.liverpoolmuseums.org.uk/artifact/ash-chest-24"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Ash Chest 2.jpg", new ImageInfo("<html>" +
                "<div style='width:400px;'>" +
                "Date: 140 BC – 50 AD<br>" +
                "Culture: Roman Imperial; early Antonine<br>" +
                "Description: Large circular ash chest with an inscription panel surrounded by decorative motifs. The rest of the spaces at the front and the " +
                "sides are decorated with strigilations (S-Shaped fluting). The top edge is not shaped to hold a lid but the two holes surviving at the top may have " +
                "held pins for this purpose. The interior cavity is smoothly worked. The inscription is: D.M ANTONIAE GEMELLAE DIADVMENVS PIENTISSIMAE FECIT VIXIT." +
                "ANNIS. XXXIII To the Shades. Diadumenus made this for Antonia Gemella, most dutiful, who lived 33 years. A strange feature of the inscription is " +
                "the fact that there is not a noun to go with pientissimae (most common noun used for that would be coniugi or a word desribing the relationship " +
                "with the dedicator). It is therefore uncertain if Antoniae was the wife, the mistress or the a patron of the dedicator. Diadumenus is given only" +
                " one name which may suggest that he was a slave to Antonia and she may have been a freedwoman. The insription is flanked by dolphins with their" +
                " nose down and with water underneath them. A fruit garland is suspended between the dolphins' tails and hangs down beneath the inscription panel" +
                " with no space for any addtional motifs. Below the garland in the space between it and the moulding at the bottom of the urn are a series of short" +
                " flutes with rounded ends and the fluting is visible on the back of the chest. Below the dolphins is the strigilitated fluting and it covers the " +
                "back fo the ash chest. At the bottom of the ash chest there is moulding decorated with a complex leaf design. The strigilated fluting was adopted" +
                " from fluting in sarcophagi as is the garland and the style of it was common from the 2nd century AD. The drill is used in the garland as drilled" +
                " holes rather than a continuous channel (running drill). The details of the fruit were rendered with the chisel. Because fluting is also at the " +
                "back of the ash chest it is possible that the chest was made some point after sarcophagoi had become the alternative to ash chests, towards the 2nd" +
                " century AD. Similar ash chests date from the Hadrianic and generally Antonine period. The ash chest may have been enhanced with 18th century " +
                "additions and restorations such as the inscription and the lid. It exact provenance is unknown and was probably exported from Rome by d'Este, the " +
                "famous dealer and collector. The ash chest was not included in the Monumenta Mattheiana. The ash chest is in a poor condition.",
                "https://www.liverpoolmuseums.org.uk/artifact/ash-chest-11"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Sculpture of Cybele .jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 100 BC – 100 AD<br>" +
                "Culture: Roman<br>" +
                "Description: Seated figurine of Cybele holding a tympanum (small drum) in her left hand, accompanied by a lion. Charles Gatty's" +
                " description: \"Figure of a woman, seated, with high headdress, the right arm gone, the left leaning on a shield (?); a dog or lio on her lap," +
                " a lion on either side the trhine: in white marble\"; with another not by AWF (?) \"Cybele - very good - Graeco-Roman\".",
                "https://www.liverpoolmuseums.org.uk/artifact/sculpture-of-cybele"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Statue of Anchirroe .jpg", new ImageInfo("<html>" +
                "<div style='width:400px;'>" +
                "Date: 1 AD – 100 AD<br>" +
                "Culture: Roman<br>" +
                "Description: Headless young female statue, inscribed 'Anchirroe'. The statue represents a type that is known in more than twenty versions but" +
                " none of them preserves the original head, arms or attributes. The draped female tiptoes forward in a graceful movement that is most certainly a" +
                " restoration. The figure faces the front and has the weight on her left leg, bent at the knee. The extended right leg is a restoration and adds" +
                " movement to the statue but does result to the arched foot overhanging from the plinth. It is most certain that the right lower leg and the foot" +
                " were bare because the woman raises the hem of her skirt grasping the drapery in her right hand. The female wears a chiton and an himation. The" +
                " sleeveless chiton is cut with a deep armhole and the himation is hiked up on her right leg and folded back off her left shoulder. On the left" +
                " hand side there are swallowtail folds on the edge of the chiton's long overfold. The Greek inscription Anchyrrhoe at the front of the plinth," +
                " inspired the 18th restoration of the statue as holding a water jug and adorned with a lotus flower. The inscription was forgotten because " +
                "according to Henry Blundell it was covered by mortar and it was rediscovered in the 18th century. The authenticity of the inscription was doubted " +
                "because of the unorthodox spelling. 16th century restorations of nymphs were extremely popular. The modern head, formerly associated with the" +
                " statue, has the accession number WAG 8777. The statue was once at the Villa d'Este at Tivoli, where it decorated the Fontanile della Civetta," +
                " one of the famous fountains at the Villa. It was purchased in 1790 by Lisanroni perhaps with the involvement of Pacetti. Hadrian Villa's" +
                " provenance is speculative. The head that was originally removed was added by Lisandroni and his partner Antonio d'Este, restorations also include" +
                " the arms the right leg below the knee and the foot, the outer edges of the drapery. The front of the plinth has been broken and rejoined.",
                "https://www.liverpoolmuseums.org.uk/artifact/statue-of-anchirroe"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bust of Boy .jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 100 AD – 125 AD<br>" +
                "Culture: Roman<br>" +
                "Description: Portrait of a boy restored on a modern bust. A young boy of around ten is portrayed. He has a typical ‘fringe’ hairstyle with the" +
                " hair combed deeply and thickly onto the forehead. Individual hairs are depicted in the locks by claw-chiselling, and the locks have a slightly" +
                " pointed flame-like termination. The eyes are large with modest indication of lids the lips are softly modelled. The bust is quite fleshy around" +
                " the pectorals, a restoration. There is damage to the right cheek and the chin, and part of the nose is missing.",
                "https://www.liverpoolmuseums.org.uk/artifact/bust-of-boy-1"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bust of Trajan.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 1 -100 AD<br>" +
                "Culture: Imperial Rome<br>" +
                "Description: Portrait of Trajan restored on an ancient bust. The head is turned left and slightly upwards. It portrays Trajan in the ‘Opferbild’" +
                " type – best recognized by the long row of parallel hair locks terminating in a claw above the outer corner of the left eye, and is a high quality" +
                " example of this military-style haircut. The clearly shaped hair around the temples stands in strong contrast to the barely engraved hair at the " +
                "back. The motifs on the bust suggest that it dates to the Hadrianic or early Antonine period. The face has been heavily restored in terms of the " +
                "nose, eyebrows, chin, upper lip and ears. The bust is ancient but probably later to the head, probably Hadrianic or early Antonine period.",
                "https://www.liverpoolmuseums.org.uk/artifact/bust-of-trajan)"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bowl 1 .jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: Mid 1st Century AD<br>" +
                "Culture: Imperial Rome<br>" +
                "Description: A deep and short bowl of the pillar mould decoration with deep ribs on its sides and a flaring rim. The glass is of amber colour" +
                " with decoration in opaque white and red marbling. There are signs of previous damage and repair to one side and the base of the bowl.",
                "https://www.liverpoolmuseums.org.uk/artifact/bowl-369"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bowl 2.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 1 – 100 AD<br>" +
                "Culture: Roman<br>" +
                "Description: Clear glass dish with rolled down cylindrical foot, gently curving side and a everted rim. Heavy patches of iridescent weathering" +
                " are present on the inside of the piece.",
                "https://www.liverpoolmuseums.org.uk/artifact/bowl-382"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bowl 3.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 100 BC – 100 AD<br>" +
                "Culture: Roman<br>" +
                "Description: Transparent dark purple glass shallow bowl, pillar-moulding decoration with ribs outside of body and a wheel-cut groove inside." +
                " Deep folded rim and thick walls. Worn, surface dirt, some surface residue and pitted.",
                "https://www.liverpoolmuseums.org.uk/artifact/bowl-368"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bust of a Priest of Isis.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 100 -200 AD<br>" +
                "Culture: Imperial Rome<br>" +
                "Description: Roman bust, mounted on a metal rod set into a piece of stone (perhaps a column fragment?). Discovered at Corwen in 1909 by Mr John" +
                " Williams, architect of Dee View, Corwen, beneath foundations adjacent to The Crown Hotel, thought to date back to 1628. There is a newspaper" +
                " article about the discovery in the History File, along with a postcard. Yr Adsain, 30 May 1916 page 6: \"Priest of Isis Bust. The Greek Marble" +
                " head of the First Century Bust discovered at Corwen by Mr. J. Williams, Dee View, a few years ago - this being the only one in history ever" +
                " discovered in the British Isles is to be offered for sale by public auction at an early date for the benefit of the Red Cross Socity. The bust is" +
                " mounted upon a mullion pedestal, the piece of mullion was obtained from a window in Corwen Church previous to the restoration some 45 years ago." +
                "\" Dimensions of just the bust: H 190 mm; W 95 mm.",
                "https://www.liverpoolmuseums.org.uk/artifact/bust-of-priest-of-isis"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Statue of Apollo.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 100 – 200 AD<br>" +
                "Culture: Roman<br>" +
                "Description: Under life-size statue of Apollo, leaning against a tripod with a snake. The god has a hipshot pose with the weight on the straight" +
                " right leg. The torso is mainly the ancient piece, the ancient arms, do not survive but it is very likely that the god raised his right arm and " +
                "lowered the left. The left may have rested on a large object that was doweled into place at the left hip. The youth and appearance of the god " +
                "suggest he is either Apollo or Dionysus. The strap across the torso may had been for a quiver or a cithara, both attributes of Apollo. He slightly " +
                "leans in his pose. A missing dowel suggests that there was a musical instrument attached to the left arm as an accessory. The presence of a tripod" +
                " with a snake entwined in its legs and a vessel in the right hand suggest a mysticism, relevant to the god Apollo. There are restorations from " +
                "different marble especially in the head and the neck, the arms below the shoulder, the right knee, the left lower calf, the tripod which has been" +
                " executed in two parts, the basin and the stand. The strut also serves to attach the tripod to the left leg and another joins at the calves, a " +
                "dowel hole below the left hip was possibly used to attach his cithara. Blundell mentioned a vessel in the right hand but today it grasps a baton. " +
                "There is erosion in numerous surfaces such as the ends of the hair, the nipples, the patches of the right shoulder and quiver. The modern plinth is" +
                " also in pieces.",
                "https://www.liverpoolmuseums.org.uk/artifact/statue-of-apollo-0"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Statuette of Hermes.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 1- 300 AD<br>" +
                "Culture: Imperial Rome<br>" +
                "Description: Statuette of the god Hermes (Roman Mercury). He stands frontally in a relaxed pose with his weight on his right leg, left leg " +
                "relaxed. He wears a chlamys pinned at his left shoulder and hanging low at the back and over his left arm. In his bent left arm he carries a " +
                "caduceus and in his right a purse which rests on a ram, the traditional attributes of the god Mercury. The statuette is modest and evokes the" +
                " work of the 5th century BC sculptor Polykleitos or is in a Polykleitan style. There are restorations at the head and upper neck, the collar " +
                "around the neck, the legs, the plinth, the left hand and part of the caduceus. The statuette has a very smooth and shiny surface that may have " +
                "been the result of modern treatment.",
                "https://www.liverpoolmuseums.org.uk/artifact/statuette-of-hermes-1"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Statue of Bacchus.jpg", new ImageInfo("<html>" +
                "<div style='width:300px;'>" +
                "Date: 0 -200 AD<br>" +
                "Culture: Imperial Rome<br>" +
                "Description: Statue of a young naked male identified as the god Dionysus. Youthful representations of the god were very popular from the late" +
                " classical period and there are many surviving statues. The Ince statue is different in that the Bacchus has his left hip outward, he is thus" +
                " supported on his left leg with a tree trunk which is ornamented with grapes and spiralling vines. It cannot be identified as a Bacchus with " +
                "absolute certainty because he is missing any attributes. The long hair could be easily attributed to Apollo. The restoration kept the head turning" +
                " to its side and giving the statue an aloofness. There is a broken patch on the left hip and this may indicate that the strut was originally on" +
                " the left. The torso is ancient and was joined with the modern addition of the lower legs, ams, strust and base The restorations at the knees," +
                " elbows and hips would require significant technical skills in joining such a big modern piece with the ancient torso and having to rethink scale" +
                " and planes. Blundell was aware of the restoration and mentioned in a letter to Townley that he bought the statue in pieces. An illustration of " +
                "the same statue in the Vetera Monumenta Matthaeiorum as a complete statue may be a fiction or may have been fabricated to increase the appeal to" +
                " potential collectors. Formerly Mattei collection, from Hadrian's Villa at Tivoli. Purchased from the Villa Mattei Collection by Henry Blundell on" +
                " his first trip to Italy in 1777.",
                "https://www.liverpoolmuseums.org.uk/artifact/statue-of-bacchus"
        ));
        imageInfoMap.put("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Sarcophagus.jpg", new ImageInfo("<html>" +
                "<div style='width:500px;'>" +
                "Date: Late 2nd Century AD<br>" +
                "Culture: Roman Imperial<br>" +
                "Description: Relief from the front of a sarcophagus. The side panels of the sarcophagus are: 59.148.264 and 265. The scene is of Phaethon " +
                "imploring Helios to lend him the sun chariot. Phaeton is missing because of the three different restorations the relief had. The scene takes place" +
                " in the interior of the Helio's palace as suggested by the cloth that hangs above the heads and attached to a Corinthian column to the left. The" +
                " main figures of the scene slightly off the centre are: Helios sits on a rock and leans on with his left hand on his seat. He is nude apart from" +
                " a chlamys, flung behind his back. The gesture of his right hand with a whip across his chest is the correct one, although a repair. On his head" +
                " which is ancient he has a diadem. Behind Helios there is a female figure with a long chiton lifting a large swathe of her cloth in her right hand." +
                " She is probably Clymene, Phaeton's mother or Selene, the moon goddess, a good counterpart to Helios, the Sun. There are other sarcophagi which " +
                "show Selene as part of the Phaeton myth with a cresent moon adoring her forehead. She could also be a mixture of both Clymene and Selene. The " +
                "figure of Phaeton originally stood in front of his mother and would have faced his father, with his hand or elbow on his father's left knee in " +
                "a gesture of supplication. His hand was removed and the area was restored but there are still indentations on the rock. The lower part of his " +
                "mother's chiton where he probably stood is very flat and remnants of his chlamys. Oceanus, Clymene's father and relevant to Phaeton's watery " +
                "demise, is below them, reclined. He is an old bearded man and his body is enveloped in a mantle. Oceanus' attribute, a sea monster (ketos) or " +
                "sea dragon is twisted around his left arm. He may have originally held a reed stalk in his right hand and he had crayfish claws in his head, both" +
                " of the claws are chipped. Tellus, another figure of a cosmic relevance is also at the far right corner and is a reminder of Phaeton's destruction" +
                " of the earth. She is on a rocky terrain with her mantle around her lower body, her right hand resting on a cornucopia whose end she holds in her " +
                "left hand. The four standing masculine men are the wind-gods who are trying to harness a quartet of wild steeds to the sun's chariots. The shaven " +
                "wind in the background has wings in his hair, a feature used for semi-gods. The others have beards and carry felline skins around their left arms." +
                " The first wind standing in front of the chariot parades atop his pelt a marine trumpet in the form of he long spiral shell. The winds would have " +
                "originally blown on their sea trumpets but here they hold them like weapons. Their masculinity as well as of the figures on the side panels reminds" +
                " us of Hercules ( 59.148.264 and 59.148.265 ) The first god has successfully harnessed the unruly horse on to the quadriga but the trunctuated " +
                "forms of the other three suggest of their return to the ocean. The chariot's body is decorated with a figure of Tethys, a mantle around her lower" +
                " body and a winged Eos. According to the myth Eos preceded the sun's chariot during its heavenly journey and Tethys received Helios at the end of" +
                " the day. The counterparts of the male winds are the female seasons on the left hand side of the relief: at the left corner Winter, dressed in a " +
                "chiton with a mantle around her head, and her right hand on her chest, holds a bare brunch on her left hand. Autumn, next to her and similarly " +
                "dressed in a chiton and cloak holds a vine in her left hand from which the grape dangled. According to earlier descriptions of the relief, she was" +
                " originally crowned with grapes and vines. Summer seated and leaning to the right, nude upper body. She holds a bundle of wheat in her left hand," +
                " a bull and a sheep at her feet. Spring stands, her right breast exposed and with a spray of flowers in her left hand. Roman sarcophagi of this " +
                "type were placed in mausolea against a wall or in a niche and so were decorated on only the front and two sides.",
                "https://www.liverpoolmuseums.org.uk/artifact/sarcophagus-2"
        ));
    }

    /**
     * Helper method to generate a friendly display name from a file path.
     * E.g., "/Users/taashfeen/Desktop/Group Project/src/Ancient Cyprus/Human-Remains-1.jpg" becomes "Human Remains".
     * This is for "Choose Jigsaw".
     */
    private static String getDisplayName(String filePath) {
        String fileName = new File(filePath).getName();  // e.g., "Human-Remains-1.jpg"
        fileName = fileName.replaceFirst("[.][^.]+$", ""); // remove extension -> "Human-Remains-1"
        fileName = fileName.replace("-", " "); // replace dashes with spaces -> "Human Remains 1"
        fileName = fileName.replaceAll("\\s*\\d+$", ""); // remove trailing numbers -> "Human Remains"
        return fileName.trim();
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Check if the blurred background is available
        if (blurredBackground != null) {
            g.drawImage(blurredBackground, 0, 0, getWidth(), getHeight(), this);
        } else { // If no blurred background is avaiable set it to red
            g.setColor(Color.RED);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /**
     * Dynamically loads a collection based on the collectionName.
     * This method updates the imageComboBox with images corresponding to the collection.
     */
    public void loadCollection(String collectionName) {
        // Removes all previous items, so that we can create 'collections' based off of what the user selects in the main menu
        // Each If, Else if statement corresponds to a different collection
        imageComboBox.removeAllItems();
        if ("Ancient Cyprus".equals(collectionName)) { // Ancient Cyprus collection
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/jug-1.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/jug-2.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/jug-3.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/jug-4.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/jug-5.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/bowl-1.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/bowl-2.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/bowl-3.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/bowl-4.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/bowl-5.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/Pyxis-Lid.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/Spindle-Whorl.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/Temple-Boy.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/Human-Remains-1.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/VotiveStatueHead.jpg"); // 15 Jigsaws
        } else if ("Ancient Greece".equals(collectionName)) { // Ancient Greece collection
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Amour Helmet.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Wine Flask.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Wine Flask 2.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Amphora.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Drinking Vessel.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Krater.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Kylix.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Aryballos.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Aryballos 2.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Aryballos 3.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Rhyton.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Horse votive offering.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Fragments of deer figurine.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Wreath Shaped votive offering.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Greece/Loutrophoros.jpg"); // 15 Jigsaws
        } else if ("Rome".equals(collectionName)) { // Rome Collection
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Serapis with Cerberus.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Ash Chest.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Ash Chest 2.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Sculpture of Cybele .jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Statue of Anchirroe .jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bust of Boy .jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bust of Trajan.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bowl 1 .jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bowl 2.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bowl 3.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Bust of a Priest of Isis.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Statue of Apollo.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Statuette of Hermes.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Statue of Bacchus.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Rome/Sarcophagus.jpg"); // 15 Jigsaws
        } else if ("Ancient Near East".equals(collectionName)) { // Ancient Near East Collection
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Master of Animals Standard.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/South Arabian Statue.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Seated Goddess with a Child .jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Plaque with horned lion-griffins.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Panel with Lion.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Standing Male Worshiper.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Head of a Ruler.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Helmet with Divine figures.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Statue of Gudea.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Enthroned Deity.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Shaft-hole axe head.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Openwork furniture plaque.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Kneeling Bull.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Headdress.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Near East/Stag Vessel.jpeg"); // 15 Jigsaws
        } else if ("Ancient Egypt".equals(collectionName)) { // Ancient Egypt Collection
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Shabti of Djedkhonsuefankh.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Figurine of a Pygmy Dance Leader.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Schist Statuette Fragment.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Offering Table.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Amulet of Jackal Headed Deity.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Composite Papyrus Capital.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Chariots with Court Ladies.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Wedjat Eye Amulet.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Artist's Sketch of a Sparrow.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Ring, signet.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Ring with Cat and Kittens.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Sarcophagus of Harkhebit.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Kneeling statue of Hatshepsut.jpeg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Inner Coffin Box of Taenty.jpg");
            imageComboBox.addItem("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Egypt/Game of Hounds and Jackals.jpeg"); // 15 Jigsaws
        } else {
            for (String img : imageOptions) {
                imageComboBox.addItem(img);
            }
        }
        if (imageComboBox.getItemCount() > 0) {
            puzzlePanel.setImage((String) imageComboBox.getItemAt(0));
        }
    }

}
