using System;
using System.Collections.Generic;

/*
 * Numberlink game state.
 *
 * The board is an int[,]:
 *   0  = empty cell
 *   k  = cell used by pair k (the two given numbers + the path drawn between them)
 *
 * We draw the pairs in order: pair 1 first, then pair 2, ...
 * Each move grows the path of the current pair by one cell.
 */
public class NumberLink
{
    private int[,] board;        // the board
    private int rows;
    private int cols;

    // the two endpoints of each pair: index k = pair k (index 0 not used)
    private int[] startRow;
    private int[] startCol;
    private int[] endRow;
    private int[] endCol;
    private int pairsCount;

    private int currentPair;     // the pair we are drawing now (1, 2, ...)
    private int headrow;         // last cell of the current path
    private int headcol;

    private int g = 0;
    private NumberLink parent;

    /// main constructor: builds the start state from a board
    /// parent = null for the start state
    public NumberLink(NumberLink parent, int[,] startBoard)
    {
        rows = startBoard.GetLength(0);
        cols = startBoard.GetLength(1);
        board = (int[,])startBoard.Clone();   // copy the data

        // find the biggest number = number of pairs
        pairsCount = 0;
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                if (board[i, j] > pairsCount)
                {
                    pairsCount = board[i, j];
                }
            }
        }

        // first time we see a number = start, second time = end
        startRow = new int[pairsCount + 1];
        startCol = new int[pairsCount + 1];
        endRow = new int[pairsCount + 1];
        endCol = new int[pairsCount + 1];
        int[] seen = new int[pairsCount + 1];
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                int k = board[i, j];
                if (k != 0)
                {
                    if (seen[k] == 0)
                    {
                        startRow[k] = i;
                        startCol[k] = j;
                    }
                    else
                    {
                        endRow[k] = i;
                        endCol[k] = j;
                    }
                    seen[k]++;
                }
            }
        }
        for (int k = 1; k <= pairsCount; k++)
        {
            if (seen[k] != 2)
            {
                throw new ArgumentException("number " + k + " must appear exactly 2 times");
            }
        }

        // start drawing pair 1 from its start cell
        currentPair = 1;
        headrow = startRow[1];
        headcol = startCol[1];

        this.parent = parent;
    }

    /// copy constructor
    public NumberLink(NumberLink p)
    {
        rows = p.rows;
        cols = p.cols;
        board = (int[,])p.board.Clone();
        // endpoints never change, so all states can share the same arrays
        startRow = p.startRow;
        startCol = p.startCol;
        endRow = p.endRow;
        endCol = p.endCol;
        pairsCount = p.pairsCount;

        currentPair = p.currentPair;
        headrow = p.headrow;
        headcol = p.headcol;
        g = p.g;
        parent = p.parent;
    }

    /// is this the goal state: all pairs connected and no empty cell
    public bool IsGoal()
    {
        if (currentPair <= pairsCount)
        {
            return false;
        }
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                if (board[i, j] == 0)
                {
                    return false;
                }
            }
        }
        return true;
    }

    /// yes if the cell is empty, or if it is the end of the current pair
    private bool CanEnter(int row, int col)
    {
        if (row == endRow[currentPair] && col == endCol[currentPair])
        {
            return true;
        }
        return board[row, col] == 0;
    }

    /// put the head on (row, col); if it is the end of the pair, go to the next pair
    private void StepTo(int row, int col)
    {
        if (row == endRow[currentPair] && col == endCol[currentPair])
        {
            currentPair++;
            if (currentPair <= pairsCount)
            {
                headrow = startRow[currentPair];
                headcol = startCol[currentPair];
            }
        }
        else
        {
            board[row, col] = currentPair;
            headrow = row;
            headcol = col;
        }
    }

    public bool CanMoveUp()
    {
        return currentPair <= pairsCount && headrow != 0 && CanEnter(headrow - 1, headcol);
    }

    public NumberLink MoveUp()
    {
        if (!CanMoveUp())
        {
            return this;
        }
        NumberLink child = new NumberLink(this);
        child.StepTo(headrow - 1, headcol);
        child.g = g + 1;
        child.parent = this;
        return child;
    }

    public bool CanMoveDown()
    {
        return currentPair <= pairsCount && headrow != rows - 1 && CanEnter(headrow + 1, headcol);
    }

    public NumberLink MoveDown()
    {
        if (!CanMoveDown())
        {
            return this;
        }
        NumberLink child = new NumberLink(this);
        child.StepTo(headrow + 1, headcol);
        child.g = g + 1;
        child.parent = this;
        return child;
    }

    public bool CanMoveLeft()
    {
        return currentPair <= pairsCount && headcol != 0 && CanEnter(headrow, headcol - 1);
    }

    public NumberLink MoveLeft()
    {
        if (!CanMoveLeft())
        {
            return this;
        }
        NumberLink child = new NumberLink(this);
        child.StepTo(headrow, headcol - 1);
        child.g = g + 1;
        child.parent = this;
        return child;
    }

    public bool CanMoveRight()
    {
        return currentPair <= pairsCount && headcol != cols - 1 && CanEnter(headrow, headcol + 1);
    }

    public NumberLink MoveRight()
    {
        if (!CanMoveRight())
        {
            return this;
        }
        NumberLink child = new NumberLink(this);
        child.StepTo(headrow, headcol + 1);
        child.g = g + 1;
        child.parent = this;
        return child;
    }

    public List<NumberLink> GetNextStates()
    {
        List<NumberLink> list = new List<NumberLink>();
        if (CanMoveDown())
        {
            list.Add(MoveDown());
        }
        if (CanMoveRight())
        {
            list.Add(MoveRight());
        }
        if (CanMoveUp())
        {
            list.Add(MoveUp());
        }
        if (CanMoveLeft())
        {
            list.Add(MoveLeft());
        }
        return list;
    }

    public override string ToString()
    {
        string s = "";
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                s += (board[i, j] == 0 ? "." : board[i, j].ToString()) + "\t";
            }
            s += "\n";
        }
        if (currentPair <= pairsCount)
        {
            s += "drawing pair " + currentPair + ", head at (" + headrow + "," + headcol + ")\n";
        }
        else
        {
            s += "all pairs connected\n";
        }
        return s;
    }

    /// used by HashSet later (same idea as hashCode in Java)
    public override int GetHashCode()
    {
        int hash = 17;
        foreach (int v in board)
        {
            hash = hash * 31 + v;
        }
        hash = hash * 31 + currentPair;
        hash = hash * 31 + headrow * cols + headcol;
        return hash;
    }

    /// same board + same current pair + same head = same state
    public override bool Equals(object obj)
    {
        NumberLink other = obj as NumberLink;
        if (other == null)
        {
            return false;
        }
        if (currentPair != other.currentPair || headrow != other.headrow || headcol != other.headcol)
        {
            return false;
        }
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                if (board[i, j] != other.board[i, j])
                {
                    return false;
                }
            }
        }
        return true;
    }

    public int GetG()
    {
        return g;
    }

    public NumberLink GetParent()
    {
        return parent;
    }

    public void PrintPath()
    {
        List<NumberLink> path = new List<NumberLink>();
        NumberLink state = this;
        path.Add(state);
        while (state.GetParent() != null)
        {
            path.Add(state.GetParent());
            state = state.GetParent();
        }
        path.Reverse();
        foreach (NumberLink s in path)
        {
            Console.WriteLine(s + "\n____________________");
        }
        Console.WriteLine("solved in " + (path.Count - 1) + " moves");
    }

    public static void Main(string[] args)
    {
        int[,] board = {
            {1, 0, 0, 2, 0},
            {0, 3, 0, 4, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 4, 0, 2},
            {3, 1, 5, 0, 5}
        };
        NumberLink start = new NumberLink(null, board);
        Console.WriteLine(start);
        Console.WriteLine("final? " + start.IsGoal());

        Console.WriteLine("\npossible next states:");
        foreach (NumberLink next in start.GetNextStates())
        {
            Console.WriteLine(next);
        }

        // draw pair 1 by hand: right, right, down, down, left, down, down
        NumberLink p = start.MoveRight().MoveRight().MoveDown().MoveDown()
                .MoveLeft().MoveDown().MoveDown();
        Console.WriteLine("after drawing pair 1:\n" + p);
    }
}
