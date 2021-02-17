package de.raidcraft.achievements.listener;

import de.raidcraft.achievements.RCAchievements;
import de.raidcraft.achievements.entities.PlayerAchievement;
import de.raidcraft.achievements.events.PlayerUnlockedAchievementEvent;
import io.artframework.ArtContext;
import io.artframework.ParseException;
import io.artframework.Scope;
import lombok.extern.java.Log;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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

        if (event.achievement().rewards().isEmpty()) return;

        Player player = event.player().offlinePlayer().getPlayer();
        PlayerAchievement playerAchievement = event.playerAchievement();

        if (!playerAchievement.claimedRewards()) {
            rewards.computeIfAbsent(event.achievement().id(),
                    uuid -> {
                        try {
                            return scope.load("rcachievement:rewards:" + event.achievement().alias() + ":" + uuid, event.achievement().rewards());
                        } catch (ParseException e) {
                            log.severe("failed to parse rewards of " + event.achievement().alias() + " (" + uuid + "): " + e.getMessage());
                            return ArtContext.empty();
                        }
                    }
            ).execute(player);
            playerAchievement.claimedRewards(true);
        }


        if (!plugin.pluginConfig().getGlobalRewards().isEmpty()) {
            this.globalRewards.computeIfAbsent(event.achievement().id(), uuid -> {
                try {
                    return scope.load("rcachievement:global-rewards:" + event.achievement().alias() + ":" + uuid, plugin.pluginConfig().getGlobalRewards());
                } catch (ParseException e) {
                    log.severe("failed to parse global rewards of " + event.achievement().alias() + " (" + uuid + "): " + e.getMessage());
                    return ArtContext.empty();
                }
            }).execute(player);
            playerAchievement.claimedGlobalRewards(true);
        }

        playerAchievement.save();
    }
}
