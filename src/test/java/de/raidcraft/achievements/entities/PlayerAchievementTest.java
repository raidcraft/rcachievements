package de.raidcraft.achievements.entities;

import de.raidcraft.achievements.TestBase;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
}