package de.raidcraft.achievements.entities;

import de.raidcraft.achievements.Constants;
import io.ebean.Finder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;
import org.bukkit.OfflinePlayer;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
public class AchievementPlayer extends BaseEntity {

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

    public boolean unlocked(Achievement achievement) {

        return PlayerAchievement.of(achievement, this).unlocked() != null;
    }
}
