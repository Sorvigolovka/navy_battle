package battleship;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.Timer;

class BattleshipFrame extends JFrame {
    private enum Screen {
        MENU, GAME
    }

    private static final Color PLAYER_SHIP = new Color(0x90a4ae);
    private static final Color FOG = new Color(0xeceff1);
    private static final Color MISS = new Color(0x90caf9);
    private static final Color HIT = new Color(0xef9a9a);
    private static final Color SUNK = new Color(0xe64a19);

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    private JPanel menuPanel;
    private JPanel gamePanel;

    private GameController controller;
    private GameMode currentMode = GameMode.VS_AI;
    private boolean placementMode = false;
    private Board pendingPlayerOneBoard;
    private Board pendingPlayerTwoBoard;
    private Board currentPlacementBoard;
    private int placementPlayerIndex = 1;
    private Map<Integer, Integer> remainingShips = new LinkedHashMap<>();
    private JComboBox<Integer> shipSizeCombo;
    private JRadioButton horizontalButton;
    private JRadioButton verticalButton;
    private JLabel remainingShipsLabel;
    private JButton placementDoneButton;
    private JPanel placementControls;
    private JButton[][] playerButtons;
    private JButton[][] aiButtons;
    private JLabel statusLabel;
    private JButton newGameButton;
    private JButton backToMenuButton;

    private Language currentLanguage = Language.UKRAINIAN;

    BattleshipFrame() {
        super(Localization.t("window.title", Language.UKRAINIAN));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        menuPanel = createMenuPanel();
        mainPanel.add(menuPanel, Screen.MENU.name());
        add(mainPanel, BorderLayout.CENTER);

        showScreen(Screen.MENU);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JButton newVsAi = new JButton();
        newVsAi.addActionListener(e -> startNewVsAiGame());

        JButton localTwoPlayers = new JButton();
        localTwoPlayers.addActionListener(e -> startLocalTwoPlayersGame());

        JButton loadGame = new JButton();
        loadGame.addActionListener(e -> loadGameFromMenu());

        JButton hostOnline = new JButton();
        hostOnline.addActionListener(e -> createOnlineGame());

        JButton joinOnline = new JButton();
        joinOnline.addActionListener(e -> joinOnlineGame());

        JButton changeLanguage = new JButton();
        changeLanguage.addActionListener(e -> showLanguageDialog());

        JButton resetStats = new JButton();
        resetStats.addActionListener(e -> resetStatisticsFromMenu());

        JButton exitButton = new JButton();
        exitButton.addActionListener(e -> exitGame());

        panel.add(newVsAi);
        panel.add(localTwoPlayers);
        panel.add(loadGame);
        panel.add(hostOnline);
        panel.add(joinOnline);
        panel.add(changeLanguage);
        panel.add(resetStats);
        panel.add(exitButton);

        panel.putClientProperty("buttons", new JButton[] {
                newVsAi, localTwoPlayers, loadGame, hostOnline, joinOnline, changeLanguage, resetStats, exitButton
        });

        applyMenuTexts(panel);
        return panel;
    }

    private JPanel createGamePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        statusLabel = new JLabel(Localization.t("status.yourTurn", currentLanguage), SwingConstants.CENTER);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 16f));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        newGameButton = new JButton(Localization.t("game.newGame", currentLanguage));
        newGameButton.addActionListener(e -> startNewVsAiGame());

        backToMenuButton = new JButton(Localization.t("game.backToMenu", currentLanguage));
        backToMenuButton.addActionListener(e -> returnToMenu());

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.add(statusLabel, BorderLayout.CENTER);
        topPanel.add(newGameButton, BorderLayout.EAST);
        topPanel.add(backToMenuButton, BorderLayout.WEST);

        controller = new GameController(new Board(), new Board());
        playerButtons = new JButton[Board.SIZE][Board.SIZE];
        aiButtons = new JButton[Board.SIZE][Board.SIZE];

        JPanel boards = new JPanel(new GridLayout(1, 2, 10, 10));
        boards.add(createBoardPanel(playerButtons, false));
        boards.add(createBoardPanel(aiButtons, true));

        placementControls = createPlacementControls();

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(boards, BorderLayout.CENTER);
        panel.add(placementControls, BorderLayout.SOUTH);
        panel.putClientProperty("boardsPanel", boards);
        panel.putClientProperty("topPanel", topPanel);

        refreshBoards();
        return panel;
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
                int row = r;
                int col = c;
                if (enemyBoard) {
                    cellButton.addActionListener(e -> handlePlayerShot(row, col));
                } else {
                    cellButton.addActionListener(e -> handlePlacementClick(row, col));
                    cellButton.setEnabled(false);
                }
                buttons[r][c] = cellButton;
                panel.add(cellButton);
            }
        }
        return panel;
    }

    private JPanel createPlacementControls() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        shipSizeCombo = new JComboBox<>();
        horizontalButton = new JRadioButton(currentLanguage == Language.UKRAINIAN ? "Горизонтально" : "Horizontal", true);
        verticalButton = new JRadioButton(currentLanguage == Language.UKRAINIAN ? "Вертикально" : "Vertical", false);
        ButtonGroup orientationGroup = new ButtonGroup();
        orientationGroup.add(horizontalButton);
        orientationGroup.add(verticalButton);

        remainingShipsLabel = new JLabel();
        placementDoneButton = new JButton(currentLanguage == Language.UKRAINIAN ? "Готово" : "Done");
        placementDoneButton.setEnabled(false);
        placementDoneButton.addActionListener(e -> finishPlacementForCurrentPlayer());

        panel.add(new JLabel(currentLanguage == Language.UKRAINIAN ? "Розмір корабля" : "Ship size", SwingConstants.RIGHT));
        panel.add(shipSizeCombo);
        panel.add(horizontalButton);
        panel.add(verticalButton);
        panel.add(remainingShipsLabel);
        panel.add(placementDoneButton);

        panel.setVisible(false);
        return panel;
    }

    private void showScreen(Screen screen) {
        cardLayout.show(mainPanel, screen.name());
        pack();
        setLocationRelativeTo(null);
    }

    private void startNewVsAiGame() {
        currentMode = GameMode.VS_AI;
        if (gamePanel == null) {
            gamePanel = createGamePanel();
            mainPanel.add(gamePanel, Screen.GAME.name());
        }
        boolean manual = askManualPlacement();
        Board aiBoard = new Board();
        if (manual) {
            pendingPlayerTwoBoard = aiBoard;
            beginManualPlacement(new Board(false), GameMode.VS_AI, 1);
        } else {
            controller = new GameController(new Board(), aiBoard, GameMode.VS_AI);
            placementMode = false;
            placementControls.setVisible(false);
            statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
            refreshBoards();
            enableEnemyBoard();
            showScreen(Screen.GAME);
        }
    }

    private void startLocalTwoPlayersGame() {
        currentMode = GameMode.LOCAL_PVP;
        if (gamePanel == null) {
            gamePanel = createGamePanel();
            mainPanel.add(gamePanel, Screen.GAME.name());
        }
        pendingPlayerOneBoard = null;
        pendingPlayerTwoBoard = null;
        promptPlacementForPlayer(1);
    }

    private void loadGameFromMenu() {
        JOptionPane.showMessageDialog(this,
                Localization.t("dialog.loadPlaceholder", currentLanguage));
    }

    private void createOnlineGame() {
        JOptionPane.showMessageDialog(this,
                Localization.t("dialog.onlinePlaceholder", currentLanguage));
    }

    private void joinOnlineGame() {
        JOptionPane.showMessageDialog(this,
                Localization.t("dialog.onlinePlaceholder", currentLanguage));
    }

    private boolean askManualPlacement() {
        Object[] options = {currentLanguage == Language.UKRAINIAN ? "Ручна розстановка" : "Manual placement",
                currentLanguage == Language.UKRAINIAN ? "Автоматична розстановка" : "Automatic placement"};
        int choice = JOptionPane.showOptionDialog(this,
                currentLanguage == Language.UKRAINIAN ? "Як ви хочете розставити кораблі?"
                        : "How do you want to place ships?",
                Localization.t("window.title", currentLanguage),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
        return choice == 0;
    }

    private void beginManualPlacement(Board board, GameMode mode, int playerIndex) {
        placementMode = true;
        currentPlacementBoard = board;
        placementPlayerIndex = playerIndex;
        remainingShips = buildFleetCount(board);
        placementControls.setVisible(true);
        placementDoneButton.setEnabled(false);
        populateShipSizeCombo();
        statusLabel.setText(currentLanguage == Language.UKRAINIAN
                ? "Розміщення кораблів" + (mode == GameMode.LOCAL_PVP ? ": Гравець " + playerIndex : "")
                : "Place your fleet" + (mode == GameMode.LOCAL_PVP ? ": Player " + playerIndex : ""));
        enablePlacementBoard();
        refreshBoards();
        showScreen(Screen.GAME);
    }

    private Map<Integer, Integer> buildFleetCount(Board board) {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (int size : board.getFleetTemplate()) {
            counts.put(size, counts.getOrDefault(size, 0) + 1);
        }
        return counts;
    }

    private void populateShipSizeCombo() {
        shipSizeCombo.removeAllItems();
        for (Map.Entry<Integer, Integer> entry : remainingShips.entrySet()) {
            if (entry.getValue() > 0) {
                shipSizeCombo.addItem(entry.getKey());
            }
        }
    }

    private void promptPlacementForPlayer(int playerIndex) {
        if (playerIndex == 2) {
            JOptionPane.showMessageDialog(this,
                    currentLanguage == Language.UKRAINIAN
                            ? "Передайте керування другому гравцеві і натисніть ОК, коли будете готові"
                            : "Pass control to Player 2 and press OK when ready");
        }
        boolean manual = askManualPlacement();
        if (manual) {
            beginManualPlacement(new Board(false), GameMode.LOCAL_PVP, playerIndex);
        } else {
            Board readyBoard = new Board();
            if (playerIndex == 1) {
                pendingPlayerOneBoard = readyBoard;
                promptPlacementForPlayer(2);
            } else {
                pendingPlayerTwoBoard = readyBoard;
                beginLocalBattle();
            }
        }
    }

    private void showLanguageDialog() {
        Object[] options = {Language.UKRAINIAN.getDisplayName(), Language.ENGLISH.getDisplayName()};
        int choice = JOptionPane.showOptionDialog(this,
                Localization.t("dialog.languageTitle", currentLanguage),
                Localization.t("dialog.languageTitle", currentLanguage),
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);
        if (choice == 0) {
            currentLanguage = Language.UKRAINIAN;
        } else if (choice == 1) {
            currentLanguage = Language.ENGLISH;
        } else {
            return;
        }
        applyLocalization();
    }

    private void resetStatisticsFromMenu() {
        int confirm = JOptionPane.showConfirmDialog(this,
                Localization.t("dialog.resetStatsConfirm", currentLanguage),
                Localization.t("menu.resetStats", currentLanguage),
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this,
                    Localization.t("dialog.resetStatsNotImplemented", currentLanguage));
        }
    }

    private void exitGame() {
        dispose();
        System.exit(0);
    }

    private void handlePlayerShot(int row, int col) {
        if (placementMode) {
            return;
        }
        if (!controller.isPlayerTurn()) {
            statusLabel.setText(Localization.t("status.wait", currentLanguage));
            return;
        }
        if (controller.isGameOver()) {
            statusLabel.setText(Localization.t("status.gameOver", currentLanguage));
            return;
        }
        if (currentMode == GameMode.LOCAL_PVP) {
            ShotResult result = controller.playerFire(row, col);
            if (result.getOutcome() == ShotOutcome.ALREADY) {
                statusLabel.setText(Localization.t("status.already", currentLanguage));
                return;
            }
            refreshBoards();
            if (controller.isGameOver()) {
                statusLabel.setText(Localization.t("status.win", currentLanguage));
                disableEnemyBoard();
            } else {
                statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
                refreshBoards();
                enableEnemyBoard();
            }
            return;
        }
        ShotResult result = controller.playerFire(row, col);
        if (result.getOutcome() == ShotOutcome.ALREADY) {
            statusLabel.setText(Localization.t("status.already", currentLanguage));
            return;
        }
        paintEnemyShot(result);
        if (controller.isGameOver()) {
            statusLabel.setText(Localization.t("status.win", currentLanguage));
            disableEnemyBoard();
            return;
        }
        statusLabel.setText(Localization.t("status.opponentTurn", currentLanguage));
        disableEnemyBoard();
        Timer timer = new Timer(600, e -> {
            ShotResult ai = controller.aiFire();
            paintPlayerShot(ai);
            if (controller.isGameOver()) {
                statusLabel.setText(Localization.t("status.lose", currentLanguage));
            } else {
                statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
                enableEnemyBoard();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void handlePlacementClick(int row, int col) {
        if (!placementMode || currentPlacementBoard == null) {
            return;
        }
        Integer length = (Integer) shipSizeCombo.getSelectedItem();
        if (length == null || remainingShips.getOrDefault(length, 0) <= 0) {
            JOptionPane.showMessageDialog(this,
                    currentLanguage == Language.UKRAINIAN ? "Оберіть наявний розмір корабля" : "Select an available ship size");
            return;
        }
        boolean horizontal = horizontalButton.isSelected();
        boolean placed = currentPlacementBoard.placeShip(length, row, col, horizontal);
        if (!placed) {
            JOptionPane.showMessageDialog(this,
                    currentLanguage == Language.UKRAINIAN ? "Неможливо розмістити корабель тут" : "Cannot place a ship here");
            return;
        }
        remainingShips.put(length, remainingShips.get(length) - 1);
        if (remainingShips.get(length) <= 0) {
            populateShipSizeCombo();
        }
        updateRemainingShipsLabel();
        placementDoneButton.setEnabled(allShipsPlaced());
        refreshBoards();
    }

    private void updateRemainingShipsLabel() {
        if (remainingShips == null || remainingShips.isEmpty()) {
            if (remainingShipsLabel != null) {
                remainingShipsLabel.setText("");
            }
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Integer, Integer> entry : remainingShips.entrySet()) {
            if (entry.getValue() > 0) {
                builder.append(entry.getValue()).append("×").append(entry.getKey()).append("  ");
            }
        }
        remainingShipsLabel.setText((currentLanguage == Language.UKRAINIAN ? "Залишилось: " : "Remaining: ") + builder);
    }

    private boolean allShipsPlaced() {
        return remainingShips.values().stream().allMatch(v -> v == 0);
    }

    private void finishPlacementForCurrentPlayer() {
        if (!allShipsPlaced()) {
            JOptionPane.showMessageDialog(this,
                    currentLanguage == Language.UKRAINIAN ? "Розставте всі кораблі" : "Place all ships first");
            return;
        }
        placementMode = false;
        placementControls.setVisible(false);
        if (currentMode == GameMode.VS_AI) {
            Board aiBoard = pendingPlayerTwoBoard != null ? pendingPlayerTwoBoard : new Board();
            controller = new GameController(currentPlacementBoard, aiBoard, GameMode.VS_AI);
            statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
            refreshBoards();
            enableEnemyBoard();
            showScreen(Screen.GAME);
        } else {
            if (placementPlayerIndex == 1) {
                pendingPlayerOneBoard = currentPlacementBoard;
                promptPlacementForPlayer(2);
            } else {
                pendingPlayerTwoBoard = currentPlacementBoard;
                beginLocalBattle();
            }
        }
    }

    private void beginLocalBattle() {
        if (pendingPlayerOneBoard == null || pendingPlayerTwoBoard == null) {
            return;
        }
        controller = new GameController(pendingPlayerOneBoard, pendingPlayerTwoBoard, GameMode.LOCAL_PVP);
        statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
        placementMode = false;
        placementControls.setVisible(false);
        refreshBoards();
        enableEnemyBoard();
        showScreen(Screen.GAME);
    }

    private void enablePlacementBoard() {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                playerButtons[r][c].setEnabled(true);
            }
        }
        disableEnemyBoard();
        updateRemainingShipsLabel();
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
        if (placementMode && currentPlacementBoard != null) {
            Cell[][] cells = currentPlacementBoard.getCells();
            for (int r = 0; r < Board.SIZE; r++) {
                for (int c = 0; c < Board.SIZE; c++) {
                    JButton playerBtn = playerButtons[r][c];
                    JButton aiBtn = aiButtons[r][c];
                    playerBtn.setText("");
                    aiBtn.setText("");
                    if (cells[r][c].hasShip()) {
                        playerBtn.setBackground(PLAYER_SHIP);
                    } else {
                        playerBtn.setBackground(FOG);
                    }
                    aiBtn.setBackground(FOG);
                    aiBtn.setEnabled(false);
                }
            }
            return;
        }
        if (controller == null) {
            return;
        }
        Board self = currentMode == GameMode.LOCAL_PVP && !controller.isPlayerTurn()
                ? controller.getAiBoard() : controller.getPlayerBoard();
        Board target = currentMode == GameMode.LOCAL_PVP && !controller.isPlayerTurn()
                ? controller.getPlayerBoard() : controller.getAiBoard();
        Cell[][] selfCells = self.getCells();
        Cell[][] targetCells = target.getCells();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                JButton playerBtn = playerButtons[r][c];
                JButton aiBtn = aiButtons[r][c];
                playerBtn.setText("");
                aiBtn.setText("");
                playerBtn.setEnabled(!placementMode && currentMode == GameMode.LOCAL_PVP && !controller.isPlayerTurn());
                if (selfCells[r][c].hasShip()) {
                    if (selfCells[r][c].isShot()) {
                        playerBtn.setBackground(selfCells[r][c].getShip().isSunk() ? SUNK : HIT);
                        playerBtn.setText("✕");
                    } else {
                        playerBtn.setBackground(PLAYER_SHIP);
                    }
                } else {
                    if (selfCells[r][c].isShot()) {
                        playerBtn.setBackground(MISS);
                        playerBtn.setText("•");
                    } else {
                        playerBtn.setBackground(FOG);
                    }
                }
                if (targetCells[r][c].isShot()) {
                    if (targetCells[r][c].hasShip()) {
                        aiBtn.setBackground(targetCells[r][c].getShip().isSunk() ? SUNK : HIT);
                        aiBtn.setText("✕");
                    } else {
                        aiBtn.setBackground(MISS);
                        aiBtn.setText("•");
                    }
                    aiBtn.setEnabled(false);
                } else {
                    aiBtn.setBackground(FOG);
                    aiBtn.setEnabled(true);
                }
            }
        }
    }

    private void disableEnemyBoard() {
        Arrays.stream(aiButtons).flatMap(Arrays::stream).forEach(btn -> btn.setEnabled(false));
    }

    private void enableEnemyBoard() {
        if (placementMode || controller == null) {
            disableEnemyBoard();
            return;
        }
        Board target = currentMode == GameMode.LOCAL_PVP && !controller.isPlayerTurn()
                ? controller.getPlayerBoard() : controller.getAiBoard();
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (!target.getCells()[r][c].isShot()) {
                    aiButtons[r][c].setEnabled(true);
                } else {
                    aiButtons[r][c].setEnabled(false);
                }
            }
        }
    }

    private void returnToMenu() {
        showScreen(Screen.MENU);
    }

    private void applyLocalization() {
        setTitle(Localization.t("window.title", currentLanguage));
        applyMenuTexts(menuPanel);
        if (gamePanel != null) {
            newGameButton.setText(Localization.t("game.newGame", currentLanguage));
            backToMenuButton.setText(Localization.t("game.backToMenu", currentLanguage));
            statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
            if (horizontalButton != null && verticalButton != null) {
                horizontalButton
                        .setText(currentLanguage == Language.UKRAINIAN ? "Горизонтально" : "Horizontal");
                verticalButton.setText(currentLanguage == Language.UKRAINIAN ? "Вертикально" : "Vertical");
            }
            if (placementDoneButton != null) {
                placementDoneButton.setText(currentLanguage == Language.UKRAINIAN ? "Готово" : "Done");
            }
            updateRemainingShipsLabel();
        }
        revalidate();
        repaint();
    }

    private void applyMenuTexts(JPanel menuPanel) {
        JButton[] buttons = (JButton[]) menuPanel.getClientProperty("buttons");
        if (buttons == null || buttons.length < 8) {
            return;
        }
        buttons[0].setText(Localization.t("menu.newVsAi", currentLanguage));
        buttons[1].setText(Localization.t("menu.localTwoPlayers", currentLanguage));
        buttons[2].setText(Localization.t("menu.load", currentLanguage));
        buttons[3].setText(Localization.t("menu.host", currentLanguage));
        buttons[4].setText(Localization.t("menu.join", currentLanguage));
        buttons[5].setText(Localization.t("menu.language", currentLanguage));
        buttons[6].setText(Localization.t("menu.resetStats", currentLanguage));
        buttons[7].setText(Localization.t("menu.exit", currentLanguage));
    }
}
