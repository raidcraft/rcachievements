package de.raidcraft.achievements.util;

import lombok.extern.java.Log;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Log
public final class Locations {

    private static LocationProvider provider;
    private static final Map<String, ConfiguredLocation> queuedLocations = new HashMap<>();

    public static Optional<LocationProvider> getProvider() {
        return Optional.ofNullable(provider);
    }

    /**
     * Tries to parse the provided {@link ConfigurationSection} into a {@link ConfiguredLocation}.
     * The parsing occurs in the following order:
     *      1. See if there is a location:string and pass it on to {@link #fromString(String)}
     *      2. See if there is a location:ConfigurationSection and recurse to {@link #fromConfig(ConfigurationSection)}
     *      3. Check if x,y,z,world,yaw,pitch,radius are directly defined and parse a {@link Location} object.
     *      5. If nothing matches return {@link Optional#empty()}.
     *
     * @param config to parse
     * @return parsed {@link ConfiguredLocation} or {@link Optional#empty()}
     */
    public static Optional<ConfiguredLocation> fromConfig(ConfigurationSection config) {
        if (config == null) return Optional.empty();
        if (config.isString("location") || config.isString("x")) return fromString(config.getString("location", config.getString("x")));
        if (config.isConfigurationSection("location")) return fromConfig(config.getConfigurationSection("location"));
        if (config.isSet("x") && config.isSet("y") && config.isSet("z")) {
            World world = LocationUtil.getCaseInsensitiveWorld(config.getString("world"));
            if (world == null) return Optional.empty();
            return Optional.of(new ConfiguredLocation(config));
        }
        return Optional.empty();
    }

    /**
     * Will work the same as {@link #fromConfig(ConfigurationSection)} with the difference
     * that all unknown parameters will be replaced with the current player position.
     *
     * @see #fromConfig(ConfigurationSection)
     *
     * @param player to use as default parameters
     */
    public static Optional<ConfiguredLocation> fromConfig(ConfigurationSection config, Player player) {
        if (config == null) return Optional.empty();
        if (config.isString("location") || config.isString("x")) return fromString(config.getString("location", config.getString("x")), player);

        if (player != null) {
            String prefix = config.isConfigurationSection("location") ? "location." : "";
            if (!config.isSet(prefix + "world")) config.set(prefix + "world", player.getWorld().getName());
            if (!config.isSet(prefix + "x")) config.set(prefix + "x", player.getLocation().getX());
            if (!config.isSet(prefix + "y")) config.set(prefix + "y", player.getLocation().getY());
            if (!config.isSet(prefix + "z")) config.set(prefix + "z", player.getLocation().getZ());
            if (!config.isSet(prefix + "pitch")) config.set(prefix + "pitch", player.getLocation().getPitch());
            if (!config.isSet(prefix + "yaw")) config.set(prefix + "yaw", player.getLocation().getYaw());
        }

        return fromConfig(config);
    }

    /**
     * @see LocationProvider#getLocation(String)
     */
    public static Optional<ConfiguredLocation> fromString(String location) {
        return getProvider().flatMap(provider -> provider.getLocation(location));
    }

    /**
     * @see LocationProvider#getLocation(String, Player)
     */
    public static Optional<ConfiguredLocation> fromString(String location, Player player) {
        return getProvider().flatMap(provider -> provider.getLocation(location, player));
    }

    /**
     * Wraps the {@link Location} into a {@link ConfiguredLocation}.
     *
     * @param location to create {@link ConfiguredLocation} from
     * @return {@link ConfiguredLocation}
     */
    public static ConfiguredLocation fromLocation(Location location) {
        return new ConfiguredLocation(location);
    }

    /**
     * Wraps the given radius and {@link Location} into a {@link ConfiguredLocation}.
     *
     * @param location to wrap
     * @param radius to include
     * @return {@link ConfiguredLocation}
     */
    public static ConfiguredLocation fromLocation(Location location, int radius) {
        return new ConfiguredLocation(location, radius);
    }

    /**
     * @see LocationProvider#registerLocation(String, ConfiguredLocation)
     */
    public static boolean registerLocation(String name, ConfiguredLocation location) {
        if (!getProvider().isPresent()) {
            if (queuedLocations.containsKey(name)) return false;
            queuedLocations.put(name, location);
            return true;
        }
        return provider.registerLocation(name, location);
    }

    /**
     * Registers a new {@link LocationProvider} enabling the alias lookup feature
     * and loading all queued alias registrations into the provider.
     *
     * @param provider to register
     */
    public static void registerLocationProvider(LocationProvider provider) {

        if (getProvider().isPresent()) {
            log.warning("LocationProvider is already registered!");
            return;
        }
        Locations.provider = provider;
        queuedLocations.forEach(Locations::registerLocation);
        queuedLocations.clear();
    }

    private Locations() {}
}