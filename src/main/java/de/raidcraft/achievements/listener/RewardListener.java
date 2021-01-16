package de.raidcraft.achievements.listener;

import de.raidcraft.achievements.events.PlayerUnlockedAchievementEvent;
import io.artframework.ArtContext;
import io.artframework.ParseException;
import io.artframework.Scope;
import lombok.extern.java.Log;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Log(topic = "RCAchievements")
public class RewardListener implements Listener {

    private final Scope scope;
    private final Map<UUID, ArtContext> rewards = new HashMap<>();

    public RewardListener(Scope scope) {

        this.scope = scope;
    }

    public void reload() {

        rewards.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onUnlocked(PlayerUnlockedAchievementEvent event) {

        if (event.achievement().rewards().isEmpty()) return;

        ArtContext context = rewards.computeIfAbsent(event.achievement().id(),
                uuid -> {
                    try {
                        return scope.load(uuid.toString(), event.achievement().rewards());
                    } catch (ParseException e) {
                        log.severe("failed to parse rewards of " + event.achievement().alias() + " (" + uuid + "): " + e.getMessage());
                        return ArtContext.empty();
                    }
                }
        );

        context.execute(event.player().offlinePlayer().getPlayer());
    }
}
