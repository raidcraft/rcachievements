package de.raidcraft.achievements.entities;

import de.raidcraft.achievements.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SuppressWarnings("ALL")
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

    @Nested
    @DisplayName("create(...)")
    class Create {

        @Test
        @DisplayName("should throw if alias exists")
        void shouldThrowIfAliasExists() {

            loadAchievement("foobar");

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> Achievement.create("foobar", configurationSection -> {
                    }));
        }

        @Test
        @DisplayName("should store type config passed to builder")
        void shouldSetTypeConfig() {

            Achievement.create("foo", cfg -> cfg.set("foo", "bar")).save();

            assertThat(Achievement.byAlias("foo"))
                    .isPresent().get()
                    .extracting(Achievement::achievementConfig)
                    .extracting(configurationSection -> configurationSection.getString("foo"))
                    .isEqualTo("bar");
        }

        @Test
        @DisplayName("should set properties on achievement")
        void shouldSetProperties() {

            Achievement.create("bar", configurationSection -> {
            })
                    .name("Foobar")
                    .enabled(false)
                    .hidden(true)
                    .type("foobar")
                    .save();

            assertThat(Achievement.byAlias("bar"))
                    .isPresent().get()
                    .extracting(
                            Achievement::name,
                            Achievement::enabled,
                            Achievement::hidden,
                            Achievement::type
                    ).contains(
                    "Foobar",
                    false,
                    true,
                    "foobar"
            );
        }
    }
}