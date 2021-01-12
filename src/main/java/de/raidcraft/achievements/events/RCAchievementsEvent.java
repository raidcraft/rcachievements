package de.raidcraft.achievements.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class RCAchievementsEvent extends Event {

    @Getter
    private static final HandlerList handlerList = new HandlerList();
}
