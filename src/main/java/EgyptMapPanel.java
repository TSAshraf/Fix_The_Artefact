import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

// Interactive map of Ancient Egypt showing archaeological find-sites along the Nile.
// Extended south to include Nubia properly, with Natural Earth Nile river data.

// Coastline: Natural Earth 10m land polygons (public domain).
// Rivers: Natural Earth 10m rivers (public domain).
public class EgyptMapPanel extends JPanel implements ThemeAware {

    public interface MapListener {
        void onSiteSelected(String siteName, List<String> artefactPaths);
        void onBackToCollections();
    }

    private MapListener listener;
    public void setMapListener(MapListener l) { this.listener = l; }

    // Site positions for BBOX: lon [24,37] lat [17,33]
    private static final List<Site> SITES = new ArrayList<>();
    static {
        SITES.add(new Site("Thebes", "Luxor \u2014 Valley of the Kings", 0.6654, 0.4563,
                new String[]{
                        "/Ancient Egypt/Artifacts/Game of Hounds and Jackals.jpeg",
                        "/Ancient Egypt/Artifacts/Kneeling statue of Hatshepsut.jpeg",
                        "/Ancient Egypt/Artifacts/Shabti of Djedkhonsuefankh.jpeg",
                        "/Ancient Egypt/Artifacts/Ring, signet.jpeg"
                }));
        SITES.add(new Site("Amarna", "Tell el-Amarna", 0.5308, 0.3344,
                new String[]{
                        "/Ancient Egypt/Artifacts/Chariots with Court Ladies.jpeg",
                        "/Ancient Egypt/Artifacts/Artist\'s Sketch of a Sparrow.jpeg",
                        "/Ancient Egypt/Artifacts/Ring with Cat and Kittens.jpeg"
                }));
        SITES.add(new Site("Memphis", "Saqqara \u2014 Old Capital", 0.5538, 0.1875,
                new String[]{
                        "/Ancient Egypt/Artifacts/Sarcophagus of Harkhebit.jpeg",
                        "/Ancient Egypt/Artifacts/Figurine of a Pygmy Dance Leader.jpeg",
                        "/Ancient Egypt/Artifacts/Schist Statuette Fragment.jpg"
                }));
        SITES.add(new Site("Nubia", "Upper Nile \u2014 Kush", 0.6769, 0.7812,
                new String[]{
                        "/Ancient Egypt/Artifacts/Inner Coffin Box of Taenty.jpg",
                        "/Ancient Egypt/Artifacts/Offering Table.jpg",
                        "/Ancient Egypt/Artifacts/Wedjat Eye Amulet.jpeg"
                }));
        SITES.add(new Site("Kharga Oasis", "Western Desert", 0.5038, 0.4719,
                new String[]{
                        "/Ancient Egypt/Artifacts/Composite Papyrus Capital.jpeg",
                        "/Ancient Egypt/Artifacts/Amulet of Jackal Headed Deity.jpg"
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

    public EgyptMapPanel() {
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
        String bg = BackgroundCatalog.backgroundFor("/Ancient Egypt/Artifacts/", ThemeManager.get().getCurrent());
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

        // BBOX: lon [24,37] lat [17,33], aspect = 13/16 = 0.8125
        double aspect=0.8125; int margin=60;
        int aW=w-margin*2, aH=h-margin*2-50; int iw,ih;
        if((double)aW/aH>aspect){ih=aH;iw=(int)(ih*aspect);}else{iw=aW;ih=(int)(iw/aspect);}
        int ix=(w-iw)/2, iy=margin+36+(aH-ih)/2;
        mapBounds=new Rectangle(ix,iy,iw,ih);

        boolean dark=ThemeManager.get().getCurrent()==Theme.DARK;
        Color landFill=dark?new Color(65,55,40,200):new Color(215,195,160,220);
        Color landStroke=dark?new Color(130,110,70):new Color(155,125,75);

        Shape oldClip = g2.getClip();
        g2.clipRect(ix, iy, iw, ih);

        // Fill whole map with land, then carve out the sea
        g2.setColor(landFill);
        g2.fillRect(ix, iy, iw, ih);

        Shape[] ss=new Shape[ALL_LANDS.length];
        for(int i=0;i<ALL_LANDS.length;i++) ss[i]=createShape(ALL_LANDS[i],ix,iy,iw,ih);

        // Clear sea areas
        Composite origComp = g2.getComposite();
        g2.setComposite(AlphaComposite.Clear);
        for(Shape s:ss) g2.fill(s);
        g2.setComposite(origComp);

        // Re-draw background in sea
        if(backgroundImage!=null) {
            Shape seaClip = g2.getClip();
            for(Shape s:ss) {
                g2.setClip(s);
                g2.drawImage(backgroundImage,0,0,w,h,this);
                g2.setColor(new Color(0,0,0,140));
                g2.fillRect(0,0,w,h);
            }
            g2.setClip(seaClip);
        } else {
            g2.setColor(ThemeManager.get().palette().base.appBg);
            for(Shape s:ss) g2.fill(s);
        }

        // Coastline strokes
        g2.setColor(new Color(40,80,130,40)); g2.setStroke(new BasicStroke(6f));
        for(Shape s:ss) g2.draw(s);
        g2.setColor(landStroke); g2.setStroke(new BasicStroke(1.0f)); for(Shape s:ss) g2.draw(s);

        // Draw the Nile River
        Color nileColor = dark ? new Color(70, 120, 170, 200) : new Color(50, 100, 150, 180);
        g2.setColor(nileColor);
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(createOpenPath(NILE_MAIN, ix, iy, iw, ih));
        // Modern branches (Natural Earth)
        g2.draw(createOpenPath(NILE_ROSETTA, ix, iy, iw, ih));
        g2.draw(createOpenPath(NILE_DAMIETTA, ix, iy, iw, ih));
        // Ancient delta branches (Herodotus, c. 450 BC)
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(dark ? new Color(70, 120, 170, 130) : new Color(50, 100, 150, 120));
        g2.draw(createOpenPath(NILE_CANOPIC, ix, iy, iw, ih));
        g2.draw(createOpenPath(NILE_SEBENNYTIC, ix, iy, iw, ih));
        g2.draw(createOpenPath(NILE_MENDESIAN, ix, iy, iw, ih));
        g2.draw(createOpenPath(NILE_TANITIC, ix, iy, iw, ih));
        g2.draw(createOpenPath(NILE_PELUSIAC, ix, iy, iw, ih));

        g2.setClip(oldClip);

        // Labels
        Theme.Palette.Fonts f=ThemeManager.get().palette().fonts;
        Theme.Palette.Base b=ThemeManager.get().palette().base;
        g2.setFont(f.caption); g2.setColor(new Color(100,140,180,dark?100:75));
        drawCentred(g2,"Mediterranean Sea",ix+(int)(0.42*iw),iy+(int)(0.04*ih));
        drawCentred(g2,"Red Sea",ix+(int)(0.88*iw),iy+(int)(0.42*ih));
        g2.setColor(new Color(180,150,100,dark?70:55));
        drawCentred(g2,"Western Desert",ix+(int)(0.22*iw),iy+(int)(0.40*ih));
        drawCentred(g2,"Eastern Desert",ix+(int)(0.72*iw),iy+(int)(0.38*ih));
        drawCentred(g2,"Nubian Desert",ix+(int)(0.40*iw),iy+(int)(0.80*ih));

        // Nile label (rotated)
        g2.setColor(new Color(80,130,180,dark?110:85));
        int nlx = ix+(int)(0.58*iw), nly = iy+(int)(0.30*ih);
        Graphics2D g2r = (Graphics2D) g2.create();
        g2r.setFont(f.caption);
        g2r.rotate(-0.55, nlx, nly);
        g2r.drawString("River Nile", nlx, nly);
        g2r.dispose();

        for(int i=0;i<SITES.size();i++) drawMarker(g2,SITES.get(i),i);

        g2.setFont(f.heading);
        String t="Ancient Egypt \u2014 Archaeological Sites"; FontMetrics fm=g2.getFontMetrics();
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
            // Fixed cream colour, the pill is always dark, so theme-muted text (which
            // is dark in light mode) would be invisible against it.
            g2.setColor(new Color(220, 200, 160)); g2.drawString(sub,sx2,sy);
        }
    }

    private void drawLegend(Graphics2D g2,int x,int y,int w){
        g2.setFont(ThemeManager.get().palette().fonts.caption); FontMetrics fm=g2.getFontMetrics();
        String[] ls={"Thebes (4)","Amarna (3)","Memphis (3)","Nubia (3)","Kharga Oasis (2)"};
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

    // Sea outlines (Natural Earth 10m land, public domain)
    // BBOX: lon [24.0, 37.0], lat [17.0, 33.0], aspect 0.8125

    private static final double[][] MED_SEA = {
            {-0.05, -0.05}, {0.2, -0.05}, {0.5, -0.05}, {0.9, -0.05},
            {1.05, -0.05}, {1.05, 0.0}, {0.8522, 0.0}, {0.8512, 0.0048},
            {0.8523, 0.0059}, {0.851, 0.0089}, {0.8483, 0.0108},
            {0.8427, 0.0104}, {0.8392, 0.0269}, {0.8337, 0.045},
            {0.824, 0.0655}, {0.8161, 0.0772}, {0.8061, 0.0889},
            {0.796, 0.0976}, {0.7864, 0.1046}, {0.7728, 0.1107},
            {0.758, 0.1148}, {0.7422, 0.1177}, {0.7307, 0.117},
            {0.7236, 0.1153}, {0.7236, 0.1164}, {0.7214, 0.1167},
            {0.722, 0.1178}, {0.7136, 0.1183}, {0.7167, 0.1187},
            {0.7156, 0.1188}, {0.7157, 0.12}, {0.7125, 0.1187},
            {0.7073, 0.1204}, {0.7093, 0.1213}, {0.7026, 0.1222},
            {0.7034, 0.1201}, {0.7015, 0.1183}, {0.7044, 0.1176},
            {0.6999, 0.1135}, {0.702, 0.1132}, {0.7007, 0.1127},
            {0.6946, 0.1149}, {0.6911, 0.1182}, {0.6888, 0.1183},
            {0.6946, 0.1187}, {0.6973, 0.1166}, {0.6967, 0.1187},
            {0.693, 0.1196}, {0.6941, 0.1204}, {0.6931, 0.1215},
            {0.6893, 0.1209}, {0.6904, 0.1209}, {0.6899, 0.1196},
            {0.6867, 0.118}, {0.6857, 0.1187}, {0.6878, 0.1192},
            {0.6859, 0.1198}, {0.6841, 0.1187}, {0.6746, 0.1222},
            {0.6693, 0.1226}, {0.6704, 0.1217}, {0.6667, 0.1217},
            {0.68, 0.1187}, {0.6867, 0.1153}, {0.674, 0.1199},
            {0.6617, 0.1212}, {0.6546, 0.1183}, {0.6454, 0.1108},
            {0.6419, 0.1099}, {0.6409, 0.108}, {0.6313, 0.1066},
            {0.6319, 0.1076}, {0.6372, 0.1084}, {0.6362, 0.1089},
            {0.6381, 0.1117}, {0.6377, 0.1144}, {0.6356, 0.1144},
            {0.6372, 0.1165}, {0.6334, 0.1184}, {0.6295, 0.1172},
            {0.6292, 0.1183}, {0.6282, 0.1178}, {0.6271, 0.1183},
            {0.6276, 0.1192}, {0.626, 0.1192}, {0.6266, 0.12},
            {0.6229, 0.1192}, {0.6236, 0.1204}, {0.6255, 0.1209},
            {0.6236, 0.1217}, {0.6218, 0.1209}, {0.6218, 0.1197},
            {0.6188, 0.1196}, {0.6208, 0.1183}, {0.6192, 0.1161},
            {0.6166, 0.117}, {0.6182, 0.1156}, {0.6182, 0.1115},
            {0.6161, 0.1118}, {0.6144, 0.1106}, {0.6097, 0.1133},
            {0.6097, 0.1118}, {0.6076, 0.1123}, {0.6082, 0.1115},
            {0.6071, 0.1115}, {0.6082, 0.111}, {0.6082, 0.1093},
            {0.6066, 0.111}, {0.6026, 0.1093}, {0.6016, 0.1076},
            {0.5975, 0.1072}, {0.5997, 0.1056}, {0.5998, 0.1025},
            {0.6045, 0.1029}, {0.6021, 0.102}, {0.6034, 0.1003},
            {0.6023, 0.0995}, {0.6061, 0.099}, {0.6042, 0.0986},
            {0.6045, 0.0973}, {0.6019, 0.0973}, {0.6032, 0.096},
            {0.6061, 0.0982}, {0.605, 0.0939}, {0.6034, 0.0939},
            {0.6039, 0.0926}, {0.605, 0.093}, {0.605, 0.0922},
            {0.6061, 0.0926}, {0.6069, 0.0917}, {0.6091, 0.0923},
            {0.6149, 0.0976}, {0.6129, 0.0977}, {0.615, 0.1003},
            {0.6307, 0.1066}, {0.6173, 0.1}, {0.6111, 0.0924},
            {0.6057, 0.0915}, {0.596, 0.0949}, {0.5813, 0.0973},
            {0.5555, 0.0888}, {0.546, 0.087}, {0.5368, 0.0885},
            {0.5402, 0.0886}, {0.547, 0.0913}, {0.5485, 0.0927},
            {0.547, 0.093}, {0.5484, 0.0935}, {0.5476, 0.0942},
            {0.5439, 0.0927}, {0.5407, 0.0926}, {0.537, 0.0977},
            {0.5338, 0.0964}, {0.5338, 0.0973}, {0.5302, 0.0988},
            {0.523, 0.0971}, {0.5197, 0.0986}, {0.518, 0.1007},
            {0.5135, 0.0987}, {0.5097, 0.1007}, {0.5033, 0.0998},
            {0.5045, 0.0986}, {0.5099, 0.0979}, {0.5365, 0.0883},
            {0.5028, 0.096}, {0.4966, 0.0961}, {0.4894, 0.0932},
            {0.49, 0.0961}, {0.4922, 0.0973}, {0.4896, 0.0962},
            {0.488, 0.0939}, {0.4883, 0.0987}, {0.4856, 0.1031},
            {0.4809, 0.106}, {0.4753, 0.1076}, {0.4811, 0.1076},
            {0.4801, 0.1084}, {0.4837, 0.1076}, {0.4848, 0.108},
            {0.4837, 0.1101}, {0.4822, 0.1084}, {0.4816, 0.1097},
            {0.4785, 0.1112}, {0.4771, 0.1108}, {0.4774, 0.1101},
            {0.4742, 0.1115}, {0.4723, 0.1102}, {0.4748, 0.108},
            {0.4688, 0.1072}, {0.4667, 0.1059}, {0.4669, 0.1042},
            {0.4537, 0.1123}, {0.4521, 0.1117}, {0.451, 0.1127},
            {0.4527, 0.1132}, {0.4491, 0.1154}, {0.4447, 0.1158},
            {0.4435, 0.1174}, {0.4233, 0.1277}, {0.4015, 0.1351},
            {0.3868, 0.1358}, {0.372, 0.1306}, {0.3702, 0.1277},
            {0.3483, 0.1219}, {0.3445, 0.1218}, {0.3403, 0.1198},
            {0.3322, 0.1213}, {0.3234, 0.1206}, {0.3196, 0.1192},
            {0.3021, 0.1188}, {0.2998, 0.1178}, {0.2964, 0.11},
            {0.2816, 0.1137}, {0.2777, 0.1123}, {0.276, 0.1129},
            {0.2651, 0.111}, {0.261, 0.1091}, {0.256, 0.1013},
            {0.2497, 0.1012}, {0.2482, 0.102}, {0.235, 0.0995},
            {0.2281, 0.0969}, {0.2204, 0.0977}, {0.2118, 0.0952},
            {0.1817, 0.0924}, {0.1513, 0.0865}, {0.1403, 0.0866},
            {0.1078, 0.0936}, {0.0916, 0.0918}, {0.0894, 0.0885},
            {0.0889, 0.0836}, {0.0859, 0.0789}, {0.0792, 0.0727},
            {0.0795, 0.0677}, {0.0783, 0.0657}, {0.0755, 0.0645},
            {0.0564, 0.0604}, {0.0516, 0.0605}, {0.042, 0.063},
            {0.0052, 0.0618}, {0.0, 0.0591}, {-0.05, 0.06}, {-0.05, -0.05}
    };

    private static final double[][] RED_SEA_1 = {
            {1.05, 1.05}, {1.05, 0.7223}, {1.0, 0.7223},
            {0.9914, 0.7094}, {0.9921, 0.7076},
            {0.9903, 0.699}, {0.9903, 0.6915}, {0.9877, 0.6896},
            {0.9908, 0.6887}, {0.9922, 0.6864}, {0.992, 0.6834},
            {0.986, 0.6807}, {0.9832, 0.6772}, {0.9813, 0.6764},
            {0.9806, 0.6774}, {0.978, 0.6769}, {0.9664, 0.669},
            {0.9569, 0.6652}, {0.956, 0.6609}, {0.9472, 0.6568},
            {0.9454, 0.652}, {0.9405, 0.6478}, {0.9324, 0.6453},
            {0.9207, 0.6439}, {0.9113, 0.6398}, {0.9065, 0.6354},
            {0.9043, 0.6299}, {0.9018, 0.6293}, {0.9049, 0.6331},
            {0.9029, 0.6323}, {0.8981, 0.6271}, {0.8965, 0.6204},
            {0.8904, 0.6117}, {0.8883, 0.6004}, {0.8838, 0.5939},
            {0.8854, 0.5777}, {0.8833, 0.5767}, {0.8827, 0.5751},
            {0.8839, 0.5706}, {0.8834, 0.5676}, {0.8857, 0.5639},
            {0.8922, 0.5669}, {0.8988, 0.5681}, {0.9039, 0.5667},
            {0.9066, 0.5692}, {0.9071, 0.5686}, {0.903, 0.5635},
            {0.8927, 0.5613}, {0.8914, 0.5578}, {0.8849, 0.5537},
            {0.8841, 0.5538}, {0.8854, 0.5555}, {0.8841, 0.5557},
            {0.8791, 0.5509}, {0.8763, 0.5448}, {0.8643, 0.5375},
            {0.8604, 0.5321}, {0.8573, 0.5312}, {0.8585, 0.5266},
            {0.8546, 0.5223}, {0.851, 0.5147}, {0.8463, 0.5115},
            {0.8454, 0.5059}, {0.8426, 0.5035}, {0.8415, 0.4998},
            {0.8313, 0.4868}, {0.8264, 0.478}, {0.8217, 0.4732},
            {0.818, 0.4644}, {0.8113, 0.4547}, {0.7953, 0.4374},
            {0.7703, 0.3993}, {0.7647, 0.3967}, {0.764, 0.3948},
            {0.7655, 0.391}, {0.7653, 0.3856}, {0.7661, 0.3844},
            {0.7689, 0.3851}, {0.7689, 0.3815}, {0.7655, 0.3793},
            {0.7616, 0.3741}, {0.7616, 0.3713}, {0.7567, 0.3675},
            {0.7557, 0.3637}, {0.7568, 0.3629}, {0.7568, 0.3598},
            {0.7455, 0.3536}, {0.741, 0.344}, {0.7352, 0.3415},
            {0.7357, 0.3393}, {0.7305, 0.3346}, {0.7328, 0.3327},
            {0.7342, 0.3326}, {0.7352, 0.3338}, {0.7347, 0.3324},
            {0.7379, 0.3346}, {0.7365, 0.3324}, {0.7336, 0.3307},
            {0.7352, 0.3275}, {0.7325, 0.328}, {0.7287, 0.3236},
            {0.7299, 0.3221}, {0.7379, 0.3255}, {0.735, 0.319},
            {0.7317, 0.3151}, {0.7263, 0.3115}, {0.7187, 0.3093},
            {0.7183, 0.3072}, {0.7115, 0.3036}, {0.7109, 0.3016},
            {0.7086, 0.3006}, {0.702, 0.2945}, {0.7005, 0.2908},
            {0.6956, 0.2875}, {0.6925, 0.2819}, {0.6822, 0.2763},
            {0.6775, 0.2639}, {0.673, 0.2622}, {0.6678, 0.2575},
            {0.6672, 0.2549}, {0.6635, 0.2517}, {0.6634, 0.247},
            {0.6661, 0.2444}, {0.6661, 0.2431}, {0.664, 0.2348},
            {0.6609, 0.2282}, {0.656, 0.2235}, {0.6452, 0.2171},
            {0.6414, 0.213}, {0.6434, 0.207}, {0.6456, 0.2041},
            {0.6546, 0.1958}, {0.6518, 0.1949}, {0.6521, 0.1917},
            {0.6549, 0.1899}, {0.658, 0.1899}, {0.6593, 0.1911},
            {0.6583, 0.189}, {0.6593, 0.1868}, {0.6598, 0.1885},
            {0.6586, 0.189}, {0.6604, 0.1905}, {0.6593, 0.1919},
            {0.6635, 0.1954}, {0.6635, 0.1971}, {0.6619, 0.1985},
            {0.6653, 0.2003}, {0.6685, 0.2041}, {0.6678, 0.2125},
            {0.6702, 0.2137}, {0.6711, 0.2163}, {0.6704, 0.2215},
            {0.6714, 0.2215}, {0.6709, 0.2208}, {0.6719, 0.2198},
            {0.6724, 0.2215}, {0.6782, 0.2241}, {0.6783, 0.2275},
            {0.68, 0.2302}, {0.6822, 0.2314}, {0.6835, 0.2348},
            {0.6891, 0.2374}, {0.697, 0.2437}, {0.6984, 0.2459},
            {0.7035, 0.248}, {0.706, 0.2507}, {0.7051, 0.2515},
            {0.7057, 0.2602}, {0.7099, 0.2648}, {0.7073, 0.2703},
            {0.7099, 0.2769}, {0.712, 0.278}, {0.7106, 0.2771},
            {0.7125, 0.2764}, {0.7131, 0.2785}, {0.712, 0.2785},
            {0.7207, 0.2845}, {0.7257, 0.2896}, {0.7357, 0.2939},
            {0.7399, 0.2979}, {0.7411, 0.3012}, {0.7442, 0.3025},
            {0.7474, 0.3078}, {0.7511, 0.3094}, {0.751, 0.311},
            {0.7589, 0.3149}, {0.76, 0.3166}, {0.7618, 0.3161},
            {0.7683, 0.3196}, {0.7734, 0.3241}, {0.7838, 0.3258},
            {0.7884, 0.3295}, {0.7884, 0.3286}, {0.7895, 0.329},
            {0.789, 0.3269}, {0.7862, 0.3269}, {0.7857, 0.3255},
            {0.7892, 0.3249}, {0.7907, 0.3214}, {0.7939, 0.3212},
            {0.7947, 0.3179}, {0.7978, 0.3173}, {0.8001, 0.3145},
            {0.8027, 0.314}, {0.8034, 0.3092}, {0.8021, 0.3074},
            {0.8043, 0.3016}, {0.801, 0.2979}, {0.8005, 0.2932},
            {0.8053, 0.284}, {0.8095, 0.2808}, {0.8085, 0.2789},
            {0.8173, 0.2665}, {0.818, 0.2643}, {0.817, 0.2596},
            {0.819, 0.2586}, {0.8185, 0.2532}, {0.822, 0.2507},
            {0.8211, 0.2433}, {0.8227, 0.2397}, {0.8253, 0.2376},
            {0.8266, 0.2318}, {0.8259, 0.2305}, {0.8317, 0.2258},
            {0.8312, 0.2252}, {0.8354, 0.2205}, {0.8393, 0.2194},
            {0.8427, 0.2151}, {0.8459, 0.2166}, {0.8438, 0.2218},
            {0.8433, 0.2275}, {0.8419, 0.2289}, {0.8427, 0.2309},
            {0.841, 0.2319}, {0.8417, 0.2343}, {0.8392, 0.2356},
            {0.8348, 0.2472}, {0.8351, 0.2523}, {0.8327, 0.2562},
            {0.834, 0.2569}, {0.8337, 0.2607}, {0.8291, 0.271},
            {0.8311, 0.2796}, {0.8242, 0.292}, {0.8211, 0.2952},
            {0.8222, 0.296}, {0.8211, 0.2956}, {0.8206, 0.2994},
            {0.8192, 0.2999}, {0.8198, 0.3018}, {0.8175, 0.3015},
            {0.8174, 0.3025}, {0.8133, 0.3055}, {0.8134, 0.3066},
            {0.8159, 0.3065}, {0.8164, 0.3106}, {0.8181, 0.3104},
            {0.819, 0.3081}, {0.8206, 0.3081}, {0.819, 0.3063},
            {0.8211, 0.3055}, {0.8217, 0.3041}, {0.8227, 0.3046},
            {0.8238, 0.3037}, {0.8252, 0.3055}, {0.8285, 0.3059},
            {0.8285, 0.3067}, {0.8322, 0.3055}, {0.8301, 0.3076},
            {0.8311, 0.3081}, {0.8336, 0.3052}, {0.8359, 0.3055},
            {0.834, 0.3063}, {0.8343, 0.3081}, {0.8354, 0.3072},
            {0.8406, 0.3076}, {0.846, 0.3054}, {0.8485, 0.3063},
            {0.8477, 0.3054}, {0.8501, 0.305}, {0.8586, 0.3089},
            {0.8628, 0.3091}, {0.8636, 0.3104}, {0.8587, 0.3119},
            {0.865, 0.3149}, {0.871, 0.3202}, {0.8732, 0.3249},
            {0.8765, 0.3264}, {0.8846, 0.3363}, {0.8854, 0.341},
            {0.8892, 0.3465}, {0.9065, 0.3603}, {0.9087, 0.3631},
            {0.9079, 0.3661}, {0.9163, 0.375}, {0.9241, 0.3811},
            {0.9255, 0.3808}, {0.9306, 0.3904}, {0.9408, 0.3984},
            {0.9468, 0.4064}, {0.9525, 0.4166}, {0.9576, 0.4221},
            {0.9625, 0.4306}, {0.9644, 0.4323}, {0.9724, 0.4337},
            {0.9766, 0.4361}, {0.9774, 0.441}, {0.974, 0.4451},
            {0.9771, 0.4513}, {0.984, 0.455}, {0.9849, 0.4546},
            {0.9843, 0.4525}, {0.986, 0.4526}, {0.9909, 0.4579},
            {0.9951, 0.4596}, {1.0, 0.4665}, {1.0, 0.4708},
            {0.997, 0.4701}, {0.9957, 0.472}, {1.0, 0.4732},
            {1.05, 0.4732}, {1.05, 1.05}
    };

    private static final double[][][] ALL_LANDS = {
            MED_SEA,
            RED_SEA_1,
    };

    // Nile River (Natural Earth 10m rivers, simplified)
    private static final double[][] NILE_MAIN = {
            {0.6819, 1.0312}, {0.7021, 1.0291}, {0.7137, 1.0213},
            {0.7384, 1.0147}, {0.7494, 0.9998}, {0.7463, 0.9935},
            {0.7502, 0.9817}, {0.7679, 0.9603}, {0.7637, 0.921},
            {0.745, 0.9062}, {0.744, 0.8928}, {0.7356, 0.8907},
            {0.7367, 0.8662}, {0.725, 0.8582}, {0.7215, 0.8486},
            {0.6967, 0.8419}, {0.6845, 0.8465}, {0.6659, 0.8647},
            {0.6545, 0.8658}, {0.6419, 0.8819}, {0.6302, 0.8818},
            {0.6182, 0.8885}, {0.619, 0.8988}, {0.6061, 0.902},
            {0.5818, 0.9307}, {0.5544, 0.9385}, {0.5198, 0.9258},
            {0.5156, 0.9083}, {0.5067, 0.9004}, {0.4989, 0.8837},
            {0.4991, 0.8518}, {0.4848, 0.8233}, {0.505, 0.8161},
            {0.508, 0.81}, {0.5067, 0.7955}, {0.5018, 0.7908},
            {0.4914, 0.7899}, {0.4845, 0.7761}, {0.4884, 0.7624},
            {0.5052, 0.7589}, {0.508, 0.7483}, {0.5165, 0.7442},
            {0.5149, 0.7372}, {0.5375, 0.7252}, {0.5355, 0.7216},
            {0.5628, 0.695}, {0.5707, 0.6781}, {0.6047, 0.6579},
            {0.6103, 0.6483}, {0.6211, 0.6421}, {0.6296, 0.6406},
            {0.6378, 0.6479}, {0.6457, 0.6483}, {0.6593, 0.6393},
            {0.6609, 0.6306}, {0.6881, 0.604}, {0.6813, 0.5913},
            {0.686, 0.5782}, {0.6818, 0.5691}, {0.6872, 0.5195},
            {0.6832, 0.4991}, {0.6584, 0.4823}, {0.6525, 0.4656},
            {0.6742, 0.4499}, {0.6741, 0.4305}, {0.6653, 0.4262},
            {0.6358, 0.4356}, {0.6244, 0.4239}, {0.6166, 0.4262},
            {0.6164, 0.4213}, {0.6019, 0.4117}, {0.6006, 0.4037},
            {0.5926, 0.4035}, {0.5934, 0.3984}, {0.5874, 0.3969},
            {0.56, 0.3651}, {0.5473, 0.3624}, {0.547, 0.3581},
            {0.5268, 0.3463}, {0.5297, 0.3249}, {0.526, 0.3101},
            {0.5189, 0.3042}, {0.5186, 0.2924}, {0.5258, 0.2845},
            {0.5315, 0.2606}, {0.5555, 0.2354}, {0.5549, 0.2232},
            {0.5618, 0.2082}, {0.5567, 0.1798}
    };

    private static final double[][] NILE_ROSETTA = {
            {0.5567, 0.1798}, {0.5437, 0.1737}, {0.5378, 0.174},
            {0.5321, 0.1665}, {0.5256, 0.1666}, {0.5256, 0.1649},
            {0.5292, 0.1642}, {0.5265, 0.1624}, {0.527, 0.1534},
            {0.5249, 0.153}, {0.526, 0.1508}, {0.5211, 0.1427},
            {0.5225, 0.1398}, {0.5196, 0.1397}, {0.5243, 0.1358},
            {0.5201, 0.1358}, {0.5226, 0.1332}, {0.5198, 0.1321},
            {0.5217, 0.1298}, {0.5203, 0.126}, {0.5153, 0.1193},
            {0.5054, 0.1125}, {0.5019, 0.1127}, {0.4996, 0.1101},
            {0.5014, 0.1045}, {0.4968, 0.1033}, {0.4922, 0.0973}
    };

    private static final double[][] NILE_DAMIETTA = {
            {0.5496, 0.1765}, {0.5464, 0.1667}, {0.5428, 0.1641},
            {0.5467, 0.1636}, {0.5462, 0.1615}, {0.5481, 0.1623},
            {0.5489, 0.1596}, {0.556, 0.1551}, {0.556, 0.1525},
            {0.5586, 0.153}, {0.5593, 0.1462}, {0.5549, 0.1352},
            {0.558, 0.1342}, {0.5565, 0.1324}, {0.5581, 0.1273},
            {0.5688, 0.1216}, {0.575, 0.1135}, {0.5856, 0.1128},
            {0.5866, 0.1098}, {0.5905, 0.1088}, {0.5892, 0.1076},
            {0.5918, 0.1068}, {0.5936, 0.1011}, {0.6008, 0.0986},
            {0.6034, 0.0922}
    };

    // Ancient Nile Delta branches (Herodotus, c. 450 BC)
    private static final double[][] NILE_CANOPIC = {
            {0.5538, 0.1812}, {0.5077, 0.15}, {0.4846, 0.125}, {0.4692, 0.1}
    };
    private static final double[][] NILE_SEBENNYTIC = {
            {0.5538, 0.1812}, {0.5385, 0.1438}, {0.5346, 0.1125}, {0.5308, 0.0938}
    };
    private static final double[][] NILE_MENDESIAN = {
            {0.5538, 0.1812}, {0.5615, 0.15}, {0.5692, 0.125}, {0.5769, 0.1062}
    };
    private static final double[][] NILE_TANITIC = {
            {0.5538, 0.1812}, {0.5769, 0.1562}, {0.5923, 0.1313}, {0.6077, 0.1125}
    };
    private static final double[][] NILE_PELUSIAC = {
            {0.5538, 0.1812}, {0.5846, 0.1625}, {0.6231, 0.1438}, {0.6615, 0.125}
    };

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
}
