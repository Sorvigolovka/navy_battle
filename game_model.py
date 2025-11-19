from __future__ import annotations

import random
from dataclasses import dataclass, field
from enum import Enum
from typing import List, Optional, Set, Tuple


class CellState(Enum):
    EMPTY = 0
    SHIP = 1
    MISS = 2
    HIT = 3


@dataclass
class Cell:
    row: int
    col: int
    state: CellState = CellState.EMPTY
    ship_id: Optional[int] = None


@dataclass
class Ship:
    length: int
    positions: List[Tuple[int, int]]
    hits: Set[Tuple[int, int]] = field(default_factory=set)

    def register_hit(self, row: int, col: int) -> bool:
        self.hits.add((row, col))
        return self.is_sunk

    @property
    def is_sunk(self) -> bool:
        return len(self.hits) == self.length


class Board:
    def __init__(self, size: int = 10):
        self.size = size
        self.grid: List[List[Cell]] = [
            [Cell(r, c) for c in range(size)] for r in range(size)
        ]
        self.ships: List[Ship] = []

    def reset(self) -> None:
        for row in self.grid:
            for cell in row:
                cell.state = CellState.EMPTY
                cell.ship_id = None
        self.ships.clear()

    def random_place_fleet(self, fleet_layout: List[int]) -> None:
        self.reset()
        ship_id = 0
        for length in fleet_layout:
            placed = False
            while not placed:
                placed = self._place_ship_randomly(length, ship_id)
                ship_id += 1

    def _place_ship_randomly(self, length: int, ship_id: int) -> bool:
        orientation = random.choice(["horizontal", "vertical"])
        if orientation == "horizontal":
            row = random.randrange(self.size)
            col = random.randrange(self.size - length + 1)
            positions = [(row, col + i) for i in range(length)]
        else:
            row = random.randrange(self.size - length + 1)
            col = random.randrange(self.size)
            positions = [(row + i, col) for i in range(length)]

        if any(self.grid[r][c].state != CellState.EMPTY for r, c in positions):
            return False

        # Check surrounding cells to avoid touching ships
        for r, c in positions:
            for dr in (-1, 0, 1):
                for dc in (-1, 0, 1):
                    nr, nc = r + dr, c + dc
                    if 0 <= nr < self.size and 0 <= nc < self.size:
                        if self.grid[nr][nc].state == CellState.SHIP:
                            return False

        ship = Ship(length, positions)
        self.ships.append(ship)
        for r, c in positions:
            cell = self.grid[r][c]
            cell.state = CellState.SHIP
            cell.ship_id = ship_id
        return True

    def receive_shot(self, row: int, col: int) -> Tuple[str, Optional[Ship]]:
        cell = self.grid[row][col]
        if cell.state == CellState.MISS or cell.state == CellState.HIT:
            return "repeat", None
        if cell.state == CellState.SHIP:
            cell.state = CellState.HIT
            assert cell.ship_id is not None
            ship = self.ships[cell.ship_id]
            sunk = ship.register_hit(row, col)
            return ("sunk" if sunk else "hit"), ship
        cell.state = CellState.MISS
        return "miss", None

    def all_ships_sunk(self) -> bool:
        return all(ship.is_sunk for ship in self.ships)


class Player:
    def __init__(self, name: str):
        self.name = name
        self.board = Board()


class ComputerAI(Player):
    def __init__(self):
        super().__init__("Computer")
        self.available_targets: Set[Tuple[int, int]] = set(
            (r, c) for r in range(self.board.size) for c in range(self.board.size)
        )

    def reset_targets(self) -> None:
        self.available_targets = set(
            (r, c) for r in range(self.board.size) for c in range(self.board.size)
        )

    def select_target(self) -> Tuple[int, int]:
        choice = random.choice(tuple(self.available_targets))
        self.available_targets.remove(choice)
        return choice
