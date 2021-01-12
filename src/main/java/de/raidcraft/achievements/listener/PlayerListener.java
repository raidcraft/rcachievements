package de.raidcraft.achievements.listener;

import de.raidcraft.achievements.Messages;
import de.raidcraft.achievements.RCAchievements;
import de.raidcraft.achievements.events.PlayerUnlockedAchievementEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerListener implements Listener {

    private final RCAchievements plugin;

    public PlayerListener(RCAchievements plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onAchievementUnlocked(PlayerUnlockedAchievementEvent event) {

        Messages.send(event.player(), Messages.achievementUnlockedSelf(event.playerAchievement()));
        Messages.send(event.player().id(), Messages.achievementUnlockedTitle(event.playerAchievement()));
    }
}
