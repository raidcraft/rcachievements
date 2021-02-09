package de.raidcraft.achievements.events;

import de.raidcraft.achievements.entities.PlayerAchievement;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * The PlayerUnlockedAchievementEvent is fired after the player received an achievement.
 */
public class PlayerUnlockedAchievementEvent extends PlayerAchievementEvent {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    public PlayerUnlockedAchievementEvent(PlayerAchievement playerAchievement) {

        super(playerAchievement);
    }

    @Override
    public HandlerList getHandlers() {

        return handlerList;
    }
}
