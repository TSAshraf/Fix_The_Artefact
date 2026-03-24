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

public class FixThePotGame extends JPanel implements MouseListener, MouseMotionListener {

    private ArrayList<PuzzlePiece> pieces;      // pieces in logical (image) coords
    private PuzzlePiece selectedPiece = null;
    private int offsetX, offsetY;               // drag offsets in logical coords
    private final int snapThreshold = 100;      // screen-pixel feel, converted to logical

    private BufferedImage potImage;

    private int puzzleRows = 2;
    private int puzzleCols = 2;

    public int getPuzzleRows() { return puzzleRows; }
    public int getPuzzleCols() { return puzzleCols; }

    // view transform
    private double viewScale = 1.0;             // screen = draw + logical * scale
    private double drawX = 0;
    private double drawY = 0;

    // solved listener
    public interface PuzzleSolvedListener { void puzzleSolved(); }
    private PuzzleSolvedListener solvedListener;
    public void setPuzzleSolvedListener(PuzzleSolvedListener l) { this.solvedListener = l; }

    // hint system: 0=off, 1=edges only, 2=corners only, 3=placement guide
    private int hintLevel = 0;
    private PuzzlePiece guidedPiece = null; // tier 3: which piece to highlight
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

    public FixThePotGame() {
        // Load default image from resources (NO File paths)
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

    /**
     * Loads an image from the classpath resources.
     * @param resourcePath must start with "/" like "/Ancient Cyprus/jug-1.jpg"
     */
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
            return null; // unreachable, required by compiler
        }
    }

    /**
     * Loads an image from resources; returns null on failure.
     * Use this for user-selected images (don’t crash the program).
     */
    private BufferedImage loadImageResource(String resourcePath) {
        try (var in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            return ImageIO.read(in);
        } catch (Exception e) {
            return null;
        }
    }

    private void createPieces() {
        if (potImage == null) return;
        pieces.clear();
        hintLevel = 0;
        guidedPiece = null;
        int pieceW = potImage.getWidth() / puzzleCols;
        int pieceH = potImage.getHeight() / puzzleRows;
        for (int r = 0; r < puzzleRows; r++) {
            for (int c = 0; c < puzzleCols; c++) {
                int cx = c * pieceW;
                int cy = r * pieceH;

                BufferedImage img = potImage.getSubimage(cx, cy, pieceW, pieceH);
                PuzzlePiece p = new PuzzlePiece(img, cx, cy, r, c);

                p.x = (int) (Math.random() * (1000 - pieceW));
                p.y = (int) (Math.random() * (1000 - pieceH));
                pieces.add(p);
            }
        }
        Collections.shuffle(pieces);
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

    /**
     * Called by your panel when changing puzzles.
     * resourcePath should look like "/Ancient Cyprus/jug-2.jpg"
     */
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

    // --- view math ---
    private void updateViewTransform() {
        int pw = getWidth(), ph = getHeight();
        int iw = potImage.getWidth(), ih = potImage.getHeight();
        if (pw <= 0 || ph <= 0 || iw <= 0 || ih <= 0) {
            viewScale = 1.0;
            drawX = drawY = 0;
            return;
        }
        double sx = pw / (double) iw, sy = ph / (double) ih;
        viewScale = Math.min(1.0, Math.min(sx, sy)); // never upscale
        double sw = iw * viewScale, sh = ih * viewScale;
        drawX = (pw - sw) / 2.0;
        drawY = (ph - sh) / 2.0;
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
        Graphics2D g2 = (Graphics2D) g.create();

        // assembly area background
        int areaX = (int) Math.round(drawX);
        int areaY = (int) Math.round(drawY);
        int areaW = (int) Math.round(potImage.getWidth() * viewScale);
        int areaH = (int) Math.round(potImage.getHeight() * viewScale);
        g2.setColor(Color.BLACK);
        g2.fillRect(areaX, areaY, areaW, areaH);

        // draw pieces scaled
        for (PuzzlePiece p : pieces) {
            int px = logicalToScreenX(p.x);
            int py = logicalToScreenY(p.y);
            int pw = (int) Math.round(p.image.getWidth() * viewScale);
            int ph = (int) Math.round(p.image.getHeight() * viewScale);

            boolean dimmed = false;
            if (hintLevel == 1 && !isEdge(p) && !p.placed) dimmed = true;
            if (hintLevel == 2 && !isCorner(p) && !p.placed) dimmed = true;
            if (hintLevel == 3 && p != guidedPiece && !p.placed) dimmed = true;

            if (dimmed) {
                Composite orig = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
                g2.drawImage(p.image, px, py, pw, ph, this);
                g2.setComposite(orig);
            } else {
                g2.drawImage(p.image, px, py, pw, ph, this);
            }
        }
        // Tier 3: highlight guided piece and its destination
        if (hintLevel == 3 && guidedPiece != null && !guidedPiece.placed) {
            // Ghost at destination
            int gx = logicalToScreenX(guidedPiece.correctX);
            int gy = logicalToScreenY(guidedPiece.correctY);
            int gw = (int) Math.round(guidedPiece.image.getWidth() * viewScale);
            int gh = (int) Math.round(guidedPiece.image.getHeight() * viewScale);

            Composite orig = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            g2.drawImage(guidedPiece.image, gx, gy, gw, gh, this);
            g2.setComposite(orig);

            // Yellow outline on destination
            g2.setColor(Color.YELLOW);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(gx, gy, gw, gh);

            // Yellow outline on the piece itself
            int px = logicalToScreenX(guidedPiece.x);
            int py = logicalToScreenY(guidedPiece.y);
            g2.drawRect(px, py, gw, gh);
        }
        g2.dispose();
    }

    // --- mouse ---
    @Override
    public void mousePressed(MouseEvent e) {
        int mx = screenToLogicalX(e.getX());
        int my = screenToLogicalY(e.getY());

        for (int i = pieces.size() - 1; i >= 0; i--) {
            PuzzlePiece p = pieces.get(i);
            int pw = p.image.getWidth(), ph = p.image.getHeight();
            if (mx >= p.x && mx <= p.x + pw && my >= p.y && my <= p.y + ph) {
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
            // snap to the piece’s correct logical position (NO extra offset)
            int snapX = selectedPiece.correctX;
            int snapY = selectedPiece.correctY;

            // keep the same on-screen snap feel regardless of scale
            int logicalThreshold = (int) Math.round(snapThreshold / Math.max(0.0001, viewScale));

            if (Math.abs(selectedPiece.x - snapX) < logicalThreshold &&
                    Math.abs(selectedPiece.y - snapY) < logicalThreshold) {

                selectedPiece.x = snapX;
                selectedPiece.y = snapY;
                selectedPiece.placed = true;

                // advance guided piece if the current one was just placed
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
                    JOptionPane.showMessageDialog(this, "Congratulations! You fixed the pot!");
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

    // --- piece model ---
    private static final class PuzzlePiece {
        final BufferedImage image;
        final int correctX, correctY;
        final int row, col;
        int x, y;
        boolean placed = false;

        PuzzlePiece(BufferedImage img, int cx, int cy, int row, int col) {
            image = img;
            correctX = cx;
            correctY = cy;
            this.row = row;
            this.col = col;
        }
    }

}
