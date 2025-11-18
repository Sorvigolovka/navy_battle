package battleship;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(boards, BorderLayout.CENTER);
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

    private void showScreen(Screen screen) {
        cardLayout.show(mainPanel, screen.name());
        pack();
        setLocationRelativeTo(null);
    }

    private void startNewVsAiGame() {
        if (gamePanel == null) {
            gamePanel = createGamePanel();
            mainPanel.add(gamePanel, Screen.GAME.name());
        }
        controller = new GameController(new Board(), new Board());
        statusLabel.setText(Localization.t("status.yourTurn", currentLanguage));
        refreshBoards();
        enableEnemyBoard();
        showScreen(Screen.GAME);
    }

    private void startLocalTwoPlayersGame() {
        JOptionPane.showMessageDialog(this,
                Localization.t("dialog.localNotImplemented", currentLanguage));
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
        if (!controller.isPlayerTurn()) {
            statusLabel.setText(Localization.t("status.wait", currentLanguage));
            return;
        }
        if (controller.isGameOver()) {
            statusLabel.setText(Localization.t("status.gameOver", currentLanguage));
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
