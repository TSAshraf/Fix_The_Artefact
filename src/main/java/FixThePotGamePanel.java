import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.imageio.ImageIO;

public class FixThePotGamePanel extends JPanel {

    private InfoOverlayPanel infoOverlay;
    private FixThePotGame puzzlePanel;
    private JLayeredPane layeredPane;
    private JPanel controlPanel;

    private SplitChooserOverlay splitOverlay;
    private RowsColsOverlay rowsColsOverlay;

    private InfoOverlayPanel persistentOverlay;
    private JComponent glassPaneRef;

    private JButton previousJigsawButton;
    private JButton restartButton;
    private JButton showCompletedButton;
    private JButton jigsawSplitButton;
    private JButton nextJigsawButton;
    private JButton extraInfoButton;
    private JButton favouriteToggleButton;

    private JButton musicToggleButton;
    private JButton chooseJigsawButton;
    private JButton backToCollectionsButton;
    private JButton hintButton;

    private JButton timerButton;
    private JButton zenModeButton;
    private JButton themeButton;

    private MusicPlayer musicPlayer;
    private String currentTrackname;
    private boolean musicPlaying = true;

    private final String musicFolderPath = "/Music/";

    private JComboBox<String> imageComboBox;
    private final String[] imageOptions = ArtifactCatalog.IMAGE_OPTIONS;

    private Timer timer;
    private int elapsedSeconds;
    private boolean timerRunning;

    private javax.swing.Timer peekHideTimer;

    private final Map<String, ImageInfo> imageInfoMap = ArtifactCatalog.IMAGE_INFO_MAP;

    private String currentCollection = "/Rome/Artifacts";
    private BufferedImage backgroundImage;
    private BufferedImage blurredBackground;

    private ImageOverlay completedOverlay;
    private ImagePeek completedPeek;

    private boolean zenMode = false;
    private JComponent[] zenHideComponents;

    private GameState saveState;

    public interface GamePanelListener { void onBackToCollections(); }
    private GamePanelListener gamePanelListener;
    public void setGamePanelListener(GamePanelListener listener) { this.gamePanelListener = listener; }

    public FixThePotGamePanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // 1) Load save + apply theme/collection
        loadSaveStateAndApplyEarly();

        // 2) Background depends on (theme + collection)
        applyBackgroundForCurrentState();

        // 3) Build UI
        createPuzzlePanel();
        buildControlPanel();

        // 4) Restore selection/timer/zen (after controls exist)
        restoreUIStateAfterControlsCreated();

        // Next always starts disabled (until solved)
        if (nextJigsawButton != null) nextJigsawButton.setEnabled(false);

        ThemeManager.get().register(this);

        // 5) Timer must not reset save values
        setupTimer();

        setupKeyboardShortcuts();

        // Enable next once solved and save completion state
        puzzlePanel.setPuzzleSolvedListener(() -> {
            nextJigsawButton.setEnabled(true);

            // Update save state + persist
            markCurrentJigsawCompleted();
            SaveManager.save(saveState);
        });

        revalidate();

        // Setup glassPane + overlays + save on close
        SwingUtilities.invokeLater(() -> {
            JFrame f = (JFrame) SwingUtilities.getWindowAncestor(FixThePotGamePanel.this);
            if (f != null) {
                JRootPane rp = f.getRootPane();
                glassPaneRef = (JComponent) rp.getGlassPane();
                glassPaneRef.setLayout(null);
                glassPaneRef.setVisible(false);

                persistentOverlay = new InfoOverlayPanel();
                persistentOverlay.setLocation(40, 40);

                f.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override public void windowClosing(java.awt.event.WindowEvent e) {
                        snapshotToSaveState();
                        SaveManager.save(saveState);
                    }
                });
            }
        });
    }

    // ---------- Save/load helpers ----------

    private void loadSaveStateAndApplyEarly() {
        saveState = SaveManager.loadOrDefault();
        if (saveState == null) saveState = new GameState();

        // Theme (only safe if you have exactly two themes)
        Theme current = ThemeManager.get().getCurrent();
        if (saveState.theme != null && !current.name().equals(saveState.theme)) {
            ThemeManager.get().toggleTheme();
        }

        // Collection
        if (saveState.currentCollection != null) {
            currentCollection = saveState.currentCollection;
        }
        if (currentCollection == null) currentCollection = "/Rome/Artifacts/";
    }

    private void restoreUIStateAfterControlsCreated() {
        if (saveState == null) return;

        // Jigsaw selection
        if (imageComboBox != null && imageComboBox.getItemCount() > 0) {
            int idx = saveState.selectedJigsawIndex;
            idx = Math.max(0, Math.min(idx, imageComboBox.getItemCount() - 1));
            imageComboBox.setSelectedIndex(idx);

            Object sel = imageComboBox.getSelectedItem();
            if (sel != null) puzzlePanel.setImage(sel.toString());
        }

        // Timer
        elapsedSeconds = Math.max(0, saveState.elapsedSeconds);
        timerRunning = saveState.timerRunning;
        if (timerButton != null) {
            timerButton.setText(timerRunning
                    ? "Time: " + elapsedSeconds + " s"
                    : "Paused: " + elapsedSeconds + " s");
        }
        updateTimerTooltip();

        // Music
        musicPlaying = saveState.musicPlaying;
        updateMusicTooltip();

        // Zen mode
        if (saveState.zenMode && !zenMode) toggleZenMode();
    }

    private void snapshotToSaveState() {
        if (saveState == null) saveState = new GameState();

        saveState.theme = ThemeManager.get().getCurrent().name();
        saveState.musicPlaying = musicPlaying;
        saveState.zenMode = zenMode;

        saveState.timerRunning = timerRunning;
        saveState.elapsedSeconds = elapsedSeconds;

        saveState.currentCollection = currentCollection;

        if (imageComboBox != null) {
            saveState.selectedJigsawIndex = imageComboBox.getSelectedIndex();
            Object sel = imageComboBox.getSelectedItem();
            saveState.selectedJigsawPath = (sel != null) ? sel.toString() : null;
        }
    }

    private String getDifficultyLabel() {
        int r = puzzlePanel.getPuzzleRows();
        int c = puzzlePanel.getPuzzleCols();
        if (r == 2 && c == 2) return "EASY";
        if (r == 3 && c == 3) return "MEDIUM";
        if (r == 4 && c == 4) return "HARD";
        return "CUSTOM";
    }

    private void markCurrentJigsawCompleted() {
        if (saveState == null) saveState = new GameState();
        if (saveState.progress == null) saveState.progress = new java.util.HashMap<>();

        // Identify the current jigsaw uniquely (path is fine as a key)
        String jigsawPath = null;
        if (imageComboBox != null) {
            Object sel = imageComboBox.getSelectedItem();
            if (sel != null) jigsawPath = sel.toString();
        }
        if (jigsawPath == null || jigsawPath.isBlank()) return;

        // Fetch or create progress entry
        GameState.ProgressEntry entry = saveState.progress.get(jigsawPath);
        if (entry == null) {
            entry = new GameState.ProgressEntry();
            saveState.progress.put(jigsawPath, entry);
        }

        // Mark completion
        entry.completed = true;

        // Attempts / stats (optional but useful)
        entry.attempts = Math.max(entry.attempts, 0) + 1;

        // Best time (optional)
        int time = Math.max(0, elapsedSeconds);
        if (time > 0 && (entry.bestTimeSeconds <= 0 || time < entry.bestTimeSeconds)) {
            entry.bestTimeSeconds = time;
        }

        // Save current context (useful for UI later)
        entry.collectionPath = currentCollection;
        entry.lastPlayedEpoch = System.currentTimeMillis();

        entry.bestDifficulty = getDifficultyLabel();

        // Also keep top-level “last state” up to date
        snapshotToSaveState();
        saveState.difficulty.mode = getDifficultyLabel();
        saveState.difficulty.rows = puzzlePanel.getPuzzleRows();
        saveState.difficulty.cols = puzzlePanel.getPuzzleCols();

    }


    // ---------- Background ----------

    private void applyBackgroundForCurrentState() {
        Theme theme = ThemeManager.get().getCurrent();
        String path = BackgroundCatalog.backgroundFor(currentCollection, theme);

        try (var in = FixThePotGamePanel.class.getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Missing resource: " + path);
            backgroundImage = ImageIO.read(in);

            // Optional blur (can be removed later)
            // blurredBackground = BlurUtil.blurImage(backgroundImage, 5);
        } catch (Exception e) {
            e.printStackTrace();
            backgroundImage = null;
            blurredBackground = null;
        }

        repaint();
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

    // ---------- UI creation ----------

    private void createPuzzlePanel() {
        puzzlePanel = new FixThePotGame();
        puzzlePanel.setOpaque(false);
        puzzlePanel.setPreferredSize(new Dimension(800, 500));

        layeredPane = new JLayeredPane();
        layeredPane.setOpaque(false);

        puzzlePanel.setBounds(0, 0, 800, 500);
        layeredPane.add(puzzlePanel, JLayeredPane.DEFAULT_LAYER);

        add(layeredPane, BorderLayout.CENTER);
    }

    @Override
    public void doLayout() {
        super.doLayout();

        if (layeredPane != null) {
            Dimension lpSize = layeredPane.getSize();

            if (puzzlePanel != null) {
                puzzlePanel.setBounds(0, 0, lpSize.width, lpSize.height);
            }

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

    private static ImageIcon icon(String resourcePath) {
        java.net.URL url = FixThePotGamePanel.class.getResource(resourcePath);
        if (url == null) throw new RuntimeException("Missing resource: " + resourcePath);
        return new ImageIcon(url);
    }

    private static ImageIcon scaledIcon(String resourcePath, int w, int h) {
        Image img = icon(resourcePath).getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    private void buildControlPanel() {
        controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        controlPanel.setOpaque(false);

        // Combo box
        imageComboBox = new JComboBox<>(imageOptions);
        imageComboBox.setToolTipText("Pick a Jigsaw");
        imageComboBox.addActionListener(e -> {
            String selectedImage = (String) imageComboBox.getSelectedItem();
            if (selectedImage != null) {
                puzzlePanel.setImage(selectedImage);
                if (nextJigsawButton != null) nextJigsawButton.setEnabled(false);
            }
            snapshotToSaveState();
            SaveManager.save(saveState);
            updateFavouriteTooltip();
        });

        // Previous
        previousJigsawButton = new JButton(scaledIcon("/Buttons/left.png", 24, 24));
        previousJigsawButton.setToolTipText("Previous Jigsaw (←)");
        previousJigsawButton.setFocusable(false);
        previousJigsawButton.addActionListener(e -> {
            int itemCount = imageComboBox.getItemCount();
            if (itemCount <= 1) return;
            int currentIndex = imageComboBox.getSelectedIndex();
            int prevIndex = (currentIndex - 1 + itemCount) % itemCount;
            imageComboBox.setSelectedIndex(prevIndex);
            String selectedPath = (String) imageComboBox.getItemAt(prevIndex);
            puzzlePanel.setImage(selectedPath);
            hideInfoOverlay();
        });

        // Hint button
        JButton hintButton = new JButton(scaledIcon("/Buttons/hint.png", 24, 24)); // reuse icon for now
        hintButton.setToolTipText("Show hint (h)");
        hintButton.setFocusable(false);
        hintButton.addActionListener(e -> {
            int level = puzzlePanel.cycleHint();
            switch (level) {
                case 0: hintButton.setToolTipText("Show hint (h)"); break;
                case 1: hintButton.setToolTipText("Hint: edges only (h)"); break;
                case 2: hintButton.setToolTipText("Hint: corners only (h)"); break;
                case 3: hintButton.setToolTipText("Hint: placement guide (h)"); break;
            }
        });

        // Restart
        restartButton = new JButton(scaledIcon("/Buttons/restart.png", 24, 24));
        restartButton.setToolTipText("Restart Jigsaw (r)");
        restartButton.addActionListener(e -> {
            puzzlePanel.restartGame();
            elapsedSeconds = 0;
            if (timerButton != null) timerButton.setText("Time: 0 s");
            if (nextJigsawButton != null) nextJigsawButton.setEnabled(false);

            snapshotToSaveState();
            SaveManager.save(saveState);
        });

        // Show completed
        showCompletedButton = new JButton(scaledIcon("/Buttons/show_completed.png", 24, 24));
        updateShowCompletedTooltip();
        showCompletedButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
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
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (peekHideTimer != null) peekHideTimer.restart();
            }
        });
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

        // Split chooser
        jigsawSplitButton = new JButton(scaledIcon("/Buttons/jigsaw_split.jpeg", 24, 24));
        jigsawSplitButton.setToolTipText("Pick the amount of Jigsaw Pieces");
        jigsawSplitButton.addActionListener(e -> {
            if (splitOverlay == null) {
                splitOverlay = new SplitChooserOverlay();
                splitOverlay.setOnCustom(() -> {
                    splitOverlay.close();
                    if (glassPaneRef != null) glassPaneRef.repaint();

                    if (rowsColsOverlay == null) rowsColsOverlay = new RowsColsOverlay();
                    rowsColsOverlay.open(glassPaneRef, (rows, cols) -> {
                        puzzlePanel.setDifficulty(rows, cols);
                        rowsColsOverlay.close();
                        if (glassPaneRef != null) glassPaneRef.repaint();
                    });
                });
            }
            if (splitOverlay.isShowing()) {
                splitOverlay.close();
                if (glassPaneRef != null) glassPaneRef.repaint();
                return;
            }
            splitOverlay.openBottom(glassPaneRef, (rows, cols) -> {
                puzzlePanel.setDifficulty(rows, cols);
                splitOverlay.close();
                if (glassPaneRef != null) glassPaneRef.repaint();
            });
        });

        // Theme
        themeButton = new JButton();
        themeButton.setFocusable(false);
        themeButton.setPreferredSize(new Dimension(36, 36));
        updateThemeButtonIcon();
        themeButton.addActionListener(e -> {
            ThemeManager.get().toggleTheme();
            updateThemeButtonIcon();
            applyBackgroundForCurrentState();
            ThemeManager.refreshThemeTree(this);
            if (glassPaneRef != null) ThemeManager.refreshThemeTree(glassPaneRef);

            snapshotToSaveState();
            SaveManager.save(saveState);
        });

        // Info
        extraInfoButton = new JButton(scaledIcon("/Buttons/information.png", 24, 24));
        updateInfoTooltip();
        extraInfoButton.addActionListener(e -> {
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
                        icon(selectedImage)
                );
                extraInfoButton.setToolTipText("Extra information (i)");
                return;
            }

            if (persistentOverlay.isVisible()) {
                persistentOverlay.close();
                if (glassPaneRef != null) glassPaneRef.repaint();
                updateInfoTooltip();
                return;
            }

            String selectedImage = (String) imageComboBox.getSelectedItem();
            if (selectedImage == null || !imageInfoMap.containsKey(selectedImage)) {
                JOptionPane.showMessageDialog(FixThePotGamePanel.this, "No extra information available.");
                return;
            }
            ImageInfo info = imageInfoMap.get(selectedImage);

            ImageIcon previewIcon = icon(selectedImage);
            String titleText = "Artifact: " + ArtifactCatalog.displayName(selectedImage);
            String fullMuseumDescription = info.getDescription();

            persistentOverlay.updateContent(previewIcon, titleText, fullMuseumDescription, info.getUrl());

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

        // Timer button
        timerButton = new JButton(timerRunning ? "Time: " + elapsedSeconds + " s" : "Paused: " + elapsedSeconds + " s");
        timerButton.setPreferredSize(new Dimension(120, 36));
        timerButton.setFocusPainted(false);
        updateTimerTooltip();
        timerButton.addActionListener(e -> {
            if (timerRunning) {
                if (timer != null) timer.stop();
                timerRunning = false;
                timerButton.setText("Paused: " + elapsedSeconds + " s");
            } else {
                if (timer != null) timer.start();
                timerRunning = true;
                timerButton.setText("Time: " + elapsedSeconds + " s");
            }
            updateTimerTooltip();

            snapshotToSaveState();
            SaveManager.save(saveState);
        });

        // Music (kept minimal here)
        String[] musicTracks = {
                "Lukrembo - Bread (freetouse.com).mp3",
                "John-Bartmann-Another-Grappa-Monsieur_(chosic.com).mp3",
                "scott-buckley-permafrost(chosic.com).mp3",
                "John-Bartmann-Allez-Allez(chosic.com).mp3"
        };

        currentTrackname = musicTracks[0];
        musicPlayer = new MusicPlayer(musicFolderPath + currentTrackname);

        musicToggleButton = new JButton(scaledIcon("/Buttons/music note.png", 24, 24));
        updateMusicTooltip();
        musicToggleButton.addActionListener(e -> {
            if (musicPlayer != null) {
                musicPlayer.togglePlayPause();
                musicPlaying = !musicPlaying;
                updateMusicTooltip();

                snapshotToSaveState();
                SaveManager.save(saveState);
            }
        });

        JButton chooseTrackButton = new JButton(scaledIcon("/Buttons/music_track.png", 24, 24));
        chooseTrackButton.setToolTipText("Select a music track");
        final MusicChooserPopover[] musicPopoverRef = new MusicChooserPopover[1];
        chooseTrackButton.addActionListener(e -> {
            if (glassPaneRef == null) return;
            if (musicPopoverRef[0] != null && musicPopoverRef[0].isShowing()) {
                musicPopoverRef[0].close();
                glassPaneRef.repaint();
                return;
            }

            musicPopoverRef[0] = new MusicChooserPopover();
            musicPopoverRef[0].openAbove(glassPaneRef, chooseTrackButton, musicTracks, pickedFile -> {
                if (musicPlayer != null) musicPlayer.stopPlayback();
                currentTrackname = pickedFile;
                musicPlayer = new MusicPlayer(musicFolderPath + pickedFile);
                musicPlayer.play();

                musicPopoverRef[0].close();
                glassPaneRef.repaint();

                snapshotToSaveState();
                SaveManager.save(saveState);
            });
        });

        // Back to collections
        backToCollectionsButton = new JButton(scaledIcon("/Buttons/collections.png", 24, 24));
        backToCollectionsButton.setToolTipText("Return to collection selection");
        backToCollectionsButton.addActionListener(e -> {
            if (gamePanelListener != null) gamePanelListener.onBackToCollections();
        });

        // Choose jigsaw popover
        chooseJigsawButton = new JButton(scaledIcon("/Buttons/choose.png", 24, 24));
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
                String heart = (saveState != null && saveState.isFavourite(path)) ? "\u2665 " : "";
                display.add(heart + ArtifactCatalog.displayName(path));
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

                        snapshotToSaveState();
                        SaveManager.save(saveState);
                    }
            );
        });

        // Next
        nextJigsawButton = new JButton(scaledIcon("/Buttons/right.png", 24, 24));
        nextJigsawButton.setToolTipText("Next Jigsaw (→)");
        nextJigsawButton.setEnabled(false);
        nextJigsawButton.setFocusable(false);
        nextJigsawButton.addActionListener(e -> {
            int itemCount = imageComboBox.getItemCount();
            if (itemCount <= 1) return;
            int currentIndex = imageComboBox.getSelectedIndex();
            int nextIndex = (currentIndex + 1) % itemCount;
            imageComboBox.setSelectedIndex(nextIndex);
            String selectedPath = (String) imageComboBox.getItemAt(nextIndex);
            puzzlePanel.setImage(selectedPath);
            nextJigsawButton.setEnabled(false);
            hideInfoOverlay();

            snapshotToSaveState();
            SaveManager.save(saveState);
        });

        // Zen
        zenModeButton = new JButton(scaledIcon("/Buttons/zen.png", 24, 24));
        zenModeButton.setToolTipText("Toggle Zen mode (z)");
        zenModeButton.setFocusPainted(false);
        zenModeButton.setBackground(new Color(28, 28, 28));
        zenModeButton.setForeground(Color.WHITE);
        zenModeButton.addActionListener(e -> {
            toggleZenMode();
            snapshotToSaveState();
            SaveManager.save(saveState);
        });

        // Favourite toggle (heart)
        favouriteToggleButton = new JButton(scaledIcon("/Buttons/favourites.jpg", 24, 24));
        updateFavouriteTooltip();
        favouriteToggleButton.setFocusable(false);
        favouriteToggleButton.addActionListener(e -> {
            if (saveState == null || imageComboBox == null) return;
            Object sel = imageComboBox.getSelectedItem();
            if (sel == null) return;
            saveState.toggleFavourite(sel.toString());
            updateFavouriteTooltip();
            SaveManager.save(saveState);
        });

        // Add controls
        controlPanel.add(previousJigsawButton);
        controlPanel.add(backToCollectionsButton);
        controlPanel.add(musicToggleButton);
        controlPanel.add(chooseTrackButton);
        controlPanel.add(restartButton);
        controlPanel.add(extraInfoButton);
        controlPanel.add(zenModeButton);
        controlPanel.add(timerButton);
        controlPanel.add(themeButton);
        controlPanel.add(hintButton);
        controlPanel.add(showCompletedButton);
        controlPanel.add(jigsawSplitButton);
        controlPanel.add(chooseJigsawButton);
        controlPanel.add(favouriteToggleButton);
        controlPanel.add(nextJigsawButton);

        add(controlPanel, BorderLayout.SOUTH);

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
                hintButton,
                favouriteToggleButton
        };
    }

    // ---------- Overlays ----------

    private void hideInfoOverlay() {
        if (glassPaneRef != null && persistentOverlay != null) {
            glassPaneRef.setVisible(false);
            glassPaneRef.remove(persistentOverlay);
            glassPaneRef.revalidate();
            glassPaneRef.repaint();
        }
    }

    private void initCompletedOverlays() {
        if (completedOverlay != null) return;

        JRootPane root = SwingUtilities.getRootPane(FixThePotGamePanel.this);
        if (root == null) return;
        JLayeredPane layered = root.getLayeredPane();

        completedOverlay = new ImageOverlay();
        layered.add(completedOverlay, JLayeredPane.POPUP_LAYER);
        completedOverlay.setVisible(false);

        completedPeek = new ImagePeek();
        layered.add(completedPeek, JLayeredPane.POPUP_LAYER);
        completedPeek.setVisible(false);

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

        peekHideTimer = new javax.swing.Timer(250, e -> completedPeek.setVisible(false));
        peekHideTimer.setRepeats(false);
    }

    // ---------- Tooltips / Theme icon ----------

    private void updateFavouriteTooltip() {
        if (favouriteToggleButton == null || imageComboBox == null) return;
        Object sel = imageComboBox.getSelectedItem();
        boolean isFav = sel != null && saveState != null && saveState.isFavourite(sel.toString());
        String iconPath = isFav ? "/Buttons/Filled_favourites.png" : "/Buttons/favourites.jpg";
        favouriteToggleButton.setIcon(scaledIcon(iconPath, 24, 24));
        favouriteToggleButton.setToolTipText(isFav ? "Unfavourite (f)" : "Favourite (f)");
    }

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

        String themeIconPath = isDark ? "/Buttons/dark_mode.png" : "/Buttons/light_mode.png";

        themeButton.setIcon(scaledIcon(themeIconPath, 24, 24));
        themeButton.setText(null);
        themeButton.setPreferredSize(new Dimension(36, 36));
        themeButton.setToolTipText(isDark ? "Switch to light mode" : "Switch to dark mode");
    }

    // ---------- Zen / Timer / Collection ----------

    private void toggleZenMode() {
        zenMode = !zenMode;

        if (zenHideComponents != null) {
            for (JComponent c : zenHideComponents) {
                if (c != null) c.setVisible(!zenMode);
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

        if (controlPanel != null) {
            if (zenMode) {
                controlPanel.setOpaque(true);
                controlPanel.setBackground(new Color(28, 28, 28));
            } else {
                controlPanel.setOpaque(false);
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

    private void updateFavouriteButtonState() {
        if (favouriteToggleButton == null || imageComboBox == null) return;
        Object sel = imageComboBox.getSelectedItem();
        boolean isFav = sel != null && saveState != null && saveState.isFavourite(sel.toString());
        favouriteToggleButton.setText(isFav ? "\u2665" : "\u2661"); // filled vs hollow heart
        favouriteToggleButton.setToolTipText(isFav ? "Unfavourite (f)" : "Favourite (f)");
    }



    private void setupTimer() {
        timer = new Timer(1000, e -> {
            elapsedSeconds++;
            if (timerButton != null) timerButton.setText("Time: " + elapsedSeconds + " s");
        });

        if (timerRunning) timer.start();
    }

    public void loadCollection(String collectionName) {
        this.currentCollection = collectionName;
        applyBackgroundForCurrentState();

        imageComboBox.removeAllItems();
        for (String img : ArtifactCatalog.imagesFor(collectionName)) {
            imageComboBox.addItem(img);
        }

        if (imageComboBox.getItemCount() > 0) {
            imageComboBox.setSelectedIndex(0);
            puzzlePanel.setImage((String) imageComboBox.getItemAt(0));
        }

        if (nextJigsawButton != null) nextJigsawButton.setEnabled(false);
        hideInfoOverlay();

        snapshotToSaveState();
        SaveManager.save(saveState);
    }

    /** Select a specific jigsaw by its resource path. */
    public void selectJigsaw(String jigsawPath) {
        if (imageComboBox == null || jigsawPath == null) return;
        for (int i = 0; i < imageComboBox.getItemCount(); i++) {
            if (jigsawPath.equals(imageComboBox.getItemAt(i))) {
                imageComboBox.setSelectedIndex(i);
                puzzlePanel.setImage(jigsawPath);
                if (nextJigsawButton != null) nextJigsawButton.setEnabled(false);
                hideInfoOverlay();
                break;
            }
        }
    }



    // ---------- Keyboard shortcuts ----------

    private void setupKeyboardShortcuts() {
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        java.util.function.BiConsumer<String, Runnable> bind = (key, run) -> {
            im.put(KeyStroke.getKeyStroke(key), key);
            am.put(key, new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { run.run(); }
            });
        };

        bind.accept("LEFT", () -> { if (previousJigsawButton != null) previousJigsawButton.doClick(); });
        bind.accept("RIGHT", () -> { if (nextJigsawButton != null) nextJigsawButton.doClick(); });
        bind.accept("R", () -> { if (restartButton != null) restartButton.doClick(); });
        bind.accept("I", () -> { if (extraInfoButton != null) extraInfoButton.doClick(); });
        bind.accept("C", () -> { if (showCompletedButton != null) showCompletedButton.doClick(); });
        bind.accept("Z", () -> { if (zenModeButton != null) zenModeButton.doClick(); });
        bind.accept("T", () -> { if (timerButton != null) timerButton.doClick(); });
        bind.accept("M", () -> { if (musicToggleButton != null) musicToggleButton.doClick(); });
        bind.accept("F", () -> { if (favouriteToggleButton != null) favouriteToggleButton.doClick(); });
        bind.accept("H", () -> { if (hintButton != null) hintButton.doClick(); });

        bind.accept("ESCAPE", () -> {
            boolean changed = false;

            if (persistentOverlay != null && persistentOverlay.isVisible()) {
                persistentOverlay.close();
                changed = true;
                updateInfoTooltip();
            }
            if (completedOverlay != null && completedOverlay.isVisible()) {
                completedOverlay.setVisible(false);
                changed = true;
                updateShowCompletedTooltip();
            }
            if (splitOverlay != null && splitOverlay.isShowing()) {
                splitOverlay.close();
                changed = true;
            }
            if (rowsColsOverlay != null && rowsColsOverlay.isShowing()) {
                rowsColsOverlay.close();
                changed = true;
            }

            if (changed && glassPaneRef != null) glassPaneRef.repaint();
            repaint();
        });
    }
}
