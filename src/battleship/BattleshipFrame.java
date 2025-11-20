package battleship;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.KeyStroke;

class BattleshipFrame extends JFrame implements OnlineMatch.Listener {
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

    private final StatisticsManager statisticsManager = StatisticsManager.load();
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
    private JButton saveGameButton;
    private JLabel statsLabel;
    private Timer turnDelayTimer;
    private OnlineMatch onlineMatch;
    private JCheckBox fullscreenToggle;
    private boolean fullscreen;
    private Rectangle windowedBounds;

    private Language currentLanguage = Language.UKRAINIAN;

    BattleshipFrame() {
        super(Localization.t("window.title", Language.UKRAINIAN));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        menuPanel = createMenuPanel();
        mainPanel.add(menuPanel, Screen.MENU.name());
        add(mainPanel, BorderLayout.CENTER);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("F11"), "toggleFullscreen");
        getRootPane().getActionMap().put("toggleFullscreen", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleFullscreen();
            }
        });

        showScreen(Screen.MENU);
        pack();
        setLocationRelativeTo(null);
        windowedBounds = getBounds();
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

        fullscreenToggle = new JCheckBox();
        fullscreenToggle.addActionListener(e -> setFullscreen(fullscreenToggle.isSelected()));
        panel.add(fullscreenToggle);

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

        saveGameButton = new JButton(currentLanguage == Language.UKRAINIAN ? "Зберегти гру" : "Save game");
        saveGameButton.addActionListener(e -> saveCurrentGame());

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.add(statusLabel, BorderLayout.CENTER);
        JPanel rightButtons = new JPanel(new GridLayout(1, 2, 6, 0));
        rightButtons.add(saveGameButton);
        rightButtons.add(newGameButton);
        topPanel.add(rightButtons, BorderLayout.EAST);
        topPanel.add(backToMenuButton, BorderLayout.WEST);

        controller = new GameController(new Board(), new Board(), GameMode.VS_AI, statisticsManager);
        playerButtons = new JButton[Board.SIZE][Board.SIZE];
        aiButtons = new JButton[Board.SIZE][Board.SIZE];

        JPanel boards = new JPanel(new GridLayout(1, 2, 10, 10));
        boards.add(createBoardPanel(playerButtons, false));
        boards.add(createBoardPanel(aiButtons, true));

        placementControls = createPlacementControls();

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(boards, BorderLayout.CENTER);
        statsLabel = new JLabel();
        statsLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        statsLabel.setFont(statsLabel.getFont().deriveFont(12f));
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(placementControls, BorderLayout.CENTER);
        bottomPanel.add(statsLabel, BorderLayout.SOUTH);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        panel.putClientProperty("boardsPanel", boards);
        panel.putClientProperty("topPanel", topPanel);

        refreshBoards();
        updateStatsLabel();
        updateSaveButtonState();
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
        if (!fullscreen) {
            pack();
            setLocationRelativeTo(null);
        } else {
            revalidate();
            repaint();
        }
    }

    private void startNewVsAiGame() {
        cancelTurnDelay();
        shutdownOnlineMatch(true);
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
            controller = new GameController(new Board(), aiBoard, GameMode.VS_AI, statisticsManager);
            placementMode = false;
            placementControls.setVisible(false);
            statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
            refreshBoards();
            enableEnemyBoard();
            updateSaveButtonState();
            showScreen(Screen.GAME);
        }
    }

    private void startLocalTwoPlayersGame() {
        cancelTurnDelay();
        shutdownOnlineMatch(true);
        currentMode = GameMode.LOCAL_PVP;
        if (gamePanel == null) {
            gamePanel = createGamePanel();
            mainPanel.add(gamePanel, Screen.GAME.name());
        }
        pendingPlayerOneBoard = null;
        pendingPlayerTwoBoard = null;
        promptPlacementForPlayer(1);
        updateSaveButtonState();
    }

    private void loadGameFromMenu() {
        shutdownOnlineMatch(true);
        List<String> saves = SaveManager.listSaves();
        if (saves.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    localized("Немає збережених ігор", "No saves available"));
            return;
        }
        Object selection = JOptionPane.showInputDialog(this,
                localized("Оберіть збереження для завантаження", "Choose a save to load"),
                Localization.t("window.title", currentLanguage),
                JOptionPane.PLAIN_MESSAGE,
                null,
                saves.toArray(),
                saves.get(0));
        if (selection == null) {
            return;
        }
        try {
            GameState state = SaveManager.load(selection.toString());
            if (state.getMode() != GameMode.VS_AI) {
                JOptionPane.showMessageDialog(this,
                        localized("Поки що підтримується лише завантаження ігор проти комп'ютера",
                                "Only VS AI saves are supported right now"));
                return;
            }
            applyLoadedGame(state);
        } catch (IOException | ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(this,
                    localized("Не вдалося завантажити гру", "Unable to load the save") + "\n" + ex.getMessage(),
                    Localization.t("window.title", currentLanguage),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyLoadedGame(GameState state) {
        cancelTurnDelay();
        currentMode = state.getMode();
        currentLanguage = state.getLanguage();
        if (gamePanel == null) {
            gamePanel = createGamePanel();
            mainPanel.add(gamePanel, Screen.GAME.name());
        }
        controller = new GameController(state.getPlayerOneBoard(), state.getPlayerTwoBoard(), state.getMode(),
                state.isPlayerTurn(), state.isPlayerOneTurn(), state.getComputerAi(), statisticsManager);
        placementMode = false;
        placementControls.setVisible(false);
        applyLocalization();
        refreshBoards();
        updateSaveButtonState();
        updateStatsLabel();
        showScreen(Screen.GAME);
        if (controller.isGameOver()) {
            statusLabel.setText(controller.getPlayerBoard().allShipsSunk()
                    ? Localization.t("status.lose", currentLanguage)
                    : Localization.t("status.win", currentLanguage));
            disableEnemyBoard();
            return;
        }
        if (currentMode == GameMode.VS_AI) {
            if (controller.isPlayerTurn()) {
                statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
                enableEnemyBoard();
            } else {
                statusLabel.setText(Localization.t("status.wait", currentLanguage));
                disableEnemyBoard();
                scheduleTurnDelay(this::executeAiTurn);
            }
        } else {
            statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
            enableEnemyBoard();
        }
    }

    private void createOnlineGame() {
        shutdownOnlineMatch(true);
        String input = JOptionPane.showInputDialog(this,
                localized("Вкажіть порт для сервера", "Enter port to host"), "5000");
        if (input == null) {
            return;
        }
        int port;
        try {
            port = Integer.parseInt(input.trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    localized("Некоректний порт", "Invalid port"));
            return;
        }
        JDialog waiting = showProgressDialog(localized("Очікування підключення суперника...", "Waiting for opponent..."));
        new Thread(() -> {
            try {
                NetworkServer server = new NetworkServer(port);
                Socket socket = server.waitForClient();
                server.close();
                SwingUtilities.invokeLater(() -> {
                    waiting.dispose();
                    beginOnlineSession(GameMode.ONLINE_HOST, socket);
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    waiting.dispose();
                    JOptionPane.showMessageDialog(this,
                            localized("Не вдалося створити онлайн-гру", "Unable to host the game") + "\n"
                                    + ex.getMessage(),
                            Localization.t("window.title", currentLanguage),
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "host-wait").start();
    }

    private void joinOnlineGame() {
        shutdownOnlineMatch(true);
        JTextField hostField = new JTextField("127.0.0.1");
        JTextField portField = new JTextField("5000");
        Object[] message = {localized("Адреса сервера:", "Server address:"), hostField,
                localized("Порт:", "Port:"), portField};
        int option = JOptionPane.showConfirmDialog(this, message,
                Localization.t("menu.join", currentLanguage), JOptionPane.OK_CANCEL_OPTION);
        if (option != JOptionPane.OK_OPTION) {
            return;
        }
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    localized("Некоректний порт", "Invalid port"));
            return;
        }
        String host = hostField.getText().trim();
        if (host.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    localized("Адреса не може бути порожньою", "Host cannot be empty"));
            return;
        }
        JDialog connecting = showProgressDialog(localized("З'єднання...", "Connecting..."));
        new Thread(() -> {
            try {
                NetworkClient client = new NetworkClient(host, port);
                Socket socket = client.connect();
                SwingUtilities.invokeLater(() -> {
                    connecting.dispose();
                    beginOnlineSession(GameMode.ONLINE_CLIENT, socket);
                });
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    connecting.dispose();
                    JOptionPane.showMessageDialog(this,
                            localized("Не вдалося підключитися", "Unable to join the game") + "\n"
                                    + ex.getMessage(),
                            Localization.t("window.title", currentLanguage),
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "client-join").start();
    }

    private void beginOnlineSession(GameMode mode, Socket socket) {
        cancelTurnDelay();
        currentMode = mode;
        if (gamePanel == null) {
            gamePanel = createGamePanel();
            mainPanel.add(gamePanel, Screen.GAME.name());
        }
        Board playerBoard = new Board(false);
        Board opponentBoard = new Board(false);
        opponentBoard.setVirtualFleet(true);
        boolean playerStarts = mode == GameMode.ONLINE_HOST;
        controller = new GameController(playerBoard, opponentBoard, mode, playerStarts, true, null, statisticsManager);
        placementMode = false;
        placementControls.setVisible(false);
        refreshBoards();
        showScreen(Screen.GAME);
        updateStatsLabel();
        updateSaveButtonState();
        try {
            onlineMatch = new OnlineMatch(controller, this, socket);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    localized("Не вдалося розпочати онлайн-гру", "Unable to start the online match") + "\n"
                            + ex.getMessage(),
                    Localization.t("window.title", currentLanguage), JOptionPane.ERROR_MESSAGE);
            returnToMenu();
            return;
        }
        statusLabel.setText(localized("Розмістіть кораблі", "Place your ships"));
        promptOnlinePlacement();
    }

    private void promptOnlinePlacement() {
        if (controller == null) {
            return;
        }
        Board playerBoard = controller.getPlayerBoard();
        boolean manual = askManualPlacement();
        if (manual) {
            beginManualPlacement(playerBoard, currentMode, 1);
        } else {
            playerBoard.reset(true);
            placementMode = false;
            placementControls.setVisible(false);
            refreshBoards();
            statusLabel.setText(localized("Очікуємо готовність суперника", "Waiting for opponent readiness"));
            if (onlineMatch != null) {
                onlineMatch.markLocalReady();
            }
            disableEnemyBoard();
        }
    }

    private JDialog showProgressDialog(String message) {
        JDialog dialog = new JDialog(this, Localization.t("window.title", currentLanguage), false);
        dialog.getContentPane().add(new JLabel(message, SwingConstants.CENTER));
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return dialog;
    }

    private void saveCurrentGame() {
        if (controller == null || currentMode != GameMode.VS_AI || placementMode || controller.isGameOver()) {
            JOptionPane.showMessageDialog(this,
                    localized("Зберігати можна лише активну гру проти комп'ютера",
                            "You can only save an active VS AI game"));
            return;
        }
        String name = JOptionPane.showInputDialog(this,
                localized("Введіть назву збереження", "Enter a save name"),
                Localization.t("window.title", currentLanguage),
                JOptionPane.PLAIN_MESSAGE);
        if (name == null) {
            return;
        }
        name = name.trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    localized("Назва збереження не може бути порожньою", "Save name cannot be empty"));
            return;
        }
        try {
            SaveManager.save(controller.createState(currentLanguage), name);
            JOptionPane.showMessageDialog(this,
                    localized("Гру збережено", "Game saved"));
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    localized("Не вдалося зберегти гру", "Unable to save the game") + "\n" + ex.getMessage(),
                    Localization.t("window.title", currentLanguage),
                    JOptionPane.ERROR_MESSAGE);
        }
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
        cancelTurnDelay();
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
        updateSaveButtonState();
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
            statisticsManager.reset();
            updateStatsLabel();
            JOptionPane.showMessageDialog(this,
                    Localization.t("dialog.resetStatsDone", currentLanguage));
        }
    }

    private void exitGame() {
        shutdownOnlineMatch(true);
        dispose();
        System.exit(0);
    }

    private void handlePlayerShot(int row, int col) {
        if (placementMode || controller == null) {
            return;
        }
        if (controller.isGameOver()) {
            statusLabel.setText(Localization.t("status.gameOver", currentLanguage));
            return;
        }
        if (!isOnlineMode() && currentMode != GameMode.LOCAL_PVP && !controller.isPlayerTurn()) {
            statusLabel.setText(Localization.t("status.wait", currentLanguage));
            return;
        }
        if (currentMode == GameMode.LOCAL_PVP && controller.isLocalSwitchPending()) {
            statusLabel.setText(localized("Зміна гравця триває", "Player switch in progress"));
            return;
        }
        if (isOnlineMode()) {
            handleOnlineShot(row, col);
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
                updateStatsLabel();
                return;
            }
            if (result.getOutcome() == ShotOutcome.MISS) {
                statusLabel.setText(localized("Передайте керування іншому гравцеві",
                        "Pass control to the other player"));
                disableEnemyBoard();
                showLocalTurnSwitchDialog();
            } else {
                statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
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
            cancelTurnDelay();
            statusLabel.setText(Localization.t("status.win", currentLanguage));
            disableEnemyBoard();
            updateStatsLabel();
            updateSaveButtonState();
            return;
        }
        if (result.getOutcome() == ShotOutcome.MISS) {
            statusLabel.setText(Localization.t("status.wait", currentLanguage));
            disableEnemyBoard();
            scheduleTurnDelay(this::executeAiTurn);
        } else {
            statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
            enableEnemyBoard();
        }
        updateSaveButtonState();
    }

    private void handleOnlineShot(int row, int col) {
        if (onlineMatch == null || controller == null) {
            return;
        }
        if (!onlineMatch.isReadyToPlay()) {
            statusLabel.setText(localized("Очікуємо готовність суперника", "Waiting for opponent readiness"));
            return;
        }
        if (!controller.isPlayerTurn()) {
            statusLabel.setText(Localization.t("status.wait", currentLanguage));
            return;
        }
        Cell cell = controller.getAiBoard().getCells()[row][col];
        if (cell.isShot()) {
            statusLabel.setText(Localization.t("status.already", currentLanguage));
            return;
        }
        disableEnemyBoard();
        statusLabel.setText(localized("Відправляємо постріл...", "Sending shot..."));
        onlineMatch.fireShot(row, col);
    }

    private void updateOnlineTurnState() {
        if (!isOnlineMode() || controller == null || statusLabel == null) {
            return;
        }
        if (controller.isGameOver()) {
            return;
        }
        if (onlineMatch == null || !onlineMatch.isReadyToPlay()) {
            statusLabel.setText(localized("Очікуємо готовність суперника", "Waiting for opponent readiness"));
            disableEnemyBoard();
            return;
        }
        if (controller.isPlayerTurn()) {
            statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
            enableEnemyBoard();
        } else {
            statusLabel.setText(Localization.t("status.wait", currentLanguage));
            disableEnemyBoard();
        }
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
        cancelTurnDelay();
        placementMode = false;
        placementControls.setVisible(false);
        if (currentMode == GameMode.VS_AI) {
            Board aiBoard = pendingPlayerTwoBoard != null ? pendingPlayerTwoBoard : new Board();
            controller = new GameController(currentPlacementBoard, aiBoard, GameMode.VS_AI, statisticsManager);
            statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
            refreshBoards();
            enableEnemyBoard();
            updateSaveButtonState();
            showScreen(Screen.GAME);
        } else if (currentMode == GameMode.LOCAL_PVP) {
            if (placementPlayerIndex == 1) {
                pendingPlayerOneBoard = currentPlacementBoard;
                promptPlacementForPlayer(2);
            } else {
                pendingPlayerTwoBoard = currentPlacementBoard;
                beginLocalBattle();
            }
        } else if (isOnlineMode()) {
            placementMode = false;
            placementControls.setVisible(false);
            refreshBoards();
            statusLabel.setText(localized("Очікуємо готовність суперника", "Waiting for opponent readiness"));
            if (onlineMatch != null) {
                onlineMatch.markLocalReady();
            }
            disableEnemyBoard();
        }
    }

    private void beginLocalBattle() {
        if (pendingPlayerOneBoard == null || pendingPlayerTwoBoard == null) {
            return;
        }
        cancelTurnDelay();
        controller = new GameController(pendingPlayerOneBoard, pendingPlayerTwoBoard, GameMode.LOCAL_PVP,
                statisticsManager);
        statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
        placementMode = false;
        placementControls.setVisible(false);
        refreshBoards();
        enableEnemyBoard();
        updateSaveButtonState();
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

    private void showLocalTurnSwitchDialog() {
        JOptionPane.showMessageDialog(this,
                localized("Зміна гравця. Передайте керування іншому гравцеві і натисніть ОК",
                        "Switch players. Pass control and press OK"));
        controller.completeLocalSwitch();
        refreshBoards();
        statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
        enableEnemyBoard();
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
                refreshBoards();
                break;
            default:
                break;
        }
    }

    private void executeAiTurn() {
        ShotResult ai = controller.aiFire();
        paintPlayerShot(ai);
        refreshBoards();
        if (controller.isGameOver()) {
            statusLabel.setText(Localization.t("status.lose", currentLanguage));
            disableEnemyBoard();
            updateStatsLabel();
            updateSaveButtonState();
            return;
        }
        statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
        disableEnemyBoard();
        scheduleTurnDelay(this::enableEnemyBoard);
        updateSaveButtonState();
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
                refreshBoards();
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

    private Board getSelfBoardForDisplay() {
        if (controller == null) {
            return null;
        }
        if (currentMode == GameMode.LOCAL_PVP && !controller.isPlayerTurn()) {
            return controller.getAiBoard();
        }
        return controller.getPlayerBoard();
    }

    private Board getTargetBoardForDisplay() {
        if (controller == null) {
            return null;
        }
        if (currentMode == GameMode.LOCAL_PVP && !controller.isPlayerTurn()) {
            return controller.getPlayerBoard();
        }
        return controller.getAiBoard();
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
        Board self = getSelfBoardForDisplay();
        Board target = getTargetBoardForDisplay();
        if (self == null || target == null) {
            return;
        }
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
                    } else if (targetCells[r][c].isRemoteHit()) {
                        aiBtn.setBackground(targetCells[r][c].isRemoteSunk() ? SUNK : HIT);
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
        if (placementMode || controller == null || controller.isGameOver()) {
            disableEnemyBoard();
            return;
        }
        if ((currentMode == GameMode.VS_AI && !controller.isPlayerTurn())
                || (currentMode == GameMode.LOCAL_PVP && controller.isLocalSwitchPending())) {
            disableEnemyBoard();
            return;
        }
        if (isOnlineMode()) {
            if (onlineMatch == null || !onlineMatch.isReadyToPlay() || !controller.isPlayerTurn()) {
                disableEnemyBoard();
                return;
            }
        }
        Board target = getTargetBoardForDisplay();
        if (target == null) {
            disableEnemyBoard();
            return;
        }
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

    private void updateStatsLabel() {
        if (statsLabel != null) {
            statsLabel.setText(statisticsManager.formatInline(currentLanguage));
        }
    }

    private boolean isOnlineMode() {
        return currentMode == GameMode.ONLINE_HOST || currentMode == GameMode.ONLINE_CLIENT;
    }

    private void updateSaveButtonState() {
        if (saveGameButton == null) {
            return;
        }
        boolean enabled = controller != null && currentMode == GameMode.VS_AI && !placementMode
                && !controller.isGameOver() && controller.isPlayerTurn();
        saveGameButton.setEnabled(enabled);
    }

    private void shutdownOnlineMatch(boolean notifyOpponent) {
        if (onlineMatch != null) {
            if (notifyOpponent) {
                onlineMatch.disconnect();
            } else {
                onlineMatch.shutdown();
            }
            onlineMatch = null;
        }
    }

    private void updateStatusForCurrentTurn() {
        if (statusLabel == null) {
            return;
        }
        if (controller == null || controller.isGameOver()) {
            statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
            return;
        }
        if (currentMode == GameMode.VS_AI) {
            statusLabel.setText(controller.isPlayerTurn()
                    ? Localization.t("status.yourTurn", currentLanguage)
                    : Localization.t("status.wait", currentLanguage));
        } else if (isOnlineMode()) {
            updateOnlineTurnState();
        } else {
            statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
        }
    }

    private void returnToMenu() {
        cancelTurnDelay();
        shutdownOnlineMatch(true);
        showScreen(Screen.MENU);
        updateSaveButtonState();
    }

    private void scheduleTurnDelay(Runnable action) {
        cancelTurnDelay();
        turnDelayTimer = new Timer(1000, e -> {
            ((Timer) e.getSource()).stop();
            turnDelayTimer = null;
            action.run();
        });
        turnDelayTimer.setRepeats(false);
        turnDelayTimer.start();
    }

    private void cancelTurnDelay() {
        if (turnDelayTimer != null) {
            turnDelayTimer.stop();
            turnDelayTimer = null;
        }
    }

    private void applyLocalization() {
        setTitle(Localization.t("window.title", currentLanguage));
        applyMenuTexts(menuPanel);
        if (gamePanel != null) {
            newGameButton.setText(Localization.t("game.newGame", currentLanguage));
            backToMenuButton.setText(Localization.t("game.backToMenu", currentLanguage));
            if (saveGameButton != null) {
                saveGameButton.setText(currentLanguage == Language.UKRAINIAN ? "Зберегти гру" : "Save game");
            }
            updateStatusForCurrentTurn();
            if (horizontalButton != null && verticalButton != null) {
                horizontalButton
                        .setText(currentLanguage == Language.UKRAINIAN ? "Горизонтально" : "Horizontal");
                verticalButton.setText(currentLanguage == Language.UKRAINIAN ? "Вертикально" : "Vertical");
            }
            if (placementDoneButton != null) {
                placementDoneButton.setText(currentLanguage == Language.UKRAINIAN ? "Готово" : "Done");
            }
            updateRemainingShipsLabel();
            updateStatsLabel();
            updateSaveButtonState();
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
        if (fullscreenToggle != null) {
            fullscreenToggle.setText(Localization.t("menu.fullscreen", currentLanguage));
            fullscreenToggle.setSelected(fullscreen);
        }
    }

    @Override
    public void onOpponentReady() {
        statusLabel.setText(localized("Суперник готовий", "Opponent is ready"));
        updateOnlineTurnState();
    }

    @Override
    public void onLocalShotResult(ShotResult result) {
        paintEnemyShot(result);
        refreshBoards();
        updateOnlineTurnState();
    }

    @Override
    public void onIncomingShot(ShotResult result) {
        paintPlayerShot(result);
        refreshBoards();
        if (controller != null && controller.isGameOver()) {
            handleOnlineDefeat();
        } else {
            updateOnlineTurnState();
        }
    }

    @Override
    public void onTurnChanged(boolean yourTurn) {
        updateOnlineTurnState();
    }

    @Override
    public void onGameOver(boolean localWon) {
        if (localWon) {
            handleOnlineWin();
        } else {
            handleOnlineDefeat();
        }
    }

    @Override
    public void onNetworkError(String message) {
        if (controller != null && !controller.isGameOver()) {
            controller.concludeOnlineGame(true);
            handleOnlineWin();
        } else {
            shutdownOnlineMatch(false);
        }
        JOptionPane.showMessageDialog(this,
                localized("Помилка мережі: ", "Network error: ") + message,
                Localization.t("window.title", currentLanguage), JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void requestBoardRefresh() {
        refreshBoards();
    }

    private void handleOnlineWin() {
        statusLabel.setText(Localization.t("status.win", currentLanguage));
        disableEnemyBoard();
        updateStatsLabel();
        updateSaveButtonState();
        JOptionPane.showMessageDialog(this,
                localized("Ви виграли! Вітаємо!", "You won! Congratulations!"),
                Localization.t("window.title", currentLanguage), JOptionPane.INFORMATION_MESSAGE);
        shutdownOnlineMatch(false);
    }

    private void handleOnlineDefeat() {
        statusLabel.setText(Localization.t("status.lose", currentLanguage));
        disableEnemyBoard();
        updateStatsLabel();
        updateSaveButtonState();
        JOptionPane.showMessageDialog(this,
                localized("Ви програли. Спробуйте ще раз!", "You lost. Try again!"),
                Localization.t("window.title", currentLanguage), JOptionPane.WARNING_MESSAGE);
        shutdownOnlineMatch(false);
    }

    private void toggleFullscreen() {
        setFullscreen(!fullscreen);
    }

    private void setFullscreen(boolean enable) {
        if (fullscreen == enable) {
            return;
        }
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        Rectangle currentBounds = getBounds();
        dispose();
        setUndecorated(enable);
        if (enable) {
            windowedBounds = currentBounds;
            setVisible(true);
            device.setFullScreenWindow(this);
        } else {
            device.setFullScreenWindow(null);
            setVisible(true);
            if (windowedBounds != null) {
                setBounds(windowedBounds);
            } else {
                pack();
                setLocationRelativeTo(null);
            }
        }
        fullscreen = enable;
        if (fullscreenToggle != null) {
            fullscreenToggle.setSelected(enable);
        }
    }

    private String localized(String ua, String en) {
        return currentLanguage == Language.UKRAINIAN ? ua : en;
    }
}
