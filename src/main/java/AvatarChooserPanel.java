import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AvatarChooserPanel extends JPanel implements ThemeAware {

    public interface AvatarListener {
        void onAvatarConfirmed(String style, String seed, Map<String, String> options, String cachedPath);
        void onAvatarCancelled();
    }

    private AvatarListener listener;
    private JLabel previewLabel;
    private JPanel previewContainer;
    private JPanel optionsPanel;
    private JPanel rightSide;
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JButton confirmBtn, cancelBtn, randomBtn;
    private BufferedImage currentPreview;
    private BufferedImage backgroundImage;
    private String profileName = "";

    private int styleIdx = 0;
    private int[] optIdx = new int[4];

    private JLabel styleValLbl;
    private JLabel slotALbl, slotBLbl, slotCLbl, slotDLbl;
    private JLabel slotAValLbl, slotBValLbl, slotCValLbl, slotDValLbl;
    private JPanel slotASwatch, slotBSwatch, slotCSwatch, slotDSwatch;
    private JPanel slotARow, slotBRow, slotCRow, slotDRow;

    // Style definitions
    private static final String[] STYLES = {
            "adventurer", "adventurer-neutral", "avataaars", "big-ears",
            "bottts", "lorelei", "notionists", "pixel-art", "thumbs"
    };
    private static final String[] STYLE_DISPLAY = {
            "Adventurer", "Adventurer Neutral", "Avataaars", "Big Ears",
            "Bottts", "Lorelei", "Notionists", "Pixel Art", "Thumbs"
    };

    private static final StyleConfig[] STYLE_CONFIGS = {
            new StyleConfig(
                    new Opt("Hair", "hair",
                            new String[]{"long01","long02","long03","long04","long05","long06","long07","long08","long09","long10","long11","long12","long13","long14","long15","long16","long17","long18","long19","long20","long21","long22","long23","long24","long25","long26","short01","short02","short03","short04","short05","short06","short07","short08","short09","short10","short11","short12","short13","short14","short15","short16","short17","short18","short19"},
                            null),
                    new Opt("Eyes", "eyes",
                            new String[]{"variant01","variant02","variant03","variant04","variant05","variant06","variant07","variant08","variant09","variant10","variant11","variant12","variant13","variant14","variant15","variant16","variant17","variant18","variant19","variant20","variant21","variant22","variant23","variant24","variant25","variant26"},
                            null),
                    new Opt("Mouth", "mouth",
                            new String[]{"variant01","variant02","variant03","variant04","variant05","variant06","variant07","variant08","variant09","variant10","variant11","variant12","variant13","variant14","variant15","variant16","variant17","variant18","variant19","variant20","variant21","variant22","variant23","variant24","variant25","variant26","variant27","variant28","variant29","variant30"},
                            null),
                    new Opt("Skin", "skinColor",
                            new String[]{"f2d3b1","d4a574","c68642","8d5524"},
                            new String[]{"Fair","Tan","Brown","Dark Brown"})
            ),
            new StyleConfig(
                    new Opt("Hair", "hair",
                            new String[]{"long01","long02","long03","long04","long05","long06","long07","long08","long09","long10","long11","long12","long13","long14","long15","long16","long17","long18","long19","long20","long21","long22","long23","long24","long25","long26","short01","short02","short03","short04","short05","short06","short07","short08","short09","short10","short11","short12","short13","short14","short15","short16","short17","short18","short19"},
                            null),
                    new Opt("Eyes", "eyes",
                            new String[]{"variant01","variant02","variant03","variant04","variant05","variant06","variant07","variant08","variant09","variant10","variant11","variant12","variant13","variant14","variant15","variant16","variant17","variant18","variant19","variant20","variant21","variant22","variant23","variant24","variant25","variant26"},
                            null),
                    new Opt("Mouth", "mouth",
                            new String[]{"variant01","variant02","variant03","variant04","variant05","variant06","variant07","variant08","variant09","variant10","variant11","variant12","variant13","variant14","variant15","variant16","variant17","variant18","variant19","variant20","variant21","variant22","variant23","variant24","variant25","variant26","variant27","variant28","variant29","variant30"},
                            null),
                    new Opt("Background", "backgroundColor",
                            new String[]{"9e5622","763900","ecad80","f2d3b1","b6e3f4","c0aede","d1d4f9","ffd5dc","ffdfbf"},
                            new String[]{"Brown","Dark Brown","Tan","Fair","Blue","Purple","Lavender","Pink","Peach"})
            ),
            new StyleConfig(
                    new Opt("Hair", "top",
                            new String[]{"bigHair","bob","bun","curly","curvy","dreads","frida","fro","froAndBand","longButNotTooLong","miaWallace","shaggy","shavedSides","straight01","straight02","straightAndStrand","theCaesar","theCaesarAndSidePart","winterHat01","winterHat02","winterHat03","winterHat04","hijab","turban"},
                            new String[]{"Big Hair","Bob","Bun","Curly","Curvy","Dreads","Frida","Fro","Fro & Band","Long","Mia Wallace","Shaggy","Shaved Sides","Straight 1","Straight 2","Straight & Strand","Caesar","Caesar & Part","Winter Hat 1","Winter Hat 2","Winter Hat 3","Winter Hat 4","Hijab","Turban"}),
                    new Opt("Eyes", "eyes",
                            new String[]{"closed","cry","default","eyeRoll","happy","hearts","side","squint","surprised","wink","winkWacky","xDizzy"},
                            new String[]{"Closed","Cry","Default","Eye Roll","Happy","Hearts","Side","Squint","Surprised","Wink","Wink Wacky","X Dizzy"}),
                    new Opt("Mouth", "mouth",
                            new String[]{"concerned","default","disbelief","eating","grimace","sad","screamOpen","serious","smile","tongue","twinkle","vomit"},
                            new String[]{"Concerned","Default","Disbelief","Eating","Grimace","Sad","Scream","Serious","Smile","Tongue","Twinkle","Vomit"}),
                    new Opt("Skin", "skinColor",
                            new String[]{"614335","d08b5b","ae5d29","ffdbb4","edb98a","fd9841"},
                            new String[]{"Dark Brown","Brown","Medium","Light","Beige","Tan"})
            ),
            new StyleConfig(
                    new Opt("Hair", "_seed_hair",
                            new String[]{"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t"},
                            new String[]{"Style 1","Style 2","Style 3","Style 4","Style 5","Style 6","Style 7","Style 8","Style 9","Style 10","Style 11","Style 12","Style 13","Style 14","Style 15","Style 16","Style 17","Style 18","Style 19","Style 20"}),
                    new Opt("Eyes", "_seed_eyes",
                            new String[]{"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p"},
                            new String[]{"Style 1","Style 2","Style 3","Style 4","Style 5","Style 6","Style 7","Style 8","Style 9","Style 10","Style 11","Style 12","Style 13","Style 14","Style 15","Style 16"}),
                    new Opt("Mouth", "_seed_mouth",
                            new String[]{"a","b","c","d","e","f","g","h","i","j","k","l"},
                            new String[]{"Style 1","Style 2","Style 3","Style 4","Style 5","Style 6","Style 7","Style 8","Style 9","Style 10","Style 11","Style 12"}),
                    new Opt("Face", "_seed_face",
                            new String[]{"a","b","c","d","e","f","g","h","i","j"},
                            new String[]{"Style 1","Style 2","Style 3","Style 4","Style 5","Style 6","Style 7","Style 8","Style 9","Style 10"})
            ),
            new StyleConfig(
                    new Opt("Eyes", "eyes",
                            new String[]{"bulging","dizzy","eva","frame1","frame2","glow","happy","hearts","robocop","round","roundFrame01","roundFrame02","sensor","shade01"},
                            new String[]{"Bulging","Dizzy","Eva","Frame 1","Frame 2","Glow","Happy","Hearts","Robocop","Round","Round Frame 1","Round Frame 2","Sensor","Shade"}),
                    new Opt("Face", "face",
                            new String[]{"round01","round02","square01","square02","square03","square04"},
                            new String[]{"Round 1","Round 2","Square 1","Square 2","Square 3","Square 4"}),
                    new Opt("Mouth", "mouth",
                            new String[]{"bite","diagram","grill01","grill02","grill03","smile01","smile02","square01","square02"},
                            new String[]{"Bite","Diagram","Grill 1","Grill 2","Grill 3","Smile 1","Smile 2","Square 1","Square 2"}),
                    null
            ),
            new StyleConfig(
                    new Opt("Hair", "hair",
                            new String[]{"variant01","variant02","variant03","variant04","variant05","variant06","variant07","variant08","variant09","variant10","variant11","variant12","variant13","variant14","variant15","variant16","variant17","variant18","variant19","variant20","variant21","variant22","variant23","variant24","variant25","variant26","variant27","variant28","variant29","variant30","variant31","variant32","variant33","variant34","variant35","variant36","variant37","variant38","variant39","variant40","variant41","variant42","variant43","variant44","variant45","variant46","variant47","variant48"},
                            null),
                    new Opt("Eyes", "eyes",
                            new String[]{"variant01","variant02","variant03","variant04","variant05","variant06","variant07","variant08","variant09","variant10","variant11","variant12","variant13","variant14","variant15","variant16","variant17","variant18","variant19","variant20","variant21","variant22","variant23","variant24"},
                            null),
                    new Opt("Mouth", "mouth",
                            new String[]{"happy01","happy02","happy03","happy04","happy05","happy06","happy07","happy08","happy09","happy10","happy11","happy12","happy13","happy14","sad01","sad02","sad03","sad04","sad05","sad06","sad07","sad08","sad09","sad10","sad11","sad12","sad13"},
                            new String[]{"Happy 1","Happy 2","Happy 3","Happy 4","Happy 5","Happy 6","Happy 7","Happy 8","Happy 9","Happy 10","Happy 11","Happy 12","Happy 13","Happy 14","Sad 1","Sad 2","Sad 3","Sad 4","Sad 5","Sad 6","Sad 7","Sad 8","Sad 9","Sad 10","Sad 11","Sad 12","Sad 13"}),
                    null
            ),
            new StyleConfig(
                    new Opt("Hair", "hair",
                            new String[]{"variant01","variant02","variant03","variant04","variant05","variant06","variant07","variant08","variant09","variant10","variant11","variant12","variant13","variant14","variant15","variant16","variant17","variant18","variant19","variant20","variant21","variant22","variant23","variant24","variant25","variant26","variant27","variant28","variant29","variant30","variant31","variant32","variant33","variant34","variant35","variant36","variant37","variant38","variant39","variant40","variant41","variant42","variant43","variant44","variant45","variant46","variant47","variant48","variant49","variant50","variant51","variant52","variant53","variant54","variant55","variant56","variant57","variant58","variant59","variant60","variant61","variant62","variant63","variant64"},
                            null),
                    new Opt("Eyes", "eyes",
                            new String[]{"variant01","variant02","variant03","variant04","variant05"},
                            null),
                    new Opt("Lips", "lips",
                            new String[]{"variant01","variant02","variant03","variant04","variant05","variant06","variant07","variant08","variant09","variant10","variant11","variant12","variant13","variant14","variant15","variant16","variant17","variant18","variant19","variant20","variant21","variant22","variant23","variant24","variant25","variant26","variant27","variant28","variant29","variant30"},
                            null),
                    new Opt("Nose", "nose",
                            new String[]{"variant01","variant02","variant03","variant04","variant05","variant06","variant07","variant08","variant09","variant10","variant11","variant12","variant13","variant14","variant15","variant16","variant17","variant18","variant19","variant20"},
                            null)
            ),
            new StyleConfig(
                    new Opt("Hair", "hair",
                            new String[]{"long01","long02","long03","long04","long05","long06","long07","long08","long09","long10","long11","long12","long13","long14","long15","long16","long17","long18","long19","long20","long21","short01","short02","short03","short04","short05","short06","short07","short08","short09","short10","short11","short12","short13","short14","short15","short16","short17","short18","short19","short20","short21","short22","short23","short24"},
                            null),
                    new Opt("Eyes", "eyes",
                            new String[]{"variant01","variant02","variant03","variant04","variant05","variant06","variant07","variant08","variant09","variant10","variant11","variant12"},
                            null),
                    new Opt("Mouth", "mouth",
                            new String[]{"happy01","happy02","happy03","happy04","happy05","happy06","happy07","happy08","happy09","happy10","happy11","happy12","sad01","sad02","sad03","sad04","sad05","sad06","sad07","sad08","sad09","sad10","sad11"},
                            new String[]{"Happy 1","Happy 2","Happy 3","Happy 4","Happy 5","Happy 6","Happy 7","Happy 8","Happy 9","Happy 10","Happy 11","Happy 12","Sad 1","Sad 2","Sad 3","Sad 4","Sad 5","Sad 6","Sad 7","Sad 8","Sad 9","Sad 10","Sad 11"}),
                    new Opt("Skin", "skinColor",
                            new String[]{"f2d3b1","d4a574","c68642","8d5524","5c3a1e","ffdbb4","e8b98a","c48e6a"},
                            new String[]{"Fair","Light Tan","Tan","Brown","Dark Brown","Peach","Warm Beige","Bronze"})
            ),
            new StyleConfig(
                    new Opt("Eyes", "eyes",
                            new String[]{"variant1W10","variant1W12","variant1W14","variant1W16","variant2W10","variant2W12","variant2W14","variant2W16","variant3W10","variant3W12","variant3W14","variant3W16","variant4W10","variant4W12","variant4W14","variant4W16","variant5W10","variant5W12","variant5W14","variant5W16","variant6W10","variant6W12","variant6W14","variant6W16","variant7W10","variant7W12","variant7W14","variant7W16","variant8W10","variant8W12","variant8W14","variant8W16","variant9W10","variant9W12","variant9W14","variant9W16"},
                            null),
                    new Opt("Mouth", "mouth",
                            new String[]{"variant1","variant2","variant3","variant4","variant5"},
                            new String[]{"Style 1","Style 2","Style 3","Style 4","Style 5"}),
                    new Opt("Face", "face",
                            new String[]{"variant1","variant2","variant3","variant4","variant5"},
                            new String[]{"Style 1","Style 2","Style 3","Style 4","Style 5"}),
                    new Opt("Shape", "shapeColor",
                            new String[]{"0a5b83","1c799f","69d2e7","f1f4dc","f88c49"},
                            new String[]{"Navy","Teal","Sky Blue","Cream","Orange"})
            )
    };

    // Helper classes
    private static class Opt {
        final String label;
        final String paramName;
        final String[] values;
        final String[] display;

        Opt(String label, String paramName, String[] values, String[] display) {
            this.label = label;
            this.paramName = paramName;
            this.values = values;
            this.display = display;
        }

        String displayAt(int idx) {
            if (display != null && idx < display.length) return display[idx];
            String v = values[idx];
            if (v.startsWith("variant") && v.contains("W")) {
                String num = v.replaceAll("variant(\\d+)W.*", "$1");
                String w = v.replaceAll(".*W(\\d+)", "$1");
                return "Style " + num + " (Width " + w + ")";
            }
            if (v.startsWith("variant")) return "Style " + v.replace("variant", "").replaceFirst("^0+", "");
            if (v.startsWith("long")) return "Long " + v.replace("long", "").replaceFirst("^0+", "");
            if (v.startsWith("short")) return "Short " + v.replace("short", "").replaceFirst("^0+", "");
            if (v.startsWith("happy")) return "Happy " + v.replace("happy", "").replaceFirst("^0+", "");
            if (v.startsWith("sad")) return "Sad " + v.replace("sad", "").replaceFirst("^0+", "");
            return v.substring(0, 1).toUpperCase() + v.substring(1);
        }

        boolean isSkinOrColor() {
            return paramName.toLowerCase().contains("color") || paramName.toLowerCase().contains("skin");
        }
    }

    private static class StyleConfig {
        final Opt[] opts;

        StyleConfig(Opt a, Opt b, Opt c, Opt d) {
            List<Opt> list = new ArrayList<>();
            if (a != null) list.add(a);
            if (b != null) list.add(b);
            if (c != null) list.add(c);
            if (d != null) list.add(d);
            opts = list.toArray(new Opt[0]);
        }
    }

    // Constructor
    public AvatarChooserPanel() {
        setLayout(new BorderLayout());
        setOpaque(true);
        buildUI();
        randomise();
    }

    private void buildUI() {
        Theme.Palette p = ThemeManager.get().palette();

        setBackground(p.base.appBg);

        // Left: large preview
        previewContainer = new JPanel(new GridBagLayout());
        previewContainer.setOpaque(false);
        previewContainer.setPreferredSize(new Dimension(350, 0));

        previewLabel = new JLabel();
        previewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewLabel.setVerticalAlignment(SwingConstants.CENTER);
        previewLabel.setPreferredSize(new Dimension(256, 256));
        previewContainer.add(previewLabel);

        add(previewContainer, BorderLayout.WEST);

        // Right: options
        rightSide = new JPanel(new BorderLayout());
        rightSide.setOpaque(false);
        rightSide.setBorder(BorderFactory.createEmptyBorder(40, 30, 30, 30));

        titleLabel = new JLabel("Choose Your Avatar");
        titleLabel.setFont(p.fonts.title);
        titleLabel.setForeground(p.base.text);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        rightSide.add(titleLabel, BorderLayout.NORTH);

        optionsPanel = new JPanel();
        optionsPanel.setOpaque(false);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));

        // Style row
        styleValLbl = new JLabel();
        JPanel styleRow = createOptionRow(new JLabel("Style"), styleValLbl, null,
                e -> { styleIdx = cycle(styleIdx, STYLES.length, -1); resetOptIndices(); updateOptionDisplays(); refreshPreview(); repaint(); },
                e -> { styleIdx = cycle(styleIdx, STYLES.length, 1); resetOptIndices(); updateOptionDisplays(); refreshPreview(); repaint(); });
        optionsPanel.add(styleRow);
        optionsPanel.add(Box.createVerticalStrut(12));

        // 4 generic slots
        slotALbl = new JLabel(); slotAValLbl = new JLabel(); slotASwatch = createSwatch();
        slotBLbl = new JLabel(); slotBValLbl = new JLabel(); slotBSwatch = createSwatch();
        slotCLbl = new JLabel(); slotCValLbl = new JLabel(); slotCSwatch = createSwatch();
        slotDLbl = new JLabel(); slotDValLbl = new JLabel(); slotDSwatch = createSwatch();

        slotARow = createOptionRow(slotALbl, slotAValLbl, slotASwatch,
                e -> { optIdx[0] = cycle(optIdx[0], curSlotLen(0), -1); updateOptionDisplays(); refreshPreview(); repaint(); },
                e -> { optIdx[0] = cycle(optIdx[0], curSlotLen(0), 1); updateOptionDisplays(); refreshPreview(); repaint(); });
        optionsPanel.add(slotARow);
        optionsPanel.add(Box.createVerticalStrut(12));

        slotBRow = createOptionRow(slotBLbl, slotBValLbl, slotBSwatch,
                e -> { optIdx[1] = cycle(optIdx[1], curSlotLen(1), -1); updateOptionDisplays(); refreshPreview(); repaint(); },
                e -> { optIdx[1] = cycle(optIdx[1], curSlotLen(1), 1); updateOptionDisplays(); refreshPreview(); repaint(); });
        optionsPanel.add(slotBRow);
        optionsPanel.add(Box.createVerticalStrut(12));

        slotCRow = createOptionRow(slotCLbl, slotCValLbl, slotCSwatch,
                e -> { optIdx[2] = cycle(optIdx[2], curSlotLen(2), -1); updateOptionDisplays(); refreshPreview(); repaint(); },
                e -> { optIdx[2] = cycle(optIdx[2], curSlotLen(2), 1); updateOptionDisplays(); refreshPreview(); repaint(); });
        optionsPanel.add(slotCRow);
        optionsPanel.add(Box.createVerticalStrut(12));

        slotDRow = createOptionRow(slotDLbl, slotDValLbl, slotDSwatch,
                e -> { optIdx[3] = cycle(optIdx[3], curSlotLen(3), -1); updateOptionDisplays(); refreshPreview(); repaint(); },
                e -> { optIdx[3] = cycle(optIdx[3], curSlotLen(3), 1); updateOptionDisplays(); refreshPreview(); repaint(); });
        optionsPanel.add(slotDRow);

        optionsPanel.add(Box.createVerticalStrut(30));

        randomBtn = createStyledButton("Randomise");
        randomBtn.addActionListener(e -> randomise());
        randomBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        optionsPanel.add(randomBtn);
        rightSide.add(optionsPanel, BorderLayout.CENTER);

        // Bottom buttons, centred with Back on the left, Confirm on the right,
        // matching the navigational vocabulary used on every other screen.
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        cancelBtn = createStyledButton("\u2190 Back");
        cancelBtn.addActionListener(e -> {
            if (listener != null) listener.onAvatarCancelled();
        });
        buttonPanel.add(cancelBtn);

        confirmBtn = createStyledButton("Confirm");
        confirmBtn.addActionListener(e -> confirm());
        buttonPanel.add(confirmBtn);

        rightSide.add(buttonPanel, BorderLayout.SOUTH);
        add(rightSide, BorderLayout.CENTER);

        ThemeManager.get().register(this);
        refreshTheme();
    }

    // UI helpers
    private JPanel createSwatch() {
        JPanel s = new JPanel();
        s.setPreferredSize(new Dimension(60, 24));
        s.setMaximumSize(new Dimension(60, 24));
        return s;
    }

    private int curSlotLen(int slot) {
        StyleConfig cfg = STYLE_CONFIGS[styleIdx];
        return (slot < cfg.opts.length) ? cfg.opts[slot].values.length : 1;
    }

    private JPanel createOptionRow(JLabel label, JLabel valueLabel, JPanel swatchPanel,
                                   ActionListener onLeft, ActionListener onRight) {
        Theme.Palette p = ThemeManager.get().palette();

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        label.setFont(p.fonts.body);
        label.setForeground(p.base.mutedText);
        label.setPreferredSize(new Dimension(100, 30));
        row.add(label, BorderLayout.WEST);

        JPanel centre = new JPanel(new BorderLayout(6, 0));
        centre.setOpaque(false);

        JButton leftBtn = createArrowButton("\u25C0");
        leftBtn.addActionListener(onLeft);
        centre.add(leftBtn, BorderLayout.WEST);

        JButton rightBtn = createArrowButton("\u25B6");
        rightBtn.addActionListener(onRight);
        centre.add(rightBtn, BorderLayout.EAST);

        JPanel centreContent = new JPanel(new GridBagLayout());
        centreContent.setOpaque(false);

        if (valueLabel != null) {
            valueLabel.setFont(p.fonts.bodyBold);
            valueLabel.setForeground(p.base.text);
            valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
            centreContent.add(valueLabel);
        }

        if (swatchPanel != null) {
            centreContent.add(swatchPanel);
        }

        centre.add(centreContent, BorderLayout.CENTER);
        row.add(centre, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(row, BorderLayout.CENTER);

        JSeparator sep = new JSeparator();
        sep.setForeground(p.base.controlBorder);
        wrapper.add(sep, BorderLayout.SOUTH);

        return wrapper;
    }

    private JButton createArrowButton(String arrow) {
        Theme.Palette p = ThemeManager.get().palette();
        JButton btn = new JButton(arrow);
        btn.setFont(p.fonts.caption);
        btn.setForeground(p.base.mutedText);
        btn.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setForeground(ThemeManager.get().palette().base.text);
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setForeground(ThemeManager.get().palette().base.mutedText);
            }
        });

        return btn;
    }

    // Halo-styled button: rounded pill behind text that contrasts with the
    // foreground, and a font-size bump on hover. Matches the rest of the app
    // (main menu, collections, profile card, new-profile overlay).
    private JButton createStyledButton(String text) {
        final float normalFont = 16f;
        final float hoverFont = 20f;
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                drawTextHalo(this, g);
                super.paintComponent(g);
            }
        };
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, normalFont));
        btn.setFocusPainted(false);
        btn.setFocusable(false);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension size = new Dimension(170, 46);
        btn.setPreferredSize(size);
        btn.setMinimumSize(size);
        btn.setMaximumSize(size);
        applyBtnColors(btn, false);

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setFont(btn.getFont().deriveFont(Font.BOLD, hoverFont));
                btn.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setFont(btn.getFont().deriveFont(Font.BOLD, normalFont));
                btn.repaint();
            }
        });

        return btn;
    }

    private void applyBtnColors(JButton btn, boolean hover) {
        Theme.Palette.Base b = ThemeManager.get().palette().base;
        btn.setForeground(b.text);
    }

    private static void drawTextHalo(JButton btn, Graphics g) {
        String text = btn.getText();
        if (text == null || text.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color tc = btn.getForeground();
        double lum = 0.299 * tc.getRed() + 0.587 * tc.getGreen() + 0.114 * tc.getBlue();
        Color halo = lum > 128 ? new Color(0, 0, 0, 150) : new Color(255, 255, 255, 150);
        FontMetrics fm = g2.getFontMetrics(btn.getFont());
        int textW = fm.stringWidth(text);
        int textH = fm.getAscent();
        int padX = 16, padY = 6;
        int pillW = textW + padX * 2;
        int pillH = textH + padY * 2;
        int pillX = (btn.getWidth() - pillW) / 2;
        int pillY = (btn.getHeight() - pillH) / 2;
        g2.setColor(halo);
        g2.fillRoundRect(pillX, pillY, pillW, pillH, 20, 20);
        g2.dispose();
    }

    // Logic
    private int cycle(int current, int length, int direction) {
        return (current + direction + length) % length;
    }

    private void resetOptIndices() {
        for (int i = 0; i < 4; i++) optIdx[i] = 0;
    }

    private void updateOptionDisplays() {
        Theme.Palette p = ThemeManager.get().palette();
        Theme.Palette.Base b = p.base;

        styleValLbl.setText(STYLE_DISPLAY[styleIdx]);
        styleValLbl.setForeground(b.text);
        styleValLbl.setFont(p.fonts.bodyBold);

        StyleConfig cfg = STYLE_CONFIGS[styleIdx];

        JLabel[] valLabels = {slotAValLbl, slotBValLbl, slotCValLbl, slotDValLbl};
        JLabel[] nameLabels = {slotALbl, slotBLbl, slotCLbl, slotDLbl};
        JPanel[] swatches = {slotASwatch, slotBSwatch, slotCSwatch, slotDSwatch};
        JPanel[] rowPanels = {slotARow, slotBRow, slotCRow, slotDRow};

        for (int i = 0; i < 4; i++) {
            if (i < cfg.opts.length) {
                Opt opt = cfg.opts[i];
                int idx = optIdx[i] % opt.values.length;
                nameLabels[i].setText(opt.label);
                nameLabels[i].setForeground(b.mutedText);
                nameLabels[i].setFont(p.fonts.body);
                rowPanels[i].setVisible(true);

                if (opt.isSkinOrColor()) {
                    valLabels[i].setVisible(false);
                    swatches[i].setVisible(true);
                    try {
                        swatches[i].setBackground(Color.decode("#" + opt.values[idx]));
                    } catch (Exception ignored) {}
                    swatches[i].setToolTipText(opt.displayAt(idx));
                } else {
                    valLabels[i].setVisible(true);
                    valLabels[i].setText(opt.displayAt(idx));
                    valLabels[i].setForeground(b.text);
                    valLabels[i].setFont(p.fonts.bodyBold);
                    swatches[i].setVisible(false);
                }
            } else {
                rowPanels[i].setVisible(false);
            }
        }

        // Update title color
        if (titleLabel != null) titleLabel.setForeground(b.text);

        // Update separator and arrow colors
        updateChildColors(optionsPanel, b.text, b.mutedText, b.separatorColor);
    }

    // Recursively update arrow buttons and separators within the options panel.
    private void updateChildColors(Container container, Color text, Color muted, Color sepColor) {
        for (Component c : container.getComponents()) {
            if (c instanceof JButton) {
                JButton btn = (JButton) c;
                String t = btn.getText();
                if ("\u25C0".equals(t) || "\u25B6".equals(t)) {
                    btn.setForeground(muted);
                }
            } else if (c instanceof JSeparator) {
                ((JSeparator) c).setForeground(sepColor);
            } else if (c instanceof Container) {
                updateChildColors((Container) c, text, muted, sepColor);
            }
        }
    }

    public void setAvatarListener(AvatarListener listener) {
        this.listener = listener;
    }

    // Set the profile name, displayed as the main title.
    public void setProfileName(String name) {
        this.profileName = (name == null) ? "" : name;
        if (titleLabel != null) {
            titleLabel.setText(profileName.isEmpty() ? "Choose Your Avatar" : profileName);
        }
    }

    private void randomise() {
        styleIdx = (int) (Math.random() * STYLES.length);
        StyleConfig cfg = STYLE_CONFIGS[styleIdx];
        for (int i = 0; i < 4; i++) {
            optIdx[i] = (i < cfg.opts.length) ? (int) (Math.random() * cfg.opts[i].values.length) : 0;
        }
        updateOptionDisplays();
        refreshPreview();
    }

    private String buildUrl() {
        StyleConfig cfg = STYLE_CONFIGS[styleIdx];
        StringBuilder url = new StringBuilder("https://api.dicebear.com/9.x/" + STYLES[styleIdx] + "/png?size=256");

        boolean useSeed = false;
        for (Opt opt : cfg.opts) {
            if (opt.paramName.startsWith("_seed_")) {
                useSeed = true;
                break;
            }
        }

        if (useSeed) {
            StringBuilder seed = new StringBuilder(STYLES[styleIdx]);
            for (int i = 0; i < cfg.opts.length; i++) {
                seed.append(optIdx[i] % cfg.opts[i].values.length);
            }
            url.append("&seed=").append(seed);
        } else {
            for (int i = 0; i < cfg.opts.length; i++) {
                Opt opt = cfg.opts[i];
                int idx = optIdx[i] % opt.values.length;
                url.append("&").append(opt.paramName).append("=").append(opt.values[idx]);
            }
        }

        return url.toString();
    }

    private void refreshPreview() {
        String url = buildUrl();
        new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() {
                try {
                    return ImageIO.read(new URL(url));
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    currentPreview = get();
                    if (currentPreview != null) {
                        Image scaled = currentPreview.getScaledInstance(256, 256, Image.SCALE_SMOOTH);
                        previewLabel.setIcon(new ImageIcon(scaled));
                        previewLabel.setText("");
                    } else {
                        previewLabel.setIcon(null);
                        previewLabel.setText("Preview unavailable");
                        previewLabel.setForeground(ThemeManager.get().palette().base.mutedText);
                    }
                } catch (Exception e) {
                    previewLabel.setIcon(null);
                    previewLabel.setText("Preview unavailable");
                    previewLabel.setForeground(ThemeManager.get().palette().base.mutedText);
                }
            }
        }.execute();
    }

    private void confirm() {
        if (listener == null) return;

        Map<String, String> options = new LinkedHashMap<>();
        StyleConfig cfg = STYLE_CONFIGS[styleIdx];
        for (int i = 0; i < cfg.opts.length; i++) {
            Opt opt = cfg.opts[i];
            options.put(opt.paramName, opt.values[optIdx[i] % opt.values.length]);
        }

        String style = STYLES[styleIdx];
        String seed = style + "_" + System.currentTimeMillis();
        String cachedPath = cacheAvatar(currentPreview);

        listener.onAvatarConfirmed(style, seed, options, cachedPath);
    }

    private String cacheAvatar(BufferedImage img) {
        if (img == null) return "";
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            Path dir;
            if (os.contains("win")) {
                String appData = System.getenv("APPDATA");
                dir = Paths.get(appData != null ? appData : System.getProperty("user.home"),
                        "FixTheArtefact", "avatars");
            } else {
                dir = Paths.get(System.getProperty("user.home"), ".fix-the-artefact", "avatars");
            }
            Files.createDirectories(dir);

            String filename = SaveManager.getActiveProfile() + ".png";
            Path file = dir.resolve(filename);
            ImageIO.write(img, "png", file.toFile());
            return file.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static BufferedImage loadCachedAvatar(String path) {
        if (path == null || path.isEmpty()) return null;
        try {
            return ImageIO.read(new File(path));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void refreshTheme() {
        Theme.Palette p = ThemeManager.get().palette();
        Theme theme = ThemeManager.get().getCurrent();

        // Load themed background
        String bgPath = BackgroundCatalog.backgroundFor("/Rome/Artifacts/", theme);
        try (var in = getClass().getResourceAsStream(bgPath)) {
            if (in != null) backgroundImage = ImageIO.read(in);
        } catch (Exception ex) {
            backgroundImage = null;
        }

        setOpaque(false); // We paint our own background

        Theme.Palette.Base b = p.base;

        titleLabel.setFont(p.fonts.title);
        titleLabel.setForeground(b.text);

        // Style buttons from centralised theme
        for (JButton btn : new JButton[]{confirmBtn, cancelBtn, randomBtn}) {
            if (btn != null) applyBtnColors(btn, false);
        }

        updateOptionDisplays();
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Paint background image
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            Theme.Palette.Base b = ThemeManager.get().palette().base;
            g.setColor(b.appBg);
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        // Paint semi-transparent overlay matching the current theme
        boolean dark = ThemeManager.get().getCurrent() == Theme.DARK;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(dark ? new Color(30, 30, 30, 200) : new Color(240, 240, 240, 200));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}
