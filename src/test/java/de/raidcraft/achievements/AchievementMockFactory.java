package de.raidcraft.achievements;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Stack;

import static org.mockito.Mockito.spy;

@Getter
@Accessors(fluent = true)
public class AchievementMockFactory implements TypeFactory<AchievementType> {

    public static final String TYPE = "mock";

    private final Stack<AchievementType> mocks = new Stack<>();

    public AchievementType last() {

        return mocks().peek();
    }

    @Override
    public String identifier() {

        return TYPE;
    }

    @Override
    public Class<AchievementType> typeClass() {

        return AchievementType.class;
    }

    @Override
    public AchievementType create(AchievementContext context) {

        AchievementType mock = spy(new MockType(context));
        mocks.push(mock);
        return mock;
    }

    public static class MockType extends AbstractAchievementType {

        protected MockType(AchievementContext context) {

            super(context);
        }
    }
}
