package de.raidcraft.achievements.entities;

import de.raidcraft.achievements.RCAchievements;
import de.raidcraft.achievements.events.PlayerLostAchievementEvent;
import de.raidcraft.achievements.events.PlayerUnlockedAchievementEvent;
import io.ebean.Finder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;
import org.bukkit.Bukkit;

import javax.persistence.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static de.raidcraft.achievements.Constants.TABLE_PREFIX;

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

    public static final Finder<UUID, PlayerAchievement> find = new Finder<>(PlayerAchievement.class);

    /**
     * Finds or creates a player achievement from the given achievement and player combination.
     *
     * @param achievement the achievement
     * @param player the player
     * @return the created or existing player achievement
     */
    public static PlayerAchievement of(@NonNull Achievement achievement, @NonNull AchievementPlayer player) {

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
    public static Optional<PlayerAchievement> find(@NonNull Achievement achievement, @NonNull AchievementPlayer player) {

        return find.query().where()
                .eq("achievement_id", achievement.id())
                .and().eq("player_id", player.id())
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
     * True if the player already claimed the global rewards of the achievement.
     */
    @Setter
    private boolean claimedGlobalRewards = false;

    /**
     * True if the player claimed the achievement specific rewards.
     */
    @Setter
    private boolean claimedRewards = false;

    /**
     * The meta data store of the player achievement.
     * <p>It can hold additional persistent information for the execution of various achievement types.
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private DataStore data = new DataStore();

    PlayerAchievement(@NonNull Achievement achievement, @NonNull AchievementPlayer player) {
        this.achievement = achievement;
        this.player = player;
    }

    /**
     * @return true if the player unlocked the achievement
     */
    public boolean isUnlocked() {

        return unlocked() != null;
    }

    /**
     * Checks if this achievement is active.
     * <p>Active means the achievement is not unlocked and its
     * parents have been unlocked.
     *
     * @return true if the achievement is active
     */
    public boolean isActive() {

        if (isUnlocked()) return false;

        if (achievement().isChild()) {
            return PlayerAchievement.of(achievement().parent(), player()).isUnlocked();
        }

        return true;
    }

    /**
     * Unlocks the achievement for this player if it is not unlocked.
     * <p>Nothing will happen if the achievement is already unlocked.
     */
    public void unlock() {

        if (unlocked() != null) return;

        unlocked(Instant.now()).save();

        Bukkit.getScheduler().runTask(RCAchievements.instance(), () ->
            Bukkit.getPluginManager().callEvent(new PlayerUnlockedAchievementEvent(this)));
    }

    /**
     * Removes the achievement from the player if he already unlocked it.
     * <p>The {@link #claimedGlobalRewards()} properties will stay untouched.
     */
    public void remove() {

        if (!isUnlocked()) return;

        unlocked(null).save();

        Bukkit.getScheduler().runTask(RCAchievements.instance(), () ->
                Bukkit.getPluginManager().callEvent(new PlayerLostAchievementEvent(this)));
    }
}
