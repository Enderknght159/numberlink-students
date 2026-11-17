# NumberLink

NumberLink puzzle state and search algorithms, implemented in Python, C++, C# and Java.

## What it does

- A board is a grid where `0` is empty and `k` marks the cells of pair `k`.
- Pairs are drawn in order: each move grows the current pair's path by one cell, and reaching the pair's end jumps the head to the start of the next pair.
- The goal is every pair connected and no empty cell left.
- Java contains the search algorithms: `taqyeem1dfs`, `tkyeem1BFS` and `ucs`.

## Cost rule (UCS)

Every cell the path enters costs `1`, except a cell where the path turns, which costs `2`.
The state therefore stores the direction of the last move (`lastDir`), and the frontier is a
priority queue ordered by `g`. Because costs are not uniform, a state can be reached with
different costs, so `ucs` keeps a `bestG` map instead of a plain visited set.

## Files

| File | Contents |
|---|---|
| `NumberLink.java` | state, DFS, BFS, UCS and a demo `main` |
| `numberlink.py` | state and demo (no search) |
| `NumberLink.cpp` | state and demo (no search) |
| `NumberLink.cs` | state and demo (no search) |
| `report.html` | write-up of the state model and the search |
| `visualizer.html` | BFS/DFS visualizer |
| `ucs-visualizer.html` | step-by-step UCS walkthrough: Java line highlighting, board drawing and heap state |

## Run

Java (the file declares `package numberlink`):

```
mkdir numberlink
cp NumberLink.java numberlink/
javac -d out numberlink/NumberLink.java
java -cp out numberlink.NumberLink
```

Python:

```
python numberlink.py
```

C++:

```
g++ -std=c++17 NumberLink.cpp -o numberlink
./numberlink
```

The HTML files are plain static pages; open them in a browser.

## Notes

- The sample 5x5 board has an optimal cost of 26: 20 entered cells plus 6 turns.
- Every complete solution fills the same 20 cells, so BFS and DFS also return a 20-move
  solution on this board; UCS is the one that minimises the turn-aware cost.
- The visualizer's tie-breaking can differ from Java's `PriorityQueue`, but the final cost
  is the same.
