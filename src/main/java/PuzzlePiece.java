import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Point;

public class PuzzlePiece {
    private BufferedImage image;
    private int x, y;
    private int correctX, correctY;
    private int row, col;
    private int correctRow, correctCol;

    public PuzzlePiece(BufferedImage image, int correctX, int correctY, int col, int row) {
        this.image = image;
        this.correctX = correctX;
        this.correctY = correctY;
        this.x = col * image.getWidth();
        this.y = row * image.getHeight();
        this.col = col;
        this.row = row;
        this.correctCol = col;
        this.correctRow = row;
    }

    public void draw(Graphics g) { // 绘制拼图块
        g.drawImage(image, x, y, null);
    }

    public boolean contains(Point p) { // 判断点击是否在拼图块内
        return p.x >= this.x && p.x < this.x + image.getWidth() && p.y >= this.y && p.y < this.y + image.getHeight();
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /*
     * 获取拼图块的原始位置
     */
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    /*
     * 获取拼图块的当前位置
     */
    public int getCorrectX() {
        return correctX;
    }

    public int getCorrectY() {
        return correctY;
    }

    // 新增的方法
    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public int getCorrectRow() {
        return correctRow;
    }

    public int getCorrectCol() {
        return correctCol;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public void setCol(int col) {
        this.col = col;
    }
}
