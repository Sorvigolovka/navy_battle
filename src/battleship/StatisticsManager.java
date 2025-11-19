package battleship;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

class StatisticsManager implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String FILE = "stats.sav";
    private int vsAiWins;
    private int vsAiLosses;
    private int localWins;
    private int localLosses;
    private int onlineWins;
    private int onlineLosses;

    static StatisticsManager load() {
        File f = new File(FILE);
        if (!f.exists()) {
            return new StatisticsManager();
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            return (StatisticsManager) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new StatisticsManager();
        }
    }

    void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE))) {
            out.writeObject(this);
        } catch (IOException ignored) {
        }
    }

    void recordWin(GameMode mode) {
        switch (mode) {
            case VS_AI:
                vsAiWins++;
                break;
            case LOCAL_PVP:
                localWins++;
                break;
            default:
                onlineWins++;
        }
        save();
    }

    void recordLoss(GameMode mode) {
        switch (mode) {
            case VS_AI:
                vsAiLosses++;
                break;
            case LOCAL_PVP:
                localLosses++;
                break;
            default:
                onlineLosses++;
        }
        save();
    }

    void reset() {
        vsAiWins = vsAiLosses = localWins = localLosses = onlineWins = onlineLosses = 0;
        save();
    }

    String formatInline(Language language) {
        return label(language, "Проти ПК", "VS AI") + ": " + vsAiWins + "/" + vsAiLosses
                + "  |  " + label(language, "Локально (P1/P2)", "Local (P1/P2)") + ": " + localWins + "/"
                + localLosses + "  |  " + label(language, "Онлайн", "Online") + ": " + onlineWins + "/"
                + onlineLosses;
    }

    private String label(Language language, String ua, String en) {
        return language == Language.UKRAINIAN ? ua : en;
    }
}
