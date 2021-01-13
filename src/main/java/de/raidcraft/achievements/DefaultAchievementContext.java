package de.raidcraft.achievements;

import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Log(topic = "RCAchievements")
@Setter(AccessLevel.PACKAGE)
@Accessors(fluent = true)
public class DefaultAchievementContext implements AchievementContext {

    private final Plugin plugin;
    private final Achievement achievement;
    private final AchievementType.Registration<?> registration;
    private AchievementType type;
    private boolean initialized = false;
    private boolean loadFailed = false;
    private boolean enabled = false;

    private final Map<UUID, Boolean> applicableCheckCache = new HashMap<>();

    public DefaultAchievementContext(Plugin plugin, Achievement achievement, AchievementType.Registration<?> registration) {

        this.plugin = plugin;

        this.achievement = achievement;
        this.registration = registration;
    }

    @Override
    public void initialize() {

        if (initialized()) return;

        try {
            type = registration().create(this);
            load();
            initialized(true);
        } catch (Exception e) {
            log.severe("faield to initialize context of " + achievement().alias()
                    + " (" + achievement().id() + "): " + e.getMessage());
            e.printStackTrace();
            loadFailed(true);
        }
    }

    private void load() {

        if (type == null) {
            loadFailed(true);
            return;
        }

        try {
            loadFailed(!type.load(achievement().achievementConfig()));
        } catch (Exception e) {
            log.severe("failed to load " + achievement().alias() + " (" + achievement().id() + "): " + e.getMessage());
            e.printStackTrace();
            loadFailed(true);
        }

        if (loadFailed()) {
            log.severe("loading config of " + achievement().alias() + " failed. Achievement will not enable!");
        }
    }

    @Override
    public void enable() {

        if (enabled()) return;
        if (!initialized()) initialize();
        if (loadFailed()) return;

        try {
            type.enable();
            if (type instanceof Listener) {
                plugin.getServer().getPluginManager().registerEvents((Listener) type, plugin);
            }
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
            if (type instanceof Listener) {
                HandlerList.unregisterAll((Listener) type);
            }
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

    @Override
    public boolean applicable(AchievementPlayer player) {

        if (player == null) return false;

        return applicableCheckCache.computeIfAbsent(player.id(),
                uuid -> !player.unlocked(achievement())
        );
    }

    @Override
    public boolean addTo(AchievementPlayer player) {

        applicableCheckCache.remove(player.id());

        return achievement().addTo(player);
    }

    @Override
    public void removeFrom(AchievementPlayer player) {

        applicableCheckCache.remove(player.id());

        achievement().removeFrom(player);
    }
}
