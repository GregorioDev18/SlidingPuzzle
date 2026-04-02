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
import java.awt.HeadlessException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class Main extends EngineFrame {

    private static final int size = 3;

    private final double normalAnimationTime = 0.3;
    private final double shuffleAnimationTime = 0.01;
    private final double solveAnimationTime = 0.001;

    private int lastShuffleLin = -1;
    private int lastShuffleCol = -1;
    private int shuffleMovesRemaining = 0;
    private int solutionStep = 0;
    private int calls = 0;
    private int MAX_CALLS = 1000000;

    private double pieceSize;
    private double animationTime;
    private double animationSteps;
    private double xStart;
    private double yStart;
    private double xEnd;
    private double yEnd;
    private double hudX;
    private double hudTargetX;
    private double hudStartX;
    private double hudAnimTime = 0.3;
    private double hudAnimProgress = 0;

    private boolean hudAnimating = false;
    private boolean shuffling = false;
    private boolean solving = false;
    private boolean atStart = true;
    private boolean solveFailed = false;

    private java.util.List<int[]> solutionPath;
    private java.util.List<GuiComponent> components;

    private String idUrlStatus;
    private String fullInput = "";

    private GuiButton btnShuffle;
    private GuiButton btnSolve;
    private GuiButton btnUrl;
    private GuiGroup groupUntitled;
    private GuiInputDialog idUrl;

    private Piece[][] grid;
    private Piece isMoving;

    private URL urlImg;

    private Image pieceImg;

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

        hudTargetX = 0;
        hudX = -120;

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

                    } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e) {
                    }
                }

            } catch (HeadlessException | UnsupportedFlavorException | IOException e) {
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
                solveFailed = false;
                solutionStep = 0;
                if (btnSolve.isEnabled()) {
                    atStart = false;
                }
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
            solveFailed = false;
            atStart = false;
            shuffleMovesRemaining = size * size * size * 5;
            shuffling = true;
        }

        if (shuffling && isMoving == null && shuffleMovesRemaining > 0) {
            shuffle();
            shuffleMovesRemaining--;
        }

        if (shuffleMovesRemaining == 0) {
            shuffling = false;
        }

        if (btnSolve.isMousePressed() && isMoving == null) {
            int[] start = getCurrentState();

            solutionPath = new ArrayList<>();

            calls = 0;

            boolean found = solveDFS(start, new java.util.HashSet<>(), solutionPath, 0, 1000);

            if (found) {
                solving = true;
                solutionStep = 0;

                hudStartX = -140;
                hudTargetX = 0;
                hudAnimProgress = 0;
                hudAnimating = true;

                solveFailed = false;
            } else {
                solveFailed = true;
            }
        }

        if (solving && isMoving == null && solutionStep < solutionPath.size()) {
            int[] current = getCurrentState();
            int[] next = solutionPath.get(solutionStep);

            int movePos = -1;

            for (int i = 0; i < current.length; i++) {
                if (current[i] != next[i] && current[i] != -1) {
                    movePos = i;
                    break;
                }
            }

            int lin = movePos / size;
            int col = movePos % size;

            movePiece(lin, col);

            solutionStep++;

        }

        if (solutionStep >= solutionPath.size()) {
            solving = false;
        }

        if (hudAnimating) {
            hudAnimProgress += delta;

            double t = hudAnimProgress / hudAnimTime;
            if (t > 1) {
                t = 1;
            }

            hudX = hudStartX + (hudTargetX - hudStartX) * t;

            if (t >= 1) {
                hudAnimating = false;
            }
        }

        if (shuffling) {
            animationTime = shuffleAnimationTime;
        } else if (solving) {
            animationTime = solveAnimationTime;
        } else {
            animationTime = normalAnimationTime;
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

        if (isSolved(getCurrentState()) && !atStart && !shuffling && !solving) {
            fillRectangle(0, 0, getScreenWidth(), pieceImg.getHeight(), ColorUtils.fade(BLACK, 0.5));
            int messageFontSize = 60;

            String wonMessage = "!YOU WON!";
            String subMessage = "PLAY IT AGAIN";

            drawText(wonMessage, getScreenWidth() / 2 - measureText(wonMessage, messageFontSize) / 2, pieceImg.getHeight() / 2 - messageFontSize, messageFontSize, GREEN);
            drawText(subMessage, getScreenWidth() / 2 - measureText(subMessage, messageFontSize / 2) / 2, pieceImg.getHeight() / 2, messageFontSize / 2, WHITE);
        }

        if (solving || isSolved(getCurrentState()) && !atStart && solutionStep > 0) {
            fillRectangle(hudX, 0, 140, 30, LIGHTGRAY);
            drawRectangle(hudX, 0, 140, 30, GRAY);
            drawText(String.valueOf(solutionStep) + " / " + String.valueOf(solutionPath.size()), (int) hudX + 8, 8, 20, DARKGRAY);
        }

        if (solveFailed) {
            String msg = "UNABLE TO SOLVE (DEPTH LIMIT)";
            int fontSize = 30;

            fillRectangle(0, 0, getScreenWidth(), pieceImg.getHeight(), ColorUtils.fade(BLACK, 0.5));
            drawText(msg, getScreenWidth() / 2 - measureText(msg, fontSize) / 2, pieceImg.getHeight() / 2, fontSize, RED);
        }
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
        }

        pieceImg = ImageUtils.loadImage(urlImg);
        pieceImg.resize(getScreenWidth(), getScreenWidth());

        drawImage();
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

    private void shuffle() {
        int emptyLin = -1;
        int emptyCol = -1;

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

    private boolean isSolved(int[] state) {
        for (int i = 0; i < state.length - 1; i++) {
            if (state[i] != i) {
                return false;
            }
        }

        return state[state.length - 1] == -1;
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

    private boolean solveDFS(int[] state, java.util.Set<String> visited, java.util.List<int[]> path, int depth, int maxDepth) {
        calls++;

        if (calls > MAX_CALLS) {
            return false;
        }

        if (depth >= maxDepth) {
            return false;
        }

        if (isSolved(state)) {
            return true;
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
        
        visited.remove(key);

        return false;
    }

    public static void main(String[] args) {
        new Main();
    }
}
