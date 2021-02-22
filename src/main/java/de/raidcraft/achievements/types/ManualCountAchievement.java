package de.raidcraft.achievements.types;

import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.AchievementManager;
import de.raidcraft.achievements.RCAchievements;
import de.raidcraft.achievements.TypeFactory;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.events.AchievementCountChangedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ManualCountAchievement extends CountAchievement implements Listener {

    public static class Factory implements TypeFactory<ManualCountAchievement> {

        @Override
        public String identifier() {

            return "count";
        }

        @Override
        public Class<ManualCountAchievement> typeClass() {

            return ManualCountAchievement.class;
        }

        @Override
        public ManualCountAchievement create(AchievementContext context) {

            return new ManualCountAchievement(context);
        }
    }

    protected ManualCountAchievement(AchievementContext context) {

        super(context);
    }

    @Override
    public void increaseAndCheck(AchievementPlayer player, long amount) {

        allChildren()
                .forEach(countAchievement -> countAchievement.increaseAndCheck(player, amount));

        super.increaseAndCheck(player, amount);
    }

    @Override
    public long count(AchievementPlayer player, long count) {

        allChildren()
                .forEach(countAchievement -> countAchievement.count(player, count));

        return super.count(player, count);
    }

    private Collection<CountAchievement> allChildren() {

        AchievementManager achievementManager = RCAchievements.instance().achievementManager();
        return achievement().children().stream()
                .map(achievementManager::active)
                .flatMap(Optional::stream)
                .map(AchievementContext::type)
                .filter(type -> type instanceof CountAchievement)
                .map(type -> (CountAchievement) type)
                .collect(Collectors.toUnmodifiableList());
    }
}
