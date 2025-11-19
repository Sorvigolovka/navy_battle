package battleship;

import java.util.HashMap;
import java.util.Map;

class Localization {
    private static final Map<String, String> UA = new HashMap<>();
    private static final Map<String, String> EN = new HashMap<>();

    static {
        UA.put("window.title", "Морський бій (Java)");
        UA.put("menu.newVsAi", "Нова гра проти комп'ютера");
        UA.put("menu.localTwoPlayers", "Нова локальна гра для двох гравців");
        UA.put("menu.load", "Завантажити збереження");
        UA.put("menu.host", "Створити онлайн-гру");
        UA.put("menu.join", "Підключитися до онлайн-гри");
        UA.put("menu.language", "Змінити мову");
        UA.put("menu.resetStats", "Скинути статистику");
        UA.put("menu.exit", "Вихід");
        UA.put("game.newGame", "Нова гра");
        UA.put("game.backToMenu", "Повернутися до меню");
        UA.put("status.yourTurn", "Ваш хід");
        UA.put("status.wait", "Зачекайте на свій хід");
        UA.put("status.win", "Ви перемогли! Натисніть 'Нова гра'");
        UA.put("status.lose", "Поразка. Спробуйте ще раз");
        UA.put("status.already", "Ви вже стріляли сюди");
        UA.put("status.gameOver", "Гра завершена — натисніть 'Нова гра'");
        UA.put("status.opponentTurn", "Хід суперника...");
        UA.put("dialog.localNotImplemented", "Режим локальної гри ще не реалізовано");
        UA.put("dialog.loadPlaceholder", "Функція завантаження гри буде додана пізніше");
        UA.put("dialog.onlinePlaceholder", "Онлайн-режим ще не перенесено у графічну версію");
        UA.put("dialog.resetStatsNotImplemented", "Статистика ще не реалізована");
        UA.put("dialog.resetStatsConfirm", "Ви впевнені, що хочете скинути статистику?");
        UA.put("dialog.resetStatsDone", "Статистику скинуто.");
        UA.put("dialog.languageTitle", "Оберіть мову");

        EN.put("window.title", "Battleship (Java)");
        EN.put("menu.newVsAi", "New game vs Computer");
        EN.put("menu.localTwoPlayers", "New local two-player game");
        EN.put("menu.load", "Load save");
        EN.put("menu.host", "Create online game");
        EN.put("menu.join", "Join online game");
        EN.put("menu.language", "Change language");
        EN.put("menu.resetStats", "Reset statistics");
        EN.put("menu.exit", "Exit");
        EN.put("game.newGame", "New game");
        EN.put("game.backToMenu", "Back to menu");
        EN.put("status.yourTurn", "Your turn");
        EN.put("status.wait", "Please wait for your turn");
        EN.put("status.win", "You win! Click 'New game'");
        EN.put("status.lose", "Defeat. Try again");
        EN.put("status.already", "You already fired here");
        EN.put("status.gameOver", "Game is over — press 'New game'");
        EN.put("status.opponentTurn", "Opponent is thinking...");
        EN.put("dialog.localNotImplemented", "Local game mode is not implemented yet");
        EN.put("dialog.loadPlaceholder", "Load game feature will be added later");
        EN.put("dialog.onlinePlaceholder", "Online mode has not been ported to GUI yet");
        EN.put("dialog.resetStatsNotImplemented", "Statistics are not implemented yet");
        EN.put("dialog.resetStatsConfirm", "Are you sure you want to reset statistics?");
        EN.put("dialog.resetStatsDone", "Statistics reset.");
        EN.put("dialog.languageTitle", "Choose language");
    }

    static String t(String key, Language language) {
        Map<String, String> bundle = language == Language.ENGLISH ? EN : UA;
        return bundle.getOrDefault(key, key);
    }
}
