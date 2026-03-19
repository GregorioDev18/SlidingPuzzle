package template;

import br.com.davidbuzatto.jsge.core.engine.EngineFrame;
import static br.com.davidbuzatto.jsge.core.engine.EngineFrame.BLACK;
import br.com.davidbuzatto.jsge.core.utils.ColorUtils;
import br.com.davidbuzatto.jsge.image.Image;
import br.com.davidbuzatto.jsge.image.ImageUtils;
import br.com.davidbuzatto.jsge.imgui.GuiButton;
import br.com.davidbuzatto.jsge.imgui.GuiComponent;
import br.com.davidbuzatto.jsge.imgui.GuiGroup;
import br.com.davidbuzatto.jsge.imgui.GuiInputDialog;
import br.com.davidbuzatto.jsge.imgui.GuiTextField;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;

public class Main extends EngineFrame {

    private static final int size = 3;

    private java.util.List<GuiComponent> components;

    private Piece[][] grid;
    private Piece isMoving;

    private int lastShuffleLin = -1;
    private int lastShuffleCol = -1;
    private int shuffleMovesRemaining = 0;

    private boolean shuffling = false;

    private double pieceSize;
    private double animationTime;
    private double normalAnimationTime = 0.3;
    private double shuffleAnimationTime = 0.05;
    private double animationSteps;
    private double xStart;
    private double yStart;
    private double xEnd;
    private double yEnd;

    private double solveTimer;
    private double solveDelay;

    private java.util.List<int[]> solutionPath;
    private int solutionStep = 0;
    private boolean solving = false;
    private boolean atStart = true;

    private URL urlImg;

    private Image pieceImg;

    private GuiButton btnShuffle;
    private GuiButton btnSolve;
    private GuiButton btnUrl;
    private GuiGroup groupUntitled;
    private GuiInputDialog idUrl;

    private String idUrlStatus;
    private String fullInput = "";

    public Main() {

        super(600, 700, "Sliding Puzzle", 60, true);

    }

    @Override
    public void create() {
        useAsDependencyForIMGUI();

        components = new ArrayList<>();

        grid = new Piece[size][size];

        solutionPath = new ArrayList<>();

        pieceImg = ImageUtils.loadImage("resources/images/ifsp.jpg");

        pieceSize = getScreenWidth() / size;

        pieceImg.resize(getScreenWidth(), getScreenWidth());

        isMoving = null;
        animationTime = normalAnimationTime;
        animationSteps = 0.0;

        solveTimer = 0;
        solveDelay = 0.2;

        groupUntitled = new GuiGroup(0, pieceImg.getHeight(), getScreenWidth(), getScreenHeight() - pieceImg.getHeight(), "");

        btnShuffle = new GuiButton(groupUntitled.getX() + 10, pieceImg.getHeight() + 10, getScreenWidth() / 2 - 15, 30, "SHUFFLE");

        btnSolve = new GuiButton(btnShuffle.getX() + btnShuffle.getWidth() + 10, btnShuffle.getY(), btnShuffle.getWidth(), 30, "SOLVE");

        btnUrl = new GuiButton(btnShuffle.getX(), btnShuffle.getY() + btnShuffle.getHeight() + 15, getScreenWidth() - 20, 30, "LOAD IMAGE");

        idUrl = new GuiInputDialog("Load Image", "Provide the image URL:", true);

        idUrlStatus = "";

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = new Piece(j * pieceSize, i * pieceSize, pieceSize, i * size + j, pieceImg);
            }
        }

        grid[size - 1][size - 1] = null;

        components.add(btnShuffle);
        components.add(btnSolve);
        components.add(btnUrl);
        components.add(groupUntitled);
        components.add(idUrl);
    }

    @Override
    public void update(double delta) {
        if (isKeyDown(KEY_CONTROL) && isKeyPressed(KEY_V)) {
            try {
                String clipboard = (String) Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .getData(DataFlavor.stringFlavor);

                if (idUrl.isVisible()) {

                    try {
                        java.lang.reflect.Field field = GuiInputDialog.class.getDeclaredField("textField");
                        field.setAccessible(true);

                        GuiTextField internalTf = (GuiTextField) field.get(idUrl);

                        fullInput += clipboard;

                        int maxChars = 25;

                        String display = fullInput;

                        if (display.length() > maxChars) {
                            display = display.substring(display.length() - maxChars);
                        }

                        internalTf.setValue(display);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (btnUrl.isMousePressed()) {
            atStart = true;
            idUrl.show();
            fullInput = "";
        }

        if (idUrl.isCloseButtonPressed()) {
            atStart = false;
            idUrlStatus = "closed";
            idUrl.hide();
        }

        if (idUrl.isOkButtonPressed()) {
            atStart = true;
            idUrlStatus = fullInput;
            changeImage(idUrlStatus);
            idUrl.hide();
        }

        if (idUrl.isEnterKeyPressed()) {
            atStart = true;
            idUrlStatus = fullInput;
            changeImage(idUrlStatus);
            idUrl.hide();
        }

        if (idUrl.isCancelButtonPressed()) {
            atStart = false;
            idUrlStatus = "canceled";
            idUrl.hide();
        }

        for (GuiComponent c : components) {
            c.update(delta);
        }

        if (!idUrl.isVisible()) {
            if (isMouseButtonPressed(MOUSE_BUTTON_LEFT) && isMoving == null && !shuffling && !solving) {
                atStart = false;
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
        }

        if (isMoving != null) {

            animationSteps += delta;

            double t = animationSteps / animationTime;

            if (t > 1) {
                t = 1;
            }

            double x = xStart + (xEnd - xStart) * t;
            double y = yStart + (yEnd - yStart) * t;

            isMoving.setPos(x, y);

            if (t >= 1) {
                isMoving.setPos(xEnd, yEnd);
                isMoving = null;
            }

        }

        if (btnShuffle.isMousePressed()) {
            atStart = false;
            animationTime = shuffleAnimationTime;
            shuffleMovesRemaining = size * size * size * 2;
            shuffling = true;
        }

        if (shuffling && isMoving == null && shuffleMovesRemaining > 0) {
            shuffleOneMove();
            shuffleMovesRemaining--;
        }

        if (shuffleMovesRemaining == 0) {
            shuffling = false;
            animationTime = normalAnimationTime;
        }

        if (btnSolve.isMousePressed() && isMoving == null) {

            int[] start = getCurrentState();

            solutionPath = new ArrayList<>();

            boolean found = solveDFS(start, new java.util.HashSet<>(), solutionPath, 0, 80);

            if (found) {
                solving = true;
                solutionStep = 0;
            }

        }

        if (solving && isMoving == null && solutionStep < solutionPath.size()) {

            solveTimer += delta;

            if (solveTimer >= solveDelay) {

                applyState(solutionPath.get(solutionStep));
                solutionStep++;

                solveTimer = 0;
            }
        }

        if (solutionStep >= solutionPath.size()) {
            solving = false;
        }

        btnSolve.setEnabled(!(isSolved(getCurrentState())) && !shuffling && !solving);
        btnShuffle.setEnabled(!shuffling && !solving);
        btnUrl.setEnabled(!shuffling && !solving);
    }

    @Override
    public void draw() {

        clearBackground(BLACK);

        fillRectangle(groupUntitled.getBounds(), LIGHTGRAY);

        groupUntitled.setBorderColor(LIGHTGRAY);

        drawImage();

        for (GuiComponent c : components) {
            c.draw();
            c.setBackgroundColor(WHITE);
        }

        if (isSolved(getCurrentState()) && atStart == false) {
            fillRectangle(0, 0, getScreenWidth(), pieceImg.getHeight(), ColorUtils.fade(BLACK, 0.5));
        }

        drawText(String.valueOf(solutionStep) + " / " + String.valueOf(solutionPath.size()), 0, 0, 20, PINK);
    }

    private void drawImage() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (grid[i][j] != null) {
                    grid[i][j].setImage(pieceImg);
                    grid[i][j].draw(this, size);
                }
            }
        }
    }

    private void changeImage(String url) {
        try {
            urlImg = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        pieceImg = ImageUtils.loadImage(urlImg);
        pieceImg.resize(getScreenWidth(), getScreenWidth());

        drawImage();
    }

    private void movePiece(int lin, int col) {

        if (isMoving != null) {
            return;
        }

        int[] neighborLin = {-1, 0, 1, 0};
        int[] neighborCol = {0, 1, 0, -1};

        int destLin = -1;
        int destCol = -1;

        for (int i = 0; i < 4; i++) {

            int currentLin = lin + neighborLin[i];
            int currentCol = col + neighborCol[i];

            if (currentLin >= 0 && currentLin < size
                    && currentCol >= 0 && currentCol < size) {

                if (grid[currentLin][currentCol] == null) {
                    destLin = currentLin;
                    destCol = currentCol;
                    break;
                }

            }

        }

        if (destLin != -1) {

            Piece p = grid[lin][col];

            grid[destLin][destCol] = p;
            grid[lin][col] = null;

            xStart = col * pieceSize;
            yStart = lin * pieceSize;

            xEnd = destCol * pieceSize;
            yEnd = destLin * pieceSize;

            isMoving = p;

            animationSteps = 0;

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

    private void applyState(int[] state) {

        Piece[] pieces = new Piece[size * size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {

                Piece p = grid[i][j];

                if (p != null) {
                    pieces[p.getValue()] = p;
                }

            }
        }

        int k = 0;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {

                int val = state[k++];

                if (val == -1) {
                    grid[i][j] = null;
                } else {

                    Piece p = pieces[val];
                    grid[i][j] = p;

                    if (p != null) {
                        p.setPos(j * pieceSize, i * pieceSize);
                    }

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

    private int findEmpty(int[] state) {

        for (int i = 0; i < state.length; i++) {
            if (state[i] == -1) {
                return i;
            }
        }

        return -1;
    }

    private int[] swap(int[] state, int a, int b) {

        int[] newState = state.clone();

        int temp = newState[a];
        newState[a] = newState[b];
        newState[b] = temp;

        return newState;

    }

    private boolean solveDFS(int[] state, java.util.Set<String> visited, java.util.List<int[]> path, int depth, int maxDepth) {

        if (isSolved(state)) {
            return true;
        }

        if (depth >= maxDepth) {
            return false;
        }

        String key = java.util.Arrays.toString(state);

        if (visited.contains(key)) {
            return false;
        }

        visited.add(key);

        int empty = findEmpty(state);

        int lin = empty / size;
        int col = empty % size;

        int[] dLin = {-1, 0, 1, 0};
        int[] dCol = {0, 1, 0, -1};

        for (int i = 0; i < 4; i++) {

            int nl = lin + dLin[i];
            int nc = col + dCol[i];

            if (nl >= 0 && nl < size && nc >= 0 && nc < size) {

                int pos = nl * size + nc;

                int[] next = swap(state, empty, pos);

                path.add(next);

                if (solveDFS(next, visited, path, depth + 1, maxDepth)) {
                    return true;
                }

                path.remove(path.size() - 1);

            }

        }

        return false;

    }

    private void shuffleOneMove() {

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

        int prevEmptyLin = emptyLin;
        int prevEmptyCol = emptyCol;

        int[] neighborLin = {-1, 0, 1, 0};
        int[] neighborCol = {0, 1, 0, -1};

        java.util.List<int[]> candidates = new ArrayList<>();

        for (int i = 0; i < 4; i++) {

            int lin = emptyLin + neighborLin[i];
            int col = emptyCol + neighborCol[i];

            if (lin >= 0 && lin < size && col >= 0 && col < size) {

                if (!(lin == lastShuffleLin && col == lastShuffleCol)) {
                    candidates.add(new int[]{lin, col});
                }

            }

        }

        if (!candidates.isEmpty()) {

            java.util.Random rand = new java.util.Random();

            int p = rand.nextInt(candidates.size());

            int lin = candidates.get(p)[0];
            int col = candidates.get(p)[1];

            movePiece(lin, col);

            lastShuffleLin = prevEmptyLin;
            lastShuffleCol = prevEmptyCol;
        }
    }

    private int[] getCurrentState() {

        int[] state = new int[size * size];
        int k = 0;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {

                if (grid[i][j] == null) {
                    state[k++] = -1;
                } else {
                    state[k++] = grid[i][j].getValue();
                }

            }
        }

        return state;
    }

    private boolean isSolved(int[] state) {

        for (int i = 0; i < state.length - 1; i++) {
            if (state[i] != i) {
                return false;
            }
        }

        return state[state.length - 1] == -1;
    }

    //voltar a peça -> pilha
    //salvar estado -> copia do array
    public static void main(String[] args) {
        new Main();
    }
}
