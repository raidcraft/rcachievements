package de.raidcraft.achievements;

import de.raidcraft.achievements.entities.AchievementPlayer;
import net.kyori.adventure.text.Component;

/**
 * Marks an achievement type as a progress achievement that can return details about the current progress of the achievement.
 */
@FunctionalInterface
public interface Progressable {

    /**
     * Renders the current progress of the achievement for the given player.
     *
     * @param player the player to render the progress for
     * @return the rendered progress of the player
     */
    Component progress(AchievementPlayer player);
}
