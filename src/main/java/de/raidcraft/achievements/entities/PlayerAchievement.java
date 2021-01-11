package de.raidcraft.achievements.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import static de.raidcraft.achievements.AchievementsPlugin.TABLE_PREFIX;

/**
 * The player achievement is created when a player discovers or unlocks an achievement.
 * <p>It is also used to track any datapoints an achievement gathers and updated once unlocked.
 */
@Entity
@Getter
@Setter
@Accessors(fluent = true)
@Table(name = TABLE_PREFIX + "player_achievements")
public class PlayerAchievement extends BaseEntity {

    /**
     * The achievement this player achievement references.
     */
    @ManyToOne(optional = false)
    private Achievement achievement;
    /**
     * The player that unlocked the achievement.
     */
    @ManyToOne(optional = false)
    private AchievementPlayer player;
}
