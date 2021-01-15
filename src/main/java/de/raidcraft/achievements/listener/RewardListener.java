package de.raidcraft.achievements.listener;

import de.raidcraft.achievements.events.PlayerUnlockedAchievementEvent;
import io.artframework.ArtContext;
import io.artframework.Scope;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
                uuid -> scope.load(event.achievement().rewards())
        );

        context.execute(event.player().offlinePlayer().getPlayer());
    }
}
