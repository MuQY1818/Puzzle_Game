import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;

public class GamePanel extends JPanel implements MouseListener, MouseMotionListener {
    private final PuzzleGame game;
    private BufferedImage image;
    private List<PuzzlePiece> puzzlePieces;
    private int rows;
    private int cols;
    private int pieceWidth;
    private int pieceHeight;
    private PuzzlePiece emptyPiece;
    private boolean isDraggingMode;
    private PuzzlePiece draggedPiece;
    private Point dragOffset;

    public GamePanel(PuzzleGame game, BufferedImage image, int rows, int cols) {
        this.game = game;
        this.image = image;
        this.rows = rows;
        this.cols = cols;
        setPreferredSize(new Dimension(PuzzleGame.PUZZLE_WIDTH, PuzzleGame.PUZZLE_HEIGHT));
        addMouseListener(this);
        addMouseMotionListener(this);
        initializePuzzle();
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        initializePuzzle();  // 重新初始化拼图
    }

    private void initializePuzzle() {
        puzzlePieces = new ArrayList<>();
        pieceWidth = PuzzleGame.PUZZLE_WIDTH / cols;
        pieceHeight = PuzzleGame.PUZZLE_HEIGHT / rows;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int x = j * pieceWidth;
                int y = i * pieceHeight;
                BufferedImage pieceImage = image.getSubimage(x, y, pieceWidth, pieceHeight);
                PuzzlePiece piece = new PuzzlePiece(pieceImage, x, y, pieceWidth, pieceHeight, j, i);
                puzzlePieces.add(piece);
            }
        }
        emptyPiece = puzzlePieces.get(puzzlePieces.size() - 1);
        emptyPiece = new PuzzlePiece(null, emptyPiece.getX(), emptyPiece.getY(), pieceWidth, pieceHeight, cols - 1, rows - 1);
        puzzlePieces.set(puzzlePieces.size() - 1, emptyPiece);
        randomizePuzzle();
    }

    public void randomizePuzzle() {
        do {
            Collections.shuffle(puzzlePieces);
            for (int i = 0; i < puzzlePieces.size(); i++) {
                PuzzlePiece piece = puzzlePieces.get(i);
                piece.setCurrentPosition(i / cols, i % cols);
                if (piece == emptyPiece) {
                    emptyPiece = piece;
                }
            }
        } while (!isSolvable() || isPuzzleSolved());
        repaint();
    }

    private boolean isSolvable() {
        int inversions = 0;
        int emptyRow = 0;
        List<Integer> flatPuzzle = new ArrayList<>();

        for (int i = 0; i < puzzlePieces.size(); i++) {
            PuzzlePiece piece = puzzlePieces.get(i);
            if (piece == emptyPiece) {
                emptyRow = piece.getRow();
            } else {
                flatPuzzle.add(piece.getCorrectRow() * cols + piece.getCorrectCol());
            }
        }

        for (int i = 0; i < flatPuzzle.size() - 1; i++) {
            for (int j = i + 1; j < flatPuzzle.size(); j++) {
                if (flatPuzzle.get(i) > flatPuzzle.get(j)) {
                    inversions++;
                }
            }
        }

        if (cols % 2 == 1) {
            return inversions % 2 == 0;
        } else {
            return ((rows - emptyRow) % 2 == 1) == (inversions % 2 == 0);
        }
    }

    private void handleMousePress(Point p) {
        for (PuzzlePiece piece : puzzlePieces) {
            if (piece != emptyPiece && piece.contains(p)) {
                if (isAdjacentToEmpty(piece)) {
                    swapWithEmpty(piece);
                    repaint();
                    if (isPuzzleSolved()) {
                        game.puzzleSolved(); // 只在这里调用一次
                    }
                }
                break;
            }
        }
    }

    private boolean isAdjacentToEmpty(PuzzlePiece piece) {
        int rowDiff = Math.abs(piece.getRow() - emptyPiece.getRow());
        int colDiff = Math.abs(piece.getCol() - emptyPiece.getCol());
        return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1);
    }

    private void swapWithEmpty(PuzzlePiece piece) {
        int tempRow = piece.getRow();
        int tempCol = piece.getCol();
        piece.setCurrentPosition(emptyPiece.getRow(), emptyPiece.getCol());
        emptyPiece.setCurrentPosition(tempRow, tempCol);
    }

    public void solvePuzzle() {
        if (rows != 3 || cols != 3) {
            JOptionPane.showMessageDialog(this, "自动解题功能仅支持3x3难度", "无法解题", JOptionPane.WARNING_MESSAGE);
            return;
        }

        PuzzleSolver solver = new PuzzleSolver(puzzlePieces, rows, cols);
        List<Point> solution = solver.solve();

        if (solution != null) {
            System.out.println("Solution found with " + solution.size() + " moves.");
            animateSolution(solution);
        } else {
            System.out.println("No solution found.");
            JOptionPane.showMessageDialog(this, "无法解决当前拼图", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void resetGame() {
        randomizePuzzle();
    }

    public boolean isPuzzleSolved() {
        for (PuzzlePiece piece : puzzlePieces) {
            if (piece.getRow() != piece.getCorrectRow() || piece.getCol() != piece.getCorrectCol()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (PuzzlePiece piece : puzzlePieces) {
            if (piece != draggedPiece && piece != emptyPiece) {
                piece.draw(g);
            }
        }
        if (draggedPiece != null) {
            draggedPiece.draw(g);
        }
    }

    private void animateSolution(List<Point> solution) {
        Timer timer = new Timer(500, null);
        final int[] index = {0};
        
        timer.addActionListener(e -> {
            if (index[0] < solution.size()) {
                Point move = solution.get(index[0]);
                PuzzlePiece pieceToMove = getPieceAt(move.x, move.y);
                if (pieceToMove != null) {
                    swapWithEmpty(pieceToMove);
                    repaint();
                }
                index[0]++;
            } else {
                ((Timer)e.getSource()).stop();
                if (isPuzzleSolved()) {
                    JOptionPane.showMessageDialog(GamePanel.this, "拼图已解决！", "成功", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        
        timer.start();
    }

    private PuzzlePiece getPieceAt(int col, int row) {
        for (PuzzlePiece piece : puzzlePieces) {
            if (piece.getCol() == col && piece.getRow() == row) {
                return piece;
            }
        }
        return null;
    }

    public void initialize() {
        initializePuzzle();
    }

    public boolean isDraggingMode() {
        return isDraggingMode;
    }

    public void setDraggingMode(boolean isDraggingMode) {
        this.isDraggingMode = isDraggingMode;
        setCursor(isDraggingMode ? new Cursor(Cursor.HAND_CURSOR) : new Cursor(Cursor.DEFAULT_CURSOR));
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // 可以留空，或者根据需要实现
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (isDraggingMode) {
            Point p = e.getPoint();
            for (PuzzlePiece piece : puzzlePieces) {
                if (piece.contains(p) && piece != emptyPiece) {
                    draggedPiece = piece;
                    dragOffset = new Point(p.x - piece.getX(), p.y - piece.getY());
                    break;
                }
            }
        } else {
            handleMousePress(e.getPoint());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (isDraggingMode && draggedPiece != null) {
            Point p = e.getPoint();
            PuzzlePiece targetPiece = null;
            boolean isValidMove = false;

            // 检查是否在拼图区域内
            if (p.x >= 0 && p.x < PuzzleGame.PUZZLE_WIDTH && p.y >= 0 && p.y < PuzzleGame.PUZZLE_HEIGHT) {
                int targetCol = p.x / pieceWidth;
                int targetRow = p.y / pieceHeight;
                
                targetPiece = getPieceAt(targetCol, targetRow);
                if (targetPiece != null && targetPiece != draggedPiece) {
                    isValidMove = true;
                }
            }

            if (isValidMove && targetPiece != null) {
                swapPieces(draggedPiece, targetPiece);
                repaint();
                if (isPuzzleSolved()) {
                    game.puzzleSolved();
                }
            } else {
                // 如果不是有效移动，将拖动的拼图块返回原位
                draggedPiece.setLocation(draggedPiece.getCol() * pieceWidth, draggedPiece.getRow() * pieceHeight);
            }

            draggedPiece = null;
            repaint(); // 确保重绘以更新拼图块位置
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // 可以留空，或者根据需要实现
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // 可以留空，或者根据需要实现
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (isDraggingMode && draggedPiece != null) {
            int newX = e.getX() - dragOffset.x;
            int newY = e.getY() - dragOffset.y;
            draggedPiece.setLocation(newX, newY);
            repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // 可以留空，或者根据需要实现
    }

    private void swapPieces(PuzzlePiece piece1, PuzzlePiece piece2) {
        int tempRow = piece1.getRow();
        int tempCol = piece1.getCol();
        piece1.setCurrentPosition(piece2.getRow(), piece2.getCol());
        piece2.setCurrentPosition(tempRow, tempCol);
    }

    public void resetPuzzle(BufferedImage image, int rows, int cols) {
        this.image = image;
        this.rows = rows;
        this.cols = cols;
        initializePuzzle();
        repaint();
    }
}
