package de.raidcraft.achievements;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public abstract class AbstractAchievementType implements AchievementType {

    private final AchievementContext context;

    protected AbstractAchievementType(AchievementContext context) {

        this.context = context;
    }
}
