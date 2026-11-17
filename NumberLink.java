package numberlink;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

/**
 * Numberlink game state.
 *
 * The board is an int[][]:
 * 0 = empty cell
 * k = cell used by pair k (the two given numbers + the path drawn between them)
 *
 * We draw the pairs in order: pair 1 first, then pair 2, ...
 * Each move grows the path of the current pair by one cell.
 */
public class NumberLink {

    private int[][] board; // the board
    private int rows;
    private int cols;

    // the two endpoints of each pair: index k = pair k (index 0 not used)
    private int[] startRow;
    private int[] startCol;
    private int[] endRow;
    private int[] endCol;
    private int pairsCount;

    private int currentPair; // the pair we are drawing now (1, 2, ...)
    private int headrow; // last cell of the current path
    private int headcol;

    private int g = 0;
    private NumberLink parent;

    private static final int DIR_NONE = 0;
    private static final int DIR_UP = 1;
    private static final int DIR_DOWN = 2;
    private static final int DIR_LEFT = 3;
    private static final int DIR_RIGHT = 4;

    private int lastDir = DIR_NONE;

    /**
     * main constructor: builds the start state from a board
     *
     * @param parent     null for the start state
     * @param startBoard the puzzle: 0 = empty, k = endpoint of pair k
     */
    public NumberLink(NumberLink parent, int[][] startBoard) {
        rows = startBoard.length;
        cols = startBoard[0].length;
        board = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(startBoard[i], 0, board[i], 0, cols);
        }

        // find the biggest number = number of pairs
        pairsCount = 0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] > pairsCount) {
                    pairsCount = board[i][j];
                }
            }
        }

        // first time we see a number = start, second time = end
        startRow = new int[pairsCount + 1];
        startCol = new int[pairsCount + 1];
        endRow = new int[pairsCount + 1];
        endCol = new int[pairsCount + 1];
        int[] seen = new int[pairsCount + 1];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int k = board[i][j];
                if (k != 0) {
                    if (seen[k] == 0) {
                        startRow[k] = i;
                        startCol[k] = j;
                    } else {
                        endRow[k] = i;
                        endCol[k] = j;
                    }
                    seen[k]++;
                }
            }
        }
        for (int k = 1; k <= pairsCount; k++) {
            if (seen[k] != 2) {
                throw new IllegalArgumentException("number " + k + " must appear exactly 2 times");
            }
        }

        // start drawing pair 1 from its start cell
        currentPair = 1;
        headrow = startRow[1];
        headcol = startCol[1];

        this.parent = parent;
    }

    /**
     * copy constructor
     *
     * @param p a state to copy from
     */
    public NumberLink(NumberLink p) {
        rows = p.rows;
        cols = p.cols;
        board = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(p.board[i], 0, board[i], 0, cols);
        }
        // endpoints never change, so all states can share the same arrays
        startRow = p.startRow;
        startCol = p.startCol;
        endRow = p.endRow;
        endCol = p.endCol;
        pairsCount = p.pairsCount;

        currentPair = p.currentPair;
        headrow = p.headrow;
        headcol = p.headcol;
        lastDir = p.lastDir;
        g = p.g;
        parent = p.parent;
    }

    /**
     * is this the goal state: all pairs connected and no empty cell
     *
     * @return true if final
     */
    public boolean isGoal() {
        if (currentPair <= pairsCount) {
            return false;
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * can the current path enter cell (row, col)?
     * yes if it is empty, or if it is the end of the current pair
     */
    private boolean canEnter(int row, int col) {
        if (row == endRow[currentPair] && col == endCol[currentPair]) {
            return true;
        }
        return board[row][col] == 0;
    }

    /**
     * put the head of the current path on cell (row, col)
     * if it is the end of the pair, the pair is done and we go to the next pair
     */
    private void stepTo(int row, int col) {
        if (row == endRow[currentPair] && col == endCol[currentPair]) {
            currentPair++;
            lastDir = DIR_NONE;
            if (currentPair <= pairsCount) {
                headrow = startRow[currentPair];
                headcol = startCol[currentPair];
            }
        } else {
            board[row][col] = currentPair;
            headrow = row;
            headcol = col;
        }
    }

    private int stepCost(int dir) {
        if (lastDir != DIR_NONE && dir != lastDir) {
            return 2;
        }
        return 1;
    }

    private NumberLink makeChild(int row, int col, int dir) {
        NumberLink child = new NumberLink(this);
        child.g = g + stepCost(dir);
        child.lastDir = dir;
        child.stepTo(row, col);
        child.parent = this;
        return child;
    }

    /**
     * can the current path go up
     *
     * @return true if it can be moved
     */
    public boolean canMoveUp() {
        return currentPair <= pairsCount && headrow != 0 && canEnter(headrow - 1, headcol);
    }

    /**
     * move the current path up and put it in new Object
     *
     * @return same state if it can't be moved
     */
    public NumberLink moveUp() {
        if (!canMoveUp()) {
            return this;
        }
        return makeChild(headrow - 1, headcol, DIR_UP);
    }

    /**
     * can the current path go down
     *
     * @return true if it can be moved
     */
    public boolean canMoveDown() {
        return currentPair <= pairsCount && headrow != rows - 1 && canEnter(headrow + 1, headcol);
    }

    /**
     * move the current path down and put it in new Object
     *
     * @return same state if it can't be moved
     */
    public NumberLink moveDown() {
        if (!canMoveDown()) {
            return this;
        }
        return makeChild(headrow + 1, headcol, DIR_DOWN);
    }

    /**
     * can the current path go left
     *
     * @return true if it can be moved
     */
    public boolean canMoveLeft() {
        return currentPair <= pairsCount && headcol != 0 && canEnter(headrow, headcol - 1);
    }

    /**
     * move the current path left and put it in new Object
     *
     * @return same state if it can't be moved
     */
    public NumberLink moveLeft() {
        if (!canMoveLeft()) {
            return this;
        }
        return makeChild(headrow, headcol - 1, DIR_LEFT);
    }

    /**
     * can the current path go right
     *
     * @return true if it can be moved
     */
    public boolean canMoveRight() {
        return currentPair <= pairsCount && headcol != cols - 1 && canEnter(headrow, headcol + 1);
    }

    /**
     * move the current path right and put it in new Object
     *
     * @return same state if it can't be moved
     */
    public NumberLink moveRight() {
        if (!canMoveRight()) {
            return this;
        }
        return makeChild(headrow, headcol + 1, DIR_RIGHT);
    }

    public List<NumberLink> getNextStates() {
        List<NumberLink> list = new LinkedList<>();

        if (canMoveDown()) {
            list.add(this.moveDown());
        }
        if (canMoveRight()) {
            list.add(this.moveRight());
        }
        if (canMoveUp()) {
            list.add(this.moveUp());
        }
        if (canMoveLeft()) {
            list.add(this.moveLeft());
        }
        return list;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (board[i][j] == 0) {
                    sb.append(".");
                } else {
                    sb.append(board[i][j]);
                }
                sb.append("\t");
            }
            sb.append("\n");
        }
        if (currentPair <= pairsCount) {
            sb.append("drawing pair ").append(currentPair)
                    .append(", head at (").append(headrow).append(",").append(headcol).append(")\n");
        } else {
            sb.append("all pairs connected\n");
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = Arrays.deepHashCode(board);
        hash = 31 * hash + currentPair;
        hash = 31 * hash + headrow * cols + headcol;
        hash = 31 * hash + lastDir;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NumberLink other = (NumberLink) obj;
        if (this.currentPair != other.currentPair
                || this.headrow != other.headrow
                || this.headcol != other.headcol
                || this.lastDir != other.lastDir) {
            return false;
        }
        return Arrays.deepEquals(this.board, other.board);
    }

    public int getG() {
        return g;
    }

    public NumberLink getParent() {
        return this.parent;
    }

    public void printPath() {
        NumberLink state = this;
        LinkedList<NumberLink> path = new LinkedList<>();
        path.add(state);
        while (state.getParent() != null) {
            path.add(state.getParent());
            state = state.getParent();
        }

        Collections.reverse(path);

        path.stream().forEach((get) -> {
            System.out.println(get + "\n____________________");
        });
        System.out.println("solved in " + (path.size() - 1) + " moves");
    }

    public static NumberLink taqyeem1dfs(NumberLink start) {
        if (start == null)
            return null;
        if (start.isGoal())
            return start;

        Stack<NumberLink> stack = new Stack<>();
        Set<NumberLink> visited = new HashSet<>();

        stack.push(start);
        visited.add(start);

        while (!stack.isEmpty()) {
            NumberLink current = stack.pop();

            if (current.isGoal())
                return current;

            for (NumberLink next : current.getNextStates()) {
                // skip if already seen
                if (visited.add(next)) {
                    stack.push(next);
                }
            }
        }
        return null;
    }

    public static NumberLink tkyeem1BFS(NumberLink start) {
        if (start == null)
            return null;
        if (start.isGoal())
            return start;

        Queue<NumberLink> queue = new LinkedList<>();
        Set<NumberLink> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            NumberLink current = queue.poll();

            if (current.isGoal())
                return current;

            for (NumberLink next : current.getNextStates()) {
                // SKIP iza knt shayefha
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return null;
    }

    public static NumberLink ucs(NumberLink start) {
        if (start == null)
            return null;

        Map<NumberLink, Integer> bestG = new HashMap<>();

        PriorityQueue<NumberLink> frontier = new PriorityQueue<>(
                (a, b) -> Integer.compare(a.getG(), b.getG()));

        frontier.add(start);
        bestG.put(start, start.getG());

        while (!frontier.isEmpty()) {
            NumberLink current = frontier.poll();
            if (current.getG() > bestG.get(current))
                continue;
            if (current.isGoal())
                return current;
            for (NumberLink next : current.getNextStates()) {
                Integer known = bestG.get(next);
                if (known == null || next.getG() < known) {
                    bestG.put(next, next.getG());
                    frontier.add(next);
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        int[][] board = {
                { 1, 0, 0, 2, 0 },
                { 0, 3, 0, 4, 0 },
                { 0, 0, 0, 0, 0 },
                { 0, 0, 4, 0, 2 },
                { 3, 1, 5, 0, 5 }
        };

        NumberLink start = new NumberLink(null, board);

        NumberLink solution = ucs(start);

        // print result
        if (solution != null) {
            solution.printPath();
            System.out.println("total cost = " + solution.getG());
        } else {
            System.out.println("no solution");
        }
    }
}
