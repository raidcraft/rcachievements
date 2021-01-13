package de.raidcraft.achievements.types;

import de.raidcraft.achievements.AbstractAchievementType;
import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.entities.AchievementPlayer;
import io.ebean.annotation.Transactional;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Accessors(fluent = true)
public abstract class CountAchievement extends AbstractAchievementType {

    static final String COUNT_KEY = "count";

    protected CountAchievement(AchievementContext context) {

        super(context);
    }

    @Getter
    private int count = 1;

    private final Map<UUID, Long> countCache = new HashMap<>();

    @Override
    public boolean load(ConfigurationSection config) {

        count = config.getInt("count", 1);

        return true;
    }

    @Override
    public void enable() {

        countCache.clear();
    }

    @Override
    @Transactional
    public void disable() {

        countCache.keySet().stream()
                .map(AchievementPlayer.find::byId)
                .filter(Objects::nonNull)
                .forEach(this::save);
    }

    /**
     * Increases the counter for the player by one and then
     * checks if the player count is equal or greater then the configured count.
     *
     * @param player the player to check the counter for
     */
    protected void increaseAndCheck(AchievementPlayer player) {

        if (increase(player) >= count) {
            addTo(player);
        }
    }

    /**
     * Increases the counter for the player by the given amount and then
     * checks if the player count is equal or greater then the configured count.
     *
     * @param player the player to check the counter for
     * @param amount the amount for which the counter should be increased
     */
    protected void increaseAndCheck(AchievementPlayer player, long amount) {

        if (increase(player, amount) >= count) {
            addTo(player);
        }
    }

    /**
     * Checks the current player count and gives the player the achievement if successful.
     *
     * @param player the player to check the counter for
     */
    protected void check(AchievementPlayer player) {

        if (success(player)) {
            addTo(player);
        }
    }

    /**
     * Gets the current counter for the given player.
     *
     * @param player the player to get the counter for
     * @return true if the current player count is greater or equal to the required count
     */
    protected boolean success(AchievementPlayer player) {

        return count(player) >= count;
    }

    /**
     * Increases the counter of the player by one and returns
     * the new count.
     *
     * @param player the player to increase the counter for
     * @return the new count of the player. can be negative.
     */
    protected long increase(AchievementPlayer player) {

        return countCache.compute(player.id(), (uuid, integer) -> integer != null ? ++integer : 1L);
    }

    /**
     * Increases the count of the player by the given amount.
     * @param player the player to increase the counter for
     * @param amount the amount for which the counter is increased
     * @return the new count of the player. can be negative.
     */
    protected long increase(AchievementPlayer player, long amount) {

        return countCache.compute(player.id(), (uuid, aLong) -> aLong != null ? aLong + amount : amount);
    }

    /**
     * Decreases the counter of the player by one and returns
     * the new count.
     *
     * @param player the player to decrease the counter for
     * @return the new count of the player. can be negative.
     */
    protected long decrease(AchievementPlayer player) {

        return countCache.compute(player.id(), (uuid, integer) -> integer != null ? --integer : -1L);
    }

    /**
     * Gets the current counter of the given player.
     * <p>Performs a database request if the counter is not cached yet.
     *
     * @param player the player to get the counter for
     * @return the current player count
     */
    protected long count(@NonNull AchievementPlayer player) {

        return countCache.computeIfAbsent(player.id(),
                uuid -> store(player).get(COUNT_KEY, Long.class, 0L)
        );
    }

    /**
     * Sets the counter of the player to the given value.
     *
     * @param player the player to set the counter for
     * @param count the count value to set
     * @return the given count value
     */
    protected long count(AchievementPlayer player, long count) {

        return countCache.compute(player.id(), (uuid, integer) -> count);
    }

    /**
     * Stores the current cached counter in the database.
     *
     * @param player the player that should store its counter
     * @return the count value that was stored
     */
    protected long save(AchievementPlayer player) {

        long count = count(player);
        store(player).set(COUNT_KEY, count);
        return count;
    }
}
