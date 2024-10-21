import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

public class ControlPanel extends JPanel {
    private final PuzzleGame game;
    private final JButton randomizeButton;
    private final JButton changeDifficultyButton;
    private final JButton challengeButton;
    private final JButton solveButton;
    private final JButton resetButton;
    private final JButton chooseImageButton;
    private final JLabel timerLabel;
    private Timer challengeTimer;
    private int remainingSeconds = 0;
    private int challengeDuration = 0;
    private final JToggleButton toggleDragModeButton;
    private static final Color TOGGLE_OFF_COLOR = new Color(52, 152, 219);  // 蓝色
    private static final Color TOGGLE_ON_COLOR = new Color(189, 195, 199);  // 灰色
    private JToggleButton toggleGameModeButton;

    public ControlPanel(PuzzleGame game) {
        this.game = game;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(new Color(245, 245, 245));

        // 创建顶部面板，包含模式切换开关
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setOpaque(false);
        
        JLabel modeLabel = new JLabel("游戏模式：");
        modeLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));  // 增大字体
        
        toggleDragModeButton = createToggleButton("拖动模式", "点击模式");
        
        toggleGameModeButton = createToggleButton("标准模式", "华容道模式");
        
        topPanel.add(modeLabel);
        topPanel.add(toggleDragModeButton);
        topPanel.add(toggleGameModeButton);

        // 创建中央按钮面板
        JPanel buttonPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        buttonPanel.setOpaque(false);

        randomizeButton = createStyledButton("随机打乱", new Color(52, 152, 219));
        changeDifficultyButton = createStyledButton("更改难度", new Color(46, 204, 113));
        challengeButton = createStyledButton("挑战模式", new Color(230, 126, 34));
        solveButton = createStyledButton("一键解题", new Color(155, 89, 182));
        resetButton = createStyledButton("重新开始", new Color(231, 76, 60));
        chooseImageButton = createStyledButton("选择图片", new Color(52, 73, 94));

        buttonPanel.add(randomizeButton);
        buttonPanel.add(changeDifficultyButton);
        buttonPanel.add(challengeButton);
        buttonPanel.add(solveButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(chooseImageButton);

        // 创建底部面板，包含计时器
        timerLabel = new JLabel("时间: 0", JLabel.CENTER);
        timerLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        timerLabel.setForeground(new Color(52, 73, 94));

        add(topPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        add(timerLabel, BorderLayout.SOUTH);

        addListeners();
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JToggleButton createToggleButton(String onText, String offText) {
        JToggleButton toggleButton = new JToggleButton(offText);
        toggleButton.setFont(new Font("微软雅黑", Font.BOLD, 14));
        toggleButton.setForeground(Color.WHITE);
        toggleButton.setBackground(TOGGLE_OFF_COLOR);
        toggleButton.setFocusPainted(false);
        toggleButton.setBorderPainted(false);
        toggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        toggleButton.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                toggleButton.setText(onText);
                toggleButton.setBackground(TOGGLE_ON_COLOR);
                game.getGamePanel().setDraggingMode(true);
            } else {
                toggleButton.setText(offText);
                toggleButton.setBackground(TOGGLE_OFF_COLOR);
                game.getGamePanel().setDraggingMode(false);
            }
        });

        return toggleButton;
    }

    private void addListeners() {
        randomizeButton.addActionListener(e -> game.randomizePuzzle());
        resetButton.addActionListener(e -> game.resetGame());
        changeDifficultyButton.addActionListener(e -> {
            String[] options = {"简单 (3x3)", "中等 (4x4)", "困难 (5x5)"};
            int choice = JOptionPane.showOptionDialog(this, "选择难度", "难度设置",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            switch (choice) {
                case 0: 
                    game.setDifficulty(3, 3); 
                    solveButton.setEnabled(true);
                    break;
                case 1: 
                    game.setDifficulty(4, 4); 
                    solveButton.setEnabled(false);
                    break;
                case 2: 
                    game.setDifficulty(5, 5); 
                    solveButton.setEnabled(false);
                    break;
            }
        });
        challengeButton.addActionListener(e -> startChallengeMode());
        solveButton.addActionListener(e -> game.solvePuzzle());
        chooseImageButton.addActionListener(e -> game.loadNewImage());
        toggleGameModeButton.addItemListener(e -> {
            boolean isStandardMode = e.getStateChange() == ItemEvent.SELECTED;
            game.setStandardMode(isStandardMode);
            if (isStandardMode) {
                solveButton.setEnabled(false);
            } else {
                solveButton.setEnabled(game.getRows() == 3 && game.getCols() == 3);
            }
        });
    }

    public void startChallengeMode() {
        String[] options = {"1分钟", "3分钟", "5分钟"};
        int choice = JOptionPane.showOptionDialog(this, "选择挑战时间", "挑战模式",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        
        switch (choice) {
            case 0: remainingSeconds = 60; break;
            case 1: remainingSeconds = 180; break;
            case 2: remainingSeconds = 300; break;
            default: return; // 如果用户取消，则不开始挑战模式
        }
        challengeDuration = remainingSeconds;

        timerLabel.setText("剩余时间: " + formatTime(remainingSeconds));
        game.randomizePuzzle();
        if (challengeTimer != null) {
            challengeTimer.stop();
        }
        challengeTimer = new Timer(1000, e -> {
            remainingSeconds--;
            timerLabel.setText("剩余时间: " + formatTime(remainingSeconds));
            if (remainingSeconds <= 0) {
                stopChallengeTimer();
                JOptionPane.showMessageDialog(this, "挑战失败！", "时间到", JOptionPane.INFORMATION_MESSAGE);
            } else if (game.isPuzzleSolved()) {
                stopChallengeTimer();
                game.puzzleSolved(); // 让 PuzzleGame 处理成功消息
            }
        });
        challengeTimer.start();
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    public void stopChallengeTimer() {
        if (challengeTimer != null) {
            challengeTimer.stop();
        }
    }

    public void resetChallengeTimer() {
        remainingSeconds = challengeDuration;
        timerLabel.setText("剩余时间: " + formatTime(remainingSeconds));
        if (challengeTimer != null) {
            challengeTimer.start();
        }
    }

    public boolean isChallengeMode() {
        return challengeTimer != null && challengeTimer.isRunning();
    }

    public int getRemainingTime() {
        return remainingSeconds;
    }
}
