package de.raidcraft.achievements;

import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.entities.DataStore;
import de.raidcraft.achievements.entities.PlayerAchievement;
import lombok.NonNull;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;

import java.util.UUID;

/**
 * The achievement context holds additional information for the instantiated achievement type.
 * <p>Use it to store persistent data, query applicable players for the achievement or
 * get information about the achievement itself.
 * <p>Every achievement type has its own achievement context that is created and loaded for each achievement.
 */
public interface AchievementContext {

    /**
     * Creates a new achievement context for the given achievement.
     *
     * @param plugin the plugin that created the context
     * @param achievement the achievement to create a context for
     * @return the created context
     */
    static AchievementContext create(RCAchievements plugin, Achievement achievement, AchievementType.Registration<?> registration) {

        return new DefaultAchievementContext(plugin, achievement, registration);
    }

    /**
     * @return the achievement that is referenced by this context
     */
    Achievement achievement();

    /**
     * Initializes this achievement context creating a new type instance,
     * loading and enabling it.
     */
    void initialize();

    /**
     * Enables this context, initializing it if not initialized
     * and then calls {@link AchievementType#enable()} on the implementing
     * achievement type.
     */
    void enable();

    /**
     * Disables the achievement context and the underlying
     * achievement type if it is enabled.
     */
    void disable();

    /**
     * Reloads the achievement context and the underlying achievement type.
     */
    void reload();

    /**
     * Clears the cache of the achievement context.
     */
    void clearCache();

    /**
     * The type is null until {@link #initialize()} is called and {@link #initialized()} returns true.
     *
     * @return the achievement type after {@link #initialize()} was called
     */
    AchievementType type();

    /**
     * Returns true if the context was initialized and created
     * an instance of the achievement type.
     * <p>{@link #type()} is null until the context is initialized
     *
     * @return true if the context was initialized
     */
    boolean initialized();

    /**
     * Returns true if the achievement type is enabled and actively listening on events.
     *
     * @return true if the context is enabled
     */
    boolean enabled();

    /**
     * Adds this achievement to the given player, unlocking it if not unlocked.
     * <p>Will do nothing and return the existing achievement if it is already unlocked.
     *
     * @param player the player to add the achievement to
     * @see PlayerAchievement#unlock()
     */
    void addTo(AchievementPlayer player);

    /**
     * Removes this achievement from the player if it is present.
     *
     * @param player the player to remove the achievement from
     */
    void removeFrom(AchievementPlayer player);

    /**
     * Gets an achievement player instance of the given bukkit player.
     *
     * @param player the bukkit player to get an achievement player instance from
     * @return the achievement player
     */
    default AchievementPlayer player(OfflinePlayer player) {

        return AchievementPlayer.of(player);
    }

    /**
     * Gets the persistent global meta data store for the achievement of this context.
     * <p>Generally you should use the player specific {@link #store(AchievementPlayer)} to
     * store meta data. Use this global store if tracking something across player, e.g. toplists, etc.
     *
     * @return the global meta store for this achievement context
     */
    default DataStore store() {

        return achievement().data();
    }

    /**
     * Gets the persistent data store for the given player and the achievement of this context.
     * <p>Use the data store to track persistent data for this achievement context and player.
     * <p>There is an additional {@link #store()} to store data global to this achievement.
     * <p>Make sure to call the {@link DataStore#save()} method after setting your data with {@link DataStore#set(String, Object)}.
     * <p>Will create a new {@link PlayerAchievement} if none exists yet.
     *
     * @param player the player to get the linked data store for this achievement
     * @return the data store of this achievement player combination
     */
    default DataStore store(@NonNull AchievementPlayer player) {

        return PlayerAchievement.of(achievement(), player).data();
    }

    /**
     * Checks if the given player can receive this achievement and should be checked.
     * <p>Players that already obtained the achievement or do not have the permission
     * are not applicable for obtaining the achievement.
     * <p>Important: make sure to check the applicability of players in bukkit events
     * before processing them inside your achievement type.
     *
     * @param uuid of the player to check. can be null.
     * @return true if the player is applicable and should be included in this achievement
     */
    boolean applicable(UUID uuid);

    /**
     * Checks if the given player can receive this achievement and should be checked.
     * <p>Players that already obtained the achievement or do not have the permission
     * are not applicable for obtaining the achievement.
     * <p>Important: make sure to check the applicability of players in bukkit events
     * before processing them inside your achievement type.
     *
     * @param uuid of the player to check. can be null.
     * @return true if the player is not applicable and should be excluded in this achievement
     */
    default boolean notApplicable(UUID uuid) {

        if (uuid == null) return true;

        return !applicable(uuid);
    }

    /**
     * Checks if the given block was placed by a player.
     *
     * @param block the block to check
     * @return true if the block was placed by a player
     */
    boolean isPlayerPlaced(Block block);
}
