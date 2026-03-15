package template;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.geom.Rectangle;
import br.com.davidbuzatto.jsge.image.Image;
import br.com.davidbuzatto.jsge.math.Vector2;

public class Piece {

    private Vector2 pos;
    private double size;
    private int value;
    private Image image;

    public Piece(double x, double y, double size, int value, Image image) {
        this.pos = new Vector2(x, y);
        this.size = size;
        this.value = value;
        this.image = image;
    }

    public void draw(EngineFrame e, int gridSize) {
        /*e.fillRectangle(pos.x, pos.y, size, size, EngineFrame.VIOLET);
        e.drawText(String.valueOf(value), pos.x + size / 2 - 15, pos.y + size / 2 - 15, 60, EngineFrame.BLACK);*/
        e.drawImage(image, new Rectangle(value % gridSize * size, value / gridSize * size, size, size), new Rectangle(pos.x, pos.y, size, size), EngineFrame.WHITE);
        e.drawRectangle(pos.x, pos.y, size - 1, size - 1, EngineFrame.BLACK);
    }

    public boolean intercepts(int x, int y) {
        return x >= pos.x && x <= pos.x + size
                && y >= pos.y && y <= pos.y + size;
    }

    public int getValue() {
        return value;
    }

    public void setPos(double x, double y) {
        this.pos.x = x;
        this.pos.y = y;
    }
}
