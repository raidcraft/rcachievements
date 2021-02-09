package de.raidcraft.achievements;

import de.raidcraft.achievements.entities.AchievementPlayer;
import net.kyori.adventure.text.Component;

/**
 * Marks an achievement type as a progress achievement that can return details about the current progress of the achievement.
 */
public interface Progressable {

    /**
     * Renders the current progress of the achievement for the given player.
     *
     * @param player the player to render the progress for
     * @return the rendered progress of the player
     */
    Component progressText(AchievementPlayer player);

    /**
     * Calculates the progress for the achievement in a range from 0.0f t 1.0f.
     * <p>Where 1.0f is 100% and 0.0f is 0%.
     * <p>The function should be callable from an async thread.
     *
     * @param player the player to calculate the progress for
     * @return the calculated progress of the player
     */
    float progress(AchievementPlayer player);
}
