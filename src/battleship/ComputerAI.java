package battleship;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

class ComputerAI implements Serializable {
    private final Random random = new Random();
    private final Set<String> tried = new HashSet<>();
    private final Deque<int[]> huntQueue = new ArrayDeque<>();
    private final List<int[]> currentHits = new ArrayList<>();

    ShotResult fire(Board playerBoard) {
        int[] target = selectTarget(playerBoard);
        tried.add(key(target[0], target[1]));
        ShotResult result = playerBoard.fireAt(target[0], target[1]);
        if (result.getOutcome() == ShotOutcome.HIT) {
            currentHits.add(new int[] { target[0], target[1] });
            enqueueNeighbors(target[0], target[1]);
        } else if (result.getOutcome() == ShotOutcome.SUNK) {
            currentHits.add(new int[] { target[0], target[1] });
            markAdjacentAsTried(playerBoard, result.getShip());
            currentHits.clear();
            huntQueue.clear();
        }
        pruneQueue(playerBoard);
        if (result.getOutcome() == ShotOutcome.MISS) {
            return result;
        }
        return result;
    }

    private int[] selectTarget(Board board) {
        pruneQueue(board);
        if (!huntQueue.isEmpty()) {
            return huntQueue.pollFirst();
        }
        List<int[]> remaining = new ArrayList<>();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (!tried.contains(key(r, c))) {
                    remaining.add(new int[] { r, c });
                }
            }
        }
        Collections.shuffle(remaining, random);
        return remaining.isEmpty() ? new int[] { 0, 0 } : remaining.get(0);
    }

    private void enqueueNeighbors(int row, int col) {
        int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int[] d : dirs) {
            int nr = row + d[0];
            int nc = col + d[1];
            if (inBounds(nr, nc) && !tried.contains(key(nr, nc))) {
                huntQueue.addLast(new int[] { nr, nc });
            }
        }
        if (currentHits.size() >= 2) {
            int[] a = currentHits.get(0);
            int[] b = currentHits.get(1);
            boolean horizontal = a[0] == b[0];
            huntQueue.removeIf(p -> horizontal ? p[0] != a[0] : p[1] != a[1]);
            List<int[]> oriented = new ArrayList<>();
            for (int[] hit : currentHits) {
                oriented.add(new int[] { hit[0] + (horizontal ? 0 : 1), hit[1] + (horizontal ? 1 : 0) });
                oriented.add(new int[] { hit[0] - (horizontal ? 0 : 1), hit[1] - (horizontal ? 1 : 0) });
            }
            for (int[] p : oriented) {
                if (inBounds(p[0], p[1]) && !tried.contains(key(p[0], p[1]))) {
                    huntQueue.addFirst(p);
                }
            }
        }
    }

    private void pruneQueue(Board board) {
        huntQueue.removeIf(p -> tried.contains(key(p[0], p[1])) || !inBounds(p[0], p[1]));
    }

    private void markAdjacentAsTried(Board board, Ship ship) {
        for (Cell cell : ship.getCells()) {
            for (int r = cell.getRow() - 1; r <= cell.getRow() + 1; r++) {
                for (int c = cell.getCol() - 1; c <= cell.getCol() + 1; c++) {
                    if (inBounds(r, c)) {
                        tried.add(key(r, c));
                    }
                }
            }
        }
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < Board.SIZE && c >= 0 && c < Board.SIZE;
    }

    private String key(int r, int c) {
        return r + "," + c;
    }
}
