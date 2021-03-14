package de.raidcraft.achievements;

import com.google.common.collect.ImmutableList;
import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.entities.DataStore;
import de.raidcraft.achievements.entities.PlayerAchievement;
import io.ebean.Model;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

@Getter
@Log(topic = "RCAchievements")
@Setter(AccessLevel.PACKAGE)
@Accessors(fluent = true)
public class DefaultAchievementContext implements AchievementContext {

    private final RCAchievements plugin;
    private final Achievement achievement;
    private final AchievementType.Registration<?> registration;
    private AchievementType type;
    private boolean initialized = false;
    private boolean loadFailed = false;
    private boolean enabled = false;
    private BukkitTask tickTask;

    private final Map<UUID, Boolean> applicableCheckCache = Collections.synchronizedMap(new HashMap<>());
    private final Map<UUID, DataStore> playerDataCache = Collections.synchronizedMap(new HashMap<>());

    public DefaultAchievementContext(RCAchievements plugin, Achievement achievement, AchievementType.Registration<?> registration) {

        this.plugin = plugin;

        this.achievement = achievement;
        this.registration = registration;
    }

    @Override
    public void initialize() {

        if (initialized()) return;

        try {
            type = registration().create(this);
            initialized(true);
            load();
        } catch (Exception e) {
            log.severe("faield to initialize context of " + achievement().alias()
                    + " (" + achievement().id() + "): " + e.getMessage());
            e.printStackTrace();
            loadFailed(true);
        }
    }

    private void load() {

        if (!initialized()) initialize();
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
            updateEventListener(true);
            enabled(true);
            if (type instanceof Periodic) {
                Periodic periodic = (Periodic) this.type;
                Bukkit.getScheduler().runTaskTimer(plugin(), () ->
                        Bukkit.getOnlinePlayers().forEach(periodic::tick),
                        plugin().pluginConfig().getPeriodicAchievementInterval(),
                        plugin().pluginConfig().getPeriodicAchievementInterval()
                );
            } else if (type instanceof PeriodicAsync) {
                PeriodicAsync periodic = (PeriodicAsync) this.type;
                Bukkit.getScheduler().runTaskTimer(plugin(), () ->
                        {
                            ImmutableList<? extends Player> players = ImmutableList.copyOf(Bukkit.getOnlinePlayers());
                            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> players.stream()
                                    .filter(OfflinePlayer::isOnline)
                                    .forEach(periodic::tickAsync)
                            );
                        },
                        plugin().pluginConfig().getPeriodicAchievementInterval(),
                        plugin().pluginConfig().getPeriodicAchievementInterval()
                );
            }
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
            updateEventListener(false);
            type.disable();
            synchronized (playerDataCache) {
                playerDataCache.values().forEach(Model::save);
            }
        } catch (Exception e) {
            log.severe("failed to call disable() on achievement " + achievement().alias()
                    + " (" + achievement().id() + "): " + e.getMessage());
            e.printStackTrace();
        }
        enabled(false);
    }

    @Override
    public void reload() {

        achievement().refresh();
        disable();

        if (achievement.enabled()) {
            clearCache();
            load();
            enable();
        }
    }

    @Override
    public void clearCache() {

        applicableCheckCache.clear();
        synchronized (playerDataCache) {
            playerDataCache.values().forEach(Model::save);
            playerDataCache.clear();
        }
    }

    @Override
    public DataStore store(@NonNull AchievementPlayer player) {

        synchronized (playerDataCache) {
            return playerDataCache.computeIfAbsent(player.id(), uuid -> PlayerAchievement.of(achievement(), player).data());
        }
    }

    @Override
    public boolean applicable(UUID uuid) {

        if (uuid == null) return false;

        synchronized (applicableCheckCache) {
            boolean check = applicableCheckCache.computeIfAbsent(uuid,
                    id -> AchievementPlayer.byId(id)
                            .map(player -> player.canUnlock(achievement()))
                            .orElse(false)
            );
            if (!check || achievement.worlds().isEmpty()) return check;

            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                return achievement.worlds().contains(player.getWorld().getName());
            }

            return true;
        }
    }

    @Override
    public boolean isPlayerPlaced(Block block) {

        return plugin.blockTracker().isPlayerPlacedBlock(block);
    }

    @Override
    public void addTo(AchievementPlayer player) {

        synchronized (applicableCheckCache) {
            applicableCheckCache.remove(player.id());
        }

        achievement().addTo(player);
    }

    @Override
    public void removeFrom(AchievementPlayer player) {

        synchronized (applicableCheckCache) {
            applicableCheckCache.remove(player.id());
        }

        achievement().removeFrom(player);
    }

    private void updateEventListener(boolean register) {

        if (type instanceof Listener) {
            if (register) {
                plugin.getServer().getPluginManager().registerEvents((Listener) type, plugin);
            } else {
                HandlerList.unregisterAll((Listener) type);
            }
        }
    }
}
