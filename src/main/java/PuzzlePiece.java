import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class PuzzlePiece {
    private final BufferedImage image;
    private int x, y;
    private final int correctX, correctY;
    private int row, col;
    private final int correctRow, correctCol;
    private final int width, height;
    private static final int CORNER_RADIUS = 10; // 圆角半径

    public PuzzlePiece(BufferedImage image, int correctX, int correctY, int width, int height, int col, int row) {
        this.image = image;
        this.correctX = correctX;
        this.correctY = correctY;
        this.width = width;
        this.height = height;
        this.col = col;
        this.row = row;
        this.correctCol = col;
        this.correctRow = row;
        this.x = col * width;
        this.y = row * height;
    }

    public void draw(Graphics g) {
        if (image != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 创建圆角矩形
            RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(x, y, width, height, CORNER_RADIUS, CORNER_RADIUS);
            
            // 设置裁剪区域为圆角矩形
            g2d.setClip(roundedRectangle);
            
            // 绘制图像
            g2d.drawImage(image, x, y, null);
            
            // 绘制边框
            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(1));
            g2d.draw(roundedRectangle);
            
            g2d.dispose();
        }
    }

    public boolean contains(Point p) {
        return p.x >= this.x && p.x < this.x + width && p.y >= this.y && p.y < this.y + height;
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getCorrectX() {
        return correctX;
    }

    public int getCorrectY() {
        return correctY;
    }

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

    public void setCurrentPosition(int row, int col) {
        this.row = row;
        this.col = col;
        this.x = col * width;
        this.y = row * height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public BufferedImage getImage() {
        return image;
    }
}
