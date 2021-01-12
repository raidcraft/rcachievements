package de.raidcraft.achievements;

import de.raidcraft.achievements.entities.Achievement;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;

@Getter
@Log(topic = "RCAchievements")
@Setter(AccessLevel.PACKAGE)
@Accessors(fluent = true)
public class DefaultAchievementContext implements AchievementContext {

    private final Achievement achievement;
    private final AchievementType.Registration<?> registration;
    private AchievementType type;
    private boolean initialized = false;
    private boolean enabled = false;

    public DefaultAchievementContext(Achievement achievement, AchievementType.Registration<?> registration) {

        this.achievement = achievement;
        this.registration = registration;
    }

    @Override
    public void initialize() {

        if (initialized()) return;

        try {
            type = registration().create(this);
            type.load(achievement().achievementConfig());
            initialized(true);
        } catch (Exception e) {
            log.severe("faield to initialize context of " + achievement().alias()
                    + " (" + achievement().id() + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void enable() {

        if (enabled()) return;
        if (!initialized()) initialize();

        try {
            type.enable();
            enabled(true);
        } catch (Exception e) {
            log.severe("failed to call enable() on achievement " + achievement().alias()
                    + " (" + achievement().id() + "): " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void disable() {

        if (!enabled()) return;

        try {
            type.disable();
        } catch (Exception e) {
            log.severe("failed to call disable() on achievement " + achievement().alias()
                    + " (" + achievement().id() + "): " + e.getMessage());
            e.printStackTrace();
        }
        enabled(false);
    }

    @Override
    public void reload() {

        if (!enabled()) return;

        achievement().refresh();

        type.disable();
        type.load(achievement().achievementConfig());
        type.enable();
    }
}
