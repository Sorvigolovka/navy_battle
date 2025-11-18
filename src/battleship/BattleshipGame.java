package battleship;

import javax.swing.SwingUtilities;

public class BattleshipGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(BattleshipFrame::new);
    }
}
