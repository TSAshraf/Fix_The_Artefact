import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.IntConsumer;

class JigsawChooserPopover extends JPanel implements ThemeAware {

    private final JLabel title = new JLabel("Choose a jigsaw");
    private final JTextField search = new JTextField();
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final JScrollPane sp = new JScrollPane(list);

    private final JButton cancel = new JButton("Cancel");
    private final JButton ok     = new JButton("OK");

    private List<String> allDisplay = List.of(); // display names
    private IntConsumer onPick;

    private final JPanel card = new JPanel(null) {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Theme.Palette.Overlay o = ThemeManager.get().palette().overlay;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(o.cardFill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);

            g2.setColor(o.cardStroke);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 14, 14);

            g2.dispose();
        }
    };

    JigsawChooserPopover() {
        setOpaque(false);
        setLayout(null);

        card.setOpaque(false);
        add(card);

        title.setFont(new Font("Serif", Font.BOLD, 20));
        card.add(title);

        // search
        search.setFont(new Font("SansSerif", Font.PLAIN, 16));
        card.add(search);

        // list
        list.setFont(new Font("Serif", Font.PLAIN, 20));
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setOpaque(true);
        FantasyScrollBarUI.install(sp);
        card.add(sp);

        styleButton(cancel);
        styleButton(ok);
        card.add(cancel);
        card.add(ok);

        // actions
        cancel.addActionListener(e -> close());
        ok.addActionListener(e -> chooseSelected());

        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) chooseSelected();
            }
        });

        search.getDocument().addDocumentListener(new DocumentListener() {
            void refilter() {
                String q = search.getText().toLowerCase().trim();
                model.clear();
                for (String name : allDisplay) {
                    if (q.isEmpty() || name.toLowerCase().contains(q)) model.addElement(name);
                }
                if (!model.isEmpty()) list.setSelectedIndex(0);
            }
            public void insertUpdate(DocumentEvent e) { refilter(); }
            public void removeUpdate(DocumentEvent e) { refilter(); }
            public void changedUpdate(DocumentEvent e) { refilter(); }
        });

        // esc to close
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "esc");
        getActionMap().put("esc", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { close(); }
        });

        // outside click closes
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!card.getBounds().contains(e.getPoint())) close();
            }
        });

        // internal layout when resized
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int m = 16, w = card.getWidth(), h = card.getHeight();
                title.setBounds(m, m, w - 2 * m, 26);
                search.setBounds(m, m + 30, w - 2 * m, 34);
                sp.setBounds(m, m + 30 + 34 + 10, w - 2 * m,
                        h - (m + 30 + 34 + 10) - (m + 44) - 10);

                int btnW = 120, btnH = 40, gap = 12, y = h - m - btnH;
                cancel.setBounds(w - m - (btnW * 2 + gap), y, btnW, btnH);
                ok.setBounds(w - m - btnW, y, btnW, btnH);
            }
        });

        // Apply initial theme (ThemeManager will handle base stuff; we handle overlay specifics)
        refreshTheme();
    }

    private void styleButton(JButton b) {
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 16));
        // colors/borders come from refreshTheme()
    }

    private void chooseSelected() {
        int sel = list.getSelectedIndex();
        if (sel < 0) return;

        String chosenName = model.get(sel);
        int originalIndex = -1;
        for (int i = 0; i < allDisplay.size(); i++) {
            if (allDisplay.get(i).equals(chosenName)) { originalIndex = i; break; }
        }
        if (originalIndex >= 0 && onPick != null) onPick.accept(originalIndex);
    }

    // Open a 420*420 card above anchor; contents come from displayNames.
    void openAbove(JComponent glassPane, JComponent anchor, List<String> displayNames, IntConsumer onPick) {
        this.onPick = onPick;
        this.allDisplay = new ArrayList<>(displayNames);

        model.clear();
        for (String s : allDisplay) model.addElement(s);
        if (!model.isEmpty()) list.setSelectedIndex(0);

        if (getParent() != glassPane) glassPane.add(this);
        setBounds(0, 0, glassPane.getWidth(), glassPane.getHeight());
        setVisible(true);
        glassPane.setVisible(true);

        // register when shown (so theme toggles update this popover while it is open)
        ThemeManager.get().register(this);

        // position the card
        int cw = 420, ch = 420;
        Point aScreen = anchor.getLocationOnScreen();
        Point gScreen = glassPane.getLocationOnScreen();
        int ax = aScreen.x - gScreen.x;
        int ay = aScreen.y - gScreen.y;

        int cx = ax + (anchor.getWidth() - cw) / 2;
        int cy = ay - ch - 12;

        cx = Math.max(12, Math.min(cx, glassPane.getWidth() - cw - 12));
        cy = Math.max(12, Math.min(cy, glassPane.getHeight() - ch - 12));

        card.setBounds(cx, cy, cw, ch);

        // ensure correct colors immediately
        refreshTheme();

        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> search.requestFocusInWindow());
    }

    void close() {
        setVisible(false);
        ThemeManager.get().unregister(this);
        Container p = getParent();
        if (p != null) p.repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        Theme.Palette.Overlay o = ThemeManager.get().palette().overlay;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(o.scrim);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    // ThemeAware
    @Override
    public void refreshTheme() {
        Theme.Palette pal = ThemeManager.get().palette();
        Theme.Palette.Overlay o = pal.overlay;
        Theme.Palette.Base b = pal.base;

        // title + inputs
        title.setForeground(o.text);

        search.setBackground(o.inputBg);
        search.setForeground(o.text);
        search.setCaretColor(o.text);
        search.setBorder(new LineBorder(o.inputBorder));

        // list (use base list colors; popover overlay doesn't define list selection)
        list.setBackground(b.listBg);
        list.setForeground(o.text);
        list.setSelectionBackground(b.listSelectionBg);
        list.setSelectionForeground(o.text);
        sp.getViewport().setBackground(b.listBg);

        // buttons
        for (JButton btn : new JButton[]{cancel, ok}) {
            btn.setBackground(o.buttonBg);
            btn.setForeground(o.text);
            btn.setBorder(new LineBorder(o.buttonBorder));
        }

        card.repaint();
        repaint();
    }
}
