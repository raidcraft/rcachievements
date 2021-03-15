package de.raidcraft.achievements.entities;

import de.raidcraft.achievements.TestBase;
import org.assertj.core.util.Arrays;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

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

        @Test
        @DisplayName("should load reward list from config")
        void shouldLoadRewardsFromConfig() {

            MemoryConfiguration cfg = new MemoryConfiguration();
            ArrayList<String> rewards = new ArrayList<>();
            rewards.add("foobar");
            cfg.set("rewards", rewards);
            Achievement achievement = Achievement.load("foobar", cfg);

            assertThat(Achievement.byId(achievement.id()))
                    .isPresent().get()
                    .extracting(Achievement::rewards)
                    .isEqualTo(rewards);
        }
    }

    @Nested
    @DisplayName("isParent(...)")
    class isParent {

        private Achievement parent;

        @BeforeEach
        void setUp() {

            parent = loadAchievement("parent").get();
        }

        @Test
        @DisplayName("should not check non child achievements")
        void shouldNotCheckNonChildAchievements() {

            assertThat(parent.isParentOf(loadAchievement())).isFalse();
        }

        @Test
        @DisplayName("should return true if direct parent")
        void shouldReturnTrueIfDirectParent() {

            Achievement achievement = loadAchievement();
            achievement.parent(parent).save();
            parent.refresh();

            assertThat(parent.isParentOf(achievement)).isTrue();
        }

        @Test
        @DisplayName("should return true if root parent")
        void shouldReturnTrueIfRootParent() {

            Achievement parent = loadAchievement().parent(this.parent);
            parent.save();
            Achievement achievement = loadAchievement().parent(parent);
            achievement.save();
            this.parent.refresh();

            assertThat(this.parent.isParentOf(achievement)).isTrue();
        }

        @Test
        @DisplayName("should not return true if is child of parent")
        void shouldNotReturnTrueIfIsChild() {

            Achievement achievement = loadAchievement();
            parent.parent(achievement).save();
            parent.refresh();

            assertThat(parent.isParentOf(achievement)).isFalse();
        }
    }

    @Nested
    @DisplayName("data()")
    class DataStoreTest {

        @Test
        @DisplayName("should create and new data store if store is null")
        void shouldCreateNewDataIfNull() {

            Achievement achievement = loadAchievement();
            achievement.data(null).save();

            assertThat(achievement.data()).isNotNull();
        }

        @Test
        @DisplayName("should persist new data store in player achievement")
        void shouldPersistNewDataStore() {

            Achievement achievement = loadAchievement();
            achievement.data(null).save();

            achievement.data().set("test", 2).save();

            achievement.refresh();
            DataStore data = achievement.data();

            assertThat(data.get("test", Long.class))
                    .isNotEmpty()
                    .get()
                    .isEqualTo(2L);
        }
    }
}