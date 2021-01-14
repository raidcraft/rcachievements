package de.raidcraft.achievements.types;

import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.TypeFactory;
import lombok.extern.java.Log;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashSet;
import java.util.Set;

@Log(topic = "RCAchievements:kill-entity")
public class MobKillAchievement extends CountAchievement implements Listener {

    public static class Factory implements TypeFactory<MobKillAchievement> {

        @Override
        public String identifier() {

            return "kill-entity";
        }

        @Override
        public Class<MobKillAchievement> typeClass() {

            return MobKillAchievement.class;
        }

        @Override
        public MobKillAchievement create(AchievementContext context) {

            return new MobKillAchievement(context);
        }
    }

    private final Set<EntityType> entities = new HashSet<>();

    protected MobKillAchievement(AchievementContext context) {

        super(context);
    }

    @Override
    public boolean load(ConfigurationSection config) {

        super.load(config);
        prefix(config.getString("suffix", "Mobs getötet"));

        entities.clear();

        for (String entity : config.getStringList("entities")) {
            try {
                entities.add(EntityType.valueOf(entity.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.severe("unknown entity type " + entity + " in config of: " + alias());
                e.printStackTrace();
            }
        }

        if (entities.isEmpty()) {
            log.severe("no entities configured in config of " + alias() + "!");
            return false;
        }

        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerKillEntity(EntityDeathEvent event) {

        Player killer = event.getEntity().getKiller();
        if (notApplicable(killer)) return;
        if (!entities.contains(event.getEntityType())) return;

        increaseAndCheck(player(killer));
    }
}
