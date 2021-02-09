package de.raidcraft.achievements.entities;

import de.raidcraft.achievements.Constants;
import io.ebean.Finder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The achievement player is the database link between the bukkit player
 * and the achievement plugin.
 * <p>It's id and name always match the bukkit representation of the player.
 */
@Entity
@Getter
@Setter
@Accessors(fluent = true)
@Table(name = Constants.TABLE_PREFIX + "players")
public class AchievementPlayer extends BaseEntity implements Comparable<AchievementPlayer> {

    public static final Finder<UUID, AchievementPlayer> find = new Finder<>(AchievementPlayer.class);

    /**
     * Gets or creates a new achievement player from the given offline player.
     * <p>The id will be the same as the players id.
     *
     * @param player the player to create or get the achievement player for
     * @return the created or existing achievement player
     */
    public static AchievementPlayer of(OfflinePlayer player) {

        return Optional.ofNullable(find.byId(player.getUniqueId()))
                .orElseGet(() -> {
                    AchievementPlayer achievementPlayer = new AchievementPlayer(player);
                    achievementPlayer.insert();
                    return achievementPlayer;
                });
    }

    /**
     * Tries to find an achievement player with the given id.
     * <p>The id is the same as the Minecraft's player id.
     * <p>Returns an empty optional if no player by the id is found.
     *
     * @param uuid the unique id of the player
     * @return the player or an empty optional
     */
    public static Optional<AchievementPlayer> byId(UUID uuid) {

        if (uuid == null) return Optional.empty();

        return Optional.ofNullable(find.byId(uuid));
    }

    /**
     * The name of the player.
     */
    @Setter(AccessLevel.PRIVATE)
    private String name;

    /**
     * A list of achievements the player unlocked or discovered.
     */
    @OneToMany(cascade = CascadeType.ALL)
    private List<PlayerAchievement> achievements = new ArrayList<>();

    AchievementPlayer(OfflinePlayer player) {

        this.id(player.getUniqueId());
        this.name(player.getName());
    }

    /**
     * @return the offline player of this achievement player
     */
    public OfflinePlayer offlinePlayer() {

        return Bukkit.getOfflinePlayer(id());
    }

    /**
     * @return the online player of this achievement player if it is online
     */
    public Optional<Player> bukkitPlayer() {

        return Optional.ofNullable(Bukkit.getPlayer(id()));
    }

    /**
     * @return a list of all unlocked achievements of this player
     */
    public List<PlayerAchievement> unlockedAchievements() {

        return achievements().stream()
                .filter(PlayerAchievement::isUnlocked)
                .collect(Collectors.toList());
    }

    /**
     * Adds the given achievement to the player unlocking it.
     *
     * @param achievement the achievement to add
     * @return true if the achievement was unlocked or is already unlocked.
     *         <p>false if the unlock failed, e.g. a cancelled event
     * @see Achievement#addTo(AchievementPlayer)
     */
    public boolean add(Achievement achievement) {

        return achievement.addTo(this);
    }

    /**
     * Removes the given achievement from this player.
     *
     * @param achievement the achievement to remove
     */
    public void remove(Achievement achievement) {

        achievement.removeFrom(this);
    }

    /**
     * Checks if the given achievement was unlocked by this player.
     * <p>Will create a new {@link PlayerAchievement} entry if none exists.
     *
     * @param achievement the achievement to check
     * @return true if the achievement is unlocked
     */
    public boolean unlocked(Achievement achievement) {

        return PlayerAchievement.of(achievement, this).unlocked() != null;
    }

    /**
     * Checks if the player can unlock the given achievement.
     * <p>The check will return false if the player already has the achievement
     * or does not have the permission to unlock it.
     *
     * @param achievement the achievement to check
     * @return true if the achievement can be unlocked
     */
    public boolean canUnlock(Achievement achievement) {

        if (unlocked(achievement)) return false;

        if (achievement.restricted()) {
            Player player = Bukkit.getPlayer(id());
            if (player == null) return false;
            return player.hasPermission(Constants.ACHIEVEMENT_PERMISSION_PREFIX + achievement.alias());
        }

        return true;
    }

    /**
     * Checks if this player can view this achievement.
     * <p>Will check if it is hidden, the player has the bypass permission
     * or unlocked the achievement.
     *
     * @param achievement the achievement to check
     * @return true if the player is allowed to view the achievement
     */
    public boolean canView(Achievement achievement) {

        if (!achievement.hidden()) return true;
        if (unlocked(achievement)) return true;

        Player player = Bukkit.getPlayer(id());
        if (player == null) return false;

        return player.hasPermission(Constants.SHOW_HIDDEN);
    }

    /**
     * Checks if the player is allowed to view details of the achievement.
     * <p>Details may be hidden if the achievement is secret and the player
     * has not unlocked it or does not have the permission to view secret achievements.
     *
     * @param achievement the achievement to check
     * @return true if the player is allowed to view details of the achievement
     */
    public boolean canViewDetails(Achievement achievement) {

        if (!achievement.secret() && !achievement.hidden()) return true;
        if (unlocked(achievement)) return true;

        Player player = Bukkit.getPlayer(id());
        if (player == null) return false;

        boolean canViewSecret = !achievement.secret() || player.hasPermission(Constants.SHOW_SECRET);

        return canViewSecret && canView(achievement);
    }

    @Override
    public int compareTo(AchievementPlayer o) {

        return Integer.compare(unlockedAchievements().size(), o.unlockedAchievements().size());
    }
}
