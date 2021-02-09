package de.raidcraft.achievements;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Mark an achievement type with this interface to periodically
 * call the {@link #tickAsync(Player)} method for each online player.
 * <p>Do not call any bukkit api from this method as async operations are not allowed.
 */
public interface PeriodicAsync {

    /**
     * Called in a configurable interval for each online player.
     * <p>Make sure to filter players with the {@link AchievementType#notApplicable(OfflinePlayer)} function.
     *
     * @param player the player to check the achievement for
     */
    void tickAsync(Player player);
}
