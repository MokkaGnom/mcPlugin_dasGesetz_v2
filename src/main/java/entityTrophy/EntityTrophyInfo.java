package entityTrophy;

import org.bukkit.entity.EntityType;
import org.bukkit.profile.PlayerProfile;

public record EntityTrophyInfo(EntityType type, double dropProbability, PlayerProfile profile) {
}
