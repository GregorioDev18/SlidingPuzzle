package template;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import br.com.davidbuzatto.jsge.image.Image;
import br.com.davidbuzatto.jsge.imgui.GuiButton;
import br.com.davidbuzatto.jsge.imgui.GuiComponent;
import java.util.ArrayList;

public class Main extends EngineFrame {

    private java.util.List<GuiComponent> components;
    private static final int size = 3;
    private Piece[][] grid;
    private double pieceSize;
    private Image pieceImg;
    private GuiButton btnShuffle;

    public Main() {

        super(600, 700, "Slinding Puzzle", 60, true);

    }

    @Override
    public void create() {
        useAsDependencyForIMGUI();

        grid = new Piece[size][size];
        pieceSize = getScreenWidth() / size;
        pieceImg = loadImage("resources/images/twice.png");

        pieceImg.resize(getScreenWidth(), getScreenHeight());

        components = new ArrayList<>();

        btnShuffle = new GuiButton(0, getScreenHeight() - 60, 100, 30, "SHUFFLE");

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = new Piece(j * pieceSize, i * pieceSize, pieceSize, i * size + j, pieceImg);
            }
        }

        grid[size - 1][size - 1] = null;

        components.add(btnShuffle);
    }

    @Override
    public void update(double delta) {
        if (isMouseButtonPressed(MOUSE_BUTTON_LEFT)) {
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    if (grid[i][j] != null) {
                        if (grid[i][j].intercepts(getMouseX(), getMouseY())) {
                            movePiece(i, j);
                        }
                    }
                }
            }
        }

        if (btnShuffle.isMousePressed()) {
            shuffle(1);
        }

        for (GuiComponent c : components) {
            c.update(delta);
        }
    }

    @Override
    public void draw() {

        clearBackground(BLACK);

        for (GuiComponent c : components) {
            c.draw();
        }

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (grid[i][j] != null) {
                    grid[i][j].draw(this, size);
                }
            }
        }
    }

    private void movePiece(int lin, int col) {
        int[] neighborLin = {-1, 0, 1, 0};
        int[] neighborCol = {0, 1, 0, -1};

        int destLin = -1;
        int destCol = -1;

        for (int i = 0; i < 4; i++) {
            int currentLine = lin + neighborLin[i];
            int currentCol = col + neighborCol[i];

            if (currentLine >= 0 && currentLine < size && currentCol >= 0 && currentCol < size) {
                if (grid[currentLine][currentCol] == null) {
                    destLin = currentLine;
                    destCol = currentCol;
                    break;
                }
            }
        }

        if (destLin != -1) {
            grid[destLin][destCol] = grid[lin][col];
            grid[lin][col] = null;
            recalculatePositions();
        }
    }

    private void recalculatePositions() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (grid[i][j] != null) {
                    grid[i][j].setPos(j * pieceSize, i * pieceSize);
                }
            }
        }
    }

    private void shuffle(int times) {

        java.util.Random rand = new java.util.Random();

        while (times > 0) {

            int emptyLin = -1;
            int emptyCol = -1;

            // encontrar espaço vazio
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    if (grid[i][j] == null) {
                        emptyLin = i;
                        emptyCol = j;
                    }
                }
            }

            int[] neighborLin = {-1, 0, 1, 0};
            int[] neighborCol = {0, 1, 0, -1};

            int dir = rand.nextInt(4);

            int lin = emptyLin + neighborLin[dir];
            int col = emptyCol + neighborCol[dir];

            if (lin >= 0 && lin < size && col >= 0 && col < size) {
                movePiece(lin, col);
                times--;
            }

        }

    }

    //voltar a peça -> pilha
    //salvar estado -> copia do array
    public static void main(String[] args) {
        new Main();
    }
}
