import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

// Tabbed overlay combining Help (keyboard shortcuts reference) and Settings
// (theme, control-bar customisation, tutorial replay). Opened by either the
// guide button (defaults to the Help tab) or the settings button (defaults to
// the Settings tab), and dismissed via close, ESC, or click-outside.
// Created by FixThePotGamePanel via showSettingsOverlay(). All mutations are
// written into the panel's saveState and persisted immediately.

public class SettingsOverlay extends JPanel implements ThemeAware {

    public interface DismissListener {
        void onDismiss();
    }

    public static final int TAB_HELP = 0;
    public static final int TAB_SETTINGS = 1;

    private final FixThePotGamePanel host;
    private DismissListener dismissListener;

    private final JPanel card;
    private final JLabel titleLabel;
    private final JTabbedPane tabs;

    // Help tab
    private final JPanel helpContent;

    // Settings tab
    private final JPanel buttonsListPanel;
    private final JScrollPane buttonsScroll;

    private final JButton resetButton;
    private final JButton closeButton;

    // Appearance controls, kept as fields so refreshFromSaveState can re-sync them.
    private JSlider iconSizeSlider;
    private JLabel iconSizeValueLabel;
    private JCheckBox autoHideCheck;
    private JPanel normalColourRow;
    private JPanel zenColourRow;

    // Which dock the user is currently editing: normal or zen.
    private boolean editingZen = false;
    private JButton modeNormalBtn;
    private JButton modeZenBtn;

    // Working copy of the live order, mutated by the up/down arrows and applied on close.
    private final List<String> workingOrder = new ArrayList<>();

    public SettingsOverlay(FixThePotGamePanel host) {
        this.host = host;

        setOpaque(true);
        setBackground(new Color(0, 0, 0, 170));
        setLayout(new GridBagLayout());

        card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 28, 20, 28));
        card.setOpaque(true);

        // Title
        titleLabel = new JLabel("Help & Settings");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.putClientProperty("settings-role", "title");
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(14));

        // Tabs
        tabs = new JTabbedPane();
        tabs.setFocusable(false);
        tabs.setOpaque(false);
        tabs.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Build Help tab
        helpContent = new JPanel();
        helpContent.setLayout(new BoxLayout(helpContent, BoxLayout.Y_AXIS));
        helpContent.setOpaque(false);
        helpContent.setBorder(new EmptyBorder(16, 18, 16, 18));
        buildHelpContent();

        JScrollPane helpScroll = new JScrollPane(helpContent,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        helpScroll.setBorder(BorderFactory.createEmptyBorder());
        helpScroll.getViewport().setOpaque(false);
        helpScroll.setOpaque(false);
        helpScroll.getVerticalScrollBar().setUnitIncrement(16);
        FantasyScrollBarUI.install(helpScroll);
        tabs.addTab("Help", helpScroll);

        // Build Settings tab
        JPanel settingsContent = new JPanel();
        settingsContent.setLayout(new BoxLayout(settingsContent, BoxLayout.Y_AXIS));
        settingsContent.setOpaque(false);
        settingsContent.setBorder(new EmptyBorder(16, 18, 16, 18));

        // Theme, music, zen, and timer live inline in the dock list below as
        // state toggles next to their respective checkboxes, no separate section.

        JLabel buttonsSection = new JLabel("Control bar dock");
        buttonsSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonsSection.putClientProperty("settings-role", "section");
        settingsContent.add(buttonsSection);
        settingsContent.add(Box.createVerticalStrut(6));

        // Normal / Zen mode selector, configures each mode's dock independently
        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        modeRow.setOpaque(false);
        modeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        modeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        modeNormalBtn = makeModeButton("Normal", false);
        modeNormalBtn.addActionListener(e -> setEditingMode(false));
        modeRow.add(modeNormalBtn);

        modeZenBtn = makeModeButton("Zen", true);
        modeZenBtn.addActionListener(e -> setEditingMode(true));
        modeRow.add(modeZenBtn);

        settingsContent.add(modeRow);
        settingsContent.add(Box.createVerticalStrut(4));

        JLabel buttonsHint = new JLabel("Tick to show, untick to remove. Use \u2191\u2193 to reorder.");
        buttonsHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonsHint.putClientProperty("settings-role", "hint");
        settingsContent.add(buttonsHint);
        settingsContent.add(Box.createVerticalStrut(8));

        buttonsListPanel = new JPanel();
        buttonsListPanel.setLayout(new BoxLayout(buttonsListPanel, BoxLayout.Y_AXIS));
        buttonsListPanel.setOpaque(false);

        buttonsScroll = new JScrollPane(buttonsListPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        buttonsScroll.setBorder(BorderFactory.createEmptyBorder());
        buttonsScroll.getViewport().setOpaque(false);
        buttonsScroll.setOpaque(false);
        buttonsScroll.getVerticalScrollBar().setUnitIncrement(16);
        buttonsScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonsScroll.setPreferredSize(new Dimension(440, 260));
        buttonsScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        FantasyScrollBarUI.install(buttonsScroll);
        settingsContent.add(buttonsScroll);
        settingsContent.add(Box.createVerticalStrut(10));

        // Reset dock row, primary customisation action, kept near the dock list
        JPanel resetRow = new JPanel(new BorderLayout(12, 0));
        resetRow.setOpaque(false);
        resetRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        resetRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel resetHint = new JLabel("Restore this mode's default dock layout.");
        resetHint.putClientProperty("settings-role", "hint");
        resetRow.add(resetHint, BorderLayout.CENTER);

        resetButton = makeSmallButton("Reset dock");
        resetButton.addActionListener(e -> resetCurrentModeToDefaults());
        resetRow.add(resetButton, BorderLayout.EAST);
        settingsContent.add(resetRow);
        settingsContent.add(Box.createVerticalStrut(18));

        // Appearance section: icon size + auto-hide + assembly area colour
        JLabel appearanceSection = new JLabel("Appearance");
        appearanceSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        appearanceSection.putClientProperty("settings-role", "section");
        settingsContent.add(appearanceSection);
        settingsContent.add(Box.createVerticalStrut(6));

        // Icon size row
        JPanel iconSizeRow = new JPanel(new BorderLayout(12, 0));
        iconSizeRow.setOpaque(false);
        iconSizeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        iconSizeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel iconLabel = new JLabel("Toolbar icon size");
        iconLabel.putClientProperty("settings-role", "value");
        iconSizeRow.add(iconLabel, BorderLayout.WEST);

        // Slider with snap-to-ticks so only the four supported sizes are selectable.
        int[] sizes = FixThePotGamePanel.TOOLBAR_ICON_SIZES;
        int minSize = sizes[0];
        int maxSize = sizes[sizes.length - 1];
        int step = sizes.length > 1 ? (sizes[1] - sizes[0]) : 4;
        iconSizeSlider = new JSlider(minSize, maxSize, FixThePotGamePanel.TOOLBAR_ICON_SIZE_DEFAULT);
        iconSizeSlider.setOpaque(false);
        iconSizeSlider.setFocusable(false);
        iconSizeSlider.setMajorTickSpacing(step);
        iconSizeSlider.setSnapToTicks(true);
        iconSizeSlider.setPaintTicks(true);
        iconSizeSlider.setPreferredSize(new Dimension(160, 32));
        iconSizeSlider.addChangeListener(e -> {
            if (!iconSizeSlider.getValueIsAdjusting()) {
                int v = iconSizeSlider.getValue();
                if (iconSizeValueLabel != null) iconSizeValueLabel.setText(Integer.toString(v));
                host.applyToolbarIconSize(v);
            } else if (iconSizeValueLabel != null) {
                iconSizeValueLabel.setText(Integer.toString(iconSizeSlider.getValue()));
            }
        });

        iconSizeValueLabel = new JLabel(Integer.toString(FixThePotGamePanel.TOOLBAR_ICON_SIZE_DEFAULT));
        iconSizeValueLabel.setPreferredSize(new Dimension(30, 28));
        iconSizeValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        iconSizeValueLabel.putClientProperty("settings-role", "value");

        JPanel iconSizeHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        iconSizeHolder.setOpaque(false);
        iconSizeHolder.add(iconSizeSlider);
        iconSizeHolder.add(iconSizeValueLabel);
        iconSizeRow.add(iconSizeHolder, BorderLayout.EAST);
        settingsContent.add(iconSizeRow);
        settingsContent.add(Box.createVerticalStrut(4));

        // Auto-hide toolbar row
        JPanel autoHideRow = new JPanel(new BorderLayout(12, 0));
        autoHideRow.setOpaque(false);
        autoHideRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoHideRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel autoHideLabel = new JLabel("Auto-hide toolbar when idle");
        autoHideLabel.putClientProperty("settings-role", "value");
        autoHideRow.add(autoHideLabel, BorderLayout.WEST);

        autoHideCheck = new JCheckBox();
        autoHideCheck.setOpaque(false);
        autoHideCheck.setFocusPainted(false);
        autoHideCheck.addActionListener(e -> host.applyAutoHideToolbar(autoHideCheck.isSelected()));
        JPanel autoHideHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        autoHideHolder.setOpaque(false);
        autoHideHolder.add(autoHideCheck);
        autoHideRow.add(autoHideHolder, BorderLayout.EAST);
        settingsContent.add(autoHideRow);
        settingsContent.add(Box.createVerticalStrut(14));

        // Assembly-area colour: two rows, one per mode, each with preset swatches + reset
        JLabel assemblySection = new JLabel("Assembly area colour");
        assemblySection.setAlignmentX(Component.LEFT_ALIGNMENT);
        assemblySection.putClientProperty("settings-role", "section");
        settingsContent.add(assemblySection);
        settingsContent.add(Box.createVerticalStrut(4));

        JLabel assemblyHint = new JLabel("Pick the background colour for each mode.");
        assemblyHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        assemblyHint.putClientProperty("settings-role", "hint");
        settingsContent.add(assemblyHint);
        settingsContent.add(Box.createVerticalStrut(6));

        normalColourRow = makeColourRow("Normal mode", false);
        settingsContent.add(normalColourRow);
        settingsContent.add(Box.createVerticalStrut(4));
        zenColourRow = makeColourRow("Zen mode", true);
        settingsContent.add(zenColourRow);
        settingsContent.add(Box.createVerticalStrut(18));

        // Background dim: participant-requested option to tone down the
        // themed background during active gameplay while preserving the imagery.
        // Uses a cheap per-frame alpha overlay in the game panel's
        // paintComponent rather than a cached blur, so slider drags stay responsive.
        JLabel bgSection = new JLabel("Background dim");
        bgSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        bgSection.putClientProperty("settings-role", "section");
        settingsContent.add(bgSection);
        settingsContent.add(Box.createVerticalStrut(4));

        JLabel bgHint = new JLabel("Darken the game panel background so puzzle pieces stand out more.");
        bgHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        bgHint.putClientProperty("settings-role", "hint");
        settingsContent.add(bgHint);
        settingsContent.add(Box.createVerticalStrut(6));

        JPanel bgRow = new JPanel(new BorderLayout(12, 0));
        bgRow.setOpaque(false);
        bgRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        bgRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel bgLabel = new JLabel("Dim");
        bgLabel.putClientProperty("settings-role", "value");
        bgRow.add(bgLabel, BorderLayout.WEST);

        JPanel bgSliderWrap = new JPanel(new BorderLayout(8, 0));
        bgSliderWrap.setOpaque(false);

        JSlider bgSlider = new JSlider(
                FixThePotGamePanel.BACKGROUND_DIM_MIN,
                FixThePotGamePanel.BACKGROUND_DIM_MAX,
                host.getBackgroundDim());
        bgSlider.setOpaque(false);
        bgSlider.setFocusable(false);
        bgSlider.setMajorTickSpacing(20);
        bgSlider.setPaintTicks(true);

        JLabel bgValueLabel = new JLabel(host.getBackgroundDim() + "%");
        bgValueLabel.setPreferredSize(new Dimension(44, 20));
        bgValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        bgValueLabel.putClientProperty("settings-role", "value");

        bgSlider.addChangeListener(e -> {
            int v = bgSlider.getValue();
            bgValueLabel.setText(v + "%");
            // Apply live so the user sees the change behind the overlay; the
            // persist cost is one JSON write, acceptable on slider release.
            if (!bgSlider.getValueIsAdjusting()) {
                host.applyBackgroundDim(v);
            } else {
                // During drag, just preview without persisting every tick.
                host.previewBackgroundDim(v);
            }
        });

        bgSliderWrap.add(bgSlider, BorderLayout.CENTER);
        bgSliderWrap.add(bgValueLabel, BorderLayout.EAST);
        bgRow.add(bgSliderWrap, BorderLayout.CENTER);

        JButton bgReset = makeSmallButton("Reset");
        bgReset.addActionListener(e -> {
            host.resetBackgroundDim();
            bgSlider.setValue(FixThePotGamePanel.BACKGROUND_DIM_DEFAULT);
        });
        bgRow.add(bgReset, BorderLayout.EAST);

        settingsContent.add(bgRow);
        settingsContent.add(Box.createVerticalStrut(18));

        // Tutorial row
        JLabel tutorialSection = new JLabel("Tutorial");
        tutorialSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        tutorialSection.putClientProperty("settings-role", "section");
        settingsContent.add(tutorialSection);
        settingsContent.add(Box.createVerticalStrut(6));

        JPanel tutorialRow = new JPanel(new BorderLayout(12, 0));
        tutorialRow.setOpaque(false);
        tutorialRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        tutorialRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel tutorialHint = new JLabel("Replay the first-launch walkthrough.");
        tutorialHint.putClientProperty("settings-role", "hint");
        tutorialRow.add(tutorialHint, BorderLayout.CENTER);

        JButton replayButton = makeSmallButton("Show tutorial");
        replayButton.addActionListener(e -> host.requestTutorialReplay());
        tutorialRow.add(replayButton, BorderLayout.EAST);
        settingsContent.add(tutorialRow);

        JScrollPane settingsScroll = new JScrollPane(settingsContent,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        settingsScroll.setBorder(BorderFactory.createEmptyBorder());
        settingsScroll.getViewport().setOpaque(false);
        settingsScroll.setOpaque(false);
        settingsScroll.getVerticalScrollBar().setUnitIncrement(16);
        FantasyScrollBarUI.install(settingsScroll);
        tabs.addTab("Settings", settingsScroll);

        // Fixed size so the dialog doesn't jump between tabs
        tabs.setPreferredSize(new Dimension(540, 460));
        tabs.setMaximumSize(new Dimension(540, 460));
        card.add(tabs);
        card.add(Box.createVerticalStrut(14));

        // Footer, just Close now that Reset lives inside the Settings tab
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        footer.setOpaque(false);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);

        closeButton = makeFooterButton("Close");
        closeButton.addActionListener(e -> dismiss());
        footer.add(closeButton);

        card.add(footer);

        card.setMinimumSize(new Dimension(580, 580));
        card.setMaximumSize(new Dimension(620, 640));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        add(card, gbc);

        // Stay sized to the parent (layered pane) even if the window resizes while
        // the overlay is visible — otherwise the semi-transparent backdrop becomes
        // smaller than the window and the corners "leak" the UI behind.
        addHierarchyBoundsListener(new java.awt.event.HierarchyBoundsAdapter() {
            @Override public void ancestorResized(java.awt.event.HierarchyEvent e) {
                Container p = getParent();
                if (p != null) setBounds(0, 0, p.getWidth(), p.getHeight());
            }
        });

        // Click outside the card to dismiss
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!card.getBounds().contains(e.getPoint())) {
                    dismiss();
                }
            }
        });

        // ESC dismisses
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "settings-dismiss");
        getActionMap().put("settings-dismiss", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isVisible()) dismiss();
            }
        });

        ThemeManager.get().register(this);
        refreshTheme();
    }

    public void setDismissListener(DismissListener l) { this.dismissListener = l; }

    // Show with the specified tab preselected (TAB_HELP or TAB_SETTINGS).
    public void setActiveTab(int index) {
        if (index >= 0 && index < tabs.getTabCount()) {
            tabs.setSelectedIndex(index);
        }
    }

    private void dismiss() {
        if (dismissListener != null) dismissListener.onDismiss();
    }

    // Re-read the live save state and rebuild the rows. Called every time the overlay is shown.
    public void refreshFromSaveState() {
        workingOrder.clear();
        GameState st = host.getSaveState();
        List<String> defaultOrder = host.getDefaultButtonOrder();
        LinkedHashMap<String, String> labels = host.getButtonLabels();

        if (st != null && st.buttonOrder != null) {
            for (String id : st.buttonOrder) {
                if (labels.containsKey(id) && !workingOrder.contains(id)) {
                    workingOrder.add(id);
                }
            }
        }
        for (String id : defaultOrder) {
            if (labels.containsKey(id) && !workingOrder.contains(id)) {
                workingOrder.add(id);
            }
        }

        // Default to showing the dock for whichever mode is currently active
        editingZen = host.isZenMode();
        updateModeButtons();
        rebuildButtonRows();

        // Appearance controls, sync to the latest saved values.
        if (iconSizeSlider != null) {
            int current = host.getToolbarIconSize();
            iconSizeSlider.setValue(current);
            if (iconSizeValueLabel != null) iconSizeValueLabel.setText(Integer.toString(current));
        }
        if (autoHideCheck != null) {
            autoHideCheck.setSelected(host.isAutoHideToolbar());
        }
        if (normalColourRow != null) repaintSwatchRow(normalColourRow, false);
        if (zenColourRow != null) repaintSwatchRow(zenColourRow, true);
    }

    // Assembly-area colour swatches
    private JPanel makeColourRow(String label, boolean forZen) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel lbl = new JLabel(label);
        lbl.setPreferredSize(new Dimension(110, 28));
        lbl.putClientProperty("settings-role", "value");
        row.add(lbl, BorderLayout.WEST);

        JPanel swatches = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        swatches.setOpaque(false);
        for (int i = 0; i < FixThePotGamePanel.ASSEMBLY_COLOUR_PRESETS.length; i++) {
            final int argb = FixThePotGamePanel.ASSEMBLY_COLOUR_PRESETS[i][0];
            String tooltip = FixThePotGamePanel.ASSEMBLY_COLOUR_LABELS[i];
            JButton sw = makeSwatchButton(argb, tooltip);
            sw.addActionListener(e -> {
                host.applyAssemblyAreaColor(forZen, argb);
                repaintSwatchRow(row, forZen);
            });
            swatches.add(sw);
        }
        JButton reset = makeSmallButton("Reset");
        reset.setPreferredSize(new Dimension(72, 28));
        reset.addActionListener(e -> {
            host.resetAssemblyAreaColor(forZen);
            repaintSwatchRow(row, forZen);
        });
        swatches.add(reset);

        row.add(swatches, BorderLayout.EAST);
        return row;
    }

    private JButton makeSwatchButton(int argb, String tooltip) {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(28, 28));
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setToolTipText(tooltip);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder());
        b.setIcon(new ImageIcon(makeSwatchImage(new Color(argb, true), false)));
        b.putClientProperty("swatch-argb", argb);
        return b;
    }

    private java.awt.image.BufferedImage makeSwatchImage(Color fill, boolean selected) {
        int sz = 24;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                sz, sz, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillRoundRect(1, 1, sz - 2, sz - 2, 8, 8);
        Color stroke = selected
                ? ThemeManager.get().palette().base.text
                : ThemeManager.get().palette().base.mutedText;
        g2.setColor(stroke);
        g2.setStroke(new BasicStroke(selected ? 2f : 1f));
        g2.drawRoundRect(1, 1, sz - 2, sz - 2, 8, 8);
        g2.dispose();
        return img;
    }

    // Redraw every swatch in the row so the one matching the saved colour gets the thicker "selected" stroke.
    private void repaintSwatchRow(JPanel row, boolean forZen) {
        int current = host.getAssemblyAreaColor(forZen);
        for (Component c1 : row.getComponents()) {
            if (!(c1 instanceof JPanel)) continue;
            for (Component c2 : ((JPanel) c1).getComponents()) {
                if (!(c2 instanceof JButton)) continue;
                JButton b = (JButton) c2;
                Object a = b.getClientProperty("swatch-argb");
                if (a instanceof Integer) {
                    int argb = (Integer) a;
                    boolean selected = argb == current;
                    b.setIcon(new ImageIcon(makeSwatchImage(new Color(argb, true), selected)));
                }
            }
        }
    }

    // Help tab content
    // Build the keyboard shortcuts reference (previously in HelpOverlay).
    private void buildHelpContent() {
        addHelpSection("Keyboard Shortcuts");
        addHelpRow("\u2190  /  \u2192", "Previous / next jigsaw");
        addHelpRow("R", "Restart current jigsaw");
        addHelpRow("I", "Open the artefact information overlay");
        addHelpRow("H", "Cycle hint tier (off \u2192 edges \u2192 corners \u2192 guide)");
        addHelpRow("Z", "Toggle Zen mode");
        addHelpRow("T", "Pause / resume the timer");
        addHelpRow("M", "Mute / unmute music");
        addHelpRow("F", "Favourite / unfavourite the current jigsaw");
        addHelpRow("C", "Show the completed image as a reference");
        addHelpRow("S", "Open this dialog on the Settings tab");
        addHelpRow("?", "Open this dialog on the Help tab");
        addHelpRow("ESC", "Dismiss any open overlay");

        helpContent.add(Box.createVerticalStrut(14));
        addHelpSection("Hint Tiers");
        addHelpRow("Tier 0", "No hint — all pieces visible normally");
        addHelpRow("Tier 1", "Highlight only the edge pieces");
        addHelpRow("Tier 2", "Highlight only the corner pieces");
        addHelpRow("Tier 3", "Highlight a single guided piece showing where to place next");

        helpContent.add(Box.createVerticalStrut(14));
        addHelpSection("Navigation");
        addHelpRow("Back button", "Returns to Collections directly from the game panel");
        addHelpRow("Click outside", "Dismisses most overlays");
        addHelpRow("Continue / \u2192", "Advances through reflection prompts and post-puzzle screens");
    }

    private void addHelpSection(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.putClientProperty("settings-role", "section");
        helpContent.add(label);
        helpContent.add(Box.createVerticalStrut(6));
    }

    private void addHelpRow(String key, String description) {
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel keyLabel = new JLabel(key);
        keyLabel.setPreferredSize(new Dimension(110, 20));
        keyLabel.setHorizontalAlignment(SwingConstants.LEFT);
        keyLabel.putClientProperty("settings-role", "help-key");

        JLabel descLabel = new JLabel(description);
        descLabel.putClientProperty("settings-role", "help-desc");

        row.add(keyLabel, BorderLayout.WEST);
        row.add(descLabel, BorderLayout.CENTER);
        helpContent.add(row);
        helpContent.add(Box.createVerticalStrut(3));
    }

    // Settings tab rows

    private void rebuildButtonRows() {
        buttonsListPanel.removeAll();

        GameState st = host.getSaveState();
        java.util.List<String> sourceList = currentHiddenList(st);
        java.util.Set<String> hidden = sourceList != null
                ? new java.util.HashSet<>(sourceList)
                : new java.util.HashSet<>();
        LinkedHashMap<String, String> labels = host.getButtonLabels();

        for (int i = 0; i < workingOrder.size(); i++) {
            String id = workingOrder.get(i);
            if ("settings".equals(id)) continue;

            String label = labels.getOrDefault(id, id);
            boolean visible = !hidden.contains(id);
            buttonsListPanel.add(makeButtonRow(id, label, visible, i));
            buttonsListPanel.add(Box.createVerticalStrut(2));
        }

        buttonsListPanel.revalidate();
        buttonsListPanel.repaint();
    }

    private JPanel makeButtonRow(String id, String label, boolean visible, int indexInWorking) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        row.setBorder(new EmptyBorder(2, 4, 2, 4));

        JCheckBox check = new JCheckBox(label, visible);
        check.setOpaque(false);
        check.setFocusPainted(false);
        check.putClientProperty("settings-role", "check");
        check.addActionListener(e -> {
            GameState st = host.getSaveState();
            if (st == null) return;
            java.util.List<String> target = currentHiddenList(st);
            if (target == null) return;
            if (check.isSelected()) {
                target.remove(id);
            } else if (!target.contains(id)) {
                target.add(id);
            }
            host.applyButtonCustomization();
            host.persistSaveState();
        });
        row.add(check, BorderLayout.CENTER);

        // Right-hand controls: optional inline state toggle + up/down arrows
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightControls.setOpaque(false);

        JButton stateToggle = createInlineStateToggle(id);
        if (stateToggle != null) {
            rightControls.add(stateToggle);
        }

        JButton up = makeArrowButton("\u25B2"); // up arrow
        up.setEnabled(indexInWorking > 0);
        up.addActionListener(e -> moveButton(id, -1));
        rightControls.add(up);

        JButton down = makeArrowButton("\u25BC"); // down arrow
        down.setEnabled(indexInWorking < workingOrder.size() - 1);
        down.addActionListener(e -> moveButton(id, +1));
        rightControls.add(down);

        row.add(rightControls, BorderLayout.EAST);
        return row;
    }

    // For buttons that have a meaningful binary state (theme, music, zen, timer),
    // returns a small inline toggle that shows the current state and flips it when
    // clicked, saving the user a trip to the actual toolbar. Returns null for
    // buttons without on/off state (previous, restart, back, etc.).
    private JButton createInlineStateToggle(String id) {
        switch (id) {
            case "theme":  return makeThemeToggle();
            case "music":  return makeMusicToggle();
            case "zen":    return makeZenToggle();
            case "timer":  return makeTimerToggle();
            default:       return null;
        }
    }

    private JButton makeThemeToggle() {
        JButton b = makeStateToggleButton();
        b.setText(themeLabel());
        b.addActionListener(e -> {
            ThemeManager.get().toggleTheme();
            if (host.getSaveState() != null) {
                host.getSaveState().theme = ThemeManager.get().getCurrent().name();
                host.persistSaveState();
            }
            b.setText(themeLabel());
        });
        return b;
    }

    private String themeLabel() {
        String n = ThemeManager.get().getCurrent().name();
        return n.length() > 0 ? (n.charAt(0) + n.substring(1).toLowerCase()) : n;
    }

    private JButton makeMusicToggle() {
        JButton b = makeStateToggleButton();
        b.setText(host.isMusicPlaying() ? "Playing" : "Muted");
        b.addActionListener(e -> {
            host.triggerMusicToggle();
            b.setText(host.isMusicPlaying() ? "Playing" : "Muted");
        });
        return b;
    }

    private JButton makeZenToggle() {
        JButton b = makeStateToggleButton();
        b.setText(host.isZenMode() ? "On" : "Off");
        b.addActionListener(e -> {
            host.triggerZenToggle();
            b.setText(host.isZenMode() ? "On" : "Off");
        });
        return b;
    }

    private JButton makeTimerToggle() {
        JButton b = makeStateToggleButton();
        b.setText(host.isTimerRunning() ? "Running" : "Paused");
        b.addActionListener(e -> {
            host.triggerTimerToggle();
            b.setText(host.isTimerRunning() ? "Running" : "Paused");
        });
        return b;
    }

    private JButton makeStateToggleButton() {
        JButton b = new JButton();
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(80, 26));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(
                        ThemeManager.get().palette().base.mutedText, 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        b.putClientProperty("settings-role", "state-btn");
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                b.setBorder(BorderFactory.createCompoundBorder(
                        new javax.swing.border.LineBorder(
                                ThemeManager.get().palette().base.text, 1, true),
                        BorderFactory.createEmptyBorder(2, 6, 2, 6)));
            }
            @Override public void mouseExited(MouseEvent e) {
                b.setBorder(BorderFactory.createCompoundBorder(
                        new javax.swing.border.LineBorder(
                                ThemeManager.get().palette().base.mutedText, 1, true),
                        BorderFactory.createEmptyBorder(2, 6, 2, 6)));
            }
        });
        return b;
    }

    private void moveButton(String id, int delta) {
        int idx = workingOrder.indexOf(id);
        if (idx < 0) return;
        int target = idx + delta;
        if (target < 0 || target >= workingOrder.size()) return;
        java.util.Collections.swap(workingOrder, idx, target);

        GameState st = host.getSaveState();
        if (st != null) {
            st.buttonOrder = new ArrayList<>(workingOrder);
            host.applyButtonCustomization();
            host.persistSaveState();
        }
        rebuildButtonRows();
    }

    // Default hidden-button set for zen mode — everything except navigation + help + settings.
    private static final java.util.List<String> ZEN_DEFAULT_HIDDEN = java.util.List.of(
            "back", "music", "trackChooser", "restart", "timer", "info",
            "showCompleted", "split", "chooser", "theme", "hint", "favourite"
    );

    private void resetCurrentModeToDefaults() {
        GameState st = host.getSaveState();
        if (st == null) return;
        if (editingZen) {
            st.zenHiddenButtons.clear();
            st.zenHiddenButtons.addAll(ZEN_DEFAULT_HIDDEN);
        } else {
            st.hiddenButtons.clear();
        }
        // Order is shared across modes; only reset it when clearing normal mode,
        // since that's the mode whose "defaults" include the built-in order.
        if (!editingZen) {
            st.buttonOrder.clear();
        }
        host.applyButtonCustomization();
        host.persistSaveState();
        refreshFromSaveState();
    }

    // Which list the current tab edits — normal or zen hidden buttons.
    private java.util.List<String> currentHiddenList(GameState st) {
        if (st == null) return null;
        return editingZen ? st.zenHiddenButtons : st.hiddenButtons;
    }

    private void setEditingMode(boolean zen) {
        if (editingZen == zen) return;
        editingZen = zen;
        updateModeButtons();
        rebuildButtonRows();

        // Clicking the Normal / Zen selector doesn't just change what the dock list shows,
        // it also switches the app's live mode to match.
        // Users reasonably expect "clicking Zen" to mean "I want to be in zen mode now",
        // and it lets them preview the dock they're configuring in real time.
        if (host.isZenMode() != zen) {
            host.triggerZenToggle();
        }
    }

    private void updateModeButtons() {
        if (modeNormalBtn == null || modeZenBtn == null) return;
        styleModeButton(modeNormalBtn, !editingZen);
        styleModeButton(modeZenBtn, editingZen);
    }

    private JButton makeModeButton(String text, boolean isZen) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Boolean active = (Boolean) getClientProperty("mode-active");
                Boolean hover  = (Boolean) getClientProperty("mode-hover");
                int alpha = Boolean.TRUE.equals(active) ? 180
                          : Boolean.TRUE.equals(hover)  ? 110
                                                        : 60;
                drawTextPill(this, g, alpha);
                super.paintComponent(g);
            }
        };
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension size = new Dimension(88, 32);
        b.setPreferredSize(size);
        b.setMinimumSize(size);
        b.setMaximumSize(size);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.putClientProperty("settings-role", "mode-btn");
        b.putClientProperty("mode-zen", isZen);
        b.putClientProperty("mode-active", false);
        b.putClientProperty("mode-hover", false);
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                b.putClientProperty("mode-hover", true); b.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                b.putClientProperty("mode-hover", false); b.repaint();
            }
        });
        return b;
    }

    // Apply the active/inactive state for a mode-selector button. The visual
    // pill opacity is driven from this flag in paintComponent, active = solid
    // halo, inactive = subtle, and the mouse listener adds an intermediate
    // hover level.
    private void styleModeButton(JButton b, boolean active) {
        Color textC = ThemeManager.get().palette().base.text;
        Color mutedC = ThemeManager.get().palette().base.mutedText;
        b.putClientProperty("mode-active", active);
        b.setForeground(active ? textC : mutedC);
        b.repaint();
    }

    // styling helpers

    private JButton makeSmallButton(String text) {
        final float normalFont = 13f;
        final float hoverFont  = 16f;
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Boolean hover = (Boolean) getClientProperty("btn-hover");
                drawTextPill(this, g, Boolean.TRUE.equals(hover) ? 180 : 150);
                super.paintComponent(g);
            }
        };
        b.setFont(b.getFont().deriveFont(Font.BOLD, normalFont));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension size = new Dimension(140, 34);
        b.setPreferredSize(size);
        b.setMinimumSize(size);
        b.setMaximumSize(size);
        b.putClientProperty("settings-role", "small-btn");
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                b.setFont(b.getFont().deriveFont(Font.BOLD, hoverFont));
                b.putClientProperty("btn-hover", true);
                b.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                b.setFont(b.getFont().deriveFont(Font.BOLD, normalFont));
                b.putClientProperty("btn-hover", false);
                b.repaint();
            }
        });
        return b;
    }

    private JButton makeFooterButton(String text) {
        final float normalFont = 16f;
        final float hoverFont  = 20f;
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Boolean hover = (Boolean) getClientProperty("btn-hover");
                drawTextPill(this, g, Boolean.TRUE.equals(hover) ? 180 : 150);
                super.paintComponent(g);
            }
        };
        b.setFont(b.getFont().deriveFont(Font.BOLD, normalFont));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension size = new Dimension(180, 44);
        b.setPreferredSize(size);
        b.setMinimumSize(size);
        b.setMaximumSize(size);
        b.putClientProperty("settings-role", "footer-btn");
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                b.setFont(b.getFont().deriveFont(Font.BOLD, hoverFont));
                b.putClientProperty("btn-hover", true);
                b.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                b.setFont(b.getFont().deriveFont(Font.BOLD, normalFont));
                b.putClientProperty("btn-hover", false);
                b.repaint();
            }
        });
        return b;
    }

    // Paint a luminance-adaptive rounded pill behind the button's text,
    // matches the halo used by every other actionable button in the app
    // (main menu, profile card, map panels, new-profile / avatar overlays).
    // Used only for the primary settings actions (Close, Reset dock,
    // Show tutorial, Reset colour, Normal / Zen tabs); fine-grained list
    // controls keep their existing form styling to stay readable.
    private static void drawTextPill(JButton btn, Graphics g, int alpha) {
        String text = btn.getText();
        if (text == null || text.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color tc = btn.getForeground();
        double lum = 0.299 * tc.getRed() + 0.587 * tc.getGreen() + 0.114 * tc.getBlue();
        Color halo = lum > 128 ? new Color(0, 0, 0, alpha) : new Color(255, 255, 255, alpha);
        FontMetrics fm = g2.getFontMetrics(btn.getFont());
        int textW = fm.stringWidth(text);
        int textH = fm.getAscent();
        int padX = 14, padY = 6;
        int pillW = textW + padX * 2;
        int pillH = textH + padY * 2;
        int pillX = (btn.getWidth() - pillW) / 2;
        int pillY = (btn.getHeight() - pillH) / 2;
        g2.setColor(halo);
        g2.fillRoundRect(pillX, pillY, pillW, pillH, 20, 20);
        g2.dispose();
    }

    private JButton makeArrowButton(String glyph) {
        JButton b = new JButton(glyph);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFocusable(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Widen so the glyph doesn't get truncated to "..." and remove default button
        // margins. Use SansSerif explicitly because the theme's Serif body font doesn't
        // render the Unicode up/down arrows cleanly at small sizes.
        b.setPreferredSize(new Dimension(40, 26));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.putClientProperty("settings-role", "arrow-btn");
        return b;
    }

    @Override
    public void refreshTheme() {
        Theme.Palette.Base b = ThemeManager.get().palette().base;
        Theme.Palette.Fonts f = ThemeManager.get().palette().fonts;

        Color cardBg = new Color(b.controlBg.getRed(), b.controlBg.getGreen(), b.controlBg.getBlue(), 235);
        card.setBackground(cardBg);

        titleLabel.setFont(f.heading);
        titleLabel.setForeground(b.text);

        tabs.setBackground(cardBg);
        tabs.setForeground(b.text);
        tabs.setFont(f.bodyBold.deriveFont(Font.BOLD, 14f));

        styleByRole(card, f, b);
        updateModeButtons();
        repaint();
    }

    private void styleByRole(Container parent, Theme.Palette.Fonts f, Theme.Palette.Base b) {
        for (Component c : parent.getComponents()) {
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                Object role = l.getClientProperty("settings-role");
                if ("section".equals(role)) {
                    l.setFont(f.bodyBold.deriveFont(Font.BOLD, 16f));
                    l.setForeground(b.text);
                } else if ("hint".equals(role)) {
                    l.setFont(f.caption);
                    l.setForeground(b.mutedText);
                } else if ("value".equals(role)) {
                    l.setFont(f.body);
                    l.setForeground(b.text);
                } else if ("help-key".equals(role)) {
                    l.setFont(f.body.deriveFont(Font.BOLD, 13f));
                    l.setForeground(b.text);
                } else if ("help-desc".equals(role)) {
                    l.setFont(f.body.deriveFont(13f));
                    l.setForeground(b.mutedText);
                }
            } else if (c instanceof JCheckBox) {
                JCheckBox cb = (JCheckBox) c;
                cb.setFont(f.body);
                cb.setForeground(b.text);
            } else if (c instanceof JButton) {
                JButton btn = (JButton) c;
                Object role = btn.getClientProperty("settings-role");
                btn.setForeground(b.text);
                if ("arrow-btn".equals(role)) {
                    // Don't override the SansSerif font set in makeArrowButton, the theme's
                    // body font (Serif) doesn't render Unicode arrow glyphs cleanly.
                } else if ("small-btn".equals(role)) {
                    // Bold base matches the haloed-button vocabulary used
                    // elsewhere; the mouse listener flips between 13f/16f bold.
                    btn.setFont(f.button.deriveFont(Font.BOLD, 13f));
                } else if ("state-btn".equals(role)) {
                    btn.setFont(f.body.deriveFont(12f));
                } else if ("mode-btn".equals(role)) {
                    btn.setFont(f.bodyBold.deriveFont(Font.BOLD, 13f));
                } else if ("footer-btn".equals(role)) {
                    btn.setFont(f.button.deriveFont(Font.BOLD, 16f));
                }
            }
            if (c instanceof Container) {
                styleByRole((Container) c, f, b);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}
