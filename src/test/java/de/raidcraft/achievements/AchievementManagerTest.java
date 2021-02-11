package de.raidcraft.achievements;

import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.types.NoneAchievementType;
import lombok.SneakyThrows;
import net.silthus.ebean.BaseEntity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static de.raidcraft.achievements.AchievementMockFactory.TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings("ALL")
class AchievementManagerTest extends TestBase {

    @Nested
    @DisplayName("register(...)")
    class TypeRegistration {

        @SneakyThrows
        @Test
        @DisplayName("should load achievements that failed to load after type registration")
        void shouldLoadFailedAchievementsAfterRegistration() {

            manager.types().clear();

            assertThat(loadAchievement("test")).isEmpty();
            assertThat(manager.failedLoads()).containsKey(TYPE);

            AchievementMockFactory factory = new AchievementMockFactory();
            manager.register(factory);

            assertThat(manager.failedLoads()).isEmpty();
            assertThat(manager.active("test")).isPresent();
            assertThat(factory.last()).isNotNull()
                    .extracting(AchievementType::alias)
                    .isEqualTo("test");

            verify(factory.last(), times(1)).enable();
        }

        @Test
        @DisplayName("should throw if type is duplicate")
        void shouldThrowIfTypeIsDuplicate() {

            assertThatExceptionOfType(TypeRegistrationException.class)
                    .isThrownBy(() -> manager.register("mock", NoneAchievementType.class, context -> mock(NoneAchievementType.class)));
        }
    }

    @Nested
    @DisplayName("load(...)")
    class AchievementLoading {

        @Test
        @DisplayName("should use id from config to load achievement")
        void shouldUseIdFromConfigToLoadAchievement() {

            ConfigurationSection cfg = new MemoryConfiguration();
            UUID id = UUID.randomUUID();
            cfg.set("id", id.toString());
            cfg.set("type", "mock");
            cfg.set("name", "Foo Bar");

            assertThat(manager.loadAchievement("foobar", cfg))
                    .isPresent().get()
                    .extracting(BaseEntity::id, Achievement::alias, Achievement::name)
                    .contains(id, "foobar", "Foo Bar");
        }

        @Test
        @DisplayName("should generate and set new random id if none is set")
        void shouldGenerateNewRandomId() {

            ConfigurationSection cfg = new MemoryConfiguration();
            cfg.set("name", "Foo Bar");
            cfg.set("type", "mock");

            assertThat(manager.loadAchievement("foobar", cfg))
                    .isPresent().get()
                    .extracting(Achievement::alias, Achievement::name)
                    .contains("foobar", "Foo Bar");
            assertThat(cfg.isSet("id")).isTrue();
        }

        @Test
        @DisplayName("should update existing achievement with same id")
        void shouldUpdateExistingWithSameId() {

            ConfigurationSection cfg = new MemoryConfiguration();
            UUID id = UUID.randomUUID();
            cfg.set("id", id.toString());
            cfg.set("type", "mock");
            cfg.set("name", "Foo Bar");

            assertThat(manager.loadAchievement("foobar", cfg))
                    .isPresent().get()
                    .extracting(BaseEntity::id, Achievement::alias, Achievement::name, Achievement::enabled)
                    .contains(id, "foobar", "Foo Bar", true);

            cfg.set("name", "Bar");
            cfg.set("enabled", false);

            assertThat(manager.loadAchievement("foobar", cfg))
                    .isPresent().get()
                    .extracting(BaseEntity::id, Achievement::alias, Achievement::name, Achievement::enabled)
                    .contains(id, "foobar", "Bar", false);
            assertThat(Achievement.find.all()).hasSize(1);
        }

        @Test
        @DisplayName("should update existing achievement with same alias")
        void shouldUpdateExistingWithSameAlias() {

            ConfigurationSection cfg = new MemoryConfiguration();
            cfg.set("name", "Foo Bar");
            cfg.set("type", "mock");

            assertThat(manager.loadAchievement("foobar", cfg))
                    .isPresent().get()
                    .extracting(Achievement::alias, Achievement::name, Achievement::hidden)
                    .contains("foobar", "Foo Bar", false);

            cfg = new MemoryConfiguration();
            cfg.set("name", "Bar");
            cfg.set("type", "mock");
            cfg.set("hidden", true);

            assertThat(manager.loadAchievement("foobar", cfg))
                    .isPresent().get()
                    .extracting(Achievement::alias, Achievement::name, Achievement::hidden)
                    .contains("foobar", "Bar", true);
            assertThat(Achievement.find.all()).hasSize(1);
        }

        @Test
        @DisplayName("should fail if id is set and duplicate alias exists")
        void shouldFailIfIdIsSetAndDuplicateAliasExists() {

            ConfigurationSection cfg = new MemoryConfiguration();
            cfg.set("name", "Foo Bar");
            cfg.set("type", "mock");

            assertThat(manager.loadAchievement("foobar", cfg))
                    .isPresent().get()
                    .extracting(Achievement::alias, Achievement::name, Achievement::hidden)
                    .contains("foobar", "Foo Bar", false);

            cfg = new MemoryConfiguration();
            cfg.set("id", UUID.randomUUID().toString());
            cfg.set("name", "Bar");
            cfg.set("type", "mock");
            cfg.set("hidden", true);

            assertThat(manager.loadAchievement("foobar", cfg))
                    .isEmpty();
            assertThat(Achievement.byAlias("foobar"))
                    .isPresent().get()
                    .extracting(Achievement::alias, Achievement::name, Achievement::hidden)
                    .contains("foobar", "Foo Bar", false);
            assertThat(Achievement.find.all()).hasSize(1);
        }

        @Test
        @DisplayName("should activate achievements on load")
        void shouldActivateAchievementsOnLoad() {

            Achievement achievement = Achievement.create("load-test",
                    configurationSection -> {
            }).type(TYPE);
            achievement.save();

            manager.load();

            assertThat(manager.active(achievement))
                    .isPresent().get()
                    .extracting(AchievementContext::enabled)
                    .isEqualTo(true);

            verify(factory().last(), times(1)).enable();
        }

        @Test
        @DisplayName("should load nested child achievements")
        void shouldLoadNestedChildAchievements() {

            ConfigurationSection cfg = new MemoryConfiguration();
            UUID id = UUID.randomUUID();
            cfg.set("id", id.toString());
            cfg.set("type", "mock");
            cfg.set("name", "Foo Bar");
            cfg.set("childs.child1.name", "Child 1");

            Optional<Achievement> foobar = manager.loadAchievement("foobar", cfg);
            foobar.get().refresh();
            assertThat(foobar)
                    .isPresent().get()
                    .extracting(BaseEntity::id, Achievement::alias, Achievement::name, Achievement::isParent)
                    .contains(id, "foobar", "Foo Bar", true);

            assertThat(foobar.get().children())
                    .hasSize(1)
                    .first()
                    .extracting(Achievement::alias, Achievement::name, Achievement::type, Achievement::isChild)
                    .contains("foobar:child1", "Child 1", "mock", true);
        }

        @Test
        @DisplayName("should activate nested child achievements on load")
        void shouldActivateNestedChildAchievements() {

            ConfigurationSection cfg = new MemoryConfiguration();
            UUID id = UUID.randomUUID();
            cfg.set("id", id.toString());
            cfg.set("type", "mock");
            cfg.set("name", "Foo Bar");
            cfg.set("childs.child1.name", "Child 1");
            cfg.set("childs.child1.alias", "child");

            Optional<Achievement> foobar = manager.loadAchievement("foobar", cfg);
            assertThat(foobar).isPresent();

            manager.initialize(foobar.get());

            assertThat(factory().mocks())
                    .hasSize(2);

            assertThat(manager.active("child"))
                    .isPresent().get()
                    .extracting(AchievementContext::enabled)
                    .isEqualTo(true);
        }

        @Test
        @DisplayName("should use nested key as alias")
        void shouldUseNestedKeyAsAlias() {

            ConfigurationSection cfg = new MemoryConfiguration();
            UUID id = UUID.randomUUID();
            cfg.set("id", id.toString());
            cfg.set("type", "mock");
            cfg.set("name", "Foo Bar");
            cfg.set("childs.child1.name", "Child 1");

            Optional<Achievement> foobar = manager.loadAchievement("foobar", cfg);

            foobar.get().refresh();
            assertThat(foobar.get().children())
                    .hasSize(1)
                    .first()
                    .extracting(Achievement::alias)
                    .isEqualTo("foobar:child1");
        }

        @Test
        @DisplayName("should use explicit child alias as alias")
        void shouldUseExplicitAlias() {

            ConfigurationSection cfg = new MemoryConfiguration();
            UUID id = UUID.randomUUID();
            cfg.set("id", id.toString());
            cfg.set("type", "mock");
            cfg.set("name", "Foo Bar");
            cfg.set("childs.child1.name", "Child 1");
            cfg.set("childs.child1.alias", "bar");

            Optional<Achievement> foobar = manager.loadAchievement("foobar", cfg);

            foobar.get().refresh();
            assertThat(foobar.get().children())
                    .hasSize(1)
                    .first()
                    .extracting(Achievement::alias)
                    .isEqualTo("bar");
        }
    }
}