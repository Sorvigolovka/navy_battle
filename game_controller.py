from __future__ import annotations

from typing import Callable, List, Optional

from game_model import CellState, ComputerAI, Player


class GameController:
    FLEET_LAYOUT: List[int] = [4, 3, 3, 2, 2, 2, 1, 1, 1, 1]

    def __init__(self):
        self.human = Player("Admiral")
        self.computer = ComputerAI()
        self.on_update: Optional[Callable[[], None]] = None
        self.status_message: str = ""
        self.current_turn: str = "human"
        self.game_over: bool = False
        self.new_game()

    def new_game(self) -> None:
        self.human.board.random_place_fleet(self.FLEET_LAYOUT)
        self.computer.board.random_place_fleet(self.FLEET_LAYOUT)
        self.computer.reset_targets()
        self.current_turn = "human"
        self.game_over = False
        self.status_message = "Ваш хід: оберіть клітинку на полі супротивника."
        self._notify()

    def set_update_callback(self, callback: Callable[[], None]) -> None:
        self.on_update = callback

    def _notify(self) -> None:
        if self.on_update:
            self.on_update()

    def player_fire(self, row: int, col: int) -> None:
        if self.game_over or self.current_turn != "human":
            return

        result, ship = self.computer.board.receive_shot(row, col)
        if result == "repeat":
            self.status_message = "Ви вже стріляли сюди!"
            self._notify()
            return

        self._set_status_after_shot("Ви", result)
        self._check_victory()
        if not self.game_over:
            self.current_turn = "computer"
            self._notify()

    def computer_fire(self) -> None:
        if self.game_over or self.current_turn != "computer":
            return

        row, col = self.computer.select_target()
        result, ship = self.human.board.receive_shot(row, col)
        self._set_status_after_shot("Комп'ютер", result)
        self._check_victory()
        if not self.game_over:
            self.current_turn = "human"
        self._notify()

    def _check_victory(self) -> None:
        if self.computer.board.all_ships_sunk():
            self.status_message = "Перемога! Ви знищили флот супротивника."
            self.game_over = True
        elif self.human.board.all_ships_sunk():
            self.status_message = "Поразка. Усі ваші кораблі потоплено."
            self.game_over = True

    def _set_status_after_shot(self, shooter: str, result: str) -> None:
        if result == "miss":
            self.status_message = f"{shooter}: промах."
        elif result == "hit":
            self.status_message = f"{shooter}: влучання!"
        elif result == "sunk":
            self.status_message = f"{shooter}: корабель потоплено!"
        else:
            self.status_message = f"{shooter}: позиція вже атакована."

    def get_cell_state(self, board_owner: str, row: int, col: int) -> CellState:
        board = self.human.board if board_owner == "human" else self.computer.board
        cell = board.grid[row][col]
        return cell.state

    def reveal_player_ship(self, row: int, col: int) -> bool:
        cell = self.human.board.grid[row][col]
        return cell.state == CellState.SHIP
