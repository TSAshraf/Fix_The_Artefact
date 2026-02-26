import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.imageio.ImageIO;

public class FixThePotGamePanel extends JPanel { // This is the main panel for the "Fix the pot game", extension of JPanel
    private InfoOverlayPanel infoOverlay;
    private FixThePotGame puzzlePanel; // Puzzle panel where the game is played
    private JLayeredPane layeredPane;
    private JPanel controlPanel;
    private SplitChooserOverlay splitOverlay; // Easy/Med/Hard/Custom popover
    private RowsColsOverlay rowsColsOverlay; // Holds the small rows/cols picker overlay
    private InfoOverlayPanel persistentOverlay;
    private JComponent glassPaneRef;
    private JButton previousJigsawButton; // Button to move to previous Jigsaw
    private JButton restartButton; // Button to Restart (reconstruct) the jigsaw
    private JButton showCompletedButton; // Button to show the completed image
    private JButton jigsawSplitButton; // Button to let the user choose Jigsaw Difficulty
    private JButton nextJigsawButton; // Button to move to the next jigsaw
    private JButton extraInfoButton; // Button to open Extra information and view sources
    private JButton musicToggleButton; // Button to the music on/off
    private JButton chooseJigsawButton;  // Button to choose a jigsaw
    private JButton backToCollectionsButton; // Button to return to the collections panel
    private JButton timerButton; // Button for displaying and controlling the timer
    private JButton zenModeButton; // Zen mode
    private JButton themeButton;
    private MusicPlayer musicPlayer; // Music player instance
    private String currentTrackname; // Name of the current music track
    private boolean musicPlaying = true; // first track auto-starts, so assume playing
    private String musicFolderPath = "/Music/"; // Music folder path
    private JComboBox<String> imageComboBox; // Combobox to select images (Jigsaw Puzzles)
    private final String[] imageOptions = ArtifactCatalog.IMAGE_OPTIONS;
    private Timer timer; // Timer instance
    private int elapsedSeconds; // Time elapsed, in seconds
    private boolean timerRunning; // Whether the timer is on or not
    private javax.swing.Timer peekHideTimer; // Timer used to delay hiding the small preview after mouse exit
    private final Map<String, ImageInfo> imageInfoMap = ArtifactCatalog.IMAGE_INFO_MAP;
    private String currentCollection = null;
    private BufferedImage backgroundImage; // Active background (collection + theme)
    private BufferedImage blurredBackground; // Blurred background for game-screen
    private ImageOverlay completedOverlay; // Overlay panel for displaying the full completed image
    private ImagePeek completedPeek; // Small preview panel shown when hovering over the button
    private boolean zenMode = false; // Zen mode
    private JComponent[] zenHideComponents; // Components to hide when Zen mode is on

    public interface GamePanelListener {void onBackToCollections();} // Listener Interface for Screen Navigation
    private GamePanelListener gamePanelListener; // Listener instance
    public void setGamePanelListener(GamePanelListener listener) {this.gamePanelListener = listener;} // Setter Interface for Screen Navigation

    private void setupKeyboardShortcuts() {
        // WHEN_IN_FOCUSED_WINDOW = works as long as this panel is in the active window
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        // Helper: bind keystroke -> runnable
        java.util.function.BiConsumer<String, Runnable> bind = (key, run) -> {
            im.put(KeyStroke.getKeyStroke(key), key);
            am.put(key, new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                    run.run();
                }
            });
        };

        // ==== Suggested shortcuts (change to match what you want) ====
        bind.accept("LEFT", () -> previousJigsawButton.doClick());
        bind.accept("RIGHT", () -> nextJigsawButton.doClick());
        bind.accept("R", () -> restartButton.doClick());
        bind.accept("I", () -> extraInfoButton.doClick());         // info overlay
        bind.accept("C", () -> showCompletedButton.doClick());     // completed image overlay
        bind.accept("Z", () -> zenModeButton.doClick());           // zen mode
        bind.accept("T", () -> timerButton.doClick());         // pause/start timer
        bind.accept("M", () -> musicToggleButton.doClick());       // mute/unmute

        // Escape: close overlays if open (nice UX)
        bind.accept("ESCAPE", () -> {
            boolean changed = false;

            if (persistentOverlay != null && persistentOverlay.isVisible()) {
                persistentOverlay.close();
                changed = true;
                updateInfoTooltip();
            }
            if (completedOverlay != null && completedOverlay.isVisible()) {
                completedOverlay.setVisible(false); // or completedOverlay.close() if you have it
                changed = true;
                updateShowCompletedTooltip();
            }
            if (splitOverlay != null && splitOverlay.isShowing()) {
                splitOverlay.close();
                changed = true;
            }
            if (rowsColsOverlay != null && rowsColsOverlay.isShowing()) { // if you have an isShowing()
                rowsColsOverlay.close();
                changed = true;
            }

            if (changed && glassPaneRef != null) glassPaneRef.repaint();
            repaint();
        });
    }

    public FixThePotGamePanel() { // Sets up the game panel
        setLayout(new BorderLayout()); // Set layout
        setOpaque(false); // Set opacity
        currentCollection = "/Rome";   // or null if you have a safe default in BackgroundCatalog
        applyBackgroundForCurrentState();
        createPuzzlePanel();
        buildControlPanel(); // Build the control panel (all buttons and controls in one row)
        if (imageComboBox.getItemCount() > 0) {
            imageComboBox.setSelectedIndex(0);
            puzzlePanel.setImage((String) imageComboBox.getItemAt(0));
        }
        nextJigsawButton.setEnabled(false); // always start with Next disabled
        ThemeManager.get().register(this);
        setupTimer(); // Set up the timer
        setupKeyboardShortcuts(); // Set up keyboard shortcuts
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

    private void applyBackgroundForCurrentState() {
        Theme theme = ThemeManager.get().getCurrent();
        String path = BackgroundCatalog.backgroundFor(currentCollection, theme);

        try (var in = FixThePotGamePanel.class.getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Missing resource: " + path);

            backgroundImage = ImageIO.read(in);

            // Optional blur for game screen
            blurredBackground = BlurUtil.blurImage(backgroundImage, 5);

        } catch (Exception e) {
            e.printStackTrace();
        }

        repaint();
    }

    private void hideInfoOverlay() { // Call this whenever we switch puzzles
        if (glassPaneRef != null) {
            glassPaneRef.setVisible(false);      // hide the whole overlay layer
            glassPaneRef.remove(persistentOverlay); // make sure the card is not still attached
            glassPaneRef.revalidate();
            glassPaneRef.repaint();
        }
    }

    private static ImageIcon icon(String resourcePath) {
        java.net.URL url = FixThePotGamePanel.class.getResource(resourcePath);
        if (url == null) throw new RuntimeException("Missing resource: " + resourcePath);
        return new ImageIcon(url);
    }

    private static ImageIcon scaledIcon(String resourcePath, int w, int h) {
        Image img = icon(resourcePath)
                .getImage()
                .getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
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

        // Previous Jigsaw Button: Load and scale the Jigsaw icon
        ImageIcon scaledLeftIcon = scaledIcon("/Starting/left.png", 24, 24);
        previousJigsawButton = new JButton(scaledLeftIcon); // Create previous button with scaled image
        previousJigsawButton.setToolTipText("Previous Jigsaw (←)");
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
        previousJigsawButton.setFocusable(false); // prevents SPACE from focusing on the button

        // Restart Button: Load and scale the Restart icon
        ImageIcon scaledRestartIcon = scaledIcon("/Starting/restart.png", 24, 24);
        restartButton = new JButton(scaledRestartIcon); // Create restart button with scaled image
        restartButton.setToolTipText("Restart Jigsaw (r)");
        restartButton.addActionListener(e -> {
            puzzlePanel.restartGame(); // Restarts the game
            elapsedSeconds = 0; // Resetting the Timer
            timerButton.setText("Time: 0 s"); // Reset the Timer
            nextJigsawButton.setEnabled(false); // Part of resetting the game
        });

        // Show Completed Button
        ImageIcon scaledShow_completedIcon = scaledIcon("/Starting/show_completed.png", 24, 24);
        showCompletedButton = new JButton(scaledShow_completedIcon);
        updateShowCompletedTooltip(); // Dynamic tooltip based on overlay visibility
        // Hover → show small preview
        showCompletedButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                initCompletedOverlays();
                BufferedImage img = puzzlePanel.getPotImage();
                if (img != null) {
                    completedPeek.setImage(img, 220, 160);

                    JRootPane root = SwingUtilities.getRootPane(FixThePotGamePanel.this);
                    if (root == null) return;
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
        // Click → toggle small floating overlay
        showCompletedButton.addActionListener(e -> {
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
                    completedOverlay.centerIn(layered.getSize());
                }
                completedOverlay.setVisible(true);
                completedOverlay.requestFocusInWindow();
                completedPeek.setVisible(false);
            } else {
                completedOverlay.setVisible(false);
            }
            updateShowCompletedTooltip();
        });

        // Jigsaw Split Button
        jigsawSplitButton = new JButton(scaledIcon("/Starting/jigsaw_split.jpeg", 24, 24));
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


        themeButton = new JButton();
        themeButton.setFocusable(false);
        themeButton.setPreferredSize(new Dimension(36, 36));
        updateThemeButtonIcon(); // initial icon
        themeButton.addActionListener(e -> {
            ThemeManager.get().toggleTheme();
            updateThemeButtonIcon();
            applyBackgroundForCurrentState();
            ThemeManager.refreshThemeTree(this);
            if (glassPaneRef != null) ThemeManager.refreshThemeTree(glassPaneRef);
        });



        // Information Button
        ImageIcon scaledInformationIcon = scaledIcon("/Starting/information.png", 24, 24);
        extraInfoButton = new JButton(scaledInformationIcon);
        updateInfoTooltip(); // Initialise tooltip based on current overlay state
        extraInfoButton.addActionListener(e -> {
            // Fallback if overlay system isn't ready yet
            if (glassPaneRef == null || persistentOverlay == null) {
                String selectedImage = (String) imageComboBox.getSelectedItem();
                if (selectedImage == null || !imageInfoMap.containsKey(selectedImage)) {
                    JOptionPane.showMessageDialog(
                            FixThePotGamePanel.this,
                            "No extra information available."
                    );
                    return;
                }
                ImageInfo info = imageInfoMap.get(selectedImage);
                JOptionPane.showMessageDialog(
                        FixThePotGamePanel.this,
                        info.getDescription(),
                        "Extra Information",
                        JOptionPane.INFORMATION_MESSAGE,
                        icon(selectedImage)
                );

                extraInfoButton.setToolTipText("Extra information (i)");
                return;
            }
            // Toggle close if already visible
            if (persistentOverlay.isVisible()) {
                persistentOverlay.close();
                if (glassPaneRef != null) glassPaneRef.repaint();
                updateInfoTooltip();
                return;
            }
            // Open / refresh overlay
            String selectedImage = (String) imageComboBox.getSelectedItem();
            if (selectedImage == null || !imageInfoMap.containsKey(selectedImage)) {
                JOptionPane.showMessageDialog(
                        FixThePotGamePanel.this,
                        "No extra information available."
                );
                return;
            }
            ImageInfo info = imageInfoMap.get(selectedImage);

            // Pass full-res image; banner panel will scale it correctly
            ImageIcon previewIcon = icon(selectedImage);
            String titleText = "Artifact: " + ArtifactCatalog.displayName(selectedImage);
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
            updateInfoTooltip();
        });

        // Timer Button Code
        timerButton = new JButton("Time: 0 s");
        timerButton.setPreferredSize(new Dimension(100, 36));
        timerButton.setFocusPainted(false);
        updateTimerTooltip();
        timerButton.addActionListener(e -> {
            if (timerRunning) {
                timer.stop();
                timerButton.setText("Paused: " + elapsedSeconds + " s");
                timerRunning = false;
            } else {
                timer.start();
                timerButton.setText("Time: " + elapsedSeconds + " s");
                timerRunning = true;
            }
            updateTimerTooltip();
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
        musicToggleButton = new JButton(scaledIcon("/Starting/music note.png", 24, 24));
        updateMusicTooltip();
        musicToggleButton.addActionListener(e -> {
            if (musicPlayer != null) {
                musicPlayer.togglePlayPause();
                musicPlaying = !musicPlaying;
                updateMusicTooltip();
            }
        });

        // 3) Track chooser button (the “up arrow”)
        JButton chooseTrackButton = new JButton(scaledIcon("/Starting/music_track.png", 24, 24));
        chooseTrackButton.setToolTipText("Select a music track");
        final MusicChooserPopover[] musicPopoverRef = new MusicChooserPopover[1]; // Popover field (store on your panel class if you want to reuse)
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
        ImageIcon scaledCollectionsIcon = scaledIcon("/Starting/collections.png", 24, 24);
        backToCollectionsButton = new JButton(scaledCollectionsIcon); // Menu Icon button
        backToCollectionsButton.setToolTipText("Return to collection selection"); // Hover over information
        backToCollectionsButton.addActionListener(e -> { // Notify the listener to switch back to the collection screen
            if (gamePanelListener != null) {
                gamePanelListener.onBackToCollections();
            }
        });

        // JIGSAW CHOOSER (popover)
        chooseJigsawButton = new JButton(scaledIcon("/Starting/choose.png", 24, 24));
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
                display.add(ArtifactCatalog.displayName(path));
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
        ImageIcon scaledNextIcon = scaledIcon("/Starting/right.png", 24, 24);
        // Create button with scaled image
        nextJigsawButton = new JButton(scaledNextIcon);
        nextJigsawButton.setToolTipText("Next Jigsaw (→)");
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
        nextJigsawButton.setFocusable(false); // prevents SPACE from focusing on the button


        // Zen mode button
        // Load and scale the Jigsaw icon
        ImageIcon scaledZenIcon = scaledIcon("/Starting/zen.png", 24, 24);
        zenModeButton = new JButton(scaledZenIcon);
        zenModeButton.setToolTipText("Toggle Zen mode (z)");
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
        controlPanel.add(zenModeButton);
        controlPanel.add(timerButton);
        controlPanel.add(themeButton);
        controlPanel.add(extraInfoButton);
        controlPanel.add(showCompletedButton);
        controlPanel.add(jigsawSplitButton);
        controlPanel.add(chooseJigsawButton);
        controlPanel.add(nextJigsawButton);
        add(controlPanel, BorderLayout.SOUTH);

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
                themeButton,
                // you can add zenModeButton here too if you want it to vanish as well
        };
    }



    // --- Tooltip helpers for toggle-style buttons ---
    private void updateMusicTooltip() {
        if (musicToggleButton == null) return;
        musicToggleButton.setToolTipText(musicPlaying ? "Mute music (m)" : "Unmute music (m)");
    }
    private void updateTimerTooltip() {
        if (timerButton == null) return;
        timerButton.setToolTipText(timerRunning ? "Pause timer (t)" : "Start timer (t)");
    }
    private void updateInfoTooltip() {
        if (extraInfoButton == null) return;
        boolean showing = persistentOverlay != null && persistentOverlay.isVisible();
        extraInfoButton.setToolTipText(showing ? "Hide extra information (i)" : "Show extra information (i)");
    }
    private void updateShowCompletedTooltip() {
        if (showCompletedButton == null) return;
        boolean showing = completedOverlay != null && completedOverlay.isVisible();
        showCompletedButton.setToolTipText(showing ? "Hide completed image (c)" : "Show completed image (c)");
    }

    private void updateThemeButtonIcon() {
        Theme t = ThemeManager.get().getCurrent();

        boolean isDark = (t == Theme.DARK);
        String theme_icon_Path = isDark // If you want the icon to represent the CURRENT mode:
                ? "/Starting/dark_mode.png"
                : "/Starting/light_mode.png";

        // If instead you want the icon to show the mode you will SWITCH TO, flip the two paths.

        themeButton.setIcon(scaledIcon(theme_icon_Path, 24, 24));
        themeButton.setText(null);                 // keep it icon-only (optional)
        themeButton.setPreferredSize(new Dimension(36, 36)); // nicer for icon-only (optional)

        themeButton.setToolTipText(isDark ? "Switch to light mode" : "Switch to dark mode");
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        BufferedImage imgToDraw = (blurredBackground != null) ? blurredBackground : backgroundImage;

        if (imgToDraw != null) {
            g.drawImage(imgToDraw, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    public void loadCollection(String collectionName) {
        System.out.println("collectionName = [" + collectionName + "]");
        this.currentCollection = collectionName;     // store current collection
        applyBackgroundForCurrentState();            // swap background immediately

        imageComboBox.removeAllItems();
        for (String img : ArtifactCatalog.imagesFor(collectionName)) {
            imageComboBox.addItem(img);
        }

        if (imageComboBox.getItemCount() > 0) {
            imageComboBox.setSelectedIndex(0);
            puzzlePanel.setImage((String) imageComboBox.getItemAt(0));
        }
        nextJigsawButton.setEnabled(false);
        hideInfoOverlay();
    }
}
