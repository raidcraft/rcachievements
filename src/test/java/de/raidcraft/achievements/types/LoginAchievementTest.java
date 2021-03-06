package de.raidcraft.achievements.types;

import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.TestBase;
import de.raidcraft.achievements.entities.DataStore;
import lombok.SneakyThrows;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit5.JMockitExtension;
import org.assertj.core.data.TemporalOffset;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.event.player.PlayerLoginEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(JMockitExtension.class)
class LoginAchievementTest extends TestBase {

    private DataStore store;
    private LoginAchievement achievement;
    private AchievementContext context;

    @SneakyThrows
    @BeforeEach
    protected void setUp() {

        super.setUp();

        store = new DataStore();

        context = mock(AchievementContext.class);
        when(context.store(player())).thenReturn(store);
        when(context.player(bukkitPlayer())).thenReturn(player());
        achievement = new LoginAchievement(context);
    }

    @Nested
    @DisplayName("onLogin()")
    class OnLogin {

        @SneakyThrows
        @Test
        @DisplayName("should increase count on first login")
        void shouldIncreaseCountOnFirstLogin() {

            MemoryConfiguration config = new MemoryConfiguration();
            config.set("count", 5);

            achievement.load(config);

            achievement.onLogin(new PlayerLoginEvent(bukkitPlayer(), "", InetAddress.getByName("localhost")));

            assertThat(achievement.count(player())).isEqualTo(1);
        }

        @SneakyThrows
        @Test
        @DisplayName("should reset count to one after failed streak")
        void shouldResetCountAfterFailedLogin() {

            MemoryConfiguration config = new MemoryConfiguration();
            config.set("count", 5);

            achievement.load(config);

            store.set(LoginAchievement.LAST_LOGIN, Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli());
            store.set(CountAchievement.COUNT_KEY, 4);

            achievement.onLogin(new PlayerLoginEvent(bukkitPlayer(), "", InetAddress.getByName("localhost")));

            assertThat(achievement.count(player())).isEqualTo(1);
            assertThat(store.get(LoginAchievement.LAST_LOGIN, Long.class).map(Instant::ofEpochMilli).get())
                    .isCloseTo(Instant.now(), new TemporalUnitWithinOffset(5, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("should count streak after login streak failed")
        void shouldCountStreakAfterFailedStreak() throws UnknownHostException {

            MemoryConfiguration config = new MemoryConfiguration();
            config.set("count", 5);

            achievement.load(config);

            store.set(LoginAchievement.LAST_LOGIN, Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli());
            store.set(CountAchievement.COUNT_KEY, 4);

            achievement.onLogin(new PlayerLoginEvent(bukkitPlayer(), "", InetAddress.getByName("localhost")));
            assertThat(achievement.count(player())).isEqualTo(1);

            Clock clock = Clock.fixed(Instant.now().plus(1, ChronoUnit.DAYS), ZoneId.of("UTC"));
            new MockUp<Instant>() {
                @Mock
                public Instant now() {
                    return Instant.now(clock);
                }
            };

            achievement.onLogin(new PlayerLoginEvent(bukkitPlayer(), "", InetAddress.getByName("localhost")));
            assertThat(achievement.count(player())).isEqualTo(2);
        }

        @Test
        @DisplayName("should give achievement to player when count is reached")
        void shouldGiveAchievementWhenCountIsMet() throws UnknownHostException {

            MemoryConfiguration config = new MemoryConfiguration();
            config.set("count", 1);

            achievement.load(config);

            achievement.onLogin(new PlayerLoginEvent(bukkitPlayer(), "", InetAddress.getByName("localhost")));

            verify(context, times(1)).addTo(player());
        }

        @Test
        @DisplayName("should only increase the counter once per day")
        void shouldOnlyIncreaseTheCounterOncePerDay() throws UnknownHostException {

            MemoryConfiguration config = new MemoryConfiguration();
            config.set("count", 5);

            achievement.load(config);

            achievement.onLogin(new PlayerLoginEvent(bukkitPlayer(), "", InetAddress.getByName("localhost")));
            achievement.onLogin(new PlayerLoginEvent(bukkitPlayer(), "", InetAddress.getByName("localhost")));
            achievement.onLogin(new PlayerLoginEvent(bukkitPlayer(), "", InetAddress.getByName("localhost")));

            assertThat(achievement.count(player())).isEqualTo(1);
        }

        @Test
        @DisplayName("should increase the counter if logging in two days in a row")
        void shouldIncreaseTheCounterIfLoggedInTwoDaysInARow() throws UnknownHostException {

            MemoryConfiguration config = new MemoryConfiguration();
            config.set("count", 2);

            achievement.load(config);

            achievement.onLogin(new PlayerLoginEvent(bukkitPlayer(), "", InetAddress.getByName("localhost")));
            assertThat(achievement.count(player())).isEqualTo(1);

            Clock clock = Clock.fixed(Instant.now().plus(1, ChronoUnit.DAYS), ZoneId.of("UTC"));
            new MockUp<Instant>() {
                @Mock
                public Instant now() {
                    return Instant.now(clock);
                }
            };

            achievement.onLogin(new PlayerLoginEvent(bukkitPlayer(), "", InetAddress.getByName("localhost")));

            assertThat(achievement.count(player())).isEqualTo(2);
            verify(context, times(1)).addTo(player());
        }

        @Test
        @DisplayName("should increase the counter if logging in three days in a row")
        void shouldIncreaseTheCounterIfLoggedInThreeDaysInARow() throws UnknownHostException {

            MemoryConfiguration config = new MemoryConfiguration();
            config.set("count", 5);

            achievement.load(config);

            achievement.onLogin(new PlayerLoginEvent(bukkitPlayer(), "", InetAddress.getByName("localhost")));
            assertThat(achievement.count(player())).isEqualTo(1);

            Clock clock = Clock.fixed(Instant.now().plus(1, ChronoUnit.DAYS), ZoneId.of("UTC"));
            new MockUp<Instant>() {
                @Mock
                public Instant now() {
                    return Instant.now(clock);
                }
            };

            achievement.onLogin(new PlayerLoginEvent(bukkitPlayer(), "", InetAddress.getByName("localhost")));

            assertThat(achievement.count(player())).isEqualTo(2);

            Clock newClock = Clock.fixed(Instant.now().plus(1, ChronoUnit.DAYS), ZoneId.of("UTC"));
            new MockUp<Instant>() {
                @Mock
                public Instant now() {
                    return Instant.now(newClock);
                }
            };

            achievement.onLogin(new PlayerLoginEvent(bukkitPlayer(), "", InetAddress.getByName("localhost")));

            assertThat(achievement.count(player())).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("today()")
    class Today {

        @Test
        @DisplayName("should check last login of player if cache is empty")
        void shouldCheckLastLoginOfPlayerIfCacheIsEmpty() {

            store.set(LoginAchievement.LAST_LOGIN, Instant.now().toEpochMilli()).save();

            assertThat(achievement.checkedToday(bukkitPlayer())).isTrue();
        }

        @Test
        @DisplayName("should return false if not logged in or checked today")
        void shouldReturnFalseIfNotCheckedToday() {

            assertThat(achievement.checkedToday(bukkitPlayer())).isFalse();
        }

        @Test
        @DisplayName("should return false if last login was yesterday")
        void shouldReturnFalseIfLastLoginWasYesterday() {

            store.set(LoginAchievement.LAST_LOGIN, Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli()).save();

            assertThat(achievement.checkedToday(bukkitPlayer())).isFalse();
        }
    }

    @Nested
    @DisplayName("streak()")
    class Streak {

        @Test
        @DisplayName("should have a streak on the first day")
        void shouldHaveAStreakOnTheFirstDay() {

            assertThat(achievement.streak(bukkitPlayer())).isTrue();
        }

        @Test
        @DisplayName("should have a streak on the second login day in a row")
        void shouldHaveStreakOnSecondDay() {

            store.set(LoginAchievement.LAST_LOGIN, Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli()).save();

            assertThat(achievement.streak(bukkitPlayer())).isTrue();
        }

        @Test
        @DisplayName("should reset streak on missed day")
        void shouldResetStreakOnMissedDay() {

            store.set(LoginAchievement.LAST_LOGIN, Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli()).save();

            assertThat(achievement.streak(bukkitPlayer())).isFalse();
        }
    }
}