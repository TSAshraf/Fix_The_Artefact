import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

// Interactive map of the Ancient Near East showing archaeological find-sites.
// Covers Anatolia, Mesopotamia, the Levant, Iran, and the Arabian coastline.

// Uses inverted rendering (like Egypt): fill with land, then cut out sea areas.
// Sea bodies: Mediterranean, Black Sea, Caspian Sea, Persian Gulf, Red Sea.

// Coastline data: Natural Earth 10m land polygons (public domain).
// Douglas-Peucker simplified for rendering performance.

public class NearEastMapPanel extends JPanel implements ThemeAware {

    public interface MapListener {
        void onSiteSelected(String siteName, List<String> artefactPaths);
        void onBackToCollections();
    }

    private MapListener listener;
    public void setMapListener(MapListener l) { this.listener = l; }

    private static final List<Site> SITES = new ArrayList<>();
    static {
        // Hattusha, Hittite capital, central Anatolia
        SITES.add(new Site("Hattusha", "Hittite Capital", 0.3040, 0.1667,
                new String[]{
                        "/Ancient Near East/Artifacts/Seated Goddess with a Child .jpeg",
                        "/Ancient Near East/Artifacts/Stag Vessel.jpeg"
                }));
        // Nimrud, Assyrian heartland, northern Mesopotamia
        SITES.add(new Site("Nimrud", "Assyrian Heartland", 0.6520, 0.3833,
                new String[]{
                        "/Ancient Near East/Artifacts/Openwork furniture plaque.jpeg",
                        "/Ancient Near East/Artifacts/Head of a Ruler.jpeg",
                        "/Ancient Near East/Artifacts/Helmet with Divine figures.jpeg"
                }));
        // Babylon, Neo-Babylonian, central Mesopotamia
        SITES.add(new Site("Babylon", "City of Wonders", 0.6960, 0.5833,
                new String[]{
                        "/Ancient Near East/Artifacts/Panel with Lion.jpeg",
                        "/Ancient Near East/Artifacts/Standing Male Worshiper.jpeg",
                        "/Ancient Near East/Artifacts/Enthroned Deity.jpeg"
                }));
        // Ur, Sumerian, southern Mesopotamia
        SITES.add(new Site("Ur", "Cradle of Civilisation", 0.7640, 0.6722,
                new String[]{
                        "/Ancient Near East/Artifacts/Headdress.jpeg",
                        "/Ancient Near East/Artifacts/Statue of Gudea.jpeg",
                        "/Ancient Near East/Artifacts/South Arabian Statue.jpg"
                }));
        // Susa, Elamite / Achaemenid, southwestern Iran
        SITES.add(new Site("Susa", "Elamite Capital", 0.8520, 0.6000,
                new String[]{
                        "/Ancient Near East/Artifacts/Kneeling Bull.jpeg",
                        "/Ancient Near East/Artifacts/Master of Animals Standard.jpg",
                        "/Ancient Near East/Artifacts/Plaque with horned lion-griffins.jpeg",
                        "/Ancient Near East/Artifacts/Shaft-hole axe head.jpeg"
                }));
    }

    static class Site {
        final String name, region;
        final double rx, ry;
        final String[] artefacts;
        Site(String n, String r, double rx, double ry, String[] a) {
            name=n; region=r; this.rx=rx; this.ry=ry; artefacts=a;
        }
    }

    private BufferedImage backgroundImage;
    private int hoveredSite = -1;
    private boolean hoverBack, hoverView;
    private float[] glowPhase = new float[SITES.size()];
    private final Timer glowTimer;
    private Rectangle mapBounds;

    public NearEastMapPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        glowTimer = new Timer(33, e -> {
            for (int i = 0; i < glowPhase.length; i++) {
                glowPhase[i] += 0.07f;
                if (glowPhase[i] > (float)(2*Math.PI)) glowPhase[i] -= (float)(2*Math.PI);
            }
            repaint();
        });
        glowTimer.start();

        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int old = hoveredSite;
                hoveredSite = siteAt(e.getPoint());
                boolean oldB = hoverBack, oldV = hoverView;
                hoverBack = backBounds.contains(e.getPoint());
                hoverView = viewBounds.contains(e.getPoint());
                boolean over = hoverBack || hoverView;
                setCursor((hoveredSite>=0||over) ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                if (hoveredSite!=old || hoverBack!=oldB || hoverView!=oldV) repaint();
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (backBounds.contains(e.getPoint())) { if(listener!=null) listener.onBackToCollections(); return; }
                if (viewBounds.contains(e.getPoint())) {
                    if(listener!=null) { List<String> a=new ArrayList<>(); for(Site s:SITES) a.addAll(List.of(s.artefacts)); listener.onSiteSelected("All Sites",a); } return;
                }
                int i=siteAt(e.getPoint());
                if(i>=0&&listener!=null) { Site s=SITES.get(i); listener.onSiteSelected(s.name,List.of(s.artefacts)); }
            }
        };
        addMouseListener(mouse); addMouseMotionListener(mouse);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"back");
        getActionMap().put("back", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { if(listener!=null) listener.onBackToCollections(); }});
        ThemeManager.get().register(this); refreshTheme();
    }

    private int siteAt(Point p) {
        if(mapBounds==null) return -1;
        for(int i=0;i<SITES.size();i++) if(sitePixel(SITES.get(i)).distance(p)<22) return i;
        return -1;
    }
    private Point2D sitePixel(Site s) {
        if(mapBounds==null) return new Point2D.Double(0,0);
        return new Point2D.Double(mapBounds.x+s.rx*mapBounds.width, mapBounds.y+s.ry*mapBounds.height);
    }

    @Override public void refreshTheme() {
        String bg = BackgroundCatalog.backgroundFor("/Ancient Near East/Artifacts/", ThemeManager.get().getCurrent());
        try(var in=getClass().getResourceAsStream(bg)) { if(in!=null) backgroundImage=ImageIO.read(in); } catch(Exception ex) { ex.printStackTrace(); }
        repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w=getWidth(), h=getHeight();
        Graphics2D g2=(Graphics2D)g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if(backgroundImage!=null) { g2.drawImage(backgroundImage,0,0,w,h,this); g2.setColor(new Color(0,0,0,140)); g2.fillRect(0,0,w,h); }
        else { g2.setColor(ThemeManager.get().palette().base.appBg); g2.fillRect(0,0,w,h); }

        double aspect=1.3889; int margin=60;
        int aW=w-margin*2, aH=h-margin*2-50; int iw,ih;
        if((double)aW/aH>aspect){ih=aH;iw=(int)(ih*aspect);}else{iw=aW;ih=(int)(iw/aspect);}
        int ix=(w-iw)/2, iy=margin+36+(aH-ih)/2;
        mapBounds=new Rectangle(ix,iy,iw,ih);

        boolean dark=ThemeManager.get().getCurrent()==Theme.DARK;
        Color landFill=dark?new Color(65,55,40,200):new Color(215,195,160,220);
        Color landStroke=dark?new Color(130,110,70):new Color(155,125,75);

        // Clip to map area
        Shape oldClip = g2.getClip();
        g2.clipRect(ix, iy, iw, ih);

        // Near East is mostly land, fill the whole map with land colour first,
        // then cut out the seas (Mediterranean, Black Sea, Caspian, Persian Gulf, Red Sea).
        g2.setColor(landFill);
        g2.fillRect(ix, iy, iw, ih);

        // Build sea shapes
        Shape[] seas = new Shape[ALL_SEAS.length];
        for(int i=0;i<ALL_SEAS.length;i++) seas[i]=createShape(ALL_SEAS[i],ix,iy,iw,ih);

        // Build island shapes (land inside sea, need to NOT clear these)
        Shape[] islands = new Shape[ALL_ISLANDS.length];
        for(int i=0;i<ALL_ISLANDS.length;i++) islands[i]=createShape(ALL_ISLANDS[i],ix,iy,iw,ih);

        // Clear sea areas back to the background image
        Composite origComp = g2.getComposite();
        g2.setComposite(AlphaComposite.Clear);
        for(Shape s:seas) g2.fill(s);
        g2.setComposite(origComp);

        // Re-draw land fill for islands (they got cleared with the sea)
        g2.setColor(landFill);
        for(Shape s:islands) g2.fill(s);

        // Re-draw background into the cleared sea areas
        if(backgroundImage!=null) {
            Shape seaClip = g2.getClip();
            for(Shape s:seas) {
                g2.setClip(s);
                // Exclude islands from the sea clip
                Area seaArea = new Area(s);
                for(Shape isl:islands) seaArea.subtract(new Area(isl));
                g2.setClip(seaArea);
                g2.drawImage(backgroundImage,0,0,w,h,this);
                g2.setColor(new Color(0,0,0,140));
                g2.fillRect(0,0,w,h);
            }
            g2.setClip(seaClip);
        } else {
            g2.setColor(ThemeManager.get().palette().base.appBg);
            for(Shape s:seas) g2.fill(s);
            // Redraw islands as land
            g2.setColor(landFill);
            for(Shape s:islands) g2.fill(s);
        }

        // Draw coastline strokes
        g2.setColor(new Color(40,80,130,40)); g2.setStroke(new BasicStroke(6f));
        for(Shape s:seas) g2.draw(s);
        for(Shape s:islands) g2.draw(s);
        g2.setColor(landStroke); g2.setStroke(new BasicStroke(1.0f));
        for(Shape s:seas) g2.draw(s);
        for(Shape s:islands) g2.draw(s);

        // Draw the Tigris and Euphrates rivers
        Color riverColor = dark ? new Color(70, 120, 170, 200) : new Color(50, 100, 150, 180);
        g2.setColor(riverColor);
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(createOpenPath(EUPHRATES, ix, iy, iw, ih));
        g2.draw(createOpenPath(TIGRIS, ix, iy, iw, ih));

        g2.setClip(oldClip);

        // Sea and region labels
        Theme.Palette.Fonts f=ThemeManager.get().palette().fonts;
        Theme.Palette.Base b=ThemeManager.get().palette().base;
        g2.setFont(f.caption); g2.setColor(new Color(100,140,180,dark?100:75));
        drawCentred(g2,"Mediterranean",ix+(int)(0.12*iw),iy+(int)(0.55*ih));
        drawCentred(g2,"Sea",ix+(int)(0.12*iw),iy+(int)(0.58*ih));
        drawCentred(g2,"Black Sea",ix+(int)(0.30*iw),iy+(int)(0.05*ih));
        drawCentred(g2,"Caspian",ix+(int)(0.92*iw),iy+(int)(0.15*ih));
        drawCentred(g2,"Sea",ix+(int)(0.92*iw),iy+(int)(0.18*ih));
        drawCentred(g2,"Persian Gulf",ix+(int)(0.90*iw),iy+(int)(0.78*ih));

        // Region labels
        g2.setColor(new Color(180,150,100,dark?70:55));
        drawCentred(g2,"Anatolia",ix+(int)(0.30*iw),iy+(int)(0.22*ih));
        drawCentred(g2,"Mesopotamia",ix+(int)(0.63*iw),iy+(int)(0.48*ih));

        // River labels
        g2.setFont(f.caption);
        g2.setColor(new Color(70,120,170,dark?120:90));
        AffineTransform origTransform = g2.getTransform();
        // Euphrates label (rotated to follow river)
        int elx = ix+(int)(0.52*iw), ely = iy+(int)(0.43*ih);
        g2.translate(elx, ely);
        g2.rotate(Math.toRadians(50));
        g2.drawString("Euphrates", 0, 0);
        g2.setTransform(origTransform);
        // Tigris label
        int tlx = ix+(int)(0.68*iw), tly = iy+(int)(0.38*ih);
        g2.translate(tlx, tly);
        g2.rotate(Math.toRadians(58));
        g2.drawString("Tigris", 0, 0);
        g2.setTransform(origTransform);

        for(int i=0;i<SITES.size();i++) drawMarker(g2,SITES.get(i),i);

        g2.setFont(f.heading);
        String t="The Ancient Near East \u2014 Archaeological Sites"; FontMetrics fm=g2.getFontMetrics();
        TextRender.drawStringWithHalo(g2,t,(w-fm.stringWidth(t))/2,margin+16,b.text);
        g2.setFont(f.caption); fm=g2.getFontMetrics();
        String hint="Click a site to explore its artefacts";
        TextRender.drawStringWithHalo(g2,hint,(w-fm.stringWidth(hint))/2,margin+35,b.mutedText);

        Font baseBtn = f.button, hoverBtn = baseBtn.deriveFont(baseBtn.getSize2D()+4f);
        g2.setFont(baseBtn); fm=g2.getFontMetrics();
        String bk="\u2190 Back", va="View All Artefacts";
        int gap=60,tw2=fm.stringWidth(bk)+gap+fm.stringWidth(va),sx=(w-tw2)/2,by=h-26;
        int bkW=fm.stringWidth(bk),vaW=fm.stringWidth(va),asc=fm.getAscent(),ht=fm.getHeight();
        int vx=sx+bkW+gap;
        backBounds=new Rectangle(sx-8,by-asc-4,bkW+16,ht+8);
        viewBounds=new Rectangle(vx-8,by-asc-4,vaW+16,ht+8);
        g2.setFont(hoverBack?hoverBtn:baseBtn);
        TextRender.drawStringWithPill(g2,bk,sx,by,b.text);
        g2.setFont(hoverView?hoverBtn:baseBtn);
        TextRender.drawStringWithPill(g2,va,vx,by,b.text);

        drawLegend(g2,ix,iy+ih+10,iw);
        g2.dispose();
    }

    private Rectangle backBounds=new Rectangle(), viewBounds=new Rectangle();

    // Tutorial pointer target, the "View All Artefacts" label.
    public Rectangle getTutorialTargetBounds() {
        if (viewBounds == null || viewBounds.width == 0) return null;
        return new Rectangle(viewBounds);
    }
    private void drawCentred(Graphics2D g,String t,int x,int y){FontMetrics m=g.getFontMetrics();g.drawString(t,x-m.stringWidth(t)/2,y);}

    private void drawMarker(Graphics2D g2, Site site, int index) {
        Point2D p=sitePixel(site); double x=p.getX(),y=p.getY();
        boolean hov=(index==hoveredSite);
        float pulse=0.5f+0.5f*(float)Math.sin(glowPhase[index]);
        int gr=hov?28:18; int al=(int)(60+40*pulse);
        g2.setColor(hov?new Color(255,200,80,al):new Color(200,170,100,al/2));
        g2.fill(new Ellipse2D.Double(x-gr,y-gr,gr*2,gr*2));
        int r=hov?10:7;
        g2.setColor(new Color(200,160,60)); g2.fill(new Ellipse2D.Double(x-r,y-r,r*2,r*2));
        g2.setColor(new Color(120,90,30)); g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new Ellipse2D.Double(x-r,y-r,r*2,r*2));
        g2.setColor(new Color(255,240,180)); g2.fill(new Ellipse2D.Double(x-2,y-2,4,4));

        g2.setFont(ThemeManager.get().palette().fonts.bodyBold);
        FontMetrics fm=g2.getFontMetrics(); String label=site.name;
        int lx=(int)x-fm.stringWidth(label)/2, ly=(int)y-(hov?32:22);
        g2.setColor(new Color(0,0,0,160));
        g2.fillRoundRect(lx-8,ly-fm.getAscent()-3,fm.stringWidth(label)+16,fm.getHeight()+6,10,10);
        g2.setColor(new Color(240,220,160)); g2.drawString(label,lx,ly);

        if(hov){
            g2.setFont(ThemeManager.get().palette().fonts.caption); fm=g2.getFontMetrics();
            String sub=site.region+" \u2014 "+site.artefacts.length+" artefacts";
            int sx2=(int)x-fm.stringWidth(sub)/2, sy=ly+fm.getHeight()+2;
            g2.setColor(new Color(0,0,0,140));
            g2.fillRoundRect(sx2-6,sy-fm.getAscent()-2,fm.stringWidth(sub)+12,fm.getHeight()+4,8,8);
            g2.setColor(new Color(220, 200, 160)); g2.drawString(sub,sx2,sy);
        }
    }

    private void drawLegend(Graphics2D g2,int x,int y,int w){
        g2.setFont(ThemeManager.get().palette().fonts.caption); FontMetrics fm=g2.getFontMetrics();
        String[] ls={"Hattusha (2)","Nimrud (3)","Babylon (3)","Ur (3)","Susa (4)"};
        int tw=0; for(String l:ls) tw+=fm.stringWidth(l)+30;
        int lx=x+(w-tw)/2;
        for(String l:ls){
            g2.setColor(new Color(200,160,60)); g2.fillOval(lx,y+2,8,8);
            g2.setColor(new Color(120,90,30)); g2.drawOval(lx,y+2,8,8);
            TextRender.drawStringWithHalo(g2,l,lx+14,y+10,
                    ThemeManager.get().palette().base.mutedText);
            lx+=fm.stringWidth(l)+30;
        }
    }

    // Shape creation
    private Shape createShape(double[][] outline, int ox, int oy, int w, int h) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(ox + outline[0][0] * w, oy + outline[0][1] * h);
        for (int i = 1; i < outline.length; i++) {
            if (i + 1 < outline.length) {
                double cx = ox + outline[i][0] * w;
                double cy = oy + outline[i][1] * h;
                double ex = ox + (outline[i][0] + outline[i + 1][0]) / 2.0 * w;
                double ey = oy + (outline[i][1] + outline[i + 1][1]) / 2.0 * h;
                path.quadTo(cx, cy, ex, ey);
            } else {
                path.lineTo(ox + outline[i][0] * w, oy + outline[i][1] * h);
            }
        }
        return path;
    }

    // Create an open path for river lines (no fill, just stroke).
    private Shape createOpenPath(double[][] pts, int ox, int oy, int w, int h) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(ox + pts[0][0] * w, oy + pts[0][1] * h);
        for (int i = 1; i < pts.length; i++) {
            if (i + 1 < pts.length) {
                double cx = ox + pts[i][0] * w; double cy = oy + pts[i][1] * h;
                double ex = ox + (pts[i][0] + pts[i + 1][0]) / 2.0 * w;
                double ey = oy + (pts[i][1] + pts[i + 1][1]) / 2.0 * h;
                path.quadTo(cx, cy, ex, ey);
            } else { path.lineTo(ox + pts[i][0] * w, oy + pts[i][1] * h); }
        }
        return path;
    }

    // Natural Earth 10m land polygons (public domain, no political borders)
    // BBOX: lon [27, 52], lat [25, 43], aspect 1.3889
    // Inverted rendering: sea polygons are cut out from land fill.

    // MEDITERRANEAN SEA
    // Coastline traces eastern Med from Egypt/Sinai to Levant to S. Turkey to Aegean
    // Closed via top-left and bottom-left off-screen corners.
    private static final double[][] MEDITERRANEAN_SEA = {
            {-0.0107, 0.6400}, {0.0131, 0.6456}, {0.0191, 0.6548},
            {0.0341, 0.6533}, {0.0371, 0.6612}, {0.0570, 0.6620},
            {0.0811, 0.6763}, {0.1001, 0.6691}, {0.1228, 0.6481},
            {0.1315, 0.6534}, {0.1272, 0.6512}, {0.1345, 0.6384},
            {0.1590, 0.6341}, {0.1417, 0.6443}, {0.1494, 0.6451},
            {0.1648, 0.6393}, {0.1591, 0.6342}, {0.1639, 0.6329},
            {0.1823, 0.6420}, {0.1978, 0.6377}, {0.2080, 0.6503},
            {0.1956, 0.6371}, {0.1907, 0.6509}, {0.2015, 0.6546},
            {0.2043, 0.6637}, {0.2113, 0.6591}, {0.2083, 0.6503},
            {0.2241, 0.6633}, {0.2371, 0.6580}, {0.2280, 0.6645},
            {0.2404, 0.6635}, {0.2444, 0.6558}, {0.2453, 0.6641},
            {0.2819, 0.6539}, {0.3044, 0.6241}, {0.3182, 0.5648},
            {0.3227, 0.5630}, {0.3388, 0.5054}, {0.3456, 0.5000},
            {0.3466, 0.4827}, {0.3593, 0.4701}, {0.3547, 0.4487},
            {0.3568, 0.4212}, {0.3489, 0.4124}, {0.3590, 0.3879},
            {0.3515, 0.3711}, {0.3686, 0.3523}, {0.3608, 0.3374},
            {0.3443, 0.3478}, {0.3484, 0.3489}, {0.3449, 0.3554},
            {0.3338, 0.3588}, {0.3077, 0.3437}, {0.2910, 0.3563},
            {0.2784, 0.3770}, {0.2748, 0.3714}, {0.2675, 0.3813},
            {0.2321, 0.3874}, {0.2146, 0.3790}, {0.2008, 0.3585},
            {0.1736, 0.3440}, {0.1475, 0.3394}, {0.1371, 0.3763},
            {0.1263, 0.3723}, {0.1069, 0.3820}, {0.0839, 0.3672},
            {0.0846, 0.3582}, {0.0806, 0.3584}, {0.0839, 0.3519},
            {0.0776, 0.3472}, {0.0732, 0.3554}, {0.0581, 0.3398},
            {0.0558, 0.3450}, {0.0503, 0.3419}, {0.0518, 0.3489},
            {0.0414, 0.3575}, {0.0384, 0.3554}, {0.0436, 0.3531},
            {0.0386, 0.3506}, {0.0447, 0.3444}, {0.0148, 0.3512},
            {0.0258, 0.3441}, {0.0411, 0.3455}, {0.0417, 0.3367},
            {0.0532, 0.3313}, {0.0105, 0.3354}, {0.0101, 0.3264},
            {0.0226, 0.3264}, {0.0246, 0.3181}, {0.0185, 0.3193},
            {0.0163, 0.3104}, {0.0076, 0.3135}, {0.0083, 0.3002},
            {0.0005, 0.2963}, {0.0100, 0.2920}, {0.0107, 0.2805},
            {-0.0051, 0.2758}, {-0.0117, 0.2663}, {-0.0119, 0.2539},
            {0.0066, 0.2526}, {-0.0026, 0.2536}, {-0.0112, 0.2415},
            {0.0025, 0.2285}, {-0.0077, 0.2250}, {-0.0047, 0.2182},
            {-0.0119, 0.2073}, {-0.0116, 0.2035}, {-0.0028, 0.1951},
            {-0.0077, 0.1905}, {-0.0200, -0.0278}, {-0.0200, 1.0278}
    };

    // BLACK SEA
    // Southern coast from western Turkey to Georgia. Closed via top corners.
    private static final double[][] BLACK_SEA = {
            {-0.0200, -0.0278}, {0.0412, -0.0138}, {0.0362, -0.0112},
            {0.0357, 0.0161}, {0.0293, 0.0159}, {0.0186, 0.0318},
            {0.0312, 0.0367}, {0.0403, 0.0534}, {0.0389, 0.0654},
            {0.0445, 0.0771}, {0.0838, 0.0974}, {0.0821, 0.1066},
            {0.0729, 0.1132}, {0.0611, 0.1064}, {0.0617, 0.1117},
            {0.0466, 0.1064}, {0.0210, 0.1117}, {0.0071, 0.1315},
            {-0.0117, 0.1408}, {-0.0113, 0.1360}, {-0.0068, 0.1332},
            {-0.0114, 0.1305}, {-0.0114, 0.1450}, {0.0115, 0.1407},
            {0.0181, 0.1488}, {0.0305, 0.1491}, {0.0353, 0.1452},
            {0.0274, 0.1391}, {0.0304, 0.1368}, {0.0414, 0.1403},
            {0.0378, 0.1463}, {0.0821, 0.1463}, {0.0861, 0.1426},
            {0.0717, 0.1359}, {0.1176, 0.1258}, {0.0902, 0.1216},
            {0.0803, 0.1094}, {0.0868, 0.0982}, {0.1692, 0.1060},
            {0.1761, 0.0935}, {0.2245, 0.0645}, {0.2530, 0.0545},
            {0.3099, 0.0583}, {0.3179, 0.0501}, {0.3287, 0.0542},
            {0.3239, 0.0604}, {0.3394, 0.0753}, {0.3618, 0.0721},
            {0.3654, 0.0854}, {0.3780, 0.0978}, {0.3903, 0.0908},
            {0.4054, 0.1030}, {0.4210, 0.1090}, {0.4314, 0.1043},
            {0.4358, 0.1121}, {0.4532, 0.1157}, {0.4972, 0.1049},
            {0.5257, 0.1154}, {0.5756, 0.0900}, {0.5910, 0.0655},
            {0.5783, 0.0159}, {0.5660, 0.0114}, {0.5542, -0.0035},
            {0.5334, -0.0078}, {0.5303, -0.0154}, {1.0200, -0.0278}
    };

    // CASPIAN SEA (Natural Earth 10m, cleaned)
    // SW shore clipped to map BBOX; closed via top-right edge.
    private static final double[][] CASPIAN_SEA = {
            {1.05, -0.05}, {1.05, 0.356}, {0.996, 0.357},
            {0.96, 0.346}, {0.933, 0.325}, {0.928, 0.312},
            {0.917, 0.307}, {0.895, 0.305}, {0.881, 0.292},
            {0.875, 0.264}, {0.873, 0.231}, {0.878, 0.215},
            {0.885, 0.215}, {0.89, 0.205}, {0.896, 0.2},
            {0.896, 0.193}, {0.897, 0.175}, {0.9, 0.163},
            {0.916, 0.146}, {0.93, 0.148}, {0.934, 0.146},
            {0.922, 0.134}, {0.902, 0.13}, {0.902, 0.123},
            {0.888, 0.108}, {0.882, 0.088}, {0.866, 0.067},
            {0.856, 0.059}, {0.844, 0.036}, {0.829, 0.019},
            {0.819, 0.0}, {0.819, -0.05}, {1.05, -0.05}
    };

    // PERSIAN GULF (~3000 BC, hybrid)
    // Real NE 10m coastline (cleaned) for Iranian + Arabian coasts.
    // Ancient NW extension: gulf head at 31.5N; Ur was coastal; Kuwait submerged.
    private static final double[][] PERSIAN_GULF = {
            // Arabian coast (south, cleaned NE 10m)
            {0.984, 1.05}, {0.9808, 0.9835}, {0.9789, 0.971},
            {0.9831, 0.9511}, {0.97, 0.9355}, {0.9594, 0.9455},
            {0.955, 0.965}, {0.9519, 1.0}, {0.9425, 0.997},
            {0.9389, 0.9757}, {0.928, 0.962}, {0.924, 0.945},
            {0.9196, 0.9443}, {0.9213, 0.9333}, {0.9287, 0.9264},
            {0.921, 0.917}, {0.9195, 0.904}, {0.926, 0.908},
            {0.908, 0.891}, {0.903, 0.878}, {0.895, 0.881},
            {0.893, 0.87}, {0.891, 0.866}, {0.885, 0.864},
            {0.889, 0.859}, {0.875, 0.855}, {0.875, 0.85},
            {0.871, 0.849}, {0.875, 0.843}, {0.87, 0.835},
            {0.866, 0.831}, {0.861, 0.811}, {0.859, 0.806},
            {0.846, 0.777},
            // Ancient NW head, gulf extended inland
            {0.832, 0.761}, {0.812, 0.733}, {0.792, 0.706},
            {0.78, 0.683}, {0.78, 0.667}, {0.792, 0.65},
            {0.808, 0.639}, {0.824, 0.65}, {0.84, 0.667},
            {0.852, 0.694}, {0.86, 0.711},
            // Iranian coast (cleaned NE 10m)
            {0.876, 0.72}, {0.885, 0.698}, {0.89, 0.698},
            {0.895, 0.71}, {0.9, 0.714}, {0.905, 0.721},
            {0.922, 0.711}, {0.926, 0.726}, {0.946, 0.753},
            {0.946, 0.77}, {0.957, 0.775}, {0.952, 0.782},
            {0.962, 0.792}, {0.976, 0.837},
            // Off-screen closure (right + bottom edge)
            {1.05, 0.843}, {1.05, 1.05}, {0.984, 1.05}
    };

    // RED SEA
    // Both coasts of the Red Sea / Gulf of Aqaba. Self-closing.
    private static final double[][] RED_SEA = {
            {0.4103, 1.0165}, {0.4059, 1.0083}, {0.4103, 1.0072},
            {0.4097, 0.9903}, {0.3975, 0.9641}, {0.3881, 0.9567},
            {0.3878, 0.9432}, {0.3805, 0.9383}, {0.3287, 0.8303},
            {0.3030, 0.8281}, {0.3122, 0.8040}, {0.3199, 0.7481},
            {0.3095, 0.7605}, {0.2963, 0.8162}, {0.2974, 0.8347},
            {0.2900, 0.8484}, {0.2705, 0.8320}, {0.2491, 0.8017},
            {0.2471, 0.7784}, {0.2286, 0.7524}, {0.2229, 0.7216},
            {0.2135, 0.7449}, {0.2237, 0.7584}, {0.2250, 0.7793},
            {0.2622, 0.8391}, {0.2637, 0.8449}, {0.2589, 0.8432},
            {0.2637, 0.8530}, {0.2599, 0.8530}, {0.2798, 0.8946},
            {0.2777, 0.9081}, {0.3232, 1.0147}
    };

    private static final double[][][] ALL_SEAS = {
            MEDITERRANEAN_SEA, BLACK_SEA, CASPIAN_SEA, PERSIAN_GULF, RED_SEA
    };

    // ISLANDS (land inside sea areas)
    private static final double[][] CYPRUS = {
            {0.3018, 0.4068}, {0.3037, 0.4060}, {0.2769, 0.4290},
            {0.2760, 0.4353}, {0.2840, 0.4464}, {0.2672, 0.4463},
            {0.2642, 0.4543}, {0.2435, 0.4614}, {0.2404, 0.4653},
            {0.2412, 0.4684}, {0.2376, 0.4684}, {0.2365, 0.4635},
            {0.2283, 0.4641}, {0.2184, 0.4599}, {0.2110, 0.4418},
            {0.2115, 0.4390}, {0.2180, 0.4412}, {0.2222, 0.4346},
            {0.2346, 0.4357}, {0.2372, 0.4315}, {0.2368, 0.4223},
            {0.2571, 0.4262}, {0.2664, 0.4245}, {0.3018, 0.4068}
    };

    private static final double[][][] ALL_ISLANDS = {
            CYPRUS
    };

    // RIVERS (Natural Earth 10m rivers + ancient lower courses)
    // Upper courses from Natural Earth; lower courses follow ancient channels
    // (separate mouths into the Gulf, before Shatt al-Arab formed c. 1000 AD)
    private static final double[][] EUPHRATES = {
            {0.5782, 0.1567}, {0.5711, 0.1677}, {0.5264, 0.1722},
            {0.5304, 0.1818}, {0.5241, 0.1911}, {0.498, 0.1833},
            {0.4545, 0.2}, {0.4685, 0.213}, {0.4725, 0.2292},
            {0.4593, 0.2359}, {0.4549, 0.248}, {0.4846, 0.2587},
            {0.4846, 0.2671}, {0.4903, 0.2693}, {0.4759, 0.2886},
            {0.4769, 0.2937}, {0.472, 0.2946}, {0.4747, 0.2971},
            {0.4474, 0.3104}, {0.4396, 0.3086}, {0.4336, 0.3185},
            {0.4418, 0.3478}, {0.4517, 0.3596}, {0.4424, 0.3814},
            {0.4454, 0.3889}, {0.4584, 0.3967}, {0.4814, 0.3928},
            {0.5122, 0.4036}, {0.5167, 0.4151}, {0.5367, 0.4327},
            {0.5378, 0.4425}, {0.5571, 0.4656}, {0.5597, 0.4762},
            // Lower Euphrates, ancient course through Babylon, Uruk, Ur
            {0.58, 0.5}, {0.6, 0.5278}, {0.62, 0.5444},
            {0.64, 0.5556}, {0.66, 0.5667}, {0.68, 0.5778},
            {0.692, 0.5833}, {0.704, 0.5944}, {0.72, 0.6111},
            {0.732, 0.6278}, {0.744, 0.65}, {0.762, 0.662}, {0.775, 0.67}
    };

    private static final double[][] TIGRIS = {
            {0.4869, 0.2542}, {0.5004, 0.2491}, {0.5076, 0.2568},
            {0.5194, 0.2568}, {0.5276, 0.2667}, {0.5256, 0.2714},
            {0.5307, 0.2731}, {0.5303, 0.2867}, {0.5912, 0.2918},
            {0.594, 0.3096}, {0.6011, 0.3093}, {0.6138, 0.32},
            {0.6202, 0.3416}, {0.6357, 0.3497}, {0.63, 0.3608},
            {0.645, 0.3668}, {0.6521, 0.381}, {0.6544, 0.3955},
            {0.6497, 0.4086}, {0.6508, 0.4216}, {0.6634, 0.4427},
            {0.6609, 0.4482}, {0.6708, 0.4706}, {0.6729, 0.4869},
            {0.6771, 0.4941}, {0.6979, 0.5036}, {0.6923, 0.525},
            {0.6952, 0.5392}, {0.7059, 0.5541}, {0.7224, 0.5614},
            {0.7236, 0.5671}, {0.7307, 0.5681}, {0.7302, 0.5726},
            {0.7411, 0.5814}, {0.7524, 0.5832}, {0.7642, 0.5747},
            {0.7865, 0.5845}, {0.79, 0.5912}, {0.7888, 0.604},
            {0.7986, 0.6184}, {0.8054, 0.6187}, {0.806, 0.6271},
            {0.816, 0.642}, {0.828, 0.652}
    };
}
