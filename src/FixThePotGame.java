import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class FixThePotGame extends JPanel implements MouseListener, MouseMotionListener {
    private ArrayList<PuzzlePiece> pieces; // List of puzzle pieces
    private PuzzlePiece selectedPiece = null; // The currently selected puzzle piece
    private int offsetX, offsetY;
    private final int snapThreshold = 100; // Distance threshold for snapping the pieces into place

    private BufferedImage potImage; // Pot/artifact image

    // Puzzle split settings (default 2x2)
    private int puzzleRows = 2;
    private int puzzleCols = 2;

    // Listener for puzzle solved event.
    private PuzzleSolvedListener solvedListener;

    // Interface for when the puzzle is solved
    public interface PuzzleSolvedListener {
        void puzzleSolved();
    }

    // Setter for the puzzle solved listener
    public void setPuzzleSolvedListener(PuzzleSolvedListener listener) {
        this.solvedListener = listener;
    }

    // Constructor: Loads image, creates pieces, adds mouse listener
    public FixThePotGame() {
        // Load the default image.
        try {
            potImage = ImageIO.read(new File("/Users/taashfeen/Desktop/Jigsaw Game/src/Ancient Cyprus/jug-1.jpg"));
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not load jug-1.jpg");
            System.exit(1);
        }

        pieces = new ArrayList<>();
        createPieces();
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    // Returns a large preferred size for the full canvas so that pieces can be scattered
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(1000, 1000);
    }

    // Splits the pot image into pieces based on the current puzzle grid
    private void createPieces() {
        pieces.clear();
        int pieceWidth = potImage.getWidth() / puzzleCols;
        int pieceHeight = potImage.getHeight() / puzzleRows;
        for (int row = 0; row < puzzleRows; row++) {
            for (int col = 0; col < puzzleCols; col++) {
                int correctX = col * pieceWidth;  // relative correct position
                int correctY = row * pieceHeight; // relative correct position
                BufferedImage pieceImage = potImage.getSubimage(correctX, correctY, pieceWidth, pieceHeight);
                PuzzlePiece piece = new PuzzlePiece(pieceImage, correctX, correctY);
                // Random starting position within the large 1000x1000 area.
                piece.x = (int) (Math.random() * (1000 - pieceWidth));
                piece.y = (int) (Math.random() * (1000 - pieceHeight));
                pieces.add(piece);
            }
        }
        Collections.shuffle(pieces); // Shuffle pieces to randomise their order
    }

    /**
     * Restarts the game by re-creating the puzzle pieces.
     */
    public void restartGame() {
        createPieces();
        repaint();
    }

    // Sets a new split (difficulty) for the puzzle, then restarts.
    public void setDifficulty(int rows, int cols) {
        this.puzzleRows = rows;
        this.puzzleCols = cols;
        restartGame();
    }

    // Returns the complete image (useful for reference).
    public BufferedImage getPotImage() {
        return potImage;
    }

    // Loads a new image from the specified file and restarts the puzzle.
    public void setImage(String filePath) {
        try {
            potImage = ImageIO.read(new File(filePath));
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not load " + filePath);
            return;
        }
        restartGame();
    }

    // Paints the assembly area and puzzle pieces
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setOpaque(false);

        // Calculate target (assembly) area offset so that the potImage is centered.
        int targetOffsetX = (getWidth() - potImage.getWidth()) / 2;
        int targetOffsetY = (getHeight() - potImage.getHeight()) / 2;

        // Draw a black box for the assembly area.
        g.setColor(Color.BLACK);
        g.drawRect(targetOffsetX, targetOffsetY, potImage.getWidth(), potImage.getHeight());
        g.fillRect(targetOffsetX, targetOffsetY, potImage.getWidth(), potImage.getHeight());

        // Draw each puzzle piece.
        for (PuzzlePiece piece : pieces) {
            g.drawImage(piece.image, piece.x, piece.y, this);
        }
    }

    // When the mouse is pressed, select the topmost puzzle piece under the cursor
    @Override
    public void mousePressed(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();
        for (int i = pieces.size() - 1; i >= 0; i--) {
            PuzzlePiece piece = pieces.get(i);
            if (mx >= piece.x && mx <= piece.x + piece.image.getWidth() &&
                    my >= piece.y && my <= piece.y + piece.image.getHeight()) {
                selectedPiece = piece;
                offsetX = mx - piece.x;
                offsetY = my - piece.y;
                // Bring selected piece to front.
                pieces.remove(piece);
                pieces.add(piece);
                repaint();
                break;
            }
        }
    }

    // Move the selected piece with the mouse
    @Override
    public void mouseDragged(MouseEvent e) {
        if (selectedPiece != null) {
            selectedPiece.x = e.getX() - offsetX;
            selectedPiece.y = e.getY() - offsetY;
            repaint();
        }
    }

    // When the mouse is released, snap the piece into place if close enough
    @Override
    public void mouseReleased(MouseEvent e) {
        if (selectedPiece != null) {
            // Calculate target area offset (centered assembly area).
            int targetOffsetX = (getWidth() - potImage.getWidth()) / 2;
            int targetOffsetY = (getHeight() - potImage.getHeight()) / 2;
            // Check if the piece is within snapThreshold of its correct position (offset by target area).
            if (Math.abs(selectedPiece.x - (targetOffsetX + selectedPiece.correctX)) < snapThreshold &&
                    Math.abs(selectedPiece.y - (targetOffsetY + selectedPiece.correctY)) < snapThreshold) {
                // Snap piece into place.
                selectedPiece.x = targetOffsetX + selectedPiece.correctX;
                selectedPiece.y = targetOffsetY + selectedPiece.correctY;
                selectedPiece.placed = true;
                // Check if all pieces are placed
                boolean solved = true;
                for (PuzzlePiece piece : pieces) {
                    if (!piece.placed) {
                        solved = false;
                        break;
                    }
                }
                // If solved, display a message and notify the listener
                if (solved) {
                    JOptionPane.showMessageDialog(this, "Congratulations! You fixed the pot!");
                    if (solvedListener != null) {
                        solvedListener.puzzleSolved();
                    }
                }
            }
            selectedPiece = null;
            repaint();
        }
    }

    // Unused mouse events
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}
}