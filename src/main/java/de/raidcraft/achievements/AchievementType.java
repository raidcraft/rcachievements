package de.raidcraft.achievements;

import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.entities.DataStore;
import de.raidcraft.achievements.entities.PlayerAchievement;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;
import java.util.function.Function;

/**
 * Achievement types do the heavy lifting and process events and data
 * to decide if an achievement should be awarded to players.
 * <p>Every configured achievement gets its own type instance and passes
 * the config down to the {@link #load(ConfigurationSection)} method of this.
 * <p>If the class implementing {@code AchievementType} also implements {@link org.bukkit.event.Listener}
 * it will have its bukkit event listeners automatically registered and unregistered
 * when {@link #enable()} or {@link #disable()} is called.
 */
public interface AchievementType {

    /**
     * Every type gets its own context instance that holds additional methods
     * and information for processing the achievement.
     * <p>Use it to retrive the store, player, achievement or any other information required
     * to prcess your achievement type.
     *
     * @return the context that was created for the instance of this achievement type
     */
    AchievementContext context();

    /**
     * @return the achievement that is referenced by this type and context
     */
    default Achievement achievement() {

        return context().achievement();
    }

    /**
     * @return the unique id of the configured achievement
     * @see Achievement#id()
     */
    default UUID id() {

        return achievement().id();
    }

    /**
     * @return the unique alias of the achievement
     * @see Achievement#alias()
     */
    default String alias() {

        return achievement().alias();
    }

    /**
     * @return name of the achievement
     * @see Achievement#name()
     */
    default String name() {

        return achievement().name();
    }

    /**
     * Adds this achievement to the given player, unlocking it if not unlocked.
     * <p>Will do nothing and return the existing achievement if it is already unlocked.
     *
     * @param player the player to add the achievement to
     * @return true if the achievement was unlocked or is already unlocked
     *         false if the unlock failed, e.g. a cancelled event
     * @see AchievementContext#addTo(AchievementPlayer)
     */
    default boolean addTo(AchievementPlayer player) {

        return context().addTo(player);
    }

    /**
     * Removes this achievement from the player if it is present.
     *
     * @param player the player to remove the achievement from
     * @see AchievementContext#removeFrom(AchievementPlayer)
     */
    default void removeFrom(AchievementPlayer player) {

        context().removeFrom(player);
    }

    /**
     * Gets an achievement player instance of the given bukkit player.
     *
     * @param player the bukkit player to get an achievement player instance from
     * @return the achievement player
     */
    default AchievementPlayer player(OfflinePlayer player) {

        return context().player(player);
    }

    /**
     * Gets the persistent global meta data store for the achievement of this context.
     * <p>Generally you should use the player specific {@link #store(OfflinePlayer)} to
     * store meta data. Use this global store if tracking something across player, e.g. toplists, etc.
     *
     * @return the global meta store for this achievement context
     */
    default DataStore store() {

        return context().store();
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
    default DataStore store(OfflinePlayer player) {

        return context().store(player);
    }

    /**
     * Checks if the given player can receive this achievement and should be checked.
     * <p>Players that already obtained the achievement or do not have the permission
     * are not applicable for obtaining the achievement.
     * <p>Important: make sure to check the applicability of players in bukkit events
     * before processing them inside your achievement type.
     *
     * @param player the player to check
     * @return true if the player is applicable and should be included in this achievement
     */
    default boolean applicable(OfflinePlayer player) {

        return applicable(player);
    }

    /**
     * Checks if the given player can receive this achievement and should be checked.
     * <p>Players that already obtained the achievement or do not have the permission
     * are not applicable for obtaining the achievement.
     * <p>Important: make sure to check the applicability of players in bukkit events
     * before processing them inside your achievement type.
     *
     * @param player the player to check
     * @return true if the player is not applicable and should be excluded in this achievement
     */
    default boolean notApplicable(OfflinePlayer player) {

        return context().notApplicable(player);
    }

    /**
     * Load is called with the achievement config when the context and type is created.
     * <p>It will also be called on reload calls and should be atomic.
     *
     * @param config the config of the achievement
     */
    default void load(ConfigurationSection config) {}

    /**
     * Enable is called after the achievement was loaded and when this type is active.
     * <p>Use it to load cached data from the store and initialize any tasks.
     * <p>When the achievement is reloaded {@link #load(ConfigurationSection)}, {@link #disable()}
     * and {@code #enable()} will be called in this order.
     */
    default void enable() {}

    /**
     * Disable is called when the achievement is disabled.
     * <p>Use it to store your cache that should be persisted.
     * <p>When the achievement is reloaded {@link #load(ConfigurationSection)}, {@link #disable()}
     * and {@code #enable()} will be called in this order.
     */
    default void disable() {}

    /**
     * Every achievement that is registered with the {@link AchievementManager#register(String, Class, Function)}
     * or {@link AchievementManager#register(TypeFactory)} is stored as a registration
     * and referenced when creating new achievement types.
     *
     * @param <TType> the type of the achievement
     */
    @Value
    @Accessors(fluent = true)
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    class Registration<TType extends AchievementType> {

        String identifier;
        Class<TType> typeClass;
        Function<AchievementContext, TType> factory;

        /**
         * Creates a new instance of this registered type using the factory of this registration.
         *
         * @param context the context that is passed to the type instance
         * @return a fresh instance of the type
         */
        public TType create(AchievementContext context) {

            return factory.apply(context);
        }
    }
}
