package de.raidcraft.achievements;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import de.raidcraft.achievements.entities.Achievement;
import de.raidcraft.achievements.entities.AchievementPlayer;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;
import java.util.function.Consumer;

import static de.raidcraft.achievements.AchievementMockFactory.TYPE;

@Getter
@Accessors(fluent = true)
public class TestBase {

    private ServerMock server;
    private RCAchievements plugin;
    private AchievementPlayer player;

    @SneakyThrows
    @BeforeEach
    void setUp() {

        server = MockBukkit.mock();
        plugin = MockBukkit.load(RCAchievements.class);
        player = AchievementPlayer.of(server.addPlayer());
        plugin.achievementManager().register(new AchievementMockFactory());
    }

    @AfterEach
    void tearDown() {

        MockBukkit.unmock();
    }

    protected Optional<Achievement> loadAchievement(String alias, Consumer<ConfigurationSection> cfg) {

        MemoryConfiguration config = new MemoryConfiguration();
        config.set("type", TYPE);
        cfg.accept(config);
        return plugin.achievementManager().loadAchievement(alias, config);
    }

    protected Optional<Achievement> loadAchievement(String alias) {

        return loadAchievement(alias, configurationSection -> {});
    }

    protected Achievement loadAchievement() {

        return loadAchievement(RandomStringUtils.randomAlphabetic(20)).get();
    }
}
