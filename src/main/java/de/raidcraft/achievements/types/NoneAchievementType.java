package de.raidcraft.achievements.types;

import de.raidcraft.achievements.AbstractAchievementType;
import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.TypeFactory;

import static de.raidcraft.achievements.Constants.DEFAULT_TYPE;

public class NoneAchievementType extends AbstractAchievementType {

    public static class Factory implements TypeFactory<NoneAchievementType> {

        @Override
        public String identifier() {

            return DEFAULT_TYPE;
        }

        @Override
        public Class<NoneAchievementType> typeClass() {

            return NoneAchievementType.class;
        }

        @Override
        public NoneAchievementType create(AchievementContext context) {

            return new NoneAchievementType(context);
        }
    }

    protected NoneAchievementType(AchievementContext context) {

        super(context);
    }
}
