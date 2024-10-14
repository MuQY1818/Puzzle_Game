import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.awt.Point;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.Graphics2D;
import java.util.Random;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Comparator;

public class PuzzleGame extends JFrame {
    private BufferedImage originalImage; // 原始图片
    private BufferedImage resizedImage; // 调整大小后的图片（用于拼图）
    private List<PuzzlePiece> puzzlePieces;  // 拼图块列表
    private JPanel gamePanel;  // 游戏面板
    private int rows = 3;  // 行数
    private int cols = 3;  // 列数
    private static final int PUZZLE_WIDTH = 300; // 减小拼图宽度
    private static final int PUZZLE_HEIGHT = 200; // 减小拼图高度
    private int pieceWidth; // 拼图块宽度
    private int pieceHeight; // 拼图块高度
    private PuzzlePiece selectedPiece;  // 当前选中的拼图块
    private Point mouseOffset;  // 鼠标偏移量
    private JButton showOriginalButton;  // 显示原始图片按钮
    private JButton randomizeButton; // 随机切换图片
    private JButton changeDifficultyButton; // 改变难度按钮
    private JButton challengeButton; // 挑战模式
    private Timer challengeTimer; // 挑战模式计时器
    private int remainingTime; // 剩余时间
    private JLabel timerLabel; // 时间
    private Difficulty currentDifficulty = Difficulty.EASY; // 当前难度
    private int emptyRow; // 空白格的行
    private int emptyCol; // 空白格的列
    private JPanel originalImagePanel; // 新增：用于显示原图的面板
    private JButton solveButton; // 新增：一键解题按钮

    public PuzzleGame() {
        setTitle("拼图游戏");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 400); // 调整窗口大小
        setLayout(new BorderLayout());

        // 创建一个包含游戏面板和原图面板的容器
        JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                for(PuzzlePiece piece : puzzlePieces) {
                    piece.draw(g);
                }
            }
        };
        gamePanel.setPreferredSize(new Dimension(PUZZLE_WIDTH, PUZZLE_HEIGHT));
        mainPanel.add(gamePanel);

        // 创建并添加原图面板
        originalImagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (resizedImage != null) {
                    g.drawImage(resizedImage, 0, 0, this);
                }
            }
        };
        originalImagePanel.setPreferredSize(new Dimension(PUZZLE_WIDTH, PUZZLE_HEIGHT));
        mainPanel.add(originalImagePanel);

        add(mainPanel, BorderLayout.CENTER);

        // 控制面板
        JPanel controlPanel = new JPanel();
        showOriginalButton = new JButton("显示原始图片");
        randomizeButton = new JButton("随机切换图片");
        changeDifficultyButton = new JButton("改变难度");
        challengeButton = new JButton("挑战模式");
        timerLabel = new JLabel("时间: 0");
        solveButton = new JButton("一键解题");

        controlPanel.add(showOriginalButton);
        controlPanel.add(randomizeButton);
        controlPanel.add(changeDifficultyButton);
        controlPanel.add(challengeButton);
        controlPanel.add(timerLabel);
        controlPanel.add(solveButton);

        add(controlPanel, BorderLayout.SOUTH);

        addListeners();
        loadImage("D:\\Code\\Acwing Spring Boot\\Puzzle_Game\\src\\main\\Images\\KeLi.png");
        setDifficulty(currentDifficulty);
        initializePuzzle();
    }

    public void addListeners() {
        gamePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int clickedRow = e.getY() / pieceHeight;
                int clickedCol = e.getX() / pieceWidth;
                if (isAdjacentToEmpty(clickedRow, clickedCol)) {
                    swapWithEmpty(clickedRow, clickedCol);
                    checkPuzzleCompletion();
                }
            }
        });

        gamePanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (selectedPiece != null) {
                    int newX = e.getX() - mouseOffset.x;
                    int newY = e.getY() - mouseOffset.y;
                    selectedPiece.setLocation(newX, newY);
                    gamePanel.repaint();
                }
            }
        });

        // showOriginalButton.addActionListener(e -> showOriginalImage());
        randomizeButton.addActionListener(e -> randomizeImage());
        changeDifficultyButton.addActionListener(e -> changeDifficulty());
        challengeButton.addActionListener(e -> startChallengeMode());
        solveButton.addActionListener(e -> solvePuzzle());
    }

    private BufferedImage loadImage(String imagePath) {
        try {
            originalImage = ImageIO.read(new File(imagePath));
            if (originalImage == null) {
                throw new IOException("无法加载图像: " + imagePath);
            }
            resizeImage();
            return originalImage;
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "加载图像失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private void resizeImage() {
        if (originalImage == null) {
            return;
        }
        resizedImage = new BufferedImage(PUZZLE_WIDTH, PUZZLE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, PUZZLE_WIDTH, PUZZLE_HEIGHT, null);
        g.dispose();
    }

    private void initializePuzzle() {
        if (resizedImage == null) {
            JOptionPane.showMessageDialog(this, "请先加载图像", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        puzzlePieces = new ArrayList<>();
        pieceWidth = PUZZLE_WIDTH / cols;
        pieceHeight = PUZZLE_HEIGHT / rows;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (i == rows - 1 && j == cols - 1) {
                    // 最后一个位置设为空白
                    emptyRow = i;
                    emptyCol = j;
                    continue;
                }
                BufferedImage pieceImage = resizedImage.getSubimage(
                        j * pieceWidth, i * pieceHeight, pieceWidth, pieceHeight);
                PuzzlePiece piece = new PuzzlePiece(pieceImage, j * pieceWidth, i * pieceHeight, j, i);
                puzzlePieces.add(piece);
            }
        }
        shufflePuzzle();
        gamePanel.repaint();
        originalImagePanel.repaint();
    }

    private void shufflePuzzle() {
        // 随机移动空白格多次来打乱拼图
        Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            List<Point> validMoves = getValidMoves();
            if (!validMoves.isEmpty()) {
                Point move = validMoves.get(random.nextInt(validMoves.size()));
                swapWithEmpty(move.x, move.y);
            }
        }
    }

    private List<Point> getValidMoves() {
        List<Point> validMoves = new ArrayList<>();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : directions) {
            int newRow = emptyRow + dir[0];
            int newCol = emptyCol + dir[1];
            if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
                validMoves.add(new Point(newRow, newCol));
            }
        }
        return validMoves;
    }

    private void swapWithEmpty(int row, int col) {
        PuzzlePiece piece = getPieceAt(row, col);
        if (piece != null) {
            piece.setLocation(emptyCol * pieceWidth, emptyRow * pieceHeight);
            piece.setRow(emptyRow);
            piece.setCol(emptyCol);
            emptyRow = row;
            emptyCol = col;
            gamePanel.repaint();
        }
    }

    private PuzzlePiece getPieceAt(int row, int col) {
        for (PuzzlePiece piece : puzzlePieces) {
            if (piece.getRow() == row && piece.getCol() == col) {
                return piece;
            }
        }
        return null;
    }

    private void checkPuzzleCompletion() {
        boolean completed = true;
        for (PuzzlePiece piece : puzzlePieces) {
            if (piece.getRow() != piece.getCorrectRow() || piece.getCol() != piece.getCorrectCol()) {
                completed = false;
                break;
            }
        }
        if (completed && emptyRow == rows - 1 && emptyCol == cols - 1) {
            JOptionPane.showMessageDialog(this, "恭喜你完成了拼图！");
            if (challengeTimer != null) {
                challengeTimer.stop();
            }
        }
    }

    private void randomizeImage() {
        loadImage("D:\\Code\\Acwing Spring Boot\\Puzzle_Game\\src\\main\\Images\\KeLi.png");
        initializePuzzle();
        gamePanel.repaint();
        originalImagePanel.repaint(); // 重绘原图面板
    }

    private void setDifficulty(Difficulty difficulty) {
        currentDifficulty = difficulty;
        switch (difficulty) {
            case EASY:
                rows = 3;
                cols = 3;
                break;
            case MEDIUM:
                rows = 4;
                cols = 4;
                break;
            case HARD:
                rows = 5;
                cols = 5;
                break;
        }
    }

    private void changeDifficulty() {
        String[] options = {"容易 (3x3)", "中等 (4x4)", "困难 (5x5)"};
        int choice = JOptionPane.showOptionDialog(this, "选择难度", "难度设置",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        
        switch (choice) {
            case 0:
                setDifficulty(Difficulty.EASY);
                break;
            case 1:
                setDifficulty(Difficulty.MEDIUM);
                break;
            case 2:
                setDifficulty(Difficulty.HARD);
                break;
        }
        initializePuzzle();
        gamePanel.repaint();
    }

    private void startChallengeMode() {
        remainingTime = 60; // 60秒挑战
        timerLabel.setText("时间: " + remainingTime);
        if (challengeTimer != null) {
            challengeTimer.stop();
        }
        challengeTimer = new Timer(1000, e -> {
            remainingTime--;
            timerLabel.setText("时间: " + remainingTime);
            if (remainingTime <= 0) {
                ((Timer)e.getSource()).stop();
                JOptionPane.showMessageDialog(this, "挑战失败！时间到了。");
            }
        });
        challengeTimer.start();
        initializePuzzle();
        gamePanel.repaint();
    }

    private void solvePuzzle() {
        System.out.println("开始解题...");
        State initialState = getCurrentState();
        if (!isSolvable(initialState)) {
            System.out.println("当前拼图状态无解");
            JOptionPane.showMessageDialog(this, "当前拼图状态无解，请重新打乱拼图。");
            return;
        }
        List<Point> solution = findSolution();
        if (solution == null) {
            System.out.println("未找到解决方案");
            JOptionPane.showMessageDialog(this, "无法找到解决方案");
            return;
        }

        System.out.println("找到解决方案，步骤数：" + solution.size());
        System.out.println("解题步骤：");
        for (int i = 0; i < solution.size(); i++) {
            Point move = solution.get(i);
            System.out.println("步骤 " + (i + 1) + ": 移动空格到 (" + move.x + ", " + move.y + ")");
        }

        // 使用Timer来以0.5秒的间隔执行每一步
        Timer timer = new Timer(500, null);
        final int[] index = {0};
        timer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (index[0] < solution.size()) {
                    Point move = solution.get(index[0]);
                    swapWithEmpty(move.x, move.y);
                    gamePanel.repaint();
                    System.out.println("执行步骤 " + (index[0] + 1) + ": 移动空格到 (" + move.x + ", " + move.y + ")");
                    index[0]++;
                } else {
                    ((Timer)e.getSource()).stop();
                    JOptionPane.showMessageDialog(PuzzleGame.this, "拼图已解决！");
                    System.out.println("拼图已解决！");
                }
            }
        });
        timer.start();
    }

    private List<Point> findSolution() {
        System.out.println("开始寻找解决方案...");
        PriorityQueue<State> openSet = new PriorityQueue<>(Comparator.comparingInt(a -> a.f));
        HashMap<State, State> parentMap = new HashMap<>();
        HashMap<State, Integer> gScore = new HashMap<>();
        
        State initialState = getCurrentState();
        System.out.println("初始状态：");
        printState(initialState);
        openSet.offer(initialState);
        gScore.put(initialState, 0);
        initialState.f = heuristic(initialState);

        int iterations = 0;
        while (!openSet.isEmpty()) {
            iterations++;
            if (iterations % 1000 == 0) {
                System.out.println("已搜索 " + iterations + " 个状态");
            }
            State current = openSet.poll();
            if (isGoalState(current)) {
                System.out.println("找到目标状态，共搜索 " + iterations + " 个状态");
                System.out.println("目标状态：");
                printState(current);
                return reconstructPath(parentMap, current);
            }

            List<Point> validMoves = getValidMoves(current.emptyRow, current.emptyCol);
            for (Point move : validMoves) {
                State neighbor = makeMove(current, move);
                int tentativeG = gScore.get(current) + 1;
                
                if (!gScore.containsKey(neighbor) || tentativeG < gScore.get(neighbor)) {
                    parentMap.put(neighbor, current);
                    gScore.put(neighbor, tentativeG);
                    neighbor.f = tentativeG + heuristic(neighbor);
                    
                    if (!openSet.contains(neighbor)) {
                        openSet.offer(neighbor);
                    }
                }
            }
        }

        System.out.println("搜索完毕，未找到解决方案，共搜索 " + iterations + " 个状态");
        return null; // 如果找不到解决方案
    }

    private State getCurrentState() {
        int[][] board = new int[rows][cols];
        for (PuzzlePiece piece : puzzlePieces) {
            board[piece.getRow()][piece.getCol()] = piece.getCorrectRow() * cols + piece.getCorrectCol() + 1;
        }
        board[emptyRow][emptyCol] = 0;
        return new State(board, emptyRow, emptyCol);
    }

    private boolean isGoalState(State state) {
        int value = 1;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (i == rows - 1 && j == cols - 1) {
                    if (state.board[i][j] != 0) return false;
                } else if (state.board[i][j] != value) {
                    return false;
                }
                value++;
            }
        }
        return true;
    }

    private State makeMove(State state, Point move) {
        int[][] newBoard = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            newBoard[i] = state.board[i].clone();
        }
        newBoard[state.emptyRow][state.emptyCol] = newBoard[move.x][move.y];
        newBoard[move.x][move.y] = 0;
        return new State(newBoard, move.x, move.y);
    }

    private List<Point> reconstructPath(HashMap<State, State> parentMap, State goalState) {
        List<Point> path = new ArrayList<>();
        State current = goalState;
        State parent = parentMap.get(current);
        while (parent != null) {
            path.add(new Point(current.emptyRow, current.emptyCol));
            current = parent;
            parent = parentMap.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    private int heuristic(State state) {
        int distance = 0;
        int linearConflicts = 0;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (state.board[i][j] != 0) {
                    int value = state.board[i][j] - 1;
                    int targetRow = value / cols;
                    int targetCol = value % cols;
                    distance += Math.abs(i - targetRow) + Math.abs(j - targetCol);

                    // 检查行冲突
                    if (i == targetRow) {
                        for (int k = j + 1; k < cols; k++) {
                            if (state.board[i][k] != 0) {
                                int otherValue = state.board[i][k] - 1;
                                int otherTargetRow = otherValue / cols;
                                int otherTargetCol = otherValue % cols;
                                if (i == otherTargetRow && otherTargetCol < targetCol) {
                                    linearConflicts += 2;
                                }
                            }
                        }
                    }

                    // 检查列冲突
                    if (j == targetCol) {
                        for (int k = i + 1; k < rows; k++) {
                            if (state.board[k][j] != 0) {
                                int otherValue = state.board[k][j] - 1;
                                int otherTargetRow = otherValue / cols;
                                int otherTargetCol = otherValue % cols;
                                if (j == otherTargetCol && otherTargetRow < targetRow) {
                                    linearConflicts += 2;
                                }
                            }
                        }
                    }
                }
            }
        }
        return distance + linearConflicts;
    }

    private static class State {
        int[][] board;
        int emptyRow;
        int emptyCol;
        int f; // f = g + h，其中g是到达该状态的成本，h是启发式估计

        State(int[][] board, int emptyRow, int emptyCol) {
            this.board = board;
            this.emptyRow = emptyRow;
            this.emptyCol = emptyCol;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            State state = (State) o;
            return Arrays.deepEquals(board, state.board);
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(board);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new PuzzleGame().setVisible(true);
        });
    }

    private boolean isAdjacentToEmpty(int row, int col) {
        return (Math.abs(row - emptyRow) + Math.abs(col - emptyCol) == 1);
    }

    private List<Point> getValidMoves(int emptyRow, int emptyCol) {
        List<Point> validMoves = new ArrayList<>();
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}}; // 上、下、左、右

        for (int[] dir : directions) {
            int newRow = emptyRow + dir[0];
            int newCol = emptyCol + dir[1];
            if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
                validMoves.add(new Point(newRow, newCol));
            }
        }

        return validMoves;
    }

    private void printState(State state) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                System.out.printf("%2d ", state.board[i][j]);
            }
            System.out.println();
        }
        System.out.println("f = " + state.f);
        System.out.println();
    }

    private boolean isSolvable(State state) {
        int[] flatBoard = new int[rows * cols];
        int index = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                flatBoard[index++] = state.board[i][j];
            }
        }

        int invCount = getInvCount(flatBoard);
        
        if (rows % 2 == 1) {
            // 对于奇数大小的拼图，逆序数必须为偶数
            return invCount % 2 == 0;
        } else {
            // 对于偶数大小的拼图，逆序数加上空格所在的行数（从底部数起）必须为奇数
            int emptyRow = rows - state.emptyRow;
            return (invCount + emptyRow) % 2 == 1;
        }
    }

    private int getInvCount(int[] arr) {
        int invCount = 0;
        for (int i = 0; i < arr.length - 1; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                if (arr[i] != 0 && arr[j] != 0 && arr[i] > arr[j]) {
                    invCount++;
                }
            }
        }
        return invCount;
    }
}