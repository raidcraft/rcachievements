package de.raidcraft.achievements.types;

import de.raidcraft.achievements.AbstractAchievementType;
import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.TypeFactory;
import de.raidcraft.achievements.entities.AchievementPlayer;
import io.artframework.ART;
import io.artframework.ArtContext;
import io.artframework.ParseException;
import lombok.extern.java.Log;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

@Log(topic = "RCAchievements:art")
public class ArtAchievement extends AbstractAchievementType {

    public static class Factory implements TypeFactory<ArtAchievement> {

        @Override
        public String identifier() {

            return "art";
        }

        @Override
        public Class<ArtAchievement> typeClass() {

            return ArtAchievement.class;
        }

        @Override
        public ArtAchievement create(AchievementContext context) {

            return new ArtAchievement(context);
        }
    }

    private ArtContext trigger;
    private ArtContext requirements;

    protected ArtAchievement(AchievementContext context) {
        super(context);
    }

    @Override
    public boolean load(ConfigurationSection config) {

        try {
            trigger = ART.load("rcachievements:achievement:trigger:" + id(), config.getStringList("trigger"));
            trigger.onTrigger(Player.class, (target, context) -> {
                if (notApplicable(target.source())) return;
                if (requirements.test(target).success()) {
                    addTo(AchievementPlayer.of(target.source()));
                }
            });
        } catch (ParseException e) {
            log.severe("failed to load art trigger config of " + alias() + " (" + id() + ")");
            return false;
        }

        try {
            requirements = ART.load("rcachievements:achievement:requirement:" + id(), config.getStringList("requirements"));
        } catch (ParseException e) {
            log.severe("failed to load art requirements config of " + alias() + " (" + id() + ")");
            return false;
        }

        return true;
    }

    @Override
    public void enable() {

        trigger.enableTrigger();
    }

    @Override
    public void disable() {

        trigger.disableTrigger();
    }
}
