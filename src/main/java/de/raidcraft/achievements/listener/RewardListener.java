package de.raidcraft.achievements.listener;

import de.raidcraft.achievements.RCAchievements;
import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.entities.PlayerAchievement;
import de.raidcraft.achievements.events.PlayerUnlockedAchievementEvent;
import io.artframework.ArtContext;
import io.artframework.ParseException;
import io.artframework.Scope;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Log(topic = "RCAchievements")
public class RewardListener implements Listener {

    private final RCAchievements plugin;
    private final Scope scope;
    private final Map<UUID, ArtContext> rewards = new HashMap<>();
    private final Map<UUID, ArtContext> globalRewards = new HashMap<>();

    public RewardListener(RCAchievements plugin, Scope scope) {

        this.plugin = plugin;
        this.scope = scope;
    }

    public void reload() {

        rewards.clear();
        globalRewards.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onUnlocked(PlayerUnlockedAchievementEvent event) {

        Player player = event.player().offlinePlayer().getPlayer();
        checkRewards(player, event.achievement());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                AchievementPlayer.of(player)
                        .unlockedAchievements().stream()
                        .filter(achievement -> !achievement.claimedGlobalRewards() || !achievement.claimedRewards())
                        .forEach(achievement -> checkRewards(player, achievement.achievement()));
            }
        }, 400L);
    }

    private void checkRewards(Player player, Achievement achievement) {

        PlayerAchievement playerAchievement = PlayerAchievement.of(achievement, AchievementPlayer.of(player));
        if (!playerAchievement.claimedRewards()) {
            rewards.computeIfAbsent(achievement.id(),
                    uuid -> {
                        try {
                            ArtContext context = scope.load("rcachievement:rewards:" + achievement.alias() + ":" + uuid, achievement.rewards());
                            context.var("achievement", achievement.name())
                                    .var("achievement_alias", achievement.alias())
                                    .var("achievement_id", achievement.id().toString())
                                    .var("achievement_name", achievement.name());
                            return context;
                        } catch (ParseException e) {
                            log.severe("failed to parse rewards of " + achievement.alias() + " (" + uuid + "): " + e.getMessage());
                            return ArtContext.empty();
                        }
                    }
            ).execute(player);
            playerAchievement.claimedRewards(true);
        }

        if (!plugin.pluginConfig().getGlobalRewards().isEmpty() && !playerAchievement.claimedGlobalRewards()) {
            this.globalRewards.computeIfAbsent(achievement.id(), uuid -> {
                try {
                    ArtContext context = scope.load("rcachievement:global-rewards:" + achievement.alias() + ":" + uuid, plugin.pluginConfig().getGlobalRewards());
                    context.var("achievement", achievement.name())
                            .var("achievement_alias", achievement.alias())
                            .var("achievement_id", achievement.id().toString())
                            .var("achievement_name", achievement.name());
                    return context;
                } catch (ParseException e) {
                    log.severe("failed to parse global rewards of " + achievement.alias() + " (" + uuid + "): " + e.getMessage());
                    return ArtContext.empty();
                }
            }).execute(player);
            playerAchievement.claimedGlobalRewards(true);
        }

        playerAchievement.save();
    }
}
