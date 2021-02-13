package de.raidcraft.achievements;

import be.seeseemelk.mockbukkit.WorldMock;
import de.raidcraft.achievements.entities.Achievement;
import org.bukkit.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DefaultAchievementContextTest extends TestBase {

    private Achievement achievement;
    private DefaultAchievementContext context;

    @Override
    @BeforeEach
    protected void setUp() {

        super.setUp();

        achievement = loadAchievement();
        context = context(achievement);
    }

    @Nested
    @DisplayName("enable()")
    class Enable {

        @Test
        @DisplayName("should register bukkit listener if implemented")
        void shouldRegisterBukkitListenerIfImplemented() {

            context.enable();

            assertThat(context.initialized()).isTrue();
            assertThat(context.enabled()).isTrue();

            server().addPlayer();

            verify(factory().last(), times(1)).onPlayerJoin(any());
        }

        @Test
        @DisplayName("should call enable() in type")
        void shouldCallEnableInType() {

            context.enable();

            verify(factory().last(), times(1)).enable();
        }

        @Test
        @DisplayName("should not enable if already enabled")
        void shouldNotCallEnableTwice() {

            context.enable();
            context.enable();

            verify(factory().last(), times(1)).enable();
        }

        @Test
        @DisplayName("should initialize if not initialized")
        void shouldInitializeIfNotInitialized() {

            assertThat(context.initialized()).isFalse();

            context.enable();

            assertThat(context.initialized()).isTrue();
            verify(factory().last(), times(1)).load(any());
        }

        @Test
        @DisplayName("should not initialize if already initialized")
        void shouldNotInitializeIfInitialized() {

            context.initialize();

            assertThat(context.initialized()).isTrue();

            context.enable();

            verify(factory().last(), times(1)).load(any());
        }

        @Test
        @DisplayName("should not enable if loading failed")
        void shouldNotEnableIfLoadingFailed() {

            context.initialize();
            context.loadFailed(true);

            context.enable();

            verify(factory().last(), times(1)).load(any());
            verify(factory().last(), never()).enable();
        }
    }

    @Nested
    @DisplayName("disable()")
    class Disable {

        @BeforeEach
        void setUp() {

            context.enable();
        }

        @Test
        @DisplayName("should unregister bukkit listener")
        void shouldUnregisterBukkitListener() {

            assertThat(context.initialized()).isTrue();
            assertThat(context.enabled()).isTrue();

            context.disable();
            server().addPlayer();

            assertThat(context.initialized()).isTrue();
            assertThat(context.enabled()).isFalse();

            verify(factory().last(), never()).onPlayerJoin(any());
        }

        @Test
        @DisplayName("should call disable() on implemented type")
        void shouldCallDisable() {

            context.disable();

            verify(factory().last(), times(1)).disable();
        }

        @Test
        @DisplayName("should not call disable() if not enabled")
        void shouldNotCallDisableIfNotEnabled() {

            context.disable();
            context.disable();

            verify(factory().last(), times(1)).disable();
        }
    }

    @Nested
    @DisplayName("applicable(...)")
    class ApplicableCheck {

        @Test
        @DisplayName("should not allow unlocking achievement in different world")
        void shouldNotAllowUnlockingAchievementsInDifferentWorld() {

            achievement = loadAchievement().worlds(Collections.singletonList("foobar"));
            context = context(achievement);

            assertThat(context.applicable(bukkitPlayer.getUniqueId())).isFalse();
        }

        @Test
        @DisplayName("should allow unlocking achievement if player is in world")
        void shouldAllowUnlockingAchievementIfPlayerIsInWorld() {

            achievement = loadAchievement().worlds(Collections.singletonList("foobar"));
            WorldMock foobar = server().addSimpleWorld("foobar");
            bukkitPlayer().setLocation(new Location(foobar, 0, 64, 0));

            assertThat(context.applicable(bukkitPlayer().getUniqueId())).isTrue();
        }

    }
}