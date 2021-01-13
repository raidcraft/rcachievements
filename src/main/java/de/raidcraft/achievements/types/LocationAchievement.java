package de.raidcraft.achievements.types;

import de.raidcraft.achievements.AbstractAchievementType;
import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.TypeFactory;
import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.util.ConfiguredLocation;
import de.raidcraft.achievements.util.LocationUtil;
import de.raidcraft.achievements.util.Locations;
import lombok.extern.java.Log;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Log(topic = "RCAchievements:location")
public class LocationAchievement extends AbstractAchievementType implements Listener {

    public static final String TYPE = "location";

    /**
     * Creates a new location achievement config from the given location with the radius.
     * <p>Use it to create a new Achievement with {@link Achievement#create(String, Consumer)}
     *
     * @param location the location that should be mapped to the config
     * @param radius the radius
     * @return the created configuration section
     */
    public static ConfigurationSection create(Location location, int radius) {

        MemoryConfiguration config = new MemoryConfiguration();
        config.set("x", location.getBlockX());
        config.set("y", location.getBlockY());
        config.set("z", location.getBlockZ());
        config.set("radius", radius);

        if (location.getWorld() != null) {
            config.set("world", location.getWorld().getUID().toString());
        }

        return config;
    }

    public static class Factory implements TypeFactory<LocationAchievement> {

        @Override
        public String identifier() {

            return TYPE;
        }

        @Override
        public Class<LocationAchievement> typeClass() {

            return LocationAchievement.class;
        }

        @Override
        public LocationAchievement create(AchievementContext context) {

            return new LocationAchievement(context);
        }
    }

    private ConfiguredLocation location;
    private final Map<UUID, Location> lastLocations = new HashMap<>();

    protected LocationAchievement(AchievementContext context) {

        super(context);
    }

    @Override
    public boolean load(ConfigurationSection config) {

        return Locations.fromConfig(config).map(configuredLocation -> {
            location = configuredLocation;
            return true;
        }).orElseGet(() -> {
            log.warning("cannot parse config of " + alias() + " into a valid location!");
            return false;
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {

        if (notApplicable(event.getPlayer())) return;
        if (!moved(event.getPlayer())) return;

        if (location.isInRange(event.getTo())) {
            addTo(player(event.getPlayer()));
            lastLocations.remove(event.getPlayer().getUniqueId());
        }
    }

    private boolean moved(Player player) {

        Location lastLocation = lastLocations.getOrDefault(player.getUniqueId(), player.getLocation());
        lastLocations.put(player.getUniqueId(), lastLocation);
        return !LocationUtil.isBlockEquals(lastLocation, player.getLocation());
    }
}
