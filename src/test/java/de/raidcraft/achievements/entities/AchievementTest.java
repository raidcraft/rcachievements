package de.raidcraft.achievements.entities;

import de.raidcraft.achievements.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AchievementTest extends TestBase {

    @Nested
    @DisplayName("removeFrom(...)")
    class RemoveFrom {

        private Achievement achievement;

        @BeforeEach
        void setUp() {

            achievement = loadAchievement();
            achievement.addTo(player());
        }

        @Test
        @DisplayName("should remove existing achievement from player")
        void shouldRemoveExistingAchievementFromPlayer() {

            assertThat(PlayerAchievement.of(achievement, player()))
                    .extracting(PlayerAchievement::isUnlocked)
                    .isEqualTo(true);

            achievement.removeFrom(player());

            assertThat(PlayerAchievement.of(achievement, player()))
                    .extracting(PlayerAchievement::isUnlocked)
                    .isEqualTo(false);
        }
    }
}