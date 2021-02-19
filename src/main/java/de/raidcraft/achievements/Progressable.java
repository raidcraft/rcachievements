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

    /**
     * The counter of the progress for the given player.
     * <p>The {@link #progress(AchievementPlayer)} should match progressCount/progressMaxCount.
     * <p>If the counter is not implemented return -1.
     *
     * @param player the player to get the progress counter for
     * @return the progress count for the player
     */
    default long progressCount(AchievementPlayer player) { return -1; }

    /**
     * The required count to get the achievement.
     * <p>Can be -1 if not implemented.
     *
     * @param player the player to get the max counter for
     * @return the maximum progress count of the achievement
     */
    default long progressMaxCount(AchievementPlayer player) { return -1; }
}
