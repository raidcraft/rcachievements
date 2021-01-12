package de.raidcraft.achievements;

public interface TypeFactory<TType extends AchievementType> {

    String identifier();

    Class<TType> typeClass();

    TType create(AchievementContext context);
}
