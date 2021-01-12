package de.raidcraft.achievements.events;

import de.raidcraft.achievements.entities.PlayerAchievement;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * The unlock achievement event is fired after the player received an achievement.
 * <p>Use the {@link PlayerUnlockAchievementEvent} if you want to influence or cancel the process.
 */
public class PlayerUnlockedAchievementEvent extends PlayerAchievementEvent implements Cancellable {

    @Getter
    private static final HandlerList handlerList = new HandlerList();
    @Getter
    @Setter
    private boolean cancelled;

    public PlayerUnlockedAchievementEvent(PlayerAchievement playerAchievement) {

        super(playerAchievement);
    }

    @Override
    public HandlerList getHandlers() {

        return handlerList;
    }
}
