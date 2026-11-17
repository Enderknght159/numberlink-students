"""
Numberlink game state.

The board is a list of lists:
  0  = empty cell
  k  = cell used by pair k (the two given numbers + the path drawn between them)

We draw the pairs in order: pair 1 first, then pair 2, ...
Each move grows the path of the current pair by one cell.
"""


class NumberLink:

    def __init__(self, parent, start_board):
        """main constructor: builds the start state from a board

        parent      -- None for the start state
        start_board -- the puzzle: 0 = empty, k = endpoint of pair k
        """
        self.rows = len(start_board)
        self.cols = len(start_board[0])
        self.board = [row[:] for row in start_board]   # copy every row

        # find the biggest number = number of pairs
        self.pairs_count = 0
        for i in range(self.rows):
            for j in range(self.cols):
                if self.board[i][j] > self.pairs_count:
                    self.pairs_count = self.board[i][j]

        # first time we see a number = start, second time = end
        # index k = pair k (index 0 not used); shared by all states
        self.start_row = [0] * (self.pairs_count + 1)
        self.start_col = [0] * (self.pairs_count + 1)
        self.end_row = [0] * (self.pairs_count + 1)
        self.end_col = [0] * (self.pairs_count + 1)
        seen = [0] * (self.pairs_count + 1)
        for i in range(self.rows):
            for j in range(self.cols):
                k = self.board[i][j]
                if k != 0:
                    if seen[k] == 0:
                        self.start_row[k] = i
                        self.start_col[k] = j
                    else:
                        self.end_row[k] = i
                        self.end_col[k] = j
                    seen[k] += 1
        for k in range(1, self.pairs_count + 1):
            if seen[k] != 2:
                raise ValueError("number " + str(k) + " must appear exactly 2 times")

        # start drawing pair 1 from its start cell
        self.current_pair = 1
        self.headrow = self.start_row[1]
        self.headcol = self.start_col[1]

        self.g = 0
        self.parent = parent

    def copy(self):
        """copy constructor: a new state with its own board"""
        p = NumberLink.__new__(NumberLink)        # empty object, skips __init__
        p.rows = self.rows
        p.cols = self.cols
        p.board = [row[:] for row in self.board]  # own copy of the board
        # endpoints never change, so all states share the same lists
        p.start_row = self.start_row
        p.start_col = self.start_col
        p.end_row = self.end_row
        p.end_col = self.end_col
        p.pairs_count = self.pairs_count

        p.current_pair = self.current_pair
        p.headrow = self.headrow
        p.headcol = self.headcol
        p.g = self.g
        p.parent = self.parent
        return p

    def is_goal(self):
        """is this the goal state: all pairs connected and no empty cell"""
        if self.current_pair <= self.pairs_count:
            return False
        for i in range(self.rows):
            for j in range(self.cols):
                if self.board[i][j] == 0:
                    return False
        return True

    def _can_enter(self, row, col):
        """yes if the cell is empty, or if it is the end of the current pair"""
        if row == self.end_row[self.current_pair] and col == self.end_col[self.current_pair]:
            return True
        return self.board[row][col] == 0

    def _step_to(self, row, col):
        """put the head on (row, col); if it is the end, go to the next pair"""
        if row == self.end_row[self.current_pair] and col == self.end_col[self.current_pair]:
            self.current_pair += 1
            if self.current_pair <= self.pairs_count:
                self.headrow = self.start_row[self.current_pair]
                self.headcol = self.start_col[self.current_pair]
        else:
            self.board[row][col] = self.current_pair
            self.headrow = row
            self.headcol = col

    def can_move_up(self):
        return (self.current_pair <= self.pairs_count and self.headrow != 0
                and self._can_enter(self.headrow - 1, self.headcol))

    def move_up(self):
        if not self.can_move_up():
            return self
        child = self.copy()
        child._step_to(self.headrow - 1, self.headcol)
        child.g = self.g + 1
        child.parent = self
        return child

    def can_move_down(self):
        return (self.current_pair <= self.pairs_count and self.headrow != self.rows - 1
                and self._can_enter(self.headrow + 1, self.headcol))

    def move_down(self):
        if not self.can_move_down():
            return self
        child = self.copy()
        child._step_to(self.headrow + 1, self.headcol)
        child.g = self.g + 1
        child.parent = self
        return child

    def can_move_left(self):
        return (self.current_pair <= self.pairs_count and self.headcol != 0
                and self._can_enter(self.headrow, self.headcol - 1))

    def move_left(self):
        if not self.can_move_left():
            return self
        child = self.copy()
        child._step_to(self.headrow, self.headcol - 1)
        child.g = self.g + 1
        child.parent = self
        return child

    def can_move_right(self):
        return (self.current_pair <= self.pairs_count and self.headcol != self.cols - 1
                and self._can_enter(self.headrow, self.headcol + 1))

    def move_right(self):
        if not self.can_move_right():
            return self
        child = self.copy()
        child._step_to(self.headrow, self.headcol + 1)
        child.g = self.g + 1
        child.parent = self
        return child

    def get_next_states(self):
        result = []
        if self.can_move_down():
            result.append(self.move_down())
        if self.can_move_right():
            result.append(self.move_right())
        if self.can_move_up():
            result.append(self.move_up())
        if self.can_move_left():
            result.append(self.move_left())
        return result

    def __str__(self):
        s = ""
        for i in range(self.rows):
            for j in range(self.cols):
                s += ("." if self.board[i][j] == 0 else str(self.board[i][j])) + "\t"
            s += "\n"
        if self.current_pair <= self.pairs_count:
            s += ("drawing pair " + str(self.current_pair) + ", head at ("
                  + str(self.headrow) + "," + str(self.headcol) + ")\n")
        else:
            s += "all pairs connected\n"
        return s

    def __hash__(self):
        """same idea as hashCode in Java: used by set() and dict()"""
        return hash((tuple(tuple(row) for row in self.board),
                     self.current_pair, self.headrow, self.headcol))

    def __eq__(self, other):
        """same board + same current pair + same head = same state"""
        if not isinstance(other, NumberLink):
            return False
        return (self.current_pair == other.current_pair
                and self.headrow == other.headrow
                and self.headcol == other.headcol
                and self.board == other.board)

    def get_g(self):
        return self.g

    def get_parent(self):
        return self.parent

    def print_path(self):
        path = []
        state = self
        path.append(state)
        while state.get_parent() is not None:
            path.append(state.get_parent())
            state = state.get_parent()
        path.reverse()
        for s in path:
            print(str(s) + "\n____________________")
        print("solved in " + str(len(path) - 1) + " moves")


if __name__ == "__main__":
    board = [
        [1, 0, 0, 2, 0],
        [0, 3, 0, 4, 0],
        [0, 0, 0, 0, 0],
        [0, 0, 4, 0, 2],
        [3, 1, 5, 0, 5],
    ]
    start = NumberLink(None, board)
    print(start)
    print("final?", start.is_goal())

    print("\npossible next states:")
    for nxt in start.get_next_states():
        print(nxt)

    # draw pair 1 by hand: right, right, down, down, left, down, down
    p = (start.move_right().move_right().move_down().move_down()
         .move_left().move_down().move_down())
    print("after drawing pair 1:\n" + str(p))
