package battleship;

import java.util.ArrayList;
import java.util.List;

class Ship {
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

    List<Cell> getCells() {
        return cells;
    }

    Board getOwner() {
        return owner;
    }
}
