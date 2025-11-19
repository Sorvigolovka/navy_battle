package battleship;

import java.util.ArrayList;
import java.util.List;

class Ship {
    private final List<Cell> cells = new ArrayList<>();
    private int hits;

    void addCell(Cell cell) {
        cells.add(cell);
    }

    int size() {
        return cells.size();
    }

    boolean isSunk() {
        return hits >= cells.size();
    }

    void registerHit() {
        hits++;
    }

    List<Cell> getCells() {
        return cells;
    }
}
