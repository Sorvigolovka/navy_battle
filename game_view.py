from __future__ import annotations

import tkinter as tk
from tkinter import ttk

from game_controller import GameController
from game_model import CellState


class GameUI:
    CELL_SIZE = 32

    def __init__(self, controller: GameController):
        self.controller = controller
        controller.set_update_callback(self.refresh)

        self.root = tk.Tk()
        self.root.title("Морський бій")
        self.root.resizable(False, False)

        self.status_var = tk.StringVar()

        self._build_layout()
        self.refresh()

    def _build_layout(self) -> None:
        top_bar = ttk.Frame(self.root, padding=10)
        top_bar.pack(fill=tk.X)

        ttk.Label(top_bar, text="Гра Морський бій", font=("Arial", 16, "bold")).pack(
            side=tk.LEFT
        )
        ttk.Button(top_bar, text="Нова гра", command=self._on_new_game).pack(
            side=tk.RIGHT
        )

        boards_frame = ttk.Frame(self.root, padding=10)
        boards_frame.pack()

        self.player_frame = ttk.Labelframe(boards_frame, text="Ваш флот", padding=10)
        self.enemy_frame = ttk.Labelframe(boards_frame, text="Поле супротивника", padding=10)
        self.player_frame.grid(row=0, column=0, padx=5)
        self.enemy_frame.grid(row=0, column=1, padx=5)

        self.player_buttons = self._create_grid(self.player_frame, interactive=False)
        self.enemy_buttons = self._create_grid(self.enemy_frame, interactive=True)

        status_frame = ttk.Frame(self.root, padding=(10, 0, 10, 10))
        status_frame.pack(fill=tk.X)
        ttk.Label(status_frame, textvariable=self.status_var, wraplength=420).pack(
            anchor=tk.W
        )

    def _create_grid(self, parent: ttk.Frame, interactive: bool):
        buttons = []
        for r in range(10):
            row_buttons = []
            for c in range(10):
                btn = tk.Button(
                    parent,
                    width=2,
                    height=1,
                    relief=tk.RAISED,
                    command=(lambda r=r, c=c: self._on_enemy_click(r, c))
                    if interactive
                    else None,
                )
                btn.grid(row=r, column=c, padx=1, pady=1)
                row_buttons.append(btn)
            buttons.append(row_buttons)
        return buttons

    def _on_new_game(self) -> None:
        self.controller.new_game()
        self._enable_enemy_grid(True)

    def _on_enemy_click(self, row: int, col: int) -> None:
        if self.controller.game_over or self.controller.current_turn != "human":
            return
        self.controller.player_fire(row, col)
        self.refresh()
        if not self.controller.game_over:
            self.root.after(800, self._ai_turn)

    def _ai_turn(self) -> None:
        self.controller.computer_fire()
        self.refresh()

    def _enable_enemy_grid(self, enable: bool) -> None:
        state = tk.NORMAL if enable else tk.DISABLED
        for row in self.enemy_buttons:
            for btn in row:
                btn.configure(state=state)

    def refresh(self) -> None:
        self.status_var.set(self.controller.status_message)
        self._paint_board(self.player_buttons, "human", reveal_ships=True)
        self._paint_board(
            self.enemy_buttons, "computer", reveal_ships=self.controller.game_over
        )

        if self.controller.game_over:
            self._enable_enemy_grid(False)
        elif self.controller.current_turn != "human":
            self._enable_enemy_grid(False)
        else:
            self._enable_enemy_grid(True)

    def _paint_board(
        self, buttons: list[list[tk.Button]], board_owner: str, reveal_ships: bool
    ) -> None:
        for r in range(10):
            for c in range(10):
                cell_state = self.controller.get_cell_state(board_owner, r, c)
                color = self._color_for_state(cell_state, reveal_ships, board_owner)
                btn = buttons[r][c]
                btn.configure(bg=color, activebackground=color)

    def _color_for_state(
        self, cell_state: CellState, reveal_ships: bool, board_owner: str
    ) -> str:
        if cell_state == CellState.MISS:
            return "lightgray"
        if cell_state == CellState.HIT:
            return "indianred1"
        if cell_state == CellState.SHIP:
            if board_owner == "human" or reveal_ships:
                return "skyblue"
            return "lightsteelblue"
        return "white"

    def run(self) -> None:
        self.root.mainloop()


def launch() -> None:
    controller = GameController()
    ui = GameUI(controller)
    ui.run()


if __name__ == "__main__":
    launch()
