package de.raidcraft.achievements.types;

import de.raidcraft.achievements.AbstractAchievementType;
import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.TypeFactory;
import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.PlayerAchievement;
import de.raidcraft.achievements.events.PlayerUnlockedAchievementEvent;
import lombok.extern.java.Log;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;

@Log(topic = "RCAchievements:combined")
public class CombinedAchievement extends AbstractAchievementType implements Listener {

    public static class Factory implements TypeFactory<CombinedAchievement> {

        @Override
        public String identifier() {

            return "combined";
        }

        @Override
        public Class<CombinedAchievement> typeClass() {

            return CombinedAchievement.class;
        }

        @Override
        public CombinedAchievement create(AchievementContext context) {

            return new CombinedAchievement(context);
        }
    }

    private Set<Achievement> achievements = new HashSet<>();

    protected CombinedAchievement(AchievementContext context) {

        super(context);
    }

    @Override
    public boolean load(ConfigurationSection config) {

        for (String alias : config.getStringList("achievements")) {
            Achievement.byAlias(alias)
                    .filter(Achievement::enabled)
                    .ifPresentOrElse(achievements::add, () -> {
                        log.warning("no enabled achievement by alias \"" + alias + "\" found in config of: " + alias() + " (" + id() + ")");
                    });
        }

        if (achievements.isEmpty()) {
            log.severe("no achievements configured in combined achievement of "+ alias() + " (" + id() + ")");
            return false;
        }

        return super.load(config);
    }

    @EventHandler(ignoreCancelled = true)
    public void onAchievementUnlocked(PlayerUnlockedAchievementEvent event) {

        if (notApplicable(event.player())) return;

        if (event.player().unlockedAchievements()
                .stream().map(PlayerAchievement::achievement)
                .allMatch(achievement -> achievements.contains(achievement))) {
            addTo(event.player());
        }
    }
}