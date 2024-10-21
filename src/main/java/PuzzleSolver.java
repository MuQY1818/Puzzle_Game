import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class PuzzleSolver {
    private final int rows;
    private final int cols;
    private final List<PuzzlePiece> pieces;
    private final PuzzlePiece emptyPiece;
    private final int[] patternDatabase;

    public PuzzleSolver(List<PuzzlePiece> pieces, int rows, int cols) {
        this.pieces = new ArrayList<>(pieces);
        this.rows = rows;
        this.cols = cols;
        this.emptyPiece = findEmptyPiece(pieces);
        if (this.emptyPiece == null) {
            throw new IllegalArgumentException("No empty piece found in the puzzle");
        }
        this.patternDatabase = generatePatternDatabase();
    }

    private PuzzlePiece findEmptyPiece(List<PuzzlePiece> pieces) {
        for (PuzzlePiece piece : pieces) {
            if (piece.getImage() == null) {
                return piece;
            }
        }
        return null;
    }

    public List<Point> solve() {
        if (!isSolvable()) {
            System.out.println("Puzzle is not solvable.");
            return null;
        }

        int[] initialState = getCurrentState();
        int threshold = getHeuristic(initialState);
        int maxIterations = 1000000;
        int iterations = 0;

        while (iterations < maxIterations) {
            SearchResult result = search(initialState, 0, threshold, new ArrayList<>(), iterations);
            if (result.path != null) {
                System.out.println("Solution found after " + iterations + " iterations.");
                return result.path;
            }
            if (result.cost == Integer.MAX_VALUE) {
                System.out.println("No solution found after " + iterations + " iterations.");
                return null;
            }
            threshold = result.cost;
            iterations += 1000;
            System.out.println("Iteration: " + iterations + ", Threshold: " + threshold);
        }

        System.out.println("No solution found after " + iterations + " iterations.");
        return null;
    }

    private SearchResult search(int[] state, int g, int threshold, List<Point> path, int iterations) {
        int f = g + getHeuristic(state);
        if (f > threshold) {
            return new SearchResult(null, f);
        }
        if (isGoalState(state)) {
            return new SearchResult(path, threshold);
        }
        int min = Integer.MAX_VALUE;
        int emptyIndex = findEmptyIndex(state);
        int emptyRow = emptyIndex / cols;
        int emptyCol = emptyIndex % cols;

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : directions) {
            int newRow = emptyRow + dir[0];
            int newCol = emptyCol + dir[1];
            if (newRow >= 0 && newRow < rows && newCol >= 0 && newCol < cols) {
                int[] newState = state.clone();
                int swapIndex = newRow * cols + newCol;
                newState[emptyIndex] = state[swapIndex];
                newState[swapIndex] = 0;

                List<Point> newPath = new ArrayList<>(path);
                newPath.add(new Point(newCol, newRow));

                SearchResult result = search(newState, g + 1, threshold, newPath, iterations + 1);
                if (result.path != null) {
                    return result;
                }
                min = Math.min(min, result.cost);
            }
        }
        return new SearchResult(null, min);
    }

    private boolean isSolvable() {
        int inversions = 0;
        int emptyRow = 0;
        List<Integer> flatPuzzle = new ArrayList<>();

        for (int i = 0; i < pieces.size(); i++) {
            PuzzlePiece piece = pieces.get(i);
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

    private boolean isGoalState(int[] state) {
        for (int i = 0; i < state.length - 1; i++) {
            if (state[i] != i + 1) {
                return false;
            }
        }
        return true;
    }

    private int findEmptyIndex(int[] state) {
        for (int i = 0; i < state.length; i++) {
            if (state[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    private int[] getCurrentState() {
        int[] state = new int[rows * cols];
        for (PuzzlePiece piece : pieces) {
            int index = piece.getRow() * cols + piece.getCol();
            state[index] = piece == emptyPiece ? 0 : piece.getCorrectRow() * cols + piece.getCorrectCol() + 1;
        }
        return state;
    }

    private int getHeuristic(int[] state) {
        int heuristic = 0;
        for (int i = 0; i < state.length; i++) {
            if (state[i] != 0) {
                heuristic += patternDatabase[state[i] * state.length + i];
            }
        }
        return heuristic;
    }

    private int[] generatePatternDatabase() {
        int size = rows * cols;
        int[] database = new int[size * size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i != 0) {
                    int targetRow = (i - 1) / cols;
                    int targetCol = (i - 1) % cols;
                    int currentRow = j / cols;
                    int currentCol = j % cols;
                    database[i * size + j] = Math.abs(targetRow - currentRow) + Math.abs(targetCol - currentCol);
                }
            }
        }
        return database;
    }

    private static class SearchResult {
        final List<Point> path;
        final int cost;

        SearchResult(List<Point> path, int cost) {
            this.path = path;
            this.cost = cost;
        }
    }
}
