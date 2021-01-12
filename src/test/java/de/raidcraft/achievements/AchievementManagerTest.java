package de.raidcraft.achievements;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import de.raidcraft.achievements.entities.Achievement;
import lombok.SneakyThrows;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Consumer;

import static de.raidcraft.achievements.AchievementMockFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    @DisplayName("type registration")
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
    }
}