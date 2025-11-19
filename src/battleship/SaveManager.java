package battleship;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class SaveManager {
    private static final String SAVE_DIR = "saves";

    static List<String> listSaves() {
        File dir = new File(SAVE_DIR);
        if (!dir.exists()) {
            return List.of();
        }
        String[] names = dir.list((d, name) -> name.endsWith(".sav"));
        if (names == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String n : names) {
            result.add(n);
        }
        return result;
    }

    static void save(GameState state, String name) throws IOException {
        Files.createDirectories(Path.of(SAVE_DIR));
        File file = new File(SAVE_DIR, name.endsWith(".sav") ? name : name + ".sav");
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(state);
        }
    }

    static GameState load(String name) throws IOException, ClassNotFoundException {
        File file = new File(SAVE_DIR, name);
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (GameState) in.readObject();
        }
    }
}
