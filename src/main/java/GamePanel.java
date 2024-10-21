import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.BasicStroke;

import javax.swing.SwingUtilities;

public class GamePanel extends JPanel implements MouseListener, MouseMotionListener {
    private final PuzzleGame game;
    private BufferedImage image;
    private List<PuzzlePiece> puzzlePieces;
    private int rows;
    private int cols;
    private int pieceWidth;
    private int pieceHeight;
    private PuzzlePiece emptyPiece;
    private boolean isDraggingMode = false;
    private PuzzlePiece draggedPiece;
    private Point dragOffset;
    private boolean isStandardMode = false;
    private Timer animationTimer;
    private PuzzlePiece movingPiece;
    private int targetX, targetY;
    private static final int ANIMATION_SPEED = 5;
    private static final int ANIMATION_DELAY = 10;
    private PuzzlePiece selectedPiece;
    private Timer glowTimer;
    private float glowAlpha = 0f;
    private static final float GLOW_SPEED = 0.1f;

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
        
        if (!isStandardMode) {
            emptyPiece = puzzlePieces.get(puzzlePieces.size() - 1);
            emptyPiece = new PuzzlePiece(null, emptyPiece.getX(), emptyPiece.getY(), pieceWidth, pieceHeight, cols - 1, rows - 1);
            puzzlePieces.set(puzzlePieces.size() - 1, emptyPiece);
        } else {
            emptyPiece = null;
        }
        
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
            if (piece.contains(p)) {
                if (isStandardMode) {
                    draggedPiece = piece;
                    dragOffset = new Point(p.x - piece.getX(), p.y - piece.getY());
                } else if (piece != emptyPiece && isAdjacentToEmpty(piece)) {
                    swapWithEmpty(piece);
                    repaint();
                    if (isPuzzleSolved()) {
                        game.puzzleSolved();
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
        if (isStandardMode) return;

        int emptyRow = emptyPiece.getRow();
        int emptyCol = emptyPiece.getCol();
        
        targetX = emptyCol * pieceWidth;
        targetY = emptyRow * pieceHeight;
        
        movingPiece = piece;
        
        animationTimer = new Timer(ANIMATION_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int dx = targetX - movingPiece.getX();
                int dy = targetY - movingPiece.getY();
                
                if (Math.abs(dx) < ANIMATION_SPEED && Math.abs(dy) < ANIMATION_SPEED) {
                    movingPiece.setLocation(targetX, targetY);
                    finishSwap();
                    ((Timer)e.getSource()).stop();
                } else {
                    movingPiece.setLocation(
                        movingPiece.getX() + (int)Math.signum(dx) * ANIMATION_SPEED,
                        movingPiece.getY() + (int)Math.signum(dy) * ANIMATION_SPEED
                    );
                }
                repaint();
            }
        });
        
        animationTimer.start();
    }

    private void finishSwap() {
        int tempRow = movingPiece.getRow();
        int tempCol = movingPiece.getCol();
        movingPiece.setCurrentPosition(emptyPiece.getRow(), emptyPiece.getCol());
        emptyPiece.setCurrentPosition(tempRow, tempCol);
        movingPiece = null;
        
        if (isPuzzleSolved()) {
            game.puzzleSolved();
        }
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
        Graphics2D g2d = (Graphics2D) g.create();
        
        // 绘制所有非选中的拼图块
        for (PuzzlePiece piece : puzzlePieces) {
            if (piece != emptyPiece && piece != selectedPiece) {
                piece.draw(g2d);
            }
        }
        
        // 绘制选中效果（如果在点击模式下）
        if (isStandardMode && !isDraggingMode && selectedPiece != null) {
            drawGlowEffect(g2d, selectedPiece);
        }
        
        // 最后绘制选中/拖动的拼图块，确保它在最上层
        if (selectedPiece != null) {
            selectedPiece.draw(g2d);
        }
        
        g2d.dispose();
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
        // 可以添加日志输出来调试
        System.out.println("Dragging mode set to: " + isDraggingMode);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // 可以留空，或者根据需要实现
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (isStandardMode && !isDraggingMode) {
            Point p = e.getPoint();
            PuzzlePiece clickedPiece = null;
            for (PuzzlePiece piece : puzzlePieces) {
                if (piece.contains(p)) {
                    clickedPiece = piece;
                    break;
                }
            }
            
            if (clickedPiece != null) {
                if (selectedPiece == null) {
                    selectedPiece = clickedPiece;
                    startGlowEffect();
                } else if (selectedPiece == clickedPiece) {
                    // 如果点击的是已选中的方块，取消选中
                    selectedPiece = null;
                    stopGlowEffect();
                } else {
                    swapPieces(selectedPiece, clickedPiece);
                    selectedPiece = null;
                    stopGlowEffect();
                    if (isPuzzleSolved()) {
                        game.puzzleSolved();
                    }
                }
                repaint();
            }
        } else if (isStandardMode && isDraggingMode) {
            // 处理拖动模式的逻辑
            handleDragStart(e);
        } else {
            handleMousePress(e.getPoint());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (isStandardMode && isDraggingMode && selectedPiece != null) {
            Point p = e.getPoint();
            PuzzlePiece targetPiece = null;
            boolean isValidMove = false;

            if (p.x >= 0 && p.x < PuzzleGame.PUZZLE_WIDTH && p.y >= 0 && p.y < PuzzleGame.PUZZLE_HEIGHT) {
                int targetCol = p.x / pieceWidth;
                int targetRow = p.y / pieceHeight;
                
                targetPiece = getPieceAt(targetCol, targetRow);
                if (targetPiece != null && targetPiece != selectedPiece) {
                    isValidMove = true;
                }
            }

            if (isValidMove && targetPiece != null) {
                swapPieces(selectedPiece, targetPiece);
                repaint();
            } else {
                selectedPiece.setLocation(selectedPiece.getCol() * pieceWidth, selectedPiece.getRow() * pieceHeight);
            }

            selectedPiece = null;
            repaint();

            if (isPuzzleSolved()) {
                SwingUtilities.invokeLater(() -> game.puzzleSolved());
            }
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
        if (isStandardMode && isDraggingMode && selectedPiece != null) {
            int newX = e.getX() - dragOffset.x;
            int newY = e.getY() - dragOffset.y;
            selectedPiece.setLocation(newX, newY);
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
        
        Timer animationTimer = new Timer(10, null);
        final int[] steps = {0};
        final int totalSteps = 20;
        
        int startX1 = piece1.getX();
        int startY1 = piece1.getY();
        int startX2 = piece2.getX();
        int startY2 = piece2.getY();
        
        int endX1 = piece2.getCol() * pieceWidth;
        int endY1 = piece2.getRow() * pieceHeight;
        int endX2 = piece1.getCol() * pieceWidth;
        int endY2 = piece1.getRow() * pieceHeight;
        
        animationTimer.addActionListener(e -> {
            steps[0]++;
            float progress = (float) steps[0] / totalSteps;
            
            int currentX1 = (int) (startX1 + (endX1 - startX1) * progress);
            int currentY1 = (int) (startY1 + (endY1 - startY1) * progress);
            int currentX2 = (int) (startX2 + (endX2 - startX2) * progress);
            int currentY2 = (int) (startY2 + (endY2 - startY2) * progress);
            
            piece1.setLocation(currentX1, currentY1);
            piece2.setLocation(currentX2, currentY2);
            
            repaint();
            
            if (steps[0] >= totalSteps) {
                piece1.setCurrentPosition(piece2.getRow(), piece2.getCol());
                piece2.setCurrentPosition(tempRow, tempCol);
                ((Timer)e.getSource()).stop();
                
                if (isPuzzleSolved()) {
                    SwingUtilities.invokeLater(() -> game.puzzleSolved());
                }
            }
        });
        
        animationTimer.start();
    }

    public void resetPuzzle(BufferedImage image, int rows, int cols) {
        this.image = image;
        this.rows = rows;
        this.cols = cols;
        initializePuzzle();
        repaint();
    }

    public void setStandardMode(boolean standardMode) {
        this.isStandardMode = standardMode;
        if (!isStandardMode) {
            setDraggingMode(false);  // 在华容道模式下禁止拖动
        }
        initializePuzzle();
    }

    private void startGlowEffect() {
        if (glowTimer != null) {
            glowTimer.stop();
        }
        glowTimer = new Timer(50, e -> {
            glowAlpha += GLOW_SPEED;
            if (glowAlpha > 1f) {
                glowAlpha = 0f;
            }
            repaint();
        });
        glowTimer.start();
    }

    private void stopGlowEffect() {
        if (glowTimer != null) {
            glowTimer.stop();
        }
        glowAlpha = 0f;
    }

    private void drawGlowEffect(Graphics2D g2d, PuzzlePiece piece) {
        int x = piece.getX();
        int y = piece.getY();
        int width = piece.getWidth();
        int height = piece.getHeight();
        
        BufferedImage pieceImage = piece.getImage();
        if (pieceImage != null) {
            // 绘制原始图像
            g2d.drawImage(pieceImage, x, y, width, height, null);
            
            // 绘制高亮效果
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, glowAlpha));
            g2d.setColor(new Color(255, 255, 0, 100)); // 半透明的黄色
            g2d.fillRect(x, y, width, height);
            
            // 绘制边框
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(3f));
            g2d.drawRect(x, y, width - 1, height - 1);
        }
    }

    private void handleDragStart(MouseEvent e) {
        Point p = e.getPoint();
        for (PuzzlePiece piece : puzzlePieces) {
            if (piece.contains(p)) {
                selectedPiece = piece;
                dragOffset = new Point(p.x - piece.getX(), p.y - piece.getY());
                break;
            }
        }
    }
}
