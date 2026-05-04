import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

// Interactive map of Cyprus showing archaeological find-sites.
// Proof-of-concept for Update 24 (World Map Navigation).

// Three sites are plotted:
// Vounous (north coast, Kyrenia), 9 Early Bronze Age artefacts
// Kouklia (south-west coast, Paphos), 4 Late Bronze Age artefacts
// Central Cyprus (unknown provenance), 2 Classical-period artefacts

// Clicking a site navigates to the Progress/Timeline panel filtered
// to the artefacts from that site.

public class CyprusMapPanel extends JPanel implements ThemeAware {

    // Listener
    public interface MapListener {
        void onSiteSelected(String siteName, List<String> artefactPaths);
        void onBackToCollections();
    }

    private MapListener listener;
    public void setMapListener(MapListener l) { this.listener = l; }

    // Site data
    private static final List<Site> SITES = new ArrayList<>();
    static {
        // Positions are normalised fractions of the island bounding box (0 to 1)
        // Derived from GADM 4.1 bbox: lon [32.2693, 33.7154], lat [34.625, 35.1994]
        // x = (lon - 32.2693) / 1.4461,  y = (35.1994 - lat) / 0.5744

        // Vounous: 35.30°N 33.35°E, north coast, Kyrenia district
        SITES.add(new Site("Vounous", "Kyrenia District", 0.466, 0.349,
                new String[]{
                        "/Ancient Cyprus/Artifacts/jug-1.jpg",
                        "/Ancient Cyprus/Artifacts/jug-2.jpg",
                        "/Ancient Cyprus/Artifacts/jug-3.jpg",
                        "/Ancient Cyprus/Artifacts/jug-4.jpg",
                        "/Ancient Cyprus/Artifacts/bowl-1.jpg",
                        "/Ancient Cyprus/Artifacts/bowl-2.jpg",
                        "/Ancient Cyprus/Artifacts/bowl-3.jpg",
                        "/Ancient Cyprus/Artifacts/bowl-4.jpg",
                        "/Ancient Cyprus/Artifacts/bowl-5.jpg"
                }));

        // Kouklia (Palaepaphos): 34.71°N 32.57°E, south-west coast, Paphos district
        SITES.add(new Site("Kouklia", "Paphos District", 0.130, 0.870,
                new String[]{
                        "/Ancient Cyprus/Artifacts/jug-5.jpg",
                        "/Ancient Cyprus/Artifacts/Pyxis-Lid.jpg",
                        "/Ancient Cyprus/Artifacts/Spindle-Whorl.jpg",
                        "/Ancient Cyprus/Artifacts/Human-Remains-1.jpg"
                }));

        // Central Cyprus (provenance unknown), placed near centre of island
        // ~35.0°N 33.2°E
        SITES.add(new Site("Central Cyprus", "Provenance Unknown", 0.401, 0.614,
                new String[]{
                        "/Ancient Cyprus/Artifacts/VotiveStatueHead.jpg",
                        "/Ancient Cyprus/Artifacts/Temple-Boy.jpg"
                }));
    }

    static class Site {
        final String name, region;
        final double rx, ry;          // relative position (0 to 1 within island bounds)
        final String[] artefacts;

        Site(String name, String region, double rx, double ry, String[] artefacts) {
            this.name = name;
            this.region = region;
            this.rx = rx;
            this.ry = ry;
            this.artefacts = artefacts;
        }
    }

    // State
    private BufferedImage backgroundImage;
    private int hoveredSite = -1; // index into SITES
    private boolean hoverBack, hoverViewAll;
    private float[] glowPhase = new float[SITES.size()]; // animation phase per site
    private final Timer glowTimer;

    // Island geometry cache
    private Rectangle islandBounds; // pixel bounds of the island on screen

    public CyprusMapPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());

        // Glow animation timer (30 fps)
        glowTimer = new Timer(33, e -> {
            for (int i = 0; i < glowPhase.length; i++) {
                glowPhase[i] += 0.07f;
                if (glowPhase[i] > (float)(2 * Math.PI)) glowPhase[i] -= (float)(2 * Math.PI);
            }
            repaint();
        });
        glowTimer.start();

        // Mouse interaction (single adapter handles everything)
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int old = hoveredSite;
                hoveredSite = siteAt(e.getPoint());
                boolean oldB = hoverBack, oldV = hoverViewAll;
                hoverBack = backLabelBounds.contains(e.getPoint());
                hoverViewAll = viewAllBounds.contains(e.getPoint());
                boolean hand = hoveredSite >= 0 || hoverBack || hoverViewAll;
                setCursor(hand
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
                if (hoveredSite != old || hoverBack != oldB || hoverViewAll != oldV) repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // Back label
                if (backLabelBounds.contains(e.getPoint())) {
                    if (listener != null) listener.onBackToCollections();
                    return;
                }
                // View All label, show full timeline unfiltered
                if (viewAllBounds.contains(e.getPoint())) {
                    if (listener != null) {
                        // Collect all artefact paths
                        List<String> all = new ArrayList<>();
                        for (Site s : SITES) {
                            all.addAll(List.of(s.artefacts));
                        }
                        listener.onSiteSelected("All Sites", all);
                    }
                    return;
                }
                // Site marker
                int idx = siteAt(e.getPoint());
                if (idx >= 0 && listener != null) {
                    Site s = SITES.get(idx);
                    listener.onSiteSelected(s.name, List.of(s.artefacts));
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        // Escape to go back
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "back");
        getActionMap().put("back", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (listener != null) listener.onBackToCollections();
            }
        });

        ThemeManager.get().register(this);
        refreshTheme();
    }

    // Which site is under the cursor?
    private int siteAt(Point p) {
        if (islandBounds == null) return -1;
        for (int i = 0; i < SITES.size(); i++) {
            Point2D sp = sitePixel(SITES.get(i));
            if (sp.distance(p) < 22) return i;
        }
        return -1;
    }

    private Point2D sitePixel(Site s) {
        if (islandBounds == null) return new Point2D.Double(0, 0);
        return new Point2D.Double(
                islandBounds.x + s.rx * islandBounds.width,
                islandBounds.y + s.ry * islandBounds.height);
    }

    // Theme
    @Override
    public void refreshTheme() {
        String bgPath = BackgroundCatalog.backgroundFor("/Ancient Cyprus/Artifacts/",
                ThemeManager.get().getCurrent());
        try (var in = getClass().getResourceAsStream(bgPath)) {
            if (in != null) backgroundImage = ImageIO.read(in);
        } catch (Exception ex) { ex.printStackTrace(); }
        repaint();
    }

    // Paint
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth(), h = getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Background image (dimmed)
        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, w, h, this);
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRect(0, 0, w, h);
        } else {
            g2.setColor(ThemeManager.get().palette().base.appBg);
            g2.fillRect(0, 0, w, h);
        }

        // 2. Compute island bounds (centred, aspect from OSM bbox: 2.3193 / 1.1336 = 2.046)
        double islandAspect = 2.046;
        int margin = 80;
        int availW = w - margin * 2;
        int availH = h - margin * 2 - 60; // leave room for title + back
        int iw, ih;
        if ((double) availW / availH > islandAspect) {
            ih = availH;
            iw = (int)(ih * islandAspect);
        } else {
            iw = availW;
            ih = (int)(iw / islandAspect);
        }
        int ix = (w - iw) / 2;
        int iy = margin + 40 + (availH - ih) / 2;
        islandBounds = new Rectangle(ix, iy, iw, ih);

        // 3. Draw the island outline
        Shape island = createIslandShape(ix, iy, iw, ih);

        // Sea glow behind island
        g2.setColor(new Color(60, 100, 140, 50));
        g2.setStroke(new BasicStroke(12f));
        g2.draw(island);

        // Island fill, parchment/sandy colour
        boolean dark = ThemeManager.get().getCurrent() == Theme.DARK;
        Color landFill = dark ? new Color(70, 60, 45, 200) : new Color(220, 200, 165, 220);
        Color landStroke = dark ? new Color(140, 120, 80) : new Color(160, 130, 80);
        g2.setColor(landFill);
        g2.fill(island);
        g2.setColor(landStroke);
        g2.setStroke(new BasicStroke(2f));
        g2.draw(island);

        // 4. Draw site markers
        for (int i = 0; i < SITES.size(); i++) {
            drawSiteMarker(g2, SITES.get(i), i);
        }

        // 5. Title
        Theme.Palette.Fonts f = ThemeManager.get().palette().fonts;
        Theme.Palette.Base b = ThemeManager.get().palette().base;

        g2.setFont(f.heading);
        String title = "Ancient Cyprus — Archaeological Sites";
        FontMetrics fm = g2.getFontMetrics();
        TextRender.drawStringWithHalo(g2, title, (w - fm.stringWidth(title)) / 2, margin + 20, b.text);

        // 6. Instruction text (below title)
        g2.setFont(f.caption);
        String hint = "Click a site to explore its artefacts";
        fm = g2.getFontMetrics();
        TextRender.drawStringWithHalo(g2, hint, (w - fm.stringWidth(hint)) / 2, margin + 42, b.mutedText);

        // 7. "View All" + "Back" labels (bottom). Hover grows each label's font;
        // the bounds rectangles stay fixed on base metrics so the hit target
        // doesn't shift as the cursor crosses the threshold.
        Font baseBtn = f.button;
        Font hoverBtn = baseBtn.deriveFont(baseBtn.getSize2D() + 4f);
        g2.setFont(baseBtn);
        fm = g2.getFontMetrics();

        String back = "← Back";
        String viewAll = "View All Artefacts";
        int gap = 60;
        int backW = fm.stringWidth(back);
        int vaW = fm.stringWidth(viewAll);
        int totalW2 = backW + gap + vaW;
        int startX = (w - totalW2) / 2;
        int by = h - 30;
        int asc = fm.getAscent(), ht = fm.getHeight();
        int vaX = startX + backW + gap;

        backLabelBounds = new Rectangle(startX - 8, by - asc - 4, backW + 16, ht + 8);
        viewAllBounds = new Rectangle(vaX - 8, by - asc - 4, vaW + 16, ht + 8);

        // "Back" label (left, consistent with the rest of the application)
        g2.setFont(hoverBack ? hoverBtn : baseBtn);
        TextRender.drawStringWithPill(g2, back, startX, by, b.text);

        // "View All" label (right)
        g2.setFont(hoverViewAll ? hoverBtn : baseBtn);
        TextRender.drawStringWithPill(g2, viewAll, vaX, by, b.text);

        // 8. Legend
        drawLegend(g2, ix, iy + ih + 16, iw);

        g2.dispose();
    }

    private Rectangle backLabelBounds = new Rectangle();
    private Rectangle viewAllBounds = new Rectangle();

    // Target bounds for the tutorial's map step, the "View All Artefacts" label,
    // which is always present and easy to point at. Returns a copy in this panel's
    // local coordinate space, or null if not yet laid out.
    public Rectangle getTutorialTargetBounds() {
        if (viewAllBounds == null || viewAllBounds.width == 0) return null;
        return new Rectangle(viewAllBounds);
    }

    // Draw a single site marker
    private void drawSiteMarker(Graphics2D g2, Site site, int index) {
        Point2D p = sitePixel(site);
        double x = p.getX(), y = p.getY();
        boolean hovered = (index == hoveredSite);

        // Glow pulse
        float phase = glowPhase[index];
        float pulse = 0.5f + 0.5f * (float) Math.sin(phase);

        // Outer glow ring
        int glowR = hovered ? 28 : 18;
        int alpha = (int)(60 + 40 * pulse);
        Color glowCol = hovered
                ? new Color(255, 200, 80, alpha)
                : new Color(200, 170, 100, alpha / 2);
        g2.setColor(glowCol);
        g2.fill(new Ellipse2D.Double(x - glowR, y - glowR, glowR * 2, glowR * 2));

        // Inner marker
        int r = hovered ? 10 : 7;
        Color markerFill = new Color(200, 160, 60);
        Color markerBorder = new Color(120, 90, 30);
        g2.setColor(markerFill);
        g2.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        g2.setColor(markerBorder);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));

        // Centre dot
        g2.setColor(new Color(255, 240, 180));
        g2.fill(new Ellipse2D.Double(x - 2, y - 2, 4, 4));

        // Label
        Theme.Palette.Base b = ThemeManager.get().palette().base;
        g2.setFont(ThemeManager.get().palette().fonts.bodyBold);
        FontMetrics fm = g2.getFontMetrics();
        String label = site.name;
        int lx = (int) x - fm.stringWidth(label) / 2;
        int ly = (int) y - (hovered ? 32 : 22);

        // Label background pill
        int pw = fm.stringWidth(label) + 16;
        int ph = fm.getHeight() + 6;
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(lx - 8, ly - fm.getAscent() - 3, pw, ph, 10, 10);

        g2.setColor(new Color(240, 220, 160));
        g2.drawString(label, lx, ly);

        // Sub-label on hover
        if (hovered) {
            g2.setFont(ThemeManager.get().palette().fonts.caption);
            fm = g2.getFontMetrics();
            String sub = site.region + " — " + site.artefacts.length + " artefacts";
            int sx = (int) x - fm.stringWidth(sub) / 2;
            int sy = ly + fm.getHeight() + 2;

            int spw = fm.stringWidth(sub) + 12;
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRoundRect(sx - 6, sy - fm.getAscent() - 2, spw, fm.getHeight() + 4, 8, 8);

            // Fixed cream colour, the pill is always dark, so theme-muted text
            // (which is dark in light mode) would be invisible against it.
            g2.setColor(new Color(220, 200, 160));
            g2.drawString(sub, sx, sy);
        }
    }

    // Legend at bottom
    private void drawLegend(Graphics2D g2, int x, int y, int w) {
        Theme.Palette.Base b = ThemeManager.get().palette().base;
        g2.setFont(ThemeManager.get().palette().fonts.caption);
        FontMetrics fm = g2.getFontMetrics();

        String[] labels = {"Vounous (9)", "Kouklia (4)", "Central (2)"};
        int totalW = 0;
        for (String l : labels) totalW += fm.stringWidth(l) + 30;
        int lx = x + (w - totalW) / 2;

        for (String label : labels) {
            // Marker dot
            g2.setColor(new Color(200, 160, 60));
            g2.fillOval(lx, y + 2, 8, 8);
            g2.setColor(new Color(120, 90, 30));
            g2.drawOval(lx, y + 2, 8, 8);

            TextRender.drawStringWithHalo(g2, label, lx + 14, y + 10, b.mutedText);
            lx += fm.stringWidth(label) + 30;
        }
    }

    // Full island of Cyprus from OpenStreetMap physical coastline
    // Includes Northern Cyprus (TRNC) and UK Sovereign Base Areas
    // Bounding box: lon [32.2692, 34.5885], lat [34.5624, 35.6960]
    // Normalised: x = (lon - 32.2692) / 2.3193, y = (35.6960 - lat) / 1.1336
    // 112 points, Douglas-Peucker simplified (eps 0.007) from 25,823
    private static final double[][] CYPRUS_OUTLINE = {
            {0.0000, 0.5519}, {0.0027, 0.5812}, {0.0192, 0.6175},
            {0.0190, 0.6468}, {0.0139, 0.6527}, {0.0242, 0.6792},
            {0.0202, 0.7027}, {0.0338, 0.7402}, {0.0513, 0.7547},
            {0.0535, 0.7991}, {0.0592, 0.8202}, {0.0565, 0.8304},
            {0.0633, 0.8297}, {0.0971, 0.8761}, {0.1232, 0.8804},
            {0.1480, 0.9070}, {0.1872, 0.9313}, {0.2250, 0.9101},
            {0.2611, 0.9078}, {0.2821, 0.9504}, {0.2883, 0.9762},
            {0.2863, 0.9969}, {0.3037, 0.9904}, {0.3274, 0.9999},
            {0.3308, 0.9954}, {0.3188, 0.9724}, {0.3176, 0.9501},
            {0.3197, 0.9304}, {0.3293, 0.9224}, {0.3181, 0.9246},
            {0.3256, 0.9219}, {0.3260, 0.9121}, {0.3426, 0.8912},
            {0.3793, 0.8674}, {0.4269, 0.8740}, {0.4407, 0.8569},
            {0.4526, 0.8653}, {0.5001, 0.8221}, {0.5287, 0.8106},
            {0.5528, 0.7735}, {0.5750, 0.7764}, {0.5908, 0.7322},
            {0.5936, 0.6629}, {0.6112, 0.6357}, {0.6497, 0.6313},
            {0.6852, 0.6646}, {0.6997, 0.6540}, {0.7028, 0.6392},
            {0.7337, 0.6243}, {0.7530, 0.6286}, {0.7832, 0.6524},
            {0.7741, 0.6030}, {0.7411, 0.5467}, {0.7282, 0.5069},
            {0.7083, 0.4700}, {0.7030, 0.4341}, {0.7060, 0.3870},
            {0.7320, 0.3301}, {0.7748, 0.3239}, {0.7811, 0.2892},
            {0.8041, 0.2470}, {0.8757, 0.1823}, {0.9005, 0.1420},
            {0.9228, 0.1224}, {0.9348, 0.0830}, {0.9676, 0.0550},
            {0.9889, 0.0482}, {1.0000, 0.0007}, {0.9813, 0.0072},
            {0.9725, 0.0222}, {0.9639, 0.0177}, {0.9578, 0.0329},
            {0.9360, 0.0356}, {0.9224, 0.0536}, {0.8957, 0.0643},
            {0.8923, 0.0817}, {0.8713, 0.1058}, {0.8539, 0.1101},
            {0.8451, 0.1227}, {0.8226, 0.1179}, {0.8007, 0.1682},
            {0.7807, 0.1768}, {0.7365, 0.2206}, {0.6815, 0.2498},
            {0.6479, 0.2487}, {0.5938, 0.2991}, {0.5773, 0.2956},
            {0.5225, 0.3209}, {0.4652, 0.3185}, {0.4409, 0.3047},
            {0.4194, 0.3099}, {0.3998, 0.2973}, {0.3887, 0.3041},
            {0.3642, 0.2889}, {0.3570, 0.3018}, {0.3426, 0.3006},
            {0.3124, 0.2892}, {0.2809, 0.2575}, {0.2883, 0.3660},
            {0.2786, 0.4333}, {0.2640, 0.4709}, {0.2394, 0.4897},
            {0.2024, 0.4552}, {0.1529, 0.4437}, {0.1302, 0.4643},
            {0.1217, 0.4589}, {0.0942, 0.5403}, {0.0687, 0.5744},
            {0.0385, 0.5717}, {0.0077, 0.5210}, {0.0029, 0.5257},
            {0.0000, 0.5519}
    };

    private Shape createIslandShape(int ox, int oy, int w, int h) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(ox + CYPRUS_OUTLINE[0][0] * w, oy + CYPRUS_OUTLINE[0][1] * h);
        for (int i = 1; i < CYPRUS_OUTLINE.length; i++) {
            // Quadratic curves between midpoints for smooth coastline
            if (i + 1 < CYPRUS_OUTLINE.length) {
                double cx = ox + CYPRUS_OUTLINE[i][0] * w;
                double cy = oy + CYPRUS_OUTLINE[i][1] * h;
                double ex = ox + (CYPRUS_OUTLINE[i][0] + CYPRUS_OUTLINE[i + 1][0]) / 2.0 * w;
                double ey = oy + (CYPRUS_OUTLINE[i][1] + CYPRUS_OUTLINE[i + 1][1]) / 2.0 * h;
                path.quadTo(cx, cy, ex, ey);
            } else {
                path.lineTo(ox + CYPRUS_OUTLINE[i][0] * w, oy + CYPRUS_OUTLINE[i][1] * h);
            }
        }
        path.closePath();
        return path;
    }
}
