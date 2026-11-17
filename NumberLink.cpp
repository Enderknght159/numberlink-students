#include <iostream>
#include <vector>
#include <string>
#include <stdexcept>
#include <memory>
#include <algorithm>

using namespace std;

/*
 * Numberlink game state.
 *
 * The board is a 2-D vector:
 *   0  = empty cell
 *   k  = cell used by pair k (the two given numbers + the path drawn between them)
 *
 * We draw the pairs in order: pair 1 first, then pair 2, ...
 * Each move grows the path of the current pair by one cell.
 *
 * States are created with make_shared and linked by shared_ptr,
 * so a state stays alive as long as a child (or a search) points to it.
 */
class NumberLink : public enable_shared_from_this<NumberLink> {
private:
    vector<vector<int>> board;   // the board
    int rows;
    int cols;

    // the two endpoints of each pair: index k = pair k (index 0 not used)
    // shared by all states, because they never change
    shared_ptr<vector<int>> startRow;
    shared_ptr<vector<int>> startCol;
    shared_ptr<vector<int>> endRow;
    shared_ptr<vector<int>> endCol;
    int pairsCount;

    int currentPair;             // the pair we are drawing now (1, 2, ...)
    int headrow;                 // last cell of the current path
    int headcol;

    int g = 0;
    shared_ptr<NumberLink> parent;

    // can the current path enter cell (row, col)?
    // yes if it is empty, or if it is the end of the current pair
    bool canEnter(int row, int col) const {
        if (row == (*endRow)[currentPair] && col == (*endCol)[currentPair]) {
            return true;
        }
        return board[row][col] == 0;
    }

    // put the head of the current path on cell (row, col)
    // if it is the end of the pair, the pair is done and we go to the next pair
    void stepTo(int row, int col) {
        if (row == (*endRow)[currentPair] && col == (*endCol)[currentPair]) {
            currentPair++;
            if (currentPair <= pairsCount) {
                headrow = (*startRow)[currentPair];
                headcol = (*startCol)[currentPair];
            }
        } else {
            board[row][col] = currentPair;
            headrow = row;
            headcol = col;
        }
    }

    // builds the child: copy of this state, then apply the move
    shared_ptr<NumberLink> makeChild(int row, int col) {
        auto child = make_shared<NumberLink>(*this);   // copy constructor
        child->stepTo(row, col);
        child->g = g + 1;
        child->parent = shared_from_this();
        return child;
    }

public:
    // main constructor: builds the start state from a board
    NumberLink(shared_ptr<NumberLink> parent, const vector<vector<int>>& startBoard) {
        rows = startBoard.size();
        cols = startBoard[0].size();
        board = startBoard;          // vector assignment copies the data

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
        startRow = make_shared<vector<int>>(pairsCount + 1);
        startCol = make_shared<vector<int>>(pairsCount + 1);
        endRow = make_shared<vector<int>>(pairsCount + 1);
        endCol = make_shared<vector<int>>(pairsCount + 1);
        vector<int> seen(pairsCount + 1, 0);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int k = board[i][j];
                if (k != 0) {
                    if (seen[k] == 0) {
                        (*startRow)[k] = i;
                        (*startCol)[k] = j;
                    } else {
                        (*endRow)[k] = i;
                        (*endCol)[k] = j;
                    }
                    seen[k]++;
                }
            }
        }
        for (int k = 1; k <= pairsCount; k++) {
            if (seen[k] != 2) {
                throw invalid_argument("number " + to_string(k) + " must appear exactly 2 times");
            }
        }

        // start drawing pair 1 from its start cell
        currentPair = 1;
        headrow = (*startRow)[1];
        headcol = (*startCol)[1];

        this->parent = parent;
    }

    // copy constructor: the default one does exactly what we need
    //  - board (vector) is copied
    //  - the endpoint shared_ptrs are shared, not copied
    NumberLink(const NumberLink& p) = default;

    // is this the goal state: all pairs connected and no empty cell
    bool isGoal() const {
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

    bool canMoveUp() const {
        return currentPair <= pairsCount && headrow != 0 && canEnter(headrow - 1, headcol);
    }

    shared_ptr<NumberLink> moveUp() {
        if (!canMoveUp()) {
            return shared_from_this();
        }
        return makeChild(headrow - 1, headcol);
    }

    bool canMoveDown() const {
        return currentPair <= pairsCount && headrow != rows - 1 && canEnter(headrow + 1, headcol);
    }

    shared_ptr<NumberLink> moveDown() {
        if (!canMoveDown()) {
            return shared_from_this();
        }
        return makeChild(headrow + 1, headcol);
    }

    bool canMoveLeft() const {
        return currentPair <= pairsCount && headcol != 0 && canEnter(headrow, headcol - 1);
    }

    shared_ptr<NumberLink> moveLeft() {
        if (!canMoveLeft()) {
            return shared_from_this();
        }
        return makeChild(headrow, headcol - 1);
    }

    bool canMoveRight() const {
        return currentPair <= pairsCount && headcol != cols - 1 && canEnter(headrow, headcol + 1);
    }

    shared_ptr<NumberLink> moveRight() {
        if (!canMoveRight()) {
            return shared_from_this();
        }
        return makeChild(headrow, headcol + 1);
    }

    vector<shared_ptr<NumberLink>> getNextStates() {
        vector<shared_ptr<NumberLink>> list;
        if (canMoveDown()) {
            list.push_back(moveDown());
        }
        if (canMoveRight()) {
            list.push_back(moveRight());
        }
        if (canMoveUp()) {
            list.push_back(moveUp());
        }
        if (canMoveLeft()) {
            list.push_back(moveLeft());
        }
        return list;
    }

    string toString() const {
        string s;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                s += (board[i][j] == 0) ? "." : to_string(board[i][j]);
                s += "\t";
            }
            s += "\n";
        }
        if (currentPair <= pairsCount) {
            s += "drawing pair " + to_string(currentPair) + ", head at ("
                 + to_string(headrow) + "," + to_string(headcol) + ")\n";
        } else {
            s += "all pairs connected\n";
        }
        return s;
    }

    // used by unordered_set later (same idea as hashCode in Java)
    size_t hashCode() const {
        size_t hash = 0;
        for (const auto& row : board) {
            for (int v : row) {
                hash = hash * 31 + v;
            }
        }
        hash = hash * 31 + currentPair;
        hash = hash * 31 + headrow * cols + headcol;
        return hash;
    }

    // same board + same current pair + same head = same state
    bool equals(const NumberLink& other) const {
        return currentPair == other.currentPair
            && headrow == other.headrow
            && headcol == other.headcol
            && board == other.board;
    }

    int getG() const {
        return g;
    }

    shared_ptr<NumberLink> getParent() const {
        return parent;
    }

    void printPath() {
        vector<shared_ptr<NumberLink>> path;
        shared_ptr<NumberLink> state = shared_from_this();
        path.push_back(state);
        while (state->getParent() != nullptr) {
            path.push_back(state->getParent());
            state = state->getParent();
        }
        reverse(path.begin(), path.end());
        for (const auto& s : path) {
            cout << s->toString() << "\n____________________\n";
        }
        cout << "solved in " << path.size() - 1 << " moves" << endl;
    }
};

int main() {
    vector<vector<int>> board = {
        {1, 0, 0, 2, 0},
        {0, 3, 0, 4, 0},
        {0, 0, 0, 0, 0},
        {0, 0, 4, 0, 2},
        {3, 1, 5, 0, 5}
    };
    auto start = make_shared<NumberLink>(nullptr, board);
    cout << start->toString() << endl;
    cout << "final? " << (start->isGoal() ? "true" : "false") << endl;

    cout << "\npossible next states:" << endl;
    for (const auto& next : start->getNextStates()) {
        cout << next->toString() << endl;
    }

    // draw pair 1 by hand: right, right, down, down, left, down, down
    auto p = start->moveRight()->moveRight()->moveDown()->moveDown()
                  ->moveLeft()->moveDown()->moveDown();
    cout << "after drawing pair 1:\n" << p->toString() << endl;
    return 0;
}
