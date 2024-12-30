package villagerCreator;

import org.bukkit.entity.Player;

public class PlayerSneakInfo
{
    private final Player main;
    private final Player other;
    private long startTime;
    private int sneakCount;

    public PlayerSneakInfo(Player main, Player other, Long startTime) {
        this.main = main;
        this.other = other;
        this.startTime = startTime;
    }

    @Override
    public String toString() {
        return "PlayerSneakInfo{" +
                "main=" + main.getUniqueId() + ": " + main.getName() +
                ", other=" + other.getUniqueId() + ": " + other.getName() +
                ", startTime=" + startTime +
                ", sneakCount=" + sneakCount +
                '}';
    }

    public Player getMain() {
        return main;
    }

    public Player getOther() {
        return other;
    }

    public long getStartTime() {
        return startTime;
    }

    public int getSneakCount() {
        return sneakCount;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setStartTime() {
        setStartTime(System.currentTimeMillis());
    }

    public void setSneakCount(int sneakCount) {
        this.sneakCount = sneakCount;
    }

    public int incrementSneakCount() {
        return sneakCount++;
    }
}
