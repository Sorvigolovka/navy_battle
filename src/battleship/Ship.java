package battleship;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

class Ship implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Board owner;
    private final List<Cell> cells = new ArrayList<>();
    private int hits;

    Ship(Board owner) {
        this.owner = owner;
    }

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

    void forceSunk() {
        hits = cells.size();
    }

    List<Cell> getCells() {
        return cells;
    }

    Board getOwner() {
        return owner;
    }
}
