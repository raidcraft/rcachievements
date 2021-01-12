package de.raidcraft.achievements.events;

import de.raidcraft.achievements.entities.PlayerAchievement;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * The unlock achievement event is fired before the player receives an achievement.
 * <p>The event can be {@link #setCancelled(boolean)} to stop giving the player the achievement.
 * <p>Use the {@link PlayerUnlockedAchievementEvent} if you want to react a successful achievement unlock.
 */
public class PlayerUnlockAchievementEvent extends PlayerAchievementEvent implements Cancellable {

    @Getter
    private static final HandlerList handlerList = new HandlerList();
    @Getter
    @Setter
    private boolean cancelled;

    public PlayerUnlockAchievementEvent(PlayerAchievement playerAchievement) {

        super(playerAchievement);
    }

    @Override
    public HandlerList getHandlers() {

        return handlerList;
    }
}
