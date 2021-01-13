package de.raidcraft.achievements.types;

import de.raidcraft.achievements.AbstractAchievementType;
import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.TypeFactory;
import de.raidcraft.achievements.util.ConfiguredLocation;
import de.raidcraft.achievements.util.LocationUtil;
import de.raidcraft.achievements.util.Locations;
import lombok.extern.java.Log;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Log(topic = "RCAchievements:location")
public class LocationAchievement extends AbstractAchievementType implements Listener {

    public static class Factory implements TypeFactory<LocationAchievement> {

        @Override
        public String identifier() {

            return "location";
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
