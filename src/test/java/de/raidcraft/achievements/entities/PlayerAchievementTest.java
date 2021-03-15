package de.raidcraft.achievements.entities;

import de.raidcraft.achievements.TestBase;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerAchievementTest extends TestBase {

    @Nested
    @DisplayName("unlock()")
    class Unlock {

        @Test
        @DisplayName("should unlock player achievement if not unlocked")
        void shouldUnlockPlayerAchievementIfNotUnlocked() {

            PlayerAchievement achievement = PlayerAchievement.of(loadAchievement(), player());

            achievement.unlock();
            assertThat(achievement.unlocked())
                    .isNotNull()
                    .isCloseTo(Instant.now(), new TemporalUnitWithinOffset(10, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("data()")
    class DataStoreTest {

        @Test
        @DisplayName("should create and new data store if store is null")
        void shouldCreateNewDataIfNull() {

            PlayerAchievement achievement = PlayerAchievement.of(loadAchievement(), player());
            achievement.data(null).save();

            assertThat(achievement.data()).isNotNull();
        }

        @Test
        @DisplayName("should persist new data store in player achievement")
        void shouldPersistNewDataStore() {

            Achievement cfg = loadAchievement();
            PlayerAchievement achievement = PlayerAchievement.of(cfg, player());
            achievement.data(null).save();

            achievement.data().set("test", 2).save();

            DataStore data = PlayerAchievement.of(cfg, player()).data();
            assertThat(data.get("test", Long.class))
                    .isNotEmpty()
                    .get()
                    .isEqualTo(2L);
        }
    }
}