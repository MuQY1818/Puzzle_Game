import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.formdev.flatlaf.FlatLightLaf;

public class PuzzleGame extends JFrame {
    private BufferedImage originalImage;
    private BufferedImage resizedImage;
    private final GamePanel gamePanel;
    private final ControlPanel controlPanel;
    private final JLabel originalImageLabel;
    private int rows = 3;
    private int cols = 3;
    public static final int PUZZLE_WIDTH = 400;
    public static final int PUZZLE_HEIGHT = 300;
    private boolean puzzleSolvedHandled = false;

    public PuzzleGame() {
        setTitle("拼图游戏");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(new Color(245, 245, 245));

        loadImage("D:\\Code\\Project\\Puzzle_Game\\src\\main\\Images\\KeLi.png");
        
        gamePanel = new GamePanel(this, resizedImage, rows, cols);
        gamePanel.initialize();
        controlPanel = new ControlPanel(this);
        
        originalImageLabel = new JLabel(new ImageIcon(resizedImage));
        originalImageLabel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setOpaque(false);
        mainPanel.add(gamePanel, BorderLayout.CENTER);
        
        JPanel rightPanel = new JPanel(new BorderLayout(15, 15));
        rightPanel.setOpaque(false);
        rightPanel.add(originalImageLabel, BorderLayout.NORTH);
        rightPanel.add(controlPanel, BorderLayout.CENTER);
        
        JPanel contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        contentPanel.add(mainPanel, BorderLayout.CENTER);
        contentPanel.add(rightPanel, BorderLayout.EAST);
        
        add(contentPanel, BorderLayout.CENTER);
        
        pack();
        setLocationRelativeTo(null);
    }


    private void loadImage(String imagePath) {
        try {
            originalImage = ImageIO.read(new File(imagePath));
            resizeImage();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "加载图像失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resizeImage() {
        if (originalImage == null) return;
        resizedImage = new BufferedImage(PUZZLE_WIDTH, PUZZLE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, PUZZLE_WIDTH, PUZZLE_HEIGHT, null);
        g.dispose();
    }

    public void setDifficulty(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        gamePanel.resetPuzzle(resizedImage, rows, cols);
        revalidate();
        repaint();
    }

    public void randomizePuzzle() {
        gamePanel.randomizePuzzle();
    }

    public void solvePuzzle() {
        gamePanel.solvePuzzle();
    }

    public boolean isPuzzleSolved() {
        return gamePanel.isPuzzleSolved();
    }
    
    public void disableMouseListener() {
        for (MouseListener listener : getMouseListeners()) {
            removeMouseListener(listener);
        }
    }

    public void puzzleSolved() {
        if (puzzleSolvedHandled) {
            return;
        }
        puzzleSolvedHandled = true;

        if (controlPanel.isChallengeMode()) {
            int remainingTime = controlPanel.getRemainingTime();
            JOptionPane.showMessageDialog(this, "恭喜你在挑战模式下完成拼图！\n剩余时间: " + formatTime(remainingTime), "成功", JOptionPane.INFORMATION_MESSAGE);
            controlPanel.stopChallengeTimer();
        } else {
            JOptionPane.showMessageDialog(this, "恭喜你完成拼图！", "成功", JOptionPane.INFORMATION_MESSAGE);
        }

        puzzleSolvedHandled = false;
        gamePanel.resetGame();
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    public void resetGame() {
        gamePanel.initialize();
        puzzleSolvedHandled = false;
        if (controlPanel.isChallengeMode()) {
            controlPanel.stopChallengeTimer();
        }
    }
    public void loadNewImage() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("图片文件", "jpg", "jpeg", "png", "gif");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                BufferedImage newImage = ImageIO.read(selectedFile);
                if (newImage != null) {
                    this.originalImage = newImage;
                    resizeImage();
                    gamePanel.setImage(resizedImage);
                    gamePanel.initialize();
                    originalImageLabel.setIcon(new ImageIcon(resizedImage));
                    repaint();
                } else {
                    JOptionPane.showMessageDialog(this, "无法加载选择的图片", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "读取图片时发生错误", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        try {
            FlatLightLaf.setup();
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf");
        }
        new PuzzleGame().setVisible(true);
    });
}

    public GamePanel getGamePanel() {
        return gamePanel;
    }
}
