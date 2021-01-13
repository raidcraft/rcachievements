package de.raidcraft.achievements;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.java.Log;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Stack;

import static org.mockito.Mockito.spy;

@Getter
@Log
@Accessors(fluent = true)
public class AchievementMockFactory implements TypeFactory<AchievementType> {

    public static final String TYPE = "mock";

    private final Stack<MockType> mocks = new Stack<>();

    public MockType last() {

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

        MockType mock = spy(new MockType(context));
        mocks.push(mock);
        return mock;
    }

    public static class MockType extends AbstractAchievementType implements Listener {

        public MockType(AchievementContext context) {

            super(context);
        }

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {

            log.info("called onPlayerJoin(...)");
        }
    }
}
