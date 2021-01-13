package de.raidcraft.achievements.util;

import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Provides access to aliased locations.
 */
public interface LocationProvider {

    /**
     * Tries to find an alias location with the given name.
     *
     * @param name of the location
     * @return {@link ConfiguredLocation} if alias was found
     */
    Optional<ConfiguredLocation> getLocation(String name);

    /**
     * Uses {@link #getLocation(String)} to find a location alias
     * but will also replace all undefined parameters with the current player position.
     *
     * @param name of the location
     * @param player to use as defaults for undefined parameters
     * @return {@link ConfiguredLocation} if alias was found with player defaults
     */
    Optional<ConfiguredLocation> getLocation(String name, Player player);

    /**
     * Checks if the given location exists in the location registry.
     *
     * @param name to check
     * @return true if location exists
     */
    boolean isLocation(String name);

    /**
     * Registers a new {@link ConfiguredLocation} with the given location alias,
     *
     * @param name of the location
     * @param location to register
     * @return false if a location with that name already exists
     */
    boolean registerLocation(String name, ConfiguredLocation location);

    /**
     * Removes the given location from the registry.
     *
     * @param name of the location to remove
     * @return false if no location with that name was found
     */
    boolean unregisterLocation(String name);
}