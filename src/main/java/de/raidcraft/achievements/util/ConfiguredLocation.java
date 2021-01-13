package de.raidcraft.achievements.util;

import lombok.Data;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

/**
 * A {@link ConfiguredLocation} is a wrapper around the standard {@link Location}
 * with an additional {@link ConfiguredLocation#getRadius()}.
 *
 * Use the static methods to parse various inputs into a {@link ConfiguredLocation}.
 */
@Data
public class ConfiguredLocation implements Cloneable {

    private final Location location;
    private final int radius;

    protected ConfiguredLocation(Location location) {
        this.location = location;
        this.radius = 0;
    }

    protected ConfiguredLocation(Location location, int radius) {
        this.location = location;
        this.radius = radius;
    }

    protected ConfiguredLocation(ConfigurationSection config) {
        this.location = new Location(
                LocationUtil.getWorld(config.getString("world", "world")),
                config.getDouble("x"),
                config.getDouble("y"),
                config.getDouble("z"),
                (float) config.getDouble("yaw", 0),
                (float) config.getDouble("pitch", 0)
        );
        this.radius = config.getInt("radius", 0);
    }

    @Override
    protected ConfiguredLocation clone() {
        return new ConfiguredLocation(getLocation().clone(), getRadius());
    }

    public boolean isBlockEquals(Location location) {
        return location.getWorld().equals(getLocation().getWorld())
                && location.getBlockX() == getLocation().getBlockX()
                && location.getBlockY() == getLocation().getBlockY()
                && location.getBlockZ() == getLocation().getBlockZ();
    }

    public boolean isInRange(Location location) {
        if (getRadius() > 0) {
            return LocationUtil.isWithinRadius(location, getLocation(), getRadius());
        }
        return isBlockEquals(location);
    }
}