package battleship;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

class StatisticsManager implements Serializable {
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

    String format(Language language) {
        return Localization.t("stats.vsAi", language) + ": " + vsAiWins + " / " + vsAiLosses + "\n"
                + Localization.t("stats.local", language) + ": " + localWins + " / " + localLosses + "\n"
                + Localization.t("stats.online", language) + ": " + onlineWins + " / " + onlineLosses;
    }
}
