package battleship;

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Hunting AI that keeps state between turns and prioritizes finishing ships it has found.
 */
class ComputerAI implements Serializable {
    private final Random random = new Random();
    private final Set<String> tried = new HashSet<>();
    private final Deque<Point> huntQueue = new ArrayDeque<>();
    private final List<Point> currentHits = new ArrayList<>();

    Point chooseTarget(Board playerBoard) {
        pruneQueue();

        Point oriented = selectAlongLine();
        if (oriented != null) {
            return oriented;
        }

        if (!huntQueue.isEmpty()) {
            return huntQueue.pollFirst();
        }

        return chooseRandom(playerBoard);
    }

    void handleShotResult(Point target, ShotResult result) {
        tried.add(key(target.x, target.y));
        if (result.getOutcome() == ShotOutcome.HIT) {
            registerHit(target);
        } else if (result.getOutcome() == ShotOutcome.SUNK && result.getShip() != null) {
            registerHit(target);
            markShipPerimeter(result.getShip());
            currentHits.clear();
            huntQueue.clear();
        }
        pruneQueue();
    }

    void reset() {
        tried.clear();
        huntQueue.clear();
        currentHits.clear();
    }

    private Point chooseRandom(Board board) {
        List<Point> remaining = new ArrayList<>();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (!tried.contains(key(r, c))) {
                    remaining.add(new Point(r, c));
                }
            }
        }
        if (remaining.isEmpty()) {
            return new Point(0, 0);
        }
        Collections.shuffle(remaining, random);
        return remaining.get(0);
    }

    private Point selectAlongLine() {
        Orientation orientation = determineOrientation();
        if (orientation == null) {
            return null;
        }
        List<Point> candidates = new ArrayList<>();
        if (orientation == Orientation.HORIZONTAL) {
            int row = currentHits.get(0).x;
            int minCol = currentHits.stream().mapToInt(p -> p.y).min().orElse(0);
            int maxCol = currentHits.stream().mapToInt(p -> p.y).max().orElse(0);
            candidates.add(new Point(row, minCol - 1));
            candidates.add(new Point(row, maxCol + 1));
        } else {
            int col = currentHits.get(0).y;
            int minRow = currentHits.stream().mapToInt(p -> p.x).min().orElse(0);
            int maxRow = currentHits.stream().mapToInt(p -> p.x).max().orElse(0);
            candidates.add(new Point(minRow - 1, col));
            candidates.add(new Point(maxRow + 1, col));
        }

        Collections.shuffle(candidates, random);
        for (Point candidate : candidates) {
            if (isAvailable(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private void registerHit(Point target) {
        if (currentHits.stream().noneMatch(p -> p.equals(target))) {
            currentHits.add(target);
        }
        addNeighbors(target);
        enforceOrientationQueue();
    }

    private void addNeighbors(Point center) {
        int[][] dirs = { {1, 0}, {-1, 0}, {0, 1}, {0, -1} };
        for (int[] d : dirs) {
            Point n = new Point(center.x + d[0], center.y + d[1]);
            if (isAvailable(n) && !huntQueue.contains(n)) {
                huntQueue.addLast(n);
            }
        }
    }

    private void enforceOrientationQueue() {
        Orientation orientation = determineOrientation();
        if (orientation == null) {
            return;
        }
        huntQueue.removeIf(p -> orientation == Orientation.HORIZONTAL
                ? p.x != currentHits.get(0).x
                : p.y != currentHits.get(0).y);
    }

    private Orientation determineOrientation() {
        if (currentHits.size() < 2) {
            return null;
        }
        Point a = currentHits.get(0);
        Point b = currentHits.get(1);
        if (a.x == b.x) {
            return Orientation.HORIZONTAL;
        }
        if (a.y == b.y) {
            return Orientation.VERTICAL;
        }
        return null;
    }

    private void markShipPerimeter(Ship ship) {
        for (Cell cell : ship.getCells()) {
            for (int r = cell.getRow() - 1; r <= cell.getRow() + 1; r++) {
                for (int c = cell.getCol() - 1; c <= cell.getCol() + 1; c++) {
                    if (r >= 0 && r < Board.SIZE && c >= 0 && c < Board.SIZE) {
                        tried.add(key(r, c));
                    }
                }
            }
        }
    }

    private void pruneQueue() {
        huntQueue.removeIf(p -> !isAvailable(p));
    }

    private boolean isAvailable(Point p) {
        return p.x >= 0 && p.x < Board.SIZE && p.y >= 0 && p.y < Board.SIZE
                && !tried.contains(key(p.x, p.y));
    }

    private String key(int r, int c) {
        return r + "," + c;
    }

    private enum Orientation {
        HORIZONTAL,
        VERTICAL
    }
}
