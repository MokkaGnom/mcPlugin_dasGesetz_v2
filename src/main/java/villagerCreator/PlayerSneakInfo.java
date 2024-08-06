package villagerCreator;

import org.bukkit.entity.Player;

public class PlayerSneakInfo
{
    private final Player player1;
    private final Player player2;
    private final Long startTime;
    private int sneakCount;

    public PlayerSneakInfo(Player player1, Player player2, Long startTime) {
        this.player1 = player1;
        this.player2 = player2;
        this.startTime = startTime;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public Long getStartTime() {
        return startTime;
    }

    public int getSneakCount() {
        return sneakCount;
    }

    public int incrementSneakCount() {
        return sneakCount++;
    }
}
