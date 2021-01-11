package de.raidcraft.achievements.entities;

import de.raidcraft.achievements.entities.query.QPlayerAchievement;
import io.ebean.Finder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;

import javax.persistence.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static de.raidcraft.achievements.AchievementsPlugin.TABLE_PREFIX;

/**
 * The player achievement is created when a player discovers or unlocks an achievement.
 * <p>It is also used to track any datapoints an achievement gathers and updated once unlocked.
 */
@Entity
@Getter
@Setter(AccessLevel.PACKAGE)
@Accessors(fluent = true)
@Table(name = TABLE_PREFIX + "player_achievements")
public class PlayerAchievement extends BaseEntity {

    /**
     * Finds or creates a player achievement from the given achievement and player combination.
     *
     * @param achievement the achievement
     * @param player the player
     * @return the created or existing player achievement
     */
    public static PlayerAchievement of(Achievement achievement, AchievementPlayer player) {

        return find(achievement, player)
                .orElseGet(() -> {
                    PlayerAchievement playerAchievement = new PlayerAchievement(achievement, player);
                    playerAchievement.insert();
                    return playerAchievement;
                });
    }

    /**
     * Tries to find a player achievement for the given achievement and player combination.
     *
     * @param achievement the achievement
     * @param player the player
     * @return the player achievement or an empty optional
     */
    public static Optional<PlayerAchievement> find(Achievement achievement, AchievementPlayer player) {

        return new QPlayerAchievement().where()
                .achievement.eq(achievement)
                .and()
                .player.eq(player)
                .findOneOrEmpty();
    }

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

    /**
     * The time when the player unlocked the achievement.
     * Is null until it is unlocked.
     */
    private Instant unlocked;

    /**
     * The meta data store of the player achievement.
     * <p>It can hold additional persistent information for the execution of various achievement types.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private DataStore data = new DataStore();

    PlayerAchievement(Achievement achievement, AchievementPlayer player) {
        this.achievement = achievement;
        this.player = player;
    }
}
