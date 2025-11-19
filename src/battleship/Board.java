package battleship;

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

class Board implements Serializable {
    static final int SIZE = 10;
    private static final int[] FLEET = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};

    private static final long serialVersionUID = 1L;
    private final Cell[][] cells = new Cell[SIZE][SIZE];
    private final List<Ship> ships = new ArrayList<>();
    private final Random random = new Random();

    Board() {
        this(true);
    }

    Board(boolean autoPlace) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                cells[r][c] = new Cell(r, c);
            }
        }
        if (autoPlace) {
            placeFleet();
        }
    }

    void reset() {
        reset(true);
    }

    void reset(boolean autoPlace) {
        ships.clear();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                cells[r][c] = new Cell(r, c);
            }
        }
        if (autoPlace) {
            placeFleet();
        }
    }

    void clearFleet() {
        ships.clear();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                cells[r][c].setShip(null);
                cells[r][c].setShot(false);
                cells[r][c].setMiss(false);
            }
        }
    }

    Cell[][] getCells() {
        return cells;
    }

    List<Ship> getShips() {
        return ships;
    }

    ShotResult fireAt(int row, int col) {
        if (row < 0 || col < 0 || row >= SIZE || col >= SIZE) {
            throw new IllegalArgumentException("Координати за межами поля");
        }
        Cell cell = cells[row][col];
        if (cell.isShot()) {
            return ShotResult.already(row, col);
        }
        cell.markShot();
        if (cell.hasShip()) {
            Ship ship = cell.getShip();
            ship.registerHit();
            return ShotResult.hit(row, col, ship, ship.isSunk());
        }
        cell.setMiss(true);
        return ShotResult.miss(row, col);
    }

    boolean allShipsSunk() {
        if (ships.isEmpty()) {
            return false;
        }
        return ships.stream().allMatch(Ship::isSunk);
    }

    private void placeFleet() {
        for (int length : FLEET) {
            boolean placed = false;
            while (!placed) {
                boolean horizontal = random.nextBoolean();
                int row = random.nextInt(SIZE);
                int col = random.nextInt(SIZE);
                if (canPlaceShip(length, row, col, horizontal)) {
                    placeShip(length, row, col, horizontal);
                    placed = true;
                }
            }
        }
    }

    boolean canPlaceShip(int length, int row, int col, boolean horizontal) {
        int endRow = horizontal ? row : row + length - 1;
        int endCol = horizontal ? col + length - 1 : col;
        if (endRow >= SIZE || endCol >= SIZE) {
            return false;
        }
        for (int r = row - 1; r <= endRow + 1; r++) {
            for (int c = col - 1; c <= endCol + 1; c++) {
                if (r < 0 || c < 0 || r >= SIZE || c >= SIZE) {
                    continue;
                }
                if (cells[r][c].hasShip()) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean placeShip(int length, int row, int col, boolean horizontal) {
        if (!canPlaceShip(length, row, col, horizontal)) {
            return false;
        }
        Ship ship = new Ship(this);
        for (int i = 0; i < length; i++) {
            int r = horizontal ? row : row + i;
            int c = horizontal ? col + i : col;
            Cell cell = cells[r][c];
            cell.setShip(ship);
            ship.addCell(cell);
        }
        ships.add(ship);
        return true;
    }

    List<Point> availableTargets() {
        List<Point> targets = new ArrayList<>();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (!cells[r][c].isShot()) {
                    targets.add(new Point(r, c));
                }
            }
        }
        Collections.shuffle(targets, random);
        return targets;
    }

    int[] getFleetTemplate() {
        return FLEET.clone();
    }

    void markSurroundingCellsAsMiss(Ship ship) {
        if (!containsShip(ship)) {
            return;
        }
        for (Cell shipCell : ship.getCells()) {
            int baseRow = shipCell.getRow();
            int baseCol = shipCell.getCol();
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) {
                        continue;
                    }
                    int nr = baseRow + dr;
                    int nc = baseCol + dc;
                    if (nr < 0 || nc < 0 || nr >= SIZE || nc >= SIZE) {
                        continue;
                    }
                    Cell neighbor = cells[nr][nc];
                    if (!neighbor.hasShip() && !neighbor.isShot()) {
                        neighbor.markShot();
                        neighbor.setMiss(true);
                    }
                }
            }
        }
    }

    boolean containsShip(Ship ship) {
        return ship != null && ship.getOwner() == this && ships.contains(ship);
    }
}
