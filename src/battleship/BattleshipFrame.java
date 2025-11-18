package battleship;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

class BattleshipFrame extends JFrame {
    private static final Color PLAYER_SHIP = new Color(0x90a4ae);
    private static final Color FOG = new Color(0xeceff1);
    private static final Color MISS = new Color(0x90caf9);
    private static final Color HIT = new Color(0xef9a9a);
    private static final Color SUNK = new Color(0xe64a19);

    private final GameController controller;
    private final JButton[][] playerButtons = new JButton[Board.SIZE][Board.SIZE];
    private final JButton[][] aiButtons = new JButton[Board.SIZE][Board.SIZE];
    private final JLabel statusLabel = new JLabel("Ваш хід", SwingConstants.CENTER);
    private final JButton newGameButton = new JButton("Нова гра");

    BattleshipFrame() {
        super("Морський бій (Java)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        Board playerBoard = new Board();
        Board aiBoard = new Board();
        controller = new GameController(playerBoard, aiBoard);

        JPanel boards = new JPanel(new GridLayout(1, 2, 10, 10));
        boards.add(createBoardPanel(playerButtons, false));
        boards.add(createBoardPanel(aiButtons, true));

        JPanel topPanel = new JPanel(new BorderLayout());
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 16f));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        topPanel.add(statusLabel, BorderLayout.CENTER);
        newGameButton.addActionListener(e -> resetGame());
        topPanel.add(newGameButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(boards, BorderLayout.CENTER);

        refreshBoards();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createBoardPanel(JButton[][] buttons, boolean enemyBoard) {
        JPanel panel = new JPanel(new GridLayout(Board.SIZE, Board.SIZE));
        panel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                JButton cellButton = new JButton();
                cellButton.setPreferredSize(new Dimension(32, 32));
                cellButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                cellButton.setFocusPainted(false);
                cellButton.setOpaque(true);
                cellButton.setBackground(FOG);
                cellButton.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                if (enemyBoard) {
                    int row = r;
                    int col = c;
                    cellButton.addActionListener(e -> handlePlayerShot(row, col));
                } else {
                    cellButton.setEnabled(false);
                }
                buttons[r][c] = cellButton;
                panel.add(cellButton);
            }
        }
        return panel;
    }

    private void handlePlayerShot(int row, int col) {
        if (!controller.isPlayerTurn()) {
            statusLabel.setText("Зачекайте на свій хід");
            return;
        }
        if (controller.isGameOver()) {
            statusLabel.setText("Гра завершена — натисніть 'Нова гра'");
            return;
        }
        ShotResult result = controller.playerFire(row, col);
        if (result.getOutcome() == ShotOutcome.ALREADY) {
            statusLabel.setText("Ви вже стріляли сюди");
            return;
        }
        paintEnemyShot(result);
        if (controller.isGameOver()) {
            statusLabel.setText("Ви перемогли! Натисніть 'Нова гра'");
            disableEnemyBoard();
            return;
        }
        statusLabel.setText("Хід суперника...");
        disableEnemyBoard();
        Timer timer = new Timer(600, e -> {
            ShotResult ai = controller.aiFire();
            paintPlayerShot(ai);
            if (controller.isGameOver()) {
                statusLabel.setText("Поразка. Спробуйте ще раз");
            } else {
                statusLabel.setText("Ваш хід");
                enableEnemyBoard();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void paintEnemyShot(ShotResult result) {
        JButton target = aiButtons[result.getRow()][result.getCol()];
        target.setEnabled(false);
        switch (result.getOutcome()) {
            case MISS:
                target.setBackground(MISS);
                target.setText("•");
                break;
            case HIT:
                target.setBackground(HIT);
                target.setText("✕");
                break;
            case SUNK:
                target.setBackground(SUNK);
                target.setText("✕");
                highlightSunkShip(result.getShip(), false);
                break;
            default:
                break;
        }
    }

    private void paintPlayerShot(ShotResult result) {
        if (result == null) {
            return;
        }
        JButton target = playerButtons[result.getRow()][result.getCol()];
        switch (result.getOutcome()) {
            case MISS:
                target.setBackground(MISS);
                target.setText("•");
                break;
            case HIT:
                target.setBackground(HIT);
                target.setText("✕");
                break;
            case SUNK:
                target.setBackground(SUNK);
                target.setText("✕");
                highlightSunkShip(result.getShip(), true);
                break;
            default:
                break;
        }
    }

    private void highlightSunkShip(Ship ship, boolean onPlayerBoard) {
        if (ship == null) {
            return;
        }
        for (Cell cell : ship.getCells()) {
            JButton button = (onPlayerBoard ? playerButtons : aiButtons)[cell.getRow()][cell.getCol()];
            button.setBackground(SUNK);
            button.setText("✕");
        }
    }

    private void refreshBoards() {
        Cell[][] playerCells = controller.getPlayerBoard().getCells();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                JButton playerBtn = playerButtons[r][c];
                JButton aiBtn = aiButtons[r][c];
                playerBtn.setText("");
                aiBtn.setText("");
                aiBtn.setEnabled(true);
                if (playerCells[r][c].hasShip()) {
                    playerBtn.setBackground(PLAYER_SHIP);
                } else {
                    playerBtn.setBackground(FOG);
                }
                aiBtn.setBackground(FOG);
            }
        }
    }

    private void resetGame() {
        controller.resetGame();
        refreshBoards();
        enableEnemyBoard();
        statusLabel.setText("Ваш хід");
    }

    private void disableEnemyBoard() {
        Arrays.stream(aiButtons).flatMap(Arrays::stream).forEach(btn -> btn.setEnabled(false));
    }

    private void enableEnemyBoard() {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (!controller.getAiBoard().getCells()[r][c].isShot()) {
                    aiButtons[r][c].setEnabled(true);
                }
            }
        }
    }
}
