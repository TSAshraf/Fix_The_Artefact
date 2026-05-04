import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import javax.imageio.ImageIO;

public class FixThePotGamePanel extends JPanel implements ThemeAware {

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
    private JButton helpButton;
    private JButton settingsButton;
    private JButton profileButton;
    private JButton favouritesListButton;
    private SettingsOverlay settingsOverlay;

    // Registry of every customisable control bar button keyed by stable ID (HE-23).
    private final java.util.LinkedHashMap<String, JButton> buttonsById = new java.util.LinkedHashMap<>();
    // Human-readable label for each button ID, shown in the SettingsOverlay.
    private final java.util.LinkedHashMap<String, String> buttonLabels = new java.util.LinkedHashMap<>();
    // Default order used when the saved buttonOrder is empty or when new buttons appear after upgrade.
    private static final java.util.List<String> DEFAULT_BUTTON_ORDER = java.util.List.of(
            "previous", "back", "profile", "favouritesList", "music", "trackChooser", "restart", "info",
            "zen", "timer", "theme", "hint", "showCompleted", "split",
            "chooser", "favourite", "help", "settings", "next"
    );

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

    // Toolbar icon size in px. Loaded from saveState and used everywhere a toolbar
    // icon is scaled so the Settings slider can apply at runtime.
    private int toolbarIconSize = 24;

    // Auto-hide toolbar (Tier 3): hide the control panel unless the cursor
    // is within a strip at the bottom of the game area. Position-based rather
    // than timer-based so the user gets deterministic "reach down to reveal"
    // behaviour without flicker as they drag pieces.

    // Height in px of the sensitive strip at the bottom that reveals the toolbar on hover.
    // Kept small so the toolbar doesn't pop up every time the cursor drifts near the bottom half of the puzzle,
    // users have to deliberately reach down for it.

    private static final int TOOLBAR_REVEAL_ZONE_PX = 35;
    private boolean toolbarHidden = false;

    private GameState saveState;

    private ReflectionPromptOverlay reflectionOverlay;
    private boolean reflectionPromptsReady = false; // suppress during initial load
    private int lastXpEarned = 0;
    private java.util.List<String> lastAchievementNames = new java.util.ArrayList<>();
    private boolean pendingCompletionToast = false;

    // Micro narrative label (fades in at top of game area)
    private JLabel narrativeLabel;
    private javax.swing.Timer narrativeFadeTimer;

    public interface GamePanelListener {
        void onBackToCollections();
        void onJumpToProfile();
        void onJumpToFavourites();
    }
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
            persistSaveState();

            // If the tutorial is still running, dismiss it now,
            // the user has demonstrated they understand the game mechanics,
            // so the remaining in-game tutorial steps are redundant and would compete with the
            // post-puzzle reflection prompt for screen space.

            Window w = SwingUtilities.getWindowAncestor(this);
            if (w instanceof MainFrame) {
                ((MainFrame) w).notifyPuzzleSolved();
            }

            // Show post-puzzle completion overlay
            showPostReflectionPrompt();
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

                reflectionPromptsReady = true;

                f.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override public void windowClosing(java.awt.event.WindowEvent e) {
                        snapshotToSaveState();
                        persistSaveState();
                    }
                });
            }
        });
    }

    // Save/load helpers
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

        // Toolbar icon size, clamped to the supported set so a corrupted save can't produce a 3px or 300px icon.
        int s = saveState.toolbarIconSize;
        toolbarIconSize = (s == 20 || s == 24 || s == 28 || s == 32) ? s : 24;
    }

    private void restoreUIStateAfterControlsCreated() {
        if (saveState == null) return;

        // Difficulty (set before setImage so piece regeneration uses the restored grid)
        if (saveState.difficulty != null
                && saveState.difficulty.rows > 0
                && saveState.difficulty.cols > 0) {
            puzzlePanel.setDifficulty(saveState.difficulty.rows, saveState.difficulty.cols);
        }

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

        // Assembly-area colour matching the current mode
        refreshAssemblyAreaColor();

        // Auto-hide toolbar wiring, listeners attach unconditionally so the
        // feature can be toggled at runtime from Settings without a relaunch.
        installAutoHideListeners();
        if (saveState.autoHideToolbar) hideToolbar();
    }

    // Listen for mouse activity across the game area.
    // Each event is converted to panel-local coordinates and routed to onMouseActivity,
    // which picks show/hide based on whether the cursor is within the reveal zone.

    private void installAutoHideListeners() {
        java.awt.event.MouseAdapter ma = new java.awt.event.MouseAdapter() {
            @Override public void mouseMoved(java.awt.event.MouseEvent e)   { onMouseActivity(e, e.getComponent()); }
            @Override public void mouseDragged(java.awt.event.MouseEvent e) { onMouseActivity(e, e.getComponent()); }
            @Override public void mousePressed(java.awt.event.MouseEvent e) { onMouseActivity(e, e.getComponent()); }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { onMouseActivity(e, e.getComponent()); }
        };
        addMouseMotionListener(ma);
        addMouseListener(ma);
        if (puzzlePanel != null) {
            puzzlePanel.addMouseMotionListener(ma);
            puzzlePanel.addMouseListener(ma);
        }
        if (controlPanel != null) {
            controlPanel.addMouseMotionListener(ma);
            controlPanel.addMouseListener(ma);
        }
    }

    private void snapshotToSaveState() {
        if (saveState == null) saveState = new GameState();

        saveState.theme = ThemeManager.get().getCurrent().name();
        saveState.musicPlaying = musicPlaying;
        saveState.currentMusicTrack = currentTrackname != null ? currentTrackname : "";
        saveState.zenMode = zenMode;

        // Difficulty (mode label is also written on completion in markCurrentJigsawCompleted,
        // but mirroring it here keeps any save call consistent with the live puzzle state)
        if (puzzlePanel != null) {
            saveState.difficulty.rows = puzzlePanel.getPuzzleRows();
            saveState.difficulty.cols = puzzlePanel.getPuzzleCols();
            saveState.difficulty.mode = getDifficultyLabel();
        }

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

        entry.completed = true; // Mark completion
        entry.attempts = Math.max(entry.attempts, 0) + 1; // Attempts / stats

        // Best time (optional)
        int time = Math.max(0, elapsedSeconds);
        if (time > 0 && (entry.bestTimeSeconds <= 0 || time < entry.bestTimeSeconds)) {
            entry.bestTimeSeconds = time;
        }

        // Save current context (useful for UI later)
        entry.collectionPath = currentCollection;
        entry.lastPlayedEpoch = System.currentTimeMillis();

        entry.bestDifficulty = getDifficultyLabel();

        // Also keep top-level "last state" up to date
        snapshotToSaveState();
        saveState.difficulty.mode = getDifficultyLabel();
        saveState.difficulty.rows = puzzlePanel.getPuzzleRows();
        saveState.difficulty.cols = puzzlePanel.getPuzzleCols();

        // Award XP
        String diff = getDifficultyLabel();
        int rows = puzzlePanel.getPuzzleRows();
        int cols = puzzlePanel.getPuzzleCols();
        lastXpEarned = AchievementManager.awardXp(saveState, diff, rows, cols);

        // Check achievements, resolve names for display
        java.util.List<String> newIds = AchievementManager.checkAchievements(saveState);
        lastAchievementNames = new java.util.ArrayList<>();
        for (String id : newIds) {
            AchievementManager.Achievement a = AchievementManager.ALL.get(id);
            if (a != null) lastAchievementNames.add(a.name);
        }

        // Save
        persistSaveState();
    }


    // Background
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

        // Background dim overlay, cheap per-frame cost (one fillRect) and
        // preserves the themed imagery underneath, unlike replacing it with a
        // solid colour. 0 = no overlay, 80 = heavily darkened. Added in
        // response to participant feedback that the themed backgrounds were
        // visually noisy during active gameplay.

        int dim = (saveState != null) ? saveState.backgroundDim : 0;
        if (dim > 0) {
            int alpha = (int) Math.round(Math.min(80, dim) * 255.0 / 100.0);
            g.setColor(new Color(0, 0, 0, alpha));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    // UI creation
    private void createPuzzlePanel() {
        puzzlePanel = new FixThePotGame();
        puzzlePanel.setOpaque(false);
        puzzlePanel.setPreferredSize(new Dimension(920, 575));

        layeredPane = new JLayeredPane();
        layeredPane.setOpaque(false);

        puzzlePanel.setBounds(0, 0, 920, 575);
        layeredPane.add(puzzlePanel, JLayeredPane.DEFAULT_LAYER);

        // Micro narrative label (floats at top centre of puzzle area)
        narrativeLabel = new JLabel("", SwingConstants.CENTER);
        narrativeLabel.setOpaque(true);
        narrativeLabel.setBackground(new Color(0, 0, 0, 180));
        narrativeLabel.setForeground(ThemeManager.get().palette().base.narrativeText);
        narrativeLabel.setFont(new Font(ThemeManager.get().palette().fonts.caption.getFamily(), Font.ITALIC, 14));
        narrativeLabel.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        narrativeLabel.setVisible(false);
        layeredPane.add(narrativeLabel, JLayeredPane.PALETTE_LAYER);

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

            if (narrativeLabel != null && narrativeLabel.isVisible()) {
                Dimension pref = narrativeLabel.getPreferredSize();
                int nw = Math.min(pref.width + 32, lpSize.width - 40);
                int nx = (lpSize.width - nw) / 2;
                narrativeLabel.setBounds(nx, 12, nw, pref.height);
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
                dismissAllOverlays();
                puzzlePanel.setImage(selectedImage);
                if (nextJigsawButton != null) nextJigsawButton.setEnabled(false);
                // Micro narrative + reflection prompt
                showMicroNarrative(selectedImage);
                showPreReflectionPrompt();
            }
            snapshotToSaveState();
            persistSaveState();
            updateFavouriteTooltip();
        });

        // Previous
        previousJigsawButton = new JButton(scaledIcon("/Buttons/left.png", toolbarIconSize, toolbarIconSize));
        previousJigsawButton.setToolTipText("Previous Jigsaw (←)");
        previousJigsawButton.setFocusable(false);
        previousJigsawButton.addActionListener(e -> {
            int itemCount = imageComboBox.getItemCount();
            if (itemCount <= 1) return;
            dismissAllOverlays();
            int currentIndex = imageComboBox.getSelectedIndex();
            int prevIndex = (currentIndex - 1 + itemCount) % itemCount;
            imageComboBox.setSelectedIndex(prevIndex);
            String selectedPath = (String) imageComboBox.getItemAt(prevIndex);
            puzzlePanel.setImage(selectedPath);
        });

        // Hint button, assigns the field (line 31), not a local that would shadow it.
        // Fixing this also makes the G keyboard shortcut actually work.
        hintButton = new JButton(scaledIcon("/Buttons/hint.png", toolbarIconSize, toolbarIconSize));
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
        restartButton = new JButton(scaledIcon("/Buttons/restart.png", toolbarIconSize, toolbarIconSize));
        restartButton.setToolTipText("Restart Jigsaw (r)");
        restartButton.addActionListener(e -> {
            puzzlePanel.restartGame();
            elapsedSeconds = 0;
            if (timerButton != null) timerButton.setText("Time: 0 s");
            if (nextJigsawButton != null) nextJigsawButton.setEnabled(false);

            snapshotToSaveState();
            persistSaveState();

            // Re-show pre-puzzle prompt on restart
            showPreReflectionPrompt();
        });

        // Show completed
        showCompletedButton = new JButton(scaledIcon("/Buttons/show_completed.png", toolbarIconSize, toolbarIconSize));
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
        jigsawSplitButton = new JButton(scaledIcon("/Buttons/jigsaw_split.jpeg", toolbarIconSize, toolbarIconSize));
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
            persistSaveState();
        });

        // Info
        extraInfoButton = new JButton(scaledIcon("/Buttons/information.png", toolbarIconSize, toolbarIconSize));
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
            persistSaveState();
        });

        // Music (kept minimal here)
        String[] musicTracks = {
                "Lukrembo - Bread (freetouse.com).mp3",
                "John-Bartmann-Another-Grappa-Monsieur_(chosic.com).mp3",
                "scott-buckley-permafrost(chosic.com).mp3",
                "John-Bartmann-Allez-Allez(chosic.com).mp3"
        };

        // Restore the saved track if it's one we still ship, otherwise fall back to default
        String savedTrack = (saveState != null) ? saveState.currentMusicTrack : null;
        currentTrackname = musicTracks[0];
        if (savedTrack != null && !savedTrack.isBlank()) {
            for (String t : musicTracks) {
                if (t.equals(savedTrack)) { currentTrackname = savedTrack; break; }
            }
        }

        musicPlayer = createMusicPlayerSafely(musicFolderPath + currentTrackname);

        musicToggleButton = new JButton(scaledIcon("/Buttons/music note.png", toolbarIconSize, toolbarIconSize));
        updateMusicTooltip();
        musicToggleButton.addActionListener(e -> {
            if (musicPlayer != null) {
                musicPlayer.togglePlayPause();
                musicPlaying = !musicPlaying;
                updateMusicTooltip();

                snapshotToSaveState();
                persistSaveState();
            }
        });

        JButton chooseTrackButton = new JButton(scaledIcon("/Buttons/music_track.png", toolbarIconSize, toolbarIconSize));
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
                String previousTrack = currentTrackname;
                currentTrackname = pickedFile;
                MusicPlayer newPlayer = createMusicPlayerSafely(musicFolderPath + pickedFile);
                if (newPlayer != null) {
                    musicPlayer = newPlayer;
                    musicPlayer.play();
                    musicPlaying = true;
                } else {
                    // Restore previous selection so the UI reflects what is actually playing (nothing)
                    currentTrackname = previousTrack;
                    musicPlayer = null;
                    musicPlaying = false;
                }
                updateMusicTooltip();

                musicPopoverRef[0].close();
                glassPaneRef.repaint();

                snapshotToSaveState();
                persistSaveState();
            });
        });

        // Back to collections
        backToCollectionsButton = new JButton(scaledIcon("/Buttons/collections.png", toolbarIconSize, toolbarIconSize));
        backToCollectionsButton.setToolTipText("Return to collection selection");
        backToCollectionsButton.addActionListener(e -> {
            dismissAllOverlays();
            if (gamePanelListener != null) gamePanelListener.onBackToCollections();
        });

        // Choose jigsaw popover
        chooseJigsawButton = new JButton(scaledIcon("/Buttons/choose.png", toolbarIconSize, toolbarIconSize));
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
                        persistSaveState();
                    }
            );
        });

        // Next
        nextJigsawButton = new JButton(scaledIcon("/Buttons/right.png", toolbarIconSize, toolbarIconSize));
        nextJigsawButton.setToolTipText("Next Jigsaw (→)");
        nextJigsawButton.setEnabled(false);
        nextJigsawButton.setFocusable(false);
        nextJigsawButton.addActionListener(e -> {
            int itemCount = imageComboBox.getItemCount();
            if (itemCount <= 1) return;
            dismissAllOverlays();
            int currentIndex = imageComboBox.getSelectedIndex();
            int nextIndex = (currentIndex + 1) % itemCount;
            imageComboBox.setSelectedIndex(nextIndex);
            String selectedPath = (String) imageComboBox.getItemAt(nextIndex);
            puzzlePanel.setImage(selectedPath);
            nextJigsawButton.setEnabled(false);

            snapshotToSaveState();
            persistSaveState();
        });

        // Zen
        zenModeButton = new JButton(scaledIcon("/Buttons/zen.png", toolbarIconSize, toolbarIconSize));
        zenModeButton.setToolTipText("Toggle Zen mode (z)");
        zenModeButton.setFocusPainted(false);
        zenModeButton.setBackground(new Color(28, 28, 28));
        zenModeButton.setForeground(Color.WHITE);
        zenModeButton.addActionListener(e -> {
            toggleZenMode();
            snapshotToSaveState();
            persistSaveState();
        });

        // Favourite toggle (heart)
        favouriteToggleButton = new JButton(scaledIcon("/Buttons/favourites.jpg", toolbarIconSize, toolbarIconSize));
        updateFavouriteTooltip();
        favouriteToggleButton.setFocusable(false);
        favouriteToggleButton.addActionListener(e -> {
            if (saveState == null || imageComboBox == null) return;
            Object sel = imageComboBox.getSelectedItem();
            if (sel == null) return;
            saveState.toggleFavourite(sel.toString());
            updateFavouriteTooltip();
            persistSaveState();
        });

        // Help button, opens HelpOverlay listing all keyboard shortcuts and tier behaviours
        helpButton = new JButton(scaledIcon("/Buttons/guide.png", toolbarIconSize, toolbarIconSize));
        helpButton.setToolTipText("Help & shortcuts (?)");
        helpButton.setFocusable(false);
        helpButton.addActionListener(e -> showSettingsOverlayAtTab(SettingsOverlay.TAB_HELP));

        // Profile button, one-click jump to the profile screen from the game panel.
        // Participant feedback (P4) asked for quick access to profile/favourites
        // without stepping back through Collections.
        profileButton = new JButton(scaledIcon("/Buttons/Profile_Icon.png", toolbarIconSize, toolbarIconSize));
        profileButton.setToolTipText("Open your profile");
        profileButton.setFocusable(false);
        profileButton.addActionListener(e -> {
            dismissAllOverlays();
            if (gamePanelListener != null) gamePanelListener.onJumpToProfile();
        });

        // Favourites-list button, one-click jump to the Favourites screen so users
        // don't have to step back through Collections to browse their saved picks.
        favouritesListButton = new JButton(scaledIcon("/Buttons/Filled_favourites.png", toolbarIconSize, toolbarIconSize));
        favouritesListButton.setToolTipText("View your favourites");
        favouritesListButton.setFocusable(false);
        favouritesListButton.addActionListener(e -> {
            dismissAllOverlays();
            if (gamePanelListener != null) gamePanelListener.onJumpToFavourites();
        });

        // Settings button, opens SettingsOverlay (theme + button visibility + reorder, HE-23)
        settingsButton = new JButton(scaledIcon("/Buttons/settings.png", toolbarIconSize, toolbarIconSize));
        settingsButton.setToolTipText("Settings (s)");
        settingsButton.setFocusable(false);
        settingsButton.addActionListener(e -> showSettingsOverlayAtTab(SettingsOverlay.TAB_SETTINGS));

        // Register every customisable button with its stable ID and human-readable label.
        // Order here doesn't matter, DEFAULT_BUTTON_ORDER controls the initial layout.
        registerButton("previous",      "Previous Jigsaw",      previousJigsawButton);
        registerButton("back",          "Back to Collections",  backToCollectionsButton);
        registerButton("profile",       "Profile",              profileButton);
        registerButton("favouritesList","Favourites List",      favouritesListButton);
        registerButton("music",         "Mute / Unmute Music",  musicToggleButton);
        registerButton("trackChooser",  "Choose Music Track",   chooseTrackButton);
        registerButton("restart",       "Restart Jigsaw",       restartButton);
        registerButton("info",          "Artefact Information", extraInfoButton);
        registerButton("zen",           "Zen Mode",             zenModeButton);
        registerButton("timer",         "Timer",                timerButton);
        registerButton("theme",         "Theme (Dark / Light)", themeButton);
        registerButton("hint",          "Hint",                 hintButton);
        registerButton("showCompleted", "Show Completed Image", showCompletedButton);
        registerButton("split",         "Jigsaw Split",         jigsawSplitButton);
        registerButton("chooser",       "Choose Jigsaw",        chooseJigsawButton);
        registerButton("favourite",     "Favourite",            favouriteToggleButton);
        registerButton("help",          "Help & Shortcuts",     helpButton);
        registerButton("settings",      "Settings",             settingsButton);
        registerButton("next",          "Next Jigsaw",          nextJigsawButton);

        // Initial layout, apply the saved order/visibility (or defaults on first run)
        applyButtonCustomization();

        add(controlPanel, BorderLayout.SOUTH);

        // Zen-mode hiding is now driven by saveState.zenHiddenButtons via
        // applyButtonCustomization(). The old hardcoded zenHideComponents array was
        // superseded by the same customisation layer used for normal mode.
    }

    // Overlays
    private void hideInfoOverlay() {
        if (glassPaneRef != null && persistentOverlay != null) {
            // Reset the component's own visibility flag too, the info button's
            // handler toggles on persistentOverlay.isVisible(), and detaching from
            // the glass pane alone leaves that flag stale.
            persistentOverlay.setVisible(false);
            glassPaneRef.setVisible(false);
            glassPaneRef.remove(persistentOverlay);
            glassPaneRef.revalidate();
            glassPaneRef.repaint();
        }
    }

    // Button customisation (HE-23)
    private void registerButton(String id, String label, JButton button) {
        if (button == null) return;
        buttonsById.put(id, button);
        buttonLabels.put(id, label);
    }

    // Re-layout the control bar based on saveState's hiddenButtons and buttonOrder.
    // Called once during initial UI construction and again whenever the SettingsOverlay
    // mutates the customisation state.

    void applyButtonCustomization() {
        if (controlPanel == null || buttonsById.isEmpty()) return;

        // 1) Compute the effective order: take whatever the user saved, then append any
        //    registered buttons not already in that list (so new buttons added in future
        //    versions appear at the end of the bar rather than vanishing).
        java.util.List<String> effectiveOrder = new java.util.ArrayList<>();
        if (saveState != null && saveState.buttonOrder != null) {
            for (String id : saveState.buttonOrder) {
                if (buttonsById.containsKey(id) && !effectiveOrder.contains(id)) {
                    effectiveOrder.add(id);
                }
            }
        }
        for (String id : DEFAULT_BUTTON_ORDER) {
            if (buttonsById.containsKey(id) && !effectiveOrder.contains(id)) {
                effectiveOrder.add(id);
            }
        }

        // 2) Compute hidden set. Uses zenHiddenButtons while in zen mode so users can
        //    configure a separate minimal dock for focused play (HE-23 zen-aware).
        //    Settings button is always visible, users would be locked out otherwise.
        java.util.Set<String> hidden = new java.util.HashSet<>();
        if (saveState != null) {
            java.util.List<String> source = zenMode
                    ? saveState.zenHiddenButtons
                    : saveState.hiddenButtons;
            if (source != null) hidden.addAll(source);
        }
        hidden.remove("settings");

        // 3) Re-add components in order, applying visibility
        controlPanel.removeAll();
        for (String id : effectiveOrder) {
            JButton b = buttonsById.get(id);
            if (b == null) continue;
            b.setVisible(!hidden.contains(id));
            controlPanel.add(b);
        }
        controlPanel.revalidate();
        controlPanel.repaint();
    }

    // Accessors used by SettingsOverlay
    java.util.LinkedHashMap<String, String> getButtonLabels() { return buttonLabels; }
    java.util.List<String> getDefaultButtonOrder() { return DEFAULT_BUTTON_ORDER; }
    GameState getSaveState() { return saveState; }

    // Save the in-memory state, re-reading avatar fields from disk first so the
    // stale in-memory copy held by this panel can't overwrite a newer avatar
    // that was just picked via the avatar chooser. Avatar fields are written
    // exclusively by MainFrame.onAvatarConfirmed; disk is the source of truth
    // for them, so a disk to memory merge immediately before save prevents the
    // "avatar resets after playing a puzzle / toggling music" regression.

    void persistSaveState() {
        if (saveState == null) { SaveManager.save(null); return; }
        String active = SaveManager.getActiveProfile();
        if (active != null) {
            GameState disk = SaveManager.loadProfile(active);
            if (disk != null) {
                saveState.avatarStyle     = disk.avatarStyle;
                saveState.avatarSeed      = disk.avatarSeed;
                saveState.avatarImagePath = disk.avatarImagePath;
                saveState.avatarOptions   = disk.avatarOptions;
            }
        }
        SaveManager.save(saveState);
    }

    // Toolbar icon size (Tier 2)

    // Icon sizes exposed in the Settings dropdown.
    static final int[] TOOLBAR_ICON_SIZES = { 20, 24, 28, 32 };
    static final int TOOLBAR_ICON_SIZE_DEFAULT = 24;

    int getToolbarIconSize() { return toolbarIconSize; }

    // Apply a new toolbar icon size, rebuilds every icon and persists.
    void applyToolbarIconSize(int size) {
        boolean valid = false;
        for (int allowed : TOOLBAR_ICON_SIZES) { if (allowed == size) { valid = true; break; } }
        if (!valid) return;
        toolbarIconSize = size;
        saveState.toolbarIconSize = size;
        rebuildAllToolbarIcons();
        persistSaveState();
    }

    // Re-scale every toolbar icon to the current toolbarIconSize.
    // Static icons are looked up by their known resource path;
    // dynamic icons (favourite, theme, zen) are refreshed through their state-aware update methods.
    private void rebuildAllToolbarIcons() {
        int s = toolbarIconSize;
        if (previousJigsawButton != null)
            previousJigsawButton.setIcon(scaledIcon("/Buttons/left.png", s, s));
        if (hintButton != null)
            hintButton.setIcon(scaledIcon("/Buttons/hint.png", s, s));
        if (restartButton != null)
            restartButton.setIcon(scaledIcon("/Buttons/restart.png", s, s));
        if (showCompletedButton != null)
            showCompletedButton.setIcon(scaledIcon("/Buttons/show_completed.png", s, s));
        if (jigsawSplitButton != null)
            jigsawSplitButton.setIcon(scaledIcon("/Buttons/jigsaw_split.jpeg", s, s));
        if (extraInfoButton != null)
            extraInfoButton.setIcon(scaledIcon("/Buttons/information.png", s, s));
        if (musicToggleButton != null)
            musicToggleButton.setIcon(scaledIcon("/Buttons/music note.png", s, s));
        JButton trackBtn = buttonsById.get("trackChooser");
        if (trackBtn != null)
            trackBtn.setIcon(scaledIcon("/Buttons/music_track.png", s, s));
        if (backToCollectionsButton != null)
            backToCollectionsButton.setIcon(scaledIcon("/Buttons/collections.png", s, s));
        if (chooseJigsawButton != null)
            chooseJigsawButton.setIcon(scaledIcon("/Buttons/choose.png", s, s));
        if (nextJigsawButton != null)
            nextJigsawButton.setIcon(scaledIcon("/Buttons/right.png", s, s));
        if (helpButton != null)
            helpButton.setIcon(scaledIcon("/Buttons/guide.png", s, s));
        if (profileButton != null)
            profileButton.setIcon(scaledIcon("/Buttons/Profile_Icon.png", s, s));
        if (favouritesListButton != null)
            favouritesListButton.setIcon(scaledIcon("/Buttons/Filled_favourites.png", s, s));
        if (settingsButton != null)
            settingsButton.setIcon(scaledIcon("/Buttons/settings.png", s, s));

        // Dynamic icons: pick the correct sprite based on current state.
        updateFavouriteTooltip();
        updateThemeButtonIcon();
        if (zenModeButton != null) {
            zenModeButton.setIcon(scaledIcon(
                    zenMode ? "/Buttons/StandingMan.png" : "/Buttons/zen.png", s, s));
        }

        if (controlPanel != null) {
            controlPanel.revalidate();
            controlPanel.repaint();
        }
    }

    // Assembly-area colour presets (Tier 2)

    // Presets offered in the Settings swatch row, label + packed ARGB.
    static final int[][] ASSEMBLY_COLOUR_PRESETS = {
            { 0xFF000000, 0 }, // Black (default)
            { 0xFF2A2A2A, 0 }, // Charcoal
            { 0xFF3B4252, 0 }, // Slate
            { 0xFFF5EFE0, 0 }, // Cream
    };
    static final String[] ASSEMBLY_COLOUR_LABELS = { "Black", "Charcoal", "Slate", "Cream" };
    static final int ASSEMBLY_COLOUR_DEFAULT = 0xFF000000;

    // Apply a new assembly-area colour to one of the two modes and persist.
    void applyAssemblyAreaColor(boolean forZen, int argb) {
        if (forZen) saveState.assemblyAreaColorZen = argb;
        else        saveState.assemblyAreaColorNormal = argb;
        refreshAssemblyAreaColor();
        persistSaveState();
    }

    // Reset one of the two modes' assembly-area colour to the default.
    void resetAssemblyAreaColor(boolean forZen) {
        applyAssemblyAreaColor(forZen, ASSEMBLY_COLOUR_DEFAULT);
    }

    int getAssemblyAreaColor(boolean forZen) {
        return forZen ? saveState.assemblyAreaColorZen : saveState.assemblyAreaColorNormal;
    }

    // Background dim

    // Background-dim range, exposed to SettingsOverlay so the slider tick
    // marks and the default match the persisted value's domain.
    static final int BACKGROUND_DIM_MIN = 0;
    static final int BACKGROUND_DIM_MAX = 80;
    static final int BACKGROUND_DIM_DEFAULT = 0;

    // Apply a new background-dim percentage (0\u201380) and persist.
    void applyBackgroundDim(int percent) {
        int clamped = Math.max(BACKGROUND_DIM_MIN, Math.min(BACKGROUND_DIM_MAX, percent));
        if (saveState == null) return;
        saveState.backgroundDim = clamped;
        persistSaveState();
        repaint();
    }

    // Update the in-memory dim value and repaint without writing to disk.
    // Called while the settings slider is being dragged,
    // persisting on every tick would trigger a full JSON write per pixel of drag travel.
    // The drag-end handler calls {@link #applyBackgroundDim(int)} to commit.
    void previewBackgroundDim(int percent) {
        int clamped = Math.max(BACKGROUND_DIM_MIN, Math.min(BACKGROUND_DIM_MAX, percent));
        if (saveState == null) return;
        saveState.backgroundDim = clamped;
        repaint();
    }

    // Reset the background dim back to its default (no overlay).
    void resetBackgroundDim() {
        applyBackgroundDim(BACKGROUND_DIM_DEFAULT);
    }

    int getBackgroundDim() {
        return saveState != null ? saveState.backgroundDim : BACKGROUND_DIM_DEFAULT;
    }

    // Push the colour matching the current mode into the puzzle canvas.
    private void refreshAssemblyAreaColor() {
        if (puzzlePanel == null || saveState == null) return;
        int argb = zenMode ? saveState.assemblyAreaColorZen : saveState.assemblyAreaColorNormal;
        puzzlePanel.setAssemblyAreaColor(new Color(argb, true));
    }

    // Auto-hide toolbar (Tier 3)
    // Toggle the auto-hide toolbar setting. On: hide immediately; off: always show.
    void applyAutoHideToolbar(boolean enabled) {
        saveState.autoHideToolbar = enabled;
        persistSaveState();
        if (enabled) {
            // Hide on enable so the user sees the effect. Any mouse move into
            // the reveal zone will bring it back.
            hideToolbar();
        } else {
            showToolbar();
        }
    }

    boolean isAutoHideToolbar() { return saveState != null && saveState.autoHideToolbar; }

    private void hideToolbar() {
        if (controlPanel == null || toolbarHidden) return;
        // Don't hide while an overlay is up, the user may be mid-task.
        if (settingsOverlay != null && settingsOverlay.isVisible()) return;
        if (glassPaneRef != null && glassPaneRef.isVisible()) return;
        toolbarHidden = true;
        controlPanel.setVisible(false);
        revalidate();
        repaint();
    }

    private void showToolbar() {
        if (controlPanel == null || !toolbarHidden) {
            if (controlPanel != null && !controlPanel.isVisible()) {
                controlPanel.setVisible(true);
                revalidate();
                repaint();
            }
            toolbarHidden = false;
            return;
        }
        toolbarHidden = false;
        controlPanel.setVisible(true);
        revalidate();
        repaint();
    }

    // Mouse handler: decide show/hide based on cursor Y relative to this panel.
    // A small hysteresis band avoids flicker when the cursor hovers near the
    // reveal zone boundary, once the toolbar is shown it takes a slightly
    // bigger upward move to re-hide it.
    private void onMouseActivity(java.awt.event.MouseEvent e, java.awt.Component source) {
        if (!isAutoHideToolbar()) return;
        Point p = SwingUtilities.convertPoint(source, e.getPoint(), this);
        int showThreshold = getHeight() - TOOLBAR_REVEAL_ZONE_PX;
        int hideThreshold = showThreshold - 10;
        if (p.y >= showThreshold) {
            if (toolbarHidden) showToolbar();
        } else if (p.y < hideThreshold) {
            if (!toolbarHidden) hideToolbar();
        }
    }

    // Live state accessors for inline toggles in SettingsOverlay
    boolean isMusicPlaying() { return musicPlaying; }
    boolean isZenMode() { return zenMode; }
    boolean isTimerRunning() { return timerRunning; }

    // Trigger the real toolbar button so the existing state machine + persistence runs.
    void triggerMusicToggle() { if (musicToggleButton != null) musicToggleButton.doClick(); }
    void triggerZenToggle()   { if (zenModeButton != null) zenModeButton.doClick(); }
    void triggerTimerToggle() { if (timerButton != null) timerButton.doClick(); }

    // Request the owning MainFrame to start the tutorial. Called from SettingsOverlay.
    void requestTutorialReplay() {
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w instanceof MainFrame) {
            hideSettingsOverlay();
            SwingUtilities.invokeLater(((MainFrame) w)::startTutorial);
        }
    }

    // Show the unified Help & Settings overlay at the specified tab. Toggles off if already visible.
    // Both the help button and the settings button route through here so only one overlay exists,
    // reducing maintenance surface and avoiding two competing overlays at once.

    private void showSettingsOverlayAtTab(int tabIndex) {
        if (settingsOverlay != null && settingsOverlay.isVisible()) {
            hideSettingsOverlay();
            return;
        }

        JRootPane root = SwingUtilities.getRootPane(this);
        if (root == null) return;

        if (settingsOverlay == null) {
            settingsOverlay = new SettingsOverlay(this);
            settingsOverlay.setDismissListener(this::hideSettingsOverlay);
        }

        if (settingsOverlay.getParent() != null) {
            settingsOverlay.getParent().remove(settingsOverlay);
        }

        settingsOverlay.setActiveTab(tabIndex);
        settingsOverlay.refreshFromSaveState();

        JLayeredPane layered = root.getLayeredPane();
        settingsOverlay.setBounds(0, 0, layered.getWidth(), layered.getHeight());
        layered.add(settingsOverlay, JLayeredPane.MODAL_LAYER);
        settingsOverlay.setVisible(true);
        settingsOverlay.requestFocusInWindow();
        layered.revalidate();
        layered.repaint();
    }

    private void hideSettingsOverlay() {
        if (settingsOverlay == null) return;
        settingsOverlay.setVisible(false);
        Container parent = settingsOverlay.getParent();
        if (parent != null) {
            parent.remove(settingsOverlay);
            parent.revalidate();
            parent.repaint();
        }
    }

    // Dismiss every overlay and popover that might be open. Called on every screen
    // transition (back to collections, collection switch, jigsaw switch, profile reload)
    // so nothing lingers from the previous context.

    private void dismissAllOverlays() {
        hideInfoOverlay();
        hideSettingsOverlay();
        dismissReflectionOverlay();
        if (completedOverlay != null && completedOverlay.isVisible()) {
            completedOverlay.setVisible(false);
        }
        if (completedPeek != null && completedPeek.isVisible()) {
            completedPeek.setVisible(false);
        }
        if (splitOverlay != null && splitOverlay.isShowing()) {
            splitOverlay.close();
        }
        if (rowsColsOverlay != null && rowsColsOverlay.isShowing()) {
            rowsColsOverlay.close();
        }
        if (glassPaneRef != null) glassPaneRef.repaint();
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

    // Tooltips / Theme icon
    private void updateFavouriteTooltip() {
        if (favouriteToggleButton == null || imageComboBox == null) return;
        Object sel = imageComboBox.getSelectedItem();
        boolean isFav = sel != null && saveState != null && saveState.isFavourite(sel.toString());
        String iconPath = isFav ? "/Buttons/Filled_favourites.png" : "/Buttons/favourites.jpg";
        favouriteToggleButton.setIcon(scaledIcon(iconPath, toolbarIconSize, toolbarIconSize));
        favouriteToggleButton.setToolTipText(isFav ? "Unfavourite (f)" : "Favourite (f)");
    }

    private void updateMusicTooltip() {
        if (musicToggleButton == null) return;
        if (musicPlayer == null) {
            musicToggleButton.setToolTipText("Music unavailable (audio failed to load)");
            musicToggleButton.setEnabled(false);
            return;
        }
        musicToggleButton.setEnabled(true);
        musicToggleButton.setToolTipText(musicPlaying ? "Mute music (m)" : "Unmute music (m)");
    }

    // Construct a MusicPlayer without crashing the game panel if anything goes wrong.
    // Returns null on failure; callers must null-check before using the returned player.
    // Catches Throwable rather than Exception because the JavaFX runtime can throw
    // NoClassDefFoundError / UnsatisfiedLinkError when natives are missing.

    private MusicPlayer createMusicPlayerSafely(String resourcePath) {
        try {
            return new MusicPlayer(resourcePath);
        } catch (Throwable t) {
            System.err.println("[Music] Failed to initialise track '" + resourcePath
                    + "': " + t.getClass().getSimpleName() + " - " + t.getMessage());
            return null;
        }
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

        themeButton.setIcon(scaledIcon(themeIconPath, toolbarIconSize, toolbarIconSize));
        themeButton.setText(null);
        themeButton.setPreferredSize(new Dimension(36, 36));
        themeButton.setToolTipText(isDark ? "Switch to light mode" : "Switch to dark mode");
    }

    // Zen / Timer / Collection

    private void toggleZenMode() {
        zenMode = !zenMode;

        // Re-apply the control bar based on the new mode. applyButtonCustomization()
        // picks between saveState.hiddenButtons (normal) and saveState.zenHiddenButtons
        // (zen) depending on the zenMode flag, so the bar reflects the right set.
        applyButtonCustomization();

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

        // Switch the assembly-area colour to the one saved for this mode.
        refreshAssemblyAreaColor();

        if (controlPanel != null) {
            if (zenMode) {
                controlPanel.setOpaque(true);
                controlPanel.setBackground(new Color(28, 28, 28));
            } else {
                controlPanel.setOpaque(false);
            }
        }

        if (zenModeButton != null) {
            // Icon shows the action that clicking will perform (play/pause convention):
            // zenMode on to show StandingMan, meaning "click to stand up / exit Zen"
            // zenMode off to show meditation, meaning "click to enter Zen"
            zenModeButton.setIcon(scaledIcon(
                    zenMode ? "/Buttons/StandingMan.png" : "/Buttons/zen.png", toolbarIconSize, toolbarIconSize));
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

    // Tutorial target accessors
    public JComponent getTutorialTargetPuzzleArea() { return puzzlePanel; }
    public JComponent getTutorialTargetArrows()     { return previousJigsawButton; }
    public JComponent getTutorialTargetHelpButton() { return helpButton; }
    public JComponent getTutorialTargetBackButton() { return backToCollectionsButton; }

    // Re-read the currently active profile from disk and apply its settings to this panel.
    // Used by the Resume flow when switching profiles after the panel has been constructed,
    // without this, the panel still holds the previous profile's in-memory saveState and any
    // subsequent save() would overwrite the new profile's file with the old profile's data.

    public void reloadFromActiveProfile() {
        dismissAllOverlays();
        loadSaveStateAndApplyEarly();
        applyBackgroundForCurrentState();
        restoreUIStateAfterControlsCreated();
    }

    public void loadCollection(String collectionName) {
        dismissAllOverlays();

        // Re-read the active profile from disk so we have the latest avatar, achievements,
        // favourites, and any other fields that were set outside this panel (e.g. by the
        // avatar chooser or the profile screen). Without this, the stale in-memory saveState
        // from panel construction time overwrites the profile's data on the next save,
        // which is what caused avatars to vanish after completing a puzzle.

        GameState fresh = SaveManager.loadOrDefault();
        if (fresh != null) {
            saveState = fresh;
        }

        this.currentCollection = collectionName;
        applyBackgroundForCurrentState();

        // Load items in chronological order from TimelineData
        imageComboBox.removeAllItems();
        java.util.List<TimelineData.Entry> timeline = TimelineData.forCollection(collectionName);
        if (!timeline.isEmpty()) {
            for (TimelineData.Entry te : timeline) {
                imageComboBox.addItem(te.imagePath);
            }
        } else {
            // Fallback: catalog order
            for (String img : ArtifactCatalog.imagesFor(collectionName)) {
                imageComboBox.addItem(img);
            }
        }

        if (imageComboBox.getItemCount() > 0) {
            imageComboBox.setSelectedIndex(0);
            puzzlePanel.setImage((String) imageComboBox.getItemAt(0));
        }

        if (nextJigsawButton != null) nextJigsawButton.setEnabled(false);
        hideInfoOverlay();

        snapshotToSaveState();
        persistSaveState();
    }

    // Select a specific jigsaw by its resource path.
    public void selectJigsaw(String jigsawPath) {
        if (imageComboBox == null || jigsawPath == null) return;
        dismissAllOverlays();
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



    // Keyboard shortcuts
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
        // H cycles hint tier (restored to original binding, H for hint is more intuitive).
        bind.accept("H", () -> { if (hintButton != null) hintButton.doClick(); });
        // S opens the settings overlay (HE-23, control bar customisation).
        bind.accept("S", () -> { if (settingsButton != null) settingsButton.doClick(); });
        // ? opens the help overlay (HE-28). Both the unshifted and shifted forms
        // are bound so users on any keyboard layout can reach it.
        bind.accept("shift SLASH", () -> { if (helpButton != null) helpButton.doClick(); });
        bind.accept("SLASH", () -> { if (helpButton != null) helpButton.doClick(); });

        bind.accept("ESCAPE", () -> {
            boolean changed = false;

            if (settingsOverlay != null && settingsOverlay.isVisible()) {
                hideSettingsOverlay();
                changed = true;
            }
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

    // ThemeAware
    @Override
    public void refreshTheme() {
        // Only update the narrative label colour, icon buttons stay as-is
        if (narrativeLabel != null) {
            narrativeLabel.setForeground(ThemeManager.get().palette().base.narrativeText);
        }
        // Refresh background for current collection + theme
        applyBackgroundForCurrentState();
        repaint();
    }

    // Micro Narrative
    // Show a short contextual sentence at the top of the puzzle area that auto-hides.
    private void showMicroNarrative(String imagePath) {
        if (narrativeLabel == null || imagePath == null) return;

        // Cancel any existing fade timer
        if (narrativeFadeTimer != null && narrativeFadeTimer.isRunning()) {
            narrativeFadeTimer.stop();
        }

        // Look up narrative from TimelineData
        TimelineData.Entry entry = TimelineData.forImage(currentCollection, imagePath);
        if (entry == null || entry.microNarrative == null || entry.microNarrative.isEmpty()) {
            narrativeLabel.setVisible(false);
            return;
        }

        narrativeLabel.setText(entry.microNarrative);
        narrativeLabel.setVisible(true);
        revalidate();

        // Auto-hide after 8 seconds
        narrativeFadeTimer = new javax.swing.Timer(8000, e -> {
            narrativeLabel.setVisible(false);
            ((javax.swing.Timer) e.getSource()).stop();
        });
        narrativeFadeTimer.setRepeats(false);
        narrativeFadeTimer.start();
    }

    // Reflection Prompts (Learning-theory PoC)
    private void ensureReflectionOverlay() {
        if (reflectionOverlay != null) return;
        reflectionOverlay = new ReflectionPromptOverlay();
        reflectionOverlay.setDismissListener(this::dismissReflectionOverlay);
        reflectionOverlay.setMoreInfoListener(() -> {
            dismissReflectionOverlay();
            // Trigger the info button to open the info panel
            if (extraInfoButton != null) extraInfoButton.doClick();
        });
    }

    // Show a pre-puzzle prediction prompt if the current artefact has one.
    private void showPreReflectionPrompt() {
        if (!reflectionPromptsReady) return;
        if (!ReflectionPromptData.isSupported(currentCollection)) return;

        String selectedImage = (String) imageComboBox.getSelectedItem();
        if (selectedImage == null) return;

        ReflectionPromptData.Prompts prompts = ReflectionPromptData.forArtefact(selectedImage);
        if (prompts == null) return;

        SwingUtilities.invokeLater(() -> {
            ensureReflectionOverlay();
            dismissReflectionOverlay(); // remove if already showing
            reflectionOverlay.showPre(prompts);
            addReflectionToLayeredPane();
        });
    }

    // Show a post-puzzle completion overlay with reflection (if available) + XP + achievements.
    private void showPostReflectionPrompt() {
        String selectedImage = (String) imageComboBox.getSelectedItem();

        // Get reflection prompts if available (null is fine, overlay handles it)
        ReflectionPromptData.Prompts prompts = null;
        if (selectedImage != null && ReflectionPromptData.isSupported(currentCollection)) {
            prompts = ReflectionPromptData.forArtefact(selectedImage);
        }

        // Mark that the next overlay dismissal should fire a save / achievement
        // toast (HE-02, HE-03). Cleared in dismissReflectionOverlay so PRE-prompt
        // dismissals don't accidentally trigger it.
        pendingCompletionToast = true;

        final ReflectionPromptData.Prompts finalPrompts = prompts;
        SwingUtilities.invokeLater(() -> {
            ensureReflectionOverlay();
            dismissReflectionOverlay(); // remove if already showing
            reflectionOverlay.showPost(finalPrompts, lastXpEarned, lastAchievementNames);
            addReflectionToLayeredPane();
            // dismissReflectionOverlay above flips the flag off; restore it now
            // that the post-mode overlay is genuinely on screen.
            pendingCompletionToast = true;
        });
    }

    private void addReflectionToLayeredPane() {
        JRootPane rp = SwingUtilities.getRootPane(this);
        if (rp == null) return;
        JLayeredPane layered = rp.getLayeredPane();
        reflectionOverlay.setBounds(0, 0, layered.getWidth(), layered.getHeight());
        layered.add(reflectionOverlay, JLayeredPane.MODAL_LAYER);
        reflectionOverlay.setVisible(true);
        layered.revalidate();
        layered.repaint();
    }

    private void dismissReflectionOverlay() {
        if (reflectionOverlay == null) return;
        reflectionOverlay.setVisible(false);
        Container parent = reflectionOverlay.getParent();
        if (parent != null) {
            parent.remove(reflectionOverlay);
            parent.revalidate();
            parent.repaint();
        }

        // HE-02 / HE-03: surface a transient toast confirming the save and any
        // achievements unlocked, so the user has an explicit save acknowledgement
        // and a reminder of achievements they may have skimmed past.
        if (pendingCompletionToast) {
            pendingCompletionToast = false;
            showCompletionToast();
        }
    }

    // HE-02 / HE-03: themed transient toast at the bottom-centre of the game panel.
    // Used after the post-completion reflection overlay dismisses to
    // confirm the auto-save and surface newly-unlocked achievements.

    private void showCompletionToast() {
        if (glassPaneRef == null) return;

        StringBuilder msg = new StringBuilder("Progress saved");
        if (lastAchievementNames != null && !lastAchievementNames.isEmpty()) {
            msg.append("  —  Unlocked: ");
            msg.append(String.join(", ", lastAchievementNames));
        }

        Theme.Palette p = ThemeManager.get().palette();
        JPanel toast = new JPanel(new BorderLayout());
        toast.setOpaque(true);
        toast.setBackground(p.overlay.cardFill);
        toast.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(p.overlay.cardStroke, 1, true),
                BorderFactory.createEmptyBorder(8, 18, 8, 18)));

        JLabel label = new JLabel(msg.toString());
        label.setForeground(p.overlay.text);
        label.setFont(p.fonts.bodyBold);
        toast.add(label, BorderLayout.CENTER);

        Dimension pref = toast.getPreferredSize();
        int w = Math.min(pref.width, glassPaneRef.getWidth() - 80);
        int h = Math.max(36, pref.height);
        int x = Math.max(20, (glassPaneRef.getWidth() - w) / 2);
        int y = glassPaneRef.getHeight() - h - 60;
        toast.setBounds(x, y, w, h);

        glassPaneRef.add(toast);
        glassPaneRef.setVisible(true);
        glassPaneRef.repaint();

        // Linger longer when an achievement is included so the user has time to read it.
        int durationMs = (lastAchievementNames != null && !lastAchievementNames.isEmpty()) ? 4500 : 1800;
        javax.swing.Timer t = new javax.swing.Timer(durationMs, e -> {
            if (toast.getParent() != null) {
                glassPaneRef.remove(toast);
                glassPaneRef.repaint();
            }
        });
        t.setRepeats(false);
        t.start();
    }
}
