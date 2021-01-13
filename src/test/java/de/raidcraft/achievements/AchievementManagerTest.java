package de.raidcraft.achievements;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.types.NoneAchievementType;
import lombok.SneakyThrows;
import net.silthus.ebean.BaseEntity;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static de.raidcraft.achievements.AchievementMockFactory.TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings("ALL")
class AchievementManagerTest {

    private ServerMock server;
    private RCAchievements plugin;
    private AchievementManager manager;

    @BeforeEach
    void setUp() {

        server = MockBukkit.mock();
        plugin = MockBukkit.load(RCAchievements.class);
        manager = new AchievementManager(plugin);
    }

    @AfterEach
    void tearDown() {

        MockBukkit.unmock();
    }

    Optional<Achievement> loadAchievement(String alias, Consumer<ConfigurationSection> cfg) {

        MemoryConfiguration config = new MemoryConfiguration();
        config.set("type", TYPE);
        cfg.accept(config);
        return manager.loadAchievement(alias, config);
    }

    Optional<Achievement> loadAchievement(String alias) {

        return loadAchievement(alias, configurationSection -> {});
    }

    Optional<Achievement> loadAchievement() {

        return loadAchievement(RandomStringUtils.randomAlphabetic(20));
    }

    @Nested
    @DisplayName("register(...)")
    class TypeRegistration {

        @SneakyThrows
        @Test
        @DisplayName("should load achievements that failed to load after type registration")
        void shouldLoadFailedAchievementsAfterRegistration() {

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

            assertThatCode(() -> manager.register(new AchievementMockFactory()))
                    .doesNotThrowAnyException();

            assertThatExceptionOfType(TypeRegistrationException.class)
                    .isThrownBy(() -> manager.register("mock", NoneAchievementType.class, context -> mock(NoneAchievementType.class)));
        }
    }

    @Nested
    @DisplayName("load(...)")
    class AchievementLoading {

        @SneakyThrows
        @BeforeEach
        void setUp() {

            manager.register(new AchievementMockFactory());
        }

        @AfterEach
        void tearDown() {

            Achievement.find.all().forEach(achievement -> achievement.delete());
        }

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
    }
}