import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Collections;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.BasicStroke;
import java.awt.geom.Path2D;
import java.awt.geom.AffineTransform;

public class FixThePotGame extends JPanel implements MouseListener, MouseMotionListener {

    private ArrayList<PuzzlePiece> pieces; // pieces in logical (image) coords
    private PuzzlePiece selectedPiece = null;
    private int offsetX, offsetY; // drag offsets in logical coords
    private final int snapThreshold = 100; // screen-pixel feel, converted to logical

    private BufferedImage potImage;

    private int puzzleRows = 2;
    private int puzzleCols = 2;

    public int getPuzzleRows() { return puzzleRows; }
    public int getPuzzleCols() { return puzzleCols; }

    // view transform
    private double viewScale = 1.0; // screen = draw + logical * scale
    private double drawX = 0;
    private double drawY = 0;

    // Assembly-area (puzzle canvas) background colour.
    // Set by FixThePotGamePanel from the user's saved preset; defaults to black.
    private Color assemblyAreaColor = Color.BLACK;
    public void setAssemblyAreaColor(Color c) {
        if (c != null) {
            this.assemblyAreaColor = c;
            repaint();
        }
    }

    // True when pieces have been (re)created but not yet positioned in the tray.
    // The tray layout depends on the panel size and viewScale, which are only
    // known after the first paint, so deferred until then.
    private boolean piecesNeedLayout = false;

    // Solved listener
    public interface PuzzleSolvedListener { void puzzleSolved(); }
    private PuzzleSolvedListener solvedListener;
    public void setPuzzleSolvedListener(PuzzleSolvedListener l) { this.solvedListener = l; }

    // hint system: 0 = off, 1 =edges only, 2 = corners only, 3 = placement guide
    private int hintLevel = 0;
    private PuzzlePiece guidedPiece = null;
    public int cycleHint() {
        hintLevel = (hintLevel + 1) % 4;
        if (hintLevel == 3) {
            guidedPiece = null;
            for (PuzzlePiece p : pieces) {
                if (!p.placed) { guidedPiece = p; break; }
            }
        } else {
            guidedPiece = null;
        }
        repaint();
        return hintLevel;
    }
    public int getHintLevel() { return hintLevel; }
    private boolean isEdge(PuzzlePiece p) {
        return p.row == 0 || p.row == puzzleRows - 1 || p.col == 0 || p.col == puzzleCols - 1;
    }
    private boolean isCorner(PuzzlePiece p) {
        return (p.row == 0 || p.row == puzzleRows - 1) && (p.col == 0 || p.col == puzzleCols - 1);
    }

    // jigsaw edge assignments
    private int[][] hEdges; // horizontal edges between rows
    private int[][] vEdges; // vertical edges between columns
    private static final int FLAT = 0, TAB = 1, BLANK = -1;

    public FixThePotGame() {
        potImage = loadImageResourceOrDie("/Ancient Cyprus/Artifacts/jug-1.jpg");
        pieces = new ArrayList<>();
        createPieces();
        addMouseListener(this);
        addMouseMotionListener(this);
        setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(1000, 1000);
    }

    private BufferedImage loadImageResourceOrDie(String resourcePath) {
        try (var in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new RuntimeException("Missing resource: " + resourcePath);
            }
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                throw new RuntimeException("Unsupported/invalid image: " + resourcePath);
            }
            return img;
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load " + resourcePath + "\n\n" + e.getMessage(),
                    "Image Load Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
            return null;
        }
    }

    private BufferedImage loadImageResource(String resourcePath) {
        try (var in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            return ImageIO.read(in);
        } catch (Exception e) {
            return null;
        }
    }

    // jigsaw edge generation
    private void generateEdges() {
        hEdges = new int[puzzleRows - 1][puzzleCols];
        vEdges = new int[puzzleRows][puzzleCols - 1];
        java.util.Random rng = new java.util.Random();
        for (int r = 0; r < puzzleRows - 1; r++)
            for (int c = 0; c < puzzleCols; c++)
                hEdges[r][c] = rng.nextBoolean() ? 1 : -1;
        for (int r = 0; r < puzzleRows; r++)
            for (int c = 0; c < puzzleCols - 1; c++)
                vEdges[r][c] = rng.nextBoolean() ? 1 : -1;
    }

    private int topEdge(int r, int c)    { return r == 0 ? FLAT : -hEdges[r - 1][c]; }
    private int bottomEdge(int r, int c) { return r == puzzleRows - 1 ? FLAT : hEdges[r][c]; }
    private int leftEdge(int r, int c)   { return c == 0 ? FLAT : -vEdges[r][c - 1]; }
    private int rightEdge(int r, int c)  { return c == puzzleCols - 1 ? FLAT : vEdges[r][c]; }

    private void addEdge(Path2D.Double path, double x1, double y1, double x2, double y2,
                         int type, double tabSize) {
        if (type == FLAT) { path.lineTo(x2, y2); return; }

        double dx = x2 - x1, dy = y2 - y1;
        double len = Math.sqrt(dx * dx + dy * dy);
        double tx = dx / len, ty = dy / len;
        double nx = ty,  ny = -tx; // outward normal (right of CW travel)
        double s = type; // +1 tab, -1 blank
        double t = tabSize;

        path.lineTo(x1 + 0.35 * dx, y1 + 0.35 * dy);

        // neck in
        path.curveTo(
                x1 + 0.35 * dx + nx * s * t * 0.15, y1 + 0.35 * dy + ny * s * t * 0.15,
                x1 + 0.30 * dx + nx * s * t * 0.35, y1 + 0.30 * dy + ny * s * t * 0.35,
                x1 + 0.30 * dx + nx * s * t * 0.50, y1 + 0.30 * dy + ny * s * t * 0.50
        );
        // bulb
        path.curveTo(
                x1 + 0.30 * dx + nx * s * t * 0.85, y1 + 0.30 * dy + ny * s * t * 0.85,
                x1 + 0.70 * dx + nx * s * t * 0.85, y1 + 0.70 * dy + ny * s * t * 0.85,
                x1 + 0.70 * dx + nx * s * t * 0.50, y1 + 0.70 * dy + ny * s * t * 0.50
        );
        // neck out
        path.curveTo(
                x1 + 0.70 * dx + nx * s * t * 0.35, y1 + 0.70 * dy + ny * s * t * 0.35,
                x1 + 0.65 * dx + nx * s * t * 0.15, y1 + 0.65 * dy + ny * s * t * 0.15,
                x1 + 0.65 * dx, y1 + 0.65 * dy
        );

        path.lineTo(x2, y2);
    }

    private Shape buildPieceShape(int r, int c, int pieceW, int pieceH,
                                  double tabSize, int padL, int padT) {
        double x0 = padL, y0 = padT;
        double x1 = padL + pieceW, y1 = padT + pieceH;

        Path2D.Double path = new Path2D.Double();
        path.moveTo(x0, y0);
        addEdge(path, x0, y0, x1, y0, topEdge(r, c),    tabSize); // top: left to right
        addEdge(path, x1, y0, x1, y1, rightEdge(r, c),  tabSize); // right: top to bottom
        addEdge(path, x1, y1, x0, y1, bottomEdge(r, c), tabSize); // bottom: right to left
        addEdge(path, x0, y1, x0, y0, leftEdge(r, c),   tabSize); // left: bottom to top
        path.closePath();
        return path;
    }

    // piece creation
    private void createPieces() {
        if (potImage == null) return;
        pieces.clear();
        hintLevel = 0;
        guidedPiece = null;

        generateEdges();

        int pieceW = potImage.getWidth() / puzzleCols;
        int pieceH = potImage.getHeight() / puzzleRows;
        double tabSize = Math.min(pieceW, pieceH) * 0.2;
        int tabInt = (int) Math.round(tabSize);

        for (int r = 0; r < puzzleRows; r++) {
            for (int c = 0; c < puzzleCols; c++) {
                int cellX = c * pieceW;
                int cellY = r * pieceH;

                int top = topEdge(r, c), bot = bottomEdge(r, c);
                int left = leftEdge(r, c), right = rightEdge(r, c);

                int padT = (top == TAB)   ? tabInt : 0;
                int padB = (bot == TAB)    ? tabInt : 0;
                int padL = (left == TAB)   ? tabInt : 0;
                int padR = (right == TAB)  ? tabInt : 0;

                int srcX = cellX - padL;
                int srcY = cellY - padT;
                int srcW = pieceW + padL + padR;
                int srcH = pieceH + padT + padB;

                Shape shape = buildPieceShape(r, c, pieceW, pieceH, tabSize, padL, padT);

                BufferedImage pieceImg = new BufferedImage(srcW, srcH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = pieceImg.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(shape);
                g2.drawImage(potImage.getSubimage(srcX, srcY, srcW, srcH), 0, 0, null);
                g2.dispose();

                PuzzlePiece p = new PuzzlePiece(pieceImg, srcX, srcY, r, c, shape, padL, padT);
                // Fallback positions in case paintComponent never runs (e.g. headless).
                // The real tray-aware layout happens in layoutPiecesInTray() on first paint.
                p.x = (int) (Math.random() * Math.max(1, potImage.getWidth() - srcW));
                p.y = (int) (Math.random() * Math.max(1, potImage.getHeight() - srcH));
                pieces.add(p);
            }
        }
        Collections.shuffle(pieces);
        piecesNeedLayout = true;
    }

    // Place each unplaced piece somewhere inside the panel's tray region,
    // fully on-screen, not overlapping the assembly area, and clear of the
    // reserved bottom strip (where the toolbar lives / may return).
    // Called from paintComponent once the view transform is known.

    private void layoutPiecesInTray() {
        int pw = getWidth(), ph = getHeight();
        if (pw <= 0 || ph <= 0 || viewScale <= 0 || potImage == null) return;

        Rectangle assembly = getAssemblyBoundsScreen();
        int availH = Math.max(1, ph - BOTTOM_SPAWN_RESERVE_PX);
        java.util.Random rng = new java.util.Random();

        for (PuzzlePiece p : pieces) {
            if (p.placed) continue;
            int srcW = p.image.getWidth();
            int srcH = p.image.getHeight();
            int pieceScreenW = (int) Math.ceil(srcW * viewScale);
            int pieceScreenH = (int) Math.ceil(srcH * viewScale);

            int maxX = Math.max(1, pw - pieceScreenW);
            int maxY = Math.max(1, availH - pieceScreenH);

            int chosenScreenX = 0;
            int chosenScreenY = 0;
            boolean placed = false;
            for (int attempt = 0; attempt < 80; attempt++) {
                int sx = rng.nextInt(maxX);
                int sy = rng.nextInt(maxY);
                Rectangle pieceRect = new Rectangle(sx, sy, pieceScreenW, pieceScreenH);
                if (!pieceRect.intersects(assembly)) {
                    chosenScreenX = sx;
                    chosenScreenY = sy;
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                // Tray genuinely too small to avoid the assembly, accept any on-screen position so the piece is still reachable.
                chosenScreenX = rng.nextInt(maxX);
                chosenScreenY = rng.nextInt(maxY);
            }

            // Convert screen position back into the logical (image-space) coords the drag/snap logic expects.
            p.x = (int) Math.round((chosenScreenX - drawX) / viewScale);
            p.y = (int) Math.round((chosenScreenY - drawY) / viewScale);
        }
    }

    public void restartGame() {
        createPieces();
        repaint();
    }

    public void setDifficulty(int rows, int cols) {
        this.puzzleRows = Math.max(1, rows);
        this.puzzleCols = Math.max(1, cols);
        restartGame();
    }

    public BufferedImage getPotImage() {
        return potImage;
    }

    void setImage(String resourcePath) {
        BufferedImage img = loadImageResource(resourcePath);
        if (img == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Could not load " + resourcePath + " (resource not found or unreadable)",
                    "Image Load Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        potImage = img;
        restartGame();
    }

    // View math
    // Fraction of the panel the assembly area should occupy in each axis.
    // Tuned so the canvas reads as "the main thing" but leaves a real tray strip on all sides for pieces to spawn into.
    // The old code capped the scale at 1.0 which made small images look marooned in a sea of empty background;
    // here we always scale to fit the target fraction.

    private static final double ASSEMBLY_TARGET_FRACTION = 0.65;
    // Reserved strip at the bottom that pieces must not spawn into, even when the toolbar is auto-hidden,
    // so a returning toolbar can never cover a piece the user placed there.
    private static final int BOTTOM_SPAWN_RESERVE_PX = 80;

    private void updateViewTransform() {
        int pw = getWidth(), ph = getHeight();
        int iw = potImage.getWidth(), ih = potImage.getHeight();
        if (pw <= 0 || ph <= 0 || iw <= 0 || ih <= 0) {
            viewScale = 1.0;
            drawX = drawY = 0;
            return;
        }
        double sx = pw * ASSEMBLY_TARGET_FRACTION / (double) iw;
        double sy = ph * ASSEMBLY_TARGET_FRACTION / (double) ih;
        viewScale = Math.min(sx, sy);
        double sw = iw * viewScale, sh = ih * viewScale;
        drawX = (pw - sw) / 2.0;
        drawY = (ph - sh) / 2.0;
    }

    // Screen-space rectangle of the assembly area (where solved pieces live).
    public Rectangle getAssemblyBoundsScreen() {
        if (potImage == null) return new Rectangle(0, 0, 0, 0);
        int x = (int) Math.round(drawX);
        int y = (int) Math.round(drawY);
        int w = (int) Math.round(potImage.getWidth() * viewScale);
        int h = (int) Math.round(potImage.getHeight() * viewScale);
        return new Rectangle(x, y, w, h);
    }

    private int screenToLogicalX(int sx) { return (int) Math.round((sx - drawX) / viewScale); }
    private int screenToLogicalY(int sy) { return (int) Math.round((sy - drawY) / viewScale); }
    private int logicalToScreenX(int lx) { return (int) Math.round(drawX + lx * viewScale); }
    private int logicalToScreenY(int ly) { return (int) Math.round(drawY + ly * viewScale); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (potImage == null) return;

        updateViewTransform();
        if (piecesNeedLayout && getWidth() > 0 && getHeight() > 0 && viewScale > 0) {
            layoutPiecesInTray();
            piecesNeedLayout = false;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // assembly area background
        int areaX = (int) Math.round(drawX);
        int areaY = (int) Math.round(drawY);
        int areaW = (int) Math.round(potImage.getWidth() * viewScale);
        int areaH = (int) Math.round(potImage.getHeight() * viewScale);
        g2.setColor(assemblyAreaColor);
        g2.fillRect(areaX, areaY, areaW, areaH);

        // draw pieces
        for (PuzzlePiece p : pieces) {
            int px = logicalToScreenX(p.x);
            int py = logicalToScreenY(p.y);
            int pw = (int) Math.round(p.image.getWidth() * viewScale);
            int ph = (int) Math.round(p.image.getHeight() * viewScale);

            boolean dimmed = false;
            if (hintLevel == 1 && !isEdge(p) && !p.placed) dimmed = true;
            if (hintLevel == 2 && !isCorner(p) && !p.placed) dimmed = true;
            if (hintLevel == 3 && p != guidedPiece && !p.placed) dimmed = true;

            Composite orig = g2.getComposite();
            if (dimmed) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
            }
            g2.drawImage(p.image, px, py, pw, ph, this);

            // draw jigsaw outline
            AffineTransform at = new AffineTransform();
            at.translate(px, py);
            at.scale(viewScale, viewScale);
            Shape screenShape = at.createTransformedShape(p.shape);
            g2.setColor(new Color(80, 80, 80, dimmed ? 40 : 160));
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(screenShape);

            if (dimmed) {
                g2.setComposite(orig);
            }
        }

        // Tier 3: highlight guided piece and its destination
        if (hintLevel == 3 && guidedPiece != null && !guidedPiece.placed) {
            int gx = logicalToScreenX(guidedPiece.correctX);
            int gy = logicalToScreenY(guidedPiece.correctY);
            int gw = (int) Math.round(guidedPiece.image.getWidth() * viewScale);
            int gh = (int) Math.round(guidedPiece.image.getHeight() * viewScale);

            Composite orig = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            g2.drawImage(guidedPiece.image, gx, gy, gw, gh, this);
            g2.setComposite(orig);

            // Yellow outline on destination (jigsaw shape)
            AffineTransform atDest = new AffineTransform();
            atDest.translate(gx, gy);
            atDest.scale(viewScale, viewScale);
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(2));
            g2.draw(atDest.createTransformedShape(guidedPiece.shape));

            // Yellow outline on the piece itself
            int px = logicalToScreenX(guidedPiece.x);
            int py = logicalToScreenY(guidedPiece.y);
            AffineTransform atPiece = new AffineTransform();
            atPiece.translate(px, py);
            atPiece.scale(viewScale, viewScale);
            g2.draw(atPiece.createTransformedShape(guidedPiece.shape));
        }
        g2.dispose();
    }

    // mouse
    @Override
    public void mousePressed(MouseEvent e) {
        int mx = screenToLogicalX(e.getX());
        int my = screenToLogicalY(e.getY());

        for (int i = pieces.size() - 1; i >= 0; i--) {
            PuzzlePiece p = pieces.get(i);
            // hit test using jigsaw shape
            if (p.shape.contains(mx - p.x, my - p.y)) {
                selectedPiece = p;
                selectedPiece.placed = false;
                if (hintLevel == 3) {
                    guidedPiece = p;
                }
                offsetX = mx - p.x;
                offsetY = my - p.y;

                pieces.remove(i);
                pieces.add(p); // bring to front

                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                repaint();
                break;
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (selectedPiece != null) {
            int mx = screenToLogicalX(e.getX());
            int my = screenToLogicalY(e.getY());
            selectedPiece.x = mx - offsetX;
            selectedPiece.y = my - offsetY;

            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (selectedPiece != null) {
            int snapX = selectedPiece.correctX;
            int snapY = selectedPiece.correctY;

            int logicalThreshold = (int) Math.round(snapThreshold / Math.max(0.0001, viewScale));

            if (Math.abs(selectedPiece.x - snapX) < logicalThreshold &&
                    Math.abs(selectedPiece.y - snapY) < logicalThreshold) {

                selectedPiece.x = snapX;
                selectedPiece.y = snapY;
                selectedPiece.placed = true;

                if (hintLevel == 3 && guidedPiece == selectedPiece) {
                    guidedPiece = null;
                    for (PuzzlePiece pp : pieces) {
                        if (!pp.placed) { guidedPiece = pp; break; }
                    }
                }

                boolean solved = true;
                for (PuzzlePiece p : pieces) {
                    if (!p.placed) { solved = false; break; }
                }
                if (solved) {
                    if (solvedListener != null) solvedListener.puzzleSolved();
                }
            }

            selectedPiece = null;
            repaint();
        }
        setCursor(Cursor.getDefaultCursor());
    }

    // unused
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}

    // piece model
    private static final class PuzzlePiece {
        final BufferedImage image;
        final int correctX, correctY;
        final int row, col;
        final Shape shape; // jigsaw outline in local (padded image) coords
        final int padLeft, padTop; // offset from image origin to cell origin
        int x, y;
        boolean placed = false;

        PuzzlePiece(BufferedImage img, int cx, int cy, int row, int col,
                    Shape shape, int padLeft, int padTop) {
            image = img;
            correctX = cx;
            correctY = cy;
            this.row = row;
            this.col = col;
            this.shape = shape;
            this.padLeft = padLeft;
            this.padTop = padTop;
        }
    }
}
