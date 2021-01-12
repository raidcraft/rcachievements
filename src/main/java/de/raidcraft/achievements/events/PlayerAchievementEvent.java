package de.raidcraft.achievements.events;

import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.entities.PlayerAchievement;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.event.HandlerList;

@Data
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public abstract class PlayerAchievementEvent extends RCAchievementsEvent {

    @Getter
    @Accessors
    private static final HandlerList handlerList = new HandlerList();

    /**
     * The player achievement that triggered this event.
     */
    private final PlayerAchievement playerAchievement;

    /**
     * @return the achievement associated with the event
     */
    public Achievement achievement() {

        return playerAchievement().achievement();
    }

    /**
     * @return the player associated with the event
     */
    public AchievementPlayer player() {

        return playerAchievement().player();
    }
}
